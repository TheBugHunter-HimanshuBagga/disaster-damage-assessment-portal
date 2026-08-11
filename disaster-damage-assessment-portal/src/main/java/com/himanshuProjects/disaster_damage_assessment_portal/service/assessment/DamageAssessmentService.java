package com.himanshuProjects.disaster_damage_assessment_portal.service.assessment;

import com.himanshuProjects.disaster_damage_assessment_portal.dto.assessment.AddInspectionImagesRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.assessment.CreateDamageAssessmentRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.assessment.DamageAssessmentPageResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.assessment.DamageAssessmentResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.assessment.UpdateDamageAssessmentRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.DamageLevel;

public interface DamageAssessmentService {

    DamageAssessmentResponse submitAssessment(Long reportId, String officerEmail,
                                               CreateDamageAssessmentRequest request);

    DamageAssessmentResponse getAssessmentById(Long id);

    DamageAssessmentResponse getAssessmentByReportId(Long reportId);

    DamageAssessmentResponse updateAssessment(Long id, String officerEmail,
                                               UpdateDamageAssessmentRequest request);

    DamageAssessmentPageResponse searchAssessments(String search, DamageLevel damageLevel,
                                                    Long officerId, int page, int size,
                                                    String sortBy, String sortDirection);

    DamageAssessmentPageResponse getMyAssessments(String officerEmail, int page, int size,
                                                    String sortBy, String sortDirection);

    DamageAssessmentResponse addImages(Long assessmentId, String officerEmail,
                                        AddInspectionImagesRequest request);

    void removeImage(Long assessmentId, Long imageId, String officerEmail);

    void deleteAssessment(Long id, String officerEmail);
}
