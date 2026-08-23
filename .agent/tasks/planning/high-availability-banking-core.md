# OpenSpec Task Proposal: high-availability-banking-core

## Objective
Evolve the core application into an enterprise-grade banking receivables and multi-account ledger engine. This release introduces multi-bank account management, event-driven payment reconciliation via SNS/SQS fanout, and an asynchronous analytics pipeline that transforms real-time JSON transaction streams into columnar Apache Parquet files uploaded to S3.

---

## Domain & Database Architecture Expansion

### 1. New Entities & Enums

#### `Bank`
- `id` (UUID, Primary Key)
- `compeCode` (VARCHAR 3 - e.g., "001", "341") - Indexed & Unique
- `ispb` (VARCHAR 8 - e.g., "00000000")
- `name` (VARCHAR 100)

#### `Client` (Updated)
- `id` (UUID, Primary Key)
- `clientType` (ENUM: `INDIVIDUAL`, `CORPORATE`, `FINTECH_PARTNER`)
- `documentNumber` (VARCHAR - Encrypted CPF/CNPJ)
- `fullName` / `companyName`
- `status` (ENUM: `ACTIVE`, `BLOCKED`, `SUSPENDED`)

#### `BankAccount`
- `id` (UUID, Primary Key)
- `clientId` (UUID, FK -> `Client`)
- `bankId` (UUID, FK -> `Bank`)
- `accountNumber` (VARCHAR 20)
- `agency` (VARCHAR 10)
- `accountType` (ENUM: `CHECKING`, `PAYMENT`, `SAVINGS`, `ESCROW`, `SETTLEMENT`)
- `balance` (DECIMAL 19,4 - Double-entry ledger balance)
- `monthlyMaintenanceFee` (DECIMAL 10,2 - `0.00` for `PAYMENT`, custom recurring rate for `CHECKING`)
- Database Constraint: `UNIQUE(client_id, bank_id, account_type)`

---

## Enterprise System Architecture

[REST Client / Partner Fintech]
│
▼
[ POST /api/v1/payments ] (Non-blocking Ingestion)
│
▼
[ SNS Topic: payment-events ]
│                       │
▼                       ▼
[ SQS: reconciliation-q ]   [ SQS: analytics-q ]
│                       │
▼                       ▼
[ Ledger Engine ]         [ Parquet Batch Builder ]
(Pessimistic Lock)         (DuckDB/Parquet-Avro)
│                       │
▼                       ▼
[ PostgreSQL DB ]         [ LocalStack S3 Bucket ]
(Row Store)               (.parquet Lake Format)


---

## Tasks

### Phase 1: Database & Liquibase Schema Migration
- [ ] Create Liquibase migration `v2__banking_core_entities.sql`:
    - Create `banks` table.
    - Create `bank_accounts` table with unique constraint `uk_client_bank_account_type (client_id, bank_id, account_type)`.
    - Update `receivables` table to link `debtor_account_id` and `creditor_account_id`.
- [ ] Add pre-populated seed migration with standard Brazilian banks (001-BB, 033-Santander, 104-Caixa, 237-Bradesco, 341-Itaú, 260-Nubank).

### Phase 2: Domain Logic & Fee Matrix Rule Engine
- [ ] Implement `BankAccount` lifecycle validation:
    - Validate that a client cannot create a duplicate `accountType` in the same `Bank`.
    - Automatically evaluate `monthlyMaintenanceFee`: strictly `0.00` if `accountType == PAYMENT`, configurable if `CHECKING`.
- [ ] Implement REST Endpoints:
    - `POST /api/v1/clients/{id}/accounts`
    - `GET /api/v1/clients/{id}/accounts`
    - `GET /api/v1/banks`

### Phase 3: SQS / SNS Event-Driven Messaging Pipeline
- [ ] Provision AWS infrastructure in `init-aws.sh` (LocalStack):
    - SNS Topic: `payment-fanout-topic.fifo`
    - SQS Queue: `payment-reconciliation-queue.fifo`
    - SQS Queue: `payment-analytics-queue.fifo`
    - Bind queues to SNS topic.
- [ ] Implement `SnsEventPublisher`:
    - On `POST /api/v1/payments`, validate basic payload, publish `PaymentReceivedEvent` to SNS topic, and return `202 Accepted` (< 30ms response time).
- [ ] Implement `ReconciliationSqsListener`:
    - Consume from `payment-reconciliation-queue.fifo`.
    - Execute double-entry ledger debit/credit using `SELECT FOR UPDATE` on `BankAccount`.
    - Mark `Receivable` as `PAID` or `PARTIAL`.

### Phase 4: Columnar Analytics & Parquet S3 Export
- [ ] Add `duckdb` or `parquet-avro` lightweight Java dependency to `pom.xml`.
- [ ] Implement `AnalyticsBatchWorker`:
    - Consume batch messages from `payment-analytics-queue.fifo` or scheduled interval.
    - Convert raw JSON transaction payloads into structured `.parquet` files in-memory using Snappy compression.
- [ ] Upload generated `.parquet` files to LocalStack S3 path:
  `s3://recebiveis-analytics/year={YYYY}/month={MM}/day={DD}/payments_{timestamp}.parquet`.

### Phase 5: Verification & End-to-End Testing
- [ ] Integration Test: Verify creation of multiple bank accounts succeeds for different types, but fails with HTTP 409 Conflict when violating `(clientId, bankId, accountType)`.
- [ ] Load Test Simulation: Fire 100 concurrent payments for the same account -> verify zero double-spending or dirty reads via SQS FIFO reconciliation.
- [ ] Data Lake Verification Test: Download generated `.parquet` from S3 and query schema/records to verify columnar structure.

---

## Definition of Done (DoD)
1. Unique constraint `(client_id, bank_id, account_type)` is enforced at both DB level and Java domain service level.
2. `POST /api/v1/payments` yields `202 Accepted` with asynchronous SQS/SNS fanout processing.
3. Payment reconciliation executes safely with database locking, preventing race conditions.
4. Parquet files are generated without full Apache Spark runtime overhead and stored in S3.
5. All migrations execute cleanly via Liquibase without manual intervention.