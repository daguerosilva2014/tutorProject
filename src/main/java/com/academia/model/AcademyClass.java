package com.academia.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "classes")
public class AcademyClass {
    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClassStatus status;

    @Column(length = 1000)
    private String notes;

    protected AcademyClass() {
    }

    private AcademyClass(User teacher, LocalDate date, LocalTime startTime, LocalTime endTime, String notes) {
        this.id = UUID.randomUUID();
        this.teacher = Objects.requireNonNull(teacher);
        this.status = ClassStatus.SCHEDULED;
        update(date, startTime, endTime, notes);
    }

    public static AcademyClass create(User teacher, LocalDate date, LocalTime startTime, LocalTime endTime, String notes) {
        return new AcademyClass(teacher, date, startTime, endTime, notes);
    }

    public void update(LocalDate date, LocalTime startTime, LocalTime endTime, String notes) {
        if (status != ClassStatus.SCHEDULED) {
            throw new IllegalStateException("Only scheduled classes can be updated");
        }
        if (date == null || startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("A class needs a date and an end time after its start time");
        }
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.notes = notes == null || notes.isBlank() ? null : notes.trim();
    }

    public void cancel() {
        if (status != ClassStatus.SCHEDULED) {
            throw new IllegalStateException("Only scheduled classes can be cancelled");
        }
        this.status = ClassStatus.CANCELLED;
    }

    public UUID id() { return id; }
    public User teacher() { return teacher; }
    public LocalDate date() { return date; }
    public LocalTime startTime() { return startTime; }
    public LocalTime endTime() { return endTime; }
    public ClassStatus status() { return status; }
    public String notes() { return notes; }
}
