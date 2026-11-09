package edu.itc.se.shop.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.itc.se.shop.domain.Order;
import edu.itc.se.shop.domain.OrderStatus;
import edu.itc.se.shop.repository.OrderRepository;
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

    @Transactional(readOnly = true)
    public void wrongReadOnlyChangeStatus(Long id, OrderStatus next) {
        findOrder(id).setStatus(next);
    }

    private Order findOrder(Long id) {
        return orders.findById(id).orElseThrow(
                () -> new IllegalArgumentException("Order not found: " + id));
    }
}
