package com.mrlbs.telemetry.model;

import java.time.Instant;

public record TelemetryMeasurement(
    Instant timestamp,
    double gpuUtilizationPct,
    double gpuMemoryUtilizationPct,
    double temperatureCelsius,
    double powerDrawWatts
) {}
