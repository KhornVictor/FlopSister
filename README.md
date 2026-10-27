# Shop Application - Lab 2 & Lab 3

## Task 2: Inversion of Control, Dependency Injection, and Bean Lifecycle

### Rule Deciding Between Two `@Primary` Beans

When Spring injects a dependency by type (e.g., `Notifier`), it searches the application context for candidate beans matching that type.
* If multiple candidates exist and exactly **one** is annotated with `@Primary`, Spring selects that primary bean.
* If **more than one** bean candidate of the same type is marked with `@Primary` and both are currently active (e.g., if both `EmailNotifier` and `ConsoleNotifier` were active simultaneously in the `dev` profile), Spring does **not** pick arbitrarily. Instead, Spring fails fast during context startup and throws:
  ```
  org.springframework.beans.factory.NoUniqueBeanDefinitionException:
  No qualifying bean of type 'edu.itc.se.shop.service.Notifier' available:
  more than one 'primary' bean found among candidates: [consoleNotifier, emailNotifier]
  ```

#### How to Resolve Ambiguity Between Primary Beans
1. **Profiles (`@Profile`)**: Ensure that conflicting `@Primary` beans are active in mutually exclusive profiles (e.g., `@Profile("default")` or `@Profile("!dev")` on `EmailNotifier` and `@Profile("dev")` on `ConsoleNotifier`).
2. **Qualifiers (`@Qualifier`)**: Use `@Qualifier("specificBeanName")` at injection points to bypass primary selection and target a specific bean.
3. **`@Priority`**: Apply `jakarta.annotation.Priority` (where a lower value indicates higher priority) if supported.
4. **`@Fallback` (Spring 6.2+)**: Mark default beans with `@Fallback` so that any regular bean takes precedence.

---

## Task 3: JDBC, `JdbcClient`, and Transactions

### 1. Raw JDBC vs `JdbcClient` Comparison

| Implementation | Lines of Code in `findAll()` | Boilerplate / Management |
| :--- | :---: | :--- |
| **`RawJdbcProductRepository`** | **19 lines** | Manual `Connection`, `PreparedStatement`, `ResultSet`, explicit column index/name extraction, constructor mapping, checked `SQLException` catching & rethrowing, try-with-resources cleanup. |
| **`ProductRepository` (`JdbcClient`)** | **5 lines** | Declarative fluent API: automatic connection handling, automatic record/class reflection mapping (`.query(Product.class)`), automatic cleanup, and exception translation. |

### 2. Duplicate Key Exception & Spring Exception Translation

#### Observed Exception Class
```
org.springframework.dao.DuplicateKeyException
```
(Subclass of `org.springframework.dao.DataIntegrityViolationException`, which extends `org.springframework.dao.DataAccessException`).

#### Where the Translation Happened
1. **Database / JDBC Driver Layer**: When attempting to insert a product with an existing `name`, the H2 database triggers a unique constraint violation and throws a vendor-specific checked exception:
   `org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException` (Vendor error code: `23505`).
2. **Spring JDBC Layer**: `JdbcClient` delegates execution to `JdbcTemplate.execute(...)`. Inside `JdbcTemplate`, the `catch (SQLException ex)` block intercepts the checked `SQLException`.
3. **`SQLExceptionTranslator`**: `JdbcTemplate` invokes its configured `SQLExceptionTranslator`, specifically `SQLErrorCodeSQLExceptionTranslator`.
4. **Error Code Mapping**: The translator references error codes defined for H2 (from Spring's `sql-error-codes.xml`). It recognizes error code `23505` as a unique index or primary key duplicate error and wraps the root cause in Spring's uniform, unchecked `org.springframework.dao.DuplicateKeyException`.
