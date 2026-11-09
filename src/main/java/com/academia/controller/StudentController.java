package com.academia.controller;

import com.academia.dto.request.ChangeStudentStatusRequest;
import com.academia.dto.request.CreateStudentRequest;
import com.academia.dto.request.UpdateStudentRequest;
import com.academia.dto.response.StudentResponse;
import com.academia.model.StudentStatus;
import com.academia.service.StudentService;
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
@RequestMapping("/api/v1/students")
public class StudentController {
    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping
    public List<StudentResponse> findAll(
            @RequestParam(required = false) StudentStatus status,
            @RequestParam(required = false) String search
    ) {
        return studentService.findAll(status, search).stream().map(StudentResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudentResponse create(@Valid @RequestBody CreateStudentRequest request) {
        return StudentResponse.from(studentService.create(request.name(), request.contactNumber(), request.notes()));
    }

    @GetMapping("/{studentId}")
    public StudentResponse findById(@PathVariable UUID studentId) {
        return StudentResponse.from(studentService.findById(studentId));
    }

    @PatchMapping("/{studentId}")
    public StudentResponse update(@PathVariable UUID studentId, @Valid @RequestBody UpdateStudentRequest request) {
        return StudentResponse.from(studentService.update(studentId, request.name(), request.contactNumber(), request.notes()));
    }

    @PatchMapping("/{studentId}/status")
    public StudentResponse changeStatus(@PathVariable UUID studentId, @Valid @RequestBody ChangeStudentStatusRequest request) {
        return StudentResponse.from(studentService.changeStatus(studentId, request.status()));
    }
}
