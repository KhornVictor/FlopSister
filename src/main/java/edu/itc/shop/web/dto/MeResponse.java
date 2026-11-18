package edu.itc.shop.web.dto;

import java.util.List;

/**
 * Authenticated caller summary returned by {@code GET /api/me}.
 */
public record MeResponse(String name, List<String> roles) {
}
