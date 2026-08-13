package com.himanshuProjects.disaster_damage_assessment_portal.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDashboardResponse {

    private long totalUsers;
    private Map<String, Long> usersByRole;
    private long totalReports;
    private Map<String, Long> reportsByStatus;
    private long pendingReports;
    private long totalCompensations;
    private Map<String, Long> compensationsByStatus;
    private long approvedCompensations;
    private BigDecimal totalCompensationAmount;
    private BigDecimal averageCompensationAmount;
    private List<OfficerWorkloadResponse> officerWorkloads;
    private List<MonthlyReportStat> monthlyReports;
}
