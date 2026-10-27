param(
    [string]$JdkHome = "",
    [string]$OutputZipPath = ""
)

$ErrorActionPreference = "Stop"
$root = (Get-Item .).FullName

if ([string]::IsNullOrWhiteSpace($OutputZipPath)) {
    $desktopPath = [System.Environment]::GetFolderPath([System.Environment+SpecialFolder]::Desktop)
    if ([string]::IsNullOrWhiteSpace($desktopPath)) { $desktopPath = Join-Path $env:USERPROFILE "Desktop" }
    $OutputZipPath = Join-Path $desktopPath "PersonalGainTrackingSystem-Windows.zip"
}

if ([string]::IsNullOrWhiteSpace($JdkHome)) {
    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME "bin\jpackage.exe"))) {
        $JdkHome = $env:JAVA_HOME
    } elseif (Test-Path "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot\bin\jpackage.exe") {
        $JdkHome = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
    } else {
        $found = Get-ChildItem "C:\Program Files\Eclipse Adoptium", "C:\Program Files\Java" -Filter "jpackage.exe" -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($found) {
            $JdkHome = $found.Directory.Parent.FullName
        } else {
            throw "Could not locate a JDK with jpackage.exe. Please specify -JdkHome."
        }
    }
}
Write-Host "=================================================================="
Write-Host " BUILDING WINDOWS DISTRIBUTION FOR PERSONAL GAIN TRACKING SYSTEM "
Write-Host "=================================================================="
Write-Host "Project root: $root"
Write-Host "JDK Home:     $JdkHome"
Write-Host "Target ZIP:   $OutputZipPath"

# 1. Verify JDK and jpackage
$jpackageExe = Join-Path $JdkHome "bin\jpackage.exe"
$javaExe = Join-Path $JdkHome "bin\java.exe"
if (-not (Test-Path $jpackageExe)) {
    throw "jpackage.exe not found at $jpackageExe"
}

# 2. Package Jar with Maven
Write-Host "`n[1/6] Compiling and packaging shaded application JAR with Maven..."
$env:JAVA_HOME = $JdkHome
$mvnCmd = Join-Path $root "mvnw.cmd"
& $mvnCmd clean package -DskipTests
if ($LASTEXITCODE -ne 0) {
    throw "Maven packaging failed with exit code $LASTEXITCODE"
}

$jarFile = Join-Path $root "target\personal-gain-tracking-system-1.0.0.jar"
if (-not (Test-Path $jarFile)) {
    throw "Shaded JAR not found at $jarFile"
}
$jarSizeMB = [Math]::Round((Get-Item $jarFile).Length / 1MB, 2)
Write-Host "Shaded JAR successfully built: $jarFile ($jarSizeMB MB)"

# 3. Prepare Staging Input Directory
Write-Host "`n[2/6] Preparing clean staging directory for jpackage..."
$stagingDir = Join-Path $root "target\staging-input"
if (Test-Path $stagingDir) { Remove-Item -Recurse -Force $stagingDir }
New-Item -ItemType Directory -Path $stagingDir -Force | Out-Null
Copy-Item $jarFile -Destination $stagingDir -Force

# 4. Run jpackage to create self-contained app-image
Write-Host "`n[3/6] Running jpackage to generate self-contained Windows application image..."
$packageDest = Join-Path $root "dist\package"
if (Test-Path $packageDest) { Remove-Item -Recurse -Force $packageDest }
New-Item -ItemType Directory -Path $packageDest -Force | Out-Null

$icoFile = Join-Path $root "src\main\resources\icons\logo.ico"
$jpackageArgs = @(
    "--type", "app-image",
    "--name", "PersonalGainTrackingSystem",
    "--app-version", "1.0.0",
    "--vendor", "Personal Gain",
    "--description", "Personal Gain Tracking System - Desktop Dashboard",
    "--input", $stagingDir,
    "--main-jar", "personal-gain-tracking-system-1.0.0.jar",
    "--main-class", "com.personaltracker.Main",
    "--dest", $packageDest
)

if (Test-Path $icoFile) {
    $jpackageArgs += @("--icon", $icoFile)
}

Write-Host "Executing jpackage..."
& $jpackageExe @jpackageArgs
if ($LASTEXITCODE -ne 0) {
    throw "jpackage failed with exit code $LASTEXITCODE"
}

