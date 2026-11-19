# shop-secure (Lab-05: Security in Java Web Applications)

Spring Boot 4.1.2 / Spring Security 7.1.2 / JDK 25 / Maven 3.9 project built step-by-step across Lab-05.

## How to Run & Verify

### Required Environment Variables

Copy `.env.example` to `.env` or set environment variables in your shell:

| Variable | Description | Example / Minimum |
| --- | --- | --- |
| `JWT_SECRET` | HS256 JWT signing secret | Min 32 characters (e.g. `openssl rand -base64 48`) |
| `SHOP_SEED_ADMIN_PASSWORD` | Password for seeded `admin` account | E.g. `Admin-pass-123` |
| `SHOP_SEED_CUSTOMER_PASSWORD` | Password for seeded `alice` account | E.g. `Alice-pass-123` |

### Running the Application

```bash
export JWT_SECRET=$(openssl rand -base64 48)
export SHOP_SEED_ADMIN_PASSWORD="Admin-pass-123"
export SHOP_SEED_CUSTOMER_PASSWORD="Alice-pass-123"
./mvnw spring-boot:run
```

On Windows (PowerShell):
```powershell
$env:JWT_SECRET = (openssl rand -base64 48).Trim()
$env:SHOP_SEED_ADMIN_PASSWORD = "Admin-pass-123"
$env:SHOP_SEED_CUSTOMER_PASSWORD = "Alice-pass-123"
.\mvnw.cmd spring-boot:run
```

### Running the Test Suite

```bash
$env:JWT_SECRET = (openssl rand -base64 48).Trim()
./mvnw test
```

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

---

## Task 3 — Roles and Method Security

Task 3 adds customer-owned orders, declarative method-level authorization (`@PreAuthorize` and `@PostAuthorize`), a `RoleHierarchy` where `ADMIN` implies `CUSTOMER`, unified `application/problem+json` handling for method-security denials, and service/controller security tests.

### 1. Order Domain and Repository

#### `edu.itc.shop.domain.Order`

The `Order` entity maps customer orders to the `shop_orders` database table.

| Field | Description |
| --- | --- |
| `id` | Generated primary key (`Long`) |
| `customer` | Non-null owner username (`String`) |
| `productId` | Non-null ordered product ID (`Long`) |
| `quantity` | Non-null item quantity (`int`) |
| `status` | Non-null `Order.Status` enum (`NEW`, `SHIPPED`, `CANCELLED`), defaulting to `NEW` |

#### `edu.itc.shop.repository.OrderRepository`

Extends `JpaRepository<Order, Long>` and provides customer order lookups:

```java
List<Order> findByCustomer(String customer);
```

### 2. Method Security and Role Hierarchy

**`edu.itc.shop.config.MethodSecurityConfig`**

Enables Spring Security method-level authorization via `@EnableMethodSecurity` and defines a `RoleHierarchy` bean:

```java
@Bean
RoleHierarchy roleHierarchy() {
    return RoleHierarchyImpl.fromHierarchy("ROLE_ADMIN > ROLE_CUSTOMER");
}
```

With this hierarchy in place, `ADMIN` implicitly satisfies `CUSTOMER` role checks across security evaluations.

### 3. Order Service Authorization Rules

**`edu.itc.shop.service.OrderService`**

Order authorization is enforced declaratively on service methods rather than imperative `if` checks in controllers:

| Method | Annotation | Rule / Behavior |
| --- | --- | --- |
| `place(String customerId, PlaceOrder cmd)` | `@PreAuthorize("#customerId == authentication.name")` | A user can only place an order under their own username |
| `history(String customerId)` | `@PreAuthorize("hasRole('ADMIN') or #customerId == authentication.name")` | Customers can view their own history; `ADMIN` can view any customer's history |
| `get(long id)` | `@PostAuthorize("hasRole('ADMIN') or returnObject.customer == authentication.name")` | Customers can view only their own order; `ADMIN` can view any order |
| `ship(long id)` | `@PreAuthorize("hasRole('ADMIN')")` | Only `ADMIN` can ship an order (sets status to `Order.Status.SHIPPED`) |
| `all()` | `@PreAuthorize("hasRole('ADMIN')")` | Only `ADMIN` can retrieve all orders |

### 4. Order API and Request Validation

**`edu.itc.shop.web.OrderController`** (`/api/orders`)

| Method & Path | Behavior | Response |
| --- | --- | --- |
| `POST /api/orders` | Places an order using `Authentication.getName()` as `customer` (never taken from the request body) | `201 Created` (`Location: /api/orders/{id}`) with `Order` JSON |
| `GET /api/orders` | Returns the authenticated user's order history, or `?customer=<username>` (checked by `OrderService.history`) | `200 OK` with JSON array of `Order` |
| `GET /api/orders/{id}` | Retrieves a single order by ID (owner or `ADMIN` enforced via `@PostAuthorize`) | `200 OK` with `Order` JSON |
| `POST /api/orders/{id}/ship` | Marks an order as `SHIPPED` (`ADMIN` only) | `204 No Content` |

