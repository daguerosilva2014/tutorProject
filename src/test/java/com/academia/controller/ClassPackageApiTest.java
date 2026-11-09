package com.academia.controller;

import com.academia.model.Student;
import com.academia.model.User;
import com.academia.model.UserRole;
import com.academia.service.StudentService;
import com.academia.service.UserService;
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

import java.util.Map;
import java.util.UUID;
import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClassPackageApiTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserService userService;
    @Autowired StudentService studentService;

    private User teacher;
    private Student student;

    @BeforeEach
    void createData() {
        teacher = userService.create("Package Teacher", "package.teacher." + UUID.randomUUID(),
                "Password123!", UserRole.TEACHER);
        student = studentService.create("Package Student", null, null);
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/students/{id}/packages", student.id()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createsAndFiltersSeveralPackages() throws Exception {
        String token = token();
        String firstId = createPackage(token, student.id(), "32000.00", 8);
        createPackage(token, student.id(), "0.00", 4);

        mockMvc.perform(get("/api/v1/packages/{id}", firstId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(student.id().toString()))
                .andExpect(jsonPath("$.usedClasses").value(0))
                .andExpect(jsonPath("$.remainingClasses").value(8))
                .andExpect(jsonPath("$.paymentStatus").value("PENDING"));

        mockMvc.perform(get("/api/v1/students/{id}/packages?usageStatus=PENDING&paymentStatus=PAID", student.id())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].price").value(0.00))
                .andExpect(jsonPath("$[0].currency").doesNotExist());
    }

    @Test
    void recordsPartialAndFullPaymentsAndRejectsOverpayment() throws Exception {
        String token = token();
        String packageId = createPackage(token, student.id(), "32000.00", 8);

        String first = mockMvc.perform(post("/api/v1/packages/{id}/payments", packageId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentRequest("12000.25")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentStatus").value("PARTIAL"))
                .andExpect(jsonPath("$.outstandingAmount").value(19999.75))
                .andExpect(jsonPath("$.currency").doesNotExist())
                .andExpect(jsonPath("$.registeredBy").value(teacher.id().toString()))
                .andReturn().getResponse().getContentAsString();
        String firstPaymentId = objectMapper.readTree(first).get("id").asText();

        mockMvc.perform(post("/api/v1/packages/{id}/payments", packageId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentRequest("20000.00")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PAYMENT_EXCEEDS_BALANCE"));

        mockMvc.perform(get("/api/v1/packages/{id}/payments", packageId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(firstPaymentId));

        mockMvc.perform(post("/api/v1/packages/{id}/payments", packageId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentRequest("19999.75")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentStatus").value("PAID"))
                .andExpect(jsonPath("$.outstandingAmount").value(0.00));

        mockMvc.perform(get("/api/v1/packages/{id}", packageId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paidAmount").value(32000.00))
                .andExpect(jsonPath("$.usageStatus").value("PENDING"));
    }

    @Test
    void validatesMoneyAndMissingResources() throws Exception {
        String token = token();
        String packageId = createPackage(token, student.id(), "0.00", 1);

        mockMvc.perform(post("/api/v1/packages/{id}/payments", packageId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentRequest("1.00")))
                .andExpect(status().isConflict());

        for (String amount : new String[] {"0.00", "-1.00", "1.001"}) {
            mockMvc.perform(post("/api/v1/packages/{id}/payments", packageId)
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(paymentRequest(amount)))
                    .andExpect(status().isBadRequest());
        }

        mockMvc.perform(post("/api/v1/students/{id}/packages", student.id())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"totalClasses\":8,\"price\":{\"amount\":10,\"currency\":\"USD\"},\"purchasedAt\":\"2026-11-09\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(post("/api/v1/students/{id}/packages", student.id())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(packageRequest("10.00", 0)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/packages/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PACKAGE_NOT_FOUND"));

        mockMvc.perform(get("/api/v1/students/{id}/packages", UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    private String createPackage(String token, UUID studentId, String amount, int totalClasses) throws Exception {
        String body = mockMvc.perform(post("/api/v1/students/{id}/packages", studentId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(packageRequest(amount, totalClasses)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private String packageRequest(String amount, int totalClasses) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "totalClasses", totalClasses, "price", new BigDecimal(amount),
                "purchasedAt", "2026-11-09"));
    }

    private String paymentRequest(String amount) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "amount", new BigDecimal(amount),
                "paidAt", "2026-11-09", "method", "CASH"));
    }

    private String token() throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", teacher.username(), "password", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }
}
