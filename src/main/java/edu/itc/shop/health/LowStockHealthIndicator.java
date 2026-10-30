// Somnang's part: Health check indicator for low-stock monitoring.
package edu.itc.shop.health;

import java.util.List;

import edu.itc.shop.service.ProductService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class LowStockHealthIndicator implements HealthIndicator {
    private final ProductService productService;

    public LowStockHealthIndicator(ProductService productService) {
        this.productService = productService;
    }

    @Override
    public Health health() {
        List<String> lowStockProducts = productService.lowStockProducts();
        if (lowStockProducts.isEmpty()) {
            return Health.up().build();
        }
        return Health.down().withDetail("lowStockProducts", lowStockProducts).build();
    }
}
