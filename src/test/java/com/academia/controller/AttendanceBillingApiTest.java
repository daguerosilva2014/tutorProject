package com.academia.controller;

import com.academia.model.AcademyClass;
import com.academia.model.ClassPackage;
import com.academia.model.Money;
import com.academia.model.Student;
import com.academia.model.User;
import com.academia.model.UserRole;
import com.academia.exception.NoPackageBalanceException;
import com.academia.repository.ClassConsumptionRepository;
import com.academia.service.AcademyClassService;
import com.academia.service.ClassClosureScheduler;
import com.academia.service.ClassClosureService;
import com.academia.service.ClassPackageService;
import com.academia.service.StudentService;
import com.academia.service.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AttendanceBillingApiTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 9);

    @TestConfiguration
    static class FixedTime {
        @Bean @Primary Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-11-09T15:00:00Z"), ZoneId.of("America/Argentina/Buenos_Aires"));
        }
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserService users;
    @Autowired StudentService students;
    @Autowired AcademyClassService classes;
    @Autowired ClassPackageService packages;
    @Autowired ClassClosureService closure;
    @Autowired ClassClosureScheduler scheduler;
    @Autowired ClassConsumptionRepository consumptions;

    private User teacher;
    private User otherTeacher;
    private Student student;
    private String token;

    @BeforeEach
    void setup() throws Exception {
        String suffix = UUID.randomUUID().toString();
        teacher = users.create("Teacher", "billing.teacher." + suffix, "Password123!", UserRole.TEACHER);
        otherTeacher = users.create("Other", "billing.other." + suffix, "Password123!", UserRole.TEACHER);
        student = students.create("Billing student " + suffix, null, null);
        token = token(teacher);
    }

    @Test
    void packageIsIndependentAndOldestPackageIsConsumedOnlyOnce() throws Exception {
        ClassPackage oldest = packages.create(student.id(), 1, money("1000"), TODAY.minusDays(2));
        ClassPackage newer = packages.create(student.id(), 2, money("2000"), TODAY.minusDays(1));
        AcademyClass first = pastClass();
        UUID firstId = assign(first, "PACKAGE", null);
        long initialConsumptions = consumptions.count();
        scheduler.closeDueClasses();
        closure.close(first.id());
        assertEquals(1, packages.findById(oldest.id()).usedClasses());
        assertEquals(0, packages.findById(newer.id()).usedClasses());
        assertEquals(initialConsumptions + 1, consumptions.count());
        assertEquals("CONSUMED", participant(first, firstId).get("consumptionStatus").asText());
        assertEquals(oldest.id().toString(), participant(first, firstId).get("consumedPackageId").asText());

        AcademyClass second = pastClass();
        assign(second, "PACKAGE", null);
        closure.close(second.id());
        assertEquals(1, packages.findById(newer.id()).usedClasses());
        assertEquals(initialConsumptions + 2, consumptions.count());
    }

    @Test
    void perClassCreatesDebtAndAcceptsPartialPayments() throws Exception {
        AcademyClass academyClass = pastClass();
        UUID id = assign(academyClass, "PER_CLASS", "1500.00");
        closure.close(academyClass.id());
        assertEquals("ABSENT", participant(academyClass, id).get("attendanceStatus").asText());
        assertEquals("NOT_APPLICABLE", participant(academyClass, id).get("consumptionStatus").asText());
        assertEquals(0, new BigDecimal("1500.00").compareTo(participant(academyClass, id).get("outstandingAmount").decimalValue()));

        String payment = mapper.writeValueAsString(Map.of("amount", "500.00", "paidAt", TODAY.toString()));
        mockMvc.perform(post("/api/v1/class-participations/{id}/payments", id)
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(payment))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.outstandingAmount").value(1000.0));
        assertEquals(0, new BigDecimal("1000.00").compareTo(participant(academyClass, id).get("outstandingAmount").decimalValue()));
        mockMvc.perform(post("/api/v1/class-participations/{id}/payments", id)
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(payment))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/class-participations/{id}/payments", id)
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(payment))
                .andExpect(status().isCreated());
        assertEquals(0, BigDecimal.ZERO.compareTo(participant(academyClass, id).get("outstandingAmount").decimalValue()));
        mockMvc.perform(post("/api/v1/class-participations/{id}/payments", id)
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(payment))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PAYMENT_EXCEEDS_BALANCE"));
    }

    @Test
    void earlyAbsenceAvoidsConsumptionAndPaymentAndCannotBeChanged() throws Exception {
        AcademyClass academyClass = classes.create(teacher.username(), null, TODAY.plusDays(1),
                LocalTime.of(10, 0), LocalTime.of(11, 0), null);
        UUID id = assign(academyClass, "PER_CLASS", "900.00");
        mockMvc.perform(put("/api/v1/class-participations/{id}/attendance", id)
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ABSENT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attendanceStatus").value("JUSTIFIED_ABSENCE"));
        assertEquals("2026-11-09T15:00:00Z", participant(academyClass, id).get("absenceNotifiedAt").asText());
        mockMvc.perform(put("/api/v1/class-participations/{id}/attendance", id)
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ABSENT\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/class-participations/{id}/attendance", id)
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PRESENT\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ATTENDANCE_CONFLICT"));
    }

    @Test
    void packageAssignmentNeedsBalanceAndTeacherCannotEditOtherClass() throws Exception {
        AcademyClass academyClass = pastClass();
        String request = mapper.writeValueAsString(Map.of("studentId", student.id(), "billingMode", "PACKAGE"));
        mockMvc.perform(post("/api/v1/classes/{id}/participants", academyClass.id())
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("NO_PACKAGE_BALANCE"));
        UUID id = assign(academyClass, "PER_CLASS", "100.00");
        mockMvc.perform(put("/api/v1/class-participations/{id}/attendance", id)
                        .header("Authorization", "Bearer " + token(otherTeacher)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PRESENT\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/class-participations/{id}/payments", id)
                        .header("Authorization", "Bearer " + token(otherTeacher)))
                .andExpect(status().isForbidden());
    }

    @Test
    void absenceThresholdIncludesExactlyTwelveHours() throws Exception {
        AcademyClass exactly = classes.create(teacher.username(), null, TODAY.plusDays(1),
                LocalTime.MIDNIGHT, LocalTime.of(1, 0), null);
        AcademyClass late = classes.create(teacher.username(), null, TODAY,
                LocalTime.of(23, 59), LocalTime.of(23, 59, 59), null);
        UUID earlyId = assign(exactly, "PER_CLASS", "100.00");
        UUID lateId = assign(late, "PER_CLASS", "100.00");
        for (UUID id : new UUID[]{earlyId, lateId}) {
            mockMvc.perform(put("/api/v1/class-participations/{id}/attendance", id)
                            .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"ABSENT\"}"))
                    .andExpect(status().isOk());
        }
        assertEquals("JUSTIFIED_ABSENCE", participant(exactly, earlyId).get("attendanceStatus").asText());
        assertEquals("ABSENT", participant(late, lateId).get("attendanceStatus").asText());
    }

    @Test
    void missingBalanceRollsBackClosureAndCancelledClassDoesNotConsume() throws Exception {
        ClassPackage classPackage = packages.create(student.id(), 1, money("100"), TODAY);
        AcademyClass first = pastClass();
        AcademyClass second = pastClass();
        AcademyClass cancelled = pastClass();
        assign(first, "PACKAGE", null);
        assign(second, "PACKAGE", null);
        assign(cancelled, "PACKAGE", null);
        classes.cancel(teacher.username(), cancelled.id());
        closure.close(first.id());
        assertThrows(NoPackageBalanceException.class, () -> closure.close(second.id()));
        closure.close(cancelled.id());
        assertEquals("SCHEDULED", classes.findById(teacher.username(), second.id()).status().name());
        assertEquals("CANCELLED", classes.findById(teacher.username(), cancelled.id()).status().name());
        assertEquals(1, packages.findById(classPackage.id()).usedClasses());
    }

    private AcademyClass pastClass() {
        return classes.create(teacher.username(), null, TODAY, LocalTime.of(10, 0), LocalTime.of(11, 0), null);
    }

    private UUID assign(AcademyClass academyClass, String mode, String price) throws Exception {
        var body = new java.util.HashMap<String, Object>();
        body.put("studentId", student.id());
        body.put("billingMode", mode);
        if (price != null) body.put("unitPrice", price);
        String json = mockMvc.perform(post("/api/v1/classes/{id}/participants", academyClass.id())
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(mapper.readTree(json).get("id").asText());
    }

    private JsonNode participant(AcademyClass academyClass, UUID id) throws Exception {
        String json = mockMvc.perform(get("/api/v1/classes/{id}/participants", academyClass.id())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        for (JsonNode node : mapper.readTree(json)) if (node.get("id").asText().equals(id.toString())) return node;
        throw new AssertionError("Participation not found");
    }

    private Money money(String value) { return new Money(new BigDecimal(value)); }

    private String token(User user) throws Exception {
        String json = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("username", user.username(), "password", "Password123!"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(json).get("accessToken").asText();
    }
}
