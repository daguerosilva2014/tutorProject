package com.academia.dto.response;

import com.academia.model.Student;
import com.academia.model.StudentStatus;

import java.time.Instant;
import java.util.UUID;

public record StudentResponse(
        UUID id,
        String name,
        String contactNumber,
        String notes,
        StudentStatus status,
        Instant createdAt
) {
    public static StudentResponse from(Student student) {
        return new StudentResponse(student.id(), student.name(), student.contactNumber(),
                student.notes(), student.status(), student.createdAt());
    }
}
