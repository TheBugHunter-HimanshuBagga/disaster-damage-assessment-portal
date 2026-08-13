package com.himanshuProjects.disaster_damage_assessment_portal.entity.compensation;

import com.himanshuProjects.disaster_damage_assessment_portal.common.BaseEntity;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.disaster.DamageAssessment;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.User;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.CompensationStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.PaymentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "compensation")
public class Compensation extends BaseEntity {
    // FLOW TILL NOW
    // citizen -> DisasterReport -> OfficerAssignment -> DamageAssessment -> Compensation

    @NotNull(message = "Approved amount is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Amount approved cannot be null")
    @Column(name = "approved_amount", nullable = false, precision = 12, scale = 2) // precision -> total number of digits , scale = number of digits after the decial point
    private BigDecimal approvedAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "compensation_status" ,nullable = false ,length = 20)
    private CompensationStatus compensationStatus = CompensationStatus.PENDING;

    @Column(name = "approved_date")
    private LocalDateTime approvedDate;

    @NotBlank(message = "Remarks are required")
    @Column(nullable = false, length = 500)
    private String remarks;

    @OneToOne(fetch = FetchType.LAZY) // One damage assessment produces one compensation
    @JoinColumn(name = "damage_assessment_id", nullable = false, unique = true)
    private DamageAssessment damageAssessment; // compensation is given on the bases of this assessment

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.NOT_INITIATED;

    @Column(name = "paid_date")
    private LocalDateTime paidDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paid_by")
    private User paidBy;
}
