// Meylorng's part: Product repository for database access and product queries.
package edu.itc.shop.repository;

import java.util.List;
import java.util.Map;

import edu.itc.shop.model.Product;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository("productNamedParameterJdbcRepository")
public class ProductRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final RowMapper<Product> ROW_MAPPER = (rs, rowNum) -> {
        Product product = new Product();
        product.setId(rs.getLong("id"));
        product.setName(rs.getString("name"));
        product.setPrice(rs.getBigDecimal("price"));
        product.setStock(rs.getInt("stock"));
        product.setGross(rs.getBigDecimal("price"));
        return product;
    };

    public ProductRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Product> findAll(String q) {
        String sql = "SELECT id, name, price, stock FROM products";
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (StringUtils.hasText(q)) {
            sql += " WHERE LOWER(name) LIKE LOWER(:term)";
            params.addValue("term", "%" + q.trim() + "%");
        }

        sql += " ORDER BY id";
        return jdbcTemplate.query(sql, params, ROW_MAPPER);
    }

    public List<String> findLowStockProducts(int threshold) {
        String sql = "SELECT name FROM products WHERE stock < :threshold ORDER BY name";
        return jdbcTemplate.queryForList(sql, Map.of("threshold", threshold), String.class);
    }

    public Product save(Product product) {
        String sql = "INSERT INTO products (name, price, stock) VALUES (:name, :price, :stock)";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("name", product.getName())
                .addValue("price", product.getPrice())
                .addValue("stock", product.getStock());

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, params, keyHolder, new String[] {"id"});

        Number id = keyHolder.getKey();
        if (id != null) {
            product.setId(id.longValue());
        }
        return product;
    }
}
