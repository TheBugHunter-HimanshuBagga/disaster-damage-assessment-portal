package com.himanshuProjects.disaster_damage_assessment_portal.service.impl.audit;

import com.himanshuProjects.disaster_damage_assessment_portal.dto.audit.AuditLogPageResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.audit.AuditLogResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.system.AuditLog;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.AuditAction;
import com.himanshuProjects.disaster_damage_assessment_portal.exception.BadRequestException;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.system.AuditLogRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.user.UserRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.service.audit.AuditLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogServiceImpl.class);

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "action", "entityName", "entityId", "performedAt", "ipAddress"
    );

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AuditLogPageResponse searchAuditLogs(String search, AuditAction action,
                                                 String entityName, String userEmail,
                                                 int page, int size,
                                                 String sortBy, String sortDirection) {
        log.info("Searching audit logs - search: {}, action: {}, entity: {}, user: {}",
                search, action, entityName, userEmail);

        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new BadRequestException("Invalid sort field: " + sortBy
                    + ". Allowed fields: " + ALLOWED_SORT_FIELDS);
        }

        Sort sort = sortDirection.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<AuditLog> pageResult = auditLogRepository.searchAuditLogs(
                search, action, entityName, userEmail, pageable);

        List<AuditLogResponse> auditLogs = pageResult.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return AuditLogPageResponse.builder()
                .auditLogs(auditLogs)
                .pageNumber(pageResult.getNumber())
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAuditLogsByEntity(String entityName, Long entityId) {
        log.info("Fetching audit logs for {} ID: {}", entityName, entityId);

        List<AuditLog> logs = auditLogRepository.findByEntityNameAndEntityIdOrderByPerformedAtDesc(
                entityName, entityId);

        return logs.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getMyAuditLogs(String userEmail) {
        log.info("Fetching audit logs for user: {}", userEmail);

        var user = userRepository.findByEmail(userEmail)
                .orElse(null);
        if (user == null) return List.of();

        List<AuditLog> logs = auditLogRepository.findByPerformedByIdOrderByPerformedAtDesc(user.getId());

        return logs.stream()
                .map(this::mapToResponse)
                .toList();
    }

    private AuditLogResponse mapToResponse(AuditLog auditLog) {
        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .action(auditLog.getAction())
                .entityName(auditLog.getEntityName())
                .entityId(auditLog.getEntityId())
                .description(auditLog.getDescription())
                .performedAt(auditLog.getPerformedAt())
                .ipAddress(auditLog.getIpAddress())
                .performedByName(auditLog.getPerformedBy().getFullName())
                .performedByEmail(auditLog.getPerformedBy().getEmail())
                .build();
    }
}
