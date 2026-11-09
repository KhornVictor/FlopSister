package edu.itc.se.shop.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import jakarta.persistence.EntityManagerFactory;
import edu.itc.se.shop.domain.OrderStatus;
import edu.itc.se.shop.domain.Product;
import edu.itc.se.shop.repository.OrderRepository;
import edu.itc.se.shop.repository.ProductRepository;
import edu.itc.se.shop.support.TestData;

@SpringBootTest
@ActiveProfiles("test")
class OrderServiceTest {
    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orders;

    @Autowired
    private ProductRepository products;

    @Autowired
    private TestData data;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void managedChangeIsDirtyCheckedWithoutSave() {
        Long orderId = data.newOrder();
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        orderService.markPaid(orderId);

        assertThat(orders.findById(orderId).orElseThrow().getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(statistics.getEntityUpdateCount()).isEqualTo(1);
    }

    @Test
    void detachedChangeIsIgnoredUntilSavedByMerge() {
        Long productId = data.newProduct();
        Product detached = products.findById(productId).orElseThrow();
        int originalStock = detached.getStock();

        detached.setStock(99);
        assertThat(products.findById(productId).orElseThrow().getStock()).isEqualTo(originalStock);

        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        products.save(detached);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
        assertThat(products.findById(productId).orElseThrow().getStock()).isEqualTo(99);
    }

    @Test
    void readOnlyTransactionDoesNotFlushAnAccidentalMutation() {
        Long orderId = data.newOrder();

        orderService.wrongReadOnlyChangeStatus(orderId, OrderStatus.PAID);

        assertThat(orders.findById(orderId).orElseThrow().getStatus()).isEqualTo(OrderStatus.PENDING);
    }
}
