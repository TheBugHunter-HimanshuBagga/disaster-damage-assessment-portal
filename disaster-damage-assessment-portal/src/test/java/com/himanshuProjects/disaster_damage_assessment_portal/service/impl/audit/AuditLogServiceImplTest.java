package com.himanshuProjects.disaster_damage_assessment_portal.service.impl.audit;

import com.himanshuProjects.disaster_damage_assessment_portal.dto.audit.AuditLogPageResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.audit.AuditLogResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.system.AuditLog;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.User;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.AuditAction;
import com.himanshuProjects.disaster_damage_assessment_portal.exception.BadRequestException;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.system.AuditLogRepository;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceImplTest {

    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private UserRepository userRepository;

    private AuditLogServiceImpl auditLogService;

    @BeforeEach
    void setUp() {
        auditLogService = new AuditLogServiceImpl(auditLogRepository, userRepository);
    }

    private User admin(long id) {
        return TestDataFactory.createDistrictAdmin(id, "admin" + id + "@example.com");
    }

    @Test
    @DisplayName("shouldSearchAuditLogsWithAllFilters")
    void shouldSearchAuditLogsWithAllFilters() {
        User admin = admin(1L);
        AuditLog log = TestDataFactory.createAuditLog(1L, admin, AuditAction.CREATE_REPORT, "DisasterReport", 10L);
        Page<AuditLog> page = new PageImpl<>(List.of(log), PageRequest.of(0, 10), 1);

        when(auditLogRepository.searchAuditLogs(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        AuditLogPageResponse response = auditLogService.searchAuditLogs(
                "report", AuditAction.CREATE_REPORT, "DisasterReport", admin.getEmail(),
                0, 10, "performedAt", "desc");

        assertThat(response).isNotNull();
        assertThat(response.getAuditLogs()).hasSize(1);
        assertThat(response.getAuditLogs().get(0).getId()).isEqualTo(1L);
        assertThat(response.getAuditLogs().get(0).getAction()).isEqualTo(AuditAction.CREATE_REPORT);
        assertThat(response.getAuditLogs().get(0).getPerformedByName()).isEqualTo(admin.getFullName());
        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getTotalPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("shouldSearchAuditLogsWithNullFilters")
    void shouldSearchAuditLogsWithNullFilters() {
        User admin = admin(1L);
        AuditLog log = TestDataFactory.createAuditLog(1L, admin, AuditAction.LOGIN, "User", 1L);
        Page<AuditLog> page = new PageImpl<>(List.of(log), PageRequest.of(0, 20), 1);

        when(auditLogRepository.searchAuditLogs(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        AuditLogPageResponse response = auditLogService.searchAuditLogs(
                null, null, null, null, 0, 20, "id", "asc");

        assertThat(response.getAuditLogs()).hasSize(1);
        assertThat(response.getAuditLogs().get(0).getAction()).isEqualTo(AuditAction.LOGIN);
    }

    @Test
    @DisplayName("shouldRejectInvalidSortFieldOnSearch")
    void shouldRejectInvalidSortFieldOnSearch() {
        assertThatThrownBy(() ->
                auditLogService.searchAuditLogs(null, null, null, null, 0, 10, "evil", "asc"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid sort field");

        verify(auditLogRepository, never()).searchAuditLogs(any(), any(), any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("shouldGetAuditLogsByEntity")
    void shouldGetAuditLogsByEntity() {
        User admin = admin(1L);
        AuditLog log1 = TestDataFactory.createAuditLog(1L, admin, AuditAction.UPDATE_REPORT, "DisasterReport", 10L);
        AuditLog log2 = TestDataFactory.createAuditLog(2L, admin, AuditAction.DELETE_REPORT, "DisasterReport", 10L);

        when(auditLogRepository.findByEntityNameAndEntityIdOrderByPerformedAtDesc("DisasterReport", 10L))
                .thenReturn(List.of(log1, log2));

        List<AuditLogResponse> response = auditLogService.getAuditLogsByEntity("DisasterReport", 10L);

        assertThat(response).hasSize(2);
        assertThat(response.get(0).getEntityId()).isEqualTo(10L);
        assertThat(response.get(0).getAction()).isEqualTo(AuditAction.UPDATE_REPORT);
        assertThat(response.get(1).getAction()).isEqualTo(AuditAction.DELETE_REPORT);
    }

    @Test
    @DisplayName("shouldReturnEmptyListWhenNoLogsForEntity")
    void shouldReturnEmptyListWhenNoLogsForEntity() {
        when(auditLogRepository.findByEntityNameAndEntityIdOrderByPerformedAtDesc("DisasterReport", 99L))
                .thenReturn(List.of());

        List<AuditLogResponse> response = auditLogService.getAuditLogsByEntity("DisasterReport", 99L);

        assertThat(response).isEmpty();
    }

    @Test
    @DisplayName("shouldGetMyAuditLogs")
    void shouldGetMyAuditLogs() {
        User admin = admin(1L);
        AuditLog log1 = TestDataFactory.createAuditLog(1L, admin, AuditAction.CREATE_REPORT, "DisasterReport", 5L);
        AuditLog log2 = TestDataFactory.createAuditLog(2L, admin, AuditAction.APPROVE_COMPENSATION, "Compensation", 7L);

        when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(auditLogRepository.findByPerformedByIdOrderByPerformedAtDesc(1L))
                .thenReturn(List.of(log1, log2));

        List<AuditLogResponse> response = auditLogService.getMyAuditLogs(admin.getEmail());

        assertThat(response).hasSize(2);
        assertThat(response.get(0).getPerformedByEmail()).isEqualTo(admin.getEmail());
        assertThat(response.get(1).getAction()).isEqualTo(AuditAction.APPROVE_COMPENSATION);
    }

    @Test
    @DisplayName("shouldReturnEmptyListWhenUserNotFoundOnGetMyAuditLogs")
    void shouldReturnEmptyListWhenUserNotFoundOnGetMyAuditLogs() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        List<AuditLogResponse> response = auditLogService.getMyAuditLogs("missing@example.com");

        assertThat(response).isEmpty();
        verify(auditLogRepository, never()).findByPerformedByIdOrderByPerformedAtDesc(any(Long.class));
    }
}
