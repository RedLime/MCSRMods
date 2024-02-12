package com.redlimerl.mcsr;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.redlimerl.mcsr.helper.HttpRequestHelper;
import com.redlimerl.mcsr.helper.MRPackHelper;
import com.redlimerl.mcsr.mod.FabricLoader;
import com.redlimerl.mcsr.mod.ModInfo;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class MCSRModLoader {

    public static final String VERSION = "v4";
    private static final Path MODPACKS_PATH = Paths.get("./modpacks/" + VERSION);
    public static final Gson GSON = new GsonBuilder().serializeNulls().disableHtmlEscaping().create();
    public static String GITHUB_TOKEN = "none";

    public static void main(String[] args) throws Throwable {
        GITHUB_TOKEN = args[0];
        List<ModInfo> modInfoList = new ArrayList<>();
        FabricLoader fabricLoader = null;

        mod: for (JsonElement jsonElement : HttpRequestHelper.getJsonFromUrl("https://raw.githubusercontent.com/tildejustin/mcsr-meta/main/mods.json").getAsJsonObject().getAsJsonArray("mods")) {
            ModInfo modInfo = GSON.fromJson(jsonElement, ModInfo.class);
            for (ModInfo.ModVersion version : modInfo.versions()) {
                for (String s : version.target_version()) {
                    if (s.equals("1.16.1")) {
                        modInfoList.add(modInfo);
                        continue mod;
                    }
                }
            }
        }
        for (JsonElement jsonElement : HttpRequestHelper.getJsonFromUrl("https://meta.fabricmc.net/v2/versions/loader/").getAsJsonArray()) {
            FabricLoader loader = GSON.fromJson(jsonElement, FabricLoader.class);
            if (loader.stable()) {
                fabricLoader = loader;
                break;
            }
        }

        if (fabricLoader == null) return;



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
        Set<String> osSet = Set.of("Windows", "OSX", "Linux");
        Set<String> whitelist = Set.of("antigone", "fastreset", "krypton", "lazydfu", "lazystronghold", "lithium", "sodium", "starlight", "voyager");
        Set<String> proWhitelist = Sets.newHashSet("standardsettings", "antiresourcereload");
        proWhitelist.addAll(whitelist);
        Set<String> allWhitelist = Sets.newHashSet("atum", "state-output", "worldpreview", "forceport", "sleepbackground", "speedrunigt");
        allWhitelist.addAll(proWhitelist);
        Set<String> rankedOptions = Set.of("", "Pro", "All");
        
        for (String os : osSet) {
            for (String rankedOption : rankedOptions) {
                Map<String, String> map = new HashMap<>();
                map.put("os", os.toLowerCase(Locale.ROOT));
                List<ModInfo> rankedMods = modInfoList.stream().filter(mod ->
                        switch (rankedOption) {
                            case "" -> whitelist.contains(mod.modid());
                            case "Pro" -> proWhitelist.contains(mod.modid());
                            default -> allWhitelist.contains(mod.modid());
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