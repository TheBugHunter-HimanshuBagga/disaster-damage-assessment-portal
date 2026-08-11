package com.himanshuProjects.disaster_damage_assessment_portal.dto.disaster;

import com.himanshuProjects.disaster_damage_assessment_portal.enums.ReportStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateReportStatusRequest {

    @NotNull(message = "Status is required")
    private ReportStatus status;
}
