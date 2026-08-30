package com.rms.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdatePasswordRequest(
        @NotBlank(message = "passwordUpdateToken is required")
        String passwordUpdateToken,

        @NotBlank(message = "newPassword is required")
        String newPassword
) {
}
