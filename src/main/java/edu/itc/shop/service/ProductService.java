// Meylorng's part: Product service for business logic and product creation/listing.
package edu.itc.shop.service;

import java.math.BigDecimal;
import java.util.List;

import edu.itc.shop.config.ShopProperties;
import edu.itc.shop.model.Product;
import edu.itc.shop.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service("shopProductService")
public class ProductService {
    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final ShopProperties shopProperties;

    public ProductService(@Qualifier("productNamedParameterJdbcRepository") ProductRepository productRepository, ShopProperties shopProperties) {
        this.productRepository = productRepository;
        this.shopProperties = shopProperties;
    }

    public List<Product> findAll(String q) {
        List<Product> products = productRepository.findAll(q);
        products.forEach(product -> product.setGross(calculateGross(product.getPrice())));
        return products;
    }

    public Product create(Product product) {
        Product saved = productRepository.save(product);
        saved.setGross(calculateGross(saved.getPrice()));
        log.info("created product {} '{}'", saved.getId(), saved.getName());
        return saved;
    }

    public List<String> lowStockProducts() {
        return productRepository.findLowStockProducts(shopProperties.lowStockThreshold());
    }

    private BigDecimal calculateGross(BigDecimal netPrice) {
        if (netPrice == null) {
            return BigDecimal.ZERO;
        }
        return netPrice.multiply(BigDecimal.ONE.add(shopProperties.taxRate()));
    }
}
