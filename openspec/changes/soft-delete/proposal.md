## Why

The application currently relies on physical deletion for domain records, which removes data needed for auditability and can weaken historical traceability. This change introduces audit-compliant soft deletion while preserving existing identifiers, history, and normal API behavior.

## What Changes

- Add nullable, time-zone-aware `deleted_at` columns to all primary domain entity tables through Liquibase migrations.
- Add shared soft-delete state to the applicable JPA base entities and configure Hibernate to update `deleted_at` instead of physically deleting rows.
- Filter soft-deleted records from ordinary repository queries and `GET` endpoints by default.
- Refactor client and receivable `DELETE` endpoints to return successful no-content responses while retaining the rows for audit purposes.
- Preserve primary keys and historical audit tables; do not physically remove soft-deleted records.
- Add an administrative archived-record lookup capability for records with `deleted_at` populated, subject to the project’s authorization conventions.

## Capabilities

### New Capabilities

- `soft-delete`: Audit-preserving deletion semantics, persistence filtering, and archived-record retrieval for primary domain entities.

### Modified Capabilities

<!-- No existing capability specs are present in openspec/specs/. -->

## Impact

- Liquibase changelogs and the schemas for clients, receivables, payments, and other primary domain tables.
- JPA entity mappings, repositories, and delete behavior across the domain model.
- Client and receivable controllers/services, including default and administrative read paths.
- Integration and repository tests covering deletion, filtering, archival lookup, and migration compatibility.
