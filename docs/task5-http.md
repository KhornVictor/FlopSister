# Task 5 — HTTP Feature Tests

This document records the HTTP tests performed for Task 5 of Lab 04.

Base URL: `http://localhost:8080/api`

## 1. Pagination

**Request:**

```http
GET /products?page=1&size=5
```

**Expected results:**
- HTTP 200 OK
- 5 products returned
- `X-Total-Count: 33`
- `Link` header contains `first`, `prev`, `next`, and `last` relations.

**Observed Link header:**

```http
Link: <http://localhost:8080/api/products?size=5&page=0>; rel="first",
      <http://localhost:8080/api/products?size=5&page=0>; rel="prev",
      <http://localhost:8080/api/products?size=5&page=2>; rel="next",
      <http://localhost:8080/api/products?size=5&page=6>; rel="last"
```

Result: PASS

## 2. ETag Caching

**Request:**

```http
GET /products/1
```

**Observed response headers:**

```http
ETag: "1"
Cache-Control: private, no-transform, max-age=60
```

**Conditional request:**

```http
GET /products/1
If-None-Match: "1"
```

Expected response:

```http
304 Not Modified
```

Result: PASS

## 3. Conditional PUT

The API requires the `X-Api-Key` header for PUT requests.

**Test A — Missing If-Match**

```http
PUT /products/1
X-Api-Key: dev-secret-key
```

Expected response:

```http
428 Precondition Required
```

**Test B — Stale ETag**

```http
PUT /products/1
If-Match: "999"
X-Api-Key: dev-secret-key
```

Expected response:

```http
412 Precondition Failed
```

**Test C — Correct ETag**

```http
PUT /products/1
If-Match: "1"
X-Api-Key: dev-secret-key
```

Expected response:

```http
200 OK
ETag: "2"
```

Result: PASS

## 4. API Versioning

**Test A — Version 1**

```http
GET /products/1
Accept: application/json
```

Expected response: Product information using the original JSON representation.

Example:

```json
{
  "id": 1,
  "name": "Kettle",
  "price": 24.90,
  "sku": "KT-01",
  "stock": 12
}
```

**Test B — Version 2**

```http
GET /products/1
Accept: application/vnd.shop.v2+json
```

Expected response:

```json
{
  "available": true,
  "id": 1,
  "name": "Kettle",
  "price": {
    "amount": 24.90,
    "currency": "USD"
  },
  "sku": "KT-01",
  "stock": 12
}
```

Result: PASS

## 5. Product Reviews

**Test A — Get reviews**

```http
GET /products/1/reviews
```

Expected response:

```http
200 OK
```

Initially, the response body was:

```json
[]
```

**Test B — Create a review**

```http
POST /products/1/reviews
Content-Type: application/json
X-Api-Key: dev-secret-key
```

Request body:

```json
{
  "author": "Boramey",
  "stars": 5,
  "text": "Excellent product! I really like it."
}
```

Observed response:

```http
201 Created
Location: http://localhost:8080/api/products/1/reviews/1
```

Response body:

```json
{
  "author": "Boramey",
  "id": 1,
  "productId": 1,
  "stars": 5,
  "text": "Excellent product! I really like it."
}
```

**Test C — Retrieve saved reviews**

```http
GET /products/1/reviews
```

Observed response:

```http
200 OK
```

The saved review was returned successfully.

**Test D — Invalid review rating**

Submitted a review with:

```json
{
  "author": "Test User",
  "stars": 6,
  "text": "Testing invalid stars."
}
```

Expected response:

```http
400 Bad Request
```

**Test E — Nonexistent product**

```http
GET /products/999/reviews
```

Expected response:

```http
404 Not Found
```

Result: PASS

---

## 6. Overall Result

All implemented Task 5 features were tested successfully:

- Pagination and Link headers
- ETag caching and conditional GET
- Conditional PUT with If-Match
- API versioning (v1 and v2)
- Review creation and retrieval
- Review validation
- Nonexistent product handling

**Overall result: PASS**
