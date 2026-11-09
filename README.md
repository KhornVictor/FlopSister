# Shop JPA — Domain Model, Entity Relationships & Flyway Schema

## 1. Overview

This project implements the Shop JPA domain model, entity relationships, database schema migrations, and persistence behavior.

It covers:
- Task 1: Domain model and Flyway schema.
- Task 2: Associations, cascading, and entity identity.

## 2. Database Schema

The database schema is managed by Flyway using two migrations:

- `V1__init.sql`: Creates the initial database schema.
- `V2__align_schema.sql`: Aligns table names, columns, and data types with the Java entities.

### Sequences

All five sequences use an allocation size of 50:

- `customer_seq`
- `orders_seq`
- `order_line_seq`
- `product_seq`
- `tag_seq`

### Tables

| Table | Description | Relationships |
|---|---|---|
| `customer` | Customer information and addresses | One-to-many with orders |
| `product` | Product catalog and pricing | Many-to-many with tags |
| `tag` | Product classification tags | Many-to-many with products |
| `product_tag` | Product–Tag join table | Foreign keys to product and tag |
| `orders` | Customer orders | Many-to-one with customer |
| `order_line` | Individual order items | Many-to-one with order and product |

### Important Schema Details

- Money amounts use `NUMERIC(12,2)`.
- Currency columns use `CHAR(3)`.
- Customer shipping columns use the `ship_*` prefix.
- Product SKU is unique and not null.
- Product stock is not null and must be non-negative.
- Order timestamps use `TIMESTAMP WITH TIME ZONE`.

---

## 3. Domain Model Design

### A. Identifier Generation

Entities use `GenerationType.SEQUENCE` with `allocationSize = 50`.

This allows Hibernate to allocate identifiers in advance and supports efficient persistence operations.

### B. Value Objects

`Money` and `Address` are implemented as embeddable value objects.

- `Money` stores amount and currency.
- `Address` stores street, city, state, zip code, and country.

Customer uses separate embedded shipping and billing addresses.

### C. Enum Representation

`Category` and `OrderStatus` use `@Enumerated(EnumType.STRING)`.

This stores readable enum names instead of ordinal numbers.

### D. Lazy Relationships

The `@ManyToOne` associations use `FetchType.LAZY` to avoid unnecessary eager loading.

---

## 4. Task 2 — Associations and Cascading

### A. Order Aggregate Root

`Order` is the aggregate root for its `OrderLine` collection.

The relationship uses:

```java
@OneToMany(
    mappedBy = "order",
    cascade = CascadeType.ALL,
    orphanRemoval = true
)
```

The collection is initialized with `ArrayList` and exposed through a read-only getter.

The `Order` entity provides:

- `addLine(Product, int)`: Creates an order line, sets the owning side, and copies the product price into the order line.
- `removeLine(OrderLine)`: Removes a line and clears its order reference.
- `total()`: Calculates the total using each order line's unit price and quantity.

The order line stores a snapshot of the product price at the time it is added to the order.

### B. Product–Tag Many-to-Many

`Product` and `Tag` have a bidirectional many-to-many relationship.

The join table is:

```text
product_tag
```

`Product` owns the relationship, while `Tag` uses `mappedBy = "tags"`.

The `Product.tag(Tag)` helper updates both sides of the association.

### C. Cascade and Orphan Removal

The order cascade test verifies:

1. Persisting an order also persists its order lines.
2. Removing an order line deletes the corresponding database row.
3. Removing an order deletes its remaining order lines.
4. Removing an order does not delete its associated customer or products.

---

## 5. Entity Identity

Entities implement hand-written `equals()` and `hashCode()` methods.

The identity strategy follows these rules:

- Two entities are equal only when they have the same entity class and the same non-null database ID.
- Two transient entities with null IDs are not equal unless they are the same object.
- `hashCode()` uses the entity class hash code, keeping the hash code stable before and after persistence.

Entity `toString()` methods include scalar fields only and exclude relationships to prevent recursive calls and unintended lazy loading.

---

## 6. Persistence and SQL Evidence

The order cascade test was executed successfully.

The captured Hibernate SQL log demonstrated:

| Operation | Observed SQL |
|---|---|
| Persist order | 1 INSERT into `orders` |
| Persist order lines | 2 INSERTs into `order_line` |
| Unnecessary updates | None observed |
| Remove one order line | 1 DELETE from `order_line` |
| Remove the order | 1 DELETE for the remaining `order_line` |

