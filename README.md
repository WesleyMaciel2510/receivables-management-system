# 🏦 RMS — Receivables Management System

> A study-case project exploring **banking-style ledger and receivables patterns** — built with **Java 25**, **Spring Boot 4**, **PostgreSQL**, **AWS SNS/SQS**, **LocalStack/S3**, and **DuckDB / Apache Parquet** and more. 
> 

---

> 🌌🪐🌎 **Just like the universe, our knowledge increases with time.** 🌕💫☄️

---

## 📋 Table of Contents
- [👤 About This Project](#-about-this-project)
- [🎯 Project Overview & Objectives](#-project-overview--objectives)
- [⚡ Key Architecture & Mechanisms](#-key-architecture--mechanisms)
- [📐 System Architecture Diagram](#-system-architecture-diagram)
- [🗄️ Domain Model & Data Architecture](#️-domain-model--data-architecture)
- [🔌 API Endpoints Reference](#-api-endpoints-reference)
- [📊 Asynchronous Analytics & Data Lake Pipeline](#-asynchronous-analytics--data-lake-pipeline)
- [⚖️ Business Rules & Domain Mechanics](#️-business-rules--domain-mechanics)
- [🛠️ Technology Stack](#️-technology-stack)
- [🚀 Quick Start & Local Setup](#-quick-start--local-setup)
- [🧪 Verification & Load Testing](#-verification--load-testing)

---

## 👤 About This Project

This is a **personal side project**, not a production system. The goal is to practice patterns commonly used in real banking/fintech backends — event-driven ingestion, double-entry ledgers, concurrency control, and a lightweight analytics pipeline — as a way to learn and demonstrate familiarity with the kind of architecture larger teams use. It isn't audited, isn't battle-tested at scale, and doesn't claim production-grade uptime or throughput guarantees; the numbers and behaviors below describe what the code is designed and tested to do locally, not a live SLA.

---

## 🎯 Project Overview & Objectives

RMS (*Receivables Management System*) models receivables tracking for Brazilian businesses on top of a simple multi-bank ledger, as a way to practice the mechanics below:

* 🏛️ **Multi-Bank Client Ledgers** — Checking, Payment, Savings, Escrow, and Settlement accounts linked to COMPE/ISPB financial institutions.
* ⚡ **Non-Blocking Ingestion** — Payment events go through `POST /api/v1/payments` (`202 Accepted`) via an AWS SNS/SQS fanout pattern, targeting sub-30ms response times in local testing.
* 🔒 **Double-Entry Integrity** — Guards against race conditions and double-spending via database-level **Pessimistic Locking** (`SELECT FOR UPDATE`) during asynchronous reconciliation.
* 📈 **Receivables Lifecycle Control** — Tracks status transitions (`PENDING` → `PARTIAL` → `PAID` / `OVERDUE`).
* 🗃️ **In-Memory Data Lake Streaming** — Converts transaction events into Snappy-compressed **Apache Parquet** files via DuckDB and uploads them to S3, without needing a Spark cluster.

---

## ⚡ Key Architecture & Mechanisms

### 1. Non-Blocking SNS/SQS Fanout Pipeline 📨
When payments arrive, the HTTP layer validates the contract and emits a `PaymentReceivedEvent` to an **AWS SNS FIFO Topic**, returning `202 Accepted` (targeting < 30ms locally). The event fans out to dual SQS FIFO queues:
* 🔄 **`payment-reconciliation-queue.fifo`** — Consumed by the transactional double-entry ledger engine.
* 📊 **`payment-analytics-queue.fifo`** — Consumed by the columnar analytics pipeline.

### 2. Double-Entry Concurrency & Pessimistic Locks 🔐
* The `ReconciliationSqsListener` acquires row-level pessimistic locks (`SELECT FOR UPDATE`) on target `BankAccount` balances in PostgreSQL.
* Debits/credits execute atomically.
* The corresponding `Receivable` balance updates and adjusts status (`PARTIAL` or `PAID`).
* An immutable audit log (`Transaction`) records every status transition.

### 3. Lightweight Columnar Data Lake Ingestion 📊
* `AnalyticsBatchWorker` processes JSON messages from `payment-analytics-queue.fifo`.
* Transforms payloads into **Apache Parquet** format using **DuckDB / parquet-avro**.
* Streams Snappy-compressed datasets to S3 using date partitioning:
  `s3://recebiveis-analytics/year={YYYY}/month={MM}/day={DD}/payments_{timestamp}.parquet`

### 4. Automated Delinquency Batch ⏱️
A scheduled **Spring Batch** job checks active receivables daily. Any item where `dueDate < CURRENT_DATE` and `remainingBalance > 0` automatically transitions to `OVERDUE`.

---

## 📐 System Architecture Diagram

```text
[ 💻 REST Client / Partner Fintech ]
                │
                ▼
[ 📥 POST /api/v1/payments ]  ─── (Non-blocking < 30ms Response)
                │
                ▼
[ 📢 SNS: payment-events.fifo ]
                │
    ┌───────────┴───────────────────┐
    ▼                                ▼
[ 📥 SQS: reconciliation-q ]    [ 📥 SQS: analytics-q ]
    │                                │
    ▼                                ▼
[ 🔒 Ledger Engine Worker ]     [ 📊 Analytics Batch Worker ]
  (SELECT FOR UPDATE)             (DuckDB / Parquet Engine)
    │                                │
    ▼                                ▼
[ 🐘 PostgreSQL DB ]             [ 🪣 AWS S3 Data Lake ]
  (Row-Store ACID)                 (.parquet Format)
```

---

## 🗄️ Domain Model & Data Architecture

> [!NOTE]
> Database schema updates and seed data are maintained using **Liquibase** migrations (`v2__banking_core_entities.sql`).

### 🛠️ Core Entities

#### 1. `Bank` (`banks`) 🏦
* `id` (UUID, Primary Key)
* `compeCode` (VARCHAR 3, Unique) — e.g., `"001"`, `"341"`
* `ispb` (VARCHAR 8) — e.g., `"00000000"`
* `name` (VARCHAR 100)

#### 2. `Client` (`clients`) 👤
* `id` (UUID, Primary Key)
* `clientType` (`INDIVIDUAL`, `CORPORATE`, `FINTECH_PARTNER`)
* `documentNumber` (VARCHAR — Encrypted CPF/CNPJ)
* `fullName` / `companyName` (VARCHAR)
* `status` (`ACTIVE`, `BLOCKED`, `SUSPENDED`)

#### 3. `BankAccount` (`bank_accounts`) 💳
* `id` (UUID, Primary Key)
* `clientId` (UUID, FK → `Client`)
* `bankId` (UUID, FK → `Bank`)
* `accountNumber` (VARCHAR 20)
* `agency` (VARCHAR 10)
* `accountType` (`CHECKING`, `PAYMENT`, `SAVINGS`, `ESCROW`, `SETTLEMENT`)
* `balance` (DECIMAL 19,4)
* `monthlyMaintenanceFee` (DECIMAL 10,2)
* ⚠️ **Database Constraint**: `UNIQUE(client_id, bank_id, account_type)`

#### 4. `Receivable` (`receivables`) 📜
* `id` (UUID, Primary Key)
* `clientId` (UUID, FK → `Client`)
* `debtorAccountId` (UUID, FK → `BankAccount`)
* `creditorAccountId` (UUID, FK → `BankAccount`)
* `originalAmount` / `remainingBalance` (DECIMAL 19,2)
* `dueDate` (DATE)
* `status` (`PENDING`, `PARTIAL`, `PAID`, `OVERDUE`, `CANCELLED`)

#### 5. `Payment` (`payments`) 💸
> [!IMPORTANT]
> Payments are **append-only** financial records. Reversals or adjustments must occur via explicit Refund/Adjustment workflows.

* `id` (UUID, Primary Key)
* `receivableId` (UUID, FK → `Receivable`)
* `amount` (DECIMAL 19,2)
* `paymentMethod` (`PIX`, `CREDIT_CARD`, `DEBIT_CARD`, `BOLETO`, `CASH`, `TED`, `DIGITAL_WALLET`)
* `transactionReference` (VARCHAR)

#### 6. `Document` & `Transaction` Audit Log 📂
* `Document` — Stores `fileName` and `s3Key` for payment receipt verification.
* `Transaction` — Audit log tracking state changes (`RECEIVABLE_CREATED`, `PAYMENT_RECEIVED`, `CANCELLATION`, `MANUAL_ADJUSTMENT`).

---

## 🔌 API Endpoints Reference

### 🏦 Multi-Bank & Account Management
| Method | Endpoint | Description | Status Code |
| :--- | :--- | :--- | :---: |
| `GET` | `/api/v1/banks` | List pre-populated Brazilian banks | `200 OK` |
| `POST` | `/api/v1/clients/{id}/accounts` | Register a bank account | `201 Created` |
| `GET` | `/api/v1/clients/{id}/accounts` | Fetch all accounts for client | `200 OK` |

### 👤 Client Management
| Method | Endpoint | Description | Status Code |
| :--- | :--- | :--- | :---: |
| `POST` | `/api/v1/clients` | Register a new client | `201 Created` |
| `GET` | `/api/v1/clients` | Paginated client listing | `200 OK` |
| `GET` | `/api/v1/clients/{id}` | Get client details | `200 OK` |
| `PUT` | `/api/v1/clients/{id}` | Update client info | `200 OK` |
| `DELETE` | `/api/v1/clients/{id}` | Suspend client | `204 No Content` |

### 📜 Receivables Management
| Method | Endpoint | Description | Status Code |
| :--- | :--- | :--- | :---: |
| `POST` | `/api/v1/receivables` | Register receivable | `201 Created` |
| `GET` | `/api/v1/receivables` | Search receivables | `200 OK` |
| `GET` | `/api/v1/receivables/{id}` | Get receivable balance | `200 OK` |
| `PATCH` | `/api/v1/receivables/{id}/status` | Update status (e.g., `CANCELLED`) | `200 OK` |

### ⚡ Non-Blocking Ingestion & Receipts
| Method | Endpoint | Description | Status Code |
| :--- | :--- | :--- | :---: |
| `POST` | `/api/v1/payments` | Publish event to SNS/SQS (<30ms) | `202 Accepted` |
| `GET` | `/api/v1/payments/{id}` | Query payment execution state | `200 OK` |
| `POST` | `/api/v1/receivables/{id}/documents` | Upload receipt proof to S3 | `201 Created` |

---

## 📊 Asynchronous Analytics & Data Lake Pipeline

```text
[ Raw SQS Payload ] ➔ [ DuckDB In-Memory Engine ] ➔ [ Parquet File ] ➔ [ S3 Storage ]
```

1. **Ingestion** — Raw JSON payloads from `payment-analytics-queue.fifo` are batched by `AnalyticsBatchWorker`.
2. **Columnar Transformation** — Converts untyped JSON stream into structured columnar `.parquet` schema compressed with Snappy.
3. **Partitioned Export** — Writes directly to S3 without intermediary disk writes:

```text
s3://recebiveis-analytics/year=2026/month=08/day=23/payments_1724409100.parquet
```

---

## ⚖️ Business Rules & Domain Mechanics

* 🔒 **Unique Account Guard** — Enforces `UNIQUE(client_id, bank_id, account_type)`. Violations return `HTTP 409 Conflict`.
* 💵 **Fee Matrix Policy**:
    * `PAYMENT` accounts strictly require `monthlyMaintenanceFee = 0.00`.
    * `CHECKING` accounts support custom monthly recurring rates.
* 📊 **Dynamic Settlement Rules**:
    * `paymentAmount < remainingBalance` → Balance reduced, status set to `PARTIAL`.
    * `paymentAmount >= remainingBalance` → Balance set to `0.00`, status set to `PAID`.
* 🛑 **Financial Audit Immutability** — No direct `UPDATE` or `DELETE` operations allowed on `payments`.

---

## 🛠️ Technology Stack

| Category | Technology |
| :--- | :--- |
| **Core Runtime** | Java 25, Spring Boot 4 |
| **Relational Database** | PostgreSQL, Liquibase |
| **Messaging & Queues** | AWS SNS FIFO, AWS SQS FIFO (via LocalStack) |
| **Data Lake & Cloud** | AWS S3, DuckDB, Apache Parquet (Snappy) |
| **Scheduled Jobs** | Spring Batch |
| **Security** | Spring Security, JWT, CPF/CNPJ Encryption |
| **Email Delivery** | Gmail SMTP |

---

## 🚀 Quick Start & Local Setup

### Prerequisites
* ☕ **JDK 25+**
* 🐳 **Docker & Docker Compose**
* 📦 **Maven 3.9+**

### 1. Provision Local Infrastructure
Launch PostgreSQL and LocalStack containerized services:

```bash
docker-compose up -d
```

### 2. Configure Environment Variables
Copy the sample file and fill in secrets:

```bash
cp .env-example .env
```

Required variables:
| Variable | Description |
| :--- | :--- |
| `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_URL` | PostgreSQL connection |
| `APP_ENCRYPTION_SECRET` | 32-char hex for field encryption |
| `APP_SECURITY_JWT_SECRET` | 32-char hex for JWT signing |
| `APP_SECURITY_PASSWORD` | Bootstrap admin password |
| `GMAIL_USERNAME` | Gmail address (used as `from` in SMTP) |
| `GMAIL_APP_PASSWORD` | Gmail App Password (not account password) |

**Email delivery**: Gmail SMTP. The client receives a generic `200 OK` response regardless of delivery success; failures are logged server-side only.

### 3. Auto-Provision Queues, Topics & Buckets
LocalStack runs `init-aws.sh` automatically on startup:

* 📢 **SNS Topic**: `arn:aws:sns:us-east-1:000000000000:payment-fanout-topic.fifo`
* 📥 **SQS Queues**: `payment-reconciliation-queue.fifo`, `payment-analytics-queue.fifo`
* 🪣 **S3 Buckets**: `recebiveis-analytics`

Liquibase automatically seeds standard Brazilian banks on application startup (001-BB, 033-Santander, 104-Caixa, 237-Bradesco, 341-Itaú, 260-Nubank).

### 3. Build & Run Application

```bash
mvn clean install
mvn spring-boot:run
```

---

## 🧪 Verification & Load Testing

Execute the test suites to check the implementation:

```bash
# Run standard integration tests
mvn test

# Run strict pessimistic lock concurrency tests
mvn test -Dtest=PaymentConcurrencyLoadTest
```

* 🧪 **Conflict Test** — Validates `HTTP 409 Conflict` when registering duplicate account types under the same bank.
* ⚡ **Concurrency Load Test** — Fires 100 parallel payment requests to check that row-level pessimistic locking (`SELECT FOR UPDATE`) prevents double-spending and dirty reads.
* 🪣 **Data Lake Test** — Downloads generated `.parquet` files from LocalStack S3 and queries the schema via DuckDB to confirm columnar integrity.