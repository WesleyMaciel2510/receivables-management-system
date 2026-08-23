## Purpose

Provides a stateless authentication and authorization boundary so only callers with valid credentials and signed access tokens can use protected API resources.

## ADDED Requirements

### Requirement: Authenticate users and issue access tokens

The system SHALL expose `POST /api/auth/login` to verify a user's credentials and return a signed JWT access token with its token type, expiration information, issuer, subject, and applicable authorities.

#### Scenario: Successful login
- **WHEN** a caller submits valid username and password credentials
- **THEN** the system SHALL return `200 OK` with a Bearer-compatible access token and expiration metadata

#### Scenario: Invalid login
- **WHEN** a caller submits an unknown username or an incorrect password
- **THEN** the system SHALL return `401 Unauthorized` without revealing which credential was invalid

### Requirement: Validate Bearer access tokens

The system SHALL accept a JWT from the `Authorization: Bearer <token>` header, validate its signature, issuer, subject, and expiration, and establish the authenticated identity and authorities for the request.

#### Scenario: Valid access token
- **WHEN** a caller sends a correctly signed, unexpired token issued by the configured issuer
- **THEN** the system SHALL authenticate the request and make the token subject and authorities available to authorization rules

#### Scenario: Invalid or expired access token
- **WHEN** a caller sends a malformed, incorrectly signed, expired, or wrong-issuer token
- **THEN** the system SHALL reject the request with `401 Unauthorized` and SHALL not establish an authenticated security context

### Requirement: Protect business endpoints with stateless authorization

The system SHALL require a valid Bearer token for `/clients/**` and `/receivables/**`, SHALL allow `/api/auth/**` without authentication, and SHALL not create server-side sessions for API authentication.

#### Scenario: Unauthenticated protected request
- **WHEN** a caller requests a client or receivable endpoint without a valid access token
- **THEN** the system SHALL return `401 Unauthorized` with a stable JSON error response

#### Scenario: Authenticated protected request
- **WHEN** a caller requests a client or receivable endpoint with a valid access token
- **THEN** the system SHALL evaluate the request using the authenticated principal and continue to the existing business API

#### Scenario: Public login request
- **WHEN** a caller requests an authentication endpoint without an access token
- **THEN** the system SHALL process the authentication request rather than reject it as unauthorized

### Requirement: Handle authorization failures consistently

The system SHALL return `403 Forbidden` with a stable JSON error response when an authenticated principal lacks the authority required by a protected operation, and SHALL apply stateless REST security behavior without CSRF session protection.

#### Scenario: Insufficient authority
- **WHEN** an authenticated caller attempts an operation requiring an authority not present in the token
- **THEN** the system SHALL return `403 Forbidden` and SHALL not execute the protected operation

#### Scenario: Browser preflight
- **WHEN** a browser sends an allowed CORS preflight request for an API endpoint
- **THEN** the system SHALL respond according to the configured CORS policy without requiring a JWT on the preflight request
