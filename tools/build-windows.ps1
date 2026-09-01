[CmdletBinding()]
param(
    [string]$SdkRoot = "",
    [switch]$SkipToolDownloads
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$ToolsRoot = Join-Path $ProjectRoot ".tools"
$DistRoot = Join-Path $ProjectRoot "dist"
$GradleVersion = "8.13"
$GradleZip = Join-Path $ToolsRoot "gradle-$GradleVersion-bin.zip"
$GradleDir = Join-Path $ToolsRoot "gradle-$GradleVersion"
$GradleUrl = "https://services.gradle.org/distributions/gradle-$GradleVersion-bin.zip"
$GradleShaUrl = "$GradleUrl.sha256"
$CmdlineVersion = "15859902"
$CmdlineUrl = "https://dl.google.com/android/repository/commandlinetools-win-${CmdlineVersion}_latest.zip"
$CmdlineSha256 = "90ae805d20434428bffcb699c290860f19bb5f66a67e6b330067e3de801fb04a"

$ProgressPreference = "SilentlyContinue"

function Write-Step([string]$Message) {
    Write-Host "`n==> $Message" -ForegroundColor Cyan
}

function Download-File([string]$Url, [string]$Destination) {
    Write-Host "Downloading $Url"
    if (Get-Command curl.exe -ErrorAction SilentlyContinue) {
        & curl.exe -fSL "$Url" -o "$Destination"
        if ($LASTEXITCODE -ne 0) {
            Invoke-WebRequest -UseBasicParsing -Uri $Url -OutFile $Destination
        }
    } else {
        Invoke-WebRequest -UseBasicParsing -Uri $Url -OutFile $Destination
    }
}

function Assert-Sha256([string]$Path, [string]$Expected) {
    $actual = (Get-FileHash -Algorithm SHA256 -Path $Path).Hash.ToLowerInvariant()
    if ($actual -ne $Expected.ToLowerInvariant()) {
        throw "SHA-256 mismatch for $Path`nExpected: $Expected`nActual:   $actual"
    }
}

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw "Java is not in PATH. Install JDK 17+ (your Temurin 21 installation is suitable) and reopen PowerShell."
}

New-Item -ItemType Directory -Force -Path $ToolsRoot, $DistRoot | Out-Null

if ([string]::IsNullOrWhiteSpace($SdkRoot)) {
    if ($env:ANDROID_SDK_ROOT -and (Test-Path $env:ANDROID_SDK_ROOT)) {
        $SdkRoot = $env:ANDROID_SDK_ROOT
    } elseif ($env:ANDROID_HOME -and (Test-Path $env:ANDROID_HOME)) {
        $SdkRoot = $env:ANDROID_HOME
    } else {
        $SdkRoot = Join-Path $ToolsRoot "android-sdk"
    }
}
$SdkRoot = [System.IO.Path]::GetFullPath($SdkRoot)
New-Item -ItemType Directory -Force -Path $SdkRoot | Out-Null
$env:ANDROID_SDK_ROOT = $SdkRoot
$env:ANDROID_HOME = $SdkRoot

$SdkManager = Join-Path $SdkRoot "cmdline-tools\latest\bin\sdkmanager.bat"
if (-not (Test-Path $SdkManager)) {
    if ($SkipToolDownloads) {
        throw "sdkmanager not found at $SdkManager and -SkipToolDownloads was supplied."
    }
    Write-Step "Installing Android command-line tools"
    $cmdZip = Join-Path $ToolsRoot "commandlinetools-win-${CmdlineVersion}_latest.zip"
    if (-not (Test-Path $cmdZip) -or ((Get-FileHash -Algorithm SHA256 -Path $cmdZip).Hash.ToLowerInvariant() -ne $CmdlineSha256)) {
        Download-File $CmdlineUrl $cmdZip
        Assert-Sha256 $cmdZip $CmdlineSha256
    }

    $unpack = Join-Path $ToolsRoot "cmdline-unpack"
    Remove-Item -Recurse -Force $unpack -ErrorAction SilentlyContinue
    Expand-Archive -Path $cmdZip -DestinationPath $unpack -Force
    $latest = Join-Path $SdkRoot "cmdline-tools\latest"
    Remove-Item -Recurse -Force $latest -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Force -Path (Split-Path $latest) | Out-Null
    Move-Item -Path (Join-Path $unpack "cmdline-tools") -Destination $latest
    Remove-Item -Recurse -Force $unpack
}

