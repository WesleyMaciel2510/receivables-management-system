package com.rms.config;

import com.rms.security.AesGcmAttributeConverter;
import com.rms.security.FieldEncryptionService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(EncryptionProperties.class)
public class EncryptionConfig {
    public EncryptionConfig(FieldEncryptionService service) {
        AesGcmAttributeConverter.configure(service);
    }
}
