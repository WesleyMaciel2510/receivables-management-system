## Context

The current persistence model has JPA entities backed by Liquibase-managed tables and client/receivable `DELETE` endpoints. The affected tables identified in the current model are `clients`, `receivables`, `app_users`, `banks`, `bank_accounts`, `payment_effects`, and payment-related tables introduced by the banking-core changelog. See proposal.md for motivation and specs/soft-delete/spec.md for the behavioral contract.

## Goals / Non-Goals

**Goals:**

- Establish one consistent nullable `Instant` deletion state for all managed primary entities.
- Make normal JPA reads exclude archived rows and make JPA deletes issue timestamp updates.
- Preserve API compatibility for client and receivable deletion, including no-content success responses.
- Provide an explicit, authorization-aware archived query path without weakening active-record filtering.
- Keep the migration additive and safe for existing data.

**Non-Goals:**

- Rewriting or removing existing audit tables, primary keys, or historical records.
- Adding restore/undelete workflows.
- Exposing archived data through ordinary public list/detail endpoints.
- Changing unrelated authorization policy beyond protecting the archived lookup.

## Decisions

- **Use a shared mapped soft-delete field where the entity model permits it.** A mapped superclass or equivalent shared base keeps column mapping consistent; entities without a common base receive the same field explicitly. The field is an `Instant` mapped to `deleted_at`.
- **Use Hibernate SQL delete and restriction annotations.** `@SQLDelete` changes entity deletion into an `UPDATE ... SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?`; `@SQLRestriction("deleted_at IS NULL")` provides current Hibernate filtering. If the project’s exact Hibernate version does not support that annotation, use its supported equivalent (`@Where`) while preserving the same behavior. These annotations are preferred over service-only flags because all JPA deletion paths and default reads share the rule.
- **Add one Liquibase migration for all applicable tables.** The migration adds nullable `TIMESTAMP WITH TIME ZONE` columns and does not alter keys or audit tables. It is appended to the master changelog after the existing domain migrations.
- **Use explicit archived repository queries.** Hibernate restrictions are active-record defaults, so archived reads must use a deliberate native or unrestricted query mechanism that can select `deleted_at IS NOT NULL`. The administrative controller delegates to a service/repository boundary and applies existing authorization conventions.
- **Retain existing endpoint shapes.** Client and receivable `DELETE /{id}` endpoints continue to return `204 No Content` on successful deletion. Normal reads inherit the active filter; archived endpoints are additive and separately authorized.

## Risks / Trade-offs

- [Risk] Hibernate annotation behavior can vary by version, especially around unrestricted archived queries. → Mitigation: confirm the dependency version, use the supported restriction annotation, and add integration tests for both active and archived paths.
- [Risk] Foreign-key relationships may cause archived parent records to appear inconsistent in active views. → Mitigation: define relationship/query expectations in repository tests and avoid cascading physical deletes.
- [Risk] Existing bulk JPQL/native delete statements can bypass entity-level `@SQLDelete`. → Mitigation: inventory delete queries and replace them with explicit soft-delete updates or entity deletion before rollout.
- [Risk] A partially applied migration could leave entities and schema out of sync. → Mitigation: make the migration additive, validate it in a clean database, and test startup plus rollback handling in deployment automation.

## Migration Plan

1. Apply the additive Liquibase migration in a maintenance-safe deployment; existing rows receive `NULL` deletion timestamps.
2. Deploy entity mappings, repository queries, services, and controllers together.
3. Verify normal reads omit archived rows, deletes retain rows, and authorized archived lookups work.
4. If application deployment must be rolled back, keep the additive columns in place and roll back code only; do not drop columns or remove retained audit data.
