# Lab-03: Hibernate Framework and Spring Data JPA

## Contributors & Project Information

- **Institution:** Institute of Technology of Cambodia (ITC)
- **Department:** Information and Communication Engineering (GIC)
- **Class / Academic Year:** I4-GIC-A · Semester 1
- **Course:** Software Engineering — C3: Hibernate Framework and Spring Data JPA
- **Project Repository:** `FlopSister` (`shop-jpa`)
- **Group Members & Contributors:**
  - **KHORN Victor**
  - **BE Mengseang**
  - **KIM Boramey**
  - **MEY Lorngheng**
  - **HOUR Samnang**

---

## Technical Stack & Configuration

- **Framework:** Spring Boot 4.1.x, Spring Data JPA
- **ORM / Persistence:** Hibernate ORM 7.4.x, Jakarta Persistence (JPA) 3.2
- **Runtime:** Java 25, Apache Maven 3.9.x
- **Databases:**
  - Runtime: In-memory H2 database (`jdbc:h2:mem:shop;DB_CLOSE_DELAY=-1`) validated against Flyway schema migrations
  - Testing: In-memory H2 database (`jdbc:h2:mem:shop-test;DB_CLOSE_DELAY=-1` / `jdbc:h2:mem:flyway-test` / `jdbc:h2:mem:bulk-test`)
- **Schema Management:** Flyway 11 (`V1__initial_schema.sql`, `V2__indexes.sql`)
- **Security Compliance:** Main configuration strictly sets `spring.jpa.hibernate.ddl-auto: validate` and contains **zero committed passwords**.

---

## 1. Domain Model & Database Schema

The shop domain contains six core tables with sequences allocating IDs in blocks of 50 (`allocationSize = 50`):

```mermaid
erDiagram
    CUSTOMER ||--o{ ORDERS : places
    ORDERS ||--|{ ORDER_LINES : contains
    PRODUCT ||--o{ ORDER_LINES : references
    PRODUCT }|--|{ TAG : tagged_with
    PRODUCT ||--o{ PRODUCT_TAG : has
    TAG ||--o{ PRODUCT_TAG : belongs_to

    CUSTOMER {
        bigint id PK
        varchar(255) email UK
        varchar(255) name
        varchar(255) street
        varchar(255) city
        varchar(255) zip
        varchar(255) ship_street
        varchar(255) ship_city
        varchar(255) ship_zip
    }

    ORDERS {
        bigint id PK
        bigint customer_id FK
        timestamp placed_at
        varchar(20) status
        bigint version
    }

    ORDER_LINES {
        bigint id PK
        bigint order_id FK
        bigint product_id FK
        integer quantity
        numeric(12_2) unit_price_amount
        char(3) unit_price_currency
    }

    PRODUCT {
        bigint id PK
        varchar(20) sku UK
        varchar(120) name
        numeric(12_2) price_amount
        char(3) price_currency
        integer stock
        varchar(20) category
        bigint version
    }

    TAG {
        bigint id PK
        varchar(255) name UK
    }

    PRODUCT_TAG {
        bigint product_id PK_FK
        bigint tag_id PK_FK
    }
```

### Database Sequences
- `customer_seq` (START 1, INCREMENT 50)
- `orders_seq` (START 1, INCREMENT 50)
- `order_line_seq` (START 1, INCREMENT 50)
- `product_seq` (START 1, INCREMENT 50)
- `tag_seq` (START 1, INCREMENT 50)

### V2 Indexes
- `idx_orders_customer_placed`: `orders(customer_id, placed_at DESC)`
- `idx_order_line_order`: `order_lines(order_id)`
- `idx_product_name_stock`: `products(name, stock)`

---

## 2. Spring Data Repository Method-to-SQL Mapping

Every repository method used in the application is mapped to its underlying SQL query (with parameterized inputs displayed as `?`):

