# shop-secure (Lab-05: Security in Java Web Applications)

Spring Boot 4.1.2 / Spring Security 7.1.2 / JDK 25 / Maven 3.9 project built step-by-step across Lab-05.

## How to Run & Verify

```bash
mvn verify
mvn spring-boot:run
```

*(Note: `./mvnw` can also be used once the Maven Wrapper distribution has been fetched.)*

---

## Task 1 — Default Spring Security Auto-Configuration

When the application starts with Spring Security on the classpath and **no** custom `SecurityFilterChain` or `UserDetailsService` bean (and without `spring-boot-starter-oauth2-resource-server` on the classpath during Step 2 observation), Spring Boot logs:

```text
WARN 3300 --- [shop-secure] [           main] .s.a.UserDetailsServiceAutoConfiguration :

Using generated security password: 0a16c1b7-a15d-4381-b2ed-8ccad66424a3

This generated password is for development use only. Your security configuration must be updated before running your application in production.
```

Running with `--debug` shows the exact auto-configuration classes in the **Conditions Evaluation Report** responsible for this behavior:

1. **`UserDetailsServiceAutoConfiguration`**:
   Matched when no `AuthenticationManager`, `AuthenticationProvider`, `UserDetailsService`, `AuthenticationManagerResolver`, or `JwtDecoder` bean is defined, no `spring.security.user.name` / `spring.security.user.password` properties are configured, and `MissingAlternativeUserDetailsManagerOrUserPropertiesConfigured` finds none of `ClientRegistrationRepository`, `OpaqueTokenIntrospector`, or `RelyingPartyRegistrationRepository` on the classpath. It registers a default `InMemoryUserDetailsManager` with username `user` and a random UUID password logged at startup.
   *(Boot 4 nuance: if `spring-boot-starter-oauth2-resource-server` is already on the classpath, `OpaqueTokenIntrospector` causes `UserDetailsServiceAutoConfiguration` to back off until a custom `UserDetailsService` or `SecurityFilterChain` is added; commenting out `spring-boot-starter-oauth2-resource-server` during Step 2 allowed observing the generated password.)*
2. **`ServletWebSecurityAutoConfiguration`** (`EnableWebSecurityConfiguration` and `SecurityFilterChainConfiguration`):
   Matched via `@ConditionalOnMissingBean(SecurityFilterChain.class)`. It activates `@EnableWebSecurity` and creates the default `SecurityFilterChain` protecting every endpoint (`anyRequest().authenticated()`), enabling HTTP Basic and form login, and creating an HTTP session (`Set-Cookie: JSESSIONID=...`).
3. **`SecurityFilterAutoConfiguration`** (`securityFilterChainRegistration`):
   Registers the `springSecurityFilterChain` `DelegatingFilterProxy` servlet filter on `/*`.
4. **`SecurityAutoConfiguration`**:
   Provides the `DefaultAuthenticationEventPublisher`.

Once `edu.itc.shop.config.SecurityConfig` defines an explicit `SecurityFilterChain` bean and a `UserDetailsService` bean, both `ServletWebSecurityAutoConfiguration`'s default chain and `UserDetailsServiceAutoConfiguration` back off cleanly.

---

## Task 2 — JPA User Accounts, Password Encoding, and Registration

Task 2 replaces temporary in-memory production users with persistent JPA-based accounts. It introduces role-based authorization, secure password encoding, database seeding, and a public registration endpoint.

### 1. User Roles and JPA Entity

#### `edu.itc.shop.domain.Role`

The application defines two roles:

- `CUSTOMER` — standard shop customer.
- `ADMIN` — administrator with permission to perform administrative operations.

#### `edu.itc.shop.domain.UserAccount`

The `UserAccount` entity maps application accounts to the `user_accounts` database table.

| Field | Description |
| --- | --- |
| `id` | Generated primary key |
| `username` | Unique, non-null username |
| `passwordHash` | Encoded password |
| `enabled` | Indicates whether the account is enabled |
| `roles` | Eagerly fetched collection of account roles |

- Roles are stored in the separate `user_account_roles` collection table using enum string values.
- The entity's `toString()` method does not expose the password hash.

### 2. User Account Repository

**`edu.itc.shop.repository.UserAccountRepository`**

Extends `JpaRepository<UserAccount, Long>` and provides lookup and duplicate-username checks:

```java
Optional<UserAccount> findByUsername(String username);
boolean existsByUsername(String username);
```

### 3. Password Encoding

**`edu.itc.shop.config.PasswordConfig`**

The application uses:

```java
PasswordEncoderFactories.createDelegatingPasswordEncoder()
```

