package com.himanshuProjects.disaster_damage_assessment_portal.controller.user;

import com.himanshuProjects.disaster_damage_assessment_portal.dto.user.CitizenProfileResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.user.ChangePasswordRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.user.UpdateAccountStatusRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.user.UpdateCitizenProfileRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.user.UpdateProfileRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.user.UserPageResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.user.UserResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.AccountStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.RoleType;
import com.himanshuProjects.disaster_damage_assessment_portal.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Management", description = "User profile, password, and account management")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    @Operation(summary = "Get current user profile", description = "Returns the authenticated user's profile information.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile returned successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<UserResponse> getProfile(Authentication authentication) {
        String email = authentication.getName();
        UserResponse response = userService.getProfile(email);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/profile")
    @Operation(summary = "Update current user profile", description = "Updates the authenticated user's profile (fullName, phone).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<UserResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) {
        String email = authentication.getName();
        UserResponse response = userService.updateProfile(email, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/change-password")
    @Operation(summary = "Change password", description = "Changes the authenticated user's password. Requires current password verification.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password changed successfully"),
            @ApiResponse(responseCode = "400", description = "Current password incorrect"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<Map<String, String>> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        String email = authentication.getName();
        userService.changePassword(email, request);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Returns a user by their ID. Admin only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    @Operation(summary = "Search users", description = "Search and filter users by role, status, district. Supports pagination and sorting.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search results returned"),
            @ApiResponse(responseCode = "400", description = "Invalid sort field")
    })
    public ResponseEntity<UserPageResponse> searchUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) RoleType role,
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(required = false) Long districtId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {

        UserPageResponse response = userService.searchUsers(
                search, role, status, districtId,
                page, size, sortBy, sortDirection);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/citizen/profile")
    @Operation(summary = "Get citizen profile", description = "Returns the authenticated user's citizen-specific profile (address, district, etc.).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Citizen profile returned"),
            @ApiResponse(responseCode = "404", description = "Citizen profile not found")
    })
    public ResponseEntity<CitizenProfileResponse> getCitizenProfile(Authentication authentication) {
        String email = authentication.getName();
        CitizenProfileResponse response = userService.getCitizenProfile(email);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/citizen/profile")
    @Operation(summary = "Update citizen profile", description = "Updates the citizen-specific profile (address, district, pincode).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Citizen profile updated"),
            @ApiResponse(responseCode = "400", description = "Validation error")
    })
    public ResponseEntity<CitizenProfileResponse> updateCitizenProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateCitizenProfileRequest request) {
        String email = authentication.getName();
        CitizenProfileResponse response = userService.updateCitizenProfile(email, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/account-status")
    @Operation(summary = "Update account status", description = "Activate or deactivate a user account. Admin only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account status updated"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserResponse> updateUserAccountStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAccountStatusRequest request) {
        UserResponse response = userService.updateUserAccountStatus(id, request);
        return ResponseEntity.ok(response);
    }
}
