package com.himanshuProjects.disaster_damage_assessment_portal.dto.disaster;

import com.himanshuProjects.disaster_damage_assessment_portal.enums.DisasterType;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.ReportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisasterReportResponse {

    private Long id;
    private String title;
    private String description;
    private DisasterType disasterType;
    private ReportStatus status;
    private String incidentAddress;
    private Double latitude;
    private Double longitude;
    private LocalDateTime reportedAt;
    private Long citizenId;
    private String citizenName;
    private String citizenEmail;
    private List<ReportImageResponse> images;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
