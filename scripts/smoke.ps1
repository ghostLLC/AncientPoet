[CmdletBinding()]
param(
    [int]$DockerTimeoutSeconds = 120,
    [int]$ServiceTimeoutSeconds = 120,
    [int]$ServerTimeoutSeconds = 90
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$composeFile = Join-Path $repoRoot 'deploy\docker-compose.yml'
$envFile = Join-Path $repoRoot 'deploy\.env'
$envTemplate = Join-Path $repoRoot 'deploy\.env.test.example'
$javaExe = 'D:\AndroidStudio\jbr\bin\java.exe'
$baseUri = $null
$serverProcess = $null
$composeStarted = $false

function Write-Step([string]$Name) {
    Write-Host "[smoke] $Name"
}

function Invoke-Compose([string[]]$Arguments) {
    & docker compose --env-file $envFile -f $composeFile @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "docker compose $($Arguments -join ' ') failed with exit code $LASTEXITCODE"
    }
}

function Test-DockerDaemon {
    $previousErrorAction = $ErrorActionPreference
    $ErrorActionPreference = 'SilentlyContinue'
    & docker info *> $null
    $available = $LASTEXITCODE -eq 0
    $ErrorActionPreference = $previousErrorAction
    return $available
}

function Wait-Until([string]$Description, [int]$TimeoutSeconds, [scriptblock]$Condition) {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        if (& $Condition) {
            return
        }
        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $deadline)
    throw "Timed out waiting for $Description after $TimeoutSeconds seconds"
}

function Invoke-JsonRequest(
    [string]$Method,
    [string]$Uri,
    [object]$Body = $null,
    [hashtable]$Headers = @{}
) {
    $parameters = @{
        Method = $Method
        Uri = $Uri
        Headers = $Headers
        UseBasicParsing = $true
        TimeoutSec = 15
    }
    if ($null -ne $Body) {
        $parameters.ContentType = 'application/json'
        $parameters.Body = $Body | ConvertTo-Json -Compress
    }
    $response = Invoke-WebRequest @parameters
    if ($response.StatusCode -lt 200 -or $response.StatusCode -ge 300) {
        throw "$Method $Uri returned HTTP $($response.StatusCode)"
    }
    if ([string]::IsNullOrWhiteSpace($response.Content)) {
        return $null
    }
    return $response.Content | ConvertFrom-Json
}

function Read-DotEnv([string]$Path) {
    $values = @{}
    foreach ($line in Get-Content -LiteralPath $Path) {
        $trimmed = $line.Trim()
        if ($trimmed.Length -eq 0 -or $trimmed.StartsWith('#') -or -not $trimmed.Contains('=')) {
            continue
        }
        $key, $value = $trimmed.Split('=', 2)
        $values[$key.Trim()] = $value.Trim().Trim('"').Trim("'")
    }
    return $values
}

function Get-Setting([hashtable]$Values, [string]$Name, [string]$Default) {
    if ($Values.ContainsKey($Name) -and -not [string]::IsNullOrWhiteSpace($Values[$Name])) {
        return $Values[$Name]
    }
    return $Default
}

