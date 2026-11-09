package com.academia.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Money(BigDecimal amount) {
    public Money {
        Objects.requireNonNull(amount, "amount");
        amount = amount.setScale(2, RoundingMode.UNNECESSARY);
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("amount must not be negative");
        }
    }

    public Money add(Money other) {
        return new Money(amount.add(Objects.requireNonNull(other).amount()));
    }

    public Money subtract(Money other) {
        return new Money(amount.subtract(Objects.requireNonNull(other).amount()));
    }

    public int compareTo(Money other) {
        return amount.compareTo(Objects.requireNonNull(other).amount());
    }
}
