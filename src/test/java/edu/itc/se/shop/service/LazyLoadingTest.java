
package edu.itc.se.shop.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import edu.itc.se.shop.domain.Order;
import edu.itc.se.shop.domain.OrderStatus;
import edu.itc.se.shop.repository.OrderRepository;
import edu.itc.se.shop.support.TestData;

@SpringBootTest
@ActiveProfiles("test")
@Execution(ExecutionMode.SAME_THREAD)
class LazyLoadingTest {

    @Autowired
    private OrderRepository orders;

    @Autowired
    private OrderService orderService;

    @Autowired
    private TestData data;

    @Test
    void accessingLazyLinesAfterTransactionFails() {
        Long customerId = data.createPaidOrders(1, 3);

        Order detachedOrder = orders
                .findByStatusAndCustomer_Id(OrderStatus.PAID, customerId)
                .get(0);

        assertThatThrownBy(() -> detachedOrder.getLines().size())
                .isInstanceOf(LazyInitializationException.class);
    }

    @Test
    void transactionalDtoMappingSuccessfullyLoadsLazyRelationships() {
        Long customerId = data.createPaidOrders(1, 3);

        Long orderId = orders
                .findByStatusAndCustomer_Id(OrderStatus.PAID, customerId)
                .get(0)
                .getId();

        var details = orderService.viewDetails(orderId);

        assertThat(details.id()).isEqualTo(orderId);
        assertThat(details.status()).isEqualTo(OrderStatus.PAID);
        assertThat(details.customerEmail()).contains("@example.test");
        assertThat(details.lines()).hasSize(3);

        assertThat(details.lines())
                .allSatisfy(line ->
                        assertThat(line.productName()).isNotBlank());
    }
}
