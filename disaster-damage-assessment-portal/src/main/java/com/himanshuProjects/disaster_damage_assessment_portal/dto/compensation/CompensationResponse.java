package com.himanshuProjects.disaster_damage_assessment_portal.dto.compensation;

import com.himanshuProjects.disaster_damage_assessment_portal.enums.CompensationStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.DamageLevel;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.DisasterType;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.PaymentStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.ReportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompensationResponse {

    private Long id;
    private BigDecimal approvedAmount;
    private CompensationStatus compensationStatus;
    private LocalDateTime approvedDate;
    private String remarks;
    private PaymentStatus paymentStatus;
    private LocalDateTime paidDate;

    private Long damageAssessmentId;
    private BigDecimal estimatedLoss;
    private DamageLevel damageLevel;

    private Long reportId;
    private String reportTitle;
    private DisasterType disasterType;
    private ReportStatus reportStatus;

    private String citizenName;
    private String citizenEmail;

    private String approvedByName;
    private String approvedByEmail;

    private String paidByName;
    private String paidByEmail;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
