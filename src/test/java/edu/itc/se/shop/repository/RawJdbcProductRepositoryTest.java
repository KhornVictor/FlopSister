package edu.itc.se.shop.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.itc.se.shop.domain.Product;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;

@JdbcTest
@Import(RawJdbcProductRepository.class)
class RawJdbcProductRepositoryTest {

    @Autowired
    private RawJdbcProductRepository repo;

    @Test
    void findAllReturnsInitialProducts() {
        List<Product> products = repo.findAll();
        assertEquals(5, products.size());
    }
}
