# CSRF Strategy (`shop-secure`)

## 1. Credentials Accepted Per Filter Chain

The `shop-secure` application defines a stateless security filter chain matching `/api/**` and public web pages:

- **`/api/**`**: Accepts **HTTP Bearer tokens only** (`Authorization: Bearer <JWT>`). No cookie-based credentials (such as `JSESSIONID`) or HTTP Basic credentials are accepted. `sessionCreationPolicy` is explicitly set to `SessionCreationPolicy.STATELESS`.
- **Public resources** (e.g. `GET /api/products/**`, `GET /products/{id}/reviews.html`): Anonymous access allowed without credentials.

## 2. Why CSRF is Disabled for the Bearer-Only `/api/**` Chain

Cross-Site Request Forgery (CSRF) exploits **ambient authority**—the browser's default behavior of automatically attaching credentials stored in cookies to cross-origin requests made by third-party malicious sites.

Because `/api/**` relies strictly on Bearer tokens:
1. Bearer tokens must be read from application state (e.g., in-memory JavaScript variable) and explicitly attached to HTTP request headers (`Authorization: Bearer <token>`).
2. Browsers **never** automatically attach custom `Authorization` headers to cross-origin requests.
3. Therefore, an attacker cannot forge an authenticated state-changing request from another origin. Disabling CSRF protection (`.csrf(csrf -> csrf.disable())`) is both safe and standard practice for pure Bearer-token-based REST APIs.

## 3. Configuration for Same-Origin SPA with Session Cookies

If a same-origin Single Page Application (SPA) utilizing session cookies (or cookie-based authentication) were added in the future, CSRF protection would become mandatory.

### Spring Security Configuration

In Spring Security, the SPA filter chain would configure CSRF protection using `CookieCsrfTokenRepository.withHttpOnlyFalse()` so that the front-end JavaScript client can read the anti-forgery token from a cookie (`XSRF-TOKEN`) and return it in a header (`X-XSRF-TOKEN`):

```java
http.csrf(csrf -> csrf
    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
    .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()) // or CsrfTokenRequestAttributeHandler
);
```

### Front-End JavaScript `fetch` Snippet

The front-end client reads the `XSRF-TOKEN` cookie and transmits it in the `X-XSRF-TOKEN` HTTP header on all mutating operations (`POST`, `PUT`, `DELETE`, `PATCH`):

```javascript
// Helper to extract a cookie value by name
function getCookie(name) {
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length === 2) return parts.pop().split(';').shift();
}

// State-changing request sending X-XSRF-TOKEN
async function postReview(productId, stars, text) {
  const xsrfToken = getCookie('XSRF-TOKEN');

  const response = await fetch('/api/reviews', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-XSRF-TOKEN': xsrfToken // send CSRF token back in custom header
    },
    body: JSON.stringify({ productId, stars, text })
  });

  return response.json();
}
```

Since cross-origin scripts cannot read the `XSRF-TOKEN` cookie due to the browser's Same-Origin Policy (SOP), an attacker cannot supply the valid `X-XSRF-TOKEN` header, thwarting CSRF attempts.
