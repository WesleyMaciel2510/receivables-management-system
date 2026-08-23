## Purpose

Provides the first persistent business capabilities for registering Brazilian clients and receivables through validated, auditable-ready REST contracts.

## ADDED Requirements

### Requirement: Client management
The system SHALL allow callers to create, list, retrieve, update, and delete clients using the `/clients` resource.

#### Scenario: Create a valid client
- **WHEN** a caller submits a client with a name, CPF/CNPJ, email, phone, and valid status
- **THEN** the system SHALL persist the client with a generated UUID and creation timestamp and return the created representation

#### Scenario: Reject invalid client data
- **WHEN** a caller submits a client with a missing required field or malformed email/CPF/CNPJ data
- **THEN** the system SHALL return `400 Bad Request` with field-level validation errors and SHALL NOT persist the client

#### Scenario: Retrieve a missing client
- **WHEN** a caller requests `/clients/{id}` for an unknown UUID
- **THEN** the system SHALL return `404 Not Found`

#### Scenario: Delete a client
- **WHEN** a caller deletes an existing client that has no dependent receivables
- **THEN** the system SHALL remove the client and return a successful empty response

### Requirement: Receivable management
The system SHALL allow callers to create, list, retrieve, update, change status, and delete receivables using the `/receivables` resource.

#### Scenario: Create a pending receivable
- **WHEN** a caller submits a client ID, description, positive amount, and due date for an existing client
- **THEN** the system SHALL persist a receivable with status `PENDING`, original amount equal to the submitted amount, remaining balance equal to the original amount, and a generated ID and creation timestamp

#### Scenario: Reject a receivable for an unknown client
- **WHEN** a caller submits a receivable referencing an unknown client ID
- **THEN** the system SHALL return `404 Not Found` and SHALL NOT persist the receivable

#### Scenario: Reject invalid receivable values
- **WHEN** a caller submits a non-positive amount, a missing description, or an invalid due date
- **THEN** the system SHALL return `400 Bad Request` with validation details and SHALL NOT persist the receivable

#### Scenario: Update a receivable
- **WHEN** a caller updates an existing receivable with valid editable fields
- **THEN** the system SHALL persist the changes without changing its ID, creation timestamp, or remaining-balance invariant

#### Scenario: Change receivable status
- **WHEN** a caller requests `/receivables/{id}/status` with a supported lifecycle status
- **THEN** the system SHALL persist the status change only when it does not violate the receivable lifecycle rules and SHALL return the updated representation

#### Scenario: Reject an invalid status transition
- **WHEN** a caller requests a status change that violates lifecycle rules or references an unknown receivable
- **THEN** the system SHALL return an appropriate client error (`400 Bad Request` or `404 Not Found`) and SHALL leave the receivable unchanged

#### Scenario: Delete a receivable
- **WHEN** a caller deletes an existing receivable
- **THEN** the system SHALL remove it and return a successful empty response

### Requirement: Persistence and database migrations
The system SHALL persist clients and receivables in PostgreSQL and SHALL create or update their schema through versioned Liquibase migrations.

#### Scenario: Start with an empty database
- **WHEN** the application starts against an empty configured PostgreSQL database
- **THEN** Liquibase SHALL create the clients and receivables tables, their UUID primary keys, required constraints, and the client-to-receivable relationship before the application accepts requests

#### Scenario: Preserve referential integrity
- **WHEN** a receivable references a client
- **THEN** the database SHALL enforce that the referenced client exists and SHALL prevent deletion of a client with dependent receivables unless an explicit supported policy is applied

### Requirement: Externalized database configuration
Database connection settings SHALL be supplied through profile configuration or environment variables and SHALL not contain credentials committed to source control.

#### Scenario: Local profile database configuration
- **WHEN** the application starts with the local profile and the Compose database is available
- **THEN** the application SHALL connect using the configured externalized URL, username, and password values and SHALL complete startup successfully

#### Scenario: Missing database configuration
- **WHEN** required database configuration is absent or the database is unavailable
- **THEN** the application SHALL fail startup clearly rather than silently using an embedded or unconfigured database
