package com.himanshuProjects.disaster_damage_assessment_portal.controller.assignment;

import com.himanshuProjects.disaster_damage_assessment_portal.dto.assignment.AssignmentPageResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.assignment.AssignOfficerRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.assignment.OfficerAssignmentResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.assignment.UpdateAssignmentStatusRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.AssignmentStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.service.assignment.OfficerAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assignments")
@Tag(name = "Officer Assignments", description = "Field officer assignment to disaster reports")
public class OfficerAssignmentController {

    private final OfficerAssignmentService assignmentService;

    public OfficerAssignmentController(OfficerAssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @PostMapping("/report/{reportId}")
    @Operation(summary = "Assign officer to report", description = "Admin assigns a field officer to a disaster report. Status set to PENDING.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Officer assigned"),
            @ApiResponse(responseCode = "400", description = "Validation error or officer already assigned"),
            @ApiResponse(responseCode = "404", description = "Report not found")
    })
    public ResponseEntity<OfficerAssignmentResponse> assignOfficer(
            @PathVariable Long reportId,
            @Valid @RequestBody AssignOfficerRequest request) {
        OfficerAssignmentResponse response = assignmentService.assignOfficer(reportId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get assignment by ID", description = "Returns a specific officer assignment with details.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Assignment found"),
            @ApiResponse(responseCode = "404", description = "Assignment not found")
    })
    public ResponseEntity<OfficerAssignmentResponse> getAssignmentById(@PathVariable Long id) {
        OfficerAssignmentResponse response = assignmentService.getAssignmentById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/report/{reportId}")
    @Operation(summary = "Get assignment by report ID", description = "Returns the officer assignment for a specific disaster report.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Assignment found"),
            @ApiResponse(responseCode = "404", description = "Assignment not found for this report")
    })
    public ResponseEntity<OfficerAssignmentResponse> getAssignmentByReportId(
            @PathVariable Long reportId) {
        OfficerAssignmentResponse response = assignmentService.getAssignmentByReportId(reportId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update assignment status", description = "Field officer updates assignment status (ACCEPTED, IN_PROGRESS, COMPLETED).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated"),
            @ApiResponse(responseCode = "403", description = "Not the assigned officer"),
            @ApiResponse(responseCode = "404", description = "Assignment not found")
    })
    public ResponseEntity<OfficerAssignmentResponse> updateAssignmentStatus(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody UpdateAssignmentStatusRequest request) {
        String email = authentication.getName();
        OfficerAssignmentResponse response = assignmentService.updateAssignmentStatus(
                id, email, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Search assignments", description = "Search and filter assignments by status, officer. Supports pagination.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search results returned"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters")
    })
    public ResponseEntity<AssignmentPageResponse> searchAssignments(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) AssignmentStatus status,
            @RequestParam(required = false) Long officerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "assignedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        AssignmentPageResponse response = assignmentService.searchAssignments(
                search, status, officerId, page, size, sortBy, sortDirection);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    @Operation(summary = "Get my assignments", description = "Returns all assignments for the authenticated field officer.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Assignments returned"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<AssignmentPageResponse> getMyAssignments(
            Authentication authentication,
            @RequestParam(required = false) AssignmentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "assignedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        String email = authentication.getName();
        AssignmentPageResponse response = assignmentService.getMyAssignments(
                email, status, page, size, sortBy, sortDirection);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reassign")
    @Operation(summary = "Reassign officer", description = "Admin reassigns a different officer to an existing assignment.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Officer reassigned"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "Assignment not found")
    })
    public ResponseEntity<OfficerAssignmentResponse> reassignOfficer(
            @PathVariable Long id,
            @Valid @RequestBody AssignOfficerRequest request) {
        OfficerAssignmentResponse response = assignmentService.reassignOfficer(id, request);
        return ResponseEntity.ok(response);
    }
}
