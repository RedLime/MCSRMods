package com.redlimerl.mcsr.mod;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.redlimerl.mcsr.MCSRModLoader;
import com.redlimerl.mcsr.helper.HttpRequestHelper;
import com.redlimerl.mcsr.helper.ShaHelper;
import com.redlimerl.mcsr.helper.VersionPredicateHelper;
import com.redlimerl.mcsr.mod.abst.*;
import net.fabricmc.loader.api.Version;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FabricMod extends ModInfo {

    private record VersionRulesIdentifier(Version version, List<ModRule> rules) {}
    private Set<VersionRulesIdentifier> minVersionSet = new HashSet<>();

    public FabricMod(String name, String description, List<ModDownload> downloads, boolean recommended) {
        super(name, description, "fabric_mod", downloads, recommended);
    }

    @Override
    protected void init(List<ModAsset> modAssetList) throws Throwable {
        this.minVersionSet = new HashSet<>();
        for (ModDownload download : this.getDownloads()) {
            switch (download.getType()) {
                case "github" -> modAssetList.addAll(this.getAssetFromGithub(download));
                case "modrinth" -> modAssetList.addAll(this.getAssetFromModrinth(download));
                case "direct" -> modAssetList.add(this.getAssetFromDirect(download));
            }
        }
    }

    private ModAsset getAssetFromDirect(ModDownload download) throws Throwable {
        ByteArrayInputStream inputStream = HttpRequestHelper.getInputStreamFromUrl(download.getApiUrl());
        int length = inputStream.available();
        JsonObject modJson = this.getFabricJsonFromInputStream(inputStream);
        inputStream.reset();
        String sha1 = ShaHelper.getSha1FromInputStream(inputStream);
        inputStream.reset();
        String sha512 = ShaHelper.getSha512FromInputStream(inputStream);
        String[] url = download.getApiUrl().split("/");

        assert modJson != null;
        return new ModAsset(
                modJson.get("version").getAsString(),
                VersionPredicateHelper.getFromStringArray(download.getVersions()),
                url[url.length - 1],
                download.getApiUrl(),
                null,
                sha1,
                sha512,
                length,
                download.getRules()
        );
    }

    private Collection<ModAsset> getAssetFromModrinth(ModDownload download) throws Throwable {
        List<ModAsset> list = new ArrayList<>();
        JsonArray jsonArray = HttpRequestHelper.getJsonFromUrl(download.getApiUrl()).getAsJsonArray();

        for (JsonElement jsonElement : jsonArray) {
            JsonObject releaseData = jsonElement.getAsJsonObject();
            JsonArray gameVersions = releaseData.get("game_versions").getAsJsonArray();
            String[] mcVersions = new String[gameVersions.size()];
            for (int i = 0; i < gameVersions.size(); i++) {
                mcVersions[i] = gameVersions.get(i).getAsString();
            }

            for (JsonElement fileElement : releaseData.getAsJsonArray("files")) {
                JsonObject fileData = fileElement.getAsJsonObject();

                String fileName = fileData.get("filename").getAsString();
                if (fileName.contains("-source")) continue;

                ModAsset modAsset = new ModAsset(
                        releaseData.get("version_number").getAsString(),
                        VersionPredicateHelper.getFromStringArray(mcVersions),
                        fileName, fileData.get("url").getAsString(),
                        String.format("https://modrinth.com/mod/%s/version/%s", releaseData.get("project_id").getAsString(), releaseData.get("id").getAsString()),
                        fileData.getAsJsonObject("hashes").get("sha1").getAsString(),
                        fileData.getAsJsonObject("hashes").get("sha512").getAsString(),
                        fileData.get("size").getAsInt(),
                        download.getRules()
                );
                if (modAsset.mcVersion().toString().isBlank() || modAsset.mcVersion().toString().contains("*") || !VersionPredicateHelper.getFromStringArray(download.getVersions()).test(modAsset.mcVersion().getMinVersion())) continue;

                boolean add = this.hasVersionRange(modAsset);
                if (add) list.add(modAsset);
            }

            if (list.size() > 0 && download.isRequireLatestOnly()) break;
        }
        return list;
    }

    private Collection<ModAsset> getAssetFromGithub(ModDownload download) throws Throwable {
        Map.Entry<String, String> apiVersion = Map.entry("X-GitHub-Api-Version", "2022-11-28");
        Map.Entry<String, String> githubToken = Map.entry("Authorization", "token " + MCSRModLoader.GITHUB_TOKEN);

        List<ModAsset> list = new ArrayList<>();
        JsonArray jsonArray = HttpRequestHelper.getJsonFromUrl(download.getApiUrl(), apiVersion, githubToken).getAsJsonArray();

        boolean shouldCheckPreRelease = true;
        for (JsonElement jsonElement : jsonArray) {
            JsonObject releaseData = jsonElement.getAsJsonObject();
            if (releaseData.get("draft").getAsBoolean()) continue;
            if (!releaseData.get("prerelease").getAsBoolean()) {
                shouldCheckPreRelease = false;
                break;
            }
        }

        for (JsonElement jsonElement : jsonArray) {
            JsonObject releaseData = jsonElement.getAsJsonObject();
            if (releaseData.get("draft").getAsBoolean()) continue;
            if (releaseData.get("prerelease").getAsBoolean() && !shouldCheckPreRelease) continue;

            String pageUrl = releaseData.get("html_url").getAsString();
            List<JsonElement> assetList = Lists.newArrayList();
            for (JsonElement asset : releaseData.getAsJsonArray("assets")) assetList.add(asset);
            assetList.sort(Collections.reverseOrder(Comparator.comparing(element -> element.getAsJsonObject().get("name").getAsString())));

            for (JsonElement asset : assetList) {
                JsonObject assetData = asset.getAsJsonObject();
                String fileName = assetData.get("name").getAsString();
                String downloadUrl = assetData.get("browser_download_url").getAsString();

                if (fileName.contains("-source") || !fileName.endsWith(".jar")) continue;

                ByteArrayInputStream inputStream = HttpRequestHelper.getInputStreamFromUrl(downloadUrl, apiVersion, githubToken);
                JsonObject modJson = getFabricJsonFromInputStream(inputStream);
                inputStream.reset();
                if (modJson == null) continue;
                String sha1Hash = ShaHelper.getSha1FromInputStream(inputStream);
                inputStream.reset();
                String sha512Hash = ShaHelper.getSha512FromInputStream(inputStream);

                String modVersion = modJson.get("version").getAsString();
                String[] mcVersions = download.getVersions();

                if (modJson.has("depends") && modJson.getAsJsonObject("depends").has("minecraft")) {
                    JsonElement mcVerElement = modJson.getAsJsonObject("depends").get("minecraft");
                    if (mcVerElement.isJsonArray()) {
                        mcVersions = new String[mcVerElement.getAsJsonArray().size()];
                        for (int i = 0; i < mcVerElement.getAsJsonArray().size(); i++) {
                            mcVersions[i] = mcVerElement.getAsJsonArray().get(i).getAsString();
                        }
                    } else {
                        mcVersions = new String[] { mcVerElement.getAsString() };
                    }
                }
                if (download.getOverride() != null) {
                    for (VersionOverride versionOverride : download.getOverride()) {
                        if (versionOverride.getVersion().equals("*") || modVersion.equals(versionOverride.getVersion()) || fileName.equals(versionOverride.getVersion())) {
                            mcVersions = versionOverride.getGameVersions();
                            break;
                        }
                    }
                }

                ModAsset modAsset = new ModAsset(
                        modVersion,
                        VersionPredicateHelper.getFromStringArray(mcVersions),
                        fileName, downloadUrl, pageUrl,
                        sha1Hash, sha512Hash, assetData.get("size").getAsInt(), download.getRules()
                );
                if (modAsset.mcVersion().toString().isBlank() || modAsset.mcVersion().toString().contains("*") || !VersionPredicateHelper.getFromStringArray(download.getVersions()).test(modAsset.mcVersion().getMinVersion())) continue;

                boolean add = this.hasVersionRange(modAsset);
                if (add) list.add(modAsset);
            }

            if (list.size() > 0 && download.isRequireLatestOnly()) break;
        }

        return list;
    }

    private JsonObject getFabricJsonFromInputStream(InputStream inputStream) throws Throwable {
        ZipInputStream zipInputStream = new ZipInputStream(inputStream);
        JsonElement result = null;
        ZipEntry entry;
        while ((entry = zipInputStream.getNextEntry()) != null) {
            if (entry.getName().equals("fabric.mod.json")) {
                result = JsonParser.parseReader(new InputStreamReader(zipInputStream));
                zipInputStream.closeEntry();
                break;
            }
        }
        return result == null ? null : result.getAsJsonObject();
    }

    private boolean hasVersionRange(ModAsset modAsset) {
        for (VersionRulesIdentifier version : minVersionSet) {
            if (modAsset.mcVersion().getMinVersion().compareTo(version.version) == 0 && Objects.equals(version.rules, modAsset.rules())) {
                return false;
            }
        }
        minVersionSet.add(new VersionRulesIdentifier(modAsset.mcVersion().getMinVersion(), modAsset.rules()));
        return true;
    }
}
