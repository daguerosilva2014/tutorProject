package com.academia.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MoneyTest {
    @Test
    void keepsExactArsAmounts() {
        Money price = new Money(new BigDecimal("32000.00"));
        Money paid = new Money(new BigDecimal("12000.25"));

        assertEquals(new BigDecimal("19999.75"), price.subtract(paid).amount());
        assertEquals(new BigDecimal("32000.00"), paid.add(price.subtract(paid)).amount());
        assertEquals(1, Integer.signum(price.compareTo(paid)));
        assertEquals(new BigDecimal("32000.00"), price.amount());
    }

    @Test
    void rejectsNegativeAmountsAndExtraDecimals() {
        assertThrows(IllegalArgumentException.class, () -> new Money(new BigDecimal("-0.01")));
        assertThrows(ArithmeticException.class, () -> new Money(new BigDecimal("1.001")));
    }
}
