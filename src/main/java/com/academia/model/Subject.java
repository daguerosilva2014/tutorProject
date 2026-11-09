package com.academia.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "subjects")
public class Subject {
    @Id
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubjectStatus status;

    protected Subject() {
    }

    private Subject(String name, String description) {
        this.id = UUID.randomUUID();
        update(name, description);
        this.status = SubjectStatus.ACTIVE;
    }

    public static Subject create(String name, String description) {
        return new Subject(name, description);
    }

    public void update(String name, String description) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        this.name = name.trim();
        this.description = description == null || description.isBlank() ? null : description.trim();
    }

    public void changeStatus(SubjectStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        this.status = status;
    }

    public UUID id() { return id; }
    public String name() { return name; }
    public String description() { return description; }
    public SubjectStatus status() { return status; }
}
