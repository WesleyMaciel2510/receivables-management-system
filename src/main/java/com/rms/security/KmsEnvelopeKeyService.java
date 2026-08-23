package com.rms.security;

import com.rms.config.AwsProperties;
import com.rms.config.EncryptionProperties;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DataKeySpec;
import software.amazon.awssdk.services.kms.model.DecryptRequest;
import software.amazon.awssdk.services.kms.model.GenerateDataKeyRequest;

@Service
public class KmsEnvelopeKeyService {
    private final KmsClient kmsClient;
    private final EncryptionProperties encryptionProperties;

    public KmsEnvelopeKeyService(KmsClient kmsClient, EncryptionProperties encryptionProperties,
            AwsProperties awsProperties) {
        this.kmsClient = kmsClient;
        this.encryptionProperties = encryptionProperties;
    }

    public EncryptedDataKey generateDataKey() {
        String keyId = encryptionProperties.kmsKeyAlias();
        if (keyId == null || keyId.isBlank()) throw new EncryptionException("KMS key alias is not configured");
        var response = kmsClient.generateDataKey(GenerateDataKeyRequest.builder()
                .keyId(keyId).keySpec(DataKeySpec.AES_256).build());
        return new EncryptedDataKey(response.keyId(), response.ciphertextBlob().asByteArray(),
                response.plaintext().asByteArray());
    }

    public SecretKey decryptDataKey(EncryptedDataKey encryptedDataKey) {
        var response = kmsClient.decrypt(DecryptRequest.builder()
                .ciphertextBlob(SdkBytes.fromByteArray(encryptedDataKey.encryptedKey()))
                .keyId(encryptedDataKey.keyId()).build());
        return new SecretKeySpec(response.plaintext().asByteArray(), "AES");
    }

    public record EncryptedDataKey(String keyId, byte[] encryptedKey, byte[] transientPlaintextKey) {
        public EncryptedDataKey withoutPlaintext() {
            return new EncryptedDataKey(keyId, encryptedKey, null);
        }
    }
}
