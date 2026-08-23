package com.rms.security;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.rms.config.SecurityProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class JwtServiceTests {
    private static final String SECRET = "test-secret-with-at-least-32-characters-1234567890";
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    private final JwtService service = new JwtService(new SecurityProperties(
            new SecurityProperties.Jwt("test-issuer", SECRET, 30),
            new SecurityProperties.Bootstrap(false, "", "", "ROLE_USER"),
            new SecurityProperties.Cors("http://localhost:3000")));

    @Test
    void validatesGeneratedTokenAndAuthorities() {
        JwtService.TokenData generated = service.generate("user", List.of(new SimpleGrantedAuthority("ROLE_USER")));

        assertEquals("user", service.parseAndValidate(generated.token()).getSubject());
        assertEquals("test-issuer", service.parseAndValidate(generated.token()).getIssuer());
    }

    @Test
    void rejectsMalformedExpiredWrongIssuerAndWrongSignatureTokens() {
        assertThrows(RuntimeException.class, () -> service.parseAndValidate("not-a-jwt"));
        assertThrows(RuntimeException.class, () -> service.parseAndValidate(signedToken(
                "test-issuer", Instant.now().minusSeconds(120), Instant.now().minusSeconds(60), KEY)));
        assertThrows(RuntimeException.class, () -> service.parseAndValidate(signedToken(
                "another-issuer", Instant.now(), Instant.now().plusSeconds(60), KEY)));
        SecretKey wrongKey = Keys.hmacShaKeyFor(
                "different-secret-with-at-least-32-characters-1234567890".getBytes(StandardCharsets.UTF_8));
        assertThrows(RuntimeException.class, () -> service.parseAndValidate(signedToken(
                "test-issuer", Instant.now(), Instant.now().plusSeconds(60), wrongKey)));
    }

    private static String signedToken(String issuer, Instant issuedAt, Instant expiration, SecretKey key) {
        return Jwts.builder()
                .subject("user")
                .issuer(issuer)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiration))
                .signWith(key)
                .compact();
    }
}
