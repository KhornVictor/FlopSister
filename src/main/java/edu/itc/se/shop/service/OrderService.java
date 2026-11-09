
package edu.itc.se.shop.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.itc.se.shop.domain.Order;
import edu.itc.se.shop.domain.OrderStatus;
import edu.itc.se.shop.repository.OrderRepository;
import edu.itc.se.shop.web.dto.OrderDetailsDto;
import edu.itc.se.shop.web.dto.OrderDto;
import edu.itc.se.shop.web.dto.OrderFeedResponse;
import edu.itc.se.shop.web.dto.OrderResponse;
import edu.itc.se.shop.web.dto.OrderSummary;
import edu.itc.se.shop.web.dto.OrderSummaryDto;

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

    @Transactional(readOnly = true)
    public OrderFeedResponse feedWithKeyset(int size, String after) {
        ScrollPosition scrollPosition;
        if (after != null && !after.isBlank()) {
            String[] parts = after.split(",");
            Instant placedAt = Instant.parse(parts[0].trim());
            Long id = Long.parseLong(parts[1].trim());
            Map<String, Object> keyset = new LinkedHashMap<>();
            keyset.put("placedAt", placedAt);
            keyset.put("id", id);
            scrollPosition = ScrollPosition.forward(keyset);
        } else {
            scrollPosition = ScrollPosition.keyset();
        }

        Sort sort = Sort.by(Sort.Order.desc("placedAt"), Sort.Order.desc("id"));
        Window<Order> window = orderRepository.findBy(scrollPosition, Limit.of(size), sort);

        List<OrderSummary> items = window.getContent().stream()
                .map(OrderSummaryDto::from)
                .collect(Collectors.toList());

        String nextCursor = null;
        if (!window.isEmpty()) {
            Order last = window.getContent().get(window.getContent().size() - 1);
            nextCursor = last.getPlacedAt().toString() + "," + last.getId();
        }

        return new OrderFeedResponse(items, nextCursor, window.hasNext());
    }

    @Transactional(readOnly = true)
    public OrderFeedResponse feedWithSlice(int size, int page) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("placedAt"), Sort.Order.desc("id")));
        Slice<OrderSummary> slice = orderRepository.findSliceByOrderByPlacedAtDescIdDesc(pageable);
        List<OrderSummary> items = slice.getContent();

        String nextCursor = null;
        if (!items.isEmpty()) {
            OrderSummary last = items.get(items.size() - 1);
            nextCursor = last.getPlacedAt().toString() + "," + last.getId();
        }

        return new OrderFeedResponse(items, nextCursor, slice.hasNext());
    }

    private Order findOrder(Long id) {
        return orderRepository.findById(id).orElseThrow(
                () -> new IllegalArgumentException("Order not found: " + id));
    }
}
