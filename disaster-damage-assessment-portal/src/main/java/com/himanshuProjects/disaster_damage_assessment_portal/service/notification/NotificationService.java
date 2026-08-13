package com.himanshuProjects.disaster_damage_assessment_portal.service.notification;

import com.himanshuProjects.disaster_damage_assessment_portal.dto.notification.NotificationPageResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.notification.NotificationResponse;

public interface NotificationService {

    NotificationPageResponse getMyNotifications(String userEmail, Boolean unreadOnly,
                                                 int page, int size);

    long getUnreadCount(String userEmail);

    NotificationResponse markAsRead(Long notificationId, String userEmail);

    void markAllAsRead(String userEmail);

    void deleteNotification(Long notificationId, String userEmail);
}
