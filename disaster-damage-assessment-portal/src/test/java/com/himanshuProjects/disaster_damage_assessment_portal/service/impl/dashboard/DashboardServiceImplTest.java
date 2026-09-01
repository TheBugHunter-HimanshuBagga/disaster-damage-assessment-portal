package com.himanshuProjects.disaster_damage_assessment_portal.service.impl.dashboard;

import com.himanshuProjects.disaster_damage_assessment_portal.dto.dashboard.AdminDashboardResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.dashboard.CitizenDashboardResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.dashboard.OfficerDashboardResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.User;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.AssignmentStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.CompensationStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.ReportStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.RoleType;
import com.himanshuProjects.disaster_damage_assessment_portal.exception.BadRequestException;
import com.himanshuProjects.disaster_damage_assessment_portal.exception.ResourceNotFoundException;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.compensation.CompensationRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.disaster.DisasterReportRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.disaster.OfficerAssignmentRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.user.UserRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.testutil.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private DisasterReportRepository reportRepository;
    @Mock
    private OfficerAssignmentRepository assignmentRepository;
    @Mock
    private CompensationRepository compensationRepository;

    private DashboardServiceImpl dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardServiceImpl(
                userRepository, reportRepository, assignmentRepository, compensationRepository);
    }

    // ── getAdminDashboard ──────────────────────────────────────────────

    @Test
    @DisplayName("shouldReturnAdminDashboardWithAllStats")
    void shouldReturnAdminDashboardWithAllStats() {
        when(userRepository.count()).thenReturn(50L);
        when(userRepository.countGroupByRole()).thenReturn(rows(
                new Object[]{"CITIZEN", 30L},
                new Object[]{"FIELD_OFFICER", 15L},
                new Object[]{"SUPER_ADMIN", 5L}
        ));

        when(reportRepository.count()).thenReturn(120L);
        when(reportRepository.countGroupByStatus()).thenReturn(rows(
                new Object[]{"SUBMITTED", 20L},
                new Object[]{"COMPLETED", 80L}
        ));
        when(reportRepository.countByStatus(ReportStatus.SUBMITTED)).thenReturn(20L);
        when(reportRepository.countByStatus(ReportStatus.ASSIGNED)).thenReturn(10L);
        when(reportRepository.countByStatus(ReportStatus.UNDER_INSPECTION)).thenReturn(5L);
        when(reportRepository.countByStatus(ReportStatus.UNDER_REVIEW)).thenReturn(3L);

        when(compensationRepository.count()).thenReturn(40L);
        when(compensationRepository.countByCompensationStatus(CompensationStatus.APPROVED)).thenReturn(25L);
        when(compensationRepository.countByCompensationStatus(CompensationStatus.PAID)).thenReturn(10L);
        when(compensationRepository.countGroupByStatus()).thenReturn(rows(
                new Object[]{"APPROVED", 25L},
                new Object[]{"PAID", 10L}
        ));
        when(compensationRepository.sumAmountByStatus(CompensationStatus.APPROVED))
                .thenReturn(new BigDecimal("50000"));
        when(compensationRepository.sumAmountByStatus(CompensationStatus.PAID))
                .thenReturn(new BigDecimal("30000"));
        when(compensationRepository.avgAmountByStatus(CompensationStatus.APPROVED))
                .thenReturn(new BigDecimal("2000"));

        when(assignmentRepository.countWorkloadGroupByOfficer()).thenReturn(rows(
                new Object[]{1L, "Officer A", "a@test.com", 5L, 3L, 8L}
        ));

        when(reportRepository.countMonthlyReports(any(LocalDateTime.class))).thenReturn(rows(
                new Object[]{2026, 1, 15L},
                new Object[]{2026, 2, 22L}
        ));

        AdminDashboardResponse response = dashboardService.getAdminDashboard();

        assertThat(response).isNotNull();
        assertThat(response.getTotalUsers()).isEqualTo(50L);
        assertThat(response.getUsersByRole()).containsEntry("CITIZEN", 30L);
        assertThat(response.getUsersByRole()).containsEntry("FIELD_OFFICER", 15L);

        assertThat(response.getTotalReports()).isEqualTo(120L);
        assertThat(response.getReportsByStatus()).containsEntry("SUBMITTED", 20L);
        assertThat(response.getPendingReports()).isEqualTo(38L);

        assertThat(response.getTotalCompensations()).isEqualTo(40L);
        assertThat(response.getApprovedCompensations()).isEqualTo(35L);
        assertThat(response.getCompensationsByStatus()).containsEntry("APPROVED", 25L);
        assertThat(response.getTotalCompensationAmount()).isEqualByComparingTo(new BigDecimal("80000"));
        assertThat(response.getAverageCompensationAmount()).isEqualByComparingTo(new BigDecimal("2000"));

        assertThat(response.getOfficerWorkloads()).hasSize(1);
        assertThat(response.getOfficerWorkloads().get(0).getOfficerId()).isEqualTo(1L);
        assertThat(response.getOfficerWorkloads().get(0).getOfficerName()).isEqualTo("Officer A");
        assertThat(response.getOfficerWorkloads().get(0).getActiveAssignments()).isEqualTo(5L);

        assertThat(response.getMonthlyReports()).hasSize(2);
        assertThat(response.getMonthlyReports().get(0).getYear()).isEqualTo(2026);
        assertThat(response.getMonthlyReports().get(0).getCount()).isEqualTo(15L);
    }

    @Test
    @DisplayName("shouldReturnEmptyAdminDashboardWhenNoData")
    void shouldReturnEmptyAdminDashboardWhenNoData() {
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.countGroupByRole()).thenReturn(List.of());
        when(reportRepository.count()).thenReturn(0L);
        when(reportRepository.countGroupByStatus()).thenReturn(List.of());
        when(reportRepository.countByStatus(any(ReportStatus.class))).thenReturn(0L);
        when(compensationRepository.count()).thenReturn(0L);
        when(compensationRepository.countByCompensationStatus(any(CompensationStatus.class))).thenReturn(0L);
        when(compensationRepository.countGroupByStatus()).thenReturn(List.of());
        when(compensationRepository.sumAmountByStatus(any(CompensationStatus.class)))
                .thenReturn(BigDecimal.ZERO);
        when(compensationRepository.avgAmountByStatus(any(CompensationStatus.class)))
                .thenReturn(BigDecimal.ZERO);
        when(assignmentRepository.countWorkloadGroupByOfficer()).thenReturn(List.of());
        when(reportRepository.countMonthlyReports(any(LocalDateTime.class))).thenReturn(List.of());

        AdminDashboardResponse response = dashboardService.getAdminDashboard();

        assertThat(response.getTotalUsers()).isZero();
        assertThat(response.getTotalReports()).isZero();
        assertThat(response.getPendingReports()).isZero();
        assertThat(response.getTotalCompensations()).isZero();
        assertThat(response.getApprovedCompensations()).isZero();
        assertThat(response.getTotalCompensationAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getOfficerWorkloads()).isEmpty();
        assertThat(response.getMonthlyReports()).isEmpty();
    }

    // ── getOfficerDashboard ────────────────────────────────────────────

    @Test
    @DisplayName("shouldReturnOfficerDashboardWithStats")
    void shouldReturnOfficerDashboardWithStats() {
        User officer = TestDataFactory.createOfficer(1L, "officer@test.com");
        when(userRepository.findByEmail("officer@test.com")).thenReturn(Optional.of(officer));

        when(assignmentRepository.countByFieldOfficerId(1L)).thenReturn(20L);
        when(assignmentRepository.countByFieldOfficerIdAndAssignmentStatus(1L, AssignmentStatus.ACCEPTED))
                .thenReturn(3L);
        when(assignmentRepository.countByFieldOfficerIdAndAssignmentStatus(1L, AssignmentStatus.IN_PROGRESS))
                .thenReturn(5L);
        when(assignmentRepository.countByFieldOfficerIdAndAssignmentStatus(1L, AssignmentStatus.COMPLETED))
                .thenReturn(10L);
        when(assignmentRepository.countByFieldOfficerIdAndAssignmentStatus(1L, AssignmentStatus.REASSIGNED))
                .thenReturn(1L);
        when(assignmentRepository.countByFieldOfficerIdAndAssignmentStatus(1L, AssignmentStatus.ASSIGNED))
                .thenReturn(1L);

        OfficerDashboardResponse response = dashboardService.getOfficerDashboard("officer@test.com");

        assertThat(response).isNotNull();
        assertThat(response.getOfficerId()).isEqualTo(1L);
        assertThat(response.getOfficerName()).isEqualTo("Test FIELD_OFFICER");
        assertThat(response.getTotalAssigned()).isEqualTo(20L);
        assertThat(response.getAccepted()).isEqualTo(3L);
        assertThat(response.getInProgress()).isEqualTo(5L);
        assertThat(response.getCompleted()).isEqualTo(10L);
        assertThat(response.getReassigned()).isEqualTo(1L);
        assertThat(response.getPendingInspections()).isEqualTo(1L);
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundWhenOfficerEmailDoesNotExist")
    void shouldThrowResourceNotFoundWhenOfficerEmailDoesNotExist() {
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dashboardService.getOfficerDashboard("missing@test.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("email");
    }

    @Test
    @DisplayName("shouldThrowBadRequestWhenNonOfficerAccessesOfficerDashboard")
    void shouldThrowBadRequestWhenNonOfficerAccessesOfficerDashboard() {
        User citizen = TestDataFactory.createCitizen(2L, "citizen@test.com");
        when(userRepository.findByEmail("citizen@test.com")).thenReturn(Optional.of(citizen));

        assertThatThrownBy(() -> dashboardService.getOfficerDashboard("citizen@test.com"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("FIELD_OFFICER");
    }

    // ── getCitizenDashboard ────────────────────────────────────────────

    @Test
    @DisplayName("shouldReturnCitizenDashboardWithStats")
    void shouldReturnCitizenDashboardWithStats() {
        User citizen = TestDataFactory.createCitizen(1L, "citizen@test.com");
        when(userRepository.findByEmail("citizen@test.com")).thenReturn(Optional.of(citizen));

        when(reportRepository.countByCitizenId(1L)).thenReturn(8L);
        when(reportRepository.countByCitizenIdAndStatus(1L, ReportStatus.SUBMITTED)).thenReturn(2L);
        when(reportRepository.countByCitizenIdAndStatus(1L, ReportStatus.ASSIGNED)).thenReturn(1L);
        when(reportRepository.countByCitizenIdAndStatus(1L, ReportStatus.UNDER_INSPECTION)).thenReturn(1L);
        when(reportRepository.countByCitizenIdAndStatus(1L, ReportStatus.UNDER_REVIEW)).thenReturn(1L);
        when(reportRepository.countByCitizenIdAndStatus(1L, ReportStatus.COMPLETED)).thenReturn(3L);

        when(compensationRepository.countByDamageAssessmentDisasterReportCitizenId(1L)).thenReturn(2L);
        when(compensationRepository.sumAmountByCitizenId(1L)).thenReturn(new BigDecimal("15000"));

        CitizenDashboardResponse response = dashboardService.getCitizenDashboard("citizen@test.com");

        assertThat(response).isNotNull();
        assertThat(response.getCitizenId()).isEqualTo(1L);
        assertThat(response.getCitizenName()).isEqualTo("Test CITIZEN");
        assertThat(response.getTotalReports()).isEqualTo(8L);
        assertThat(response.getPendingReports()).isEqualTo(5L);
        assertThat(response.getCompletedReports()).isEqualTo(3L);
        assertThat(response.getTotalCompensations()).isEqualTo(2L);
        assertThat(response.getTotalCompensationReceived()).isEqualByComparingTo(new BigDecimal("15000"));
    }

    @Test
    @DisplayName("shouldReturnZeroStatsWhenCitizenHasNoReports")
    void shouldReturnZeroStatsWhenCitizenHasNoReports() {
        User citizen = TestDataFactory.createCitizen(1L, "citizen@test.com");
        when(userRepository.findByEmail("citizen@test.com")).thenReturn(Optional.of(citizen));

        when(reportRepository.countByCitizenId(1L)).thenReturn(0L);
        when(reportRepository.countByCitizenIdAndStatus(eq(1L), any(ReportStatus.class))).thenReturn(0L);
        when(compensationRepository.countByDamageAssessmentDisasterReportCitizenId(1L)).thenReturn(0L);
        when(compensationRepository.sumAmountByCitizenId(1L)).thenReturn(BigDecimal.ZERO);

        CitizenDashboardResponse response = dashboardService.getCitizenDashboard("citizen@test.com");

        assertThat(response.getTotalReports()).isZero();
        assertThat(response.getPendingReports()).isZero();
        assertThat(response.getCompletedReports()).isZero();
        assertThat(response.getTotalCompensations()).isZero();
        assertThat(response.getTotalCompensationReceived()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundWhenCitizenEmailDoesNotExist")
    void shouldThrowResourceNotFoundWhenCitizenEmailDoesNotExist() {
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dashboardService.getCitizenDashboard("missing@test.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("email");
    }

    @Test
    @DisplayName("shouldThrowBadRequestWhenNonCitizenAccessesCitizenDashboard")
    void shouldThrowBadRequestWhenNonCitizenAccessesCitizenDashboard() {
        User officer = TestDataFactory.createOfficer(3L, "officer@test.com");
        when(userRepository.findByEmail("officer@test.com")).thenReturn(Optional.of(officer));

        assertThatThrownBy(() -> dashboardService.getCitizenDashboard("officer@test.com"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("CITIZEN");
    }

    private java.util.List<Object[]> rows(Object[]... values) {
        return java.util.Arrays.asList(values);
    }
}
