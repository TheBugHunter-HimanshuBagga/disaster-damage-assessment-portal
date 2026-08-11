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
import com.himanshuProjects.disaster_damage_assessment_portal.service.district.DistrictService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class DistrictServiceImpl implements DistrictService {

    private static final Logger log = LoggerFactory.getLogger(DistrictServiceImpl.class);

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "name", "createdAt", "updatedAt"
    );

    private final DistrictRepository districtRepository;
    private final StateRepository stateRepository;

    public DistrictServiceImpl(DistrictRepository districtRepository,
                               StateRepository stateRepository) {
        this.districtRepository = districtRepository;
        this.stateRepository = stateRepository;
    }

    @Override
    @Transactional
    public DistrictResponse createDistrict(DistrictRequest request) {
        log.info("Creating district: {} in state ID: {}", request.getName(), request.getStateId());

        State state = stateRepository.findById(request.getStateId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "State", "id", request.getStateId()));

        if (districtRepository.existsByNameIgnoreCaseAndStateId(request.getName(), request.getStateId())) {
            throw new ConflictException(
                    "District '" + request.getName() + "' already exists in state '" + state.getName() + "'");
        }

        District district = new District();
        district.setName(request.getName());
        district.setState(state);

        District savedDistrict = districtRepository.save(district);
        log.info("District created successfully: {} (ID: {})", savedDistrict.getName(), savedDistrict.getId());
        return mapToResponse(savedDistrict);
    }

    @Override
    @Transactional(readOnly = true)
    public DistrictResponse getDistrictById(Long id) {
        log.info("Fetching district by ID: {}", id);
        District district = districtRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("District", "id", id));
        return mapToResponse(district);
    }

    @Override
    @Transactional(readOnly = true)
    public DistrictPageResponse searchDistricts(String search, Long stateId,
                                                 int page, int size,
                                                 String sortBy, String sortDirection) {
        log.info("Searching districts - search: {}, stateId: {}, page: {}, size: {}, sortBy: {}, sortDir: {}",
                search, stateId, page, size, sortBy, sortDirection);

        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new BadRequestException("Invalid sort field: " + sortBy
                    + ". Allowed fields: " + ALLOWED_SORT_FIELDS);
        }

        Sort sort = sortDirection.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<District> districtPage = districtRepository.searchDistricts(search, stateId, pageable);

        List<DistrictResponse> districts = districtPage.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return DistrictPageResponse.builder()
                .districts(districts)
                .pageNumber(districtPage.getNumber())
                .pageSize(districtPage.getSize())
                .totalElements(districtPage.getTotalElements())
                .totalPages(districtPage.getTotalPages())
                .last(districtPage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DistrictResponse> getDistrictsByStateId(Long stateId) {
        log.info("Fetching all districts for state ID: {}", stateId);

        if (!stateRepository.existsById(stateId)) {
            throw new ResourceNotFoundException("State", "id", stateId);
        }

        return districtRepository.findByStateIdOrderByAsc(stateId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public DistrictResponse updateDistrict(Long id, DistrictRequest request) {
        log.info("Updating district ID: {} with name: {} in state ID: {}",
                id, request.getName(), request.getStateId());

        District district = districtRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("District", "id", id));

        State state = stateRepository.findById(request.getStateId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "State", "id", request.getStateId()));

        if (districtRepository.existsByNameIgnoreCaseAndStateIdAndIdNot(
                request.getName(), request.getStateId(), id)) {
            throw new ConflictException(
                    "District '" + request.getName() + "' already exists in state '" + state.getName() + "'");
        }

        district.setName(request.getName());
        district.setState(state);

        District updatedDistrict = districtRepository.save(district);
        log.info("District updated successfully: {}", updatedDistrict.getName());
        return mapToResponse(updatedDistrict);
    }

    @Override
    @Transactional
    public void deleteDistrict(Long id) {
        log.info("Deleting district ID: {}", id);

        District district = districtRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("District", "id", id));

        districtRepository.delete(district);
        log.info("District deleted successfully: {} (ID: {})", district.getName(), id);
    }

    private DistrictResponse mapToResponse(District district) {
        return DistrictResponse.builder()
                .id(district.getId())
                .name(district.getName())
                .stateId(district.getState().getId())
                .stateName(district.getState().getName())
                .createdAt(district.getCreatedAt())
                .updatedAt(district.getUpdatedAt())
                .build();
    }
}
