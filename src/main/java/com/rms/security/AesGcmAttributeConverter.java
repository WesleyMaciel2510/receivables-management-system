package com.rms.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class AesGcmAttributeConverter implements AttributeConverter<String, String> {
    private static volatile FieldEncryptionService service;

    public static void configure(FieldEncryptionService encryptionService) {
        service = encryptionService;
    }

    private FieldEncryptionService service() {
        FieldEncryptionService current = service;
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
