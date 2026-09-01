package com.himanshuProjects.disaster_damage_assessment_portal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.himanshuProjects.disaster_damage_assessment_portal.controller.auth.AuthController;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.auth.AuthResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.auth.LoginRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.auth.RegisterRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.AccountStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.RoleType;
import com.himanshuProjects.disaster_damage_assessment_portal.security.JwtService;
import com.himanshuProjects.disaster_damage_assessment_portal.service.auth.AuthService;
import com.himanshuProjects.disaster_damage_assessment_portal.testutil.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("should register a new citizen and return 201 with token")
    void shouldRegisterUser() throws Exception {
        AuthResponse response = AuthResponse.builder()
                .id(1L)
                .fullName("John Doe")
                .email("john.doe@example.com")
                .role(RoleType.CITIZEN)
                .accountStatus(AccountStatus.PENDING_VERIFICATION)
                .token("some-token")
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TestDataFactory.createRegisterRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.role").value("CITIZEN"));
    }

    @Test
    @DisplayName("should login and return 200 with JWT token")
    void shouldLogin() throws Exception {
        AuthResponse response = AuthResponse.builder()
                .id(2L)
                .fullName("John Doe")
                .email("john.doe@example.com")
                .role(RoleType.CITIZEN)
                .accountStatus(AccountStatus.ACTIVE)
                .token("jwt-token")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        LoginRequest request = new LoginRequest();
        request.setEmail("john.doe@example.com");
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.accountStatus").value("ACTIVE"));
    }

    @Test
    @DisplayName("should return 400 when registering with invalid payload")
    void shouldReturn400ForInvalidRegisterPayload() throws Exception {
        String invalid = "{\"fullName\":\"\",\"email\":\"not-an-email\",\"password\":\"short\"}";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should return 400 when logging in with blank password")
    void shouldReturn400ForInvalidLoginPayload() throws Exception {
        String invalid = "{\"email\":\"john@example.com\",\"password\":\"\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest());
    }
}

