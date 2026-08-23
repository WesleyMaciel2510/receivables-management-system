## 1. Maven / Spring Boot Setup

- [x] 1.1 Replace `pom.xml` with Spring Boot parent POM (latest stable 3.x), set `groupId` to `com.rms`, add `spring-boot-starter-web`, `spring-boot-starter-actuator`, and `spring-boot-starter-test` dependencies. Verify with `mvn validate` succeeding without errors
- [x] 1.2 Resolve Java version: check Spring Boot 3.x compatibility with Java 26; if unsupported, set `java.version` property to latest supported LTS (21). Verify `mvn compile` passes cleanly

## 2. Application Class and Package Structure

- [x] 2.1 Delete placeholder `src/main/java/org/example/Main.java` and create `src/main/java/com/rms/RmsApplication.java` with `@SpringBootApplication` annotation and `main` method. Verify the class compiles with `mvn compile`
- [x] 2.2 Create package skeleton under `com.rms`: `controller`, `service`, `repository`, `config`, `dto`, `model` — each with a `package-info.java` to prevent IDE/tool pruning. Verify all six packages exist after build

## 3. Configuration

- [x] 3.1 Create `src/main/resources/application.yml` with `spring.profiles.default: local` and server port configuration. Verify application starts with `mvn spring-boot:run` and logs the `local` profile as active
- [x] 3.2 Create `src/main/resources/application-local.yml`, `application-dev.yml`, and `application-prod.yml` profile files with placeholder structure (server port, logging level). Verify `mvn spring-boot:run -Dspring-boot.run.profiles=dev` activates the `dev` profile in logs

## 4. Health Endpoint

- [x] 4.1 Enable Actuator health endpoint by configuring `management.endpoints.web.exposure.include: health` in `application.yml`. Verify `GET /actuator/health` returns `200 OK` with `{"status":"UP"}` when app is running locally

## 5. Docker

- [x] 5.1 Create `Dockerfile` with multi-stage build: stage 1 builds with Maven, stage 2 copies the JAR into a slim JRE image. Verify `docker build -t rms .` completes successfully
- [x] 5.2 Create `docker-compose.yml` with `app` service (built from Dockerfile, port mapping, `local` profile) and `localstack` service (pinned version, health check). Verify `docker compose up` starts both containers and `GET localhost:<port>/actuator/health` returns `200 OK`
- [x] 5.3 Configure Docker networking so the app container can reach LocalStack by service name. Verify by checking `localstack` is resolvable from the app container (e.g., `docker compose exec app ping localstack` or equivalent connectivity check)

## 6. Integration Test

- [x] 6.1 Create `src/test/java/com/rms/RmsApplicationTests.java` with `@SpringBootTest` that verifies the application context loads. Verify with `mvn test` — the test passes

## 7. Credential Audit

- [x] 7.1 Review all created files and confirm no hardcoded passwords, API keys, or secret values are present. Verify by searching the repository for common credential patterns (e.g., `password=`, `secret=`, `apiKey=`)
