package com.himanshuProjects.disaster_damage_assessment_portal.service.impl.feedback;

import com.himanshuProjects.disaster_damage_assessment_portal.dto.feedback.CreateFeedbackRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.feedback.FeedbackPageResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.feedback.FeedbackResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.disaster.DisasterReport;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.system.Feedback;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.User;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.ReportStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.RoleType;
import com.himanshuProjects.disaster_damage_assessment_portal.exception.BadRequestException;
import com.himanshuProjects.disaster_damage_assessment_portal.exception.ConflictException;
import com.himanshuProjects.disaster_damage_assessment_portal.exception.ResourceNotFoundException;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.disaster.DisasterReportRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.system.FeedbackRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.user.UserRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.testutil.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceImplTest {

    @Mock
    private FeedbackRepository feedbackRepository;
    @Mock
    private DisasterReportRepository reportRepository;
    @Mock
    private UserRepository userRepository;

    private FeedbackServiceImpl feedbackService;

    @BeforeEach
    void setUp() {
        feedbackService = new FeedbackServiceImpl(feedbackRepository, reportRepository, userRepository);
    }

    private User citizen(long id) {
        return TestDataFactory.createCitizen(id, "citizen" + id + "@example.com");
    }

    private CreateFeedbackRequest request(Long reportId, Integer rating, String comments) {
        CreateFeedbackRequest request = new CreateFeedbackRequest();
        request.setDisasterReportId(reportId);
        request.setRating(rating);
        request.setComments(comments);
        return request;
    }

    @Test
    @DisplayName("shouldSubmitFeedbackSuccessfully")
    void shouldSubmitFeedbackSuccessfully() {
        User citizen = citizen(1L);
        DisasterReport report = TestDataFactory.createReport(10L, citizen, ReportStatus.COMPLETED);
        CreateFeedbackRequest request = request(10L, 5, "Excellent service");
        Feedback saved = TestDataFactory.createFeedback(1L, citizen, report, 5);

        when(userRepository.findByEmail(citizen.getEmail())).thenReturn(Optional.of(citizen));
        when(reportRepository.findById(10L)).thenReturn(Optional.of(report));
        when(feedbackRepository.existsByDisasterReportId(10L)).thenReturn(false);
        when(feedbackRepository.save(any(Feedback.class))).thenReturn(saved);

        FeedbackResponse response = feedbackService.submitFeedback(citizen.getEmail(), request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getRating()).isEqualTo(5);
        assertThat(response.getComments()).isEqualTo("Good service");
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getReportId()).isEqualTo(10L);

        verify(feedbackRepository).save(any(Feedback.class));
    }

    @Test
    @DisplayName("shouldRejectFeedbackForNonCitizen")
    void shouldRejectFeedbackForNonCitizen() {
        User officer = TestDataFactory.createOfficer(5L, "officer@example.com");
        when(userRepository.findByEmail(officer.getEmail())).thenReturn(Optional.of(officer));

        assertThatThrownBy(() -> feedbackService.submitFeedback(officer.getEmail(), request(10L, 5, "ok")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only CITIZEN");

        verify(feedbackRepository, never()).save(any(Feedback.class));
    }

    @Test
    @DisplayName("shouldThrowWhenUserNotFoundOnSubmit")
    void shouldThrowWhenUserNotFoundOnSubmit() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> feedbackService.submitFeedback("missing@example.com", request(10L, 5, "ok")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");
    }

    @Test
    @DisplayName("shouldThrowWhenReportNotFoundOnSubmit")
    void shouldThrowWhenReportNotFoundOnSubmit() {
        User citizen = citizen(1L);
        when(userRepository.findByEmail(citizen.getEmail())).thenReturn(Optional.of(citizen));
        when(reportRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> feedbackService.submitFeedback(citizen.getEmail(), request(99L, 5, "ok")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("DisasterReport");
    }

    @Test
    @DisplayName("shouldRejectFeedbackForOthersReport")
    void shouldRejectFeedbackForOthersReport() {
        User citizen = citizen(1L);
        User other = citizen(2L);
        DisasterReport report = TestDataFactory.createReport(10L, other, ReportStatus.COMPLETED);

        when(userRepository.findByEmail(citizen.getEmail())).thenReturn(Optional.of(citizen));
        when(reportRepository.findById(10L)).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> feedbackService.submitFeedback(citizen.getEmail(), request(10L, 5, "ok")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("your own reports");

        verify(feedbackRepository, never()).save(any(Feedback.class));
    }

    @Test
    @DisplayName("shouldRejectFeedbackForReportInNonAllowedStatus")
    void shouldRejectFeedbackForReportInNonAllowedStatus() {
        User citizen = citizen(1L);
        DisasterReport report = TestDataFactory.createReport(10L, citizen, ReportStatus.SUBMITTED);

        when(userRepository.findByEmail(citizen.getEmail())).thenReturn(Optional.of(citizen));
        when(reportRepository.findById(10L)).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> feedbackService.submitFeedback(citizen.getEmail(), request(10L, 5, "ok")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("COMPLETED or APPROVED");

        verify(feedbackRepository, never()).save(any(Feedback.class));
    }

    @Test
    @DisplayName("shouldRejectDuplicateFeedbackForReport")
    void shouldRejectDuplicateFeedbackForReport() {
        User citizen = citizen(1L);
        DisasterReport report = TestDataFactory.createReport(10L, citizen, ReportStatus.APPROVED);

        when(userRepository.findByEmail(citizen.getEmail())).thenReturn(Optional.of(citizen));
        when(reportRepository.findById(10L)).thenReturn(Optional.of(report));
        when(feedbackRepository.existsByDisasterReportId(10L)).thenReturn(true);

        assertThatThrownBy(() -> feedbackService.submitFeedback(citizen.getEmail(), request(10L, 5, "ok")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");

        verify(feedbackRepository, never()).save(any(Feedback.class));
    }

    @Test
    @DisplayName("shouldGetFeedbackById")
    void shouldGetFeedbackById() {
        User citizen = citizen(1L);
        DisasterReport report = TestDataFactory.createReport(10L, citizen, ReportStatus.COMPLETED);
        Feedback feedback = TestDataFactory.createFeedback(1L, citizen, report, 4);

        when(feedbackRepository.findById(1L)).thenReturn(Optional.of(feedback));

        FeedbackResponse response = feedbackService.getFeedbackById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getRating()).isEqualTo(4);
        assertThat(response.getUserName()).isEqualTo(citizen.getFullName());
        assertThat(response.getReportTitle()).isEqualTo(report.getTitle());
    }

    @Test
    @DisplayName("shouldThrowWhenFeedbackNotFoundById")
    void shouldThrowWhenFeedbackNotFoundById() {
        when(feedbackRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> feedbackService.getFeedbackById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Feedback");
    }

    @Test
    @DisplayName("shouldGetFeedbackByReportIdFromFindById")
    void shouldGetFeedbackByReportIdFromFindById() {
        User citizen = citizen(1L);
        DisasterReport report = TestDataFactory.createReport(10L, citizen, ReportStatus.COMPLETED);
        Feedback feedback = TestDataFactory.createFeedback(1L, citizen, report, 3);

        when(feedbackRepository.findById(10L)).thenReturn(Optional.of(feedback));

        FeedbackResponse response = feedbackService.getFeedbackByReportId(10L);

        assertThat(response.getReportId()).isEqualTo(10L);
        assertThat(response.getRating()).isEqualTo(3);
    }

    @Test
    @DisplayName("shouldThrowWhenFeedbackNotFoundByReportId")
    void shouldThrowWhenFeedbackNotFoundByReportId() {
        when(feedbackRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> feedbackService.getFeedbackByReportId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Feedback");
    }

    @Test
    @DisplayName("shouldSearchFeedbacksWithAllFilters")
    void shouldSearchFeedbacksWithAllFilters() {
        User citizen = citizen(1L);
        DisasterReport report = TestDataFactory.createReport(10L, citizen, ReportStatus.COMPLETED);
        Feedback f1 = TestDataFactory.createFeedback(1L, citizen, report, 5);
        Page<Feedback> page = new PageImpl<>(List.of(f1), PageRequest.of(0, 10), 1);

        when(feedbackRepository.searchFeedbacks(any(), any(), any(Pageable.class))).thenReturn(page);

        FeedbackPageResponse response =
                feedbackService.searchFeedbacks("good", 5, 0, 10, "submittedAt", "desc");

        assertThat(response.getFeedbacks()).hasSize(1);
        assertThat(response.getFeedbacks().get(0).getRating()).isEqualTo(5);
        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("shouldRejectInvalidSortFieldOnSearch")
    void shouldRejectInvalidSortFieldOnSearch() {
        assertThatThrownBy(() ->
                feedbackService.searchFeedbacks(null, null, 0, 10, "invalidField", "asc"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid sort field");

        assertThatThrownBy(() ->
                feedbackService.searchFeedbacks(null, null, 0, 10, "evilField", "asc"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("shouldGetMyFeedbacksFilteredByCitizen")
    void shouldGetMyFeedbacksFilteredByCitizen() {
        User citizen = citizen(1L);
        User other = citizen(2L);
        DisasterReport report1 = TestDataFactory.createReport(10L, citizen, ReportStatus.COMPLETED);
        DisasterReport report2 = TestDataFactory.createReport(20L, other, ReportStatus.COMPLETED);
        Feedback mine = TestDataFactory.createFeedback(1L, citizen, report1, 5);
        Feedback theirs = TestDataFactory.createFeedback(2L, other, report2, 4);
        Page<Feedback> page = new PageImpl<>(List.of(mine, theirs), PageRequest.of(0, 10), 2);

        when(userRepository.findByEmail(citizen.getEmail())).thenReturn(Optional.of(citizen));
        when(feedbackRepository.searchFeedbacks(any(), any(), any(Pageable.class))).thenReturn(page);

        FeedbackPageResponse response =
                feedbackService.getMyFeedbacks(citizen.getEmail(), 0, 10, "submittedAt", "asc");

        assertThat(response.getFeedbacks()).hasSize(1);
        assertThat(response.getFeedbacks().get(0).getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("shouldRejectInvalidSortFieldOnGetMyFeedbacks")
    void shouldRejectInvalidSortFieldOnGetMyFeedbacks() {
        User citizen = citizen(1L);
        when(userRepository.findByEmail(citizen.getEmail())).thenReturn(Optional.of(citizen));

        assertThatThrownBy(() ->
                feedbackService.getMyFeedbacks(citizen.getEmail(), 0, 10, "bogus", "asc"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid sort field");
    }

    @Test
    @DisplayName("shouldThrowWhenUserNotFoundOnGetMyFeedbacks")
    void shouldThrowWhenUserNotFoundOnGetMyFeedbacks() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                feedbackService.getMyFeedbacks("missing@example.com", 0, 10, "id", "asc"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("shouldDeleteOwnFeedback")
    void shouldDeleteOwnFeedback() {
        User citizen = citizen(1L);
        DisasterReport report = TestDataFactory.createReport(10L, citizen, ReportStatus.COMPLETED);
        Feedback feedback = TestDataFactory.createFeedback(1L, citizen, report, 5);

        when(userRepository.findByEmail(citizen.getEmail())).thenReturn(Optional.of(citizen));
        when(feedbackRepository.findById(1L)).thenReturn(Optional.of(feedback));

        feedbackService.deleteFeedback(1L, citizen.getEmail());

        verify(feedbackRepository).delete(feedback);
    }

    @Test
    @DisplayName("shouldThrowWhenUserNotFoundOnDelete")
    void shouldThrowWhenUserNotFoundOnDelete() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> feedbackService.deleteFeedback(1L, "missing@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("shouldThrowWhenFeedbackNotFoundOnDelete")
    void shouldThrowWhenFeedbackNotFoundOnDelete() {
        User citizen = citizen(1L);
        when(userRepository.findByEmail(citizen.getEmail())).thenReturn(Optional.of(citizen));
        when(feedbackRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> feedbackService.deleteFeedback(99L, citizen.getEmail()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Feedback");
    }

    @Test
    @DisplayName("shouldRejectDeletingOthersFeedback")
    void shouldRejectDeletingOthersFeedback() {
        User owner = citizen(1L);
        User other = citizen(2L);
        DisasterReport report = TestDataFactory.createReport(10L, owner, ReportStatus.COMPLETED);
        Feedback feedback = TestDataFactory.createFeedback(1L, owner, report, 5);

        when(userRepository.findByEmail(other.getEmail())).thenReturn(Optional.of(other));
        when(feedbackRepository.findById(1L)).thenReturn(Optional.of(feedback));

        assertThatThrownBy(() -> feedbackService.deleteFeedback(1L, other.getEmail()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("delete your own feedback");

        verify(feedbackRepository, never()).delete(any(Feedback.class));
    }
}
