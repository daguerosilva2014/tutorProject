package com.academia.controller;

import com.academia.model.AcademyClass;
import com.academia.model.Student;
import com.academia.model.StudentStatus;
import com.academia.model.Subject;
import com.academia.model.SubjectStatus;
import com.academia.model.User;
import com.academia.model.UserRole;
import com.academia.service.AcademyClassService;
import com.academia.service.StudentService;
import com.academia.service.SubjectService;
import com.academia.service.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClassParticipationApiTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserService userService;
    @Autowired StudentService studentService;
    @Autowired SubjectService subjectService;
    @Autowired AcademyClassService classService;
    @Autowired JdbcTemplate jdbcTemplate;

    private User admin;
    private User teacher;
    private User otherTeacher;
    private AcademyClass academyClass;
    private Student student;
    private Subject math;
    private Subject physics;

    @BeforeEach
    void createData() {
        String suffix = UUID.randomUUID().toString();
        admin = userService.create("Admin", "participation.admin." + suffix, "Password123!", UserRole.ADMIN);
        teacher = userService.create("Teacher", "participation.teacher." + suffix, "Password123!", UserRole.TEACHER);
        otherTeacher = userService.create("Other", "participation.other." + suffix, "Password123!", UserRole.TEACHER);
        academyClass = classService.create(teacher.username(), null,
                LocalDate.of(2026, 11, 15), LocalTime.of(18, 0), LocalTime.of(19, 0), null);
        student = studentService.create("Student E4", null, null);
        math = subjectService.create("Math E4", null);
        physics = subjectService.create("Physics E4", null);
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/classes/{id}/participants", academyClass.id()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void assignsZeroOneAndManySubjectsThenReplacesAndClearsThem() throws Exception {
        String token = token(teacher);
        String firstId = assign(token, student.id(), null);
        Student second = studentService.create("Second E4", null, null);
        assign(token, second.id(), List.of(math.id()));
        Student third = studentService.create("Third E4", null, null);
        assign(token, third.id(), List.of(math.id(), physics.id()));

        String listBody = mockMvc.perform(get("/api/v1/classes/{id}/participants", academyClass.id())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode participants = objectMapper.readTree(listBody);
        assertEquals(3, participants.size());
        for (JsonNode participant : participants) {
            if (participant.get("id").asText().equals(firstId)) {
                assertEquals(0, participant.get("subjects").size());
            }
            if (participant.get("student").get("id").asText().equals(third.id().toString())) {
                assertEquals(2, participant.get("subjects").size());
            }
        }

        mockMvc.perform(patch("/api/v1/class-participations/{id}", firstId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subjects(List.of(math.id(), physics.id()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subjects.length()").value(2));

        mockMvc.perform(patch("/api/v1/class-participations/{id}", firstId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subjects(List.of())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subjects").isEmpty());
    }

    @Test
    void rejectsDuplicatesAndDatabaseAlsoEnforcesUniquePair() throws Exception {
        String token = token(teacher);
        assign(token, student.id(), List.of());

        mockMvc.perform(post("/api/v1/classes/{id}/participants", academyClass.id())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest(student.id(), List.of())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_PARTICIPATION"));

        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                "INSERT INTO class_participations (id, class_id, student_id) VALUES (?, ?, ?)",
                UUID.randomUUID(), academyClass.id(), student.id()));
    }

    @Test
    void rejectsInactiveStudentAndNewInactiveSubjectButKeepsHistoricalSubject() throws Exception {
        String token = token(teacher);
        String id = assign(token, student.id(), List.of(math.id()));
        subjectService.changeStatus(math.id(), SubjectStatus.INACTIVE);
        subjectService.changeStatus(physics.id(), SubjectStatus.INACTIVE);

        mockMvc.perform(patch("/api/v1/class-participations/{id}", id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subjects(List.of(math.id()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subjects[0].id").value(math.id().toString()));

        mockMvc.perform(patch("/api/v1/class-participations/{id}", id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subjects(List.of(physics.id()))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INACTIVE_SUBJECT"));

        Student inactive = studentService.create("Inactive E4", null, null);
        studentService.changeStatus(inactive.id(), StudentStatus.INACTIVE);
        mockMvc.perform(post("/api/v1/classes/{id}/participants", academyClass.id())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest(inactive.id(), List.of())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INACTIVE_STUDENT"));
    }

    @Test
    void enforcesClassOwnershipAndRejectsCancelledClass() throws Exception {
        String teacherToken = token(teacher);
        String otherToken = token(otherTeacher);
        String adminToken = token(admin);
        String id = assign(teacherToken, student.id(), List.of());

        mockMvc.perform(get("/api/v1/classes/{id}/participants", academyClass.id())
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/classes/{id}/participants", academyClass.id())
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest(student.id(), List.of())))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/v1/class-participations/{id}", id)
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subjects(List.of())))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/classes/{id}/participants", academyClass.id())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id));
        mockMvc.perform(patch("/api/v1/class-participations/{id}", id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subjects(List.of(math.id()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subjects[0].id").value(math.id().toString()));

        classService.cancel(teacher.username(), academyClass.id());
        mockMvc.perform(get("/api/v1/classes/{id}/participants", academyClass.id())
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id));
        mockMvc.perform(post("/api/v1/classes/{id}/participants", academyClass.id())
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest(UUID.randomUUID(), List.of())))
                .andExpect(status().isConflict());
        mockMvc.perform(patch("/api/v1/class-participations/{id}", id)
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subjects(List.of())))
                .andExpect(status().isConflict());
    }

    @Test
    void validatesSubjectsAndMissingReferences() throws Exception {
        String token = token(teacher);
        mockMvc.perform(post("/api/v1/classes/{id}/participants", academyClass.id())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest(student.id(), List.of(math.id(), math.id()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DUPLICATE_SUBJECT"));

        mockMvc.perform(post("/api/v1/classes/{id}/participants", academyClass.id())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest(student.id(), List.of(UUID.randomUUID()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SUBJECT_NOT_FOUND"));

        mockMvc.perform(post("/api/v1/classes/{id}/participants", academyClass.id())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(patch("/api/v1/class-participations/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subjects(List.of())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PARTICIPATION_NOT_FOUND"));

        String id = assign(token, student.id(), List.of());
        mockMvc.perform(patch("/api/v1/class-participations/{id}", id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    private String assign(String token, UUID studentId, List<UUID> subjectIds) throws Exception {
        String body = mockMvc.perform(post("/api/v1/classes/{id}/participants", academyClass.id())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest(studentId, subjectIds)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private String createRequest(UUID studentId, List<UUID> subjectIds) throws Exception {
        if (subjectIds == null) {
            return objectMapper.writeValueAsString(Map.of("studentId", studentId));
        }
        return objectMapper.writeValueAsString(Map.of("studentId", studentId, "subjectIds", subjectIds));
    }

    private String subjects(List<UUID> subjectIds) throws Exception {
        return objectMapper.writeValueAsString(Map.of("subjectIds", subjectIds));
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
