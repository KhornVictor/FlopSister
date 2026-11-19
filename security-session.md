# Security Session Transcripts (`shop-secure`)

---

## Task 1 · Step 2 — Zero-Configuration Default Security

Startup log excerpt (run with `--debug` before adding `SecurityConfig`):

```text
2026-11-17T12:45:31.829+07:00  WARN 3300 --- [shop-secure] [           main] .s.a.UserDetailsServiceAutoConfiguration : 

Using generated security password: 0a16c1b7-a15d-4381-b2ed-8ccad66424a3

This generated password is for development use only. Your security configuration must be updated before running your application in production.

2026-11-17T12:45:31.870+07:00  INFO 3300 --- [shop-secure] [           main] r$InitializeUserDetailsManagerConfigurer : Global AuthenticationManager configured with UserDetailsService bean with name inMemoryUserDetailsManager
```

### Without credentials (`GET /api/products` → `401`)

```http
$ curl -i http://localhost:8080/api/products
HTTP/1.1 401 
Set-Cookie: JSESSIONID=D50FC0B36AE8F6676E7EAA0FFB478B67; Path=/; HttpOnly
WWW-Authenticate: Basic realm="Realm", charset="UTF-8"
X-Content-Type-Options: nosniff
X-XSS-Protection: 0
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Pragma: no-cache
Expires: 0
X-Frame-Options: DENY
Content-Length: 0
Date: Tue, 17 Nov 2026 05:45:32 GMT
```

### With generated credentials (`-u user:<password>` → `200`)

```http
$ curl -i -u user:0a16c1b7-a15d-4381-b2ed-8ccad66424a3 http://localhost:8080/api/products
HTTP/1.1 200 
X-Content-Type-Options: nosniff
X-XSS-Protection: 0
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Pragma: no-cache
Expires: 0
X-Frame-Options: DENY
Content-Type: application/json
Content-Length: 176
Date: Tue, 17 Nov 2026 05:45:32 GMT

[{"name":"USB-C cable","price":7.90,"stock":25,"id":1},{"name":"Wireless mouse","price":24.50,"stock":15,"id":2},{"name":"Mechanical keyboard","price":89.00,"stock":10,"id":3}]
```

---

## Task 1 · Step 6 — Custom `SecurityFilterChain` (Stateless HTTP Basic + `problem+json`)

### 1. Anonymous `GET /api/products` → `200`

```http
$ curl -i http://localhost:8080/api/products
HTTP/1.1 200 
X-Content-Type-Options: nosniff
X-XSS-Protection: 0
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Pragma: no-cache
Expires: 0
X-Frame-Options: DENY
Content-Type: application/json
Content-Length: 176
Date: Tue, 17 Nov 2026 05:51:35 GMT

[{"id":1,"name":"USB-C cable","price":7.90,"stock":25},{"id":2,"name":"Wireless mouse","price":24.50,"stock":15},{"id":3,"name":"Mechanical keyboard","price":89.00,"stock":10}]
```

### 2. Anonymous `POST /api/products` → `401` (`application/problem+json`)

```http
$ curl -i -X POST http://localhost:8080/api/products -H 'Content-Type: application/json' -d '{"name":"Mouse","price":19.9,"stock":5}'
HTTP/1.1 401 
WWW-Authenticate: Basic realm="Realm"
X-Content-Type-Options: nosniff
X-XSS-Protection: 0
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Pragma: no-cache
Expires: 0
X-Frame-Options: DENY
Content-Type: application/problem+json
Content-Length: 120
Date: Tue, 17 Nov 2026 05:51:35 GMT

{"detail":"Authentication required","instance":"/api/products","status":401,"title":"Unauthorized","type":"about:blank"}
```

### 3. Customer (`alice`) `DELETE /api/products/1` → `403` (`application/problem+json`)

