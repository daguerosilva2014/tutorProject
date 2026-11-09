package com.academia.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class StudentTest {
    @Test
    void startsActiveAndKeepsIdentityWhenStatusChanges() {
        Student student = Student.create(" Ana ", " ", null);
        var id = student.id();

        assertEquals("Ana", student.name());
        assertNull(student.contactNumber());
        student.changeStatus(StudentStatus.INACTIVE);
        assertEquals(StudentStatus.INACTIVE, student.status());
        student.changeStatus(StudentStatus.ACTIVE);
        assertEquals(StudentStatus.ACTIVE, student.status());
        assertEquals(id, student.id());
    }
}
