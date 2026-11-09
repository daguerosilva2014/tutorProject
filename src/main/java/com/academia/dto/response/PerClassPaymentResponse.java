package com.academia.dto.response;

import com.academia.model.PaymentMethod;
import com.academia.model.PerClassPayment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PerClassPaymentResponse(UUID id, UUID participationId, BigDecimal amount, LocalDate paidAt,
                                      PaymentMethod method, String notes, UUID registeredBy,
                                      BigDecimal outstandingAmount) {
    public static PerClassPaymentResponse from(PerClassPayment payment) {
        return new PerClassPaymentResponse(payment.id(), payment.participation().id(), payment.amount().amount(),
                payment.paidAt(), payment.method(), payment.notes(), payment.registeredBy().id(),
                payment.participation().outstandingAmount().amount());
    }
}
