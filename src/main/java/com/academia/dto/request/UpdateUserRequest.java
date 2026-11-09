package com.academia.dto.request;

import com.academia.model.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @NotBlank @Size(max = 120) String name,
        @NotNull UserRole role,
        boolean enabled
) {
}
