package com.himanshuProjects.disaster_damage_assessment_portal.service.dashboard;

import com.himanshuProjects.disaster_damage_assessment_portal.dto.dashboard.AdminDashboardResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.dashboard.CitizenDashboardResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.dashboard.OfficerDashboardResponse;

public interface DashboardService {

    AdminDashboardResponse getAdminDashboard();

    OfficerDashboardResponse getOfficerDashboard(String officerEmail);

    CitizenDashboardResponse getCitizenDashboard(String citizenEmail);
}
