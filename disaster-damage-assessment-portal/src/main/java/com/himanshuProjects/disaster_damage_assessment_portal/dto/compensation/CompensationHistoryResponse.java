package com.himanshuProjects.disaster_damage_assessment_portal.dto.compensation;

import com.himanshuProjects.disaster_damage_assessment_portal.enums.CompensationStatus;
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
public class CompensationHistoryResponse {

    private Long id;
    private CompensationStatus previousStatus;
    private CompensationStatus newStatus;
    private String remarks;
    private String changedByName;
    private String changedByEmail;
    private LocalDateTime changedAt;
}
