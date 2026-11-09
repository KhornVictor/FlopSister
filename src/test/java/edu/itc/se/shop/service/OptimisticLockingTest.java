package edu.itc.se.shop.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import edu.itc.se.shop.domain.Order;
import edu.itc.se.shop.domain.OrderStatus;
import edu.itc.se.shop.repository.OrderRepository;
import edu.itc.se.shop.support.TestData;

@SpringBootTest
@ActiveProfiles("test")
class OptimisticLockingTest {
    @Autowired
    private OrderRepository orders;

    @Autowired
    private TransactionTemplate tx;

    @Autowired
    private TestData data;

    @Autowired
    private OrderFacade orderFacade;

    @Test
    void secondWriterFailsWithOptimisticLock() throws Exception {
        Long orderId = data.newOrder();
        long versionBefore = orders.findById(orderId).orElseThrow().getVersion();
        CountDownLatch bothLoaded = new CountDownLatch(2);

        Function<OrderStatus, Callable<WriterResult>> writer = target -> () -> {
            try {
                tx.executeWithoutResult(status -> {
                    Order order = orders.findById(orderId).orElseThrow();
                    bothLoaded.countDown();
                    await(bothLoaded);
                    order.setStatus(target);
                });
                return new WriterResult(target, null);
            } catch (RuntimeException e) {
                return new WriterResult(target, e);
            }
        };

        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            List<Future<WriterResult>> futures = pool.invokeAll(
                    List.of(writer.apply(OrderStatus.PAID), writer.apply(OrderStatus.CANCELLED)));
            List<WriterResult> results = futures.stream().map(this::get).toList();
            List<WriterResult> failures = results.stream()
                    .filter(result -> result.failure() != null)
                    .toList();
            WriterResult winner = results.stream()
                    .filter(result -> result.failure() == null)
                    .findFirst()
                    .orElseThrow();

            assertThat(failures).hasSize(1);
            assertThat(failures.get(0).failure()).isInstanceOf(ObjectOptimisticLockingFailureException.class);
            Order after = orders.findById(orderId).orElseThrow();
            assertThat(after.getStatus()).isEqualTo(winner.status());
            assertThat(after.getVersion()).isEqualTo(versionBefore + 1);
        }
    }

    @Test
    void facadeRetriesConflictsUntilBothWritersCommit() throws Exception {
        Long orderId = data.newOrder();
        long versionBefore = orders.findById(orderId).orElseThrow().getVersion();
        CountDownLatch start = new CountDownLatch(1);
        List<OrderStatus> requested = List.of(OrderStatus.PAID, OrderStatus.CANCELLED);

        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            var futures = requested.stream()
                    .map(status -> pool.submit(() -> {
                        await(start);
                        orderFacade.changeStatusWithRetry(orderId, status);
                    }))
                    .toList();
            start.countDown();
            for (Future<?> future : futures) {
                future.get();
            }
        }

        Order after = orders.findById(orderId).orElseThrow();
        assertThat(requested).contains(after.getStatus());
        assertThat(after.getVersion()).isEqualTo(versionBefore + 2);
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("latch timeout");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private WriterResult get(Future<WriterResult> future) {
        try {
            return future.get();
        } catch (Exception e) {
            return new WriterResult(null, e);
        }
    }

    private record WriterResult(OrderStatus status, Throwable failure) {
    }
}
