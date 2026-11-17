package edu.itc.shop.service;

/**
 * Thrown when a requested product does not exist.
 */
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(long id) {
        super("Product %d not found".formatted(id));
    }
}
