package com.rms.service;

import com.rms.security.FieldEncryptionService;
import org.springframework.stereotype.Service;

@Service
public class BlindIndexService {
    private final FieldEncryptionService encryptionService;

    public BlindIndexService(FieldEncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    public String document(String value) {
        return encryptionService.blindIndex(value.replaceAll("\\D", ""));
    }

    public String email(String value) {
        return encryptionService.blindIndex(value.trim().toLowerCase());
    }
}
