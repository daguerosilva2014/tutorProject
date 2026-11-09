package com.academia.model;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    @Id
    private UUID id;
    @Column(nullable = false, length = 120)
    private String name;
    @Column(nullable = false, length = 80, unique = true)
    private String username;
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected User() {
    }

    private User(
            UUID id,
            String name,
            String username,
            String passwordHash,
            UserRole role,
            UserStatus status,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.name = requireText(name, "name");
        this.username = normalizeUsername(username);
        this.passwordHash = requireText(passwordHash, "passwordHash");
        this.role = Objects.requireNonNull(role);
        this.status = Objects.requireNonNull(status);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public static User create(String name, String username, String passwordHash, UserRole role) {
        return new User(
                UUID.randomUUID(),
                name,
                username,
                passwordHash,
                role,
                UserStatus.ACTIVE,
                Instant.now()
        );
    }

    public void update(String name, UserRole role, UserStatus status) {
        this.name = requireText(name, "name");
        this.role = Objects.requireNonNull(role);
        this.status = Objects.requireNonNull(status);
    }

    public void deactivate() {
        this.status = UserStatus.INACTIVE;
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
    }

    private static String normalizeUsername(String username) {
        return requireText(username, "username").toLowerCase(Locale.ROOT);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    public UUID id() { return id; }
    public String name() { return name; }
    public String username() { return username; }
    public String passwordHash() { return passwordHash; }
    public UserRole role() { return role; }
    public UserStatus status() { return status; }
    public Instant createdAt() { return createdAt; }
}
