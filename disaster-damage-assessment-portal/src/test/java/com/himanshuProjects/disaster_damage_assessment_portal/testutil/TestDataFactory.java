package com.himanshuProjects.disaster_damage_assessment_portal.testutil;

import com.himanshuProjects.disaster_damage_assessment_portal.dto.auth.RegisterRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.compensation.Compensation;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.compensation.CompensationStatusLog;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.disaster.DamageAssessment;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.disaster.DisasterReport;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.disaster.OfficerAssignment;
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

import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class TestDataFactory {

    private TestDataFactory() {
    }

    public static State createState(Long id, String name, String code) {
        State state = new State();
        state.setId(id);
        state.setName(name);
        state.setCode(code);
        return state;
    }

    public static District createDistrict(Long id, String name, State state) {
        District district = new District();
        district.setId(id);
        district.setName(name);
        district.setState(state);
        return district;
    }

    public static User createUser(Long id, String email, RoleType role) {
        User user = new User();
        user.setId(id);
        user.setFullName("Test " + role.name());
        user.setEmail(email);
        user.setPhoneNumber("9876543210");
        user.setPassword("$2a$10$encodedPassword");
        user.setGender(Gender.MALE);
        user.setRole(role);
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setDistrict(createDistrict(1L, "TestDistrict", createState(1L, "TestState", "TS")));
        return user;
    }

    public static User createCitizen(Long id, String email) {
        return createUser(id, email, RoleType.CITIZEN);
    }

    public static User createOfficer(Long id, String email) {
        return createUser(id, email, RoleType.FIELD_OFFICER);
    }

    public static User createDistrictAdmin(Long id, String email) {
        return createUser(id, email, RoleType.DISTRICT_ADMIN);
    }

    public static User createSuperAdmin(Long id, String email) {
        return createUser(id, email, RoleType.SUPER_ADMIN);
    }

    public static CitizenProfile createCitizenProfile(Long id, User user) {
        CitizenProfile profile = new CitizenProfile();
        profile.setId(id);
        profile.setAadhaarNumber("123456789012");
        profile.setDateOfBirth(LocalDate.of(1990, 1, 1));
        profile.setAddress("123 Test Street");
        profile.setProfilePhotoUrl("http://example.com/photo.jpg");
        profile.setEmergencyContact("9123456789");
        profile.setUser(user);
        return profile;
    }

    public static DisasterReport createReport(Long id, User citizen, ReportStatus status) {
        DisasterReport report = new DisasterReport();
        report.setId(id);
        report.setTitle("Flood report");
        report.setDescription("Severe flooding in the area");
        report.setDisasterType(DisasterType.FLOOD);
        report.setStatus(status);
        report.setIncidentAddress("Test Incident Address");
        report.setLatitude(12.34);
        report.setLongitude(56.78);
        report.setReportedAt(LocalDateTime.now());
        report.setCitizen(citizen);
        return report;
    }

    public static OfficerAssignment createAssignment(Long id, DisasterReport report, User officer,
                                                     AssignmentStatus status) {
        OfficerAssignment assignment = new OfficerAssignment();
        assignment.setId(id);
        assignment.setDisasterReport(report);
        assignment.setFieldOfficer(officer);
        assignment.setInspectionDate(LocalDateTime.now().plusDays(1));
        assignment.setAssignmentStatus(status);
        assignment.setNotes("Inspect the site");
        assignment.setAssignedAt(LocalDateTime.now());
        return assignment;
    }

    public static DamageAssessment createAssessment(Long id, DisasterReport report, User officer,
                                                    DamageLevel level, BigDecimal loss) {
        DamageAssessment assessment = new DamageAssessment();
        assessment.setId(id);
        assessment.setDisasterReport(report);
        assessment.setFieldOfficer(officer);
        assessment.setDamageLevel(level);
        assessment.setEstimatedLoss(loss);
        assessment.setAssessmentNotes("Significant structural damage observed.");
        assessment.setRecommendation("Recommended for compensation.");
        assessment.setAssessedAt(LocalDateTime.now());
        return assessment;
    }

    public static Compensation createCompensation(Long id, DamageAssessment assessment, User admin,
                                                  CompensationStatus status, BigDecimal amount) {
        Compensation compensation = new Compensation();
        compensation.setId(id);
        compensation.setApprovedAmount(amount);
        compensation.setCompensationStatus(status);
        compensation.setRemarks("Initial compensation");
        compensation.setDamageAssessment(assessment);
        compensation.setPaymentStatus(PaymentStatus.NOT_INITIATED);
        return compensation;
    }

    public static CompensationStatusLog createStatusLog(Long id, Compensation compensation, User changedBy,
                                                        CompensationStatus prev, CompensationStatus next) {
        CompensationStatusLog log = new CompensationStatusLog();
        log.setId(id);
        log.setCompensation(compensation);
        log.setChangedBy(changedBy);
        log.setPreviousStatus(prev);
        log.setNewStatus(next);
        log.setRemarks("Status changed");
        return log;
    }

    public static Notification createNotification(Long id, User user, boolean read) {
        Notification notification = new Notification();
        notification.setId(id);
        notification.setUser(user);
        notification.setTitle("Test Notification");
        notification.setMessage("Test message");
        notification.setNotificationType(NotificationType.SYSTEM_NOTIFICATION);
        notification.setIsRead(read);
        notification.setReferenceId(1L);
        notification.setEntityType("DISASTER_REPORT");
        return notification;
    }

    public static Feedback createFeedback(Long id, User citizen, DisasterReport report, Integer rating) {
        Feedback feedback = new Feedback();
        feedback.setId(id);
        feedback.setUser(citizen);
        feedback.setDisasterReport(report);
        feedback.setRating(rating);
        feedback.setComments("Good service");
        feedback.setSubmittedAt(LocalDateTime.now());
        return feedback;
    }

    public static AuditLog createAuditLog(Long id, User user, AuditAction action, String entity, Long entityId) {
        AuditLog auditLog = new AuditLog();
        auditLog.setId(id);
        auditLog.setPerformedBy(user);
        auditLog.setAction(action);
        auditLog.setEntityName(entity);
        auditLog.setEntityId(entityId);
        auditLog.setDescription("Test audit description");
        auditLog.setIpAddress("127.0.0.1");
        auditLog.setPerformedAt(LocalDateTime.now());
        return auditLog;
    }

    public static State persistState(EntityManager em, String name, String code) {
        State state = createState(null, name, code);
        em.persist(state);
        em.flush();
        return state;
    }

    public static District persistDistrict(EntityManager em, String name, State state) {
        District district = createDistrict(null, name, state);
        em.persist(district);
        em.flush();
        return district;
    }

    public static User persistUser(EntityManager em, Long id, String email, RoleType role, District district) {
        User user = createUser(id, email, role);
        user.setDistrict(district);
        em.persist(user);
        em.flush();
        return user;
    }

    public static CitizenProfile persistCitizenProfile(EntityManager em, User user) {
        CitizenProfile profile = createCitizenProfile(null, user);
        profile.setAadhaarNumber("12345678901" + (int)(Math.random() * 9));
        em.persist(profile);
        em.flush();
        return profile;
    }

    public static DisasterReport persistReport(EntityManager em, Long id, User citizen, ReportStatus status) {
        DisasterReport report = createReport(id, citizen, status);
        em.persist(report);
        em.flush();
        return report;
    }

    public static OfficerAssignment persistAssignment(EntityManager em, DisasterReport report, User officer, AssignmentStatus status) {
        OfficerAssignment assignment = createAssignment(null, report, officer, status);
        em.persist(assignment);
        em.flush();
        return assignment;
    }

    public static DamageAssessment persistAssessment(EntityManager em, DisasterReport report, User officer, DamageLevel level, BigDecimal loss) {
        DamageAssessment assessment = createAssessment(null, report, officer, level, loss);
        em.persist(assessment);
        em.flush();
        return assessment;
    }

    public static Compensation persistCompensation(EntityManager em, DamageAssessment assessment, User admin, CompensationStatus status, BigDecimal amount) {
        Compensation compensation = createCompensation(null, assessment, admin, status, amount);
        em.persist(compensation);
        em.flush();
        return compensation;
    }

    public static Notification persistNotification(EntityManager em, User user, boolean read) {
        Notification notification = createNotification(null, user, read);
        em.persist(notification);
        em.flush();
        return notification;
    }

    public static Feedback persistFeedback(EntityManager em, User citizen, DisasterReport report, Integer rating) {
        Feedback feedback = createFeedback(null, citizen, report, rating);
        em.persist(feedback);
        em.flush();
        return feedback;
    }

    public static AuditLog persistAuditLog(EntityManager em, User user, AuditAction action, String entity, Long entityId) {
        AuditLog auditLog = createAuditLog(null, user, action, entity, entityId);
        em.persist(auditLog);
        em.flush();
        return auditLog;
    }

    public static RegisterRequest createRegisterRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("John Doe");
        request.setEmail("john.doe@example.com");
        request.setPhoneNumber("9876543210");
        request.setPassword("password123");
        request.setGender(Gender.MALE);
        request.setDistrictId(1L);
        return request;
    }
}
