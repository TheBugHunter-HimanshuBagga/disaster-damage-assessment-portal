package com.himanshuProjects.disaster_damage_assessment_portal.repository.disaster;

import com.himanshuProjects.disaster_damage_assessment_portal.entity.disaster.DisasterReport;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.DisasterType;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DisasterReportRepository extends JpaRepository<DisasterReport, Long> {

    Page<DisasterReport> findByCitizenIdOrderByCreatedAtDesc(Long citizenId, Pageable pageable);

    @Query("SELECT r FROM DisasterReport r WHERE " +
            "(:search IS NULL OR LOWER(r.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(r.description) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(r.incidentAddress) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:disasterType IS NULL OR r.disasterType = :disasterType) " +
            "AND (:status IS NULL OR r.status = :status) " +
            "AND (:citizenId IS NULL OR r.citizen.id = :citizenId)")
    Page<DisasterReport> searchReports(
            @Param("search") String search,
            @Param("disasterType") DisasterType disasterType,
            @Param("status") ReportStatus status,
            @Param("citizenId") Long citizenId,
            Pageable pageable
    );
}
