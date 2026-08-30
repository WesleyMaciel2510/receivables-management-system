## Why

SRR currently needs a safe, repeatable way to learn and integration-test real AWS SQS, SNS, and Cognito behavior while retaining LocalStack for fast, offline development. A dedicated `dev` profile will expose real IAM, delivery, quota, and authentication semantics without changing the default local workflow or introducing real AWS S3 usage.

## What Changes

- Add a `dev` environment profile backed by real AWS services in `us-east-1`, while keeping LocalStack as the default `local` environment.
- Provision least-privilege access and budget guardrails for a dedicated project IAM identity; keep credentials outside version control.
- Add an SQS standard event queue and dead-letter queue with a five-attempt redrive policy, and connect the application producer and consumer to the real queue.
- Add an SNS notifications topic with SQS fan-out, including room for subscription filtering and an optional email smoke-test subscription.
- Add a Cognito user pool and app client for API-only testing, with Cognito-issued JWT validation used only by `dev`.
- Preserve the existing custom JWT authentication flow for `local`.
- Keep S3 on LocalStack for `dev`; document Cloudflare R2 only as a future S3-compatible option.
- Document learning checkpoints and a teardown/recreation workflow that avoids unbounded traffic against real AWS.

## Capabilities

### New Capabilities

- `environment-profiles`: Environment-specific configuration that selects LocalStack for `local` and real AWS endpoints and credentials for `dev` without changing application behavior.
- `aws-messaging-dev`: Real AWS SQS and SNS development integration, including DLQ handling, SNS-to-SQS fan-out, and safe integration-test boundaries.
- `cognito-dev-auth`: Cognito user-pool/app-client authentication and Cognito JWT enforcement for the `dev` profile while retaining custom JWT authentication locally.
- `dev-environment-guardrails`: Least-privilege access, credential handling, region/resource naming, billing alarm, and usage-safety requirements for the real AWS learning environment.

### Modified Capabilities

- None.

## Impact

- Spring profiles and AWS SDK configuration, including new `application-dev.yml` settings and environment/profile documentation.
- Existing event publishing and consuming code, which must support real SQS/SNS endpoints and AWS delivery semantics.
- Spring Security/JWT configuration, with profile-specific Cognito issuer/JWK validation in `dev` and the current custom JWT flow in `local`.
- AWS account resources in `us-east-1`: one standard SQS queue, one DLQ, one SNS topic, an SNS-to-SQS subscription, and one Cognito user pool with one client.
- Local development remains dependent on LocalStack for S3, SQS, SNS, and Cognito emulation; no real AWS S3 resource is introduced.
