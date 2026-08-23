package com.rms.dto;

import com.rms.model.ClientStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ClientRequest", description = "Dados para criação ou atualização de um cliente.")
public record ClientRequest(
        @Schema(example = "Maria Silva")
        @NotBlank @Size(max = 160) String name,
        @Schema(description = "CPF ou CNPJ com ou sem pontuação.", example = "123.456.789-09")
        @NotBlank @Size(max = 18) String cpfCnpj,
        @Schema(example = "maria.silva@example.com")
        @NotBlank @Email @Size(max = 254) String email,
        @Schema(example = "+55 11 99999-0000")
        @NotBlank @Size(max = 32) String phone,
        @Schema(example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE"})
        @NotNull ClientStatus status) {
}
