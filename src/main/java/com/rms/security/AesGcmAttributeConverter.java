package com.rms.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.concurrent.atomic.AtomicReference;

@Converter
public class AesGcmAttributeConverter implements AttributeConverter<String, String> {
    private static final AtomicReference<FieldEncryptionService> SERVICE_REF = new AtomicReference<>();

    public static void configure(FieldEncryptionService encryptionService) {
        SERVICE_REF.set(encryptionService);
    }

    private FieldEncryptionService service() {
        FieldEncryptionService current = SERVICE_REF.get();
        if (current == null) throw new EncryptionException("Field encryption is not initialized");
        return current;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return service().encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return service().decrypt(dbData);
    }
}
