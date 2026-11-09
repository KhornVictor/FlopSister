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