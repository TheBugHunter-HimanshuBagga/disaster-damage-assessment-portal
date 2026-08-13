package com.himanshuProjects.disaster_damage_assessment_portal.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfficerWorkloadResponse {

    private Long officerId;
    private String officerName;
    private String officerEmail;
    private long activeAssignments;
    private long completedAssignments;
    private long totalAssignments;
}
