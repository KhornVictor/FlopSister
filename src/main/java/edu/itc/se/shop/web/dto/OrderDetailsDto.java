package edu.itc.se.shop.web.dto;

import java.util.List;

import edu.itc.se.shop.domain.OrderStatus;

public record OrderDetailsDto(
        Long id,
        OrderStatus status,
        String customerEmail,
        List<OrderLineDetails> lines) {

    public record OrderLineDetails(
            Long productId,
            String productName,
            int quantity) {
    }
}