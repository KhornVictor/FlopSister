package edu.itc.se.shop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class WelcomeServiceTest {

	@Test
	void welcomeSendsTheExpectedTextToTheDefaultNotifier() {
		RecordingNotifier defaultNotifier = new RecordingNotifier();
		RecordingNotifier urgentNotifier = new RecordingNotifier();
		Clock clock = Clock.fixed(Instant.parse("2026-10-27T09:34:56Z"), ZoneOffset.UTC);
		WelcomeService service = new WelcomeService(defaultNotifier, urgentNotifier, clock);

		String text = service.welcome("ann", false);

		assertEquals("Welcome ann! It is 09:34:56", text);
		assertEquals("ann", defaultNotifier.to);
		assertEquals(text, defaultNotifier.text);
		assertNull(urgentNotifier.to);
	}

	@Test
	void urgentWelcomeUsesTheUrgentNotifier() {
		RecordingNotifier defaultNotifier = new RecordingNotifier();
		RecordingNotifier urgentNotifier = new RecordingNotifier();
		Clock clock = Clock.fixed(Instant.parse("2026-10-27T09:34:56Z"), ZoneOffset.UTC);
		WelcomeService service = new WelcomeService(defaultNotifier, urgentNotifier, clock);

		String text = service.welcome("bob", true);

		assertEquals("Welcome bob! It is 09:34:56", text);
		assertEquals("bob", urgentNotifier.to);
		assertEquals(text, urgentNotifier.text);
		assertNull(defaultNotifier.to);
	}

	private static final class RecordingNotifier implements Notifier {
		private String to;
		private String text;

		@Override
		public void send(String to, String text) {
			this.to = to;
			this.text = text;
		}
	}
}
