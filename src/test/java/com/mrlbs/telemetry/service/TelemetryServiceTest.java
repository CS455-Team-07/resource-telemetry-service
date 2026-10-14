package com.mrlbs.telemetry.service;

import com.mrlbs.telemetry.model.CurrentHealthResponse;
import com.mrlbs.telemetry.model.DataQuality;
import com.mrlbs.telemetry.model.HistoricalTelemetryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TelemetryServiceTest {

    private TelemetryService telemetryService;

    @BeforeEach
    void setUp() {
        telemetryService = new TelemetryService(
                new DefaultResourceLoader(),
                "classpath:config/telemetry-dataset.json",
                15,
                90.0
        );
        telemetryService.loadDataset();
    }

    @Test
    void testGetCurrentHealth_KnownResource() {
        Optional<CurrentHealthResponse> healthOpt = telemetryService.getCurrentHealth("gpu-node-01");
        assertTrue(healthOpt.isPresent());
        CurrentHealthResponse health = healthOpt.get();
        assertEquals("gpu-node-01", health.externalResourceId());
        assertEquals("HEALTHY", health.healthStatus());
        assertTrue(health.isDemonstrationData());
        assertEquals("DEMO_DETERMINISTIC_FIXTURE", health.dataSource());
        assertEquals(42.5, health.gpuUtilizationPct());
    }

    @Test
    void testGetCurrentHealth_UnknownResource() {
        Optional<CurrentHealthResponse> healthOpt = telemetryService.getCurrentHealth("unknown-gpu");
        assertTrue(healthOpt.isEmpty());
    }

    @Test
    void testGetHistoricalTelemetry_CompleteCoverage() {
        Instant start = Instant.parse("2026-10-10T10:00:00Z");
        Instant end = Instant.parse("2026-10-10T12:00:00Z");

        Optional<HistoricalTelemetryResponse> historyOpt =
                telemetryService.getHistoricalTelemetry("gpu-node-01", start, end);

        assertTrue(historyOpt.isPresent());
        HistoricalTelemetryResponse history = historyOpt.get();
        assertEquals("gpu-node-01", history.externalResourceId());
        assertEquals(DataQuality.COMPLETE, history.dataQuality());
        assertEquals(100.0, history.coveragePct());
        assertEquals(8, history.measurements().size());
        assertTrue(history.isDemonstrationData());
        assertEquals("DEMO_DETERMINISTIC_FIXTURE", history.dataSource());
    }

    @Test
    void testGetHistoricalTelemetry_PartialCoverage() {
        Instant start = Instant.parse("2026-10-11T14:00:00Z");
        Instant end = Instant.parse("2026-10-11T16:00:00Z");

        Optional<HistoricalTelemetryResponse> historyOpt =
                telemetryService.getHistoricalTelemetry("gpu-node-01", start, end);

        assertTrue(historyOpt.isPresent());
        HistoricalTelemetryResponse history = historyOpt.get();
        assertEquals(DataQuality.PARTIAL, history.dataQuality());
        assertEquals(37.5, history.coveragePct());
        assertEquals(3, history.measurements().size());
    }

    @Test
    void testGetHistoricalTelemetry_UnavailableCoverage() {
        Instant start = Instant.parse("2026-08-01T00:00:00Z");
        Instant end = Instant.parse("2026-08-01T02:00:00Z");

        Optional<HistoricalTelemetryResponse> historyOpt =
                telemetryService.getHistoricalTelemetry("gpu-node-01", start, end);

        assertTrue(historyOpt.isPresent());
        HistoricalTelemetryResponse history = historyOpt.get();
        assertEquals(DataQuality.UNAVAILABLE, history.dataQuality());
        assertEquals(0.0, history.coveragePct());
        assertTrue(history.measurements().isEmpty());
    }

    @Test
    void testGetHistoricalTelemetry_UnknownResource() {
        Instant start = Instant.parse("2026-10-10T10:00:00Z");
        Instant end = Instant.parse("2026-10-10T12:00:00Z");

        Optional<HistoricalTelemetryResponse> historyOpt =
                telemetryService.getHistoricalTelemetry("non-existent-resource", start, end);

        assertTrue(historyOpt.isEmpty());
    }

    @Test
    void testDeterministicOutput_IdenticalQueriesReturnIdenticalResults() {
        Instant start = Instant.parse("2026-10-10T10:00:00Z");
        Instant end = Instant.parse("2026-10-10T12:00:00Z");

        HistoricalTelemetryResponse query1 = telemetryService.getHistoricalTelemetry("gpu-node-01", start, end).orElseThrow();
        HistoricalTelemetryResponse query2 = telemetryService.getHistoricalTelemetry("gpu-node-01", start, end).orElseThrow();

        assertEquals(query1, query2);
    }
}
