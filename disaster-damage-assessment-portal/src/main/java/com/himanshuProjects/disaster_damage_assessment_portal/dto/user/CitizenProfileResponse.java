package com.himanshuProjects.disaster_damage_assessment_portal.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CitizenProfileResponse {

    private Long id;
    private String aadhaarNumber;
    private LocalDate dateOfBirth;
    private String address;
    private String profilePhotoUrl;
    private String emergencyContact;
    private String userFullName;
    private String userEmail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
