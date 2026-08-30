@echo off
set POSTGRES_USER=postgres
set POSTGRES_PASSWORD=postgres
set POSTGRES_URL=jdbc:postgresql://localhost:5432/rms
set APP_ENCRYPTION_SECRET=7f4c9a2e8b1d6f3a5c7e9b2d4a6f8c1e
set APP_SECURITY_JWT_ISSUER=rms-local
set APP_SECURITY_JWT_SECRET=5104a74d44bc52ba155e2ae5852f984239dd1c72f3fba1e81bdddcd17386f9ae
set APP_SECURITY_PASSWORD=local-admin-password
cd C:\Users\wesle\IdeaProjects\rms
mvn spring-boot:run -Dspring-boot.run.profiles=local
