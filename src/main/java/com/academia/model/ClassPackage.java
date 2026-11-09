package com.academia.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "class_packages")
public class ClassPackage {
    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "total_classes", nullable = false)
    private int totalClasses;

    @Column(name = "used_classes", nullable = false)
    private int usedClasses;

    @Column(name = "price_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal priceAmount;

    @Column(name = "paid_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal paidAmount;

    @Column(name = "purchased_at", nullable = false)
    private LocalDate purchasedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected ClassPackage() {
    }

    private ClassPackage(Student student, int totalClasses, Money price, LocalDate purchasedAt) {
        if (totalClasses <= 0) {
            throw new IllegalArgumentException("totalClasses must be positive");
        }
        this.id = UUID.randomUUID();
        this.student = Objects.requireNonNull(student);
        this.totalClasses = totalClasses;
        this.usedClasses = 0;
        this.priceAmount = Objects.requireNonNull(price).amount();
        this.paidAmount = BigDecimal.ZERO.setScale(2);
        this.purchasedAt = Objects.requireNonNull(purchasedAt);
    }

    public static ClassPackage create(Student student, int totalClasses, Money price, LocalDate purchasedAt) {
        return new ClassPackage(student, totalClasses, price, purchasedAt);
    }

    public void registerPayment(Money amount) {
        Objects.requireNonNull(amount);
        if (amount.amount().signum() <= 0) {
            throw new IllegalArgumentException("payment amount must be positive");
        }
        if (amount.compareTo(outstandingAmount()) > 0) {
            throw new IllegalStateException("payment exceeds outstanding amount");
        }
        paidAmount = paidAmount().add(amount).amount();
    }

    public void consumeClass() {
        if (usedClasses >= totalClasses) {
            throw new IllegalStateException("Package has no classes remaining");
        }
        usedClasses++;
    }

    public int remainingClasses() { return totalClasses - usedClasses; }
    public Money price() { return new Money(priceAmount); }
    public Money paidAmount() { return new Money(paidAmount); }
    public Money outstandingAmount() { return price().subtract(paidAmount()); }

    public PackageUsageStatus usageStatus() {
        if (usedClasses == 0) return PackageUsageStatus.PENDING;
        if (usedClasses == totalClasses) return PackageUsageStatus.EXHAUSTED;
        return PackageUsageStatus.IN_USE;
    }

    public PackagePaymentStatus paymentStatus() {
        if (outstandingAmount().amount().signum() == 0) return PackagePaymentStatus.PAID;
        if (paidAmount.signum() == 0) return PackagePaymentStatus.PENDING;
        return PackagePaymentStatus.PARTIAL;
    }

    public UUID id() { return id; }
    public Student student() { return student; }
    public int totalClasses() { return totalClasses; }
    public int usedClasses() { return usedClasses; }
    public LocalDate purchasedAt() { return purchasedAt; }
}
