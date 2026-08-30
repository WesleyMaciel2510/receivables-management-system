## Purpose

Provides explicit local and real-AWS development profiles so the same application behavior can be exercised against different service endpoints without accidental environment crossover.

## ADDED Requirements

### Requirement: Local profile remains the safe default
The application MUST use LocalStack endpoints and emulated credentials when started with the default `local` profile, including for S3, SQS, SNS, and Cognito integrations.

#### Scenario: Application starts without an explicit profile
- **WHEN** a developer starts the application without selecting an environment profile
- **THEN** the application uses the `local` profile and does not address real AWS service endpoints

### Requirement: Dev profile selects co-located real AWS services
The `dev` profile MUST target the configured AWS region `us-east-1` for SQS, SNS, and Cognito, and MUST obtain credentials from an external environment variable or AWS profile rather than committed configuration.

#### Scenario: Developer selects dev
- **WHEN** the application is started with the `dev` profile and valid external AWS credentials
- **THEN** SQS, SNS, and Cognito requests target real AWS resources in `us-east-1`

### Requirement: Object storage stays local in dev
The `dev` profile MUST continue to use LocalStack for S3 and MUST NOT require or create a real AWS S3 bucket.

#### Scenario: Dev application accesses object storage
- **WHEN** an object-storage operation runs under the `dev` profile
- **THEN** the operation uses the LocalStack S3 endpoint

