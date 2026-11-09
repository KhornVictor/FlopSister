package edu.itc.se.shop.web;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.itc.se.shop.domain.Category;
import edu.itc.se.shop.repository.ProductRepository;
import edu.itc.se.shop.web.dto.ProductReportRow;

@RestController
@RequestMapping("/api/reports/products")
public class ProductReportController {

    private final ProductRepository productRepository;

    public ProductReportController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping
    public List<ProductReportRow> generateReport(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer minStock,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String sort) {

        return productRepository.findReport(q, category, maxPrice, minStock, tag, sort);
    }
}
