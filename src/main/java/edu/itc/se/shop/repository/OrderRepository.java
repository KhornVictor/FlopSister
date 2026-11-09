package edu.itc.se.shop.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.itc.se.shop.domain.Order;
import edu.itc.se.shop.domain.OrderStatus;
import edu.itc.se.shop.web.dto.OrderSummary;

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

    List<Order> findByCustomerEmailAndStatusOrderByPlacedAtDesc(String email, OrderStatus status);

    long countByStatus(OrderStatus status);

    Optional<Order> findFirstByCustomerIdOrderByPlacedAtDesc(Long customerId);

    Page<OrderSummary> findByStatus(OrderStatus status, Pageable pageable);

    @Query("""
        select distinct o from Order o
          join fetch o.lines l
          join fetch l.product
         where o.customer.email = :email
         order by o.placedAt desc
        """)
    List<Order> findWithLinesByCustomerEmail(@Param("email") String email);

    @Query("select count(l) from OrderLine l where l.order.status = :status")
    long countLinesByStatus(@Param("status") OrderStatus status);

    // Slice pagination for feed
    Slice<OrderSummary> findSliceByOrderByPlacedAtDescIdDesc(Pageable pageable);

    // Keyset scrolling for feed
    Window<Order> findBy(ScrollPosition scrollPosition, Limit limit, Sort sort);
}
