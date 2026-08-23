package com.rms.dto;

import com.rms.model.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record BankAccountRequest(
        @NotNull UUID bankId,
        @NotBlank @Size(max = 20) String accountNumber,
        @NotBlank @Size(max = 10) String agency,
        @NotNull AccountType accountType,
        @DecimalMin(value = "0.00") BigDecimal monthlyMaintenanceFee) {
}
