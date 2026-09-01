package com.himanshuProjects.disaster_damage_assessment_portal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.himanshuProjects.disaster_damage_assessment_portal.controller.user.UserController;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.user.CitizenProfileResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.user.ChangePasswordRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.user.UpdateAccountStatusRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.user.UpdateCitizenProfileRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.user.UpdateProfileRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.user.UserPageResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.user.UserResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.AccountStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.Gender;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.RoleType;
import com.himanshuProjects.disaster_damage_assessment_portal.exception.GlobalExceptionHandler;
import com.himanshuProjects.disaster_damage_assessment_portal.service.user.UserService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest {

    private ObjectMapper objectMapper;
    private UserService userService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        userService = mock(UserService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new UserController(userService))
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

    private UserResponse userResponse() {
        return UserResponse.builder()
                .id(1L)
                .fullName("John Doe")
                .email("user@example.com")
                .phoneNumber("9876543210")
                .gender(Gender.MALE)
                .role(RoleType.CITIZEN)
                .accountStatus(AccountStatus.ACTIVE)
                .districtName("TestDistrict")
                .stateName("TestState")
                .build();
    }

    @Test
    @DisplayName("should get current user profile")
    void shouldGetProfile() throws Exception {
        when(userService.getProfile("user@example.com")).thenReturn(userResponse());

        mockMvc.perform(get("/api/users/profile")
                        .with(authenticatedUser("user@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.role").value("CITIZEN"));
    }

    @Test
    @DisplayName("should update current user profile")
    void shouldUpdateProfile() throws Exception {
        when(userService.updateProfile(eq("user@example.com"), any(UpdateProfileRequest.class)))
                .thenReturn(userResponse());

        String body = "{\"fullName\":\"John Doe\",\"phoneNumber\":\"9876543210\"}";

        mockMvc.perform(put("/api/users/profile")
                        .with(authenticatedUser("user@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("John Doe"));
    }

    @Test
    @DisplayName("should change password")
    void shouldChangePassword() throws Exception {
        doNothing().when(userService).changePassword(eq("user@example.com"), any(ChangePasswordRequest.class));

        String body = """
                {"currentPassword":"oldPass123","newPassword":"newPass123","confirmPassword":"newPass123"}
                """;

        mockMvc.perform(patch("/api/users/change-password")
                        .with(authenticatedUser("user@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password changed successfully"));
    }

    @Test
    @DisplayName("should get user by id")
    void shouldGetUserById() throws Exception {
        when(userService.getUserById(5L)).thenReturn(userResponse());

        mockMvc.perform(get("/api/users/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("should search users")
    void shouldSearchUsers() throws Exception {
        UserPageResponse page = UserPageResponse.builder()
                .users(List.of(userResponse()))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();
        when(userService.searchUsers(anyString(), any(), any(), any(), anyInt(), anyInt(),
                anyString(), anyString())).thenReturn(page);

        mockMvc.perform(get("/api/users/search")
                        .param("search", "john")
                        .param("role", "CITIZEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.users[0].email").value("user@example.com"));
    }

    @Test
    @DisplayName("should get citizen profile")
    void shouldGetCitizenProfile() throws Exception {
        CitizenProfileResponse profile = CitizenProfileResponse.builder()
                .id(1L)
                .aadhaarNumber("123456789012")
                .userEmail("user@example.com")
                .build();
        when(userService.getCitizenProfile("user@example.com")).thenReturn(profile);

        mockMvc.perform(get("/api/users/citizen/profile")
                        .with(authenticatedUser("user@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aadhaarNumber").value("123456789012"));
    }

    @Test
    @DisplayName("should update citizen profile")
    void shouldUpdateCitizenProfile() throws Exception {
        CitizenProfileResponse profile = CitizenProfileResponse.builder()
                .id(1L)
                .aadhaarNumber("123456789012")
                .userEmail("user@example.com")
                .build();
        when(userService.updateCitizenProfile(eq("user@example.com"), any(UpdateCitizenProfileRequest.class)))
                .thenReturn(profile);

        String body = """
                {"aadhaarNumber":"123456789012","dateOfBirth":"1990-01-01","address":"123 Test Street","emergencyContact":"9123456789"}
                """;

        mockMvc.perform(put("/api/users/citizen/profile")
                        .with(authenticatedUser("user@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aadhaarNumber").value("123456789012"));
    }

    @Test
    @DisplayName("should update user account status")
    void shouldUpdateAccountStatus() throws Exception {
        when(userService.updateUserAccountStatus(eq(5L), any(UpdateAccountStatusRequest.class)))
                .thenReturn(userResponse());

        String body = "{\"accountStatus\":\"SUSPENDED\"}";

        mockMvc.perform(patch("/api/users/5/account-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountStatus").value("ACTIVE"));
    }

    @Test
    @DisplayName("should return 400 for invalid account status payload")
    void shouldReturn400ForInvalidAccountStatus() throws Exception {
        String body = "{}";

        mockMvc.perform(patch("/api/users/5/account-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}