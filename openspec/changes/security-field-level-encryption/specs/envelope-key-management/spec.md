## Purpose

Provide a controlled envelope-encryption boundary for development and future key rotation, using LocalStack KMS without making the local emulator a production trust anchor.

## ADDED Requirements

### Requirement: Development KMS key is provisioned reproducibly
The local development environment SHALL provision a KMS customer-managed key and a stable alias for envelope-encryption tests when LocalStack starts.

#### Scenario: LocalStack starts for the first time
- **WHEN** the Compose development environment initializes
- **THEN** the KMS key and alias are created or confirmed idempotently and are available to the application test workflow

#### Scenario: LocalStack is restarted
- **WHEN** LocalStack restarts and initialization runs again
- **THEN** provisioning completes without creating uncontrolled duplicate aliases or failing because the key already exists

### Requirement: Data keys are separated from ciphertext storage
The envelope-encryption boundary SHALL keep the key-encrypting key reference and encrypted data-key material separate from protected business ciphertexts, and SHALL never persist plaintext data keys.

#### Scenario: A data key is used for field encryption
- **WHEN** an encryption operation obtains a data key through the configured KMS boundary
- **THEN** only encrypted key material and non-sensitive key identifiers are retained for later decryption

#### Scenario: KMS is unavailable
- **WHEN** a required envelope-key operation cannot reach the configured KMS endpoint
- **THEN** the operation fails closed without writing plaintext protected data or logging key material
