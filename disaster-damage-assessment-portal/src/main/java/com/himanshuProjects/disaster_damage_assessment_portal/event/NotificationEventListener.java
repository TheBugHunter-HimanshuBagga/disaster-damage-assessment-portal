package com.himanshuProjects.disaster_damage_assessment_portal.event;

import com.himanshuProjects.disaster_damage_assessment_portal.entity.system.Notification;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.User;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.system.NotificationRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationEventListener(NotificationRepository notificationRepository,
                                      UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Async
    @EventListener
    public void handleNotificationEvent(NotificationEvent event) {
        log.info("Received notification event: {} for user ID: {}",
                event.getNotificationType(), event.getUserId());

        User user = userRepository.findById(event.getUserId()).orElse(null);
        if (user == null) {
            log.warn("User not found for notification: ID {}", event.getUserId());
            return;
        }

        Notification notification = new Notification();
        notification.setTitle(event.getTitle());
        notification.setMessage(event.getMessage());
        notification.setNotificationType(event.getNotificationType());
        notification.setReferenceId(event.getReferenceId());
        notification.setEntityType(event.getEntityType());
        notification.setUser(user);

        notificationRepository.save(notification);
        log.info("Notification created: {} for user: {}",
                event.getNotificationType(), user.getEmail());
    }
}
