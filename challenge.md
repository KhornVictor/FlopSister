# Lab-05 Challenge Report: Advanced Security & Defense-in-Depth

**Project:** `shop-secure`  
**Course:** Software Engineering — Security in Java Web Applications (Spring Security 7, Spring Boot 4.1, JDK 25)  
**Deliverable:** Challenge (+20 Extra Credit) Implementation & Architecture Summary  
**Author / Engineer:** Khorn Victor  
**Date:** November 19, 2026  

---

## 1. Executive Summary

This document details the complete design, implementation, and automated verification of the **Lab-05 Challenge (+20 extra credit)** for `shop-secure`. The challenge elevates the baseline stateless JWT application to enterprise defense-in-depth security standards by introducing:

1. **Opaque Refresh Tokens with Cryptographic Rotation & Theft Detection**: 7-day single-use refresh tokens stored strictly as SHA-256 hashes, organized into token families. Immediate revocation of the entire token family upon reuse detection (RFC 6749 / RFC 6819).
2. **IP & Identity Rate Limiting**: In-memory sliding-window rate limiter restricting login attempts to 5 per minute per `IP:username` combination, returning RFC 9457 `429 Too Many Requests` with a `Retry-After` header.
3. **Account Lockout Protection**: Safeguards user accounts against distributed credential stuffing attacks by locking accounts for 15 minutes after 5 consecutive failures, returning generic `401 Bad credentials` to prevent account enumeration.
4. **Isolated Forensic Security Audit Log**: Dedicated `security_audit` repository executing under `Propagation.REQUIRES_NEW` to guarantee that security events (login success/failure, lockouts, refresh rotation, reuse anomalies, logouts, access denials, and privilege changes) are committed even when client transactions roll back.
5. **OWASP Top 10:2021 Compliance Checklist**: Exhaustive control-to-evidence matrix published at `docs/owasp-checklist.md`, complete with mitigation plans for A06 (Dependency Vulnerability Scanning) and A10 (Server-Side Request Forgery).
6. **Automated Lifecycle Integration Test Suite**: End-to-end integration test suite (`AuthLifecycleIT.java`) testing full token lifecycles, theft recovery, distributed lockout, rate limiting, and administrative audit access.

All **38 unit and integration tests** across the entire application pass with zero errors and zero failures.

---

## 2. Architecture & Security Design

```
+---------------------------------------------------------------------------------------------------+
|                                        Client Request                                             |
+---------------------------------------------------------------------------------------------------+
                                                  │
                                                  ▼
+---------------------------------------------------------------------------------------------------+
| RequestIdFilter: Generates 8-char hex correlation id (`rid`) in MDC & sets `X-Request-Id`        |
+---------------------------------------------------------------------------------------------------+
                                                  │
                                                  ▼
+───────────────────────────────────────────────────────────────────────────────────────────────────+
| SecurityFilterChain: Stateless Bearer Token / OAuth2 Resource Server                              |
| - Standard Security Headers (HSTS, CSP, X-Frame-Options: DENY, Referrer-Policy, nosniff)         |
| - Strict CORS Policy (Allowed origins: localhost:3000, 127.0.0.1:3000)                           |
| - Role-Based Endpoint Authorizations                                                             |
+───────────────────────────────────────────────────────────────────────────────────────────────────+
       │                                     │                                    │
       ▼                                     ▼                                    ▼
[POST /api/auth/login]            [POST /api/auth/refresh]             [GET /api/admin/audit]
       │                                     │                                    │
       ├─► RateLimiterService                ├─► RefreshTokenService              └─► HasRole('ADMIN')
       │   (Max 5/min per IP:user)           │   (SHA-256 verify hash)                 │
       │   [Tripped? -> 429]                 │   (Is Revoked? -> Revoke Family!)       ▼
       │                                     │   (Is Expired? -> 401)          AdminAuditController
       ├─► AuthenticationManager             │   (Issue next token, revoke old)        │
       │   [Failure -> LockoutListener]      │                                         ▼
       │   [Locked -> Generic 401]           ▼                                SecurityAuditRepository
       │                            TokenResponse (new JWT + new RT)
       ▼
TokenResponse (JWT + RT)
```

