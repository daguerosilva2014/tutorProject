package com.academia.controller;

import com.academia.dto.request.CreateRecoveryRequest;
import com.academia.dto.response.RecoveryResponse;
import com.academia.service.RecoveryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/class-participations/{participationId}/recovery")
public class RecoveryController {
    private final RecoveryService service;

    public RecoveryController(RecoveryService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecoveryResponse assign(Authentication authentication, @PathVariable UUID participationId,
                                   @Valid @RequestBody CreateRecoveryRequest request) {
        return RecoveryResponse.from(service.assign(authentication.getName(), participationId, request.targetClassId()));
    }
}
