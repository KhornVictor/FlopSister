package edu.itc.shop.api.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import edu.itc.shop.domain.OrderStatus;

/**
 * Order API representation including HATEOAS {@code _links} hypermedia controls.
 */
public record OrderDto(
        String id,
        OrderStatus status,
        BigDecimal total,
        List<LineDto> lines,
        Map<String, String> _links) {
}
