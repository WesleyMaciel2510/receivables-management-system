package com.rms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.aws")
public record AwsProperties(String endpoint, String region, String accessKey, String secretKey,
                            String paymentTopicArn, String reconciliationQueueUrl, String analyticsQueueUrl,
                            String analyticsBucket, int analyticsBatchSize, int visibilityTimeoutSeconds) {
}
