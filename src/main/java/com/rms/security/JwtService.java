package com.rms.security;

import com.rms.config.SecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final SecurityProperties properties;
    private final SecretKey signingKey;

    public JwtService(SecurityProperties properties) {
        this.properties = properties;
        if (properties.jwt().secret() == null || properties.jwt().secret().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("APP_SECURITY_JWT_SECRET must contain at least 32 bytes");
        }
        this.signingKey = Keys.hmacShaKeyFor(properties.jwt().secret().getBytes(StandardCharsets.UTF_8));
    }

    public TokenData generate(String subject, List<? extends GrantedAuthority> authorities) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(properties.jwt().accessTokenMinutes() * 60);
        String token = Jwts.builder()
                .subject(subject)
                .issuer(properties.jwt().issuer())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .claim("authorities", authorities.stream().map(GrantedAuthority::getAuthority).toList())
                .signWith(signingKey)
                .compact();
        return new TokenData(token, subject, properties.jwt().issuer(), issuedAt, expiresAt,
                authorities.stream().map(GrantedAuthority::getAuthority).toList());
    }

    public Claims parseAndValidate(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(properties.jwt().issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public record TokenData(String token, String subject, String issuer, Instant issuedAt,
            Instant expiresAt, List<String> authorities) {}
}
