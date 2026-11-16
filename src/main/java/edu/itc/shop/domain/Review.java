
package edu.itc.shop.domain;

public record Review(
        long id,
        long productId,
        String author,
        int stars,
        String text
) {
}
