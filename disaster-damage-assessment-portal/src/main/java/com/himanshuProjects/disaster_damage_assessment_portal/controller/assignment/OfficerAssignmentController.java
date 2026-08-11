package com.himanshuProjects.disaster_damage_assessment_portal.controller.assignment;

import com.himanshuProjects.disaster_damage_assessment_portal.dto.assignment.AssignmentPageResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.assignment.AssignOfficerRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.assignment.OfficerAssignmentResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.assignment.UpdateAssignmentStatusRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.AssignmentStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.service.assignment.OfficerAssignmentService;
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
public class OfficerAssignmentController {

    private final OfficerAssignmentService assignmentService;

    public OfficerAssignmentController(OfficerAssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @PostMapping("/report/{reportId}")
    public ResponseEntity<OfficerAssignmentResponse> assignOfficer(
            @PathVariable Long reportId,
            @Valid @RequestBody AssignOfficerRequest request) {
        OfficerAssignmentResponse response = assignmentService.assignOfficer(reportId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OfficerAssignmentResponse> getAssignmentById(@PathVariable Long id) {
        OfficerAssignmentResponse response = assignmentService.getAssignmentById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/report/{reportId}")
    public ResponseEntity<OfficerAssignmentResponse> getAssignmentByReportId(
            @PathVariable Long reportId) {
        OfficerAssignmentResponse response = assignmentService.getAssignmentByReportId(reportId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
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
    public ResponseEntity<OfficerAssignmentResponse> reassignOfficer(
            @PathVariable Long id,
            @Valid @RequestBody AssignOfficerRequest request) {
        OfficerAssignmentResponse response = assignmentService.reassignOfficer(id, request);
        return ResponseEntity.ok(response);
    }
}
