package com.mrlbs.telemetry.model;

import java.time.Instant;

public record ResourceCatalogItem(
    String externalResourceId,
    String healthStatus,
    double gpuUtilizationPct,
    double gpuMemoryUtilizationPct,
    double temperatureCelsius,
    int historicalSamplesCount,
    Instant earliestHistoricalTimestamp,
    Instant latestHistoricalTimestamp
) {}
