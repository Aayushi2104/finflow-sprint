param(
    [Parameter(Mandatory = $true)]
    [ValidateSet(
        "config-service",
        "eureka-server",
        "auth-service",
        "application-service",
        "document-service",
        "admin-service",
        "gateway-service"
    )]
    [string]$ServiceName,

    [string]$Duration = "2m",

    [string]$OutputDir = "$PSScriptRoot\profiling"
)

$serviceMainClasses = @{
    "config-service" = "com.finflow.config_service.ConfigServiceApplication"
    "eureka-server" = "com.finflow.eureka_server.EurekaServerApplication"
    "auth-service" = "com.finflow.auth_service.AuthServiceApplication"
    "application-service" = "com.finflow.application_service.ApplicationServiceApplication"
    "document-service" = "com.finflow.document_service.DocumentServiceApplication"
    "admin-service" = "com.finflow.admin_service.AdminServiceApplication"
    "gateway-service" = "com.finflow.gateway_service.GatewayServiceApplication"
}

$mainClass = $serviceMainClasses[$ServiceName]

$javaProcess = Get-CimInstance Win32_Process |
    Where-Object { $_.Name -eq "java.exe" -and $_.CommandLine -like "*$mainClass*" } |
    Select-Object -First 1

if (-not $javaProcess) {
    throw "No running Java process found for $ServiceName ($mainClass). Start the service first."
}

if (-not (Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir | Out-Null
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$recordingName = "FinFlow-$ServiceName"
$outputFile = Join-Path $OutputDir "$ServiceName-$timestamp.jfr"

$jcmd = Join-Path $env:JAVA_HOME "bin\jcmd.exe"
if (-not (Test-Path $jcmd)) {
    $jcmd = "jcmd"
}

& $jcmd $javaProcess.ProcessId JFR.start "name=$recordingName" "settings=profile" "duration=$Duration" "filename=$outputFile"

Write-Host "JFR recording started for $ServiceName"
Write-Host "PID: $($javaProcess.ProcessId)"
Write-Host "Duration: $Duration"
Write-Host "Output: $outputFile"
