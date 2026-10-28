package com.skillswap.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Hashes a password using SHA-256 with a cryptographically secure 16-byte salt.
     * Returns "salt$hash" in Base64 encoding.
     */
    public static String hashPassword(String password) {
        if (password == null) return null;
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        String saltBase64 = Base64.getEncoder().encodeToString(salt);
        String hash = computeSha256(password, salt);
        return saltBase64 + "$" + hash;
    }

    /**
     * Verifies an entered password against a stored password.
     * Supports both salt$hash format and plain-text legacy seed passwords (e.g. initial admin).
     */
    public static boolean verifyPassword(String enteredPassword, String storedPassword) {
        if (enteredPassword == null || storedPassword == null) return false;
        if (storedPassword.contains("$")) {
            String[] parts = storedPassword.split("\\$", 2);
            if (parts.length == 2) {
                try {
                    byte[] salt = Base64.getDecoder().decode(parts[0]);
                    String expectedHash = computeSha256(enteredPassword, salt);
                    return expectedHash.equals(parts[1]);
                } catch (Exception e) {
                    return false;
                }
            }
        }
        // Backwards compatibility with initial seed accounts
        return storedPassword.equals(enteredPassword.trim());
    }

    private static String computeSha256(String password, byte[] salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hashed = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
