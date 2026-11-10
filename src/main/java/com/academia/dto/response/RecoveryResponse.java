package com.academia.dto.response;

import com.academia.model.ClassParticipation;

import java.util.UUID;

public record RecoveryResponse(UUID originalParticipationId, UUID targetParticipationId, String status) {
    public static RecoveryResponse from(ClassParticipation original) {
        return new RecoveryResponse(original.id(), original.recoveryTarget().id(), "RECOVERED");
    }
}
