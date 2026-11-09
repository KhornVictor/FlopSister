package edu.itc.se.shop.repository;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import edu.itc.se.shop.domain.Category;
import edu.itc.se.shop.domain.Money;
import edu.itc.se.shop.domain.Product;

@DataJpaTest
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class ProductMappingTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void shouldMapMoneyAndCategoryCorrectly() {

        Product product = new Product(
                "MAPPING-001",
                "Mapping Test Product",
                Money.eur("199.99"),
                Category.ELECTRONICS
        );

        Product savedProduct = entityManager.persistFlushFind(product);

        assertNotNull(savedProduct);

        // Verify Money mapping
        assertEquals(
                0,
                new BigDecimal("199.99").compareTo(
                        savedProduct.getPrice().amount()
                )
        );

        assertEquals(
                "EUR",
                savedProduct.getPrice().currency()
        );

        // Verify Category mapping
        assertEquals(
                Category.ELECTRONICS,
                savedProduct.getCategory()
        );

        // Verify the database stores the enum as STRING, not ordinal.
        String categoryValue = (String) entityManager.getEntityManager()
                .createNativeQuery(
                        "SELECT category FROM product WHERE id = :id"
                )
                .setParameter("id", savedProduct.getId())
                .getSingleResult();

        assertEquals("ELECTRONICS", categoryValue);
    }
}