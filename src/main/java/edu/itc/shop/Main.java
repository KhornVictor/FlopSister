package edu.itc.shop;

import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import edu.itc.shop.api.OrderResource;
import edu.itc.shop.domain.InMemoryOrderRepository;
import edu.itc.shop.domain.InMemoryProductRepository;
import edu.itc.shop.domain.InMemoryReviewRepository;
import edu.itc.shop.domain.OrderRepository;
import edu.itc.shop.domain.OrderService;
import edu.itc.shop.domain.ProductRepository;
import edu.itc.shop.domain.ReviewRepository;

/**
 * Embedded Grizzly server bootstrap and Jersey {@link ResourceConfig} definition.
 */
public class Main {

    public static final String BASE_URI = "http://localhost:8080/api/";

    /**
     * Builds a fresh application configuration with isolated singleton repositories and services.
     */
    public static ResourceConfig config() {
        ProductRepository repository = new InMemoryProductRepository();
        ReviewRepository reviewRepository = new InMemoryReviewRepository();
        OrderRepository orderRepository = new InMemoryOrderRepository();
        OrderService orderService = new OrderService(repository, orderRepository);
        ConcurrentMap<String, OrderResource.Stored> idempotency = new ConcurrentHashMap<>();

        return new ResourceConfig()
                .packages("edu.itc.shop.api")
                .register(new AbstractBinder() {
                    @Override
                    protected void configure() {
                        bind(repository).to(ProductRepository.class);
                        bind(reviewRepository).to(ReviewRepository.class);
                        bind(orderService).to(OrderService.class);
                        bind(idempotency).to(
                                new TypeLiteral<ConcurrentMap<String, OrderResource.Stored>>() {});
                    }
                });
    }

    public static HttpServer startServer() {
        return GrizzlyHttpServerFactory.createHttpServer(
                URI.create(BASE_URI),
                config());
    }

    public static void main(String[] args) throws Exception {
        HttpServer server = startServer();
        System.out.println("Jersey server started at " + BASE_URI);
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdownNow));
        Thread.currentThread().join();
    }
}
