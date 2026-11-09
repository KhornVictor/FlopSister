package edu.itc.se.shop.web.dto;

import edu.itc.se.shop.domain.Order;
import edu.itc.se.shop.domain.OrderStatus;

public record OrderDto(Long id, OrderStatus status, long version) {
    public static OrderDto from(Order order) {
        return new OrderDto(order.getId(), order.getStatus(), order.getVersion());
    }
}
