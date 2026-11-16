# FlopSister — RESTful Web Services

## Task 1: Product REST API
This task implements a RESTful API using Jakarta REST (JAX-RS), Jersey, and an embedded Grizzly HTTP server.

### Base URL
`http://localhost:8080/api/`

### Available Endpoints (Task 1)
| Method | Endpoint | Description |
|---|---|---|
| GET | `/products` | Retrieve all products |
| GET | `/products/{id}` | Retrieve a product by ID |

### Product Repository
The application uses an `InMemoryProductRepository` backed by `ConcurrentHashMap` and `AtomicLong`.
Three products are seeded when the repository is created:
- Kettle (KT-01)
- Mug (MG-01)
- Tea (TE-01)

### Dependency Injection
Jersey uses `AbstractBinder` to bind one `ProductRepository` instance. `ProductResource` receives the repository through constructor injection.

### Stateless Resource Design
The resource class has no mutable fields because it is stateless and can safely handle multiple HTTP requests without sharing request-specific state.

### HTTP Test Evidence (Task 1)
See [`docs/task1-http.md`](docs/task1-http.md) for the four HTTP test commands and their actual responses.

---

## Task 2: Full CRUD with Status Codes, Headers, JSON and XML

Task 2 completes the CRUD operations for `/products`, adds content negotiation for JSON and XML via `ProductDto`, and enforces domain business rules.

### Endpoints (Task 2)
| Method | Endpoint | Request Body | Response Status & Body | Description |
|---|---|---|---|---|
| GET | `/products` | — | 200 OK (List of `ProductDto`) | Retrieve all products |
| GET | `/products/{id}` | — | 200 OK (`ProductDto`) / 404 | Retrieve product by ID |
| POST | `/products` | `ProductDto` (JSON/XML) | 201 Created + `Location` header / 409 / 415 | Create new product |
| PUT | `/products/{id}` | `ProductDto` (JSON/XML) | 200 OK (`ProductDto`) / 404 / 409 | Replace product details |
| DELETE | `/products/{id}` | — | 204 No Content / 404 | Delete product |
| PATCH | `/products/{id}/stock` | `StockChange` (JSON only) | 200 OK (`ProductDto`) / 404 / 409 | Adjust inventory stock |

### DTO and Representations
- **`ProductDto`**: JAXB and JSON-B annotated DTO class serving both `application/json` and `application/xml`. The internal entity `version` field is not exposed.
- **`StockChange`**: Record DTO carrying `delta` for stock adjustment (`application/json` only).

### Business Rules & Error Handling
- **Duplicate SKU**: If creating a product or updating SKU to an existing SKU, domain throws `DuplicateSkuException` returning `409 Conflict`.
- **Negative Stock**: If stock adjustment would cause negative stock, `Product.adjustStock` throws `InsufficientStockException` returning `409 Conflict`.
- **Missing Resource**: `NotFoundException` returns `404 Not Found`.
- **Unsupported Media Type**: Non-JSON/XML bodies return `415 Unsupported Media Type`.

### Running the Application
```bash
mvn -q compile exec:java
```
Or using the Maven wrapper:
```bash
./mvnw -q compile exec:java
```

### HTTP Test Evidence (Task 2)
See [`docs/task2-http.md`](docs/task2-http.md) for the recorded HTTP test commands, headers, and responses.

---

## Task 3: Parameters and Validation

The product list accepts typed price filters, case-insensitive name search, in-stock filtering, and whitelisted sorting. Bean Validation protects product and stock-change inputs, while errors are returned as RFC 9457 `application/problem+json` responses.

### HTTP Test Evidence (Task 3)
See [`docs/task3-http.md`](docs/task3-http.md) for the HTTP checks and recorded responses.

---

## Task 4: Providers and Filters

Task 4 adds cross-cutting HTTP features using Jakarta REST (JAX-RS) providers and filters. These features include correlation IDs, CORS handling, API key authentication, and GZIP response compression.

### Correlation ID Filter

- Generates a unique UUID for each incoming request.
- Adds the ID to the response using the `X-Correlation-Id` header.
- Logs the correlation ID, HTTP method, request URI, and response status.
- Allows requests to be traced through server logs.

### CORS Filter

- Allows requests from `http://localhost:5173`.
- Supports `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, and `OPTIONS`.
- Allows the `Content-Type` and `X-Api-Key` request headers.
- Exposes the `X-Correlation-Id` response header.
- Handles `OPTIONS` preflight requests.

### API Key Authentication Filter

- Uses the `X-Api-Key` request header for authentication.
- Requires a valid API key for write operations (`POST`, `PUT`, `PATCH`, and `DELETE`).
- Allows public read operations (`GET`, `HEAD`, and `OPTIONS`) without an API key.
- Returns `401 Unauthorized` when the API key is missing or invalid.

**Development API key:** `dev-secret-key`

### GZIP Compression

- Registers Jersey's `EncodingFilter` and `GZipEncoder`.
- Compresses responses when the client sends `Accept-Encoding: gzip`.
- Uses the `Content-Encoding: gzip` response header to indicate compression.
- Includes `Vary: Accept-Encoding` so responses can be cached correctly based on the requested encoding.

### HTTP Test Evidence (Task 4)

See [`docs/task4-http.md`](docs/task4-http.md) for the HTTP test commands, expected results, and recorded responses.