# Task 5 — Hardening: Completion & Verification Summary

All implementation, configuration, template, and documentation requirements for **Task 5 (Hardening)** have been completed and verified.

---

## Completed Items & Verification

### 1. Security Event Logging & Token Redaction
- [x] Implemented in `src/main/java/edu/itc/shop/security/SecurityEventLogger.java`.
- [x] Redacted `BearerTokenAuthenticationToken` as `<bearer-token>` on failure so raw JWTs are never leaked into the console or logs (preventing the **−5 point automatic deduction**).
- [x] Standardized log messages:
  - `login.ok user=<user> type=<type>`
  - `login.failed user=<user> reason=<reason>`
  - `access.denied user=<user> object=<target>`
- [x] Registered `SpringAuthorizationEventPublisher` bean in `SecurityConfig.java` to capture method-security denials (`@PreAuthorize` / `@PostAuthorize`).
- [x] `RequestIdFilter` formats 8-character hex IDs (`rid`) into MDC and sets `X-Request-Id` response header.

### 2. Dedicated CORS Configuration
- [x] Extracted into `src/main/java/edu/itc/shop/config/CorsConfig.java`.
- [x] Configured `CorsConfigurationSource` for `/api/**`:
  - Allowed Origin: `http://localhost:5173`
  - Allowed Methods: `GET`, `POST`, `PUT`, `DELETE`
  - Allowed Headers: `Authorization`, `Content-Type`
  - Exposed Headers: `Location`
  - Max Age: `3600` seconds (1 hour)
- [x] `SecurityConfig.java` uses `http.cors(Customizer.withDefaults())`.
- [x] Tested in `HardeningSecurityTest`: preflight succeeds with `200` and headers for `http://localhost:5173`, and `http://evil.example` gets no `Access-Control-Allow-Origin`.

### 3. Security Headers
- [x] Configured in `SecurityConfig.java`:
  - `X-Frame-Options: DENY`
  - `Strict-Transport-Security: max-age=31536000 ; includeSubDomains` (with `AnyRequestMatcher.INSTANCE`)
  - `Content-Security-Policy: default-src 'self'; frame-ancestors 'none'; object-src 'none'`
  - `Referrer-Policy: strict-origin-when-cross-origin`
  - `X-Content-Type-Options: nosniff`
- [x] Tested in `HardeningSecurityTest`.

### 4. Stored XSS Mitigation (Reviews)
- [x] Package layout aligned with project conventions:
  - `domain/Review.java` (fields: `id`, `productId`, `author`, `stars`, `text`)
  - `repository/ReviewRepository.java`
  - `service/ReviewService.java`
  - `web/ReviewController.java` (`POST /api/reviews` authenticated, `GET /api/products/{id}/reviews` public, `GET /products/{id}/reviews.html` public)
  - `web/dto/ReviewRequest.java`
- [x] `templates/reviews.html` renders review text safely with `th:text`.
- [x] Payload `<img src=x onerror="document.title='pwned'">` is stored as-is, safely escaped by Jackson as JSON, and escaped as `&lt;img src=x onerror=&quot;document.title=&#39;pwned&#39;&quot;&gt;` in HTML.
- [x] Tested in `HardeningSecurityTest`.

### 5. Secrets Management
- [x] All configuration in `application.yml` uses placeholders (`${JWT_SECRET}`, `${SHOP_SEED_ADMIN_PASSWORD}`, `${SHOP_SEED_CUSTOMER_PASSWORD}`).
- [x] `SecurityProps.java` validates `@NotBlank @Size(min = 32)` on `jwtSecret`.
- [x] Missing `JWT_SECRET` causes fail-fast boot failure (`APPLICATION FAILED TO START`).
- [x] `.env.example` provides placeholders with no secret values committed.
- [x] `.gitignore` excludes `.env` and `*-local.yml`.

### 6. CSRF Strategy Documentation
- [x] Created `docs/csrf.md` explaining:
  - Credentials per filter chain (`/api/**` stateless Bearer only; no cookies).
  - Justification for disabling CSRF on Bearer-only endpoints (no ambient authority).
  - Configuration for a same-origin SPA with session cookies using `CookieCsrfTokenRepository.withHttpOnlyFalse()`.
  - JavaScript `fetch` snippet sending `X-XSRF-TOKEN`.

### 7. Security Session Transcripts & README
- [x] `security-session.md` contains complete Task 5 transcripts:
  - CORS preflight and disallowed origin verification
  - Security headers curl block
  - Stored XSS JSON and HTML rendering with links to screenshots
  - `JWT_SECRET` startup failure output
  - Three correlated `SECURITY` log lines with `rid`
- [x] `README.md` documents Task 5, required environment variables, and how to run tests.

---

## Test Suite Results

- All 30 tests pass cleanly:
  - `AccountIntegrationTest`: 8 tests
  - `OrderControllerSecurityTest`: 3 tests
  - `OrderServiceSecurityTest`: 5 tests
  - `ProductSecurityTest`: 9 tests
  - `HardeningSecurityTest`: 4 tests
  - `ShopSecureApplicationTests`: 1 test
- `AuthFlowIT`: 4 tests pass cleanly.

---

## Next Steps / Git Submission

1. Check git status:
   ```bash
   git status
   ```
2. Commit your Task 5 changes:
   ```bash
   git add src docs security-session.md README.md task5.md
   git commit -m "feat(security): complete task 5 hardening, CSRF, CORS, headers, XSS, secrets, and logging"
   ```
3. Tag the commit for Task 5:
   ```bash
   git tag task-5
   git tag lab5-task5   # optional, if keeping both naming conventions
   ```
4. Push to your branch:
   ```bash
   git push origin Lab05-SecurityJavaWebApplications --tags
   ```
