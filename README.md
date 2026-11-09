# Task 4 — Entity states, dirty checking, and optimistic locking

This task is implemented in the existing `edu.itc.se.shop` application. The original workspace did not contain JPA entities or repositories, so the small `Customer`, `Product`, `Order`, and `Tag` entities and their repositories were added to support the requested tests. Test schema is created and dropped by the `test` profile.

## Database configuration

The application uses PostgreSQL from Docker Compose at `localhost:5432`; set `POSTGRES_PASSWORD` in the same PowerShell session before starting the application. H2 is test-scoped and the test profile explicitly selects an in-memory H2 database, so automated tests do not require Docker.

```powershell
$env:POSTGRES_PASSWORD = "your-local-password"
docker compose up -d postgres
.\mvnw.cmd spring-boot:run
```

Do not commit a real database password. The Compose database stores its initialization credentials in the named volume, so changing the environment variable later does not automatically change the password already stored in that volume.

## What the tests demonstrate

- `OrderService.markPaid` changes a managed order without calling `save()`. `OrderServiceTest` clears Hibernate statistics first, then verifies the reloaded status and exactly one entity update.
- A `Product` returned by a repository call is detached after that call's transaction ends. Changing it does not persist by itself. Calling `save(detached)` uses JPA `merge`; in this test it produces one SELECT and one UPDATE.
- `OrderService.wrongReadOnlyChangeStatus` is deliberately incorrect: because the transaction is read-only, Hibernate uses manual flush behavior and does not persist its accidental mutation. `OrderService.view` is the intended read-only DTO method.
- `FlushOrderTest` demonstrates that Hibernate may execute the insert before the queued delete, causing the unique `tags.name` constraint to fail. Flushing after the delete and before inserting the replacement succeeds.
- `OptimisticLockingTest` uses two real threads and separate `TransactionTemplate` transactions. Both first load the same version; one commit wins and the other receives `ObjectOptimisticLockingFailureException`. The facade retries outside the failed transaction, allowing both updates to complete.

Run the focused tests with:

```powershell
.\mvnw.cmd -q '-Dtest=OrderServiceTest,FlushOrderTest,OptimisticLockingTest' test
```

## Representative test log

The following lines are from the two-thread optimistic-lock test (timestamps and thread IDs abbreviated):

```text
TRACE ... TransactionInterceptor : Getting transaction for [edu.itc.se.shop.service.OrderService.changeStatus]
DEBUG ... org.hibernate.SQL       : select o1_0.id,o1_0.customer_id,o1_0.placed_at,o1_0.status,o1_0.version from orders o1_0 where o1_0.id=?
DEBUG ... org.hibernate.SQL       : select o1_0.id,o1_0.customer_id,o1_0.placed_at,o1_0.status,o1_0.version from orders o1_0 where o1_0.id=?
DEBUG ... org.hibernate.SQL       : update orders set customer_id=?,placed_at=?,status=?,version=? where id=? and version=?
DEBUG ... org.hibernate.SQL       : update orders set customer_id=?,placed_at=?,status=?,version=? where id=? and version=?
DEBUG ... JpaTransactionManager   : Initiating transaction rollback after commit exception
DEBUG ... JpaTransactionManager   : Rolling back JPA transaction on EntityManager [...]
org.springframework.orm.ObjectOptimisticLockingFailureException: Unexpected row count (expected row count 1 but was 0) [update orders ... where id=? and version=?]
```

The conflicting UPDATEs both include the version in the `WHERE` clause. After one changes the version, the other affects zero rows and is rolled back. The retry begins a new transaction and reloads the winning version before updating.

# Task 5 — N+1, Performance Tuning, and Database Migration

## Objectives

This task focuses on improving Hibernate performance and database management through:

- Solving N+1 query problems.
- Comparing Hibernate fetching strategies.
- Configuring JDBC batch processing.
- Managing lazy loading and transactions.
- Implementing Flyway database migrations and indexes.
- Analyzing SQL execution plans using `EXPLAIN`.

## N+1 Query Optimization

Implemented four fetching strategies in `ReportService` and tested them using 50 paid orders with 3 order lines each.

| Fetching strategy | SQL statements |
|---|---:|
| Naive | 51 |
| JOIN FETCH | 1 |
| EntityGraph | 1 |
| BatchSize (16) | 5 |

**Result:** JOIN FETCH and EntityGraph reduced the SQL statements from 51 to 1.

### Performance benchmark

