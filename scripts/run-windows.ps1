[CmdletBinding()]
param(
    [string]$Avd = 'Pleos_Connect_2_1',
    [string]$BuildRoot = $env:PARKIT_BUILD_ROOT,
    [string]$DataDirectory = $env:PARKIT_DATA_DIR,
    [switch]$Verify
)

$ErrorActionPreference = 'Stop'
$source = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$sdk = $env:ANDROID_HOME
if (!$sdk) { $sdk = $env:ANDROID_SDK_ROOT }
if (!$sdk) { $sdk = Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
$adb = Join-Path $sdk 'platform-tools\adb.exe'
$emulator = Join-Path $sdk 'emulator\emulator.exe'
foreach ($tool in @($adb, $emulator)) {
    if (!(Test-Path -LiteralPath $tool -PathType Leaf)) { throw "Android SDK tool not found: $tool" }
}
$env:ANDROID_HOME = $sdk
if ($env:JAVA_HOME) {
    $java = Join-Path $env:JAVA_HOME 'bin\java.exe'
} else {
    $java = (Get-Command java.exe -ErrorAction Stop).Source
    $env:JAVA_HOME = Split-Path -Parent (Split-Path -Parent $java)
}
if (!(Test-Path -LiteralPath $java -PathType Leaf)) { throw "Java not found: $java" }
if (!$BuildRoot) { $BuildRoot = Join-Path $env:LOCALAPPDATA 'PARK-IT' }
if (!$DataDirectory) { $DataDirectory = Join-Path $source 'server\data' }
$env:NANGMANROAD_DATA = [IO.Path]::GetFullPath($DataDirectory)
$env:PORT = '8080'

# Android builds and test class loading can fail in non-ASCII checkout paths.
$stage = [IO.Path]::GetFullPath((Join-Path $BuildRoot 'parkit-windows-run'))
if ($stage -match '[^\x20-\x7e]') { throw 'Set PARKIT_BUILD_ROOT to an ASCII path, for example D:\PleosDev\projects.' }
if ($stage -ieq $source -or $stage.StartsWith("$source\", [StringComparison]::OrdinalIgnoreCase) -or
    $source.StartsWith("$stage\", [StringComparison]::OrdinalIgnoreCase)) {
    throw 'The build directory must be outside the source checkout.'
}
$stageItem = New-Item -ItemType Directory -Path $stage -Force
if ($stageItem.Attributes -band [IO.FileAttributes]::ReparsePoint) { throw 'The build directory cannot be a junction.' }
$lib = Join-Path $stage 'server\build\install\server\lib'

# An unrelated service may own this port. Only restart a server launched from this build directory.
$listener = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
if ($listener) {
    $process = Get-CimInstance Win32_Process -Filter "ProcessId=$($listener.OwningProcess)"
    if (!$process.CommandLine -or $process.CommandLine.IndexOf($lib, [StringComparison]::OrdinalIgnoreCase) -lt 0 -or
        $process.CommandLine -notlike '*com.nangmanroad.server.MainKt*') {
        throw 'Port 8080 is in use by a different process. Close the older server and run this launcher again.'
    }
    Stop-Process -Id $process.ProcessId
    Wait-Process -Id $process.ProcessId -Timeout 10 -ErrorAction SilentlyContinue
}

Write-Host 'Preparing and building PARK-IT...'
foreach ($module in @('core', 'gradle', 'phone', 'server', 'shared', 'vehicle')) {
    $target = Join-Path $stage $module
    if ((Test-Path -LiteralPath $target) -and ((Get-Item -LiteralPath $target).Attributes -band [IO.FileAttributes]::ReparsePoint)) {
        throw "Build module is a junction: $target"
    }
    $excluded = @('build', '.gradle', '.kotlin', '.idea')
    if ($module -eq 'server') { $excluded += 'data' }
    # Mirror only known module directories into this dedicated build copy.
    & robocopy (Join-Path $source $module) $target /MIR /XJ /XD @excluded /NFL /NDL /NJH /NJS /NP /R:2 /W:1 | Out-Null
    if ($LASTEXITCODE -ge 8) { throw "Could not stage $module (robocopy exit $LASTEXITCODE)." }
}
foreach ($file in @('build.gradle.kts', 'settings.gradle.kts', 'gradle.properties', 'gradlew.bat')) {
    Copy-Item -LiteralPath (Join-Path $source $file) -Destination (Join-Path $stage $file) -Force
}
$localProperties = Join-Path $source 'local.properties'
$stagedProperties = Join-Path $stage 'local.properties'
if (Test-Path -LiteralPath $localProperties -PathType Leaf) {
    Copy-Item -LiteralPath $localProperties -Destination $stagedProperties -Force
} elseif (Test-Path -LiteralPath $stagedProperties -PathType Leaf) {
    Remove-Item -LiteralPath $stagedProperties
}
$tasks = @(':server:installDist', ':vehicle:assembleDebug', ':phone:assembleDebug')
if ($Verify) {
    $tasks += @(':server:test', ':core:testDebugUnitTest', ':vehicle:testDebugUnitTest', ':phone:testDebugUnitTest')
}
Push-Location -LiteralPath $stage
try {
    & '.\gradlew.bat' --console=plain @tasks
    if ($LASTEXITCODE -ne 0) { throw "Build failed (Gradle exit $LASTEXITCODE)." }
} finally {
    Pop-Location
}

$server = Start-Process -FilePath $java -ArgumentList '-cp', ('"{0}\*"' -f $lib), 'com.nangmanroad.server.MainKt' `
    -WorkingDirectory $stage -WindowStyle Hidden -PassThru `
    -RedirectStandardOutput (Join-Path $stage 'server.out.log') -RedirectStandardError (Join-Path $stage 'server.err.log')
$healthy = $false
for ($attempt = 0; $attempt -lt 20; $attempt++) {
    if ($server.HasExited) { break }
    try {
        $healthy = (Invoke-RestMethod 'http://localhost:8080/health' -TimeoutSec 2) -eq 'ok'
        if ($healthy) { break }
    } catch { }
    Start-Sleep -Seconds 1
}
if (!$healthy) { throw "Sync server failed to start. See $stage\server.err.log." }

$serial = 'emulator-5554'
$attached = & $adb devices
if (!($attached -match "^$serial\s")) {
    Write-Host "Opening $Avd..."
    Start-Process -FilePath $emulator -ArgumentList '-avd', $Avd, '-port', '5554', '-no-snapshot', '-gpu', 'host' `
        -RedirectStandardOutput (Join-Path $stage 'emulator.out.log') -RedirectStandardError (Join-Path $stage 'emulator.err.log')
}
Write-Host 'Waiting for Pleos to finish booting...'
$booted = $false
for ($attempt = 0; $attempt -lt 60; $attempt++) {
    if ((& $adb devices) -match "^$serial\s+device\s*$") {
        $knownAvd = & $adb -s $serial emu avd name | Select-Object -First 1
        if ($knownAvd -ne $Avd) { throw "Port 5554 is already in use by AVD $knownAvd." }
        $boot = & $adb -s $serial shell getprop sys.boot_completed
        if ("$boot".Trim() -eq '1') {
            $user = & $adb -s $serial shell am get-current-user
            if ("$user".Trim() -match '^\d+$') { $booted = $true; break }
        }
    }
    Start-Sleep -Seconds 2
}
if (!$booted) { throw "Pleos did not finish booting. See $stage\emulator.err.log." }
$androidUser = "$user".Trim()

foreach ($app in @('vehicle', 'phone')) {
    & $adb -s $serial install -r (Join-Path $stage "$app\build\outputs\apk\debug\$app-debug.apk")
    if ($LASTEXITCODE -ne 0) { throw "Could not install the $app app." }
}
& $adb -s $serial shell am start --user $androidUser --display 0 -n com.nangmanroad.vehicle/.MainActivity
if ($LASTEXITCODE -ne 0) { throw 'Could not open the vehicle app.' }

& $adb -s $serial emu multidisplay add 1 1080 2400 420 0
if ($LASTEXITCODE -ne 0) { throw 'Could not create the phone display.' }
$phoneDisplay = $null
for ($attempt = 0; $attempt -lt 20; $attempt++) {
    $displays = (& $adb -s $serial shell dumpsys display) -join [char]10
    $match = [regex]::Match($displays, 'DisplayInfo\{"Emulator 2D Display", displayId (\d+),[^\r\n]*real 1080 x 2400')
    if ($match.Success) { $phoneDisplay = $match.Groups[1].Value; break }
    Start-Sleep -Seconds 1
}
if (!$phoneDisplay) { throw 'The Pleos emulator did not expose the portrait display.' }
& $adb -s $serial shell am start --user $androidUser --display $phoneDisplay -n com.nangmanroad.phone/.MainActivity
if ($LASTEXITCODE -ne 0) { throw 'Could not open the phone app.' }

for ($attempt = 0; $attempt -lt 12; $attempt++) {
    $activities = (& $adb -s $serial shell dumpsys activity activities) -join [char]10
    if ($activities -match 'topResumedActivity=.*com\.nangmanroad\.vehicle/' -and
        $activities -match 'topResumedActivity=.*com\.nangmanroad\.phone/') {
        Write-Host 'PARK-IT is ready. Vehicle and phone are open. The sync server is running on port 8080.'
        exit 0
    }
    Start-Sleep -Seconds 1
}
throw 'Both apps were installed, but one did not stay open. Check the emulator screen.'
