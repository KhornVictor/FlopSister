# Task 1 — HTTP Test Evidence

Base URL: `http://localhost:8080/api/`

## Test 1: Get all products

Command:

```bash
curl -i http://localhost:8080/api/products
```

Actual response:

```http
HTTP/1.1 200 OK
Content-Type: application/json
Content-Length: 220
```

```json
[
  {"id":1,"name":"Kettle","price":24.90,"sku":"KT-01","stock":12,"version":1},
  {"id":2,"name":"Mug","price":6.50,"sku":"MG-01","stock":40,"version":1},
  {"id":3,"name":"Tea","price":4.20,"sku":"TE-01","stock":0,"version":1}
]
```

Result: PASSED — All three products were returned.

## Test 2: Get product with ID 2

Command:

```bash
curl -i http://localhost:8080/api/products/2
```

Actual response:

```http
HTTP/1.1 200 OK
Content-Type: application/json
Content-Length: 71
```

```json
{"id":2,"name":"Mug","price":6.50,"sku":"MG-01","stock":40,"version":1}
```

Result: PASSED — The Mug was returned.

## Test 3: Get unknown product with ID 99

Command:

```bash
curl -i http://localhost:8080/api/products/99
```

Actual response:

```http
HTTP/1.1 404 Not Found
Content-Length: 0
```

Result: PASSED — Unknown product returned HTTP 404.

## Test 4: Get product with invalid ID abc

Command:

```bash
curl -i http://localhost:8080/api/products/abc
```

Actual response:

```http
HTTP/1.1 404 Not Found
Content-Length: 0
```

Result: PASSED — Invalid non-numeric ID returned HTTP 404.

## Summary

All four required HTTP tests passed.
