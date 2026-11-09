package edu.itc.se.shop.web;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.itc.se.shop.domain.OrderStatus;
import edu.itc.se.shop.repository.OrderRepository;
import edu.itc.se.shop.service.OrderService;
import edu.itc.se.shop.web.dto.OrderFeedResponse;
import edu.itc.se.shop.web.dto.OrderSummary;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderRepository orders;
    private final OrderService orderService;

    public OrderController(OrderRepository orders, OrderService orderService) {
        this.orders = orders;
        this.orderService = orderService;
    }

    @GetMapping
    public Page<OrderSummary> list(
            @RequestParam OrderStatus status,
            @PageableDefault(size = 20, sort = "placedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return orders.findByStatus(status, pageable);
    }

    @GetMapping("/feed")
    public OrderFeedResponse feed(
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String after,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "keyset") String mode) {
        if ("slice".equalsIgnoreCase(mode)) {
            return orderService.feedWithSlice(size, page);
        }
        return orderService.feedWithKeyset(size, after);
    }
}
