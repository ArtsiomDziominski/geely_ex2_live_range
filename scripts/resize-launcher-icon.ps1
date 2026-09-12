# Resize master PNG into Android mipmap launcher assets.
param(
    [string]$Source = "",
    [string]$ResRoot = ""
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
if (-not $Source) {
    $Source = Join-Path $root "docs\assets\dynamic-range-ex2-app-icon.png"
}
if (-not $ResRoot) {
    $ResRoot = Join-Path $root "app\src\main\res"
}
if (-not (Test-Path $Source)) {
    throw "Source not found: $Source"
}

Add-Type -AssemblyName System.Drawing

function Save-ResizedPng([string]$inputPath, [string]$outputPath, [int]$size) {
    $img = [System.Drawing.Image]::FromFile($inputPath)
    $bmp = New-Object System.Drawing.Bitmap $size, $size
    $graphics = [System.Drawing.Graphics]::FromImage($bmp)
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
    $graphics.Clear([System.Drawing.Color]::FromArgb(0, 0, 0, 0))
    $graphics.DrawImage($img, 0, 0, $size, $size)
    $dir = Split-Path -Parent $outputPath
    if (-not (Test-Path $dir)) {
        New-Item -ItemType Directory -Force -Path $dir | Out-Null
    }
    $bmp.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $graphics.Dispose()
    $bmp.Dispose()
    $img.Dispose()
}

$densityMap = @{
    "mipmap-mdpi"    = @{ Launcher = 48; Foreground = 108 }
    "mipmap-hdpi"    = @{ Launcher = 72; Foreground = 162 }
    "mipmap-xhdpi"   = @{ Launcher = 96; Foreground = 216 }
    "mipmap-xxhdpi"  = @{ Launcher = 144; Foreground = 324 }
    "mipmap-xxxhdpi" = @{ Launcher = 192; Foreground = 432 }
}

foreach ($entry in $densityMap.GetEnumerator()) {
    $folder = Join-Path $ResRoot $entry.Key
    Save-ResizedPng $Source (Join-Path $folder "ic_launcher.png") $entry.Value.Launcher
    Save-ResizedPng $Source (Join-Path $folder "ic_launcher_round.png") $entry.Value.Launcher
    Save-ResizedPng $Source (Join-Path $folder "ic_launcher_foreground.png") $entry.Value.Foreground
    Write-Host "OK $($entry.Key)"
}

Write-Host "Done: $Source"
