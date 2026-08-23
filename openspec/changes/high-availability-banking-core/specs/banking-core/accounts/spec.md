## Purpose

Provide a consistent multi-bank account model that supports client account management, ledger balances, and enforceable account-type and fee rules.

## ADDED Requirements

### Requirement: Bank directory
The system SHALL maintain banks with a unique three-digit COMPE code, ISPB code, and display name, and SHALL expose the available banks through `GET /api/v1/banks`.

#### Scenario: List seeded banks
- **WHEN** an authorized client requests `GET /api/v1/banks`
- **THEN** the response SHALL include the configured standard Brazilian banks with their codes and names

### Requirement: Client bank account creation
The system SHALL allow an authorized client to create a checking, payment, savings, escrow, or settlement account for a selected bank through `POST /api/v1/clients/{id}/accounts`.

#### Scenario: Create a valid account
- **WHEN** the client exists, is eligible, the bank exists, and the account type is not already present for that client and bank
- **THEN** the system SHALL create the account with a unique identifier, zero initial ledger balance, and the requested account metadata

#### Scenario: Reject duplicate account type
- **WHEN** a client attempts to create a second account with the same bank and account type
- **THEN** the system SHALL reject the request with HTTP `409 Conflict` and SHALL not create another account

### Requirement: Account maintenance fee rules
The system SHALL set a payment account's monthly maintenance fee to exactly `0.00` and SHALL permit a configured fee for checking accounts; unsupported fee values SHALL be rejected rather than silently altered.

#### Scenario: Payment account fee is zero
- **WHEN** a payment account is created with a non-zero requested maintenance fee
- **THEN** the system SHALL persist the account with a fee of `0.00` or reject the request according to the API contract, but SHALL never persist a non-zero fee for that account type

### Requirement: Account listing
The system SHALL expose all accounts belonging to a client through `GET /api/v1/clients/{id}/accounts` and SHALL not include accounts belonging to another client.

#### Scenario: List a client's accounts
- **WHEN** an authorized client requests the account list for an existing client
- **THEN** the response SHALL contain that client's accounts, including bank, account type, balance, and maintenance fee
