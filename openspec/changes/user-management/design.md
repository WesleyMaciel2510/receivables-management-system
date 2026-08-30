## Context

The `User` entity (`com.rms.model.User`) already exists with all required columns (`id`, `username`, `password`, `email`, `first_name`, `last_name`, `created_at`, `updated_at`, `active`, `linked_table`). The `UserRepository` provides `findById`, `findByUsername`, and `findByEmail`. Spring Security's `DaoAuthenticationProvider` already uses the `UserDetailsService` implementation (`AppUserDetailsService`) which delegates to the repository; Spring Security's built-in `isEnabled()` check already rejects inactive users at login time. The `Argon2PasswordEncoder` bean is configured in `SecurityConfig`. A `ConflictException` and `ResourceNotFoundException` already exist and are handled by `ApiExceptionHandler`.

## Goals / Non-Goals

**Goals:**
- Add a public-facing authenticated endpoint for user creation with full validation.
- Add a public-facing authenticated endpoint for soft-deleting (deactivating) a user.
- Ensure both endpoints follow the project's existing controller/service/DTO conventions.
- Never serialize or log password hashes.

**Non-Goals:**
- No new database columns, Liquibase migrations, or schema changes.
- No role-based access control on these endpoints beyond existing JWT authentication.
- No user listing, updating, or hard-deletion endpoints.
- No changes to `SecurityConfig` filter chain rules — only new protected paths.
- No email notifications on user creation or deletion.
- No audit-log table for user lifecycle events.

## Decisions

### D1: Package layout — reuse `dto/` and add `controller/UserController` + `service/UserService`

**Decision:** Place new DTOs in the existing `com.rms.dto` package, the controller in `com.rms.controller`, and the service in `com.rms.service`.

**Rationale:** Consistent with every other domain in the project (e.g., `ClientController`/`ClientService`/`ClientRequest`/`ClientResponse`). No need for a new package.

**Alternatives considered:** A dedicated `com.rms.user.*` package. Rejected — over-engineers a small addition and diverges from the project's flat-layer convention.

### D2: Use `record` DTOs matching the project's existing pattern

**Decision:** `CreateUserRequest` and `UserResponse` are both Java records with Jakarta Validation annotations on the request.

**Rationale:** Mirrors `LoginRequest`, `ClientRequest`, and all other DTOs in the project. Records are immutable and self-documenting.

**Alternatives considered:** Lombok `@Data` classes. Rejected — the project does not use Lombok; records are the established convention.

### D3: Password hashing via injected `PasswordEncoder` in `UserService`

**Decision:** `UserService` receives `PasswordEncoder` through constructor injection and calls `encode()` before persisting.

**Rationale:** Follows the same pattern used in the bootstrap seed logic in `SecurityConfig`. Avoids duplicating encoder configuration.

**Alternatives considered:** Hashing in the controller. Rejected — controllers should not contain business logic; hashing is part of user creation semantics.

### D4: Soft delete via `active` flag only, no new column

**Decision:** `markAsDeleted` performs a simple `user.setActive(false)` + `repository.save(user)`.

**Rationale:** The `users` table already has an `active` column. The task explicitly forbids adding columns. Spring Security's `isEnabled()` (mapped to `active`) already blocks login for inactive users.

**Alternatives considered:** A `deleted_at` column. Rejected — would require a Liquibase migration and violates the no-schema-change constraint.

### D5: Duplicate-key errors surfaced as `409 Conflict` via `ConflictException`

**Decision:** Catch `DataIntegrityViolationException` (or check existence beforehand) and re-throw as `ConflictException`, which `ApiExceptionHandler` maps to HTTP 409.

**Rationale:** `ConflictException` already exists and is handled. Pre-checking (`existsByUsername`, `existsByEmail`) avoids relying on exception-driven flow control and gives clearer error messages. The repository will need two additional boolean methods.

**Alternatives considered:** Catching `DataIntegrityViolationException`. Rejected — coupling to JPA/PostgreSQL constraint names leaks implementation details. Pre-checks are cleaner and faster for this case.

## Risks / Trade-offs

- **Race condition on duplicate username/email** → Pre-check (`existsByUsername`) followed by insert is a classic TOCTOU race. Low risk in practice for an internal admin API; acceptable. If strict serializability is required later, a unique-constraint exception handler is a fallback.
- **No authorization check on who can create/delete users** → Both endpoints only require a valid JWT; any authenticated role can invoke them. This matches the existing `/clients` and `/receivables` pattern. Admin-only restriction is deferred (non-goal per proposal).
- **`updated_at` update on no-op soft-delete** → When the user is already inactive, the `@PreUpdate` hook still fires and updates `updatedAt`. This is harmless but may surprise observers. Acceptable.

## Migration Plan

No migration required. The change is additive only:
1. Add new source files (controller, service, DTOs).
2. Run the application — no Liquibase change needed.
3. Existing tests continue to pass; no existing behavior is modified.
4. Rollback: delete the new files; no data is affected.

## Open Questions

None. All design decisions are resolved.
