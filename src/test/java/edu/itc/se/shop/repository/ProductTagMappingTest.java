package edu.itc.se.shop.repository;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import edu.itc.se.shop.domain.Category;
import edu.itc.se.shop.domain.Money;
import edu.itc.se.shop.domain.Product;
import edu.itc.se.shop.domain.Tag;
import jakarta.persistence.EntityManager;

@DataJpaTest
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class ProductTagMappingTest {

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldMaintainBidirectionalProductTagRelationship() {
        Product product = new Product(
                "TAG-001",
                "Test Product",
                Money.eur("100.00"),
                Category.ELECTRONICS
        );

        Tag tag = new Tag("Electronics");

        entityManager.persist(tag);

        product.tag(tag);

        entityManager.persist(product);
        entityManager.flush();

        Long productId = product.getId();
        Long tagId = tag.getId();

        entityManager.clear();

        Product savedProduct = entityManager.find(Product.class, productId);
        Tag savedTag = entityManager.find(Tag.class, tagId);

        assertNotNull(savedProduct);
        assertNotNull(savedTag);

        assertTrue(savedProduct.getTags().contains(savedTag));
        assertTrue(savedTag.getProducts().contains(savedProduct));
    }
}