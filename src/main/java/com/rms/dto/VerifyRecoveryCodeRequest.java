package com.rms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyRecoveryCodeRequest(
        @NotBlank(message = "challengeToken is required")
        String challengeToken,

        @NotBlank(message = "code is required")
        @Pattern(regexp = "^\\d{6}$", message = "Verification code must be 6 numeric digits")
        String code
) {
}
