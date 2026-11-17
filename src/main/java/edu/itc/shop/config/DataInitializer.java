package edu.itc.shop.config;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import edu.itc.shop.domain.Product;
import edu.itc.shop.domain.Role;
import edu.itc.shop.repository.ProductRepository;
import edu.itc.shop.service.AccountService;

@Configuration(proxyBeanMethods = false)
public class DataInitializer {

    @Bean
    CommandLineRunner seedData(
            ObjectProvider<ProductRepository> productsProvider,
            AccountService accountService,
            @Value("${shop.seed.admin-password}") String adminPassword,
            @Value("${shop.seed.customer-password}") String customerPassword) {

        return args -> {
            productsProvider.ifAvailable(products -> {
                if (products.count() == 0) {
                    products.saveAll(List.of(
                            new Product("USB-C cable", new BigDecimal("7.90"), 25),
                            new Product("Wireless mouse", new BigDecimal("24.50"), 15),
                            new Product("Mechanical keyboard", new BigDecimal("89.00"), 10)
                    ));
                }
            });

            if (!accountService.existsByUsername("admin")) {
                accountService.register(
                        "admin",
                        adminPassword,
                        Set.of(Role.ADMIN, Role.CUSTOMER)
                );
            }

            if (!accountService.existsByUsername("alice")) {
                accountService.register(
                        "alice",
                        customerPassword,
                        Set.of(Role.CUSTOMER)
                );
            }
        };
    }
}
