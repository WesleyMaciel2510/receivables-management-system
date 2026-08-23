## Context

The current service is a Spring Boot application with PostgreSQL, Liquibase-managed schema changes, and a synchronous receivables API. The proposal and capability specs define the externally visible behavior; this design establishes the persistence, messaging, and worker boundaries needed to add that behavior without making HTTP requests wait for ledger or analytics processing.

## Goals / Non-Goals

**Goals:**

- Extend the existing relational domain with bank accounts and explicit receivable account ownership.
- Make payment acceptance durable and asynchronous while keeping reconciliation and analytics independently retryable.
- Preserve accounting correctness under concurrent events and duplicate delivery.
- Produce small, queryable Parquet files without introducing a distributed Spark runtime.
- Keep LocalStack configuration representative of the production SNS, SQS, and S3 topology.

**Non-Goals:**

- Replacing the existing authentication and authorization model.
- Building a general-purpose general ledger, card processor, settlement network, or bank connectivity adapter.
- Providing real-time analytics query APIs or a production data warehouse.
- Supporting cross-region disaster recovery in this change; the design leaves room for a later outbox/replication enhancement.

## Decisions

### Relational model and migration

Add `banks` and `bank_accounts` through the next Liquibase change, with UUID primary keys, indexed unique COMPE codes, foreign keys to clients and banks, decimal monetary columns, and a database unique constraint on `(client_id, bank_id, account_type)`. Add nullable debtor and creditor account references to receivables first so existing rows remain deployable; application validation will require the references for new payment flows. Seed the standard Brazilian banks with an idempotent data change.

The database is the final authority for uniqueness. The service performs a pre-check to return a useful conflict response, while translating a concurrent unique-constraint failure into the same HTTP `409` result. Monetary values use `BigDecimal` semantics and the account service applies the payment-fee invariant before persistence.

Alternative considered: encode account uniqueness only in Java. Rejected because concurrent requests could bypass an application pre-check and create duplicate accounts.

### Payment event topology

Use one SNS FIFO topic with two SQS FIFO subscriptions: reconciliation and analytics. Events carry a stable payment/event identifier, a message group key based on the relevant account or business partition, schema version, transaction timestamp, amount, account IDs, and receivable ID. The publisher sends the event before returning `202`; it does not synchronously call either consumer.

Use explicit queue visibility timeouts, dead-letter queues or equivalent redrive settings, and consumer acknowledgements only after successful processing. FIFO deduplication is an optimization, not the sole idempotency mechanism, because retries can occur after visibility expiration or across deployments.

Alternative considered: publish directly to two application-owned queues. Rejected because SNS provides the required independent fanout and keeps publisher knowledge of consumers small.

### Reconciliation transaction

The reconciliation handler runs one database transaction per payment event. It first checks a durable processed-event/payment-effect record, then locks the debtor and creditor account rows in deterministic UUID order using `SELECT FOR UPDATE`, validates the debit, applies both balance changes, updates the receivable status and applied amount, and records the event as processed. The processed record is protected by a unique event identifier so duplicate deliveries become harmless acknowledgements.

If the debtor and creditor are the same account, the handler must avoid applying two opposing balance mutations and must still record the payment according to the domain rule. Failed validation or insufficient funds is represented as a durable failure outcome and routed through the queue retry/dead-letter policy rather than partially committing one side of the entry.

Alternative considered: optimistic version checks with retries. Rejected for the first implementation because high contention on a single account makes deterministic row locking easier to reason about and verify.

### Analytics worker and object layout

Use a lightweight Java Parquet writer with Snappy compression and an in-memory or bounded temporary batch buffer. The worker normalizes the event contract into a stable column schema, groups records by transaction date, writes one or more bounded files, uploads them through the S3-compatible client, and acknowledges messages only after upload succeeds. Object names include a timestamp and a uniqueness suffix to avoid collisions between workers.

The worker must retain enough batch metadata to retry an interrupted upload without dropping source messages. A failed upload leaves messages visible again; a successfully uploaded file is safe to re-detect through a deterministic batch identity or object metadata before retry acknowledgement.

Alternative considered: Apache Spark. Rejected because the requested workload is a lightweight stream-to-file transformation and Spark would add disproportionate runtime and deployment overhead.

### API and configuration boundaries

Keep account APIs in the existing controller/service/repository layering. Add payment publishing behind a small application service so controllers do not depend directly on AWS client details. Bind topic, queue, bucket, endpoint, region, credentials, batch size, and visibility settings from environment-backed configuration; LocalStack values remain profile-specific.

### Migration and compatibility

Deploy the schema before enabling new consumers. The migration is additive: create tables and columns, seed banks, then deploy application code that uses the new columns and messaging. During rollback, stop consumers and disable new payment submission before reverting application code; retain additive schema objects until data has been migrated or explicitly archived. Do not drop account columns or tables in the rollback path.

## Risks / Trade-offs

- [Risk] A publisher can accept traffic faster than consumers drain queues → [Mitigation] configure queue alarms/metrics, bounded consumer concurrency, visibility extensions, and redrive policies.
- [Risk] A database lock held during a slow transaction reduces throughput for hot accounts → [Mitigation] keep the reconciliation transaction small, lock accounts in deterministic order, and test the 100-concurrent-payment case.
- [Risk] Partial deployment can create events newer consumers cannot parse → [Mitigation] version the event envelope and deploy consumers that tolerate the previous schema before enabling the publisher.
- [Risk] In-memory Parquet batches can exceed worker memory → [Mitigation] cap records and bytes per batch, spill or finalize bounded files, and expose batch-size configuration.
- [Risk] LocalStack behavior differs from AWS FIFO/S3 semantics → [Mitigation] keep infrastructure scripts explicit and run end-to-end tests against the same LocalStack resources used by development.
- [Risk] Existing receivables lack account references → [Mitigation] keep migration columns nullable for rollout, require references only for the new payment path, and provide a data-migration/reconciliation check before production enforcement.
