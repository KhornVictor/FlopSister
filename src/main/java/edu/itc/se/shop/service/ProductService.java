package edu.itc.se.shop.service;

import edu.itc.se.shop.repository.ProductRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public void restock(long fromId, long toId, int qty) {
        try {
            int updatedFrom = productRepository.updateStock(fromId, -qty);
            if (updatedFrom == 0) {
                throw new IllegalStateException("Insufficient stock or source product not found: " + fromId);
            }
            int updatedTo = productRepository.updateStock(toId, qty);
            if (updatedTo == 0) {
                throw new IllegalStateException("Target product not found: " + toId);
            }
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("CHECK constraint failed during restock", e);
        }
    }
}
