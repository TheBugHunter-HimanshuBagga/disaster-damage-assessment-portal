package com.himanshuProjects.disaster_damage_assessment_portal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.himanshuProjects.disaster_damage_assessment_portal.controller.district.DistrictController;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.district.DistrictPageResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.district.DistrictRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.district.DistrictResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.exception.GlobalExceptionHandler;
import com.himanshuProjects.disaster_damage_assessment_portal.service.district.DistrictService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DistrictControllerTest {

    private ObjectMapper objectMapper;
    private DistrictService districtService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        districtService = mock(DistrictService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new DistrictController(districtService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private DistrictResponse districtResponse() {
        return DistrictResponse.builder()
                .id(1L)
                .name("Pune")
                .stateId(1L)
                .stateName("Maharashtra")
                .build();
    }

    @Test
    @DisplayName("should create a district and return 201")
    void shouldCreateDistrict() throws Exception {
        when(districtService.createDistrict(any(DistrictRequest.class))).thenReturn(districtResponse());

        String body = "{\"name\":\"Pune\",\"stateId\":1}";

        mockMvc.perform(post("/api/districts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Pune"))
                .andExpect(jsonPath("$.stateName").value("Maharashtra"));
    }

    @Test
    @DisplayName("should get district by id")
    void shouldGetDistrictById() throws Exception {
        when(districtService.getDistrictById(1L)).thenReturn(districtResponse());

        mockMvc.perform(get("/api/districts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.stateId").value(1));
    }

    @Test
    @DisplayName("should search districts")
    void shouldSearchDistricts() throws Exception {
        DistrictPageResponse page = DistrictPageResponse.builder()
                .districts(List.of(districtResponse()))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();
        when(districtService.searchDistricts(any(), any(), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(page);

        mockMvc.perform(get("/api/districts")
                        .param("search", "Pune"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.districts[0].name").value("Pune"));
    }

    @Test
    @DisplayName("should get districts by state id")
    void shouldGetDistrictsByStateId() throws Exception {
        when(districtService.getDistrictsByStateId(1L)).thenReturn(List.of(districtResponse()));

        mockMvc.perform(get("/api/districts/by-state/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Pune"));
    }

    @Test
    @DisplayName("should update a district")
    void shouldUpdateDistrict() throws Exception {
        when(districtService.updateDistrict(eq(1L), any(DistrictRequest.class))).thenReturn(districtResponse());

        String body = "{\"name\":\"Pune Updated\",\"stateId\":1}";

        mockMvc.perform(put("/api/districts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Pune"));
    }

    @Test
    @DisplayName("should delete a district and return 204")
    void shouldDeleteDistrict() throws Exception {
        doNothing().when(districtService).deleteDistrict(anyLong());

        mockMvc.perform(delete("/api/districts/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("should return 400 when creating a district with invalid payload")
    void shouldReturn400ForInvalidCreatePayload() throws Exception {
        String body = "{\"name\":\"\",\"stateId\":null}";

        mockMvc.perform(post("/api/districts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}