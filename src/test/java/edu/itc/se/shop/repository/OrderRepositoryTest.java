package edu.itc.se.shop.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import edu.itc.se.shop.domain.Address;
import edu.itc.se.shop.domain.Category;
import edu.itc.se.shop.domain.Customer;
import edu.itc.se.shop.domain.Money;
import edu.itc.se.shop.domain.Order;
import edu.itc.se.shop.domain.OrderStatus;
import edu.itc.se.shop.domain.Product;
import edu.itc.se.shop.web.dto.OrderSummary;
import jakarta.persistence.EntityManager;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private EntityManager entityManager;

    private Customer customer;
    private Product product1;
    private Product product2;
    private Order orderPaid;
    private Order orderNew;

    @BeforeEach
    void setUp() {
        customer = new Customer(
                "Ada Lovelace",
                "ada.test@example.com",
                new Address("123 Ada St", "London", "10001"),
                new Address("123 Ada St", "London", "10001")
        );
        customerRepository.save(customer);

        product1 = new Product("TEST-KB-1", "Mechanical Keyboard", Money.eur("50.00"), Category.HARDWARE);
        product1.setStock(5);
        productRepository.save(product1);

        product2 = new Product("TEST-MS-1", "Wireless Mouse", Money.eur("25.00"), Category.HARDWARE);
        product2.setStock(15);
        productRepository.save(product2);

        orderPaid = new Order(customer, OrderStatus.PAID);
        orderPaid.setPlacedAt(Instant.parse("2026-09-24T09:12:44.120Z"));
        orderPaid.addLine(product1, 1);
        orderPaid.addLine(product2, 2);
        orderRepository.save(orderPaid);

        orderNew = new Order(customer, OrderStatus.NEW);
        orderNew.setPlacedAt(Instant.parse("2026-09-23T17:03:10.551Z"));
        orderNew.addLine(product1, 3);
        orderRepository.save(orderNew);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void testDerivedQueryFindByCustomerEmailAndStatusOrderByPlacedAtDesc() {
        List<Order> orders = orderRepository.findByCustomerEmailAndStatusOrderByPlacedAtDesc(
                "ada.test@example.com", OrderStatus.PAID);

        assertThat(orders).hasSize(1);
        assertEquals(orderPaid.getId(), orders.get(0).getId());
        assertEquals(OrderStatus.PAID, orders.get(0).getStatus());
    }

    @Test
    void testFetchJoinFindWithLinesByCustomerEmail() {
        // 1. Plain findById + clear -> lines should NOT be initialized (lazy loading)
        entityManager.clear();
        Order plainOrder = orderRepository.findById(orderPaid.getId()).orElseThrow();
        assertFalse(Hibernate.isInitialized(plainOrder.getLines()),
                "Order lines should not be initialized after a plain findById");

        // 2. Fetch join query -> lines and line product SHOULD be initialized
        entityManager.clear();
        List<Order> ordersWithLines = orderRepository.findWithLinesByCustomerEmail("ada.test@example.com");
        assertThat(ordersWithLines).isNotEmpty();

        Order fetchedOrder = ordersWithLines.stream()
                .filter(o -> o.getId().equals(orderPaid.getId()))
                .findFirst()
                .orElseThrow();

        assertTrue(Hibernate.isInitialized(fetchedOrder.getLines()),
                "Order lines must be initialized after fetch join query");
        assertFalse(fetchedOrder.getLines().isEmpty());
        assertTrue(Hibernate.isInitialized(fetchedOrder.getLines().get(0).getProduct()),
                "Product on order line must be initialized after fetch join");
    }

    @Test
    void testNativeQueryFindLowStock() {
        // product1 has stock 5, product2 has stock 15
        List<Product> lowStock = productRepository.findLowStock(10);

        assertThat(lowStock).isNotEmpty();
        assertThat(lowStock).extracting(Product::getSku).contains("TEST-KB-1");
        assertThat(lowStock).extracting(Product::getSku).doesNotContain("TEST-MS-1");
        assertThat(lowStock.get(0).getStock()).isLessThan(10);
    }

    @Test
    void testProjectionFindByStatus() {
        Page<OrderSummary> page = orderRepository.findByStatus(
                OrderStatus.PAID,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "placedAt"))
        );

        assertThat(page.getContent()).isNotEmpty();
        OrderSummary summary = page.getContent().stream()
                .filter(s -> s.getId().equals(orderPaid.getId()))
                .findFirst()
                .orElseThrow();

        assertEquals(orderPaid.getId(), summary.getId());
        assertEquals(OrderStatus.PAID, summary.getStatus());
        assertEquals("Ada Lovelace", summary.getCustomer().getName());
        assertThat(summary.getPlacedAt()).isNotNull();
    }

    @Test
    void testAggregateCountLinesByStatus() {
        long paidLinesCount = orderRepository.countLinesByStatus(OrderStatus.PAID);
        // orderPaid has 2 order lines
        assertThat(paidLinesCount).isGreaterThanOrEqualTo(2);

        long newLinesCount = orderRepository.countLinesByStatus(OrderStatus.NEW);
        // orderNew has 1 order line
        assertThat(newLinesCount).isGreaterThanOrEqualTo(1);
    }
}
