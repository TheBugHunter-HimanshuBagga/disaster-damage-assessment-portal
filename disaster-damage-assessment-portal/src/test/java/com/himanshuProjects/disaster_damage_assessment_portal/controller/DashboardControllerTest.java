package com.himanshuProjects.disaster_damage_assessment_portal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.himanshuProjects.disaster_damage_assessment_portal.controller.dashboard.DashboardController;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.dashboard.AdminDashboardResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.dashboard.CitizenDashboardResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.dashboard.OfficerDashboardResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.exception.GlobalExceptionHandler;
import com.himanshuProjects.disaster_damage_assessment_portal.service.dashboard.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DashboardControllerTest {

    private ObjectMapper objectMapper;
    private DashboardService dashboardService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        dashboardService = mock(DashboardService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new DashboardController(dashboardService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static RequestPostProcessor authenticatedUser(String email, String role) {
        return request -> {
            request.setUserPrincipal(new UsernamePasswordAuthenticationToken(
                    email, "password", List.of(new SimpleGrantedAuthority(role))));
            return request;
        };
    }

    private AdminDashboardResponse adminDashboard() {
        return AdminDashboardResponse.builder()
                .totalUsers(100)
                .totalReports(50)
                .pendingReports(5)
                .totalCompensations(10)
                .approvedCompensations(8)
                .totalCompensationAmount(new BigDecimal("1000000"))
                .build();
    }

    @Test
    @DisplayName("should get dashboard for authenticated user")
    void shouldGetDashboard() throws Exception {
        when(dashboardService.getAdminDashboard()).thenReturn(adminDashboard());

        mockMvc.perform(get("/api/dashboard")
                        .with(authenticatedUser("admin@example.com", "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(100))
                .andExpect(jsonPath("$.totalReports").value(50));
    }

    @Test
    @DisplayName("should get admin dashboard")
    void shouldGetAdminDashboard() throws Exception {
        when(dashboardService.getAdminDashboard()).thenReturn(adminDashboard());

        mockMvc.perform(get("/api/dashboard/admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCompensations").value(10))
                .andExpect(jsonPath("$.approvedCompensations").value(8));
    }

    @Test
    @DisplayName("should get officer dashboard")
    void shouldGetOfficerDashboard() throws Exception {
        OfficerDashboardResponse officer = OfficerDashboardResponse.builder()
                .officerId(2L)
                .officerName("Officer Test")
                .totalAssigned(7)
                .accepted(3)
                .completed(2)
                .build();
        when(dashboardService.getOfficerDashboard("officer@example.com")).thenReturn(officer);

        mockMvc.perform(get("/api/dashboard/officer")
                        .with(authenticatedUser("officer@example.com", "ROLE_FIELD_OFFICER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAssigned").value(7))
                .andExpect(jsonPath("$.accepted").value(3));
    }

    @Test
    @DisplayName("should get citizen dashboard")
    void shouldGetCitizenDashboard() throws Exception {
        CitizenDashboardResponse citizen = CitizenDashboardResponse.builder()
                .citizenId(1L)
                .citizenName("John Doe")
                .totalReports(4)
                .pendingReports(1)
                .completedReports(3)
                .totalCompensations(1)
                .totalCompensationReceived(new BigDecimal("50000"))
                .build();
        when(dashboardService.getCitizenDashboard("citizen@example.com")).thenReturn(citizen);

        mockMvc.perform(get("/api/dashboard/citizen")
                        .with(authenticatedUser("citizen@example.com", "ROLE_CITIZEN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReports").value(4))
                .andExpect(jsonPath("$.completedReports").value(3));
    }
}