package com.redlimerl.mcsr.mod;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.redlimerl.mcsr.helper.HttpRequestHelper;
import com.redlimerl.mcsr.helper.VersionPredicateHelper;
import com.redlimerl.mcsr.mod.abst.ModAsset;
import com.redlimerl.mcsr.mod.abst.ModDownload;
import com.redlimerl.mcsr.mod.abst.ModInfo;

import java.util.List;
import java.util.SortedSet;

public class Loader extends ModInfo {

    public Loader(String name, String description, List<ModDownload> downloads, boolean recommended) {
        super(name, description, "mod_loader", downloads, recommended);
    }

    @Override
    protected void init(SortedSet<ModAsset> treeSet) throws Throwable {
        ModDownload loaderInfo = getDownloads().get(0);

        String loaderVersion = null;
        JsonArray versions = HttpRequestHelper.getJsonFromUrl(loaderInfo.getApiUrl()).getAsJsonArray();
        for (JsonElement version : versions) {
            if (version.getAsJsonObject().get("stable").getAsBoolean()) {
                loaderVersion = version.getAsJsonObject().get("version").getAsString();
                break;
            }
        }

        treeSet.add(new ModAsset(
                loaderVersion,
                VersionPredicateHelper.getFromStringArray(loaderInfo.getVersions()),
                "",
                null,
                loaderInfo.getPageUrl()
        ));
    }
}
