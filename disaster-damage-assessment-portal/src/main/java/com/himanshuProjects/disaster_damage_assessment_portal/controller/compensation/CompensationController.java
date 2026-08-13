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
public class CompensationController {

    private final CompensationService compensationService;

    public CompensationController(CompensationService compensationService) {
        this.compensationService = compensationService;
    }

    @PostMapping
    public ResponseEntity<CompensationResponse> createCompensation(
            Authentication authentication,
            @Valid @RequestBody CreateCompensationRequest request) {
        String email = authentication.getName();
        CompensationResponse response = compensationService.createCompensation(email, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompensationResponse> getCompensationById(@PathVariable Long id) {
        CompensationResponse response = compensationService.getCompensationById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompensationResponse> updateCompensation(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody UpdateCompensationRequest request) {
        String email = authentication.getName();
        CompensationResponse response = compensationService.updateCompensation(id, email, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/approve")
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
    public ResponseEntity<CompensationResponse> rejectCompensation(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody RejectCompensationRequest request) {
        String email = authentication.getName();
        CompensationResponse response = compensationService.rejectCompensation(id, email, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/payment-status")
    public ResponseEntity<CompensationResponse> updatePaymentStatus(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody UpdatePaymentStatusRequest request) {
        String email = authentication.getName();
        CompensationResponse response = compensationService.updatePaymentStatus(id, email, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
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
    public ResponseEntity<List<CompensationHistoryResponse>> getCompensationHistory(
            @PathVariable Long id) {
        List<CompensationHistoryResponse> response = compensationService.getCompensationHistory(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCompensation(
            Authentication authentication,
            @PathVariable Long id) {
        String email = authentication.getName();
        compensationService.deleteCompensation(id, email);
        return ResponseEntity.noContent().build();
    }
}
