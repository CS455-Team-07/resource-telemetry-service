package com.mrlbs.telemetry.model;

import java.util.List;

public record DatasetRoot(
    List<ResourceTelemetryData> resources
) {}
