package com.academia.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AcademyClassTest {
    private final User teacher = User.create("Teacher", "class.test.teacher", "hash", UserRole.TEACHER);
    private final LocalDate date = LocalDate.of(2026, 11, 15);

    @Test
    void rejectsInvalidTimeRange() {
        assertThrows(IllegalArgumentException.class, () -> AcademyClass.create(
                teacher, date, LocalTime.of(18, 0), LocalTime.of(18, 0), null));
        assertThrows(IllegalArgumentException.class, () -> AcademyClass.create(
                teacher, date, LocalTime.of(19, 0), LocalTime.of(18, 0), null));
    }

    @Test
    void cancelledClassCannotBeEditedOrCancelledAgain() {
        AcademyClass academyClass = AcademyClass.create(
                teacher, date, LocalTime.of(18, 0), LocalTime.of(19, 0), null);
        academyClass.cancel();

        assertEquals(ClassStatus.CANCELLED, academyClass.status());
        assertThrows(IllegalStateException.class, () -> academyClass.update(
                date, LocalTime.of(17, 0), LocalTime.of(18, 0), null));
        assertThrows(IllegalStateException.class, academyClass::cancel);
    }
}
