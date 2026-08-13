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
import com.himanshuProjects.disaster_damage_assessment_portal.service.compensation.CompensationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class CompensationServiceImpl implements CompensationService {

    private static final Logger log = LoggerFactory.getLogger(CompensationServiceImpl.class);

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "approvedAmount", "compensationStatus", "paymentStatus",
            "approvedDate", "paidDate", "createdAt", "updatedAt"
    );

    private final CompensationRepository compensationRepository;
    private final CompensationStatusLogRepository statusLogRepository;
    private final DamageAssessmentRepository assessmentRepository;
    private final DisasterReportRepository reportRepository;
    private final UserRepository userRepository;

    public CompensationServiceImpl(CompensationRepository compensationRepository,
                                   CompensationStatusLogRepository statusLogRepository,
                                   DamageAssessmentRepository assessmentRepository,
                                   DisasterReportRepository reportRepository,
                                   UserRepository userRepository) {
        this.compensationRepository = compensationRepository;
        this.statusLogRepository = statusLogRepository;
        this.assessmentRepository = assessmentRepository;
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public CompensationResponse createCompensation(String adminEmail, CreateCompensationRequest request) {
        log.info("Creating compensation for assessment ID: {} by admin: {}", request.getDamageAssessmentId(), adminEmail);

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", adminEmail));

        if (admin.getRole() != RoleType.SUPER_ADMIN && admin.getRole() != RoleType.DISTRICT_ADMIN) {
            throw new BadRequestException("Only SUPER_ADMIN or DISTRICT_ADMIN can create compensations");
        }

        DamageAssessment assessment = assessmentRepository.findById(request.getDamageAssessmentId())
                .orElseThrow(() -> new ResourceNotFoundException("DamageAssessment", "id", request.getDamageAssessmentId()));

        if (compensationRepository.existsByDamageAssessmentId(request.getDamageAssessmentId())) {
            throw new ConflictException("Compensation already exists for assessment ID: " + request.getDamageAssessmentId());
        }

        DisasterReport report = assessment.getDisasterReport();
        if (report.getStatus() != ReportStatus.UNDER_REVIEW) {
            throw new BadRequestException(
                    "Cannot create compensation for report with status: " + report.getStatus()
                            + ". Report must be UNDER_REVIEW.");
        }

        Compensation compensation = new Compensation();
        compensation.setApprovedAmount(request.getApprovedAmount());
        compensation.setRemarks(request.getRemarks());
        compensation.setDamageAssessment(assessment);

        Compensation saved = compensationRepository.save(compensation);

        logStatusChange(null, CompensationStatus.PENDING, "Compensation created with PENDING status", admin, saved);

        report.setStatus(ReportStatus.APPROVED);
        reportRepository.save(report);

        log.info("Compensation created: {} for assessment ID: {}", saved.getId(), request.getDamageAssessmentId());
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CompensationResponse getCompensationById(Long id) {
        log.info("Fetching compensation ID: {}", id);
        Compensation compensation = compensationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compensation", "id", id));
        return mapToResponse(compensation);
    }

    @Override
    @Transactional
    public CompensationResponse updateCompensation(Long id, String adminEmail, UpdateCompensationRequest request) {
        log.info("Updating compensation ID: {} by admin: {}", id, adminEmail);

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", adminEmail));

        if (admin.getRole() != RoleType.SUPER_ADMIN && admin.getRole() != RoleType.DISTRICT_ADMIN) {
            throw new BadRequestException("Only SUPER_ADMIN or DISTRICT_ADMIN can update compensations");
        }

        Compensation compensation = compensationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compensation", "id", id));

        if (compensation.getCompensationStatus() != CompensationStatus.PENDING) {
            throw new BadRequestException(
                    "Cannot update compensation with status: " + compensation.getCompensationStatus()
                            + ". Only PENDING compensations can be updated.");
        }

        if (request.getApprovedAmount() != null) {
            compensation.setApprovedAmount(request.getApprovedAmount());
        }
        if (request.getRemarks() != null) {
            compensation.setRemarks(request.getRemarks());
        }

        Compensation updated = compensationRepository.save(compensation);
        log.info("Compensation updated: {}", id);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public CompensationResponse approveCompensation(Long id, String adminEmail, ApproveCompensationRequest request) {
        log.info("Approving compensation ID: {} by admin: {}", id, adminEmail);

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", adminEmail));

        if (admin.getRole() != RoleType.SUPER_ADMIN && admin.getRole() != RoleType.DISTRICT_ADMIN) {
            throw new BadRequestException("Only SUPER_ADMIN or DISTRICT_ADMIN can approve compensations");
        }

        Compensation compensation = compensationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compensation", "id", id));

        if (compensation.getCompensationStatus() != CompensationStatus.PENDING) {
            throw new BadRequestException(
                    "Cannot approve compensation with status: " + compensation.getCompensationStatus()
                            + ". Only PENDING compensations can be approved.");
        }

        CompensationStatus previousStatus = compensation.getCompensationStatus();
        compensation.setCompensationStatus(CompensationStatus.APPROVED);
        compensation.setApprovedDate(LocalDateTime.now());
        compensation.setApprovedAt(admin);

        if (request.getRemarks() != null && !request.getRemarks().isBlank()) {
            compensation.setRemarks(request.getRemarks());
        }

        Compensation saved = compensationRepository.save(compensation);

        logStatusChange(previousStatus, CompensationStatus.APPROVED,
                request.getRemarks() != null ? request.getRemarks() : "Compensation approved", admin, saved);

        log.info("Compensation approved: {}", id);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public CompensationResponse rejectCompensation(Long id, String adminEmail, RejectCompensationRequest request) {
        log.info("Rejecting compensation ID: {} by admin: {}", id, adminEmail);

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", adminEmail));

        if (admin.getRole() != RoleType.SUPER_ADMIN && admin.getRole() != RoleType.DISTRICT_ADMIN) {
            throw new BadRequestException("Only SUPER_ADMIN or DISTRICT_ADMIN can reject compensations");
        }

        Compensation compensation = compensationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compensation", "id", id));

        if (compensation.getCompensationStatus() != CompensationStatus.PENDING) {
            throw new BadRequestException(
                    "Cannot reject compensation with status: " + compensation.getCompensationStatus()
                            + ". Only PENDING compensations can be rejected.");
        }

        CompensationStatus previousStatus = compensation.getCompensationStatus();
        compensation.setCompensationStatus(CompensationStatus.REJECTED);
        compensation.setRemarks(request.getReason());

        Compensation saved = compensationRepository.save(compensation);

        logStatusChange(previousStatus, CompensationStatus.REJECTED, request.getReason(), admin, saved);

        DisasterReport report = compensation.getDamageAssessment().getDisasterReport();
        report.setStatus(ReportStatus.REJECTED);
        reportRepository.save(report);

        log.info("Compensation rejected: {}", id);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public CompensationResponse updatePaymentStatus(Long id, String adminEmail, UpdatePaymentStatusRequest request) {
        log.info("Updating payment status for compensation ID: {} to {} by admin: {}",
                id, request.getPaymentStatus(), adminEmail);

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", adminEmail));

        if (admin.getRole() != RoleType.SUPER_ADMIN && admin.getRole() != RoleType.DISTRICT_ADMIN) {
            throw new BadRequestException("Only SUPER_ADMIN or DISTRICT_ADMIN can update payment status");
        }

        Compensation compensation = compensationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compensation", "id", id));

        if (compensation.getCompensationStatus() != CompensationStatus.APPROVED) {
            throw new BadRequestException(
                    "Cannot update payment status for compensation with status: " + compensation.getCompensationStatus()
                            + ". Only APPROVED compensations can have payment processed.");
        }

        PaymentStatus previousPaymentStatus = compensation.getPaymentStatus();
        compensation.setPaymentStatus(request.getPaymentStatus());

        if (request.getPaymentStatus() == PaymentStatus.COMPLETED) {
            compensation.setPaidDate(LocalDateTime.now());
            compensation.setPaidBy(admin);
            compensation.setCompensationStatus(CompensationStatus.PAID);

            logStatusChange(CompensationStatus.APPROVED, CompensationStatus.PAID,
                    "Payment completed", admin, compensation);

            DisasterReport report = compensation.getDamageAssessment().getDisasterReport();
            report.setStatus(ReportStatus.COMPLETED);
            reportRepository.save(report);
        } else if (request.getPaymentStatus() == PaymentStatus.FAILED) {
            compensation.setCompensationStatus(CompensationStatus.APPROVED);

            logStatusChange(CompensationStatus.PAID, CompensationStatus.APPROVED,
                    "Payment failed, reverted to APPROVED", admin, compensation);
        }

        Compensation saved = compensationRepository.save(compensation);
        log.info("Payment status updated for compensation ID: {} from {} to {}", id, previousPaymentStatus, request.getPaymentStatus());
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CompensationPageResponse searchCompensations(String search, CompensationStatus status,
                                                         PaymentStatus paymentStatus, int page, int size,
                                                         String sortBy, String sortDirection) {
        log.info("Searching compensations - search: {}, status: {}, paymentStatus: {}",
                search, status, paymentStatus);

        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new BadRequestException("Invalid sort field: " + sortBy
                    + ". Allowed fields: " + ALLOWED_SORT_FIELDS);
        }

        Sort sort = sortDirection.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Compensation> pageResult = compensationRepository.searchCompensations(
                search, status, paymentStatus, pageable);

        List<CompensationResponse> compensations = pageResult.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return CompensationPageResponse.builder()
                .compensations(compensations)
                .pageNumber(pageResult.getNumber())
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CompensationPageResponse getMyCompensations(String citizenEmail, int page, int size,
                                                        String sortBy, String sortDirection) {
        log.info("Fetching compensations for citizen: {}", citizenEmail);

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
        Page<Compensation> pageResult = compensationRepository.findByCitizenId(citizen.getId(), pageable);

        List<CompensationResponse> compensations = pageResult.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return CompensationPageResponse.builder()
                .compensations(compensations)
                .pageNumber(pageResult.getNumber())
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompensationHistoryResponse> getCompensationHistory(Long compensationId) {
        log.info("Fetching compensation history for compensation ID: {}", compensationId);

        Compensation compensation = compensationRepository.findById(compensationId)
                .orElseThrow(() -> new ResourceNotFoundException("Compensation", "id", compensationId));

        List<CompensationStatusLog> logs = statusLogRepository
                .findByCompensationIdOrderByCreatedAtAsc(compensation.getId());

        return logs.stream()
                .map(logEntry -> CompensationHistoryResponse.builder()
                        .id(logEntry.getId())
                        .previousStatus(logEntry.getPreviousStatus())
                        .newStatus(logEntry.getNewStatus())
                        .remarks(logEntry.getRemarks())
                        .changedByName(logEntry.getChangedBy().getFullName())
                        .changedByEmail(logEntry.getChangedBy().getEmail())
                        .changedAt(logEntry.getCreatedAt())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public void deleteCompensation(Long id, String adminEmail) {
        log.info("Deleting compensation ID: {} by admin: {}", id, adminEmail);

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", adminEmail));

        if (admin.getRole() != RoleType.SUPER_ADMIN) {
            throw new BadRequestException("Only SUPER_ADMIN can delete compensations");
        }

        Compensation compensation = compensationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compensation", "id", id));

        if (compensation.getCompensationStatus() != CompensationStatus.PENDING) {
            throw new BadRequestException(
                    "Cannot delete compensation with status: " + compensation.getCompensationStatus()
                            + ". Only PENDING compensations can be deleted.");
        }

        DisasterReport report = compensation.getDamageAssessment().getDisasterReport();
        report.setStatus(ReportStatus.UNDER_REVIEW);
        reportRepository.save(report);

        compensationRepository.delete(compensation);
        log.info("Compensation deleted: {}", id);
    }

    private void logStatusChange(CompensationStatus previousStatus, CompensationStatus newStatus,
                                  String remarks, User changedBy, Compensation compensation) {
        CompensationStatusLog logEntry = new CompensationStatusLog();
        logEntry.setPreviousStatus(previousStatus);
        logEntry.setNewStatus(newStatus);
        logEntry.setRemarks(remarks);
        logEntry.setChangedBy(changedBy);
        logEntry.setCompensation(compensation);
        statusLogRepository.save(logEntry);
    }

    private CompensationResponse mapToResponse(Compensation compensation) {
        DamageAssessment assessment = compensation.getDamageAssessment();
        DisasterReport report = assessment.getDisasterReport();

        return CompensationResponse.builder()
                .id(compensation.getId())
                .approvedAmount(compensation.getApprovedAmount())
                .compensationStatus(compensation.getCompensationStatus())
                .approvedDate(compensation.getApprovedDate())
                .remarks(compensation.getRemarks())
                .paymentStatus(compensation.getPaymentStatus())
                .paidDate(compensation.getPaidDate())
                .damageAssessmentId(assessment.getId())
                .estimatedLoss(assessment.getEstimatedLoss())
                .damageLevel(assessment.getDamageLevel())
                .reportId(report.getId())
                .reportTitle(report.getTitle())
                .disasterType(report.getDisasterType())
                .reportStatus(report.getStatus())
                .citizenName(report.getCitizen().getFullName())
                .citizenEmail(report.getCitizen().getEmail())
                .approvedByName(compensation.getApprovedAt() != null ? compensation.getApprovedAt().getFullName() : null)
                .approvedByEmail(compensation.getApprovedAt() != null ? compensation.getApprovedAt().getEmail() : null)
                .paidByName(compensation.getPaidBy() != null ? compensation.getPaidBy().getFullName() : null)
                .paidByEmail(compensation.getPaidBy() != null ? compensation.getPaidBy().getEmail() : null)
                .createdAt(compensation.getCreatedAt())
                .updatedAt(compensation.getUpdatedAt())
                .build();
    }
}
