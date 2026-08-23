## 1. Database Foundation

- [ ] 1.1 Add PostgreSQL, Liquibase, Spring Data JPA, and Jakarta Bean Validation dependencies while preserving the existing Spring Boot build; verify dependency resolution and compilation succeed
- [ ] 1.2 Add externalized database settings for local, dev, and prod profiles, update Docker Compose with a pinned PostgreSQL service on the application network, and create the first Liquibase changelog for `clients` and `receivables` with UUID keys, constraints, timestamps, rollback blocks, and a restrictive foreign key; verify `docker compose config` has no committed credentials and the migration applies to an empty PostgreSQL volume

## 2. Domain and Persistence

- [ ] 2.1 Create Client and Receivable entities with explicit UUID, enum, date, relationship, and `BigDecimal` mappings plus the required domain status enums; verify the entities compile and JPA startup validates the mappings
- [ ] 2.2 Create Client and Receivable repository interfaces plus a shared API error model and exception handler for validation failures, missing resources, conflicts, and invalid lifecycle transitions; verify repository beans start against PostgreSQL and representative errors return structured JSON with stable HTTP statuses

## 3. Client API

- [ ] 3.1 Create Client request/response DTOs with required-field, email, CPF/CNPJ, and field-length validation plus normalization rules; verify invalid payload tests reject data before persistence
- [ ] 3.2 Implement Client service operations for create, list, retrieve, update, and delete, including UUID lookup behavior and dependent-receivable deletion conflict handling; verify service tests cover success, missing, and conflict cases
- [ ] 3.3 Implement the `/clients` REST controller with POST, GET collection, GET by ID, PUT, and DELETE endpoints; verify MockMvc or equivalent integration tests cover the complete client CRUD contract

## 4. Receivable API and Rules

- [ ] 4.1 Create Receivable request/response DTOs for client ID, description, amount, due date, status, and balance fields with positive monetary and date validation; verify invalid payload tests return field-level errors
- [ ] 4.2 Implement Receivable service operations for create, list, retrieve, update, delete, and status changes, enforcing client existence, PENDING initialization, balance bounds, and supported lifecycle transitions; verify unit tests cover pending, overdue, cancelled, and invalid-transition cases
- [ ] 4.3 Implement the `/receivables` REST controller with CRUD endpoints and `PATCH /receivables/{id}/status`; verify integration tests cover unknown clients, unknown receivables, valid updates, status changes, and deletion

## 5. End-to-End Verification

- [ ] 5.1 Add PostgreSQL-backed integration tests for Liquibase startup, client/receivable persistence, referential integrity, and the complete API workflow; verify `mvn test` passes with the Compose database running
- [ ] 5.2 Review all changed configuration, migrations, Docker files, and source for hardcoded credentials and migration hazards; verify with credential-pattern search, `openspec validate`, and `docker compose config`
