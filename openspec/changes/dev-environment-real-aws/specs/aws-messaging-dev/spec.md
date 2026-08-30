## Purpose

Defines a bounded real-AWS messaging path for development that demonstrates queue delivery, dead-letter handling, and SNS fan-out while preserving predictable local emulation.

## ADDED Requirements

### Requirement: Dev event queue has dead-letter protection
The real-AWS development environment MUST provide a standard queue named `rms-dev-events`, a matching dead-letter queue named `rms-dev-events-dlq`, and a redrive policy that moves a message after five receive attempts.

#### Scenario: Message exceeds receive attempts
- **WHEN** a message from `rms-dev-events` is received unsuccessfully five times
- **THEN** the message is made available in `rms-dev-events-dlq` according to the queue redrive policy

### Requirement: Application publishes and consumes dev events
The application MUST publish its supported events to the configured `rms-dev-events` queue and MUST consume messages from that queue when running under `dev`.

#### Scenario: Dev event round trip
- **WHEN** a supported event is published under the `dev` profile
- **THEN** a dev consumer can receive the event from `rms-dev-events` and process it using the same event contract as local development

### Requirement: SNS notifications fan out through SQS
The real-AWS development environment MUST provide an SNS topic named `rms-dev-notifications` with the dev event queue subscribed as an SQS endpoint.

#### Scenario: Topic publish reaches queue
- **WHEN** a message is published to `rms-dev-notifications`
- **THEN** the subscribed SQS queue receives the SNS-delivered message

### Requirement: Messaging behavior is observable and bounded
Development verification MUST cover visibility timeout, long polling, at-least-once delivery/idempotency, DLQ redrive, and SNS subscription filtering without generating unbounded requests.

#### Scenario: Failed smoke test is contained
- **WHEN** a dev messaging smoke test fails or is interrupted
- **THEN** it exits with bounded polling and does not continuously publish or receive messages