- New passwords are encoded before being stored in the database.
- `BCrypt` is the default encoding algorithm, and encoded passwords use the `{bcrypt}` prefix.
- Raw passwords are not stored in the database or returned by the registration endpoint.

### 4. Database-Backed Authentication

**`edu.itc.shop.security.JpaUserDetailsService`**

Implements Spring Security's `UserDetailsService`:

- Finds an account by username using `UserAccountRepository`.
- Throws `UsernameNotFoundException` if the account does not exist.
- Maps stored roles to Spring Security authorities using `.roles(...)` (e.g., `ADMIN` → `ROLE_ADMIN`, `CUSTOMER` → `ROLE_CUSTOMER`).
- Uses the stored password hash for authentication.
- Maps the account's `enabled` status to Spring Security's disabled status.

Production authentication uses the JPA-backed service rather than temporary in-memory users.

### 5. Account Registration Service

**`edu.itc.shop.service.AccountService`**

Handles account creation within a transaction. Its registration workflow:

1. Checks whether the username already exists.
2. Throws `UsernameTakenException` when the username is taken.
3. Encodes the submitted raw password with `PasswordEncoder`.
4. Creates an enabled `UserAccount`.
5. Saves the account through the repository.

A new public registration receives the `CUSTOMER` role only; users cannot assign themselves the `ADMIN` role through the registration request.

### 6. Registration API

**Endpoint:** `POST /api/register`

**Request example:**

```http
POST /api/register
Content-Type: application/json

{
  "username": "new_customer",
  "password": "StrongPass123"
}
```

**Successful response (`201 Created`):**

```json
{
  "username": "new_customer"
}
```

The response contains only the username and does not expose the raw password or stored password hash.

#### Registration Validation (`RegisterRequest`)

| Field | Requirements |
| --- | --- |
| `username` | Required; 3–40 characters; lowercase letters, digits, underscores, and periods only |
| `password` | Required; 12–72 characters; must contain an uppercase letter, lowercase letter, and digit |

#### Response Statuses

| HTTP Status | Meaning |
| --- | --- |
| `201 Created` | Account successfully registered |
| `400 Bad Request` | Request validation failed |
| `409 Conflict` | Username already exists |

Validation and conflict responses use `application/problem+json`.

### 7. Initial Account Seeding

**`edu.itc.shop.config.DataInitializer`**

A `CommandLineRunner` initializes sample product data and account data when the application starts.

| Username | Roles |
| --- | --- |
| `admin` | `ADMIN`, `CUSTOMER` |
| `alice` | `CUSTOMER` |

The seeder checks whether each username already exists before creating it, so running the seeder again does not create duplicate accounts.

Seed passwords are read from application properties:

```yaml
shop:
  seed:
    admin-password: ${SHOP_SEED_ADMIN_PASSWORD:ChangeMeAdmin123}
    customer-password: ${SHOP_SEED_CUSTOMER_PASSWORD:ChangeMeAlice123}
```

The environment variables `SHOP_SEED_ADMIN_PASSWORD` and `SHOP_SEED_CUSTOMER_PASSWORD` can override the development defaults.

> **Important:** Change the development defaults and configure secure environment variables before using the application outside a local development environment.

### 8. Security Configuration

**`edu.itc.shop.config.SecurityConfig`**

The application uses a stateless HTTP Basic security configuration:

| Request | Access |
| --- | --- |
| `GET /api/products/**` | Public |
| `POST /api/register` | Public |
| `DELETE /api/products/**` | `ADMIN` role required |
| Other `/api/**` endpoints | Authentication required |
| Other requests | Denied |

Additional security configuration:

- HTTP Basic authentication is enabled.
- Form login is disabled.
- CSRF protection is disabled for this stateless API configuration.
- Session creation policy is `STATELESS`.
- Authentication failures return `401 Unauthorized`.
- Authorization failures return `403 Forbidden`.
- Security error responses use `application/problem+json`.

### 9. Automated Testing and Verification

The project includes product security tests and account integration tests.

**Account integration tests:**

```bash
./mvnw -Dtest=AccountIntegrationTest test
```

Verified scenarios include:

- Seeded admin and customer accounts have the expected roles.
- Stored account passwords use the `{bcrypt}` format.
- `JpaUserDetailsService` loads the expected authorities.
- Registration creates a customer account in the database.
- Duplicate registration returns `409 Conflict`.
- Invalid registration returns `400 Bad Request`.
- Re-running the seed process does not duplicate accounts.

**Complete test suite:**

```bash
./mvnw clean test
```

Verified result:

```text
Tests run: 16, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

The complete test suite passed with 16 tests, zero failures, zero errors, and zero skipped tests.
