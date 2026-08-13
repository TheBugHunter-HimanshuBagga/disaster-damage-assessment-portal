package com.himanshuProjects.disaster_damage_assessment_portal.event;

import com.himanshuProjects.disaster_damage_assessment_portal.enums.NotificationType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class NotificationEvent extends ApplicationEvent {

    private final Long userId;
    private final NotificationType notificationType;
    private final String title;
    private final String message;
    private final Long referenceId;
    private final String entityType;

    public NotificationEvent(Object source, Long userId, NotificationType notificationType,
                              String title, String message, Long referenceId, String entityType) {
        super(source);
        this.userId = userId;
        this.notificationType = notificationType;
        this.title = title;
        this.message = message;
        this.referenceId = referenceId;
        this.entityType = entityType;
    }
}
