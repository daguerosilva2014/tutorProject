package com.academia.dto.request;

import com.academia.model.SubjectStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeSubjectStatusRequest(@NotNull SubjectStatus status) {
}
