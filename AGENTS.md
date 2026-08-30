# RMS — Receivables Management System

## Quick Start
```bash
# 1. Copy env and fill secrets (JWT, encryption)
cp .env-example .env

# 2. Start infra (PostgreSQL + LocalStack)
docker-compose up -d

# 3. Build & run (uses 'local' profile, port 8081)
mvn clean install
mvn spring-boot:run
```

## Required Environment Variables
| Variable | Source |
|----------|--------|
| `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_URL` | `.env` |
| `APP_ENCRYPTION_SECRET` (32-char hex) | `.env` |
| `APP_SECURITY_JWT_SECRET` (32-char hex) | `.env` |
| `APP_SECURITY_PASSWORD` | `.env` |

## Developer Commands
| Task | Command |
|------|---------|
| Run all tests | `mvn test` |
| Run concurrency/load test | `mvn test -Dtest=PaymentConcurrencyLoadTest` |
| Build only | `mvn clean install -DskipTests` |
| Lint (if configured) | `mvn checkstyle:check` |
| View API docs | `http://localhost:8081/swagger-ui.html` |

## Architecture Notes
- **Entry point**: `RmsApplication.java` — Spring Boot 4, Java 25
- **Default profile**: `local` (port 8081)
- **DB migrations**: Liquibase (`db/changelog/*.yaml`), auto-run on startup
- **Event flow**: `POST /api/v1/payments` → SNS FIFO → dual SQS FIFO:
  - `payment-reconciliation-queue.fifo` → `ReconciliationSqsListener` (pessimistic locks)
  - `payment-analytics-queue.fifo` → `AnalyticsBatchWorker` (DuckDB → Parquet → S3)
- **Security**: JWT (RS256 via LocalStack KMS), field-level AES-GCM encryption for CPF/CNPJ

## Key Conventions
- **No direct UPDATE/DELETE on `payments` table** — append-only, reversals via explicit workflows
- **Unique constraint**: `(client_id, bank_id, account_type)` on `bank_accounts` → HTTP 409 on conflict
- **Fee rules**: `PAYMENT` accounts require `monthlyMaintenanceFee = 0.00`
- **Receivable status transitions**: `PENDING` → `PARTIAL` → `PAID` / `OVERDUE`

## Test Notes
- Integration tests require Docker (LocalStack + Postgres)
- Concurrency test fires 100 parallel requests to validate `SELECT FOR UPDATE` locking
- Data Lake test downloads Parquet from LocalStack S3 and validates schema via DuckDB

## LocalStack Init
`init-aws.sh` runs on LocalStack startup — creates:
- SNS topic: `payment-fanout-topic.fifo`
- SQS queues: `payment-reconciliation-queue.fifo`, `payment-analytics-queue.fifo`
- S3 bucket: `recebiveis-analytics`

## Agent Execution Rules
- **Batch File Edits**: Apply all file modifications, creations, and updates directly in one pass without asking for confirmation on individual files.
- **Autonomous Operations**: When asked to implement or update code, execute all necessary file changes across the project immediately.