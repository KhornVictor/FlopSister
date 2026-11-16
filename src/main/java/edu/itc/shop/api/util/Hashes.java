package edu.itc.shop.api.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.stream.Collectors;

import edu.itc.shop.api.dto.PlaceOrder;

/**
 * Payload hashing utilities for idempotent request verification.
 */
public final class Hashes {

    private Hashes() {
    }

    /**
     * Computes a deterministic SHA-256 hex digest of an order placement payload.
     */
    public static String sha256(PlaceOrder cmd) {
        String canonical = cmd.lines().stream()
                .map(line -> line.productId() + ":" + line.quantity())
                .collect(Collectors.joining(","));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
