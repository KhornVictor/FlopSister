# Task 5 — Pagination, Caching, Versioning, and Reviews

Base URL: `http://localhost:8080/api/`

API key for write operations (`PUT`, `POST`, `PATCH`, `DELETE`): `X-Api-Key: demo-123`

---

## Test 1: Paginated product list (`page=1&size=10`)

Command:

```bash
curl -si 'http://localhost:8080/api/products?page=1&size=10'
```

Actual response:

```http
HTTP/1.1 200 OK
Link: <http://localhost:8080/api/products?size=10&page=0>; rel="first"
Link: <http://localhost:8080/api/products?size=10&page=0>; rel="prev"
Link: <http://localhost:8080/api/products?size=10&page=2>; rel="next"
Link: <http://localhost:8080/api/products?size=10&page=3>; rel="last"
X-Total-Count: 33
Vary: Accept-Encoding
X-Correlation-Id: 6a79a306-0cd1-4514-8863-a52ba372fb4c
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, X-Api-Key
Access-Control-Expose-Headers: X-Correlation-Id
Access-Control-Max-Age: 3600
Content-Type: application/json
Content-Length: 663

[{"id":12,"name":"Sample 09","price":1.90,"sku":"SP-09","stock":12},{"id":13,"name":"Sample 10","price":2.00,"sku":"SP-10","stock":0},{"id":14,"name":"Sample 11","price":2.10,"sku":"SP-11","stock":3},{"id":15,"name":"Sample 12","price":2.20,"sku":"SP-12","stock":6},{"id":16,"name":"Sample 13","price":2.30,"sku":"SP-13","stock":9},{"id":17,"name":"Sample 14","price":2.40,"sku":"SP-14","stock":12},{"id":18,"name":"Sample 15","price":2.50,"sku":"SP-15","stock":0},{"id":19,"name":"Sample 16","price":2.60,"sku":"SP-16","stock":3},{"id":20,"name":"Sample 17","price":2.70,"sku":"SP-17","stock":6},{"id":21,"name":"Sample 18","price":2.80,"sku":"SP-18","stock":9}]
```

Result: PASSED — `X-Total-Count: 33`, 10 items on page 1, and `first`, `prev`, `next`, `last` `Link` headers built from the request URI.

---

## Test 2: GET product with `ETag` and `Cache-Control`

Command:

```bash
curl -si http://localhost:8080/api/products/1
```

Actual response:

```http
HTTP/1.1 200 OK
ETag: "1"
Cache-Control: private, no-transform, max-age=60
Vary: Accept-Encoding
X-Correlation-Id: 1360b649-c9fe-4ac0-99f0-dcb1e0fe90ff
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, X-Api-Key
Access-Control-Expose-Headers: X-Correlation-Id
Access-Control-Max-Age: 3600
Content-Type: application/json
Content-Length: 63

{"id":1,"name":"Kettle","price":24.90,"sku":"KT-01","stock":12}
```

Result: PASSED — Returned `ETag: "1"`, `Cache-Control: private, no-transform, max-age=60`, and the default v1 JSON representation.

---

## Test 3: Conditional GET returning `304 Not Modified`

Command:

```bash
curl -si http://localhost:8080/api/products/1 -H 'If-None-Match: "1"'
```

Actual response:

```http
HTTP/1.1 304 Not Modified
ETag: "1"
X-Correlation-Id: 01733132-b631-4152-a2bb-20cc49e42c0f
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, X-Api-Key
Access-Control-Expose-Headers: X-Correlation-Id
Access-Control-Max-Age: 3600
```

Result: PASSED — Matching `If-None-Match` returned `304 Not Modified` with `ETag: "1"` and no body.

---

## Test 4: PUT without `If-Match` (`428 Precondition Required`)

Command:

```bash
curl -si -X PUT http://localhost:8080/api/products/1 \
     -H 'X-Api-Key: demo-123' \
     -H 'Content-Type: application/json' \
     -d '{"name":"Kettle XL","sku":"KT-01","price":29.90,"stock":12}'
```

