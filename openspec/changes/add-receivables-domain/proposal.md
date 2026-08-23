## Why

The project foundation is now runnable, but it has no persistence model or business API for the receivables workflow. The next change should establish the PostgreSQL/Liquibase foundation and deliver the first two roadmap capabilities—client management and receivable management—so later payment, audit, document, and collection features have stable contracts to build on.

## What Changes

- Add PostgreSQL and Liquibase support with externalized profile configuration and an initial schema migration.
- Introduce Client and Receivable domain models, enums, persistence repositories, DTOs, validation, services, and REST controllers.
- Expose CRUD endpoints for clients and receivables according to the architecture plan.
- Enforce receivable status transitions, monetary validation, due-date validation, and remaining-balance invariants.
- Add integration coverage for persistence, client endpoints, receivable endpoints, and the core business rules.

## Capabilities

### New Capabilities

- `receivables-domain`: PostgreSQL-backed Client and Receivable management with validated REST APIs and receivable lifecycle rules.

### Modified Capabilities

None.

## Impact

- Adds PostgreSQL, Liquibase, Spring Data JPA, and validation dependencies.
- Adds profile-based database configuration without hardcoded credentials.
- Adds database migrations under `src/main/resources/db/changelog` with rollback blocks.
- Populates the existing `model`, `dto`, `repository`, `service`, `controller`, and `config` packages.
- Adds the `/clients` and `/receivables` API surface and integration tests.
- Updates Docker Compose with a PostgreSQL service for local development.
