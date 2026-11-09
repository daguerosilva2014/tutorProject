package com.academia.dto.response;

import com.academia.model.PackagePaymentStatus;
import com.academia.model.Payment;
import com.academia.model.PaymentMethod;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID packageId,
        BigDecimal amount,
        LocalDate paidAt,
        PaymentMethod method,
        String notes,
        UUID registeredBy,
        PackagePaymentStatus paymentStatus,
        BigDecimal outstandingAmount
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(payment.id(), payment.classPackage().id(), payment.amount().amount(), payment.paidAt(),
                payment.method(), payment.notes(), payment.registeredBy().id(),
                payment.classPackage().paymentStatus(), payment.classPackage().outstandingAmount().amount());
    }
}
