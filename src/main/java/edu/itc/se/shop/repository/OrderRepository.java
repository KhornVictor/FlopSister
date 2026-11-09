package edu.itc.se.shop.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.itc.se.shop.domain.Order;
import edu.itc.se.shop.domain.OrderStatus;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByStatusAndCustomer_Id(
            OrderStatus status, Long customerId);

    // 1. JOIN FETCH orders, lines, and products
    @Query("""
        select distinct o
        from Order o
        join fetch o.lines l
        join fetch l.product
        where o.status = :status
        and o.customer.id = :customerId
        """)
    List<Order> findByStatusWithLines(
            @Param("status") OrderStatus status,
            @Param("customerId") Long customerId);

    // 2. EntityGraph approach
    @EntityGraph(attributePaths = {"lines", "lines.product"})
    List<Order> findWithGraphByStatusAndCustomer_Id(
            OrderStatus status, Long customerId);

    // 3. Fetch orders and lines WITHOUT fetching products
    // Used to demonstrate the product-name N+1 problem
    @Query("""
        select distinct o
        from Order o
        join fetch o.lines
        where o.status = :status
        and o.customer.id = :customerId
        """)
    List<Order> findByStatusWithLinesOnly(
            @Param("status") OrderStatus status,
            @Param("customerId") Long customerId);
}