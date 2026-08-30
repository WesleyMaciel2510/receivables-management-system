## 1. Profile and credential boundaries

- [ ] 1.1 Add explicit `local` and `dev` configuration for AWS region, endpoints, resource identifiers, and Cognito issuer; verify default startup resolves only LocalStack and `dev` resolves `us-east-1` placeholders from external configuration
- [ ] 1.2 Add gitignored `.env.dev`/AWS-profile documentation and a repository credential-safety check; verify no secret values are committed and local startup works without dev credentials
- [ ] 1.3 Document the `rms-dev-` naming convention, `us-east-1` region, profile-selection guardrail, and LocalStack-only S3 behavior; verify the documented commands match the profile configuration

## 2. AWS account and messaging resources

- [ ] 2.1 Create the dedicated non-root project IAM identity with service-specific least-privilege SQS, SNS, and Cognito permissions; verify an allowed operation succeeds and an out-of-scope operation is denied
- [ ] 2.2 Configure the one-dollar billing budget/alarm before provisioning application resources; verify the budget is visible in the AWS account
- [ ] 2.3 Provision `rms-dev-events` and `rms-dev-events-dlq` in `us-east-1` with a five-receive redrive policy; verify queue attributes and redrive configuration through AWS CLI/API
- [ ] 2.4 Provision `rms-dev-notifications` and subscribe the dev SQS endpoint; verify an SNS publish is delivered to the queue and subscription permissions are valid

## 3. SQS/SNS application integration

- [ ] 3.1 Point the existing dev event publisher at the profile-selected real queue while retaining the LocalStack path; verify a supported event reaches `rms-dev-events` under `dev`
- [ ] 3.2 Enable the dev consumer for the real queue with bounded polling, visibility-timeout handling, and idempotent processing; verify successful acknowledgment and duplicate delivery do not create duplicate effects
- [ ] 3.3 Add bounded SQS/SNS integration smoke checks for long polling, at-least-once delivery, DLQ redrive, SNS fan-out, and subscription filtering; verify the checks terminate without unbounded requests

## 4. Cognito dev authentication

- [ ] 4.1 Provision `rms-dev-users`, one no-secret app client, and one or two test users; verify the client can obtain Cognito tokens through the documented API-only flow
- [ ] 4.2 Add dev-only Cognito issuer/JWK and token validation configuration; verify valid access tokens authenticate and expired, wrong-issuer, wrong-audience, and invalid-signature tokens are rejected
- [ ] 4.3 Preserve the custom JWT security path for `local`; verify protected local requests work without AWS credentials or Cognito network access

## 5. End-to-end verification and operations

- [ ] 5.1 Run the rollout in order—SQS, SNS, then Cognito—and record resource identifiers and smoke-test results; verify each stage can be independently repeated
- [ ] 5.2 Add teardown/recreation instructions for all `rms-dev-` resources and test them against the dev inventory; verify teardown does not touch LocalStack or local application data
- [ ] 5.3 Run the full local and dev verification suite, including profile isolation, messaging, authentication, credential-safety, and bounded-traffic checks; verify the default local workflow remains unchanged
