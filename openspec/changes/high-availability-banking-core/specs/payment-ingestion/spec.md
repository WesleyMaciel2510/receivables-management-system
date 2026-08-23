## Purpose

Accept payment requests quickly while handing durable processing to independently consumable reconciliation and analytics workflows.

## ADDED Requirements

### Requirement: Asynchronous payment acceptance
The system SHALL validate the basic payment payload, assign or preserve an idempotency identifier, publish a payment-received event to the payment fanout, and return HTTP `202 Accepted` without waiting for ledger reconciliation.

#### Scenario: Payment is accepted for processing
- **WHEN** a syntactically valid payment is submitted to `POST /api/v1/payments`
- **THEN** the system SHALL publish a payment-received event and return `202 Accepted` within 30 milliseconds under the defined local-service performance test

#### Scenario: Invalid payment is rejected synchronously
- **WHEN** the payment payload is missing required fields or contains invalid amounts or account references
- **THEN** the system SHALL return a validation error and SHALL not publish a payment-received event

### Requirement: Independent FIFO fanout
Each accepted payment event SHALL be delivered independently to reconciliation and analytics consumers through FIFO queues while preserving the configured message-group ordering.

#### Scenario: Reconciliation and analytics both receive an event
- **WHEN** a payment-received event is published successfully
- **THEN** the reconciliation queue and analytics queue SHALL each receive a corresponding event without one consumer's availability determining the other's delivery

### Requirement: Payment event durability
The system SHALL treat event publication failure as an acceptance failure and SHALL expose a retry-safe response path without reporting a payment as accepted when no durable event was published.

#### Scenario: Fanout publication fails
- **WHEN** the payment event cannot be durably published
- **THEN** the system SHALL return a non-202 error and SHALL provide enough event identity for a safe client retry
