package com.redlimerl.mcsr.helper;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.io.InputStream;

public class ShaHelper {

    public static String getSha1FromInputStream(InputStream inputStream) throws IOException {
        return DigestUtils.sha1Hex(inputStream);
    }

    public static String getSha512FromInputStream(InputStream inputStream) throws IOException {
        return DigestUtils.sha512Hex(inputStream);
    }
}
