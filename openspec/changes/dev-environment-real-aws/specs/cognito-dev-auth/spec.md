## Purpose

Defines real Cognito authentication for the development profile while keeping local authentication independent, allowing token behavior to be learned without disrupting offline development.

## ADDED Requirements

### Requirement: Dev Cognito resources support API-only testing
The real-AWS development environment MUST provide a Cognito user pool named `rms-dev-users` and one app client without a client secret, suitable for Postman or command-line authentication tests.

#### Scenario: Test client authenticates without a secret
- **WHEN** a test client submits valid credentials to the configured Cognito authentication flow
- **THEN** it can obtain tokens without requiring a confidential client secret

### Requirement: Dev validates Cognito-issued JWTs
The application MUST validate Cognito-issued access tokens for protected requests under `dev`, using the configured user-pool issuer and public keys, and MUST reject invalid, expired, wrong-issuer, or wrong-audience tokens.

#### Scenario: Valid Cognito request
- **WHEN** a protected `dev` request presents a valid access token issued by `rms-dev-users`
- **THEN** the request is authenticated and proceeds according to the existing authorization rules

#### Scenario: Invalid Cognito request
- **WHEN** a protected `dev` request presents an invalid, expired, or incorrectly issued token
- **THEN** the application rejects the request as unauthenticated

### Requirement: Local authentication remains custom
The application MUST retain the existing custom JWT authentication behavior under `local` and MUST NOT require access to Cognito for local startup or local protected requests.

#### Scenario: Local auth without AWS
- **WHEN** the application runs under `local` without real AWS credentials
- **THEN** its existing custom JWT flow remains available and Cognito is not contacted

