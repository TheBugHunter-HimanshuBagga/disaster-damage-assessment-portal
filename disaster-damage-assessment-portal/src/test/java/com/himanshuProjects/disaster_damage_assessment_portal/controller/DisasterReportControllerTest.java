package com.himanshuProjects.disaster_damage_assessment_portal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.himanshuProjects.disaster_damage_assessment_portal.controller.disaster.DisasterReportController;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.disaster.AddReportImagesRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.disaster.CreateDisasterReportRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.disaster.DisasterReportPageResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.disaster.DisasterReportResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.disaster.UpdateDisasterReportRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.disaster.UpdateReportStatusRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.DisasterType;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.ReportStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.exception.GlobalExceptionHandler;
import com.himanshuProjects.disaster_damage_assessment_portal.service.disaster.DisasterReportService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DisasterReportControllerTest {

    private ObjectMapper objectMapper;
    private DisasterReportService reportService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        reportService = mock(DisasterReportService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new DisasterReportController(reportService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static RequestPostProcessor authenticatedCitizen(String email) {
        return request -> {
            request.setUserPrincipal(new UsernamePasswordAuthenticationToken(
                    email, "password", List.of(new SimpleGrantedAuthority("ROLE_CITIZEN"))));
            return request;
        };
    }

    private DisasterReportResponse reportResponse() {
        return DisasterReportResponse.builder()
                .id(1L)
                .title("Flood report")
                .description("Severe flooding in the area")
                .disasterType(DisasterType.FLOOD)
                .status(ReportStatus.SUBMITTED)
                .incidentAddress("Main Street")
                .citizenId(1L)
                .citizenName("John Doe")
                .citizenEmail("citizen@example.com")
                .build();
    }

    private DisasterReportPageResponse reportPage() {
        return DisasterReportPageResponse.builder()
                .reports(List.of(reportResponse()))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();
    }

    @Test
    @DisplayName("should create a disaster report and return 201")
    void shouldCreateReport() throws Exception {
        when(reportService.createReport(eq("citizen@example.com"), any(CreateDisasterReportRequest.class)))
                .thenReturn(reportResponse());

        String body = """
                {"title":"Flood report","description":"Severe flooding","disasterType":"FLOOD",
                 "incidentAddress":"Main Street","latitude":12.34,"longitude":56.78}
                """;

        mockMvc.perform(post("/api/reports")
                        .with(authenticatedCitizen("citizen@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Flood report"))
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    @DisplayName("should get report by id")
    void shouldGetReportById() throws Exception {
        when(reportService.getReportById(1L)).thenReturn(reportResponse());

        mockMvc.perform(get("/api/reports/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.disasterType").value("FLOOD"));
    }

    @Test
    @DisplayName("should get my reports")
    void shouldGetMyReports() throws Exception {
        when(reportService.getMyReports(eq("citizen@example.com"), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(reportPage());

        mockMvc.perform(get("/api/reports/my")
                        .with(authenticatedCitizen("citizen@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.reports[0].citizenEmail").value("citizen@example.com"));
    }

    @Test
    @DisplayName("should search reports")
    void shouldSearchReports() throws Exception {
        when(reportService.searchReports(anyString(), any(), any(), any(), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(reportPage());

        mockMvc.perform(get("/api/reports/search")
                        .param("search", "flood")
                        .param("disasterType", "FLOOD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reports[0].title").value("Flood report"));
    }

    @Test
    @DisplayName("should update a report")
    void shouldUpdateReport() throws Exception {
        when(reportService.updateReport(eq(1L), eq("citizen@example.com"), any(UpdateDisasterReportRequest.class)))
                .thenReturn(reportResponse());

        String body = """
                {"title":"Flood report","description":"Updated details","disasterType":"FLOOD",
                 "incidentAddress":"Main Street","latitude":12.34,"longitude":56.78}
                """;

        mockMvc.perform(put("/api/reports/1")
                        .with(authenticatedCitizen("citizen@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Flood report"));
    }

    @Test
    @DisplayName("should update report status")
    void shouldUpdateReportStatus() throws Exception {
        DisasterReportResponse updated = DisasterReportResponse.builder()
                .id(1L)
                .title("Flood report")
                .status(ReportStatus.APPROVED)
                .build();
        when(reportService.updateReportStatus(eq(1L), any(UpdateReportStatusRequest.class)))
                .thenReturn(updated);

        String body = "{\"status\":\"APPROVED\"}";

        mockMvc.perform(patch("/api/reports/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @DisplayName("should delete a report and return 204")
    void shouldDeleteReport() throws Exception {
        doNothing().when(reportService).deleteReport(eq(1L), eq("citizen@example.com"));

        mockMvc.perform(delete("/api/reports/1")
                        .with(authenticatedCitizen("citizen@example.com")))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("should add images when the image list is not empty")
    void shouldAddImagesWhenListIsNotEmpty() throws Exception {
        when(reportService.addImages(eq(1L), eq("citizen@example.com"), any(AddReportImagesRequest.class)))
                .thenReturn(reportResponse());

        String body = "{\"imageUrls\":[\"http://example.com/img1.jpg\"]}";

        mockMvc.perform(post("/api/reports/1/images")
                        .with(authenticatedCitizen("citizen@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportId").value(1));
    }

    @Test
    @DisplayName("should return 400 when adding an empty image list")
    void shouldReturn400ForEmptyImageList() throws Exception {
        String body = "{\"imageUrls\":[]}";

        mockMvc.perform(post("/api/reports/1/images")
                        .with(authenticatedCitizen("citizen@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should remove an image from a report and return 204")
    void shouldRemoveImage() throws Exception {
        doNothing().when(reportService).removeImage(eq(1L), anyLong(), eq("citizen@example.com"));

        mockMvc.perform(delete("/api/reports/1/images/5")
                        .with(authenticatedCitizen("citizen@example.com")))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("should return 400 when creating a report with invalid payload")
    void shouldReturn400ForInvalidCreatePayload() throws Exception {
        String body = "{\"title\":\"\",\"description\":\"\",\"disasterType\":\"FLOOD\"}";

        mockMvc.perform(post("/api/reports")
                        .with(authenticatedCitizen("citizen@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}