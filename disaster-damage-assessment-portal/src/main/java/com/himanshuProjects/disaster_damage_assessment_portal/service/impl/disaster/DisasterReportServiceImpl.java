package com.himanshuProjects.disaster_damage_assessment_portal.service.impl.disaster;

import com.himanshuProjects.disaster_damage_assessment_portal.dto.disaster.AddReportImagesRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.disaster.CreateDisasterReportRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.disaster.DisasterReportPageResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.disaster.DisasterReportResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.disaster.ReportImageResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.disaster.UpdateDisasterReportRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.disaster.UpdateReportStatusRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.disaster.DisasterReport;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.disaster.ReportImage;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.User;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.DisasterType;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.ReportStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.RoleType;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.NotificationType;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.AuditAction;
import com.himanshuProjects.disaster_damage_assessment_portal.aspect.Auditable;
import com.himanshuProjects.disaster_damage_assessment_portal.event.NotificationEvent;
import com.himanshuProjects.disaster_damage_assessment_portal.exception.BadRequestException;
import com.himanshuProjects.disaster_damage_assessment_portal.exception.ConflictException;
import com.himanshuProjects.disaster_damage_assessment_portal.exception.ResourceNotFoundException;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.disaster.DisasterReportRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.disaster.ReportImageRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.user.UserRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.service.disaster.DisasterReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class DisasterReportServiceImpl implements DisasterReportService {

    private static final Logger log = LoggerFactory.getLogger(DisasterReportServiceImpl.class);

    private static final int MAX_IMAGES_PER_REPORT = 10;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "title", "disasterType", "status", "reportedAt", "createdAt", "updatedAt"
    );

    private static final Set<ReportStatus> EDITABLE_STATUSES = Set.of(
            ReportStatus.SUBMITTED
    );

    private final DisasterReportRepository reportRepository;
    private final ReportImageRepository reportImageRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public DisasterReportServiceImpl(DisasterReportRepository reportRepository,
                                     ReportImageRepository reportImageRepository,
                                     UserRepository userRepository,
                                     ApplicationEventPublisher eventPublisher) {
        this.reportRepository = reportRepository;
        this.reportImageRepository = reportImageRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    @Auditable(action = AuditAction.CREATE_REPORT, entityName = "DisasterReport", description = "New disaster report created")
    public DisasterReportResponse createReport(String citizenEmail, CreateDisasterReportRequest request) {
        log.info("Creating disaster report for citizen: {}", citizenEmail);

        User citizen = userRepository.findByEmail(citizenEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", citizenEmail));

        DisasterReport report = new DisasterReport();
        report.setTitle(request.getTitle());
        report.setDescription(request.getDescription());
        report.setDisasterType(request.getDisasterType());
        report.setIncidentAddress(request.getIncidentAddress());
        report.setLatitude(request.getLatitude());
        report.setLongitude(request.getLongitude());
        report.setCitizen(citizen);
        report.setStatus(ReportStatus.SUBMITTED);

        DisasterReport savedReport = reportRepository.save(report);

        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            if (request.getImageUrls().size() > MAX_IMAGES_PER_REPORT) {
                throw new BadRequestException(
                        "Cannot upload more than " + MAX_IMAGES_PER_REPORT + " images");
            }
            for (String imageUrl : request.getImageUrls()) {
                ReportImage image = new ReportImage();
                image.setImageUrl(imageUrl);
                image.setDisasterReport(savedReport);
                reportImageRepository.save(image);
            }
        }

        eventPublisher.publishEvent(new NotificationEvent(
                this, citizen.getId(), NotificationType.REPORT_SUBMITTED,
                "Report Submitted",
                "Your disaster report \"" + savedReport.getTitle() + "\" has been submitted successfully.",
                savedReport.getId(), "DISASTER_REPORT"));

        log.info("Disaster report created: {} (ID: {})", savedReport.getTitle(), savedReport.getId());
        return mapToResponse(savedReport);
    }

    @Override
    @Transactional(readOnly = true)
    public DisasterReportResponse getReportById(Long id) {
        log.info("Fetching report ID: {}", id);
        DisasterReport report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DisasterReport", "id", id));
        return mapToResponse(report);
    }

    @Override
    @Transactional(readOnly = true)
    public DisasterReportResponse getReportByIdForCitizen(Long id, String citizenEmail) {
        log.info("Fetching report ID: {} for citizen: {}", id, citizenEmail);

        DisasterReport report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DisasterReport", "id", id));

        if (!report.getCitizen().getEmail().equals(citizenEmail)) {
            throw new BadRequestException("You do not have access to this report");
        }

        return mapToResponse(report);
    }

    @Override
    @Transactional(readOnly = true)
    public DisasterReportPageResponse getMyReports(String citizenEmail, int page, int size,
                                                    String sortBy, String sortDirection) {
        log.info("Fetching reports for citizen: {}", citizenEmail);

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
        Page<DisasterReport> reportPage = reportRepository.findByCitizenIdOrderByCreatedAtDesc(
                citizen.getId(), pageable);

        List<DisasterReportResponse> reports = reportPage.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return DisasterReportPageResponse.builder()
                .reports(reports)
                .pageNumber(reportPage.getNumber())
                .pageSize(reportPage.getSize())
                .totalElements(reportPage.getTotalElements())
                .totalPages(reportPage.getTotalPages())
                .last(reportPage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public DisasterReportPageResponse searchReports(String search, DisasterType disasterType,
                                                     ReportStatus status, Long citizenId,
                                                     int page, int size,
                                                     String sortBy, String sortDirection) {
        log.info("Searching reports - search: {}, type: {}, status: {}, citizenId: {}",
                search, disasterType, status, citizenId);

        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new BadRequestException("Invalid sort field: " + sortBy
                    + ". Allowed fields: " + ALLOWED_SORT_FIELDS);
        }

        Sort sort = sortDirection.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<DisasterReport> reportPage = reportRepository.searchReports(
                search, disasterType, status, citizenId, pageable);

        List<DisasterReportResponse> reports = reportPage.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return DisasterReportPageResponse.builder()
                .reports(reports)
                .pageNumber(reportPage.getNumber())
                .pageSize(reportPage.getSize())
                .totalElements(reportPage.getTotalElements())
                .totalPages(reportPage.getTotalPages())
                .last(reportPage.isLast())
                .build();
    }

    @Override
    @Transactional
    @Auditable(action = AuditAction.UPDATE_REPORT, entityName = "DisasterReport", description = "Disaster report updated")
    public DisasterReportResponse updateReport(Long id, String citizenEmail,
                                                UpdateDisasterReportRequest request) {
        log.info("Updating report ID: {} by citizen: {}", id, citizenEmail);

        DisasterReport report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DisasterReport", "id", id));

        if (!report.getCitizen().getEmail().equals(citizenEmail)) {
            throw new BadRequestException("You can only update your own reports");
        }

        if (!EDITABLE_STATUSES.contains(report.getStatus())) {
            throw new BadRequestException(
                    "Cannot update report with status: " + report.getStatus()
                            + ". Only SUBMITTED reports can be edited.");
        }

        report.setTitle(request.getTitle());
        report.setDescription(request.getDescription());
        report.setDisasterType(request.getDisasterType());
        report.setIncidentAddress(request.getIncidentAddress());
        report.setLatitude(request.getLatitude());
        report.setLongitude(request.getLongitude());

        DisasterReport updatedReport = reportRepository.save(report);
        log.info("Report updated: {} (ID: {})", updatedReport.getTitle(), id);
        return mapToResponse(updatedReport);
    }

    @Override
    @Transactional
    public DisasterReportResponse updateReportStatus(Long id, UpdateReportStatusRequest request) {
        log.info("Updating status of report ID: {} to {}", id, request.getStatus());

        DisasterReport report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DisasterReport", "id", id));

        validateStatusTransition(report.getStatus(), request.getStatus());

        report.setStatus(request.getStatus());
        DisasterReport updatedReport = reportRepository.save(report);

        log.info("Report status updated: {} -> {} for report ID: {}",
                report.getStatus(), request.getStatus(), id);
        return mapToResponse(updatedReport);
    }

    @Override
    @Transactional
    @Auditable(action = AuditAction.DELETE_REPORT, entityName = "DisasterReport", description = "Disaster report deleted")
    public void deleteReport(Long id, String citizenEmail) {
        log.info("Deleting report ID: {} by citizen: {}", id, citizenEmail);

        DisasterReport report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DisasterReport", "id", id));

        if (!report.getCitizen().getEmail().equals(citizenEmail)) {
            throw new BadRequestException("You can only delete your own reports");
        }

        if (!EDITABLE_STATUSES.contains(report.getStatus())) {
            throw new BadRequestException(
                    "Cannot delete report with status: " + report.getStatus()
                            + ". Only SUBMITTED reports can be deleted.");
        }

        reportImageRepository.deleteByDisasterReportId(id);
        reportRepository.delete(report);
        log.info("Report deleted: {} (ID: {})", report.getTitle(), id);
    }

    @Override
    @Transactional
    public DisasterReportResponse addImages(Long reportId, String citizenEmail,
                                             AddReportImagesRequest request) {
        log.info("Adding images to report ID: {} by citizen: {}", reportId, citizenEmail);

        DisasterReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("DisasterReport", "id", reportId));

        if (!report.getCitizen().getEmail().equals(citizenEmail)) {
            throw new BadRequestException("You can only modify your own reports");
        }

        if (!EDITABLE_STATUSES.contains(report.getStatus())) {
            throw new BadRequestException(
                    "Cannot add images to report with status: " + report.getStatus());
        }

        long currentCount = reportImageRepository.countByDisasterReportId(reportId);
        if (currentCount + request.getImageUrls().size() > MAX_IMAGES_PER_REPORT) {
            throw new BadRequestException(
                    "Cannot add " + request.getImageUrls().size() + " images. "
                            + "Report already has " + currentCount + " images. "
                            + "Maximum allowed is " + MAX_IMAGES_PER_REPORT);
        }

        for (String imageUrl : request.getImageUrls()) {
            ReportImage image = new ReportImage();
            image.setImageUrl(imageUrl);
            image.setDisasterReport(report);
            reportImageRepository.save(image);
        }

        log.info("Added {} images to report ID: {}", request.getImageUrls().size(), reportId);
        return mapToResponse(report);
    }

    @Override
    @Transactional
    public void removeImage(Long reportId, Long imageId, String citizenEmail) {
        log.info("Removing image ID: {} from report ID: {}", imageId, reportId);

        DisasterReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("DisasterReport", "id", reportId));

        if (!report.getCitizen().getEmail().equals(citizenEmail)) {
            throw new BadRequestException("You can only modify your own reports");
        }

        if (!EDITABLE_STATUSES.contains(report.getStatus())) {
            throw new BadRequestException(
                    "Cannot remove images from report with status: " + report.getStatus());
        }

        ReportImage image = reportImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("ReportImage", "id", imageId));

        if (!image.getDisasterReport().getId().equals(reportId)) {
            throw new BadRequestException("Image does not belong to this report");
        }

        reportImageRepository.delete(image);
        log.info("Image ID: {} removed from report ID: {}", imageId, reportId);
    }

    private void validateStatusTransition(ReportStatus current, ReportStatus next) {
        boolean valid = switch (current) {
            case SUBMITTED -> next == ReportStatus.ASSIGNED;
            case ASSIGNED -> next == ReportStatus.UNDER_INSPECTION;
            case UNDER_INSPECTION -> next == ReportStatus.UNDER_REVIEW
                    || next == ReportStatus.REINSPECTION_REQUIRED;
            case UNDER_REVIEW -> next == ReportStatus.APPROVED
                    || next == ReportStatus.REJECTED
                    || next == ReportStatus.REINSPECTION_REQUIRED;
            case REINSPECTION_REQUIRED -> next == ReportStatus.ASSIGNED;
            case APPROVED -> next == ReportStatus.COMPLETED;
            case REJECTED -> false;
            case COMPLETED -> false;
        };

        if (!valid) {
            throw new BadRequestException(
                    "Invalid status transition: " + current + " -> " + next);
        }
    }

    private DisasterReportResponse mapToResponse(DisasterReport report) {
        List<ReportImage> images = reportImageRepository
                .findByDisasterReportIdOrderByUploadedAtAsc(report.getId());

        List<ReportImageResponse> imageResponses = images.stream()
                .map(img -> ReportImageResponse.builder()
                        .id(img.getId())
                        .imageUrl(img.getImageUrl())
                        .uploadedAt(img.getUploadedAt())
                        .build())
                .toList();

        return DisasterReportResponse.builder()
                .id(report.getId())
                .title(report.getTitle())
                .description(report.getDescription())
                .disasterType(report.getDisasterType())
                .status(report.getStatus())
                .incidentAddress(report.getIncidentAddress())
                .latitude(report.getLatitude())
                .longitude(report.getLongitude())
                .reportedAt(report.getReportedAt())
                .citizenId(report.getCitizen().getId())
                .citizenName(report.getCitizen().getFullName())
                .citizenEmail(report.getCitizen().getEmail())
                .images(imageResponses)
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }
}
