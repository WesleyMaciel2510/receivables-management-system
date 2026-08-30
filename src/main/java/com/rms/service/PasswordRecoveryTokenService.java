package com.rms.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PasswordRecoveryTokenService {

    public static final String PURPOSE_PASSWORD_RECOVERY = "password-recovery";
    public static final String PURPOSE_PASSWORD_UPDATE = "password-update";
    private static final String CLAIM_PURPOSE = "purpose";
    private static final String CLAIM_CODE_HASH = "code_hash";
    private static final Duration TOKEN_EXPIRATION = Duration.ofMinutes(10);

    private final SecretKey key;
    private final byte[] secretBytes;

    public PasswordRecoveryTokenService(@Value("${app.security.jwt.secret:${JWT_SECRET:0123456789abcdef0123456789abcdef}}") String secret) {
        this.secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(secretBytes);
    }

    public String generateRecoveryChallengeToken(Long userId, String verificationCode) {
        Instant now = Instant.now();
        String codeHash = hashCode(verificationCode);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_PURPOSE, PURPOSE_PASSWORD_RECOVERY)
                .claim(CLAIM_CODE_HASH, codeHash)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(TOKEN_EXPIRATION)))
                .signWith(key)
                .compact();
    }

    public String generateDummyRecoveryChallengeToken() {
        Instant now = Instant.now();
        String dummyHash = hashCode("000000");
        return Jwts.builder()
                .subject("dummy-nonexistent-user")
                .claim(CLAIM_PURPOSE, PURPOSE_PASSWORD_RECOVERY)
                .claim(CLAIM_CODE_HASH, dummyHash)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(TOKEN_EXPIRATION)))
                .signWith(key)
                .compact();
    }

    public Long validateRecoveryChallengeAndGetUserId(String token, String submittedCode) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String purpose = claims.get(CLAIM_PURPOSE, String.class);
            if (!PURPOSE_PASSWORD_RECOVERY.equals(purpose)) {
                return null;
            }

            String subject = claims.getSubject();
            if (subject == null || "dummy-nonexistent-user".equals(subject)) {
                return null;
            }

            String storedCodeHash = claims.get(CLAIM_CODE_HASH, String.class);
            if (storedCodeHash == null) {
                return null;
            }

            String submittedCodeHash = hashCode(submittedCode);
            if (!MessageDigest.isEqual(storedCodeHash.getBytes(StandardCharsets.UTF_8), submittedCodeHash.getBytes(StandardCharsets.UTF_8))) {
                return null;
            }

            return Long.valueOf(subject);
        } catch (Exception e) {
            return null;
        }
    }

    public String generatePasswordUpdateToken(Long userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_PURPOSE, PURPOSE_PASSWORD_UPDATE)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(TOKEN_EXPIRATION)))
                .signWith(key)
                .compact();
    }

    public Long validatePasswordUpdateTokenAndGetUserId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String purpose = claims.get(CLAIM_PURPOSE, String.class);
            if (!PURPOSE_PASSWORD_UPDATE.equals(purpose)) {
                return null;
            }

            String subject = claims.getSubject();
            if (subject == null || "dummy-nonexistent-user".equals(subject)) {
                return null;
            }

            return Long.valueOf(subject);
        } catch (Exception e) {
            return null;
        }
    }

    private String hashCode(String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretBytes, "HmacSHA256"));
            byte[] hash = mac.doFinal(code.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to calculate HMAC hash for code", e);
        }
    }
}
