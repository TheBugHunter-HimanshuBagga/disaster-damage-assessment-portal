package com.himanshuProjects.disaster_damage_assessment_portal.config;

import com.himanshuProjects.disaster_damage_assessment_portal.entity.compensation.Compensation;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.compensation.CompensationStatusLog;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.disaster.DamageAssessment;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.disaster.DisasterReport;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.disaster.InspectionImage;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.disaster.OfficerAssignment;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.disaster.ReportImage;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.system.AuditLog;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.system.Feedback;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.system.Notification;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.CitizenProfile;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.District;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.State;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.User;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.AccountStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.AssignmentStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.AuditAction;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.CompensationStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.DamageLevel;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.DisasterType;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.Gender;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.NotificationType;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.PaymentStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.ReportStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.RoleType;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.compensation.CompensationRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.compensation.CompensationStatusLogRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.disaster.DamageAssessmentRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.disaster.DisasterReportRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.disaster.InspectionImageRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.disaster.OfficerAssignmentRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.disaster.ReportImageRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.system.AuditLogRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.system.FeedbackRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.system.NotificationRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.user.CitizenProfileRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.user.DistrictRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.user.StateRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@Order(10)
@RequiredArgsConstructor
@Slf4j
public class DemoDataSeeder implements CommandLineRunner {

    private static final String RAVI_EMAIL = "ravi.kumar@portal.gov.in";
    private static final String PRIYA_EMAIL = "priya.sharma@portal.gov.in";
    private static final String IP = "127.0.0.1";

