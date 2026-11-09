package com.academia.service;

import com.academia.exception.ClassParticipationNotFoundException;
import com.academia.exception.DuplicateClassParticipationException;
import com.academia.exception.DuplicateSubjectException;
import com.academia.exception.InactiveStudentException;
import com.academia.exception.InactiveSubjectException;
import com.academia.exception.InvalidClassStateException;
import com.academia.model.AcademyClass;
import com.academia.model.ClassParticipation;
import com.academia.model.ClassStatus;
import com.academia.model.Student;
import com.academia.model.StudentStatus;
import com.academia.model.Subject;
import com.academia.model.SubjectStatus;
import com.academia.repository.ClassParticipationRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ClassParticipationService {
    private final ClassParticipationRepository repository;
    private final AcademyClassService classService;
    private final StudentService studentService;
    private final SubjectService subjectService;

    public ClassParticipationService(ClassParticipationRepository repository, AcademyClassService classService,
                                     StudentService studentService, SubjectService subjectService) {
        this.repository = repository;
        this.classService = classService;
        this.studentService = studentService;
        this.subjectService = subjectService;
    }

    @Transactional(readOnly = true)
    public List<ClassParticipation> findAll(String username, UUID classId) {
        classService.findById(username, classId);
        return repository.findByAcademyClassIdOrderByStudentNameAsc(classId);
    }

    @Transactional
    public ClassParticipation create(String username, UUID classId, UUID studentId, List<UUID> subjectIds) {
        AcademyClass academyClass = classService.findById(username, classId);
        requireScheduled(academyClass);
        Student student = studentService.findById(studentId);
        if (student.status() != StudentStatus.ACTIVE) {
            throw new InactiveStudentException();
        }
        if (repository.existsByAcademyClassIdAndStudentId(classId, studentId)) {
            throw new DuplicateClassParticipationException();
        }
        Set<Subject> subjects = resolveSubjects(subjectIds, Set.of());
        try {
            return repository.saveAndFlush(ClassParticipation.create(academyClass, student, subjects));
        } catch (DataIntegrityViolationException exception) {
            if (exception.getCause() instanceof ConstraintViolationException violation
                    && "uq_class_participation_class_student".equalsIgnoreCase(violation.getConstraintName())) {
                throw new DuplicateClassParticipationException();
            }
            throw exception;
        }
    }

    @Transactional
    public ClassParticipation replaceSubjects(String username, UUID participationId, List<UUID> subjectIds) {
        ClassParticipation participation = repository.findById(participationId)
                .orElseThrow(ClassParticipationNotFoundException::new);
        classService.findById(username, participation.academyClass().id());
        requireScheduled(participation.academyClass());
        Set<UUID> currentSubjectIds = participation.subjects().stream()
                .map(Subject::id).collect(Collectors.toSet());
        participation.replaceSubjects(resolveSubjects(subjectIds, currentSubjectIds));
        return participation;
    }

    private Set<Subject> resolveSubjects(List<UUID> subjectIds, Set<UUID> currentSubjectIds) {
        Set<Subject> subjects = new LinkedHashSet<>();
        if (subjectIds == null) {
            return subjects;
        }
        Set<UUID> seen = new LinkedHashSet<>();
        for (UUID subjectId : subjectIds) {
            if (!seen.add(subjectId)) {
                throw new DuplicateSubjectException();
            }
            Subject subject = subjectService.findById(subjectId);
            if (subject.status() != SubjectStatus.ACTIVE && !currentSubjectIds.contains(subjectId)) {
                throw new InactiveSubjectException();
            }
            subjects.add(subject);
        }
        return subjects;
    }

    private void requireScheduled(AcademyClass academyClass) {
        if (academyClass.status() != ClassStatus.SCHEDULED) {
            throw new InvalidClassStateException();
        }
    }
}
