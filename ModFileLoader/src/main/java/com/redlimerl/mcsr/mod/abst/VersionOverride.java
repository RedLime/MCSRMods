package com.redlimerl.mcsr.mod.abst;

import java.util.Objects;

public final class VersionOverride {
    private String version;
    private String game_versions;

    public VersionOverride(String version, String game_versions) {
        this.version = version;
        this.game_versions = game_versions;
    }

    public String[] getGameVersions() {
        return this.game_versions.split(" \\|\\| ");
    }

    public String getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (VersionOverride) obj;
        return Objects.equals(this.version, that.version) &&
                Objects.equals(this.game_versions, that.game_versions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, game_versions);
    }

    @Override
    public String toString() {
        return "VersionOverride[" +
                "version=" + version + ", " +
                "game_versions=" + game_versions + ']';
    }

}
