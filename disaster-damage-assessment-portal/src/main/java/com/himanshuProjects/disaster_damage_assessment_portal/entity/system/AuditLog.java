package com.himanshuProjects.disaster_damage_assessment_portal.entity.system;

import com.himanshuProjects.disaster_damage_assessment_portal.common.BaseEntity;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.User;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.AuditAction;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "audit_logs")
public class AuditLog extends BaseEntity {
    // whenEver someone performs an important action, we store it
    // District admin approved compensation -> Audit Log saved
    // citizen created a Disaster report -> audit Log saved

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Audit action is required")
    @Column(nullable = false, length = 50)
    private AuditAction action; // stores what actions user performed

    @NotBlank(message = "Entity name is required")
    @Size(max = 100,  message = "Entity name cannot exceed 100 characters")
    @Column(name = "entity_name", nullable = false, length = 100)
    private String entityName; // which entity was affected

    @NotNull(message = "Entity ID is required")
    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @NotBlank(message = "Description is required")
    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    @Column(nullable = false, length = 1000)
    private String description;

    @Column(name = "performed_at", nullable = false)
    private LocalDateTime performedAt = LocalDateTime.now();

    @Size(max = 45,  message = "IP address cannot exceed 45 characters")
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by", nullable = false)
    private User performedBy;
}
