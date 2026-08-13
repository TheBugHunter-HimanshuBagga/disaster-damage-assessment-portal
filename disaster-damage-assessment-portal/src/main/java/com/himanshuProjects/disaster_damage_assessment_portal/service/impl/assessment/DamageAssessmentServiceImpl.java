package com.himanshuProjects.disaster_damage_assessment_portal.service.impl.assessment;

import com.himanshuProjects.disaster_damage_assessment_portal.dto.assessment.AddInspectionImagesRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.assessment.CreateDamageAssessmentRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.assessment.DamageAssessmentPageResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.assessment.DamageAssessmentResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.assessment.InspectionImageResponse;
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
import com.himanshuProjects.disaster_damage_assessment_portal.enums.NotificationType;
import com.himanshuProjects.disaster_damage_assessment_portal.event.NotificationEvent;
import com.himanshuProjects.disaster_damage_assessment_portal.exception.BadRequestException;
import com.himanshuProjects.disaster_damage_assessment_portal.exception.ConflictException;
import com.himanshuProjects.disaster_damage_assessment_portal.exception.ResourceNotFoundException;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.disaster.DamageAssessmentRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.disaster.DisasterReportRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.disaster.InspectionImageRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.disaster.OfficerAssignmentRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.user.UserRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.service.assessment.DamageAssessmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class DamageAssessmentServiceImpl implements DamageAssessmentService {

    private static final Logger log = LoggerFactory.getLogger(DamageAssessmentServiceImpl.class);

    private static final int MAX_INSPECTION_IMAGES = 10;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "damageLevel", "estimatedLoss", "assessedAt", "createdAt", "updatedAt"
    );

    private static final List<AssignmentStatus> OFFICER_ACTIVE_STATUSES = List.of(
            AssignmentStatus.ACCEPTED,
            AssignmentStatus.IN_PROGRESS,
            AssignmentStatus.COMPLETED
    );

    private final DamageAssessmentRepository assessmentRepository;
    private final InspectionImageRepository inspectionImageRepository;
    private final DisasterReportRepository reportRepository;
    private final OfficerAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public DamageAssessmentServiceImpl(DamageAssessmentRepository assessmentRepository,
                                       InspectionImageRepository inspectionImageRepository,
                                       DisasterReportRepository reportRepository,
                                       OfficerAssignmentRepository assignmentRepository,
                                       UserRepository userRepository,
                                       ApplicationEventPublisher eventPublisher) {
        this.assessmentRepository = assessmentRepository;
        this.inspectionImageRepository = inspectionImageRepository;
        this.reportRepository = reportRepository;
        this.assignmentRepository = assignmentRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public DamageAssessmentResponse submitAssessment(Long reportId, String officerEmail,
                                                      CreateDamageAssessmentRequest request) {
        log.info("Submitting assessment for report ID: {} by officer: {}", reportId, officerEmail);

        User officer = userRepository.findByEmail(officerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", officerEmail));

        if (officer.getRole() != RoleType.FIELD_OFFICER) {
            throw new BadRequestException("Only FIELD_OFFICER can submit assessments");
        }

        DisasterReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("DisasterReport", "id", reportId));

        if (report.getStatus() != ReportStatus.UNDER_INSPECTION
                && report.getStatus() != ReportStatus.REINSPECTION_REQUIRED) {
            throw new BadRequestException(
                    "Cannot assess report with status: " + report.getStatus()
                            + ". Report must be UNDER_INSPECTION or REINSPECTION_REQUIRED.");
        }

        if (!assignmentRepository.existsByDisasterReportIdAndAssignmentStatusIn(
                reportId, List.of(AssignmentStatus.ACCEPTED, AssignmentStatus.IN_PROGRESS))) {
            throw new BadRequestException(
                    "No active assignment found for report ID: " + reportId);
        }

        OfficerAssignment assignment = assignmentRepository
                .findByDisasterReportIdAndAssignmentStatusIn(reportId, OFFICER_ACTIVE_STATUSES)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "OfficerAssignment", "reportId", reportId));

        if (!assignment.getFieldOfficer().getId().equals(officer.getId())) {
            throw new BadRequestException(
                    "You are not assigned to report ID: " + reportId);
        }

        if (assessmentRepository.existsByDisasterReportId(reportId)) {
            throw new ConflictException(
                    "Assessment already exists for report ID: " + reportId);
        }

        DamageAssessment assessment = new DamageAssessment();
        assessment.setDamageLevel(request.getDamageLevel());
        assessment.setEstimatedLoss(request.getEstimatedLoss());
        assessment.setAssessmentNotes(request.getAssessmentNotes());
        assessment.setRecommendation(request.getRecommendation());
        assessment.setDisasterReport(report);
        assessment.setFieldOfficer(officer);

        DamageAssessment saved = assessmentRepository.save(assessment);

        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            if (request.getImageUrls().size() > MAX_INSPECTION_IMAGES) {
                throw new BadRequestException(
                        "Cannot upload more than " + MAX_INSPECTION_IMAGES + " images");
            }
            for (String imageUrl : request.getImageUrls()) {
                InspectionImage image = new InspectionImage();
                image.setImageUrl(imageUrl);
                image.setDamageAssessment(saved);
                inspectionImageRepository.save(image);
            }
        }

        report.setStatus(ReportStatus.UNDER_REVIEW);
        reportRepository.save(report);

        eventPublisher.publishEvent(new NotificationEvent(
                this, report.getCitizen().getId(), NotificationType.DAMAGE_ASSESSED,
                "Assessment Completed",
                "Damage assessment for your report \"" + report.getTitle() + "\" has been completed and is under review.",
                saved.getId(), "DAMAGE_ASSESSMENT"));

        log.info("Assessment submitted: {} for report ID: {}", saved.getId(), reportId);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public DamageAssessmentResponse getAssessmentById(Long id) {
        log.info("Fetching assessment ID: {}", id);
        DamageAssessment assessment = assessmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DamageAssessment", "id", id));
        return mapToResponse(assessment);
    }

    @Override
    @Transactional(readOnly = true)
    public DamageAssessmentResponse getAssessmentByReportId(Long reportId) {
        log.info("Fetching assessment for report ID: {}", reportId);
        DamageAssessment assessment = assessmentRepository.findByDisasterReportId(reportId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "DamageAssessment", "reportId", reportId));
        return mapToResponse(assessment);
    }

    @Override
    @Transactional
    public DamageAssessmentResponse updateAssessment(Long id, String officerEmail,
                                                      UpdateDamageAssessmentRequest request) {
        log.info("Updating assessment ID: {} by officer: {}", id, officerEmail);

        DamageAssessment assessment = assessmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DamageAssessment", "id", id));

        User officer = userRepository.findByEmail(officerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", officerEmail));

        if (!assessment.getFieldOfficer().getId().equals(officer.getId())) {
            throw new BadRequestException("You can only update your own assessments");
        }

        DisasterReport report = assessment.getDisasterReport();
        if (report.getStatus() != ReportStatus.UNDER_REVIEW) {
            throw new BadRequestException(
                    "Cannot update assessment for report with status: " + report.getStatus());
        }

        assessment.setDamageLevel(request.getDamageLevel());
        assessment.setEstimatedLoss(request.getEstimatedLoss());
        assessment.setAssessmentNotes(request.getAssessmentNotes());
        assessment.setRecommendation(request.getRecommendation());

        DamageAssessment updated = assessmentRepository.save(assessment);
        log.info("Assessment updated: {}", id);
        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public DamageAssessmentPageResponse searchAssessments(String search, DamageLevel damageLevel,
                                                          Long officerId, int page, int size,
                                                          String sortBy, String sortDirection) {
        log.info("Searching assessments - search: {}, level: {}, officerId: {}",
                search, damageLevel, officerId);

        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new BadRequestException("Invalid sort field: " + sortBy
                    + ". Allowed fields: " + ALLOWED_SORT_FIELDS);
        }

        Sort sort = sortDirection.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<DamageAssessment> pageResult = assessmentRepository.searchAssessments(
                search, damageLevel, officerId, pageable);

        List<DamageAssessmentResponse> assessments = pageResult.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return DamageAssessmentPageResponse.builder()
                .assessments(assessments)
                .pageNumber(pageResult.getNumber())
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public DamageAssessmentPageResponse getMyAssessments(String officerEmail, int page, int size,
                                                          String sortBy, String sortDirection) {
        log.info("Fetching assessments for officer: {}", officerEmail);

        User officer = userRepository.findByEmail(officerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", officerEmail));

        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new BadRequestException("Invalid sort field: " + sortBy
                    + ". Allowed fields: " + ALLOWED_SORT_FIELDS);
        }

        Sort sort = sortDirection.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<DamageAssessment> pageResult = assessmentRepository.searchAssessments(
                null, null, officer.getId(), pageable);

        List<DamageAssessmentResponse> assessments = pageResult.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return DamageAssessmentPageResponse.builder()
                .assessments(assessments)
                .pageNumber(pageResult.getNumber())
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .build();
    }

    @Override
    @Transactional
    public DamageAssessmentResponse addImages(Long assessmentId, String officerEmail,
                                               AddInspectionImagesRequest request) {
        log.info("Adding images to assessment ID: {} by officer: {}", assessmentId, officerEmail);

        DamageAssessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new ResourceNotFoundException("DamageAssessment", "id", assessmentId));

        User officer = userRepository.findByEmail(officerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", officerEmail));

        if (!assessment.getFieldOfficer().getId().equals(officer.getId())) {
            throw new BadRequestException("You can only modify your own assessments");
        }

        long currentCount = inspectionImageRepository.countByDamageAssessmentId(assessmentId);
        if (currentCount + request.getImageUrls().size() > MAX_INSPECTION_IMAGES) {
            throw new BadRequestException(
                    "Cannot add " + request.getImageUrls().size() + " images. "
                            + "Assessment already has " + currentCount + " images. "
                            + "Maximum allowed is " + MAX_INSPECTION_IMAGES);
        }

        for (String imageUrl : request.getImageUrls()) {
            InspectionImage image = new InspectionImage();
            image.setImageUrl(imageUrl);
            image.setDamageAssessment(assessment);
            inspectionImageRepository.save(image);
        }

        log.info("Added {} images to assessment ID: {}", request.getImageUrls().size(), assessmentId);
        return mapToResponse(assessment);
    }

    @Override
    @Transactional
    public void removeImage(Long assessmentId, Long imageId, String officerEmail) {
        log.info("Removing image ID: {} from assessment ID: {}", imageId, assessmentId);

        DamageAssessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new ResourceNotFoundException("DamageAssessment", "id", assessmentId));

        User officer = userRepository.findByEmail(officerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", officerEmail));

        if (!assessment.getFieldOfficer().getId().equals(officer.getId())) {
            throw new BadRequestException("You can only modify your own assessments");
        }

        InspectionImage image = inspectionImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("InspectionImage", "id", imageId));

        if (!image.getDamageAssessment().getId().equals(assessmentId)) {
            throw new BadRequestException("Image does not belong to this assessment");
        }

        inspectionImageRepository.delete(image);
        log.info("Image ID: {} removed from assessment ID: {}", imageId, assessmentId);
    }

    @Override
    @Transactional
    public void deleteAssessment(Long id, String officerEmail) {
        log.info("Deleting assessment ID: {} by officer: {}", id, officerEmail);

        DamageAssessment assessment = assessmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DamageAssessment", "id", id));

        User officer = userRepository.findByEmail(officerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", officerEmail));

        if (!assessment.getFieldOfficer().getId().equals(officer.getId())) {
            throw new BadRequestException("You can only delete your own assessments");
        }

        DisasterReport report = assessment.getDisasterReport();
        if (report.getStatus() != ReportStatus.UNDER_REVIEW) {
            throw new BadRequestException(
                    "Cannot delete assessment for report with status: " + report.getStatus());
        }

        inspectionImageRepository.deleteByDamageAssessmentId(id);
        assessmentRepository.delete(assessment);

        report.setStatus(ReportStatus.UNDER_INSPECTION);
        reportRepository.save(report);

        log.info("Assessment deleted: {} for report ID: {}", id, report.getId());
    }

    private DamageAssessmentResponse mapToResponse(DamageAssessment assessment) {
        DisasterReport report = assessment.getDisasterReport();
        User officer = assessment.getFieldOfficer();

        List<InspectionImage> images = inspectionImageRepository
                .findByDamageAssessmentIdOrderByUploadedAtAsc(assessment.getId());

        List<InspectionImageResponse> imageResponses = images.stream()
                .map(img -> InspectionImageResponse.builder()
                        .id(img.getId())
                        .imageUrl(img.getImageUrl())
                        .uploadedAt(img.getUploadedAt())
                        .build())
                .toList();

        return DamageAssessmentResponse.builder()
                .id(assessment.getId())
                .damageLevel(assessment.getDamageLevel())
                .estimatedLoss(assessment.getEstimatedLoss())
                .assessmentNotes(assessment.getAssessmentNotes())
                .recommendation(assessment.getRecommendation())
                .assessedAt(assessment.getAssessedAt())
                .reportId(report.getId())
                .reportTitle(report.getTitle())
                .disasterType(report.getDisasterType())
                .reportStatus(report.getStatus())
                .officerId(officer.getId())
                .officerName(officer.getFullName())
                .officerEmail(officer.getEmail())
                .citizenName(report.getCitizen().getFullName())
                .citizenEmail(report.getCitizen().getEmail())
                .images(imageResponses)
                .createdAt(assessment.getCreatedAt())
                .updatedAt(assessment.getUpdatedAt())
                .build();
    }
}
