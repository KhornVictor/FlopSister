package edu.itc.se.shop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"edu.itc.shop", "edu.itc.se.shop"})
@EntityScan(basePackages = {"edu.itc.shop.domain", "edu.itc.se.shop"})
@EnableJpaRepositories(basePackages = {"edu.itc.shop.repository", "edu.itc.se.shop.repository"})
public class ShopApplication {

	public static void main(String[] args) {
		SpringApplication.run(ShopApplication.class, args);
	}

}
