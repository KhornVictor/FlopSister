
package edu.itc.shop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;

import edu.itc.shop.domain.Order;
import edu.itc.shop.repository.OrderRepository;
import edu.itc.shop.service.OrderService;

@SpringBootTest
class OrderServiceSecurityTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        orderRepository.save(new Order("alice", 1L, 2));
        orderRepository.save(new Order("bob", 2L, 3));
    }

    @Test
    @WithMockUser(username = "alice", roles = "CUSTOMER")
    void customerCanViewOwnOrderHistory() {
        var orders = orderService.history("alice");

        assertThat(orders)
                .hasSize(1)
                .allMatch(order -> order.getCustomer().equals("alice"));
    }

    @Test
    @WithMockUser(username = "alice", roles = "CUSTOMER")
    void customerCannotViewAnotherCustomersHistory() {
        assertThatThrownBy(() -> orderService.history("bob"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN", "CUSTOMER"})
    void adminCanViewAnotherCustomersHistory() {
        var orders = orderService.history("bob");

        assertThat(orders)
                .hasSize(1)
                .allMatch(order -> order.getCustomer().equals("bob"));
    }

    @Test
    @WithMockUser(username = "alice", roles = "CUSTOMER")
    void customerCannotShipOrder() {
        Order order = orderRepository.findByCustomer("alice").get(0);

        assertThatThrownBy(() -> orderService.ship(order.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void unauthenticatedUserCannotAccessOrderHistory() {
        assertThatThrownBy(() -> orderService.history("alice"))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }
}
