package com.redlimerl.mcsr.mod.abst;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.redlimerl.mcsr.MCSRModLoader;
import com.redlimerl.mcsr.helper.OptionalVersionPredicate;

import java.util.List;

public record ModAsset(String modVersion, OptionalVersionPredicate<?> mcVersion, String fileName, String downloadUrl, String pageUrl, String sha1, int size, List<ModRule> rules) {
    public ModAsset(String modVersion, OptionalVersionPredicate<?> mcVersion, String fileName, String downloadUrl, String pageUrl) {
        this(modVersion, mcVersion, fileName, downloadUrl, pageUrl, null, 0, null);
    }

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("version", modVersion());

        JsonArray gameVersions = new JsonArray();
        for (String s : mcVersion().toString().split("\\|")) {
            gameVersions.add(s);
        }
        jsonObject.add("game_versions", gameVersions);
        jsonObject.addProperty("name", fileName());
        jsonObject.addProperty("url", downloadUrl());
        jsonObject.addProperty("page", pageUrl());
        jsonObject.addProperty("sha1", sha1());
        jsonObject.addProperty("size", size());
        if (rules() != null) jsonObject.add("rules", MCSRModLoader.GSON.toJsonTree(rules()));
        return jsonObject;
    }
}
