package com.himanshuProjects.disaster_damage_assessment_portal.dto.assignment;

import com.himanshuProjects.disaster_damage_assessment_portal.enums.AssignmentStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.DisasterType;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.ReportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfficerAssignmentResponse {

    private Long id;
    private Long reportId;
    private String reportTitle;
    private DisasterType disasterType;
    private ReportStatus reportStatus;
    private String incidentAddress;
    private Long officerId;
    private String officerName;
    private String officerEmail;
    private AssignmentStatus assignmentStatus;
    private LocalDateTime assignedAt;
    private LocalDateTime inspectionDate;
    private String notes;
    private String citizenName;
    private String citizenEmail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
