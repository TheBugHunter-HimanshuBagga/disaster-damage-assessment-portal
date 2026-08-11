package com.himanshuProjects.disaster_damage_assessment_portal.dto.state;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StateRequest {

    @NotBlank(message = "State name is required")
    @Size(max = 100, message = "State name cannot exceed 100 characters")
    private String name;

    @NotBlank(message = "State code is required")
    @Size(min = 2, max = 3, message = "State code must be between 2 and 3 characters")
    private String code;
}
