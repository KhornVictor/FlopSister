# Lab-05 (`shop-secure`) — Task Paths (Tree Structure)

---

## Current Workspace (Before Task 0)

```text
shop-secure/
├── .gitattributes
├── .gitignore                                  # lacks .env and *-local.yml
├── .mvn/wrapper/
├── HELP.md
├── PATHS.md
├── README.md                                   # currently empty
├── mvnw
├── mvnw.cmd
├── pom.xml                                     # Boot 4.1.2, Java 25; starter + starter-test only
└── src/
    ├── main/
    │   ├── java/edu/itc/se/shop_secure/
    │   │   └── ShopSecureApplication.java      # Lab-05 uses edu/itc/shop/
    │   └── resources/
    │       └── application.properties          # Lab-05 uses application.yml
    └── test/
        └── java/edu/itc/se/shop_secure/
            └── ShopSecureApplicationTests.java # Lab-05 uses edu/itc/shop/
```

---

## Task 0 · Setup

```text
shop-secure/
├── .gitignore                                  [modify] exclude target/, .env, *-local.yml
├── pom.xml                                     [modify] webmvc, security, oauth2-resource-server, data-jpa, h2, validation, spring-security-test
└── src/
    ├── main/
    │   ├── java/edu/itc/shop/
    │   │   ├── ShopSecureApplication.java      [move] from edu/itc/se/shop_secure/
    │   │   ├── config/                         [package]
    │   │   ├── security/                       [package]
    │   │   ├── web/
    │   │   │   └── dto/                        [package]
    │   │   ├── service/                        [package]
    │   │   ├── repository/                     [package]
    │   │   └── domain/                         [package]
    │   └── resources/
    │       └── application.yml                 [create] jdbc:h2:mem:shop, ddl-auto=update, h2.console.enabled=false
    └── test/
        └── java/edu/itc/shop/
            └── ShopSecureApplicationTests.java [move] from edu/itc/se/shop_secure/
```

---

## Task 1 · Default Security & First Filter Chain (`git tag task-1`)

```text
shop-secure/
├── README.md                                   [modify] quote generated password + explain UserDetailsServiceAutoConfiguration
├── security-session.md                         [create] default password + Task 1 curl transcripts (200/401/403/204)
└── src/main/java/edu/itc/shop/
    ├── ShopSecureApplication.java              [modify] CommandLineRunner seeding 3 products
    ├── config/
    │   └── SecurityConfig.java                 [create] SecurityFilterChain + temp InMemoryUserDetailsManager + 401/403 problem+json
    ├── domain/
    │   └── Product.java                        [create] id, name, price, stock
    ├── repository/
    │   └── ProductRepository.java              [create]
    ├── service/
    │   └── ProductService.java                 [create]
    └── web/
        ├── ApiExceptionHandler.java            [create] @RestControllerAdvice ProblemDetail
        ├── ProductController.java              [create]
        └── dto/                                [optional product DTOs]

Endpoints:
/api/products
├── GET    /api/products                        (permitAll)
├── GET    /api/products/{id}                   (permitAll)
├── POST   /api/products                        (authenticated)
└── DELETE /api/products/{id}                   (hasRole("ADMIN"))
```

---

## Task 2 · Users and Passwords in the Database (`git tag task-2`)

