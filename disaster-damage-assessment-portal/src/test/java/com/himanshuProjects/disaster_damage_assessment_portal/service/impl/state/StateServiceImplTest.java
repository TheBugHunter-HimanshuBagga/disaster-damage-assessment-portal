package com.himanshuProjects.disaster_damage_assessment_portal.service.impl.state;

import com.himanshuProjects.disaster_damage_assessment_portal.dto.state.StatePageResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.state.StateRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.state.StateResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.District;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.State;
import com.himanshuProjects.disaster_damage_assessment_portal.exception.BadRequestException;
import com.himanshuProjects.disaster_damage_assessment_portal.exception.ConflictException;
import com.himanshuProjects.disaster_damage_assessment_portal.exception.ResourceNotFoundException;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.user.DistrictRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.user.StateRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.testutil.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StateServiceImplTest {

    @Mock
    private StateRepository stateRepository;
    @Mock
    private DistrictRepository districtRepository;

    private StateServiceImpl stateService;

    @BeforeEach
    void setUp() {
        stateService = new StateServiceImpl(stateRepository, districtRepository);
    }

    private State defaultState() {
        return TestDataFactory.createState(1L, "Maharashtra", "MH");
    }

    private StateRequest validStateRequest() {
        StateRequest request = new StateRequest();
        request.setName("Maharashtra");
        request.setCode("MH");
        return request;
    }

    // ==================== createState ====================

    @Test
    @DisplayName("shouldCreateStateSuccessfully")
    void shouldCreateStateSuccessfully() {
        StateRequest request = validStateRequest();
        State savedState = defaultState();
        when(stateRepository.existsByNameIgnoreCase("Maharashtra")).thenReturn(false);
        when(stateRepository.existsByCodeIgnoreCase("MH")).thenReturn(false);
        when(stateRepository.save(any(State.class))).thenAnswer(inv -> {
            State s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });
        when(districtRepository.findByStateIdOrderByIdAsc(1L)).thenReturn(Collections.emptyList());

        StateResponse response = stateService.createState(request);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Maharashtra");
        assertThat(response.getCode()).isEqualTo("MH");
        assertThat(response.getDistrictCount()).isEqualTo(0);
        verify(stateRepository).save(any(State.class));
    }

    @Test
    @DisplayName("shouldThrowConflictExceptionWhenStateNameAlreadyExists")
    void shouldThrowConflictExceptionWhenStateNameAlreadyExists() {
        StateRequest request = validStateRequest();
        when(stateRepository.existsByNameIgnoreCase("Maharashtra")).thenReturn(true);

        assertThatThrownBy(() -> stateService.createState(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("State already exists with name");

        verify(stateRepository, never()).save(any(State.class));
    }

    @Test
    @DisplayName("shouldThrowConflictExceptionWhenStateCodeAlreadyExists")
    void shouldThrowConflictExceptionWhenStateCodeAlreadyExists() {
        StateRequest request = validStateRequest();
        when(stateRepository.existsByNameIgnoreCase("Maharashtra")).thenReturn(false);
        when(stateRepository.existsByCodeIgnoreCase("MH")).thenReturn(true);

        assertThatThrownBy(() -> stateService.createState(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("State already exists with code");

        verify(stateRepository, never()).save(any(State.class));
    }

    @Test
    @DisplayName("shouldUpperCaseStateCodeOnCreate")
    void shouldUpperCaseStateCodeOnCreate() {
        StateRequest request = new StateRequest();
        request.setName("Rajasthan");
        request.setCode("rj");

        when(stateRepository.existsByNameIgnoreCase("Rajasthan")).thenReturn(false);
        when(stateRepository.existsByCodeIgnoreCase("rj")).thenReturn(false);
        when(stateRepository.save(any(State.class))).thenAnswer(inv -> {
            State s = inv.getArgument(0);
            s.setId(2L);
            return s;
        });
        when(districtRepository.findByStateIdOrderByIdAsc(2L)).thenReturn(Collections.emptyList());

        StateResponse response = stateService.createState(request);

        assertThat(response.getCode()).isEqualTo("RJ");
        verify(stateRepository).save(any(State.class));
    }

    // ==================== getStateById ====================

    @Test
    @DisplayName("shouldReturnStateById")
    void shouldReturnStateById() {
        State state = defaultState();
        when(stateRepository.findById(1L)).thenReturn(Optional.of(state));
        when(districtRepository.findByStateIdOrderByIdAsc(1L)).thenReturn(Collections.emptyList());

        StateResponse response = stateService.getStateById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Maharashtra");
        assertThat(response.getCode()).isEqualTo("MH");
    }

    @Test
    @DisplayName("shouldIncludeDistrictCountInStateResponse")
    void shouldIncludeDistrictCountInStateResponse() {
        State state = defaultState();
        District d1 = TestDataFactory.createDistrict(1L, "Pune", state);
        District d2 = TestDataFactory.createDistrict(2L, "Mumbai", state);
        when(stateRepository.findById(1L)).thenReturn(Optional.of(state));
        when(districtRepository.findByStateIdOrderByIdAsc(1L)).thenReturn(List.of(d1, d2));

        StateResponse response = stateService.getStateById(1L);

        assertThat(response.getDistrictCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenStateNotFoundById")
    void shouldThrowResourceNotFoundExceptionWhenStateNotFoundById() {
        when(stateRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stateService.getStateById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("State");
    }

    // ==================== searchStates ====================

    @Test
    @DisplayName("shouldSearchStatesWithPaginationAscending")
    void shouldSearchStatesWithPaginationAscending() {
        State state = defaultState();
        Page<State> statePage = new PageImpl<>(List.of(state), PageRequest.of(0, 10), 1);
        when(stateRepository.searchStates(null, PageRequest.of(0, 10,
                Sort.by("name").ascending())))
                .thenReturn(statePage);
        when(districtRepository.findByStateIdOrderByIdAsc(1L)).thenReturn(Collections.emptyList());

        StatePageResponse response = stateService.searchStates(null, 0, 10, "name", "asc");

        assertThat(response).isNotNull();
        assertThat(response.getStates()).hasSize(1);
        assertThat(response.getPageNumber()).isEqualTo(0);
        assertThat(response.getPageSize()).isEqualTo(10);
        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getTotalPages()).isEqualTo(1);
        assertThat(response.isLast()).isTrue();
    }

    @Test
    @DisplayName("shouldSearchStatesWithPaginationDescending")
    void shouldSearchStatesWithPaginationDescending() {
        State state = defaultState();
        Page<State> statePage = new PageImpl<>(List.of(state), PageRequest.of(0, 10), 1);
        when(stateRepository.searchStates(null, PageRequest.of(0, 10,
                Sort.by("code").descending())))
                .thenReturn(statePage);
        when(districtRepository.findByStateIdOrderByIdAsc(1L)).thenReturn(Collections.emptyList());

        StatePageResponse response = stateService.searchStates(null, 0, 10, "code", "desc");

        assertThat(response).isNotNull();
        assertThat(response.getStates()).hasSize(1);
    }

    @Test
    @DisplayName("shouldReturnEmptyPageWhenNoStatesMatchSearch")
    void shouldReturnEmptyPageWhenNoStatesMatchSearch() {
        Page<State> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
        when(stateRepository.searchStates("nomatch", PageRequest.of(0, 10,
                Sort.by("name").ascending())))
                .thenReturn(emptyPage);

        StatePageResponse response = stateService.searchStates("nomatch", 0, 10, "name", "asc");

        assertThat(response).isNotNull();
        assertThat(response.getStates()).isEmpty();
        assertThat(response.getTotalElements()).isEqualTo(0);
        assertThat(response.isLast()).isTrue();
    }

    @Test
    @DisplayName("shouldThrowBadRequestExceptionForInvalidSortFieldOnSearch")
    void shouldThrowBadRequestExceptionForInvalidSortFieldOnSearch() {
        assertThatThrownBy(() -> stateService.searchStates(null, 0, 10, "invalidField", "asc"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid sort field");
    }

    // ==================== getAllStates ====================

    @Test
    @DisplayName("shouldReturnAllStates")
    void shouldReturnAllStates() {
        State state = defaultState();
        when(stateRepository.findAll(Sort.by("name").ascending())).thenReturn(List.of(state));
        when(districtRepository.findByStateIdOrderByIdAsc(1L)).thenReturn(Collections.emptyList());

        List<StateResponse> responses = stateService.getAllStates();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getName()).isEqualTo("Maharashtra");
    }

    @Test
    @DisplayName("shouldReturnEmptyListWhenNoStatesExist")
    void shouldReturnEmptyListWhenNoStatesExist() {
        when(stateRepository.findAll(Sort.by("name").ascending())).thenReturn(Collections.emptyList());

        List<StateResponse> responses = stateService.getAllStates();

        assertThat(responses).isEmpty();
    }

    // ==================== updateState ====================

    @Test
    @DisplayName("shouldUpdateStateSuccessfully")
    void shouldUpdateStateSuccessfully() {
        State state = defaultState();
        when(stateRepository.findById(1L)).thenReturn(Optional.of(state));
        when(stateRepository.existsByNameIgnoreCaseAndIdNot("Maharashtra", 1L)).thenReturn(false);
        when(stateRepository.existsByCodeIgnoreCaseAndIdNot("MH", 1L)).thenReturn(false);
        when(stateRepository.save(any(State.class))).thenAnswer(inv -> inv.getArgument(0));
        when(districtRepository.findByStateIdOrderByIdAsc(1L)).thenReturn(Collections.emptyList());

        StateRequest request = validStateRequest();
        StateResponse response = stateService.updateState(1L, request);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Maharashtra");
        verify(stateRepository).save(any(State.class));
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenUpdatingNonExistentState")
    void shouldThrowResourceNotFoundExceptionWhenUpdatingNonExistentState() {
        when(stateRepository.findById(999L)).thenReturn(Optional.empty());

        StateRequest request = validStateRequest();

        assertThatThrownBy(() -> stateService.updateState(999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("State");

        verify(stateRepository, never()).save(any(State.class));
    }

    @Test
    @DisplayName("shouldThrowConflictExceptionOnUpdateWhenNameAlreadyExistsForAnotherState")
    void shouldThrowConflictExceptionOnUpdateWhenNameAlreadyExistsForAnotherState() {
        State state = defaultState();
        when(stateRepository.findById(1L)).thenReturn(Optional.of(state));
        when(stateRepository.existsByNameIgnoreCaseAndIdNot("Maharashtra", 1L)).thenReturn(true);

        StateRequest request = validStateRequest();

        assertThatThrownBy(() -> stateService.updateState(1L, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("State already exists with name");

        verify(stateRepository, never()).save(any(State.class));
    }

    @Test
    @DisplayName("shouldThrowConflictExceptionOnUpdateWhenCodeAlreadyExistsForAnotherState")
    void shouldThrowConflictExceptionOnUpdateWhenCodeAlreadyExistsForAnotherState() {
        State state = defaultState();
        when(stateRepository.findById(1L)).thenReturn(Optional.of(state));
        when(stateRepository.existsByNameIgnoreCaseAndIdNot("Maharashtra", 1L)).thenReturn(false);
        when(stateRepository.existsByCodeIgnoreCaseAndIdNot("MH", 1L)).thenReturn(true);

        StateRequest request = validStateRequest();

        assertThatThrownBy(() -> stateService.updateState(1L, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("State already exists with code");

        verify(stateRepository, never()).save(any(State.class));
    }

    @Test
    @DisplayName("shouldUpperCaseStateCodeOnUpdate")
    void shouldUpperCaseStateCodeOnUpdate() {
        State state = defaultState();
        when(stateRepository.findById(1L)).thenReturn(Optional.of(state));
        when(stateRepository.existsByNameIgnoreCaseAndIdNot("Maharashtra", 1L)).thenReturn(false);
        when(stateRepository.existsByCodeIgnoreCaseAndIdNot("mh", 1L)).thenReturn(false);
        when(stateRepository.save(any(State.class))).thenAnswer(inv -> inv.getArgument(0));
        when(districtRepository.findByStateIdOrderByIdAsc(1L)).thenReturn(Collections.emptyList());

        StateRequest request = new StateRequest();
        request.setName("Maharashtra");
        request.setCode("mh");

        StateResponse response = stateService.updateState(1L, request);

        assertThat(response.getCode()).isEqualTo("MH");
    }

    // ==================== deleteState ====================

    @Test
    @DisplayName("shouldDeleteStateSuccessfully")
    void shouldDeleteStateSuccessfully() {
        State state = defaultState();
        when(stateRepository.findById(1L)).thenReturn(Optional.of(state));
        when(districtRepository.findByStateIdOrderByIdAsc(1L)).thenReturn(Collections.emptyList());

        stateService.deleteState(1L);

        verify(stateRepository).delete(state);
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenDeletingNonExistentState")
    void shouldThrowResourceNotFoundExceptionWhenDeletingNonExistentState() {
        when(stateRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stateService.deleteState(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("State");

        verify(stateRepository, never()).delete(any(State.class));
    }

    @Test
    @DisplayName("shouldThrowBadRequestExceptionWhenDeletingStateWithDistricts")
    void shouldThrowBadRequestExceptionWhenDeletingStateWithDistricts() {
        State state = defaultState();
        District d1 = TestDataFactory.createDistrict(1L, "Pune", state);
        when(stateRepository.findById(1L)).thenReturn(Optional.of(state));
        when(districtRepository.findByStateIdOrderByIdAsc(1L)).thenReturn(List.of(d1));

        assertThatThrownBy(() -> stateService.deleteState(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot delete state")
                .hasMessageContaining("1 district(s)");

        verify(stateRepository, never()).delete(any(State.class));
    }
}
