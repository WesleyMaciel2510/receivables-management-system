package com.rms.dto;

public record PasswordRecoveryResponse(
        String message,
        String challengeToken
) {
}
