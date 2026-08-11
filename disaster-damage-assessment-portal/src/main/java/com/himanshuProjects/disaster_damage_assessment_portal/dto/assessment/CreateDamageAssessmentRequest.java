package com.himanshuProjects.disaster_damage_assessment_portal.dto.assessment;

import com.himanshuProjects.disaster_damage_assessment_portal.enums.DamageLevel;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class CreateDamageAssessmentRequest {

    @NotNull(message = "Damage level is required")
    private DamageLevel damageLevel;

    @NotNull(message = "Estimated loss is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Estimated loss cannot be negative")
    private BigDecimal estimatedLoss;

    @NotBlank(message = "Assessment notes are required")
    @Size(max = 1000, message = "Assessment notes cannot exceed 1000 characters")
    private String assessmentNotes;

    @NotBlank(message = "Recommendation is required")
    @Size(max = 500, message = "Recommendation cannot exceed 500 characters")
    private String recommendation;

    @Size(max = 10, message = "Cannot upload more than 10 inspection images")
    private List<@Size(max = 500, message = "Image URL cannot exceed 500 characters") String> imageUrls;
}
