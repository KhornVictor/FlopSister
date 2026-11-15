package edu.itc.shop.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record Money(BigDecimal amount, String currency) {

    private static final Pattern FORMAT =
            Pattern.compile("([0-9]+(?:\\.[0-9]+)?) ([A-Z]{3})");

    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
    }

    public static Money parse(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Cannot parse Money from 'null'");
        }

        Matcher matcher = FORMAT.matcher(value);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Cannot parse Money from '" + value + "'");
        }
        return new Money(new BigDecimal(matcher.group(1)), matcher.group(2));
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency;
    }
}
