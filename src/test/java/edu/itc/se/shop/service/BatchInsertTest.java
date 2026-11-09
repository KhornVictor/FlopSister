package edu.itc.se.shop.service;

import static org.assertj.core.api.Assertions.assertThat;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import edu.itc.se.shop.domain.Product;
import edu.itc.se.shop.repository.ProductRepository;
import jakarta.persistence.EntityManagerFactory;

@SpringBootTest
@ActiveProfiles("test")
@Execution(ExecutionMode.SAME_THREAD)
class BatchInsertTest {

    @Autowired
    private ProductRepository products;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    @Transactional
    void insertsOneThousandProductsUsingJdbcBatching() {

        SessionFactory sessionFactory =
                entityManagerFactory.unwrap(SessionFactory.class);

        Statistics stats = sessionFactory.getStatistics();

        stats.clear();

        // Insert 1,000 products in one transaction
        for (int i = 1; i <= 1000; i++) {
            products.save(new Product(
                    "Batch Product " + i, 100));
        }

        // Flush pending inserts to the database
        products.flush();

        long insertedEntities = stats.getEntityInsertCount();
        long preparedStatements = stats.getPrepareStatementCount();

        System.out.println("======= JDBC BATCH INSERT RESULTS =======");
        System.out.println("Inserted entities: " + insertedEntities);
        System.out.println("Prepared statements: " + preparedStatements);
        System.out.println("=========================================");

        // Verify all 1,000 products were inserted
        assertThat(insertedEntities).isEqualTo(1000);

        // Verify prepared statement count is below 1,000
        assertThat(preparedStatements).isLessThan(1000);
    }
}
