package edu.itc.se.shop.web.dto;

import java.time.Instant;
import edu.itc.se.shop.domain.OrderStatus;

public interface OrderSummary {
    Long getId();
    OrderStatus getStatus();
    Instant getPlacedAt();
    CustomerName getCustomer();

    interface CustomerName {
        String getName();
    }
}
