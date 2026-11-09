package com.academia.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record UpdateClassParticipationRequest(@NotNull List<@NotNull UUID> subjectIds) {
}
