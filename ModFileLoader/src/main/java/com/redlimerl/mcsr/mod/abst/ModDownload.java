package com.redlimerl.mcsr.mod.abst;

import java.util.List;
import java.util.Objects;

public final class ModDownload {
    private final String versions;
    private final String type;
    private final String page_url;
    private final String api_url;
    private final List<ModRule> rules;
    private final boolean latest_only;

    public ModDownload(String versions, String type, String page_url, String api_url, List<ModRule> rules, boolean latest_only) {
        this.versions = versions;
        this.type = type;
        this.page_url = page_url;
        this.api_url = api_url;
        this.rules = rules;
        this.latest_only = latest_only;
    }

    public String[] getVersions() {
        return versions.split(" \\|\\| ");
    }

    public String getType() {
        return type;
    }

    public String getPageUrl() {
        return page_url;
    }

    public String getApiUrl() {
        return api_url;
    }

    public List<ModRule> getRules() {
        return rules;
    }

    public boolean isRequireLatestOnly() {
        return latest_only;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ModDownload) obj;
        return Objects.equals(this.versions, that.versions) &&
                Objects.equals(this.type, that.type) &&
                Objects.equals(this.page_url, that.page_url) &&
                Objects.equals(this.api_url, that.api_url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(versions, type, page_url, api_url);
    }

    @Override
    public String toString() {
        return "ModDownload[" +
                "versions=" + versions + ", " +
                "type=" + type + ", " +
                "page_url=" + page_url + ", " +
                "api_url=" + api_url + ']';
    }

}
