package com.academia.dto.response;

import com.academia.model.ClassParticipation;
import com.academia.model.BillingMode;
import com.academia.model.AttendanceStatus;
import com.academia.model.ConsumptionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record ClassParticipationResponse(UUID id, UUID classId, Student student, List<Subject> subjects,
                                         BillingMode billingMode, BigDecimal unitPrice,
                                         AttendanceStatus attendanceStatus, Instant absenceNotifiedAt,
                                         ConsumptionStatus consumptionStatus, UUID consumedPackageId,
                                         BigDecimal paidAmount, BigDecimal outstandingAmount, UUID recoveryTargetId) {
    public static ClassParticipationResponse from(ClassParticipation participation) {
        return new ClassParticipationResponse(
                participation.id(),
                participation.academyClass().id(),
                new Student(participation.student().id(), participation.student().name()),
                participation.subjects().stream()
                        .map(subject -> new Subject(subject.id(), subject.name()))
                        .sorted(Comparator.comparing(Subject::name).thenComparing(Subject::id))
                        .toList(),
                participation.billingMode(),
                participation.unitPrice() == null ? null : participation.unitPrice().amount(),
                participation.attendanceStatus(), participation.absenceNotifiedAt(),
                participation.consumptionStatus(),
                participation.consumedPackage() == null ? null : participation.consumedPackage().id(),
                participation.paidAmount().amount(), participation.outstandingAmount().amount(),
                participation.recoveryTarget() == null ? null : participation.recoveryTarget().id()
        );
    }

    public record Student(UUID id, String name) {
    }

    public record Subject(UUID id, String name) {
    }
}
