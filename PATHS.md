# Structure of Each Tasks

## Task 0: Project Setup & Baseline Toolchain

Files and directories created during the initial setup:

```text
shop-rest/
├── pom.xml                                 # Jersey BOM, Grizzly container, JSON-B, JAXB, Bean Validation, JUnit
├── README.md                               # Project documentation starter
├── .gitignore                              # Git ignore rules (target/, .idea/, *.iml)
├── docs/                                   # Empty directory placeholder for curl transcripts
└── src/
    ├── main/java/edu/itc/shop/             # Application source root
    └── test/java/edu/itc/shop/             # Test source root
```

## Task 1: First Resource and Server

Files introduced for embedded Grizzly bootstrap, in-memory repository, and initial product endpoints:

```text
shop-rest/
├── README.md                               # Stateless resource rationale note
├── docs/
│   └── task1-http.md                       # curl transcripts: list, id 2, 404 (id 99), 404 (non-digit abc)
└── src/main/java/edu/itc/shop/
    ├── Main.java                           # Grizzly bootstrap, BASE URI, and ResourceConfig DI binder
    ├── domain/
    │   ├── InMemoryProductRepository.java  # Thread-safe repository using ConcurrentHashMap & AtomicLong
    │   ├── Product.java                    # Domain record (id, name, sku, price, stock, version)
    │   └── ProductRepository.java          # Repository interface (findAll, findById, findBySku, save, delete)
    └── api/
        └── ProductResource.java            # @Path("/products") resource with GET list and GET {id: \d+}
```

## Task 2: Full CRUD with Status Codes, Headers, JSON and XML

Files introduced for full CRUD operations, DTO dual JSON/XML representation, and business exceptions:

```text
shop-rest/
├── docs/
│   └── task2-http.md                       # HTTP transcripts: POST 201, 409 duplicate, XML, PATCH, DELETE 204/404, 415
└── src/main/java/edu/itc/shop/
    ├── domain/
    │   ├── DuplicateSkuException.java      # ClientErrorException (409 Conflict) for duplicate SKU
    │   └── InsufficientStockException.java # ClientErrorException (409 Conflict) for negative stock
    └── api/
        ├── ProductResource.java            # Updated CRUD endpoints (POST, PUT, DELETE, PATCH /stock)
        └── dto/
            ├── ProductDto.java             # JAXB-annotated class for XML & JSON (excludes version)
            └── StockChange.java            # Record DTO for stock adjustment delta
```

## Task 3: Parameters and Validation

Files introduced for query parameter grouping, Money converter, Bean Validation, and RFC 9457 error mappers:

```text
shop-rest/
├── docs/
│   └── task3-http.md                       # HTTP transcripts: filtered/sorted, minPrice=abc 400, validation 400, 404, 500
└── src/main/java/edu/itc/shop/
    ├── domain/
    │   └── Money.java                      # Domain record (amount, currency) with parse() and toString()
    └── api/
        ├── ProductResource.java            # GET /products updated with @BeanParam @Valid ProductFilter
        ├── dto/
        │   ├── Problem.java                # RFC 9457 application/problem+json representation
        │   ├── ProductDto.java             # Bean Validation constraints on name, sku, price, stock
        │   └── StockChange.java            # Bean Validation constraints on delta (@Min, @Max)
        ├── error/
        │   ├── ProblemMapper.java          # ExceptionMapper<Throwable> for RFC 9457 & 500 reference IDs
        │   └── ValidationMapper.java       # ExceptionMapper<ConstraintViolationException> for 400 field errors
        └── params/
            ├── MoneyConverterProvider.java # ParamConverterProvider returning ParamConverter<Money>
            └── ProductFilter.java          # @BeanParam for q, minPrice, maxPrice, inStock, sort whitelist
```

## Task 4: Providers and Filters

Files introduced for cross-cutting filters, authentication, CORS, correlation logging, and gzip compression:

