package com.academia.controller;

import com.academia.dto.request.CreateUserRequest;
import com.academia.dto.request.UpdateUserRequest;
import com.academia.dto.response.UserResponse;
import com.academia.model.UserRole;
import com.academia.model.UserStatus;
import com.academia.service.UserService;
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

import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponse> findAll(
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) UserStatus status
    ) {
        return userService.findAll(role, status).stream()
                .map(UserResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return UserResponse.from(userService.create(
                request.name(), request.username(), request.password(), request.role()));
    }

    @GetMapping("/{userId}")
    public UserResponse findById(@PathVariable UUID userId) {
        return UserResponse.from(userService.findById(userId));
    }

    @PatchMapping("/{userId}")
    public UserResponse update(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        return UserResponse.from(userService.update(userId, request.name(), request.role(), request.enabled()));
    }
}
