package com.academia.dto.request;

import com.academia.model.BillingMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateClassParticipationRequest(
        @NotNull UUID studentId,
        List<@NotNull UUID> subjectIds,
        @NotNull BillingMode billingMode,
        @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal unitPrice
) {
}
