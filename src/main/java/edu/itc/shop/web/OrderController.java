
package edu.itc.shop.web;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.itc.shop.domain.Order;
import edu.itc.shop.service.OrderService;
import edu.itc.shop.web.dto.PlaceOrder;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Place an order using the authenticated user's username.
     */
    @PostMapping
    public ResponseEntity<Order> place(
            @Valid @RequestBody PlaceOrder command,
            Authentication authentication) {

        Order order = orderService.place(
                authentication.getName(),
                command
        );

        return ResponseEntity
                .created(URI.create("/api/orders/" + order.getId()))
                .body(order);
    }

    /**
     * View the authenticated user's order history.
     * ADMIN may provide a customer username to view that customer's history.
     */
    @GetMapping
    public List<Order> history(
            @RequestParam(required = false) String customer,
            Authentication authentication) {

        String customerId = customer == null
                ? authentication.getName()
                : customer;

        return orderService.history(customerId);
    }

    /**
     * View one order. The service checks ownership or ADMIN role.
     */
    @GetMapping("/{id}")
    public Order get(@PathVariable long id) {
        return orderService.get(id);
    }

    /**
     * Ship an order. Only ADMIN is authorized by the service.
     */
    @PostMapping("/{id}/ship")
    public ResponseEntity<Void> ship(@PathVariable long id) {
        orderService.ship(id);
        return ResponseEntity.noContent().build();
    }
}