| Repository | Method Signature | Underlying SQL Query (Parameterized) |
|---|---|---|
| `CustomerRepository` | `findByEmail(String email)` | `select c.id, c.name, c.email, c.street, c.city, c.zip, c.ship_street, c.ship_city, c.ship_zip from customer c where c.email=?` |
| `CustomerRepository` | `existsByEmail(String email)` | `select count(*) > 0 from customer c where c.email=?` |
| `CustomerRepository` | `findById(Long id)` | `select c.id, c.name, c.email, c.street, c.city, c.zip, c.ship_street, c.ship_city, c.ship_zip from customer c where c.id=?` |
| `CustomerRepository` | `save(Customer customer)` | `insert into customer (email, name, street, city, zip, ship_street, ship_city, ship_zip, id) values (?, ?, ?, ?, ?, ?, ?, ?, ?)` |
| `ProductRepository` | `existsBySku(String sku)` | `select count(*) > 0 from product p where p.sku=?` |
| `ProductRepository` | `findTop3ByCategoryOrderByPriceAmountDesc(Category cat)` | `select p.id, p.sku, p.name, p.price_amount, p.price_currency, p.category, p.stock, p.version from product p where p.category=? order by p.price_amount desc limit ?` |
| `ProductRepository` | `findByTagsName(String name)` | `select p.id, p.sku, p.name, p.price_amount, p.price_currency, p.category, p.stock, p.version from product p join product_tag pt on p.id=pt.product_id join tag t on t.id=pt.tag_id where t.name=?` |
| `ProductRepository` | `findLowStock(int min)` | `SELECT p.* FROM product p WHERE p.stock < ? ORDER BY p.stock` |
| `ProductRepository` | `findByCategory(Category cat, Pageable pageable)` | `select p.sku as sku, p.name as name, p.price_amount as amount, p.price_currency as currency, p.stock as stock from product p where p.category=? limit ? offset ?` |
| `ProductRepository` | `findForUpdate(Long id)` | `select p.id, p.sku, p.name, p.price_amount, p.price_currency, p.category, p.stock, p.version from product p where p.id=? for update` |
| `ProductRepository` | `decrementStockAtomic(Long id, int qty)` | `update product set stock = stock - ? where id = ? and stock >= ?` |
| `ProductRepository` | `findAll(Specification<Product> spec)` | `select p.id, p.sku, p.name, p.price_amount, p.price_currency, p.category, p.stock, p.version from product p where [dynamic predicates: lower(p.name) like ? and p.category=? and p.price_amount <= ? and p.stock > ?]` |
| `ProductRepository` | `findReport(...)` | `select p.sku, p.name, p.price_amount, p.stock, (select count(*) from product_tag pt where p.id=pt.product_id) from product p [left join product_tag pt join tag t on ...] where [dynamic filters] group by p.id, p.sku, p.name, p.price_amount, p.stock order by ?` |
| `OrderRepository` | `findByStatus(OrderStatus status)` | `select o.id, o.customer_id, o.placed_at, o.status, o.version from orders o where o.status=?` |
| `OrderRepository` | `findByStatusAndCustomer_Id(OrderStatus status, Long customerId)` | `select o.id, o.customer_id, o.placed_at, o.status, o.version from orders o where o.status=? and o.customer_id=?` |
| `OrderRepository` | `findByStatusWithLines(...)` | `select distinct o.id, o.customer_id, o.placed_at, o.status, o.version, l.id, l.order_id, l.product_id, l.quantity, l.unit_price_amount, l.unit_price_currency, p.id, p.sku, p.name, p.price_amount, p.price_currency, p.category, p.stock, p.version from orders o join order_lines l on o.id=l.order_id join product p on l.product_id=p.id where o.status=? and o.customer_id=?` |
| `OrderRepository` | `findWithGraphByStatusAndCustomer_Id(...)` | `select o.id, o.customer_id, o.placed_at, o.status, o.version, l.id, l.order_id, l.product_id, l.quantity, l.unit_price_amount, l.unit_price_currency, p.id, p.sku, p.name, p.price_amount, p.price_currency, p.category, p.stock, p.version from orders o left join order_lines l on o.id=l.order_id left join product p on l.product_id=p.id where o.status=? and o.customer_id=?` |
| `OrderRepository` | `findByStatusWithLinesOnly(...)` | `select distinct o.id, o.customer_id, o.placed_at, o.status, o.version, l.id, l.order_id, l.product_id, l.quantity, l.unit_price_amount, l.unit_price_currency from orders o join order_lines l on o.id=l.order_id where o.status=? and o.customer_id=?` |
| `OrderRepository` | `findByCustomerEmailAndStatusOrderByPlacedAtDesc(...)` | `select o.id, o.customer_id, o.placed_at, o.status, o.version from orders o join customer c on c.id=o.customer_id where c.email=? and o.status=? order by o.placed_at desc` |
| `OrderRepository` | `countByStatus(OrderStatus status)` | `select count(*) from orders o where o.status=?` |
| `OrderRepository` | `findFirstByCustomerIdOrderByPlacedAtDesc(Long customerId)` | `select o.id, o.customer_id, o.placed_at, o.status, o.version from orders o where o.customer_id=? order by o.placed_at desc limit ?` |
| `OrderRepository` | `findByStatus(OrderStatus status, Pageable pageable)` | `select o.id, o.placed_at, o.status, c.name, sum(l.quantity * l.unit_price_amount) from orders o join customer c on c.id=o.customer_id left join order_lines l on o.id=l.order_id where o.status=? group by o.id, o.placed_at, o.status, c.name order by ... limit ? offset ?` |
| `OrderRepository` | `findWithLinesByCustomerEmail(String email)` | `select distinct o.id, o.customer_id, o.placed_at, o.status, o.version, l.id, l.order_id, l.product_id, l.quantity, l.unit_price_amount, l.unit_price_currency, p.id, p.sku, p.name, p.price_amount, p.price_currency, p.category, p.stock, p.version from orders o join customer c on c.id=o.customer_id join order_lines l on o.id=l.order_id join product p on l.product_id=p.id where c.email=? order by o.placed_at desc` |
| `OrderRepository` | `countLinesByStatus(OrderStatus status)` | `select count(l.id) from order_lines l join orders o on o.id=l.order_id where o.status=?` |
| `OrderRepository` | `findSliceByOrderByPlacedAtDescIdDesc(Pageable pageable)` | `select o.id, o.placed_at, o.status, c.name, sum(l.quantity * l.unit_price_amount) from orders o join customer c on c.id=o.customer_id left join order_lines l on o.id=l.order_id group by o.id, o.placed_at, o.status, c.name order by o.placed_at desc, o.id desc limit ? offset ?` |
| `OrderRepository` | `findBy(ScrollPosition pos, Limit limit, Sort sort)` | `select o.id, o.customer_id, o.placed_at, o.status, o.version from orders o where (o.placed_at < ? or (o.placed_at = ? and o.id < ?)) order by o.placed_at desc, o.id desc limit ?` |
| `TagRepository` | `findByName(String name)` | `select t.id, t.name from tag t where t.name=?` |

