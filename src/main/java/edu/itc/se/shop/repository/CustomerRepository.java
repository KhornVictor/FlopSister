package edu.itc.se.shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.itc.se.shop.domain.Customer;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
}
