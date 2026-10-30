
package edu.itc.se.shop.service;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(
            long id, int available, int requested) {
        super("Product " + id + " has " + available
                + " units, cannot remove " + requested);
    }
}
