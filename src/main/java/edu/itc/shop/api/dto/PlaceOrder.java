package edu.itc.shop.api.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Validated request payload for placing a new order.
 */
public record PlaceOrder(@NotEmpty List<@NotNull @Valid LineDto> lines) {
}