Using 5 warm-up runs and 20 measured runs:

| Strategy | Median time |
|---|---:|
| Naive | 3.944 ms |
| JOIN FETCH | 1.224 ms |
| EntityGraph | 1.642 ms |
| BatchSize (16) | 2.285 ms |

JOIN FETCH achieved the lowest median execution time in this benchmark.

## Product N+1 Optimization

Tested loading 150 unique products through 50 paid orders.

| Strategy | SQL statements |
|---|---:|
| Naive product fetching | 151 |
| BatchSize (32) | 6 |

**Result:** Batch fetching reduced SQL statements from 151 to 6 while retrieving all product names.

## JDBC Batch Insert

Configured Hibernate in `application.yml`:

```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 50
        order_inserts: true
        order_updates: true
```

`BatchInsertTest` inserts 1,000 products in one transaction.

| Metric | Result |
|---|---:|
| Inserted entities | 1,000 |
| Prepared statements | 22 |
| Batch size | 50 |

Hibernate TRACE logs confirmed JDBC batch execution.

### SEQUENCE vs IDENTITY

| Strategy | Inserted entities | Prepared statements |
|---|---:|---:|
| SEQUENCE | 1,000 | 22 |
| IDENTITY | 1,000 | 1,000 |

**Conclusion:** SEQUENCE supports effective JDBC insert batching, while IDENTITY prevents normal JDBC insert batching in this experiment. The main project retains SEQUENCE with allocation size 50.

## Lazy Loading and Transaction Management

Disabled Open Session in View:

```yaml
spring:
  jpa:
    open-in-view: false
```

Implemented `OrderService.viewDetails()` to map lazy-loaded relationships into `OrderDetailsDto` inside a read-only transaction.

`LazyLoadingTest` verified:

- Accessing lazy relationships after the transaction causes `LazyInitializationException`.
- Mapping order details inside the transaction succeeds.

Both tests passed.

## Flyway Database Migration

Added the Spring Boot Flyway starter.

**V1 — Initial schema**

`src/main/resources/db/migration/V1__initial_schema.sql`

Creates customers, products, tags, orders, order lines, join tables, constraints, and sequences.

**V2 — Indexes**

`src/main/resources/db/migration/V2__indexes.sql`

```sql
CREATE INDEX IF NOT EXISTS idx_orders_customer_placed
ON orders (customer_id, placed_at DESC);

CREATE INDEX IF NOT EXISTS idx_order_line_order
ON order_lines (order_id);

CREATE INDEX IF NOT EXISTS idx_product_name_stock
ON products (name, stock);
```

`FlywayMigrationTest` verified:

- V1 and V2 successfully applied.
- Both migration versions recorded in Flyway history.
- All three indexes exist.
- No pending migrations remain.

## EXPLAIN Query Plan Analysis

Compared execution plans before and after V2 using H2.

| Query | Index used before | Index used after |
|---|---|---|
| Orders by status and customer | `FK_ORDERS_CUSTOMER_INDEX_8` | Same |
| Order lines by order ID | `FK_ORDER_LINES_ORDER_INDEX_2` | Same |

**Analysis:** H2 continued using its existing foreign-key indexes. Although all three V2 indexes were successfully created, the tested execution plans did not change. Therefore, this experiment does not claim a measured query-performance improvement from the new indexes.

## Test Verification

The following tests were individually executed successfully:

| Test class | Verification |
|---|---|
| `NPlusOneTest` | Fetching strategies and query counts |
| `PerformanceBenchmarkTest` | Execution-time comparison |
| `ProductNPlusOneTest` | Product N+1 optimization |
| `BatchInsertTest` | JDBC batch inserts |
| `LazyLoadingTest` | Lazy loading and transactional DTO |
| `FlywayMigrationTest` | Migrations, indexes, and EXPLAIN |
| `SchemaInspectionTest` | Tables, constraints, and sequences |

Run the complete test suite:

```bash
bash mvnw test
```

## Conclusion

Task 5 demonstrates Hibernate performance optimization, transaction management, JDBC batching, and database migration.

JOIN FETCH reduced collection queries from 51 to 1, and batch fetching reduced product queries from 151 to 6. JDBC batching successfully inserted 1,000 products using the SEQUENCE strategy. Lazy-loading issues were handled through transactional DTO mapping, while Flyway successfully applied and verified V1 and V2 migrations.

These implementations demonstrate practical understanding of ORM performance, database schema management, and SQL query analysis.
