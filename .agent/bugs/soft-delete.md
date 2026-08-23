Role & Task:
You are a Senior Banking Software Engineer. Replace hard physical deletes across all entities in the project with audit-compliant Soft Deletes using Spring Data JPA and Hibernate annotations.

Fix Requirements:

1. Database Schema (Liquibase):
    - Add a Liquibase migration file to append a `deleted_at` timestamp column (nullable, TIMESTAMP WITH TIME ZONE) to all primary entity tables (`clients`, `receivables`, `payments`, etc.).
    - DO NOT remove or alter existing primary keys or historical audit tables.

2. Entity Mapping Modifications:
    - Add `@Column(name = "deleted_at") private Instant deletedAt;` property to base entities/classes.
    - Annotate entities with Hibernate annotations for automatic filtering:
        - `@SQLDelete(sql = "UPDATE <table_name> SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")`
        - `@SQLRestriction("deleted_at IS NULL")` (or `@Where(clause = "deleted_at IS NULL")` depending on your Hibernate version)
    - Ensure soft-deleted records remain immutable in physical storage for auditing purposes.

3. Controller & API Refactoring:
    - Update `DELETE` HTTP endpoints for `Clients` and `Receivables` so they trigger soft-delete behavior through JPA entity deletion or an explicit soft-delete service call.
    - Return appropriate HTTP responses (e.g., `204 No Content`) while verifying that subsequent `GET` requests omit soft-deleted records by default.
    - Optionally add an administrative audit lookup endpoint (e.g., `GET /api/v1/clients/archived`) to fetch records where `deleted_at IS NOT NULL`.

Output Required:

- Updated Java JPA Entities and Controllers showing the updated annotations and logic.
- Summary of modified files.