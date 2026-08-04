package com.himanshuProjects.disaster_damage_assessment_portal.repository.compensation;

import com.himanshuProjects.disaster_damage_assessment_portal.entity.compensation.Compensation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompensationRepository extends JpaRepository<Compensation, Long> {

}
