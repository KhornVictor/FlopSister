package edu.itc.se.shop.repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.springframework.stereotype.Repository;

import edu.itc.se.shop.domain.Category;
import edu.itc.se.shop.domain.Money_;
import edu.itc.se.shop.domain.Product;
import edu.itc.se.shop.domain.Product_;
import edu.itc.se.shop.domain.Tag;
import edu.itc.se.shop.domain.Tag_;
import edu.itc.se.shop.web.dto.ProductReportRow;

@Repository
public class ProductReportRepositoryImpl implements ProductReportRepository {

    @PersistenceContext
    private EntityManager entityManager;

    private static final Set<String> SORTABLE_ATTRIBUTES = Set.of("amount", "name", "stock", "sku", "tagCount");

    @Override
    public List<ProductReportRow> findReport(
            String q,
            Category category,
            BigDecimal maxPrice,
            Integer minStock,
            String tag,
            String sort) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ProductReportRow> cq = cb.createQuery(ProductReportRow.class);
        Root<Product> product = cq.from(Product.class);

        List<Predicate> predicates = new ArrayList<>();

        if (q != null && !q.isBlank()) {
            predicates.add(cb.like(cb.lower(product.get(Product_.name)), "%" + q.trim().toLowerCase() + "%"));
        }

        if (category != null) {
            predicates.add(cb.equal(product.get(Product_.category), category));
        }

        if (maxPrice != null) {
            predicates.add(cb.lessThanOrEqualTo(product.get(Product_.price).get(Money_.amount), maxPrice));
        }

        if (minStock != null) {
            predicates.add(cb.greaterThanOrEqualTo(product.get(Product_.stock), minStock));
        }

        // Join to tags only when tag is given
        if (tag != null && !tag.isBlank()) {
            Join<Product, Tag> tagJoin = product.join(Product_.tags);
            predicates.add(cb.equal(tagJoin.get(Tag_.name), tag.trim()));
        }

        if (!predicates.isEmpty()) {
            cq.where(cb.and(predicates.toArray(new Predicate[0])));
        }

        Expression<Integer> tagCountExpr = cb.size(product.get(Product_.tags));

        cq.select(cb.construct(
                ProductReportRow.class,
                product.get(Product_.sku),
                product.get(Product_.name),
                product.get(Product_.price).get(Money_.amount),
                product.get(Product_.stock),
                tagCountExpr
        ));

        cq.groupBy(
                product.get(Product_.id),
                product.get(Product_.sku),
                product.get(Product_.name),
                product.get(Product_.price).get(Money_.amount),
                product.get(Product_.stock)
        );

        // Whitelist of sortable attributes
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            String attribute = parts[0].trim();
            boolean desc = parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim());

            if (SORTABLE_ATTRIBUTES.contains(attribute)) {
                Expression<?> sortExpr = switch (attribute) {
                    case "amount" -> product.get(Product_.price).get(Money_.amount);
                    case "name" -> product.get(Product_.name);
                    case "stock" -> product.get(Product_.stock);
                    case "sku" -> product.get(Product_.sku);
                    case "tagCount" -> tagCountExpr;
                    default -> product.get(Product_.price).get(Money_.amount);
                };
                cq.orderBy(desc ? cb.desc(sortExpr) : cb.asc(sortExpr));
            } else {
                cq.orderBy(cb.desc(product.get(Product_.price).get(Money_.amount)));
            }
        } else {
            cq.orderBy(cb.desc(product.get(Product_.price).get(Money_.amount)));
        }

        return entityManager.createQuery(cq).getResultList();
    }
}
