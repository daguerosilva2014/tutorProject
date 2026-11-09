package com.academia.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "class_consumptions")
public class ClassConsumption {
    @Id
    private UUID id;

    @OneToOne(optional = false)
    @JoinColumn(name = "participation_id", nullable = false, unique = true)
    private ClassParticipation participation;

    @ManyToOne(optional = false)
    @JoinColumn(name = "package_id", nullable = false)
    private ClassPackage classPackage;

    @Column(name = "consumed_at", nullable = false)
    private Instant consumedAt;

    protected ClassConsumption() {
    }

    private ClassConsumption(ClassParticipation participation, ClassPackage classPackage, Instant consumedAt) {
        this.id = UUID.randomUUID();
        this.participation = Objects.requireNonNull(participation);
        this.classPackage = Objects.requireNonNull(classPackage);
        this.consumedAt = Objects.requireNonNull(consumedAt);
    }

    public static ClassConsumption create(ClassParticipation participation, ClassPackage classPackage, Instant consumedAt) {
        return new ClassConsumption(participation, classPackage, consumedAt);
    }

    public UUID id() { return id; }
    public ClassParticipation participation() { return participation; }
    public ClassPackage classPackage() { return classPackage; }
    public Instant consumedAt() { return consumedAt; }
}
