package com.himanshuProjects.disaster_damage_assessment_portal.service.district;

import com.himanshuProjects.disaster_damage_assessment_portal.dto.district.DistrictPageResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.district.DistrictRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.district.DistrictResponse;

import java.util.List;

public interface DistrictService {

    DistrictResponse createDistrict(DistrictRequest request);

    DistrictResponse getDistrictById(Long id);

    DistrictPageResponse searchDistricts(String search, Long stateId,
                                          int page, int size,
                                          String sortBy, String sortDirection);

    List<DistrictResponse> getDistrictsByStateId(Long stateId);

    DistrictResponse updateDistrict(Long id, DistrictRequest request);

    void deleteDistrict(Long id);
}
