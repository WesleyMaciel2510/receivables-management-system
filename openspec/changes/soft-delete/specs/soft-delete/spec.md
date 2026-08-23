## Purpose

Preserve deleted banking records for audit and historical traceability while keeping ordinary application reads focused on active records.

## ADDED Requirements

### Requirement: Domain records are deleted logically
The system SHALL represent deletion of primary domain records by recording the deletion timestamp and SHALL retain the original row, primary key, and historical audit data.

#### Scenario: Client or receivable is deleted
- **WHEN** an authorized caller deletes an existing client or receivable
- **THEN** the record remains physically stored with a non-null `deleted_at` timestamp and the operation returns success without deleting the row

#### Scenario: Delete an already deleted record
- **WHEN** a delete request targets a record that is already soft-deleted
- **THEN** the system SHALL NOT physically delete or recreate the record and SHALL return the API's established not-found or idempotent-success behavior

### Requirement: Active reads exclude soft-deleted records
The system SHALL omit records whose `deleted_at` value is non-null from default repository queries and ordinary collection or detail `GET` endpoints.

#### Scenario: Collection read after deletion
- **WHEN** a client requests the normal clients or receivables collection after a record has been soft-deleted
- **THEN** the deleted record is absent from the response

#### Scenario: Detail read after deletion
- **WHEN** a client requests a normal detail endpoint for a soft-deleted record
- **THEN** the system responds as if the record is unavailable according to the API's existing not-found contract

### Requirement: Soft deletion applies across primary domain tables
The system SHALL support the same nullable deletion timestamp semantics for all primary domain entity tables, including clients, receivables, payments, banks, bank accounts, payment effects, and application users where those entities are managed by JPA.

#### Scenario: Migration adds deletion state
- **WHEN** the database migration is applied
- **THEN** each applicable primary table has a nullable time-zone-aware `deleted_at` column and existing rows remain active

#### Scenario: Existing audit structures remain intact
- **WHEN** the migration is applied
- **THEN** existing primary keys and historical audit tables are unchanged

### Requirement: Administrators can inspect archived records
The system SHALL provide an authorized administrative read path for records with a non-null `deleted_at` value without changing the default active-record read behavior.

#### Scenario: Archived lookup
- **WHEN** an authorized administrator requests archived clients or receivables
- **THEN** the response contains matching soft-deleted records and their deletion timestamps

#### Scenario: Unauthorized archived lookup
- **WHEN** a caller without the required administrative permission requests archived records
- **THEN** the system denies the request using the established authorization response
