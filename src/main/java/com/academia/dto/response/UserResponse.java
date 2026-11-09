package com.academia.dto.response;

import com.academia.model.User;
import com.academia.model.UserStatus;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String username,
        String role,
        boolean enabled,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.id(),
                user.name(),
                user.username(),
                user.role().name(),
                user.status() == UserStatus.ACTIVE,
                user.createdAt()
        );
    }
}
