package edu.itc.shop.domain;

/**
 * Domain line item associating a product identifier with an ordered quantity.
 */
public record Line(long productId, int quantity) {
}
