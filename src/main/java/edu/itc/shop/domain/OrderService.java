package edu.itc.shop.domain;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

/**
 * Coordinates atomic stock reservation, order creation, payment, and cancellation.
 */
public class OrderService {

    private final ProductRepository products;
    private final OrderRepository orders;

    public OrderService(ProductRepository products, OrderRepository orders) {
        this.products = products;
        this.orders = orders;
    }

    /**
     * Validates all requested lines, reserves stock atomically, and persists a {@code NEW} order.
     */
    public synchronized Order place(List<Line> requestedLines) {
        Map<Long, Product> updatedProducts = new LinkedHashMap<>();
        BigDecimal total = BigDecimal.ZERO;

        for (Line line : requestedLines) {
            Product current = updatedProducts.get(line.productId());
            if (current == null) {
                current = products.findById(line.productId())
                        .orElseThrow(() -> new NotFoundException("Product " + line.productId() + " not found"));
            }
            Product reserved = current.adjustStock(-line.quantity());
            updatedProducts.put(line.productId(), reserved);
            total = total.add(current.price().multiply(BigDecimal.valueOf(line.quantity())));
        }

        updatedProducts.values().forEach(products::save);
        return orders.save(new Order(null, requestedLines, total, OrderStatus.NEW));
    }

    public Order get(String id) {
        return orders.findById(id)
                .orElseThrow(() -> new NotFoundException("Order " + id + " not found"));
    }

    /**
     * Transitions an order from {@code NEW} to {@code PAID}, throwing 409 otherwise.
     */
    public synchronized Order pay(String id) {
        Order order = get(id);
        if (order.status() != OrderStatus.NEW) {
            throw new ClientErrorException(
                    "Order " + id + " cannot be paid from status " + order.status(),
                    Response.Status.CONFLICT);
        }
        return orders.save(order.withStatus(OrderStatus.PAID));
    }

    /**
     * Transitions an order from {@code NEW} to {@code CANCELLED} and restores reserved stock.
     */
    public synchronized Order cancel(String id) {
        Order order = get(id);
        if (order.status() != OrderStatus.NEW) {
            throw new ClientErrorException(
                    "Order " + id + " cannot be cancelled from status " + order.status(),
                    Response.Status.CONFLICT);
        }
        for (Line line : order.lines()) {
            products.findById(line.productId())
                    .ifPresent(product -> products.save(product.adjustStock(line.quantity())));
        }
        return orders.save(order.withStatus(OrderStatus.CANCELLED));
    }
}
