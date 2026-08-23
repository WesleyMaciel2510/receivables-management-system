# JWT security local setup

The application uses stateless Bearer JWT authentication. Liquibase creates the
`app_users` table; Hibernate schema generation remains disabled.

Required environment variables:

- `APP_SECURITY_JWT_SECRET`: at least 32 bytes; use a secret manager outside local development.
- `APP_SECURITY_USERNAME` and `APP_SECURITY_PASSWORD`: development bootstrap credentials.
- `APP_SECURITY_BOOTSTRAP_ENABLED`: set to `true` only when the configured local user should be created or updated.

Optional variables include `APP_SECURITY_JWT_ISSUER`,
`APP_SECURITY_JWT_ACCESS_TOKEN_MINUTES`, `APP_SECURITY_ROLES`, and
`APP_SECURITY_CORS_ALLOWED_ORIGINS`.

Example PowerShell startup with Docker Compose:

```powershell
$env:POSTGRES_URL = 'rms'
$env:POSTGRES_USER = 'rms_app'
$env:POSTGRES_PASSWORD = 'local-only-test-value'
$env:APP_SECURITY_JWT_SECRET = 'local-development-secret-change-me-32-bytes'
$env:APP_SECURITY_USERNAME = 'local-admin'
$env:APP_SECURITY_PASSWORD = 'local-only-password'
$env:APP_SECURITY_BOOTSTRAP_ENABLED = 'true'
docker compose up -d --build
```

Login is available at `POST /api/auth/login`. Protected client and receivable
endpoints require `Authorization: Bearer <token>`.
