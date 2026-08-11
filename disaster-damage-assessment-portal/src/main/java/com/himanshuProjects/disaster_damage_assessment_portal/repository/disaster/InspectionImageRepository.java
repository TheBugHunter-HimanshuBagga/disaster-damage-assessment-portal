package com.himanshuProjects.disaster_damage_assessment_portal.repository.disaster;

import com.himanshuProjects.disaster_damage_assessment_portal.entity.disaster.InspectionImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InspectionImageRepository extends JpaRepository<InspectionImage, Long> {

    List<InspectionImage> findByDamageAssessmentIdOrderByUploadedAtAsc(Long assessmentId);

    long countByDamageAssessmentId(Long assessmentId);

    void deleteByDamageAssessmentId(Long assessmentId);
}
