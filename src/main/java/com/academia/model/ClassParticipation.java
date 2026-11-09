package com.academia.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "class_participations", uniqueConstraints =
        @UniqueConstraint(name = "uq_class_participation_class_student", columnNames = {"class_id", "student_id"}))
public class ClassParticipation {
    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "class_id", nullable = false)
    private AcademyClass academyClass;

    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "class_participation_subjects",
            joinColumns = @JoinColumn(name = "participation_id"),
            inverseJoinColumns = @JoinColumn(name = "subject_id"))
    private Set<Subject> subjects = new LinkedHashSet<>();

    protected ClassParticipation() {
    }

    private ClassParticipation(AcademyClass academyClass, Student student, Set<Subject> subjects) {
        this.id = UUID.randomUUID();
        this.academyClass = Objects.requireNonNull(academyClass);
        this.student = Objects.requireNonNull(student);
        replaceSubjects(subjects);
    }

    public static ClassParticipation create(AcademyClass academyClass, Student student, Set<Subject> subjects) {
        return new ClassParticipation(academyClass, student, subjects);
    }

    public void replaceSubjects(Set<Subject> subjects) {
        this.subjects.clear();
        this.subjects.addAll(Objects.requireNonNull(subjects));
    }

    public UUID id() { return id; }
    public AcademyClass academyClass() { return academyClass; }
    public Student student() { return student; }
    public Set<Subject> subjects() { return Set.copyOf(subjects); }
}
