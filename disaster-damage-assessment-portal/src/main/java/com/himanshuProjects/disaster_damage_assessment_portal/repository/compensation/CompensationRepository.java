package com.himanshuProjects.disaster_damage_assessment_portal.repository.compensation;

import com.himanshuProjects.disaster_damage_assessment_portal.entity.compensation.Compensation;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.CompensationStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompensationRepository extends JpaRepository<Compensation, Long> {

    boolean existsByDamageAssessmentId(Long damageAssessmentId);

    Optional<Compensation> findByDamageAssessmentId(Long damageAssessmentId);

    long countByCompensationStatus(CompensationStatus status);

    @Query("SELECT COALESCE(SUM(c.approvedAmount), 0) FROM Compensation c WHERE c.compensationStatus = :status")
    BigDecimal sumAmountByStatus(@Param("status") CompensationStatus status);

    @Query("SELECT COALESCE(AVG(c.approvedAmount), 0) FROM Compensation c WHERE c.compensationStatus = :status")
    BigDecimal avgAmountByStatus(@Param("status") CompensationStatus status);

    @Query("SELECT c.compensationStatus as status, COUNT(c) as count FROM Compensation c GROUP BY c.compensationStatus")
    List<Object[]> countGroupByStatus();

    long countByDamageAssessmentDisasterReportCitizenId(Long citizenId);

    @Query("SELECT COALESCE(SUM(c.approvedAmount), 0) FROM Compensation c " +
            "WHERE c.damageAssessment.disasterReport.citizen.id = :citizenId " +
            "AND c.compensationStatus = com.himanshuProjects.disaster_damage_assessment_portal.enums.CompensationStatus.APPROVED")
    BigDecimal sumAmountByCitizenId(@Param("citizenId") Long citizenId);

    @Query("SELECT c FROM Compensation c WHERE " +
            "(:search IS NULL OR LOWER(c.damageAssessment.disasterReport.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(c.approvedAt.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(c.damageAssessment.disasterReport.citizen.fullName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:status IS NULL OR c.compensationStatus = :status) " +
            "AND (:paymentStatus IS NULL OR c.paymentStatus = :paymentStatus)")
    Page<Compensation> searchCompensations(
            @Param("search") String search,
            @Param("status") CompensationStatus status,
            @Param("paymentStatus") PaymentStatus paymentStatus,
            Pageable pageable
    );

    @Query("SELECT c FROM Compensation c WHERE " +
            "c.damageAssessment.disasterReport.citizen.id = :citizenId")
    Page<Compensation> findByCitizenId(
            @Param("citizenId") Long citizenId,
            Pageable pageable
    );
}