Write-Step "Accepting Android SDK licenses"
$licDir = Join-Path $SdkRoot "licenses"
New-Item -ItemType Directory -Force -Path $licDir | Out-Null
@"
24333f8a63b6825ea9c5514f83c2829b004d1fee
8933ed6d10575d8c13d2d790140b9c51e5ff0d98
d56f5187479451eabf01fb7871519401464956ea
"@ | Set-Content -Path (Join-Path $licDir "android-sdk-license") -Encoding ASCII
@"
84831b9409646a53ee4421a182361f607429f966
"@ | Set-Content -Path (Join-Path $licDir "android-sdk-preview-license") -Encoding ASCII
@"
601085b94cd77f0b54ff86406957099150008536
"@ | Set-Content -Path (Join-Path $licDir "android-googletv-license") -Encoding ASCII
@"
33b6a2b64925de8ab3f92f9b1f5e50136fe742e1
"@ | Set-Content -Path (Join-Path $licDir "google-gdk-license") -Encoding ASCII
@"
d975f751698a77b662f1254ddbeed3901e976f5a
"@ | Set-Content -Path (Join-Path $licDir "intel-android-extra-license") -Encoding ASCII
@"
e9acab587f6c534929cb42b6cf752c42400b7301
"@ | Set-Content -Path (Join-Path $licDir "mips-android-extra-license") -Encoding ASCII

Write-Step "Installing Android 16 SDK packages"
& $SdkManager --sdk_root="$SdkRoot" "platforms;android-36" "build-tools;35.0.0" "platform-tools"
if ($LASTEXITCODE -ne 0) { throw "sdkmanager package installation failed with exit code $LASTEXITCODE" }

$AndroidJar = Join-Path $SdkRoot "platforms\android-36\android.jar"
if (-not (Test-Path $AndroidJar)) { throw "Android 36 platform is missing after sdkmanager completed." }

if (-not (Test-Path (Join-Path $GradleDir "bin\gradle.bat"))) {
    if ($SkipToolDownloads) {
        throw "Gradle $GradleVersion not found under $GradleDir and -SkipToolDownloads was supplied."
    }
    Write-Step "Installing Gradle $GradleVersion"
    Download-File $GradleUrl $GradleZip
    $shaFile = "$GradleZip.sha256"
    Download-File $GradleShaUrl $shaFile
    $expected = (Get-Content $shaFile -Raw).Trim().Split()[0]
    Assert-Sha256 $GradleZip $expected
    Expand-Archive -Path $GradleZip -DestinationPath $ToolsRoot -Force
}

$Gradle = Join-Path $GradleDir "bin\gradle.bat"
Write-Step "Running unit tests and assembling debug APK"
Push-Location $ProjectRoot
try {
    & $Gradle --no-daemon --stacktrace clean ":xposed-stubs:test" ":app:testDebugUnitTest" ":app:assembleDebug"
    if ($LASTEXITCODE -ne 0) { throw "Gradle build failed with exit code $LASTEXITCODE" }
} finally {
    Pop-Location
}

$BuiltApk = Join-Path $ProjectRoot "app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path $BuiltApk)) { throw "Gradle reported success but APK was not found: $BuiltApk" }
$OutputApk = Join-Path $DistRoot "LunaaAdaptiveAod-v1.6.7-build.apk"
Copy-Item -Force $BuiltApk $OutputApk

Write-Step "Verifying APK metadata and legacy Xposed entry"
$Jar = (Get-Command jar -ErrorAction Stop).Source
$Entries = & $Jar tf $OutputApk
if ($LASTEXITCODE -ne 0) { throw "Could not inspect APK as a ZIP archive." }
if (-not ($Entries -contains "assets/xposed_init")) { throw "APK is missing assets/xposed_init" }

$Aapt2 = Join-Path $SdkRoot "build-tools\35.0.0\aapt2.exe"
if (-not (Test-Path $Aapt2)) { throw "aapt2.exe is missing from build-tools 35.0.0." }
$Badging = (& $Aapt2 dump badging $OutputApk) -join "`n"
if ($LASTEXITCODE -ne 0) { throw "aapt2 could not read APK metadata." }
if ($Badging -notmatch "package: name='dev\.lunaa\.aod'") { throw "Unexpected APK package id." }
if ($Badging -notmatch "targetSdkVersion:'36'") { throw "Unexpected targetSdkVersion." }

$hash = (Get-FileHash -Algorithm SHA256 $OutputApk).Hash.ToLowerInvariant()
Write-Host "`nBUILD OK" -ForegroundColor Green
Write-Host "APK:    $OutputApk"
Write-Host "SHA256: $hash"
Write-Host "Next: adb install -r `"$OutputApk`""
