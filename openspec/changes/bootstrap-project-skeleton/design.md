## Context

The project has a bare Maven POM (Java 26, `org.example:RMS`) and a placeholder `Main.java`. No framework, no containers, no configuration. See [proposal.md](file:///C:/Users/wesle/IdeaProjects/RMS/openspec/changes/bootstrap-project-skeleton/proposal.md) for motivation.

Constraints from `project.md`: modular monolith, layered architecture (`Controller → Service → Repository`), SOLID/DRY, externalized config, no hardcoded credentials, LocalStack as default local environment.

## Goals / Non-Goals

**Goals:**
- Working Spring Boot app with `GET /actuator/health` returning 200
- Layered package skeleton ready for future stories to populate
- Docker Compose with app + LocalStack, runnable via `docker compose up`
- Profile-based configuration (`local` / `dev` / `prod`)

**Non-Goals:**
- Database setup — deferred to story 2 (`add-database-and-migrations`)
- Authentication — deferred to story 3
- Any business domain entities or APIs
- Real AWS deployment — deferred to story 10
- CI/CD pipeline

## Decisions

### 1. Spring Boot version: latest stable (3.x on Java 21+ LTS)
**Rationale**: The project targets Java LTS. Spring Boot 3.x requires Java 17+ and is the current maintained line. Using the Spring Boot parent POM simplifies dependency management.
**Alternative considered**: Spring Boot 2.x — rejected because it is in maintenance mode and doesn't support the latest Java features.

### 2. Base package: `com.rms`
**Rationale**: Short, matches the project name (RMS — Receivables Management System). Component scan from the root package covers all sub-packages.
**Alternative considered**: `com.example.rms` — unnecessarily verbose for a portfolio project.

### 3. Package structure: flat layered packages
**Rationale**: The `project.md` mandates `Controller → Service → Repository` layering. Creating the package skeleton upfront (`controller`, `service`, `repository`, `config`, `dto`, `model`) establishes convention before code grows. A flat layout (not nested by domain) matches the modular monolith guidance — domains don't need separate modules yet.
**Alternative considered**: Package-by-feature — cleaner for large codebases, but premature for a greenfield project with a single domain story pending. Can be refactored later if needed.

### 4. Dockerfile: multi-stage build
**Rationale**: Stage 1 builds with Maven; stage 2 copies the JAR into a slim JRE image. Keeps images small and doesn't leak build tools or source into the runtime image.
**Alternative considered**: Single-stage with pre-built JAR — faster locally but requires the developer to build the JAR before `docker compose up`. Multi-stage is self-contained.

### 5. Docker Compose: app + LocalStack (no DB yet)
**Rationale**: Story 2 makes the database decision. Adding a DB service now would pre-commit to a choice. LocalStack is included because later stories (S3, SQS, SNS) all need it, and having it from day one ensures the environment is stable early.
**Alternative considered**: Include Postgres as a placeholder — violates the roadmap's explicit deferral.

### 6. Configuration format: `application.yml` with profile variants
**Rationale**: YAML is more readable than properties for nested config. Profile activation via `spring.profiles.active` with `application-{profile}.yml` files. Default profile is `local`.
**Alternative considered**: `.properties` files — equivalent functionality, less readable for nested structures.

## Risks / Trade-offs

- **[Java 26 vs Spring Boot compatibility]** → The existing POM targets Java 26 (source/target). Spring Boot 3.x officially supports up to Java 23 at latest. Mitigation: verify compatibility at implementation time; fall back to latest officially supported Java LTS (21) if needed, updating `pom.xml` accordingly.
- **[Empty packages may be pruned by IDEs/tools]** → Some tools remove empty directories. Mitigation: add a `package-info.java` in each package to ensure they persist.
- **[LocalStack version drift]** → LocalStack evolves rapidly; features may differ between versions. Mitigation: pin a specific LocalStack version in `docker-compose.yml`.
