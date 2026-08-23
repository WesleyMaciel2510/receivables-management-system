## 1. Database Migration

- [x] 1.1 Inventory every primary JPA-backed table and existing changelog/master ordering, then define the target table list without touching primary keys or audit tables; verify the inventory matches the entity mappings and current Liquibase changelog
- [ ] 1.2 Add `src/main/resources/db/changelog/005-soft-delete.yaml` (or the next project-consistent version) with nullable `TIMESTAMP WITH TIME ZONE` `deleted_at` columns for every applicable primary table; verify the changelog parses and applies to a clean database
- [ ] 1.3 Include the new migration in `db.changelog-master.yaml` after existing domain migrations; verify application startup recognizes the migration and existing rows retain `deleted_at = NULL`

## 2. Persistence Model

- [x] 2.1 Add the nullable `Instant deletedAt` mapping to the shared entity base or each applicable entity (`Client`, `Receivable`, `Payment`, `Bank`, `BankAccount`, `PaymentEffect`, `AppUser`, and any other managed primary entity); verify the application context starts against the migrated schema
- [x] 2.2 Configure each applicable entity with Hibernate soft-delete SQL using `UPDATE <table> SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?` and active-row restriction using the Hibernate version supported by the project; verify repository delete tests show an update and no physical row removal
- [x] 2.3 Inventory bulk/native delete operations and relationship cascades, replacing paths that bypass entity soft deletion; verify no in-scope delete path issues physical `DELETE` SQL

## 3. Active and Archived Data Access

- [x] 3.1 Ensure default repositories and service queries exclude non-null `deleted_at` records, including client and receivable detail/list reads; verify active collection and detail tests omit archived rows
- [x] 3.2 Add explicit archived queries for clients and receivables that select only `deleted_at IS NOT NULL` without weakening default restrictions; verify archived results include deletion timestamps and active results remain unchanged
- [x] 3.3 Protect archived lookup methods with the projectâ€™s existing administrative authorization convention; verify unauthorized requests receive the established authorization failure response

## 4. API Behavior

- [x] 4.1 Refactor `ClientController` and `ReceivableController` `DELETE /{id}` flows to use the soft-delete service/repository behavior and return `204 No Content` on successful deletion; verify endpoint integration tests retain the row with a timestamp
- [x] 4.2 Add administrative archived endpoints using the projectâ€™s established route and response conventions; verify authorized callers can retrieve archived clients/receivables while ordinary `GET` endpoints treat them as unavailable
- [x] 4.3 Define and test repeated-delete behavior for already archived records according to the existing not-found or idempotent-success contract; verify no timestamp reset or physical deletion occurs

## 5. Verification

- [ ] 5.1 Add repository, controller, and migration integration coverage for active filtering, soft deletion, archived lookup, authorization, primary-key preservation, and audit-table preservation; verify the focused test suite passes
- [ ] 5.2 Run the full project validation/build and inspect generated SQL or database state for the absence of physical deletes in covered flows; verify all OpenSpec requirements have passing evidence
