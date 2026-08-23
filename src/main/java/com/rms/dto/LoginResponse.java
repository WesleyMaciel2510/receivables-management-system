package com.rms.dto;

import java.time.Instant;
import java.util.List;

public record LoginResponse(String tokenType, String accessToken, String issuer, String subject,
        Instant issuedAt, Instant expiresAt, List<String> authorities) {
}
