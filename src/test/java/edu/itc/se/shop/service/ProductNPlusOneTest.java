
package edu.itc.se.shop.service;

import java.util.List;

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
class ProductNPlusOneTest {

    @Autowired
    private ReportService reports;

    @Autowired
    private QueryCounter counter;

    @Autowired
    private TestData data;

    @Test
    void comparesProductNameNPlusOneSolutions() {

        // Create 50 PAID orders with 150 unique products
        Long customerId =
                data.createPaidOrdersWithUniqueProducts(50, 3);

        // 1. Naive approach: load products individually
        counter.reset();

        List<String> naive =
                reports.productNamesPerPaidOrder(customerId);

        long naiveQueries = counter.count();

        // 2. Batch fetching approach: batch size 32
        counter.reset();

        List<String> batch =
                reports.productNamesPerPaidOrderWithBatchSize(customerId);

        long batchQueries = counter.count();

        // Verify both approaches return 150 product names
        assertThat(naive).hasSize(150);
        assertThat(batch).hasSize(150);

        assertThat(naive).allMatch(
                name -> name != null && !name.isBlank());

        assertThat(batch).allMatch(
                name -> name != null && !name.isBlank());

        // Verify SQL statement counts
        assertThat(naiveQueries).isEqualTo(151);
        assertThat(batchQueries).isEqualTo(6);

        // Display actual SQL statement counts
        System.out.println("======= PRODUCT N+1 QUERY RESULTS =======");
        System.out.println("Naive:       " + naiveQueries);
        System.out.println("BatchSize32: " + batchQueries);
        System.out.println("Naive expected:       151");
        System.out.println("BatchSize32 expected: 6");
        System.out.println("=========================================");
    }
}
