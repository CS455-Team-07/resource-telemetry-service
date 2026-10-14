# ==============================================================================
# Script: runtelemetry.ps1
# Description: Frees port 8085 and launches the Resource Telemetry Service executable.
# ==============================================================================

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptDir

# 1. Stop any existing process running on port 8085
$existingConn = Get-NetTCPConnection -LocalPort 8085 -ErrorAction SilentlyContinue
if ($existingConn) {
    Write-Host "[*] Freeing port 8085..." -ForegroundColor Yellow
    Stop-Process -Id $existingConn.OwningProcess -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 1
}

# 2. Ensure target JAR exists; compile if missing
$jarPath = Join-Path $scriptDir "target\resource-telemetry-service-0.0.1-SNAPSHOT.jar"
if (-not (Test-Path $jarPath)) {
    Write-Host "[*] Target JAR not found. Building with Maven..." -ForegroundColor Cyan
    & ".\mvnw.cmd" package -DskipTests
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[-] Build failed." -ForegroundColor Red
        exit $LASTEXITCODE
    }
}

# 3. Launch Spring Boot Telemetry Service
Write-Host "[+] Starting Resource Telemetry Service on port 8085..." -ForegroundColor Green
Write-Host "[*] Press Ctrl+C to stop the server.`n" -ForegroundColor DarkGray

java -jar $jarPath $args
