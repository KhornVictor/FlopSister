package edu.itc.se.shop.web.dto;

import java.time.Instant;
import edu.itc.se.shop.domain.Order;
import edu.itc.se.shop.domain.OrderStatus;

public record OrderSummaryDto(
    Long id,
    OrderStatus status,
    Instant placedAt,
    CustomerName customer
) implements OrderSummary {

    public static OrderSummaryDto from(Order order) {
        String customerName = order.getCustomer() != null ? order.getCustomer().getName() : null;
        return new OrderSummaryDto(
            order.getId(),
            order.getStatus(),
            order.getPlacedAt(),
            () -> customerName
        );
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public OrderStatus getStatus() {
        return status;
    }

    @Override
    public Instant getPlacedAt() {
        return placedAt;
    }

    @Override
    public CustomerName getCustomer() {
        return customer;
    }
}
