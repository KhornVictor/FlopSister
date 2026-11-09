package edu.itc.se.shop.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

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
import edu.itc.se.shop.service.OrderService;
import edu.itc.se.shop.web.dto.OrderFeedResponse;
import edu.itc.se.shop.web.dto.ProductReportRow;

@SpringBootTest
@ActiveProfiles("test")
class ProductReportAndFeedEndpointTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductReportController reportController;

    @Autowired
    private OrderController orderController;

    private Product p1;
    private Product p2;
    private Tag saleTag;
    private Tag premiumTag;

    @BeforeEach
    @Transactional
    void setupData() {
        saleTag = tagRepository.findByName("sale").orElseGet(() -> tagRepository.save(new Tag("sale")));
        premiumTag = tagRepository.findByName("premium").orElseGet(() -> tagRepository.save(new Tag("premium")));

        if (!productRepository.existsBySku("KB-7")) {
            p1 = new Product("KB-7", "Mechanical keyboard", Money.eur("129.00"), Category.HARDWARE, 4);
            p1.tag(saleTag);
            p1.tag(premiumTag);
            p1 = productRepository.save(p1);
        } else {
            p1 = productRepository.findByTagsName("sale").get(0);
        }

        if (!productRepository.existsBySku("BK-1")) {
            p2 = new Product("BK-1", "Clean Architecture", Money.eur("35.00"), Category.BOOKS, 10);
            p2.tag(saleTag);
            p2 = productRepository.save(p2);
        }

        Customer c = customerRepository.findByEmail("grace@example.com")
                .orElseGet(() -> customerRepository.save(new Customer("Grace Hopper", "grace@example.com")));

        if (orderRepository.count() < 25) {
            for (int i = 1; i <= 25; i++) {
                Order o = new Order(c, Instant.now().minusSeconds(3600 - i * 60), OrderStatus.PAID);
                orderRepository.save(o);
            }
        }
    }

    @Test
    void testProductReportCriteriaWithFiltersAndMetamodel() {
        List<ProductReportRow> rows = reportController.generateReport(
                null,
                Category.HARDWARE,
                null,
                1,
                "sale",
                "amount,desc"
        );

        assertThat(rows).isNotEmpty();
        ProductReportRow first = rows.get(0);
        assertThat(first.sku()).isEqualTo("KB-7");
        assertThat(first.name()).isEqualTo("Mechanical keyboard");
        assertThat(first.amount()).isEqualByComparingTo(new BigDecimal("129.00"));
        assertThat(first.stock()).isEqualTo(4);
        assertThat(first.tagCount()).isEqualTo(2);
    }

    @Test
    void testOrderFeedWithKeysetScrolling() {
        OrderFeedResponse firstPage = orderController.feed(20, null, 0, "keyset");

        assertThat(firstPage.items()).hasSize(20);
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(firstPage.nextCursor()).isNotNull();

        // Second page using nextCursor
        OrderFeedResponse secondPage = orderController.feed(20, firstPage.nextCursor(), 0, "keyset");
        assertThat(secondPage.items()).isNotEmpty();
        assertThat(secondPage.items().size()).isLessThanOrEqualTo(20);
    }

    @Test
    void testOrderFeedWithSlice() {
        OrderFeedResponse firstSlice = orderController.feed(20, null, 0, "slice");

        assertThat(firstSlice.items()).hasSize(20);
        assertThat(firstSlice.hasNext()).isTrue();
        assertThat(firstSlice.nextCursor()).isNotNull();

        OrderFeedResponse secondSlice = orderController.feed(20, null, 1, "slice");
        assertThat(secondSlice.items()).isNotEmpty();
    }
}
