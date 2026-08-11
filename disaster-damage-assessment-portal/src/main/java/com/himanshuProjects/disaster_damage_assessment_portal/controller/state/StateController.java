package com.himanshuProjects.disaster_damage_assessment_portal.controller.state;

import com.himanshuProjects.disaster_damage_assessment_portal.dto.state.StatePageResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.state.StateRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.state.StateResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.service.state.StateService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/states")
public class StateController {

    private final StateService stateService;

    public StateController(StateService stateService) {
        this.stateService = stateService;
    }

    @PostMapping
    public ResponseEntity<StateResponse> createState(
            @Valid @RequestBody StateRequest request) {
        StateResponse response = stateService.createState(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StateResponse> getStateById(@PathVariable Long id) {
        StateResponse response = stateService.getStateById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<StatePageResponse> searchStates(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {

        StatePageResponse response = stateService.searchStates(
                search, page, size, sortBy, sortDirection);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    public ResponseEntity<List<StateResponse>> getAllStates() {
        List<StateResponse> response = stateService.getAllStates();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<StateResponse> updateState(
            @PathVariable Long id,
            @Valid @RequestBody StateRequest request) {
        StateResponse response = stateService.updateState(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteState(@PathVariable Long id) {
        stateService.deleteState(id);
        return ResponseEntity.noContent().build();
    }
}
