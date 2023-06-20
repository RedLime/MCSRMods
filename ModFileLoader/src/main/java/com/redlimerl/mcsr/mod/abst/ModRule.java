package com.redlimerl.mcsr.mod.abst;

import com.redlimerl.mcsr.MCSRModLoader;

import java.util.Map;
import java.util.Objects;

public final class ModRule {
    private String action;
    private Map<String, String> properties;

    public ModRule(String action, Map<String, String> properties) {
        this.action = action;
        this.properties = properties;
    }

    public String action() {
        return action;
    }

    public Map<String, String> properties() {
        return properties;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ModRule) obj;
        return Objects.equals(this.action, that.action) &&
                Objects.equals(MCSRModLoader.GSON.toJson(this.properties), MCSRModLoader.GSON.toJson(that.properties));
    }

    @Override
    public int hashCode() {
        return Objects.hash(action, properties);
    }

    @Override
    public String toString() {
        return "ModRule[" +
                "action=" + action + ", " +
                "properties=" + properties + ']';
    }

}
