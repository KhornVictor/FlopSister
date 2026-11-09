package edu.itc.se.shop.domain;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import jakarta.persistence.EntityManager;

@DataJpaTest
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@org.springframework.test.context.ActiveProfiles("test")
class EntityIdentityTest {

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldMaintainEntityIdentityBeforeAndAfterPersistence() {

        Customer customer = new Customer(
                "Identity Customer",
                "identity@example.com"
        );

        Product product = new Product(
                "IDENTITY-001",
                "Identity Product",
                Money.eur("100.00"),
                Category.ELECTRONICS
        );

        Tag tag = new Tag("Identity Tag");

        Order order = new Order(customer);
        order.addLine(product, 2);

        OrderLine orderLine = order.getLines().get(0);

        // Different transient entities should not be equal.
        assertNotEquals(customer, new Customer("Another", "another@example.com"));
        assertNotEquals(product, new Product(
                "IDENTITY-002", "Another Product",
                Money.eur("50.00"), Category.BOOKS
        ));
        assertNotEquals(tag, new Tag("Another Tag"));

        // Store entities in a HashSet before persistence.
        Set<Object> entities = new HashSet<>();
        entities.add(customer);
        entities.add(product);
        entities.add(tag);
        entities.add(order);
        entities.add(orderLine);

        // Persist entities.
        entityManager.persist(customer);
        entityManager.persist(tag);

        product.tag(tag);
        entityManager.persist(product);

        entityManager.persist(order);

        entityManager.flush();

        // Entities should remain findable in the HashSet.
        assertTrue(entities.contains(customer));
        assertTrue(entities.contains(product));
        assertTrue(entities.contains(tag));
        assertTrue(entities.contains(order));
        assertTrue(entities.contains(orderLine));

        // Save database IDs.
        Long customerId = customer.getId();
        Long productId = product.getId();
        Long tagId = tag.getId();
        Long orderId = order.getId();
        Long orderLineId = orderLine.getId();

        entityManager.clear();

        // Reload entities from the database.
        Customer savedCustomer = entityManager.find(Customer.class, customerId);
        Product savedProduct = entityManager.find(Product.class, productId);
        Tag savedTag = entityManager.find(Tag.class, tagId);
        Order savedOrder = entityManager.find(Order.class, orderId);
        OrderLine savedOrderLine = entityManager.find(OrderLine.class, orderLineId);

        // Original and reloaded entities should be equal.
        assertEquals(customer, savedCustomer);
        assertEquals(product, savedProduct);
        assertNotSame(product, savedProduct);
        assertEquals(tag, savedTag);
        assertEquals(order, savedOrder);
        assertEquals(orderLine, savedOrderLine);

        // Their hash codes should remain consistent.
        assertEquals(customer.hashCode(), savedCustomer.hashCode());
        assertEquals(product.hashCode(), savedProduct.hashCode());
        assertEquals(tag.hashCode(), savedTag.hashCode());
        assertEquals(order.hashCode(), savedOrder.hashCode());
        assertEquals(orderLine.hashCode(), savedOrderLine.hashCode());

        // Reloaded entities should not create duplicates in the HashSet.
        assertFalse(entities.add(savedCustomer));
        assertFalse(entities.add(savedProduct));
        assertFalse(entities.add(savedTag));
        assertFalse(entities.add(savedOrder));
        assertFalse(entities.add(savedOrderLine));
    }
}