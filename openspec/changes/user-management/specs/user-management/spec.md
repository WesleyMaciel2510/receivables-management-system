## Purpose

Provides programmatic user lifecycle endpoints so that users can be created and soft-deactivated through authenticated API calls, without requiring direct database access or schema changes.

## ADDED Requirements

### Requirement: Create User
The system SHALL expose `POST /api/users` to create a new active user with a hashed password.

#### Scenario: Successful creation with valid data
- **WHEN** an authenticated user submits a request body containing a unique username, unique email, non-blank password, firstName, and lastName
- **THEN** the system SHALL hash the password with Argon2id, persist the user with `active = true`, populate `createdAt` and `updatedAt`, and return `201 Created` with a JSON object containing `id`, `username`, `email`, `firstName`, `lastName`, `createdAt`, `updatedAt`, `active`, and `linkedTable`

#### Scenario: Duplicate username
- **WHEN** an authenticated user submits a request body with a username that already exists
- **THEN** the system SHALL return `409 Conflict` with a generic error message and no database constraint details

#### Scenario: Duplicate email
- **WHEN** an authenticated user submits a request body with an email that already exists
- **THEN** the system SHALL return `409 Conflict` with a generic error message and no database constraint details

#### Scenario: Missing required fields
- **WHEN** an authenticated user submits a request body missing `username`, `password`, `email`, `firstName`, or `lastName`
- **THEN** the system SHALL return `400 Bad Request` with validation error details

#### Scenario: Password is never exposed
- **WHEN** a user is created successfully
- **THEN** the response JSON SHALL NOT contain any `password` or password-hash field

### Requirement: Mark User as Deleted (Soft Delete)
The system SHALL expose `PUT /api/users/{id}/mark-as-deleted` to deactivate an existing user without removing the database row.

#### Scenario: Successful deactivation of an active user
- **WHEN** an authenticated user sends the request for a valid, existing user ID whose `active` flag is `true`
- **THEN** the system SHALL set `active = false`, update `updatedAt`, and return `204 No Content`

#### Scenario: User not found
- **WHEN** the requested user ID does not exist
- **THEN** the system SHALL return `404 Not Found`

#### Scenario: Inactive user re-marked as deleted
- **WHEN** the requested user already has `active = false`
- **THEN** the system SHALL still return `204 No Content` (idempotent operation)

#### Scenario: Deactivated user cannot log in
- **WHEN** a previously deactivated user attempts to authenticate via `POST /api/auth/login`
- **THEN** the system SHALL return `401 Unauthorized` with `{ "error": "Invalid credentials" }`

#### Scenario: No fields other than active are modified
- **WHEN** the mark-as-deleted operation completes
- **THEN** `username`, `email`, `password`, `firstName`, `lastName`, `linkedTable`, and `createdAt` SHALL remain unchanged in the database
