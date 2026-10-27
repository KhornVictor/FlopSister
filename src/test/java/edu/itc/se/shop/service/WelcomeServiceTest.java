package edu.itc.se.shop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WelcomeServiceTest {

    private FakeNotifier defaultNotifier;
    private FakeNotifier urgentNotifier;
    private Clock fixedClock;
    private WelcomeService welcomeService;

    record Call(String to, String text) {}

    static class FakeNotifier implements Notifier {
        final List<Call> calls = new ArrayList<>();

        @Override
        public void send(String to, String text) {
            calls.add(new Call(to, text));
        }
    }

    @BeforeEach
    void setUp() {
        defaultNotifier = new FakeNotifier();
        urgentNotifier = new FakeNotifier();
        fixedClock = Clock.fixed(Instant.parse("2026-10-27T09:15:02Z"), ZoneId.of("UTC"));
        welcomeService = new WelcomeService(defaultNotifier, urgentNotifier, fixedClock);
    }

    @Test
    void welcomeNonUrgentUsesDefaultNotifier() {
        String message = welcomeService.welcome("ann", false);

        assertEquals("Welcome ann! It is 09:15:02", message);
        assertEquals(1, defaultNotifier.calls.size());
        assertEquals(new Call("ann", "Welcome ann! It is 09:15:02"), defaultNotifier.calls.get(0));
        assertTrue(urgentNotifier.calls.isEmpty());
    }

    @Test
    void welcomeUrgentUsesUrgentNotifier() {
        String message = welcomeService.welcome("bob", true);

        assertEquals("Welcome bob! It is 09:15:02", message);
        assertEquals(1, urgentNotifier.calls.size());
        assertEquals(new Call("bob", "Welcome bob! It is 09:15:02"), urgentNotifier.calls.get(0));
        assertTrue(defaultNotifier.calls.isEmpty());
    }
}
