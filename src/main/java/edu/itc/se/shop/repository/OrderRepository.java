package edu.itc.se.shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.itc.se.shop.domain.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
