package com.mrlbs.telemetry.model;

import java.util.List;

public record ResourceTelemetryData(
    String externalResourceId,
    CurrentHealthResponse currentHealth,
    List<TelemetryMeasurement> historicalMeasurements
) {}
