package edu.itc.se.shop.repository;

import edu.itc.se.shop.domain.Product;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.stereotype.Repository;

@Repository
public class RawJdbcProductRepository {

    private final DataSource dataSource;

    public RawJdbcProductRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Product> findAll() {
        String sql = "SELECT id, name, price, stock FROM product ORDER BY id";
        List<Product> products = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                long id = rs.getLong("id");
                String name = rs.getString("name");
                BigDecimal price = rs.getBigDecimal("price");
                int stock = rs.getInt("stock");
                products.add(new Product(id, name, price, stock));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error during findAll", e);
        }
        return products;
    }
}
