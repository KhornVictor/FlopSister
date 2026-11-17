package edu.itc.shop.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Request payload for creating a new product.
 */
public record CreateProductRequest(
        @NotBlank @Size(max = 120) String name,
        @NotNull @DecimalMin("0.01") BigDecimal price,
        @Min(0) int stock) {
}
