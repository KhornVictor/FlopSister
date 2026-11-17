package edu.itc.shop.service;

import edu.itc.shop.domain.Product;
import edu.itc.shop.repository.ProductRepository;
import edu.itc.shop.web.dto.CreateProductRequest;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business operations for the product catalog.
 */
@Service
public class ProductService {

    private final ProductRepository products;

    public ProductService(ProductRepository products) {
        this.products = products;
    }

    public List<Product> findAll() {
        return products.findAll();
    }

    public Product findById(long id) {
        return products.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Transactional
    public Product create(CreateProductRequest request) {
        return products.save(new Product(request.name(), request.price(), request.stock()));
    }

    @Transactional
    public void delete(long id) {
        if (!products.existsById(id)) {
            throw new ProductNotFoundException(id);
        }
        products.deleteById(id);
    }
}
