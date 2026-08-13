package com.himanshuProjects.disaster_damage_assessment_portal.dto.notification;

import com.himanshuProjects.disaster_damage_assessment_portal.enums.NotificationType;
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
public class NotificationResponse {

    private Long id;
    private String title;
    private String message;
    private NotificationType notificationType;
    private Boolean isRead;
    private Long referenceId;
    private String entityType;
    private LocalDateTime createdAt;
}
