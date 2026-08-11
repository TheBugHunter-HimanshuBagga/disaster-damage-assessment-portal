package com.himanshuProjects.disaster_damage_assessment_portal.dto.assessment;

import com.himanshuProjects.disaster_damage_assessment_portal.enums.DamageLevel;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.DisasterType;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.ReportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DamageAssessmentResponse {

    private Long id;
    private DamageLevel damageLevel;
    private BigDecimal estimatedLoss;
    private String assessmentNotes;
    private String recommendation;
    private LocalDateTime assessedAt;
    private Long reportId;
    private String reportTitle;
    private DisasterType disasterType;
    private ReportStatus reportStatus;
    private Long officerId;
    private String officerName;
    private String officerEmail;
    private String citizenName;
    private String citizenEmail;
    private List<InspectionImageResponse> images;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
