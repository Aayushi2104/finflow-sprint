$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path

$services = @(
    @{ Name = "auth-service"; Path = "auth-service\auth-service"; UseWrapper = $true },
    @{ Name = "application-service"; Path = "application-service\application-service"; UseWrapper = $true },
    @{ Name = "document-service"; Path = "document-service\document-service"; UseWrapper = $true },
    @{ Name = "admin-service"; Path = "admin-service\admin-service"; UseWrapper = $true },
    @{ Name = "gateway-service"; Path = "gateway-service\gateway-service"; UseWrapper = $true },
    @{ Name = "eureka-server"; Path = "eureka-server\eureka-server"; UseWrapper = $true },
    @{ Name = "config-service"; Path = "config-service\config-service"; UseWrapper = $false }
)

foreach ($service in $services) {
    $workdir = Join-Path $root $service.Path
    Write-Host "Generating JaCoCo report for $($service.Name)..."

    if ($service.UseWrapper) {
        Push-Location $workdir
        try {
            cmd /c ".\mvnw.cmd -Dmaven.test.failure.ignore=true clean test jacoco:report"
        } finally {
            Pop-Location
        }
    } else {
        Push-Location $workdir
        try {
            mvn -Dmaven.test.failure.ignore=true clean test jacoco:report
        } finally {
            Pop-Location
        }
    }
}

$sonarToken = [System.Environment]::GetEnvironmentVariable("SONAR_TOKEN", "Process")
if ([string]::IsNullOrWhiteSpace($sonarToken)) {
    $sonarToken = [System.Environment]::GetEnvironmentVariable("SONAR_TOKEN", "User")
}

if ([string]::IsNullOrWhiteSpace($sonarToken)) {
    throw "SONAR_TOKEN is not set."
}

Write-Host "Running SonarQube scan..."
docker run --rm -e SONAR_HOST_URL=http://host.docker.internal:9000 -e SONAR_TOKEN=$sonarToken -v "${root}:/usr/src" sonarsource/sonar-scanner-cli

Write-Host "Done. Open http://localhost:9000/dashboard?id=finflow"
