package com.himanshuProjects.disaster_damage_assessment_portal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.himanshuProjects.disaster_damage_assessment_portal.controller.audit.AuditLogController;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.audit.AuditLogPageResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.audit.AuditLogResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.AuditAction;
import com.himanshuProjects.disaster_damage_assessment_portal.exception.GlobalExceptionHandler;
import com.himanshuProjects.disaster_damage_assessment_portal.service.audit.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuditLogControllerTest {

    private ObjectMapper objectMapper;
    private AuditLogService auditLogService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        auditLogService = mock(AuditLogService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AuditLogController(auditLogService))
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

    private AuditLogResponse auditLogResponse() {
        return AuditLogResponse.builder()
                .id(1L)
                .action(AuditAction.CREATE_REPORT)
                .entityName("DISASTER_REPORT")
                .entityId(3L)
                .description("Report created")
                .performedByName("Admin Test")
                .performedByEmail("admin@example.com")
                .build();
    }

    private AuditLogPageResponse auditLogPage() {
        return AuditLogPageResponse.builder()
                .auditLogs(List.of(auditLogResponse()))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();
    }

    @Test
    @DisplayName("should search audit logs")
    void shouldSearchAuditLogs() throws Exception {
        when(auditLogService.searchAuditLogs(any(), any(), any(), any(),
                anyInt(), anyInt(), anyString(), anyString())).thenReturn(auditLogPage());

        mockMvc.perform(get("/api/audit-logs")
                        .param("action", "CREATE_REPORT")
                        .param("entityName", "DISASTER_REPORT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.auditLogs[0].action").value("CREATE_REPORT"));
    }

    @Test
    @DisplayName("should get audit logs by entity")
    void shouldGetAuditLogsByEntity() throws Exception {
        when(auditLogService.getAuditLogsByEntity("DISASTER_REPORT", 3L))
                .thenReturn(List.of(auditLogResponse()));

        mockMvc.perform(get("/api/audit-logs/entity/DISASTER_REPORT/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].entityId").value(3))
                .andExpect(jsonPath("$[0].performedByEmail").value("admin@example.com"));
    }

    @Test
    @DisplayName("should get my audit logs")
    void shouldGetMyAuditLogs() throws Exception {
        when(auditLogService.getMyAuditLogs("admin@example.com")).thenReturn(List.of(auditLogResponse()));

        mockMvc.perform(get("/api/audit-logs/my")
                        .with(authenticatedAdmin("admin@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].description").value("Report created"));
    }
}