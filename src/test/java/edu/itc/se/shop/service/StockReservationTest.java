package edu.itc.se.shop.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import edu.itc.se.shop.domain.Category;
import edu.itc.se.shop.domain.Money;
import edu.itc.se.shop.domain.Product;
import edu.itc.se.shop.repository.ProductRepository;

@SpringBootTest
@ActiveProfiles("test")
class StockReservationTest {

    private static final Logger log = LoggerFactory.getLogger(StockReservationTest.class);

    @Autowired
    private StockService stockService;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void compareReservationStrategies() throws Exception {
        testPessimistic();
        testOptimistic();
        testAtomic();
    }

    private void testPessimistic() throws Exception {
        Product p = productRepository.save(new Product("SKU-PESS", "Pessimistic Item", Money.eur("10.00"), Category.BOOKS, 5));
        Long productId = p.getId();

        int numThreads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(numThreads);
        CountDownLatch ready = new CountDownLatch(numThreads);
        CountDownLatch start = new CountDownLatch(1);

        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger outOfStockCount = new AtomicInteger(0);

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < numThreads; i++) {
            tasks.add(() -> {
                ready.countDown();
                start.await(5, TimeUnit.SECONDS);
                try {
                    stockService.reserve(productId, 1);
                    successes.incrementAndGet();
                } catch (OutOfStockException e) {
                    outOfStockCount.incrementAndGet();
                }
                return null;
            });
        }

        long startTime = System.currentTimeMillis();
        List<Future<Void>> futures = new ArrayList<>();
        for (Callable<Void> task : tasks) {
            futures.add(pool.submit(task));
        }

        ready.await(5, TimeUnit.SECONDS);
        start.countDown();

        for (Future<Void> future : futures) {
            future.get();
        }
        long duration = System.currentTimeMillis() - startTime;
        pool.shutdown();

        Product updated = productRepository.findById(productId).orElseThrow();
        assertThat(successes.get()).isEqualTo(5);
        assertThat(outOfStockCount.get()).isEqualTo(5);
        assertThat(updated.getStock()).isEqualTo(0);

        log.info("pessimistic: {} ok / {} out-of-stock, stock={}, {} threads in {} ms",
                successes.get(), outOfStockCount.get(), updated.getStock(), numThreads, duration);
    }

    private void testOptimistic() throws Exception {
        Product p = productRepository.save(new Product("SKU-OPT", "Optimistic Item", Money.eur("10.00"), Category.BOOKS, 5));
        Long productId = p.getId();

        int numThreads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(numThreads);
        CountDownLatch ready = new CountDownLatch(numThreads);
        CountDownLatch start = new CountDownLatch(1);

        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger outOfStockCount = new AtomicInteger(0);
        AtomicInteger totalRetries = new AtomicInteger(0);

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < numThreads; i++) {
            tasks.add(() -> {
                ready.countDown();
                start.await(5, TimeUnit.SECONDS);
                while (true) {
                    try {
                        stockService.reserveOptimistic(productId, 1);
                        successes.incrementAndGet();
                        break;
                    } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
                        totalRetries.incrementAndGet();
                        Thread.sleep(10);
                    } catch (OutOfStockException e) {
                        outOfStockCount.incrementAndGet();
                        break;
                    }
                }
                return null;
            });
        }

        long startTime = System.currentTimeMillis();
        List<Future<Void>> futures = new ArrayList<>();
        for (Callable<Void> task : tasks) {
            futures.add(pool.submit(task));
        }

        ready.await(5, TimeUnit.SECONDS);
        start.countDown();

        for (Future<Void> future : futures) {
            future.get();
        }
        long duration = System.currentTimeMillis() - startTime;
        pool.shutdown();

        Product updated = productRepository.findById(productId).orElseThrow();
        assertThat(successes.get()).isEqualTo(5);
        assertThat(outOfStockCount.get()).isEqualTo(5);
        assertThat(updated.getStock()).isEqualTo(0);

        log.info("optimistic : {} ok / {} out-of-stock after {} retries, {} ms",
                successes.get(), outOfStockCount.get(), totalRetries.get(), duration);
    }

    private void testAtomic() throws Exception {
        Product p = productRepository.save(new Product("SKU-ATOM", "Atomic Item", Money.eur("10.00"), Category.BOOKS, 5));
        Long productId = p.getId();

        int numThreads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(numThreads);
        CountDownLatch ready = new CountDownLatch(numThreads);
        CountDownLatch start = new CountDownLatch(1);

        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger outOfStockCount = new AtomicInteger(0);

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < numThreads; i++) {
            tasks.add(() -> {
                ready.countDown();
                start.await(5, TimeUnit.SECONDS);
                try {
                    stockService.reserveAtomic(productId, 1);
                    successes.incrementAndGet();
                } catch (OutOfStockException e) {
                    outOfStockCount.incrementAndGet();
                }
                return null;
            });
        }

        long startTime = System.currentTimeMillis();
        List<Future<Void>> futures = new ArrayList<>();
        for (Callable<Void> task : tasks) {
            futures.add(pool.submit(task));
        }

        ready.await(5, TimeUnit.SECONDS);
        start.countDown();

        for (Future<Void> future : futures) {
            future.get();
        }
        long duration = System.currentTimeMillis() - startTime;
        pool.shutdown();

        Product updated = productRepository.findById(productId).orElseThrow();
        assertThat(successes.get()).isEqualTo(5);
        assertThat(outOfStockCount.get()).isEqualTo(5);
        assertThat(updated.getStock()).isEqualTo(0);

        log.info("atomic upd : {} ok / {} out-of-stock, {} ms",
                successes.get(), outOfStockCount.get(), duration);
    }
}
