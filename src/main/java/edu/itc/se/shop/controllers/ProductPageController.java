package edu.itc.se.shop.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/products")
public class ProductPageController {
    @GetMapping
    public String getProductsPage() {
        return "products/list";
    }
}
