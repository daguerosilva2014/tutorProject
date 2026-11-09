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
@Table(name = "payments")
public class Payment {
    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "package_id", nullable = false)
    private ClassPackage classPackage;

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

    protected Payment() {
    }

    private Payment(ClassPackage classPackage, Money amount, LocalDate paidAt,
                    PaymentMethod method, String notes, User registeredBy) {
        if (amount == null || amount.amount().signum() <= 0) {
            throw new IllegalArgumentException("payment amount must be positive");
        }
        this.id = UUID.randomUUID();
        this.classPackage = Objects.requireNonNull(classPackage);
        this.amount = amount.amount();
        this.paidAt = Objects.requireNonNull(paidAt);
        this.method = method;
        this.notes = notes == null || notes.isBlank() ? null : notes.trim();
        this.registeredBy = Objects.requireNonNull(registeredBy);
    }

    public static Payment create(ClassPackage classPackage, Money amount, LocalDate paidAt,
                                 PaymentMethod method, String notes, User registeredBy) {
        return new Payment(classPackage, amount, paidAt, method, notes, registeredBy);
    }

    public UUID id() { return id; }
    public ClassPackage classPackage() { return classPackage; }
    public Money amount() { return new Money(amount); }
    public LocalDate paidAt() { return paidAt; }
    public PaymentMethod method() { return method; }
    public String notes() { return notes; }
    public User registeredBy() { return registeredBy; }
}
