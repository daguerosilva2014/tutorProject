package com.academia.service;

import com.academia.exception.ClassParticipationNotFoundException;
import com.academia.exception.InvalidRecoveryException;
import com.academia.model.AcademyClass;
import com.academia.model.AttendanceStatus;
import com.academia.model.ClassParticipation;
import com.academia.model.ClassStatus;
import com.academia.model.ConsumptionStatus;
import com.academia.repository.ClassParticipationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class RecoveryService {
    private final ClassParticipationRepository repository;
    private final ClassParticipationService participationService;
    private final AcademyClassService classService;
    private final Clock clock;

    public RecoveryService(ClassParticipationRepository repository, ClassParticipationService participationService,
                           AcademyClassService classService, Clock clock) {
        this.repository = repository;
        this.participationService = participationService;
        this.classService = classService;
        this.clock = clock;
    }

    @Transactional
    public ClassParticipation assign(String username, UUID originalId, UUID targetClassId) {
        ClassParticipation original = repository.findById(originalId)
                .orElseThrow(ClassParticipationNotFoundException::new);
        classService.findById(username, original.academyClass().id());
        AcademyClass targetClass = classService.findById(username, targetClassId);
        if (original.recoveryTarget() != null &&
                original.recoveryTarget().academyClass().status() != ClassStatus.CANCELLED) {
            if (original.recoveryTarget().academyClass().id().equals(targetClassId)) return original;
            throw new InvalidRecoveryException();
        }
        if (original.attendanceStatus() != AttendanceStatus.JUSTIFIED_ABSENCE ||
                original.consumptionStatus() != ConsumptionStatus.NOT_CONSUMED ||
                original.academyClass().status() != ClassStatus.FINALIZED ||
                targetClass.status() != ClassStatus.SCHEDULED ||
                !start(targetClass).isAfter(clock.instant()) ||
                !start(targetClass).isAfter(end(original.academyClass()))) {
            throw new InvalidRecoveryException();
        }
        ClassParticipation target = participationService.create(username, targetClassId, original.student().id(),
                List.of(), original.billingMode(),
                original.unitPrice() == null ? null : original.unitPrice().amount());
        original.assignRecovery(target);
        repository.flush();
        return original;
    }

    private Instant start(AcademyClass academyClass) {
        return ZonedDateTime.of(academyClass.date(), academyClass.startTime(), clock.getZone()).toInstant();
    }

    private Instant end(AcademyClass academyClass) {
        return ZonedDateTime.of(academyClass.date(), academyClass.endTime(), clock.getZone()).toInstant();
    }
}
