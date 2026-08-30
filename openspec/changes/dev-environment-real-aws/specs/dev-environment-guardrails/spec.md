## Purpose

Constrains the real-AWS learning environment so credentials, permissions, region, naming, and spending risks remain isolated from local development and unrelated AWS resources.

## ADDED Requirements

### Requirement: Dev resources are isolated and named consistently
All resources created for this learning environment MUST be in `us-east-1` and MUST use the `rms-dev-` naming prefix where the service supports resource names.

#### Scenario: Resource inventory is reviewed
- **WHEN** the dev environment inventory is inspected
- **THEN** its SQS, SNS, and Cognito resources are identifiable by the `rms-dev-` prefix and are located in `us-east-1`

### Requirement: Access is least privilege and non-root
Dev access MUST use a dedicated non-root project identity with service-specific least-privilege permissions for the required SQS, SNS, and Cognito operations.

#### Scenario: Unauthorized service access is attempted
- **WHEN** the dev identity attempts an operation outside the declared service permissions
- **THEN** AWS denies the operation

### Requirement: Credentials and spending guardrails are protected
Credentials MUST be supplied through a gitignored environment file or named AWS profile, MUST never be committed, and the account MUST have a billing budget or alarm with a one-dollar tripwire before real resources are used.

#### Scenario: Repository safety check runs
- **WHEN** the repository is checked for committed development credentials
- **THEN** no dev secret is present and the documented external credential source is used

### Requirement: Local cannot silently use real AWS
The local profile MUST not resolve real AWS endpoints or real-AWS credentials through fallback configuration.

#### Scenario: Local starts with dev credentials present
- **WHEN** dev credentials exist on the machine but the application starts under `local`
- **THEN** local service calls remain directed to LocalStack

