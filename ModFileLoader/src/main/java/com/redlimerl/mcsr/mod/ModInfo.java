package com.redlimerl.mcsr.mod;

import java.util.List;
import java.util.Objects;

public final class ModInfo {
    private String modid;
    private List<ModVersion> versions;
    private List<String> traits;

    public ModInfo(String modid, List<ModVersion> versions, List<String> traits) {
        this.modid = modid;
        this.versions = versions;
        this.traits = traits;
    }

    public String modid() {
        return modid;
    }

    public List<ModVersion> versions() {
        return versions;
    }

    public List<String> traits() {
        return Objects.requireNonNullElse(traits, List.of());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ModInfo) obj;
        return Objects.equals(this.modid, that.modid) &&
                Objects.equals(this.versions, that.versions) &&
                Objects.equals(this.traits, that.traits);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modid, versions, traits);
    }

    @Override
    public String toString() {
        return "ModInfo[" +
                "modid=" + modid + ", " +
                "versions=" + versions + ", " +
                "traits=" + traits + ']';
    }

    public static final class ModVersion {
        private List<String> target_version;
        private String url;
        private String sha1;
        private String sha512;
        private int size;

        public ModVersion(List<String> target_version, String url, String sha1, String sha512, int size) {
            this.target_version = target_version;
            this.url = url;
            this.sha1 = sha1;
            this.sha512 = sha512;
            this.size = size;
        }

        public List<String> target_version() {
            return target_version;
        }

        public String url() {
            return url;
        }

        public String sha1() {
            return sha1;
        }

        public String sha512() {
            return sha512;
        }

        public int size() {
            return size;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (ModVersion) obj;
            return Objects.equals(this.target_version, that.target_version) &&
                    Objects.equals(this.url, that.url) &&
                    Objects.equals(this.sha1, that.sha1) &&
                    Objects.equals(this.sha512, that.sha512) &&
                    this.size == that.size;
        }

        @Override
        public int hashCode() {
            return Objects.hash(target_version, url, sha1, sha512, size);
        }

        @Override
        public String toString() {
            return "ModVersion[" +
                    "target_version=" + target_version + ", " +
                    "url=" + url + ", " +
                    "sha1=" + sha1 + ", " +
                    "sha512=" + sha512 + ", " +
                    "size=" + size + ']';
        }

        }
}

