package com.academia.controller;

import com.academia.dto.request.CreateClassParticipationRequest;
import com.academia.dto.request.RecordAttendanceRequest;
import com.academia.dto.request.UpdateClassParticipationRequest;
import com.academia.dto.response.ClassParticipationResponse;
import com.academia.service.AttendanceService;
import com.academia.service.ClassParticipationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ClassParticipationController {
    private final ClassParticipationService service;
    private final AttendanceService attendanceService;

    public ClassParticipationController(ClassParticipationService service, AttendanceService attendanceService) {
        this.service = service;
        this.attendanceService = attendanceService;
    }

    @GetMapping("/classes/{classId}/participants")
    public List<ClassParticipationResponse> findAll(Authentication authentication, @PathVariable UUID classId) {
        return service.findAll(authentication.getName(), classId).stream()
                .map(ClassParticipationResponse::from).toList();
    }

    @PostMapping("/classes/{classId}/participants")
    @ResponseStatus(HttpStatus.CREATED)
    public ClassParticipationResponse create(Authentication authentication, @PathVariable UUID classId,
                                             @Valid @RequestBody CreateClassParticipationRequest request) {
        return ClassParticipationResponse.from(service.create(authentication.getName(), classId,
                request.studentId(), request.subjectIds(), request.billingMode(), request.unitPrice()));
    }

    @PatchMapping("/class-participations/{participationId}")
    public ClassParticipationResponse replaceSubjects(Authentication authentication, @PathVariable UUID participationId,
                                                      @Valid @RequestBody UpdateClassParticipationRequest request) {
        return ClassParticipationResponse.from(service.replaceSubjects(authentication.getName(),
                participationId, request.subjectIds()));
    }

    @PutMapping("/class-participations/{participationId}/attendance")
    public ClassParticipationResponse recordAttendance(Authentication authentication, @PathVariable UUID participationId,
                                                       @Valid @RequestBody RecordAttendanceRequest request) {
        return ClassParticipationResponse.from(attendanceService.record(authentication.getName(),
                participationId, request.status()));
    }
}
