# Shop JPA — Domain Model & Flyway Schema (Task 1)

## 1. Overview
This module implements the core domain model, baseline Flyway migration, and JPA entity mappings for the `shop-jpa` application.

## 2. Database Schema (Flyway `V1__init.sql`)

### 5 Sequences (`allocationSize = 50`)
- `customer_seq`
- `order_seq`
- `order_line_seq`
- `product_seq`
- `tag_seq`

### 6 Tables
| Table | Description | Primary Key | Key Relationships |
| :--- | :--- | :--- | :--- |
| `customers` | Customers with embedded shipping/billing addresses | `id` (via `customer_seq`) | 1-to-many with `orders` |
| `tags` | Classification tags for products | `id` (via `tag_seq`) | Many-to-many with `products` |
| `products` | Catalog items with embedded Money and version | `id` (via `product_seq`) | Many-to-many via `product_tags` |
| `product_tags` | Join table for Product-Tag association | `(product_id, tag_id)` | FKs to `products` and `tags` (ON DELETE CASCADE) |
| `orders` | Customer purchases | `id` (via `order_seq`) | FK to `customers` (`customer_id`) |
| `order_lines` | Line items belonging to orders | `id` (via `order_line_seq`) | FK to `orders` (`order_id`), FK to `products` (`product_id`) |

---

## 3. Deliberate Schema Mismatches & Design Rationales

### A. Identifier Generation: `SEQUENCE` vs `IDENTITY`
- **Choice**: `GenerationType.SEQUENCE` with `allocationSize = 50`.
- **Rationale**: `IDENTITY` forces an immediate JDBC roundtrip `INSERT` to retrieve the database-generated ID, which breaks JDBC statement batching (`hibernate.jdbc.batch_size`). Using sequences pre-allocates blocks of 50 IDs in memory, enabling Hibernate to batch inserts efficiently.

### B. Value Objects as Records (`@Embeddable`)
- **`Money`**: Embeds `amount` (`NUMERIC(12, 2)`) and `currency` (`VARCHAR(3)`). Prevents primitive obsession and ensures currency safety.
- **`Address`**: Embeds street, city, state, zip code, and country. `Customer` embeds two distinct addresses (`shippingAddress` and `billingAddress`) using `@AttributeOverrides` to map to distinct columns (`shipping_*` and `billing_*`) in the single `customers` table.

### C. Enum Representation: `STRING` vs `ORDINAL`
- **Choice**: `@Enumerated(EnumType.STRING)` for `Category` and `OrderStatus`.
- **Rationale**: Storing enums as integers (`ORDINAL`) causes silent data corruption if enum constants are ever reordered, inserted, or removed. `STRING` preserves semantic readability and database migration safety.

### D. Explicit `FetchType.LAZY` for `@ManyToOne`
- **Choice**: All `@ManyToOne` associations (`Order.customer`, `OrderLine.order`, `OrderLine.product`) are explicitly marked `FetchType.LAZY`.
- **Rationale**: In JPA, `@ManyToOne` defaults to `FetchType.EAGER`, which leads to unwanted joins and triggers the N+1 query problem when loading collections. Overriding to `LAZY` ensures data is loaded only on demand or via explicit fetch joins.

### E. Optimistic Locking (`@Version`)
- **Choice**: `@Version int version` on `Product` and `Order`.
- **Rationale**: Enables optimistic concurrency control without database-level row locks. Hibernate automatically appends `WHERE version = ?` to `UPDATE` statements and throws `OptimisticLockException` if another transaction modified the row concurrently.

### F. Flyway Managed Schema Validation
- **Choice**: `spring.jpa.hibernate.ddl-auto: validate`.
- **Rationale**: Schema generation (`create-drop` or `update`) is strictly prohibited in production. Flyway maintains immutable versioned migrations (`V1__init.sql`), while Hibernate merely validates that Java entities conform to the existing database schema.
