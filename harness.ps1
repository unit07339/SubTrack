# 🛡️ SubTrack Harness Runner (PowerShell)
param(
    [string]$Action = "verify"
)

# 1. Ensure JAVA_HOME points to Java 21 (Temurin)
$jdk21 = "C:\Users\Ohyoonseul\.jdks\temurin-21.0.12.1"
if (Test-Path "$jdk21\bin\javac.exe") {
    $env:JAVA_HOME = $jdk21
} elseif (-not $env:JAVA_HOME -or -not (Test-Path "$env:JAVA_HOME\bin\javac.exe")) {
    $env:JAVA_HOME = "C:\Program Files\Java\jdk11.0.32_10"
}

# 2. Locate Maven executable
$mvnCmd = "mvn"
if (-not (Get-Command "mvn" -ErrorAction SilentlyContinue)) {
    $ideaMvn = "C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.3\plugins\maven-plugin\lib\maven3\bin\mvn.cmd"
    if (Test-Path $ideaMvn) {
        $mvnCmd = $ideaMvn
    } else {
        Write-Error "[HARNESS ERROR] Maven executable not found."
        exit 1
    }
}

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "🛡️ SubTrack Test Harness - Action: $Action" -ForegroundColor Cyan
Write-Host "Java Home: $env:JAVA_HOME" -ForegroundColor DarkGray
Write-Host "Maven Cmd: $mvnCmd" -ForegroundColor DarkGray
Write-Host "==========================================================" -ForegroundColor Cyan

$startTime = Get-Date

switch ($Action.ToLower()) {
    "compile" {
        & $mvnCmd test-compile -q
    }
    "quick" {
        & $mvnCmd test -Dtest="BillingCalculatorTest,SubscriptionTest" -q
    }
    "unit" {
        & $mvnCmd test -Dtest="*Test" -q
    }
    "api" {
        & $mvnCmd test -Dtest="*HandlerTest" -q
    }
    "verify" {
        & $mvnCmd clean test
    }
    "package" {
        & $mvnCmd clean package
    }
    default {
        & $mvnCmd test
    }
}

$exitCode = $LASTEXITCODE
$elapsed = (Get-Date) - $startTime

if ($exitCode -eq 0) {
    Write-Host ""
    Write-Host "✅ [HARNESS PASS] Action '$Action' completed successfully in $($elapsed.TotalSeconds.ToString("F2"))s." -ForegroundColor Green
    exit 0
} else {
    Write-Host ""
    Write-Host "❌ [HARNESS FAILURE] Action '$Action' failed with exit code $exitCode." -ForegroundColor Red
    exit $exitCode
}
