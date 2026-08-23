package com.rms.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequest(
        @NotNull UUID debtorAccountId,
        @NotNull UUID creditorAccountId,
        @NotNull UUID receivableId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        UUID idempotencyKey) {
}
