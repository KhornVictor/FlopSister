package edu.itc.se.shop;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ShopApplicationTests {

	@Autowired
	private DataSource dataSource;

	@Test
	void contextLoads() {
	}

	@Test
	void testDatabaseConnection() throws SQLException {
		assertThat(dataSource).isNotNull();

		try (Connection connection = dataSource.getConnection()) {
			assertThat(connection.isValid(1)).isTrue();
			System.out.println("==========================================");
			System.out.println("Connected to: " + connection.getMetaData().getDatabaseProductName());
			System.out.println("Database URL: " + connection.getMetaData().getURL());
			System.out.println("User Name:    " + connection.getMetaData().getUserName());
			System.out.println("==========================================");
		}
	}
}
