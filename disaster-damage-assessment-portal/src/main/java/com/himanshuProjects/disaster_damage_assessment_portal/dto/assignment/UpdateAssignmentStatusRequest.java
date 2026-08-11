package com.himanshuProjects.disaster_damage_assessment_portal.dto.assignment;

import com.himanshuProjects.disaster_damage_assessment_portal.enums.AssignmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateAssignmentStatusRequest {

    @NotNull(message = "Assignment status is required")
    private AssignmentStatus assignmentStatus;

    private String notes;
}
