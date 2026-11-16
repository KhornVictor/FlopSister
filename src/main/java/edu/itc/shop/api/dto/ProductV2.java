package edu.itc.shop.api.dto;

import edu.itc.shop.domain.Money;
import edu.itc.shop.domain.Product;

/**
 * Version 2 JSON representation of a product with typed money and stock availability.
 */
public record ProductV2(
        long id,
        String name,
        String sku,
        Money price,
        int stock,
        boolean available) {

    /**
     * Maps a domain {@link Product} into a {@link ProductV2} representation.
     */
    public static ProductV2 from(Product product) {
        return new ProductV2(
                product.id(),
                product.name(),
                product.sku(),
                new Money(product.price(), "EUR"),
                product.stock(),
                product.stock() > 0);
    }
}
