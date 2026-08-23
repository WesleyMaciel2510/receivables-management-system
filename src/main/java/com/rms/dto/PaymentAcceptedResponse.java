package com.rms.dto;

import java.util.UUID;

public record PaymentAcceptedResponse(UUID eventId, String status) {
}
