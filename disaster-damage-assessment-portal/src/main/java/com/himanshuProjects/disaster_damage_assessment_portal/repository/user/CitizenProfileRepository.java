package com.himanshuProjects.disaster_damage_assessment_portal.repository.user;

import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.CitizenProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CitizenProfileRepository extends JpaRepository<CitizenProfile, Long> {

    Optional<CitizenProfile> findByUserId(Long userId);
}
