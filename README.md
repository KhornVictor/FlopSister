# Task 4 — Entity states, dirty checking, and optimistic locking

This task is implemented in the existing `edu.itc.se.shop` application. The original workspace did not contain JPA entities or repositories, so the small `Customer`, `Product`, `Order`, and `Tag` entities and their repositories were added to support the requested tests. Test schema is created and dropped by the `test` profile.

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