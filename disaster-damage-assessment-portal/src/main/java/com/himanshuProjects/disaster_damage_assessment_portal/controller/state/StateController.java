package com.himanshuProjects.disaster_damage_assessment_portal.controller.state;

import com.himanshuProjects.disaster_damage_assessment_portal.dto.state.StatePageResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.state.StateRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.state.StateResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.service.state.StateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "States", description = "State master data management")
public class StateController {

    private final StateService stateService;

    public StateController(StateService stateService) {
        this.stateService = stateService;
    }

    @PostMapping
    @Operation(summary = "Create state", description = "Creates a new state entry. Admin only.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "State created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "409", description = "State name already exists")
    })
    public ResponseEntity<StateResponse> createState(
            @Valid @RequestBody StateRequest request) {
        StateResponse response = stateService.createState(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get state by ID", description = "Returns a specific state.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "State found"),
            @ApiResponse(responseCode = "404", description = "State not found")
    })
    public ResponseEntity<StateResponse> getStateById(@PathVariable Long id) {
        StateResponse response = stateService.getStateById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Search states", description = "Search states with pagination. Supports name search.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search results returned")
    })
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
    @Operation(summary = "Get all states", description = "Returns all states without pagination.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "States returned")
    })
    public ResponseEntity<List<StateResponse>> getAllStates() {
        List<StateResponse> response = stateService.getAllStates();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update state", description = "Updates state name. Admin only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "State updated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "State not found"),
            @ApiResponse(responseCode = "409", description = "State name already exists")
    })
    public ResponseEntity<StateResponse> updateState(
            @PathVariable Long id,
            @Valid @RequestBody StateRequest request) {
        StateResponse response = stateService.updateState(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete state", description = "Deletes a state. Fails if districts are associated. Admin only.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "State deleted"),
            @ApiResponse(responseCode = "400", description = "State has associated districts"),
            @ApiResponse(responseCode = "404", description = "State not found")
    })
    public ResponseEntity<Void> deleteState(@PathVariable Long id) {
        stateService.deleteState(id);
        return ResponseEntity.noContent().build();
    }
}
