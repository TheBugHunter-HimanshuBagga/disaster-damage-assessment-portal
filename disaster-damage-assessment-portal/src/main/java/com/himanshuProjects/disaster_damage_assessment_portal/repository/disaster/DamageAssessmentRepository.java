package com.himanshuProjects.disaster_damage_assessment_portal.repository.disaster;

import com.himanshuProjects.disaster_damage_assessment_portal.entity.disaster.DamageAssessment;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.DamageLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DamageAssessmentRepository extends JpaRepository<DamageAssessment, Long> {

    boolean existsByDisasterReportId(Long reportId);

    Optional<DamageAssessment> findByDisasterReportId(Long reportId);

    Optional<DamageAssessment> findByFieldOfficerIdAndDisasterReportId(
            Long officerId, Long reportId);

    @Query("SELECT a FROM DamageAssessment a WHERE " +
            "(:search IS NULL OR LOWER(a.disasterReport.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(a.fieldOfficer.fullName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:damageLevel IS NULL OR a.damageLevel = :damageLevel) " +
            "AND (:officerId IS NULL OR a.fieldOfficer.id = :officerId)")
    Page<DamageAssessment> searchAssessments(
            @Param("search") String search,
            @Param("damageLevel") DamageLevel damageLevel,
            @Param("officerId") Long officerId,
            Pageable pageable
    );
}