```text
shop-rest/
├── docs/
│   └── task4-http.md                       # HTTP transcripts: 401 missing/invalid key, 201 valid, OPTIONS CORS, gzip
└── src/main/java/edu/itc/shop/api/
    ├── ProductResource.java                # List endpoint annotated with @Compress
    └── filter/
        ├── ApiKeyFilter.java               # @PreMatching @Priority(AUTHENTICATION) filter for X-Api-Key
        ├── Compress.java                   # @NameBinding annotation for selective compression
        ├── CorrelationFilter.java          # ContainerRequestFilter/ResponseFilter for X-Correlation-Id & access log
        ├── CorsFilter.java                 # CORS headers and @PreMatching OPTIONS preflight handler
        └── GzipWriterInterceptor.java      # WriterInterceptor wrapping output stream with GZIPOutputStream
```

## Task 5: Pagination, Caching and Versioning

Files introduced for pagination Link headers, ETag preconditions, v2 vendor media type, and review sub-resource:

```text
shop-rest/
├── docs/
│   └── task5-http.md                       # HTTP transcripts: pagination Links, ETag 304/412/428, v1/v2, reviews CRUD
└── src/main/java/edu/itc/shop/
    ├── Main.java                           # Register ReviewRepository binding in ResourceConfig
    ├── domain/
    │   ├── InMemoryProductRepository.java  # Extended with 30 additional seeded products
    │   ├── InMemoryReviewRepository.java   # ConcurrentHashMap implementation for reviews
    │   ├── Review.java                     # Review record (id, productId, stars, author, text)
    │   └── ReviewRepository.java           # Review repository interface
    └── api/
        ├── ProductResource.java            # Pagination Link headers, ETag preconditions, v2 endpoint, sub-resource locator
        ├── ReviewResource.java             # Sub-resource handling GET and POST /products/{id}/reviews
        ├── dto/
        │   ├── CreateReviewDto.java        # Validated DTO for review creation (stars, author, text)
        │   └── ProductV2.java              # Vendor v2 representation (Money price, available boolean)
        └── params/
            └── ProductFilter.java          # Added page and size query parameters with Bean Validation
```

## Challenge: Orders with HATEOAS, Idempotent POST, OpenAPI and Full Test Suite

Files introduced for orders sub-API, HATEOAS navigation, idempotent retries, OpenAPI 3.1 documentation, and test suite:

```text
shop-rest/
├── pom.xml                                 # Adds MicroProfile OpenAPI dependency & SmallRye Maven plugin
├── README.md                               # "JAX-RS vs Spring MVC" comparison and 8-point differences table
├── docs/
│   └── challenge-http.md                   # HTTP transcripts: HATEOAS orders, idempotency replay/mismatch, OpenAPI YAML
└── src/
    ├── main/java/edu/itc/shop/
    │   ├── Main.java                       # Register OrderService & idempotency ConcurrentMap singletons
    │   ├── domain/
    │   │   ├── InMemoryOrderRepository.java# ConcurrentHashMap repository for order entities
    │   │   ├── Line.java                   # Order item record (productId, quantity)
    │   │   ├── Order.java                  # Order aggregate record (id, lines, total, status)
    │   │   ├── OrderRepository.java        # Order repository interface
    │   │   ├── OrderService.java           # Business logic: stock reservation, order state transitions
    │   │   └── OrderStatus.java            # Order lifecycle enum (NEW, PAID, CANCELLED)
    │   ├── api/
    │   │   ├── OpenApiResource.java        # @Path("/openapi") serving generated openapi.yaml
    │   │   ├── OrderResource.java          # Orders API with HATEOAS links, idempotent POST, pay, cancel
    │   │   ├── dto/
    │   │   │   ├── LineDto.java            # Line item DTO with @Min(1) quantity constraint
    │   │   │   ├── OrderDto.java           # Response DTO containing HATEOAS _links map
    │   │   │   └── PlaceOrder.java         # Order placement command DTO
    │   │   └── util/
    │   │       └── Hashes.java             # SHA-256 payload hashing utility for idempotency keys
    └── test/java/edu/itc/shop/
        └── ShopApiTest.java                # Full JerseyTest suite verifying 13+ HTTP status codes
```
