package edu.itc.shop.repository;

import edu.itc.shop.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for managing {@link Product} entities.
 */
public interface ProductRepository extends JpaRepository<Product, Long> {
}
