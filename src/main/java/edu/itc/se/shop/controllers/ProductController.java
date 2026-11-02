package edu.itc.se.shop.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;
import edu.itc.se.shop.service.ProductService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController 
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }
    @GetMapping
    public List<String> getProducts() {
        return productService.getProducts();
    }
}
