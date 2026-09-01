package com.himanshuProjects.disaster_damage_assessment_portal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.himanshuProjects.disaster_damage_assessment_portal.controller.state.StateController;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.state.StatePageResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.state.StateRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.state.StateResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.exception.GlobalExceptionHandler;
import com.himanshuProjects.disaster_damage_assessment_portal.service.state.StateService;
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

class StateControllerTest {

    private ObjectMapper objectMapper;
    private StateService stateService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        stateService = mock(StateService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new StateController(stateService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private StateResponse stateResponse() {
        return StateResponse.builder()
                .id(1L)
                .name("Maharashtra")
                .code("MH")
                .districtCount(35)
                .build();
    }

    @Test
    @DisplayName("should create a state and return 201")
    void shouldCreateState() throws Exception {
        when(stateService.createState(any(StateRequest.class))).thenReturn(stateResponse());

        String body = "{\"name\":\"Maharashtra\",\"code\":\"MH\"}";

        mockMvc.perform(post("/api/states")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Maharashtra"))
                .andExpect(jsonPath("$.code").value("MH"));
    }

    @Test
    @DisplayName("should get state by id")
    void shouldGetStateById() throws Exception {
        when(stateService.getStateById(1L)).thenReturn(stateResponse());

        mockMvc.perform(get("/api/states/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.districtCount").value(35));
    }

    @Test
    @DisplayName("should search states")
    void shouldSearchStates() throws Exception {
        StatePageResponse page = StatePageResponse.builder()
                .states(List.of(stateResponse()))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();
        when(stateService.searchStates(anyString(), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(page);

        mockMvc.perform(get("/api/states")
                        .param("search", "Mah"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.states[0].name").value("Maharashtra"));
    }

    @Test
    @DisplayName("should get all states")
    void shouldGetAllStates() throws Exception {
        when(stateService.getAllStates()).thenReturn(List.of(stateResponse()));

        mockMvc.perform(get("/api/states/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("MH"));
    }

    @Test
    @DisplayName("should update a state")
    void shouldUpdateState() throws Exception {
        when(stateService.updateState(eq(1L), any(StateRequest.class))).thenReturn(stateResponse());

        String body = "{\"name\":\"Maharashtra Updated\",\"code\":\"MH\"}";

        mockMvc.perform(put("/api/states/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Maharashtra"));
    }

    @Test
    @DisplayName("should delete a state and return 204")
    void shouldDeleteState() throws Exception {
        doNothing().when(stateService).deleteState(anyLong());

        mockMvc.perform(delete("/api/states/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("should return 400 when creating a state with invalid payload")
    void shouldReturn400ForInvalidCreatePayload() throws Exception {
        String body = "{\"name\":\"\",\"code\":\"\"}";

        mockMvc.perform(post("/api/states")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}