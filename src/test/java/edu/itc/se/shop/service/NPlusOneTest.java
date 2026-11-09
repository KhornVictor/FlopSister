package edu.itc.se.shop.service;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import edu.itc.se.shop.support.QueryCounter;
import edu.itc.se.shop.support.TestData;

@SpringBootTest
@ActiveProfiles("test")
@Execution(ExecutionMode.SAME_THREAD)
class NPlusOneTest {

    @Autowired
    private ReportService reports;

    @Autowired
    private QueryCounter counter;

    @Autowired
    private TestData data;

    @Test
    void comparesNPlusOneSolutions() {

        // Create 50 PAID orders, each with 3 order lines
        Long customerId = data.createPaidOrders(50, 3);

        // 1. Naive approach
        counter.reset();
        var naive = reports.linesPerPaidOrder(customerId);
        long naiveQueries = counter.count();

        // 2. JOIN FETCH approach
        counter.reset();
        var fetchJoin = reports.linesPerPaidOrderWithFetchJoin(customerId);
        long fetchJoinQueries = counter.count();

        // 3. EntityGraph approach
        counter.reset();
        var entityGraph =
                reports.linesPerPaidOrderWithEntityGraph(customerId);
        long entityGraphQueries = counter.count();

        // 4. BatchSize approach
        counter.reset();
        var batchSize =
                reports.linesPerPaidOrderWithBatchSize(customerId);
        long batchSizeQueries = counter.count();

        // Verify all approaches return 50 orders with 3 lines each
        assertThat(naive).hasSize(50);
        assertThat(naive.values()).containsOnly(3);

        assertThat(fetchJoin).hasSize(50);
        assertThat(fetchJoin.values()).containsOnly(3);

        assertThat(entityGraph).hasSize(50);
        assertThat(entityGraph.values()).containsOnly(3);

        assertThat(batchSize).hasSize(50);
        assertThat(batchSize.values()).containsOnly(3);

        // Verify SQL statement counts
        assertThat(naiveQueries).isEqualTo(51);
        assertThat(fetchJoinQueries).isEqualTo(1);
        assertThat(entityGraphQueries).isEqualTo(1);
        assertThat(batchSizeQueries).isEqualTo(5);

        // Display SQL statement counts
        System.out.println("========== N+1 QUERY RESULTS ==========");
        System.out.println("Naive:       " + naiveQueries);
        System.out.println("JOIN FETCH:  " + fetchJoinQueries);
        System.out.println("EntityGraph: " + entityGraphQueries);
        System.out.println("BatchSize:   " + batchSizeQueries);
        System.out.println("=======================================");
    }
}