package com.academia.controller;

import com.academia.dto.request.CreatePaymentRequest;
import com.academia.dto.response.PerClassPaymentResponse;
import com.academia.model.Money;
import com.academia.service.PerClassPaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/class-participations/{participationId}/payments")
public class PerClassPaymentController {
    private final PerClassPaymentService service;

    public PerClassPaymentController(PerClassPaymentService service) {
        this.service = service;
    }

    @GetMapping
    public List<PerClassPaymentResponse> findAll(Authentication authentication, @PathVariable UUID participationId) {
        return service.findAll(authentication.getName(), participationId).stream()
                .map(PerClassPaymentResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PerClassPaymentResponse create(Authentication authentication, @PathVariable UUID participationId,
                                          @Valid @RequestBody CreatePaymentRequest request) {
        return PerClassPaymentResponse.from(service.create(authentication.getName(), participationId,
                new Money(request.amount()), request.paidAt(), request.method(), request.notes()));
    }
}
