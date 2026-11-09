package edu.itc.se.shop.service;

import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.itc.se.shop.domain.Category;
import edu.itc.se.shop.domain.Product;
import edu.itc.se.shop.repository.ProductRepository;
import edu.itc.se.shop.repository.ProductSpecs;
import edu.itc.se.shop.web.dto.ProductView;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Page<ProductView> searchProducts(String q, Category category, BigDecimal maxPrice, Boolean inStock, Pageable pageable) {
        Specification<Product> spec = ProductSpecs.filter(q, category, maxPrice, inStock);
        Page<Product> page = productRepository.findAll(spec, pageable);
        return page.map(p -> new ProductView(
                p.getId(),
                p.getSku(),
                p.getName(),
                p.getPrice().amount(),
                p.getStock()
        ));
    }
}
