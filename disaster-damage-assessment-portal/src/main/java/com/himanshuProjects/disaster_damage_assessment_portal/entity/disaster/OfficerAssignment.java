package com.himanshuProjects.disaster_damage_assessment_portal.entity.disaster;

import com.himanshuProjects.disaster_damage_assessment_portal.common.BaseEntity;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.User;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.AssignmentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "officer_assignments")
public class OfficerAssignment extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disaster_report_id", nullable = false)
    private DisasterReport disasterReport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "field_officer_id", nullable = false)
    private User fieldOfficer;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt = LocalDateTime.now();

    @NotNull(message = "Estimated Inspection date is required")
    @Column(name = "inspection_date", nullable = false)
    private LocalDateTime inspectionDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssignmentStatus assignmentStatus = AssignmentStatus.ASSIGNED;

    @Column(name = "notes", length = 500)
    private String notes;
}
