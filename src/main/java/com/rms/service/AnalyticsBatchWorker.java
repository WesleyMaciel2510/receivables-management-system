package com.rms.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.rms.config.AwsProperties;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.apache.parquet.io.OutputFile;
import org.apache.parquet.io.PositionOutputStream;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

@Component
public class AnalyticsBatchWorker {
    private static final DateTimeFormatter PATH_DATE = DateTimeFormatter.ofPattern("yyyy/MM/dd").withZone(ZoneOffset.UTC);
    private static final Schema SCHEMA = new Schema.Parser().parse("""
            {"type":"record","name":"PaymentTransaction","fields":[
              {"name":"event_id","type":"string"},
              {"name":"debtor_account_id","type":"string"},
              {"name":"creditor_account_id","type":"string"},
              {"name":"receivable_id","type":"string"},
              {"name":"amount","type":"double"},
              {"name":"transaction_timestamp","type":"long"}
            ]}
            """);

    private final SqsClient sqsClient;
    private final S3Client s3Client;
    private final AwsProperties properties;
    private final ObjectMapper objectMapper;

    public AnalyticsBatchWorker(SqsClient sqsClient, S3Client s3Client, AwsProperties properties,
            ObjectMapper objectMapper) {
        this.sqsClient = sqsClient;
        this.s3Client = s3Client;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 10000, initialDelay = 30000)
    public void poll() {
        if (properties.analyticsQueueUrl() == null || properties.analyticsQueueUrl().isBlank()) return;
        List<Message> messages = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                .queueUrl(properties.analyticsQueueUrl()).maxNumberOfMessages(Math.clamp(properties.analyticsBatchSize(), 1, 10))
                .waitTimeSeconds(1).visibilityTimeout(properties.visibilityTimeoutSeconds()).build()).messages();
        if (messages.isEmpty()) return;
        try {
            byte[] data = writeBatch(messages);
            String key = "year=" + PATH_DATE.format(Instant.now()).substring(0, 4) + "/month="
                    + PATH_DATE.format(Instant.now()).substring(5, 7) + "/day="
                    + PATH_DATE.format(Instant.now()).substring(8, 10) + "/payments_"
                    + System.currentTimeMillis() + ".parquet";
            s3Client.putObject(PutObjectRequest.builder().bucket(properties.analyticsBucket()).key(key)
                    .contentType("application/octet-stream").build(), RequestBody.fromBytes(data));
            for (Message message : messages) {
                sqsClient.deleteMessage(DeleteMessageRequest.builder().queueUrl(properties.analyticsQueueUrl())
                        .receiptHandle(message.receiptHandle()).build());
            }
        } catch (Exception _) {
            // Keep the batch retryable when Parquet generation or object storage fails.
        }
    }

    private byte[] writeBatch(List<Message> messages) throws IOException {
        // Batch size is capped at the max SQS batch (10), so encoding in memory
        // avoids writing files to the (possibly publicly writable) temp directory.
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ParquetWriter<GenericRecord> writer = AvroParquetWriter.<GenericRecord>builder(new MemoryOutputFile(buffer))
                .withSchema(SCHEMA).withCompressionCodec(CompressionCodecName.SNAPPY)
                .withConf(new Configuration(false)).build()) {
            for (Message message : messages) {
                PaymentPublisher.PaymentEvent event = readEvent(message.body());
                GenericRecord avroRecord = new GenericData.Record(SCHEMA);
                avroRecord.put("event_id", event.eventId().toString());
                avroRecord.put("debtor_account_id", event.debtorAccountId().toString());
                avroRecord.put("creditor_account_id", event.creditorAccountId().toString());
                avroRecord.put("receivable_id", event.receivableId().toString());
                avroRecord.put("amount", event.amount().doubleValue());
                avroRecord.put("transaction_timestamp", event.transactionTimestamp() == null ? Instant.now().toEpochMilli() : event.transactionTimestamp().toEpochMilli());
                writer.write(avroRecord);
            }
        }
        return buffer.toByteArray();
    }

    private PaymentPublisher.PaymentEvent readEvent(String body) {
        JsonNode node = objectMapper.readTree(body);
        if (node.hasNonNull("Message")) node = objectMapper.readTree(node.get("Message").asString());
        return objectMapper.treeToValue(node, PaymentPublisher.PaymentEvent.class);
    }

    private record MemoryOutputFile(ByteArrayOutputStream out) implements OutputFile {
        @Override public PositionOutputStream create(long blockSizeHint) { return new MemoryPositionOutputStream(out); }
        @Override public PositionOutputStream createOrOverwrite(long blockSizeHint) { return create(blockSizeHint); }
        @Override public boolean supportsBlockSize() { return false; }
        @Override public long defaultBlockSize() { return 0; }
    }

    private static final class MemoryPositionOutputStream extends PositionOutputStream {
        private final ByteArrayOutputStream delegate;
        private long position;

        private MemoryPositionOutputStream(ByteArrayOutputStream delegate) { this.delegate = delegate; }
        @Override public long getPos() { return position; }
        @Override public void write(int value) { delegate.write(value); position++; }
        @Override public void write(byte[] bytes, int offset, int length) { delegate.write(bytes, offset, length); position += length; }
        @Override public void flush() { /* In-memory stream — nothing to flush. */ }
        @Override public void close() { /* Delegated ByteArrayOutputStream does not need explicit close. */ }
    }
}
