package com.himanshuProjects.disaster_damage_assessment_portal.repository;

import com.himanshuProjects.disaster_damage_assessment_portal.config.MySqlTestContainerConfig;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.disaster.DisasterReport;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.District;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.State;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.User;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.DisasterType;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.ReportStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.RoleType;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.disaster.DisasterReportRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.testutil.TestDataFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(MySqlTestContainerConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DisasterReportRepositoryTest {

    @Autowired
    private DisasterReportRepository reportRepository;

    @Autowired
    private EntityManager em;

    private int stateSeq;

    private User persistCitizen(String email, String phone) {
        State state = TestDataFactory.persistState(em, "St_" + email, String.format("S%02d", ++stateSeq));
        District district = TestDataFactory.persistDistrict(em, "Di_" + email, state);
        User user = TestDataFactory.createUser(null, email, RoleType.CITIZEN);
        user.setPhoneNumber(phone);
        user.setDistrict(district);
        em.persist(user);
        em.flush();
        return user;
    }

    private DisasterReport persistReport(User citizen, ReportStatus status, String title, DisasterType type) {
        DisasterReport report = TestDataFactory.createReport(null, citizen, status);
        report.setTitle(title);
        report.setDisasterType(type);
        report.setReportedAt(LocalDateTime.now());
        em.persist(report);
        em.flush();
        return report;
    }

    @Test
    @DisplayName("should find reports by citizen ordered by createdAt descending")
    void findByCitizenIdOrderByCreatedAtDesc_shouldOrderByCreatedAtDesc() {
        User c1 = persistCitizen("cit1@example.com", "9111111411");
        User c2 = persistCitizen("cit2@example.com", "9222222422");
        persistReport(c1, ReportStatus.SUBMITTED, "First flood", DisasterType.FLOOD);
        persistReport(c1, ReportStatus.APPROVED, "Second cyclone", DisasterType.CYCLONE);
        persistReport(c2, ReportStatus.SUBMITTED, "Other fire", DisasterType.FIRE);

        var page = reportRepository.findByCitizenIdOrderByCreatedAtDesc(c1.getId(), PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent()).extracting(DisasterReport::getCitizen).map(User::getId).containsOnly(c1.getId());
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("should count reports by status")
    void countByStatus_shouldCount() {
        persistReport(persistCitizen("a@example.com", "9111111412"), ReportStatus.SUBMITTED, "R1", DisasterType.FLOOD);
        persistReport(persistCitizen("b@example.com", "9222222423"), ReportStatus.SUBMITTED, "R2", DisasterType.FIRE);
        persistReport(persistCitizen("c@example.com", "9333333433"), ReportStatus.APPROVED, "R3", DisasterType.CYCLONE);

        assertThat(reportRepository.countByStatus(ReportStatus.SUBMITTED)).isEqualTo(2);
        assertThat(reportRepository.countByStatus(ReportStatus.APPROVED)).isEqualTo(1);
        assertThat(reportRepository.countByStatus(ReportStatus.COMPLETED)).isZero();
    }

    @Test
    @DisplayName("should count all reports for a citizen")
    void countByCitizenId_shouldCount() {
        User c1 = persistCitizen("cit3@example.com", "9111111413");
        persistReport(c1, ReportStatus.SUBMITTED, "R1", DisasterType.FLOOD);
        persistReport(c1, ReportStatus.REJECTED, "R2", DisasterType.FIRE);

        assertThat(reportRepository.countByCitizenId(c1.getId())).isEqualTo(2);
    }

    @Test
    @DisplayName("should count reports by citizen and status")
    void countByCitizenIdAndStatus_shouldCount() {
        User c1 = persistCitizen("cit4@example.com", "9111111414");
        persistReport(c1, ReportStatus.SUBMITTED, "R1", DisasterType.FLOOD);
        persistReport(c1, ReportStatus.SUBMITTED, "R2", DisasterType.FIRE);
        persistReport(c1, ReportStatus.APPROVED, "R3", DisasterType.CYCLONE);

        assertThat(reportRepository.countByCitizenIdAndStatus(c1.getId(), ReportStatus.SUBMITTED)).isEqualTo(2);
        assertThat(reportRepository.countByCitizenIdAndStatus(c1.getId(), ReportStatus.APPROVED)).isEqualTo(1);
    }

    @Test
    @DisplayName("should group report counts by status")
    void countGroupByStatus_shouldGroup() {
        persistReport(persistCitizen("g1@example.com", "9111111415"), ReportStatus.SUBMITTED, "R1", DisasterType.FLOOD);
        persistReport(persistCitizen("g2@example.com", "9222222426"), ReportStatus.SUBMITTED, "R2", DisasterType.FIRE);
        persistReport(persistCitizen("g3@example.com", "9333333437"), ReportStatus.APPROVED, "R3", DisasterType.CYCLONE);

        var rows = reportRepository.countGroupByStatus();

        assertThat(rows).hasSize(2);
        boolean submittedFound = false;
        boolean approvedFound = false;
        for (Object[] row : rows) {
            ReportStatus status = (ReportStatus) row[0];
            long count = (Long) row[1];
            if (status == ReportStatus.SUBMITTED) {
                submittedFound = true;
                assertThat(count).isEqualTo(2);
            }
            if (status == ReportStatus.APPROVED) {
                approvedFound = true;
                assertThat(count).isEqualTo(1);
            }
        }
        assertThat(submittedFound).isTrue();
        assertThat(approvedFound).isTrue();
    }

    @Test
    @DisplayName("should count monthly reports grouped by year and month")
    void countMonthlyReports_shouldGroupByYearMonth() {
        User citizen = persistCitizen("m@example.com", "9111111416");
        DisasterReport r1 = persistReport(citizen, ReportStatus.SUBMITTED, "R1", DisasterType.FLOOD);
        DisasterReport r2 = persistReport(citizen, ReportStatus.SUBMITTED, "R2", DisasterType.FIRE);
        em.createQuery("UPDATE DisasterReport r SET r.createdAt = :ts WHERE r.id = :id")
                .setParameter("ts", LocalDateTime.of(2024, 1, 15, 10, 0))
                .setParameter("id", r1.getId())
                .executeUpdate();
        em.createQuery("UPDATE DisasterReport r SET r.createdAt = :ts WHERE r.id = :id")
                .setParameter("ts", LocalDateTime.of(2024, 1, 20, 10, 0))
                .setParameter("id", r2.getId())
                .executeUpdate();
        em.flush();

        var rows = reportRepository.countMonthlyReports(LocalDateTime.of(2023, 12, 31, 0, 0));

        assertThat(rows).isNotEmpty();
        int found2024Jan = 0;
        for (Object[] row : rows) {
            int year = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            long count = ((Number) row[2]).longValue();
            if (year == 2024 && month == 1) {
                found2024Jan += (int) count;
            }
        }
        assertThat(found2024Jan).isEqualTo(2);
    }

    @Test
    @DisplayName("should search reports by search text, disaster type, status and citizen")
    void searchReports_shouldFilter() {
        User c1 = persistCitizen("s1@example.com", "9111111417");
        persistReport(c1, ReportStatus.SUBMITTED, "Heavy floods in valley", DisasterType.FLOOD);
        persistReport(c1, ReportStatus.APPROVED, "Wildfire near town", DisasterType.FIRE);

        var bySearch = reportRepository.searchReports("floods", null, null, null, PageRequest.of(0, 10));
        assertThat(bySearch.getContent()).hasSize(1);
        assertThat(bySearch.getContent().get(0).getTitle()).contains("floods");

        var byType = reportRepository.searchReports(null, DisasterType.FIRE, null, null, PageRequest.of(0, 10));
        assertThat(byType.getContent()).hasSize(1);

        var byStatus = reportRepository.searchReports(null, null, ReportStatus.APPROVED, null, PageRequest.of(0, 10));
        assertThat(byStatus.getContent()).hasSize(1);

        var byCitizen = reportRepository.searchReports(null, null, null, c1.getId(), PageRequest.of(0, 10));
        assertThat(byCitizen.getContent()).hasSize(2);
    }
}
