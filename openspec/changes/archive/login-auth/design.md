## Context

This feature implements a complete, minimal, production-grade authentication and password recovery system based on the strict constraints defined in `.agent/tasks/0001/login.md`.

## Architecture & Data Model

### Database Table: `users`
- `id` (BIGSERIAL PRIMARY KEY)
- `username` (VARCHAR(255) UNIQUE NOT NULL)
- `password` (VARCHAR(255) NOT NULL, Argon2id hash)
- `email` (VARCHAR(255) UNIQUE NOT NULL)
- `first_name` (VARCHAR(255) NOT NULL)
- `last_name` (VARCHAR(255) NOT NULL)
- `created_at` (TIMESTAMPTZ NOT NULL DEFAULT now())
- `updated_at` (TIMESTAMPTZ NOT NULL DEFAULT now()) - updated via `@PreUpdate`
- `active` (BOOLEAN NOT NULL DEFAULT true)
- `linked_table` (VARCHAR(255) NULL)

No token tables, session tables, or password recovery tables are created.

### Security Architecture & JWTs

1. **Login Token**:
   - Algorithm: HS256 (signed with `JWT_SECRET`)
   - Expiration: Exactly 30 minutes
   - Claims: `sub` (user ID), `iat`, `exp`
   - Purpose: Stateless API access.

2. **Recovery Challenge JWT**:
   - Expiration: Exactly 10 minutes
   - Claims: `sub` (user ID), `purpose` = `password-recovery`, `code_hash` (HMAC/hash of 6-digit code), `iat`, `exp`
   - Unknown emails receive syntactically valid dummy challenge tokens to prevent user enumeration.

3. **Password-Update Authorization JWT**:
   - Expiration: Exactly 10 minutes
   - Claims: `sub` (user ID), `purpose` = `password-update`, `iat`, `exp`
   - Only accepted by `POST /api/auth/password-recovery/password`.

### Resend Email Failover
- SDK: Official Resend Java SDK (`com.resend.*`).
- Flow: Sequential attempt with `RESEND_API_KEY_PRIMARY`, if failed attempt with `RESEND_API_KEY_SECONDARY`.
- Resend errors are logged internally; HTTP endpoint always returns HTTP 200 generic message.

### Endpoints
- `POST /api/auth/login`: `{ "usernameOrEmail", "password" }` -> `200 { "token" }` or `401 { "error": "Invalid credentials" }`
- `POST /api/auth/password-recovery`: `{ "email" }` -> `200 { "message", "challengeToken" }`
- `POST /api/auth/password-recovery/verify`: `{ "challengeToken", "code" }` -> `200 { "passwordUpdateToken" }` or `401 { "error": "Invalid verification code" }`
- `POST /api/auth/password-recovery/password`: `{ "passwordUpdateToken", "newPassword" }` -> `204 No Content` or `401 { "error": "Invalid password update token" }`
