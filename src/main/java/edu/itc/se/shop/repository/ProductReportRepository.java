package edu.itc.se.shop.repository;

import java.math.BigDecimal;
import java.util.List;

import edu.itc.se.shop.domain.Category;
import edu.itc.se.shop.web.dto.ProductReportRow;

public interface ProductReportRepository {
    List<ProductReportRow> findReport(
        String q,
        Category category,
        BigDecimal maxPrice,
        Integer minStock,
        String tag,
        String sort
    );
}
