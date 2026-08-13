package com.himanshuProjects.disaster_damage_assessment_portal.dto.compensation;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApproveCompensationRequest {

    @Size(max = 500, message = "Remarks cannot exceed 500 characters")
    private String remarks;
}
