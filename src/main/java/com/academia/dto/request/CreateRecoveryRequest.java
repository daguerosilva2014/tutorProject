package com.academia.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateRecoveryRequest(@NotNull UUID targetClassId) {
}
