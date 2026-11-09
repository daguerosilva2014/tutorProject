package com.academia.dto.response;

import com.academia.model.ClassParticipation;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record ClassParticipationResponse(UUID id, UUID classId, Student student, List<Subject> subjects) {
    public static ClassParticipationResponse from(ClassParticipation participation) {
        return new ClassParticipationResponse(
                participation.id(),
                participation.academyClass().id(),
                new Student(participation.student().id(), participation.student().name()),
                participation.subjects().stream()
                        .map(subject -> new Subject(subject.id(), subject.name()))
                        .sorted(Comparator.comparing(Subject::name).thenComparing(Subject::id))
                        .toList()
        );
    }

    public record Student(UUID id, String name) {
    }

    public record Subject(UUID id, String name) {
    }
}
