## 1. Dependencies and configuration

- [x] 1.1 Add Spring Security and JJWT API/implementation dependencies to `pom.xml`, preserving the existing Spring Boot 4/Jakarta conventions; verify the project compiles and dependency resolution succeeds
- [x] 1.2 Add JWT issuer, signing secret, access-token expiration, CORS origins, and local bootstrap-user properties to `application-local.yml`, `application-dev.yml`, and `application-prod.yml`; verify no secret or plaintext password is committed
- [x] 1.3 Document the required JWT and bootstrap-user environment variables in the project configuration/development documentation; verify the documented local startup command supplies all required values

## 2. User credential boundary

- [x] 2.1 Add a Liquibase changeset under `src/main/resources/db/changelog/` for an `app_users` table with UUID identity, unique username, BCrypt password hash, authorities/roles, enabled flag, timestamps, and a rollback block; register it in the master changelog and verify existing changesets remain unchanged
- [x] 2.2 Add the JPA user entity, repository, and `UserDetailsService` adapter under the existing `com.rms` package structure; verify the application context can load the repository and resolve an enabled user without exposing the password hash
- [x] 2.3 Add a controlled local bootstrap mechanism that creates or updates the configured development user using `APP_SECURITY_USERNAME` and `APP_SECURITY_PASSWORD` only when explicitly enabled; verify passwords are stored as BCrypt hashes and production startup does not use a committed fallback credential
- [x] 2.4 Add a `PasswordEncoder` bean using BCrypt and verify password matching succeeds for the configured user while plaintext passwords are never logged or serialized

## 3. JWT authentication flow

- [x] 3.1 Implement a JWT service for token creation, parsing, signature validation, issuer validation, subject extraction, expiration validation, and authority claims using the configured secret; verify unit tests cover valid, malformed, expired, wrong-issuer, and wrong-signature tokens
- [x] 3.2 Add login request/response DTOs with validation and an authentication service/controller at `POST /api/auth/login`; verify valid credentials return `200` with token type, token, issuer, subject, authorities, and expiration metadata
- [x] 3.3 Verify invalid username/password login returns the same `401 Unauthorized` JSON shape without identifying which credential failed
- [x] 3.4 Implement a `OncePerRequestFilter` that extracts only the Bearer scheme, validates the token, populates the security context, and leaves invalid-token requests unauthenticated; verify the filter does not create sessions or accept tokens from query parameters/cookies

## 4. Authorization and error handling

- [x] 4.1 Add a stateless `SecurityFilterChain` with CORS, CSRF disabled for the header-based REST API, `SessionCreationPolicy.STATELESS`, public `/api/auth/**`, and authenticated `/clients/**` and `/receivables/**`; verify public login and protected-resource rules with MockMvc tests
- [x] 4.2 Add JSON `AuthenticationEntryPoint` and `AccessDeniedHandler` responses for stable `401` and `403` payloads without stack traces or sensitive details; verify missing/invalid credentials and insufficient authority scenarios
- [x] 4.3 Configure explicit environment-backed CORS origins and allow preflight requests without authentication; verify allowed preflight behavior and rejection of unsafe wildcard/credential combinations
- [x] 4.4 Apply least-privilege authorities to the initial user and add endpoint authority rules without changing existing authenticated business behavior; verify an authenticated request reaches the existing controllers and an unauthorized authority cannot execute the operation

## 5. Integration and operational verification

- [x] 5.1 Extend PostgreSQL-backed integration tests to cover Liquibase user-schema startup, local user bootstrap, login, protected endpoints, invalid/expired tokens, `401`, `403`, stateless sessions, and CORS preflight; verify the complete test suite passes with Compose PostgreSQL
- [x] 5.2 Verify a clean database applies the new Liquibase changeset and an existing database preserves all prior changesets and data; verify `docker compose config` has no committed credentials and Liquibase reports the database up to date on restart
- [x] 5.3 Verify the running local application exposes `/api/auth/login`, rejects unauthenticated `/clients` and `/receivables` requests, accepts a valid Bearer token, and does not emit JWTs or passwords in logs
