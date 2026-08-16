package com.himanshuProjects.disaster_damage_assessment_portal.controller.audit;

import com.himanshuProjects.disaster_damage_assessment_portal.dto.audit.AuditLogPageResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.audit.AuditLogResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.AuditAction;
import com.himanshuProjects.disaster_damage_assessment_portal.service.audit.AuditLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ResponseEntity<AuditLogPageResponse> searchAuditLogs(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) String entityName,
            @RequestParam(required = false) String userEmail,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "performedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        AuditLogPageResponse response = auditLogService.searchAuditLogs(
                search, action, entityName, userEmail, page, size, sortBy, sortDirection);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/entity/{entityName}/{entityId}")
    public ResponseEntity<List<AuditLogResponse>> getAuditLogsByEntity(
            @PathVariable String entityName,
            @PathVariable Long entityId) {
        List<AuditLogResponse> response = auditLogService.getAuditLogsByEntity(entityName, entityId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    public ResponseEntity<List<AuditLogResponse>> getMyAuditLogs(Authentication authentication) {
        String email = authentication.getName();
        List<AuditLogResponse> response = auditLogService.getMyAuditLogs(email);
        return ResponseEntity.ok(response);
    }
}
