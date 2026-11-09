package com.academia.service;

import com.academia.exception.AttendanceConflictException;
import com.academia.exception.ClassParticipationNotFoundException;
import com.academia.exception.InvalidClassStateException;
import com.academia.model.AttendanceMark;
import com.academia.model.AttendanceStatus;
import com.academia.model.ClassParticipation;
import com.academia.model.ClassStatus;
import com.academia.repository.ClassParticipationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.UUID;

@Service
public class AttendanceService {
    private final ClassParticipationRepository repository;
    private final AcademyClassService classService;
    private final Clock clock;

    public AttendanceService(ClassParticipationRepository repository, AcademyClassService classService, Clock clock) {
        this.repository = repository;
        this.classService = classService;
        this.clock = clock;
    }

    @Transactional
    public ClassParticipation record(String username, UUID participationId, AttendanceMark mark) {
        ClassParticipation participation = repository.findById(participationId)
                .orElseThrow(ClassParticipationNotFoundException::new);
        classService.findById(username, participation.academyClass().id());
        if (participation.attendanceStatus() != AttendanceStatus.PENDING) {
            if (matches(participation.attendanceStatus(), mark)) return participation;
            throw new AttendanceConflictException();
        }
        if (participation.academyClass().status() != ClassStatus.SCHEDULED) throw new InvalidClassStateException();
        if (mark == AttendanceMark.PRESENT) {
            participation.recordAttendance(AttendanceStatus.PRESENT, null);
        } else {
            Instant now = clock.instant();
            Instant start = ZonedDateTime.of(participation.academyClass().date(),
                    participation.academyClass().startTime(), clock.getZone()).toInstant();
            AttendanceStatus status = !now.isAfter(start.minusSeconds(12 * 60 * 60))
                    ? AttendanceStatus.JUSTIFIED_ABSENCE : AttendanceStatus.ABSENT;
            participation.recordAttendance(status, now);
        }
        return participation;
    }

    private boolean matches(AttendanceStatus status, AttendanceMark mark) {
        return mark == AttendanceMark.PRESENT ? status == AttendanceStatus.PRESENT
                : status == AttendanceStatus.ABSENT || status == AttendanceStatus.JUSTIFIED_ABSENCE;
    }
}
