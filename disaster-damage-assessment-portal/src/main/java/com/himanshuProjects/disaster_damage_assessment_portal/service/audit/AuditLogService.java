package com.himanshuProjects.disaster_damage_assessment_portal.service.audit;

import com.himanshuProjects.disaster_damage_assessment_portal.dto.audit.AuditLogPageResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.audit.AuditLogResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.AuditAction;

import java.util.List;

public interface AuditLogService {

    AuditLogPageResponse searchAuditLogs(String search, AuditAction action,
                                          String entityName, String userEmail,
                                          int page, int size,
                                          String sortBy, String sortDirection);

    List<AuditLogResponse> getAuditLogsByEntity(String entityName, Long entityId);

    List<AuditLogResponse> getMyAuditLogs(String userEmail);
}
