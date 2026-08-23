## 1. Schema and seed data

- [ ] 1.1 Add the Liquibase migration for `banks`, `bank_accounts`, account enums/constraints, indexes, and the unique `(client_id, bank_id, account_type)` rule; verify the migration applies cleanly to a fresh PostgreSQL database.
- [ ] 1.2 Add nullable debtor and creditor bank-account foreign keys to receivables and verify existing receivable rows remain readable after migration.
- [ ] 1.3 Add an idempotent seed migration for banks 001, 033, 104, 237, 341, and 260; verify COMPE uniqueness and expected names/codes.
- [ ] 1.4 Add a durable payment-effect/idempotency record schema with a unique event identifier; verify duplicate event identifiers are rejected at the database layer.

## 2. Banking domain and APIs

- [ ] 2.1 Implement bank and bank-account domain models, enums, repositories, and decimal monetary mappings; verify persistence and retrieval of all supported account types.
- [ ] 2.2 Implement account lifecycle and fee validation, including zero fees for payment accounts and configurable checking fees; verify invalid fee combinations through unit tests.
- [ ] 2.3 Implement bank-directory and client-account DTOs, services, and controllers for `GET /api/v1/banks`, `POST /api/v1/clients/{id}/accounts`, and `GET /api/v1/clients/{id}/accounts`; verify response shapes and client scoping with integration tests.
- [ ] 2.4 Translate service pre-check and database unique-constraint races into HTTP `409 Conflict`; verify concurrent duplicate account creation leaves exactly one account.

## 3. Messaging infrastructure and event contract

- [ ] 3.1 Add LocalStack provisioning for the FIFO SNS topic, reconciliation and analytics FIFO queues, subscriptions, redrive/visibility settings, and analytics S3 bucket; verify resources and subscriptions with AWS CLI smoke checks.
- [ ] 3.2 Add configuration properties for AWS endpoint, region, credentials, topic/queue names, bucket, batch limits, and visibility timeouts across local and deployed profiles; verify application startup with the LocalStack profile.
- [ ] 3.3 Define versioned payment event and idempotency contracts with stable event ID, message group key, transaction metadata, account IDs, and receivable ID; verify serialization/deserialization compatibility with contract tests.
- [ ] 3.4 Implement the SNS publisher behind an application service and wire `POST /api/v1/payments` to validate, publish, and return `202 Accepted`; verify invalid payloads publish nothing and publisher failure never returns `202`.

## 4. Reconciliation worker

- [ ] 4.1 Implement the reconciliation consumer with one database transaction per event and retry-safe acknowledgement behavior; verify successful events are acknowledged only after commit.
- [ ] 4.2 Implement deterministic `SELECT FOR UPDATE` locking for debtor and creditor accounts, double-entry balance updates, and receivable `PAID`/`PARTIAL` transitions; verify full and partial payment integration scenarios.
- [ ] 4.3 Implement duplicate-event detection and durable processing records, including same-account handling and failed-payment outcomes; verify duplicate delivery changes balances only once.
- [ ] 4.4 Add the 100-concurrent-payment integration test and verify no lost updates, dirty reads, double-spending, or inconsistent receivable state.

## 5. Analytics Parquet export

- [ ] 5.1 Add a lightweight Parquet writer and S3-compatible client dependency/configuration without introducing Spark; verify dependency resolution and application startup.
- [ ] 5.2 Implement bounded analytics batching and stable event-to-column mapping with Snappy compression; verify generated files contain one record per input event and the expected schema.
- [ ] 5.3 Implement date-partitioned object naming under `year={YYYY}/month={MM}/day={DD}/payments_{timestamp}.parquet` and upload to S3; verify object retrieval from LocalStack.
- [ ] 5.4 Acknowledge analytics messages only after durable upload and make retries safe using batch identity/object detection; verify upload failure leaves messages retryable and does not drop records.

## 6. End-to-end verification and rollout

- [ ] 6.1 Add an end-to-end test that creates multiple account types, rejects duplicate `(client, bank, account type)` creation with `409`, submits a payment with `202`, and observes both queue deliveries.
- [ ] 6.2 Add a data-lake verification test that downloads a generated Parquet object and queries its schema and records with a Parquet-compatible reader.
- [ ] 6.3 Run Liquibase, application, integration, concurrency, and LocalStack end-to-end test suites together; verify all existing receivables and security tests remain green.
- [ ] 6.4 Document deployment order, consumer enablement, queue monitoring, rollback precautions, and the precondition for making receivable account references non-null.
