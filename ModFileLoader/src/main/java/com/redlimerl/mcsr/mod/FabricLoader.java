package com.redlimerl.mcsr.mod;

import java.util.Objects;

public final class FabricLoader {
    private String version;
    private boolean stable;

    public FabricLoader(String version, boolean stable) {
        this.version = version;
        this.stable = stable;
    }

    public String version() {
        return version;
    }

    public boolean stable() {
        return stable;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (FabricLoader) obj;
        return Objects.equals(this.version, that.version) &&
                this.stable == that.stable;
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, stable);
    }

    @Override
    public String toString() {
        return "FabricLoader[" +
                "version=" + version + ", " +
                "stable=" + stable + ']';
    }

}