    private final StateRepository stateRepository;
    private final DistrictRepository districtRepository;
    private final UserRepository userRepository;
    private final CitizenProfileRepository citizenProfileRepository;
    private final DisasterReportRepository disasterReportRepository;
    private final ReportImageRepository reportImageRepository;
    private final OfficerAssignmentRepository officerAssignmentRepository;
    private final DamageAssessmentRepository damageAssessmentRepository;
    private final InspectionImageRepository inspectionImageRepository;
    private final CompensationRepository compensationRepository;
    private final CompensationStatusLogRepository compensationStatusLogRepository;
    private final NotificationRepository notificationRepository;
    private final FeedbackRepository feedbackRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail(RAVI_EMAIL) && userRepository.existsByEmail(PRIYA_EMAIL)) {
            log.info("DemoDataSeeder skipped - demo data already present");
            return;
        }
        log.info("Seeding demo data (reports, assessments, compensations, notifications)...");

        State punjab = stateRepository.findAll().stream()
                .filter(s -> "PB".equalsIgnoreCase(s.getCode()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Punjab state missing - run DataSeeder first"));
        District ludhiana = districtRepository.findByNameIgnoreCaseAndStateId("Ludhiana", punjab.getId())
                .orElseThrow(() -> new IllegalStateException("Ludhiana district missing - run DataSeeder first"));

        User superAdmin = requireUser("superadmin@portal.gov.in");
        User districtAdmin = requireUser("districtadmin@portal.gov.in");
        User officer1 = requireUser("officer@portal.gov.in");
        User officer2 = requireUser("officer2@portal.gov.in");
        User citizenMain = requireUser("citizen@portal.gov.in");

        User ravi = createUser("Ravi Kumar", RAVI_EMAIL, "9800001111", Gender.MALE, ludhiana, "Citizen@1234");
        User priya = createUser("Priya Sharma", PRIYA_EMAIL, "9800002222", Gender.FEMALE, ludhiana, "Citizen@1234");

        createProfile(ravi, "123456789001", LocalDate.of(1992, 4, 17),
                "12 Green Park Colony, Ludhiana, Punjab", "9800001112");
        createProfile(priya, "123456789002", LocalDate.of(1996, 9, 3),
                "45 Model Town Extension, Ludhiana, Punjab", "9800002223");
        createProfile(citizenMain, "123456789003", LocalDate.of(1988, 1, 25),
                "7 Ferozepur Road, Ludhiana, Punjab", "9800003334");

        DisasterReport r1 = createReport(ravi, "Flash flood inundated residential area",
                "Record rainfall caused waterlogging in Sundar Nagar. Ground floors of 40+ homes flooded, furniture and electronics damaged.",
                DisasterType.FLOOD, ReportStatus.COMPLETED, "Sundar Nagar, Ludhiana, Punjab",
                30.9010, 75.8573, daysAgo(22));
        addReportImages(r1, 2, "flood");
        OfficerAssignment a1 = assign(r1, officer1, AssignmentStatus.COMPLETED, daysAgo(20),
                "Initial flood inspection assigned");
        DamageAssessment as1 = assess(r1, officer1, DamageLevel.SEVERE, "485000.00",
                "Severe waterlogging. 42 houses with damaged furniture, appliances and wall plaster. Two homes need structural survey.",
                "Recommend full reimbursement for household goods plus partial structural repair grant.",
                daysAgo(18));
        addInspectionImages(as1, 3, "flood");
        Compensation c1 = compensate(as1, "450000.00",
                "Approved as per flood relief norms - household goods plus structural grant", daysAgo(16));
        approveCompensation(c1, districtAdmin, daysAgo(15));
        payCompensation(c1, districtAdmin, daysAgo(12));
        logCompensation(c1, CompensationStatus.PENDING, CompensationStatus.PENDING, "Compensation record created", districtAdmin);
        logCompensation(c1, CompensationStatus.PENDING, CompensationStatus.APPROVED, "Approved by district administration", districtAdmin);
        logCompensation(c1, CompensationStatus.APPROVED, CompensationStatus.PAID, "Payment released via direct benefit transfer", districtAdmin);
        feedback(ravi, r1, 5, "The entire process was smooth. Officer visited quickly and compensation arrived on time. Thank you.");

        DisasterReport r2 = createReport(ravi, "Earthquake cracked walls in apartment block",
                "Tremor caused structural cracks in stairwell walls and ceiling plaster of block C.",
                DisasterType.EARTHQUAKE, ReportStatus.UNDER_REVIEW, "BRS Nagar, Ludhiana, Punjab",
                30.8810, 75.8200, daysAgo(11));
        addReportImages(r2, 2, "quake");
        assign(r2, officer2, AssignmentStatus.COMPLETED, daysAgo(9), "Structural inspection scheduled");
        DamageAssessment as2 = assess(r2, officer2, DamageLevel.HIGH, "275000.00",
                "Hairline and diagonal cracks on load bearing walls of stairwell. Plaster fall in 6 flats.",
                "Recommend structural retrofitting before re-occupancy of block C stairwell.", daysAgo(7));
        addInspectionImages(as2, 2, "quake");

        DisasterReport r3 = createReport(ravi, "Kitchen fire spread to shop shutter",
                "Gas cylinder leak caused fire in a grocery shop. Shutters, stock and wiring destroyed.",
                DisasterType.FIRE, ReportStatus.SUBMITTED, "Model Town Market, Ludhiana, Punjab",
                30.9050, 75.8500, daysAgo(2));
        addReportImages(r3, 1, "fire");

        DisasterReport r4 = createReport(priya, "Cyclone uprooted trees across main road",
                "Severe storm uprooted 15 trees, damaged two shop hoardings and an electricity pole on the highway.",
                DisasterType.CYCLONE, ReportStatus.ASSIGNED, "Ferozepur Road, Ludhiana, Punjab",
                30.8700, 75.8300, daysAgo(5));
        addReportImages(r4, 2, "cyclone");
        assign(r4, officer1, AssignmentStatus.ASSIGNED, daysAgo(4), "Road clearance inspection");

        DisasterReport r5 = createReport(priya, "Landslide blocked hill access road",
                "Monsoon landslide on the bypass service road blocked vehicle access to 3 villages.",
                DisasterType.LANDSLIDE, ReportStatus.UNDER_INSPECTION, "Bypass Service Road, Ludhiana, Punjab",
                30.9200, 75.8700, daysAgo(6));
        addReportImages(r5, 2, "landslide");
        assign(r5, officer2, AssignmentStatus.IN_PROGRESS, daysAgo(5), "Debris clearance inspection in progress");

        DisasterReport r6 = createReport(priya, "Cloudburst water surge damaged godown",
                "Sudden water surge from a cloudburst entered a rented godown. Inventory damage claimed.",
                DisasterType.TSUNAMI, ReportStatus.REJECTED, "Industrial Area Phase 1, Ludhiana, Punjab",
                30.8900, 75.8600, daysAgo(30));
        addReportImages(r6, 1, "surge");
        assign(r6, officer1, AssignmentStatus.COMPLETED, daysAgo(28), "Inventory verification visit");
        DamageAssessment as6 = assess(r6, officer1, DamageLevel.MEDIUM, "90000.00",
                "Water ingress limited to rear section of godown. Inventory list provided by claimant could not be verified.",
                "Partial damage confirmed. Inventory claim unsupported by records.", daysAgo(26));
        addInspectionImages(as6, 1, "surge");
        Compensation c6 = compensate(as6, "25000.00",
                "Claim largely unsupported by invoices, only structural seepage considered", daysAgo(24));
        rejectCompensation(c6, districtAdmin, "Inventory claim rejected - no purchase invoices submitted", daysAgo(23));
        logCompensation(c6, CompensationStatus.PENDING, CompensationStatus.PENDING, "Compensation record created", districtAdmin);
        logCompensation(c6, CompensationStatus.PENDING, CompensationStatus.REJECTED, "Rejected by district administration", districtAdmin);

        DisasterReport r7 = createReport(citizenMain, "Canal breach flooded agricultural land",
                "Local canal breach submerged 12 acres of standing wheat crop in adjoining fields.",
                DisasterType.FLOOD, ReportStatus.APPROVED, "Sahnewal Village, Ludhiana, Punjab",
                30.9400, 76.0100, daysAgo(14));
        addReportImages(r7, 2, "flood");
        assign(r7, officer2, AssignmentStatus.COMPLETED, daysAgo(12), "Crop damage assessment");
        DamageAssessment as7 = assess(r7, officer2, DamageLevel.HIGH, "180000.00",
                "12 acres of standing wheat destroyed. Water logging still present in 4 acres.",
                "Recommend crop loss compensation at notified MSP plus pump rental.", daysAgo(10));
        addInspectionImages(as7, 2, "flood");
        Compensation c7 = compensate(as7, "160000.00",
                "Crop loss compensation as per MSP rates, pending disbursement", daysAgo(8));
        approveCompensation(c7, superAdmin, daysAgo(7));
        logCompensation(c7, CompensationStatus.PENDING, CompensationStatus.PENDING, "Compensation record created", superAdmin);
        logCompensation(c7, CompensationStatus.PENDING, CompensationStatus.APPROVED, "Approved by super admin", superAdmin);

        DisasterReport r8 = createReport(citizenMain, "Electrical short circuit fire in garage",
                "Short circuit in the garage meter box caused fire damaging a vehicle and stored scrap.",
                DisasterType.FIRE, ReportStatus.COMPLETED, "Dugri, Ludhiana, Punjab",
                30.8500, 75.8900, daysAgo(40));
        addReportImages(r8, 1, "fire");
        assign(r8, officer1, AssignmentStatus.COMPLETED, daysAgo(38), "Garage fire inspection");
        DamageAssessment as8 = assess(r8, officer1, DamageLevel.MEDIUM, "75000.00",
                "Vehicle rear and stored scrap burnt. Garage structure intact.",
                "Recommend partial reimbursement for vehicle repair.", daysAgo(36));
        Compensation c8 = compensate(as8, "55000.00",
                "Partial reimbursement approved for vehicle repair", daysAgo(34));
        approveCompensation(c8, districtAdmin, daysAgo(33));
        payCompensation(c8, districtAdmin, daysAgo(30));
        logCompensation(c8, CompensationStatus.PENDING, CompensationStatus.PENDING, "Compensation record created", districtAdmin);
        logCompensation(c8, CompensationStatus.PENDING, CompensationStatus.APPROVED, "Approved by district administration", districtAdmin);
        logCompensation(c8, CompensationStatus.APPROVED, CompensationStatus.PAID, "Payment completed", districtAdmin);

        feedback(citizenMain, r8, 4, "Assessment was fair and quick. Payment took a few days longer than expected.");
        feedback(citizenMain, r7, 5, "Officer explained the crop loss process clearly. Grateful for the timely approval.");

        notify(ravi, "Report submitted", "Your flood damage report has been received.", NotificationType.REPORT_SUBMITTED, r1.getId(), "DISASTER_REPORT");
        notify(ravi, "Officer assigned", "A field officer has been assigned to your flood report.", NotificationType.OFFICER_ASSIGNED, r1.getId(), "DISASTER_REPORT");
        notify(ravi, "Damage assessed", "Inspection completed. Damage level recorded as SEVERE.", NotificationType.DAMAGE_ASSESSED, r1.getId(), "DISASTER_REPORT");
        notify(ravi, "Compensation paid", "Rs. 4,50,000 has been transferred to your account.", NotificationType.COMPENSATION_APPROVED, c1.getId(), "COMPENSATION");
        notify(ravi, "Report submitted", "Your fire damage report is awaiting officer assignment.", NotificationType.REPORT_SUBMITTED, r3.getId(), "DISASTER_REPORT");
        notify(ravi, "Assessment under review", "Assessment for your earthquake report is under review.", NotificationType.DAMAGE_ASSESSED, r2.getId(), "DISASTER_REPORT");

        notify(priya, "Report submitted", "Your cyclone damage report has been received.", NotificationType.REPORT_SUBMITTED, r4.getId(), "DISASTER_REPORT");
        notify(priya, "Officer assigned", "A field officer has been assigned to your cyclone report.", NotificationType.OFFICER_ASSIGNED, r4.getId(), "DISASTER_REPORT");
        notify(priya, "Compensation rejected", "Your compensation claim was rejected. See remarks for details.", NotificationType.COMPENSATION_REJECTED, c6.getId(), "COMPENSATION");

        notify(citizenMain, "Report submitted", "Your crop damage report for Sahnewal has been received.", NotificationType.REPORT_SUBMITTED, r7.getId(), "DISASTER_REPORT");
        notify(citizenMain, "Compensation approved", "Rs. 1,60,000 crop compensation approved. Disbursement pending.", NotificationType.COMPENSATION_APPROVED, c7.getId(), "COMPENSATION");
        notify(citizenMain, "System notice", "Please keep your bank details updated for direct benefit transfer.", NotificationType.SYSTEM_NOTIFICATION, null, null);

        notify(officer1, "New assignment", "You have a new field inspection assignment.", NotificationType.OFFICER_ASSIGNED, null, "OFFICER_ASSIGNMENT");
        notify(officer2, "New assignment", "You have a new field inspection assignment.", NotificationType.OFFICER_ASSIGNED, null, "OFFICER_ASSIGNMENT");
        notify(districtAdmin, "Awaiting approval", "A compensation file is pending your approval.", NotificationType.SYSTEM_NOTIFICATION, c7.getId(), "COMPENSATION");
        notify(superAdmin, "System notice", "Monthly disaster summary is ready for review.", NotificationType.SYSTEM_NOTIFICATION, null, null);

        audit(ravi, AuditAction.CREATE_REPORT, "DisasterReport", r1.getId(), "Citizen submitted flood damage report");
        audit(priya, AuditAction.CREATE_REPORT, "DisasterReport", r4.getId(), "Citizen submitted cyclone damage report");
        audit(citizenMain, AuditAction.CREATE_REPORT, "DisasterReport", r7.getId(), "Citizen submitted crop damage report");
        audit(districtAdmin, AuditAction.ASSIGN_OFFICER, "OfficerAssignment", a1.getId(), "Assigned field officer for flood inspection");
        audit(officer1, AuditAction.COMPLETE_ASSESSMENT, "DamageAssessment", r1.getId(), "Completed severe damage assessment");
        audit(officer2, AuditAction.COMPLETE_ASSESSMENT, "DamageAssessment", r2.getId(), "Completed high damage assessment");
        audit(superAdmin, AuditAction.APPROVE_COMPENSATION, "Compensation", c7.getId(), "Approved compensation file");
        audit(districtAdmin, AuditAction.UPDATE_PAYMENT_STATUS, "Compensation", c1.getId(), "Payment marked completed");
        audit(citizenMain, AuditAction.SUBMIT_FEEDBACK, "Feedback", r7.getId(), "Submitted feedback on approved report");
        audit(superAdmin, AuditAction.LOGIN, "User", superAdmin.getId(), "Super admin signed in");

        log.info("Demo data seeded - reports={}, users={}", disasterReportRepository.count(), userRepository.count());
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Missing seeded user: " + email));
    }

    private User createUser(String name, String email, String phone, Gender gender, District district, String rawPassword) {
        if (userRepository.existsByEmail(email)) {
            return userRepository.findByEmail(email).orElseThrow();
        }
        User u = new User();
        u.setFullName(name);
        u.setEmail(email);
        u.setPhoneNumber(phone);
        u.setPassword(passwordEncoder.encode(rawPassword));
        u.setGender(gender);
        u.setRole(RoleType.CITIZEN);
        u.setAccountStatus(AccountStatus.ACTIVE);
        u.setDistrict(district);
        return userRepository.save(u);
    }

    private void createProfile(User user, String aadhaar, LocalDate dob, String address, String emergency) {
        if (citizenProfileRepository.findByUserId(user.getId()).isPresent()) {
            return;
        }
        CitizenProfile p = new CitizenProfile();
        p.setAadhaarNumber(aadhaar);
        p.setDateOfBirth(dob);
        p.setAddress(address);
        p.setProfilePhotoUrl("https://i.pravatar.cc/150?u=" + user.getEmail());
        p.setEmergencyContact(emergency);
        p.setUser(user);
        citizenProfileRepository.save(p);
    }

    private DisasterReport createReport(User citizen, String title, String description, DisasterType type,
                                        ReportStatus status, String address, double lat, double lng,
                                        LocalDateTime reportedAt) {
        DisasterReport r = new DisasterReport();
        r.setTitle(title);
        r.setDescription(description);
        r.setDisasterType(type);
        r.setStatus(status);
        r.setIncidentAddress(address);
        r.setLatitude(lat);
        r.setLongitude(lng);
        r.setReportedAt(reportedAt);
        r.setCitizen(citizen);
        return disasterReportRepository.save(r);
    }

    private void addReportImages(DisasterReport report, int count, String seed) {
        for (int i = 1; i <= count; i++) {
            ReportImage img = new ReportImage();
            img.setImageUrl("https://picsum.photos/seed/" + seed + report.getId() + "-" + i + "/800/600");
            img.setDisasterReport(report);
            reportImageRepository.save(img);
        }
    }

    private OfficerAssignment assign(DisasterReport report, User officer, AssignmentStatus status,
                                     LocalDateTime inspectionDate, String notes) {
        OfficerAssignment a = new OfficerAssignment();
        a.setDisasterReport(report);
        a.setFieldOfficer(officer);
        a.setInspectionDate(inspectionDate);
        a.setAssignmentStatus(status);
        a.setNotes(notes);
        return officerAssignmentRepository.save(a);
    }

    private DamageAssessment assess(DisasterReport report, User officer, DamageLevel level, String loss,
                                    String notes, String recommendation, LocalDateTime assessedAt) {
        DamageAssessment d = new DamageAssessment();
        d.setDamageLevel(level);
        d.setEstimatedLoss(new BigDecimal(loss));
        d.setAssessmentNotes(notes);
        d.setRecommendation(recommendation);
        d.setDisasterReport(report);
        d.setFieldOfficer(officer);
        d.setAssessedAt(assessedAt);
        return damageAssessmentRepository.save(d);
    }

    private void addInspectionImages(DamageAssessment assessment, int count, String seed) {
        for (int i = 1; i <= count; i++) {
            InspectionImage img = new InspectionImage();
            img.setImageUrl("https://picsum.photos/seed/insp" + assessment.getId() + "-" + i + "/800/600");
            img.setDamageAssessment(assessment);
            inspectionImageRepository.save(img);
        }
    }

    private Compensation compensate(DamageAssessment assessment, String amount, String remarks, LocalDateTime createdAt) {
        Compensation c = new Compensation();
        c.setApprovedAmount(new BigDecimal(amount));
        c.setRemarks(remarks);
        c.setDamageAssessment(assessment);
        c.setCompensationStatus(CompensationStatus.PENDING);
        c.setPaymentStatus(PaymentStatus.NOT_INITIATED);
        return compensationRepository.save(c);
    }

    private void approveCompensation(Compensation c, User admin, LocalDateTime when) {
        c.setCompensationStatus(CompensationStatus.APPROVED);
        c.setApprovedDate(when);
        c.setApprovedAt(admin);
        compensationRepository.save(c);
    }

    private void rejectCompensation(Compensation c, User admin, String reason, LocalDateTime when) {
        c.setCompensationStatus(CompensationStatus.REJECTED);
        c.setRemarks(reason);
        c.setApprovedDate(when);
        c.setApprovedAt(admin);
        compensationRepository.save(c);
    }

    private void payCompensation(Compensation c, User admin, LocalDateTime when) {
        c.setPaymentStatus(PaymentStatus.COMPLETED);
        c.setCompensationStatus(CompensationStatus.PAID);
        c.setPaidDate(when);
        c.setPaidBy(admin);
        compensationRepository.save(c);
    }

    private void logCompensation(Compensation c, CompensationStatus from, CompensationStatus to,
                                 String remarks, User by) {
        CompensationStatusLog l = new CompensationStatusLog();
        l.setPreviousStatus(from);
        l.setNewStatus(to);
        l.setRemarks(remarks);
        l.setChangedBy(by);
        l.setCompensation(c);
        compensationStatusLogRepository.save(l);
    }

    private void feedback(User citizen, DisasterReport report, int rating, String comments) {
        Feedback f = new Feedback();
        f.setRating(rating);
        f.setComments(comments);
        f.setUser(citizen);
        f.setDisasterReport(report);
        feedbackRepository.save(f);
    }

    private void notify(User user, String title, String message, NotificationType type,
                        Long refId, String entityType) {
        Notification n = new Notification();
        n.setTitle(title);
        n.setMessage(message);
        n.setNotificationType(type);
        n.setReferenceId(refId);
        n.setEntityType(entityType);
        n.setUser(user);
        notificationRepository.save(n);
    }

    private void audit(User by, AuditAction action, String entityName, Long entityId, String description) {
        AuditLog a = new AuditLog();
        a.setAction(action);
        a.setEntityName(entityName);
        a.setEntityId(entityId);
        a.setDescription(description);
        a.setIpAddress(IP);
        a.setPerformedBy(by);
        auditLogRepository.save(a);
    }

    private LocalDateTime daysAgo(int days) {
        return LocalDateTime.now().minusDays(days);
    }
}
