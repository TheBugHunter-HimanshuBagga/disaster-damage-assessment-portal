package com.himanshuProjects.disaster_damage_assessment_portal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.himanshuProjects.disaster_damage_assessment_portal.controller.feedback.FeedbackController;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.feedback.CreateFeedbackRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.feedback.FeedbackPageResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.feedback.FeedbackResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.exception.GlobalExceptionHandler;
import com.himanshuProjects.disaster_damage_assessment_portal.service.feedback.FeedbackService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FeedbackControllerTest {

    private ObjectMapper objectMapper;
    private FeedbackService feedbackService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        feedbackService = mock(FeedbackService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new FeedbackController(feedbackService))
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

    private FeedbackResponse feedbackResponse() {
        return FeedbackResponse.builder()
                .id(1L)
                .rating(5)
                .comments("Good service")
                .userId(1L)
                .userName("John Doe")
                .userEmail("citizen@example.com")
                .reportId(3L)
                .reportTitle("Flood report")
                .build();
    }

    private FeedbackPageResponse feedbackPage() {
        return FeedbackPageResponse.builder()
                .feedbacks(List.of(feedbackResponse()))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();
    }

    @Test
    @DisplayName("should submit feedback and return 201")
    void shouldSubmitFeedback() throws Exception {
        when(feedbackService.submitFeedback(eq("citizen@example.com"), any(CreateFeedbackRequest.class)))
                .thenReturn(feedbackResponse());

        String body = "{\"disasterReportId\":3,\"rating\":5,\"comments\":\"Good service\"}";

        mockMvc.perform(post("/api/feedbacks")
                        .with(authenticatedCitizen("citizen@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.userEmail").value("citizen@example.com"));
    }

    @Test
    @DisplayName("should get feedback by id")
    void shouldGetFeedbackById() throws Exception {
        when(feedbackService.getFeedbackById(1L)).thenReturn(feedbackResponse());

        mockMvc.perform(get("/api/feedbacks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.comments").value("Good service"));
    }

    @Test
    @DisplayName("should get feedback by report id")
    void shouldGetFeedbackByReportId() throws Exception {
        when(feedbackService.getFeedbackByReportId(3L)).thenReturn(feedbackResponse());

        mockMvc.perform(get("/api/feedbacks/report/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportId").value(3));
    }

    @Test
    @DisplayName("should search feedbacks")
    void shouldSearchFeedbacks() throws Exception {
        when(feedbackService.searchFeedbacks(any(), any(), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(feedbackPage());

        mockMvc.perform(get("/api/feedbacks")
                        .param("rating", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.feedbacks[0].rating").value(5));
    }

    @Test
    @DisplayName("should get my feedbacks")
    void shouldGetMyFeedbacks() throws Exception {
        when(feedbackService.getMyFeedbacks(eq("citizen@example.com"), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(feedbackPage());

        mockMvc.perform(get("/api/feedbacks/my")
                        .with(authenticatedCitizen("citizen@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feedbacks[0].userEmail").value("citizen@example.com"));
    }

    @Test
    @DisplayName("should delete feedback and return 204")
    void shouldDeleteFeedback() throws Exception {
        doNothing().when(feedbackService).deleteFeedback(eq(1L), eq("citizen@example.com"));

        mockMvc.perform(delete("/api/feedbacks/1")
                        .with(authenticatedCitizen("citizen@example.com")))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("should return 400 when submitting invalid feedback")
    void shouldReturn400ForInvalidSubmitPayload() throws Exception {
        String body = "{\"disasterReportId\":3,\"rating\":9,\"comments\":\"\"}";

        mockMvc.perform(post("/api/feedbacks")
                        .with(authenticatedCitizen("citizen@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}