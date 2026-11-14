package edu.itc.shop.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryProductRepository implements ProductRepository {

    private final ConcurrentMap<Long, Product> products =
            new ConcurrentHashMap<>();

    private final AtomicLong nextId = new AtomicLong(0);

    public InMemoryProductRepository() {
        save(new Product(0, "Kettle", "KT-01",
                new BigDecimal("24.90"), 12, 0));

        save(new Product(0, "Mug", "MG-01",
                new BigDecimal("6.50"), 40, 0));

        save(new Product(0, "Tea", "TE-01",
                new BigDecimal("4.20"), 0, 0));
    }

    @Override
    public List<Product> findAll() {
        return products.values().stream()
                .sorted((a, b) -> Long.compare(a.id(), b.id()))
                .toList();
    }

    @Override
    public Optional<Product> findById(long id) {
        return Optional.ofNullable(products.get(id));
    }

    @Override
    public Optional<Product> findBySku(String sku) {
        return products.values().stream()
                .filter(product -> product.sku().equals(sku))
                .findFirst();
    }

    @Override
    public Product save(Product product) {
        long id = product.id() == 0
                ? nextId.incrementAndGet()
                : product.id();

        Product saved = new Product(
                id,
                product.name(),
                product.sku(),
                product.price(),
                product.stock(),
                product.id() == 0 ? 1 : product.version() + 1
        );

        products.put(id, saved);
        nextId.accumulateAndGet(id, Math::max);

        return saved;
    }

    @Override
    public boolean delete(long id) {
        return products.remove(id) != null;
    }
}
