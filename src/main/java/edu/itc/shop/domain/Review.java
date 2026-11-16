package edu.itc.shop.domain;

/**
 * Customer review associated with a product.
 */
public record Review(
        long id,
        long productId,
        int stars,
        String author,
        String text) {
}