Actual response:

```http
HTTP/1.1 428 Precondition Required
Vary: Accept-Encoding
X-Correlation-Id: 676afaf2-d223-4402-a558-39f32e17a237
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, X-Api-Key
Access-Control-Expose-Headers: X-Correlation-Id
Access-Control-Max-Age: 3600
Content-Type: application/problem+json
Content-Length: 135

{"detail":"If-Match header is required","instance":"/api/products/1","status":428,"title":"Precondition Required","type":"about:blank"}
```

Result: PASSED — Missing `If-Match` rejected with `428 Precondition Required` and `application/problem+json`.

---

## Test 5: PUT with stale `If-Match` (`412 Precondition Failed`)

Command:

```bash
curl -si -X PUT http://localhost:8080/api/products/1 \
     -H 'X-Api-Key: demo-123' \
     -H 'If-Match: "0"' \
     -H 'Content-Type: application/json' \
     -d '{"name":"Kettle XL","sku":"KT-01","price":29.90,"stock":12}'
```

Actual response:

```http
HTTP/1.1 412 Precondition Failed
ETag: "1"
X-Correlation-Id: 3969b026-57a1-46fd-a930-560fa01b790c
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, X-Api-Key
Access-Control-Expose-Headers: X-Correlation-Id
Access-Control-Max-Age: 3600
Content-Length: 0
```

Result: PASSED — Stale `If-Match: "0"` rejected with `412 Precondition Failed`.

---

## Test 6: PUT with matching `If-Match` (`200 OK` + new `ETag`)

Command:

```bash
curl -si -X PUT http://localhost:8080/api/products/1 \
     -H 'X-Api-Key: demo-123' \
     -H 'If-Match: "1"' \
     -H 'Content-Type: application/json' \
     -d '{"name":"Kettle XL","sku":"KT-01","price":29.90,"stock":12}'
```

Actual response:

```http
HTTP/1.1 200 OK
ETag: "2"
Vary: Accept-Encoding
X-Correlation-Id: 7a78b8d8-c004-41b9-b142-6fea28b64cdf
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, X-Api-Key
Access-Control-Expose-Headers: X-Correlation-Id
Access-Control-Max-Age: 3600
Content-Type: application/json
Content-Length: 66

{"id":1,"name":"Kettle XL","price":29.90,"sku":"KT-01","stock":12}
```

Result: PASSED — Product updated and returned `200 OK` with incremented `ETag: "2"`.

---

## Test 7: Media-type versioning — v1 of product 1

Command:

```bash
curl -si http://localhost:8080/api/products/1 -H 'Accept: application/vnd.shop.v1+json'
```

Actual response:

```http
HTTP/1.1 200 OK
ETag: "2"
Cache-Control: private, no-transform, max-age=60
Vary: Accept-Encoding
X-Correlation-Id: fea2ae2f-ad2a-409f-a0db-301b3365bb0e
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, X-Api-Key
Access-Control-Expose-Headers: X-Correlation-Id
Access-Control-Max-Age: 3600
Content-Type: application/vnd.shop.v1+json
Content-Length: 66

{"id":1,"name":"Kettle XL","price":29.90,"sku":"KT-01","stock":12}
```

Result: PASSED — Returned v1 shape (`price` as a plain decimal) with `Content-Type: application/vnd.shop.v1+json`.

---

## Test 8: Media-type versioning — v2 of product 1

Command:

```bash
curl -si http://localhost:8080/api/products/1 -H 'Accept: application/vnd.shop.v2+json'
```

Actual response:

```http
HTTP/1.1 200 OK
ETag: "2"
Cache-Control: private, no-transform, max-age=60
Vary: Accept-Encoding
X-Correlation-Id: 0cd73c61-8ee6-4998-8057-3f49d0957b1a
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, X-Api-Key
Access-Control-Expose-Headers: X-Correlation-Id
Access-Control-Max-Age: 3600
Content-Type: application/vnd.shop.v2+json
Content-Length: 111

{"available":true,"id":1,"name":"Kettle XL","price":{"amount":29.90,"currency":"EUR"},"sku":"KT-01","stock":12}
```