#### Request Validation (`PlaceOrder`)

| Field | Requirements |
| --- | --- |
| `productId` | Required (`@NotNull`) |
| `quantity` | Minimum `1` (`@Min(1)`) |

#### Example Request & Responses

**1. Customer (`alice`) places an order — `201 Created`:**

```http
POST /api/orders
Authorization: Basic <alice>
Content-Type: application/json

{
  "productId": 1,
  "quantity": 2
}
```

```json
{
  "customer": "alice",
  "productId": 1,
  "quantity": 2,
  "id": 1,
  "status": "NEW"
}
```

**2. Another customer (`bob`) attempts to read Alice's order — `403 Forbidden`:**

```http
GET /api/orders/1
Authorization: Basic <bob>
```

```json
{
  "detail": "Access denied",
  "instance": "/api/orders/1",
  "status": 403,
  "title": "Forbidden",
  "type": "about:blank"
}
```

**3. Administrator (`admin`) reads Alice's order — `200 OK`:**

```http
GET /api/orders/1
Authorization: Basic <admin>
```

```json
{
  "customer": "alice",
  "productId": 1,
  "quantity": 2,
  "id": 1,
  "status": "NEW"
}
```

*(Full HTTP transcripts are recorded in [security-session.md](security-session.md).)*

### 5. Filter Chain and ProblemDetail Exception Alignment

- **`edu.itc.shop.config.SecurityConfig`**: Adds `.requestMatchers(HttpMethod.POST, "/api/orders/*/ship").hasRole("ADMIN")`, while all other `/api/orders/**` endpoints require authentication via `.requestMatchers("/api/**").authenticated()`.
- **`edu.itc.shop.web.ApiExceptionHandler`**: Handles `AccessDeniedException` thrown by method-level security so `@PreAuthorize` and `@PostAuthorize` denials return the same `403 Forbidden` `application/problem+json` `ProblemDetail` payload (`title: "Forbidden"`, `detail: "Access denied"`, `instance: request URI`) as URL-level filter-chain denials.

### 6. Automated Testing and Verification

#### `edu.itc.shop.OrderServiceSecurityTest` (`@SpringBootTest`, 5 tests)

Tests `OrderService` method-security enforcement with `@WithMockUser` against `OrderRepository`:

- `alice` (`CUSTOMER`) can view her own order history.
- `alice` (`CUSTOMER`) cannot view `bob`'s order history (`AccessDeniedException`).
- `admin` (`ADMIN`, `CUSTOMER`) can view `bob`'s order history.
- `alice` (`CUSTOMER`) cannot ship an order (`AccessDeniedException`).
- Unauthenticated caller cannot access order history (`AuthenticationCredentialsNotFoundException`).

#### `edu.itc.shop.OrderControllerSecurityTest` (`@WebMvcTest(OrderController.class)`, 3 tests)

Imports `SecurityConfig` and `TestUsersConfiguration`, verifying HTTP filter-chain behavior with `MockMvc` and `httpBasic(...)`:

- Anonymous `GET /api/orders` returns `401 Unauthorized` (`application/problem+json`) with no service invocation.
- Customer (`alice`) `POST /api/orders/1/ship` returns `403 Forbidden` (`application/problem+json`) with no service invocation.
- Admin (`admin`) `POST /api/orders/1/ship` returns `204 No Content` and delegates to `orderService.ship(1L)`.

**Order security tests:**

```bash
./mvnw -Dtest='Order*SecurityTest' test
```

Surefire reports (`OrderControllerSecurityTest` + `OrderServiceSecurityTest`):

```text
OrderControllerSecurityTest: Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
OrderServiceSecurityTest:    Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
```

**Complete test suite (`AccountIntegrationTest`, `OrderControllerSecurityTest`, `OrderServiceSecurityTest`, `ProductSecurityTest`, `ShopSecureApplicationTests`):**

```bash
./mvnw clean test
```

Combined Surefire test count across all 5 test classes:

```text
Tests run: 24, Failures: 0, Errors: 0, Skipped: 0
```

---

## Task 4 — JWT Login and Stateless API

Task 4 replaces HTTP Basic with a stateless OAuth2 Resource Server. `POST /api/auth/login` authenticates against the existing JPA-backed users and returns an HS256 access token. The token contains the `shop-api` issuer, username subject, issued/expiry times, a unique `jti`, and role authorities. Only authorities beginning with `ROLE_` are included in the roles claim.

### Configuration and API

- `SecurityProps` binds `shop.security.jwt-secret` and `shop.security.access-token-ttl`; the secret is validated to be at least 32 characters.
- `JwtConfig` creates the HMAC key, Nimbus encoder/decoder, role-claim converter, and a DAO authentication manager.
- `JWT_SECRET` is required at runtime and is not stored in the repository. PowerShell example:

  ```powershell
  $env:JWT_SECRET = (openssl rand -base64 48).Trim()
  .\mvnw.cmd spring-boot:run
  ```

