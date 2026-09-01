package com.himanshuProjects.disaster_damage_assessment_portal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.himanshuProjects.disaster_damage_assessment_portal.controller.assessment.DamageAssessmentController;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.assessment.AddInspectionImagesRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.assessment.CreateDamageAssessmentRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.assessment.DamageAssessmentPageResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.assessment.DamageAssessmentResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.assessment.UpdateDamageAssessmentRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.DamageLevel;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.DisasterType;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.ReportStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.exception.GlobalExceptionHandler;
import com.himanshuProjects.disaster_damage_assessment_portal.service.assessment.DamageAssessmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DamageAssessmentControllerTest {

    private ObjectMapper objectMapper;
    private DamageAssessmentService assessmentService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        assessmentService = mock(DamageAssessmentService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new DamageAssessmentController(assessmentService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static RequestPostProcessor authenticatedOfficer(String email) {
        return request -> {
            request.setUserPrincipal(new UsernamePasswordAuthenticationToken(
                    email, "password", List.of(new SimpleGrantedAuthority("ROLE_FIELD_OFFICER"))));
            return request;
        };
    }

    private DamageAssessmentResponse assessmentResponse() {
        return DamageAssessmentResponse.builder()
                .id(1L)
                .damageLevel(DamageLevel.SEVERE)
                .recommendation("Recommended for compensation")
                .reportId(3L)
                .reportTitle("Flood report")
                .disasterType(DisasterType.FLOOD)
                .reportStatus(ReportStatus.UNDER_INSPECTION)
                .officerId(2L)
                .officerName("Officer Test")
                .officerEmail("officer@example.com")
                .citizenName("John Doe")
                .citizenEmail("citizen@example.com")
                .build();
    }

    private DamageAssessmentPageResponse assessmentPage() {
        return DamageAssessmentPageResponse.builder()
                .assessments(List.of(assessmentResponse()))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();
    }

    @Test
    @DisplayName("should submit an assessment and return 201")
    void shouldSubmitAssessment() throws Exception {
        when(assessmentService.submitAssessment(eq(3L), eq("officer@example.com"),
                any(CreateDamageAssessmentRequest.class))).thenReturn(assessmentResponse());

        String body = """
                {"damageLevel":"SEVERE","estimatedLoss":150000.50,"assessmentNotes":"Significant damage",
                 "recommendation":"Recommended for compensation"}
                """;

        mockMvc.perform(post("/api/assessments/report/3")
                        .with(authenticatedOfficer("officer@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.damageLevel").value("SEVERE"))
                .andExpect(jsonPath("$.officerEmail").value("officer@example.com"));
    }

    @Test
    @DisplayName("should get assessment by id")
    void shouldGetAssessmentById() throws Exception {
        when(assessmentService.getAssessmentById(1L)).thenReturn(assessmentResponse());

        mockMvc.perform(get("/api/assessments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.reportTitle").value("Flood report"));
    }

    @Test
    @DisplayName("should get assessment by report id")
    void shouldGetAssessmentByReportId() throws Exception {
        when(assessmentService.getAssessmentByReportId(3L)).thenReturn(assessmentResponse());

        mockMvc.perform(get("/api/assessments/report/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportId").value(3));
    }

    @Test
    @DisplayName("should update an assessment")
    void shouldUpdateAssessment() throws Exception {
        when(assessmentService.updateAssessment(eq(1L), eq("officer@example.com"),
                any(UpdateDamageAssessmentRequest.class))).thenReturn(assessmentResponse());

        String body = """
                {"damageLevel":"SEVERE","estimatedLoss":160000.00,"assessmentNotes":"Updated notes",
                 "recommendation":"Recommended"}
                """;

        mockMvc.perform(put("/api/assessments/1")
                        .with(authenticatedOfficer("officer@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.damageLevel").value("SEVERE"));
    }

    @Test
    @DisplayName("should search assessments")
    void shouldSearchAssessments() throws Exception {
        when(assessmentService.searchAssessments(any(), any(), any(), anyInt(), anyInt(),
                anyString(), anyString())).thenReturn(assessmentPage());

        mockMvc.perform(get("/api/assessments")
                        .param("search", "flood")
                        .param("damageLevel", "SEVERE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.assessments[0].damageLevel").value("SEVERE"));
    }

    @Test
    @DisplayName("should get my assessments")
    void shouldGetMyAssessments() throws Exception {
        when(assessmentService.getMyAssessments(eq("officer@example.com"), anyInt(), anyInt(),
                anyString(), anyString())).thenReturn(assessmentPage());

        mockMvc.perform(get("/api/assessments/my")
                        .with(authenticatedOfficer("officer@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assessments[0].officerEmail").value("officer@example.com"));
    }

    @Test
    @DisplayName("should add inspection images to an assessment")
    void shouldAddImages() throws Exception {
        when(assessmentService.addImages(eq(1L), eq("officer@example.com"), any(AddInspectionImagesRequest.class)))
                .thenReturn(assessmentResponse());

        String body = "{\"imageUrls\":[\"http://example.com/insp1.jpg\"]}";

        mockMvc.perform(post("/api/assessments/1/images")
                        .with(authenticatedOfficer("officer@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportTitle").value("Flood report"));
    }

    @Test
    @DisplayName("should remove an inspection image and return 204")
    void shouldRemoveImage() throws Exception {
        doNothing().when(assessmentService).removeImage(eq(1L), anyLong(), eq("officer@example.com"));

        mockMvc.perform(delete("/api/assessments/1/images/5")
                        .with(authenticatedOfficer("officer@example.com")))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("should delete an assessment and return 204")
    void shouldDeleteAssessment() throws Exception {
        doNothing().when(assessmentService).deleteAssessment(eq(1L), eq("officer@example.com"));

        mockMvc.perform(delete("/api/assessments/1")
                        .with(authenticatedOfficer("officer@example.com")))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("should return 400 when submitting an invalid assessment")
    void shouldReturn400ForInvalidSubmitPayload() throws Exception {
        String body = "{\"damageLevel\":\"SEVERE\",\"estimatedLoss\":-5,\"assessmentNotes\":\"\",\"recommendation\":\"\"}";

        mockMvc.perform(post("/api/assessments/report/3")
                        .with(authenticatedOfficer("officer@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}