The SQL evidence was captured in `order-cascade-test.log`.

The test completed with:

```text
Tests run: 1
Failures: 0
Errors: 0
BUILD SUCCESS
```

---

## 7. toString() Recursion Experiment

A temporary experiment was performed to demonstrate the danger of including bidirectional relationships in entity `toString()` methods.

The experiment temporarily included:

- `lines` in `Order.toString()`.
- `order` in `OrderLine.toString()`.

This created recursive calls:

```text
Order.toString()
    -> OrderLine.toString()
        -> Order.toString()
            -> OrderLine.toString()
                -> ...
```

The experiment successfully reproduced:

```text
StackOverflowError
```

The error was caught by the JUnit test, which completed successfully.

After the experiment, both original `toString()` implementations were restored. They now include scalar fields only.

---

## 8. Flyway and Hibernate Validation

Flyway manages database schema changes through versioned migrations.

Hibernate uses:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

This ensures that Hibernate validates the existing schema rather than automatically modifying it.

Both migrations were successfully tested against a fresh test database, and Hibernate schema validation passed.

---

## 9. Task 3 — Repositories, Queries, Projections and Paginated Endpoints

### A. Repositories and Derived Queries

Data access was replaced with Spring Data JPA repositories:
- `CustomerRepository extends JpaRepository<Customer, Long>`
- `ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product>`
  - Derived queries:
    - `existsBySku(String sku)`
    - `findTop3ByCategoryOrderByPriceAmountDesc(Category category)` (navigates the embedded `price.amount`)
    - `findByTagsName(String name)` (navigates through `@ManyToMany` association `tags.name`)
  - Native query:
    - `findLowStock(int min)` (`SELECT p.* FROM product p WHERE p.stock < :min ORDER BY p.stock`, `nativeQuery = true`)
  - Record projection:
    - `Page<ProductView> findByCategory(Category category, Pageable pageable)`
- `OrderRepository extends JpaRepository<Order, Long>`
  - Derived queries:
    - `findByCustomerEmailAndStatusOrderByPlacedAtDesc(String email, OrderStatus status)`
    - `countByStatus(OrderStatus status)`
    - `findFirstByCustomerIdOrderByPlacedAtDesc(Long customerId)`
  - Interface projection:
    - `Page<OrderSummary> findByStatus(OrderStatus status, Pageable pageable)`
  - JPQL queries:
    - `findWithLinesByCustomerEmail(String email)` using fetch joins:
      ```java
      @Query("""
          select distinct o from Order o
            join fetch o.lines l
            join fetch l.product
           where o.customer.email = :email
           order by o.placedAt desc
          """)
      List<Order> findWithLinesByCustomerEmail(@Param("email") String email);
      ```
    - `countLinesByStatus(OrderStatus status)` with aggregate:
      ```java
      @Query("select count(l) from OrderLine l where l.order.status = :status")
      long countLinesByStatus(@Param("status") OrderStatus status);
      ```

### B. Boot-Time Method Typo Experiment

Spring Data JPA parses repository query method names at application startup during Spring context bootstrapping.

When an intentional typo was introduced into `OrderRepository`:
```java
List<Order> findByCustomerEmaliAndStatusOrderByPlacedAtDesc(String email, OrderStatus status);
```

The application failed to start with the following boot error:

```text
org.springframework.beans.factory.UnsatisfiedDependencyException: Error creating bean with name 'orderService' defined in file [C:\Desktop\Student Online (SO)\Techno\I4-GIC-A\I4-GIC-S1\Software engineering\C3-Hibernate Framework and Spring Data JPA\FlopSister\target\classes\edu\itc\se\shop\service\OrderService.class]: Unsatisfied dependency expressed through constructor parameter 0: Error creating bean with name 'orderRepository' defined in edu.itc.se.shop.repository.OrderRepository defined in @EnableJpaRepositories declared on ShopApplication: No property 'emali' found for type 'Customer'; Did you mean 'email'; Traversed path: Order.customer
	at org.springframework.beans.factory.support.ConstructorResolver.createArgumentArray(ConstructorResolver.java:804)
	at org.springframework.beans.factory.support.ConstructorResolver.autowireConstructor(ConstructorResolver.java:240)
	...
Caused by: org.springframework.data.mapping.PropertyReferenceException: No property 'emali' found for type 'Customer'; Did you mean 'email'; Traversed path: Order.customer
	at org.springframework.data.mapping.PropertyPath.<init>(PropertyPath.java:90)
	at org.springframework.data.mapping.PropertyPath.create(PropertyPath.java:437)
	at org.springframework.data.mapping.PropertyPath.create(PropertyPath.java:413)
```

