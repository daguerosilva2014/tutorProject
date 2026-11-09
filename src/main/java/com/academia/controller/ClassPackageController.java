package com.academia.controller;

import com.academia.dto.request.CreateClassPackageRequest;
import com.academia.dto.response.ClassPackageResponse;
import com.academia.model.Money;
import com.academia.model.PackagePaymentStatus;
import com.academia.model.PackageUsageStatus;
import com.academia.service.ClassPackageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ClassPackageController {
    private final ClassPackageService service;

    public ClassPackageController(ClassPackageService service) {
        this.service = service;
    }

    @GetMapping("/students/{studentId}/packages")
    public List<ClassPackageResponse> findAll(@PathVariable UUID studentId,
                                              @RequestParam(required = false) PackageUsageStatus usageStatus,
                                              @RequestParam(required = false) PackagePaymentStatus paymentStatus) {
        return service.findAll(studentId, usageStatus, paymentStatus).stream()
                .map(ClassPackageResponse::from).toList();
    }

    @PostMapping("/students/{studentId}/packages")
    @ResponseStatus(HttpStatus.CREATED)
    public ClassPackageResponse create(@PathVariable UUID studentId,
                                       @Valid @RequestBody CreateClassPackageRequest request) {
        return ClassPackageResponse.from(service.create(studentId, request.totalClasses(),
                new Money(request.price()), request.purchasedAt()));
    }

    @GetMapping("/packages/{packageId}")
    public ClassPackageResponse findById(@PathVariable UUID packageId) {
        return ClassPackageResponse.from(service.findById(packageId));
    }
}
