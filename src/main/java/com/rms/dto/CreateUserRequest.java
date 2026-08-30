package com.rms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CreateUserRequest", description = "Dados para criação de um novo usuário.")
public record CreateUserRequest(
        @Schema(example = "johndoe")
        @NotBlank @Size(max = 60) String username,
        @Schema(example = "SecureP@ssw0rd!")
        @NotBlank @Size(min = 8, max = 255) String password,
        @Schema(example = "john.doe@example.com")
        @NotBlank @Email @Size(max = 254) String email,
        @Schema(example = "John")
        @NotBlank @Size(max = 100) String firstName,
        @Schema(example = "Doe")
        @NotBlank @Size(max = 100) String lastName,
        @Schema(example = "some-table-ref") String linkedTable) {
}
