# CSRF Strategy

## Authentication Mechanism

The application uses JWT (JSON Web Token) bearer authentication through Spring Security OAuth2 Resource Server.

Clients provide access tokens explicitly using the `Authorization` HTTP header:

`Authorization: Bearer <JWT_TOKEN>`

The application is configured with `SessionCreationPolicy.STATELESS`, meaning Spring Security does not use HTTP sessions to maintain authentication state.

## CSRF Protection Decision

CSRF protection is disabled in `SecurityConfig.java` using:

`.csrf(csrf -> csrf.disable())`

This decision is based on the application's stateless, bearer-token authentication design.

Unlike cookie-based authentication, bearer tokens are explicitly attached to HTTP requests by the client. Browsers do not automatically attach these tokens to cross-origin requests.

Therefore, traditional CSRF attacks that rely on automatically transmitted authentication cookies are not applicable to this authentication mechanism.

## Security Considerations

- Protected API endpoints must require valid JWT bearer tokens.
- Authentication must not depend on automatically transmitted cookies.
- JWTs must be validated by Spring Security.
- Authorization rules must restrict sensitive operations to appropriate roles.
- CORS restrictions must be configured separately from CSRF protection.
- Tokens must be stored securely to reduce the risk of theft through XSS or other attacks.

## Future Considerations

If the application introduces cookie-based authentication, the CSRF strategy must be reviewed. CSRF protection may need to be enabled, with appropriate CSRF tokens and cookie security settings.

## Conclusion

Disabling CSRF is appropriate for the current stateless JWT bearer-token API, provided no cookie-based authentication is accepted.