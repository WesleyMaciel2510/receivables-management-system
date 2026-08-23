## Context

The bootstrap change provides a Spring Boot application, layered packages, profile configuration, Actuator, and LocalStack, but no persistence or business resources. The architecture and domain planning documents define PostgreSQL, UUID-based clients and receivables, layered `Controller → Service → Repository` flow, Brazilian client fields, and receivable statuses. See `proposal.md` and the `receivables-domain` spec for the motivation and observable contract.

## Goals / Non-Goals

**Goals:**

- Add a PostgreSQL-backed persistence boundary with reproducible schema migrations.
- Implement Client and Receivable CRUD APIs with validation and clear HTTP error behavior.
- Keep monetary values precise and enforce receivable balance and lifecycle invariants in the service layer.
- Keep all database connection details externalized for local, dev, and prod profiles.
- Establish test seams that later payment, audit, document, and dashboard changes can extend.

**Non-Goals:**

- Payment creation, refunds, reversals, or adjustments.
- Transaction/audit event persistence or publication.
- S3 document upload, SQS reconciliation, Spring Batch overdue processing, authentication, or dashboard aggregation.
- A production database deployment or cloud infrastructure.

## Decisions

### 1. PostgreSQL with Liquibase migrations

Use PostgreSQL as the first database choice recorded by the architecture plan and Liquibase for ordered, versioned DDL with explicit rollback blocks. This gives local Docker and future production environments the same schema history. An embedded database is rejected because it would hide PostgreSQL-specific constraints and diverge from the intended system. The existing executed SQL migration remains untouched; Liquibase preconditions baseline those tables safely.

### 2. Spring Data JPA with explicit domain mappings

Use JPA entities in the existing model layer and Spring Data repositories behind the repository package. Map UUID identifiers, `BigDecimal` amounts, dates, enums, and the client relationship explicitly rather than relying on implicit naming. This keeps persistence details out of controllers and services while remaining consistent with the existing layered architecture.

### 3. DTO boundaries for REST resources

Use separate request and response DTOs for clients and receivables. Bean validation handles structural input errors at the API boundary; services handle existence checks, status transitions, and financial invariants. Entities are never accepted directly from or serialized directly to API callers.

### 4. Explicit lifecycle and balance rules

New receivables start as `PENDING` with remaining balance equal to original amount. Since payment processing is out of scope, this change preserves the balance on ordinary edits and rejects changes that would make it negative or exceed the original amount. Status transition policy is centralized in the service so future payment and overdue jobs can reuse it.

### 5. Local PostgreSQL in Compose without committed secrets

Add a pinned PostgreSQL service to the existing Compose network and pass connection values through environment substitution/profile configuration. Development-only defaults, if required by the container image, must be supplied by environment variables or an ignored local file and must not be committed as application credentials.

### 6. Error mapping at the API boundary

Use a shared controller exception handler to map validation failures, missing resources, conflicts, and invalid lifecycle transitions to stable HTTP statuses and structured error bodies. This avoids leaking persistence exceptions and gives all endpoints consistent behavior.

## Risks / Trade-offs

- **Schema evolution can break local data** → Keep migrations forward-only, document destructive changes, and use disposable local database volumes during early development.
- **Deleting clients with receivables can violate referential integrity** → Enforce a restrictive foreign key and return a conflict unless a future explicit archival policy is introduced.
- **Status rules may evolve when payments are added** → Keep transitions in one service policy and cover them with tests before integrating payment workflows.
- **CPF/CNPJ validation has format and checksum complexity** → Normalize input and isolate validation so the rule can be replaced or expanded without changing the resource contract.
- **Database credentials can leak through Compose examples** → Use environment substitution and add credential-pattern checks to the implementation task.

## Migration Plan

1. Add dependencies and profile/database environment variables.
2. Add the PostgreSQL Compose service and verify Liquibase can initialize a clean volume.
3. Apply the versioned migration and run integration tests against PostgreSQL.
4. Deploy the application with the migration enabled before exposing the new endpoints.
5. Roll back application code by redeploying the prior image; do not automatically roll back an applied migration. Any schema rollback must be a separately reviewed migration.

## Open Questions

None. Payment semantics, audit events, and overdue scheduling are intentionally deferred to later changes and do not alter this task breakdown.
