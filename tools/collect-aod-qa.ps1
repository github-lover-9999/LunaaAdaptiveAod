[CmdletBinding()]
param(
    [string]$OutputDirectory = "",
    [switch]$CleanAod
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
    throw "adb is not in PATH. Add Android platform-tools to PATH first."
}

$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
if ([string]::IsNullOrWhiteSpace($OutputDirectory)) {
    $OutputDirectory = Join-Path $ProjectRoot "dist\qa"
}
New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null

$state = (& adb get-state 2>$null).Trim()
if ($state -ne "device") { throw "ADB device is not connected/authorized (state='$state')." }

$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$kernelStartLine = 1
if ($CleanAod) {
    $displayState = (& adb shell dumpsys display | Select-String -Pattern "Display State=ON" | Select-Object -First 1)
    if (-not $displayState) {
        throw "CleanAod requires the phone screen to be ON before the test. Unlock/wake it and retry."
    }

    & adb logcat -c
    $kernelLineCount = [int]((& adb shell "su -c 'dmesg 2>/dev/null | wc -l'").Trim())
    $kernelStartLine = $kernelLineCount + 1

    Write-Host "Clean AOD test: switching the screen off. Do not touch the phone for 18 seconds." -ForegroundColor Yellow
    & adb shell input keyevent 26 | Out-Null
    Start-Sleep -Seconds 18
}

$displayFile = Join-Path $OutputDirectory "display-$stamp.txt"
$logFile = Join-Path $OutputDirectory "logcat-$stamp.txt"
$kernelFile = Join-Path $OutputDirectory "kernel-$stamp.txt"
$sysfsFile = Join-Path $OutputDirectory "sysfs-$stamp.txt"
$summaryFile = Join-Path $OutputDirectory "summary-$stamp.txt"
$deviceFile = Join-Path $OutputDirectory "device-$stamp.txt"
$prefsFile = Join-Path $OutputDirectory "prefs-$stamp.txt"
$capabilityFile = Join-Path $OutputDirectory "capability-$stamp.txt"
$logFullFile = Join-Path $OutputDirectory "logcat-full-$stamp.txt"
$kernelFullFile = Join-Path $OutputDirectory "kernel-full-$stamp.txt"

$deviceProps = @(
    "ro.product.device",
    "ro.product.product.device",
    "ro.product.model",
    "ro.product.manufacturer",
    "ro.build.fingerprint",
    "ro.crdroid.version",
    "ro.axion.version",
    "ro.lineage.version"
)
$deviceProps | ForEach-Object {
    $key = $_
    $value = (& adb shell getprop $key).Trim()
    "$key=$value"
} | Out-File -Encoding utf8 $deviceFile

& adb shell "su -c 'cat /data/user/0/dev.lunaa.aod/shared_prefs/aod_settings.xml 2>/dev/null || cat /data/data/dev.lunaa.aod/shared_prefs/aod_settings.xml 2>/dev/null || echo PREFS_UNAVAILABLE'" |
    Out-File -Encoding utf8 $prefsFile

$capabilityCommand = @'
for p in /sys/kernel/oplus_display/notify_fppress /sys/kernel/oplus_display/dimlayer_hbm /sys/class/backlight/panel0-backlight/brightness; do
  echo "PATH=$p"
  if [ -e "$p" ]; then echo "exists=1"; else echo "exists=0"; fi
  if [ -r "$p" ]; then echo "readable=1"; else echo "readable=0"; fi
  if [ -w "$p" ]; then echo "writable=1"; else echo "writable=0"; fi
done
'@
& adb shell "su -c '$capabilityCommand'" | Out-File -Encoding utf8 $capabilityFile

& adb shell dumpsys display | Out-File -Encoding utf8 $displayFile
& adb logcat -d -b all -v time | Out-File -Encoding utf8 $logFullFile
& adb logcat -d -b all -v time |
    Select-String -Pattern "LunaaAOD|LunaaAODRoot|VectorLegacyBridge|DozeService|Udfps|Fingerprint|OnscreenFingerprint" |
    ForEach-Object { $_.Line } |
    Out-File -Encoding utf8 $logFile

$kernelCommand = if ($CleanAod) {
    "su -c 'dmesg 2>/dev/null | tail -n +$kernelStartLine'"
} else {
    "su -c 'dmesg 2>/dev/null'"
}
& adb shell $kernelCommand | Out-File -Encoding utf8 $kernelFullFile
Get-Content $kernelFullFile |
    Select-String -Pattern "notify fingerpress|aod-hbm|aod-high|OnscreenFingerprint|dimlayer_hbm|HBM" |
    ForEach-Object { $_.Line } |
    Out-File -Encoding utf8 $kernelFile

& adb shell "su -c 'echo -n dimHBM=; cat /sys/kernel/oplus_display/dimlayer_hbm 2>/dev/null; echo -n dimAlpha=; cat /sys/kernel/oplus_display/dim_alpha 2>/dev/null; echo -n dcAlpha=; cat /sys/kernel/oplus_display/dim_dc_alpha 2>/dev/null; echo -n hbm=; cat /sys/kernel/oplus_display/hbm 2>/dev/null; echo -n panelBL=; cat /sys/class/backlight/panel0-backlight/brightness 2>/dev/null'" |
    Out-File -Encoding utf8 $sysfsFile

$summary = Get-Content $displayFile |
    Select-String -Pattern "Display State=|Display Brightness=|mDozeBrightnessSensorValueToBrightness=|mDefaultDozeBrightness=|dozeScreenBrightness=|mScreenBrightnessDozeConfig=|mAllowAutoBrightnessWhileDozing=|mScreenBrightness=" |
    ForEach-Object { $_.Line.Trim() }
$summary | Out-File -Encoding utf8 $summaryFile

Write-Host "Saved:" -ForegroundColor Green
Write-Host "  $summaryFile"
Write-Host "  $deviceFile"
Write-Host "  $prefsFile"
Write-Host "  $capabilityFile"
Write-Host "  $logFile"
Write-Host "  $logFullFile"
Write-Host "  $kernelFile"
Write-Host "  $kernelFullFile"
Write-Host "  $sysfsFile"
Write-Host "  $displayFile"
if ($CleanAod) {
    Write-Host "`nClean AOD capture completed while the screen was left untouched for 18 seconds."
} else {
    Write-Host "`nFor a controlled Extra Bright trace run: .\tools\collect-aod-qa.ps1 -CleanAod"
}
