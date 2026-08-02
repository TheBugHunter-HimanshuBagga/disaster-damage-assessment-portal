package com.himanshuProjects.disaster_damage_assessment_portal.repository.user;

import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.District;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DistrictRepository extends JpaRepository<District, Long> {

}
