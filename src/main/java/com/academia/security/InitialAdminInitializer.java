package com.academia.security;

import com.academia.service.UserService;
import com.academia.model.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class InitialAdminInitializer implements CommandLineRunner {

    private final UserService userService;
    private final String username;
    private final String password;

    public InitialAdminInitializer(
            UserService userService,
            @Value("${security.initial-admin.username:}") String username,
            @Value("${security.initial-admin.password:}") String password
    ) {
        this.userService = userService;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (username.isBlank() && password.isBlank()) {
            return;
        }
        if (username.isBlank() || password.isBlank()) {
            throw new IllegalStateException("Initial admin username and password must be configured together");
        }
        if (!userService.hasAdmin()) {
            userService.create("Initial administrator", username, password, UserRole.ADMIN);
        }
    }
}
