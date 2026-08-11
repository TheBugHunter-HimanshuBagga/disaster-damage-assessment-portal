package com.himanshuProjects.disaster_damage_assessment_portal.dto.district;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DistrictRequest {

    @NotBlank(message = "District name is required")
    @Size(max = 100, message = "District name cannot exceed 100 characters")
    private String name;

    @NotNull(message = "State ID is required")
    private Long stateId;
}
