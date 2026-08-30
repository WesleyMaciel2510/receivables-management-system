## Tasks

- [x] 1. Database Schema & Migration <!-- id: 0 -->
  - [x] 1.1 Create Liquibase changelog for `users` table with exact columns (`id`, `username`, `password`, `email`, `first_name`, `last_name`, `created_at`, `updated_at`, `active`, `linked_table`) <!-- id: 1 -->

- [x] 2. Domain & Persistence <!-- id: 2 -->
  - [x] 2.1 Create `User` entity with `@PreUpdate` timestamp management and exact column mapping <!-- id: 3 -->
  - [x] 2.2 Create `UserRepository` with query methods for `usernameOrEmail` and `email` lookup <!-- id: 4 -->

- [x] 3. Security Configuration & Core Infrastructure <!-- id: 5 -->
  - [x] 3.1 Configure `Argon2PasswordEncoder` bean <!-- id: 6 -->
  - [x] 3.2 Configure stateless `SecurityFilterChain` permitting `/api/auth/**` and securing other paths <!-- id: 7 -->
  - [x] 3.3 Create `GlobalExceptionHandler` returning generic error structures (401, etc.) <!-- id: 8 -->

- [x] 4. JWT & Authentication Service <!-- id: 9 -->
  - [x] 4.1 Implement `JwtService` for login JWT issuing (30 min TTL, HS256, claims `sub`, `iat`, `exp`) and validation filter <!-- id: 10 -->
  - [x] 4.2 Implement `AuthService` and `LoginRequest`/`LoginResponse` DTOs with constant error response on failure <!-- id: 11 -->
  - [x] 4.3 Create `AuthController` exposing `POST /api/auth/login` <!-- id: 12 -->

- [x] 5. Resend Email Integration with Failover <!-- id: 13 -->
  - [x] 5.1 Configure `ResendConfig` reading `RESEND_API_KEY_PRIMARY`, `RESEND_API_KEY_SECONDARY`, `RESEND_FROM_EMAIL` <!-- id: 14 -->
  - [x] 5.2 Implement `EmailService` interface <!-- id: 15 -->
  - [x] 5.3 Implement `ResendEmailService` with sequential primary-to-secondary failover and idempotency <!-- id: 16 -->

- [x] 6. Password Recovery Implementation <!-- id: 17 -->
  - [x] 6.1 Implement `PasswordRecoveryTokenService` for recovery challenge JWT (10 min, `purpose=password-recovery`, code HMAC) and password update JWT (10 min, `purpose=password-update`) <!-- id: 18 -->
  - [x] 6.2 Implement `PasswordRecoveryService` (step 1 request code, step 2 verify code with constant-time check, step 3 update password) <!-- id: 19 -->
  - [x] 6.3 Implement DTOs: `PasswordRecoveryRequest`, `PasswordRecoveryResponse`, `VerifyRecoveryCodeRequest`, `VerifyRecoveryCodeResponse`, `UpdatePasswordRequest` <!-- id: 20 -->
  - [x] 6.4 Implement recovery endpoints in `AuthController` or `PasswordRecoveryController` <!-- id: 21 -->