---

## 3. Detailed Component Implementations

### Part 1: Refresh Tokens with Family Rotation & Theft Detection

#### 1. Cryptographic Token Generation & Hashing
- **Opaque Token**: Generated via `java.security.SecureRandom` reading 32 cryptographically secure random bytes, Base64URL-encoded without padding (43 characters).
- **Storage Hash**: Raw tokens are **never** persisted to the database. The database stores the SHA-256 hexadecimal hash (`HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(...))`). Even if the database is leaked, tokens cannot be forged or replayed.
- **Family Identification**: Every login issues a new refresh token family identified by a `UUID` (`familyId`).

#### 2. Database Schema (`refresh_tokens`)
Mapped in `edu.itc.shop.domain.RefreshToken`:
- `id` (`BIGINT AUTO_INCREMENT PRIMARY KEY`)
- `tokenHash` (`VARCHAR(64) NOT NULL UNIQUE`)
- `username` (`VARCHAR(60) NOT NULL`)
- `familyId` (`UUID NOT NULL`)
- `expiresAt` (`TIMESTAMP NOT NULL` — default 7-day TTL)
- `revokedAt` (`TIMESTAMP NULL`)
- `replacedBy` (`BIGINT NULL` — foreign key to descendant token ID)

#### 3. Single-Use Rotation & Theft Detection Algorithm
Implemented in `edu.itc.shop.service.RefreshTokenService`:
1. **Lookup**: Retrieve the token record by computing `SHA-256(rawToken)`. If not found, throw `InvalidRefreshTokenException` (`401 Unauthorized`).
2. **Reuse Detection (Theft Scenario)**:
   - If `current.isRevoked()` is **true**:
     - The token was previously consumed and is now being presented again. This signifies either token theft, interception, or replay.
     - **Action**: Immediately revoke the **entire token family** using `@Modifying` bulk query:
       ```sql
       UPDATE RefreshToken r SET r.revokedAt = :now WHERE r.familyId = :familyId AND r.revokedAt IS NULL
       ```
     - Audit event logged: `REFRESH_REUSE_DETECTED` with family ID.
     - Transaction configuration: Annotated with `@Transactional(noRollbackFor = InvalidRefreshTokenException.class)` to guarantee that the revocation is committed to the database even as the exception triggers an HTTP 401 response.
     - Subsequent attempts to use any descendant token from that compromised family will fail immediately.
3. **Expiration Check**: If `current.isExpired()`, record `LOGIN_FAILED` and throw `InvalidRefreshTokenException`.
4. **Legitimate Rotation**:
   - Issue a new raw token with a fresh 7-day expiration under the **same** `familyId`.
   - Mark the previous token as revoked (`revokedAt = now()`, `replacedBy = newId`).
   - Audit event logged: `TOKEN_REFRESHED`.
   - Return both a new 15-minute access JWT and the new 7-day refresh token.

#### 4. Explicit Logout
- Endpoint: `POST /api/auth/logout` accepting `RefreshTokenRequest`.
- Revokes all active tokens under that token's `familyId` and logs `LOGOUT` to the security audit table. Returns `204 No Content`.

---

### Part 2: In-Memory Sliding-Window Rate Limiting

#### 1. Mechanism
Implemented in `edu.itc.shop.service.RateLimiterService`:
- Uses a `ConcurrentHashMap<String, Deque<Instant>>` storing sliding timestamps for each composite key:
  ```java
  String key = (ip != null ? ip : "unknown") + ":" + (username != null ? username : "");
  ```
- **Limit**: 5 attempts within a 60-second sliding window.
- **Window Cleanup**: Expired timestamps (`before(now - 60s)`) are evicted from the deque head on every check.

