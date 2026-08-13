package com.himanshuProjects.disaster_damage_assessment_portal.dto.feedback;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateFeedbackRequest {

    @NotNull(message = "Disaster report ID is required")
    private Long disasterReportId;

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating cannot exceed 5")
    private Integer rating;

    @NotBlank(message = "Comments are required")
    @Size(max = 1000, message = "Comments cannot exceed 1000 characters")
    private String comments;
}
