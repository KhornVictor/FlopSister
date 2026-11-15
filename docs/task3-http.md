# Task 3 — Parameters and Validation

Base URL: `http://localhost:8080/api/`

## Filter and sort products

```bash
curl -i 'http://localhost:8080/api/products?minPrice=5%20EUR&inStock=true&sort=price,desc'
```

```http
HTTP/1.1 200 OK
Content-Type: application/json

[{"id":1,"name":"Kettle","price":24.90,"sku":"KT-01","stock":12},{"id":2,"name":"Mug","price":6.50,"sku":"MG-01","stock":40}]
```

Result: PASSED — Only products priced at least 5 EUR and in stock were returned, ordered by descending price.

## Invalid Money query parameter

```bash
curl -i 'http://localhost:8080/api/products?minPrice=abc'
```

```http
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{"detail":"Cannot parse Money from 'abc'","instance":"/api/products","status":400,"title":"Bad Request","type":"about:blank"}
```

Result: PASSED — The Money converter rejects malformed values with a problem response.

## Unsupported sort field

```bash
curl -i 'http://localhost:8080/api/products?sort=colour,asc'
```

```http
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{"detail":"Validation failed","errors":[{"field":"sort","message":"use name,asc|desc, price,asc|desc, or stock,asc|desc"}],"instance":"/api/products","status":400,"title":"Invalid request","type":"about:blank"}
```

Result: PASSED — Sorting is restricted to `name`, `price`, and `stock`.

## Invalid product body

```bash
curl -i -X POST http://localhost:8080/api/products \
     -H 'Content-Type: application/json' \
     -d '{"name":"","sku":"kettle","price":0,"stock":-1}'
```

```http
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{"detail":"Validation failed","errors":[{"field":"name","message":"must not be blank"},{"field":"price","message":"must be greater than or equal to 0.01"},{"field":"sku","message":"must match \"[A-Z]{2}-\\d{2}\""},{"field":"stock","message":"must be greater than or equal to 0"}],"instance":"/api/products","status":400,"title":"Invalid request","type":"about:blank"}
```

Result: PASSED — Bean Validation reports each invalid field.

## Invalid stock adjustment

```bash
curl -i -X PATCH http://localhost:8080/api/products/1/stock \
     -H 'Content-Type: application/json' \
     -d '{"delta":1001}'
```

```http
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{"detail":"Validation failed","errors":[{"field":"delta","message":"must be less than or equal to 1000"}],"instance":"/api/products/1/stock","status":400,"title":"Invalid request","type":"about:blank"}
```

Result: PASSED — Stock deltas outside -1000 through 1000 are rejected.

## Missing product

```bash
curl -i http://localhost:8080/api/products/99
```

```http
HTTP/1.1 404 Not Found
Content-Type: application/problem+json

{"detail":"Product 99 not found","instance":"/api/products/99","status":404,"title":"Not Found","type":"about:blank"}
```

Result: PASSED — Not-found responses use the RFC 9457 problem representation.

## Unsupported media type

```bash
curl -i -X POST http://localhost:8080/api/products \
     -H 'Content-Type: text/plain' \
     -d 'Tea'
```

```http
HTTP/1.1 415 Unsupported Media Type
Content-Type: application/problem+json

{"detail":"HTTP 415 Unsupported Media Type","instance":"/api/products","status":415,"title":"Unsupported Media Type","type":"about:blank"}
```

Result: PASSED — Unsupported request media types retain the 415 status and return `application/problem+json`.

## Unexpected server error

The automated `Task3ApiTest` registers a test-only resource that throws an `IllegalStateException`. The mapper logs the exception with a generated reference ID and returns HTTP 500 with a body like:

```json
{"detail":"reference <generated-uuid>","instance":"/task3-test/fail","status":500,"title":"Internal Server Error","type":"about:blank"}
```

Result: PASSED — The response contains a reference ID and does not expose the exception or stack trace. The test-only failure resource is not part of the production server.
