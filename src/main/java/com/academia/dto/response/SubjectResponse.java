package com.academia.dto.response;

import com.academia.model.Subject;
import com.academia.model.SubjectStatus;

import java.util.UUID;

public record SubjectResponse(UUID id, String name, String description, SubjectStatus status) {
    public static SubjectResponse from(Subject subject) {
        return new SubjectResponse(subject.id(), subject.name(), subject.description(), subject.status());
    }
}
