package com.academia.dto.request;

import com.academia.model.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 80) String username,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotNull UserRole role
) {
}
