package edu.itc.se.shop.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import edu.itc.se.shop.domain.Address;
import edu.itc.se.shop.domain.Category;
import edu.itc.se.shop.domain.Customer;
import edu.itc.se.shop.domain.Money;
import edu.itc.se.shop.domain.Order;
import edu.itc.se.shop.domain.OrderStatus;
import edu.itc.se.shop.domain.Product;
import edu.itc.se.shop.domain.Tag;
import edu.itc.se.shop.repository.CustomerRepository;
import edu.itc.se.shop.repository.OrderRepository;
import edu.itc.se.shop.repository.ProductRepository;
import edu.itc.se.shop.repository.TagRepository;

@Component
@Profile("dev")
public class DataSeeder implements ApplicationRunner {

    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final TagRepository tagRepository;

    public DataSeeder(CustomerRepository customerRepository,
                      ProductRepository productRepository,
                      OrderRepository orderRepository,
                      TagRepository tagRepository) {
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.tagRepository = tagRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (customerRepository.count() > 0) {
            return;
        }

        // 1. Insert 3 customers
        Customer ada = new Customer(
                "Ada Lovelace",
                "ada@example.com",
                new Address("123 Ada St", "London", "10001"),
                new Address("123 Ada St", "London", "10001")
        );
        Customer grace = new Customer(
                "Grace Hopper",
                "grace@example.com",
                new Address("456 Hopper Ave", "New York", "10002"),
                new Address("456 Hopper Ave", "New York", "10002")
        );
        Customer margaret = new Customer(
                "Margaret Hamilton",
                "margaret@example.com",
                new Address("789 Apollo Rd", "Cambridge", "10003"),
                new Address("789 Apollo Rd", "Cambridge", "10003")
        );

        customerRepository.saveAll(List.of(ada, grace, margaret));

        // 2. Insert tags
        Tag tagHardware = tagRepository.save(new Tag("Hardware"));
        Tag tagPeripherals = tagRepository.save(new Tag("Peripherals"));
        Tag tagBooks = tagRepository.save(new Tag("Books"));
        Tag tagElectronics = tagRepository.save(new Tag("Electronics"));
        Tag tagAccessories = tagRepository.save(new Tag("Accessories"));

        // 3. Insert 12 products
        List<Product> products = new ArrayList<>();

        Product p1 = new Product("KB-1", "Mechanical Keyboard", Money.eur("49.99"), Category.HARDWARE);
        p1.setStock(15);
        p1.tag(tagHardware);
        p1.tag(tagPeripherals);
        products.add(p1);

        Product p2 = new Product("KB-7", "Wireless Numeric Keypad", Money.eur("35.00"), Category.HARDWARE);
        p2.setStock(20);
        p2.tag(tagHardware);
        p2.tag(tagPeripherals);
        products.add(p2);

        Product p3 = new Product("KB-EXP", "Keychron Custom Keyboard", Money.eur("120.00"), Category.HARDWARE);
        p3.setStock(5);
        p3.tag(tagHardware);
        p3.tag(tagPeripherals);
        products.add(p3);

        Product p4 = new Product("KB-OUT", "Vintage Keycaps Set", Money.eur("25.00"), Category.HARDWARE);
        p4.setStock(0);
        p4.tag(tagHardware);
        p4.tag(tagAccessories);
        products.add(p4);

        Product p5 = new Product("BK-1", "Domain-Driven Design", Money.eur("45.00"), Category.BOOKS);
        p5.setStock(10);
        p5.tag(tagBooks);
        products.add(p5);

        Product p6 = new Product("BK-2", "Clean Architecture", Money.eur("35.00"), Category.BOOKS);
        p6.setStock(8);
        p6.tag(tagBooks);
        products.add(p6);

        Product p7 = new Product("BK-3", "Designing Data-Intensive Applications", Money.eur("55.00"), Category.BOOKS);
        p7.setStock(12);
        p7.tag(tagBooks);
        products.add(p7);

        Product p8 = new Product("EL-1", "USB-C Multiport Adapter", Money.eur("29.99"), Category.ELECTRONICS);
        p8.setStock(25);
        p8.tag(tagElectronics);
        p8.tag(tagAccessories);
        products.add(p8);

        Product p9 = new Product("EL-2", "Noise Cancelling Headphones", Money.eur("199.99"), Category.ELECTRONICS);
        p9.setStock(7);
        p9.tag(tagElectronics);
        products.add(p9);

        Product p10 = new Product("EL-3", "4K Ultra-HD Monitor 27-inch", Money.eur("299.99"), Category.ELECTRONICS);
        p10.setStock(3);
        p10.tag(tagElectronics);
        products.add(p10);

        Product p11 = new Product("TY-1", "Lego Apollo Saturn V", Money.eur("89.99"), Category.TOYS);
        p11.setStock(4);
        p11.tag(tagAccessories);
        products.add(p11);

        Product p12 = new Product("FD-1", "Artisanal Coffee Beans 1kg", Money.eur("18.50"), Category.FOOD);
        p12.setStock(50);
        p12.tag(tagAccessories);
        products.add(p12);

        productRepository.saveAll(products);

        // 4. Insert 30 orders (2-4 lines each, mixed statuses)
        // 11 PAID, 8 SHIPPED, 7 NEW, 4 CANCELLED
        OrderStatus[] statuses = new OrderStatus[] {
            OrderStatus.PAID, OrderStatus.PAID, OrderStatus.PAID, OrderStatus.PAID,
            OrderStatus.PAID, OrderStatus.PAID, OrderStatus.PAID, OrderStatus.PAID,
            OrderStatus.PAID, OrderStatus.PAID, OrderStatus.PAID,
            OrderStatus.SHIPPED, OrderStatus.SHIPPED, OrderStatus.SHIPPED, OrderStatus.SHIPPED,
            OrderStatus.SHIPPED, OrderStatus.SHIPPED, OrderStatus.SHIPPED, OrderStatus.SHIPPED,
            OrderStatus.NEW, OrderStatus.NEW, OrderStatus.NEW, OrderStatus.NEW,
            OrderStatus.NEW, OrderStatus.NEW, OrderStatus.NEW,
            OrderStatus.CANCELLED, OrderStatus.CANCELLED, OrderStatus.CANCELLED, OrderStatus.CANCELLED
        };

        Customer[] customers = new Customer[] { ada, grace, margaret };

        Instant baseTime = Instant.parse("2026-09-24T09:12:44.120Z");
        Instant secondTime = Instant.parse("2026-09-23T17:03:10.551Z");

        for (int i = 0; i < 30; i++) {
            Customer cust = customers[i % 3];
            OrderStatus status = statuses[i];

            Order order = new Order(cust, status);
            if (i == 0) {
                order.setPlacedAt(baseTime);
            } else if (i == 1) {
                order.setPlacedAt(secondTime);
            } else {
                order.setPlacedAt(secondTime.minus(Duration.ofHours((long) (i - 1) * 3)));
            }

            // 2 to 4 lines each
            int lineCount = 2 + (i % 3); // 2, 3, or 4 lines
            for (int j = 0; j < lineCount; j++) {
                Product prod = products.get((i * 2 + j) % products.size());
                int qty = 1 + (j % 3);
                order.addLine(prod, qty);
            }

            orderRepository.save(order);
        }
    }
}
