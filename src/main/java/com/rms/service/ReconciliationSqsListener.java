package com.rms.service;

import com.rms.config.AwsProperties;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import tools.jackson.databind.ObjectMapper;

@Component
public class ReconciliationSqsListener {
    private final SqsClient sqsClient;
    private final AwsProperties properties;
    private final ObjectMapper objectMapper;
    private final PaymentReconciliationService reconciliationService;

    public ReconciliationSqsListener(SqsClient sqsClient, AwsProperties properties, ObjectMapper objectMapper,
            PaymentReconciliationService reconciliationService) {
        this.sqsClient = sqsClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.reconciliationService = reconciliationService;
    }

    @Scheduled(fixedDelay = 10000, initialDelay = 30000)
    public void poll() {
        if (properties.reconciliationQueueUrl() == null || properties.reconciliationQueueUrl().isBlank()) return;
        List<Message> messages = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                .queueUrl(properties.reconciliationQueueUrl()).maxNumberOfMessages(10)
                .waitTimeSeconds(1).visibilityTimeout(properties.visibilityTimeoutSeconds()).build()).messages();
        for (Message message : messages) {
            try {
                PaymentPublisher.PaymentEvent event = objectMapper.readValue(message.body(), PaymentPublisher.PaymentEvent.class);
                reconciliationService.reconcile(event);
                sqsClient.deleteMessage(DeleteMessageRequest.builder().queueUrl(properties.reconciliationQueueUrl())
                        .receiptHandle(message.receiptHandle()).build());
            } catch (Exception ignored) {
                // Leave failed messages visible for retry and eventual redrive.
            }
        }
    }
}
