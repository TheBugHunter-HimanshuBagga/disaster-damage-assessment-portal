package com.himanshuProjects.disaster_damage_assessment_portal.controller.assessment;

import com.himanshuProjects.disaster_damage_assessment_portal.dto.assessment.AddInspectionImagesRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.assessment.CreateDamageAssessmentRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.assessment.DamageAssessmentPageResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.assessment.DamageAssessmentResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.assessment.UpdateDamageAssessmentRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.DamageLevel;
import com.himanshuProjects.disaster_damage_assessment_portal.service.assessment.DamageAssessmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assessments")
@Tag(name = "Damage Assessments", description = "Field officer damage assessment management")
public class DamageAssessmentController {

    private final DamageAssessmentService assessmentService;

    public DamageAssessmentController(DamageAssessmentService assessmentService) {
        this.assessmentService = assessmentService;
    }

    @PostMapping("/report/{reportId}")
    @Operation(summary = "Submit damage assessment", description = "Field officer submits a damage assessment for a disaster report. Updates report status to ASSESSED.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Assessment created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "Report not found")
    })
    public ResponseEntity<DamageAssessmentResponse> submitAssessment(
            Authentication authentication,
            @PathVariable Long reportId,
            @Valid @RequestBody CreateDamageAssessmentRequest request) {
        String email = authentication.getName();
        DamageAssessmentResponse response = assessmentService.submitAssessment(
                reportId, email, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get assessment by ID", description = "Returns a specific damage assessment with all details.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Assessment found"),
            @ApiResponse(responseCode = "404", description = "Assessment not found")
    })
    public ResponseEntity<DamageAssessmentResponse> getAssessmentById(@PathVariable Long id) {
        DamageAssessmentResponse response = assessmentService.getAssessmentById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/report/{reportId}")
    @Operation(summary = "Get assessment by report ID", description = "Returns the damage assessment associated with a specific report.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Assessment found"),
            @ApiResponse(responseCode = "404", description = "Assessment not found for this report")
    })
    public ResponseEntity<DamageAssessmentResponse> getAssessmentByReportId(
            @PathVariable Long reportId) {
        DamageAssessmentResponse response = assessmentService.getAssessmentByReportId(reportId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update damage assessment", description = "Field officer updates an existing assessment. Only the assigned officer can update.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Assessment updated"),
            @ApiResponse(responseCode = "403", description = "Not the assigned officer"),
            @ApiResponse(responseCode = "404", description = "Assessment not found")
    })
    public ResponseEntity<DamageAssessmentResponse> updateAssessment(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody UpdateDamageAssessmentRequest request) {
        String email = authentication.getName();
        DamageAssessmentResponse response = assessmentService.updateAssessment(
                id, email, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Search damage assessments", description = "Search and filter assessments by damage level, officer. Supports pagination.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search results returned"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters")
    })
    public ResponseEntity<DamageAssessmentPageResponse> searchAssessments(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) DamageLevel damageLevel,
            @RequestParam(required = false) Long officerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "assessedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        DamageAssessmentPageResponse response = assessmentService.searchAssessments(
                search, damageLevel, officerId, page, size, sortBy, sortDirection);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    @Operation(summary = "Get my assessments", description = "Returns all assessments submitted by the authenticated field officer.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Assessments returned"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<DamageAssessmentPageResponse> getMyAssessments(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "assessedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        String email = authentication.getName();
        DamageAssessmentPageResponse response = assessmentService.getMyAssessments(
                email, page, size, sortBy, sortDirection);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/images")
    @Operation(summary = "Add inspection images", description = "Adds inspection photo URLs to an existing assessment.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Images added"),
            @ApiResponse(responseCode = "403", description = "Not the assigned officer"),
            @ApiResponse(responseCode = "404", description = "Assessment not found")
    })
    public ResponseEntity<DamageAssessmentResponse> addImages(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody AddInspectionImagesRequest request) {
        String email = authentication.getName();
        DamageAssessmentResponse response = assessmentService.addImages(id, email, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{assessmentId}/images/{imageId}")
    @Operation(summary = "Remove inspection image", description = "Removes a specific image from an assessment.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Image removed"),
            @ApiResponse(responseCode = "403", description = "Not the assigned officer"),
            @ApiResponse(responseCode = "404", description = "Assessment or image not found")
    })
    public ResponseEntity<Void> removeImage(
            Authentication authentication,
            @PathVariable Long assessmentId,
            @PathVariable Long imageId) {
        String email = authentication.getName();
        assessmentService.removeImage(assessmentId, imageId, email);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete assessment", description = "Deletes an assessment. Only the assigned officer can delete.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Assessment deleted"),
            @ApiResponse(responseCode = "403", description = "Not the assigned officer"),
            @ApiResponse(responseCode = "404", description = "Assessment not found")
    })
    public ResponseEntity<Void> deleteAssessment(
            Authentication authentication,
            @PathVariable Long id) {
        String email = authentication.getName();
        assessmentService.deleteAssessment(id, email);
        return ResponseEntity.noContent().build();
    }
}
