package com.himanshuProjects.disaster_damage_assessment_portal.controller.district;

import com.himanshuProjects.disaster_damage_assessment_portal.dto.district.DistrictPageResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.district.DistrictRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.district.DistrictResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.service.district.DistrictService;
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
public class DistrictController {

    private final DistrictService districtService;

    public DistrictController(DistrictService districtService) {
        this.districtService = districtService;
    }

    @PostMapping
    public ResponseEntity<DistrictResponse> createDistrict(
            @Valid @RequestBody DistrictRequest request) {
        DistrictResponse response = districtService.createDistrict(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DistrictResponse> getDistrictById(@PathVariable Long id) {
        DistrictResponse response = districtService.getDistrictById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
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
    public ResponseEntity<List<DistrictResponse>> getDistrictsByStateId(
            @PathVariable Long stateId) {
        List<DistrictResponse> response = districtService.getDistrictsByStateId(stateId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DistrictResponse> updateDistrict(
            @PathVariable Long id,
            @Valid @RequestBody DistrictRequest request) {
        DistrictResponse response = districtService.updateDistrict(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDistrict(@PathVariable Long id) {
        districtService.deleteDistrict(id);
        return ResponseEntity.noContent().build();
    }
}
