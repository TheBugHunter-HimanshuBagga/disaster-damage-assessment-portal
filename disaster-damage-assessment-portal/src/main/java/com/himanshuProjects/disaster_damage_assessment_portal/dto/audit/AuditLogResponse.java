package com.himanshuProjects.disaster_damage_assessment_portal.dto.audit;

import com.himanshuProjects.disaster_damage_assessment_portal.enums.AuditAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogResponse {

    private Long id;
    private AuditAction action;
    private String entityName;
    private Long entityId;
    private String description;
    private LocalDateTime performedAt;
    private String ipAddress;
    private String performedByName;
    private String performedByEmail;
}
