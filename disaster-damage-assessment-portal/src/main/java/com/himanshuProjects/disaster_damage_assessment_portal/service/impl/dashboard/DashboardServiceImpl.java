package com.himanshuProjects.disaster_damage_assessment_portal.service.impl.dashboard;

import com.himanshuProjects.disaster_damage_assessment_portal.dto.dashboard.AdminDashboardResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.dashboard.CitizenDashboardResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.dashboard.MonthlyReportStat;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.dashboard.OfficerDashboardResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.dashboard.OfficerWorkloadResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.disaster.OfficerAssignment;
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
import com.himanshuProjects.disaster_damage_assessment_portal.service.dashboard.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardServiceImpl implements DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardServiceImpl.class);

    private final UserRepository userRepository;
    private final DisasterReportRepository reportRepository;
    private final OfficerAssignmentRepository assignmentRepository;
    private final CompensationRepository compensationRepository;

    public DashboardServiceImpl(UserRepository userRepository,
                                 DisasterReportRepository reportRepository,
                                 OfficerAssignmentRepository assignmentRepository,
                                 CompensationRepository compensationRepository) {
        this.userRepository = userRepository;
        this.reportRepository = reportRepository;
        this.assignmentRepository = assignmentRepository;
        this.compensationRepository = compensationRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardResponse getAdminDashboard() {
        log.info("Fetching admin dashboard statistics");

        long totalUsers = userRepository.count();

        Map<String, Long> usersByRole = new HashMap<>();
        List<Object[]> roleCounts = userRepository.countGroupByRole();
        for (Object[] row : roleCounts) {
            usersByRole.put(row[0].toString(), (Long) row[1]);
        }

        long totalReports = reportRepository.count();

        Map<String, Long> reportsByStatus = new HashMap<>();
        List<Object[]> statusCounts = reportRepository.countGroupByStatus();
        for (Object[] row : statusCounts) {
            reportsByStatus.put(row[0].toString(), (Long) row[1]);
        }

        long pendingReports = reportRepository.countByStatus(ReportStatus.SUBMITTED)
                + reportRepository.countByStatus(ReportStatus.ASSIGNED)
                + reportRepository.countByStatus(ReportStatus.UNDER_INSPECTION)
                + reportRepository.countByStatus(ReportStatus.UNDER_REVIEW);

        long totalCompensations = compensationRepository.count();
        long approvedCompensations = compensationRepository.countByCompensationStatus(CompensationStatus.APPROVED)
                + compensationRepository.countByCompensationStatus(CompensationStatus.PAID);

        Map<String, Long> compensationsByStatus = new HashMap<>();
        List<Object[]> compStatusCounts = compensationRepository.countGroupByStatus();
        for (Object[] row : compStatusCounts) {
            compensationsByStatus.put(row[0].toString(), (Long) row[1]);
        }

        BigDecimal totalCompensationAmount = compensationRepository.sumAmountByStatus(CompensationStatus.APPROVED)
                .add(compensationRepository.sumAmountByStatus(CompensationStatus.PAID));

        BigDecimal avgAmount = compensationRepository.avgAmountByStatus(CompensationStatus.APPROVED);

        List<Object[]> workloadData = assignmentRepository.countWorkloadGroupByOfficer();
        List<OfficerWorkloadResponse> workloads = workloadData.stream()
                .map(row -> OfficerWorkloadResponse.builder()
                        .officerId((Long) row[0])
                        .officerName((String) row[1])
                        .officerEmail((String) row[2])
                        .activeAssignments((Long) row[3])
                        .completedAssignments((Long) row[4])
                        .totalAssignments((Long) row[5])
                        .build())
                .toList();

        LocalDateTime sixMonthsAgo = YearMonth.now().minusMonths(5).atDay(1).atStartOfDay();
        List<Object[]> monthlyData = reportRepository.countMonthlyReports(sixMonthsAgo);
        List<MonthlyReportStat> monthlyReports = new ArrayList<>();
        for (Object[] row : monthlyData) {
            monthlyReports.add(MonthlyReportStat.builder()
                    .year(((Number) row[0]).intValue())
                    .month(((Number) row[1]).intValue())
                    .count((Long) row[2])
                    .build());
        }

        return AdminDashboardResponse.builder()
                .totalUsers(totalUsers)
                .usersByRole(usersByRole)
                .totalReports(totalReports)
                .reportsByStatus(reportsByStatus)
                .pendingReports(pendingReports)
                .totalCompensations(totalCompensations)
                .compensationsByStatus(compensationsByStatus)
                .approvedCompensations(approvedCompensations)
                .totalCompensationAmount(totalCompensationAmount)
                .averageCompensationAmount(avgAmount)
                .officerWorkloads(workloads)
                .monthlyReports(monthlyReports)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public OfficerDashboardResponse getOfficerDashboard(String officerEmail) {
        log.info("Fetching officer dashboard for: {}", officerEmail);

        User officer = userRepository.findByEmail(officerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", officerEmail));

        if (officer.getRole() != RoleType.FIELD_OFFICER) {
            throw new BadRequestException("Only FIELD_OFFICER can access officer dashboard");
        }

        long totalAssigned = assignmentRepository.countByFieldOfficerId(officer.getId());
        long accepted = assignmentRepository.countByFieldOfficerIdAndAssignmentStatus(
                officer.getId(), AssignmentStatus.ACCEPTED);
        long inProgress = assignmentRepository.countByFieldOfficerIdAndAssignmentStatus(
                officer.getId(), AssignmentStatus.IN_PROGRESS);
        long completed = assignmentRepository.countByFieldOfficerIdAndAssignmentStatus(
                officer.getId(), AssignmentStatus.COMPLETED);
        long reassigned = assignmentRepository.countByFieldOfficerIdAndAssignmentStatus(
                officer.getId(), AssignmentStatus.REASSIGNED);
        long pendingInspections = assignmentRepository.countByFieldOfficerIdAndAssignmentStatus(
                officer.getId(), AssignmentStatus.ASSIGNED);

        return OfficerDashboardResponse.builder()
                .officerId(officer.getId())
                .officerName(officer.getFullName())
                .totalAssigned(totalAssigned)
                .accepted(accepted)
                .inProgress(inProgress)
                .completed(completed)
                .reassigned(reassigned)
                .pendingInspections(pendingInspections)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CitizenDashboardResponse getCitizenDashboard(String citizenEmail) {
        log.info("Fetching citizen dashboard for: {}", citizenEmail);

        User citizen = userRepository.findByEmail(citizenEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", citizenEmail));

        if (citizen.getRole() != RoleType.CITIZEN) {
            throw new BadRequestException("Only CITIZEN can access citizen dashboard");
        }

        long totalReports = reportRepository.countByCitizenId(citizen.getId());
        long pendingReports = reportRepository.countByCitizenIdAndStatus(citizen.getId(), ReportStatus.SUBMITTED)
                + reportRepository.countByCitizenIdAndStatus(citizen.getId(), ReportStatus.ASSIGNED)
                + reportRepository.countByCitizenIdAndStatus(citizen.getId(), ReportStatus.UNDER_INSPECTION)
                + reportRepository.countByCitizenIdAndStatus(citizen.getId(), ReportStatus.UNDER_REVIEW);
        long completedReports = reportRepository.countByCitizenIdAndStatus(citizen.getId(), ReportStatus.COMPLETED);

        long totalCompensations = compensationRepository.countByDamageAssessmentDisasterReportCitizenId(citizen.getId());
        BigDecimal totalCompensationReceived = compensationRepository.sumAmountByCitizenId(citizen.getId());

        return CitizenDashboardResponse.builder()
                .citizenId(citizen.getId())
                .citizenName(citizen.getFullName())
                .totalReports(totalReports)
                .pendingReports(pendingReports)
                .completedReports(completedReports)
                .totalCompensations(totalCompensations)
                .totalCompensationReceived(totalCompensationReceived)
                .build();
    }
}
