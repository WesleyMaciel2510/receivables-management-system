# AI PROMPT: Complete Missing User CREATE + MARK AS DELETED Operations

The existing authentication feature is already implemented based on the specification above.

The agent forgot to implement **user CREATE** and **user MARK AS DELETED**.

Complete the implementation now. **Do not redesign or remove the existing login/password-recovery functionality.**

## 1. CREATE USER

Implement:

`POST /api/users`

Request:

```json
{
  "username": "string",
  "password": "string",
  "email": "user@example.com",
  "firstName": "string",
  "lastName": "string",
  "linkedTable": "string"
}
```

Behavior:

1. Validate required fields.
2. `username` must be unique.
3. `email` must be unique.
4. Hash `password` with the existing Spring Security `Argon2PasswordEncoder`.
5. Never store plaintext passwords.
6. Set `active = true`.
7. Store `linkedTable` exactly as supplied; it is an opaque nullable string.
8. Persist the user.
9. Never return the password or password hash.
10. `created_at` and `updated_at` must be populated consistently with the existing entity/schema behavior.

Success:

HTTP `201 Created`

Return the created user without `password`:

```json
{
  "id": 1,
  "username": "string",
  "email": "user@example.com",
  "firstName": "string",
  "lastName": "string",
  "createdAt": "...",
  "updatedAt": "...",
  "active": true,
  "linkedTable": "string"
}
```

Do not expose internal exceptions or database details.

For duplicate username/email, return an appropriate client error such as `409 Conflict` with a safe generic message. Do not expose database constraint/SQL details.

---

## 2. MARK USER AS DELETED

There is **no `deleted` column** and no additional users-table column may be created.

"Delete" means **soft delete by setting `active = false`**.

Implement:

`PUT /api/users/{id}/mark-as-deleted`

No request body is required.

Behavior:

1. Require normal JWT authentication.
2. Load the user by ID.
3. If the user does not exist, return `404 Not Found`.
4. Set:

```text
active = false
```

5. Persist the change.
6. `updated_at` must update through the existing mechanism.
7. Do not physically delete the database row.
8. Do not modify username, email, password, names, or `linked_table`.
9. An inactive user must subsequently fail login with the existing generic:

```json
{
  "error": "Invalid credentials"
}
```

Success:

HTTP `204 No Content`

The endpoint must never return the user's password/hash.

---

## 3. SECURITY

Preserve the existing `SecurityConfig`.

The following remain public:

```text
POST /api/auth/login
POST /api/auth/password-recovery
POST /api/auth/password-recovery/verify
POST /api/auth/password-recovery/password
```

The new user-management endpoints must require normal authentication.

Do **not** create a new authentication mechanism.

Do **not** add:

* roles
* permissions
* admin tables
* refresh tokens
* sessions
* new user-status columns
* deleted columns
* password-recovery tables
* token tables

Use the existing JWT authentication mechanism.

---

## 4. DATA MODEL

Do not modify the existing `users` table schema.

It must remain exactly:

```text
id
username
password
email
first_name
last_name
created_at
updated_at
active
linked_table
```

Do not add `deleted_at`, `is_deleted`, `deleted`, `role`, or any other column.

---

## 5. IMPLEMENTATION

Add only the minimal classes/DTOs needed, adapting to the existing project structure.

Prefer:

```text
user/CreateUserRequest.java
user/UserResponse.java
user/UserController.java
user/UserService.java
```

Reuse the existing:

```text
user/User.java
user/UserRepository.java
```

and existing password encoder/security configuration.

Add repository methods only when necessary.

Ensure:

* Java 25
* Spring Boot 4
* Spring Data JPA
* Existing Liquibase schema remains compatible
* Existing authentication/recovery tests continue passing
* No password/hash is serialized by any user response
* No secrets/tokens are logged

---

## 6. VALIDATION

After implementation:

1. Compile the project.
2. Run the complete test suite.
3. Fix compilation/test failures caused by the implementation.
4. Verify CREATE:

    * valid user → `201`
    * password is Argon2id-hashed
    * response contains no password/hash
    * duplicate username/email handled safely
5. Verify MARK AS DELETED:

    * authenticated request → `204`
    * database row remains
    * `active = false`
    * `updated_at` changes
    * subsequent login fails with `401 Invalid credentials`
6. Verify existing login and password-recovery flows still work.
7. Do not modify unrelated functionality.

Do not stop at creating files. **Implement, compile, test, and fix everything until the project is consistent and passing.**