try {
    Write-Step 'Checking local prerequisites'
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        throw 'Docker CLI was not found on PATH'
    }
    if (-not (Test-Path -LiteralPath $javaExe)) {
        throw "JBR Java was not found at $javaExe"
    }
    if (-not (Test-Path -LiteralPath $envFile)) {
        Copy-Item -LiteralPath $envTemplate -Destination $envFile
        Write-Step 'Created deploy/.env from safe local test defaults'
    } else {
        Write-Step 'Using existing deploy/.env without modifying it'
    }
    $localSettings = Read-DotEnv $envFile
    $appPort = Get-Setting $localSettings 'APP_PORT' '8080'
    $baseUri = "http://127.0.0.1:$appPort/api/v1"

    if (-not (Test-DockerDaemon)) {
        $dockerDesktop = 'C:\Program Files\Docker\Docker\Docker Desktop.exe'
        if (-not (Test-Path -LiteralPath $dockerDesktop)) {
            throw 'Docker daemon is unavailable and Docker Desktop was not found'
        }
        Write-Step 'Starting Docker Desktop'
        Start-Process -FilePath $dockerDesktop -WindowStyle Hidden | Out-Null
        Wait-Until 'Docker daemon' $DockerTimeoutSeconds { Test-DockerDaemon }
    }

    Write-Step 'Starting PostgreSQL, Redis, and MinIO'
    Invoke-Compose @('up', '-d')
    $composeStarted = $true
    try {
        Wait-Until 'healthy infrastructure containers' $ServiceTimeoutSeconds {
            $states = @(
                (& docker inspect --format '{{.State.Health.Status}}' ancientpoet-db 2>$null),
                (& docker inspect --format '{{.State.Health.Status}}' ancientpoet-redis 2>$null),
                (& docker inspect --format '{{.State.Health.Status}}' ancientpoet-minio 2>$null)
            )
            return $states.Count -eq 3 -and ($states | Where-Object { $_ -ne 'healthy' }).Count -eq 0
        }
    } catch {
        & docker compose --env-file $envFile -f $composeFile ps
        throw
    }

    Write-Step 'Building the server fat JAR'
    $previousJavaHome = $env:JAVA_HOME
    $env:JAVA_HOME = 'D:\AndroidStudio\jbr'
    & (Join-Path $repoRoot 'gradlew.bat') :server:buildFatJar --no-daemon --console=plain '-Dhttp.proxyHost=' '-Dhttps.proxyHost='
    if ($LASTEXITCODE -ne 0) {
        throw "Server fat JAR build failed with exit code $LASTEXITCODE"
    }
    $env:JAVA_HOME = $previousJavaHome

    $fatJar = Get-ChildItem -LiteralPath (Join-Path $repoRoot 'server\build\libs') -Filter '*-all.jar' |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if ($null -eq $fatJar -or $fatJar.Length -eq 0) {
        throw 'The server fat JAR was not produced'
    }

    Write-Step 'Starting the API server'
    $env:KTOR_DEVELOPMENT = 'true'
    $env:APP_HOST = '127.0.0.1'
    $env:APP_PORT = $appPort
    $env:POSTGRES_HOST = '127.0.0.1'
    $env:POSTGRES_PORT = Get-Setting $localSettings 'POSTGRES_PORT' '5432'
    $env:POSTGRES_DB = Get-Setting $localSettings 'POSTGRES_DB' 'ancientpoet'
    $env:POSTGRES_USER = Get-Setting $localSettings 'POSTGRES_USER' 'ancientpoet'
    $env:POSTGRES_PASSWORD = Get-Setting $localSettings 'POSTGRES_PASSWORD' 'ancientpoet_test'
    $env:REDIS_HOST = '127.0.0.1'
    $env:REDIS_PORT = Get-Setting $localSettings 'REDIS_PORT' '6379'
    $env:MINIO_ENDPOINT = 'http://127.0.0.1:9000'
    $env:MINIO_ACCESS_KEY = Get-Setting $localSettings 'MINIO_ACCESS_KEY' 'minioadmin'
    $env:MINIO_SECRET_KEY = Get-Setting $localSettings 'MINIO_SECRET_KEY' 'minioadmin'
    $env:MINIO_BUCKET = 'ancientpoet'
    $env:JWT_SECRET = Get-Setting $localSettings 'JWT_SECRET' 'ancientpoet-local-test-secret-at-least-32-chars'

    $stdoutLog = Join-Path $repoRoot 'server-smoke.stdout.log'
    $stderrLog = Join-Path $repoRoot 'server-smoke.stderr.log'
    $serverProcess = Start-Process -FilePath $javaExe -ArgumentList @('-jar', $fatJar.FullName) `
        -WorkingDirectory $repoRoot -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput $stdoutLog -RedirectStandardError $stderrLog

    Wait-Until 'API readiness endpoint' $ServerTimeoutSeconds {
        if ($serverProcess.HasExited) {
            throw "Server exited early with code $($serverProcess.ExitCode); inspect server-smoke.stderr.log"
        }
        try {
            $response = Invoke-WebRequest -Uri "$baseUri/health/ready" -UseBasicParsing -TimeoutSec 5
            return $response.StatusCode -eq 200
        } catch {
            return $false
        }
    }

    Write-Step 'Authenticating through the development SMS flow'
    $phone = '13800000000'
    Invoke-JsonRequest 'POST' "$baseUri/auth/sms/send" @{ phone = $phone } | Out-Null
    $tokens = Invoke-JsonRequest 'POST' "$baseUri/auth/sms/verify" @{ phone = $phone; code = '123456' }
    if ([string]::IsNullOrWhiteSpace($tokens.accessToken) -or [string]::IsNullOrWhiteSpace($tokens.refreshToken)) {
        throw 'SMS verification did not return both tokens'
    }
    $authHeaders = @{ Authorization = "Bearer $($tokens.accessToken)" }

    Write-Step 'Checking authenticated conversations'
    Invoke-JsonRequest 'GET' "$baseUri/conversations" $null $authHeaders | Out-Null

    Write-Step 'Checking seeded poets'
    $poets = Invoke-JsonRequest 'GET' "$baseUri/poets"
    if (@($poets.poets).Count -ne 15) {
        throw "Expected 15 poets but received $(@($poets.poets).Count)"
    }

    Write-Step 'Checking Tang map cities'
    $cities = Invoke-JsonRequest 'GET' "$baseUri/map/tang/cities"
    if (@($cities).Count -lt 1) {
        throw 'Tang map cities response was empty'
    }

    Write-Step 'Smoke workflow passed'
} finally {
    if ($null -ne $serverProcess -and -not $serverProcess.HasExited) {
        Write-Step 'Stopping the API server'
        Stop-Process -Id $serverProcess.Id -Force -ErrorAction SilentlyContinue
        $serverProcess.WaitForExit(10000) | Out-Null
    }
    if ($composeStarted) {
        Write-Step 'Stopping infrastructure containers (volumes preserved)'
        & docker compose --env-file $envFile -f $composeFile stop
    }
}
