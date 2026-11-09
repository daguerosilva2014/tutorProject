package com.academia.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClassPackageTest {
    private final Student student = Student.create("Student", null, null);

    @Test
    void tracksPartialAndFullPaymentWithoutUsingClasses() {
        ClassPackage classPackage = ClassPackage.create(student, 8, money("32000"), LocalDate.of(2026, 11, 9));
        assertEquals(PackageUsageStatus.PENDING, classPackage.usageStatus());
        assertEquals(PackagePaymentStatus.PENDING, classPackage.paymentStatus());
        assertEquals(8, classPackage.remainingClasses());

        classPackage.registerPayment(money("12000.25"));
        assertEquals(PackagePaymentStatus.PARTIAL, classPackage.paymentStatus());
        assertEquals(new BigDecimal("19999.75"), classPackage.outstandingAmount().amount());

        classPackage.registerPayment(money("19999.75"));
        assertEquals(PackagePaymentStatus.PAID, classPackage.paymentStatus());
        assertEquals(PackageUsageStatus.PENDING, classPackage.usageStatus());
        assertEquals(0, classPackage.outstandingAmount().amount().signum());
        assertThrows(IllegalStateException.class, () -> classPackage.registerPayment(money("0.01")));
    }

    @Test
    void freePackageIsAlreadyPaidAndNeedsPositiveClassCount() {
        ClassPackage free = ClassPackage.create(student, 1, money("0"), LocalDate.of(2026, 11, 9));
        assertEquals(PackagePaymentStatus.PAID, free.paymentStatus());
        assertThrows(IllegalStateException.class, () -> free.registerPayment(money("1")));
        assertThrows(IllegalArgumentException.class, () -> ClassPackage.create(
                student, 0, money("1"), LocalDate.of(2026, 11, 9)));
    }

    private Money money(String amount) {
        return new Money(new BigDecimal(amount));
    }
}
