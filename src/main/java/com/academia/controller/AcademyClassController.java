package com.academia.controller;

import com.academia.dto.request.CreateAcademyClassRequest;
import com.academia.dto.request.UpdateAcademyClassRequest;
import com.academia.dto.response.AcademyClassResponse;
import com.academia.model.ClassStatus;
import com.academia.service.AcademyClassService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/classes")
public class AcademyClassController {
    private final AcademyClassService service;

    public AcademyClassController(AcademyClassService service) {
        this.service = service;
    }

    @GetMapping
    public List<AcademyClassResponse> findAll(Authentication authentication,
                                              @RequestParam(required = false) LocalDate from,
                                              @RequestParam(required = false) LocalDate to,
                                              @RequestParam(required = false) UUID teacherId,
                                              @RequestParam(required = false) ClassStatus status) {
        return service.findAll(authentication.getName(), from, to, teacherId, status).stream()
                .map(AcademyClassResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AcademyClassResponse create(Authentication authentication,
                                       @Valid @RequestBody CreateAcademyClassRequest request) {
        return AcademyClassResponse.from(service.create(authentication.getName(), request.teacherId(),
                request.date(), request.startTime(), request.endTime(), request.notes()));
    }

    @GetMapping("/{classId}")
    public AcademyClassResponse findById(Authentication authentication, @PathVariable UUID classId) {
        return AcademyClassResponse.from(service.findById(authentication.getName(), classId));
    }

    @PatchMapping("/{classId}")
    public AcademyClassResponse update(Authentication authentication, @PathVariable UUID classId,
                                       @Valid @RequestBody UpdateAcademyClassRequest request) {
        return AcademyClassResponse.from(service.update(authentication.getName(), classId,
                request.date(), request.startTime(), request.endTime(), request.notes()));
    }

    @PostMapping("/{classId}/cancel")
    public AcademyClassResponse cancel(Authentication authentication, @PathVariable UUID classId) {
        return AcademyClassResponse.from(service.cancel(authentication.getName(), classId));
    }
}
