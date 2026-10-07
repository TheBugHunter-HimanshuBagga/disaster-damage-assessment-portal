package com.himanshuProjects.disaster_damage_assessment_portal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.himanshuProjects.disaster_damage_assessment_portal.controller.compensation.CompensationController;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.compensation.ApproveCompensationRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.compensation.CompensationHistoryResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.compensation.CompensationPageResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.compensation.CompensationResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.compensation.CreateCompensationRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.compensation.RejectCompensationRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.compensation.UpdateCompensationRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.compensation.UpdatePaymentStatusRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.CompensationStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.DamageLevel;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.PaymentStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.exception.GlobalExceptionHandler;
import com.himanshuProjects.disaster_damage_assessment_portal.service.compensation.CompensationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CompensationControllerTest {

    private ObjectMapper objectMapper;
    private CompensationService compensationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        compensationService = mock(CompensationService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CompensationController(compensationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static RequestPostProcessor authenticatedAdmin(String email) {
        return request -> {
            request.setUserPrincipal(new UsernamePasswordAuthenticationToken(
                    email, "password", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
            return request;
        };
    }

    private CompensationResponse compensationResponse() {
        return CompensationResponse.builder()
                .id(1L)
                .approvedAmount(new BigDecimal("100000.50"))
                .compensationStatus(CompensationStatus.PENDING)
                .paymentStatus(PaymentStatus.NOT_INITIATED)
                .damageAssessmentId(3L)
                .reportId(4L)
                .reportTitle("Flood report")
                .citizenEmail("citizen@example.com")
                .build();
    }

    private CompensationPageResponse compensationPage() {
        return CompensationPageResponse.builder()
                .compensations(List.of(compensationResponse()))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();
    }

    @Test
    @DisplayName("should create compensation and return 201")
    void shouldCreateCompensation() throws Exception {
        when(compensationService.createCompensation(eq("admin@example.com"), any(CreateCompensationRequest.class)))
                .thenReturn(compensationResponse());

        String body = "{\"damageAssessmentId\":3,\"approvedAmount\":100000.50,\"remarks\":\"Initial compensation\"}";

        mockMvc.perform(post("/api/compensations")
                        .with(authenticatedAdmin("admin@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.compensationStatus").value("PENDING"))
                .andExpect(jsonPath("$.citizenEmail").value("citizen@example.com"));
    }

    @Test
    @DisplayName("should get compensation by id")
    void shouldGetCompensationById() throws Exception {
        when(compensationService.getCompensationById(1L)).thenReturn(compensationResponse());

        mockMvc.perform(get("/api/compensations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.reportTitle").value("Flood report"));
    }

    @Test
    @DisplayName("should update compensation")
    void shouldUpdateCompensation() throws Exception {
        when(compensationService.updateCompensation(eq(1L), eq("admin@example.com"),
                any(UpdateCompensationRequest.class))).thenReturn(compensationResponse());

        String body = "{\"approvedAmount\":120000.00,\"remarks\":\"Updated\"}";

        mockMvc.perform(put("/api/compensations/1")
                        .with(authenticatedAdmin("admin@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.compensationStatus").value("PENDING"));
    }

    @Test
    @DisplayName("should approve compensation")
    void shouldApproveCompensation() throws Exception {
        CompensationResponse approved = CompensationResponse.builder()
                .id(1L)
                .compensationStatus(CompensationStatus.APPROVED)
                .citizenEmail("citizen@example.com")
                .build();
        when(compensationService.approveCompensation(eq(1L), eq("admin@example.com"),
                any(ApproveCompensationRequest.class))).thenReturn(approved);

        String body = "{\"remarks\":\"Approved\"}";

        mockMvc.perform(patch("/api/compensations/1/approve")
                        .with(authenticatedAdmin("admin@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.compensationStatus").value("APPROVED"));
    }

    @Test
    @DisplayName("should reject compensation")
    void shouldRejectCompensation() throws Exception {
        CompensationResponse rejected = CompensationResponse.builder()
                .id(1L)
                .compensationStatus(CompensationStatus.REJECTED)
                .citizenEmail("citizen@example.com")
                .build();
        when(compensationService.rejectCompensation(eq(1L), eq("admin@example.com"),
                any(RejectCompensationRequest.class))).thenReturn(rejected);

        String body = "{\"reason\":\"Not eligible\"}";

        mockMvc.perform(patch("/api/compensations/1/reject")
                        .with(authenticatedAdmin("admin@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.compensationStatus").value("REJECTED"));
    }

    @Test
    @DisplayName("should update payment status")
    void shouldUpdatePaymentStatus() throws Exception {
        CompensationResponse paid = CompensationResponse.builder()
                .id(1L)
                .compensationStatus(CompensationStatus.PAID)
                .paymentStatus(PaymentStatus.COMPLETED)
                .citizenEmail("citizen@example.com")
                .build();
        when(compensationService.updatePaymentStatus(eq(1L), eq("admin@example.com"),
                any(UpdatePaymentStatusRequest.class))).thenReturn(paid);

        String body = "{\"paymentStatus\":\"COMPLETED\"}";

        mockMvc.perform(patch("/api/compensations/1/payment-status")
                        .with(authenticatedAdmin("admin@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("COMPLETED"));
    }

    @Test
    @DisplayName("should search compensations")
    void shouldSearchCompensations() throws Exception {
        when(compensationService.searchCompensations(anyString(), any(), any(), anyInt(), anyInt(),
                anyString(), anyString())).thenReturn(compensationPage());

        mockMvc.perform(get("/api/compensations")
                        .param("search", "flood")
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.compensations[0].compensationStatus").value("PENDING"));
    }

    @Test
    @DisplayName("should get my compensations")
    void shouldGetMyCompensations() throws Exception {
        when(compensationService.getMyCompensations(eq("citizen@example.com"), any(), any(), any(),
                anyInt(), anyInt(), anyString(), anyString())).thenReturn(compensationPage());

        mockMvc.perform(get("/api/compensations/my")
                        .with(authenticatedAdmin("citizen@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.compensations[0].citizenEmail").value("citizen@example.com"));
    }

    @Test
    @DisplayName("should get compensation history")
    void shouldGetCompensationHistory() throws Exception {
        CompensationHistoryResponse history = CompensationHistoryResponse.builder()
                .id(1L)
                .previousStatus(CompensationStatus.PENDING)
                .newStatus(CompensationStatus.APPROVED)
                .changedByName("Admin Test")
                .changedByEmail("admin@example.com")
                .build();
        when(compensationService.getCompensationHistory(1L)).thenReturn(List.of(history));

        mockMvc.perform(get("/api/compensations/1/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].newStatus").value("APPROVED"))
                .andExpect(jsonPath("$[0].changedByEmail").value("admin@example.com"));
    }

    @Test
    @DisplayName("should delete compensation and return 204")
    void shouldDeleteCompensation() throws Exception {
        doNothing().when(compensationService).deleteCompensation(eq(1L), eq("admin@example.com"));

        mockMvc.perform(delete("/api/compensations/1")
                        .with(authenticatedAdmin("admin@example.com")))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("should return 400 when creating compensation with invalid payload")
    void shouldReturn400ForInvalidCreatePayload() throws Exception {
        String body = "{\"damageAssessmentId\":3,\"approvedAmount\":150000.00,\"remarks\":\"\"}";

        mockMvc.perform(post("/api/compensations")
                        .with(authenticatedAdmin("admin@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should reject compensation and return 400 when reason is blank")
    void shouldReturn400ForInvalidRejectPayload() throws Exception {
        String body = "{\"reason\":\"\"}";

        mockMvc.perform(patch("/api/compensations/1/reject")
                        .with(authenticatedAdmin("admin@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}