$appImageDir = Join-Path $packageDest "PersonalGainTrackingSystem"
if (-not (Test-Path $appImageDir)) {
    throw "App image directory not found at $appImageDir"
}

# 5. Add Supporting Distribution Files
Write-Host "`n[4/6] Adding environment and launcher support files to distribution folder..."
if (Test-Path (Join-Path $root ".env")) {
    Copy-Item (Join-Path $root ".env") -Destination $appImageDir -Force
}
if (Test-Path (Join-Path $root ".env.example")) {
    Copy-Item (Join-Path $root ".env.example") -Destination $appImageDir -Force
}
if (Test-Path (Join-Path $root "trainer_roster.json")) {
    Copy-Item (Join-Path $root "trainer_roster.json") -Destination $appImageDir -Force
}
if (Test-Path (Join-Path $root "personal_tracker.properties")) {
    Copy-Item (Join-Path $root "personal_tracker.properties") -Destination $appImageDir -Force
}

# Launcher batch file for convenience
$batLauncherContent = @"
@echo off
setlocal
cd /d "%~dp0"
echo Starting Personal Gain Tracking System...
start "" "%~dp0PersonalGainTrackingSystem.exe"
"@
Set-Content -Path (Join-Path $appImageDir "Launch-PersonalGainTrackingSystem.bat") -Value $batLauncherContent -Encoding Ascii

# Readme text file
$readmeContent = @"
================================================================================
 PERSONAL GAIN TRACKING SYSTEM - WINDOWS STANDALONE DISTRIBUTION
================================================================================

HOW TO RUN:
1. Simply double-click "PersonalGainTrackingSystem.exe"
   (or run "Launch-PersonalGainTrackingSystem.bat").
2. No separate Java installation is required. A lightweight, pre-configured
   Java 21 runtime is already included in the "runtime" folder.

CONFIGURATION:
- The included ".env" file holds your Supabase project connection settings.
- All personal data, workouts, goals, and nutrition records synchronize in real time.

REQUIREMENTS:
- Windows 10 or Windows 11 (64-bit)

Enjoy your fitness tracking journey!
================================================================================
"@
Set-Content -Path (Join-Path $appImageDir "README.txt") -Value $readmeContent -Encoding Ascii

# 6. Create ZIP Archive Atomically
Write-Host "`n[5/6] Compressing distribution into ZIP archive..."
$tempZip = "$OutputZipPath.tmp"
if (Test-Path $tempZip) { Remove-Item -Force $tempZip }
if (Test-Path $OutputZipPath) { Remove-Item -Force $OutputZipPath }

Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::CreateFromDirectory(
    $packageDest,
    $tempZip,
    [System.IO.Compression.CompressionLevel]::Optimal,
    $false
)

# Move temp zip to final destination
Move-Item -Path $tempZip -Destination $OutputZipPath -Force

$zipItem = Get-Item $OutputZipPath
$zipSizeMB = [Math]::Round($zipItem.Length / 1MB, 2)
Write-Host "ZIP created successfully at: $OutputZipPath ($zipSizeMB MB, $($zipItem.Length) bytes)"

# 7. Test and Verify the ZIP
Write-Host "`n[6/6] Verifying and testing the generated ZIP archive..."
$verifyDir = Join-Path $env:TEMP "verify_personal_gain_zip"
if (Test-Path $verifyDir) { Remove-Item -Recurse -Force $verifyDir }

Write-Host "Extracting archive to verification directory: $verifyDir"
Expand-Archive -Path $OutputZipPath -DestinationPath $verifyDir -Force

$extractedExe = Join-Path $verifyDir "PersonalGainTrackingSystem\PersonalGainTrackingSystem.exe"
$extractedRuntime = Join-Path $verifyDir "PersonalGainTrackingSystem\runtime"
$extractedJar = Join-Path $verifyDir "PersonalGainTrackingSystem\app\personal-gain-tracking-system-1.0.0.jar"

if (-not (Test-Path $extractedExe)) { throw "Verification failed: Executable missing!" }
if (-not (Test-Path $extractedRuntime)) { throw "Verification failed: Runtime missing!" }
if (-not (Test-Path $extractedJar)) { throw "Verification failed: App JAR missing!" }

Write-Host "Verification SUCCESSFUL! All required files and bundled runtime verified."
Write-Host "=================================================================="
Write-Host " DISTRIBUTION READY: $OutputZipPath "
Write-Host "=================================================================="
