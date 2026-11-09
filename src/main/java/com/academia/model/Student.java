package com.academia.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "students")
public class Student {
    @Id
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "contact_number", length = 30)
    private String contactNumber;

    @Column(length = 1000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StudentStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Student() {
    }

    private Student(String name, String contactNumber, String notes) {
        this.id = UUID.randomUUID();
        update(name, contactNumber, notes);
        this.status = StudentStatus.ACTIVE;
        this.createdAt = Instant.now();
    }

    public static Student create(String name, String contactNumber, String notes) {
        return new Student(name, contactNumber, notes);
    }

    public void update(String name, String contactNumber, String notes) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        this.name = name.trim();
        this.contactNumber = optionalText(contactNumber);
        this.notes = optionalText(notes);
    }

    public void changeStatus(StudentStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        this.status = status;
    }

    private static String optionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public UUID id() { return id; }
    public String name() { return name; }
    public String contactNumber() { return contactNumber; }
    public String notes() { return notes; }
    public StudentStatus status() { return status; }
    public Instant createdAt() { return createdAt; }
}
