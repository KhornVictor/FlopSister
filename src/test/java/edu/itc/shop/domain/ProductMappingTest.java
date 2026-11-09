
package edu.itc.shop.domain;

import edu.itc.se.shop.domain.Category;
import edu.itc.se.shop.domain.Money;
import edu.itc.se.shop.domain.Product;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

@ContextConfiguration(classes = edu.itc.se.shop.ShopApplication.class)
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
class ProductMappingTest {

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Should persist and load Product with Money and Category")
    void shouldPersistAndLoadProductWithMappings() {

        Product product = new Product(
                "SKU-001",
                "Gaming Laptop",
                Money.eur("1299.99"),
                Category.ELECTRONICS
        );

        entityManager.persist(product);
        entityManager.flush();

        Long productId = product.getId();

        entityManager.clear();

        Product loaded = entityManager.find(Product.class, productId);

        assertThat(loaded).isNotNull();
        assertThat(loaded.getSku()).isEqualTo("SKU-001");
        assertThat(loaded.getName()).isEqualTo("Gaming Laptop");
        assertThat(loaded.getCategory()).isEqualTo(Category.ELECTRONICS);
        assertThat(loaded.getPrice().amount())
                .isEqualByComparingTo(new BigDecimal("1299.99"));
        assertThat(loaded.getPrice().currency()).isEqualTo("EUR");
    }
}
