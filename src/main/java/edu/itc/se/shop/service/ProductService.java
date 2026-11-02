package edu.itc.se.shop.service;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;

@Service //@Component //Repository //ConfigurationProperties
public class ProductService {
    private List<String> lst = new ArrayList<>(List.of("Product 1", "Product 2", "Product 3"));
    public List<String> getProducts() {
        return lst;
    }
    public void addProduct(String product) {
        lst.add(product);
    }
}
