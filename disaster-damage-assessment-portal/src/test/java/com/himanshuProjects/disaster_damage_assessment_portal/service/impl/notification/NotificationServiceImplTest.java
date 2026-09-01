package com.himanshuProjects.disaster_damage_assessment_portal.service.impl.notification;

import com.himanshuProjects.disaster_damage_assessment_portal.dto.notification.NotificationPageResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.notification.NotificationResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.system.Notification;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.User;
import com.himanshuProjects.disaster_damage_assessment_portal.exception.BadRequestException;
import com.himanshuProjects.disaster_damage_assessment_portal.exception.ResourceNotFoundException;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.system.NotificationRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.user.UserRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.testutil.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserRepository userRepository;

    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationServiceImpl(notificationRepository, userRepository);
    }

    private User user(long id) {
        return TestDataFactory.createCitizen(id, "citizen@example.com");
    }

    @Test
    @DisplayName("shouldGetAllNotificationsForUser")
    void shouldGetAllNotificationsForUser() {
        User citizen = user(1L);
        Notification n1 = TestDataFactory.createNotification(1L, citizen, false);
        Notification n2 = TestDataFactory.createNotification(2L, citizen, true);
        Page<Notification> page = new PageImpl<>(List.of(n1, n2),
                org.springframework.data.domain.PageRequest.of(0, 10), 2);

        when(userRepository.findByEmail(citizen.getEmail())).thenReturn(Optional.of(citizen));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(any(Long.class), any(Pageable.class)))
                .thenReturn(page);

        NotificationPageResponse response =
                notificationService.getMyNotifications(citizen.getEmail(), null, 0, 10);

        assertThat(response).isNotNull();
        assertThat(response.getNotifications()).hasSize(2);
        assertThat(response.getPageNumber()).isZero();
        assertThat(response.getPageSize()).isEqualTo(10);
        assertThat(response.getTotalElements()).isEqualTo(2);
        assertThat(response.getTotalPages()).isEqualTo(1);
        assertThat(response.isLast()).isTrue();
        assertThat(response.getNotifications().get(0).getId()).isEqualTo(1L);
        assertThat(response.getNotifications().get(0).getIsRead()).isFalse();
        assertThat(response.getNotifications().get(1).getIsRead()).isTrue();

        verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class));
    }

    @Test
    @DisplayName("shouldGetUnreadNotificationsOnlyWhenRequested")
    void shouldGetUnreadNotificationsOnlyWhenRequested() {
        User citizen = user(1L);
        Notification unread = TestDataFactory.createNotification(1L, citizen, false);
        Page<Notification> page = new PageImpl<>(List.of(unread),
                org.springframework.data.domain.PageRequest.of(0, 5), 1);

        when(userRepository.findByEmail(citizen.getEmail())).thenReturn(Optional.of(citizen));
        when(notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(any(Long.class), any(Boolean.class), any(Pageable.class)))
                .thenReturn(page);

        NotificationPageResponse response =
                notificationService.getMyNotifications(citizen.getEmail(), true, 0, 5);

        assertThat(response.getNotifications()).hasSize(1);
        assertThat(response.getNotifications().get(0).getIsRead()).isFalse();

        verify(notificationRepository).findByUserIdAndIsReadOrderByCreatedAtDesc(eq(1L), eq(false), any(Pageable.class));
        verify(notificationRepository, never()).findByUserIdOrderByCreatedAtDesc(any(Long.class), any(Pageable.class));
    }

    @Test
    @DisplayName("shouldGetNotificationsWithUnreadOnlyFalseLikeAll")
    void shouldGetNotificationsWithUnreadOnlyFalseLikeAll() {
        User citizen = user(1L);
        Page<Notification> page = new PageImpl<>(List.of(),
                org.springframework.data.domain.PageRequest.of(0, 10), 0);

        when(userRepository.findByEmail(citizen.getEmail())).thenReturn(Optional.of(citizen));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(any(Long.class), any(Pageable.class)))
                .thenReturn(page);

        notificationService.getMyNotifications(citizen.getEmail(), false, 0, 10);

        verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class));
    }

    @Test
    @DisplayName("shouldThrowWhenUserNotFoundOnGetNotifications")
    void shouldThrowWhenUserNotFoundOnGetNotifications() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.getMyNotifications("missing@example.com", null, 0, 10))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");
    }

    @Test
    @DisplayName("shouldGetUnreadCount")
    void shouldGetUnreadCount() {
        User citizen = user(1L);
        when(userRepository.findByEmail(citizen.getEmail())).thenReturn(Optional.of(citizen));
        when(notificationRepository.countByUserIdAndIsReadFalse(1L)).thenReturn(3L);

        long count = notificationService.getUnreadCount(citizen.getEmail());

        assertThat(count).isEqualTo(3L);
        verify(notificationRepository).countByUserIdAndIsReadFalse(1L);
    }

    @Test
    @DisplayName("shouldThrowWhenUserNotFoundOnGetUnreadCount")
    void shouldThrowWhenUserNotFoundOnGetUnreadCount() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.getUnreadCount("missing@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("shouldMarkNotificationAsRead")
    void shouldMarkNotificationAsRead() {
        User citizen = user(1L);
        Notification notification = TestDataFactory.createNotification(1L, citizen, false);

        when(userRepository.findByEmail(citizen.getEmail())).thenReturn(Optional.of(citizen));
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        NotificationResponse response = notificationService.markAsRead(1L, citizen.getEmail());

        assertThat(response.getIsRead()).isTrue();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(notification.getIsRead()).isTrue();
        verify(notificationRepository).save(notification);
    }

    @Test
    @DisplayName("shouldThrowWhenUserNotFoundOnMarkAsRead")
    void shouldThrowWhenUserNotFoundOnMarkAsRead() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(1L, "missing@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("shouldThrowWhenNotificationNotFoundOnMarkAsRead")
    void shouldThrowWhenNotificationNotFoundOnMarkAsRead() {
        User citizen = user(1L);
        when(userRepository.findByEmail(citizen.getEmail())).thenReturn(Optional.of(citizen));
        when(notificationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(99L, citizen.getEmail()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Notification");
    }

    @Test
    @DisplayName("shouldRejectMarkingOthersNotificationAsRead")
    void shouldRejectMarkingOthersNotificationAsRead() {
        User owner = user(1L);
        User other = TestDataFactory.createCitizen(2L, "other@example.com");
        Notification notification = TestDataFactory.createNotification(1L, owner, false);

        when(userRepository.findByEmail(other.getEmail())).thenReturn(Optional.of(other));
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.markAsRead(1L, other.getEmail()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("mark your own notifications");

        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    @DisplayName("shouldMarkAllAsRead")
    void shouldMarkAllAsRead() {
        User citizen = user(1L);
        when(userRepository.findByEmail(citizen.getEmail())).thenReturn(Optional.of(citizen));

        notificationService.markAllAsRead(citizen.getEmail());

        verify(notificationRepository).markAllAsReadByUserId(1L);
    }

    @Test
    @DisplayName("shouldThrowWhenUserNotFoundOnMarkAllAsRead")
    void shouldThrowWhenUserNotFoundOnMarkAllAsRead() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAllAsRead("missing@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("shouldDeleteOwnNotification")
    void shouldDeleteOwnNotification() {
        User citizen = user(1L);
        Notification notification = TestDataFactory.createNotification(1L, citizen, false);

        when(userRepository.findByEmail(citizen.getEmail())).thenReturn(Optional.of(citizen));
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        notificationService.deleteNotification(1L, citizen.getEmail());

        verify(notificationRepository).delete(notification);
    }

    @Test
    @DisplayName("shouldThrowWhenUserNotFoundOnDelete")
    void shouldThrowWhenUserNotFoundOnDelete() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.deleteNotification(1L, "missing@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("shouldThrowWhenNotificationNotFoundOnDelete")
    void shouldThrowWhenNotificationNotFoundOnDelete() {
        User citizen = user(1L);
        when(userRepository.findByEmail(citizen.getEmail())).thenReturn(Optional.of(citizen));
        when(notificationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.deleteNotification(99L, citizen.getEmail()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Notification");
    }

    @Test
    @DisplayName("shouldRejectDeletingOthersNotification")
    void shouldRejectDeletingOthersNotification() {
        User owner = user(1L);
        User other = TestDataFactory.createCitizen(2L, "other@example.com");
        Notification notification = TestDataFactory.createNotification(1L, owner, false);

        when(userRepository.findByEmail(other.getEmail())).thenReturn(Optional.of(other));
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.deleteNotification(1L, other.getEmail()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("delete your own notifications");

        verify(notificationRepository, never()).delete(any(Notification.class));
    }
}
