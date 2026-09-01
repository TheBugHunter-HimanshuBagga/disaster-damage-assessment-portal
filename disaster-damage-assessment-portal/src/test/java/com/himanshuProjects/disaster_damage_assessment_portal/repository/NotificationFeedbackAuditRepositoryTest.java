package com.himanshuProjects.disaster_damage_assessment_portal.repository;

import com.himanshuProjects.disaster_damage_assessment_portal.config.MySqlTestContainerConfig;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.disaster.DisasterReport;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.system.AuditLog;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.system.Feedback;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.system.Notification;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.District;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.State;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.User;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.AuditAction;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.ReportStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.RoleType;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.system.AuditLogRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.system.FeedbackRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.system.NotificationRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.testutil.TestDataFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(MySqlTestContainerConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class NotificationFeedbackAuditRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private EntityManager em;

    private int stateSeq;

    private User persistUser(String email, String phone, RoleType role) {
        State state = TestDataFactory.persistState(em, "St_" + email, String.format("S%02d", ++stateSeq));
        District district = TestDataFactory.persistDistrict(em, "Di_" + email, state);
        User user = TestDataFactory.createUser(null, email, role);
        user.setPhoneNumber(phone);
        user.setDistrict(district);
        em.persist(user);
        em.flush();
        return user;
    }

    private DisasterReport persistReport(User citizen, String title) {
        DisasterReport report = TestDataFactory.createReport(null, citizen, ReportStatus.COMPLETED);
        report.setTitle(title);
        em.persist(report);
        em.flush();
        return report;
    }

    // ---- Notification ----

    @Test
    @DisplayName("should count unread notifications for a user")
    void countByUserIdAndIsReadFalse_shouldCountUnread() {
        User user = persistUser("u1@example.com", "9111111811", RoleType.CITIZEN);
        TestDataFactory.persistNotification(em, user, false);
        TestDataFactory.persistNotification(em, user, false);
        TestDataFactory.persistNotification(em, user, true);

        assertThat(notificationRepository.countByUserIdAndIsReadFalse(user.getId())).isEqualTo(2);
    }

    @Test
    @DisplayName("should find notifications by user ordered by createdAt desc")
    void findByUserIdOrderByCreatedAtDesc_shouldOrder() {
        User user = persistUser("u2@example.com", "9111111812", RoleType.CITIZEN);
        Notification n1 = TestDataFactory.persistNotification(em, user, false);
        Notification n2 = TestDataFactory.persistNotification(em, user, true);

        var page = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent()).extracting(Notification::getId).containsExactly(n2.getId(), n1.getId());
    }

    @Test
    @DisplayName("should find notifications by user and read status")
    void findByUserIdAndIsReadOrderByCreatedAtDesc_shouldFilter() {
        User user = persistUser("u3@example.com", "9111111813", RoleType.CITIZEN);
        TestDataFactory.persistNotification(em, user, false);
        TestDataFactory.persistNotification(em, user, false);
        TestDataFactory.persistNotification(em, user, true);

        var unread = notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(user.getId(), false, PageRequest.of(0, 10));
        assertThat(unread.getContent()).hasSize(2);

        var read = notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(user.getId(), true, PageRequest.of(0, 10));
        assertThat(read.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("should mark all notifications as read for a user")
    void markAllAsReadByUserId_shouldUpdateAll() {
        User user = persistUser("u4@example.com", "9111111814", RoleType.CITIZEN);
        TestDataFactory.persistNotification(em, user, false);
        TestDataFactory.persistNotification(em, user, false);

        notificationRepository.markAllAsReadByUserId(user.getId());
        em.clear();

        assertThat(notificationRepository.countByUserIdAndIsReadFalse(user.getId())).isZero();
        long readCount = notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(user.getId(), true, PageRequest.of(0, 10)).getTotalElements();
        assertThat(readCount).isEqualTo(2);
    }

    @Test
    @DisplayName("should mark a single notification as read by id and user")
    void markAsReadById_shouldUpdateOne() {
        User user = persistUser("u5@example.com", "9111111815", RoleType.CITIZEN);
        User other = persistUser("uOther@example.com", "9222222266", RoleType.CITIZEN);
        Notification n1 = TestDataFactory.persistNotification(em, user, false);
        TestDataFactory.persistNotification(em, user, false);

        notificationRepository.markAsReadById(n1.getId(), user.getId());
        em.clear();

        assertThat(notificationRepository.countByUserIdAndIsReadFalse(user.getId())).isEqualTo(1);
    }

    // ---- Feedback ----

    @Test
    @DisplayName("should check feedback existence by disaster report id")
    void existsByDisasterReportId_shouldCheck() {
        User citizen = persistUser("f1@example.com", "9111111821", RoleType.CITIZEN);
        DisasterReport report = persistReport(citizen, "Feedback report 1");
        TestDataFactory.persistFeedback(em, citizen, report, 5);

        assertThat(feedbackRepository.existsByDisasterReportId(report.getId())).isTrue();

        DisasterReport other = persistReport(citizen, "Feedback report 2");
        assertThat(feedbackRepository.existsByDisasterReportId(other.getId())).isFalse();
    }

    @Test
    @DisplayName("should check feedback existence by user and disaster report")
    void existsByUserIdAndDisasterReportId_shouldCheck() {
        User citizen = persistUser("f2@example.com", "9111111822", RoleType.CITIZEN);
        DisasterReport report = persistReport(citizen, "Feedback report 3");
        TestDataFactory.persistFeedback(em, citizen, report, 4);

        assertThat(feedbackRepository.existsByUserIdAndDisasterReportId(citizen.getId(), report.getId())).isTrue();
    }

    @Test
    @DisplayName("should search feedbacks by search text and rating")
    void searchFeedbacks_shouldFilter() {
        User citizen = persistUser("f3@example.com", "9111111823", RoleType.CITIZEN);
        citizen.setFullName("Rahul Kumar");
        em.merge(citizen);
        em.flush();

        DisasterReport report = persistReport(citizen, "Amazing disaster response");
        Feedback f1 = TestDataFactory.createFeedback(null, citizen, report, 5);
        f1.setComments("Very fast response by team");
        em.persist(f1);

        DisasterReport report2 = persistReport(citizen, "Another report");
        Feedback f2 = TestDataFactory.createFeedback(null, citizen, report2, 2);
        f2.setComments("Slow response");
        em.persist(f2);
        em.flush();

        var bySearch = feedbackRepository.searchFeedbacks("fast", null, PageRequest.of(0, 10));
        assertThat(bySearch.getContent()).hasSize(1);

        var byUser = feedbackRepository.searchFeedbacks("rahul", null, PageRequest.of(0, 10));
        assertThat(byUser.getContent()).hasSize(2);

        var byRating = feedbackRepository.searchFeedbacks(null, 5, PageRequest.of(0, 10));
        assertThat(byRating.getContent()).hasSize(1);
        assertThat(byRating.getContent().get(0).getRating()).isEqualTo(5);
    }

    // ---- AuditLog ----

    @Test
    @DisplayName("should find audit logs by entity name and entity id ordered by performedAt desc")
    void findByEntityNameAndEntityIdOrderByPerformedAtDesc_shouldFind() {
        User user = persistUser("a1@example.com", "9111111831", RoleType.DISTRICT_ADMIN);
        AuditLog l1 = TestDataFactory.persistAuditLog(em, user, AuditAction.CREATE_REPORT, "DISASTER_REPORT", 100L);

        var logs = auditLogRepository.findByEntityNameAndEntityIdOrderByPerformedAtDesc("DISASTER_REPORT", 100L);

        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getId()).isEqualTo(l1.getId());

        var none = auditLogRepository.findByEntityNameAndEntityIdOrderByPerformedAtDesc("DISASTER_REPORT", 999L);
        assertThat(none).isEmpty();
    }

    @Test
    @DisplayName("should find audit logs by performed by ordered by performedAt desc")
    void findByPerformedByIdOrderByPerformedAtDesc_shouldFind() {
        User user = persistUser("a2@example.com", "9111111832", RoleType.DISTRICT_ADMIN);
        AuditLog l1 = TestDataFactory.persistAuditLog(em, user, AuditAction.APPROVE_COMPENSATION, "COMPENSATION", 1L);
        AuditLog l2 = TestDataFactory.persistAuditLog(em, user, AuditAction.LOGIN, "USER", 2L);

        List<AuditLog> logs = auditLogRepository.findByPerformedByIdOrderByPerformedAtDesc(user.getId());

        assertThat(logs).hasSize(2);
        assertThat(logs).extracting(AuditLog::getId).containsExactlyInAnyOrder(l1.getId(), l2.getId());
    }

    @Test
    @DisplayName("should search audit logs by search text, action and entity name")
    void searchAuditLogs_shouldFilter() {
        User user = persistUser("a3@example.com", "9111111833", RoleType.DISTRICT_ADMIN);
        AuditLog l1 = TestDataFactory.createAuditLog(null, user, AuditAction.CREATE_REPORT, "DISASTER_REPORT", 10L);
        l1.setDescription("Citizen created a flood report");
        em.persist(l1);
        AuditLog l2 = TestDataFactory.createAuditLog(null, user, AuditAction.APPROVE_COMPENSATION, "COMPENSATION", 20L);
        l2.setDescription("Admin approved compensation");
        em.persist(l2);
        em.flush();

        var bySearch = auditLogRepository.searchAuditLogs("flood", null, null, null, PageRequest.of(0, 10));
        assertThat(bySearch.getContent()).hasSize(1);

        var byAction = auditLogRepository.searchAuditLogs(null, AuditAction.APPROVE_COMPENSATION, null, null, PageRequest.of(0, 10));
        assertThat(byAction.getContent()).hasSize(1);

        var byEntity = auditLogRepository.searchAuditLogs(null, null, "DISASTER_REPORT", null, PageRequest.of(0, 10));
        assertThat(byEntity.getContent()).hasSize(1);

        var byEmail = auditLogRepository.searchAuditLogs(null, null, null, "a3@example.com", PageRequest.of(0, 10));
        assertThat(byEmail.getContent()).hasSize(2);
    }
}