package edu.itc.se.shop.domain;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;


import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(
    replace = AutoConfigureTestDatabase.Replace.NONE
)
class ProductMappingTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Product Money and Category mappings should round-trip")
    void shouldPersistAndLoadProductMappings() {

        // 1. Create a product
        Product product = new Product(
            "TEST-001",
            "Gaming Laptop",
            new Money(new BigDecimal("1299.99"), "USD"),
            Category.HARDWARE
        );

        // 2. Persist the product
        entityManager.persist(product);

        // 3. Flush changes to the database
        entityManager.flush();

        Long productId = product.getId();

        // 4. Clear the persistence context
        entityManager.clear();

        // 5. Reload the product from the database
        Product loaded = entityManager.find(Product.class, productId);

        // 6. Verify Money mapping
        assertThat(loaded).isNotNull();

        assertThat(loaded.getPrice().amount())
            .isEqualByComparingTo("1299.99");

        assertThat(loaded.getPrice().currency())
            .isEqualTo("USD");

        // 7. Verify Category mapping
        assertThat(loaded.getCategory())
            .isEqualTo(Category.HARDWARE);

        // 8. Verify the actual database value is text
        Object categoryValue = entityManager
            .getEntityManager()
            .createNativeQuery(
                "SELECT category FROM product WHERE id = :id"
            )
            .setParameter("id", productId)
            .getSingleResult();

        assertThat(categoryValue.toString())
            .isEqualTo("HARDWARE");
    }
}
