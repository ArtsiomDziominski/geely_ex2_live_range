# Watch sources and reinstall userDebug APK on emulator when files change.
#
# Usage:
#   .\scripts\watch-and-reinstall.ps1
#   .\scripts\watch-and-reinstall.ps1 -DebounceSec 2
#
# Ctrl+C to stop.

param(
    [int]$DebounceSec = 2,
    [string]$Serial = ""
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $root

$adb = if ($env:ANDROID_HOME) {
    Join-Path $env:ANDROID_HOME "platform-tools\adb.exe"
} else {
    Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
}
if (-not (Test-Path $adb)) { throw "adb not found: $adb" }

$apk = Join-Path $root "app\build\outputs\apk\user\debug\app-user-debug.apk"
$watchDir = Join-Path $root "app\src"
$extraFiles = @(
    (Join-Path $root "app\build.gradle.kts"),
    (Join-Path $root "app\version.properties")
)

function Get-DeviceSerial([string]$Preferred) {
    if ($Preferred) {
        $alive = (& $adb devices) | Where-Object { $_ -match ("^{0}\s+device$" -f [regex]::Escape($Preferred)) }
        if ($alive) { return $Preferred }
    }
    # Prefer emulator for local test watch (avoid installing userDebug onto HU).
    $emu = (& $adb devices) | Where-Object { $_ -match "^(emulator-\d+)\s+device$" } | Select-Object -First 1
    if ($emu) { return ($emu -split "\s+")[0] }
    $line = (& $adb devices) | Where-Object { $_ -match "^([\w:-]+)\s+device$" } | Select-Object -First 1
    if (-not $line) { return $null }
    return ($line -split "\s+")[0]
}

function Get-SourcesStamp {
    $files = @(Get-ChildItem -Path $watchDir -Recurse -File -Include *.kt,*.xml,*.png -ErrorAction SilentlyContinue)
    foreach ($f in $extraFiles) {
        if (Test-Path $f) { $files += Get-Item $f }
    }
    if (-not $files -or $files.Count -eq 0) { return "empty" }
    $latest = ($files | Sort-Object LastWriteTimeUtc -Descending | Select-Object -First 1)
    return "{0}|{1}" -f $latest.FullName, $latest.LastWriteTimeUtc.Ticks
}

function Install-And-Launch([string]$Device) {
    Write-Host ""
    Write-Host ("==> rebuild userDebug ({0})" -f (Get-Date -Format "HH:mm:ss"))
    & .\gradlew.bat :app:assembleUserDebug --quiet
    if ($LASTEXITCODE -ne 0) {
        Write-Host "BUILD FAILED" -ForegroundColor Red
        return
    }
    if (-not (Test-Path $apk)) {
        Write-Host ("APK missing: {0}" -f $apk) -ForegroundColor Red
        return
    }
    Write-Host ("==> install + launch on {0}" -f $Device)
    & $adb -s $Device install -r -g $apk | Out-Host
    if ($LASTEXITCODE -ne 0) {
        Write-Host "INSTALL FAILED" -ForegroundColor Red
        return
    }
    & $adb -s $Device shell am force-stop com.geely.ex2.range | Out-Null
    & $adb -s $Device shell am start -n com.geely.ex2.range/.ui.MainActivity | Out-Host
    Write-Host "OK" -ForegroundColor Green
}

$device = Get-DeviceSerial $Serial
if (-not $device) {
    throw "No device/emulator online. Start Automotive landscape emulator first."
}

Write-Host ("Watching app\src (+ gradle/version) -> rebuild/install on {0}" -f $device)
Write-Host ("Poll every 1s, debounce {0}s  |  Ctrl+C to stop" -f $DebounceSec)

Install-And-Launch $device
$lastStamp = Get-SourcesStamp
$changedAt = $null

while ($true) {
    Start-Sleep -Seconds 1
    $stamp = Get-SourcesStamp
    if ($stamp -ne $lastStamp) {
        $lastStamp = $stamp
        $changedAt = Get-Date
        Write-Host ("change detected ({0}) - waiting {1}s..." -f (Get-Date -Format "HH:mm:ss"), $DebounceSec)
    }
    if ($null -ne $changedAt -and ((Get-Date) - $changedAt).TotalSeconds -ge $DebounceSec) {
        $changedAt = $null
        $device = Get-DeviceSerial $Serial
        if (-not $device) {
            Write-Host "No device online - skip" -ForegroundColor Yellow
            continue
        }
        Install-And-Launch $device
        $lastStamp = Get-SourcesStamp
    }
}
