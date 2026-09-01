package com.himanshuProjects.disaster_damage_assessment_portal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.District;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.State;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.User;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.AccountStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.Gender;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.RoleType;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.user.DistrictRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.user.StateRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.user.UserRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.security.CustomUserDetailsService;
import com.himanshuProjects.disaster_damage_assessment_portal.security.JwtService;
import com.himanshuProjects.disaster_damage_assessment_portal.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.testcontainers.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class FullWorkflowIntegrationTest {

    private static final String PASSWORD = "password123";
    private static final AtomicLong SEQUENCE = new AtomicLong(System.nanoTime());

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private StateRepository stateRepository;

    @Autowired
    private DistrictRepository districtRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @MockitoSpyBean
    private EmailService emailService;

    private District district;
    private Long officerId;
    private String officerEmail;
    private String superAdminToken;
    private String districtAdminToken;
    private String officerToken;

    @BeforeEach
    void setUp() {
        doNothing().when(emailService).sendOtpEmail(anyString(), anyString(), anyString());
        doNothing().when(emailService).sendWelcomeEmail(anyString(), anyString());

        State state = new State();
        state.setName("State" + SEQUENCE.incrementAndGet());
        state.setCode("C" + (SEQUENCE.incrementAndGet() % 90 + 10));
        state = stateRepository.save(state);

        district = new District();
        district.setName("District" + SEQUENCE.incrementAndGet());
        district.setState(state);
        district = districtRepository.save(district);

        User superAdmin = createUser("super", RoleType.SUPER_ADMIN);
        User districtAdmin = createUser("distadmin", RoleType.DISTRICT_ADMIN);
        User officer = createUser("officer", RoleType.FIELD_OFFICER);

        officerId = officer.getId();
        officerEmail = officer.getEmail();
        superAdminToken = tokenFor(superAdmin.getEmail());
        districtAdminToken = tokenFor(districtAdmin.getEmail());
        officerToken = tokenFor(officer.getEmail());
    }

    @Test
    @DisplayName("Full workflow: register -> OTP -> login -> report -> assignment -> assessment -> compensation -> notification -> feedback")
    void fullWorkflowHappyPath() throws Exception {
        String citizenEmail = "citizen" + SEQUENCE.incrementAndGet() + "@example.com";

        // 1. REGISTER a new citizen (OTP email is mocked, real OtpServiceImpl stores the OTP)
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", "Citizen One",
                                "email", citizenEmail,
                                "phoneNumber", uniquePhone(),
                                "password", PASSWORD,
                                "gender", "FEMALE",
                                "districtId", district.getId()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(citizenEmail))
                .andExpect(jsonPath("$.role").value("CITIZEN"))
                .andExpect(jsonPath("$.accountStatus").value("PENDING_VERIFICATION"))
                .andExpect(jsonPath("$.token").isNotEmpty());

        // 2. Capture the OTP that the mocked email service was asked to send, then verify it
        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendOtpEmail(eq(citizenEmail), otpCaptor.capture(), anyString());

        mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", citizenEmail,
                                "otp", otpCaptor.getValue()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(containsString("verified")));

        // 3. LOGIN and receive a real JWT for the citizen
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", citizenEmail,
                                "password", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();
        String citizenToken = readString(loginResult, "token");

        // 4. CREATE a disaster report as the citizen
        String reportTitle = "Flood in area " + SEQUENCE.incrementAndGet();
        MvcResult reportResult = mockMvc.perform(post("/api/reports")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", reportTitle,
                                "description", "Severe flood inundated the locality",
                                "disasterType", "FLOOD",
                                "incidentAddress", "MG Road, Ludhiana",
                                "latitude", 30.90,
                                "longitude", 75.85))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.citizenEmail").value(citizenEmail))
                .andReturn();
        long reportId = readLong(reportResult, "id");

        // 5. NEGATIVE CHECK: a CITIZEN must be forbidden from assigning an officer
        mockMvc.perform(post("/api/assignments/report/{reportId}", reportId)
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignBody(officerId)))
                .andExpect(status().isForbidden());

        // 6. ASSIGN the field officer as a DISTRICT_ADMIN
        MvcResult assignmentResult = mockMvc.perform(post("/api/assignments/report/{reportId}", reportId)
                        .header("Authorization", "Bearer " + districtAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignBody(officerId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assignmentStatus").value("ASSIGNED"))
                .andExpect(jsonPath("$.reportStatus").value("ASSIGNED"))
                .andExpect(jsonPath("$.officerEmail").value(officerEmail))
                .andReturn();
        long assignmentId = readLong(assignmentResult, "id");

        // 7. FIELD_OFFICER accepts the assignment -> report goes UNDER_INSPECTION
        mockMvc.perform(patch("/api/assignments/{id}/status", assignmentId)
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "assignmentStatus", "ACCEPTED",
                                "notes", "On the way"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignmentStatus").value("ACCEPTED"))
                .andExpect(jsonPath("$.reportStatus").value("UNDER_INSPECTION"));

        // 8. FIELD_OFFICER submits the damage assessment -> report goes UNDER_REVIEW
        MvcResult assessmentResult = mockMvc.perform(post("/api/assessments/report/{reportId}", reportId)
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "damageLevel", "HIGH",
                                "estimatedLoss", 250000.50,
                                "assessmentNotes", "Extensive structural damage observed",
                                "recommendation", "Eligible for compensation"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.damageLevel").value("HIGH"))
                .andExpect(jsonPath("$.reportStatus").value("UNDER_REVIEW"))
                .andReturn();
        long assessmentId = readLong(assessmentResult, "id");

        // 9. DISTRICT_ADMIN creates a compensation -> report goes APPROVED
        MvcResult compensationResult = mockMvc.perform(post("/api/compensations")
                        .header("Authorization", "Bearer " + districtAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "damageAssessmentId", assessmentId,
                                "approvedAmount", 250000.50,
                                "remarks", "Compensation for flood damage"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.compensationStatus").value("PENDING"))
                .andReturn();
        long compensationId = readLong(compensationResult, "id");

        // 10. SUPER_ADMIN approves the compensation
        mockMvc.perform(patch("/api/compensations/{id}/approve", compensationId)
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "remarks", "Approved"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.compensationStatus").value("APPROVED"))
                .andExpect(jsonPath("$.reportStatus").value("APPROVED"));

        // 11. DISTRICT_ADMIN marks the payment as COMPLETED -> compensation PAID, report COMPLETED
        mockMvc.perform(patch("/api/compensations/{id}/payment-status", compensationId)
                        .header("Authorization", "Bearer " + districtAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "paymentStatus", "COMPLETED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.compensationStatus").value("PAID"))
                .andExpect(jsonPath("$.paymentStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.reportStatus").value("COMPLETED"));

        // 12. NOTIFICATION: the citizen must have received the COMPENSATION_APPROVED notification
        MvcResult notificationsResult = mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(1)))
                .andReturn();
        JsonNode notifications = readNode(notificationsResult).get("notifications");
        boolean sawCompensationApproved = false;
        for (JsonNode notification : notifications) {
            if ("COMPENSATION_APPROVED".equals(notification.get("notificationType").asText())) {
                sawCompensationApproved = true;
                break;
            }
        }
        if (!sawCompensationApproved) {
            throw new AssertionError("Expected a COMPENSATION_APPROVED notification for citizen, but none was found");
        }

        // 13. CITIZEN submits FEEDBACK for the completed report
        MvcResult feedbackResult = mockMvc.perform(post("/api/feedbacks")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "disasterReportId", reportId,
                                "rating", 5,
                                "comments", "Very helpful and responsive"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.reportTitle").value(reportTitle))
                .andReturn();
        long feedbackId = readLong(feedbackResult, "id");

        // 14. Feedback is persisted and retrievable by the citizen
        mockMvc.perform(get("/api/feedbacks/{id}", feedbackId)
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.userEmail").value(citizenEmail));
    }

    private String assignBody(Long officerId) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "fieldOfficerId", officerId,
                "inspectionDate", LocalDateTime.now().plusDays(2).truncatedTo(ChronoUnit.SECONDS).toString(),
                "notes", "Initial inspection"));
    }

    private User createUser(String prefix, RoleType role) {
        User user = new User();
        user.setFullName(prefix + " " + SEQUENCE.incrementAndGet());
        user.setEmail(prefix + SEQUENCE.incrementAndGet() + "@example.com");
        user.setPhoneNumber(uniquePhone());
        user.setPassword(passwordEncoder.encode(PASSWORD));
        user.setGender(Gender.MALE);
        user.setRole(role);
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setDistrict(district);
        return userRepository.save(user);
    }

    private String uniquePhone() {
        return "9" + String.format("%09d", Math.abs(SEQUENCE.incrementAndGet() % 1_000_000_000L));
    }

    private String tokenFor(String email) {
        return jwtService.generateToken(userDetailsService.loadUserByUsername(email));
    }

    private long readLong(MvcResult result, String field) throws Exception {
        return readNode(result).get(field).asLong();
    }

    private String readString(MvcResult result, String field) throws Exception {
        return readNode(result).get(field).asText();
    }

    private JsonNode readNode(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}