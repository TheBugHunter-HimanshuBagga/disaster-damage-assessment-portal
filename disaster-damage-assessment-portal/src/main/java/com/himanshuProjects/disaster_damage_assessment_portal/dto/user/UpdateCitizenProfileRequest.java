package com.himanshuProjects.disaster_damage_assessment_portal.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UpdateCitizenProfileRequest {

    @NotBlank(message = "Aadhaar number is required")
    @Pattern(
            regexp = "^\\d{12}$",
            message = "Aadhaar number must contain exactly 12 digits"
    )
    private String aadhaarNumber;

    @NotNull(message = "Date of birth is required")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Address is required")
    @Size(max = 500, message = "Address cannot exceed 500 characters")
    private String address;

    @Size(max = 500, message = "Profile photo URL cannot exceed 500 characters")
    private String profilePhotoUrl;

    @NotBlank(message = "Emergency contact is required")
    @Pattern(
            regexp = "^[6-9]\\d{9}$",
            message = "Please enter a valid 10-digit Indian mobile number"
    )
    private String emergencyContact;
}
