package com.himanshuProjects.disaster_damage_assessment_portal.dto.auth;

import com.himanshuProjects.disaster_damage_assessment_portal.enums.AccountStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.RoleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private Long id;
    private String fullName;
    private String email;
    private RoleType role;
    private AccountStatus accountStatus;
    private String message;

    // Will be populated after JWT implementation
    private String token;
}
