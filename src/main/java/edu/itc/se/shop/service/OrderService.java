
package edu.itc.se.shop.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.itc.se.shop.domain.Order;
import edu.itc.se.shop.domain.OrderStatus;
import edu.itc.se.shop.repository.OrderRepository;
import edu.itc.se.shop.web.dto.OrderDetailsDto;
import edu.itc.se.shop.web.dto.OrderDto;

@Service
public class OrderService {
    private final OrderRepository orders;

    public OrderService(OrderRepository orders) {
        this.orders = orders;
    }

    @Transactional
    public void markPaid(Long orderId) {
        Order order = findOrder(orderId);
        order.setStatus(OrderStatus.PAID);
    }

    @Transactional
    public void changeStatus(Long id, OrderStatus next) {
        Order order = findOrder(id);
        order.setStatus(next);
    }

    @Transactional(readOnly = true)
    public OrderDto view(Long id) {
        return OrderDto.from(findOrder(id));
    }

    // Maps lazy relationships while the transaction is open.
    @Transactional(readOnly = true)
    public OrderDetailsDto viewDetails(Long id) {
        Order order = findOrder(id);

        List<OrderDetailsDto.OrderLineDetails> lines =
                order.getLines().stream()
                        .map(line -> new OrderDetailsDto.OrderLineDetails(
                                line.getProduct().getId(),
                                line.getProduct().getName(),
                                line.getQuantity()))
                        .toList();

        return new OrderDetailsDto(
                order.getId(),
                order.getStatus(),
                order.getCustomer().getEmail(),
                lines);
    }

    @Transactional(readOnly = true)
    public void wrongReadOnlyChangeStatus(Long id, OrderStatus next) {
        findOrder(id).setStatus(next);
    }

    private Order findOrder(Long id) {
        return orders.findById(id).orElseThrow(
                () -> new IllegalArgumentException("Order not found: " + id));
    }
}
