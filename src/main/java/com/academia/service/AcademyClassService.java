package com.academia.service;

import com.academia.exception.AcademyClassNotFoundException;
import com.academia.exception.ClassAccessDeniedException;
import com.academia.exception.InvalidClassScheduleException;
import com.academia.exception.InvalidClassStateException;
import com.academia.exception.InvalidTeacherException;
import com.academia.model.AcademyClass;
import com.academia.model.ClassStatus;
import com.academia.model.User;
import com.academia.model.UserRole;
import com.academia.model.UserStatus;
import com.academia.repository.AcademyClassRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
public class AcademyClassService {
    private final AcademyClassRepository repository;
    private final UserService userService;

    public AcademyClassService(AcademyClassRepository repository, UserService userService) {
        this.repository = repository;
        this.userService = userService;
    }

    @Transactional
    public AcademyClass create(String username, UUID teacherId, LocalDate date,
                               LocalTime startTime, LocalTime endTime, String notes) {
        User actor = userService.findByUsername(username);
        UUID assignedTeacherId = teacherId;
        if (actor.role() == UserRole.TEACHER) {
            if (teacherId != null && !teacherId.equals(actor.id())) {
                throw new ClassAccessDeniedException();
            }
            assignedTeacherId = actor.id();
        }
        if (assignedTeacherId == null) {
            throw new InvalidTeacherException();
        }
        User teacher = userService.findById(assignedTeacherId);
        if (teacher.role() != UserRole.TEACHER || teacher.status() != UserStatus.ACTIVE) {
            throw new InvalidTeacherException();
        }
        validateSchedule(date, startTime, endTime);
        return repository.save(AcademyClass.create(teacher, date, startTime, endTime, notes));
    }

    @Transactional(readOnly = true)
    public List<AcademyClass> findAll(String username, LocalDate fromDate, LocalDate toDate,
                                      UUID teacherId, ClassStatus status) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new InvalidClassScheduleException();
        }
        User actor = userService.findByUsername(username);
        if (actor.role() == UserRole.TEACHER) {
            if (teacherId != null && !teacherId.equals(actor.id())) {
                throw new ClassAccessDeniedException();
            }
            teacherId = actor.id();
        }
        return repository.search(fromDate, toDate, teacherId, status);
    }

    @Transactional(readOnly = true)
    public AcademyClass findById(String username, UUID id) {
        AcademyClass academyClass = repository.findById(id).orElseThrow(AcademyClassNotFoundException::new);
        requireAccess(userService.findByUsername(username), academyClass);
        return academyClass;
    }

    @Transactional
    public AcademyClass update(String username, UUID id, LocalDate date,
                               LocalTime startTime, LocalTime endTime, String notes) {
        AcademyClass academyClass = findById(username, id);
        validateSchedule(date, startTime, endTime);
        if (academyClass.status() != ClassStatus.SCHEDULED) {
            throw new InvalidClassStateException();
        }
        academyClass.update(date, startTime, endTime, notes);
        return academyClass;
    }

    @Transactional
    public AcademyClass cancel(String username, UUID id) {
        AcademyClass academyClass = findById(username, id);
        if (academyClass.status() != ClassStatus.SCHEDULED) {
            throw new InvalidClassStateException();
        }
        academyClass.cancel();
        return academyClass;
    }

    private void requireAccess(User actor, AcademyClass academyClass) {
        if (actor.role() == UserRole.TEACHER && !actor.id().equals(academyClass.teacher().id())) {
            throw new ClassAccessDeniedException();
        }
    }

    private void validateSchedule(LocalDate date, LocalTime startTime, LocalTime endTime) {
        if (date == null || startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            throw new InvalidClassScheduleException();
        }
    }
}
