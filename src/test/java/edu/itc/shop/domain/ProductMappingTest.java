package edu.itc.shop.domain;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = edu.itc.se.shop.ShopApplication.class)
@Transactional
class ProductMappingTest {

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Should persist and load Product with embedded Money, Enum, and Tags")
    void shouldPersistAndLoadProductWithMappings() {
        Product product = new Product(
                "Gaming Laptop",
                "High performance laptop",
                Money.of(new BigDecimal("1299.99"), "USD"),
                Category.ELECTRONICS
        );

        Tag techTag = new Tag("tech");
        Tag hardwareTag = new Tag("hardware");

        product.tag(techTag);
        product.tag(hardwareTag);

        entityManager.persist(techTag);
        entityManager.persist(hardwareTag);
        entityManager.persist(product);
        entityManager.flush();
        entityManager.clear();

        Product loaded = entityManager.find(Product.class, product.getId());
        assertThat(loaded).isNotNull();
        assertThat(loaded.getName()).isEqualTo("Gaming Laptop");
        assertThat(loaded.getCategory()).isEqualTo(Category.ELECTRONICS);
        assertThat(loaded.getPrice().amount()).isEqualByComparingTo(new BigDecimal("1299.99"));
        assertThat(loaded.getPrice().currency()).isEqualTo("USD");
        assertThat(loaded.getTags()).hasSize(2);
        assertThat(loaded.getVersion()).isEqualTo(0);
    }
}
