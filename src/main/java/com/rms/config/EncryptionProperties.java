package com.rms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.encryption")
public record EncryptionProperties(boolean enabled, String secret, int currentKeyVersion,
        String legacySecrets, String kmsProvider, String kmsKeyAlias, String kmsEndpoint) {
}
