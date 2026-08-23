package com.rms.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rms.config.EncryptionProperties;
import org.junit.jupiter.api.Test;

class FieldEncryptionServiceTests {
    private static FieldEncryptionService service(String secret) {
        FieldEncryptionService service = new FieldEncryptionService(new EncryptionProperties(
                true, secret, 1, "", "localstack", "alias/rms-fle", "http://localhost:4566"));
        service.initialize();
        return service;
    }

    @Test
    void roundTripsAndRandomizesEqualValues() {
        FieldEncryptionService service = service("test-secret-that-is-long-enough");
        String first = service.encrypt("12345678909");
        String second = service.encrypt("12345678909");
        assertThat(first).startsWith("rmsfle:v1:1:").isNotEqualTo(second);
        assertThat(service.decrypt(first)).isEqualTo("12345678909");
        assertThat(service.decrypt(second)).isEqualTo("12345678909");
    }

    @Test
    void rejectsMalformedAndTamperedValues() {
        FieldEncryptionService service = service("test-secret-that-is-long-enough");
        String encrypted = service.encrypt("sensitive-value");
        int payloadStart = encrypted.lastIndexOf(':') + 1;
        char flipped = encrypted.charAt(payloadStart) == 'A' ? 'B' : 'A';
        String tampered = encrypted.substring(0, payloadStart) + flipped + encrypted.substring(payloadStart + 1);

        assertThatThrownBy(() -> service.decrypt("rmsfle:v1:1:not-an-envelope"))
                .isInstanceOf(EncryptionException.class);
        assertThatThrownBy(() -> service.decrypt(tampered))
                .isInstanceOf(EncryptionException.class);
    }

    @Test
    void supportsLegacyReadsAndCurrentWrites() {
        FieldEncryptionService old = service("old-secret-that-is-long-enough");
        String oldValue = old.encrypt("rotated-value");
        FieldEncryptionService current = new FieldEncryptionService(new EncryptionProperties(
                true, "new-secret-that-is-long-enough", 2, "1=old-secret-that-is-long-enough",
                "localstack", "alias/rms-fle", "http://localhost:4566"));
        current.initialize();
        assertThat(current.decrypt(oldValue)).isEqualTo("rotated-value");
        assertThat(current.encrypt("rotated-value")).startsWith("rmsfle:v1:2:");
    }

    @Test
    void requiresStrongSecret() {
        FieldEncryptionService service = new FieldEncryptionService(new EncryptionProperties(
                true, "short", 1, "", "localstack", "alias/rms-fle", "http://localhost:4566"));
        assertThatThrownBy(service::initialize).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 16 characters");
    }
}