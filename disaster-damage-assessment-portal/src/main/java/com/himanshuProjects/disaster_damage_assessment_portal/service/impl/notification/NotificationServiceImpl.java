package com.himanshuProjects.disaster_damage_assessment_portal.service.impl.notification;

import com.himanshuProjects.disaster_damage_assessment_portal.dto.notification.NotificationPageResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.notification.NotificationResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.system.Notification;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.User;
import com.himanshuProjects.disaster_damage_assessment_portal.exception.BadRequestException;
import com.himanshuProjects.disaster_damage_assessment_portal.exception.ResourceNotFoundException;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.system.NotificationRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.user.UserRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.service.notification.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                    UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationPageResponse getMyNotifications(String userEmail, Boolean unreadOnly,
                                                        int page, int size) {
        log.info("Fetching notifications for user: {}, unread: {}", userEmail, unreadOnly);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> pageResult;

        if (unreadOnly != null && unreadOnly) {
            pageResult = notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(
                    user.getId(), false, pageable);
        } else {
            pageResult = notificationRepository.findByUserIdOrderByCreatedAtDesc(
                    user.getId(), pageable);
        }

        List<NotificationResponse> notifications = pageResult.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return NotificationPageResponse.builder()
                .notifications(notifications)
                .pageNumber(pageResult.getNumber())
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(String userEmail) {
        log.info("Fetching unread count for user: {}", userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        return notificationRepository.countByUserIdAndIsReadFalse(user.getId());
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(Long notificationId, String userEmail) {
        log.info("Marking notification ID: {} as read by user: {}", notificationId, userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You can only mark your own notifications as read");
        }

        notification.setIsRead(true);
        Notification saved = notificationRepository.save(notification);

        log.info("Notification ID: {} marked as read", notificationId);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void markAllAsRead(String userEmail) {
        log.info("Marking all notifications as read for user: {}", userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        notificationRepository.markAllAsReadByUserId(user.getId());
        log.info("All notifications marked as read for user: {}", userEmail);
    }

    @Override
    @Transactional
    public void deleteNotification(Long notificationId, String userEmail) {
        log.info("Deleting notification ID: {} by user: {}", notificationId, userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You can only delete your own notifications");
        }

        notificationRepository.delete(notification);
        log.info("Notification ID: {} deleted", notificationId);
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .notificationType(notification.getNotificationType())
                .isRead(notification.getIsRead())
                .referenceId(notification.getReferenceId())
                .entityType(notification.getEntityType())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
