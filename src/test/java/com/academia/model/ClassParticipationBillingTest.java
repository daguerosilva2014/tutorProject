package com.academia.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClassParticipationBillingTest {
    @Test
    void justifiedAbsenceDoesNotCreateIndividualDebt() {
        User teacher = User.create("Teacher", "billing.teacher", "hash", UserRole.TEACHER);
        AcademyClass academyClass = AcademyClass.create(teacher, LocalDate.of(2026, 11, 10),
                LocalTime.of(10, 0), LocalTime.of(11, 0), null);
        ClassParticipation participation = ClassParticipation.create(academyClass,
                Student.create("Student", null, null), Set.of(), BillingMode.PER_CLASS,
                new Money(new BigDecimal("1200")));

        participation.recordAttendance(AttendanceStatus.JUSTIFIED_ABSENCE, Instant.parse("2026-11-09T12:00:00Z"));
        academyClass.finalizeClass();
        participation.closeWithoutConsumption(ConsumptionStatus.NOT_CONSUMED);

        assertEquals(0, BigDecimal.ZERO.compareTo(participation.outstandingAmount().amount()));
        assertThrows(IllegalStateException.class, () -> participation.registerPayment(new Money(BigDecimal.ONE)));
    }
}
