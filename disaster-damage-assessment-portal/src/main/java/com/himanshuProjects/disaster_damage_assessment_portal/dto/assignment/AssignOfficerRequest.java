package com.himanshuProjects.disaster_damage_assessment_portal.dto.assignment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Future;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AssignOfficerRequest {

    @NotNull(message = "Field officer ID is required")
    private Long fieldOfficerId;

    @NotNull(message = "Inspection date is required")
    @Future(message = "Inspection date must be in the future")
    private LocalDateTime inspectionDate;

    private String notes;
}