```text
shop-secure/
├── security-session.md                         [modify] registration, /api/me, H2 users row with {bcrypt}$2a$10$...
└── src/main/
    ├── java/edu/itc/shop/
    │   ├── ShopSecureApplication.java          [modify] idempotently seed admin & alice from properties
    │   ├── config/
    │   │   ├── PasswordConfig.java             [create] PasswordEncoderFactories.createDelegatingPasswordEncoder()
    │   │   └── SecurityConfig.java             [modify] remove InMemoryUserDetailsManager; permitAll /api/register
    │   ├── domain/
    │   │   ├── Role.java                       [create] CUSTOMER, ADMIN
    │   │   └── UserAccount.java                [create] id, username, passwordHash, enabled, eager roles; safe toString()
    │   ├── repository/
    │   │   └── UserAccountRepository.java      [create] findByUsername, existsByUsername
    │   ├── security/
    │   │   └── JpaUserDetailsService.java      [create] DB-backed UserDetailsService
    │   ├── service/
    │   │   ├── AccountService.java             [create] register(username, rawPassword, roles)
    │   │   └── UsernameTakenException.java     [create] -> 409
    │   └── web/
    │       ├── ApiExceptionHandler.java        [modify] 409 UsernameTakenException + 400 validation field errors
    │       ├── RegisterController.java         [create] POST /api/register (and GET /api/me if placed here)
    │       └── dto/
    │           └── RegisterRequest.java        [create] username & password policy
    └── resources/
        └── application.yml                     [modify] shop.seed.admin-password, shop.seed.customer-password

Endpoints:
/api
├── POST /api/register                          (permitAll)
└── GET  /api/me                                (authenticated)
```

---

## Task 3 · Roles and Method Security (`git tag task-3`)

```text
shop-secure/
├── security-session.md                         [modify] alice creates (201), bob reads (403), admin reads (200)
└── src/
    ├── main/java/edu/itc/shop/
    │   ├── config/
    │   │   └── MethodSecurityConfig.java       [create] @EnableMethodSecurity + RoleHierarchy (ADMIN > CUSTOMER)
    │   ├── domain/
    │   │   └── Order.java                      [create] id, customer, productId, quantity, status (NEW, SHIPPED, CANCELLED)
    │   ├── repository/
    │   │   └── OrderRepository.java            [create] findByCustomer(String)
    │   ├── service/
    │   │   └── OrderService.java               [create] place, history, get, ship, all (@PreAuthorize/@PostAuthorize)
    │   └── web/
    │       ├── ApiExceptionHandler.java        [modify] method AccessDeniedException -> 403 problem+json
    │       ├── OrderController.java            [create]
    │       └── dto/
    │           └── PlaceOrder.java             [create]
    └── test/java/edu/itc/shop/
        ├── OrderServiceSecurityTest.java       [create] 5 @SpringBootTest + @WithMockUser tests
        └── OrderControllerSecurityTest.java    [create] 3 @WebMvcTest(OrderController.class) tests

Endpoints:
/api/orders
├── POST /api/orders                            (owner from Authentication)
├── GET  /api/orders                            (own history; ADMIN may pass ?customer=...)
├── GET  /api/orders/{id}                       (owner or ADMIN via @PostAuthorize)
└── POST /api/orders/{id}/ship                  (ADMIN only)
```

---

## Task 4 · JWT Login and Stateless API (`git tag task-4`)

```text
shop-secure/
├── security-session.md                         [modify] decoded JWT header/payload + 1-sentence note + Task 4 transcripts
└── src/
    ├── main/
    │   ├── java/edu/itc/shop/
    │   │   ├── ShopSecureApplication.java      [modify] @ConfigurationPropertiesScan
    │   │   ├── config/
    │   │   │   ├── JwtConfig.java              [create] SecretKey, JwtEncoder, JwtDecoder, JwtAuthenticationConverter, AuthenticationManager
    │   │   │   ├── SecurityConfig.java         [modify] permitAll /api/auth/** & /api/register, drop httpBasic, oauth2ResourceServer
    │   │   │   └── SecurityProps.java          [create] @ConfigurationProperties("shop.security")
    │   │   └── web/
    │   │       ├── ApiExceptionHandler.java    [modify] AuthenticationException -> generic 401 problem+json
    │   │       ├── AuthController.java         [create] HS256 JWT issuer
    │   │       └── dto/
    │   │           ├── LoginRequest.java       [create]
    │   │           └── TokenResponse.java      [create]
    │   └── resources/
    │       └── application.yml                 [modify] shop.security.jwt-secret=${JWT_SECRET}, access-token-ttl=15m
    └── test/java/edu/itc/shop/
        ├── OrderControllerSecurityTest.java    [modify] jwt() post-processor + malformed token 401 test
        └── AuthFlowIT.java                     [create] @SpringBootTest(RANDOM_PORT) login -> access -> expired 401

Endpoints:
/api/auth
└── POST /api/auth/login                        (permitAll)
```

