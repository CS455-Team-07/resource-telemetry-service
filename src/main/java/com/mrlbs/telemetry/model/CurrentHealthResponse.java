package com.mrlbs.telemetry.model;

import java.time.Instant;

public record CurrentHealthResponse(
    String externalResourceId,
    String healthStatus,
    double gpuUtilizationPct,
    double gpuMemoryUtilizationPct,
    double temperatureCelsius,
    double powerDrawWatts,
    Instant timestamp,
    String dataSource,
    boolean isDemonstrationData
) {}
