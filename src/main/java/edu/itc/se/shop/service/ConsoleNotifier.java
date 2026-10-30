package edu.itc.se.shop.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Service
@Primary
@Profile("dev")
public class ConsoleNotifier implements Notifier {

	private static final Logger log = LoggerFactory.getLogger(ConsoleNotifier.class);

	@PostConstruct
	void ready() {
		log.info("ConsoleNotifier ready");
	}

	@Override
	public void send(String to, String text) {
		log.info("CONSOLE to={} text={}", to, text);
	}

	@PreDestroy
	void closing() {
		log.info("ConsoleNotifier closing");
	}
}
