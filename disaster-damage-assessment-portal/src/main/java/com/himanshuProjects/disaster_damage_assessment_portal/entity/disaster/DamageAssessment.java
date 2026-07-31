package com.himanshuProjects.disaster_damage_assessment_portal.entity.disaster;

import com.himanshuProjects.disaster_damage_assessment_portal.common.BaseEntity;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.User;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.DamageLevel;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "damage_assessments")
public class DamageAssessment extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Damage level is required")
    @Column(name = "damage_level",nullable = false, length = 30)
    private DamageLevel damageLevel;

    @NotNull(message = "Estimated loss is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Estimated loss cannot be negative")
    @Column(name = "estimated_loss", nullable = false, precision = 12, scale = 2)
    private BigDecimal estimatedLoss;

    @NotBlank(message = "Assessment notes are required")
    @Size(max = 1000, message = "Assessment notes cannot exceed 1000 characters")
    @Column(name = "assessment_notes", nullable = false, length = 1000)
    private String assessmentNotes;

    @NotBlank(message = "Recommendation is required")
    @Size(max = 500, message = "Recommendation cannot exceed 500 characters")
    @Column(nullable = false, length = 500)
    private String recommendation;

    @Column(name = "assessed_at", nullable = false)
    private LocalDateTime assessedAt = LocalDateTime.now();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disaster_report_id", nullable = false, unique = true)
    private DisasterReport disasterReport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "field_officer_id", nullable = false)
    private User fieldOfficer;
}
