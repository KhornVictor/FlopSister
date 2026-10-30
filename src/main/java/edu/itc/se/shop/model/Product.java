
package edu.itc.se.shop.model;

import java.math.BigDecimal;

public record Product(
        long id,
        String name,
        BigDecimal price,
        int stock
) {
    public Product withStock(int newStock) {
        return new Product(id, name, price, newStock);
    }
}
