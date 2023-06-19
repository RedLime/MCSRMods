package com.redlimerl.mcsr.mod.abst;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.redlimerl.mcsr.MCSRModLoader;

import java.util.*;

public abstract class ModInfo {
    private final String name;
    private final String description;
    private final String type;
    private final List<ModDownload> downloads;
    private final boolean recommended;
    private final List<String> incompatible;
    private SortedSet<ModAsset> assetsResult;

    public ModInfo(String name, String description, String type, List<ModDownload> downloads, boolean recommended) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.downloads = downloads;
        this.recommended = recommended;
        this.incompatible = null;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<ModDownload> getDownloads() {
        return downloads;
    }

    public boolean isRecommended() {
        return recommended;
    }

    public String getType() {
        return type;
    }

    public List<String> getIncompatible() {
        return incompatible;
    }

    protected abstract void init(SortedSet<ModAsset> treeSet) throws Throwable;

    public Set<ModAsset> getAssetsResult() throws Throwable {
        if (this.assetsResult == null) {
            this.assetsResult = new TreeSet<>(Comparator.comparing(asset -> asset.mcVersion().getMinVersion()));
            init(this.assetsResult);
        }
        return this.assetsResult;
    }

    public JsonObject toJson() throws Throwable {
        Set<ModAsset> results = this.getAssetsResult();
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("name", this.getName());
        jsonObject.addProperty("description", this.getDescription());
        jsonObject.addProperty("type", this.getType());

        JsonArray jsonArray = new JsonArray();
        for (ModAsset result : results) {
            jsonArray.add(result.toJson());
        }
        jsonObject.add("files", jsonArray);

        if (this.getIncompatible() != null) jsonObject.add("incompatible", MCSRModLoader.GSON.toJsonTree(this.getIncompatible()));

        jsonObject.addProperty("recommended", this.isRecommended());
        return jsonObject;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ModInfo) obj;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.description, that.description) &&
                Objects.equals(this.downloads, that.downloads) &&
                this.recommended == that.recommended;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, downloads, recommended);
    }

    @Override
    public String toString() {
        return "ModInfo[" +
                "name=" + name + ", " +
                "description=" + description + ", " +
                "downloads=" + downloads + ", " +
                "recommended=" + recommended + ']';
    }

}
