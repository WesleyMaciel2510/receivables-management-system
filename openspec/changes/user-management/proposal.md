## Why

The application has a `User` entity and authentication endpoints but is missing the ability to create new users programmatically or deactivate existing ones. Without these endpoints, the bootstrap account creation flow cannot be replaced by a proper admin API, and there is no way to soft-delete users beyond manual database edits.

## What Changes

- Add `POST /api/users` to create a new active user, hashing the password with Argon2id and rejecting duplicate usernames or emails with `409 Conflict`.
- Add `PUT /api/users/{id}/mark-as-deleted` to soft-delete an existing user by setting `active = false`, returning `204 No Content`.
- Add `UserResponse` DTO that omits the password hash from serialized output.
- Secure both endpoints with the existing JWT authentication mechanism; no new auth routes are added.
- No database schema changes — the existing `users` table and its columns are used as-is.

## Capabilities

### New Capabilities

- `user-management`: Programmatic user creation (POST `/api/users`) and soft deletion via `active` flag (PUT `/api/users/{id}/mark-as-deleted`).

### Modified Capabilities

None.

## Impact

- New controller: `src/main/java/com/rms/controller/UserController.java`
- New service: `src/main/java/com/rms/service/UserService.java`
- New DTOs: `src/main/java/com/rms/dto/CreateUserRequest.java`, `src/main/java/com/rms/dto/UserResponse.java`
- Reuses existing: `User.java`, `UserRepository.java`, `Argon2PasswordEncoder`, `JwtAuthenticationFilter`, `SecurityConfig`
- No Liquibase migration; no new tables or columns
- Existing login and password-recovery endpoints remain unchanged and continue to enforce `active = true` via Spring Security's `isEnabled()` check
