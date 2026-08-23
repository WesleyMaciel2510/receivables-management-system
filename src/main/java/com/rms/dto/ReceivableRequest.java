package com.rms.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(name = "ReceivableRequest", description = "Dados para criação ou atualização de um recebível.")
public record ReceivableRequest(
        @Schema(example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d")
        @NotNull UUID clientId,
        @Schema(example = "Invoice 1001")
        @NotBlank @Size(max = 500) String description,
        @Schema(example = "125.50", minimum = "0.01")
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @Schema(example = "2026-12-31")
        @NotNull @FutureOrPresent LocalDate dueDate) {
}