---

## 3. Tasks 1–5 Implementation Summary

### Task 1: Entities, Value Objects, and Flyway Schema Validation
- Mapped value objects as `@Embeddable` records: `Money(BigDecimal amount, String currency)` with factory `eur(String)` and `times(int)`, and `Address(street, city, zip)`.
- Mapped entities with `@SequenceGenerator(allocationSize = 50)`: `Customer`, `Product`, `Order`, `OrderLine`, `Tag`.
- Configured `@AttributeOverride` for shipping addresses (`ship_street`, `ship_city`, `ship_zip`) and `Money` columns (`price_amount`, `price_currency`, `unit_price_amount`, `unit_price_currency`).
- Validated cleanly at startup against `V1__initial_schema.sql` with `spring.jpa.hibernate.ddl-auto: validate`.

### Task 2: Repositories and Queries
- Implemented derived query methods, `@Query` JPQL queries, native SQL queries (`findLowStock`), and Spring Data dynamic specifications in `ProductSpecs`.
- Built interface-based projections (`OrderSummary`, `ProductView`) and transactional DTO mappings (`OrderDetailsDto`).

### Task 3: Entity Lifecycle, Identity, and Cascading
- Enforced JPA-compliant entity equality using natural business keys where applicable and avoiding mutable field comparisons.
- Configured bidirectional associations with helper methods: `Order.addLine(Product, int)` and `Order.removeLine(OrderLine)`.
- Implemented cascading rules (`CascadeType.ALL`, `orphanRemoval = true`) verified in `OrderCascadeTest`.

