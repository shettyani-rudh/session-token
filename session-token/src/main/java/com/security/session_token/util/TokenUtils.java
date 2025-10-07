package com.security.session_token.util;



import java.security.SecureRandom;
import java.util.Base64;

public class TokenUtils {
    private static final SecureRandom rng = new SecureRandom();

    /**
     * Generates a URL-safe opaque token string (base64url, no padding).
     * Default: 32 bytes (256 bits).
     */
    public static String generateOpaqueToken() {
        return generateOpaqueToken(32);
    }

    public static String generateOpaqueToken(int numBytes) {
        byte[] random = new byte[numBytes];
        rng.nextBytes(random);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    }
}

