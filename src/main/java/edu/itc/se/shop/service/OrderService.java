package edu.itc.se.shop.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.itc.se.shop.domain.Order;
import edu.itc.se.shop.repository.OrderRepository;
import edu.itc.se.shop.web.dto.OrderResponse;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersWithLinesByCustomerEmail(String email) {
        List<Order> orders = orderRepository.findWithLinesByCustomerEmail(email);
        return orders.stream()
                .map(OrderResponse::from)
                .toList();
    }
}
