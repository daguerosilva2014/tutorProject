package com.academia.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "class_participations",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_class_participation_class_student",
                columnNames = {"class_id", "student_id"}))
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
    private Set<Subject> subjects = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_mode", nullable = false, length = 20)
    private BillingMode billingMode;

    @Column(name = "unit_price_amount", precision = 14, scale = 2)
    private BigDecimal unitPriceAmount;

    @Column(name = "paid_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal paidAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_status", nullable = false, length = 25)
    private AttendanceStatus attendanceStatus;

    @Column(name = "absence_notified_at")
    private Instant absenceNotifiedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "consumption_status", nullable = false, length = 25)
    private ConsumptionStatus consumptionStatus;

    @ManyToOne
    @JoinColumn(name = "consumed_package_id")
    private ClassPackage consumedPackage;

    @Version
    @Column(nullable = false)
    private long version;

    protected ClassParticipation() {
    }

    private ClassParticipation(AcademyClass academyClass, Student student, Set<Subject> subjects,
                               BillingMode billingMode, Money unitPrice) {
        this.id = UUID.randomUUID();
        this.academyClass = Objects.requireNonNull(academyClass);
        this.student = Objects.requireNonNull(student);
        this.billingMode = Objects.requireNonNull(billingMode);
        if (billingMode == BillingMode.PER_CLASS && unitPrice == null) {
            throw new IllegalArgumentException("PER_CLASS requires a unit price");
        }
        if (billingMode == BillingMode.PACKAGE && unitPrice != null) {
            throw new IllegalArgumentException("PACKAGE cannot have a unit price");
        }
        this.unitPriceAmount = unitPrice == null ? null : unitPrice.amount();
        this.paidAmount = BigDecimal.ZERO.setScale(2);
        this.attendanceStatus = AttendanceStatus.PENDING;
        this.consumptionStatus = ConsumptionStatus.PENDING;
        replaceSubjects(subjects);
    }

    public static ClassParticipation create(AcademyClass academyClass, Student student, Set<Subject> subjects,
                                            BillingMode billingMode, Money unitPrice) {
        return new ClassParticipation(academyClass, student, subjects, billingMode, unitPrice);
    }

    public void replaceSubjects(Set<Subject> subjects) {
        this.subjects.clear();
        this.subjects.addAll(Objects.requireNonNull(subjects));
    }

    public void recordAttendance(AttendanceStatus status, Instant notifiedAt) {
        if (attendanceStatus != AttendanceStatus.PENDING) {
            throw new IllegalStateException("Attendance is already recorded");
        }
        if (status != AttendanceStatus.PRESENT && status != AttendanceStatus.ABSENT
                && status != AttendanceStatus.JUSTIFIED_ABSENCE) {
            throw new IllegalArgumentException("Invalid attendance status");
        }
        this.attendanceStatus = status;
        this.absenceNotifiedAt = notifiedAt;
    }

    public void closeWithoutConsumption(ConsumptionStatus status) {
        if (consumptionStatus != ConsumptionStatus.PENDING ||
                (status != ConsumptionStatus.NOT_CONSUMED && status != ConsumptionStatus.NOT_APPLICABLE) ||
                (status == ConsumptionStatus.NOT_CONSUMED && attendanceStatus != AttendanceStatus.JUSTIFIED_ABSENCE) ||
                (status == ConsumptionStatus.NOT_APPLICABLE && billingMode != BillingMode.PER_CLASS)) {
            throw new IllegalStateException("Invalid consumption transition");
        }
        this.consumptionStatus = status;
    }

    public void consumeFrom(ClassPackage classPackage) {
        if (billingMode != BillingMode.PACKAGE || consumptionStatus != ConsumptionStatus.PENDING ||
                attendanceStatus == AttendanceStatus.JUSTIFIED_ABSENCE ||
                academyClass.status() != ClassStatus.FINALIZED ||
                !Objects.requireNonNull(classPackage).student().id().equals(student.id())) {
            throw new IllegalStateException("Participation cannot consume a package");
        }
        this.consumedPackage = Objects.requireNonNull(classPackage);
        this.consumptionStatus = ConsumptionStatus.CONSUMED;
    }

    public Money chargeAmount() {
        if (billingMode != BillingMode.PER_CLASS || academyClass.status() != ClassStatus.FINALIZED
                || attendanceStatus == AttendanceStatus.JUSTIFIED_ABSENCE) {
            return new Money(BigDecimal.ZERO);
        }
        return new Money(unitPriceAmount);
    }

    public Money outstandingAmount() {
        return chargeAmount().subtract(new Money(paidAmount));
    }

    public void registerPayment(Money amount) {
        if (billingMode != BillingMode.PER_CLASS || academyClass.status() != ClassStatus.FINALIZED
                || amount.amount().signum() <= 0 || amount.compareTo(outstandingAmount()) > 0) {
            throw new IllegalStateException("Invalid per-class payment");
        }
        paidAmount = new Money(paidAmount).add(amount).amount();
    }

    public UUID id() { return id; }
    public AcademyClass academyClass() { return academyClass; }
    public Student student() { return student; }
    public Set<Subject> subjects() { return Set.copyOf(subjects); }
    public BillingMode billingMode() { return billingMode; }
    public Money unitPrice() { return unitPriceAmount == null ? null : new Money(unitPriceAmount); }
    public Money paidAmount() { return new Money(paidAmount); }
    public AttendanceStatus attendanceStatus() { return attendanceStatus; }
    public Instant absenceNotifiedAt() { return absenceNotifiedAt; }
    public ConsumptionStatus consumptionStatus() { return consumptionStatus; }
    public ClassPackage consumedPackage() { return consumedPackage; }
}
