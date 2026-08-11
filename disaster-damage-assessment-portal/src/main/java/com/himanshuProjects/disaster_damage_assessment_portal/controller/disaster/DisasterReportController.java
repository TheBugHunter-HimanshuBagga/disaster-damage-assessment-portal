package com.himanshuProjects.disaster_damage_assessment_portal.controller.disaster;

import com.himanshuProjects.disaster_damage_assessment_portal.dto.disaster.AddReportImagesRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.disaster.CreateDisasterReportRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.disaster.DisasterReportPageResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.disaster.DisasterReportResponse;import com.himanshuProjects.disaster_damage_assessment_portal.dto.disaster.UpdateDisasterReportRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.disaster.UpdateReportStatusRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.DisasterType;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.ReportStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.service.disaster.DisasterReportService;
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

import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class DisasterReportController {

    private final DisasterReportService reportService;

    public DisasterReportController(DisasterReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    public ResponseEntity<DisasterReportResponse> createReport(
            Authentication authentication,
            @Valid @RequestBody CreateDisasterReportRequest request) {
        String email = authentication.getName();
        DisasterReportResponse response = reportService.createReport(email, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DisasterReportResponse> getReportById(@PathVariable Long id) {
        DisasterReportResponse response = reportService.getReportById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    public ResponseEntity<DisasterReportPageResponse> getMyReports(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        String email = authentication.getName();
        DisasterReportPageResponse pageResponse = reportService.getMyReports(
                email, page, size, sortBy, sortDirection);
        return ResponseEntity.ok(pageResponse);
    }

    @GetMapping("/search")
    public ResponseEntity<DisasterReportPageResponse> searchReports(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) DisasterType disasterType,
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) Long citizenId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        DisasterReportPageResponse response = reportService.searchReports(
                search, disasterType, status, citizenId,
                page, size, sortBy, sortDirection);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DisasterReportResponse> updateReport(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody UpdateDisasterReportRequest request) {
        String email = authentication.getName();
        DisasterReportResponse response = reportService.updateReport(id, email, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<DisasterReportResponse> updateReportStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateReportStatusRequest request) {
        DisasterReportResponse response = reportService.updateReportStatus(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReport(
            Authentication authentication,
            @PathVariable Long id) {
        String email = authentication.getName();
        reportService.deleteReport(id, email);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/images")
    public ResponseEntity<DisasterReportResponse> addImages(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody AddReportImagesRequest request) {
        String email = authentication.getName();
        DisasterReportResponse response = reportService.addImages(id, email, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{reportId}/images/{imageId}")
    public ResponseEntity<Void> removeImage(
            Authentication authentication,
            @PathVariable Long reportId,
            @PathVariable Long imageId) {
        String email = authentication.getName();
        reportService.removeImage(reportId, imageId, email);
        return ResponseEntity.noContent().build();
    }
}
