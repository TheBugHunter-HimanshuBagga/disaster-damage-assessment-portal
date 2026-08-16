package com.himanshuProjects.disaster_damage_assessment_portal.controller.compensation;

import com.himanshuProjects.disaster_damage_assessment_portal.dto.compensation.ApproveCompensationRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.compensation.CompensationHistoryResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.compensation.CompensationPageResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.compensation.CompensationResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.compensation.CreateCompensationRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.compensation.RejectCompensationRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.compensation.UpdateCompensationRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.compensation.UpdatePaymentStatusRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.CompensationStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.PaymentStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.service.compensation.CompensationService;
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

import java.util.List;

@RestController
@RequestMapping("/api/compensations")
@Tag(name = "Compensation", description = "Compensation creation, approval, and payment management")
public class CompensationController {

    private final CompensationService compensationService;

    public CompensationController(CompensationService compensationService) {
        this.compensationService = compensationService;
    }

    @PostMapping
    @Operation(summary = "Create compensation", description = "Admin creates a compensation record for an approved assessment.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Compensation created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "Assessment not found")
    })
    public ResponseEntity<CompensationResponse> createCompensation(
            Authentication authentication,
            @Valid @RequestBody CreateCompensationRequest request) {
        String email = authentication.getName();
        CompensationResponse response = compensationService.createCompensation(email, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get compensation by ID", description = "Returns a specific compensation record with details.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Compensation found"),
            @ApiResponse(responseCode = "404", description = "Compensation not found")
    })
    public ResponseEntity<CompensationResponse> getCompensationById(@PathVariable Long id) {
        CompensationResponse response = compensationService.getCompensationById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update compensation", description = "Admin updates compensation details (amount, remarks). Only PENDING compensations can be updated.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Compensation updated"),
            @ApiResponse(responseCode = "400", description = "Validation error or status not PENDING"),
            @ApiResponse(responseCode = "404", description = "Compensation not found")
    })
    public ResponseEntity<CompensationResponse> updateCompensation(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody UpdateCompensationRequest request) {
        String email = authentication.getName();
        CompensationResponse response = compensationService.updateCompensation(id, email, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/approve")
    @Operation(summary = "Approve compensation", description = "Super admin approves a pending compensation. Status changes to APPROVED.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Compensation approved"),
            @ApiResponse(responseCode = "404", description = "Compensation not found")
    })
    public ResponseEntity<CompensationResponse> approveCompensation(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody(required = false) ApproveCompensationRequest request) {
        String email = authentication.getName();
        ApproveCompensationRequest approveRequest = request != null ? request : new ApproveCompensationRequest();
        CompensationResponse response = compensationService.approveCompensation(id, email, approveRequest);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/reject")
    @Operation(summary = "Reject compensation", description = "Super admin rejects a pending compensation with reason. Status changes to REJECTED.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Compensation rejected"),
            @ApiResponse(responseCode = "404", description = "Compensation not found")
    })
    public ResponseEntity<CompensationResponse> rejectCompensation(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody RejectCompensationRequest request) {
        String email = authentication.getName();
        CompensationResponse response = compensationService.rejectCompensation(id, email, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/payment-status")
    @Operation(summary = "Update payment status", description = "Admin marks compensation as PAID with payment date and transaction details.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment status updated"),
            @ApiResponse(responseCode = "400", description = "Compensation not APPROVED"),
            @ApiResponse(responseCode = "404", description = "Compensation not found")
    })
    public ResponseEntity<CompensationResponse> updatePaymentStatus(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody UpdatePaymentStatusRequest request) {
        String email = authentication.getName();
        CompensationResponse response = compensationService.updatePaymentStatus(id, email, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Search compensations", description = "Search and filter compensations by status, payment status. Supports pagination.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search results returned"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters")
    })
    public ResponseEntity<CompensationPageResponse> searchCompensations(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) CompensationStatus status,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        CompensationPageResponse response = compensationService.searchCompensations(
                search, status, paymentStatus, page, size, sortBy, sortDirection);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    @Operation(summary = "Get my compensations", description = "Returns all compensations for the authenticated citizen.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Compensations returned"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<CompensationPageResponse> getMyCompensations(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        String email = authentication.getName();
        CompensationPageResponse response = compensationService.getMyCompensations(
                email, page, size, sortBy, sortDirection);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Get compensation history", description = "Returns the full status change history of a compensation (audit trail).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "History returned"),
            @ApiResponse(responseCode = "404", description = "Compensation not found")
    })
    public ResponseEntity<List<CompensationHistoryResponse>> getCompensationHistory(
            @PathVariable Long id) {
        List<CompensationHistoryResponse> response = compensationService.getCompensationHistory(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete compensation", description = "Deletes a compensation record. Only PENDING compensations can be deleted.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Compensation deleted"),
            @ApiResponse(responseCode = "400", description = "Status not PENDING"),
            @ApiResponse(responseCode = "404", description = "Compensation not found")
    })
    public ResponseEntity<Void> deleteCompensation(
            Authentication authentication,
            @PathVariable Long id) {
        String email = authentication.getName();
        compensationService.deleteCompensation(id, email);
        return ResponseEntity.noContent().build();
    }
}
