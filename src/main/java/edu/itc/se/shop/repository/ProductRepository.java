package edu.itc.se.shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.itc.se.shop.domain.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
