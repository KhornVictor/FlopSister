package edu.itc.se.shop.domain;

import jakarta.persistence.Embeddable;

@Embeddable
public record Address(
    String street,
    String city,
    String state,
    String zipCode,
    String country
) {
    public Address() {
        this(null, null, null, null, null);
    }
}
