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
public class OfficerDashboardResponse {

    private Long officerId;
    private String officerName;
    private long totalAssigned;
    private long accepted;
    private long inProgress;
    private long completed;
    private long reassigned;
    private long pendingInspections;
}
