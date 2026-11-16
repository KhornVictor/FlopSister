package edu.itc.shop.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Order aggregate containing ordered lines, calculated total, and lifecycle status.
 */
public record Order(
        String id,
        List<Line> lines,
        BigDecimal total,
        OrderStatus status) {

    public Order {
        lines = List.copyOf(lines);
    }

    public Order withStatus(OrderStatus newStatus) {
        return new Order(id, lines, total, newStatus);
    }
}
