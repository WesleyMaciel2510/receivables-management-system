## 1. Repository Extensions

- [x] 1.1 Add `boolean existsByUsername(String username)` and `boolean existsByEmail(String email)` to `UserRepository` and verify the project compiles
- [x] 1.2 Confirm no Liquibase migration is needed — the existing `users` table schema already contains all required columns

## 2. DTOs

- [x] 2.1 Create `CreateUserRequest.java` record in `com.rms.dto` with fields `username`, `password`, `email`, `firstName`, `lastName`, `linkedTable` and Jakarta Validation annotations (`@NotBlank` on required fields, `@Email` on email)
- [x] 2.2 Create `UserResponse.java` record in `com.rms.dto` with fields `id`, `username`, `email`, `firstName`, `lastName`, `createdAt`, `updatedAt`, `active`, `linkedTable`; implement `from(User user)` static factory that omits the password field; verify compilation

## 3. UserService

- [x] 3.1 Create `UserService.java` in `com.rms.service` with constructor injection of `UserRepository`, `PasswordEncoder`, and (optionally) a logger
- [x] 3.2 Implement `create(CreateUserRequest request)` that checks duplicate username/email via the new repository methods, throws `ConflictException` on duplicates, hashes the password, builds a `User` entity, persists it, and returns `UserResponse`
- [x] 3.3 Implement `markAsDeleted(Long id)` that loads the user via `findById`, throws `ResourceNotFoundException` if absent, sets `active = false`, persists the change, and returns `void`
- [x] 3.4 Verify both methods compile and the service class has no unchecked warnings

## 4. UserController

- [x] 4.1 Create `UserController.java` in `com.rms.controller` mapped to `/api/users` with constructor injection of `UserService`
- [x] 4.2 Implement `POST /api/users` returning `ResponseEntity<UserResponse>` with HTTP 201; accept `@Valid @RequestBody CreateUserRequest`
- [x] 4.3 Implement `PUT /api/users/{id}/mark-as-deleted` returning `ResponseEntity<Void>` with HTTP 204; accept `@PathVariable Long id`
- [x] 4.4 Verify the controller compiles and follows the project's existing style (e.g., `ClientController`)

## 5. Security Configuration

- [x] 5.1 Verify `SecurityConfig.securityFilterChain()` already requires authentication for all paths except `/api/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**`, and `/actuator/health`; no code changes needed if this already covers `/api/users/**`
- [x] 5.2 Confirm no new exception types or filter beans are required

## 6. Build & Test

- [x] 6.1 Run `mvn compile` and confirm zero compilation errors
- [x] 6.2 Run `mvn test` and confirm all existing tests pass (the new endpoints have no dedicated tests yet; existing auth tests must remain green)
- [x] 6.3 Manually verify `POST /api/users` with valid payload returns 201 and the response contains no password field
- [x] 6.4 Manually verify `POST /api/users` with a duplicate username returns 409
- [x] 6.5 Manually verify `PUT /api/users/{id}/mark-as-deleted` returns 204 and the user's `active` flag becomes `false` in the database
- [x] 6.6 Manually verify that a soft-deleted user cannot log in via `POST /api/auth/login` and receives `401 Invalid credentials`
