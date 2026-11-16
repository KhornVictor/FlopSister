# Challenge — Orders, HATEOAS, Idempotency & OpenAPI Reference

Base URL: `http://localhost:8080/api`  
Write requests require `X-Api-Key: demo-123`.

> **Note:** Verify these endpoints against a running server started with `mvn -q compile exec:java` (after `mvn -q verify` / `mvn process-classes` generates `target/classes/META-INF/openapi.yaml`).

---

## 1. Place an Order (HATEOAS + Idempotency)

### Missing `Idempotency-Key` (`428 Precondition Required`)

```bash
curl -i -X POST http://localhost:8080/api/orders \
  -H 'X-Api-Key: demo-123' \
  -H 'Content-Type: application/json' \
  -d '{"lines":[{"productId":1,"quantity":2}]}'
```

Expected response: `428 Precondition Required` (`Content-Type: application/problem+json`).

### First Order Creation (`201 Created`)

```bash
curl -i -X POST http://localhost:8080/api/orders \
  -H 'X-Api-Key: demo-123' \
  -H 'Idempotency-Key: 7c1e' \
  -H 'Content-Type: application/json' \
  -d '{"lines":[{"productId":1,"quantity":2}]}'
```

Expected response:

```http
HTTP/1.1 201 Created
Location: http://localhost:8080/api/orders/o-1001
Link: <http://localhost:8080/api/orders/o-1001>; rel="self"
Link: <http://localhost:8080/api/orders/o-1001/payment>; rel="pay"
Link: <http://localhost:8080/api/orders/o-1001>; rel="cancel"
Link: <http://localhost:8080/api/products/1>; rel="product-1"
Idempotency-Replayed: false
Content-Type: application/json

{
  "id": "o-1001",
  "status": "NEW",
  "total": 49.80,
  "lines": [{"productId": 1, "quantity": 2}],
  "_links": {
    "self": "http://localhost:8080/api/orders/o-1001",
    "pay": "http://localhost:8080/api/orders/o-1001/payment",
    "cancel": "http://localhost:8080/api/orders/o-1001",
    "product-1": "http://localhost:8080/api/products/1"
  }
}
```

### Idempotent Replay (`201 Created`, `Idempotency-Replayed: true`)

```bash
curl -i -X POST http://localhost:8080/api/orders \
  -H 'X-Api-Key: demo-123' \
  -H 'Idempotency-Key: 7c1e' \
  -H 'Content-Type: application/json' \
  -d '{"lines":[{"productId":1,"quantity":2}]}'
```

Expected headers: `HTTP/1.1 201 Created`, `Idempotency-Replayed: true`, same `Location` and body, stock deducted only once.

### Key Reuse With Different Payload (`422 Unprocessable Content`)

```bash
curl -i -X POST http://localhost:8080/api/orders \
  -H 'X-Api-Key: demo-123' \
  -H 'Idempotency-Key: 7c1e' \
  -H 'Content-Type: application/json' \
  -d '{"lines":[{"productId":2,"quantity":1}]}'
```

Expected response: `422 Unprocessable Content` (`Content-Type: application/problem+json`).

---

## 2. HATEOAS State Transitions (`pay` and `cancel`)

### Pay Order (`NEW -> PAID`)

Follow the `pay` link:

```bash
curl -i -X PUT http://localhost:8080/api/orders/o-1001/payment \
  -H 'X-Api-Key: demo-123'
```

Expected response: `200 OK` with `"status": "PAID"` and `_links` / `Link` containing `self` and `product-1` (without `pay` or `cancel`).

### Attempt to Cancel Paid Order (`409 Conflict`)

```bash
curl -i -X DELETE http://localhost:8080/api/orders/o-1001 \
  -H 'X-Api-Key: demo-123'
```

Expected response: `409 Conflict` (`Content-Type: application/problem+json`).

### Cancel a `NEW` Order (`NEW -> CANCELLED`, Restores Stock)

```bash
curl -i -X DELETE http://localhost:8080/api/orders/o-1002 \
  -H 'X-Api-Key: demo-123'
```

Expected response: `200 OK` with `"status": "CANCELLED"` and reserved stock restored to the product repository.

---

## 3. OpenAPI 3.1 Specification (`GET /openapi`)

```bash
curl -s http://localhost:8080/api/openapi
```

Expected response: `200 OK` with `Content-Type: application/yaml` starting with `openapi: 3.1.0` and documenting `/products`, `/products/{id}`, `/products/{id}/stock`, `/products/{id}/reviews`, `/orders`, `/orders/{id}`, `/orders/{id}/payment`, and `/openapi`.
