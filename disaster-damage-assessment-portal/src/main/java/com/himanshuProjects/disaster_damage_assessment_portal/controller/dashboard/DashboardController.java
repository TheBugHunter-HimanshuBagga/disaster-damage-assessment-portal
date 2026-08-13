package com.himanshuProjects.disaster_damage_assessment_portal.controller.dashboard;

import com.himanshuProjects.disaster_damage_assessment_portal.dto.dashboard.AdminDashboardResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.dashboard.CitizenDashboardResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.dashboard.OfficerDashboardResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.service.dashboard.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public ResponseEntity<?> getDashboard(Authentication authentication) {
        String email = authentication.getName();
        Object dashboard = dashboardService.getAdminDashboard();
        return ResponseEntity.ok(dashboard);
    }

    @GetMapping("/admin")
    public ResponseEntity<AdminDashboardResponse> getAdminDashboard() {
        AdminDashboardResponse response = dashboardService.getAdminDashboard();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/officer")
    public ResponseEntity<OfficerDashboardResponse> getOfficerDashboard(Authentication authentication) {
        String email = authentication.getName();
        OfficerDashboardResponse response = dashboardService.getOfficerDashboard(email);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/citizen")
    public ResponseEntity<CitizenDashboardResponse> getCitizenDashboard(Authentication authentication) {
        String email = authentication.getName();
        CitizenDashboardResponse response = dashboardService.getCitizenDashboard(email);
        return ResponseEntity.ok(response);
    }
}
