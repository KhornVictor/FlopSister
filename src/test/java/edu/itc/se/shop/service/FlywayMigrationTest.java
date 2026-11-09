
package edu.itc.se.shop.service;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("flyway-test")
class FlywayMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Flyway flyway;

    @Test
    void shouldApplyBothMigrationsAndCreateIndexes() {

        // 1. Verify successful migrations
        Integer migrationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM \"flyway_schema_history\" " +
                "WHERE \"success\" = TRUE AND \"version\" IS NOT NULL",
                Integer.class
        );

        assertThat(migrationCount).isEqualTo(2);

        // 2. Verify migration versions
        List<String> versions = jdbcTemplate.queryForList(
                "SELECT \"version\" FROM \"flyway_schema_history\" " +
                "WHERE \"success\" = TRUE AND \"version\" IS NOT NULL " +
                "ORDER BY \"installed_rank\"",
                String.class
        );

        assertThat(versions).containsExactly("1", "2");

        // 3. Verify indexes
        List<String> indexes = jdbcTemplate.queryForList(
                "SELECT INDEX_NAME FROM INFORMATION_SCHEMA.INDEXES " +
                "WHERE INDEX_SCHEMA = 'PUBLIC'",
                String.class
        );

        assertThat(indexes).contains(
                "IDX_ORDERS_CUSTOMER_PLACED",
                "IDX_ORDER_LINE_ORDER",
                "IDX_PRODUCT_NAME_STOCK"
        );

        // 4. Verify no pending migrations
        assertThat(flyway.info().pending()).isEmpty();

        // 5. Compare query plans without and with V2 indexes
        compareQueryPlans();

        System.out.println("\n======= FLYWAY MIGRATION RESULTS =======");
        System.out.println("Applied migrations: " + migrationCount);
        System.out.println("Versions: " + versions);
        System.out.println("Indexes: " + indexes);
        System.out.println("Pending migrations: " +
                flyway.info().pending().length);
        System.out.println("========================================");
    }

    private void compareQueryPlans() {

        String ordersQuery =
                "SELECT * FROM orders " +
                "WHERE status = 'PAID' AND customer_id = 1";

        String linesQuery =
                "SELECT * FROM order_lines WHERE order_id = 1";

        // Temporarily remove the V2 indexes
        jdbcTemplate.execute(
                "DROP INDEX IF EXISTS idx_orders_customer_placed"
        );
        jdbcTemplate.execute(
                "DROP INDEX IF EXISTS idx_order_line_order"
        );

        System.out.println("\n======= EXPLAIN: BEFORE V2 INDEXES =======");
        printPlan("Orders query", ordersQuery);
        printPlan("Order lines query", linesQuery);

        // Recreate the indexes defined in V2
        jdbcTemplate.execute(
                "CREATE INDEX idx_orders_customer_placed " +
                "ON orders (customer_id, placed_at DESC)"
        );
        jdbcTemplate.execute(
                "CREATE INDEX idx_order_line_order " +
                "ON order_lines (order_id)"
        );

        System.out.println("\n======= EXPLAIN: AFTER V2 INDEXES =======");
        printPlan("Orders query", ordersQuery);
        printPlan("Order lines query", linesQuery);

        System.out.println("=========================================");
    }

    private void printPlan(String label, String sql) {

        String plan = jdbcTemplate.queryForObject(
                "EXPLAIN " + sql,
                String.class
        );

        System.out.println("\n" + label + ":");
        System.out.println(plan);
    }
}
