package com.himanshuProjects.disaster_damage_assessment_portal.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.testcontainers.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class SecurityAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("should return 403 when a protected endpoint is accessed without authentication")
    void shouldReturn403WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/users/profile"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should return 403 when a role-restricted endpoint is accessed without authentication")
    void shouldReturn403WhenRoleRestrictedEndpointAccessedWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/users/search"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should return 403 when authenticated user lacks the required role")
    void shouldReturn403ForInsufficientRole() throws Exception {
        mockMvc.perform(get("/api/users/search")
                        .with(user("citizen@example.com").roles("CITIZEN")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should return 200 for an admin role on an admin-only endpoint")
    void shouldReturn200ForAdminOnAdminOnlyEndpoint() throws Exception {
        mockMvc.perform(get("/api/users/search")
                        .with(user("admin@example.com").roles("SUPER_ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("should return 200 for an officer role on an officer-accessible endpoint")
    void shouldReturn200ForOfficerOnOfficerAccessibleEndpoint() throws Exception {
        mockMvc.perform(get("/api/reports/search")
                        .with(user("officer@example.com").roles("FIELD_OFFICER")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("should return 200 for any authenticated role on an authenticated-only endpoint")
    void shouldReturn200ForAnyAuthenticatedRole() throws Exception {
        mockMvc.perform(get("/api/states")
                        .param("search", "Mah")
                        .with(user("citizen@example.com").roles("CITIZEN")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("should not require authentication for a public endpoint")
    void shouldNotRequireAuthenticationForPublicEndpoint() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}