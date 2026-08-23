package com.rms.dto;

import com.rms.model.ReceivableStatus;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

public record StatusChangeRequest(
        @Schema(example = "PAID", allowableValues = {"PENDING", "PARTIAL", "PAID", "OVERDUE", "CANCELLED"})
        @NotNull ReceivableStatus status) {
}
