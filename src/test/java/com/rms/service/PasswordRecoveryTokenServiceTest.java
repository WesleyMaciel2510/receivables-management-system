package com.rms.service;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PasswordRecoveryTokenServiceTest {

    private PasswordRecoveryTokenService service;

    @BeforeEach
    void setUp() {
        service = new PasswordRecoveryTokenService("0123456789abcdef0123456789abcdef0123456789abcdef");
    }

    @Test
    void testRecoveryChallengeTokenSuccess() {
        String token = service.generateRecoveryChallengeToken(123L, "654321");
        assertNotNull(token);

        Long userId = service.validateRecoveryChallengeAndGetUserId(token, "654321");
        assertEquals(123L, userId);
    }

    @Test
    void testRecoveryChallengeTokenWrongCode() {
        String token = service.generateRecoveryChallengeToken(123L, "654321");
        Long userId = service.validateRecoveryChallengeAndGetUserId(token, "999999");
        assertNull(userId);
    }

    @Test
    void testDummyTokenReturnsNull() {
        String token = service.generateDummyRecoveryChallengeToken();
        Long userId = service.validateRecoveryChallengeAndGetUserId(token, "000000");
        assertNull(userId);
    }

    @Test
    void testPasswordUpdateTokenSuccess() {
        String token = service.generatePasswordUpdateToken(456L);
        assertNotNull(token);

        Long userId = service.validatePasswordUpdateTokenAndGetUserId(token);
        assertEquals(456L, userId);
    }

    @Test
    void testExpiredTokenValidation() {
        Instant past = Instant.now().minusSeconds(100);
        String token = io.jsonwebtoken.Jwts.builder()
                .subject("123")
                .claim("purpose", "password-recovery")
                .claim("code_hash", "some-hash")
                .claim("iat", past.minusSeconds(60).getEpochSecond())
                .claim("exp", past.getEpochSecond())
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor("0123456789abcdef0123456789abcdef0123456789abcdef".getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .compact();

        Long userId = service.validateRecoveryChallengeAndGetUserId(token, "123456");
        assertNull(userId);
    }
}
