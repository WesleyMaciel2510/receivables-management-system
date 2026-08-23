package com.rms.service;

import com.rms.config.AwsProperties;
import com.rms.dto.PaymentAcceptedResponse;
import com.rms.dto.PaymentRequest;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import tools.jackson.databind.ObjectMapper;

@Service
public class PaymentPublisher {
    private final SnsClient snsClient;
    private final AwsProperties properties;
    private final ObjectMapper objectMapper;

    public PaymentPublisher(SnsClient snsClient, AwsProperties properties, ObjectMapper objectMapper) {
        this.snsClient = snsClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public PaymentAcceptedResponse publish(PaymentRequest request) {
        UUID eventId = request.idempotencyKey() == null ? UUID.randomUUID() : request.idempotencyKey();
        if (properties.paymentTopicArn() == null || properties.paymentTopicArn().isBlank()) {
            throw new PaymentPublicationException("Payment event topic is not configured", null);
        }
        PaymentEvent event = new PaymentEvent(eventId, 1, request.debtorAccountId().toString(), Instant.now(),
                request.debtorAccountId(), request.creditorAccountId(), request.receivableId(), request.amount());
        try {
            snsClient.publish(PublishRequest.builder().topicArn(properties.paymentTopicArn())
                    .message(objectMapper.writeValueAsString(event)).messageGroupId(request.debtorAccountId().toString())
                    .messageDeduplicationId(eventId.toString())
                    .messageAttributes(java.util.Map.of("eventType", MessageAttributeValue.builder()
                            .dataType("String").stringValue("PaymentReceived").build()))
                    .build());
            return new PaymentAcceptedResponse(eventId, "ACCEPTED");
        } catch (Exception exception) {
            throw new PaymentPublicationException("Payment event could not be published", exception);
        }
    }

    public record PaymentEvent(UUID eventId, int schemaVersion, String messageGroupKey, Instant transactionTimestamp,
                               UUID debtorAccountId, UUID creditorAccountId, UUID receivableId,
                               java.math.BigDecimal amount) {
    }
}





