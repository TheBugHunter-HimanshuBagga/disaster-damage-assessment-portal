package com.himanshuProjects.disaster_damage_assessment_portal.entity.user;

import com.himanshuProjects.disaster_damage_assessment_portal.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "citizen_profiles")
public class CitizenProfile extends BaseEntity {

    @NotBlank(message = "Aadhaar number is required")
    @Pattern(
            regexp = "^\\d{12}$",
            message = "Aadhaar number must contain exactly 12 digits"
    )
    @Column(name = "aadhaar_number", nullable = false, unique = true, length = 12)
    private String aadhaarNumber;

    @NotNull(message = "Date of birth is required")
    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @NotBlank(message = "Address is required")
    @Size(max = 500, message = "Address cannot exceed 500 characters")
    @Column(nullable = false, length = 500)
    private String address;

    @Size(max = 500, message = "Profile photo URL cannot exceed 500 characters")
    @Column(name = "profile_photo_url", length = 500)
    private String profilePhotoUrl;

    @NotBlank(message = "Emergency contact is required")
    @Pattern(
            regexp = "^[6-9]\\d{9}$",
            message = "Please enter a valid 10-digit Indian mobile number"
    )
    @Column(name = "emergency_contact", nullable = false ,length = 10)
    private String emergencyContact;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

}
