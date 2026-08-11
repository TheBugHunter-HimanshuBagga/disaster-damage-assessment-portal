package com.himanshuProjects.disaster_damage_assessment_portal.service.disaster;

import com.himanshuProjects.disaster_damage_assessment_portal.dto.disaster.AddReportImagesRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.disaster.CreateDisasterReportRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.disaster.DisasterReportPageResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.disaster.DisasterReportResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.disaster.UpdateDisasterReportRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.disaster.UpdateReportStatusRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.DisasterType;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.ReportStatus;

public interface DisasterReportService {

    DisasterReportResponse createReport(String citizenEmail, CreateDisasterReportRequest request);

    DisasterReportResponse getReportById(Long id);

    DisasterReportResponse getReportByIdForCitizen(Long id, String citizenEmail);

    DisasterReportPageResponse getMyReports(String citizenEmail, int page, int size,
                                             String sortBy, String sortDirection);

    DisasterReportPageResponse searchReports(String search, DisasterType disasterType,
                                              ReportStatus status, Long citizenId,
                                              int page, int size,
                                              String sortBy, String sortDirection);

    DisasterReportResponse updateReport(Long id, String citizenEmail,
                                         UpdateDisasterReportRequest request);

    DisasterReportResponse updateReportStatus(Long id, UpdateReportStatusRequest request);

    void deleteReport(Long id, String citizenEmail);

    DisasterReportResponse addImages(Long reportId, String citizenEmail,
                                      AddReportImagesRequest request);

    void removeImage(Long reportId, Long imageId, String citizenEmail);
}
