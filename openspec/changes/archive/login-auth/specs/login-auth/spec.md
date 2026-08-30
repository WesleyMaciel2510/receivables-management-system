## Purpose

Defines the requirements for stateless user authentication using Argon2id password hashing and short-lived JWTs, alongside a secure 3-step verification code password recovery flow using Resend email delivery.

## ADDED Requirements

### Requirement: User Authentication & Login
The system SHALL expose `POST /api/auth/login` to authenticate users by username or email and return a signed JWT access token valid for 30 minutes.

#### Scenario: Successful login
- **WHEN** user submits valid credentials for an active account
- **THEN** system SHALL return `200 OK` with `{ "token": "<jwt>" }`

#### Scenario: Invalid login or inactive account
- **WHEN** user submits incorrect credentials or belongs to an inactive account
- **THEN** system SHALL return `401 Unauthorized` with `{ "error": "Invalid credentials" }` without disclosing failure reason

### Requirement: Request Password Recovery Code
The system SHALL expose `POST /api/auth/password-recovery` to generate a 6-digit numeric verification code (10m TTL) and deliver it via email using Resend failover.

#### Scenario: Real active user
- **WHEN** user submits a registered active email
- **THEN** system SHALL send 6-digit code via Resend and return `200 OK` with a signed recovery challenge JWT (`purpose=password-recovery`)

#### Scenario: Unknown or inactive email
- **WHEN** user submits an unregistered or inactive email
- **THEN** system SHALL return `200 OK` with a dummy challenge JWT to prevent user enumeration without sending an email

### Requirement: Verify Recovery Code
The system SHALL expose `POST /api/auth/password-recovery/verify` to validate the 6-digit code and challenge token.

#### Scenario: Valid verification code
- **WHEN** user submits the correct 6-digit code and valid challenge token
- **THEN** system SHALL return `200 OK` with `{ "passwordUpdateToken": "<jwt>" }` (`purpose=password-update`, 10m TTL)

#### Scenario: Invalid or expired code/token
- **WHEN** user submits an invalid code, expired token, or mismatched purpose
- **THEN** system SHALL return `401 Unauthorized` with `{ "error": "Invalid verification code" }`

### Requirement: Update Password
The system SHALL expose `POST /api/auth/password-recovery/password` to reset the user password with a valid update token.

#### Scenario: Successful password reset
- **WHEN** user provides a valid update token and new password
- **THEN** system SHALL hash the new password with Argon2id, update `updated_at`, and return `204 No Content`
