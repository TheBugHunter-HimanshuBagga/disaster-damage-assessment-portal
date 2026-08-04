package com.himanshuProjects.disaster_damage_assessment_portal.repository.system;

import com.himanshuProjects.disaster_damage_assessment_portal.entity.system.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

}
