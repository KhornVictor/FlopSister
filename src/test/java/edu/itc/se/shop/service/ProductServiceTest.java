package edu.itc.se.shop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import edu.itc.se.shop.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ProductServiceTest {

    @Autowired
    private ProductService service;

    @Autowired
    private ProductRepository repo;

    @Test
    void restockRollsBackWhenSourceHasTooLittleStock() {
        int before1 = repo.findById(1).orElseThrow().stock();
        int before2 = repo.findById(2).orElseThrow().stock();

        assertThrows(IllegalStateException.class, () -> service.restock(1, 2, 1_000));

        assertEquals(before1, repo.findById(1).orElseThrow().stock());
        assertEquals(before2, repo.findById(2).orElseThrow().stock()); // unchanged thanks to rollback
    }

    @Test
    void restockSuccessfullyTransfersStock() {
        int before1 = repo.findById(3).orElseThrow().stock();
        int before2 = repo.findById(4).orElseThrow().stock();

        service.restock(3, 4, 5);

        assertEquals(before1 - 5, repo.findById(3).orElseThrow().stock());
        assertEquals(before2 + 5, repo.findById(4).orElseThrow().stock());
    }
}
