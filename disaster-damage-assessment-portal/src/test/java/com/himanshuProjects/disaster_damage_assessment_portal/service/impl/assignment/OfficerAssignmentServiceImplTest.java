package com.himanshuProjects.disaster_damage_assessment_portal.service.impl.assignment;

import com.himanshuProjects.disaster_damage_assessment_portal.dto.assignment.AssignmentPageResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.assignment.AssignOfficerRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.assignment.OfficerAssignmentResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.assignment.UpdateAssignmentStatusRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.disaster.DisasterReport;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.disaster.OfficerAssignment;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.User;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.AssignmentStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.DisasterType;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.NotificationType;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.ReportStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.RoleType;
import com.himanshuProjects.disaster_damage_assessment_portal.event.NotificationEvent;
import com.himanshuProjects.disaster_damage_assessment_portal.exception.BadRequestException;
import com.himanshuProjects.disaster_damage_assessment_portal.exception.ConflictException;
import com.himanshuProjects.disaster_damage_assessment_portal.exception.ResourceNotFoundException;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.disaster.DisasterReportRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.disaster.OfficerAssignmentRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.user.UserRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.testutil.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OfficerAssignmentServiceImplTest {

    @Mock
    private OfficerAssignmentRepository assignmentRepository;
    @Mock
    private DisasterReportRepository reportRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private OfficerAssignmentServiceImpl assignmentService;

    @BeforeEach
    void setUp() {
        assignmentService = new OfficerAssignmentServiceImpl(
                assignmentRepository, reportRepository, userRepository, eventPublisher);
    }

    private User citizen() {
        return TestDataFactory.createCitizen(1L, "citizen@example.com");
    }

    private User officer() {
        return TestDataFactory.createOfficer(2L, "officer@example.com");
    }

    private AssignOfficerRequest validAssignRequest() {
        AssignOfficerRequest request = new AssignOfficerRequest();
        request.setFieldOfficerId(2L);
        request.setInspectionDate(LocalDateTime.now().plusDays(2));
        request.setNotes("Please inspect the site");
        return request;
    }

    @Test
    @DisplayName("shouldAssignOfficerToSubmittedReport")
    void shouldAssignOfficerToSubmittedReport() {
        User citizen = citizen();
        User officer = officer();
        DisasterReport report = TestDataFactory.createReport(1L, citizen, ReportStatus.SUBMITTED);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(assignmentRepository.existsByDisasterReportIdAndAssignmentStatusIn(
                eq(1L), any())).thenReturn(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(officer));

        OfficerAssignment saved =
                TestDataFactory.createAssignment(10L, report, officer, AssignmentStatus.ASSIGNED);
        when(assignmentRepository.save(any(OfficerAssignment.class))).thenReturn(saved);
        when(reportRepository.save(any(DisasterReport.class))).thenReturn(report);

        OfficerAssignmentResponse response =
                assignmentService.assignOfficer(1L, validAssignRequest());

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getReportId()).isEqualTo(1L);
        assertThat(response.getOfficerId()).isEqualTo(2L);
        assertThat(response.getAssignmentStatus()).isEqualTo(AssignmentStatus.ASSIGNED);
        assertThat(response.getReportStatus()).isEqualTo(ReportStatus.ASSIGNED);
        assertThat(report.getStatus()).isEqualTo(ReportStatus.ASSIGNED);

        verify(reportRepository).save(report);

        ArgumentCaptor<NotificationEvent> eventCaptor =
                ArgumentCaptor.forClass(NotificationEvent.class);
        verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());
        List<NotificationEvent> events = eventCaptor.getAllValues();
        assertThat(events).hasSize(2);
        assertThat(events.get(0).getUserId()).isEqualTo(officer.getId());
        assertThat(events.get(0).getNotificationType()).isEqualTo(NotificationType.OFFICER_ASSIGNED);
        assertThat(events.get(1).getUserId()).isEqualTo(citizen.getId());
    }

    @Test
    @DisplayName("shouldAssignOfficerToReinspectionRequiredReport")
    void shouldAssignOfficerToReinspectionRequiredReport() {
        User citizen = citizen();
        User officer = officer();
        DisasterReport report =
                TestDataFactory.createReport(1L, citizen, ReportStatus.REINSPECTION_REQUIRED);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(assignmentRepository.existsByDisasterReportIdAndAssignmentStatusIn(
                eq(1L), any())).thenReturn(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(officer));

        OfficerAssignment saved =
                TestDataFactory.createAssignment(10L, report, officer, AssignmentStatus.ASSIGNED);
        when(assignmentRepository.save(any(OfficerAssignment.class))).thenReturn(saved);
        when(reportRepository.save(any(DisasterReport.class))).thenReturn(report);

        OfficerAssignmentResponse response =
                assignmentService.assignOfficer(1L, validAssignRequest());

        assertThat(response.getAssignmentStatus()).isEqualTo(AssignmentStatus.ASSIGNED);
        assertThat(report.getStatus()).isEqualTo(ReportStatus.ASSIGNED);
    }

    @Test
    @DisplayName("shouldThrowWhenReportNotFoundOnAssign")
    void shouldThrowWhenReportNotFoundOnAssign() {
        when(reportRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assignmentService.assignOfficer(99L, validAssignRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("DisasterReport");

        verify(assignmentRepository, never()).save(any(OfficerAssignment.class));
    }

    @Test
    @DisplayName("shouldThrowWhenAssigningToReportNotInEligibleStatus")
    void shouldThrowWhenAssigningToReportNotInEligibleStatus() {
        User citizen = citizen();
        DisasterReport report = TestDataFactory.createReport(1L, citizen, ReportStatus.APPROVED);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> assignmentService.assignOfficer(1L, validAssignRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must be SUBMITTED or REINSPECTION_REQUIRED");

        verify(assignmentRepository, never()).save(any(OfficerAssignment.class));
    }

    @Test
    @DisplayName("shouldThrowWhenReportAlreadyHasActiveAssignment")
    void shouldThrowWhenReportAlreadyHasActiveAssignment() {
        User citizen = citizen();
        DisasterReport report = TestDataFactory.createReport(1L, citizen, ReportStatus.SUBMITTED);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(assignmentRepository.existsByDisasterReportIdAndAssignmentStatusIn(
                eq(1L), any())).thenReturn(true);

        assertThatThrownBy(() -> assignmentService.assignOfficer(1L, validAssignRequest()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already has an active assignment");

        verify(assignmentRepository, never()).save(any(OfficerAssignment.class));
    }

    @Test
    @DisplayName("shouldThrowWhenOfficerNotFoundOnAssign")
    void shouldThrowWhenOfficerNotFoundOnAssign() {
        User citizen = citizen();
        DisasterReport report = TestDataFactory.createReport(1L, citizen, ReportStatus.SUBMITTED);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(assignmentRepository.existsByDisasterReportIdAndAssignmentStatusIn(
                eq(1L), any())).thenReturn(false);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        AssignOfficerRequest request = new AssignOfficerRequest();
        request.setFieldOfficerId(99L);
        request.setInspectionDate(LocalDateTime.now().plusDays(1));

        assertThatThrownBy(() -> assignmentService.assignOfficer(1L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");
    }

    @Test
    @DisplayName("shouldThrowWhenAssignedUserIsNotFieldOfficer")
    void shouldThrowWhenAssignedUserIsNotFieldOfficer() {
        User citizen = citizen();
        User nonOfficer = TestDataFactory.createCitizen(5L, "notofficer@example.com");
        DisasterReport report = TestDataFactory.createReport(1L, citizen, ReportStatus.SUBMITTED);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(assignmentRepository.existsByDisasterReportIdAndAssignmentStatusIn(
                eq(1L), any())).thenReturn(false);
        when(userRepository.findById(5L)).thenReturn(Optional.of(nonOfficer));

        AssignOfficerRequest request = new AssignOfficerRequest();
        request.setFieldOfficerId(5L);
        request.setInspectionDate(LocalDateTime.now().plusDays(1));

        assertThatThrownBy(() -> assignmentService.assignOfficer(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("is not a FIELD_OFFICER");

        verify(assignmentRepository, never()).save(any(OfficerAssignment.class));
    }

    @Test
    @DisplayName("shouldGetAssignmentById")
    void shouldGetAssignmentById() {
        User citizen = citizen();
        User officer = officer();
        DisasterReport report = TestDataFactory.createReport(1L, citizen, ReportStatus.ASSIGNED);
        OfficerAssignment assignment =
                TestDataFactory.createAssignment(10L, report, officer, AssignmentStatus.ASSIGNED);
        when(assignmentRepository.findById(10L)).thenReturn(Optional.of(assignment));

        OfficerAssignmentResponse response = assignmentService.getAssignmentById(10L);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getReportId()).isEqualTo(1L);
        assertThat(response.getOfficerId()).isEqualTo(2L);
        assertThat(response.getOfficerName()).isEqualTo(officer.getFullName());
        assertThat(response.getCitizenEmail()).isEqualTo(citizen.getEmail());
        assertThat(response.getAssignmentStatus()).isEqualTo(AssignmentStatus.ASSIGNED);
    }

    @Test
    @DisplayName("shouldThrowWhenAssignmentNotFoundOnGetById")
    void shouldThrowWhenAssignmentNotFoundOnGetById() {
        when(assignmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assignmentService.getAssignmentById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("OfficerAssignment");
    }

    @Test
    @DisplayName("shouldGetAssignmentByReportId")
    void shouldGetAssignmentByReportId() {
        User citizen = citizen();
        User officer = officer();
        DisasterReport report = TestDataFactory.createReport(1L, citizen, ReportStatus.ASSIGNED);
        OfficerAssignment assignment =
                TestDataFactory.createAssignment(10L, report, officer, AssignmentStatus.ASSIGNED);
        when(assignmentRepository.findByDisasterReportId(1L)).thenReturn(Optional.of(assignment));

        OfficerAssignmentResponse response = assignmentService.getAssignmentByReportId(1L);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getReportId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("shouldThrowWhenAssignmentNotExistForReport")
    void shouldThrowWhenAssignmentNotExistForReport() {
        when(assignmentRepository.findByDisasterReportId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assignmentService.getAssignmentByReportId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("OfficerAssignment");
    }

    @Test
    @DisplayName("shouldUpdateAssignmentStatusAssignedToAccepted")
    void shouldUpdateAssignmentStatusAssignedToAccepted() {
        User citizen = citizen();
        User officer = officer();
        DisasterReport report = TestDataFactory.createReport(1L, citizen, ReportStatus.ASSIGNED);
        OfficerAssignment assignment =
                TestDataFactory.createAssignment(10L, report, officer, AssignmentStatus.ASSIGNED);
        when(assignmentRepository.findById(10L)).thenReturn(Optional.of(assignment));
        when(userRepository.findByEmail(officer.getEmail())).thenReturn(Optional.of(officer));

        UpdateAssignmentStatusRequest request = new UpdateAssignmentStatusRequest();
        request.setAssignmentStatus(AssignmentStatus.ACCEPTED);
        when(assignmentRepository.save(any(OfficerAssignment.class))).thenReturn(assignment);
        when(reportRepository.save(any(DisasterReport.class))).thenReturn(report);

        OfficerAssignmentResponse response =
                assignmentService.updateAssignmentStatus(10L, officer.getEmail(), request);

        assertThat(response.getAssignmentStatus()).isEqualTo(AssignmentStatus.ACCEPTED);
        assertThat(report.getStatus()).isEqualTo(ReportStatus.UNDER_INSPECTION);
        verify(reportRepository).save(report);
    }

    @Test
    @DisplayName("shouldUpdateAssignmentStatusAcceptedToInProgress")
    void shouldUpdateAssignmentStatusAcceptedToInProgress() {
        User citizen = citizen();
        User officer = officer();
        DisasterReport report = TestDataFactory.createReport(1L, citizen, ReportStatus.UNDER_INSPECTION);
        OfficerAssignment assignment =
                TestDataFactory.createAssignment(10L, report, officer, AssignmentStatus.ACCEPTED);
        when(assignmentRepository.findById(10L)).thenReturn(Optional.of(assignment));
        when(userRepository.findByEmail(officer.getEmail())).thenReturn(Optional.of(officer));

        UpdateAssignmentStatusRequest request = new UpdateAssignmentStatusRequest();
        request.setAssignmentStatus(AssignmentStatus.IN_PROGRESS);
        when(assignmentRepository.save(any(OfficerAssignment.class))).thenReturn(assignment);
        when(reportRepository.save(any(DisasterReport.class))).thenReturn(report);

        OfficerAssignmentResponse response =
                assignmentService.updateAssignmentStatus(10L, officer.getEmail(), request);

        assertThat(response.getAssignmentStatus()).isEqualTo(AssignmentStatus.IN_PROGRESS);
        assertThat(report.getStatus()).isEqualTo(ReportStatus.UNDER_INSPECTION);
    }

    @Test
    @DisplayName("shouldUpdateAssignmentStatusInProgressToCompletedAndUpdateReportToUnderReview")
    void shouldUpdateAssignmentStatusInProgressToCompletedAndUpdateReportToUnderReview() {
        User citizen = citizen();
        User officer = officer();
        DisasterReport report = TestDataFactory.createReport(1L, citizen, ReportStatus.UNDER_INSPECTION);
        OfficerAssignment assignment =
                TestDataFactory.createAssignment(10L, report, officer, AssignmentStatus.IN_PROGRESS);
        when(assignmentRepository.findById(10L)).thenReturn(Optional.of(assignment));
        when(userRepository.findByEmail(officer.getEmail())).thenReturn(Optional.of(officer));

        UpdateAssignmentStatusRequest request = new UpdateAssignmentStatusRequest();
        request.setAssignmentStatus(AssignmentStatus.COMPLETED);
        when(assignmentRepository.save(any(OfficerAssignment.class))).thenReturn(assignment);
        when(reportRepository.save(any(DisasterReport.class))).thenReturn(report);

        OfficerAssignmentResponse response =
                assignmentService.updateAssignmentStatus(10L, officer.getEmail(), request);

        assertThat(response.getAssignmentStatus()).isEqualTo(AssignmentStatus.COMPLETED);
        assertThat(report.getStatus()).isEqualTo(ReportStatus.UNDER_REVIEW);
        verify(reportRepository).save(report);
    }

    @Test
    @DisplayName("shouldUpdateNotesWhenProvidedOnStatusUpdate")
    void shouldUpdateNotesWhenProvidedOnStatusUpdate() {
        User citizen = citizen();
        User officer = officer();
        DisasterReport report = TestDataFactory.createReport(1L, citizen, ReportStatus.ASSIGNED);
        OfficerAssignment assignment =
                TestDataFactory.createAssignment(10L, report, officer, AssignmentStatus.ASSIGNED);
        when(assignmentRepository.findById(10L)).thenReturn(Optional.of(assignment));
        when(userRepository.findByEmail(officer.getEmail())).thenReturn(Optional.of(officer));

        UpdateAssignmentStatusRequest request = new UpdateAssignmentStatusRequest();
        request.setAssignmentStatus(AssignmentStatus.ACCEPTED);
        request.setNotes("Accepted. Will inspect tomorrow.");
        when(assignmentRepository.save(any(OfficerAssignment.class))).thenReturn(assignment);
        when(reportRepository.save(any(DisasterReport.class))).thenReturn(report);

        assignmentService.updateAssignmentStatus(10L, officer.getEmail(), request);

        assertThat(assignment.getNotes()).isEqualTo("Accepted. Will inspect tomorrow.");
    }

    @Test
    @DisplayName("shouldThrowWhenAssignmentNotFoundOnStatusUpdate")
    void shouldThrowWhenAssignmentNotFoundOnStatusUpdate() {
        when(assignmentRepository.findById(99L)).thenReturn(Optional.empty());

        UpdateAssignmentStatusRequest request = new UpdateAssignmentStatusRequest();
        request.setAssignmentStatus(AssignmentStatus.ACCEPTED);

        assertThatThrownBy(() ->
                assignmentService.updateAssignmentStatus(99L, "officer@example.com", request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("shouldThrowWhenOfficerNotFoundOnStatusUpdate")
    void shouldThrowWhenOfficerNotFoundOnStatusUpdate() {
        User citizen = citizen();
        User officer = officer();
        DisasterReport report = TestDataFactory.createReport(1L, citizen, ReportStatus.ASSIGNED);
        OfficerAssignment assignment =
                TestDataFactory.createAssignment(10L, report, officer, AssignmentStatus.ASSIGNED);
        when(assignmentRepository.findById(10L)).thenReturn(Optional.of(assignment));
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        UpdateAssignmentStatusRequest request = new UpdateAssignmentStatusRequest();
        request.setAssignmentStatus(AssignmentStatus.ACCEPTED);

        assertThatThrownBy(() ->
                assignmentService.updateAssignmentStatus(10L, "missing@example.com", request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("shouldThrowWhenUpdatingAnotherOfficersAssignment")
    void shouldThrowWhenUpdatingAnotherOfficersAssignment() {
        User citizen = citizen();
        User officer = officer();
        User otherOfficer = TestDataFactory.createOfficer(7L, "other.officer@example.com");
        DisasterReport report = TestDataFactory.createReport(1L, citizen, ReportStatus.ASSIGNED);
        OfficerAssignment assignment =
                TestDataFactory.createAssignment(10L, report, officer, AssignmentStatus.ASSIGNED);
        when(assignmentRepository.findById(10L)).thenReturn(Optional.of(assignment));
        when(userRepository.findByEmail(otherOfficer.getEmail()))
                .thenReturn(Optional.of(otherOfficer));

        UpdateAssignmentStatusRequest request = new UpdateAssignmentStatusRequest();
        request.setAssignmentStatus(AssignmentStatus.ACCEPTED);

        assertThatThrownBy(() ->
                assignmentService.updateAssignmentStatus(10L, otherOfficer.getEmail(), request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("only update your own assignments");

        verify(assignmentRepository, never()).save(any(OfficerAssignment.class));
    }

    @Test
    @DisplayName("shouldThrowOnInvalidAssignmentStatusTransition")
    void shouldThrowOnInvalidAssignmentStatusTransition() {
        User citizen = citizen();
        User officer = officer();
        DisasterReport report = TestDataFactory.createReport(1L, citizen, ReportStatus.ASSIGNED);
        OfficerAssignment assignment =
                TestDataFactory.createAssignment(10L, report, officer, AssignmentStatus.ASSIGNED);
        when(assignmentRepository.findById(10L)).thenReturn(Optional.of(assignment));
        when(userRepository.findByEmail(officer.getEmail())).thenReturn(Optional.of(officer));

        UpdateAssignmentStatusRequest request = new UpdateAssignmentStatusRequest();
        request.setAssignmentStatus(AssignmentStatus.COMPLETED);

        assertThatThrownBy(() ->
                assignmentService.updateAssignmentStatus(10L, officer.getEmail(), request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid assignment status transition");

        verify(assignmentRepository, never()).save(any(OfficerAssignment.class));
        verify(reportRepository, never()).save(any(DisasterReport.class));
    }

    @Test
    @DisplayName("shouldThrowWhenUpdatingCompletedAssignment")
    void shouldThrowWhenUpdatingCompletedAssignment() {
        User citizen = citizen();
        User officer = officer();
        DisasterReport report = TestDataFactory.createReport(1L, citizen, ReportStatus.UNDER_REVIEW);
        OfficerAssignment assignment =
                TestDataFactory.createAssignment(10L, report, officer, AssignmentStatus.COMPLETED);
        when(assignmentRepository.findById(10L)).thenReturn(Optional.of(assignment));
        when(userRepository.findByEmail(officer.getEmail())).thenReturn(Optional.of(officer));

        UpdateAssignmentStatusRequest request = new UpdateAssignmentStatusRequest();
        request.setAssignmentStatus(AssignmentStatus.IN_PROGRESS);

        assertThatThrownBy(() ->
                assignmentService.updateAssignmentStatus(10L, officer.getEmail(), request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("shouldSearchAssignments")
    void shouldSearchAssignments() {
        User citizen = citizen();
        User officer = officer();
        DisasterReport report = TestDataFactory.createReport(1L, citizen, ReportStatus.ASSIGNED);
        OfficerAssignment assignment =
                TestDataFactory.createAssignment(10L, report, officer, AssignmentStatus.ASSIGNED);
        Page<OfficerAssignment> page = new PageImpl<>(
                List.of(assignment), PageRequest.of(0, 10), 1);
        when(assignmentRepository.searchAssignments(
                any(), any(), any(), any(Pageable.class))).thenReturn(page);

        AssignmentPageResponse response = assignmentService.searchAssignments(
                "flood", AssignmentStatus.ASSIGNED, 2L, 0, 10, "assignedAt", "desc");

        assertThat(response.getAssignments()).hasSize(1);
        assertThat(response.getAssignments().get(0).getReportTitle()).isEqualTo("Flood report");
        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("shouldThrowWhenSortFieldInvalidOnSearchAssignments")
    void shouldThrowWhenSortFieldInvalidOnSearchAssignments() {
        assertThatThrownBy(() -> assignmentService.searchAssignments(
                "flood", AssignmentStatus.ASSIGNED, 2L, 0, 10, "bogus", "asc"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid sort field");
    }

    @Test
    @DisplayName("shouldGetMyAssignmentsWithStatus")
    void shouldGetMyAssignmentsWithStatus() {
        User citizen = citizen();
        User officer = officer();
        when(userRepository.findByEmail(officer.getEmail())).thenReturn(Optional.of(officer));

        DisasterReport report = TestDataFactory.createReport(1L, citizen, ReportStatus.ASSIGNED);
        OfficerAssignment assignment =
                TestDataFactory.createAssignment(10L, report, officer, AssignmentStatus.ASSIGNED);
        Page<OfficerAssignment> page = new PageImpl<>(
                List.of(assignment), PageRequest.of(0, 10), 1);
        when(assignmentRepository.searchAssignments(
                isNull(), eq(AssignmentStatus.ASSIGNED), eq(2L), any(Pageable.class)))
                .thenReturn(page);

        AssignmentPageResponse response = assignmentService.getMyAssignments(
                officer.getEmail(), AssignmentStatus.ASSIGNED, 0, 10, "id", "asc");

        assertThat(response.getAssignments()).hasSize(1);
        assertThat(response.getAssignments().get(0).getOfficerId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("shouldGetMyAssignmentsWithoutStatus")
    void shouldGetMyAssignmentsWithoutStatus() {
        User citizen = citizen();
        User officer = officer();
        when(userRepository.findByEmail(officer.getEmail())).thenReturn(Optional.of(officer));

        DisasterReport report = TestDataFactory.createReport(1L, citizen, ReportStatus.ASSIGNED);
        OfficerAssignment assignment =
                TestDataFactory.createAssignment(10L, report, officer, AssignmentStatus.ASSIGNED);
        Page<OfficerAssignment> page = new PageImpl<>(
                List.of(assignment), PageRequest.of(0, 10), 1);
        when(assignmentRepository.searchAssignments(
                isNull(), isNull(), eq(2L), any(Pageable.class)))
                .thenReturn(page);

        AssignmentPageResponse response = assignmentService.getMyAssignments(
                officer.getEmail(), null, 0, 10, "id", "asc");

        assertThat(response.getAssignments()).hasSize(1);
    }

    @Test
    @DisplayName("shouldThrowWhenUserNotFoundOnGetMyAssignments")
    void shouldThrowWhenUserNotFoundOnGetMyAssignments() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assignmentService.getMyAssignments(
                "missing@example.com", AssignmentStatus.ASSIGNED, 0, 10, "id", "asc"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("shouldThrowWhenSortFieldInvalidOnGetMyAssignments")
    void shouldThrowWhenSortFieldInvalidOnGetMyAssignments() {
        User officer = officer();
        when(userRepository.findByEmail(officer.getEmail())).thenReturn(Optional.of(officer));

        assertThatThrownBy(() -> assignmentService.getMyAssignments(
                officer.getEmail(), AssignmentStatus.ASSIGNED, 0, 10, "bogus", "asc"))
                .isInstanceOf(BadRequestException.class);

        verify(assignmentRepository, never()).searchAssignments(
                any(), any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("shouldReassignOfficerFromActiveAssignment")
    void shouldReassignOfficerFromActiveAssignment() {
        User citizen = citizen();
        User oldOfficer = officer();
        User newOfficer = TestDataFactory.createOfficer(8L, "new.officer@example.com");
        DisasterReport report = TestDataFactory.createReport(1L, citizen, ReportStatus.ASSIGNED);
        OfficerAssignment oldAssignment =
                TestDataFactory.createAssignment(10L, report, oldOfficer, AssignmentStatus.ASSIGNED);
        when(assignmentRepository.findById(10L)).thenReturn(Optional.of(oldAssignment));
        when(userRepository.findById(8L)).thenReturn(Optional.of(newOfficer));

        OfficerAssignment reassigned =
                TestDataFactory.createAssignment(10L, report, oldOfficer, AssignmentStatus.REASSIGNED);
        when(assignmentRepository.save(any(OfficerAssignment.class)))
                .thenReturn(reassigned)
                .thenAnswer(inv -> {
                    OfficerAssignment a = inv.getArgument(0);
                    return a;
                });
        when(reportRepository.save(any(DisasterReport.class))).thenReturn(report);

        AssignOfficerRequest request = new AssignOfficerRequest();
        request.setFieldOfficerId(8L);
        request.setInspectionDate(LocalDateTime.now().plusDays(3));
        request.setNotes("Reassigning to a new officer");

        OfficerAssignmentResponse response =
                assignmentService.reassignOfficer(10L, request);

        assertThat(response.getAssignmentStatus()).isEqualTo(AssignmentStatus.ASSIGNED);
        assertThat(oldAssignment.getAssignmentStatus()).isEqualTo(AssignmentStatus.REASSIGNED);
        assertThat(report.getStatus()).isEqualTo(ReportStatus.ASSIGNED);

        ArgumentCaptor<OfficerAssignment> saveCaptor =
                ArgumentCaptor.forClass(OfficerAssignment.class);
        verify(assignmentRepository, times(2)).save(saveCaptor.capture());
        List<OfficerAssignment> saved = saveCaptor.getAllValues();
        assertThat(saved).hasSize(2);
        assertThat(saved.get(1).getFieldOfficer()).isEqualTo(newOfficer);
    }

    @Test
    @DisplayName("shouldThrowWhenReassigningUnknownAssignment")
    void shouldThrowWhenReassigningUnknownAssignment() {
        when(assignmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assignmentService.reassignOfficer(99L, validAssignRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("shouldThrowWhenReassigningCompletedAssignment")
    void shouldThrowWhenReassigningCompletedAssignment() {
        User citizen = citizen();
        User officer = officer();
        DisasterReport report = TestDataFactory.createReport(1L, citizen, ReportStatus.UNDER_REVIEW);
        OfficerAssignment completed =
                TestDataFactory.createAssignment(10L, report, officer, AssignmentStatus.COMPLETED);
        when(assignmentRepository.findById(10L)).thenReturn(Optional.of(completed));

        assertThatThrownBy(() -> assignmentService.reassignOfficer(10L, validAssignRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot reassign assignment with status");

        verify(assignmentRepository, never()).save(any(OfficerAssignment.class));
    }

    @Test
    @DisplayName("shouldThrowWhenNewOfficerNotFoundOnReassign")
    void shouldThrowWhenNewOfficerNotFoundOnReassign() {
        User citizen = citizen();
        User officer = officer();
        DisasterReport report = TestDataFactory.createReport(1L, citizen, ReportStatus.ASSIGNED);
        OfficerAssignment assignment =
                TestDataFactory.createAssignment(10L, report, officer, AssignmentStatus.ASSIGNED);
        when(assignmentRepository.findById(10L)).thenReturn(Optional.of(assignment));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        AssignOfficerRequest request = new AssignOfficerRequest();
        request.setFieldOfficerId(99L);
        request.setInspectionDate(LocalDateTime.now().plusDays(1));

        assertThatThrownBy(() -> assignmentService.reassignOfficer(10L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("shouldThrowWhenNewOfficerNotFieldOfficerOnReassign")
    void shouldThrowWhenNewOfficerNotFieldOfficerOnReassign() {
        User citizen = citizen();
        User officer = officer();
        DisasterReport report = TestDataFactory.createReport(1L, citizen, ReportStatus.ASSIGNED);
        OfficerAssignment assignment =
                TestDataFactory.createAssignment(10L, report, officer, AssignmentStatus.ASSIGNED);
        when(assignmentRepository.findById(10L)).thenReturn(Optional.of(assignment));
        when(userRepository.findById(5L)).thenReturn(Optional.of(
                TestDataFactory.createCitizen(5L, "notofficer@example.com")));

        AssignOfficerRequest request = new AssignOfficerRequest();
        request.setFieldOfficerId(5L);
        request.setInspectionDate(LocalDateTime.now().plusDays(1));

        assertThatThrownBy(() -> assignmentService.reassignOfficer(10L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("is not a FIELD_OFFICER");

        verify(assignmentRepository, never()).save(any(OfficerAssignment.class));
    }

    @Test
    @DisplayName("shouldThrowWhenReassigningToSameOfficer")
    void shouldThrowWhenReassigningToSameOfficer() {
        User citizen = citizen();
        User officer = officer();
        DisasterReport report = TestDataFactory.createReport(1L, citizen, ReportStatus.ASSIGNED);
        OfficerAssignment assignment =
                TestDataFactory.createAssignment(10L, report, officer, AssignmentStatus.ASSIGNED);
        when(assignmentRepository.findById(10L)).thenReturn(Optional.of(assignment));
        when(userRepository.findById(2L)).thenReturn(Optional.of(officer));

        AssignOfficerRequest request = new AssignOfficerRequest();
        request.setFieldOfficerId(2L);
        request.setInspectionDate(LocalDateTime.now().plusDays(1));

        assertThatThrownBy(() -> assignmentService.reassignOfficer(10L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot reassign to the same officer");

        verify(assignmentRepository, never()).save(any(OfficerAssignment.class));
    }
}
