package com.himanshuProjects.disaster_damage_assessment_portal.repository;

import com.himanshuProjects.disaster_damage_assessment_portal.config.MySqlTestContainerConfig;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.compensation.Compensation;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.disaster.DamageAssessment;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.disaster.DisasterReport;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.District;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.State;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.User;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.CompensationStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.DamageLevel;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.PaymentStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.ReportStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.RoleType;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.compensation.CompensationRepository;
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
class CompensationRepositoryTest {

    @Autowired
    private CompensationRepository compensationRepository;

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
        DisasterReport report = TestDataFactory.createReport(null, citizen, ReportStatus.APPROVED);
        report.setTitle(title);
        em.persist(report);
        em.flush();
        return report;
    }

    private Compensation persistCompensation(DamageAssessment assessment, User admin, CompensationStatus status, BigDecimal amount) {
        Compensation comp = TestDataFactory.createCompensation(null, assessment, admin, status, amount);
        comp.setApprovedAt(admin);
        em.persist(comp);
        em.flush();
        return comp;
    }

    @Test
    @DisplayName("should check existence by damage assessment id")
    void existsByDamageAssessmentId_shouldCheck() {
        User citizen = persistUser("cit@example.com", "9111111711", RoleType.CITIZEN);
        User officer = persistUser("off@example.com", "9222222712", RoleType.FIELD_OFFICER);
        User admin = persistUser("adm@example.com", "9333333733", RoleType.DISTRICT_ADMIN);
        DamageAssessment assessment = TestDataFactory.persistAssessment(em, persistReport(citizen, "Comp report 1"), officer, DamageLevel.HIGH, new BigDecimal("1000.00"));
        persistCompensation(assessment, admin, CompensationStatus.PENDING, new BigDecimal("25000.00"));

        assertThat(compensationRepository.existsByDamageAssessmentId(assessment.getId())).isTrue();
    }

    @Test
    @DisplayName("should find compensation by damage assessment id")
    void findByDamageAssessmentId_shouldFind() {
        User citizen = persistUser("cit2@example.com", "9111111712", RoleType.CITIZEN);
        User officer = persistUser("off2@example.com", "9222222713", RoleType.FIELD_OFFICER);
        User admin = persistUser("adm2@example.com", "9333333734", RoleType.DISTRICT_ADMIN);
        DamageAssessment assessment = TestDataFactory.persistAssessment(em, persistReport(citizen, "Comp report 2"), officer, DamageLevel.HIGH, new BigDecimal("1000.00"));
        persistCompensation(assessment, admin, CompensationStatus.APPROVED, new BigDecimal("30000.00"));

        Optional<Compensation> found = compensationRepository.findByDamageAssessmentId(assessment.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getCompensationStatus()).isEqualTo(CompensationStatus.APPROVED);
    }

    @Test
    @DisplayName("should count compensations by status")
    void countByCompensationStatus_shouldCount() {
        User citizen = persistUser("cit3@example.com", "9111111713", RoleType.CITIZEN);
        User officer = persistUser("off3@example.com", "9222222714", RoleType.FIELD_OFFICER);
        User admin = persistUser("adm3@example.com", "9333333735", RoleType.DISTRICT_ADMIN);
        persistCompensation(TestDataFactory.persistAssessment(em, persistReport(citizen, "Comp report 3"), officer, DamageLevel.HIGH, new BigDecimal("1000.00")), admin, CompensationStatus.APPROVED, new BigDecimal("1.00"));
        persistCompensation(TestDataFactory.persistAssessment(em, persistReport(citizen, "Comp report 4"), officer, DamageLevel.HIGH, new BigDecimal("1000.00")), admin, CompensationStatus.APPROVED, new BigDecimal("2.00"));
        persistCompensation(TestDataFactory.persistAssessment(em, persistReport(citizen, "Comp report 5"), officer, DamageLevel.HIGH, new BigDecimal("1000.00")), admin, CompensationStatus.PENDING, new BigDecimal("3.00"));

        assertThat(compensationRepository.countByCompensationStatus(CompensationStatus.APPROVED)).isEqualTo(2);
        assertThat(compensationRepository.countByCompensationStatus(CompensationStatus.PENDING)).isEqualTo(1);
    }

    @Test
    @DisplayName("should sum amount by status")
    void sumAmountByStatus_shouldSum() {
        User citizen = persistUser("cit4@example.com", "9111111714", RoleType.CITIZEN);
        User officer = persistUser("off4@example.com", "9222222715", RoleType.FIELD_OFFICER);
        User admin = persistUser("adm4@example.com", "9333333736", RoleType.DISTRICT_ADMIN);
        persistCompensation(TestDataFactory.persistAssessment(em, persistReport(citizen, "Comp report 6"), officer, DamageLevel.HIGH, new BigDecimal("1000.00")), admin, CompensationStatus.APPROVED, new BigDecimal("100.00"));
        persistCompensation(TestDataFactory.persistAssessment(em, persistReport(citizen, "Comp report 7"), officer, DamageLevel.HIGH, new BigDecimal("1000.00")), admin, CompensationStatus.APPROVED, new BigDecimal("200.00"));
        persistCompensation(TestDataFactory.persistAssessment(em, persistReport(citizen, "Comp report 8"), officer, DamageLevel.HIGH, new BigDecimal("1000.00")), admin, CompensationStatus.PENDING, new BigDecimal("50.00"));

        BigDecimal sum = compensationRepository.sumAmountByStatus(CompensationStatus.APPROVED);
        assertThat(sum).isEqualByComparingTo("300.00");

        BigDecimal pendingSum = compensationRepository.sumAmountByStatus(CompensationStatus.PENDING);
        assertThat(pendingSum).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("should average amount by status")
    void avgAmountByStatus_shouldAverage() {
        User citizen = persistUser("cit5@example.com", "9111111715", RoleType.CITIZEN);
        User officer = persistUser("off5@example.com", "9222222716", RoleType.FIELD_OFFICER);
        User admin = persistUser("adm5@example.com", "9333333737", RoleType.DISTRICT_ADMIN);
        persistCompensation(TestDataFactory.persistAssessment(em, persistReport(citizen, "Comp report 9"), officer, DamageLevel.HIGH, new BigDecimal("1000.00")), admin, CompensationStatus.APPROVED, new BigDecimal("100.00"));
        persistCompensation(TestDataFactory.persistAssessment(em, persistReport(citizen, "Comp report 10"), officer, DamageLevel.HIGH, new BigDecimal("1000.00")), admin, CompensationStatus.APPROVED, new BigDecimal("300.00"));

        BigDecimal avg = compensationRepository.avgAmountByStatus(CompensationStatus.APPROVED);
        assertThat(avg).isEqualByComparingTo("200.00");
    }

    @Test
    @DisplayName("should group compensation counts by status")
    void countGroupByStatus_shouldGroup() {
        User citizen = persistUser("cit6@example.com", "9111111716", RoleType.CITIZEN);
        User officer = persistUser("off6@example.com", "9222222717", RoleType.FIELD_OFFICER);
        User admin = persistUser("adm6@example.com", "9333333738", RoleType.DISTRICT_ADMIN);
        persistCompensation(TestDataFactory.persistAssessment(em, persistReport(citizen, "Comp report 11"), officer, DamageLevel.HIGH, new BigDecimal("1000.00")), admin, CompensationStatus.APPROVED, new BigDecimal("1.00"));
        persistCompensation(TestDataFactory.persistAssessment(em, persistReport(citizen, "Comp report 12"), officer, DamageLevel.HIGH, new BigDecimal("1000.00")), admin, CompensationStatus.APPROVED, new BigDecimal("2.00"));
        persistCompensation(TestDataFactory.persistAssessment(em, persistReport(citizen, "Comp report 13"), officer, DamageLevel.HIGH, new BigDecimal("1000.00")), admin, CompensationStatus.REJECTED, new BigDecimal("3.00"));

        var rows = compensationRepository.countGroupByStatus();

        assertThat(rows).hasSize(2);
        boolean approved = false;
        boolean rejected = false;
        for (Object[] row : rows) {
            CompensationStatus status = (CompensationStatus) row[0];
            long count = (Long) row[1];
            if (status == CompensationStatus.APPROVED) {
                approved = true;
                assertThat(count).isEqualTo(2);
            }
            if (status == CompensationStatus.REJECTED) {
                rejected = true;
                assertThat(count).isEqualTo(1);
            }
        }
        assertThat(approved).isTrue();
        assertThat(rejected).isTrue();
    }

    @Test
    @DisplayName("should count compensations by citizen id via report chain")
    void countByDamageAssessmentDisasterReportCitizenId_shouldCount() {
        User citizen = persistUser("cit7@example.com", "9111111717", RoleType.CITIZEN);
        User officer = persistUser("off7@example.com", "9222222718", RoleType.FIELD_OFFICER);
        User admin = persistUser("adm7@example.com", "9333333739", RoleType.DISTRICT_ADMIN);
        persistCompensation(TestDataFactory.persistAssessment(em, persistReport(citizen, "Comp report 14"), officer, DamageLevel.HIGH, new BigDecimal("1000.00")), admin, CompensationStatus.APPROVED, new BigDecimal("1.00"));
        persistCompensation(TestDataFactory.persistAssessment(em, persistReport(citizen, "Comp report 15"), officer, DamageLevel.HIGH, new BigDecimal("1000.00")), admin, CompensationStatus.PENDING, new BigDecimal("2.00"));

        assertThat(compensationRepository.countByDamageAssessmentDisasterReportCitizenId(citizen.getId())).isEqualTo(2);
    }

    @Test
    @DisplayName("should sum approved amount by citizen id")
    void sumAmountByCitizenId_shouldSumApprovedOnly() {
        User citizen = persistUser("cit8@example.com", "9111111718", RoleType.CITIZEN);
        User officer = persistUser("off8@example.com", "9222222719", RoleType.FIELD_OFFICER);
        User admin = persistUser("adm8@example.com", "9333333740", RoleType.DISTRICT_ADMIN);
        persistCompensation(TestDataFactory.persistAssessment(em, persistReport(citizen, "Comp report 16"), officer, DamageLevel.HIGH, new BigDecimal("1000.00")), admin, CompensationStatus.APPROVED, new BigDecimal("400.00"));
        persistCompensation(TestDataFactory.persistAssessment(em, persistReport(citizen, "Comp report 17"), officer, DamageLevel.HIGH, new BigDecimal("1000.00")), admin, CompensationStatus.APPROVED, new BigDecimal("600.00"));
        persistCompensation(TestDataFactory.persistAssessment(em, persistReport(citizen, "Comp report 18"), officer, DamageLevel.HIGH, new BigDecimal("1000.00")), admin, CompensationStatus.PENDING, new BigDecimal("99999.00"));

        BigDecimal sum = compensationRepository.sumAmountByCitizenId(citizen.getId());
        assertThat(sum).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("should search compensations by search text, status and payment status")
    void searchCompensations_shouldFilter() {
        User citizen = persistUser("cit9@example.com", "9111111719", RoleType.CITIZEN);
        User officer = persistUser("off9@example.com", "9222222720", RoleType.FIELD_OFFICER);
        User admin = persistUser("adm9@example.com", "9333333741", RoleType.DISTRICT_ADMIN);

        DisasterReport r1 = persistReport(citizen, "Flood compensation case");
        r1.setDescription("Damaged by flood waters");
        em.merge(r1);
        DamageAssessment a1 = TestDataFactory.persistAssessment(em, r1, officer, DamageLevel.HIGH, new BigDecimal("1000.00"));
        a1.setRecommendation("The house was destroyed by the flood");
        em.merge(a1);
        em.flush();
        Compensation c1 = persistCompensation(a1, admin, CompensationStatus.APPROVED, new BigDecimal("100.00"));

        DamageAssessment a2 = TestDataFactory.persistAssessment(em, persistReport(citizen, "Cyclone compensation case"), officer, DamageLevel.HIGH, new BigDecimal("1000.00"));
        a2.setRecommendation("Cyclone damaged roof");
        em.merge(a2);
        em.flush();
        persistCompensation(a2, admin, CompensationStatus.PENDING, new BigDecimal("200.00"));

        var bySearch = compensationRepository.searchCompensations("flood", null, null, PageRequest.of(0, 10));
        assertThat(bySearch.getContent()).hasSize(1);
        assertThat(bySearch.getContent().get(0).getId()).isEqualTo(c1.getId());

        var byStatus = compensationRepository.searchCompensations(null, CompensationStatus.PENDING, null, PageRequest.of(0, 10));
        assertThat(byStatus.getContent()).hasSize(1);

        var byPayment = compensationRepository.searchCompensations(null, null, PaymentStatus.NOT_INITIATED, PageRequest.of(0, 10));
        assertThat(byPayment.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("should find compensations by citizen id via report chain")
    void findByCitizenId_shouldFind() {
        User citizen = persistUser("cit10@example.com", "9111111720", RoleType.CITIZEN);
        User other = persistUser("oth@example.com", "9111111799", RoleType.CITIZEN);
        User officer = persistUser("off10@example.com", "9222222721", RoleType.FIELD_OFFICER);
        User admin = persistUser("adm10@example.com", "9333333742", RoleType.DISTRICT_ADMIN);
        persistCompensation(TestDataFactory.persistAssessment(em, persistReport(citizen, "Comp report 19"), officer, DamageLevel.HIGH, new BigDecimal("1000.00")), admin, CompensationStatus.APPROVED, new BigDecimal("1.00"));
        persistCompensation(TestDataFactory.persistAssessment(em, persistReport(citizen, "Comp report 20"), officer, DamageLevel.HIGH, new BigDecimal("1000.00")), admin, CompensationStatus.PENDING, new BigDecimal("2.00"));
        persistCompensation(TestDataFactory.persistAssessment(em, persistReport(other, "Comp report 21"), officer, DamageLevel.HIGH, new BigDecimal("1000.00")), admin, CompensationStatus.APPROVED, new BigDecimal("3.00"));

        var page = compensationRepository.findByCitizenId(citizen.getId(), PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(2);
    }
}