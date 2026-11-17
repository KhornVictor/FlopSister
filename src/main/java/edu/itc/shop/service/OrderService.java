
package edu.itc.shop.service;

import java.util.List;

import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.itc.shop.domain.Order;
import edu.itc.shop.repository.OrderRepository;
import edu.itc.shop.web.dto.PlaceOrder;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * A customer can only place an order under their own username.
     */
    @PreAuthorize("#customerId == authentication.name")
    public Order place(String customerId, PlaceOrder cmd) {
        Order order = new Order(
                customerId,
                cmd.productId(),
                cmd.quantity()
        );

        return orderRepository.save(order);
    }

    /**
     * Customers can view their own history.
     * ADMIN can view any customer's history.
     */
    @PreAuthorize("hasRole('ADMIN') or #customerId == authentication.name")
    @Transactional(readOnly = true)
    public List<Order> history(String customerId) {
        return orderRepository.findByCustomer(customerId);
    }

    /**
     * Customers can only view their own orders.
     * ADMIN can view every order.
     */
    @PostAuthorize("hasRole('ADMIN') or returnObject.customer == authentication.name")
    @Transactional(readOnly = true)
    public Order get(long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
    }

    /**
     * Only ADMIN can ship an order.
     */
    @PreAuthorize("hasRole('ADMIN')")
    public Order ship(long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        order.setStatus(Order.Status.SHIPPED);

        return orderRepository.save(order);
    }

    /**
     * Only ADMIN can retrieve all orders.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<Order> all() {
        return orderRepository.findAll();
    }
}
