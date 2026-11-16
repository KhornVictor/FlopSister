package edu.itc.shop.api.dto;

import edu.itc.shop.domain.Line;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

/**
 * Validated order line item payload.
 */
public record LineDto(
        @Positive long productId,
        @Min(1) int quantity) {

    public Line toLine() {
        return new Line(productId, quantity);
    }

    public static LineDto from(Line line) {
        return new LineDto(line.productId(), line.quantity());
    }
}
