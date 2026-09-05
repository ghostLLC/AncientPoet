[CmdletBinding()]
param([string]$EnvFile='deploy/.env',[switch]$InitializeDemo,[string]$JavaHome=$env:JAVA_HOME,[string]$ProjectName='')
$ErrorActionPreference='Stop'
$repoRoot=Split-Path -Parent $PSScriptRoot
Push-Location $repoRoot
try {
    if ($InitializeDemo -and -not (Test-Path -LiteralPath $EnvFile)) {
        function New-LocalSecret {
            $secretBytes=New-Object byte[] 32
            $generator=[System.Security.Cryptography.RandomNumberGenerator]::Create()
            try {$generator.GetBytes($secretBytes)} finally {$generator.Dispose()}
            return [Convert]::ToBase64String($secretBytes)
        }
        $template=Get-Content -LiteralPath 'deploy/.env.test.example' -Raw -Encoding utf8
        $template=$template.Replace('POSTGRES_PASSWORD=',('POSTGRES_PASSWORD='+(New-LocalSecret))).Replace('JWT_SECRET=',('JWT_SECRET='+(New-LocalSecret)))
        Set-Content -LiteralPath $EnvFile -Value $template -Encoding utf8
        Write-Host 'Created a private development environment file. Keep it out of version control.'
    }
    if (-not (Test-Path -LiteralPath $EnvFile)) {throw 'Create deploy/.env from a template, or pass -InitializeDemo.'}
    foreach ($line in Get-Content -LiteralPath $EnvFile -Encoding utf8) {
        if ($line -match '^([A-Z][A-Z0-9_]*)=(.*)$') {
            $settingName=$Matches[1]
            $settingValue=$Matches[2].Trim()
            if ($settingName -match '^(APP_|POSTGRES_|JWT_|DEEPSEEK_|SMS_|JPUSH_|WORKER_|JOB_|LETTERS_|CORS_|TRUSTED_PROXY_|ENABLE_|KTOR_DEVELOPMENT$|AI_MODE$|DEV_DELIVERY_SECONDS$)') {
                [Environment]::SetEnvironmentVariable($settingName,$settingValue,'Process')
            }
        }
    }
    if ($InitializeDemo -and ($env:KTOR_DEVELOPMENT -ne 'true' -or $env:AI_MODE -ne 'demo')) {
        throw 'The existing environment is not a demo. It was preserved; select an explicit demo environment file.'
    }
    if ([string]::IsNullOrWhiteSpace($JavaHome) -and (Test-Path -LiteralPath 'D:\AndroidStudio\jbr')) {$JavaHome='D:\AndroidStudio\jbr'}
    if ([string]::IsNullOrWhiteSpace($JavaHome)) {throw 'Set JAVA_HOME to JDK 17 or newer.'}
    $env:JAVA_HOME=$JavaHome
    $composeArguments=@('compose','--env-file',$EnvFile,'-f','deploy/docker-compose.yml')
    if ($ProjectName) {$composeArguments+=@('--project-name',$ProjectName)}
    & docker @composeArguments up -d --wait postgres
    if ($LASTEXITCODE -ne 0) {throw 'PostgreSQL startup failed. Check Docker and the configured port.'}
    & .\gradlew.bat :server:shadowJar '-Dhttp.proxyHost=' '-Dhttps.proxyHost=' --console=plain
    if ($LASTEXITCODE -ne 0) {throw 'Server build failed.'}
    $fatJar=Get-ChildItem -LiteralPath 'server/build/libs' -Filter '*-all.jar' | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    $runtimeDir=Join-Path $repoRoot 'build/local-runtime'
    New-Item -ItemType Directory -Path $runtimeDir -Force | Out-Null
    $runtimeJar=Join-Path $runtimeDir ('server-'+[guid]::NewGuid().ToString('N')+'.jar')
    Copy-Item -LiteralPath $fatJar.FullName -Destination $runtimeJar
    Write-Host 'Starting the API. Ctrl+C stops the JVM; PostgreSQL and its data remain available.'
    try {& (Join-Path $JavaHome 'bin/java.exe') '-Dfile.encoding=UTF-8' -jar $runtimeJar}
    finally {Remove-Item -LiteralPath $runtimeJar -ErrorAction SilentlyContinue}
} finally {Pop-Location}
