package com.minhtriet79.goldminer.domain.account;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Pure-Java SHA-256 password hashing and constant-time verification helper.
 */
public final class PasswordHasher {

    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    private PasswordHasher() {
    }

    public static String hash(String rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("Password must not be null");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            char[] hex = new char[bytes.length * 2];
            for (int i = 0; i < bytes.length; i++) {
                int v = bytes[i] & 0xFF;
                hex[i * 2] = HEX_DIGITS[v >>> 4];
                hex[i * 2 + 1] = HEX_DIGITS[v & 0x0F];
            }
            return new String(hex);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }

    public static boolean verify(String rawPassword, String expectedHexHash) {
        if (rawPassword == null || expectedHexHash == null || expectedHexHash.isEmpty()) {
            return false;
        }
        String actualHash = hash(rawPassword);
        return MessageDigest.isEqual(
            actualHash.getBytes(StandardCharsets.UTF_8),
            expectedHexHash.toLowerCase().getBytes(StandardCharsets.UTF_8)
        );
    }
}