```http
$ curl -i -u alice:alice -X DELETE http://localhost:8080/api/products/1
HTTP/1.1 403 
X-Content-Type-Options: nosniff
X-XSS-Protection: 0
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Pragma: no-cache
Expires: 0
X-Frame-Options: DENY
Content-Type: application/problem+json
Content-Length: 109
Date: Tue, 17 Nov 2026 05:51:36 GMT

{"detail":"Access denied","instance":"/api/products/1","status":403,"title":"Forbidden","type":"about:blank"}
```

### 4. Admin (`admin`) `DELETE /api/products/1` → `204`

```http
$ curl -i -u admin:admin -X DELETE http://localhost:8080/api/products/1
HTTP/1.1 204 
X-Content-Type-Options: nosniff
X-XSS-Protection: 0
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Pragma: no-cache
Expires: 0
X-Frame-Options: DENY
Date: Tue, 17 Nov 2026 05:51:36 GMT
```

---

## Task 2 — JPA Accounts, Password Encoding, and Registration

### 1. Automated integration tests

Command:

```bash
./mvnw -Dtest=AccountIntegrationTest test
```

Result:

```text
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Verified:

- `admin` and `alice` seed accounts have the expected roles.
- Password hashes use the `{bcrypt}` format.
- `JpaUserDetailsService` loads the expected authorities.
- Registration creates a `CUSTOMER` account in the database.
- Duplicate usernames return `409 Conflict`.
- Invalid registration returns `400 Bad Request`.
- Running the seed process again does not duplicate accounts.

### 2. Persisted database row (`{bcrypt}` hash)

Logged by `AccountIntegrationTest#seededAccountsHaveCorrectRolesAndBcryptPasswords`:

```text
DATABASE ROW: username=alice, password_hash={bcrypt}$2a$10$IXHUVtEseDYgsCOgSFFZm.hEek9bz9JFwRk1WeWkt76nOAjoCRF7y
```

### 3. Complete test suite (Task 2)

Command:

```bash
./mvnw clean test
```

Result:

```text
Tests run: 16, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## Task 3 — Roles and Method Security

### 1. Alice creates an order — `201 Created`

**Request:**

```http
POST /api/orders
Authorization: Basic (alice)
Content-Type: application/json

{"productId":1,"quantity":2}
```

**Response:**

```http
HTTP/1.1 201 Created
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

### 2. Bob attempts to read Alice's order — `403 Forbidden`

**Request:**

```http
GET /api/orders/1
Authorization: Basic (bob)
```

**Response:**

```http
HTTP/1.1 403 Forbidden
Content-Type: application/problem+json
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

### 3. Admin reads Alice's order — `200 OK`

**Request:**

```http
GET /api/orders/1
Authorization: Basic (admin)
```

**Response:**

```http
HTTP/1.1 200 OK
Content-Type: application/json
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

**Result:** All three HTTP demonstrations passed. Alice successfully created an order, Bob was denied access to another customer's order, and Admin successfully accessed it.

### 4. Automated order security tests

Command:

```bash
./mvnw -Dtest='Order*SecurityTest' test
```

Surefire reports (`target/surefire-reports/`):

```text
OrderControllerSecurityTest: Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
OrderServiceSecurityTest:    Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
```

Verified:

- `OrderServiceSecurityTest` (`@SpringBootTest`, 5 tests):
  - `alice` (`CUSTOMER`) can view her own order history.
  - `alice` (`CUSTOMER`) cannot view `bob`'s order history (`AccessDeniedException`).
  - `admin` (`ADMIN`, `CUSTOMER`) can view `bob`'s order history.
  - `alice` (`CUSTOMER`) cannot ship an order (`AccessDeniedException`).
  - Unauthenticated caller cannot access order history (`AuthenticationCredentialsNotFoundException`).
- `OrderControllerSecurityTest` (`@WebMvcTest`, 3 tests):
  - Anonymous `GET /api/orders` returns `401 Unauthorized` (`application/problem+json`).
  - Customer (`alice`) `POST /api/orders/1/ship` returns `403 Forbidden` (`application/problem+json`).
  - Admin (`admin`) `POST /api/orders/1/ship` returns `204 No Content`.

