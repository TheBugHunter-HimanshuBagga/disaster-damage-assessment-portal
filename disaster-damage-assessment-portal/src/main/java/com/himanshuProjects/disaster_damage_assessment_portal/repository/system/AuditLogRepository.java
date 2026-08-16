package com.himanshuProjects.disaster_damage_assessment_portal.repository.system;

import com.himanshuProjects.disaster_damage_assessment_portal.entity.system.AuditLog;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.AuditAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByEntityNameAndEntityIdOrderByPerformedAtDesc(String entityName, Long entityId);

    List<AuditLog> findByPerformedByIdOrderByPerformedAtDesc(Long userId);

    @Query("SELECT a FROM AuditLog a WHERE " +
            "(:search IS NULL OR LOWER(a.description) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(a.entityName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(a.performedBy.fullName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:action IS NULL OR a.action = :action) " +
            "AND (:entityName IS NULL OR a.entityName = :entityName) " +
            "AND (:userEmail IS NULL OR LOWER(a.performedBy.email) = LOWER(:userEmail))")
    Page<AuditLog> searchAuditLogs(
            @Param("search") String search,
            @Param("action") AuditAction action,
            @Param("entityName") String entityName,
            @Param("userEmail") String userEmail,
            Pageable pageable
    );
}
