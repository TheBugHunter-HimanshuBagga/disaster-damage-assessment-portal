package com.himanshuProjects.disaster_damage_assessment_portal.service.state;

import com.himanshuProjects.disaster_damage_assessment_portal.dto.state.StatePageResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.state.StateRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.state.StateResponse;

import java.util.List;

public interface StateService {

    StateResponse createState(StateRequest request);

    StateResponse getStateById(Long id);

    StatePageResponse searchStates(String search, int page, int size,
                                    String sortBy, String sortDirection);

    List<StateResponse> getAllStates();

    StateResponse updateState(Long id, StateRequest request);

    void deleteState(Long id);
}