Result: PASSED — Returned v2 shape (`Money` object `price` and `available` boolean) with `Content-Type: application/vnd.shop.v2+json`.

---

## Test 9: Create review on sub-resource (`POST /products/1/reviews`)

Command:

```bash
curl -si -X POST http://localhost:8080/api/products/1/reviews \
     -H 'X-Api-Key: demo-123' \
     -H 'Content-Type: application/json' \
     -d '{"stars":5,"author":"Ana","text":"Boils fast."}'
```

Actual response:

```http
HTTP/1.1 201 Created
Location: http://localhost:8080/api/products/1/reviews/1
Vary: Accept-Encoding
X-Correlation-Id: 135ccab5-70d9-4a5d-ab24-c63231724f2f
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, X-Api-Key
Access-Control-Expose-Headers: X-Correlation-Id
Access-Control-Max-Age: 3600
Content-Type: application/json
Content-Length: 68

{"author":"Ana","id":1,"productId":1,"stars":5,"text":"Boils fast."}
```

Result: PASSED — Review persisted for product 1 and returned `201 Created` with `Location`.

---

## Test 10: List reviews on sub-resource (`GET /products/1/reviews`)

Command:

```bash
curl -si http://localhost:8080/api/products/1/reviews
```

Actual response:

```http
HTTP/1.1 200 OK
Vary: Accept-Encoding
X-Correlation-Id: 13f82f49-2559-42b5-821c-83dc75d8ad6a
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, X-Api-Key
Access-Control-Expose-Headers: X-Correlation-Id
Access-Control-Max-Age: 3600
Content-Type: application/json
Content-Length: 70

[{"author":"Ana","id":1,"productId":1,"stars":5,"text":"Boils fast."}]
```

Result: PASSED — Returned the saved review list for product 1.

---

## Test 11: Reviews for unknown product (`GET /products/999/reviews`)

Command:

```bash
curl -si http://localhost:8080/api/products/999/reviews
```

Actual response:

```http
HTTP/1.1 404 Not Found
Vary: Accept-Encoding
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, X-Api-Key
Access-Control-Expose-Headers: X-Correlation-Id
Access-Control-Max-Age: 3600
Content-Type: application/problem+json
Content-Length: 127

{"detail":"Product 999 not found","instance":"/api/products/999/reviews","status":404,"title":"Not Found","type":"about:blank"}
```

Result: PASSED — Sub-resource locator threw `NotFoundException`, producing `404 Not Found` (`application/problem+json`).

---

## Summary

| Test | Endpoint / Scenario | Status | Result |
|---|---|---|---|
| 1 | `GET /products?page=1&size=10` (`X-Total-Count`, `Link`) | `200 OK` | PASS |
| 2 | `GET /products/1` (`ETag`, `Cache-Control`) | `200 OK` | PASS |
| 3 | `GET /products/1` with `If-None-Match: "1"` | `304 Not Modified` | PASS |
| 4 | `PUT /products/1` without `If-Match` | `428 Precondition Required` | PASS |
| 5 | `PUT /products/1` with stale `If-Match: "0"` | `412 Precondition Failed` | PASS |
| 6 | `PUT /products/1` with matching `If-Match: "1"` | `200 OK` (`ETag: "2"`) | PASS |
| 7 | `GET /products/1` with `Accept: application/vnd.shop.v1+json` | `200 OK` | PASS |
| 8 | `GET /products/1` with `Accept: application/vnd.shop.v2+json` | `200 OK` | PASS |
| 9 | `POST /products/1/reviews` | `201 Created` + `Location` | PASS |
| 10 | `GET /products/1/reviews` | `200 OK` | PASS |
| 11 | `GET /products/999/reviews` | `404 Not Found` | PASS |
