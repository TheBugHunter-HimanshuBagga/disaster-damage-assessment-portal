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
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getProfile(Authentication authentication) {
        String email = authentication.getName();
        UserResponse response = userService.getProfile(email);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) {
        String email = authentication.getName();
        UserResponse response = userService.updateProfile(email, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        String email = authentication.getName();
        userService.changePassword(email, request);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
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
    public ResponseEntity<CitizenProfileResponse> getCitizenProfile(Authentication authentication) {
        String email = authentication.getName();
        CitizenProfileResponse response = userService.getCitizenProfile(email);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/citizen/profile")
    public ResponseEntity<CitizenProfileResponse> updateCitizenProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateCitizenProfileRequest request) {
        String email = authentication.getName();
        CitizenProfileResponse response = userService.updateCitizenProfile(email, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/account-status")
    public ResponseEntity<UserResponse> updateUserAccountStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAccountStatusRequest request) {
        UserResponse response = userService.updateUserAccountStatus(id, request);
        return ResponseEntity.ok(response);
    }
}