- Access-token lifetime is 15 minutes. HTTP Basic is disabled; `/api/**` accepts Bearer tokens and the filter chain remains stateless.
- Login failures return generic `401 application/problem+json`. Missing and invalid/expired Bearer credentials also return ProblemDetail responses; no session cookie is issued.

### Verification

```powershell
.\mvnw.cmd '-Dtest=AuthFlowIT,AccountIntegrationTest,ProductSecurityTest,OrderControllerSecurityTest,OrderServiceSecurityTest' test
```

`AuthFlowIT` verifies login, issuer/subject/id/roles claims, Bearer access to `/api/orders`, no `Set-Cookie`, generic bad-password handling, and an expired token returning `401`. Web MVC security tests use Spring Security's `jwt()` request processor. The Task 4 request/response notes and decoded JWT structure are in [security-session.md](security-session.md).

Full verification (`.\mvnw.cmd -q verify`) passes with 30 tests, zero failures, and zero errors.

---

## Task 5 — Hardening: CSRF, CORS, Headers, XSS, Secrets, and Logging

Task 5 hardens the application perimeter, secures communications, prevents stored XSS, extracts sensitive values to environment variables, and adds request correlation and audit-grade security logging.

### 1. CSRF Strategy

Documented in [docs/csrf.md](docs/csrf.md).
- The `/api/**` filter chain is stateless and accepts Bearer tokens only. Since browsers never attach Bearer tokens to cross-origin requests, CSRF is impossible and `.csrf(csrf -> csrf.disable())` is safe.
- If a same-origin SPA with session cookies were introduced, CSRF would be configured using `CookieCsrfTokenRepository.withHttpOnlyFalse()` with an `X-XSRF-TOKEN` request header.

### 2. Single-Origin CORS Configuration

**`edu.itc.shop.config.CorsConfig`**:
- Dedicated `CorsConfigurationSource` registering `/api/**`.
- Allows origin `http://localhost:5173`.
- Allows methods `GET`, `POST`, `PUT`, `DELETE`.
- Allows headers `Authorization` and `Content-Type`.
- Exposes header `Location`.
- Max age: 3600 seconds (1 hour).
- Unauthorized origins (such as `http://evil.example`) receive no `Access-Control-Allow-Origin` header.

### 3. Security Headers

Configured in `SecurityConfig`:
- `X-Frame-Options: DENY` (clickjacking prevention).
- `Strict-Transport-Security: max-age=31536000 ; includeSubDomains` (enforces HTTPS for 1 year).
- `Content-Security-Policy: default-src 'self'; frame-ancestors 'none'; object-src 'none'`.
- `Referrer-Policy: strict-origin-when-cross-origin`.
- `X-Content-Type-Options: nosniff`.

### 4. Stored XSS Mitigation

- Domain model `edu.itc.shop.domain.Review` and repository `ReviewRepository`.
- `ReviewService` and `ReviewController` handling:
  - `POST /api/reviews` (authenticated) — creates a review with author from `Authentication.getName()`.
  - `GET /api/products/{id}/reviews` (public) — returns reviews as JSON with `Content-Type: application/json` and `nosniff`. The malicious `<img src=x onerror="...">` payload is safely escaped as JSON text by Jackson.
  - `GET /products/{id}/reviews.html` (public) — server-rendered Thymeleaf template using `th:text`.
- With `th:text`, Thymeleaf HTML-escapes `<` and `>` into `&lt;` and `&gt;`, rendering the payload as literal text and preventing execution.

### 5. Secrets Management

- Zero literal secrets committed in repository files: verified with `git grep -n -i -E "password|secret"`.
- `SecurityProps` validates `@NotBlank @Size(min = 32)` for `jwtSecret`.
- If `JWT_SECRET` is unset, the application fails fast at startup with a validation error.
- `.env.example` provides variable templates (`JWT_SECRET`, `SHOP_SEED_ADMIN_PASSWORD`, `SHOP_SEED_CUSTOMER_PASSWORD`) without secrets, and `.env` is ignored by `.gitignore`.

### 6. Security Event Logging and Request Correlation

- `edu.itc.shop.security.RequestIdFilter`: assigns a unique correlation ID (`rid`), attaches it to the MDC context, and returns it in the `X-Request-Id` response header.
- `application.yml` logging pattern includes `[rid=%X{rid}]`.
- `edu.itc.shop.security.SecurityEventLogger`: listens for:
  - `AuthenticationSuccessEvent` → `login.ok`
  - `AbstractAuthenticationFailureEvent` → `login.failed` (with bearer tokens redacted)
  - `AuthorizationDeniedEvent` → `access.denied`
- `SecurityConfig` registers `SpringAuthorizationEventPublisher` so method-level security denials publish events.

### Verification

Automated test suite covers all hardening features:
```bash
./mvnw clean test
```
Result: 30 tests pass with 0 failures, errors, or skipped tests. All transcripts and logs are documented in [security-session.md](security-session.md).

