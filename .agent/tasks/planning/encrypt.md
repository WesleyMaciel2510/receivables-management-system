# OpenSpec Task Proposal: security-field-level-encryption

## Objective
Enhance data protection to meet banking compliance standards (such as LGPD and PCI-DSS) by implementing **Field-Level Encryption (FLE)** for PII (Personally Identifiable Information) and sensitive financial payload properties. Database records will store encrypted ciphertexts at rest while transparently decrypting in-memory for authorized API requests.

---

## Security Architecture & Libraries

1. **At-Rest Field Encryption**:
    - Library: `jasypt-spring-boot-starter` or native Java Cryptography Extension (`javax.crypto`).
    - Implementation: JPA `AttributeConverter` using AES-256-GCM authenticated encryption.
    - Protected Fields: `Cliente` (cpf_cnpj, email, phone) and `Recebível` metadata.

2. **Secrets & Properties Encryption**:
    - Secure external configuration properties (DB passwords, AWS secret keys) using Jasypt `ENC(...)` wrappers.
    - Master encryption key injected strictly via environment variable (`APP_ENCRYPTION_SECRET`), never hardcoded.

3. **Envelope Encryption & AWS Key Management (LocalStack)**:
    - AWS KMS integrated in LocalStack to generate and rotate Data Encryption Keys (DEKs).

---

## Tasks

### 1. Security Dependencies & Configurations
- [ ] Add `jasypt-spring-boot-starter` or Spring Security Crypto dependencies to `pom.xml` / `build.gradle`.
- [ ] Update `application.yml` to support encrypted properties via `jasypt.encryptor.password=${APP_ENCRYPTION_SECRET}`.
- [ ] Provision AWS KMS in LocalStack startup script (`init-aws.sh`) to create a Customer Master Key (CMK) for key envelope management.

### 2. JPA Attribute Converters for PII
- [ ] Create `AesGcmAttributeConverter` implementing `jakarta.persistence.AttributeConverter<String, String>`.
- [ ] Implement AES-256-GCM encryption with unique, randomized Initialization Vectors (IVs) per record to prevent dictionary attacks on repeated plaintexts (e.g., duplicate CPFs).
- [ ] Annotate sensitive entity fields with `@Convert(converter = AesGcmAttributeConverter.class)`:
    - `Cliente.cpfCnpj`
    - `Cliente.email`
    - `Cliente.phone`

### 3. Database Migration & Liquibase Adjustments
- [ ] Update Liquibase column type definitions for encrypted fields from standard lengths (`VARCHAR(14)`) to expanded capacities (`VARCHAR(512)`) or `TEXT` to safely store Base64-encoded ciphertexts with IVs and authentication tags.

### 4. Verification & Testing
- [ ] Write integration test verifying database contents directly via SQL: Assert fields in Postgres display raw ciphertexts (e.g., `eyJJViI6...`).
- [ ] Write repository test: Verify `findByCpfCnpj` or exact-match queries handle encrypted search (or use blind indexing / deterministic HMAC hashes for exact lookups).
- [ ] Verify no unencrypted sensitive data appears in Liquibase logs, Spring Boot startup outputs, or Actuator endpoints.

---

## Definition of Done (DoD)
- Direct PostgreSQL queries show PII columns stored strictly as encrypted ciphertexts.
- Application transparently maps encrypted columns to plaintext domain model attributes during JPA reads/writes.
- Zero plaintexts appear in logs or trace dumps.
- Application fails startup cleanly if the master secret `APP_ENCRYPTION_SECRET` is missing.