package com.security.session_token.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TokenUtilsTest {
    @Test
    void generateOpaqueToken_lengthUnique() {
        String t1 = TokenUtils.generateOpaqueToken();
        String t2 = TokenUtils.generateOpaqueToken();
        assertNotNull(t1);
        assertNotNull(t2);
        assertNotEquals(t1, t2);
        assertTrue(t1.length() >= 43); // base64url for 32 bytes ~= 43 chars
    }
}
