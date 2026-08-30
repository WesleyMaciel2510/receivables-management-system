## Why

The system requires a secure, minimal, stateless authentication and password recovery feature for the backend. This change implements user login with Argon2id password hashing and short-lived JWTs (30 minutes), as well as a 3-step email verification code password recovery mechanism backed by Resend with primary/secondary API key failover, without introducing database token tables, session storage, or email reset links.

## What Changes

- Create the `users` table via Liquibase changelog matching the exact schema (`id`, `username`, `password`, `email`, `first_name`, `last_name`, `created_at`, `updated_at`, `active`, `linked_table`).
- Implement user login (`POST /api/auth/login`) with username/email resolution, Argon2id verification, and 30-minute stateless JWT generation (HS256).
- Implement 3-step password recovery flow:
  - Step 1: `POST /api/auth/password-recovery` - Generates a secure 6-digit numeric verification code (10m TTL) and recovery challenge JWT (`purpose=password-recovery`), delivering the code via Resend with failover (and dummy tokens for unknown emails to prevent enumeration).
  - Step 2: `POST /api/auth/password-recovery/verify` - Validates code and challenge token (constant-time check), issuing a short-lived `passwordUpdateToken` (`purpose=password-update`).
  - Step 3: `POST /api/auth/password-recovery/password` - Consumes update token and updates user password (Argon2id) returning HTTP 204.
- Configure Spring Security to permit public `/api/auth/**` endpoints, enforce stateless session policy, and protect other endpoints.
- Integrate Resend Java SDK with sequential failover (`RESEND_API_KEY_PRIMARY` -> `RESEND_API_KEY_SECONDARY`) and configurable sender (`RESEND_FROM_EMAIL`).

## Capabilities

### New Capabilities
- `login-auth`: Minimal, secure user authentication with Argon2id and stateless JWT tokens.
- `password-recovery`: 3-step verification code email recovery flow using Resend failover and purpose-bound JWT challenges.

### Modified Capabilities
None.

## Impact

- Adds `db/changelog/` Liquibase script for the `users` table.
- Adds entity, repository, DTOs, services, controllers, and exception handlers under `com.template.auth` (or target application package structure).
- Requires environment variables `RESEND_API_KEY_PRIMARY`, `RESEND_API_KEY_SECONDARY`, `RESEND_FROM_EMAIL`, and `JWT_SECRET`.
