package com.himanshuProjects.disaster_damage_assessment_portal.controller.assessment;

import com.himanshuProjects.disaster_damage_assessment_portal.dto.assessment.AddInspectionImagesRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.assessment.CreateDamageAssessmentRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.assessment.DamageAssessmentPageResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.assessment.DamageAssessmentResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.assessment.UpdateDamageAssessmentRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.DamageLevel;
import com.himanshuProjects.disaster_damage_assessment_portal.service.assessment.DamageAssessmentService;
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
public class DamageAssessmentController {

    private final DamageAssessmentService assessmentService;

    public DamageAssessmentController(DamageAssessmentService assessmentService) {
        this.assessmentService = assessmentService;
    }

    @PostMapping("/report/{reportId}")
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
    public ResponseEntity<DamageAssessmentResponse> getAssessmentById(@PathVariable Long id) {
        DamageAssessmentResponse response = assessmentService.getAssessmentById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/report/{reportId}")
    public ResponseEntity<DamageAssessmentResponse> getAssessmentByReportId(
            @PathVariable Long reportId) {
        DamageAssessmentResponse response = assessmentService.getAssessmentByReportId(reportId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
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
    public ResponseEntity<DamageAssessmentResponse> addImages(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody AddInspectionImagesRequest request) {
        String email = authentication.getName();
        DamageAssessmentResponse response = assessmentService.addImages(id, email, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{assessmentId}/images/{imageId}")
    public ResponseEntity<Void> removeImage(
            Authentication authentication,
            @PathVariable Long assessmentId,
            @PathVariable Long imageId) {
        String email = authentication.getName();
        assessmentService.removeImage(assessmentId, imageId, email);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAssessment(
            Authentication authentication,
            @PathVariable Long id) {
        String email = authentication.getName();
        assessmentService.deleteAssessment(id, email);
        return ResponseEntity.noContent().build();
    }
}
