package com.himanshuProjects.disaster_damage_assessment_portal.repository.disaster;

import com.himanshuProjects.disaster_damage_assessment_portal.entity.disaster.OfficerAssignment;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.AssignmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OfficerAssignmentRepository extends JpaRepository<OfficerAssignment, Long> {

    boolean existsByDisasterReportIdAndAssignmentStatusIn(Long reportId, List<AssignmentStatus> statuses);

    Optional<OfficerAssignment> findByDisasterReportIdAndAssignmentStatusIn(
            Long reportId, List<AssignmentStatus> statuses);

    Optional<OfficerAssignment> findByDisasterReportId(Long reportId);

    List<OfficerAssignment> findByFieldOfficerIdAndAssignmentStatusIn(
            Long officerId, List<AssignmentStatus> statuses);

    long countByFieldOfficerIdAndAssignmentStatus(Long officerId, AssignmentStatus status);

    @Query("SELECT a FROM OfficerAssignment a WHERE " +
            "(:search IS NULL OR LOWER(a.disasterReport.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(a.fieldOfficer.fullName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:status IS NULL OR a.assignmentStatus = :status) " +
            "AND (:officerId IS NULL OR a.fieldOfficer.id = :officerId)")
    Page<OfficerAssignment> searchAssignments(
            @Param("search") String search,
            @Param("status") AssignmentStatus status,
            @Param("officerId") Long officerId,
            Pageable pageable
    );
}
