package edu.itc.se.shop;

import java.util.Arrays;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import edu.itc.se.shop.service.ReportBuilder;
import edu.itc.se.shop.service.WelcomeService;

@SpringBootApplication(scanBasePackages = {"edu.itc.se.shop", "edu.itc.shop"})
@ConfigurationPropertiesScan(basePackages = {"edu.itc.se.shop", "edu.itc.shop"})
public class ShopApplication {

	private static final Logger log = LoggerFactory.getLogger(ShopApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(ShopApplication.class, args);
	}

	@Bean
	CommandLineRunner demonstrateBeans(ApplicationContext context,
			WelcomeService welcomeService,
			ObjectProvider<ReportBuilder> reportBuilders) {
		return args -> {
			String[] notifierBeans = Arrays.stream(context.getBeanDefinitionNames())
					.filter(name -> name.toLowerCase(Locale.ROOT).contains("notifier"))
					.sorted()
					.toArray(String[]::new);
			log.info("{} beans; notifiers = {}", context.getBeanDefinitionCount(),
					Arrays.toString(notifierBeans));

			welcomeService.welcome("ann", false);
			welcomeService.welcome("bob", true);

			ReportBuilder first = reportBuilders.getObject();
			ReportBuilder second = reportBuilders.getObject();
			log.info("prototype instances differ: {} ({} != {})",
					first != second,
					System.identityHashCode(first),
					System.identityHashCode(second));
		};
	}

}
