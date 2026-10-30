package edu.itc.se.shop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = {"edu.itc.se.shop", "edu.itc.shop"})
@ConfigurationPropertiesScan(basePackages = {"edu.itc.se.shop", "edu.itc.shop"})
public class ShopApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopApplication.class, args);
    }
}
