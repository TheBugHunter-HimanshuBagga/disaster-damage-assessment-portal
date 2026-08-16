package com.himanshuProjects.disaster_damage_assessment_portal.controller.dashboard;

import com.himanshuProjects.disaster_damage_assessment_portal.dto.dashboard.AdminDashboardResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.dashboard.CitizenDashboardResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.dashboard.OfficerDashboardResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.service.dashboard.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Role-based dashboard statistics")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @Operation(summary = "Get dashboard", description = "Returns the dashboard for the authenticated user based on their role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dashboard returned"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<?> getDashboard(Authentication authentication) {
        String email = authentication.getName();
        Object dashboard = dashboardService.getAdminDashboard();
        return ResponseEntity.ok(dashboard);
    }

    @GetMapping("/admin")
    @Operation(summary = "Get admin dashboard", description = "Returns aggregated statistics for admin: total reports, users, assessments, compensations. Admin only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Admin dashboard returned"),
            @ApiResponse(responseCode = "403", description = "Not admin role")
    })
    public ResponseEntity<AdminDashboardResponse> getAdminDashboard() {
        AdminDashboardResponse response = dashboardService.getAdminDashboard();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/officer")
    @Operation(summary = "Get officer dashboard", description = "Returns statistics for the authenticated field officer: assigned reports, completed assessments.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Officer dashboard returned"),
            @ApiResponse(responseCode = "403", description = "Not field officer role")
    })
    public ResponseEntity<OfficerDashboardResponse> getOfficerDashboard(Authentication authentication) {
        String email = authentication.getName();
        OfficerDashboardResponse response = dashboardService.getOfficerDashboard(email);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/citizen")
    @Operation(summary = "Get citizen dashboard", description = "Returns statistics for the authenticated citizen: my reports, compensation status.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Citizen dashboard returned"),
            @ApiResponse(responseCode = "403", description = "Not citizen role")
    })
    public ResponseEntity<CitizenDashboardResponse> getCitizenDashboard(Authentication authentication) {
        String email = authentication.getName();
        CitizenDashboardResponse response = dashboardService.getCitizenDashboard(email);
        return ResponseEntity.ok(response);
    }
}
