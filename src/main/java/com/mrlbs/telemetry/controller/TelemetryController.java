package com.mrlbs.telemetry.controller;

import com.mrlbs.telemetry.model.CurrentHealthResponse;
import com.mrlbs.telemetry.model.HistoricalTelemetryResponse;
import com.mrlbs.telemetry.service.TelemetryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/telemetry/resources")
public class TelemetryController {

    private final TelemetryService telemetryService;

    public TelemetryController(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    @GetMapping
    public ResponseEntity<java.util.List<com.mrlbs.telemetry.model.ResourceCatalogItem>> getAllResources() {
        return ResponseEntity.ok(telemetryService.getAllResourcesCatalog());
    }

    @GetMapping("/{externalResourceId}/health")
    public ResponseEntity<CurrentHealthResponse> getCurrentHealth(
            @PathVariable("externalResourceId") String externalResourceId
    ) {
        return telemetryService.getCurrentHealth(externalResourceId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/{externalResourceId}/history")
    public ResponseEntity<HistoricalTelemetryResponse> getHistoricalTelemetry(
            @PathVariable("externalResourceId") String externalResourceId,
            @RequestParam("startTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam("endTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime
    ) {
        if (endTime.isBefore(startTime)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        return telemetryService.getHistoricalTelemetry(externalResourceId, startTime, endTime)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
}
