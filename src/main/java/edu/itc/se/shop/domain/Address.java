package edu.itc.se.shop.domain;

import jakarta.persistence.Embeddable;

@Embeddable
public record Address(
        String street,
        String city,
        String zip
) {
}
