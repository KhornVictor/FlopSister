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
