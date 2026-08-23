## Purpose

Protect personally identifiable information and sensitive free-text receivable metadata at rest while allowing authorized application operations to use plaintext values in memory.

## ADDED Requirements

### Requirement: Protected fields are encrypted at rest
The system SHALL persist `Client` CPF/CNPJ, email, phone, and `Receivable` description as authenticated ciphertexts rather than plaintext values.

#### Scenario: New protected data is persisted
- **WHEN** an authorized request creates or updates a client or receivable with a protected value
- **THEN** the corresponding PostgreSQL column contains ciphertext that does not reveal the submitted plaintext

#### Scenario: Protected data is read by the application
- **WHEN** an authorized application operation loads a persisted client or receivable
- **THEN** the domain/API representation contains the original plaintext value after successful authentication and decryption

### Requirement: Encryption uses randomized authenticated protection
The system SHALL use authenticated encryption with a fresh unpredictable initialization vector for every protected value written, so equal plaintext values do not produce equal ciphertexts.

#### Scenario: Duplicate plaintext values are encrypted independently
- **WHEN** two records contain the same CPF/CNPJ, email, phone, or description
- **THEN** their stored ciphertexts differ while both decrypt successfully

#### Scenario: Ciphertext integrity is violated
- **WHEN** a ciphertext, IV, or authentication tag is altered before decryption
- **THEN** the read operation fails with a controlled encryption error and does not return attacker-controlled plaintext

### Requirement: Missing encryption key fails closed
The system SHALL require `APP_ENCRYPTION_SECRET` (or an explicitly configured equivalent external secret) at startup and SHALL refuse to start when it is missing, unusable, or too weak for the selected encryption scheme.

#### Scenario: Encryption secret is missing
- **WHEN** the application starts without the required encryption secret
- **THEN** startup fails with an actionable configuration error and no protected data operation is served

#### Scenario: Encryption secret is valid
- **WHEN** the application starts with valid externally supplied key material
- **THEN** encryption and decryption services become available without logging the secret

### Requirement: Encrypted fields have explicit search semantics
The system SHALL NOT claim plaintext equality search over randomized ciphertext columns; exact-match client lookups SHALL use a separately protected blind index or be rejected as unsupported until such an index is available.

#### Scenario: Duplicate validation is requested
- **WHEN** the API validates CPF/CNPJ or email uniqueness
- **THEN** it uses a protected searchable representation that does not expose the plaintext, or returns a documented unsupported-operation response rather than scanning ciphertext for plaintext equality

#### Scenario: Protected fields are listed or filtered
- **WHEN** a caller requests a filter or sort directly on an encrypted field without an approved searchable representation
- **THEN** the API rejects the operation without leaking stored ciphertext or plaintext
