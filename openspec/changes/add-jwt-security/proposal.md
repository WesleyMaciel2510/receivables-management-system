## Why

The receivables API currently has no authentication boundary, so any caller that can reach the service can invoke client and receivable operations. This change establishes stateless JWT Bearer authentication now, before additional business capabilities are added, while keeping credentials and signing configuration externalized.

## What Changes

- Add Spring Security and JWT dependencies using a maintained JJWT implementation.
- Add externalized JWT issuer, signing secret, and access-token expiration settings for local, dev, and prod profiles.
- Add a stateless `SecurityFilterChain` with CORS support, CSRF disabled for the REST API, and explicit public/private endpoint rules.
- Add password hashing with BCrypt and an authentication endpoint at `/api/auth/login`.
- Add JWT generation, parsing, validation, Bearer-token filtering, and SecurityContext population.
- Add clean JSON responses for unauthenticated (`401`) and forbidden (`403`) requests.
- Keep `/api/auth/**` public while securing the existing `/clients` and `/receivables` resources.

## Capabilities

### New Capabilities

- `jwt-security`: Stateless JWT authentication, login, password handling, token validation, and authorization behavior for the API.

### Modified Capabilities

None.

## Impact

- Adds Spring Security and JJWT dependencies to `pom.xml`.
- Adds security configuration, JWT service/filter, authentication controller/service, user lookup, and security exception handling under `src/main/java/com/rms`.
- Adds authentication request/response DTOs and a persistent or seeded user credential boundary as defined by the design.
- Adds profile configuration and required environment variables without committing signing secrets.
- Changes the externally observable behavior of `/clients` and `/receivables` from public access to requiring a valid Bearer token.
- Adds integration coverage for login, valid/invalid/expired tokens, unauthorized requests, and forbidden requests.
