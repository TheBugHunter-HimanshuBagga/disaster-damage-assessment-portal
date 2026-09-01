package com.himanshuProjects.disaster_damage_assessment_portal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.himanshuProjects.disaster_damage_assessment_portal.controller.notification.NotificationController;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.notification.NotificationPageResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.notification.NotificationResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.NotificationType;
import com.himanshuProjects.disaster_damage_assessment_portal.exception.GlobalExceptionHandler;
import com.himanshuProjects.disaster_damage_assessment_portal.service.notification.NotificationService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationControllerTest {

    private ObjectMapper objectMapper;
    private NotificationService notificationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        notificationService = mock(NotificationService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new NotificationController(notificationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static RequestPostProcessor authenticatedUser(String email) {
        return request -> {
            request.setUserPrincipal(new UsernamePasswordAuthenticationToken(
                    email, "password", List.of(new SimpleGrantedAuthority("ROLE_CITIZEN"))));
            return request;
        };
    }

    private NotificationResponse notificationResponse() {
        return NotificationResponse.builder()
                .id(1L)
                .title("Officer assigned")
                .message("An officer was assigned to your report")
                .notificationType(NotificationType.OFFICER_ASSIGNED)
                .isRead(false)
                .referenceId(3L)
                .entityType("DISASTER_REPORT")
                .build();
    }

    private NotificationPageResponse notificationPage() {
        return NotificationPageResponse.builder()
                .notifications(List.of(notificationResponse()))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();
    }

    @Test
    @DisplayName("should get my notifications")
    void shouldGetMyNotifications() throws Exception {
        when(notificationService.getMyNotifications(eq("user@example.com"), eq(Boolean.TRUE), anyInt(), anyInt()))
                .thenReturn(notificationPage());

        mockMvc.perform(get("/api/notifications")
                        .with(authenticatedUser("user@example.com"))
                        .param("unreadOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.notifications[0].title").value("Officer assigned"));
    }

    @Test
    @DisplayName("should get unread notification count")
    void shouldGetUnreadCount() throws Exception {
        when(notificationService.getUnreadCount("user@example.com")).thenReturn(3L);

        mockMvc.perform(get("/api/notifications/unread-count")
                        .with(authenticatedUser("user@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(3));
    }

    @Test
    @DisplayName("should mark notification as read")
    void shouldMarkAsRead() throws Exception {
        NotificationResponse read = NotificationResponse.builder()
                .id(1L)
                .title("Officer assigned")
                .isRead(true)
                .build();
        when(notificationService.markAsRead(eq(1L), eq("user@example.com"))).thenReturn(read);

        mockMvc.perform(patch("/api/notifications/1/read")
                        .with(authenticatedUser("user@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRead").value(true));
    }

    @Test
    @DisplayName("should mark all notifications as read")
    void shouldMarkAllAsRead() throws Exception {
        doNothing().when(notificationService).markAllAsRead("user@example.com");

        mockMvc.perform(patch("/api/notifications/read-all")
                        .with(authenticatedUser("user@example.com")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("should delete notification and return 204")
    void shouldDeleteNotification() throws Exception {
        doNothing().when(notificationService).deleteNotification(eq(1L), eq("user@example.com"));

        mockMvc.perform(delete("/api/notifications/1")
                        .with(authenticatedUser("user@example.com")))
                .andExpect(status().isNoContent());
    }
}