package com.academia.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateClassParticipationRequest(
        @NotNull UUID studentId,
        List<@NotNull UUID> subjectIds
) {
}