This confirms fail-fast validation: Spring Data JPA validates all query method signatures against the underlying entity metamodel at startup before any HTTP requests are accepted. The typo was corrected back to `findByCustomerEmailAndStatusOrderByPlacedAtDesc`.

### C. Projections (Interface and Record)

1. **Interface Projection (`OrderSummary`)**:
   ```java
   public interface OrderSummary {
       Long getId();
       OrderStatus getStatus();
       Instant getPlacedAt();
       CustomerName getCustomer();
       interface CustomerName {
           String getName();
       }
   }
   ```
   When queried via `OrderRepository.findByStatus(status, pageable)`, Hibernate generates a selective projection SQL query selecting only the required fields:
   ```sql
   DEBUG org.hibernate.SQL : select o1_0.id,o1_0.status,o1_0.placed_at,c1_0.id,c1_0.street,c1_0.city,c1_0.zip,c1_0.email,c1_0.name,c1_0.ship_street,c1_0.ship_city,c1_0.ship_zip from orders o1_0 join customer c1_0 on c1_0.id=o1_0.customer_id where o1_0.status=? order by o1_0.placed_at desc fetch first ? rows only
   DEBUG org.hibernate.SQL : select count(o1_0.id) from orders o1_0 where o1_0.status=?
   ```

2. **Record Projection (`ProductView`)**:
   ```java
   public record ProductView(Long id, String sku, String name, BigDecimal amount, int stock) {}
   ```
   Returned by `ProductRepository.findByCategory(Category, Pageable)` and mapped in `ProductService.searchProducts`.

### D. Specifications and Dynamic Filtering

`ProductSpecs` provides composable JPA specifications:
- `nameContains(String q)`
- `inCategory(Category category)`
- `cheaperThan(BigDecimal max)`
- `inStock()`
- `filter(String q, Category category, BigDecimal maxPrice, Boolean inStock)`: Combines non-null filters via `Specification.allOf`.

### E. Data Seeder

`DataSeeder` (`@Component @Profile("dev") ApplicationRunner`) inserts:
- 3 Customers (`Ada Lovelace`, `Grace Hopper`, `Margaret Hamilton`)
- 5 Tags
- 12 Products across various categories (including `KB-1`, `KB-7`, `KB-EXP`, etc.)
- 30 Orders with 2–4 lines each and mixed statuses (11 PAID, 8 SHIPPED, 7 NEW, 4 CANCELLED)

Executed within a single `@Transactional` method when running with `--spring.profiles.active=dev`.

### F. REST Endpoints

1. `GET /api/orders?status=PAID&page=0&size=5&sort=placedAt,desc`
   - Returns paginated `Page<OrderSummary>` using `@PageableDefault(size = 20, sort = "placedAt", direction = Sort.Direction.DESC)`.
2. `GET /api/products?q=&category=&maxPrice=&inStock=&page=&size=`
   - Returns paginated `Page<ProductView>` dynamically filtered with `ProductSpecs.filter`.
3. `GET /api/customers/{email}/orders`
   - Returns full order DTOs (`OrderResponse` with order lines) using `findWithLinesByCustomerEmail` inside a `@Transactional(readOnly = true)` service method.
   - Prevents exposing JPA entities directly through web controllers.

Full transcript details are saved in `api-session.md`.

### G. Test Suite

`OrderRepositoryTest` (`@DataJpaTest`) covers:
1. Derived query `findByCustomerEmailAndStatusOrderByPlacedAtDesc`
2. Fetch join `findWithLinesByCustomerEmail`, asserting:
   - `Hibernate.isInitialized(order.getLines())` is `true` after the fetch join query.
   - `Hibernate.isInitialized(order.getLines())` is `false` after a plain `findById` + `clear`.
3. Native query `findLowStock(int min)`
4. Interface projection `findByStatus(status, pageable)` returning `OrderSummary`
5. Aggregate query `countLinesByStatus(status)`

```text
[INFO] Running edu.itc.se.shop.repository.OrderRepositoryTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```
