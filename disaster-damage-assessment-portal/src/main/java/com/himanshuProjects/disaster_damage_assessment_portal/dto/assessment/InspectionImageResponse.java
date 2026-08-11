package com.himanshuProjects.disaster_damage_assessment_portal.dto.assessment;

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
public class InspectionImageResponse {

    private Long id;
    private String imageUrl;
    private LocalDateTime uploadedAt;
}
