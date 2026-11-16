
package edu.itc.shop.api.dto;

import edu.itc.shop.domain.Money;
import edu.itc.shop.domain.Product;

public record ProductV2(
        long id,
        String name,
        String sku,
        Money price,
        int stock,
        boolean available) {

    public static ProductV2 from(Product product) {
        return new ProductV2(
                product.id(),
                product.name(),
                product.sku(),
                new Money(product.price(), "USD"),
                product.stock(),
                product.stock() > 0
        );
    }
}
