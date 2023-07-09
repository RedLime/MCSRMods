package com.redlimerl.mcsr;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.redlimerl.mcsr.helper.HttpRequestHelper;
import com.redlimerl.mcsr.helper.VersionPredicateHelper;
import com.redlimerl.mcsr.mod.abst.ModAsset;

import java.util.ArrayList;
import java.util.List;

public class Test {
    public static void main(String[] args) throws Throwable {

        List<ModAsset> list = new ArrayList<>();
        JsonArray jsonArray = HttpRequestHelper.getJsonFromUrl("https://api.modrinth.com/v2/project/atum/version").getAsJsonArray();

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

                list.add(new ModAsset(
                        releaseData.get("version_number").getAsString(),
                        VersionPredicateHelper.getFromStringArray(mcVersions),
                        fileName, fileData.get("url").getAsString(),
                        String.format("https://modrinth.com/mod/%s/version/%s", releaseData.get("project_id").getAsString(), releaseData.get("id").getAsString()),
                        fileData.getAsJsonObject("hashes").get("sha1").getAsString(),
                        fileData.getAsJsonObject("hashes").get("sha512").getAsString(),
                        fileData.get("size").getAsInt(),
                        null
                ));
            }
        }

        for (ModAsset modAsset : list) {
            System.out.println(modAsset);
        }
    }
}
