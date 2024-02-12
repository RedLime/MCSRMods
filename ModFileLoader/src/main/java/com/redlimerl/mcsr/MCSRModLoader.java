package com.redlimerl.mcsr;

import com.google.gson.*;
import com.redlimerl.mcsr.helper.HttpRequestHelper;
import com.redlimerl.mcsr.helper.MRPackHelper;
import com.redlimerl.mcsr.mod.FabricMod;
import com.redlimerl.mcsr.mod.Loader;
import com.redlimerl.mcsr.mod.Optifine;
import com.redlimerl.mcsr.mod.abst.ModInfo;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class MCSRModLoader {

    public static final String VERSION = "v4";
    private static final Path MODS_JSON_PATH = Paths.get("./meta/" + VERSION + "/mods.json");
    private static final Path RESULT_JSON_PATH = Paths.get("./meta/" + VERSION + "/files.json");
    private static final Path MODPACKS_PATH = Paths.get("./modpacks/" + VERSION);
    public static final Gson GSON = new GsonBuilder().serializeNulls().disableHtmlEscaping().create();
    public static String GITHUB_TOKEN = "none";

    public static void main(String[] args) throws Throwable {
        GITHUB_TOKEN = args[0];
        JsonArray modList = JsonParser.parseString(Files.readString(MODS_JSON_PATH)).getAsJsonArray();
        JsonArray jsonArray = new JsonArray();
        List<ModInfo> modInfoList = new ArrayList<>();
        ModInfo fabricLoader = null;

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
            modInfoList.add(modInfo);
            jsonArray.add(modInfo.toJson());
            System.out.printf("Done with download '%s'%n", modInfo.getName());

            if (Objects.equals(modInfo.getName(), "Fabric Loader")) fabricLoader = modInfo;
        }

        Files.writeString(RESULT_JSON_PATH, GSON.toJson(jsonArray), StandardCharsets.UTF_8);

        if (fabricLoader == null) return;

        /*
          For generate .mrpack files
         */

        Set<String> osSet = Set.of("Windows", "OSX", "Linux");
        Set<String> categorySet = Set.of("RSG", "SSG");
        Set<String> versionSet = Set.of("1.14.4", "1.15.2", "1.16.1", "1.16.5", "1.17.1");

        for (String os : osSet) {
            for (String category : categorySet) {
                for (String version : versionSet) {
                    Map<String, String> map = new HashMap<>();
                    map.put("os", os.toLowerCase(Locale.ROOT));
                    map.put("category", category.toLowerCase(Locale.ROOT));
                    JsonObject mrPack = MRPackHelper.convertPack("MCSR " + category, version, fabricLoader, modInfoList, map);
                    Path path = MODPACKS_PATH.resolve(String.format("MCSR-%s-%s-%s.mrpack", version, os, category));

                    MRPackHelper.writeZipFile(path, GSON.toJson(mrPack));
                }
            }
        }


        /*
          For generate MCSR Ranked .mrpack files
         */

        JsonObject rankedModrinth = HttpRequestHelper.getJsonFromUrl("https://api.modrinth.com/v2/project/mcsr-ranked/version?featured=true")
                .getAsJsonArray().get(0).getAsJsonObject()
                .getAsJsonArray("files").get(0).getAsJsonObject();
        JsonObject ranked = MRPackHelper.getFileObject(
                rankedModrinth.get("filename").getAsString(),
                rankedModrinth.getAsJsonObject("hashes").get("sha1").getAsString(),
                rankedModrinth.getAsJsonObject("hashes").get("sha512").getAsString(),
                rankedModrinth.get("url").getAsString(),
                rankedModrinth.get("size").getAsInt()
        );
        Set<String> rankedBlocked = Set.of("ServerSideRNG", "WorldPreview", "Atum", "Force Port", "SleepBackground");
        Set<String> rankedPro = Set.of("StandardSettings", "antiresourcereload");
        Set<String> rankedOptions = Set.of("", "Pro", "All");
        for (String os : osSet) {
            for (String rankedOption : rankedOptions) {
                Map<String, String> map = new HashMap<>();
                map.put("os", os.toLowerCase(Locale.ROOT));
                map.put("category", "rsg");
                List<ModInfo> rankedMods = modInfoList.stream().filter(mod ->
                        switch (rankedOption) {
                            case "" -> !rankedBlocked.contains(mod.getName()) && !rankedPro.contains(mod.getName());
                            case "Pro" -> !rankedBlocked.contains(mod.getName());
                            default -> true;
                        }
                        ).toList();
                JsonObject mrPack = MRPackHelper.convertPack("MCSR Ranked", "1.16.1", fabricLoader, rankedMods, map);

                mrPack.getAsJsonArray("files").add(ranked);

                Path path = MODPACKS_PATH.resolve(String.format("MCSRRanked-%s-1.16.1%s.mrpack", os, rankedOption.isBlank() ? "" : ("-" + rankedOption)));
                MRPackHelper.writeZipFile(path, GSON.toJson(mrPack));
            }
        }
    }
}