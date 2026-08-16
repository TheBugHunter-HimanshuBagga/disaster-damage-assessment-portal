package com.himanshuProjects.disaster_damage_assessment_portal.service.impl.assignment;

import com.himanshuProjects.disaster_damage_assessment_portal.dto.assignment.AssignmentPageResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.assignment.AssignOfficerRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.assignment.OfficerAssignmentResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.assignment.UpdateAssignmentStatusRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.disaster.DisasterReport;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.disaster.OfficerAssignment;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.User;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.AssignmentStatus;
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
import com.himanshuProjects.disaster_damage_assessment_portal.repository.disaster.OfficerAssignmentRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.user.UserRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.service.assignment.OfficerAssignmentService;
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
public class OfficerAssignmentServiceImpl implements OfficerAssignmentService {

    private static final Logger log = LoggerFactory.getLogger(OfficerAssignmentServiceImpl.class);

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "assignedAt", "inspectionDate", "assignmentStatus", "createdAt", "updatedAt"
    );

    private static final List<AssignmentStatus> ACTIVE_STATUSES = List.of(
            AssignmentStatus.ASSIGNED,
            AssignmentStatus.ACCEPTED,
            AssignmentStatus.IN_PROGRESS
    );

    private final OfficerAssignmentRepository assignmentRepository;
    private final DisasterReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public OfficerAssignmentServiceImpl(OfficerAssignmentRepository assignmentRepository,
                                        DisasterReportRepository reportRepository,
                                        UserRepository userRepository,
                                        ApplicationEventPublisher eventPublisher) {
        this.assignmentRepository = assignmentRepository;
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    @Auditable(action = AuditAction.ASSIGN_OFFICER, entityName = "OfficerAssignment", description = "Officer assigned to disaster report")
    public OfficerAssignmentResponse assignOfficer(Long reportId, AssignOfficerRequest request) {
        log.info("Assigning officer ID: {} to report ID: {}", request.getFieldOfficerId(), reportId);

        DisasterReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("DisasterReport", "id", reportId));

        if (report.getStatus() != ReportStatus.SUBMITTED
                && report.getStatus() != ReportStatus.REINSPECTION_REQUIRED) {
            throw new BadRequestException(
                    "Cannot assign officer to report with status: " + report.getStatus()
                            + ". Report must be SUBMITTED or REINSPECTION_REQUIRED.");
        }

        if (assignmentRepository.existsByDisasterReportIdAndAssignmentStatusIn(
                reportId, ACTIVE_STATUSES)) {
            throw new ConflictException(
                    "Report ID: " + reportId + " already has an active assignment.");
        }

        User officer = userRepository.findById(request.getFieldOfficerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User", "id", request.getFieldOfficerId()));

        if (officer.getRole() != RoleType.FIELD_OFFICER) {
            throw new BadRequestException(
                    "User ID: " + officer.getId() + " is not a FIELD_OFFICER.");
        }

        OfficerAssignment assignment = new OfficerAssignment();
        assignment.setDisasterReport(report);
        assignment.setFieldOfficer(officer);
        assignment.setInspectionDate(request.getInspectionDate());
        assignment.setNotes(request.getNotes());
        assignment.setAssignmentStatus(AssignmentStatus.ASSIGNED);

        OfficerAssignment savedAssignment = assignmentRepository.save(assignment);

        report.setStatus(ReportStatus.ASSIGNED);
        reportRepository.save(report);

        eventPublisher.publishEvent(new NotificationEvent(
                this, officer.getId(), NotificationType.OFFICER_ASSIGNED,
                "Officer Assigned",
                "You have been assigned to disaster report \"" + report.getTitle() + "\".",
                savedAssignment.getId(), "OFFICER_ASSIGNMENT"));

        eventPublisher.publishEvent(new NotificationEvent(
                this, report.getCitizen().getId(), NotificationType.OFFICER_ASSIGNED,
                "Officer Assigned",
                "An officer has been assigned to your disaster report \"" + report.getTitle() + "\".",
                savedAssignment.getId(), "OFFICER_ASSIGNMENT"));

        log.info("Officer assigned successfully. Assignment ID: {}", savedAssignment.getId());
        return mapToResponse(savedAssignment);
    }

    @Override
    @Transactional(readOnly = true)
    public OfficerAssignmentResponse getAssignmentById(Long id) {
        log.info("Fetching assignment ID: {}", id);
        OfficerAssignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OfficerAssignment", "id", id));
        return mapToResponse(assignment);
    }

    @Override
    @Transactional(readOnly = true)
    public OfficerAssignmentResponse getAssignmentByReportId(Long reportId) {
        log.info("Fetching assignment for report ID: {}", reportId);

        OfficerAssignment assignment = assignmentRepository.findByDisasterReportId(reportId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "OfficerAssignment", "reportId", reportId));
        return mapToResponse(assignment);
    }

    @Override
    @Transactional
    public OfficerAssignmentResponse updateAssignmentStatus(Long id, String officerEmail,
                                                             UpdateAssignmentStatusRequest request) {
        log.info("Updating assignment ID: {} to status: {}", id, request.getAssignmentStatus());

        OfficerAssignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OfficerAssignment", "id", id));

        User officer = userRepository.findByEmail(officerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", officerEmail));

        if (!assignment.getFieldOfficer().getId().equals(officer.getId())) {
            throw new BadRequestException("You can only update your own assignments");
        }

        validateAssignmentStatusTransition(assignment.getAssignmentStatus(),
                request.getAssignmentStatus());

        assignment.setAssignmentStatus(request.getAssignmentStatus());
        if (request.getNotes() != null) {
            assignment.setNotes(request.getNotes());
        }

        OfficerAssignment updated = assignmentRepository.save(assignment);

        updateReportStatusFromAssignment(assignment.getDisasterReport(),
                request.getAssignmentStatus());

        log.info("Assignment status updated: {} -> {}", assignment.getAssignmentStatus(),
                request.getAssignmentStatus());
        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentPageResponse searchAssignments(String search, AssignmentStatus status,
                                                     Long officerId, int page, int size,
                                                     String sortBy, String sortDirection) {
        log.info("Searching assignments - search: {}, status: {}, officerId: {}",
                search, status, officerId);

        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new BadRequestException("Invalid sort field: " + sortBy
                    + ". Allowed fields: " + ALLOWED_SORT_FIELDS);
        }

        Sort sort = sortDirection.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<OfficerAssignment> assignmentPage = assignmentRepository.searchAssignments(
                search, status, officerId, pageable);

        List<OfficerAssignmentResponse> assignments = assignmentPage.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return AssignmentPageResponse.builder()
                .assignments(assignments)
                .pageNumber(assignmentPage.getNumber())
                .pageSize(assignmentPage.getSize())
                .totalElements(assignmentPage.getTotalElements())
                .totalPages(assignmentPage.getTotalPages())
                .last(assignmentPage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentPageResponse getMyAssignments(String officerEmail, AssignmentStatus status,
                                                    int page, int size,
                                                    String sortBy, String sortDirection) {
        log.info("Fetching assignments for officer: {} with status: {}", officerEmail, status);

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

        Page<OfficerAssignment> assignmentPage;
        if (status != null) {
            assignmentPage = assignmentRepository.searchAssignments(
                    null, status, officer.getId(), pageable);
        } else {
            assignmentPage = assignmentRepository.searchAssignments(
                    null, null, officer.getId(), pageable);
        }

        List<OfficerAssignmentResponse> assignments = assignmentPage.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return AssignmentPageResponse.builder()
                .assignments(assignments)
                .pageNumber(assignmentPage.getNumber())
                .pageSize(assignmentPage.getSize())
                .totalElements(assignmentPage.getTotalElements())
                .totalPages(assignmentPage.getTotalPages())
                .last(assignmentPage.isLast())
                .build();
    }

    @Override
    @Transactional
    public OfficerAssignmentResponse reassignOfficer(Long assignmentId, AssignOfficerRequest request) {
        log.info("Reassigning assignment ID: {} to officer ID: {}", assignmentId, request.getFieldOfficerId());

        OfficerAssignment oldAssignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("OfficerAssignment", "id", assignmentId));

        if (oldAssignment.getAssignmentStatus() == AssignmentStatus.COMPLETED
                || oldAssignment.getAssignmentStatus() == AssignmentStatus.REASSIGNED) {
            throw new BadRequestException(
                    "Cannot reassign assignment with status: " + oldAssignment.getAssignmentStatus());
        }

        DisasterReport report = oldAssignment.getDisasterReport();

        User newOfficer = userRepository.findById(request.getFieldOfficerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User", "id", request.getFieldOfficerId()));

        if (newOfficer.getRole() != RoleType.FIELD_OFFICER) {
            throw new BadRequestException(
                    "User ID: " + newOfficer.getId() + " is not a FIELD_OFFICER.");
        }

        if (oldAssignment.getFieldOfficer().getId().equals(newOfficer.getId())) {
            throw new BadRequestException("Cannot reassign to the same officer");
        }

        oldAssignment.setAssignmentStatus(AssignmentStatus.REASSIGNED);
        assignmentRepository.save(oldAssignment);

        OfficerAssignment newAssignment = new OfficerAssignment();
        newAssignment.setDisasterReport(report);
        newAssignment.setFieldOfficer(newOfficer);
        newAssignment.setInspectionDate(request.getInspectionDate());
        newAssignment.setNotes(request.getNotes());
        newAssignment.setAssignmentStatus(AssignmentStatus.ASSIGNED);

        OfficerAssignment saved = assignmentRepository.save(newAssignment);

        report.setStatus(ReportStatus.ASSIGNED);
        reportRepository.save(report);

        log.info("Officer reassigned. Old: {} -> New: {}", assignmentId, saved.getId());
        return mapToResponse(saved);
    }

    private void validateAssignmentStatusTransition(AssignmentStatus current, AssignmentStatus next) {
        boolean valid = switch (current) {
            case ASSIGNED -> next == AssignmentStatus.ACCEPTED || next == AssignmentStatus.REASSIGNED;
            case ACCEPTED -> next == AssignmentStatus.IN_PROGRESS;
            case IN_PROGRESS -> next == AssignmentStatus.COMPLETED;
            case COMPLETED -> false;
            case REASSIGNED -> false;
        };

        if (!valid) {
            throw new BadRequestException(
                    "Invalid assignment status transition: " + current + " -> " + next);
        }
    }

    private void updateReportStatusFromAssignment(DisasterReport report, AssignmentStatus assignmentStatus) {
        switch (assignmentStatus) {
            case ACCEPTED -> report.setStatus(ReportStatus.UNDER_INSPECTION);
            case COMPLETED -> report.setStatus(ReportStatus.UNDER_REVIEW);
            default -> { }
        }
        reportRepository.save(report);
    }

    private OfficerAssignmentResponse mapToResponse(OfficerAssignment assignment) {
        DisasterReport report = assignment.getDisasterReport();
        User officer = assignment.getFieldOfficer();

        return OfficerAssignmentResponse.builder()
                .id(assignment.getId())
                .reportId(report.getId())
                .reportTitle(report.getTitle())
                .disasterType(report.getDisasterType())
                .reportStatus(report.getStatus())
                .incidentAddress(report.getIncidentAddress())
                .officerId(officer.getId())
                .officerName(officer.getFullName())
                .officerEmail(officer.getEmail())
                .assignmentStatus(assignment.getAssignmentStatus())
                .assignedAt(assignment.getAssignedAt())
                .inspectionDate(assignment.getInspectionDate())
                .notes(assignment.getNotes())
                .citizenName(report.getCitizen().getFullName())
                .citizenEmail(report.getCitizen().getEmail())
                .createdAt(assignment.getCreatedAt())
                .updatedAt(assignment.getUpdatedAt())
                .build();
    }
}
