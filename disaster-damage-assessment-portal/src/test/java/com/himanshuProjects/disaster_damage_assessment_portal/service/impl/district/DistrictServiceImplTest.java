package com.himanshuProjects.disaster_damage_assessment_portal.service.impl.district;

import com.himanshuProjects.disaster_damage_assessment_portal.dto.district.DistrictPageResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.district.DistrictRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.district.DistrictResponse;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DistrictServiceImplTest {

    @Mock
    private DistrictRepository districtRepository;
    @Mock
    private StateRepository stateRepository;

    private DistrictServiceImpl districtService;

    @BeforeEach
    void setUp() {
        districtService = new DistrictServiceImpl(districtRepository, stateRepository);
    }

    private State defaultState() {
        return TestDataFactory.createState(1L, "Maharashtra", "MH");
    }

    private District defaultDistrict() {
        return TestDataFactory.createDistrict(1L, "Pune", defaultState());
    }

    private DistrictRequest validDistrictRequest() {
        DistrictRequest request = new DistrictRequest();
        request.setName("Pune");
        request.setStateId(1L);
        return request;
    }

    // ==================== createDistrict ====================

    @Test
    @DisplayName("shouldCreateDistrictSuccessfully")
    void shouldCreateDistrictSuccessfully() {
        State state = defaultState();
        DistrictRequest request = validDistrictRequest();
        when(stateRepository.findById(1L)).thenReturn(Optional.of(state));
        when(districtRepository.existsByNameIgnoreCaseAndStateId("Pune", 1L)).thenReturn(false);
        when(districtRepository.save(any(District.class))).thenAnswer(inv -> {
            District d = inv.getArgument(0);
            d.setId(1L);
            return d;
        });

        DistrictResponse response = districtService.createDistrict(request);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Pune");
        assertThat(response.getStateId()).isEqualTo(1L);
        assertThat(response.getStateName()).isEqualTo("Maharashtra");
        verify(districtRepository).save(any(District.class));
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenStateDoesNotExistOnCreate")
    void shouldThrowResourceNotFoundExceptionWhenStateDoesNotExistOnCreate() {
        DistrictRequest request = validDistrictRequest();
        when(stateRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> {
            request.setStateId(999L);
            districtService.createDistrict(request);
        })
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("State");

        verify(districtRepository, never()).save(any(District.class));
    }

    @Test
    @DisplayName("shouldThrowConflictExceptionWhenDistrictNameAlreadyExistsInState")
    void shouldThrowConflictExceptionWhenDistrictNameAlreadyExistsInState() {
        State state = defaultState();
        DistrictRequest request = validDistrictRequest();
        when(stateRepository.findById(1L)).thenReturn(Optional.of(state));
        when(districtRepository.existsByNameIgnoreCaseAndStateId("Pune", 1L)).thenReturn(true);

        assertThatThrownBy(() -> districtService.createDistrict(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("District 'Pune' already exists in state 'Maharashtra'");

        verify(districtRepository, never()).save(any(District.class));
    }

    // ==================== getDistrictById ====================

    @Test
    @DisplayName("shouldReturnDistrictById")
    void shouldReturnDistrictById() {
        District district = defaultDistrict();
        when(districtRepository.findById(1L)).thenReturn(Optional.of(district));

        DistrictResponse response = districtService.getDistrictById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Pune");
        assertThat(response.getStateId()).isEqualTo(1L);
        assertThat(response.getStateName()).isEqualTo("Maharashtra");
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenDistrictNotFoundById")
    void shouldThrowResourceNotFoundExceptionWhenDistrictNotFoundById() {
        when(districtRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> districtService.getDistrictById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("District");
    }

    // ==================== searchDistricts ====================

    @Test
    @DisplayName("shouldSearchDistrictsWithPaginationAscending")
    void shouldSearchDistrictsWithPaginationAscending() {
        District district = defaultDistrict();
        Page<District> districtPage = new PageImpl<>(List.of(district), PageRequest.of(0, 10), 1);
        when(districtRepository.searchDistricts(null, null, PageRequest.of(0, 10,
                Sort.by("name").ascending())))
                .thenReturn(districtPage);

        DistrictPageResponse response = districtService.searchDistricts(
                null, null, 0, 10, "name", "asc");

        assertThat(response).isNotNull();
        assertThat(response.getDistricts()).hasSize(1);
        assertThat(response.getPageNumber()).isEqualTo(0);
        assertThat(response.getPageSize()).isEqualTo(10);
        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getTotalPages()).isEqualTo(1);
        assertThat(response.isLast()).isTrue();
    }

    @Test
    @DisplayName("shouldSearchDistrictsWithPaginationDescending")
    void shouldSearchDistrictsWithPaginationDescending() {
        District district = defaultDistrict();
        Page<District> districtPage = new PageImpl<>(List.of(district), PageRequest.of(0, 10), 1);
        when(districtRepository.searchDistricts(null, null, PageRequest.of(0, 10,
                Sort.by("name").descending())))
                .thenReturn(districtPage);

        DistrictPageResponse response = districtService.searchDistricts(
                null, null, 0, 10, "name", "desc");

        assertThat(response).isNotNull();
        assertThat(response.getDistricts()).hasSize(1);
    }

    @Test
    @DisplayName("shouldSearchDistrictsByStateId")
    void shouldSearchDistrictsByStateId() {
        District district = defaultDistrict();
        Page<District> districtPage = new PageImpl<>(List.of(district), PageRequest.of(0, 10), 1);
        when(districtRepository.searchDistricts(null, 1L, PageRequest.of(0, 10,
                Sort.by("name").ascending())))
                .thenReturn(districtPage);

        DistrictPageResponse response = districtService.searchDistricts(
                null, 1L, 0, 10, "name", "asc");

        assertThat(response).isNotNull();
        assertThat(response.getDistricts()).hasSize(1);
    }

    @Test
    @DisplayName("shouldReturnEmptyPageWhenNoDistrictsMatchSearch")
    void shouldReturnEmptyPageWhenNoDistrictsMatchSearch() {
        Page<District> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
        when(districtRepository.searchDistricts("nomatch", null, PageRequest.of(0, 10,
                Sort.by("name").ascending())))
                .thenReturn(emptyPage);

        DistrictPageResponse response = districtService.searchDistricts(
                "nomatch", null, 0, 10, "name", "asc");

        assertThat(response).isNotNull();
        assertThat(response.getDistricts()).isEmpty();
        assertThat(response.getTotalElements()).isEqualTo(0);
        assertThat(response.isLast()).isTrue();
    }

    @Test
    @DisplayName("shouldThrowBadRequestExceptionForInvalidSortFieldOnSearch")
    void shouldThrowBadRequestExceptionForInvalidSortFieldOnSearch() {
        assertThatThrownBy(() -> districtService.searchDistricts(
                null, null, 0, 10, "invalidField", "asc"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid sort field");
    }

    // ==================== getDistrictsByStateId ====================

    @Test
    @DisplayName("shouldReturnDistrictsByStateId")
    void shouldReturnDistrictsByStateId() {
        State state = defaultState();
        District d1 = TestDataFactory.createDistrict(1L, "Pune", state);
        District d2 = TestDataFactory.createDistrict(2L, "Mumbai", state);
        when(stateRepository.existsById(1L)).thenReturn(true);
        when(districtRepository.findByStateIdOrderByIdAsc(1L)).thenReturn(List.of(d1, d2));

        List<DistrictResponse> responses = districtService.getDistrictsByStateId(1L);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getName()).isEqualTo("Pune");
        assertThat(responses.get(1).getName()).isEqualTo("Mumbai");
    }

    @Test
    @DisplayName("shouldReturnEmptyListWhenStateHasNoDistricts")
    void shouldReturnEmptyListWhenStateHasNoDistricts() {
        when(stateRepository.existsById(1L)).thenReturn(true);
        when(districtRepository.findByStateIdOrderByIdAsc(1L)).thenReturn(Collections.emptyList());

        List<DistrictResponse> responses = districtService.getDistrictsByStateId(1L);

        assertThat(responses).isEmpty();
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenStateNotFoundForDistricts")
    void shouldThrowResourceNotFoundExceptionWhenStateNotFoundForDistricts() {
        when(stateRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> districtService.getDistrictsByStateId(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("State");
    }

    // ==================== updateDistrict ====================

    @Test
    @DisplayName("shouldUpdateDistrictSuccessfully")
    void shouldUpdateDistrictSuccessfully() {
        State state = defaultState();
        District district = defaultDistrict();
        DistrictRequest request = validDistrictRequest();
        when(districtRepository.findById(1L)).thenReturn(Optional.of(district));
        when(stateRepository.findById(1L)).thenReturn(Optional.of(state));
        when(districtRepository.existsByNameIgnoreCaseAndStateIdAndIdNot("Pune", 1L, 1L))
                .thenReturn(false);
        when(districtRepository.save(any(District.class))).thenAnswer(inv -> inv.getArgument(0));

        DistrictResponse response = districtService.updateDistrict(1L, request);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Pune");
        verify(districtRepository).save(any(District.class));
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenDistrictNotFoundOnUpdate")
    void shouldThrowResourceNotFoundExceptionWhenDistrictNotFoundOnUpdate() {
        when(districtRepository.findById(999L)).thenReturn(Optional.empty());

        DistrictRequest request = validDistrictRequest();

        assertThatThrownBy(() -> districtService.updateDistrict(999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("District");

        verify(districtRepository, never()).save(any(District.class));
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenStateNotFoundOnUpdate")
    void shouldThrowResourceNotFoundExceptionWhenStateNotFoundOnUpdate() {
        District district = defaultDistrict();
        when(districtRepository.findById(1L)).thenReturn(Optional.of(district));
        when(stateRepository.findById(999L)).thenReturn(Optional.empty());

        DistrictRequest request = new DistrictRequest();
        request.setName("Pune");
        request.setStateId(999L);

        assertThatThrownBy(() -> districtService.updateDistrict(1L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("State");

        verify(districtRepository, never()).save(any(District.class));
    }

    @Test
    @DisplayName("shouldThrowConflictExceptionOnUpdateWhenDistrictNameAlreadyExistsInState")
    void shouldThrowConflictExceptionOnUpdateWhenDistrictNameAlreadyExistsInState() {
        State state = defaultState();
        District district = defaultDistrict();
        when(districtRepository.findById(1L)).thenReturn(Optional.of(district));
        when(stateRepository.findById(1L)).thenReturn(Optional.of(state));
        when(districtRepository.existsByNameIgnoreCaseAndStateIdAndIdNot("Pune", 1L, 1L))
                .thenReturn(true);

        DistrictRequest request = validDistrictRequest();

        assertThatThrownBy(() -> districtService.updateDistrict(1L, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("District 'Pune' already exists in state 'Maharashtra'");

        verify(districtRepository, never()).save(any(District.class));
    }

    // ==================== deleteDistrict ====================

    @Test
    @DisplayName("shouldDeleteDistrictSuccessfully")
    void shouldDeleteDistrictSuccessfully() {
        District district = defaultDistrict();
        when(districtRepository.findById(1L)).thenReturn(Optional.of(district));

        districtService.deleteDistrict(1L);

        verify(districtRepository).delete(district);
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenDeletingNonExistentDistrict")
    void shouldThrowResourceNotFoundExceptionWhenDeletingNonExistentDistrict() {
        when(districtRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> districtService.deleteDistrict(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("District");

        verify(districtRepository, never()).delete(any(District.class));
    }
}