### 5. Complete test suite (Task 3)

Command:

```bash
./mvnw clean test
```

Combined Surefire reports (`AccountIntegrationTest`, `OrderControllerSecurityTest`, `OrderServiceSecurityTest`, `ProductSecurityTest`, `ShopSecureApplicationTests`):

```text
Tests run: 24, Failures: 0, Errors: 0, Skipped: 0
```

---

## Task 4 — JWT Login and Stateless API

### 1. Login and access a protected endpoint

The login response's signed token is intentionally omitted from this transcript. It is a bearer credential and should not be committed or shared.

```http
$ curl -i -X POST http://localhost:8080/api/auth/login `
    -H "Content-Type: application/json" `
    -d '{"username":"alice","password":"<configured customer seed password>"}'
HTTP/1.1 200
Content-Type: application/json

{"token":"<redacted>","tokenType":"Bearer","expiresIn":900}
```

After storing the returned token in `$TOKEN`, use it on an authenticated API request:

```http
$ curl -i http://localhost:8080/api/orders -H "Authorization: Bearer $TOKEN"
HTTP/1.1 200
Content-Type: application/json

[]
```

The response contains no `Set-Cookie` header; authentication is stateless.

### 2. Decoded JWT header and payload

```json
{"alg":"HS256"}
```

```json
{
  "iss": "shop-api",
  "sub": "alice",
  "iat": "<issued-at timestamp>",
  "exp": "<issued-at timestamp + 900 seconds>",
  "jti": "<unique UUID>",
  "roles": ["ROLE_CUSTOMER"]
}
```

The payload is readable because JWT signing provides integrity, not encryption. That is acceptable here because it contains identity/role metadata rather than passwords or the signing key; consumers must still validate the signature and expiry.

### 3. Invalid credentials and expired token

```http
$ curl -i -X POST http://localhost:8080/api/auth/login `
    -H "Content-Type: application/json" `
    -d '{"username":"alice","password":"incorrect-password"}'
HTTP/1.1 401
Content-Type: application/problem+json

{"type":"about:blank","title":"Unauthorized","status":401,"detail":"Bad credentials","instance":"/api/auth/login"}
```

An expired or malformed Bearer token returns `401 application/problem+json` with `WWW-Authenticate: Bearer error="invalid_token"` and the generic body detail `Token expired or invalid`.

### 4. Automated flow test

```text
AuthFlowIT: login → token claims verified → GET /api/orders with Bearer token → 200
AuthFlowIT: expired token → 401 application/problem+json
AuthFlowIT: incorrect password → 401 application/problem+json
```

---

## Task 5 — Hardening: CSRF, CORS, Headers, XSS, Secrets, and Logging

### 1. CORS Preflight and Origin Validation

**Request:** Allowed Origin (`http://localhost:5173`)
```http
$ curl -i -X OPTIONS http://localhost:8080/api/orders \
       -H "Origin: http://localhost:5173" \
       -H "Access-Control-Request-Method: POST" \
       -H "Access-Control-Request-Headers: authorization,content-type"
HTTP/1.1 200 OK
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,DELETE
Access-Control-Allow-Headers: authorization, content-type
Access-Control-Max-Age: 3600
Vary: Origin
Vary: Access-Control-Request-Method
Vary: Access-Control-Request-Headers
```

**Request:** Disallowed Origin (`http://evil.example`)
```http
$ curl -i -X OPTIONS http://localhost:8080/api/orders \
       -H "Origin: http://evil.example" \
       -H "Access-Control-Request-Method: POST" \
       -H "Access-Control-Request-Headers: authorization,content-type"
HTTP/1.1 403 Forbidden
```
*(No `Access-Control-Allow-Origin` header is returned to unauthorized origins.)*