---

## Task 5 · Hardening: CSRF, CORS, Headers, XSS, Secrets, Logging (`git tag task-5`)

```text
shop-secure/
├── .env.example                                [create] JWT_SECRET, SHOP_SEED_ADMIN_PASSWORD, SHOP_SEED_CUSTOMER_PASSWORD
├── README.md                                   [modify] env vars & how to run
├── security-session.md                         [modify] CORS, curl -I headers, XSS API/HTML + th:utext, missing JWT_SECRET, SECURITY logs
├── pom.xml                                     [modify] add spring-boot-starter-thymeleaf
├── docs/
│   └── csrf.md                                 [create] per-chain CSRF strategy + CookieCsrfTokenRepository + fetch snippet
└── src/main/
    ├── java/edu/itc/shop/
    │   ├── config/
    │   │   ├── CorsConfig.java                 [create] /api/** -> http://localhost:5173
    │   │   ├── SecurityConfig.java             [modify] securityMatcher("/api/**"), cors, headers, RequestIdFilter, SpringAuthorizationEventPublisher
    │   │   └── SecurityProps.java              [modify] @Validated fail-fast on blank JWT_SECRET
    │   ├── domain/
    │   │   └── Review.java                     [create]
    │   ├── repository/
    │   │   └── ReviewRepository.java           [create]
    │   ├── security/
    │   │   ├── ProblemEntryPoint.java          [create/extract] reusable 401 problem+json entry point
    │   │   ├── RequestIdFilter.java            [create] OncePerRequestFilter, MDC rid, X-Request-Id
    │   │   └── SecurityEventLogger.java        [create] @EventListener -> logger "SECURITY"
    │   ├── service/
    │   │   └── ReviewService.java              [create]
    │   └── web/
    │       ├── ReviewController.java           [create]
    │       └── dto/
    │           └── ReviewRequest.java          [create]
    └── resources/
        ├── application.yml                     [modify] ${SHOP_SEED_*}, %X{rid} log pattern
        ├── logback-spring.xml                  [optional] %X{rid} pattern
        └── templates/
            └── reviews.html                    [create] Thymeleaf th:text (test th:utext on unmerged branch only)

Endpoints:
├── POST /api/reviews                           (authenticated)
├── GET  /api/products/{id}/reviews             (permitAll)
└── GET  /products/{id}/reviews.html            (server-rendered Thymeleaf page)
```

---

## Challenge (Optional) · Refresh Tokens, Lockout, Audit Log & OWASP (`git tag challenge`)

```text
shop-secure/
├── pom.xml                                     [modify] optional com.bucket4j:bucket4j-core
├── docs/
│   └── owasp-checklist.md                      [create] OWASP Top 10 (2021) controls + evidence (gaps/mitigations for A06, A10)
└── src/
    ├── main/java/edu/itc/shop/
    │   ├── domain/
    │   │   ├── RefreshToken.java               [create] refresh_tokens (id, tokenHash, username, familyId, expiresAt, revokedAt, replacedBy)
    │   │   ├── SecurityAudit.java              [create] security_audit (at, type, username, ip, requestId, detail) + AuditType
    │   │   └── UserAccount.java                [modify] failedAttempts, lockedUntil
    │   ├── repository/
    │   │   ├── RefreshTokenRepository.java     [create]
    │   │   └── SecurityAuditRepository.java    [create]
    │   ├── security/
    │   │   ├── JpaUserDetailsService.java      [modify] enforce lock state without leaking lock to client
    │   │   └── LockoutListener.java            [create] 5 failures -> 15m lock, success resets
    │   ├── service/
    │   │   ├── AuditService.java               [create] @Transactional(REQUIRES_NEW) for 8 event types
    │   │   ├── RateLimiterService.java         [create] 5 attempts/min per IP & username -> 429 + Retry-After
    │   │   └── RefreshTokenService.java        [create] opaque 32B token, SHA-256, rotate, revoke family
    │   └── web/
    │       ├── AdminAuditController.java       [create]
    │       └── AuthController.java             [modify] add refreshToken to login; add refresh & logout
    └── test/java/edu/itc/shop/
        └── AuthLifecycleIT.java                [create] rotation, reuse detection, logout, lockout, 429

Endpoints:
├── POST /api/auth/login                        (rate-limited)
├── POST /api/auth/refresh
├── POST /api/auth/logout
└── GET  /api/admin/audit?username=             (ADMIN only)
```

