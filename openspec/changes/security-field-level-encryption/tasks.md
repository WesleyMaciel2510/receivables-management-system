## 1. Dependencies and configuration

- [x] 1.1 Add the selected JCE/AWS KMS dependencies and verify dependency resolution and compilation with `mvn -B test`
- [x] 1.2 Add environment-backed encryption configuration for `APP_ENCRYPTION_SECRET`, active key version, KMS provider, and fail-closed startup validation; verify no secret is committed or logged
- [x] 1.3 Add local/development/production profile settings with LocalStack-only KMS endpoints for local profiles and AWS KMS configuration for production; verify `docker compose config --quiet` succeeds without committed credentials
- [x] 1.4 Document encryption secret provisioning, AWS KMS permissions, LocalStack startup, rotation, and recovery procedures; verify the documented commands are reproducible

## 2. Field-level encryption

- [x] 2.1 Implement a versioned AES-256-GCM value envelope containing key version, random IV, ciphertext, and authentication tag; verify round-trip, randomized duplicate plaintexts, malformed envelopes, and tamper detection with unit tests
- [x] 2.2 Implement the JPA `AttributeConverter<String, String>` encryption boundary and fail-safe exception mapping; verify repository writes never persist protected plaintext
- [x] 2.3 Apply the converter to `Client.cpfCnpj`, `Client.email`, `Client.phone`, and `Receivable.description`; verify authorized domain reads return original plaintext values
- [x] 2.4 Add key derivation and secret validation with no secret/plaintext logging or serialization; verify startup fails when `APP_ENCRYPTION_SECRET` is missing or invalid
- [x] 2.5 Add controlled legacy-key resolution for reads during rotation and current-key writes; verify old ciphertexts remain readable until migration completes

## 3. Search and schema migration

- [x] 3.1 Add a new Liquibase changeset that expands protected columns to safely hold envelopes and includes rollback; verify existing executed changesets are unchanged
- [x] 3.2 Add normalized keyed blind-index columns and unique indexes for CPF/CNPJ and email where current uniqueness checks require exact lookup; verify indexes do not store plaintext
- [x] 3.3 Update repositories and validation services to query blind indexes instead of randomized ciphertext equality; verify duplicate CPF/CNPJ and email rules continue to work
- [x] 3.4 Implement an idempotent, resumable batch backfill that encrypts all existing plaintext protected values and populates blind indexes; verify restart resumes safely without double-corrupting data
- [x] 3.5 Add migration verification and cutover checks that confirm every protected row decrypts before plaintext legacy storage is retired; verify failure stops cutover without data loss

## 4. Envelope encryption and KMS

- [x] 4.1 Implement a KMS abstraction that uses AWS KMS in production and LocalStack KMS in local development; verify provider selection is profile/configuration driven
- [x] 4.2 Add idempotent LocalStack initialization for a CMK and stable alias; verify first startup and repeated startup both succeed without duplicate aliases
- [x] 4.3 Implement encrypted data-key storage and version-aware decrypt/rotation flows without persisting plaintext data keys; verify KMS failures fail closed
- [x] 4.4 Define the AWS IAM permissions and KMS key policy required by the application; verify the production configuration does not grant unrelated KMS operations
- [x] 4.5 Add key rotation tooling/workflow that rewraps data keys or re-encrypts fields in batches while preserving reads during the transition; verify old and new key versions decrypt correctly

## 5. Security and operational verification

- [x] 5.1 Add PostgreSQL integration tests that inspect raw columns and verify protected fields contain ciphertext, not submitted plaintext or secrets
- [x] 5.2 Add repository/API tests for encrypted reads, writes, blind-index uniqueness, unsupported encrypted filtering, and authorization boundaries; verify existing receivable workflows remain functional
- [x] 5.3 Add tests for missing secrets, tampered ciphertext, wrong key version, unavailable KMS, failed backfill, and safe error responses; verify no sensitive values appear in logs or actuator output
- [x] 5.4 Verify a clean database applies all new Liquibase changesets and an existing database preserves prior data while the backfill completes; verify Liquibase restart is up-to-date
- [x] 5.5 Run the complete PostgreSQL/Compose test suite and verify the documented production AWS KMS configuration and local LocalStack workflow both pass their smoke checks
