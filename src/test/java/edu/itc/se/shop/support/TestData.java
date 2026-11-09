
package edu.itc.se.shop.support;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    public TestData(
            CustomerRepository customers,
            ProductRepository products,
            OrderRepository orders) {
        this.customers = customers;
        this.products = products;
        this.orders = orders;
    }

    public Long newOrder() {
        Customer customer = customers.saveAndFlush(
                new Customer("customer-" + UUID.randomUUID() + "@example.test"));

        products.saveAndFlush(
                new Product("product-" + UUID.randomUUID(), 10));

        return orders.saveAndFlush(
                new Order(customer, Instant.now(), OrderStatus.PENDING)).getId();
    }

    public Long newProduct() {
        return products.saveAndFlush(
                new Product("product-" + UUID.randomUUID(), 10)).getId();
    }

    // Existing test data: products are shared between orders
    @Transactional
    public Long createPaidOrders(int orderCount, int linesPerOrder) {

        Customer customer = customers.saveAndFlush(
                new Customer("nplusone-" + UUID.randomUUID() + "@example.test"));

        List<Product> productList = new ArrayList<>();

        for (int i = 1; i <= linesPerOrder; i++) {
            productList.add(
                    new Product("NPlusOne Product " + UUID.randomUUID(), 100));
        }

        products.saveAllAndFlush(productList);

        List<Order> orderList = new ArrayList<>();

        for (int i = 1; i <= orderCount; i++) {

            Order order = new Order(
                    customer,
                    Instant.now(),
                    OrderStatus.PAID);

            for (int j = 0; j < linesPerOrder; j++) {
                order.addLine(productList.get(j), 1);
            }

            orderList.add(order);
        }

        orders.saveAllAndFlush(orderList);

        return customer.getId();
    }

    // New test data: create a unique product for every order line
    @Transactional
    public Long createPaidOrdersWithUniqueProducts(
            int orderCount, int linesPerOrder) {

        Customer customer = customers.saveAndFlush(
                new Customer(
                        "unique-products-" + UUID.randomUUID()
                                + "@example.test"));

        List<Product> productList = new ArrayList<>();

        // Create unique products
        for (int i = 1; i <= orderCount * linesPerOrder; i++) {
            productList.add(
                    new Product("Unique Product " + i, 100));
        }

        products.saveAllAndFlush(productList);

        List<Order> orderList = new ArrayList<>();

        int productIndex = 0;

        for (int i = 1; i <= orderCount; i++) {

            Order order = new Order(
                    customer,
                    Instant.now(),
                    OrderStatus.PAID);

            for (int j = 0; j < linesPerOrder; j++) {

                order.addLine(productList.get(productIndex), 1);

                productIndex++;
            }

            orderList.add(order);
        }

        orders.saveAllAndFlush(orderList);

        return customer.getId();
    }
}
