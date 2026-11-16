# Task 4 — Providers and Filters: HTTP Tests

This document records the HTTP tests performed for Task 4 of the FlopSister RESTful Web Services project.

Base URL: `http://localhost:8080/api/`

## 1. Correlation ID

**Test:** Send multiple requests and check that each response contains a unique correlation ID.

Command:

```powershell
Invoke-WebRequest -Uri "http://localhost:8080/api/products" -UseBasicParsing
```

**Expected result:**
- HTTP `200 OK`
- Response contains the `X-Correlation-Id` header.
- Each request receives a different UUID.
- Server logs include the correlation ID, HTTP method, request URI, and response status.

**Result:** PASS

## 2. CORS Filter

### 2.1 GET Request

**Test:** Check the CORS headers on a normal GET request.

Command:

```powershell
Invoke-WebRequest `
    -Uri "http://localhost:8080/api/products" `
    -Headers @{ "Origin" = "http://localhost:5173" } `
    -UseBasicParsing
```

**Expected result:**
- HTTP `200 OK`
- `Access-Control-Allow-Origin: http://localhost:5173`
- `Access-Control-Allow-Methods` includes GET, POST, PUT, PATCH, DELETE, and OPTIONS.
- `Access-Control-Allow-Headers` includes Content-Type and X-Api-Key.
- `Access-Control-Expose-Headers` includes X-Correlation-Id.

**Result:** PASS

### 2.2 OPTIONS Preflight Request

**Test:** Check that the server handles a CORS preflight request.

Command:

```powershell
curl.exe -i -X OPTIONS "http://localhost:8080/api/products" `
    -H "Origin: http://localhost:5173" `
    -H "Access-Control-Request-Method: POST" `
    -H "Access-Control-Request-Headers: Content-Type,X-Api-Key"
```

**Expected result:**
- HTTP `200 OK`
- CORS response headers are present.
- The preflight request is handled without executing the POST operation.

**Result:** PASS

## 3. API Key Authentication

The API key filter protects write operations using the `X-Api-Key` request header.

Development API key: `dev-secret-key`

### 3.1 GET Without API Key

**Test:** Access the product list without an API key.

Command:

```powershell
Invoke-WebRequest `
    -Uri "http://localhost:8080/api/products" `
    -UseBasicParsing
```

**Result:** HTTP `200 OK` — PASS

### 3.2 POST Without API Key

**Test:** Attempt to create a product without an API key.

**Expected result:** HTTP `401 Unauthorized`.

**Result:** PASS

### 3.3 POST With Invalid API Key

**Test:** Attempt to create a product using an incorrect API key.

**Expected result:** HTTP `401 Unauthorized`.

**Result:** PASS

### 3.4 POST With Valid API Key

**Test:** Create a product using the correct API key.

Command:

```powershell
$body = '{"name":"Tea Pot","sku":"TP-01","price":15.50,"stock":10}'

Invoke-WebRequest `
    -Uri "http://localhost:8080/api/products" `
    -Method POST `
    -ContentType "application/json" `
    -Headers @{ "X-Api-Key" = "dev-secret-key" } `
    -Body $body
```

**Actual result:**
- HTTP `201 Created`
- Product created successfully.
- Response body:

```json
{
    "id": 4,
    "name": "Tea Pot",
    "price": 15.50,
    "sku": "TP-01",
    "stock": 10
}
```

**Result:** PASS

## 4. GZIP Compression

**Test:** Request a compressed response using the `Accept-Encoding: gzip` header.

Command:

```powershell
Invoke-WebRequest `
    -Uri "http://localhost:8080/api/products" `
    -Headers @{ "Accept-Encoding" = "gzip" } `
    -UseBasicParsing
```

**Expected result:**
- HTTP `200 OK`
- `Content-Encoding: gzip`
- `Vary: Accept-Encoding`
- Response body is automatically decompressed by PowerShell for display.

**Result:** PASS

## 5. Test Summary

| Feature | Test Result |
|---|---|
| Correlation ID | PASS |
| CORS GET | PASS |
| CORS OPTIONS preflight | PASS |
| API key — GET without key | PASS |
| API key — POST without key | PASS |
| API key — POST with invalid key | PASS |
| API key — POST with valid key | PASS |
| GZIP compression | PASS |

**Overall Result: All Task 4 HTTP tests passed.**