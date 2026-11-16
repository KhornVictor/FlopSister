package edu.itc.shop.api.params;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import edu.itc.shop.domain.Money;
import edu.itc.shop.domain.Product;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;

public class ProductFilter {

    @Parameter(description = "Case-insensitive substring search against product name")
    @QueryParam("q")
    public String q;

    @Parameter(description = "Minimum inclusive price formatted as '<amount> EUR'")
    @QueryParam("minPrice")
    public Money minPrice;

    @Parameter(description = "Maximum inclusive price formatted as '<amount> EUR'")
    @QueryParam("maxPrice")
    public Money maxPrice;

    @Parameter(description = "Filter to products with positive stock")
    @QueryParam("inStock")
    @DefaultValue("false")
    public boolean inStock;

    @Parameter(description = "Sort order: name|price|stock followed by ,asc or ,desc")
    @QueryParam("sort")
    @DefaultValue("name,asc")
    @Pattern(
            regexp = "(name|price|stock),(asc|desc)",
            flags = Pattern.Flag.CASE_INSENSITIVE,
            message = "use name,asc|desc, price,asc|desc, or stock,asc|desc")
    public String sort = "name,asc";

    @Parameter(description = "Zero-based page index")
    @QueryParam("page")
    @DefaultValue("0")
    @Min(0)
    public int page;

    @Parameter(description = "Page size between 1 and 100")
    @QueryParam("size")
    @DefaultValue("20")
    @Min(1)
    @Max(100)
    public int size = 20;

    @AssertTrue(message = "minPrice and maxPrice currency must be EUR")
    public boolean isCurrencySupported() {
        return (minPrice == null || "EUR".equals(minPrice.currency()))
                && (maxPrice == null || "EUR".equals(maxPrice.currency()));
    }

    public List<Product> filter(List<Product> products) {
        var stream = products.stream();

        if (q != null && !q.isBlank()) {
            String search = q.toLowerCase(Locale.ROOT);
            stream = stream.filter(product ->
                    product.name().toLowerCase(Locale.ROOT).contains(search));
        }
        if (minPrice != null) {
            stream = stream.filter(product ->
                    product.price().compareTo(minPrice.amount()) >= 0);
        }
        if (maxPrice != null) {
            stream = stream.filter(product ->
                    product.price().compareTo(maxPrice.amount()) <= 0);
        }
        if (inStock) {
            stream = stream.filter(product -> product.stock() > 0);
        }

        return stream.sorted(comparator()).toList();
    }

    private Comparator<Product> comparator() {
        String field = sort.split(",", 2)[0].toLowerCase(Locale.ROOT);
        Comparator<Product> comparator = switch (field) {
            case "name" -> Comparator.comparing(
                    Product::name, String.CASE_INSENSITIVE_ORDER);
            case "price" -> Comparator.comparing(Product::price);
            case "stock" -> Comparator.comparingInt(Product::stock);
            default -> throw new IllegalStateException("Validated sort field expected");
        };
        return sort.toLowerCase(Locale.ROOT).endsWith(",desc")
                ? comparator.reversed()
                : comparator;
    }
}
