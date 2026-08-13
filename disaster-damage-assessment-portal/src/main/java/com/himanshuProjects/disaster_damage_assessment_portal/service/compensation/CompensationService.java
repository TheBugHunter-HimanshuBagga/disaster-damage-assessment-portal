package com.himanshuProjects.disaster_damage_assessment_portal.service.compensation;

import com.himanshuProjects.disaster_damage_assessment_portal.dto.compensation.ApproveCompensationRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.compensation.CompensationHistoryResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.compensation.CompensationPageResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.compensation.CompensationResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.compensation.CreateCompensationRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.compensation.RejectCompensationRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.compensation.UpdateCompensationRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.compensation.UpdatePaymentStatusRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.CompensationStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.PaymentStatus;

import java.util.List;

public interface CompensationService {

    CompensationResponse createCompensation(String adminEmail, CreateCompensationRequest request);

    CompensationResponse getCompensationById(Long id);

    CompensationResponse updateCompensation(Long id, String adminEmail, UpdateCompensationRequest request);

    CompensationResponse approveCompensation(Long id, String adminEmail, ApproveCompensationRequest request);

    CompensationResponse rejectCompensation(Long id, String adminEmail, RejectCompensationRequest request);

    CompensationResponse updatePaymentStatus(Long id, String adminEmail, UpdatePaymentStatusRequest request);

    CompensationPageResponse searchCompensations(String search, CompensationStatus status,
                                                  PaymentStatus paymentStatus, int page, int size,
                                                  String sortBy, String sortDirection);

    CompensationPageResponse getMyCompensations(String citizenEmail, int page, int size,
                                                 String sortBy, String sortDirection);

    List<CompensationHistoryResponse> getCompensationHistory(Long compensationId);

    void deleteCompensation(Long id, String adminEmail);
}
