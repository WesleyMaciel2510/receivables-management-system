package com.rms.dto;

import com.rms.model.Receivable;
import com.rms.model.ReceivableStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ReceivableResponse(UUID id, UUID clientId, String description, BigDecimal originalAmount,
                                 BigDecimal remainingBalance, LocalDate dueDate, ReceivableStatus status,
                                 Instant createdAt, Instant deletedAt) {
    public static ReceivableResponse from(Receivable receivable) {
        return new ReceivableResponse(receivable.getId(), receivable.getClient().getId(), receivable.getDescription(),
                receivable.getOriginalAmount(), receivable.getRemainingBalance(), receivable.getDueDate(),
                receivable.getStatus(), receivable.getCreatedAt(), receivable.getDeletedAt());
    }
}
