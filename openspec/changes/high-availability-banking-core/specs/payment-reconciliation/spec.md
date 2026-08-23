## Purpose

Apply payment events to receivables and account balances safely under concurrency, preserving double-entry accounting and preventing duplicate financial effects.

## ADDED Requirements

### Requirement: Double-entry reconciliation
The reconciliation consumer SHALL debit the debtor account and credit the creditor account for a payment, and SHALL update the linked receivable to `PAID` or `PARTIAL` according to the amount applied.

#### Scenario: Full payment settles a receivable
- **WHEN** a valid payment covers the remaining receivable amount
- **THEN** the system SHALL atomically debit and credit the accounts and mark the receivable `PAID`

#### Scenario: Partial payment updates a receivable
- **WHEN** a valid payment is less than the remaining receivable amount
- **THEN** the system SHALL atomically apply the amount and mark the receivable `PARTIAL`

### Requirement: Concurrency-safe account updates
The system SHALL serialize competing updates for the affected accounts and SHALL prevent negative or otherwise invalid balances caused by race conditions.

#### Scenario: Concurrent payments use one account
- **WHEN** 100 payment events target the same account concurrently
- **THEN** reconciliation SHALL produce no dirty reads, lost updates, or double-spending, and each committed ledger effect SHALL be reflected exactly once

### Requirement: Idempotent event handling
The reconciliation consumer SHALL recognize a previously applied payment event and SHALL not debit, credit, or advance receivable state a second time.

#### Scenario: Duplicate delivery
- **WHEN** the same payment event is delivered more than once
- **THEN** only the first successful delivery SHALL change balances or receivable state and later deliveries SHALL be acknowledged safely
