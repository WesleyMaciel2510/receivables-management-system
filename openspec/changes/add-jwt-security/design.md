## Context

The application is a stateless Spring Boot REST API with public client and receivable endpoints, externalized profile configuration, and no current authentication boundary. The security design must fit the existing `com.rms` package structure, preserve the API's JSON error style, and avoid committing JWT secrets.

## Goals / Non-Goals

**Goals:**

- Establish a stateless JWT Bearer authentication boundary for the existing API.
- Provide a credential verification endpoint and BCrypt password handling.
- Make token validation and authorization failures deterministic and testable.
- Keep issuer, signing key, token lifetime, and CORS configuration externalized.

**Non-Goals:**

- OAuth2/OIDC federation, social login, or an external identity provider.
- Refresh-token rotation, revocation lists, password reset, email verification, or MFA.
- Changing client or receivable business rules after authentication succeeds.
- Building an administration UI or user-management API.

## Decisions

### JWT implementation and token shape

Use JJWT with a symmetric HMAC signing key loaded from environment-backed configuration. Include `sub`, `iss`, `iat`, `exp`, and authorities/roles claims. A symmetric key is appropriate for this single Spring Boot service; asymmetric keys and an authorization server are deferred until a separate trust boundary is required.

### Authentication data boundary

Introduce an application user abstraction behind `UserDetailsService`. The first implementation may use a dedicated database user table and a controlled local seed, but credentials must be stored as BCrypt hashes and must never be returned by an API. If the current roadmap has not yet defined user administration, keep the seeded account narrowly scoped to local bootstrap and isolate it so a later user-management change can replace it.

### Request processing

Configure one `SecurityFilterChain` with CORS enabled, CSRF disabled for the stateless REST surface, `SessionCreationPolicy.STATELESS`, public `/api/auth/**`, and authenticated business resources. Add a `OncePerRequestFilter` before the username/password filter to parse the Bearer header and populate the security context only after all JWT validations pass.

### Error handling and authorities

Use JSON `AuthenticationEntryPoint` responses for missing/invalid credentials and JSON `AccessDeniedHandler` responses for insufficient authorities. Keep the initial login contract authority-aware, but default the seeded principal to the least-privileged role needed by existing endpoints. Do not expose raw JWT parser or database exceptions.

### Configuration and operational safety

Require a sufficiently long signing secret, issuer, and expiration through profile properties/environment variables. Production configuration must not provide a committed fallback secret. Local development may document a placeholder in an ignored environment file or Compose invocation. Token lifetime defaults should be short enough for access tokens, with refresh tokens explicitly out of scope.

## Risks / Trade-offs

- **Signing-key disclosure invalidates all tokens** → Require environment/secret-manager injection, avoid logging tokens, and document key rotation as an operational procedure.
- **Database-backed users add migration and bootstrap complexity** → Keep the user schema minimal, use BCrypt hashes, and cover startup/login with PostgreSQL integration tests.
- **Disabling CSRF is unsafe for cookie-authenticated browsers** → Use header-based Bearer tokens and do not authenticate this API from cookies.
- **Role claims can become stale** → Keep access-token lifetime bounded and treat the token as an authorization snapshot; refresh/revocation is a later capability.
- **CORS misconfiguration can expose the API to browsers** → Make allowed origins explicit and environment-configurable; reject wildcard origins when credentials are enabled.

## Migration Plan

1. Add security/JWT dependencies and profile properties with no committed secrets.
2. Add the user credential schema/seed strategy and verify existing local data is unaffected.
3. Add token service, login endpoint, security filter chain, and JSON security handlers.
4. Deploy with authentication disabled only during a controlled migration window if needed; then require tokens for business endpoints.
5. Roll back application code by redeploying the prior image. Keep the user table migration forward-compatible; remove or separately review schema rollback rather than deleting credential data automatically.

## Open Questions

None. Refresh-token behavior and full user administration are intentionally deferred and do not change this change's contract.
