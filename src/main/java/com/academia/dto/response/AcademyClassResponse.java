package com.academia.dto.response;

import com.academia.model.AcademyClass;
import com.academia.model.ClassStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record AcademyClassResponse(
        UUID id,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        Teacher teacher,
        ClassStatus status,
        String notes
) {
    public static AcademyClassResponse from(AcademyClass academyClass) {
        return new AcademyClassResponse(academyClass.id(), academyClass.date(), academyClass.startTime(),
                academyClass.endTime(), new Teacher(academyClass.teacher().id(), academyClass.teacher().name()),
                academyClass.status(), academyClass.notes());
    }

    public record Teacher(UUID id, String name) {
    }
}
