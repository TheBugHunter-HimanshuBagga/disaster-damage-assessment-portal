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
import com.himanshuProjects.disaster_damage_assessment_portal.service.state.StateService;
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
public class StateServiceImpl implements StateService {

    private static final Logger log = LoggerFactory.getLogger(StateServiceImpl.class);

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "name", "code", "createdAt", "updatedAt"
    );

    private final StateRepository stateRepository;
    private final DistrictRepository districtRepository;

    public StateServiceImpl(StateRepository stateRepository,
                            DistrictRepository districtRepository) {
        this.stateRepository = stateRepository;
        this.districtRepository = districtRepository;
    }

    @Override
    @Transactional
    public StateResponse createState(StateRequest request) {
        log.info("Creating state: {}", request.getName());

        if (stateRepository.existsByNameIgnoreCase(request.getName())) {
            throw new ConflictException("State already exists with name: " + request.getName());
        }

        if (stateRepository.existsByCodeIgnoreCase(request.getCode())) {
            throw new ConflictException("State already exists with code: " + request.getCode());
        }

        State state = new State();
        state.setName(request.getName());
        state.setCode(request.getCode().toUpperCase());

        State savedState = stateRepository.save(state);
        log.info("State created successfully: {} (ID: {})", savedState.getName(), savedState.getId());
        return mapToResponse(savedState);
    }

    @Override
    @Transactional(readOnly = true)
    public StateResponse getStateById(Long id) {
        log.info("Fetching state by ID: {}", id);
        State state = stateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("State", "id", id));
        return mapToResponse(state);
    }

    @Override
    @Transactional(readOnly = true)
    public StatePageResponse searchStates(String search, int page, int size,
                                           String sortBy, String sortDirection) {
        log.info("Searching states - search: {}, page: {}, size: {}, sortBy: {}, sortDir: {}",
                search, page, size, sortBy, sortDirection);

        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new BadRequestException("Invalid sort field: " + sortBy
                    + ". Allowed fields: " + ALLOWED_SORT_FIELDS);
        }

        Sort sort = sortDirection.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<State> statePage = stateRepository.searchStates(search, pageable);

        List<StateResponse> states = statePage.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return StatePageResponse.builder()
                .states(states)
                .pageNumber(statePage.getNumber())
                .pageSize(statePage.getSize())
                .totalElements(statePage.getTotalElements())
                .totalPages(statePage.getTotalPages())
                .last(statePage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StateResponse> getAllStates() {
        log.info("Fetching all states");
        return stateRepository.findAll(Sort.by("name").ascending()).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public StateResponse updateState(Long id, StateRequest request) {
        log.info("Updating state ID: {} with name: {}", id, request.getName());

        State state = stateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("State", "id", id));

        if (stateRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), id)) {
            throw new ConflictException("State already exists with name: " + request.getName());
        }

        if (stateRepository.existsByCodeIgnoreCaseAndIdNot(request.getCode(), id)) {
            throw new ConflictException("State already exists with code: " + request.getCode());
        }

        state.setName(request.getName());
        state.setCode(request.getCode().toUpperCase());

        State updatedState = stateRepository.save(state);
        log.info("State updated successfully: {}", updatedState.getName());
        return mapToResponse(updatedState);
    }

    @Override
    @Transactional
    public void deleteState(Long id) {
        log.info("Deleting state ID: {}", id);

        State state = stateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("State", "id", id));

        List<District> districts = districtRepository.findByStateIdOrderByAsc(id);
        if (!districts.isEmpty()) {
            throw new BadRequestException(
                    "Cannot delete state '" + state.getName()
                            + "' because it has " + districts.size() + " district(s). "
                            + "Delete all districts first.");
        }

        stateRepository.delete(state);
        log.info("State deleted successfully: {} (ID: {})", state.getName(), id);
    }

    private StateResponse mapToResponse(State state) {
        List<District> districts = districtRepository.findByStateIdOrderByAsc(state.getId());
        return StateResponse.builder()
                .id(state.getId())
                .name(state.getName())
                .code(state.getCode())
                .districtCount(districts.size())
                .createdAt(state.getCreatedAt())
                .updatedAt(state.getUpdatedAt())
                .build();
    }
}
