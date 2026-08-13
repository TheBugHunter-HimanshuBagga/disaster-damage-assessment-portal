package com.himanshuProjects.disaster_damage_assessment_portal.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CitizenDashboardResponse {

    private Long citizenId;
    private String citizenName;
    private long totalReports;
    private long pendingReports;
    private long completedReports;
    private long totalCompensations;
    private BigDecimal totalCompensationReceived;
}
