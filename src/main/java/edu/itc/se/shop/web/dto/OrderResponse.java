package edu.itc.se.shop.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import edu.itc.se.shop.domain.Order;
import edu.itc.se.shop.domain.OrderStatus;

public record OrderResponse(
    Long id,
    OrderStatus status,
    Instant placedAt,
    CustomerDto customer,
    BigDecimal totalAmount,
    String totalCurrency,
    List<OrderLineDto> lines
) {
    public record CustomerDto(Long id, String name, String email) {}

    public record OrderLineDto(
        Long id,
        Long productId,
        String productSku,
        String productName,
        int quantity,
        BigDecimal unitPriceAmount,
        String unitPriceCurrency
    ) {}

    public static OrderResponse from(Order order) {
        CustomerDto customerDto = order.getCustomer() != null ? new CustomerDto(
                order.getCustomer().getId(),
                order.getCustomer().getName(),
                order.getCustomer().getEmail()
        ) : null;

        List<OrderLineDto> lineDtos = order.getLines().stream()
                .map(line -> new OrderLineDto(
                        line.getId(),
                        line.getProduct().getId(),
                        line.getProduct().getSku(),
                        line.getProduct().getName(),
                        line.getQuantity(),
                        line.getUnitPrice().amount(),
                        line.getUnitPrice().currency()
                ))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getPlacedAt(),
                customerDto,
                order.total().amount(),
                order.total().currency(),
                lineDtos
        );
    }
}
