package com.academia.controller;

import com.academia.dto.request.CreatePaymentRequest;
import com.academia.dto.response.PaymentResponse;
import com.academia.model.Money;
import com.academia.service.PaymentService;
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
@RequestMapping("/api/v1/packages/{packageId}/payments")
public class PaymentController {
    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @GetMapping
    public List<PaymentResponse> findAll(@PathVariable UUID packageId) {
        return service.findAll(packageId).stream().map(PaymentResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse create(Authentication authentication, @PathVariable UUID packageId,
                                  @Valid @RequestBody CreatePaymentRequest request) {
        return PaymentResponse.from(service.create(authentication.getName(), packageId,
                new Money(request.amount()), request.paidAt(), request.method(), request.notes()));
    }
}
