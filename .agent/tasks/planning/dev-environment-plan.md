# SRR — Dev Environment Plan (SQS / SNS / Cognito Learning Track)

## Goal

Keep LocalStack as the default local environment (fast, free, no account needed), but stand up a second, real **"dev"** AWS environment specifically to learn SQS, SNS, and Cognito against the actual services — since LocalStack mocks their behavior but doesn't teach IAM policies, delivery semantics, quotas, or console workflows.

Object storage (S3) is intentionally *not* part of the real-AWS track: it isn't Always Free, so it stays on LocalStack for `dev`, with Cloudflare R2 as the option if a real, publicly reachable bucket is ever needed.

## Environments

| Env | Purpose | Backing services |
|---|---|---|
| `local` | Day-to-day coding, fast iteration, offline | LocalStack (S3, SQS, SNS, Cognito emulation) |
| `dev` | Learning real AWS behavior, integration testing | Real AWS: SQS, SNS, Cognito · Cloudflare R2 (only if S3-equivalent is needed) |
| `test`/`prod` | Not in scope for this plan | — |

Application code should not know which environment it's in beyond endpoint/credential config — same SDK calls, different `application-dev.yml` / `application-local.yml` profile.

## Account & Access Setup

- [ ] Create a dedicated IAM user (not root) scoped to this project — e.g. `rms-dev`
- [ ] Attach least-privilege inline policies per service (see below), not `AmazonSQSFullAccess`-style managed policies
- [ ] Store credentials in `.env.dev` (gitignored) or AWS credentials profile `rms-dev`, never committed
- [ ] Set a **Billing Alarm** / Budget at $1 as a tripwire before touching anything real
- [ ] Confirm account type: if created before 2025-07-15, legacy 12-month free tier applies to S3/EC2 too; otherwise those are credit-based only — irrelevant here since SQS/SNS/Cognito are Always Free regardless of account age

## Service-by-Service Plan

### 1. SQS
- [ ] Create one standard queue: `rms-dev-events`
- [ ] Create a matching Dead Letter Queue: `rms-dev-events-dlq`, redrive policy `maxReceiveCount: 5`
- [ ] Wire producer: existing event-publishing code in SRR, pointed at real queue URL via Spring profile
- [ ] Wire consumer: a `@SqsListener` (or manual polling) reading from `rms-dev-events`
- [ ] Learning checkpoints: visibility timeout behavior, long polling vs short polling, at-least-once delivery / idempotency handling, DLQ redrive
- Free tier: 1M requests/month, resets monthly, no expiry

### 2. SNS
- [ ] Create one topic: `rms-dev-notifications`
- [ ] Subscribe the SQS queue above to the topic (fan-out pattern: SNS → SQS)
- [ ] Optionally subscribe your own email for a manual smoke test
- [ ] Learning checkpoints: pub/sub fan-out, topic policies, message filtering (subscription filter policies)
- Free tier: 1M publishes/month + 1,000 email notifications/month, resets monthly, no expiry

### 3. Cognito
- [ ] Create a User Pool: `rms-dev-users` (Lite or Essentials tier — Essentials is the current default)
- [ ] Configure one app client (no client secret, for testing via Postman/curl)
- [ ] Create 1–2 test users manually via console or CLI
- [ ] Replace/extend existing Spring Security/JWT auth flow with Cognito-issued JWTs on the `dev` profile only — keep the current custom JWT implementation intact for `local`
- [ ] Learning checkpoints: hosted UI vs API-only auth, token structure (ID vs access token), refresh token flow, custom attributes
- Free tier: 10,000 MAUs/month direct/social, 50/month SAML/OIDC, resets monthly, no expiry

### 4. S3 (out of scope for real AWS)
- Stays on LocalStack for `dev`
- If a real, internet-reachable object store is ever needed: Cloudflare R2 (10GB storage, 1M writes, 10M reads/month, free forever, S3-compatible API — same SDK, different endpoint/credentials)

## Rollout Order

1. SQS first (simplest mental model, no auth complexity)
2. SNS second (builds on SQS as a subscriber)
3. Cognito last (most moving parts — pools, clients, tokens, app integration)

## Guardrails

- Never point `local` profile at real AWS endpoints — keep LocalStack as the default so a forgotten `--profile dev` can't rack up surprise usage
- Tear down/recreate is cheap: these are Always Free monthly-reset quotas, not 12-month allowances, so there's no "used it up forever" risk — just don't script anything with unbounded loops against the real queue/topic
- Keep `dev` resource names prefixed `rms-dev-` to avoid collisions if this ever grows into a real multi-env setup

## Open Questions (to resolve before starting)

- Region choice for the `dev` resources (pick one and keep everything co-located to avoid cross-region data transfer)
- Whether Cognito replaces the existing custom JWT auth in `dev` entirely, or runs side-by-side for comparison
