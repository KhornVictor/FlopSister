package edu.itc.shop.domain;

import java.util.Optional;

/**
 * Persistence contract for orders.
 */
public interface OrderRepository {

    Optional<Order> findById(String id);

    /**
     * Persists an order, assigning a generated ID (e.g., {@code o-1001}) when {@code order.id()} is null or blank.
     */
    Order save(Order order);
}
