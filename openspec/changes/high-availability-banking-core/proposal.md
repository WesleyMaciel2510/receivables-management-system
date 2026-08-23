## Why

The current receivables application does not provide the account, ledger, and event-processing primitives required for reliable multi-bank payment operations. This change establishes an enterprise-grade banking core now so payment ingestion can remain responsive, reconciliation can be concurrency-safe, and transaction data can be delivered to an analytics lake without coupling analytics work to the request path.

## What Changes

- Add banks, multi-bank client accounts, account types, lifecycle rules, balances, and maintenance-fee rules.
- Enforce one account of each type per client and bank at both the database and domain-service layers.
- Link receivables to debtor and creditor bank accounts.
- Add bank-account and bank-directory REST APIs.
- Change payment submission to validate and publish an event asynchronously, returning `202 Accepted`.
- Add SNS FIFO fanout to separate reconciliation and analytics workloads through SQS FIFO queues.
- Reconcile payments with double-entry debit/credit updates under pessimistic database locking and idempotent status transitions.
- Add a lightweight batch analytics worker that converts transaction JSON into Snappy-compressed Parquet and uploads partitioned files to S3-compatible storage.
- Provision the LocalStack SNS, SQS, and S3 resources needed by the pipeline.
- Add integration, concurrency, and Parquet data-lake verification tests.

## Capabilities

### New Capabilities

- `banking-core/accounts`: Manage banks and client bank accounts, including account-type uniqueness, lifecycle validation, balance fields, and maintenance-fee rules.
- `payment-ingestion`: Accept and validate payment requests, publish payment-received events through SNS FIFO fanout, and expose asynchronous acceptance semantics.
- `payment-reconciliation`: Consume reconciliation events and apply safe, idempotent double-entry ledger updates to receivables and bank accounts.
- `payment-analytics-export`: Consume transaction events in batches, create partitioned Snappy Parquet files, and upload them to S3-compatible object storage.

### Modified Capabilities

<!-- No existing openspec/specs capability files are present in this repository. -->

## Impact

- Database: Liquibase migrations add `banks` and `bank_accounts`, seed Brazilian banks, add receivable account references, and introduce required indexes and constraints.
- Application domain and APIs: new bank/account models, DTOs, services, controllers, payment event contracts, and reconciliation behavior.
- Messaging and infrastructure: LocalStack SNS/SQS FIFO queues and subscriptions, plus an analytics S3 bucket/path convention.
- Dependencies and configuration: AWS messaging/S3 integration and a lightweight Parquet/DuckDB-compatible library, with queue and storage settings for local and deployed environments.
- Verification: existing receivables/payment flows must remain compatible where not explicitly changed; new tests cover conflict responses, concurrent payments, locking, and generated Parquet records.
