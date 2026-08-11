package com.himanshuProjects.disaster_damage_assessment_portal.dto.district;

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
public class DistrictResponse {

    private Long id;
    private String name;
    private Long stateId;
    private String stateName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
