package com.academia.dto.response;

import com.academia.model.ClassPackage;
import com.academia.model.PackagePaymentStatus;
import com.academia.model.PackageUsageStatus;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.UUID;

public record ClassPackageResponse(
        UUID id,
        UUID studentId,
        int totalClasses,
        int usedClasses,
        int remainingClasses,
        BigDecimal price,
        BigDecimal paidAmount,
        BigDecimal outstandingAmount,
        PackageUsageStatus usageStatus,
        PackagePaymentStatus paymentStatus,
        LocalDate purchasedAt
) {
    public static ClassPackageResponse from(ClassPackage classPackage) {
        return new ClassPackageResponse(classPackage.id(), classPackage.student().id(),
                classPackage.totalClasses(), classPackage.usedClasses(), classPackage.remainingClasses(),
                classPackage.price().amount(), classPackage.paidAmount().amount(), classPackage.outstandingAmount().amount(),
                classPackage.usageStatus(), classPackage.paymentStatus(), classPackage.purchasedAt());
    }
}
