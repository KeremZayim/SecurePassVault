package com.securevault.utils;

import java.security.MessageDigest;
import java.util.HexFormat;

public final class HashUtils {

    private HashUtils() {}

    public static String sha256(String input) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 hashing failed", e);
        }
    }
}
