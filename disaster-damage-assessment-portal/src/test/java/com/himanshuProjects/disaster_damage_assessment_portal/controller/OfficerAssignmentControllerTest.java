package com.himanshuProjects.disaster_damage_assessment_portal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.himanshuProjects.disaster_damage_assessment_portal.controller.assignment.OfficerAssignmentController;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.assignment.AssignmentPageResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.assignment.AssignOfficerRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.assignment.OfficerAssignmentResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.assignment.UpdateAssignmentStatusRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.AssignmentStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.DisasterType;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.ReportStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.exception.GlobalExceptionHandler;
import com.himanshuProjects.disaster_damage_assessment_portal.service.assignment.OfficerAssignmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OfficerAssignmentControllerTest {

    private ObjectMapper objectMapper;
    private OfficerAssignmentService assignmentService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        assignmentService = mock(OfficerAssignmentService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new OfficerAssignmentController(assignmentService))
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

    private OfficerAssignmentResponse assignmentResponse() {
        return OfficerAssignmentResponse.builder()
                .id(1L)
                .reportId(3L)
                .reportTitle("Flood report")
                .disasterType(DisasterType.FLOOD)
                .reportStatus(ReportStatus.ASSIGNED)
                .officerId(2L)
                .officerName("Officer Test")
                .officerEmail("officer@example.com")
                .assignmentStatus(AssignmentStatus.ASSIGNED)
                .build();
    }

    private AssignmentPageResponse assignmentPage() {
        return AssignmentPageResponse.builder()
                .assignments(List.of(assignmentResponse()))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();
    }

    @Test
    @DisplayName("should assign an officer and return 201")
    void shouldAssignOfficer() throws Exception {
        when(assignmentService.assignOfficer(eq(3L), any(AssignOfficerRequest.class)))
                .thenReturn(assignmentResponse());

        String inspectionDate = LocalDateTime.now().plusDays(1).withNano(0).toString();
        String body = "{\"fieldOfficerId\":2,\"inspectionDate\":\"" + inspectionDate
                + "\",\"notes\":\"Inspect\"}";

        mockMvc.perform(post("/api/assignments/report/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.officerEmail").value("officer@example.com"))
                .andExpect(jsonPath("$.assignmentStatus").value("ASSIGNED"));
    }

    @Test
    @DisplayName("should get assignment by id")
    void shouldGetAssignmentById() throws Exception {
        when(assignmentService.getAssignmentById(1L)).thenReturn(assignmentResponse());

        mockMvc.perform(get("/api/assignments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.reportTitle").value("Flood report"));
    }

    @Test
    @DisplayName("should get assignment by report id")
    void shouldGetAssignmentByReportId() throws Exception {
        when(assignmentService.getAssignmentByReportId(3L)).thenReturn(assignmentResponse());

        mockMvc.perform(get("/api/assignments/report/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.officerId").value(2));
    }

    @Test
    @DisplayName("should update assignment status")
    void shouldUpdateAssignmentStatus() throws Exception {
        OfficerAssignmentResponse accepted = OfficerAssignmentResponse.builder()
                .id(1L)
                .reportId(3L)
                .reportTitle("Flood report")
                .officerEmail("officer@example.com")
                .assignmentStatus(AssignmentStatus.ACCEPTED)
                .build();
        when(assignmentService.updateAssignmentStatus(eq(1L), eq("officer@example.com"),
                any(UpdateAssignmentStatusRequest.class))).thenReturn(accepted);

        String body = "{\"assignmentStatus\":\"ACCEPTED\",\"notes\":\"Accepted\"}";

        mockMvc.perform(patch("/api/assignments/1/status")
                        .with(authenticatedOfficer("officer@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignmentStatus").value("ACCEPTED"));
    }

    @Test
    @DisplayName("should search assignments")
    void shouldSearchAssignments() throws Exception {
        when(assignmentService.searchAssignments(any(), any(), any(), anyInt(), anyInt(),
                anyString(), anyString())).thenReturn(assignmentPage());

        mockMvc.perform(get("/api/assignments")
                        .param("search", "flood")
                        .param("status", "ASSIGNED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.assignments[0].assignmentStatus").value("ASSIGNED"));
    }

    @Test
    @DisplayName("should get my assignments")
    void shouldGetMyAssignments() throws Exception {
        when(assignmentService.getMyAssignments(eq("officer@example.com"), any(),
                eq(AssignmentStatus.ASSIGNED),
                anyInt(), anyInt(), anyString(), anyString())).thenReturn(assignmentPage());

        mockMvc.perform(get("/api/assignments/my")
                        .with(authenticatedOfficer("officer@example.com"))
                        .param("status", "ASSIGNED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignments[0].officerEmail").value("officer@example.com"));
    }

    @Test
    @DisplayName("should reassign an officer")
    void shouldReassignOfficer() throws Exception {
        when(assignmentService.reassignOfficer(eq(1L), any(AssignOfficerRequest.class)))
                .thenReturn(assignmentResponse());

        String inspectionDate = LocalDateTime.now().plusDays(2).withNano(0).toString();
        String body = "{\"fieldOfficerId\":4,\"inspectionDate\":\"" + inspectionDate
                + "\",\"notes\":\"Reassigned\"}";

        mockMvc.perform(post("/api/assignments/1/reassign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.officerId").value(2));
    }

    @Test
    @DisplayName("should return 400 when assigning with invalid payload")
    void shouldReturn400ForInvalidAssignPayload() throws Exception {
        String body = "{}";

        mockMvc.perform(post("/api/assignments/report/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}