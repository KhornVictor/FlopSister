package edu.itc.se.shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.itc.se.shop.domain.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.itc.se.shop.domain.Category;
import edu.itc.se.shop.domain.Product;
import edu.itc.se.shop.web.dto.ProductView;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    boolean existsBySku(String sku);

    List<Product> findTop3ByCategoryOrderByPriceAmountDesc(Category category);

    List<Product> findByTagsName(String name);

    @Query(value = "SELECT p.* FROM product p WHERE p.stock < :min ORDER BY p.stock", nativeQuery = true)
    List<Product> findLowStock(@Param("min") int min);

    Page<ProductView> findByCategory(Category category, Pageable pageable);
}
