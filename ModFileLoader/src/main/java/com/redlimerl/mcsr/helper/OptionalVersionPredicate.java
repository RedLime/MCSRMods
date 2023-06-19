package com.redlimerl.mcsr.helper;

import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.version.VersionInterval;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class OptionalVersionPredicate<T extends VersionPredicate & VersionPredicate.PredicateTerm> implements VersionPredicate {


    private final List<T> predicates;

    public OptionalVersionPredicate(List<VersionPredicate> versionPredicates) {
        List<T> list = new ArrayList<>();
        for (VersionPredicate versionPredicate : versionPredicates) {
            for (PredicateTerm term : versionPredicate.getTerms()) {
                if (term instanceof VersionPredicate) {
                    //noinspection unchecked
                    list.add((T) term);
                }
            }
        }
        this.predicates = list;
    }

    @Override
    public boolean test(Version version) {
        Objects.requireNonNull(version, "null version");

        for (T predicate : predicates) {
            if (predicate.test(version)) return true;
        }

        return false;
    }

    public Version getMinVersion() {
        Version minVersion = null;
        for (T predicate : this.predicates) {
            Version v = predicate.getInterval().getMin();
            if (minVersion == null) minVersion = v;
            else if (v != null && v.compareTo(minVersion) < 0) minVersion = v;
        }
        if (minVersion == null) {
            try {
                minVersion = Version.parse("1.0");
            } catch (VersionParsingException ignored) {}
        }
        return minVersion;
    }

    @Override
    public List<? extends PredicateTerm> getTerms() {
        return predicates;
    }

    @Override
    public VersionInterval getInterval() {
        return VersionInterval.INFINITE;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof OptionalVersionPredicate<?> o) {
            return predicates.equals(o.predicates);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return predicates.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();

        for (T predicate : predicates) {
            if (ret.length() > 0) ret.append('|');
            ret.append(predicate.toString());
        }

        return ret.toString();
    }
}