### Task 4: Entity States, Dirty Checking, and Optimistic Locking
- Demonstrated automatic dirty checking in `OrderService.markPaid`: changes on managed entities persist at transaction commit without explicit repository `save()` invocations.
- Handled detached entities and demonstrated that read-only transactions (`@Transactional(readOnly = true)`) prevent accidental mutations.
- Resolved entity flush ordering conflicts (`FlushOrderTest`) using explicit `entityManager.flush()`.
- Demonstrated multi-threaded optimistic locking concurrency with `@Version`: concurrent commits produce `ObjectOptimisticLockingFailureException`, which is caught and retried cleanly.

### Task 5: N+1 Optimization, Batching, and Database Migration
- Tested 50 paid orders with 3 lines each across four fetching strategies:

| Fetching Strategy | SQL Statements Executed | Median Benchmark Time |
|---|---:|---:|
| Naive Fetching | 51 queries | 3.944 ms |
| `JOIN FETCH` | **1 query** | **1.224 ms** |
| `EntityGraph` | **1 query** | 1.642 ms |
| `@BatchSize(16)` | 5 queries | 2.285 ms |

- Product loading N+1: reduced queries from 151 down to 6 using batch fetching (`@BatchSize(32)`).
- JDBC batch inserts: inserted 1,000 products in 22 prepared statements using `SEQUENCE` generator, compared to 1,000 separate statements with `IDENTITY`.
- Flyway V2 migration applied and verified indexes: `idx_orders_customer_placed`, `idx_order_line_order`, `idx_product_name_stock`.

#### Flyway Schema History & EXPLAIN Execution Plan (H2 Console)

Screenshot showing `flyway_schema_history` with applied migration versions 1 and 2, along with query execution plan `EXPLAIN`:

![H2-Console-Image](H2-Console.png)

---

## 4. Challenge Deliverables (+20 Extra Credit Points)

### Part 1: Dynamic Criteria API Report (`GET /api/reports/products`)

Implemented dynamic type-safe querying in `ProductReportRepositoryImpl` using the JPA Criteria API and generated metamodel classes (`Product_`, `Tag_`, `Money_`):
- **Dynamic Predicates:** Evaluates `q` (case-insensitive substring on name), `category`, `maxPrice` (`price.amount <= maxPrice`), `minStock` (`stock >= minStock`), and `tag` (case-sensitive tag match).
- **Conditional Join:** The join to `tags` is only performed if `tag != null` to avoid unnecessary joins and cartesian overhead.
- **Aggregation & Group By:** Uses `cb.construct(ProductReportRow.class, ...)` combined with a correlated subquery for `tagCount` and groups by all product attributes.
- **Dynamic Whitelist Sorting:** Prevents SQL injection by whitelisting sort properties (`amount`, `name`, `stock`, `sku`, `tagCount`). Defaults to `amount DESC`.
- **REST Endpoint:** Exposed at `GET /api/reports/products`.

### Part 2: Infinite Scroll & Keyset Pagination (`GET /api/orders/feed`)

Exposed `GET /api/orders/feed` supporting both standard `Slice<OrderSummary>` and Keyset scrolling (`Window<Order>`):

#### Keyset Scrolling vs. Offset/Slice Pagination
- **Offset/Slice Pagination:**
  - Query: `SELECT ... FROM orders ORDER BY placed_at DESC, id DESC LIMIT 20 OFFSET 100000;`
  - **Complexity:** $O(N)$
  - **Explanation:** The database engine cannot jump directly to row 100,000. It must traverse the index and read 100,020 row entries, discarding the first 100,000 entries. Furthermore, offset pagination suffers from page-drift anomalies if rows are inserted or deleted while the user is scrolling.
- **Keyset Pagination (Seek Method):**
  - Query: `SELECT ... FROM orders WHERE (placed_at < ? OR (placed_at = ? AND id < ?)) ORDER BY placed_at DESC, id DESC LIMIT 20;`
  - **Complexity:** $O(1)$
  - **Explanation:** Using the composite index `(placed_at, id)`, the database engine performs an immediate index range seek to locate the tuple directly. It scans only the exact 20 rows requested, regardless of whether the user is at row 0 or row 10,000,000.

### Part 3: Pessimistic Stock Reservation & Concurrency Analysis

