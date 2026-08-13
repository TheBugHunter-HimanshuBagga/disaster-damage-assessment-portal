package com.himanshuProjects.disaster_damage_assessment_portal.dto.feedback;

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
public class FeedbackResponse {

    private Long id;
    private Integer rating;
    private String comments;
    private LocalDateTime submittedAt;

    private Long userId;
    private String userName;
    private String userEmail;

    private Long reportId;
    private String reportTitle;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
