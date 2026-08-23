Role & Task:
You are a Senior Spring Boot Security Architect. Implement standard JWT-based Bearer Token Authentication in this Spring Boot project.

Implementation Requirements:

1. Dependencies & Configuration:
    - Use standard Spring Security and JSON Web Token libraries (e.g., `io.jsonwebtoken:jjwt-*`).
    - Add JWT secret key, expiration time, and issuer configuration properties to `application.yml` / `application.properties`.

2. Security Architecture Components:
    - SecurityFilterChain: Configure `SecurityFilterChain` bean with CORS, CSRF disabled (for stateless REST API), and endpoint authorization rules (`/api/auth/**` permitted, other endpoints secured).
    - Session Management: Set session creation policy to `SessionCreationPolicy.STATELESS`.
    - JWT Utility Service: Create a service to generate, parse, and validate JWT access tokens (and optional refresh tokens).
    - JWT Authentication Filter: Create a custom `OncePerRequestFilter` to extract the Bearer token from the `Authorization` header, validate it, and populate the `SecurityContextHolder` with an `Authentication` object.
    - UserDetailsService / Auth Controller: Implement an authentication endpoint (`/api/auth/login`) that verifies user credentials and returns the JWT Bearer token response.

3. Codebase Integration Rules:
    - Match existing project conventions, package structures, and Java/Spring standards already in place.
    - Use proper Spring Security password encoding (`PasswordEncoder` / `BCryptPasswordEncoder`).
    - Handle security exceptions (401 Unauthorized, 403 Forbidden) cleanly via custom `AuthenticationEntryPoint` or `AccessDeniedHandler` if necessary.

Requested Security Specifications:
[Insert any specific requirements here, e.g., "Include roles (ROLE_USER, ROLE_ADMIN) inside token claims", "Implement refresh tokens", or "Use public/private key pairs instead of symmetric key"]

Output Required:
- List of new/modified files with exact target file paths.
- Complete, copy-pasteable Java classes and configuration snippets.
- Instructions on required environment properties to add to `application.yml`.