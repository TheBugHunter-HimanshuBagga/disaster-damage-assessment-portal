package com.himanshuProjects.disaster_damage_assessment_portal.repository;

import com.himanshuProjects.disaster_damage_assessment_portal.config.MySqlTestContainerConfig;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.disaster.DisasterReport;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.disaster.OfficerAssignment;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.District;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.State;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.User;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.AssignmentStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.ReportStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.RoleType;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.disaster.OfficerAssignmentRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(MySqlTestContainerConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OfficerAssignmentRepositoryTest {

    @Autowired
    private OfficerAssignmentRepository assignmentRepository;

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

    private DisasterReport persistReport(User citizen) {
        DisasterReport report = TestDataFactory.createReport(null, citizen, ReportStatus.ASSIGNED);
        em.persist(report);
        em.flush();
        return report;
    }

    @Test
    @DisplayName("should return true if a report has an assignment in given statuses")
    void existsByDisasterReportIdAndAssignmentStatusIn_shouldCheck() {
        User citizen = persistUser("cit@example.com", "9111111511", RoleType.CITIZEN);
        User officer = persistUser("off@example.com", "9222222522", RoleType.FIELD_OFFICER);
        DisasterReport report = persistReport(citizen);
        TestDataFactory.persistAssignment(em, report, officer, AssignmentStatus.ASSIGNED);

        boolean exists = assignmentRepository.existsByDisasterReportIdAndAssignmentStatusIn(
                report.getId(), List.of(AssignmentStatus.ASSIGNED, AssignmentStatus.COMPLETED));
        assertThat(exists).isTrue();

        boolean notExists = assignmentRepository.existsByDisasterReportIdAndAssignmentStatusIn(
                report.getId(), List.of(AssignmentStatus.COMPLETED));
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("should find assignment by report id and statuses")
    void findByDisasterReportIdAndAssignmentStatusIn_shouldFind() {
        User citizen = persistUser("cit2@example.com", "9111111512", RoleType.CITIZEN);
        User officer = persistUser("off2@example.com", "9222222523", RoleType.FIELD_OFFICER);
        DisasterReport report = persistReport(citizen);
        TestDataFactory.persistAssignment(em, report, officer, AssignmentStatus.ACCEPTED);

        Optional<OfficerAssignment> found = assignmentRepository.findByDisasterReportIdAndAssignmentStatusIn(
                report.getId(), List.of(AssignmentStatus.ASSIGNED, AssignmentStatus.ACCEPTED));
        assertThat(found).isPresent();
        assertThat(found.get().getFieldOfficer().getId()).isEqualTo(officer.getId());

        Optional<OfficerAssignment> notFound = assignmentRepository.findByDisasterReportIdAndAssignmentStatusIn(
                report.getId(), List.of(AssignmentStatus.COMPLETED));
        assertThat(notFound).isEmpty();
    }

    @Test
    @DisplayName("should find assignment by disaster report id")
    void findByDisasterReportId_shouldFind() {
        User citizen = persistUser("cit3@example.com", "9111111513", RoleType.CITIZEN);
        User officer = persistUser("off3@example.com", "9222222524", RoleType.FIELD_OFFICER);
        DisasterReport report = persistReport(citizen);
        TestDataFactory.persistAssignment(em, report, officer, AssignmentStatus.IN_PROGRESS);

        Optional<OfficerAssignment> found = assignmentRepository.findByDisasterReportId(report.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getDisasterReport().getId()).isEqualTo(report.getId());
    }

    @Test
    @DisplayName("should find assignments by field officer and statuses")
    void findByFieldOfficerIdAndAssignmentStatusIn_shouldFind() {
        User citizen = persistUser("cit4@example.com", "9111111514", RoleType.CITIZEN);
        User officer = persistUser("off4@example.com", "9222222525", RoleType.FIELD_OFFICER);
        TestDataFactory.persistAssignment(em, persistReport(citizen), officer, AssignmentStatus.ASSIGNED);
        TestDataFactory.persistAssignment(em, persistReport(citizen), officer, AssignmentStatus.COMPLETED);

        List<OfficerAssignment> active = assignmentRepository.findByFieldOfficerIdAndAssignmentStatusIn(
                officer.getId(), List.of(AssignmentStatus.ASSIGNED, AssignmentStatus.ACCEPTED));
        assertThat(active).hasSize(1);
        assertThat(active.get(0).getAssignmentStatus()).isEqualTo(AssignmentStatus.ASSIGNED);
    }

    @Test
    @DisplayName("should count assignments by officer and status")
    void countByFieldOfficerIdAndAssignmentStatus_shouldCount() {
        User citizen = persistUser("cit5@example.com", "9111111515", RoleType.CITIZEN);
        User officer = persistUser("off5@example.com", "9222222526", RoleType.FIELD_OFFICER);
        TestDataFactory.persistAssignment(em, persistReport(citizen), officer, AssignmentStatus.COMPLETED);
        TestDataFactory.persistAssignment(em, persistReport(citizen), officer, AssignmentStatus.COMPLETED);
        TestDataFactory.persistAssignment(em, persistReport(citizen), officer, AssignmentStatus.ASSIGNED);

        assertThat(assignmentRepository.countByFieldOfficerIdAndAssignmentStatus(officer.getId(), AssignmentStatus.COMPLETED)).isEqualTo(2);
        assertThat(assignmentRepository.countByFieldOfficerIdAndAssignmentStatus(officer.getId(), AssignmentStatus.ASSIGNED)).isEqualTo(1);
    }

    @Test
    @DisplayName("should count total assignments by officer")
    void countByFieldOfficerId_shouldCount() {
        User citizen = persistUser("cit6@example.com", "9111111516", RoleType.CITIZEN);
        User officer = persistUser("off6@example.com", "9222222527", RoleType.FIELD_OFFICER);
        TestDataFactory.persistAssignment(em, persistReport(citizen), officer, AssignmentStatus.COMPLETED);
        TestDataFactory.persistAssignment(em, persistReport(citizen), officer, AssignmentStatus.ASSIGNED);
        TestDataFactory.persistAssignment(em, persistReport(citizen), officer, AssignmentStatus.IN_PROGRESS);

        assertThat(assignmentRepository.countByFieldOfficerId(officer.getId())).isEqualTo(3);
    }

    @Test
    @DisplayName("should group workload per officer")
    void countWorkloadGroupByOfficer_shouldAggregate() {
        User citizen = persistUser("cit7@example.com", "9111111517", RoleType.CITIZEN);
        User officer = persistUser("off7@example.com", "9222222528", RoleType.FIELD_OFFICER);
        TestDataFactory.persistAssignment(em, persistReport(citizen), officer, AssignmentStatus.ASSIGNED);
        TestDataFactory.persistAssignment(em, persistReport(citizen), officer, AssignmentStatus.COMPLETED);

        var rows = assignmentRepository.countWorkloadGroupByOfficer();

        assertThat(rows).hasSize(1);
        Object[] row = rows.get(0);
        assertThat(((Number) row[3]).longValue()).isEqualTo(1);
        assertThat(((Number) row[4]).longValue()).isEqualTo(1);
        assertThat(((Number) row[5]).longValue()).isEqualTo(2);
    }

    @Test
    @DisplayName("should search assignments by search text, status and officer")
    void searchAssignments_shouldFilter() {
        User citizen = persistUser("cit8@example.com", "9111111518", RoleType.CITIZEN);
        User officer = persistUser("off8@example.com", "9222222529", RoleType.FIELD_OFFICER);
        DisasterReport r1 = persistReport(citizen);
        r1.setTitle("Bridge collapse report");
        em.merge(r1);
        em.flush();
        TestDataFactory.persistAssignment(em, r1, officer, AssignmentStatus.ASSIGNED);
        TestDataFactory.persistAssignment(em, persistReport(citizen), officer, AssignmentStatus.COMPLETED);

        var bySearch = assignmentRepository.searchAssignments("collapse", null, null, PageRequest.of(0, 10));
        assertThat(bySearch.getContent()).hasSize(1);

        var byStatus = assignmentRepository.searchAssignments(null, AssignmentStatus.COMPLETED, null, PageRequest.of(0, 10));
        assertThat(byStatus.getContent()).hasSize(1);

        var byOfficer = assignmentRepository.searchAssignments(null, null, officer.getId(), PageRequest.of(0, 10));
        assertThat(byOfficer.getContent()).hasSize(2);
    }
}