Implemented three distinct concurrency strategies in `StockService`:
1. **Pessimistic Locking (`reserve`):**
   - Method: `findForUpdate(Long id)` with `@Lock(LockModeType.PESSIMISTIC_WRITE)`.
   - SQL: `SELECT ... FROM product WHERE id = ? FOR UPDATE;`
   - Blocks competing database transactions at the database row level until the holding transaction commits.
2. **Optimistic Locking (`reserveOptimistic`):**
   - Relies on `@Version private long version;` on `Product`.
   - No row locking; detects collisions at commit time (`WHERE id = ? AND version = ?`).
   - Requires client/service layer retry loops when collisions occur.
3. **Atomic SQL Update (`reserveAtomic`):**
   - Single atomic statement: `UPDATE product SET stock = stock - ? WHERE id = ? AND stock >= ?`.
   - Returns affected row count (1 for success, 0 for out of stock / conflict). Eliminates entity state overhead completely.

#### Concurrency Benchmark Results (`StockReservationTest`)
Tested with **10 concurrent threads** simultaneously attempting to reserve 1 unit from a product with initial stock = **5**:

| Concurrency Strategy | Concurrent Threads | Initial Stock | Successful Reservations | OutOfStock Exceptions | Optimistic Collisions Handled | Final Database Stock |
|---|---:|---:|---:|---:|---:|---:|
| **Pessimistic Lock (`FOR UPDATE`)** | 10 | 5 | **5** | **5** | 0 | **0** |
| **Optimistic Lock (`@Version` + retry)** | 10 | 5 | **5** | **5** | 15+ retries | **0** |
| **Atomic SQL Update (`decrementStockAtomic`)** | 10 | 5 | **5** | **5** | 0 | **0** |

**Conclusion:** All three strategies guarantee zero overselling. Pessimistic locking provides deterministic order of operations without retries; Atomic update offers maximum throughput under extreme load; Optimistic locking avoids lock contention when conflicts are infrequent.

### Part 4: 50,000-Row Bulk Import Benchmark

Implemented and benchmarked three bulk import approaches in `BulkImportService` importing **50,000 products**:

| Import Strategy | Rows Processed | Batch Size | Elapsed Time (s) | Heap Memory Overhead | First-Level Cache (Persistence Context) |
|---|---:|---:|---:|---:|---|
| **Hibernate 7 `StatelessSession` (`insertMultiple(500)`)** | **50,000** | 500 | **~1.8 s** | **Negligible (~1 MB)** | **Bypassed completely** (No dirty checking, no entity proxies) |
| `saveAll` (Single `@Transactional` boundary, no clear) | 50,000 | 50 | ~5.8 s | High (+50 to +90 MB) | Holds all 50,000 managed entities in memory |
| `saveAll` + periodic `flush()` and `clear()` every 500 rows | 50,000 | 500 | ~3.4 s | Low (~8 MB) | Flushed and detached every 500 rows |

**Key Takeaways:**
- Hibernate 7's `StatelessSession.insertMultiple(500)` outperforms stateful persistence contexts by bypassing first-level caching, dirty check snapshotting, and interceptor cascades, executing direct multi-row JDBC batch inserts.
- Periodically flushing and clearing the `EntityManager` prevents OutOfMemory errors in long-running jobs when using standard JPA.

---

## 5. HTTP API Session Transcript (`api-session.md`)

The complete raw HTTP request/response payloads, pagination metadata, and generated SQL statement logs are documented in [`api-session.md`](api-session.md). Below is the summary of the verified endpoints:

### Endpoint 1: Paginated Orders with `OrderSummary` Projection
- **Request:** `GET /api/orders?status=PAID&page=0&size=2&sort=placedAt,desc`
- **Behavior:** Retrieves a `Page<OrderSummary>`. Interface projection prevents loading unneeded columns and uninitialized associations.
- **Generated SQL:**
  ```sql
  select o1_0.id, o1_0.status, o1_0.placed_at, c1_0.id, c1_0.name, c1_0.email
  from orders o1_0 join customer c1_0 on c1_0.id=o1_0.customer_id
  where o1_0.status=? order by o1_0.placed_at desc fetch first ? rows only;
  select count(o1_0.id) from orders o1_0 where o1_0.status=?;
  ```

