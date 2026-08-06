package com.himanshuProjects.disaster_damage_assessment_portal.dto.user;

import com.himanshuProjects.disaster_damage_assessment_portal.enums.AccountStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.Gender;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.RoleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private Long id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private Gender gender;
    private RoleType role;
    private AccountStatus accountStatus;
    private String districtName;
    private String stateName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
