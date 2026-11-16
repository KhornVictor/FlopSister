# FlopSister (`shop-rest`) — Lab 04: RESTful Web Services (Jakarta REST / JAX-RS)

A RESTful e-commerce catalogue and orders API built with **Jakarta REST 4.0 (Eclipse Jersey 4)** on an embedded **Grizzly** HTTP server using **JDK 25** and **Maven**.

---

## Contributors & Project Information

- **Institution:** Institute of Technology of Cambodia (ITC)
- **Department:** Information and Communication Engineering (GIC)
- **Class / Academic Year:** I4-GIC-A · Semester 1
- **Course:** Software Engineering — Lab 04: RESTful Web Services (JAX-RS)
- **Project Repository:** [`https://github.com/KhornVictor/FlopSister`](https://github.com/KhornVictor/FlopSister)

### Group Members & Contributors

| Name | Student ID | Task |
|---|---|---|
| BE Mengseang | `e20230455` | Task 1 |
| KHORN Victor | `e20230078` | Task 0, Task 2, Challenge |
| HENG Meylorng | `e20230260` | Task 3 |
| HOUR Samnang | `e20230372` | Task 4 |
| KIM Boramey | `e20231250` | Task 5 |

---

## Implemented Scope

- **Task 1 — First Resource & Server:** Embedded Grizzly bootstrap (`edu.itc.shop.Main`), HK2 singleton `AbstractBinder` DI, thread-safe `InMemoryProductRepository`, stateless `ProductResource`, and `GET /products` + `GET /products/{id: \d+}` (`404` on unknown or non-numeric IDs).
- **Task 2 — Full CRUD, JSON & XML:** Dual JSON (JSON-B/Yasson) and XML (JAXB) representations via `ProductDto` (hiding internal `version`), `POST` (`201` + `Location` via `UriInfo`), `PUT` (`200`/`404`), `DELETE` (`204`/`404`), `PATCH /{id}/stock` (`200`/`409`), and domain exceptions `DuplicateSkuException` / `InsufficientStockException`.
- **Task 3 — Parameters & Validation:** `Money` domain value object with `MoneyConverterProvider` (`ParamConverterProvider`), `@BeanParam @Valid ProductFilter` (`q`, `minPrice`, `maxPrice`, `inStock`, whitelisted `sort`), Bean Validation on DTOs/query params, and RFC 9457 `application/problem+json` via `ValidationMapper` (`400`) and `ProblemMapper` (`404`, `409`, `415`, `500` with UUID reference).
- **Task 4 — Providers & Filters:** `CorrelationFilter` (`X-Correlation-Id` propagation/generation + access logging with duration), `CorsFilter` (`@PreMatching` `OPTIONS` preflight + CORS headers), `ApiKeyFilter` (`@PreMatching`, `@Priority(Priorities.AUTHENTICATION)`, `shop.api.keys` defaulting to `demo-123`, `401` `problem+json` + `WWW-Authenticate`), and selective `@Compress` `@NameBinding` with `GzipWriterInterceptor`.
- **Task 5 — Pagination, Caching, Versioning & Reviews:** `page`/`size` pagination with `X-Total-Count` and RFC 8288 `Link` headers (`first`, `prev`, `next`, `last`), `ETag`/`Cache-Control` with `304 Not Modified` and conditional `PUT` (`428`/`412`/`200`), media-type versioning (`application/vnd.shop.v2+json` via `ProductV2` while preserving v1 default), and `/products/{id}/reviews` sub-resource locator (`ReviewResource`, `ReviewRepository`, `InMemoryReviewRepository`).
- **Challenge — Orders, HATEOAS, Idempotency, OpenAPI & Full Test Suite:**
  - Orders sub-API (`OrderResource`, `OrderService`, `InMemoryOrderRepository`, `Order`, `Line`, `OrderStatus`) supporting atomic stock reservation, `NEW -> PAID`, and `NEW -> CANCELLED` (with stock restoration).
  - HATEOAS `Link` headers and `_links` maps (`self`, conditional `pay`/`cancel` while `NEW`, and `product-<id>` links).
  - Race-free idempotent `POST /orders` backed by a bound singleton `ConcurrentMap` (`computeIfAbsent`) and SHA-256 payload hashing (`Hashes`), returning `Idempotency-Replayed: true|false`, `428` when missing `Idempotency-Key`, and `422` on body mismatch.
  - Build-time OpenAPI 3.1 schema generation (`smallrye-open-api-maven-plugin` -> `target/classes/META-INF/openapi.yaml`) served at `GET /api/openapi` (`application/yaml`) via `OpenApiResource`.
  - Integration test suites in `ShopApiTest` (13+ HTTP status codes + HATEOAS + concurrency + OpenAPI + gzip), `Task3ApiTest`, and `Task5ApiTest`.

---

## Stateless Resource Design

Resource classes (`ProductResource`, `ReviewResource`, `OrderResource`, `OpenApiResource`) hold no mutable per-request fields—only references to thread-safe singleton repositories/services injected via constructor—so concurrent HTTP requests never share request-specific state.

---

## How to Build, Test, and Run

### Run the Full Verification Suite

```bash
./mvnw -q verify
```

On Windows PowerShell:

```powershell
.\mvnw.cmd -q verify
```

`mvn verify` compiles the classes, runs `smallrye-open-api-maven-plugin:generate-schema` during `process-classes` to write `target/classes/META-INF/openapi.yaml`, and executes all JUnit / `JerseyTest` suites (`ShopApiTest`, `Task3ApiTest`, `Task5ApiTest`, `ShopApplicationTests`).

### Start the Server

```bash
./mvnw -q compile exec:java
```

On Windows PowerShell:

```powershell
.\mvnw.cmd -q compile exec:java
```

The server listens on **`http://localhost:8080/api/`**.  
Write endpoints (`POST`, `PUT`, `PATCH`, `DELETE`) require header `X-Api-Key: demo-123` (configurable via `-Dshop.api.keys=key1,key2`).

---

## API Endpoints Summary

| Method | Path | Purpose | Key Headers / Status Codes |
|---|---|---|---|
| `GET` | `/products` | Filter, sort, paginate products (`@Compress`) | `200`, `400`; `X-Total-Count`, `Link`, `Content-Encoding: gzip` |
| `GET` | `/products/{id}` | Retrieve product (`v1` JSON/XML or `v2` JSON) | `200`, `304`, `404`, `406`; `ETag`, `Cache-Control` |
| `POST` | `/products` | Create product (`JSON` or `XML`) | `201` (`Location`), `400`, `401`, `409`, `415` |
| `PUT` | `/products/{id}` | Replace product conditionally | `200` (`ETag`), `400`, `401`, `404`, `409`, `412`, `415`, `428` |
| `PATCH` | `/products/{id}/stock` | Adjust stock delta (`JSON`) | `200`, `400`, `401`, `404`, `409`, `415` |
| `DELETE` | `/products/{id}` | Delete product | `204`, `401`, `404` |
| `GET` | `/products/{id}/reviews` | List reviews for product | `200`, `404`, `406` |
| `POST` | `/products/{id}/reviews` | Create product review | `201` (`Location`), `400`, `401`, `404`, `415` |
| `POST` | `/orders` | Idempotent order creation | `201` (`Location`, `Idempotency-Replayed`), `400`, `401`, `404`, `409`, `415`, `422`, `428` |
| `GET` | `/orders/{id}` | Retrieve order + HATEOAS links | `200`, `404`, `406`; `Link`, `_links` |
| `PUT` | `/orders/{id}/payment` | Pay `NEW` order (`NEW -> PAID`) | `200`, `401`, `404`, `409` |
| `DELETE` | `/orders/{id}` | Cancel `NEW` order (`NEW -> CANCELLED`) | `200`, `401`, `404`, `409` |
| `GET` | `/openapi` | Serve generated OpenAPI 3.1 YAML | `200` (`application/yaml`), `404` |

### HTTP Transcripts

- Task 1: [`docs/task1-http.md`](docs/task1-http.md)
- Task 2: [`docs/task2-http.md`](docs/task2-http.md)
- Task 3: [`docs/task3-http.md`](docs/task3-http.md)
- Task 4: [`docs/task4-http.md`](docs/task4-http.md)
- Task 5: [`docs/task5-http.md`](docs/task5-http.md)
- Challenge: [`docs/challenge-http.md`](docs/challenge-http.md)

---

## JAX-RS vs Spring MVC

### Side-by-Side Comparison (`GET /products/{id}` and `POST /products`)

#### Jakarta REST 4.0 (JAX-RS / Jersey — This Lab)

```java
@Path("/products")
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public class ProductResource {

    private final ProductRepository repository;

    @Inject
    public ProductResource(ProductRepository repository) {
        this.repository = repository;
    }

    @GET
    @Path("/{id: \\d+}")
    public Response findById(@PathParam("id") long id, @Context Request request) {
        Product product = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product " + id + " not found"));
        EntityTag tag = new EntityTag(Integer.toHexString(product.version()));
        Response.ResponseBuilder cond = request.evaluatePreconditions(tag);
        if (cond != null) {
            return cond.tag(tag).build();
        }
        return Response.ok(ProductDto.from(product)).tag(tag).build();
    }

    @POST
    public Response create(@Valid @NotNull ProductDto dto, @Context UriInfo uriInfo) {
        repository.findBySku(dto.sku).ifPresent(p -> {
            throw new DuplicateSkuException(dto.sku);
        });
        Product saved = repository.save(dto.toProduct(0, 0));
        URI location = uriInfo.getAbsolutePathBuilder()
                .path(Long.toString(saved.id()))
                .build();
        return Response.created(location).entity(ProductDto.from(saved)).build();
    }
}
```

#### Spring MVC (Lab 02 Style)

```java
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository repository;

    public ProductController(ProductRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<ProductDto> findById(@PathVariable long id, WebRequest request) {
        Product product = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Product " + id + " not found"));
        String etag = Integer.toHexString(product.version());
        if (request.checkNotModified(etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(etag).build();
        }
        return ResponseEntity.ok().eTag(etag).body(ProductDto.from(product));
    }

    @PostMapping
    public ResponseEntity<ProductDto> create(
            @Valid @RequestBody ProductDto dto,
            UriComponentsBuilder uriBuilder) {
        repository.findBySku(dto.sku).ifPresent(p -> {
            throw new DuplicateSkuException(dto.sku);
        });
        Product saved = repository.save(dto.toProduct(0, 0));
        URI location = uriBuilder.path("/api/products/{id}").buildAndExpand(saved.id()).toUri();
        return ResponseEntity.created(location).body(ProductDto.from(saved));
    }
}
```

### Eight Key Differences

| Aspect | Jakarta REST (JAX-RS / Jersey) | Spring MVC |
|---|---|---|
| **1. Annotations** | Standard `jakarta.ws.rs.*` (`@Path`, `@GET`, `@POST`, `@Produces`, `@Consumes`, `@PathParam`, `@BeanParam`) | Spring-specific (`@RestController`, `@RequestMapping`, `@GetMapping`, `@PostMapping`, `@PathVariable`) |
| **2. Body binding** | Any single unannotated method parameter (e.g., `@Valid @NotNull ProductDto dto`) is bound as the request entity by `MessageBodyReader` | Explicit `@RequestBody` annotation is required on the parameter for `HttpMessageConverter` binding |
| **3. Response building** | `jakarta.ws.rs.core.Response` fluent builder (`Response.created(uri).entity(body).tag(etag).links(...).build()`) | `org.springframework.http.ResponseEntity<T>` (`ResponseEntity.created(uri).eTag(etag).body(body)`) |
| **4. Errors** | `ExceptionMapper<T>` `@Provider` classes (e.g., `ProblemMapper`, `ValidationMapper`) plus `WebApplicationException` hierarchy | `@RestControllerAdvice` / `@ExceptionHandler` methods (or Spring's `ProblemDetail` / `ResponseEntityExceptionHandler`) |
| **5. Filters** | `ContainerRequestFilter` (with `@PreMatching` & `@Priority`), `ContainerResponseFilter`, and `@NameBinding` `WriterInterceptor` | Servlet `Filter` / `OncePerRequestFilter`, `HandlerInterceptor`, and `ResponseBodyAdvice` |
| **6. JSON library** | Jakarta JSON-B (`Yasson` via `jersey-media-json-binding`) by default, alongside JAXB for XML | Jackson (`jackson-databind`) by default via `MappingJackson2HttpMessageConverter` |
| **7. DI** | HK2 (`AbstractBinder` in `ResourceConfig`, `@Inject` from `jakarta.inject`) or CDI in Jakarta EE runtimes | Spring IoC ApplicationContext (`@Component`, `@Service`, `@Bean`, automatic constructor injection) |
| **8. Testing** | `JerseyTest` extending JUnit with `configure()` starting an embedded Grizzly test container and JAX-RS `Client` | `@WebMvcTest` with `MockMvc` (mocked servlet layer) or `@SpringBootTest` with `TestRestTemplate` / `WebTestClient` |
