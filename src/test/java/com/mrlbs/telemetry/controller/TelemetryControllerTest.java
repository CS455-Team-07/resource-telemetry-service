package com.mrlbs.telemetry.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TelemetryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetAllResources_Catalog() throws Exception {
        mockMvc.perform(get("/api/telemetry/resources")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].externalResourceId").value("gpu-node-01"))
                .andExpect(jsonPath("$[0].healthStatus").value("HEALTHY"))
                .andExpect(jsonPath("$[0].historicalSamplesCount").value(11))
                .andExpect(jsonPath("$[1].externalResourceId").value("gpu-node-02"))
                .andExpect(jsonPath("$[1].healthStatus").value("DEGRADED"))
                .andExpect(jsonPath("$[1].historicalSamplesCount").value(8));
    }

    @Test
    void testGetHealth_Success() throws Exception {
        mockMvc.perform(get("/api/telemetry/resources/gpu-node-01/health")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.externalResourceId").value("gpu-node-01"))
                .andExpect(jsonPath("$.healthStatus").value("HEALTHY"))
                .andExpect(jsonPath("$.gpuUtilizationPct").value(42.5))
                .andExpect(jsonPath("$.gpuMemoryUtilizationPct").value(65.0))
                .andExpect(jsonPath("$.temperatureCelsius").value(68.0))
                .andExpect(jsonPath("$.powerDrawWatts").value(240.0))
                .andExpect(jsonPath("$.dataSource").value("DEMO_DETERMINISTIC_FIXTURE"))
                .andExpect(jsonPath("$.isDemonstrationData").value(true));
    }

    @Test
    void testGetHealth_NotFound() throws Exception {
        mockMvc.perform(get("/api/telemetry/resources/non-existent-node/health")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetHistory_CompleteCoverage() throws Exception {
        mockMvc.perform(get("/api/telemetry/resources/gpu-node-01/history")
                        .param("startTime", "2026-10-10T10:00:00Z")
                        .param("endTime", "2026-10-10T12:00:00Z")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.externalResourceId").value("gpu-node-01"))
                .andExpect(jsonPath("$.dataQuality").value("COMPLETE"))
                .andExpect(jsonPath("$.coveragePct").value(100.0))
                .andExpect(jsonPath("$.measurements", hasSize(8)))
                .andExpect(jsonPath("$.dataSource").value("DEMO_DETERMINISTIC_FIXTURE"))
                .andExpect(jsonPath("$.isDemonstrationData").value(true));
    }

    @Test
    void testGetHistory_PartialCoverage() throws Exception {
        mockMvc.perform(get("/api/telemetry/resources/gpu-node-01/history")
                        .param("startTime", "2026-10-11T14:00:00Z")
                        .param("endTime", "2026-10-11T16:00:00Z")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.externalResourceId").value("gpu-node-01"))
                .andExpect(jsonPath("$.dataQuality").value("PARTIAL"))
                .andExpect(jsonPath("$.coveragePct").value(37.5))
                .andExpect(jsonPath("$.measurements", hasSize(3)));
    }

    @Test
    void testGetHistory_UnavailableCoverage() throws Exception {
        mockMvc.perform(get("/api/telemetry/resources/gpu-node-01/history")
                        .param("startTime", "2026-08-01T00:00:00Z")
                        .param("endTime", "2026-08-01T02:00:00Z")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.externalResourceId").value("gpu-node-01"))
                .andExpect(jsonPath("$.dataQuality").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.coveragePct").value(0.0))
                .andExpect(jsonPath("$.measurements", hasSize(0)));
    }

    @Test
    void testGetHistory_NotFoundForUnknownResource() throws Exception {
        mockMvc.perform(get("/api/telemetry/resources/ghost-gpu/history")
                        .param("startTime", "2026-10-10T10:00:00Z")
                        .param("endTime", "2026-10-10T12:00:00Z")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetHistory_BadRequestWhenEndBeforeStart() throws Exception {
        mockMvc.perform(get("/api/telemetry/resources/gpu-node-01/history")
                        .param("startTime", "2026-10-10T12:00:00Z")
                        .param("endTime", "2026-10-10T10:00:00Z")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
