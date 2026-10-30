// Somnang's part: Application configuration properties for shop settings.
package edu.itc.shop.config;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("shop")
@Validated
public record ShopProperties(
        @NotBlank(message = "Shop currency is required") String currency,
        @NotNull(message = "taxRate is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "taxRate must be greater than or equal to 0")
        @DecimalMax(value = "1.0", inclusive = true, message = "taxRate must be less than or equal to 1")
        BigDecimal taxRate,
        @NotNull(message = "lowStockThreshold is required") Integer lowStockThreshold
) {
}
