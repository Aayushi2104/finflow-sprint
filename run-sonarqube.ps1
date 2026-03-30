Write-Host "Running SonarQube analysis for FinFlow..."

$token = [System.Environment]::GetEnvironmentVariable("SONAR_TOKEN", "User")
if ([string]::IsNullOrWhiteSpace($token)) {
    $token = [System.Environment]::GetEnvironmentVariable("SONAR_TOKEN", "Process")
}

if ([string]::IsNullOrWhiteSpace($token)) {
    Write-Error "SONAR_TOKEN is not set. Set it first, then rerun this script."
    exit 1
}

$scanner = Get-Command sonar-scanner -ErrorAction SilentlyContinue
if (-not $scanner) {
    Write-Error "sonar-scanner is not installed or not on PATH."
    exit 1
}

& sonar-scanner "-Dsonar.token=$token"
