package com.rms.security;

import com.rms.config.EncryptionProperties;
import jakarta.annotation.PostConstruct;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class FieldEncryptionService {
    private static final String PREFIX = "rmsfle:v1";
    private static final byte[] KDF_SALT = "rms-field-encryption-v1".getBytes(StandardCharsets.UTF_8);
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final EncryptionProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<Integer, SecretKey> keys = new HashMap<>();
    private SecretKey blindIndexKey;

    public FieldEncryptionService(EncryptionProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void initialize() {
        if (!properties.enabled()) return;
        if (properties.secret() == null || properties.secret().isBlank()) {
            throw new IllegalStateException("APP_ENCRYPTION_SECRET must be configured");
        }
        if (properties.secret().length() < 16) {
            throw new IllegalStateException("APP_ENCRYPTION_SECRET must contain at least 16 characters");
        }
        if (properties.currentKeyVersion() < 1) {
            throw new IllegalStateException("APP_ENCRYPTION_CURRENT_KEY_VERSION must be positive");
        }
        keys.put(properties.currentKeyVersion(), deriveKey(properties.secret(), properties.currentKeyVersion()));
        if (properties.legacySecrets() != null && !properties.legacySecrets().isBlank()) {
            for (String entry : properties.legacySecrets().split(",")) {
                String[] pair = entry.split("=", 2);
                if (pair.length == 2 && !pair[1].isBlank()) {
                    keys.put(Integer.parseInt(pair[0].trim()), deriveKey(pair[1].trim(), Integer.parseInt(pair[0].trim())));
                }
            }
        }
        blindIndexKey = deriveKey(properties.secret(), 0);
    }

    public String encrypt(String plaintext) {
        if (plaintext == null || !properties.enabled()) return plaintext;
        try {
            byte[] iv = new byte[IV_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keys.get(properties.currentKeyVersion()), new GCMParameterSpec(TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return PREFIX + ":" + properties.currentKeyVersion() + ":"
                    + Base64.getUrlEncoder().withoutPadding().encodeToString(iv) + ":"
                    + Base64.getUrlEncoder().withoutPadding().encodeToString(ciphertext);
        } catch (GeneralSecurityException exception) {
            throw new EncryptionException("Unable to encrypt protected value", exception);
        }
    }

    public String decrypt(String value) {
        if (value == null || !properties.enabled() || !value.startsWith(PREFIX + ":")) return value;
        try {
            String[] parts = value.split(":", 5);
            if (parts.length != 5) throw new EncryptionException("Invalid protected value envelope");
            int version = Integer.parseInt(parts[2]);
            SecretKey key = keys.get(version);
            if (key == null) throw new EncryptionException("No encryption key available for version " + version);
            byte[] iv = Base64.getUrlDecoder().decode(parts[3]);
            byte[] ciphertext = Base64.getUrlDecoder().decode(parts[4]);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (EncryptionException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new EncryptionException("Unable to decrypt protected value", exception);
        }
    }

    public String blindIndex(String normalizedValue) {
        if (normalizedValue == null) return null;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(blindIndexKey);
            return toHex(mac.doFinal(normalizedValue.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new EncryptionException("Unable to calculate protected lookup index", exception);
        }
    }

    public boolean isCurrent(String value) {
        return value != null && value.startsWith(PREFIX + ":" + properties.currentKeyVersion() + ":");
    }

    private SecretKey deriveKey(String secret, int version) {
        try {
            PBEKeySpec spec = new PBEKeySpec(secret.toCharArray(),
                    ByteBuffer.allocate(KDF_SALT.length + 4).put(KDF_SALT).putInt(version).array(), 120_000, 256);
            byte[] encoded = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return new SecretKeySpec(encoded, "AES");
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to initialize encryption key", exception);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            result.append(Character.forDigit((value >>> 4) & 0x0f, 16));
            result.append(Character.forDigit(value & 0x0f, 16));
        }
        return result.toString();
    }
}
