package com.rms.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.rms.config.AwsProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.security.MessageDigest;
import java.util.HexFormat;
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
                .queueUrl(properties.analyticsQueueUrl()).maxNumberOfMessages(Math.min(10, Math.max(1, properties.analyticsBatchSize())))
                .waitTimeSeconds(1).visibilityTimeout(properties.visibilityTimeoutSeconds()).build()).messages();
        if (messages.isEmpty()) return;
        try {
            Path file = writeBatch(messages);
            String key = "year=" + PATH_DATE.format(Instant.now()).substring(0, 4) + "/month="
                    + PATH_DATE.format(Instant.now()).substring(5, 7) + "/day="
                    + PATH_DATE.format(Instant.now()).substring(8, 10) + "/payments_"
                    + System.currentTimeMillis() + ".parquet";
            s3Client.putObject(PutObjectRequest.builder().bucket(properties.analyticsBucket()).key(key)
                    .contentType("application/octet-stream").build(), RequestBody.fromFile(file));
            for (Message message : messages) {
                sqsClient.deleteMessage(DeleteMessageRequest.builder().queueUrl(properties.analyticsQueueUrl())
                        .receiptHandle(message.receiptHandle()).build());
            }
            Files.deleteIfExists(file);
        } catch (Exception ignored) {
            // Keep the batch retryable when Parquet generation or object storage fails.
        }
    }

    private Path writeBatch(List<Message> messages) throws IOException {
        Path file = Files.createTempFile("payments-", ".parquet");
        try (ParquetWriter<GenericRecord> writer = AvroParquetWriter.<GenericRecord>builder(new LocalOutputFile(file))
                .withSchema(SCHEMA).withCompressionCodec(CompressionCodecName.SNAPPY)
                .withConf(new Configuration(false)).build()) {
            for (Message message : messages) {
                PaymentPublisher.PaymentEvent event = readEvent(message.body());
                GenericRecord record = new GenericData.Record(SCHEMA);
                record.put("event_id", event.eventId().toString());
                record.put("debtor_account_id", event.debtorAccountId().toString());
                record.put("creditor_account_id", event.creditorAccountId().toString());
                record.put("receivable_id", event.receivableId().toString());
                record.put("amount", event.amount().doubleValue());
                record.put("transaction_timestamp", event.transactionTimestamp() == null ? Instant.now().toEpochMilli() : event.transactionTimestamp().toEpochMilli());
                writer.write(record);
            }
        }
        return file;
    }

    private String batchIdentity(List<Message> messages) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        messages.stream().map(message -> message.messageId() == null ? message.body() : message.messageId())
                .sorted().forEach(value -> digest.update(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        return HexFormat.of().formatHex(digest.digest(), 0, 12);
    }

    private PaymentPublisher.PaymentEvent readEvent(String body) throws IOException {
        JsonNode node = objectMapper.readTree(body);
        if (node.hasNonNull("Message")) node = objectMapper.readTree(node.get("Message").asText());
        return objectMapper.treeToValue(node, PaymentPublisher.PaymentEvent.class);
    }

    private record LocalOutputFile(Path path) implements OutputFile {
        @Override public PositionOutputStream create(long blockSizeHint) throws IOException { return new FilePositionOutputStream(path); }
        @Override public PositionOutputStream createOrOverwrite(long blockSizeHint) throws IOException { return create(blockSizeHint); }
        @Override public boolean supportsBlockSize() { return false; }
        @Override public long defaultBlockSize() { return 0; }
    }

    private static final class FilePositionOutputStream extends PositionOutputStream {
        private final java.io.OutputStream delegate;
        private long position;

        private FilePositionOutputStream(Path path) throws IOException {
            delegate = Files.newOutputStream(path);
        }
        @Override public long getPos() { return position; }
        @Override public void write(int value) throws IOException { delegate.write(value); position++; }
        @Override public void write(byte[] bytes, int offset, int length) throws IOException { delegate.write(bytes, offset, length); position += length; }
        @Override public void flush() throws IOException { delegate.flush(); }
        @Override public void close() throws IOException { delegate.close(); }
    }
}



