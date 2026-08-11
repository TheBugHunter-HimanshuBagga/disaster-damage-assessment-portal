package com.himanshuProjects.disaster_damage_assessment_portal.dto.state;

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
public class StateResponse {

    private Long id;
    private String name;
    private String code;
    private int districtCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