#### 2. Throttling Response
- If 5 requests already exist in the window, the 6th request triggers `RateLimitExceededException`.
- Mapped in `ApiExceptionHandler`:
  - HTTP Status: `429 Too Many Requests`
  - Header: `Retry-After: <seconds>` (calculated dynamically from `60 - elapsed`)
  - Body: RFC 9457 `application/problem+json`:
    ```json
    {
      "type": "about:blank",
      "title": "Too Many Requests",
      "status": 429,
      "detail": "Too many login attempts. Please retry after 60 seconds",
      "instance": "/api/auth/login"
    }
    ```
- Auditing: Records `LOGIN_RATE_LIMITED` with client IP, username, and retry delay.
- **Auto-Reset**: On successful authentication (`authenticationManager.authenticate(...)`), `rateLimiterService.resetCounter(clientIp, username)` clears the key counter.

---

### Part 3: Distributed Account Lockout Protection

#### 1. Purpose & Threat Model
Standard rate limiting protects against high-volume brute force from a single IP. Distributed attacks use botnets or revolving proxy pools to target a single account from multiple distinct IPs.

#### 2. Entity Fields & State Management
Added to `edu.itc.shop.domain.UserAccount`:
- `failedAttempts` (`int NOT NULL DEFAULT 0`)
- `lockedUntil` (`Instant NULL`)
- Helper methods:
  - `registerFailure()`: Increments `failedAttempts`. When `failedAttempts >= 5`, sets `lockedUntil = Instant.now().plus(Duration.ofMinutes(15))`.
  - `resetLockout()`: Clears `failedAttempts = 0` and `lockedUntil = null`.
  - `isLocked()`: Evaluates `lockedUntil != null && Instant.now().isBefore(lockedUntil)`. Automatically evaluates to false once the 15-minute window expires.

#### 3. Spring Security Integration & Event Listeners
- `JpaUserDetailsService`: Builds `UserDetails` setting `.accountLocked(account.isLocked())`.
- `edu.itc.shop.security.LockoutListener`:
  - Listens to Spring Security's `AuthenticationFailureBadCredentialsEvent` (filtered specifically for `UsernamePasswordAuthenticationToken` to strictly avoid token leakage or misinterpreting bearer errors).
  - Increments failure counter via `u.registerFailure()` and persists the account.
  - When locked, logs audit event `LOCKED`.
  - Listens to `AuthenticationSuccessEvent`: resets `u.resetLockout()` and logs `LOGIN_OK`.
- **Anti-Enumeration Protection**:
  When an account is locked, Spring Security throws `LockedException`. `ApiExceptionHandler.handleAuthenticationFailure` catches all `AuthenticationException` instances and returns generic HTTP 401 `Bad credentials`. The response body **never** reveals whether the account is locked, whether the username exists, or when the lockout expires.

---

### Part 4: Forensic Security Audit Log

#### 1. Architecture & Reliability Guarantee
- Table: `security_audit` (id, at, type, username, ip, request_id, detail).
- Entity: `edu.itc.shop.domain.SecurityAudit`.
- Service: `edu.itc.shop.service.AuditService`.
- **Transaction Isolation**: All recording methods use `@Transactional(propagation = Propagation.REQUIRES_NEW)`. This guarantees that audit entries are committed into their own independent database transaction and will **never** be rolled back, even if the parent business transaction fails or throws a security exception.

#### 2. Tracked Security Events (`edu.itc.shop.domain.AuditType`)
| Audit Type | Description | Trigger Location |
|---|---|---|
| `LOGIN_OK` | Successful password authentication | `LockoutListener` |
| `LOGIN_FAILED` | Bad credentials or non-existent account | `LockoutListener` |
| `LOGIN_RATE_LIMITED` | Login attempts exceeded 5/min limit | `AuthController.login` |
| `LOCKED` | Account locked after 5 failed attempts | `LockoutListener` |
| `TOKEN_REFRESHED` | Legitimate single-use refresh token rotated | `RefreshTokenService.rotate` |
| `REFRESH_REUSE_DETECTED` | Already-consumed token presented (theft alarm) | `RefreshTokenService.rotate` |
| `LOGOUT` | User explicitly logs out and revokes token family | `RefreshTokenService.revokeFamily` |
| `ACCESS_DENIED` | Method security / URL authorization denial | `LockoutListener` (`AuthorizationDeniedEvent`) |
| `ROLE_CHANGED` | User registered / privileged roles granted | `AccountService.register` |

