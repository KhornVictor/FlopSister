package edu.itc.shop.domain;

import java.math.BigDecimal;

public record Product(
        long id,
        String name,
        String sku,
        BigDecimal price,
        int stock,
        int version
) {
    public Product adjustStock(int delta) {
        if (this.stock + delta < 0) {
            throw new InsufficientStockException(
                    "Insufficient stock: current stock is " + this.stock + ", requested delta is " + delta
            );
        }
        return new Product(id, name, sku, price, stock + delta, version);
    }
}
