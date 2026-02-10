package com.redlimerl.mcsr.helper;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.redlimerl.mcsr.MCSRModLoader;
import com.redlimerl.mcsr.mod.FabricLoader;
import com.redlimerl.mcsr.mod.ModInfo;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
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
                        jsonArray.add(getFileObject(version.url().substring(version.url().lastIndexOf("/") + 1), version.sha1(), version.sha512(), version.url(), version.size()));
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
        Files.createDirectories(path.getParent());
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(path.toFile()));
        out.putNextEntry(new ZipEntry("modrinth.index.json"));
        out.write(packData.getBytes(StandardCharsets.UTF_8));
        out.closeEntry();
        out.close();
    }
}
