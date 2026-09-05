[CmdletBinding()]
param([string]$JavaHome=$env:JAVA_HOME,[switch]$Keep)
$ErrorActionPreference='Stop'
$repoRoot=Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($JavaHome) -and (Test-Path -LiteralPath 'D:\AndroidStudio\jbr')) {
    $JavaHome='D:\AndroidStudio\jbr'
}
if ([string]::IsNullOrWhiteSpace($JavaHome)) {throw 'Set JAVA_HOME to JDK 17 or newer.'}
$javaExe=Join-Path $JavaHome 'bin\java.exe'
if (-not (Test-Path -LiteralPath $javaExe)) {throw 'The selected JDK has no java.exe.'}
$env:JAVA_HOME=$JavaHome
$env:PYTHONIOENCODING='utf-8'
Push-Location $repoRoot
try {
    & .\gradlew.bat :server:shadowJar '-Dhttp.proxyHost=' '-Dhttps.proxyHost=' --console=plain
    if ($LASTEXITCODE -ne 0) {throw 'Server build failed.'}
    $verifyArguments=@('scripts/verify_business.py','--java',$javaExe)
    if ($Keep) {$verifyArguments+='--keep'}
    & python @verifyArguments
    if ($LASTEXITCODE -ne 0) {throw 'Business regression failed.'}
} finally {Pop-Location}
