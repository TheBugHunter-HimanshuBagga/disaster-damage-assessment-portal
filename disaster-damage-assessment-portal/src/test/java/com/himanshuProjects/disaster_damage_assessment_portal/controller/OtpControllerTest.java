package com.himanshuProjects.disaster_damage_assessment_portal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.himanshuProjects.disaster_damage_assessment_portal.controller.auth.OtpController;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.auth.OtpRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.User;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.user.UserRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.security.JwtService;
import com.himanshuProjects.disaster_damage_assessment_portal.service.OtpService;
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

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OtpController.class)
@AutoConfigureMockMvc(addFilters = false)
class OtpControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private OtpService otpService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("should resend OTP successfully when user exists")
    void shouldResendOtp() throws Exception {
        User user = TestDataFactory.createCitizen(1L, "john@example.com");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        doNothing().when(otpService).resendOtp(anyString(), anyString());

        OtpRequest request = new OtpRequest();
        request.setEmail("john@example.com");

        mockMvc.perform(post("/api/auth/resend-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    @DisplayName("should resend OTP with default name when user not found")
    void shouldResendOtpWithDefaultNameWhenUserMissing() throws Exception {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        OtpRequest request = new OtpRequest();
        request.setEmail("unknown@example.com");

        mockMvc.perform(post("/api/auth/resend-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("should verify OTP and activate account on success")
    void shouldVerifyOtpAndActivateAccount() throws Exception {
        when(otpService.verifyOtp("john@example.com", "123456")).thenReturn(true);
        doNothing().when(authService).activateAccount("john@example.com");

        String body = "{\"email\":\"john@example.com\",\"otp\":\"123456\"}";

        mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("verified")));
    }

    @Test
    @DisplayName("should return 400 with failure when OTP is invalid")
    void shouldReturn400WhenOtpInvalid() throws Exception {
        when(otpService.verifyOtp("john@example.com", "000000")).thenReturn(false);

        String body = "{\"email\":\"john@example.com\",\"otp\":\"000000\"}";

        mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("should return 400 for invalid OTP request payload")
    void shouldReturn400ForInvalidPayload() throws Exception {
        String body = "{\"email\":\"not-an-email\",\"otp\":\"abc\"}";

        mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
