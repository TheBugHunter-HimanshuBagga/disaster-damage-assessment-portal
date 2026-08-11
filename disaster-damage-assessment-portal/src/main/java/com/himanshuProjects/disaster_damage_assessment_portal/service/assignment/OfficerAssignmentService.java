package com.himanshuProjects.disaster_damage_assessment_portal.service.assignment;

import com.himanshuProjects.disaster_damage_assessment_portal.dto.assignment.AssignmentPageResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.assignment.AssignOfficerRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.assignment.OfficerAssignmentResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.assignment.UpdateAssignmentStatusRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.AssignmentStatus;

public interface OfficerAssignmentService {

    OfficerAssignmentResponse assignOfficer(Long reportId, AssignOfficerRequest request);

    OfficerAssignmentResponse getAssignmentById(Long id);

    OfficerAssignmentResponse getAssignmentByReportId(Long reportId);

    OfficerAssignmentResponse updateAssignmentStatus(Long id, String officerEmail,
                                                      UpdateAssignmentStatusRequest request);

    AssignmentPageResponse searchAssignments(String search, AssignmentStatus status,
                                              Long officerId, int page, int size,
                                              String sortBy, String sortDirection);

    AssignmentPageResponse getMyAssignments(String officerEmail, AssignmentStatus status,
                                             int page, int size,
                                             String sortBy, String sortDirection);

    OfficerAssignmentResponse reassignOfficer(Long assignmentId, AssignOfficerRequest request);
}
