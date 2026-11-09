package edu.itc.se.shop.web.dto;

import java.math.BigDecimal;

public record ProductReportRow(
    String sku,
    String name,
    BigDecimal amount,
    int stock,
    long tagCount
) {
    public ProductReportRow(String sku, String name, BigDecimal amount, int stock, Long tagCount) {
        this(sku, name, amount, stock, tagCount != null ? tagCount : 0L);
    }

    public ProductReportRow(String sku, String name, BigDecimal amount, int stock, Integer tagCount) {
        this(sku, name, amount, stock, tagCount != null ? tagCount.longValue() : 0L);
    }
}
