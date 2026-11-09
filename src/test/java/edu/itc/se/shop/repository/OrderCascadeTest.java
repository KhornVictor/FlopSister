package edu.itc.se.shop.repository;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import edu.itc.se.shop.domain.Category;
import edu.itc.se.shop.domain.Customer;
import edu.itc.se.shop.domain.Money;
import edu.itc.se.shop.domain.Order;
import edu.itc.se.shop.domain.OrderLine;
import edu.itc.se.shop.domain.Product;
import jakarta.persistence.EntityManager;

@DataJpaTest
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@org.springframework.test.context.ActiveProfiles("test")
class OrderCascadeTest {

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldCascadePersistRemoveOrphansAndDeleteOrderLines() {

        // 1. Create and persist customer
        Customer customer = new Customer(
                "Test Customer",
                "test-cascade@example.com"
        );
        entityManager.persist(customer);

        // 2. Create and persist two products
        Product product1 = new Product(
                "CASCADE-001",
                "Keyboard",
                Money.eur("100.00"),
                Category.ELECTRONICS
        );

        Product product2 = new Product(
                "CASCADE-002",
                "Mouse",
                Money.eur("150.00"),
                Category.ELECTRONICS
        );

        entityManager.persist(product1);
        entityManager.persist(product2);

        // 3. Create an order containing two lines
        Order order = new Order(customer);

        order.addLine(product1, 2);
        order.addLine(product2, 3);

        // 4. Persist order and cascade-persist its lines
        entityManager.persist(order);
        entityManager.flush();

        assertEquals(2, order.getLines().size());

        // Expected total: (100 x 2) + (150 x 3) = 650
        assertEquals(
                0,
                new BigDecimal("650.00").compareTo(order.total().amount())
        );

        // Save IDs before clearing the persistence context
        Long customerId = customer.getId();
        Long product1Id = product1.getId();
        Long product2Id = product2.getId();
        Long orderId = order.getId();

        Long firstLineId = order.getLines().get(0).getId();
        Long secondLineId = order.getLines().get(1).getId();

        // 5. Clear and reload the order
        entityManager.clear();

        Order savedOrder = entityManager.find(Order.class, orderId);

        assertNotNull(savedOrder);
        assertEquals(2, savedOrder.getLines().size());

        assertEquals(
                0,
                new BigDecimal("650.00").compareTo(savedOrder.total().amount())
        );

        // 6. Remove one line and verify orphan removal
        OrderLine firstLine = savedOrder.getLines().stream()
                .filter(line -> line.getId().equals(firstLineId))
                .findFirst()
                .orElseThrow();

        savedOrder.removeLine(firstLine);

        entityManager.flush();
        entityManager.clear();

        assertNull(entityManager.find(OrderLine.class, firstLineId));
        assertNotNull(entityManager.find(OrderLine.class, secondLineId));

        // 7. Remove the entire order
        Order orderToDelete = entityManager.find(Order.class, orderId);

        entityManager.remove(orderToDelete);

        entityManager.flush();
        entityManager.clear();

        // 8. Verify order and remaining line were deleted
        assertNull(entityManager.find(Order.class, orderId));
        assertNull(entityManager.find(OrderLine.class, secondLineId));

        // Customer and products must remain
        assertNotNull(entityManager.find(Customer.class, customerId));
        assertNotNull(entityManager.find(Product.class, product1Id));
        assertNotNull(entityManager.find(Product.class, product2Id));
    }
}