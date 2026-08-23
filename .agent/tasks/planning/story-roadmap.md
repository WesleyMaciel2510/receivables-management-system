# Story Roadmap: Gestão de Recebíveis (Java/Spring Boot + AWS, OpenSpec + agy)

This is your working backlog — not a file OpenSpec reads. The actual constitution lives in `project.md` (drop that into `openspec/project.md` after running `openspec init`); every story below inherits it automatically once it's there.

## The Stories — ordered OpenSpec changes

Each is its own `openspec` change folder (`proposal.md` / `specs/` / `design.md` / `tasks.md`). Tests and DoD live *inside* each story's tasks — don't split "testing" into a separate phase, OpenSpec expects it per-change.

**1. `bootstrap-project-skeleton`**
Empty-but-running app: Maven init, package structure decision, Docker Compose (app + LocalStack + DB), Actuator health check.
*DoD:* `docker compose up` → `GET /actuator/health` returns 200.

**2. `add-database-and-migrations`** — depends on 1
DB decision made once here (Postgres vs DynamoDB), Flyway/Liquibase, first entity + repository (`Cliente`).
*DoD:* migration runs clean, one repository integration test passes.

**3. `add-authentication`** — depends on 2
User/Role model, Spring Security, JWT issue/validate, register/login/me endpoints.
*DoD:* register → login → access protected endpoint with token; 401 without one.

**4. `add-core-domain-api`** — depends on 3
Core domain entities — `Recebível` (amount owed, due date, status), `Pagamento` (a payment applied against a receivable), `DocumentoComprobante` (metadata for the supporting document, uploaded in story 5) — plus DTOs, mappers, validation, role-based access.
*DoD:* full CRUD suite green in integration tests.

**5. `add-s3-file-storage`** — depends on 4
Storage abstraction interface (don't leak AWS SDK types upward), LocalStack bucket, pre-signed URLs — used to upload/download each `Recebível`'s supporting document (a fictional invoice/boleto).
*DoD:* upload → download → delete round-trips against LocalStack.

**6. `add-sqs-async-processing`** — depends on 4 (parallel-safe with 5)
Producer, consumer, DLQ, idempotent processing — registering a `Pagamento` enqueues async reconciliation that marks the `Recebível` as paid and recalculates its balance.
*DoD:* action enqueues, consumer processes once, a duplicate message doesn't double-process.

**7. `add-sns-fanout`** — optional, depends on 6
Only build this if you have ≥2 real independent consumers for the same event (e.g. a payment-received notification feeding both an audit-log consumer and a client-notification consumer) — otherwise skip it, it's complexity without a demonstrated need.
*DoD:* publish once, both queues receive it.

**8. `add-batch-job`** — depends on 6
Nightly job scans for overdue `Recebível` records, flags them, and publishes a reminder event per overdue item to the messaging layer. Job/Step/Reader/Processor/Writer, restartability, chunk processing.
*DoD:* job completes; re-running after a failure resumes correctly.

**9. `harden-observability-and-security`** — depends on 4 (safest done last)
Global exception handler, correlation IDs, JWT edge-case + auth test suite, secret-redaction check.
*DoD:* security test suite green, no secrets appear in logs.

**10. `deploy-to-real-aws`** — depends on everything above
Dev/prod profiles, Secrets Manager or env-based credentials, billing alarm/budget.
*DoD:* app runs against real Free-Tier resources; a documented teardown script exists.

**11. `polish-docs-and-diagrams`** — depends on 10
README, Mermaid diagrams, cost notes. Portfolio-ready.

---

## Running this with OpenSpec + agy

- Put the *constitution* (not the full original 500-line brief) into `openspec/project.md`.
- Work **one change folder at a time**: `/opsx:propose <story-name>` → review `proposal.md`/`tasks.md` yourself → `/opsx:apply` → `/opsx:verify` → `/opsx:archive` before starting the next story.
- Don't hand agy several stories in one session — the change-per-folder model exists specifically to keep each run's context small and reviewable. Batching defeats it.
- Stories 5 and 6 don't depend on each other — run them in either order, or as parallel branches, if you want.
