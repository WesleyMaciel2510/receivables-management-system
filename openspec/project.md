# Project Context

## Purpose
Gestão de Recebíveis — a backend for tracking receivables: clients, amounts owed, due dates, payments against them, and automated overdue follow-up. All data (clients, companies, amounts) is fictional for this project.

## Tech Stack
- Java — latest stable LTS
- Spring Boot — latest stable version
- Maven
- Docker / Docker Compose
- Spring Web, Spring Security, JWT authentication
- Spring Batch
- AWS SDK for Java, LocalStack for local AWS emulation

## Architecture
- Modular monolith — layered: `Controller → Service → Repository`
- No microservices without a strong, explicitly stated reason
- SOLID, DRY, separation of concerns, dependency inversion, secure-by-default
- No unnecessary abstractions — avoid overengineering

## AWS
- Services in scope: S3, SQS, SNS (only if a real second consumer justifies it), one database
- Database choice (Postgres RDS vs DynamoDB) is decided once, in the database story, and recorded here once made
- LocalStack is the default environment for local development; real AWS is a later story, not the default
- Every AWS service must be evaluated for Always Free vs Free Tier vs "will eventually charge" before use — never assume a service stays free permanently

## Non-negotiables
- No hardcoded credentials, secrets, or passwords, anywhere, ever
- Every change must leave the application in a runnable, working state
- Prefer official AWS/Spring documentation over tutorials when verifying version or API details
- Configuration is externalized (profiles: local / dev / prod); nothing environment-specific is hardcoded
