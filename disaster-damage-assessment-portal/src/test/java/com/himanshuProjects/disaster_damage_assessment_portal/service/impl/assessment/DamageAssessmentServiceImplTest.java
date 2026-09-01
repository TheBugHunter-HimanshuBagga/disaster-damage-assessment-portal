package com.himanshuProjects.disaster_damage_assessment_portal.service.impl.assessment;

import com.himanshuProjects.disaster_damage_assessment_portal.dto.assessment.AddInspectionImagesRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.assessment.CreateDamageAssessmentRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.assessment.DamageAssessmentPageResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.assessment.DamageAssessmentResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.assessment.UpdateDamageAssessmentRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.disaster.DamageAssessment;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.disaster.DisasterReport;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.disaster.InspectionImage;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.disaster.OfficerAssignment;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.User;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.AssignmentStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.DamageLevel;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.ReportStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.RoleType;
import com.himanshuProjects.disaster_damage_assessment_portal.exception.BadRequestException;
import com.himanshuProjects.disaster_damage_assessment_portal.exception.ConflictException;
import com.himanshuProjects.disaster_damage_assessment_portal.exception.ResourceNotFoundException;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.disaster.DamageAssessmentRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.disaster.DisasterReportRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.disaster.InspectionImageRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.disaster.OfficerAssignmentRepository;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DamageAssessmentServiceImplTest {

    @Mock
    private DamageAssessmentRepository assessmentRepository;
    @Mock
    private InspectionImageRepository inspectionImageRepository;
    @Mock
    private DisasterReportRepository reportRepository;
    @Mock
    private OfficerAssignmentRepository assignmentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private DamageAssessmentServiceImpl damageAssessmentService;

    @BeforeEach
    void setUp() {
        damageAssessmentService = new DamageAssessmentServiceImpl(
                assessmentRepository, inspectionImageRepository, reportRepository,
                assignmentRepository, userRepository, eventPublisher);
    }

    private User defaultOfficer() {
        return TestDataFactory.createOfficer(1L, "officer@example.com");
    }

    private User defaultCitizen() {
        return TestDataFactory.createCitizen(10L, "citizen@example.com");
    }

    private DisasterReport underInspectionReport() {
        return TestDataFactory.createReport(100L, defaultCitizen(), ReportStatus.UNDER_INSPECTION);
    }

    private OfficerAssignment activeAssignment(User officer, DisasterReport report) {
        return TestDataFactory.createAssignment(200L, report, officer, AssignmentStatus.ACCEPTED);
    }

    private DamageAssessment savedAssessment(User officer, DisasterReport report) {
        return TestDataFactory.createAssessment(300L, report, officer,
                DamageLevel.HIGH, new BigDecimal("50000.00"));
    }

    private CreateDamageAssessmentRequest createRequest() {
        CreateDamageAssessmentRequest request = new CreateDamageAssessmentRequest();
        request.setDamageLevel(DamageLevel.HIGH);
        request.setEstimatedLoss(new BigDecimal("50000.00"));
        request.setAssessmentNotes("Significant structural damage observed.");
        request.setRecommendation("Recommended for compensation.");
        request.setImageUrls(List.of("http://example.com/img1.jpg", "http://example.com/img2.jpg"));
        return request;
    }

    private void stubDefaultImages() {
        when(inspectionImageRepository.findByDamageAssessmentIdOrderByUploadedAtAsc(anyLong()))
                .thenReturn(Collections.emptyList());
    }

    // ==================== submitAssessment ====================

    @Test
    @DisplayName("shouldSubmitAssessmentSuccessfullyAndMoveReportToUnderReview")
    void shouldSubmitAssessmentSuccessfullyAndMoveReportToUnderReview() {
        User officer = defaultOfficer();
        DisasterReport report = underInspectionReport();
        OfficerAssignment assignment = activeAssignment(officer, report);
        DamageAssessment assessment = savedAssessment(officer, report);

        when(userRepository.findByEmail("officer@example.com")).thenReturn(Optional.of(officer));
        when(reportRepository.findById(100L)).thenReturn(Optional.of(report));
        when(assignmentRepository.existsByDisasterReportIdAndAssignmentStatusIn(
                100L, List.of(AssignmentStatus.ACCEPTED, AssignmentStatus.IN_PROGRESS))).thenReturn(true);
        when(assignmentRepository.findByDisasterReportIdAndAssignmentStatusIn(
                100L, List.of(AssignmentStatus.ACCEPTED, AssignmentStatus.IN_PROGRESS, AssignmentStatus.COMPLETED)))
                .thenReturn(Optional.of(assignment));
        when(assessmentRepository.existsByDisasterReportId(100L)).thenReturn(false);
        when(assessmentRepository.save(any(DamageAssessment.class))).thenAnswer(inv -> {
            DamageAssessment a = inv.getArgument(0);
            a.setId(300L);
            return a;
        });
        stubDefaultImages();

        DamageAssessmentResponse response =
                damageAssessmentService.submitAssessment(100L, "officer@example.com", createRequest());

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(300L);
        assertThat(response.getDamageLevel()).isEqualTo(DamageLevel.HIGH);
        assertThat(response.getEstimatedLoss()).isEqualByComparingTo("50000.00");
        assertThat(response.getReportId()).isEqualTo(100L);
        assertThat(response.getReportStatus()).isEqualTo(ReportStatus.UNDER_REVIEW);
        assertThat(response.getOfficerId()).isEqualTo(1L);

        assertThat(report.getStatus()).isEqualTo(ReportStatus.UNDER_REVIEW);
        verify(reportRepository).save(report);
        verify(inspectionImageRepository, org.mockito.Mockito.times(2)).save(any(InspectionImage.class));
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenOfficerNotFoundOnSubmit")
    void shouldThrowResourceNotFoundExceptionWhenOfficerNotFoundOnSubmit() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> damageAssessmentService.submitAssessment(
                100L, "missing@example.com", createRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");
    }

    @Test
    @DisplayName("shouldThrowBadRequestExceptionWhenNotFieldOfficerOnSubmit")
    void shouldThrowBadRequestExceptionWhenNotFieldOfficerOnSubmit() {
        User citizen = defaultCitizen();
        when(userRepository.findByEmail("citizen@example.com")).thenReturn(Optional.of(citizen));

        assertThatThrownBy(() -> damageAssessmentService.submitAssessment(
                100L, "citizen@example.com", createRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only FIELD_OFFICER");
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenReportNotFoundOnSubmit")
    void shouldThrowResourceNotFoundExceptionWhenReportNotFoundOnSubmit() {
        User officer = defaultOfficer();
        when(userRepository.findByEmail("officer@example.com")).thenReturn(Optional.of(officer));
        when(reportRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> damageAssessmentService.submitAssessment(
                999L, "officer@example.com", createRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("DisasterReport");
    }

    @Test
    @DisplayName("shouldThrowBadRequestExceptionWhenReportNotInEligibleStatus")
    void shouldThrowBadRequestExceptionWhenReportNotInEligibleStatus() {
        User officer = defaultOfficer();
        DisasterReport report = TestDataFactory.createReport(100L, defaultCitizen(), ReportStatus.SUBMITTED);
        when(userRepository.findByEmail("officer@example.com")).thenReturn(Optional.of(officer));
        when(reportRepository.findById(100L)).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> damageAssessmentService.submitAssessment(
                100L, "officer@example.com", createRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot assess report");
    }

    @Test
    @DisplayName("shouldThrowBadRequestExceptionWhenNoActiveAssignmentOnSubmit")
    void shouldThrowBadRequestExceptionWhenNoActiveAssignmentOnSubmit() {
        User officer = defaultOfficer();
        DisasterReport report = underInspectionReport();
        when(userRepository.findByEmail("officer@example.com")).thenReturn(Optional.of(officer));
        when(reportRepository.findById(100L)).thenReturn(Optional.of(report));
        when(assignmentRepository.existsByDisasterReportIdAndAssignmentStatusIn(
                100L, List.of(AssignmentStatus.ACCEPTED, AssignmentStatus.IN_PROGRESS))).thenReturn(false);

        assertThatThrownBy(() -> damageAssessmentService.submitAssessment(
                100L, "officer@example.com", createRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("No active assignment");
    }

    @Test
    @DisplayName("shouldThrowBadRequestExceptionWhenOfficerNotAssignedToReport")
    void shouldThrowBadRequestExceptionWhenOfficerNotAssignedToReport() {
        User officer = defaultOfficer();
        User otherOfficer = TestDataFactory.createOfficer(2L, "other@example.com");
        DisasterReport report = underInspectionReport();
        OfficerAssignment assignment = activeAssignment(otherOfficer, report);

        when(userRepository.findByEmail("officer@example.com")).thenReturn(Optional.of(officer));
        when(reportRepository.findById(100L)).thenReturn(Optional.of(report));
        when(assignmentRepository.existsByDisasterReportIdAndAssignmentStatusIn(
                100L, List.of(AssignmentStatus.ACCEPTED, AssignmentStatus.IN_PROGRESS))).thenReturn(true);
        when(assignmentRepository.findByDisasterReportIdAndAssignmentStatusIn(
                100L, List.of(AssignmentStatus.ACCEPTED, AssignmentStatus.IN_PROGRESS, AssignmentStatus.COMPLETED)))
                .thenReturn(Optional.of(assignment));

        assertThatThrownBy(() -> damageAssessmentService.submitAssessment(
                100L, "officer@example.com", createRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("You are not assigned");
    }

    @Test
    @DisplayName("shouldThrowConflictExceptionWhenAssessmentAlreadyExists")
    void shouldThrowConflictExceptionWhenAssessmentAlreadyExists() {
        User officer = defaultOfficer();
        DisasterReport report = underInspectionReport();
        OfficerAssignment assignment = activeAssignment(officer, report);

        when(userRepository.findByEmail("officer@example.com")).thenReturn(Optional.of(officer));
        when(reportRepository.findById(100L)).thenReturn(Optional.of(report));
        when(assignmentRepository.existsByDisasterReportIdAndAssignmentStatusIn(
                100L, List.of(AssignmentStatus.ACCEPTED, AssignmentStatus.IN_PROGRESS))).thenReturn(true);
        when(assignmentRepository.findByDisasterReportIdAndAssignmentStatusIn(
                100L, List.of(AssignmentStatus.ACCEPTED, AssignmentStatus.IN_PROGRESS, AssignmentStatus.COMPLETED)))
                .thenReturn(Optional.of(assignment));
        when(assessmentRepository.existsByDisasterReportId(100L)).thenReturn(true);

        assertThatThrownBy(() -> damageAssessmentService.submitAssessment(
                100L, "officer@example.com", createRequest()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("shouldThrowBadRequestExceptionWhenImageCountExceedsLimit")
    void shouldThrowBadRequestExceptionWhenImageCountExceedsLimit() {
        User officer = defaultOfficer();
        DisasterReport report = underInspectionReport();
        OfficerAssignment assignment = activeAssignment(officer, report);
        DamageAssessment assessment = savedAssessment(officer, report);

        when(userRepository.findByEmail("officer@example.com")).thenReturn(Optional.of(officer));
        when(reportRepository.findById(100L)).thenReturn(Optional.of(report));
        when(assignmentRepository.existsByDisasterReportIdAndAssignmentStatusIn(
                100L, List.of(AssignmentStatus.ACCEPTED, AssignmentStatus.IN_PROGRESS))).thenReturn(true);
        when(assignmentRepository.findByDisasterReportIdAndAssignmentStatusIn(
                100L, List.of(AssignmentStatus.ACCEPTED, AssignmentStatus.IN_PROGRESS, AssignmentStatus.COMPLETED)))
                .thenReturn(Optional.of(assignment));
        when(assessmentRepository.existsByDisasterReportId(100L)).thenReturn(false);
        when(assessmentRepository.save(any(DamageAssessment.class))).thenAnswer(inv -> {
            DamageAssessment a = inv.getArgument(0);
            a.setId(300L);
            return a;
        });

        CreateDamageAssessmentRequest request = createRequest();
        request.setImageUrls(Collections.nCopies(11, "http://example.com/img.jpg"));

        assertThatThrownBy(() -> damageAssessmentService.submitAssessment(
                100L, "officer@example.com", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot upload more than");
    }

    // ==================== getAssessmentById ====================

    @Test
    @DisplayName("shouldReturnAssessmentById")
    void shouldReturnAssessmentById() {
        User officer = defaultOfficer();
        DisasterReport report = underInspectionReport();
        DamageAssessment assessment = savedAssessment(officer, report);
        when(assessmentRepository.findById(300L)).thenReturn(Optional.of(assessment));
        stubDefaultImages();

        DamageAssessmentResponse response = damageAssessmentService.getAssessmentById(300L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(300L);
        assertThat(response.getEstimatedLoss()).isEqualByComparingTo("50000.00");
        verify(assessmentRepository).findById(300L);
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenAssessmentNotFoundById")
    void shouldThrowResourceNotFoundExceptionWhenAssessmentNotFoundById() {
        when(assessmentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> damageAssessmentService.getAssessmentById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("DamageAssessment");
    }

    // ==================== getAssessmentByReportId ====================

    @Test
    @DisplayName("shouldReturnAssessmentByReportId")
    void shouldReturnAssessmentByReportId() {
        User officer = defaultOfficer();
        DisasterReport report = underInspectionReport();
        DamageAssessment assessment = savedAssessment(officer, report);
        when(assessmentRepository.findByDisasterReportId(100L)).thenReturn(Optional.of(assessment));
        stubDefaultImages();

        DamageAssessmentResponse response = damageAssessmentService.getAssessmentByReportId(100L);

        assertThat(response).isNotNull();
        assertThat(response.getReportId()).isEqualTo(100L);
        verify(assessmentRepository).findByDisasterReportId(100L);
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenAssessmentNotFoundByReportId")
    void shouldThrowResourceNotFoundExceptionWhenAssessmentNotFoundByReportId() {
        when(assessmentRepository.findByDisasterReportId(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> damageAssessmentService.getAssessmentByReportId(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("DamageAssessment");
    }

    // ==================== updateAssessment ====================

    @Test
    @DisplayName("shouldUpdateAssessmentSuccessfully")
    void shouldUpdateAssessmentSuccessfully() {
        User officer = defaultOfficer();
        DisasterReport report = TestDataFactory.createReport(100L, defaultCitizen(), ReportStatus.UNDER_REVIEW);
        DamageAssessment assessment = savedAssessment(officer, report);

        when(assessmentRepository.findById(300L)).thenReturn(Optional.of(assessment));
        when(userRepository.findByEmail("officer@example.com")).thenReturn(Optional.of(officer));
        when(assessmentRepository.save(any(DamageAssessment.class))).thenAnswer(inv -> inv.getArgument(0));
        stubDefaultImages();

        UpdateDamageAssessmentRequest request = new UpdateDamageAssessmentRequest();
        request.setDamageLevel(DamageLevel.SEVERE);
        request.setEstimatedLoss(new BigDecimal("75000.00"));
        request.setAssessmentNotes("Updated notes");
        request.setRecommendation("Updated recommendation");

        DamageAssessmentResponse response =
                damageAssessmentService.updateAssessment(300L, "officer@example.com", request);

        assertThat(response).isNotNull();
        assertThat(response.getDamageLevel()).isEqualTo(DamageLevel.SEVERE);
        assertThat(response.getEstimatedLoss()).isEqualByComparingTo("75000.00");
        assertThat(response.getAssessmentNotes()).isEqualTo("Updated notes");
        verify(assessmentRepository).save(assessment);
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenAssessmentNotFoundOnUpdate")
    void shouldThrowResourceNotFoundExceptionWhenAssessmentNotFoundOnUpdate() {
        when(assessmentRepository.findById(999L)).thenReturn(Optional.empty());

        UpdateDamageAssessmentRequest request = new UpdateDamageAssessmentRequest();
        request.setDamageLevel(DamageLevel.LOW);
        request.setEstimatedLoss(new BigDecimal("100.00"));
        request.setAssessmentNotes("notes");
        request.setRecommendation("rec");

        assertThatThrownBy(() -> damageAssessmentService.updateAssessment(999L, "officer@example.com", request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("DamageAssessment");
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenOfficerNotFoundOnUpdate")
    void shouldThrowResourceNotFoundExceptionWhenOfficerNotFoundOnUpdate() {
        User officer = defaultOfficer();
        DisasterReport report = TestDataFactory.createReport(100L, defaultCitizen(), ReportStatus.UNDER_REVIEW);
        DamageAssessment assessment = savedAssessment(officer, report);
        when(assessmentRepository.findById(300L)).thenReturn(Optional.of(assessment));
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        UpdateDamageAssessmentRequest request = new UpdateDamageAssessmentRequest();
        request.setDamageLevel(DamageLevel.LOW);
        request.setEstimatedLoss(new BigDecimal("100.00"));
        request.setAssessmentNotes("notes");
        request.setRecommendation("rec");

        assertThatThrownBy(() -> damageAssessmentService.updateAssessment(300L, "missing@example.com", request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");
    }

    @Test
    @DisplayName("shouldThrowBadRequestExceptionWhenOfficerTriesToUpdateOthersAssessment")
    void shouldThrowBadRequestExceptionWhenOfficerTriesToUpdateOthersAssessment() {
        User officer = defaultOfficer();
        User otherOfficer = TestDataFactory.createOfficer(2L, "other@example.com");
        DisasterReport report = TestDataFactory.createReport(100L, defaultCitizen(), ReportStatus.UNDER_REVIEW);
        DamageAssessment assessment = savedAssessment(officer, report);

        when(assessmentRepository.findById(300L)).thenReturn(Optional.of(assessment));
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(otherOfficer));

        UpdateDamageAssessmentRequest request = new UpdateDamageAssessmentRequest();
        request.setDamageLevel(DamageLevel.LOW);
        request.setEstimatedLoss(new BigDecimal("100.00"));
        request.setAssessmentNotes("notes");
        request.setRecommendation("rec");

        assertThatThrownBy(() -> damageAssessmentService.updateAssessment(300L, "other@example.com", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("only update your own");
    }

    @Test
    @DisplayName("shouldThrowBadRequestExceptionWhenReportNotUnderReviewOnUpdate")
    void shouldThrowBadRequestExceptionWhenReportNotUnderReviewOnUpdate() {
        User officer = defaultOfficer();
        DisasterReport report = underInspectionReport();
        DamageAssessment assessment = savedAssessment(officer, report);

        when(assessmentRepository.findById(300L)).thenReturn(Optional.of(assessment));
        when(userRepository.findByEmail("officer@example.com")).thenReturn(Optional.of(officer));

        UpdateDamageAssessmentRequest request = new UpdateDamageAssessmentRequest();
        request.setDamageLevel(DamageLevel.LOW);
        request.setEstimatedLoss(new BigDecimal("100.00"));
        request.setAssessmentNotes("notes");
        request.setRecommendation("rec");

        assertThatThrownBy(() -> damageAssessmentService.updateAssessment(300L, "officer@example.com", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot update assessment");
    }

    // ==================== searchAssessments ====================

    @Test
    @DisplayName("shouldSearchAssessmentsWithPaginationAscending")
    void shouldSearchAssessmentsWithPaginationAscending() {
        User officer = defaultOfficer();
        DisasterReport report = underInspectionReport();
        DamageAssessment assessment = savedAssessment(officer, report);
        Page<DamageAssessment> page = new PageImpl<>(List.of(assessment),
                PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("damageLevel").ascending()), 1);

        when(assessmentRepository.searchAssessments("flood", DamageLevel.HIGH, 1L,
                PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("damageLevel").ascending())))
                .thenReturn(page);
        stubDefaultImages();

        DamageAssessmentPageResponse response = damageAssessmentService.searchAssessments(
                "flood", DamageLevel.HIGH, 1L, 0, 10, "damageLevel", "asc");

        assertThat(response).isNotNull();
        assertThat(response.getAssessments()).hasSize(1);
        assertThat(response.getPageNumber()).isEqualTo(0);
        assertThat(response.getPageSize()).isEqualTo(10);
        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getTotalPages()).isEqualTo(1);
        assertThat(response.isLast()).isTrue();
    }

    @Test
    @DisplayName("shouldSearchAssessmentsWithPaginationDescending")
    void shouldSearchAssessmentsWithPaginationDescending() {
        User officer = defaultOfficer();
        DisasterReport report = underInspectionReport();
        DamageAssessment assessment = savedAssessment(officer, report);
        Page<DamageAssessment> page = new PageImpl<>(List.of(assessment),
                PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("estimatedLoss").descending()), 1);

        when(assessmentRepository.searchAssessments(null, null, null,
                PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("estimatedLoss").descending())))
                .thenReturn(page);
        stubDefaultImages();

        DamageAssessmentPageResponse response = damageAssessmentService.searchAssessments(
                null, null, null, 0, 10, "estimatedLoss", "desc");

        assertThat(response).isNotNull();
        assertThat(response.getAssessments()).hasSize(1);
    }

    @Test
    @DisplayName("shouldThrowBadRequestExceptionForInvalidSortFieldOnSearch")
    void shouldThrowBadRequestExceptionForInvalidSortFieldOnSearch() {
        assertThatThrownBy(() -> damageAssessmentService.searchAssessments(
                null, null, null, 0, 10, "invalidField", "asc"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid sort field");
    }

    // ==================== getMyAssessments ====================

    @Test
    @DisplayName("shouldGetMyAssessmentsForOfficer")
    void shouldGetMyAssessmentsForOfficer() {
        User officer = defaultOfficer();
        DisasterReport report = underInspectionReport();
        DamageAssessment assessment = savedAssessment(officer, report);
        Page<DamageAssessment> page = new PageImpl<>(List.of(assessment),
                PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("createdAt").ascending()), 1);

        when(userRepository.findByEmail("officer@example.com")).thenReturn(Optional.of(officer));
        when(assessmentRepository.searchAssessments(null, null, 1L,
                PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("createdAt").ascending())))
                .thenReturn(page);
        stubDefaultImages();

        DamageAssessmentPageResponse response = damageAssessmentService.getMyAssessments(
                "officer@example.com", 0, 10, "createdAt", "asc");

        assertThat(response).isNotNull();
        assertThat(response.getAssessments()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenOfficerNotFoundOnGetMyAssessments")
    void shouldThrowResourceNotFoundExceptionWhenOfficerNotFoundOnGetMyAssessments() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> damageAssessmentService.getMyAssessments(
                "missing@example.com", 0, 10, "createdAt", "asc"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");
    }

    @Test
    @DisplayName("shouldThrowBadRequestExceptionForInvalidSortFieldOnGetMyAssessments")
    void shouldThrowBadRequestExceptionForInvalidSortFieldOnGetMyAssessments() {
        User officer = defaultOfficer();
        when(userRepository.findByEmail("officer@example.com")).thenReturn(Optional.of(officer));

        assertThatThrownBy(() -> damageAssessmentService.getMyAssessments(
                "officer@example.com", 0, 10, "invalidField", "asc"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid sort field");
    }

    // ==================== addImages ====================

    @Test
    @DisplayName("shouldAddImagesToAssessmentSuccessfully")
    void shouldAddImagesToAssessmentSuccessfully() {
        User officer = defaultOfficer();
        DisasterReport report = underInspectionReport();
        DamageAssessment assessment = savedAssessment(officer, report);

        when(assessmentRepository.findById(300L)).thenReturn(Optional.of(assessment));
        when(userRepository.findByEmail("officer@example.com")).thenReturn(Optional.of(officer));
        when(inspectionImageRepository.countByDamageAssessmentId(300L)).thenReturn(2L);
        stubDefaultImages();

        AddInspectionImagesRequest request = new AddInspectionImagesRequest();
        request.setImageUrls(List.of("http://example.com/new1.jpg", "http://example.com/new2.jpg"));

        DamageAssessmentResponse response =
                damageAssessmentService.addImages(300L, "officer@example.com", request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(300L);
        verify(inspectionImageRepository, org.mockito.Mockito.times(2)).save(any(InspectionImage.class));
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenAssessmentNotFoundOnAddImages")
    void shouldThrowResourceNotFoundExceptionWhenAssessmentNotFoundOnAddImages() {
        when(assessmentRepository.findById(999L)).thenReturn(Optional.empty());

        AddInspectionImagesRequest request = new AddInspectionImagesRequest();
        request.setImageUrls(List.of("http://example.com/img.jpg"));

        assertThatThrownBy(() -> damageAssessmentService.addImages(999L, "officer@example.com", request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("DamageAssessment");
    }

    @Test
    @DisplayName("shouldThrowBadRequestExceptionWhenOfficerTriesToAddImagesToOthersAssessment")
    void shouldThrowBadRequestExceptionWhenOfficerTriesToAddImagesToOthersAssessment() {
        User officer = defaultOfficer();
        User otherOfficer = TestDataFactory.createOfficer(2L, "other@example.com");
        DisasterReport report = underInspectionReport();
        DamageAssessment assessment = savedAssessment(officer, report);

        when(assessmentRepository.findById(300L)).thenReturn(Optional.of(assessment));
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(otherOfficer));

        AddInspectionImagesRequest request = new AddInspectionImagesRequest();
        request.setImageUrls(List.of("http://example.com/img.jpg"));

        assertThatThrownBy(() -> damageAssessmentService.addImages(300L, "other@example.com", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("only modify your own");
    }

    @Test
    @DisplayName("shouldThrowBadRequestExceptionWhenImageLimitExceededOnAddImages")
    void shouldThrowBadRequestExceptionWhenImageLimitExceededOnAddImages() {
        User officer = defaultOfficer();
        DisasterReport report = underInspectionReport();
        DamageAssessment assessment = savedAssessment(officer, report);

        when(assessmentRepository.findById(300L)).thenReturn(Optional.of(assessment));
        when(userRepository.findByEmail("officer@example.com")).thenReturn(Optional.of(officer));
        when(inspectionImageRepository.countByDamageAssessmentId(300L)).thenReturn(9L);

        AddInspectionImagesRequest request = new AddInspectionImagesRequest();
        request.setImageUrls(List.of("http://example.com/a.jpg", "http://example.com/b.jpg"));

        assertThatThrownBy(() -> damageAssessmentService.addImages(300L, "officer@example.com", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Maximum allowed");
    }

    // ==================== removeImage ====================

    @Test
    @DisplayName("shouldRemoveImageSuccessfully")
    void shouldRemoveImageSuccessfully() {
        User officer = defaultOfficer();
        DisasterReport report = underInspectionReport();
        DamageAssessment assessment = savedAssessment(officer, report);
        InspectionImage image = new InspectionImage();
        image.setId(500L);
        image.setImageUrl("http://example.com/img.jpg");
        image.setDamageAssessment(assessment);

        when(assessmentRepository.findById(300L)).thenReturn(Optional.of(assessment));
        when(userRepository.findByEmail("officer@example.com")).thenReturn(Optional.of(officer));
        when(inspectionImageRepository.findById(500L)).thenReturn(Optional.of(image));

        damageAssessmentService.removeImage(300L, 500L, "officer@example.com");

        verify(inspectionImageRepository).delete(image);
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenImageNotFoundOnRemove")
    void shouldThrowResourceNotFoundExceptionWhenImageNotFoundOnRemove() {
        User officer = defaultOfficer();
        DisasterReport report = underInspectionReport();
        DamageAssessment assessment = savedAssessment(officer, report);

        when(assessmentRepository.findById(300L)).thenReturn(Optional.of(assessment));
        when(userRepository.findByEmail("officer@example.com")).thenReturn(Optional.of(officer));
        when(inspectionImageRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> damageAssessmentService.removeImage(300L, 999L, "officer@example.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("InspectionImage");
    }

    @Test
    @DisplayName("shouldThrowBadRequestExceptionWhenImageDoesNotBelongToAssessment")
    void shouldThrowBadRequestExceptionWhenImageDoesNotBelongToAssessment() {
        User officer = defaultOfficer();
        DisasterReport report = underInspectionReport();
        DamageAssessment assessment = savedAssessment(officer, report);
        DamageAssessment otherAssessment = TestDataFactory.createAssessment(400L, report, officer,
                DamageLevel.LOW, new BigDecimal("100.00"));
        InspectionImage image = new InspectionImage();
        image.setId(500L);
        image.setImageUrl("http://example.com/img.jpg");
        image.setDamageAssessment(otherAssessment);

        when(assessmentRepository.findById(300L)).thenReturn(Optional.of(assessment));
        when(userRepository.findByEmail("officer@example.com")).thenReturn(Optional.of(officer));
        when(inspectionImageRepository.findById(500L)).thenReturn(Optional.of(image));

        assertThatThrownBy(() -> damageAssessmentService.removeImage(300L, 500L, "officer@example.com"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not belong");
    }

    // ==================== deleteAssessment ====================

    @Test
    @DisplayName("shouldDeleteAssessmentSuccessfullyAndRevertReportToUnderInspection")
    void shouldDeleteAssessmentSuccessfullyAndRevertReportToUnderInspection() {
        User officer = defaultOfficer();
        DisasterReport report = TestDataFactory.createReport(100L, defaultCitizen(), ReportStatus.UNDER_REVIEW);
        DamageAssessment assessment = savedAssessment(officer, report);

        when(assessmentRepository.findById(300L)).thenReturn(Optional.of(assessment));
        when(userRepository.findByEmail("officer@example.com")).thenReturn(Optional.of(officer));

        damageAssessmentService.deleteAssessment(300L, "officer@example.com");

        verify(inspectionImageRepository).deleteByDamageAssessmentId(300L);
        verify(assessmentRepository).delete(assessment);
        assertThat(report.getStatus()).isEqualTo(ReportStatus.UNDER_INSPECTION);
        verify(reportRepository).save(report);
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenAssessmentNotFoundOnDelete")
    void shouldThrowResourceNotFoundExceptionWhenAssessmentNotFoundOnDelete() {
        when(assessmentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> damageAssessmentService.deleteAssessment(999L, "officer@example.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("DamageAssessment");

        verify(assessmentRepository, never()).delete(any());
    }

    @Test
    @DisplayName("shouldThrowBadRequestExceptionWhenOfficerTriesToDeleteOthersAssessment")
    void shouldThrowBadRequestExceptionWhenOfficerTriesToDeleteOthersAssessment() {
        User officer = defaultOfficer();
        User otherOfficer = TestDataFactory.createOfficer(2L, "other@example.com");
        DisasterReport report = TestDataFactory.createReport(100L, defaultCitizen(), ReportStatus.UNDER_REVIEW);
        DamageAssessment assessment = savedAssessment(officer, report);

        when(assessmentRepository.findById(300L)).thenReturn(Optional.of(assessment));
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(otherOfficer));

        assertThatThrownBy(() -> damageAssessmentService.deleteAssessment(300L, "other@example.com"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("only delete your own");

        verify(assessmentRepository, never()).delete(any());
    }

    @Test
    @DisplayName("shouldThrowBadRequestExceptionWhenReportNotUnderReviewOnDelete")
    void shouldThrowBadRequestExceptionWhenReportNotUnderReviewOnDelete() {
        User officer = defaultOfficer();
        DisasterReport report = underInspectionReport();
        DamageAssessment assessment = savedAssessment(officer, report);

        when(assessmentRepository.findById(300L)).thenReturn(Optional.of(assessment));
        when(userRepository.findByEmail("officer@example.com")).thenReturn(Optional.of(officer));

        assertThatThrownBy(() -> damageAssessmentService.deleteAssessment(300L, "officer@example.com"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot delete assessment");

        verify(assessmentRepository, never()).delete(any());
    }
}