### Endpoint 2: Filtered Products with `ProductSpecs` & `ProductView`
- **Request:** `GET /api/products?q=key&category=HARDWARE&maxPrice=60&inStock=true`
- **Behavior:** Dynamic JPA Criteria Specification (`ProductSpecs.filter`) querying products by substring, category, price threshold, and stock availability. Projected through `ProductView` (`sku`, `name`, `amount`, `stock`).
- **Sample Result:** Returns filtered matching items (e.g. `KB-1` Mechanical Keyboard, `KB-7` Wireless Numeric Keypad).

### Endpoint 3: Customer Orders with Joined Eager Lines
- **Request:** `GET /api/customers/{email}/orders`
- **Behavior:** Retrieves complete customer order history including all lines and products using `join fetch o.lines l join fetch l.product` to guarantee zero N+1 queries.
- **SQL:** Single query fetching orders, order lines, and product details together in one database round-trip.

### Endpoint 4: Dynamic Criteria Report (Challenge Part 1)
- **Request:** `GET /api/reports/products?category=HARDWARE&minStock=1&tag=sale&sort=amount,desc`
- **Behavior:** Type-safe CriteriaBuilder query using metamodel `Product_`, `Tag_`, `Money_`, computing `tagCount` via subquery and returning `List<ProductReportRow>`.

### Endpoint 5: Keyset Feed Infinite Scroll (Challenge Part 2)
- **Request:** `GET /api/orders/feed?limit=20` (and `?cursor=...`)
- **Behavior:** High-performance keyset scrolling with `Window<Order>` and `ScrollPosition.keyset()`, avoiding $O(N)$ offset degradation.

---

## 6. Verification & Test Execution

Run the complete automated test suite (31 tests across 19 test classes):

```powershell
.\mvnw.cmd test
```

### Targeted Test Commands
- **Flyway Migrations & Indexes:**
  ```powershell
  .\mvnw.cmd test '-Dtest=FlywayMigrationTest'
  ```
- **Database Schema Inspection:**
  ```powershell
  .\mvnw.cmd test '-Dtest=SchemaInspectionTest'
  ```
- **Entity Lifecycle, Optimistic Locking, and Dirty Checking:**
  ```powershell
  .\mvnw.cmd test '-Dtest=OrderServiceTest,FlushOrderTest,OptimisticLockingTest'
  ```
- **N+1 Optimization & Fetch Strategies:**
  ```powershell
  .\mvnw.cmd test '-Dtest=NPlusOneTest,ProductNPlusOneTest,PerformanceBenchmarkTest'
  ```
- **Challenge: Criteria Report & Infinite Scroll Feed Endpoints:**
  ```powershell
  .\mvnw.cmd test '-Dtest=ProductReportAndFeedEndpointTest'
  ```
- **Challenge: Concurrent Stock Reservation Benchmark:**
  ```powershell
  .\mvnw.cmd test '-Dtest=StockReservationTest'
  ```
- **Challenge: 50,000-Row Bulk Import Benchmark:**
  ```powershell
  .\mvnw.cmd test '-Dtest=BulkImportTest'
  ```

---

## 7. Build & Test Result Verification

```text
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running edu.itc.se.shop.domain.EntityIdentityTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running edu.itc.se.shop.domain.OrderCascadeTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running edu.itc.se.shop.repository.OrderRepositoryTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running edu.itc.se.shop.repository.ProductRepositoryTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running edu.itc.se.shop.service.BatchInsertTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running edu.itc.se.shop.service.BulkImportTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running edu.itc.se.shop.service.FlushOrderTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running edu.itc.se.shop.service.FlywayMigrationTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running edu.itc.se.shop.service.LazyLoadingTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running edu.itc.se.shop.service.NPlusOneTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running edu.itc.se.shop.service.OptimisticLockingTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running edu.itc.se.shop.service.OrderServiceTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running edu.itc.se.shop.service.PerformanceBenchmarkTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running edu.itc.se.shop.service.ProductNPlusOneTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running edu.itc.se.shop.service.SchemaInspectionTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running edu.itc.se.shop.service.StockReservationTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running edu.itc.se.shop.web.OrderControllerTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running edu.itc.se.shop.web.ProductControllerTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running edu.itc.se.shop.web.ProductReportAndFeedEndpointTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 31, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```
