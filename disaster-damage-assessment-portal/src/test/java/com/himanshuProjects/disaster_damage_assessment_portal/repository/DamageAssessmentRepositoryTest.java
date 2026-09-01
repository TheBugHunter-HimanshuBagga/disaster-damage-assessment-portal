package com.himanshuProjects.disaster_damage_assessment_portal.repository;

import com.himanshuProjects.disaster_damage_assessment_portal.config.MySqlTestContainerConfig;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.disaster.DamageAssessment;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.disaster.DisasterReport;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.District;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.State;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.User;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.DamageLevel;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.ReportStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.RoleType;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.disaster.DamageAssessmentRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.testutil.TestDataFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(MySqlTestContainerConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DamageAssessmentRepositoryTest {

    @Autowired
    private DamageAssessmentRepository assessmentRepository;

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
        DisasterReport report = TestDataFactory.createReport(null, citizen, ReportStatus.UNDER_INSPECTION);
        report.setTitle(title);
        em.persist(report);
        em.flush();
        return report;
    }

    @Test
    @DisplayName("should return true when assessment exists for report")
    void existsByDisasterReportId_shouldCheck() {
        User citizen = persistUser("cit@example.com", "9111111611", RoleType.CITIZEN);
        User officer = persistUser("off@example.com", "9222222612", RoleType.FIELD_OFFICER);
        DisasterReport report = persistReport(citizen, "Damage report 1");
        TestDataFactory.persistAssessment(em, report, officer, DamageLevel.HIGH, new BigDecimal("10000.00"));

        assertThat(assessmentRepository.existsByDisasterReportId(report.getId())).isTrue();

        DisasterReport other = persistReport(citizen, "Damage report 2");
        assertThat(assessmentRepository.existsByDisasterReportId(other.getId())).isFalse();
    }

    @Test
    @DisplayName("should find assessment by disaster report id")
    void findByDisasterReportId_shouldFind() {
        User citizen = persistUser("cit2@example.com", "9111111612", RoleType.CITIZEN);
        User officer = persistUser("off2@example.com", "9222222613", RoleType.FIELD_OFFICER);
        DisasterReport report = persistReport(citizen, "Damage report 3");
        TestDataFactory.persistAssessment(em, report, officer, DamageLevel.MEDIUM, new BigDecimal("10000.00"));

        Optional<DamageAssessment> found = assessmentRepository.findByDisasterReportId(report.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getDamageLevel()).isEqualTo(DamageLevel.MEDIUM);
    }

    @Test
    @DisplayName("should find assessment by officer and report")
    void findByFieldOfficerIdAndDisasterReportId_shouldFind() {
        User citizen = persistUser("cit3@example.com", "9111111613", RoleType.CITIZEN);
        User officer = persistUser("off3@example.com", "9222222614", RoleType.FIELD_OFFICER);
        DisasterReport report = persistReport(citizen, "Damage report 4");
        TestDataFactory.persistAssessment(em, report, officer, DamageLevel.SEVERE, new BigDecimal("10000.00"));

        Optional<DamageAssessment> found = assessmentRepository.findByFieldOfficerIdAndDisasterReportId(officer.getId(), report.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getFieldOfficer().getId()).isEqualTo(officer.getId());
        assertThat(found.get().getDisasterReport().getId()).isEqualTo(report.getId());
    }

    @Test
    @DisplayName("should search assessments by search text, damage level and officer")
    void searchAssessments_shouldFilter() {
        User citizen = persistUser("cit4@example.com", "9111111614", RoleType.CITIZEN);
        User officer = persistUser("off4@example.com", "9222222615", RoleType.FIELD_OFFICER);
        DisasterReport floodReport = persistReport(citizen, "Severe flood damage");
        TestDataFactory.persistAssessment(em, floodReport, officer, DamageLevel.HIGH, new BigDecimal("10000.00"));

        var bySearch = assessmentRepository.searchAssessments("flood", null, null, PageRequest.of(0, 10));
        assertThat(bySearch.getContent()).hasSize(1);

        var byLevel = assessmentRepository.searchAssessments(null, DamageLevel.HIGH, null, PageRequest.of(0, 10));
        assertThat(byLevel.getContent()).hasSize(1);

        var byOfficer = assessmentRepository.searchAssessments(null, null, officer.getId(), PageRequest.of(0, 10));
        assertThat(byOfficer.getContent()).hasSize(1);
        assertThat(byOfficer.getContent()).extracting(DamageAssessment::getDisasterReport).extracting(DisasterReport::getTitle).containsExactly("Severe flood damage");
    }
}
