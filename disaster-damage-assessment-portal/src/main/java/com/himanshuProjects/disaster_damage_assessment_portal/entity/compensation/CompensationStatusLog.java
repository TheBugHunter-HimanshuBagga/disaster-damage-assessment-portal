package com.himanshuProjects.disaster_damage_assessment_portal.entity.compensation;

import com.himanshuProjects.disaster_damage_assessment_portal.common.BaseEntity;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.User;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.CompensationStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "compensation_status_log")
public class CompensationStatusLog extends BaseEntity {

    @NotNull(message = "Previous status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", nullable = false, length = 20)
    private CompensationStatus previousStatus;

    @NotNull(message = "New status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 20)
    private CompensationStatus newStatus;

    @NotBlank(message = "Remarks are required")
    @Column(nullable = false, length = 500)
    private String remarks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by", nullable = false)
    private User changedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compensation_id", nullable = false)
    private Compensation compensation;
}
