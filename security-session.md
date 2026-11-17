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
