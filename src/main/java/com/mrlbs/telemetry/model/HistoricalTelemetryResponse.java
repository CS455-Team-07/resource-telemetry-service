package com.mrlbs.telemetry.model;

import java.time.Instant;
import java.util.List;

public record HistoricalTelemetryResponse(
    String externalResourceId,
    Instant queryStartTime,
    Instant queryEndTime,
    DataQuality dataQuality,
    double coveragePct,
    boolean isDemonstrationData,
    String dataSource,
    List<TelemetryMeasurement> measurements
) {}