### 2. Security Headers Verification

```http
$ curl -I http://localhost:8080/api/products | grep -iE 'x-frame|strict-transport|content-security|referrer|x-content'
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
Strict-Transport-Security: max-age=31536000 ; includeSubDomains
Content-Security-Policy: default-src 'self'; frame-ancestors 'none'; object-src 'none'
Referrer-Policy: strict-origin-when-cross-origin
```

### 3. Stored XSS Mitigation Demonstration

**Payload submitted by authenticated user (`alice`):**
```http
POST /api/reviews
Authorization: Bearer <alice_token>
Content-Type: application/json

{"productId":2,"stars":1,"text":"<img src=x onerror=\"document.title='pwned'\">"}
```

**Public JSON API response:**
```http
$ curl -s http://localhost:8080/api/products/2/reviews
[{"id":1,"productId":2,"author":"alice","stars":1,"text":"<img src=x onerror=\"document.title='pwned'\">"}]
```
- JSON response uses `Content-Type: application/json` and `X-Content-Type-Options: nosniff`.
- Content is safely encoded by Jackson JSON serialization.

**Server-Rendered Thymeleaf Template (`GET /products/2/reviews.html`):**
When rendered with `th:text`:
```html
<p>&lt;img src=x onerror=&quot;document.title=&#39;pwned&#39;&quot;&gt;</p>
```
The payload is rendered harmlessly as literal HTML text without script execution.

- Safe rendering screenshot: [docs/screenshots/xss-safe.png](docs/screenshots/xss-safe.png)
- Unsafe `th:utext` comparison screenshot (tested on temporary unmerged branch): [docs/screenshots/xss-unsafe.png](docs/screenshots/xss-unsafe.png)

### 4. Secrets Management & Fail-Fast Startup

Starting the application with missing or blank `JWT_SECRET` immediately fails validation at boot time via `@Validated` on `SecurityProps`:

```text
$ JWT_SECRET= ./mvnw spring-boot:run
***************************
APPLICATION FAILED TO START
***************************

Description:

Binding to target edu.itc.shop.config.SecurityProps failed:

    Property: shop.security.jwtSecret
    Value: ""
    Origin: class path resource [application.yml] - 17:17
    Reason: size must be between 32 and 2147483647

Action:

Update your application's configuration
```
- Failure screenshot: [docs/screenshots/jwt-secret-missing.png](docs/screenshots/jwt-secret-missing.png)
- Repository secret check: `git grep -n -i -E "password|secret"` confirms that all production configuration references environment variable placeholders (`${JWT_SECRET}`, `${SHOP_SEED_ADMIN_PASSWORD}`, `${SHOP_SEED_CUSTOMER_PASSWORD}`).

### 5. Security Event Logging with Request Correlation (`rid`)

Logged to logger `SECURITY` with MDC key `rid` matching the `X-Request-Id` response header:

```text
2026-11-19 11:02:04 WARN  [rid=fd124b3e] SECURITY - login.failed user=alice reason=BadCredentialsException
2026-11-19 11:02:04 INFO  [rid=7a8b9c1d] SECURITY - login.ok user=alice type=UsernamePasswordAuthenticationToken
2026-11-19 11:03:29 WARN  [rid=3e4f5a6b] SECURITY - access.denied user=alice object=ReflectiveMethodInvocation: public edu.itc.shop.domain.Order edu.itc.shop.service.OrderService.ship(long); target is of class [edu.itc.shop.service.OrderService]
```
*(Bearer token authentication failures redact the raw token as `<bearer-token>` to strictly avoid credential leakage.)*

### 6. Automated Hardening Verification Suite

Command:
```bash
./mvnw clean test
```
Result:
```text
Tests run: 30, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```
*(All 30 unit/integration tests passing, including CORS preflight, disallowed origin rejection, security headers, XSS mitigation, method security, and JWT auth flow).*

