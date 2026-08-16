package com.himanshuProjects.disaster_damage_assessment_portal.controller.district;

import com.himanshuProjects.disaster_damage_assessment_portal.dto.district.DistrictPageResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.district.DistrictRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.district.DistrictResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.service.district.DistrictService;
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
@RequestMapping("/api/districts")
@Tag(name = "Districts", description = "District master data management")
public class DistrictController {

    private final DistrictService districtService;

    public DistrictController(DistrictService districtService) {
        this.districtService = districtService;
    }

    @PostMapping
    @Operation(summary = "Create district", description = "Creates a new district under a state. Admin only.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "District created"),
            @ApiResponse(responseCode = "400", description = "Validation error or state not found"),
            @ApiResponse(responseCode = "409", description = "District name already exists in this state")
    })
    public ResponseEntity<DistrictResponse> createDistrict(
            @Valid @RequestBody DistrictRequest request) {
        DistrictResponse response = districtService.createDistrict(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get district by ID", description = "Returns a specific district with state info.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "District found"),
            @ApiResponse(responseCode = "404", description = "District not found")
    })
    public ResponseEntity<DistrictResponse> getDistrictById(@PathVariable Long id) {
        DistrictResponse response = districtService.getDistrictById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Search districts", description = "Search and filter districts by state. Supports pagination.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search results returned")
    })
    public ResponseEntity<DistrictPageResponse> searchDistricts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long stateId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {

        DistrictPageResponse response = districtService.searchDistricts(
                search, stateId, page, size, sortBy, sortDirection);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-state/{stateId}")
    @Operation(summary = "Get districts by state", description = "Returns all districts for a specific state.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Districts returned"),
            @ApiResponse(responseCode = "404", description = "State not found")
    })
    public ResponseEntity<List<DistrictResponse>> getDistrictsByStateId(
            @PathVariable Long stateId) {
        List<DistrictResponse> response = districtService.getDistrictsByStateId(stateId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update district", description = "Updates district name. Admin only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "District updated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "District not found"),
            @ApiResponse(responseCode = "409", description = "District name already exists")
    })
    public ResponseEntity<DistrictResponse> updateDistrict(
            @PathVariable Long id,
            @Valid @RequestBody DistrictRequest request) {
        DistrictResponse response = districtService.updateDistrict(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete district", description = "Deletes a district. Fails if users are associated. Admin only.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "District deleted"),
            @ApiResponse(responseCode = "400", description = "District has associated users"),
            @ApiResponse(responseCode = "404", description = "District not found")
    })
    public ResponseEntity<Void> deleteDistrict(@PathVariable Long id) {
        districtService.deleteDistrict(id);
        return ResponseEntity.noContent().build();
    }
}
