package edu.itc.se.shop.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import edu.itc.se.shop.support.TestData;

@SpringBootTest
@ActiveProfiles("test")
@Execution(ExecutionMode.SAME_THREAD)
class PerformanceBenchmarkTest {

    private static final int WARMUP_RUNS = 5;
    private static final int MEASURED_RUNS = 20;

    @Autowired
    private ReportService reports;

    @Autowired
    private TestData data;

    @Test
    void benchmarkFetchStrategies() {

        // Prepare 50 PAID orders with 3 lines each
        Long customerId = data.createPaidOrders(50, 3);

        // Benchmark each strategy
        double naiveMedian = measureMedianMillis(
                () -> reports.linesPerPaidOrder(customerId));

        double fetchJoinMedian = measureMedianMillis(
                () -> reports.linesPerPaidOrderWithFetchJoin(customerId));

        double entityGraphMedian = measureMedianMillis(
                () -> reports.linesPerPaidOrderWithEntityGraph(customerId));

        double batchSizeMedian = measureMedianMillis(
                () -> reports.linesPerPaidOrderWithBatchSize(customerId));

        // Display benchmark results
        System.out.println();
        System.out.println("========== PERFORMANCE BENCHMARK ==========");
        System.out.println("Warm-up runs:  " + WARMUP_RUNS);
        System.out.println("Measured runs: " + MEASURED_RUNS);
        System.out.println("--------------------------------------------");

        System.out.printf(Locale.US,
                "Naive median:       %.3f ms%n", naiveMedian);

        System.out.printf(Locale.US,
                "JOIN FETCH median:  %.3f ms%n", fetchJoinMedian);

        System.out.printf(Locale.US,
                "EntityGraph median: %.3f ms%n", entityGraphMedian);

        System.out.printf(Locale.US,
                "BatchSize median:   %.3f ms%n", batchSizeMedian);

        System.out.println("============================================");

        // Basic validation
        assertThat(naiveMedian).isPositive();
        assertThat(fetchJoinMedian).isPositive();
        assertThat(entityGraphMedian).isPositive();
        assertThat(batchSizeMedian).isPositive();
    }

    private double measureMedianMillis(Supplier<?> operation) {

        // Warm up the JVM before measuring
        for (int i = 0; i < WARMUP_RUNS; i++) {
            operation.get();
        }

        List<Long> measurements = new ArrayList<>();

        // Measure execution time
        for (int i = 0; i < MEASURED_RUNS; i++) {

            long start = System.nanoTime();

            Object result = operation.get();

            long end = System.nanoTime();

            assertThat(result).isNotNull();

            measurements.add(end - start);
        }

        // Sort measurements to calculate the median
        Collections.sort(measurements);

        int middle = measurements.size() / 2;

        double medianNanos = (
                measurements.get(middle - 1)
                + measurements.get(middle)
        ) / 2.0;

        return medianNanos / 1_000_000.0;
    }
}