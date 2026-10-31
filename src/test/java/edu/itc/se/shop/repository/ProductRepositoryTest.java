package edu.itc.se.shop.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.itc.se.shop.domain.Product;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;

@JdbcTest
@Import(ProductRepository.class)
class ProductRepositoryTest {

    @Autowired
    private ProductRepository repo;

    @Test
    void findAllReturnsInitialProducts() {
        List<Product> products = repo.findAll();
        assertEquals(5, products.size());
    }

    @Test
    void findByIdReturnsProduct() {
        Optional<Product> product = repo.findById(1);
        assertTrue(product.isPresent());
        assertEquals("Laptop", product.get().name());

        Optional<Product> notFound = repo.findById(999);
        assertFalse(notFound.isPresent());
    }

    @Test
    void findByNameContainingReturnsMatches() {
        List<Product> matches = repo.findByNameContaining("Key");
        assertEquals(1, matches.size());
        assertEquals("Keyboard", matches.get(0).name());
    }

    @Test
    void insertReturnsGeneratedId() {
        Product p = new Product(null, "Webcam", new BigDecimal("59.90"), 25);
        long id = repo.insert(p);
        assertTrue(id > 0);

        Optional<Product> loaded = repo.findById(id);
        assertTrue(loaded.isPresent());
        assertEquals("Webcam", loaded.get().name());
    }

    @Test
    void updateStockModifiesStock() {
        int affected = repo.updateStock(1, 5);
        assertEquals(1, affected);

        Product updated = repo.findById(1).orElseThrow();
        assertEquals(15, updated.stock());
    }

    @Test
    void deleteRemovesProduct() {
        int affected = repo.delete(5);
        assertEquals(1, affected);

        Optional<Product> deleted = repo.findById(5);
        assertFalse(deleted.isPresent());
    }

    @Test
    void insertDuplicateNameThrowsDuplicateKeyException() {
        Product duplicate = new Product(null, "Laptop", new BigDecimal("1200.00"), 5);
        assertThrows(DuplicateKeyException.class, () -> repo.insert(duplicate));
    }
}
