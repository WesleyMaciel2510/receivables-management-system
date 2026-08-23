package com.rms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.KmsClientBuilder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.SnsClientBuilder;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;

import java.net.URI;

@Configuration
public class AwsClientConfig {
    @Bean
    SnsClient snsClient(AwsProperties properties) {
        return SnsClient.builder().region(region(properties)).credentialsProvider(credentials(properties))
                .applyMutation(builder -> endpoint(builder, properties)).build();
    }

    @Bean
    SqsClient sqsClient(AwsProperties properties) {
        return SqsClient.builder().region(region(properties)).credentialsProvider(credentials(properties))
                .applyMutation(builder -> endpoint(builder, properties)).build();
    }

    @Bean
    S3Client s3Client(AwsProperties properties) {
        return S3Client.builder().region(region(properties)).credentialsProvider(credentials(properties))
                .forcePathStyle(true).applyMutation(builder -> endpoint(builder, properties)).build();
    }

    @Bean
    KmsClient kmsClient(AwsProperties properties, EncryptionProperties encryptionProperties) {
        KmsClientBuilder builder = KmsClient.builder()
                .region(region(properties)).credentialsProvider(credentials(properties));
        if ("localstack".equalsIgnoreCase(encryptionProperties.kmsProvider())
                && encryptionProperties.kmsEndpoint() != null
                && !encryptionProperties.kmsEndpoint().isBlank()) {
            builder.endpointOverride(URI.create(encryptionProperties.kmsEndpoint()));
        }
        return builder.build();
    }

    private static Region region(AwsProperties properties) {
        return Region.of(properties.region() == null || properties.region().isBlank() ? "us-east-1" : properties.region());
    }

    private static AwsCredentialsProvider credentials(AwsProperties properties) {
        if (properties.endpoint() == null || properties.endpoint().isBlank()) {
            return DefaultCredentialsProvider.builder().build();
        }
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(
                blankDefault(properties.accessKey(), "test"), blankDefault(properties.secretKey(), "test")));
    }

    private static String blankDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static void endpoint(SnsClientBuilder builder, AwsProperties properties) {
        if (properties.endpoint() != null && !properties.endpoint().isBlank()) {
            builder.endpointOverride(URI.create(properties.endpoint()));
        }
    }

    private static void endpoint(SqsClientBuilder builder, AwsProperties properties) {
        if (properties.endpoint() != null && !properties.endpoint().isBlank()) {
            builder.endpointOverride(URI.create(properties.endpoint()));
        }
    }

    private static void endpoint(S3ClientBuilder builder, AwsProperties properties) {
        if (properties.endpoint() != null && !properties.endpoint().isBlank()) {
            builder.endpointOverride(URI.create(properties.endpoint()));
        }
    }
}
