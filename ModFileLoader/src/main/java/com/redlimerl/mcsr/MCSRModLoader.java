package com.redlimerl.mcsr;

import com.google.gson.*;
import com.redlimerl.mcsr.mod.FabricMod;
import com.redlimerl.mcsr.mod.Loader;
import com.redlimerl.mcsr.mod.Optifine;
import com.redlimerl.mcsr.mod.abst.ModInfo;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MCSRModLoader {

    private static final Path MODS_JSON_PATH = Paths.get("../meta/v4/mods.json");
    private static final Path RESULT_JSON_PATH = Paths.get("../meta/v4/files.json");
    public static final Gson GSON = new GsonBuilder().serializeNulls().disableHtmlEscaping().create();
    public static String GITHUB_TOKEN = "none";

    public static void main(String[] args) throws Throwable {
        GITHUB_TOKEN = args[0];
        JsonArray modList = JsonParser.parseString(Files.readString(MODS_JSON_PATH)).getAsJsonArray();
        JsonArray jsonArray = new JsonArray();

        for (JsonElement jsonElement : modList) {
            String type = jsonElement.getAsJsonObject().get("type").getAsString();
            ModInfo modInfo = null;
            switch (type) {
                case "mod_loader" -> modInfo = GSON.fromJson(jsonElement, Loader.class);
                case "optifine" -> modInfo = GSON.fromJson(jsonElement, Optifine.class);
                case "fabric_mod" -> modInfo = GSON.fromJson(jsonElement, FabricMod.class);
            }

            if (modInfo == null) continue;

            System.out.printf("Start downloading '%s'...%n", modInfo.getName());
            jsonArray.add(modInfo.toJson());
            System.out.printf("Done with download '%s'%n", modInfo.getName());
        }

        Files.writeString(RESULT_JSON_PATH, GSON.toJson(jsonArray), StandardCharsets.UTF_8);
    }
}