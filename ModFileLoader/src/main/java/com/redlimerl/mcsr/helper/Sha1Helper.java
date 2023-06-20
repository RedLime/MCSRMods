package com.redlimerl.mcsr.helper;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Sha1Helper {
    public static String getFromInputStream(InputStream inputStream) throws NoSuchAlgorithmException, IOException {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        int n = 0;
        byte[] buffer = new byte[8192];
        while (n != -1) {
            n = inputStream.read(buffer);
            if (n > 0) {
                digest.update(buffer, 0, n);
            }
        }

        byte[] hash = digest.digest();

        StringBuilder sb = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            sb.append(Integer.toString((b & 0xf0) >> 4, 16));
            sb.append(Integer.toString(b & 0x0f, 16));
        }
        return sb.toString();
    }
}
