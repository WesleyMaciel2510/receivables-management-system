## Why

The project currently has only a bare Maven POM and a placeholder `Main.java` — no Spring Boot, no Docker, no health check. Every subsequent story (database, auth, domain API, AWS services) depends on having a running application with a proper package structure, containerized local environment, and verified health endpoint. This must be the first story so all future work has a stable foundation.

## What Changes

- Replace the placeholder `pom.xml` with a Spring Boot parent POM (latest stable LTS-compatible version), adding `spring-boot-starter-web` and `spring-boot-starter-actuator` dependencies
- Replace the placeholder `Main.java` with a proper Spring Boot application class under a well-defined package (`com.rms`)
- Establish the layered package structure: `controller`, `service`, `repository`, `config`, `dto`, `model` — empty but present
- Add `application.yml` with profile-based configuration (`local`, `dev`, `prod`)
- Create `Dockerfile` for the application (multi-stage build)
- Create `docker-compose.yml` with app service, LocalStack, and a database placeholder (database choice deferred to story 2)
- Add Spring Boot Actuator health endpoint (`/actuator/health`)
- Add a basic integration test verifying the application context loads

## Capabilities

### New Capabilities
- `project-skeleton`: Foundational project structure, build configuration, Docker setup, health endpoint, and profile-based externalized configuration

### Modified Capabilities
_None — this is the first story; no existing specs to modify._

## Impact

- **Build**: `pom.xml` is replaced entirely (from bare Maven to Spring Boot parent)
- **Source**: `Main.java` replaced; new package directories created under `com.rms`
- **Infrastructure**: New `Dockerfile` and `docker-compose.yml` at project root
- **Configuration**: New `application.yml` with Spring profiles
- **Dependencies**: Spring Boot Starter Web, Spring Boot Starter Actuator, Spring Boot Starter Test
- **No APIs beyond**: `GET /actuator/health` (returns 200)
