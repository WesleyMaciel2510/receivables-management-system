## Context

The project is a Spring Boot 4/Jakarta application using JPA, PostgreSQL, and Liquibase. Existing executed changesets must remain immutable. Client PII currently uses constrained `VARCHAR` columns, and `Receivable.description` is free text. See `proposal.md` and the capability specs for the externally observable contract.

## Goals / Non-Goals

**Goals:**

- Add authenticated randomized field encryption with an explicit key boundary.
- Preserve transparent domain/API behavior for authorized reads and writes.
- Migrate existing data safely through new Liquibase changesets.
- Provide deterministic blind indexes only where exact-match validation requires them.
- Make LocalStack KMS provisioning repeatable for envelope-encryption tests.

**Non-Goals:**

- Encrypting every column or replacing PostgreSQL/TLS/backup encryption.
- Encrypting monetary values in this change; amounts remain queryable for receivable calculations.
- Treating LocalStack KMS or an application environment variable as a production KMS deployment.
- Adding searchable sorting, partial matching, or plaintext database-side queries over randomized ciphertext.

## Decisions

### AES-256-GCM converter boundary

Use a JPA `AttributeConverter<String, String>` for the selected string fields. Each value is encoded as a versioned envelope containing a random 96-bit IV, ciphertext, and GCM authentication tag. AES-GCM is preferred over unauthenticated AES modes because tampering must be detected during reads. A converter keeps the domain/API contract stable, while service-level encryption would be easier to bypass on direct repository writes.

An alternative Jasypt starter was considered but rejected as the primary field mechanism: its property-encryption focus does not define the per-value randomized envelope and migration semantics required here. Native JCE avoids an additional runtime abstraction.

### Key derivation and secret boundary

Derive a 256-bit AES key from `APP_ENCRYPTION_SECRET` using a memory-hard or cryptographically approved derivation boundary selected during implementation, with a versioned key identifier in the envelope. The raw environment value and derived key remain process-local and are never logged or persisted. Startup validation fails if the secret is missing or invalid.

### Searchable protected identifiers

Randomized ciphertext cannot support equality queries. Add blind-index columns for CPF/CNPJ and email only if current uniqueness validation requires database lookup. The blind index uses a keyed HMAC and normalized input; it is not returned by APIs. Phone and description do not gain query filters in this change.

### Database migration strategy

Create new Liquibase changesets that widen protected columns to `VARCHAR(512)` or `TEXT`, add blind-index columns and unique indexes where required, and provide rollback blocks. Existing changesets are not edited. The migration must define a controlled backfill path: old plaintext rows are read once by the application migration job, encrypted, indexed where applicable, and verified before plaintext columns are retired or repurposed.

### Envelope encryption and LocalStack

Add the AWS SDK KMS client and an idempotent LocalStack initialization script that creates a CMK alias. The application-facing key provider exposes encrypted data keys and key identifiers, while business ciphertext storage remains independent of LocalStack. Production configuration must point to a managed KMS or external provider; LocalStack defaults are development-only.

## Risks / Trade-offs

- [Ciphertext expansion] → Widen columns and test the maximum envelope size before migration.
- [Blind-index leakage] → Normalize and HMAC values with a separate key; document that equality frequency remains observable.
- [Key loss] → Require secret backup/rotation procedures and verify decryptability before retiring an old key version.
- [Existing plaintext rows] → Use an idempotent backfill with verification and an explicit rollback/backup plan before enforcing encrypted-only writes.
- [KMS availability] → Fail closed for operations requiring an unavailable key provider and keep LocalStack isolated to local development.

## Migration Plan

1. Add configuration and encryption components behind a feature flag or migration mode.
2. Apply additive Liquibase schema changes for expanded ciphertext and blind-index storage.
3. Backfill existing rows in batches, verify decryption and indexes, then enable encrypted writes.
4. Remove or quarantine plaintext legacy columns only after backup verification and a successful read/write soak test.
5. Roll back by disabling the feature and reverting only the new changesets where safe; do not alter prior executed changesets.

## Open Questions

- The exact key-rotation interval and retirement window remain operational policy decisions; implementation SHALL support versioned keys and retain old key material until all ciphertexts are migrated.
