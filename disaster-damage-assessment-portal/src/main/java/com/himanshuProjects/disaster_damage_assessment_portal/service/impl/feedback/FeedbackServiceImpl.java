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
import com.himanshuProjects.disaster_damage_assessment_portal.service.feedback.FeedbackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class FeedbackServiceImpl implements FeedbackService {

    private static final Logger log = LoggerFactory.getLogger(FeedbackServiceImpl.class);

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "rating", "submittedAt", "createdAt", "updatedAt"
    );

    private static final Set<ReportStatus> FEEDBACK_ALLOWED_STATUSES = Set.of(
            ReportStatus.COMPLETED,
            ReportStatus.APPROVED
    );

    private final FeedbackRepository feedbackRepository;
    private final DisasterReportRepository reportRepository;
    private final UserRepository userRepository;

    public FeedbackServiceImpl(FeedbackRepository feedbackRepository,
                                DisasterReportRepository reportRepository,
                                UserRepository userRepository) {
        this.feedbackRepository = feedbackRepository;
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public FeedbackResponse submitFeedback(String citizenEmail, CreateFeedbackRequest request) {
        log.info("Submitting feedback for report ID: {} by citizen: {}", request.getDisasterReportId(), citizenEmail);

        User citizen = userRepository.findByEmail(citizenEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", citizenEmail));

        if (citizen.getRole() != RoleType.CITIZEN) {
            throw new BadRequestException("Only CITIZEN can submit feedback");
        }

        DisasterReport report = reportRepository.findById(request.getDisasterReportId())
                .orElseThrow(() -> new ResourceNotFoundException("DisasterReport", "id", request.getDisasterReportId()));

        if (!report.getCitizen().getId().equals(citizen.getId())) {
            throw new BadRequestException("You can only submit feedback for your own reports");
        }

        if (!FEEDBACK_ALLOWED_STATUSES.contains(report.getStatus())) {
            throw new BadRequestException(
                    "Cannot submit feedback for report with status: " + report.getStatus()
                            + ". Report must be COMPLETED or APPROVED.");
        }

        if (feedbackRepository.existsByDisasterReportId(request.getDisasterReportId())) {
            throw new ConflictException(
                    "Feedback already exists for report ID: " + request.getDisasterReportId());
        }

        Feedback feedback = new Feedback();
        feedback.setRating(request.getRating());
        feedback.setComments(request.getComments());
        feedback.setUser(citizen);
        feedback.setDisasterReport(report);

        Feedback saved = feedbackRepository.save(feedback);

        log.info("Feedback submitted: {} for report ID: {}", saved.getId(), request.getDisasterReportId());
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public FeedbackResponse getFeedbackById(Long id) {
        log.info("Fetching feedback ID: {}", id);
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback", "id", id));
        return mapToResponse(feedback);
    }

    @Override
    @Transactional(readOnly = true)
    public FeedbackResponse getFeedbackByReportId(Long reportId) {
        log.info("Fetching feedback for report ID: {}", reportId);

        Feedback feedback = feedbackRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback", "reportId", reportId));
        return mapToResponse(feedback);
    }

    @Override
    @Transactional(readOnly = true)
    public FeedbackPageResponse searchFeedbacks(String search, Integer rating,
                                                 int page, int size,
                                                 String sortBy, String sortDirection) {
        log.info("Searching feedbacks - search: {}, rating: {}", search, rating);

        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new BadRequestException("Invalid sort field: " + sortBy
                    + ". Allowed fields: " + ALLOWED_SORT_FIELDS);
        }

        Sort sort = sortDirection.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Feedback> pageResult = feedbackRepository.searchFeedbacks(search, rating, pageable);

        List<FeedbackResponse> feedbacks = pageResult.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return FeedbackPageResponse.builder()
                .feedbacks(feedbacks)
                .pageNumber(pageResult.getNumber())
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public FeedbackPageResponse getMyFeedbacks(String citizenEmail, int page, int size,
                                                String sortBy, String sortDirection) {
        log.info("Fetching feedbacks for citizen: {}", citizenEmail);

        User citizen = userRepository.findByEmail(citizenEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", citizenEmail));

        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new BadRequestException("Invalid sort field: " + sortBy
                    + ". Allowed fields: " + ALLOWED_SORT_FIELDS);
        }

        Sort sort = sortDirection.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Feedback> pageResult = feedbackRepository.searchFeedbacks(null, null, pageable);

        List<FeedbackResponse> feedbacks = pageResult.getContent().stream()
                .filter(f -> f.getUser().getId().equals(citizen.getId()))
                .map(this::mapToResponse)
                .toList();

        return FeedbackPageResponse.builder()
                .feedbacks(feedbacks)
                .pageNumber(pageResult.getNumber())
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .build();
    }

    @Override
    @Transactional
    public void deleteFeedback(Long id, String citizenEmail) {
        log.info("Deleting feedback ID: {} by citizen: {}", id, citizenEmail);

        User citizen = userRepository.findByEmail(citizenEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", citizenEmail));

        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback", "id", id));

        if (!feedback.getUser().getId().equals(citizen.getId())) {
            throw new BadRequestException("You can only delete your own feedback");
        }

        feedbackRepository.delete(feedback);
        log.info("Feedback deleted: {}", id);
    }

    private FeedbackResponse mapToResponse(Feedback feedback) {
        return FeedbackResponse.builder()
                .id(feedback.getId())
                .rating(feedback.getRating())
                .comments(feedback.getComments())
                .submittedAt(feedback.getSubmittedAt())
                .userId(feedback.getUser().getId())
                .userName(feedback.getUser().getFullName())
                .userEmail(feedback.getUser().getEmail())
                .reportId(feedback.getDisasterReport().getId())
                .reportTitle(feedback.getDisasterReport().getTitle())
                .createdAt(feedback.getCreatedAt())
                .updatedAt(feedback.getUpdatedAt())
                .build();
    }
}
