package com.redlimerl.mcsr.helper;

import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;
import net.fabricmc.loader.impl.util.version.VersionPredicateParser;

import java.util.ArrayList;
import java.util.List;

public class VersionPredicateHelper {
    public static OptionalVersionPredicate<?> getFromStringArray(String[] array) throws VersionParsingException {
        List<VersionPredicate> versionPredicate = new ArrayList<>();
        for (String s : array) {
            VersionPredicate vp = VersionPredicateParser.parse(s);
            versionPredicate.add(vp);
        }
        return new OptionalVersionPredicate<>(versionPredicate);
    }
}
