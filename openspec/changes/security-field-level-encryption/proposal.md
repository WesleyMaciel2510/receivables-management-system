## Why

The receivables domain currently persists personally identifiable information
and sensitive receivable metadata in plaintext, increasing exposure under LGPD,
PCI-DSS, database backups, and operational support access. This change adds
authenticated field-level encryption with externally supplied key material while
preserving transparent access for authorized application flows.

## What Changes

- Add AES-256-GCM field encryption for selected client PII and sensitive receivable metadata.
- Store randomized ciphertexts, IVs, and authentication tags in expanded database columns.
- Add an application-managed encryption key boundary supplied only through `APP_ENCRYPTION_SECRET`.
- Add optional envelope-encryption integration with the LocalStack AWS KMS emulator for development and key rotation workflows.
- Protect encrypted-field tests and operational diagnostics from exposing plaintext values.
- Add a safe migration strategy using new Liquibase changesets; existing executed changesets will not be modified.

## Capabilities

### New Capabilities

- `field-level-encryption`: Encrypt and transparently decrypt protected domain fields with authenticated, randomized ciphertexts and fail startup when required key material is absent.
- `encrypted-configuration`: Resolve sensitive configuration values through externally supplied encryption secrets without committing master keys or plaintext production credentials.
- `envelope-key-management`: Manage development envelope-encryption keys through LocalStack KMS and support controlled data-key rotation boundaries.

### Modified Capabilities

- None. No main capability specs currently exist in this repository.

## Impact

- `Client` and `Receivable` JPA entities, converters, repositories, and integration tests.
- Liquibase changelog files and PostgreSQL column sizes/types.
- Spring configuration, Docker Compose, LocalStack initialization, and AWS SDK/KMS dependencies.
- Database search semantics for encrypted fields; exact-match lookups require a blind index or another explicitly designed searchable-encryption boundary.
