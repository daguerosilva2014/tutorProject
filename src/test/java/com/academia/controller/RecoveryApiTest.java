package com.academia.controller;

import com.academia.model.AcademyClass;
import com.academia.model.BillingMode;
import com.academia.model.ClassPackage;
import com.academia.model.Money;
import com.academia.model.Student;
import com.academia.model.User;
import com.academia.model.UserRole;
import com.academia.service.AcademyClassService;
import com.academia.service.AttendanceService;
import com.academia.service.ClassClosureService;
import com.academia.service.ClassPackageService;
import com.academia.service.ClassParticipationService;
import com.academia.service.StudentService;
import com.academia.service.UserService;
import com.academia.model.AttendanceMark;
import com.academia.repository.ClassParticipationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecoveryApiTest {
    private static final Instant INITIAL_TIME = Instant.parse("2026-11-09T15:00:00Z");

    @TestConfiguration
    static class TestTimeConfig {
        @Bean @Primary MutableClock mutableClock() { return new MutableClock(); }
    }

    static class MutableClock extends Clock {
        private volatile Instant current = INITIAL_TIME;
        private final ZoneId zone = ZoneId.of("America/Argentina/Buenos_Aires");
        void set(Instant instant) { current = instant; }
        @Override public ZoneId getZone() { return zone; }
        @Override public Clock withZone(ZoneId zone) { return Clock.fixed(current, zone); }
        @Override public Instant instant() { return current; }
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;
    @Autowired MutableClock clock;
    @Autowired UserService users;
    @Autowired StudentService students;
    @Autowired AcademyClassService classes;
    @Autowired ClassParticipationService participations;
    @Autowired AttendanceService attendance;
    @Autowired ClassClosureService closure;
    @Autowired ClassPackageService packages;
    @Autowired ClassParticipationRepository participationRepository;

    private User teacher;
    private User otherTeacher;
    private Student student;
    private String token;

    @BeforeEach
    void setup() throws Exception {
        clock.set(INITIAL_TIME);
        String suffix = UUID.randomUUID().toString();
        teacher = users.create("Teacher", "recovery.teacher." + suffix, "Password123!", UserRole.TEACHER);
        otherTeacher = users.create("Other", "recovery.other." + suffix, "Password123!", UserRole.TEACHER);
        student = students.create("Recovery student " + suffix, null, null);
        token = token(teacher);
    }

    @Test
    void recoveredPackageClassConsumesOneRemainingClassOnlyOnce() throws Exception {
        ClassPackage classPackage = packages.create(student.id(), 1, new Money(new BigDecimal("1000")),
                LocalDate.of(2026, 11, 9));
        UUID originalId = justifiedOriginal(BillingMode.PACKAGE);
        AcademyClass target = futureClass(teacher);

        String first = recover(originalId, target.id(), token, 201);
        assertEquals("RECOVERED", mapper.readTree(first).get("status").asText());
        UUID targetParticipationId = UUID.fromString(mapper.readTree(first).get("targetParticipationId").asText());
        String repeated = recover(originalId, target.id(), token, 201);
        assertEquals(targetParticipationId.toString(), mapper.readTree(repeated).get("targetParticipationId").asText());
        assertEquals(0, packages.findById(classPackage.id()).usedClasses());
        assertEquals(targetParticipationId.toString(), participant(originalId).get("recoveryTargetId").asText());

        clock.set(Instant.parse("2026-11-13T15:00:00Z"));
        closure.close(target.id());
        closure.close(target.id());
        assertEquals(1, packages.findById(classPackage.id()).usedClasses());
        assertEquals("CONSUMED", participant(targetParticipationId).get("consumptionStatus").asText());
    }

    @Test
    void recoveredIndividualClassChargesOnceWhenItCloses() throws Exception {
        UUID originalId = justifiedOriginal(BillingMode.PER_CLASS);
        AcademyClass target = futureClass(teacher);
        String json = recover(originalId, target.id(), token, 201);
        UUID targetId = UUID.fromString(mapper.readTree(json).get("targetParticipationId").asText());
        assertEquals(0, participant(originalId).get("outstandingAmount").decimalValue().compareTo(BigDecimal.ZERO));
        clock.set(Instant.parse("2026-11-13T15:00:00Z"));
        closure.close(target.id());
        assertEquals(0, participant(targetId).get("outstandingAmount").decimalValue()
                .compareTo(new BigDecimal("900.00")));
    }

    @Test
    void rejectsSecondRecoveryAndExistingTargetParticipant() throws Exception {
        UUID originalId = justifiedOriginal(BillingMode.PER_CLASS);
        AcademyClass target = futureClass(teacher);
        recover(originalId, target.id(), token, 201);
        AcademyClass another = futureClass(teacher);
        mockMvc.perform(post("/api/v1/class-participations/{id}/recovery", originalId)
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content(request(another.id())))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_RECOVERY"));

        UUID secondOriginal = justifiedOriginal(BillingMode.PER_CLASS);
        mockMvc.perform(post("/api/v1/class-participations/{id}/recovery", secondOriginal)
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content(request(target.id())))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("DUPLICATE_PARTICIPATION"));
    }

    @Test
    void rejectsWrongTeacherCancelledTargetAndNonJustifiedSource() throws Exception {
        UUID originalId = justifiedOriginal(BillingMode.PER_CLASS);
        AcademyClass foreign = futureClass(otherTeacher);
        mockMvc.perform(post("/api/v1/class-participations/{id}/recovery", originalId)
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content(request(foreign.id())))
                .andExpect(status().isForbidden());

        AcademyClass cancelled = futureClass(teacher);
        classes.cancel(teacher.username(), cancelled.id());
        mockMvc.perform(post("/api/v1/class-participations/{id}/recovery", originalId)
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content(request(cancelled.id())))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_RECOVERY"));

        AcademyClass unrelated = futureClass(teacher);
        UUID presentId = participations.create(teacher.username(), unrelated.id(), student.id(), List.of(),
                BillingMode.PER_CLASS, new BigDecimal("900")).id();
        mockMvc.perform(post("/api/v1/class-participations/{id}/recovery", presentId)
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content(request(futureClass(teacher).id())))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_RECOVERY"));
    }

    @Test
    void cancelledRecoveryCanBeAssignedToAnotherFutureClass() throws Exception {
        UUID originalId = justifiedOriginal(BillingMode.PER_CLASS);
        AcademyClass firstTarget = futureClass(teacher);
        recover(originalId, firstTarget.id(), token, 201);
        classes.cancel(teacher.username(), firstTarget.id());

        AcademyClass replacement = futureClass(teacher);
        String json = recover(originalId, replacement.id(), token, 201);
        assertEquals(replacement.id().toString(),
                participationRepository.findById(UUID.fromString(mapper.readTree(json)
                        .get("targetParticipationId").asText())).orElseThrow().academyClass().id().toString());
    }

    @Test
    void rejectsTargetThatAlreadyStarted() throws Exception {
        UUID originalId = justifiedOriginal(BillingMode.PER_CLASS);
        AcademyClass pastTarget = classes.create(teacher.username(), null, LocalDate.of(2026, 11, 11),
                LocalTime.of(10, 0), LocalTime.of(11, 0), null);
        mockMvc.perform(post("/api/v1/class-participations/{id}/recovery", originalId)
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content(request(pastTarget.id())))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_RECOVERY"));
    }

    private UUID justifiedOriginal(BillingMode mode) {
        clock.set(INITIAL_TIME);
        AcademyClass originalClass = classes.create(teacher.username(), null, LocalDate.of(2026, 11, 10),
                LocalTime.of(10, 0), LocalTime.of(11, 0), null);
        UUID id = participations.create(teacher.username(), originalClass.id(), student.id(), List.of(),
                mode, mode == BillingMode.PER_CLASS ? new BigDecimal("900") : null).id();
        attendance.record(teacher.username(), id, AttendanceMark.ABSENT);
        clock.set(Instant.parse("2026-11-11T15:00:00Z"));
        closure.close(originalClass.id());
        return id;
    }

    private AcademyClass futureClass(User owner) {
        return classes.create(owner.username(), null, LocalDate.of(2026, 11, 12),
                LocalTime.of(10, 0), LocalTime.of(11, 0), null);
    }

    private String recover(UUID originalId, UUID targetClassId, String bearer, int expectedStatus) throws Exception {
        return mockMvc.perform(post("/api/v1/class-participations/{id}/recovery", originalId)
                        .header("Authorization", "Bearer " + bearer).contentType(MediaType.APPLICATION_JSON)
                        .content(request(targetClassId)))
                .andExpect(status().is(expectedStatus)).andReturn().getResponse().getContentAsString();
    }

    private String request(UUID targetClassId) throws Exception {
        return mapper.writeValueAsString(Map.of("targetClassId", targetClassId));
    }

    private JsonNode participant(UUID id) throws Exception {
        UUID classId = participationRepository.findById(id).orElseThrow().academyClass().id();
        String json = mockMvc.perform(get("/api/v1/classes/{id}/participants", classId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        for (JsonNode node : mapper.readTree(json)) {
            if (node.get("id").asText().equals(id.toString())) return node;
        }
        throw new AssertionError("Participation not found");
    }

    private String token(User user) throws Exception {
        String json = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("username", user.username(), "password", "Password123!"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(json).get("accessToken").asText();
    }
}
