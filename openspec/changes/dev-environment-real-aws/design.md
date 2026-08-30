## Context

The application already has a LocalStack-oriented development path and event/authentication integrations that need profile-specific configuration. The proposal and capability specs define the externally visible contract. The design must add real-service wiring without making local startup depend on AWS credentials or real AWS S3.

## Goals / Non-Goals

**Goals:**

- Make `local` and `dev` explicit, mutually safe Spring configuration profiles.
- Provision and document a small, co-located SQS/SNS/Cognito learning environment in `us-east-1`.
- Reuse existing event contracts and authorization semantics while changing only service endpoints and token issuer by profile.
- Make smoke tests bounded, repeatable, and safe to tear down.

**Non-Goals:**

- Creating a production, test, or highly available AWS environment.
- Moving S3 from LocalStack or introducing Cloudflare R2 now.
- Replacing local custom JWT authentication.
- Adding unrestricted AWS-managed policies or long-running real-AWS load tests.

## Decisions

### Profile isolation

Use a checked-in non-secret profile structure with endpoint, region, resource identifier, and issuer placeholders; resolve credentials only from the AWS SDK credential chain or an explicitly documented gitignored `.env.dev`. `local` keeps explicit LocalStack endpoints rather than inheriting AWS defaults. This is safer than one profile with mutable endpoint overrides because a forgotten dev credential cannot redirect local calls.

### Resource provisioning

Represent the queue, DLQ, redrive policy, topic, subscription, and Cognito resources as an idempotent, reviewable provisioning procedure. Keep all resources co-located in `us-east-1` and prefixed `rms-dev-`. Prefer service-specific inline permissions for the dedicated project identity over broad managed policies. The procedure must also configure the one-dollar billing alarm before application smoke tests.

### Messaging integration

Configure the existing publisher and consumer against the profile-selected queue/topic identifiers. Validate the SNS-to-SQS subscription and JSON envelope behavior explicitly because real SNS delivery differs from a local mock. Consumer processing must remain idempotent and use bounded polling so at-least-once delivery is demonstrated without accidental traffic loops.

### Authentication integration

Use Cognito issuer and JWK discovery settings only in `dev`, validating access-token issuer, audience/client association, expiry, and signature. Keep the existing custom JWT decoder/provider active in `local`. Cognito Hosted UI is not required for the initial path; API-only flows reduce moving parts and match the no-secret app-client requirement.

### Verification order

Provision and verify SQS first, then add SNS fan-out, then enable Cognito. Each stage has an independent smoke test and teardown/recreation notes, which keeps failures localized and follows the learning order in the plan.

## Risks / Trade-offs

- [Real AWS charges or quota surprises] → Require the one-dollar budget/alarm, bounded tests, explicit `dev` selection, and no real S3 resources.
- [Credentials leak through configuration] → Keep secrets in gitignored files or named AWS profiles and add repository checks/documentation.
- [SNS and SQS envelope/acknowledgement differences] → Test raw delivery, visibility timeout, duplicate delivery, filtering, and DLQ behavior against the real services.
- [Cognito token claims differ from custom JWT claims] → Map only the required claims in the dev security adapter and keep authorization tests for both profiles.
- [Provisioning drift or duplicate resources] → Use deterministic names, idempotent commands, and an inventory/teardown procedure.

## Migration Plan

1. Add profile configuration and credential documentation while leaving `local` as the default.
2. Create the IAM identity and billing alarm, then provision and smoke-test SQS and its DLQ.
3. Provision SNS, subscribe SQS, and run bounded fan-out/filtering checks.
4. Provision Cognito users/client, enable dev-only token validation, and run protected-request checks.
5. Roll back by selecting `local` and removing only the dev profile/resources; local custom JWT and LocalStack operation remain unchanged.

