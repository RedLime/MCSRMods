package com.redlimerl.mcsr.helper;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.redlimerl.mcsr.MCSRModLoader;
import com.redlimerl.mcsr.mod.FabricLoader;
import com.redlimerl.mcsr.mod.ModInfo;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class MRPackHelper {

    public static JsonObject convertPack(String name, String gameVersion, FabricLoader loader, Collection<ModInfo> mods, Map<String, String> rules) throws Throwable {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("formatVersion", 1);
        jsonObject.addProperty("game", "minecraft");
        jsonObject.addProperty("versionId", MCSRModLoader.VERSION + "+" + new SimpleDateFormat("yy.MM.dd").format(new Date()) + "-" + gameVersion);
        jsonObject.addProperty("name", name + " (for " + gameVersion + ")");

        JsonArray jsonArray = new JsonArray();
        mod: for (ModInfo mod : mods) {
            for (String trait : mod.traits()) {
                if (trait.equals("mac-only") && !rules.getOrDefault("os", "").equals("osx")) continue mod;
            }

            for (ModInfo.ModVersion version : mod.versions()) {
                for (String s : version.target_version()) {
                    if (s.equals("1.16.1")) {
                        if (version.url().startsWith("https://github.com")) {
                            jsonArray.add(getGithubMeta(version.url(), version.hash()));
                        }
                        if (version.url().startsWith("https://cdn.modrinth.com")) {
                            jsonArray.add(getModrinthMeta(version.hash()));
                        }
                        continue mod;
                    }
                }
            }
        }
        jsonObject.add("files", jsonArray);

        JsonObject dependencies = new JsonObject();
        dependencies.addProperty("fabric-loader", loader.version());
        dependencies.addProperty("minecraft", gameVersion);
        jsonObject.add("dependencies", dependencies);

        return jsonObject;
    }

    public static JsonObject getFileObject(String fileName, String sha1, String sha512, String downloadUrl, int downloadSize) {
        JsonObject fileData = new JsonObject();
        fileData.addProperty("path", "mods/" + fileName);
        JsonObject hashes = new JsonObject();
        hashes.addProperty("sha1", sha1);
        hashes.addProperty("sha512", sha512);
        fileData.add("hashes", hashes);
        JsonObject env = new JsonObject();
        env.addProperty("client", "required");
        env.addProperty("server", "unsupported");
        fileData.add("env", env);
        JsonArray downloads = new JsonArray();
        downloads.add(downloadUrl);
        fileData.add("downloads", downloads);
        fileData.addProperty("fileSize", downloadSize);
        return fileData;
    }

    public static void writeZipFile(Path path, String packData) throws IOException {
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(path.toFile()));
        out.putNextEntry(new ZipEntry("modrinth.index.json"));
        out.write(packData.getBytes(StandardCharsets.UTF_8));
        out.closeEntry();
        out.close();
    }

    private static final Map<String, JsonElement> cachedApi = new HashMap<>();
    private static final Map<String, String> cachedSha1 = new HashMap<>();
    public static JsonObject getGithubMeta(String url, String sha512) throws IOException {
        Map.Entry<String, String> apiVersion = Map.entry("X-GitHub-Api-Version", "2022-11-28");
        Map.Entry<String, String> githubToken = Map.entry("Authorization", "token " + MCSRModLoader.GITHUB_TOKEN);

        url = url.replace("https://github.com/Minecraft-Java-Edition-Speedrunning/legal-mods/raw/main/", "https://api.github.com/repos/Minecraft-Java-Edition-Speedrunning/legal-mods/contents/");
        url = Arrays.stream(url.split("/")).filter(path -> !path.endsWith(".jar")).collect(Collectors.joining("/")) + "?ref=main";
        JsonArray jsonArray = cachedApi.containsKey(url) ? cachedApi.get(url).getAsJsonArray() : HttpRequestHelper.getJsonFromUrl(url, apiVersion, githubToken).getAsJsonArray();
        cachedApi.putIfAbsent(url, jsonArray);

        for (JsonElement jsonElement : jsonArray) {
            JsonObject data = jsonElement.getAsJsonObject();
            String download = data.get("download_url").getAsString();
            String sha1 = cachedSha1.containsKey(download) ? cachedSha1.get(download) : ShaHelper.getSha1FromInputStream(HttpRequestHelper.getInputStreamFromUrl(download));
            cachedSha1.putIfAbsent(download, sha1);
            return getFileObject(data.get("name").getAsString(), sha1, sha512, data.get("download_url").getAsString(), data.get("size").getAsInt());
        }

        return null;
    }
    public static JsonObject getModrinthMeta(String sha512) throws IOException {
        String url = "https://api.modrinth.com/v2/version_file/" + sha512 + "?algorithm=sha512";
        JsonObject jsonObject = cachedApi.containsKey(url) ? cachedApi.get(url).getAsJsonObject() : HttpRequestHelper.getJsonFromUrl(url).getAsJsonObject();
        cachedApi.putIfAbsent(url, jsonObject);

        for (JsonElement jsonElement : jsonObject.getAsJsonArray("files")) {
            JsonObject data = jsonElement.getAsJsonObject();
            return getFileObject(data.get("filename").getAsString(), data.getAsJsonObject("hashes").get("sha1").getAsString(), sha512, data.get("url").getAsString(), data.get("size").getAsInt());
        }

        return null;
    }
}
