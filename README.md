# Resource Telemetry Service (MRLBS)

## Purpose

The **Resource Telemetry Service** is a standalone, read-only hardware telemetry microservice designed for the Multi-Resource Lab Booking System (MRLBS) . It provides point-in-time operating health snapshots and historical telemetry time-series for lab compute resources (such as DGX GPU nodes).

The primary MRLBS backend integrates with this service over HTTP to feed device-level utilization data to the **AI Request Review Agent**.

---

## Technology Stack

- **Java 21**
- **Spring Boot 4.1.1** (Spring Web MVC)
- **Jackson** (JSON time-series streaming)
- **Maven** (with included Maven Wrapper)
- **Port:** `8085`

---

## API Endpoints

| Method | Endpoint | Query Parameters | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/telemetry/catalog` | — | Returns the list of available mapped hardware nodes |
| `GET` | `/api/v1/telemetry/health/{resourceId}` | — | Returns present operating health snapshot (status, GPU util %, memory %, temp °C, power W) |
| `GET` | `/api/v1/telemetry/historical/{resourceId}` | `start` (ISO-8601), `end` (ISO-8601) | Returns time-series telemetry measurements recorded strictly within the window |

### Sample Response: Health Snapshot
```json
{
  "resourceId": "gpu-node-01",
  "healthStatus": "DEGRADED",
  "gpuUtilizationPct": 95.0,
  "gpuMemoryUtilizationPct": 98.0,
  "temperatureCelsius": 86.5,
  "powerDrawWatts": 380.0,
  "dataSource": "DEMO_TELEMETRY_FIXTURE",
  "isDemonstrationData": true,
  "timestamp": "2026-10-14T20:24:13Z"
}
```

---

## Mapped Hardware Nodes

The service includes deterministic telemetry data for the following hardware nodes:

| Node Identifier | Hardware Name | Type | Baseline State |
| :--- | :--- | :--- | :--- |
| `gpu-node-01` | DGX H100 Node 1 | GPU | High-load / Thermal stress |
| `gpu-node-02` | DGX A100 Node 2 | GPU | Operational / Moderate load |
| `gpu-node-03` | DGX A100 Node 3 | GPU | Normal / Idle |

---

## How to Run

### Option 1: Convenience Runner Script (Recommended)
This script checks for and terminates any process currently occupying port 8085, builds the executable JAR if missing, and boots the service:

```powershell
.\runtelemetry.ps1
```

### Option 2: Running via Maven Wrapper
```powershell
# Windows
.\mvnw.cmd spring-boot:run

# macOS / Linux
./mvnw spring-boot:run
```

### Option 3: Executable JAR
```powershell
# Package the JAR
.\mvnw.cmd package -DskipTests

# Run the standalone JAR
java -jar target/resource-telemetry-service-0.0.1-SNAPSHOT.jar
```

---

## Testing

Run all unit and integration tests with:

```powershell
.\mvnw.cmd test
```

---

## Architecture Principles

1. **Read-Only Independence:** The telemetry service is completely decoupled from the booking database. It does not store user data or mutate booking states.
2. **Device-Level Attribution:** Telemetry records physical device operating metrics during reservation windows. It does not inspect user processes or provide individual job attribution.
3. **Deterministic Demonstration Data:** All fixtures include explicit `isDemonstrationData = true` flags to ensure the AI review agent never mistakes synthetic test vectors for live hardware malfunctions.
