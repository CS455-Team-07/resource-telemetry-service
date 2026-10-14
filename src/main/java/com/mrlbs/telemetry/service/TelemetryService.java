package com.mrlbs.telemetry.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mrlbs.telemetry.model.CurrentHealthResponse;
import com.mrlbs.telemetry.model.DataQuality;
import com.mrlbs.telemetry.model.DatasetRoot;
import com.mrlbs.telemetry.model.HistoricalTelemetryResponse;
import com.mrlbs.telemetry.model.ResourceTelemetryData;
import com.mrlbs.telemetry.model.TelemetryMeasurement;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class TelemetryService {

    private static final Logger log = LoggerFactory.getLogger(TelemetryService.class);

    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    private final String datasetLocation;
    private final int samplingIntervalMinutes;
    private final double completeThresholdPct;

    private Map<String, ResourceTelemetryData> resourceDataMap = Collections.emptyMap();

    public TelemetryService(
            ResourceLoader resourceLoader,
            @Value("${telemetry.dataset.location:classpath:config/telemetry-dataset.json}") String datasetLocation,
            @Value("${telemetry.sampling.interval-minutes:15}") int samplingIntervalMinutes,
            @Value("${telemetry.coverage.complete-threshold-pct:90.0}") double completeThresholdPct
    ) {
        this.resourceLoader = resourceLoader;
        this.datasetLocation = datasetLocation;
        this.samplingIntervalMinutes = samplingIntervalMinutes;
        this.completeThresholdPct = completeThresholdPct;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @PostConstruct
    public synchronized void loadDataset() {
        try {
            Resource resource = resourceLoader.getResource(datasetLocation);
            try (InputStream inputStream = resource.getInputStream()) {
                DatasetRoot root = objectMapper.readValue(inputStream, DatasetRoot.class);
                Map<String, ResourceTelemetryData> map = new HashMap<>();
                if (root.resources() != null) {
                    for (ResourceTelemetryData rtd : root.resources()) {
                        map.put(rtd.externalResourceId(), rtd);
                    }
                }
                this.resourceDataMap = Collections.unmodifiableMap(map);
                log.info("Successfully loaded deterministic telemetry dataset with {} resources from {}",
                        this.resourceDataMap.size(), datasetLocation);
            }
        } catch (Exception e) {
            log.error("Failed to load telemetry dataset from {}: {}", datasetLocation, e.getMessage(), e);
            throw new IllegalStateException("Could not load telemetry dataset from " + datasetLocation, e);
        }
    }

    public List<com.mrlbs.telemetry.model.ResourceCatalogItem> getAllResourcesCatalog() {
        return resourceDataMap.values().stream()
                .map(data -> {
                    CurrentHealthResponse health = data.currentHealth();
                    List<TelemetryMeasurement> hist = data.historicalMeasurements() != null
                            ? data.historicalMeasurements()
                            : Collections.emptyList();
                    Instant earliest = hist.stream()
                            .map(TelemetryMeasurement::timestamp)
                            .filter(t -> t != null)
                            .min(Instant::compareTo)
                            .orElse(null);
                    Instant latest = hist.stream()
                            .map(TelemetryMeasurement::timestamp)
                            .filter(t -> t != null)
                            .max(Instant::compareTo)
                            .orElse(null);
                    return new com.mrlbs.telemetry.model.ResourceCatalogItem(
                            data.externalResourceId(),
                            health != null ? health.healthStatus() : "UNKNOWN",
                            health != null ? health.gpuUtilizationPct() : 0.0,
                            health != null ? health.gpuMemoryUtilizationPct() : 0.0,
                            health != null ? health.temperatureCelsius() : 0.0,
                            hist.size(),
                            earliest,
                            latest
                    );
                })
                .sorted(Comparator.comparing(com.mrlbs.telemetry.model.ResourceCatalogItem::externalResourceId))
                .toList();
    }

    public Optional<CurrentHealthResponse> getCurrentHealth(String externalResourceId) {
        ResourceTelemetryData data = resourceDataMap.get(externalResourceId);
        if (data == null || data.currentHealth() == null) {
            return Optional.empty();
        }
        return Optional.of(data.currentHealth());
    }

    public Optional<HistoricalTelemetryResponse> getHistoricalTelemetry(
            String externalResourceId,
            Instant startTime,
            Instant endTime
    ) {
        ResourceTelemetryData data = resourceDataMap.get(externalResourceId);
        if (data == null) {
            return Optional.empty();
        }

        if (startTime == null || endTime == null || endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("Invalid query interval: startTime and endTime must be non-null and startTime <= endTime");
        }

        List<TelemetryMeasurement> allMeasurements = data.historicalMeasurements() != null
                ? data.historicalMeasurements()
                : Collections.emptyList();

        List<TelemetryMeasurement> filtered = allMeasurements.stream()
                .filter(m -> m.timestamp() != null
                        && !m.timestamp().isBefore(startTime)
                        && !m.timestamp().isAfter(endTime))
                .sorted(Comparator.comparing(TelemetryMeasurement::timestamp))
                .toList();

        long durationMinutes = Math.max(1, Duration.between(startTime, endTime).toMinutes());
        int intervalMinutes = Math.max(1, samplingIntervalMinutes);
        int expectedSamples = Math.max(1, (int) Math.ceil((double) durationMinutes / intervalMinutes));
        int actualSamples = filtered.size();

        double rawCoverage = ((double) actualSamples / expectedSamples) * 100.0;
        double coveragePct = Math.min(100.0, Math.round(rawCoverage * 10.0) / 10.0);

        DataQuality quality;
        if (actualSamples == 0) {
            quality = DataQuality.UNAVAILABLE;
            coveragePct = 0.0;
        } else if (coveragePct >= completeThresholdPct) {
            quality = DataQuality.COMPLETE;
        } else {
            quality = DataQuality.PARTIAL;
        }

        return Optional.of(new HistoricalTelemetryResponse(
                externalResourceId,
                startTime,
                endTime,
                quality,
                coveragePct,
                true,
                "DEMO_DETERMINISTIC_FIXTURE",
                filtered
        ));
    }
}
