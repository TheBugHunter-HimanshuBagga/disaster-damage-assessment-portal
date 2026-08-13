package com.himanshuProjects.disaster_damage_assessment_portal.repository.compensation;

import com.himanshuProjects.disaster_damage_assessment_portal.entity.compensation.CompensationStatusLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompensationStatusLogRepository extends JpaRepository<CompensationStatusLog, Long> {

    List<CompensationStatusLog> findByCompensationIdOrderByCreatedAtAsc(Long compensationId);
}
