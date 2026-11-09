package com.academia.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "per_class_payments")
public class PerClassPayment {
    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "participation_id", nullable = false)
    private ClassParticipation participation;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "paid_at", nullable = false)
    private LocalDate paidAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PaymentMethod method;

    @Column(length = 1000)
    private String notes;

    @ManyToOne(optional = false)
    @JoinColumn(name = "registered_by", nullable = false)
    private User registeredBy;

    protected PerClassPayment() {
    }

    private PerClassPayment(ClassParticipation participation, Money amount, LocalDate paidAt,
                            PaymentMethod method, String notes, User registeredBy) {
        this.id = UUID.randomUUID();
        this.participation = Objects.requireNonNull(participation);
        this.amount = Objects.requireNonNull(amount).amount();
        this.paidAt = Objects.requireNonNull(paidAt);
        this.method = method;
        this.notes = notes == null || notes.isBlank() ? null : notes.trim();
        this.registeredBy = Objects.requireNonNull(registeredBy);
    }

    public static PerClassPayment create(ClassParticipation participation, Money amount, LocalDate paidAt,
                                         PaymentMethod method, String notes, User registeredBy) {
        return new PerClassPayment(participation, amount, paidAt, method, notes, registeredBy);
    }

    public UUID id() { return id; }
    public ClassParticipation participation() { return participation; }
    public Money amount() { return new Money(amount); }
    public LocalDate paidAt() { return paidAt; }
    public PaymentMethod method() { return method; }
    public String notes() { return notes; }
    public User registeredBy() { return registeredBy; }
}
