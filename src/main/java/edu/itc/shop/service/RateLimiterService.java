package edu.itc.shop.service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class RateLimiterService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_SECONDS = 60;

    private final ConcurrentHashMap<String, Deque<Instant>> attempts = new ConcurrentHashMap<>();

    /**
     * Checks whether the request is rate-limited.
     * Returns 0 if allowed, or retry-after seconds if rate-limited.
     */
    public synchronized long tryConsume(String ip, String username) {
        Instant now = Instant.now();
        String key = (ip != null ? ip : "unknown") + ":" + (username != null ? username : "");
        Deque<Instant> timestamps = attempts.computeIfAbsent(key, k -> new ArrayDeque<>());
        Instant cutoff = now.minusSeconds(WINDOW_SECONDS);
        while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(cutoff)) {
            timestamps.pollFirst();
        }

        if (timestamps.size() >= MAX_ATTEMPTS) {
            Instant oldest = timestamps.peekFirst();
            long elapsed = now.getEpochSecond() - oldest.getEpochSecond();
            return Math.max(1, WINDOW_SECONDS - elapsed);
        }

        timestamps.addLast(now);
        return 0;
    }

    public synchronized void resetCounter(String ip, String username) {
        String key = (ip != null ? ip : "unknown") + ":" + (username != null ? username : "");
        attempts.remove(key);
    }

    public void reset() {
        attempts.clear();
    }
}
