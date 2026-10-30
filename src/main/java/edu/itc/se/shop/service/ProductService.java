
package edu.itc.se.shop.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

import edu.itc.se.shop.model.Product;
import edu.itc.se.shop.web.dto.CreateProduct;

@Service
public class ProductService {

    private final Map<Long, Product> products = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    public ProductService() {
        products.put(1L, new Product(
                1L, "USB-C cable", new BigDecimal("7.90"), 25));
        products.put(2L, new Product(
                2L, "Wireless mouse", new BigDecimal("15.50"), 10));
        products.put(3L, new Product(
                3L, "Keyboard", new BigDecimal("29.90"), 8));

        nextId.set(4);
    }

    public List<Product> findAll() {
        return products.values().stream()
                .sorted((a, b) -> Long.compare(a.id(), b.id()))
                .toList();
    }

    public List<Product> search(String q) {
        if (q == null || q.isBlank()) {
            return findAll();
        }

        String keyword = q.toLowerCase();

        return findAll().stream()
                .filter(p -> p.name().toLowerCase().contains(keyword))
                .toList();
    }

    public Product findById(long id) {
        Product product = products.get(id);

        if (product == null) {
            throw new ProductNotFoundException(id);
        }

        return product;
    }

    public Product create(CreateProduct cmd) {
        long id = nextId.getAndIncrement();

        Product product = new Product(
                id, cmd.name(), cmd.price(), cmd.stock());

        products.put(id, product);
        return product;
    }

    public Product updateStock(long id, int delta) {
        return products.compute(id, (key, product) -> {
            if (product == null) {
                throw new ProductNotFoundException(id);
            }

            int newStock = product.stock() + delta;

            if (newStock < 0) {
                throw new InsufficientStockException(
                        id, product.stock(), -delta);
            }

            return product.withStock(newStock);
        });
    }

    public void delete(long id) {
        if (products.remove(id) == null) {
            throw new ProductNotFoundException(id);
        }
    }
}
