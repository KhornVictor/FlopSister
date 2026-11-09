package edu.itc.se.shop.support;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.itc.se.shop.domain.Customer;
import edu.itc.se.shop.domain.Order;
import edu.itc.se.shop.domain.OrderStatus;
import edu.itc.se.shop.domain.Product;
import edu.itc.se.shop.repository.CustomerRepository;
import edu.itc.se.shop.repository.OrderRepository;
import edu.itc.se.shop.repository.ProductRepository;

@Component
public class TestData {
    private final CustomerRepository customers;
    private final ProductRepository products;
    private final OrderRepository orders;

    public TestData(CustomerRepository customers, ProductRepository products, OrderRepository orders) {
        this.customers = customers;
        this.products = products;
        this.orders = orders;
    }

    public Long newOrder() {
        Customer customer = customers.saveAndFlush(new Customer("customer-" + UUID.randomUUID() + "@example.test"));
        products.saveAndFlush(new Product("product-" + UUID.randomUUID(), 10));
        return orders.saveAndFlush(new Order(customer, Instant.now(), OrderStatus.PENDING)).getId();
    }

    public Long newProduct() {
        return products.saveAndFlush(new Product("product-" + UUID.randomUUID(), 10)).getId();
    }
}
