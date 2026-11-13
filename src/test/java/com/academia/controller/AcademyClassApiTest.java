package com.academia.controller;

import com.academia.model.User;
import com.academia.model.UserRole;
import com.academia.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AcademyClassApiTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserService userService;

    private User admin;
    private User teacher;
    private User otherTeacher;

    @BeforeEach
    void createUsers() {
        String suffix = UUID.randomUUID().toString();
        admin = userService.create("Admin", "class.admin." + suffix, "Password123!", UserRole.ADMIN);
        teacher = userService.create("Teacher", "class.teacher." + suffix, "Password123!", UserRole.TEACHER);
        otherTeacher = userService.create("Other Teacher", "class.other." + suffix, "Password123!", UserRole.TEACHER);
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/classes")).andExpect(status().isUnauthorized());
    }

    @Test
    void teacherCanCreateEditFilterAndCancelOwnClass() throws Exception {
        String token = token(teacher);
        String id = createClass(token, null);

        mockMvc.perform(get("/api/v1/classes/{id}", id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teacher.id").value(teacher.id().toString()))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));

        mockMvc.perform(patch("/api/v1/classes/{id}", id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(schedule("2026-11-16", "17:00:00", "19:00:00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startTime").value("17:00:00"));

        mockMvc.perform(get("/api/v1/classes?from=2026-11-16&to=2026-11-16&status=SCHEDULED")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + id + "')]").isNotEmpty());

        mockMvc.perform(get("/api/v1/classes?from=2026-11-16")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + id + "')]").isNotEmpty());
        mockMvc.perform(get("/api/v1/classes?to=2026-11-16")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + id + "')]").isNotEmpty());

        mockMvc.perform(post("/api/v1/classes/{id}/cancel", id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(post("/api/v1/classes/{id}/cancel", id).header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_CLASS_STATE"));
        mockMvc.perform(patch("/api/v1/classes/{id}", id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(schedule("2026-11-17", "17:00:00", "19:00:00")))
                .andExpect(status().isConflict());
    }

    @Test
    void teacherCannotCreateReadOrFilterOtherTeachersClasses() throws Exception {
        String adminToken = token(admin);
        String classId = createClass(adminToken, otherTeacher.id());
        String teacherToken = token(teacher);

        mockMvc.perform(post("/api/v1/classes")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest(otherTeacher.id())))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/classes/{id}", classId)
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/classes/{id}", classId)
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(schedule("2026-11-16", "17:00:00", "19:00:00")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/classes/{id}/cancel", classId)
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/classes?teacherId=" + otherTeacher.id())
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/classes")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + classId + "')]").isEmpty());

        mockMvc.perform(get("/api/v1/classes?teacherId=" + otherTeacher.id())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + classId + "')]").isNotEmpty());

        mockMvc.perform(patch("/api/v1/classes/{id}", classId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(schedule("2026-11-16", "17:00:00", "19:00:00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teacher.id").value(otherTeacher.id().toString()));
    }

    @Test
    void validatesScheduleTeacherAndMissingClass() throws Exception {
        String adminToken = token(admin);
        mockMvc.perform(post("/api/v1/classes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(schedule("2026-11-16", "18:00:00", "18:00:00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TEACHER"));

        mockMvc.perform(post("/api/v1/classes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest(teacher.id()).replace("19:00:00", "18:00:00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CLASS_SCHEDULE"));

        mockMvc.perform(get("/api/v1/classes?from=2026-11-17&to=2026-11-16")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/classes/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CLASS_NOT_FOUND"));

        userService.update(teacher.id(), teacher.name(), teacher.role(), false);
        mockMvc.perform(post("/api/v1/classes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest(teacher.id())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TEACHER"));
    }

    private String createClass(String token, UUID teacherId) throws Exception {
        String body = mockMvc.perform(post("/api/v1/classes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest(teacherId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private String createRequest(UUID teacherId) throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("date", "2026-11-15");
        request.put("startTime", "18:00:00");
        request.put("endTime", "19:00:00");
        if (teacherId != null) {
            request.put("teacherId", teacherId.toString());
        }
        return objectMapper.writeValueAsString(request);
    }

    private String schedule(String date, String start, String end) throws Exception {
        return objectMapper.writeValueAsString(Map.of("date", date, "startTime", start, "endTime", end));
    }

    private String token(User user) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", user.username(), "password", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }
}
