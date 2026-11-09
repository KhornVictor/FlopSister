package edu.itc.se.shop.web.dto;

import java.util.List;

public record OrderFeedResponse(
    List<OrderSummary> items,
    String nextCursor,
    boolean hasNext
) {}
