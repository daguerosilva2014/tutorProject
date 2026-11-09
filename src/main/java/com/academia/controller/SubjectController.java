package com.academia.controller;

import com.academia.dto.request.ChangeSubjectStatusRequest;
import com.academia.dto.request.CreateSubjectRequest;
import com.academia.dto.request.UpdateSubjectRequest;
import com.academia.dto.response.SubjectResponse;
import com.academia.model.SubjectStatus;
import com.academia.service.SubjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
@RequestMapping("/api/v1/subjects")
public class SubjectController {
    private final SubjectService service;

    public SubjectController(SubjectService service) {
        this.service = service;
    }

    @GetMapping
    public List<SubjectResponse> findAll(@RequestParam(required = false) SubjectStatus status,
                                         @RequestParam(required = false) String search) {
        return service.findAll(status, search).stream().map(SubjectResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubjectResponse create(@Valid @RequestBody CreateSubjectRequest request) {
        return SubjectResponse.from(service.create(request.name(), request.description()));
    }

    @GetMapping("/{subjectId}")
    public SubjectResponse findById(@PathVariable UUID subjectId) {
        return SubjectResponse.from(service.findById(subjectId));
    }

    @PatchMapping("/{subjectId}")
    public SubjectResponse update(@PathVariable UUID subjectId, @Valid @RequestBody UpdateSubjectRequest request) {
        return SubjectResponse.from(service.update(subjectId, request.name(), request.description()));
    }

    @PatchMapping("/{subjectId}/status")
    public SubjectResponse changeStatus(@PathVariable UUID subjectId,
                                        @Valid @RequestBody ChangeSubjectStatusRequest request) {
        return SubjectResponse.from(service.changeStatus(subjectId, request.status()));
    }
}
