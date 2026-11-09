package edu.itc.se.shop.service;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.itc.se.shop.domain.Product;
import edu.itc.se.shop.repository.ProductRepository;

@Service
public class StockService {

    private final ProductRepository productRepository;

    public StockService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // 1. Pessimistic stock reservation
    @Transactional
    public void reserve(Long productId, int qty) {
        Product product = productRepository.findForUpdate(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        if (product.getStock() < qty) {
            throw new OutOfStockException("Out of stock: available=" + product.getStock() + ", requested=" + qty);
        }

        product.setStock(product.getStock() - qty);
    }

    // 2. Optimistic stock reservation
    @Transactional
    public void reserveOptimistic(Long productId, int qty) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        if (product.getStock() < qty) {
            throw new OutOfStockException("Out of stock: available=" + product.getStock() + ", requested=" + qty);
        }

        product.setStock(product.getStock() - qty);
    }

    // Optimistic reservation with retry
    public int reserveOptimisticWithRetry(Long productId, int qty, int maxRetries) {
        int retries = 0;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                reserveOptimistic(productId, qty);
                return retries;
            } catch (ObjectOptimisticLockingFailureException e) {
                retries++;
                if (attempt == maxRetries - 1) {
                    throw e;
                }
                try {
                    Thread.sleep(5);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(ie);
                }
            }
        }
        return retries;
    }

    // 3. Atomic single-statement update
    @Transactional
    public void reserveAtomic(Long productId, int qty) {
        int updated = productRepository.decrementStockAtomic(productId, qty);
        if (updated == 0) {
            throw new OutOfStockException("Out of stock for product " + productId);
        }
    }
}
