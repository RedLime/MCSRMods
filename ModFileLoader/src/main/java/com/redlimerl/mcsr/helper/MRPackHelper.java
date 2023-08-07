package com.redlimerl.mcsr.helper;

import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.redlimerl.mcsr.MCSRModLoader;
import com.redlimerl.mcsr.mod.abst.ModAsset;
import com.redlimerl.mcsr.mod.abst.ModInfo;
import com.redlimerl.mcsr.mod.abst.ModRule;
import net.fabricmc.loader.api.Version;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class MRPackHelper {

    private static final Set<String> WHITELISTED_NOT_RECOMMENDED = Sets.newHashSet("StandardSettings");

    public static JsonObject convertPack(String name, String gameVersion, ModInfo loader, Collection<ModInfo> mods, Map<String, String> rules) throws Throwable {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("formatVersion", 1);
        jsonObject.addProperty("game", "minecraft");
        jsonObject.addProperty("versionId", MCSRModLoader.VERSION + "+" + new SimpleDateFormat("yy.MM.dd").format(new Date()) + "-" + gameVersion);
        jsonObject.addProperty("name", name + " (for " + gameVersion + ")");

        JsonArray jsonArray = new JsonArray();
        for (ModInfo mod : mods) {
            if (!Objects.equals(mod.getType(), "fabric_mod")) continue;
            if (!mod.isRecommended() && !WHITELISTED_NOT_RECOMMENDED.contains(mod.getName())) continue;
            assetCheck: for (ModAsset modAsset : mod.getAssetsResult()) {
                if (!modAsset.mcVersion().test(Version.parse(gameVersion))) continue;
                if (modAsset.sha1() == null || modAsset.sha512() == null) continue;
                if (modAsset.rules() != null && !modAsset.rules().isEmpty()) {
                    for (ModRule rule : modAsset.rules()) {
                        boolean result = false;
                        for (Map.Entry<String, String> entry : rule.properties().entrySet()) {
                            if (!rules.containsKey(entry.getKey())) {
                                result = false;
                                break;
                            } else if (!rules.get(entry.getKey()).equals(entry.getValue())) {
                                result = false;
                                break;
                            } else {
                                result = true;
                            }
                        }
                        if (result != Objects.equals(rule.action(), "allow")) continue assetCheck;
                    }
                }

                jsonArray.add(getFileObject(modAsset.fileName(), modAsset.sha1(), modAsset.sha512(), modAsset.downloadUrl(), modAsset.size()));
                break;
            }
        }
        jsonObject.add("files", jsonArray);

        JsonObject dependencies = new JsonObject();
        dependencies.addProperty("fabric-loader", loader.getAssetsResult().get(0).modVersion());
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
}
