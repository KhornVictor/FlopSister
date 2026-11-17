package edu.itc.shop.web.dto;

import edu.itc.shop.domain.Product;
import java.math.BigDecimal;

/**
 * Public API representation of a catalog product.
 */
public record ProductResponse(Long id, String name, BigDecimal price, int stock) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getPrice(), product.getStock());
    }
}
