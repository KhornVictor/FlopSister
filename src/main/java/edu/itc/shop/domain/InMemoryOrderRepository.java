package edu.itc.shop.domain;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe in-memory implementation of {@link OrderRepository}.
 */
public class InMemoryOrderRepository implements OrderRepository {

    private final ConcurrentMap<String, Order> orders = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1000);

    @Override
    public Optional<Order> findById(String id) {
        return Optional.ofNullable(orders.get(id));
    }

    @Override
    public Order save(Order order) {
        String id = (order.id() == null || order.id().isBlank())
                ? "o-" + nextId.incrementAndGet()
                : order.id();
        Order saved = new Order(id, order.lines(), order.total(), order.status());
        orders.put(id, saved);
        return saved;
    }
}
