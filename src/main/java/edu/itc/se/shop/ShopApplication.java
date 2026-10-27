package edu.itc.se.shop;

import edu.itc.se.shop.service.ReportBuilder;
import edu.itc.se.shop.service.WelcomeService;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class ShopApplication implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ShopApplication.class);

    private final ApplicationContext context;
    private final WelcomeService welcomeService;
    private final ObjectProvider<ReportBuilder> reportBuilderProvider;

    public ShopApplication(ApplicationContext context,
                           WelcomeService welcomeService,
                           ObjectProvider<ReportBuilder> reportBuilderProvider) {
        this.context = context;
        this.welcomeService = welcomeService;
        this.reportBuilderProvider = reportBuilderProvider;
    }

    public static void main(String[] args) {
        SpringApplication.run(ShopApplication.class, args);
    }

    @Override
    public void run(String... args) {
        String[] beanNames = context.getBeanDefinitionNames();
        List<String> notifiers = Arrays.stream(beanNames)
                .filter(name -> name.contains("Notifier"))
                .sorted()
                .toList();

        log.info("{} beans; notifiers = {}", beanNames.length, notifiers);

        welcomeService.welcome("ann", false);
        welcomeService.welcome("bob", true);

        ReportBuilder r1 = reportBuilderProvider.getObject();
        ReportBuilder r2 = reportBuilderProvider.getObject();
        log.info("prototype instances differ: {}", System.identityHashCode(r1) != System.identityHashCode(r2));
    }
}
