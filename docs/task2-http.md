# Task 2 — HTTP Test Evidence

Base URL: `http://localhost:8080/api/`

## Test 1: Create product (POST JSON)

Command:

```bash
curl -i -X POST http://localhost:8080/api/products -H 'Content-Type: application/json' \
     -d '{"name":"Teapot","sku":"TP-01","price":31.00,"stock":5}'
```

Actual response:

```http
HTTP/1.1 201 Created
Location: http://localhost:8080/api/products/4
Content-Type: application/json
Content-Length: 62

{"id":4,"name":"Teapot","price":31.00,"sku":"TP-01","stock":5}
```

Result: PASSED — Product created with ID 4, Location header returned, version omitted from representation.

---

## Test 2: Duplicate SKU conflict (POST JSON)

Command:

```bash
curl -i -X POST http://localhost:8080/api/products -H 'Content-Type: application/json' \
     -d '{"name":"Teapot","sku":"TP-01","price":31.00,"stock":5}'
```

Actual response:

```http
HTTP/1.1 409 Conflict
Content-Length: 0
```

Result: PASSED — Duplicate SKU correctly rejected with HTTP 409 Conflict via `DuplicateSkuException`.

---

## Test 3: Retrieve product as XML (Content Negotiation)

Command:

```bash
curl -s http://localhost:8080/api/products/4 -H 'Accept: application/xml'
```

Actual response:

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?><product id="4"><name>Teapot</name><sku>TP-01</sku><price>31.00</price><stock>5</stock></product>
```

Result: PASSED — Product returned in XML format with `id` attribute matching JAXB configuration.

---

## Test 4: XML Round-Trip — Create product via XML (POST XML)

Command:

```bash
curl -i -X POST http://localhost:8080/api/products \
     -H 'Content-Type: application/xml' \
     -H 'Accept: application/xml' \
     -d '<product><name>Fork</name><sku>FK-01</sku><price>3.50</price><stock>25</stock></product>'
```

Actual response:

```http
HTTP/1.1 201 Created
Location: http://localhost:8080/api/products/5
Content-Type: application/xml
Content-Length: 150

<?xml version="1.0" encoding="UTF-8" standalone="yes"?><product id="5"><name>Fork</name><sku>FK-01</sku><price>3.50</price><stock>25</stock></product>
```

Result: PASSED — Product deserialized from XML payload, assigned ID 5, and returned with XML representation and Location header.

---

## Test 5: XML Round-Trip — Retrieve product via XML (GET XML)

Command:

```bash
curl -i http://localhost:8080/api/products/5 -H 'Accept: application/xml'
```

Actual response:

```http
HTTP/1.1 200 OK
Content-Type: application/xml
Content-Length: 150

<?xml version="1.0" encoding="UTF-8" standalone="yes"?><product id="5"><name>Fork</name><sku>FK-01</sku><price>3.50</price><stock>25</stock></product>
```

Result: PASSED — Verified that product created via XML is correctly retrievable in XML.

---

## Test 6: Replace product representation (PUT 200)

Command:

```bash
curl -i -X PUT http://localhost:8080/api/products/2 -H 'Content-Type: application/json' \
     -d '{"name":"Large Mug","sku":"MG-01","price":8.00,"stock":35}'
```

Actual response:

```http
HTTP/1.1 200 OK
Content-Type: application/json
Content-Length: 65

{"id":2,"name":"Large Mug","price":8.00,"sku":"MG-01","stock":35}
```

Result: PASSED — Product with ID 2 successfully replaced and returned with HTTP 200 OK.

---

## Test 7: Replace non-existent product (PUT 404)

Command:

```bash
curl -i -X PUT http://localhost:8080/api/products/99 -H 'Content-Type: application/json' \
     -d '{"name":"Ghost","sku":"GH-01","price":1.00,"stock":1}'
```

Actual response:

```http
HTTP/1.1 404 Not Found
Content-Length: 0
```

Result: PASSED — Updating missing product returns HTTP 404 Not Found.

---

## Test 8: Adjust stock successfully (PATCH 200)

Command:

```bash
curl -i -X PATCH http://localhost:8080/api/products/1/stock -H 'Content-Type: application/json' \
     -d '{"delta":-2}'
```

Actual response:

```http
HTTP/1.1 200 OK
Content-Type: application/json
Content-Length: 63

{"id":1,"name":"Kettle","price":24.90,"sku":"KT-01","stock":10}
```

Result: PASSED — Stock decremented from 12 to 10 and updated product returned with HTTP 200 OK.

---

## Test 9: Adjust stock with insufficient inventory (PATCH 409)

Command:

```bash
curl -i -X PATCH http://localhost:8080/api/products/3/stock -H 'Content-Type: application/json' \
     -d '{"delta":-1}'
```

Actual response:

```http
HTTP/1.1 409 Conflict
Content-Length: 0
```

Result: PASSED — Delta would result in negative stock (0 + (-1) = -1); domain rule rejected it with HTTP 409 Conflict via `InsufficientStockException`.

---

## Test 10: Delete existing product (DELETE 204)

Command:

```bash
curl -i -X DELETE http://localhost:8080/api/products/4
```

Actual response:

```http
HTTP/1.1 204 No Content
```

Result: PASSED — Product 4 successfully removed; returned HTTP 204 No Content with no body.

---

## Test 11: Delete non-existent product (DELETE 404)

Command:

```bash
curl -i -X DELETE http://localhost:8080/api/products/4
```

Actual response:

```http
HTTP/1.1 404 Not Found
Content-Length: 0
```

Result: PASSED — Deleting an already deleted or non-existent product returns HTTP 404 Not Found.

---

## Test 12: Unsupported media type (POST text/plain 415)

Command:

```bash
curl -i -X POST http://localhost:8080/api/products -H 'Content-Type: text/plain' \
     -d 'Teapot'
```

Actual response:

```http
HTTP/1.1 415 Unsupported Media Type
Content-Length: 0
```

Result: PASSED — Content-Type `text/plain` rejected by Jersey with HTTP 415 Unsupported Media Type.

---

## Summary

All CRUD operations, HTTP status codes (200, 201, 204, 404, 409, 415), Location header, XML/JSON representations, and domain business rules verified.