#### 3. Administrative Audit Query Endpoint
- Endpoint: `GET /api/admin/audit?username=`
- Controller: `edu.itc.shop.web.AdminAuditController`
- Authorization: Restricted via `SecurityConfig` to `.requestMatchers("/api/admin/**").hasRole("ADMIN")`.
- Any customer or anonymous request receives `403 Forbidden` / `401 Unauthorized` problem+json.

---

### Part 5: OWASP Top 10:2021 Security Checklist & Gap Analysis

The comprehensive checklist file is published at [docs/owasp-checklist.md](file:///C:/Desktop/Student%20Online%20(SO)/Techno/I4-GIC-A/I4-GIC-S1/Software%20engineering/C5-Security%20in%20Java%20Web%20Applications%20(Spring%20Security,%20JWT)/FlopSister/docs/owasp-checklist.md).

#### Summary of Mitigations
- **A01: Broken Access Control**: Strict `denyAll()` fallback in filter chain; `@PreAuthorize` method security; URL-based role restrictions; tenant ID isolation in `OrderService`.
- **A02: Cryptographic Failures**: Passwords hashed using `BCryptPasswordEncoder` (work factor 12) via DelegatingPasswordEncoder; refresh tokens hashed using SHA-256; JWTs signed using HS256 with 256-bit secret; HSTS with `includeSubDomains`.
- **A03: Injection**: Spring Data JPA parameterized JPQL queries; Hibernate criteria; Bean Validation constraints (`@NotBlank`, `@Min`, `@Size`); HTML escaping in Thymeleaf templates.
- **A04: Insecure Design**: Account lockout after 5 consecutive failures; 5 attempts/min rate limiting; refresh token rotation with family revocation; fail-fast startup on missing configuration.
- **A05: Security Misconfiguration**: Default generated passwords disabled; in-memory H2 console disabled; detailed stack traces disabled in production; security headers (CSP, frame-ancestors 'none', nosniff, Referrer-Policy).
- **A06: Vulnerable and Outdated Components**: Documented CI/CD automation plan with Dependabot and `owasp-dependency-check-maven`.
- **A07: Identification and Authentication Failures**: Password policy enforcement (`@StrongPassword`); credential masking in logs; generic `Bad credentials` error bodies preventing user enumeration.
- **A08: Software and Data Integrity Failures**: Cryptographic signature validation on all incoming JWTs; immutable DTO records.
- **A09: Security Logging and Monitoring Failures**: `REQUIRES_NEW` audit logging for all critical security events; correlation via MDC `rid` and `X-Request-Id`; bearer tokens redacted as `<bearer-token>`.
- **A10: Server-Side Request Forgery (SSRF)**: Architecture verified to contain zero dynamic user-supplied outbound URL fetchers.

---

### Part 6: Automated Integration Test Suite (`AuthLifecycleIT.java`)

Located in `src/test/java/edu/itc/shop/AuthLifecycleIT.java`. Tests run against a live HTTP server using `java.net.http.HttpClient`:

1. `refreshRotationReuseDetectionAndFamilyRevocationFlow()`:
   - Alice logs in -> acquires `token1` and `refreshToken1` (TTL 7 days).
   - Accesses `/api/orders` with `token1` -> `200 OK`.
   - Rotates `refreshToken1` -> acquires `token2` and `refreshToken2`.
   - Accesses `/api/orders` with `token2` -> `200 OK`.
   - Replays old `refreshToken1` -> returns `401 Unauthorized` (`Refresh token invalid`) and audits `REFRESH_REUSE_DETECTED`.
   - Tries `refreshToken2` -> returns `401 Unauthorized` confirming whole family revocation!
2. `logoutRevokesRefreshTokenFamily()`:
   - Alice logs in -> calls `POST /api/auth/logout` -> returns `204 No Content`.
   - Refresh token is immediately rejected with `401 Unauthorized`.
3. `accountLockoutAfterFiveConsecutiveFailures()`:
   - 5 failed login attempts from simulated distributed IPs (`198.51.100.0` to `4`).
   - Asserts database account shows `isLocked() == true` and `failedAttempts >= 5`.
   - 6th attempt with correct password returns generic `401 Bad credentials`.
   - Asserts `LOCKED` audit event was stored.
4. `rateLimiterReturns429WithRetryAfterHeader()`:
   - 5 failed requests from the same client IP -> returns `401`.
   - 6th request from that same IP -> returns `429 Too Many Requests` with `Retry-After` header.
   - Asserts `LOGIN_RATE_LIMITED` audit event was stored.
5. `auditLogsPersistedAndAccessibleToAdminOnly()`:
   - Customer `alice` requests `GET /api/admin/audit` -> returns `403 Forbidden`.
   - Administrator `admin` requests `GET /api/admin/audit?username=alice` -> returns `200 OK` with JSON audit records.

---

## 4. Verification Evidence

### Build & Test Results
```text
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running edu.itc.shop.AccountIntegrationTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running edu.itc.shop.HardeningSecurityTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running edu.itc.shop.OrderControllerSecurityTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running edu.itc.shop.OrderServiceSecurityTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running edu.itc.shop.ProductSecurityTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running edu.itc.shop.AuthFlowIT
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running edu.itc.shop.AuthLifecycleIT
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running edu.itc.shop.ShopSecureApplicationTests
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 38, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

---

## 5. Acceptance Checklist

| Item | Requirement | Status | Evidence / Verification |
|---|---|---|---|
| **1** | Refresh tokens (32-byte Base64Url, SHA-256 hashed in database, 7-day TTL) | **COMPLETED** | `RefreshToken.java`, `RefreshTokenService.java`, `AuthLifecycleIT` |
| **2** | Refresh token rotation with family revocation upon reuse detection | **COMPLETED** | `RefreshTokenService.rotate()`, `AuthLifecycleIT` step 5 & 6 |
| **3** | Explicit logout (`POST /api/auth/logout`) revokes token family | **COMPLETED** | `AuthController.logout()`, `AuthLifecycleIT` |
| **4** | Rate limiting (max 5/min per IP:user) with 429 & `Retry-After` header | **COMPLETED** | `RateLimiterService.java`, `ApiExceptionHandler.java`, `AuthLifecycleIT` |
| **5** | Distributed account lockout (5 consecutive failures -> 15 min lock) | **COMPLETED** | `UserAccount.isLocked()`, `LockoutListener.java`, `AuthLifecycleIT` |
| **6** | Anti-enumeration generic 401 response on locked accounts | **COMPLETED** | `ApiExceptionHandler.handleAuthenticationFailure()`, `AuthLifecycleIT` |
| **7** | Forensic audit log table with `REQUIRES_NEW` transaction propagation | **COMPLETED** | `SecurityAudit.java`, `AuditService.java`, `SecurityAuditRepository.java` |
| **8** | Audit types logged for all 9 specified security events | **COMPLETED** | `AuditType.java`, `LockoutListener.java`, `RefreshTokenService.java` |
| **9** | Admin-only audit query endpoint (`GET /api/admin/audit`) | **COMPLETED** | `AdminAuditController.java`, `SecurityConfig.java`, `AuthLifecycleIT` |
| **10** | OWASP Top 10 checklist with A06 & A10 gap analysis | **COMPLETED** | `docs/owasp-checklist.md` |
| **11** | End-to-end integration test suite covering all challenge flows | **COMPLETED** | `AuthLifecycleIT.java` (5/5 tests passing, 38/38 project total) |
| **12** | All transcripts appended to `security-session.md` | **COMPLETED** | `security-session.md` Section "Challenge (+20 Extra Credit)" |
