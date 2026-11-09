package com.academia.controller;

import com.academia.service.UserService;
import com.academia.exception.UsernameAlreadyExistsException;
import com.academia.model.UserRole;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthApiTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserService userService;
    @Autowired ObjectMapper objectMapper;

    @BeforeEach
    void createTestUser() {
        try {
            userService.create("Admin", "admin", "Admin123!", UserRole.ADMIN);
        } catch (UsernameAlreadyExistsException ignored) {
        }
    }

    @Test
    void loginReturnsTokenForValidCredentials() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"Admin123!\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void loginRejectsInvalidCredentials() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserCanConsultMe() throws Exception {
        String token = loginAndGetToken("admin", "Admin123!");

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanManageUsers() throws Exception {
        String token = loginAndGetToken("admin", "Admin123!");

        mockMvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void adminCanCreateUser() throws Exception {
        String token = loginAndGetToken("admin", "Admin123!");

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New Teacher\",\"username\":\"new.teacher\",\"password\":\"Teacher123!\",\"role\":\"TEACHER\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void protectedEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void teacherCannotManageUsers() throws Exception {
        userService.create("Teacher", "teacher", "Teacher123!", UserRole.TEACHER);
        String token = loginAndGetToken("teacher", "Teacher123!");

        mockMvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void inactiveUserCannotLogin() throws Exception {
        userService.create("Inactive", "inactive", "Inactive123!", UserRole.TEACHER);
        var inactive = userService.findByUsername("inactive");
        userService.update(inactive.id(), inactive.name(), inactive.role(), false);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"inactive\",\"password\":\"Inactive123!\"}"))
                .andExpect(status().isUnauthorized());

        String adminToken = loginAndGetToken("admin", "Admin123!");
        mockMvc.perform(get("/api/v1/users?status=INACTIVE")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("inactive"));
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        var result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("accessToken").asText();
    }
}
