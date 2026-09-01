package com.himanshuProjects.disaster_damage_assessment_portal.service.impl.disaster;

import com.himanshuProjects.disaster_damage_assessment_portal.dto.disaster.AddReportImagesRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.disaster.CreateDisasterReportRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.disaster.DisasterReportPageResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.disaster.DisasterReportResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.disaster.UpdateDisasterReportRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.disaster.UpdateReportStatusRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.disaster.DisasterReport;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.disaster.ReportImage;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.User;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.DisasterType;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.ReportStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.RoleType;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.NotificationType;
import com.himanshuProjects.disaster_damage_assessment_portal.event.NotificationEvent;
import com.himanshuProjects.disaster_damage_assessment_portal.exception.BadRequestException;
import com.himanshuProjects.disaster_damage_assessment_portal.exception.ResourceNotFoundException;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.disaster.DisasterReportRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.disaster.ReportImageRepository;
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
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisasterReportServiceImplTest {

    @Mock
    private DisasterReportRepository reportRepository;
    @Mock
    private ReportImageRepository reportImageRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private DisasterReportServiceImpl reportService;

    @BeforeEach
    void setUp() {
        reportService = new DisasterReportServiceImpl(
                reportRepository, reportImageRepository, userRepository, eventPublisher);
    }

    private User citizen() {
        return TestDataFactory.createCitizen(1L, "citizen@example.com");
    }

    private void stubNoImages() {
        when(reportImageRepository.findByDisasterReportIdOrderByUploadedAtAsc(anyLong()))
                .thenReturn(Collections.emptyList());
    }

    private CreateDisasterReportRequest validCreateRequest() {
        CreateDisasterReportRequest request = new CreateDisasterReportRequest();
        request.setTitle("Flood in city");
        request.setDescription("Heavy rains caused flooding");
        request.setDisasterType(DisasterType.FLOOD);
        request.setIncidentAddress("Main Street");
        request.setLatitude(12.34);
        request.setLongitude(56.78);
        return request;
    }

    private UpdateDisasterReportRequest validUpdateRequest() {
        UpdateDisasterReportRequest request = new UpdateDisasterReportRequest();
        request.setTitle("Updated flood report");
        request.setDescription("Updated description");
        request.setDisasterType(DisasterType.FIRE);
        request.setIncidentAddress("New Address");
        request.setLatitude(1.11);
        request.setLongitude(2.22);
        return request;
    }

    @Test
    @DisplayName("shouldCreateReportSuccessfully")
    void shouldCreateReportSuccessfully() {
        User citizen = citizen();
        CreateDisasterReportRequest request = validCreateRequest();
        when(userRepository.findByEmail(citizen.getEmail())).thenReturn(Optional.of(citizen));

        DisasterReport saved = TestDataFactory.createReport(1L, citizen, ReportStatus.SUBMITTED);
        when(reportRepository.save(any(DisasterReport.class))).thenReturn(saved);
        stubNoImages();

        DisasterReportResponse response = reportService.createReport(citizen.getEmail(), request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("Flood report");
        assertThat(response.getStatus()).isEqualTo(ReportStatus.SUBMITTED);
        assertThat(response.getCitizenEmail()).isEqualTo(citizen.getEmail());

        ArgumentCaptor<DisasterReport> captor = ArgumentCaptor.forClass(DisasterReport.class);
        verify(reportRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ReportStatus.SUBMITTED);
        assertThat(captor.getValue().getCitizen()).isEqualTo(citizen);

        ArgumentCaptor<NotificationEvent> eventCaptor =
                ArgumentCaptor.forClass(NotificationEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        NotificationEvent event = eventCaptor.getValue();
        assertThat(event.getUserId()).isEqualTo(citizen.getId());
        assertThat(event.getNotificationType()).isEqualTo(NotificationType.REPORT_SUBMITTED);
        assertThat(event.getEntityType()).isEqualTo("DISASTER_REPORT");
    }

    @Test
    @DisplayName("shouldCreateReportWithImages")
    void shouldCreateReportWithImages() {
        User citizen = citizen();
        CreateDisasterReportRequest request = validCreateRequest();
        request.setImageUrls(List.of("http://img1.com/a.jpg", "http://img2.com/b.jpg"));

        when(userRepository.findByEmail(citizen.getEmail())).thenReturn(Optional.of(citizen));
        DisasterReport saved = TestDataFactory.createReport(1L, citizen, ReportStatus.SUBMITTED);
        when(reportRepository.save(any(DisasterReport.class))).thenReturn(saved);
        when(reportImageRepository.save(any(ReportImage.class))).thenAnswer(inv -> inv.getArgument(0));
        stubNoImages();

        DisasterReportResponse response = reportService.createReport(citizen.getEmail(), request);

        assertThat(response.getImages()).isEmpty();
        verify(reportImageRepository, times(2)).save(any(ReportImage.class));

        ArgumentCaptor<ReportImage> imageCaptor = ArgumentCaptor.forClass(ReportImage.class);
        verify(reportImageRepository, times(2)).save(imageCaptor.capture());
        List<ReportImage> images = imageCaptor.getAllValues();
        assertThat(images).hasSize(2);
        assertThat(images.get(0).getImageUrl()).isEqualTo("http://img1.com/a.jpg");
        assertThat(images.get(0).getDisasterReport()).isEqualTo(saved);
    }

    @Test
    @DisplayName("shouldThrowWhenCitizenNotFoundOnCreate")
    void shouldThrowWhenCitizenNotFoundOnCreate() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.createReport(
                "missing@example.com", validCreateRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");

        verify(reportRepository, never()).save(any(DisasterReport.class));
    }

    @Test
    @DisplayName("shouldThrowWhenImageCountExceedsMaxOnCreate")
    void shouldThrowWhenImageCountExceedsMaxOnCreate() {
        User citizen = citizen();
        CreateDisasterReportRequest request = validCreateRequest();
        List<String> urls = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            urls.add("http://img.com/" + i + ".jpg");
        }
        request.setImageUrls(urls);

        when(userRepository.findByEmail(citizen.getEmail())).thenReturn(Optional.of(citizen));
        DisasterReport saved = TestDataFactory.createReport(1L, citizen, ReportStatus.SUBMITTED);
        when(reportRepository.save(any(DisasterReport.class))).thenReturn(saved);

        assertThatThrownBy(() -> reportService.createReport(citizen.getEmail(), request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("more than 10 images");

        verify(reportImageRepository, never()).save(any(ReportImage.class));
    }

    @Test
    @DisplayName("shouldGetReportById")
    void shouldGetReportById() {
        User citizen = citizen();
        DisasterReport report = TestDataFactory.createReport(1L, citizen, ReportStatus.ASSIGNED);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        stubNoImages();

        DisasterReportResponse response = reportService.getReportById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("Flood report");
        assertThat(response.getStatus()).isEqualTo(ReportStatus.ASSIGNED);
        assertThat(response.getCitizenId()).isEqualTo(citizen.getId());
    }

    @Test
    @DisplayName("shouldThrowWhenReportNotFoundOnGetById")
    void shouldThrowWhenReportNotFoundOnGetById() {
        when(reportRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.getReportById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("DisasterReport");
    }

    @Test
    @DisplayName("shouldGetReportByIdForCitizenWhenOwned")
    void shouldGetReportByIdForCitizenWhenOwned() {
        User citizen = citizen();
        DisasterReport report = TestDataFactory.createReport(1L, citizen, ReportStatus.SUBMITTED);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        stubNoImages();

        DisasterReportResponse response =
                reportService.getReportByIdForCitizen(1L, citizen.getEmail());

        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("shouldThrowWhenCitizenDoesNotOwnReportOnGetForCitizen")
    void shouldThrowWhenCitizenDoesNotOwnReportOnGetForCitizen() {
        User citizen = citizen();
        User otherCitizen = TestDataFactory.createCitizen(2L, "other@example.com");
        DisasterReport report = TestDataFactory.createReport(1L, citizen, ReportStatus.SUBMITTED);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> reportService.getReportByIdForCitizen(1L, otherCitizen.getEmail()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("do not have access");
    }

    @Test
    @DisplayName("shouldGetMyReportsPaged")
    void shouldGetMyReportsPaged() {
        User citizen = citizen();
        when(userRepository.findByEmail(citizen.getEmail())).thenReturn(Optional.of(citizen));

        DisasterReport report1 = TestDataFactory.createReport(1L, citizen, ReportStatus.SUBMITTED);
        DisasterReport report2 = TestDataFactory.createReport(2L, citizen, ReportStatus.ASSIGNED);
        Page<DisasterReport> page = new PageImpl<>(
                List.of(report1, report2),
                org.springframework.data.domain.PageRequest.of(0, 10),
                2);
        when(reportRepository.findByCitizenIdOrderByCreatedAtDesc(
                eq(citizen.getId()), any(Pageable.class))).thenReturn(page);
        stubNoImages();

        DisasterReportPageResponse response =
                reportService.getMyReports(citizen.getEmail(), 0, 10, "createdAt", "desc");

        assertThat(response.getReports()).hasSize(2);
        assertThat(response.getPageNumber()).isZero();
        assertThat(response.getPageSize()).isEqualTo(10);
        assertThat(response.getTotalElements()).isEqualTo(2);
        assertThat(response.getTotalPages()).isEqualTo(1);
        assertThat(response.isLast()).isTrue();
    }

    @Test
    @DisplayName("shouldThrowWhenUserNotFoundOnGetMyReports")
    void shouldThrowWhenUserNotFoundOnGetMyReports() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.getMyReports(
                "missing@example.com", 0, 10, "createdAt", "desc"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("shouldThrowWhenSortFieldInvalidOnGetMyReports")
    void shouldThrowWhenSortFieldInvalidOnGetMyReports() {
        User citizen = citizen();
        when(userRepository.findByEmail(citizen.getEmail())).thenReturn(Optional.of(citizen));

        assertThatThrownBy(() -> reportService.getMyReports(
                citizen.getEmail(), 0, 10, "bogusField", "desc"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid sort field");

        verify(reportRepository, never()).findByCitizenIdOrderByCreatedAtDesc(
                anyLong(), any(Pageable.class));
    }

    @Test
    @DisplayName("shouldSearchReports")
    void shouldSearchReports() {
        User citizen = citizen();
        DisasterReport report = TestDataFactory.createReport(1L, citizen, ReportStatus.SUBMITTED);
        Page<DisasterReport> page = new PageImpl<>(
                List.of(report),
                org.springframework.data.domain.PageRequest.of(0, 5),
                1);
        when(reportRepository.searchReports(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);
        stubNoImages();

        DisasterReportPageResponse response = reportService.searchReports(
                "flood", DisasterType.FLOOD, ReportStatus.SUBMITTED, 1L,
                0, 5, "reportedAt", "asc");

        assertThat(response.getReports()).hasSize(1);
        assertThat(response.getReports().get(0).getTitle()).isEqualTo("Flood report");
    }

    @Test
    @DisplayName("shouldThrowWhenSortFieldInvalidOnSearchReports")
    void shouldThrowWhenSortFieldInvalidOnSearchReports() {
        assertThatThrownBy(() -> reportService.searchReports(
                "flood", DisasterType.FLOOD, ReportStatus.SUBMITTED, 1L,
                0, 5, "bogus", "asc"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid sort field");
    }

    @Test
    @DisplayName("shouldUpdateOwnedSubmittedReport")
    void shouldUpdateOwnedSubmittedReport() {
        User citizen = citizen();
        DisasterReport report = TestDataFactory.createReport(1L, citizen, ReportStatus.SUBMITTED);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        UpdateDisasterReportRequest request = validUpdateRequest();
        DisasterReport updated = TestDataFactory.createReport(1L, citizen, ReportStatus.SUBMITTED);
        updated.setTitle(request.getTitle());
        updated.setDescription(request.getDescription());
        updated.setDisasterType(request.getDisasterType());
        when(reportRepository.save(any(DisasterReport.class))).thenReturn(updated);
        stubNoImages();

        DisasterReportResponse response = reportService.updateReport(1L, citizen.getEmail(), request);

        assertThat(response.getTitle()).isEqualTo("Updated flood report");
        assertThat(response.getDescription()).isEqualTo("Updated description");
        assertThat(response.getDisasterType()).isEqualTo(DisasterType.FIRE);

        ArgumentCaptor<DisasterReport> captor = ArgumentCaptor.forClass(DisasterReport.class);
        verify(reportRepository).save(captor.capture());
        assertThat(captor.getValue().getIncidentAddress()).isEqualTo("New Address");
        assertThat(captor.getValue().getLatitude()).isEqualTo(1.11);
        assertThat(captor.getValue().getLongitude()).isEqualTo(2.22);
    }

    @Test
    @DisplayName("shouldThrowWhenUpdatingReportNotOwnedByCitizen")
    void shouldThrowWhenUpdatingReportNotOwnedByCitizen() {
        User citizen = citizen();
        User otherCitizen = TestDataFactory.createCitizen(2L, "other@example.com");
        DisasterReport report = TestDataFactory.createReport(1L, citizen, ReportStatus.SUBMITTED);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> reportService.updateReport(
                1L, otherCitizen.getEmail(), validUpdateRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("only update your own reports");

        verify(reportRepository, never()).save(any(DisasterReport.class));
    }

    @Test
    @DisplayName("shouldThrowWhenUpdatingReportNotInEditableStatus")
    void shouldThrowWhenUpdatingReportNotInEditableStatus() {
        User citizen = citizen();
        DisasterReport report = TestDataFactory.createReport(1L, citizen, ReportStatus.ASSIGNED);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> reportService.updateReport(
                1L, citizen.getEmail(), validUpdateRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only SUBMITTED reports can be edited");

        verify(reportRepository, never()).save(any(DisasterReport.class));
    }

    @Test
    @DisplayName("shouldThrowWhenReportNotFoundOnUpdate")
    void shouldThrowWhenReportNotFoundOnUpdate() {
        when(reportRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.updateReport(
                99L, "citizen@example.com", validUpdateRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("shouldUpdateReportStatusSubmittedToAssigned")
    void shouldUpdateReportStatusSubmittedToAssigned() {
        User citizen = citizen();
        DisasterReport report = TestDataFactory.createReport(1L, citizen, ReportStatus.SUBMITTED);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        UpdateReportStatusRequest request = new UpdateReportStatusRequest();
        request.setStatus(ReportStatus.ASSIGNED);
        DisasterReport updated = TestDataFactory.createReport(1L, citizen, ReportStatus.ASSIGNED);
        when(reportRepository.save(any(DisasterReport.class))).thenReturn(updated);
        stubNoImages();

        DisasterReportResponse response = reportService.updateReportStatus(1L, request);

        assertThat(response.getStatus()).isEqualTo(ReportStatus.ASSIGNED);
        verify(reportRepository).save(any(DisasterReport.class));
    }

    @Test
    @DisplayName("shouldUpdateReportStatusUnderReviewToApproved")
    void shouldUpdateReportStatusUnderReviewToApproved() {
        User citizen = citizen();
        DisasterReport report = TestDataFactory.createReport(1L, citizen, ReportStatus.UNDER_REVIEW);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        UpdateReportStatusRequest request = new UpdateReportStatusRequest();
        request.setStatus(ReportStatus.APPROVED);
        DisasterReport updated = TestDataFactory.createReport(1L, citizen, ReportStatus.APPROVED);
        when(reportRepository.save(any(DisasterReport.class))).thenReturn(updated);
        stubNoImages();

        DisasterReportResponse response = reportService.updateReportStatus(1L, request);

        assertThat(response.getStatus()).isEqualTo(ReportStatus.APPROVED);
    }

    @Test
    @DisplayName("shouldReinspectionRequiredForUnderInspection")
    void shouldReinspectionRequiredForUnderInspection() {
        User citizen = citizen();
        DisasterReport report = TestDataFactory.createReport(1L, citizen, ReportStatus.UNDER_INSPECTION);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        UpdateReportStatusRequest request = new UpdateReportStatusRequest();
        request.setStatus(ReportStatus.REINSPECTION_REQUIRED);
        DisasterReport updated =
                TestDataFactory.createReport(1L, citizen, ReportStatus.REINSPECTION_REQUIRED);
        when(reportRepository.save(any(DisasterReport.class))).thenReturn(updated);
        stubNoImages();

        DisasterReportResponse response = reportService.updateReportStatus(1L, request);

        assertThat(response.getStatus()).isEqualTo(ReportStatus.REINSPECTION_REQUIRED);
    }

    @Test
    @DisplayName("shouldThrowOnInvalidStatusTransition")
    void shouldThrowOnInvalidStatusTransition() {
        User citizen = citizen();
        DisasterReport report = TestDataFactory.createReport(1L, citizen, ReportStatus.SUBMITTED);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        UpdateReportStatusRequest request = new UpdateReportStatusRequest();
        request.setStatus(ReportStatus.APPROVED);

        assertThatThrownBy(() -> reportService.updateReportStatus(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid status transition");

        verify(reportRepository, never()).save(any(DisasterReport.class));
    }

    @Test
    @DisplayName("shouldThrowWhenReportNotFoundOnUpdateStatus")
    void shouldThrowWhenReportNotFoundOnUpdateStatus() {
        when(reportRepository.findById(99L)).thenReturn(Optional.empty());

        UpdateReportStatusRequest request = new UpdateReportStatusRequest();
        request.setStatus(ReportStatus.ASSIGNED);

        assertThatThrownBy(() -> reportService.updateReportStatus(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("shouldDeleteOwnedSubmittedReport")
    void shouldDeleteOwnedSubmittedReport() {
        User citizen = citizen();
        DisasterReport report = TestDataFactory.createReport(1L, citizen, ReportStatus.SUBMITTED);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        reportService.deleteReport(1L, citizen.getEmail());

        verify(reportImageRepository).deleteByDisasterReportId(1L);
        verify(reportRepository).delete(report);
    }

    @Test
    @DisplayName("shouldThrowWhenDeletingReportNotOwned")
    void shouldThrowWhenDeletingReportNotOwned() {
        User citizen = citizen();
        User otherCitizen = TestDataFactory.createCitizen(2L, "other@example.com");
        DisasterReport report = TestDataFactory.createReport(1L, citizen, ReportStatus.SUBMITTED);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> reportService.deleteReport(1L, otherCitizen.getEmail()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("only delete your own reports");

        verify(reportRepository, never()).delete(any(DisasterReport.class));
    }

    @Test
    @DisplayName("shouldThrowWhenDeletingReportNotSubmittable")
    void shouldThrowWhenDeletingReportNotSubmittable() {
        User citizen = citizen();
        DisasterReport report = TestDataFactory.createReport(1L, citizen, ReportStatus.APPROVED);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> reportService.deleteReport(1L, citizen.getEmail()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only SUBMITTED reports can be deleted");

        verify(reportRepository, never()).delete(any(DisasterReport.class));
    }

    @Test
    @DisplayName("shouldThrowWhenReportNotFoundOnDelete")
    void shouldThrowWhenReportNotFoundOnDelete() {
        when(reportRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.deleteReport(99L, "citizen@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("shouldAddImagesToOwnedSubmittedReport")
    void shouldAddImagesToOwnedSubmittedReport() {
        User citizen = citizen();
        DisasterReport report = TestDataFactory.createReport(1L, citizen, ReportStatus.SUBMITTED);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(reportImageRepository.countByDisasterReportId(1L)).thenReturn(2L);

        AddReportImagesRequest request = new AddReportImagesRequest();
        request.setImageUrls(List.of("http://img.com/new1.jpg", "http://img.com/new2.jpg"));
        stubNoImages();

        DisasterReportResponse response = reportService.addImages(1L, citizen.getEmail(), request);

        assertThat(response.getId()).isEqualTo(1L);
        verify(reportImageRepository, times(2)).save(any(ReportImage.class));

        ArgumentCaptor<ReportImage> captor = ArgumentCaptor.forClass(ReportImage.class);
        verify(reportImageRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getImageUrl())
                .isEqualTo("http://img.com/new1.jpg");
        assertThat(captor.getAllValues().get(0).getDisasterReport()).isEqualTo(report);
    }

    @Test
    @DisplayName("shouldThrowWhenAddingImagesToNonOwnedReport")
    void shouldThrowWhenAddingImagesToNonOwnedReport() {
        User citizen = citizen();
        User otherCitizen = TestDataFactory.createCitizen(2L, "other@example.com");
        DisasterReport report = TestDataFactory.createReport(1L, citizen, ReportStatus.SUBMITTED);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        AddReportImagesRequest request = new AddReportImagesRequest();
        request.setImageUrls(List.of("http://img.com/a.jpg"));

        assertThatThrownBy(() -> reportService.addImages(1L, otherCitizen.getEmail(), request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("only modify your own reports");

        verify(reportImageRepository, never()).save(any(ReportImage.class));
    }

    @Test
    @DisplayName("shouldThrowWhenAddingImagesToNonSubmittedReport")
    void shouldThrowWhenAddingImagesToNonSubmittedReport() {
        User citizen = citizen();
        DisasterReport report = TestDataFactory.createReport(1L, citizen, ReportStatus.ASSIGNED);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        AddReportImagesRequest request = new AddReportImagesRequest();
        request.setImageUrls(List.of("http://img.com/a.jpg"));

        assertThatThrownBy(() -> reportService.addImages(1L, citizen.getEmail(), request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot add images to report with status");

        verify(reportImageRepository, never()).save(any(ReportImage.class));
    }

    @Test
    @DisplayName("shouldThrowWhenAddingTooManyImages")
    void shouldThrowWhenAddingTooManyImages() {
        User citizen = citizen();
        DisasterReport report = TestDataFactory.createReport(1L, citizen, ReportStatus.SUBMITTED);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(reportImageRepository.countByDisasterReportId(1L)).thenReturn(9L);

        AddReportImagesRequest request = new AddReportImagesRequest();
        request.setImageUrls(List.of("http://img.com/a.jpg", "http://img.com/b.jpg"));

        assertThatThrownBy(() -> reportService.addImages(1L, citizen.getEmail(), request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Maximum allowed is 10");

        verify(reportImageRepository, never()).save(any(ReportImage.class));
    }

    @Test
    @DisplayName("shouldThrowWhenReportNotFoundOnAddImages")
    void shouldThrowWhenReportNotFoundOnAddImages() {
        when(reportRepository.findById(99L)).thenReturn(Optional.empty());

        AddReportImagesRequest request = new AddReportImagesRequest();
        request.setImageUrls(List.of("http://img.com/a.jpg"));

        assertThatThrownBy(() -> reportService.addImages(99L, "citizen@example.com", request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("shouldRemoveImageFromOwnedSubmittedReport")
    void shouldRemoveImageFromOwnedSubmittedReport() {
        User citizen = citizen();
        DisasterReport report = TestDataFactory.createReport(1L, citizen, ReportStatus.SUBMITTED);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        ReportImage image = new ReportImage();
        image.setId(55L);
        image.setImageUrl("http://img.com/a.jpg");
        image.setDisasterReport(report);
        when(reportImageRepository.findById(55L)).thenReturn(Optional.of(image));

        reportService.removeImage(1L, 55L, citizen.getEmail());

        verify(reportImageRepository).delete(image);
    }

    @Test
    @DisplayName("shouldThrowWhenRemovingImageFromNonOwnedReport")
    void shouldThrowWhenRemovingImageFromNonOwnedReport() {
        User citizen = citizen();
        User otherCitizen = TestDataFactory.createCitizen(2L, "other@example.com");
        DisasterReport report = TestDataFactory.createReport(1L, citizen, ReportStatus.SUBMITTED);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> reportService.removeImage(1L, 55L, otherCitizen.getEmail()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("only modify your own reports");

        verify(reportImageRepository, never()).delete(any(ReportImage.class));
    }

    @Test
    @DisplayName("shouldThrowWhenRemovingImageFromNonSubmittedReport")
    void shouldThrowWhenRemovingImageFromNonSubmittedReport() {
        User citizen = citizen();
        DisasterReport report = TestDataFactory.createReport(1L, citizen, ReportStatus.COMPLETED);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> reportService.removeImage(1L, 55L, citizen.getEmail()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot remove images from report with status");
    }

    @Test
    @DisplayName("shouldThrowWhenImageNotFoundOnRemove")
    void shouldThrowWhenImageNotFoundOnRemove() {
        User citizen = citizen();
        DisasterReport report = TestDataFactory.createReport(1L, citizen, ReportStatus.SUBMITTED);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(reportImageRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.removeImage(1L, 99L, citizen.getEmail()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("ReportImage");
    }

    @Test
    @DisplayName("shouldThrowWhenImageBelongsToDifferentReport")
    void shouldThrowWhenImageBelongsToDifferentReport() {
        User citizen = citizen();
        DisasterReport report = TestDataFactory.createReport(1L, citizen, ReportStatus.SUBMITTED);
        DisasterReport otherReport = TestDataFactory.createReport(2L, citizen, ReportStatus.SUBMITTED);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        ReportImage image = new ReportImage();
        image.setId(55L);
        image.setImageUrl("http://img.com/a.jpg");
        image.setDisasterReport(otherReport);
        when(reportImageRepository.findById(55L)).thenReturn(Optional.of(image));

        assertThatThrownBy(() -> reportService.removeImage(1L, 55L, citizen.getEmail()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not belong to this report");

        verify(reportImageRepository, never()).delete(any(ReportImage.class));
    }

    @Test
    @DisplayName("shouldThrowWhenReportNotFoundOnRemoveImage")
    void shouldThrowWhenReportNotFoundOnRemoveImage() {
        when(reportRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.removeImage(99L, 55L, "citizen@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
