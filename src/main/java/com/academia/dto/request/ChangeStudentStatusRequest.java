package com.academia.dto.request;

import com.academia.model.StudentStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeStudentStatusRequest(@NotNull StudentStatus status) {
}
