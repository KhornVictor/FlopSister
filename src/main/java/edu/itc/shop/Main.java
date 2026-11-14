package edu.itc.shop;

import java.net.URI;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import edu.itc.shop.domain.InMemoryProductRepository;
import edu.itc.shop.domain.ProductRepository;

public class Main {

    public static final String BASE_URI =
            "http://localhost:8080/api/";

    public static ResourceConfig config() {

        ProductRepository repository =
                new InMemoryProductRepository();

        return new ResourceConfig()
                .packages("edu.itc.shop.api")
                .register(new AbstractBinder() {
                    @Override
                    protected void configure() {
                        bind(repository)
                                .to(ProductRepository.class);
                    }
                });
    }

    public static HttpServer startServer() {
        return GrizzlyHttpServerFactory.createHttpServer(
                URI.create(BASE_URI),
                config()
        );
    }

    public static void main(String[] args) throws Exception {

        HttpServer server = startServer();

        System.out.println(
                "Jersey server started at " + BASE_URI
        );

        Runtime.getRuntime().addShutdownHook(
                new Thread(server::shutdownNow)
        );

        Thread.currentThread().join();
    }
}