---

## Final Complete Repository Tree

```text
shop-secure/
├── pom.xml
├── mvnw
├── mvnw.cmd
├── .mvn/
├── .gitignore
├── .env.example
├── README.md
├── security-session.md
├── PATHS.md
├── docs/
│   ├── csrf.md
│   └── owasp-checklist.md                      # challenge only
└── src/
    ├── main/
    │   ├── java/edu/itc/shop/
    │   │   ├── ShopSecureApplication.java
    │   │   ├── config/
    │   │   │   ├── SecurityConfig.java
    │   │   │   ├── PasswordConfig.java
    │   │   │   ├── MethodSecurityConfig.java
    │   │   │   ├── JwtConfig.java
    │   │   │   ├── CorsConfig.java
    │   │   │   └── SecurityProps.java
    │   │   ├── security/
    │   │   │   ├── JpaUserDetailsService.java
    │   │   │   ├── SecurityEventLogger.java
    │   │   │   ├── RequestIdFilter.java
    │   │   │   ├── ProblemEntryPoint.java
    │   │   │   └── LockoutListener.java        # challenge
    │   │   ├── web/
    │   │   │   ├── ProductController.java
    │   │   │   ├── OrderController.java
    │   │   │   ├── RegisterController.java
    │   │   │   ├── AuthController.java
    │   │   │   ├── ReviewController.java
    │   │   │   ├── AdminAuditController.java   # challenge
    │   │   │   ├── ApiExceptionHandler.java
    │   │   │   └── dto/
    │   │   │       ├── PlaceOrder.java
    │   │   │       ├── RegisterRequest.java
    │   │   │       ├── LoginRequest.java
    │   │   │       ├── TokenResponse.java
    │   │   │       └── ReviewRequest.java
    │   │   ├── service/
    │   │   │   ├── ProductService.java
    │   │   │   ├── OrderService.java
    │   │   │   ├── AccountService.java
    │   │   │   ├── ReviewService.java
    │   │   │   ├── RefreshTokenService.java    # challenge
    │   │   │   ├── AuditService.java           # challenge
    │   │   │   └── RateLimiterService.java     # challenge
    │   │   ├── repository/
    │   │   │   ├── ProductRepository.java
    │   │   │   ├── OrderRepository.java
    │   │   │   ├── UserAccountRepository.java
    │   │   │   ├── ReviewRepository.java
    │   │   │   ├── RefreshTokenRepository.java # challenge
    │   │   │   └── SecurityAuditRepository.java # challenge
    │   │   └── domain/
    │   │       ├── Product.java
    │   │       ├── Order.java
    │   │       ├── UserAccount.java
    │   │       ├── Role.java
    │   │       ├── Review.java
    │   │       ├── RefreshToken.java           # challenge
    │   │       └── SecurityAudit.java          # challenge
    │   └── resources/
    │       ├── application.yml
    │       ├── logback-spring.xml              # optional
    │       └── templates/
    │           └── reviews.html
    └── test/
        └── java/edu/itc/shop/
            ├── OrderServiceSecurityTest.java
            ├── OrderControllerSecurityTest.java
            ├── AuthFlowIT.java
            └── AuthLifecycleIT.java            # challenge
```
