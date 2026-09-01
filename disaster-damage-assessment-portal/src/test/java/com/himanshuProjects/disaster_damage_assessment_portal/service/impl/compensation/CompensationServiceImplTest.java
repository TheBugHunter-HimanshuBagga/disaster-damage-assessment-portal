package com.himanshuProjects.disaster_damage_assessment_portal.service.impl.compensation;

import com.himanshuProjects.disaster_damage_assessment_portal.dto.compensation.ApproveCompensationRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.compensation.CompensationHistoryResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.compensation.CompensationPageResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.compensation.CompensationResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.compensation.CreateCompensationRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.compensation.RejectCompensationRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.compensation.UpdateCompensationRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.compensation.UpdatePaymentStatusRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.compensation.Compensation;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.compensation.CompensationStatusLog;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.disaster.DamageAssessment;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.disaster.DisasterReport;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.User;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.CompensationStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.DamageLevel;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.PaymentStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.ReportStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.RoleType;
import com.himanshuProjects.disaster_damage_assessment_portal.exception.BadRequestException;
import com.himanshuProjects.disaster_damage_assessment_portal.exception.ConflictException;
import com.himanshuProjects.disaster_damage_assessment_portal.exception.ResourceNotFoundException;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.compensation.CompensationRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.compensation.CompensationStatusLogRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.disaster.DamageAssessmentRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.disaster.DisasterReportRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.user.UserRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.testutil.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompensationServiceImplTest {

    @Mock
    private CompensationRepository compensationRepository;
    @Mock
    private CompensationStatusLogRepository statusLogRepository;
    @Mock
    private DamageAssessmentRepository assessmentRepository;
    @Mock
    private DisasterReportRepository reportRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private CompensationServiceImpl compensationService;

    @BeforeEach
    void setUp() {
        compensationService = new CompensationServiceImpl(
                compensationRepository, statusLogRepository, assessmentRepository,
                reportRepository, userRepository, eventPublisher);
    }

    private User defaultAdmin() {
        return TestDataFactory.createSuperAdmin(1L, "admin@example.com");
    }

    private User defaultOfficer() {
        return TestDataFactory.createOfficer(2L, "officer@example.com");
    }

    private User defaultCitizen() {
        return TestDataFactory.createCitizen(10L, "citizen@example.com");
    }

    private DamageAssessment underReviewAssessment() {
        DisasterReport report = TestDataFactory.createReport(100L, defaultCitizen(), ReportStatus.UNDER_REVIEW);
        return TestDataFactory.createAssessment(300L, report, defaultOfficer(),
                DamageLevel.HIGH, new BigDecimal("50000.00"));
    }

    private Compensation pendingCompensation(DamageAssessment assessment) {
        return TestDataFactory.createCompensation(400L, assessment, null,
                CompensationStatus.PENDING, new BigDecimal("25000.00"));
    }

    private CreateCompensationRequest createRequest() {
        CreateCompensationRequest request = new CreateCompensationRequest();
        request.setDamageAssessmentId(300L);
        request.setApprovedAmount(new BigDecimal("25000.00"));
        request.setRemarks("Initial compensation");
        return request;
    }

    // ==================== createCompensation ====================

    @Test
    @DisplayName("shouldCreateCompensationSuccessfullyAndMoveReportToApproved")
    void shouldCreateCompensationSuccessfullyAndMoveReportToApproved() {
        User admin = defaultAdmin();
        DamageAssessment assessment = underReviewAssessment();
        DisasterReport report = assessment.getDisasterReport();

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(assessmentRepository.findById(300L)).thenReturn(Optional.of(assessment));
        when(compensationRepository.existsByDamageAssessmentId(300L)).thenReturn(false);
        when(compensationRepository.save(any(Compensation.class))).thenAnswer(inv -> {
            Compensation c = inv.getArgument(0);
            c.setId(400L);
            return c;
        });

        CompensationResponse response =
                compensationService.createCompensation("admin@example.com", createRequest());

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(400L);
        assertThat(response.getApprovedAmount()).isEqualByComparingTo("25000.00");
        assertThat(response.getCompensationStatus()).isEqualTo(CompensationStatus.PENDING);
        assertThat(response.getReportId()).isEqualTo(100L);

        assertThat(report.getStatus()).isEqualTo(ReportStatus.APPROVED);
        verify(reportRepository).save(report);
        verify(statusLogRepository).save(any(CompensationStatusLog.class));
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenAdminNotFoundOnCreate")
    void shouldThrowResourceNotFoundExceptionWhenAdminNotFoundOnCreate() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> compensationService.createCompensation("missing@example.com", createRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");
    }

    @Test
    @DisplayName("shouldThrowBadRequestExceptionWhenNonAdminTriesToCreate")
    void shouldThrowBadRequestExceptionWhenNonAdminTriesToCreate() {
        User citizen = defaultCitizen();
        when(userRepository.findByEmail("citizen@example.com")).thenReturn(Optional.of(citizen));

        assertThatThrownBy(() -> compensationService.createCompensation("citizen@example.com", createRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only SUPER_ADMIN or DISTRICT_ADMIN");
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenAssessmentNotFoundOnCreate")
    void shouldThrowResourceNotFoundExceptionWhenAssessmentNotFoundOnCreate() {
        User admin = defaultAdmin();
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(assessmentRepository.findById(999L)).thenReturn(Optional.empty());

        CreateCompensationRequest request = createRequest();
        request.setDamageAssessmentId(999L);

        assertThatThrownBy(() -> compensationService.createCompensation("admin@example.com", request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("DamageAssessment");
    }

    @Test
    @DisplayName("shouldThrowConflictExceptionWhenCompensationAlreadyExists")
    void shouldThrowConflictExceptionWhenCompensationAlreadyExists() {
        User admin = defaultAdmin();
        DamageAssessment assessment = underReviewAssessment();
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(assessmentRepository.findById(300L)).thenReturn(Optional.of(assessment));
        when(compensationRepository.existsByDamageAssessmentId(300L)).thenReturn(true);

        assertThatThrownBy(() -> compensationService.createCompensation("admin@example.com", createRequest()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("shouldThrowBadRequestExceptionWhenReportNotUnderReviewOnCreate")
    void shouldThrowBadRequestExceptionWhenReportNotUnderReviewOnCreate() {
        User admin = defaultAdmin();
        DisasterReport report = TestDataFactory.createReport(100L, defaultCitizen(), ReportStatus.APPROVED);
        DamageAssessment assessment = TestDataFactory.createAssessment(300L, report, defaultOfficer(),
                DamageLevel.HIGH, new BigDecimal("50000.00"));

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(assessmentRepository.findById(300L)).thenReturn(Optional.of(assessment));
        when(compensationRepository.existsByDamageAssessmentId(300L)).thenReturn(false);

        assertThatThrownBy(() -> compensationService.createCompensation("admin@example.com", createRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot create compensation");
    }

    // ==================== getCompensationById ====================

    @Test
    @DisplayName("shouldReturnCompensationById")
    void shouldReturnCompensationById() {
        DamageAssessment assessment = underReviewAssessment();
        Compensation compensation = pendingCompensation(assessment);
        when(compensationRepository.findById(400L)).thenReturn(Optional.of(compensation));

        CompensationResponse response = compensationService.getCompensationById(400L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(400L);
        assertThat(response.getApprovedAmount()).isEqualByComparingTo("25000.00");
        assertThat(response.getDamageAssessmentId()).isEqualTo(300L);
        assertThat(response.getCitizenName()).isEqualTo("Test CITIZEN");
        verify(compensationRepository).findById(400L);
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenCompensationNotFoundById")
    void shouldThrowResourceNotFoundExceptionWhenCompensationNotFoundById() {
        when(compensationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> compensationService.getCompensationById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Compensation");
    }

    // ==================== updateCompensation ====================

    @Test
    @DisplayName("shouldUpdateCompensationAmountSuccessfully")
    void shouldUpdateCompensationAmountSuccessfully() {
        User admin = defaultAdmin();
        DamageAssessment assessment = underReviewAssessment();
        Compensation compensation = pendingCompensation(assessment);

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(compensationRepository.findById(400L)).thenReturn(Optional.of(compensation));
        when(compensationRepository.save(any(Compensation.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateCompensationRequest request = new UpdateCompensationRequest();
        request.setApprovedAmount(new BigDecimal("30000.00"));
        request.setRemarks("Updated amount");

        CompensationResponse response =
                compensationService.updateCompensation(400L, "admin@example.com", request);

        assertThat(response).isNotNull();
        assertThat(response.getApprovedAmount()).isEqualByComparingTo("30000.00");
        assertThat(response.getRemarks()).isEqualTo("Updated amount");
        verify(compensationRepository).save(any(Compensation.class));
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenAdminNotFoundOnUpdate")
    void shouldThrowResourceNotFoundExceptionWhenAdminNotFoundOnUpdate() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        UpdateCompensationRequest request = new UpdateCompensationRequest();
        request.setApprovedAmount(new BigDecimal("30000.00"));

        assertThatThrownBy(() -> compensationService.updateCompensation(400L, "missing@example.com", request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");
    }

    @Test
    @DisplayName("shouldThrowBadRequestExceptionWhenNonAdminTriesToUpdate")
    void shouldThrowBadRequestExceptionWhenNonAdminTriesToUpdate() {
        User officer = defaultOfficer();
        when(userRepository.findByEmail("officer@example.com")).thenReturn(Optional.of(officer));

        UpdateCompensationRequest request = new UpdateCompensationRequest();
        request.setApprovedAmount(new BigDecimal("30000.00"));

        assertThatThrownBy(() -> compensationService.updateCompensation(400L, "officer@example.com", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only SUPER_ADMIN or DISTRICT_ADMIN");
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenCompensationNotFoundOnUpdate")
    void shouldThrowResourceNotFoundExceptionWhenCompensationNotFoundOnUpdate() {
        User admin = defaultAdmin();
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(compensationRepository.findById(999L)).thenReturn(Optional.empty());

        UpdateCompensationRequest request = new UpdateCompensationRequest();
        request.setApprovedAmount(new BigDecimal("30000.00"));

        assertThatThrownBy(() -> compensationService.updateCompensation(999L, "admin@example.com", request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Compensation");
    }

    @Test
    @DisplayName("shouldThrowBadRequestExceptionWhenUpdatingNonPendingCompensation")
    void shouldThrowBadRequestExceptionWhenUpdatingNonPendingCompensation() {
        User admin = defaultAdmin();
        DamageAssessment assessment = underReviewAssessment();
        Compensation compensation = TestDataFactory.createCompensation(400L, assessment, null,
                CompensationStatus.APPROVED, new BigDecimal("25000.00"));

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(compensationRepository.findById(400L)).thenReturn(Optional.of(compensation));

        UpdateCompensationRequest request = new UpdateCompensationRequest();
        request.setApprovedAmount(new BigDecimal("30000.00"));

        assertThatThrownBy(() -> compensationService.updateCompensation(400L, "admin@example.com", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only PENDING");
    }

    @Test
    @DisplayName("shouldIgnoreNullFieldsOnUpdateCompensation")
    void shouldIgnoreNullFieldsOnUpdateCompensation() {
        User admin = defaultAdmin();
        DamageAssessment assessment = underReviewAssessment();
        Compensation compensation = pendingCompensation(assessment);

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(compensationRepository.findById(400L)).thenReturn(Optional.of(compensation));
        when(compensationRepository.save(any(Compensation.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateCompensationRequest request = new UpdateCompensationRequest();
        request.setRemarks("Only remarks updated");

        CompensationResponse response =
                compensationService.updateCompensation(400L, "admin@example.com", request);

        assertThat(response.getApprovedAmount()).isEqualByComparingTo("25000.00");
        assertThat(response.getRemarks()).isEqualTo("Only remarks updated");
    }

    // ==================== approveCompensation ====================

    @Test
    @DisplayName("shouldApproveCompensationSuccessfully")
    void shouldApproveCompensationSuccessfully() {
        User admin = defaultAdmin();
        DamageAssessment assessment = underReviewAssessment();
        Compensation compensation = pendingCompensation(assessment);

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(compensationRepository.findById(400L)).thenReturn(Optional.of(compensation));
        when(compensationRepository.save(any(Compensation.class))).thenAnswer(inv -> inv.getArgument(0));

        ApproveCompensationRequest request = new ApproveCompensationRequest();
        request.setRemarks("Approved by admin");

        CompensationResponse response =
                compensationService.approveCompensation(400L, "admin@example.com", request);

        assertThat(response).isNotNull();
        assertThat(response.getCompensationStatus()).isEqualTo(CompensationStatus.APPROVED);
        assertThat(response.getApprovedAmount()).isEqualByComparingTo("25000.00");
        assertThat(response.getApprovedByName()).isEqualTo("Test SUPER_ADMIN");
        assertThat(response.getApprovedByEmail()).isEqualTo("admin@example.com");

        verify(statusLogRepository).save(any(CompensationStatusLog.class));
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenAdminNotFoundOnApprove")
    void shouldThrowResourceNotFoundExceptionWhenAdminNotFoundOnApprove() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        ApproveCompensationRequest request = new ApproveCompensationRequest();
        request.setRemarks("Approved");

        assertThatThrownBy(() -> compensationService.approveCompensation(400L, "missing@example.com", request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");
    }

    @Test
    @DisplayName("shouldThrowBadRequestExceptionWhenNonAdminTriesToApprove")
    void shouldThrowBadRequestExceptionWhenNonAdminTriesToApprove() {
        User officer = defaultOfficer();
        when(userRepository.findByEmail("officer@example.com")).thenReturn(Optional.of(officer));

        ApproveCompensationRequest request = new ApproveCompensationRequest();
        request.setRemarks("Approved");

        assertThatThrownBy(() -> compensationService.approveCompensation(400L, "officer@example.com", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only SUPER_ADMIN or DISTRICT_ADMIN");
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenCompensationNotFoundOnApprove")
    void shouldThrowResourceNotFoundExceptionWhenCompensationNotFoundOnApprove() {
        User admin = defaultAdmin();
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(compensationRepository.findById(999L)).thenReturn(Optional.empty());

        ApproveCompensationRequest request = new ApproveCompensationRequest();
        request.setRemarks("Approved");

        assertThatThrownBy(() -> compensationService.approveCompensation(999L, "admin@example.com", request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Compensation");
    }

    @Test
    @DisplayName("shouldThrowBadRequestExceptionWhenApprovingNonPendingCompensation")
    void shouldThrowBadRequestExceptionWhenApprovingNonPendingCompensation() {
        User admin = defaultAdmin();
        DamageAssessment assessment = underReviewAssessment();
        Compensation compensation = TestDataFactory.createCompensation(400L, assessment, null,
                CompensationStatus.REJECTED, new BigDecimal("25000.00"));

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(compensationRepository.findById(400L)).thenReturn(Optional.of(compensation));

        ApproveCompensationRequest request = new ApproveCompensationRequest();
        request.setRemarks("Approved");

        assertThatThrownBy(() -> compensationService.approveCompensation(400L, "admin@example.com", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only PENDING");
    }

    // ==================== rejectCompensation ====================

    @Test
    @DisplayName("shouldRejectCompensationSuccessfullyAndMoveReportToRejected")
    void shouldRejectCompensationSuccessfullyAndMoveReportToRejected() {
        User admin = defaultAdmin();
        DamageAssessment assessment = underReviewAssessment();
        DisasterReport report = assessment.getDisasterReport();
        Compensation compensation = pendingCompensation(assessment);

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(compensationRepository.findById(400L)).thenReturn(Optional.of(compensation));
        when(compensationRepository.save(any(Compensation.class))).thenAnswer(inv -> inv.getArgument(0));

        RejectCompensationRequest request = new RejectCompensationRequest();
        request.setReason("Insufficient evidence");

        CompensationResponse response =
                compensationService.rejectCompensation(400L, "admin@example.com", request);

        assertThat(response).isNotNull();
        assertThat(response.getCompensationStatus()).isEqualTo(CompensationStatus.REJECTED);
        assertThat(response.getRemarks()).isEqualTo("Insufficient evidence");

        assertThat(report.getStatus()).isEqualTo(ReportStatus.REJECTED);
        verify(reportRepository).save(report);
        verify(statusLogRepository).save(any(CompensationStatusLog.class));
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenCompensationNotFoundOnReject")
    void shouldThrowResourceNotFoundExceptionWhenCompensationNotFoundOnReject() {
        User admin = defaultAdmin();
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(compensationRepository.findById(999L)).thenReturn(Optional.empty());

        RejectCompensationRequest request = new RejectCompensationRequest();
        request.setReason("Reason");

        assertThatThrownBy(() -> compensationService.rejectCompensation(999L, "admin@example.com", request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Compensation");
    }

    @Test
    @DisplayName("shouldThrowBadRequestExceptionWhenRejectingNonPendingCompensation")
    void shouldThrowBadRequestExceptionWhenRejectingNonPendingCompensation() {
        User admin = defaultAdmin();
        DamageAssessment assessment = underReviewAssessment();
        Compensation compensation = TestDataFactory.createCompensation(400L, assessment, null,
                CompensationStatus.APPROVED, new BigDecimal("25000.00"));

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(compensationRepository.findById(400L)).thenReturn(Optional.of(compensation));

        RejectCompensationRequest request = new RejectCompensationRequest();
        request.setReason("Reason");

        assertThatThrownBy(() -> compensationService.rejectCompensation(400L, "admin@example.com", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only PENDING");
    }

    // ==================== updatePaymentStatus ====================

    @Test
    @DisplayName("shouldSetPaymentStatusToProcessingForApprovedCompensation")
    void shouldSetPaymentStatusToProcessingForApprovedCompensation() {
        User admin = defaultAdmin();
        DamageAssessment assessment = underReviewAssessment();
        Compensation compensation = TestDataFactory.createCompensation(400L, assessment, admin,
                CompensationStatus.APPROVED, new BigDecimal("25000.00"));

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(compensationRepository.findById(400L)).thenReturn(Optional.of(compensation));
        when(compensationRepository.save(any(Compensation.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdatePaymentStatusRequest request = new UpdatePaymentStatusRequest();
        request.setPaymentStatus(PaymentStatus.PROCESSING);

        CompensationResponse response =
                compensationService.updatePaymentStatus(400L, "admin@example.com", request);

        assertThat(response).isNotNull();
        assertThat(response.getPaymentStatus()).isEqualTo(PaymentStatus.PROCESSING);
        assertThat(response.getCompensationStatus()).isEqualTo(CompensationStatus.APPROVED);
        verify(statusLogRepository, never()).save(any(CompensationStatusLog.class));
    }

    @Test
    @DisplayName("shouldCompletePaymentAndMoveCompensationToPaid")
    void shouldCompletePaymentAndMoveCompensationToPaid() {
        User admin = defaultAdmin();
        DamageAssessment assessment = underReviewAssessment();
        DisasterReport report = assessment.getDisasterReport();
        Compensation compensation = TestDataFactory.createCompensation(400L, assessment, admin,
                CompensationStatus.APPROVED, new BigDecimal("25000.00"));

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(compensationRepository.findById(400L)).thenReturn(Optional.of(compensation));
        when(compensationRepository.save(any(Compensation.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdatePaymentStatusRequest request = new UpdatePaymentStatusRequest();
        request.setPaymentStatus(PaymentStatus.COMPLETED);

        CompensationResponse response =
                compensationService.updatePaymentStatus(400L, "admin@example.com", request);

        assertThat(response).isNotNull();
        assertThat(response.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(response.getCompensationStatus()).isEqualTo(CompensationStatus.PAID);
        assertThat(response.getPaidByName()).isEqualTo("Test SUPER_ADMIN");
        assertThat(response.getPaidByEmail()).isEqualTo("admin@example.com");

        assertThat(report.getStatus()).isEqualTo(ReportStatus.COMPLETED);
        verify(reportRepository).save(report);
        verify(statusLogRepository).save(any(CompensationStatusLog.class));
    }

    @Test
    @DisplayName("shouldHandleFailedPaymentByRevertingToApproved")
    void shouldHandleFailedPaymentByRevertingToApproved() {
        User admin = defaultAdmin();
        DamageAssessment assessment = underReviewAssessment();
        Compensation compensation = TestDataFactory.createCompensation(400L, assessment, admin,
                CompensationStatus.APPROVED, new BigDecimal("25000.00"));

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(compensationRepository.findById(400L)).thenReturn(Optional.of(compensation));
        when(compensationRepository.save(any(Compensation.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdatePaymentStatusRequest request = new UpdatePaymentStatusRequest();
        request.setPaymentStatus(PaymentStatus.FAILED);

        CompensationResponse response =
                compensationService.updatePaymentStatus(400L, "admin@example.com", request);

        assertThat(response).isNotNull();
        assertThat(response.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(response.getCompensationStatus()).isEqualTo(CompensationStatus.APPROVED);
        verify(statusLogRepository).save(any(CompensationStatusLog.class));
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenCompensationNotFoundOnPaymentUpdate")
    void shouldThrowResourceNotFoundExceptionWhenCompensationNotFoundOnPaymentUpdate() {
        User admin = defaultAdmin();
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(compensationRepository.findById(999L)).thenReturn(Optional.empty());

        UpdatePaymentStatusRequest request = new UpdatePaymentStatusRequest();
        request.setPaymentStatus(PaymentStatus.PROCESSING);

        assertThatThrownBy(() -> compensationService.updatePaymentStatus(999L, "admin@example.com", request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Compensation");
    }

    @Test
    @DisplayName("shouldThrowBadRequestExceptionWhenUpdatingPaymentOfNonApprovedCompensation")
    void shouldThrowBadRequestExceptionWhenUpdatingPaymentOfNonApprovedCompensation() {
        User admin = defaultAdmin();
        DamageAssessment assessment = underReviewAssessment();
        Compensation compensation = pendingCompensation(assessment);

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(compensationRepository.findById(400L)).thenReturn(Optional.of(compensation));

        UpdatePaymentStatusRequest request = new UpdatePaymentStatusRequest();
        request.setPaymentStatus(PaymentStatus.PROCESSING);

        assertThatThrownBy(() -> compensationService.updatePaymentStatus(400L, "admin@example.com", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only APPROVED");
    }

    // ==================== searchCompensations ====================

    @Test
    @DisplayName("shouldSearchCompensationsWithPaginationAscending")
    void shouldSearchCompensationsWithPaginationAscending() {
        DamageAssessment assessment = underReviewAssessment();
        Compensation compensation = pendingCompensation(assessment);
        Page<Compensation> page = new PageImpl<>(List.of(compensation),
                PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("approvedAmount").ascending()), 1);

        when(compensationRepository.searchCompensations("flood", CompensationStatus.PENDING, PaymentStatus.NOT_INITIATED,
                PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("approvedAmount").ascending())))
                .thenReturn(page);

        CompensationPageResponse response = compensationService.searchCompensations(
                "flood", CompensationStatus.PENDING, PaymentStatus.NOT_INITIATED, 0, 10, "approvedAmount", "asc");

        assertThat(response).isNotNull();
        assertThat(response.getCompensations()).hasSize(1);
        assertThat(response.getPageNumber()).isEqualTo(0);
        assertThat(response.getPageSize()).isEqualTo(10);
        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getTotalPages()).isEqualTo(1);
        assertThat(response.isLast()).isTrue();

        CompensationResponse first = response.getCompensations().get(0);
        assertThat(first.getApprovedAmount()).isEqualByComparingTo("25000.00");
    }

    @Test
    @DisplayName("shouldSearchCompensationsWithPaginationDescending")
    void shouldSearchCompensationsWithPaginationDescending() {
        DamageAssessment assessment = underReviewAssessment();
        Compensation compensation = pendingCompensation(assessment);
        Page<Compensation> page = new PageImpl<>(List.of(compensation),
                PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("compensationStatus").descending()), 1);

        when(compensationRepository.searchCompensations(null, null, null,
                PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("compensationStatus").descending())))
                .thenReturn(page);

        CompensationPageResponse response = compensationService.searchCompensations(
                null, null, null, 0, 10, "compensationStatus", "desc");

        assertThat(response).isNotNull();
        assertThat(response.getCompensations()).hasSize(1);
    }

    @Test
    @DisplayName("shouldThrowBadRequestExceptionForInvalidSortFieldOnSearch")
    void shouldThrowBadRequestExceptionForInvalidSortFieldOnSearch() {
        assertThatThrownBy(() -> compensationService.searchCompensations(
                null, null, null, 0, 10, "invalidField", "asc"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid sort field");
    }

    // ==================== getMyCompensations ====================

    @Test
    @DisplayName("shouldGetMyCompensationsForCitizen")
    void shouldGetMyCompensationsForCitizen() {
        User citizen = defaultCitizen();
        DamageAssessment assessment = underReviewAssessment();
        Compensation compensation = pendingCompensation(assessment);
        Page<Compensation> page = new PageImpl<>(List.of(compensation),
                PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("createdAt").ascending()), 1);

        when(userRepository.findByEmail("citizen@example.com")).thenReturn(Optional.of(citizen));
        when(compensationRepository.findByCitizenId(10L,
                PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("createdAt").ascending())))
                .thenReturn(page);

        CompensationPageResponse response = compensationService.getMyCompensations(
                "citizen@example.com", 0, 10, "createdAt", "asc");

        assertThat(response).isNotNull();
        assertThat(response.getCompensations()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenCitizenNotFoundOnGetMyCompensations")
    void shouldThrowResourceNotFoundExceptionWhenCitizenNotFoundOnGetMyCompensations() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> compensationService.getMyCompensations(
                "missing@example.com", 0, 10, "createdAt", "asc"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");
    }

    @Test
    @DisplayName("shouldThrowBadRequestExceptionForInvalidSortFieldOnGetMyCompensations")
    void shouldThrowBadRequestExceptionForInvalidSortFieldOnGetMyCompensations() {
        User citizen = defaultCitizen();
        when(userRepository.findByEmail("citizen@example.com")).thenReturn(Optional.of(citizen));

        assertThatThrownBy(() -> compensationService.getMyCompensations(
                "citizen@example.com", 0, 10, "invalidField", "asc"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid sort field");
    }

    // ==================== getCompensationHistory ====================

    @Test
    @DisplayName("shouldReturnCompensationHistory")
    void shouldReturnCompensationHistory() {
        User admin = defaultAdmin();
        DamageAssessment assessment = underReviewAssessment();
        Compensation compensation = pendingCompensation(assessment);
        CompensationStatusLog log = TestDataFactory.createStatusLog(
                500L, compensation, admin, CompensationStatus.PENDING, CompensationStatus.APPROVED);

        when(compensationRepository.findById(400L)).thenReturn(Optional.of(compensation));
        when(statusLogRepository.findByCompensationIdOrderByCreatedAtAsc(400L)).thenReturn(List.of(log));

        List<CompensationHistoryResponse> history = compensationService.getCompensationHistory(400L);

        assertThat(history).hasSize(1);
        CompensationHistoryResponse entry = history.get(0);
        assertThat(entry.getId()).isEqualTo(500L);
        assertThat(entry.getPreviousStatus()).isEqualTo(CompensationStatus.PENDING);
        assertThat(entry.getNewStatus()).isEqualTo(CompensationStatus.APPROVED);
        assertThat(entry.getChangedByName()).isEqualTo("Test SUPER_ADMIN");
        assertThat(entry.getChangedByEmail()).isEqualTo("admin@example.com");
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenCompensationNotFoundOnHistory")
    void shouldThrowResourceNotFoundExceptionWhenCompensationNotFoundOnHistory() {
        when(compensationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> compensationService.getCompensationHistory(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Compensation");
    }

    // ==================== deleteCompensation ====================

    @Test
    @DisplayName("shouldDeletePendingCompensationAndRevertReportToUnderReview")
    void shouldDeletePendingCompensationAndRevertReportToUnderReview() {
        User admin = defaultAdmin();
        DamageAssessment assessment = underReviewAssessment();
        DisasterReport report = assessment.getDisasterReport();
        Compensation compensation = pendingCompensation(assessment);

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(compensationRepository.findById(400L)).thenReturn(Optional.of(compensation));

        compensationService.deleteCompensation(400L, "admin@example.com");

        assertThat(report.getStatus()).isEqualTo(ReportStatus.UNDER_REVIEW);
        verify(reportRepository).save(report);
        verify(compensationRepository).delete(compensation);
    }

    @Test
    @DisplayName("shouldThrowBadRequestExceptionWhenNonSuperAdminTriesToDelete")
    void shouldThrowBadRequestExceptionWhenNonSuperAdminTriesToDelete() {
        User districtAdmin = TestDataFactory.createDistrictAdmin(3L, "district@example.com");
        when(userRepository.findByEmail("district@example.com")).thenReturn(Optional.of(districtAdmin));

        assertThatThrownBy(() -> compensationService.deleteCompensation(400L, "district@example.com"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only SUPER_ADMIN");

        verify(compensationRepository, never()).delete(any());
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenCompensationNotFoundOnDelete")
    void shouldThrowResourceNotFoundExceptionWhenCompensationNotFoundOnDelete() {
        User admin = defaultAdmin();
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(compensationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> compensationService.deleteCompensation(999L, "admin@example.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Compensation");

        verify(compensationRepository, never()).delete(any());
    }

    @Test
    @DisplayName("shouldThrowBadRequestExceptionWhenDeletingNonPendingCompensation")
    void shouldThrowBadRequestExceptionWhenDeletingNonPendingCompensation() {
        User admin = defaultAdmin();
        DamageAssessment assessment = underReviewAssessment();
        Compensation compensation = TestDataFactory.createCompensation(400L, assessment, admin,
                CompensationStatus.APPROVED, new BigDecimal("25000.00"));

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(compensationRepository.findById(400L)).thenReturn(Optional.of(compensation));

        assertThatThrownBy(() -> compensationService.deleteCompensation(400L, "admin@example.com"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only PENDING");

        verify(compensationRepository, never()).delete(any());
    }
}
