package com.academia.service;

import com.academia.exception.AcademyClassNotFoundException;
import com.academia.model.AcademyClass;
import com.academia.model.AttendanceStatus;
import com.academia.model.BillingMode;
import com.academia.model.ClassConsumption;
import com.academia.model.ClassPackage;
import com.academia.model.ClassStatus;
import com.academia.model.ConsumptionStatus;
import com.academia.model.ClassParticipation;
import com.academia.repository.AcademyClassRepository;
import com.academia.repository.ClassConsumptionRepository;
import com.academia.repository.ClassParticipationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ClassClosureService {
    private final AcademyClassRepository classRepository;
    private final ClassParticipationRepository participationRepository;
    private final ClassPackageService packageService;
    private final ClassConsumptionRepository consumptionRepository;
    private final Clock clock;

    public ClassClosureService(AcademyClassRepository classRepository,
                               ClassParticipationRepository participationRepository,
                               ClassPackageService packageService, ClassConsumptionRepository consumptionRepository,
                               Clock clock) {
        this.classRepository = classRepository;
        this.participationRepository = participationRepository;
        this.packageService = packageService;
        this.consumptionRepository = consumptionRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<UUID> findDueIds() {
        return classRepository.findDue(LocalDate.now(clock), LocalTime.now(clock)).stream()
                .map(AcademyClass::id).toList();
    }

    @Transactional
    public void close(UUID classId) {
        AcademyClass academyClass = classRepository.findById(classId).orElseThrow(AcademyClassNotFoundException::new);
        if (academyClass.status() != ClassStatus.SCHEDULED) return;
        if (clock.instant().isBefore(ZonedDateTime.of(academyClass.date(), academyClass.endTime(),
                clock.getZone()).toInstant())) return;
        academyClass.finalizeClass();
        for (ClassParticipation participation : participationRepository.findByAcademyClassIdOrderByStudentNameAsc(classId)) {
            if (participation.attendanceStatus() == AttendanceStatus.PENDING) {
                participation.recordAttendance(AttendanceStatus.ABSENT, null);
            }
            if (participation.attendanceStatus() == AttendanceStatus.JUSTIFIED_ABSENCE) {
                participation.closeWithoutConsumption(ConsumptionStatus.NOT_CONSUMED);
            } else if (participation.billingMode() == BillingMode.PER_CLASS) {
                participation.closeWithoutConsumption(ConsumptionStatus.NOT_APPLICABLE);
            } else {
                ClassPackage classPackage = packageService.findOldestAvailable(participation.student().id());
                classPackage.consumeClass();
                participation.consumeFrom(classPackage);
                consumptionRepository.save(ClassConsumption.create(participation, classPackage, clock.instant()));
            }
        }
    }
}
