package com.himanshuProjects.disaster_damage_assessment_portal.controller.disaster;

import com.himanshuProjects.disaster_damage_assessment_portal.dto.disaster.AddReportImagesRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.disaster.CreateDisasterReportRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.disaster.DisasterReportPageResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.disaster.DisasterReportResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.disaster.UpdateDisasterReportRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.disaster.UpdateReportStatusRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.DisasterType;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.ReportStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.service.disaster.DisasterReportService;
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
@RequestMapping("/api/reports")
@Tag(name = "Disaster Reports", description = "CRUD operations for disaster reports")
public class DisasterReportController {

    private final DisasterReportService reportService;

    public DisasterReportController(DisasterReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    @Operation(summary = "Create a new disaster report", description = "Citizens can submit a new disaster report with location, type, and description.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Report created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<DisasterReportResponse> createReport(
            Authentication authentication,
            @Valid @RequestBody CreateDisasterReportRequest request) {
        String email = authentication.getName();
        DisasterReportResponse response = reportService.createReport(email, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get report by ID", description = "Returns a specific disaster report with all details.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Report found"),
            @ApiResponse(responseCode = "404", description = "Report not found")
    })
    public ResponseEntity<DisasterReportResponse> getReportById(@PathVariable Long id) {
        DisasterReportResponse response = reportService.getReportById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    @Operation(summary = "Get my reports", description = "Returns all disaster reports submitted by the authenticated citizen.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reports returned"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
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
    @Operation(summary = "Search disaster reports", description = "Search and filter reports by type, status, citizen. Supports pagination and sorting.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search results returned"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters")
    })
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
    @Operation(summary = "Update a disaster report", description = "Updates report details. Only the report owner can update.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Report updated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "403", description = "Not the report owner"),
            @ApiResponse(responseCode = "404", description = "Report not found")
    })
    public ResponseEntity<DisasterReportResponse> updateReport(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody UpdateDisasterReportRequest request) {
        String email = authentication.getName();
        DisasterReportResponse response = reportService.updateReport(id, email, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update report status", description = "Admin can change the status of a disaster report (PENDING, IN_REVIEW, VERIFIED, REJECTED).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated"),
            @ApiResponse(responseCode = "404", description = "Report not found")
    })
    public ResponseEntity<DisasterReportResponse> updateReportStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateReportStatusRequest request) {
        DisasterReportResponse response = reportService.updateReportStatus(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a disaster report", description = "Deletes a report. Only the report owner can delete.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Report deleted"),
            @ApiResponse(responseCode = "403", description = "Not the report owner"),
            @ApiResponse(responseCode = "404", description = "Report not found")
    })
    public ResponseEntity<Void> deleteReport(
            Authentication authentication,
            @PathVariable Long id) {
        String email = authentication.getName();
        reportService.deleteReport(id, email);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/images")
    @Operation(summary = "Add images to report", description = "Adds image URLs to an existing disaster report.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Images added"),
            @ApiResponse(responseCode = "403", description = "Not the report owner"),
            @ApiResponse(responseCode = "404", description = "Report not found")
    })
    public ResponseEntity<DisasterReportResponse> addImages(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody AddReportImagesRequest request) {
        String email = authentication.getName();
        DisasterReportResponse response = reportService.addImages(id, email, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{reportId}/images/{imageId}")
    @Operation(summary = "Remove image from report", description = "Removes a specific image from a disaster report.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Image removed"),
            @ApiResponse(responseCode = "403", description = "Not the report owner"),
            @ApiResponse(responseCode = "404", description = "Report or image not found")
    })
    public ResponseEntity<Void> removeImage(
            Authentication authentication,
            @PathVariable Long reportId,
            @PathVariable Long imageId) {
        String email = authentication.getName();
        reportService.removeImage(reportId, imageId, email);
        return ResponseEntity.noContent().build();
    }
}
