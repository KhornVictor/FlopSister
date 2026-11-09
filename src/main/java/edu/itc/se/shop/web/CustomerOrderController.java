package edu.itc.se.shop.web;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.itc.se.shop.service.OrderService;
import edu.itc.se.shop.web.dto.OrderResponse;

@RestController
@RequestMapping("/api/customers")
public class CustomerOrderController {

    private final OrderService orderService;

    public CustomerOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/{email}/orders")
    public List<OrderResponse> getCustomerOrders(@PathVariable String email) {
        return orderService.getOrdersWithLinesByCustomerEmail(email);
    }
}
