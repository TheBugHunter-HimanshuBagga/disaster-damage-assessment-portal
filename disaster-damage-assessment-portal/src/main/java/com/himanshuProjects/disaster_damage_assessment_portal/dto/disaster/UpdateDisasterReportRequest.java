package com.himanshuProjects.disaster_damage_assessment_portal.dto.disaster;

import com.himanshuProjects.disaster_damage_assessment_portal.enums.DisasterType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateDisasterReportRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title cannot exceed 150 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @NotNull(message = "Disaster type is required")
    private DisasterType disasterType;

    @NotBlank(message = "Incident address is required")
    @Size(max = 500, message = "Incident address cannot exceed 500 characters")
    private String incidentAddress;

    @NotNull(message = "Latitude is required")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    private Double longitude;
}
