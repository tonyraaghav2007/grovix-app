# Grovix Business Management App & Web Platform - Master Build & Deploy Script
# Builds the Responsive Web Platform, Native Android App (APK), and prepares 24/7 Cloud deployment.

$projectDir = "C:\Users\legion-5pro\Documents\Grovix app"
$desktopDir = "C:\Users\legion-5pro\Desktop"
$androidDir = "$projectDir\android"
$serverDir = "$projectDir\server"
$clientDir = "$projectDir\client"
$desktopBuildDir = "$desktopDir\Grovix App Android Build"
$desktopApk = "$desktopDir\Grovix-App.apk"

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "   GROVIX MULTI-DEVICE PLATFORM - WEB & ANDROID MASTER BUILD " -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

# Step 1: Remove previous builds from Desktop
Write-Host "[1/6] Cleaning previous builds from Desktop..." -ForegroundColor Yellow

if (Test-Path $desktopApk) {
    Remove-Item -Force $desktopApk
    Write-Host " - Removed previous Grovix-App.apk" -ForegroundColor DarkGray
}

if (Test-Path $desktopBuildDir) {
    Remove-Item -Recurse -Force $desktopBuildDir
    Write-Host " - Removed previous Grovix App Android Build folder" -ForegroundColor DarkGray
}

if (Test-Path "$desktopDir\Grovix App Build") {
    Remove-Item -Recurse -Force "$desktopDir\Grovix App Build"
}

# Step 2: Build Responsive Web Frontend Application
Write-Host "[2/6] Building Responsive Web Application (React)..." -ForegroundColor Green
Set-Location $clientDir
cmd.exe /c npm run build

$webBuildDir = "$clientDir\build"
if (!(Test-Path $webBuildDir)) {
    Write-Host "ERROR: React Web build failed! $webBuildDir not found" -ForegroundColor Red
    exit 1
}

$serverPublicDir = "$serverDir\public"
if (Test-Path $serverPublicDir) {
    Remove-Item -Recurse -Force $serverPublicDir
}
Copy-Item -Recurse $webBuildDir $serverPublicDir
Write-Host " -> Web application successfully synchronized to server/public!" -ForegroundColor Cyan

# Step 3: Build Native Android APK using Gradle & Android Studio JBR
Write-Host "[3/6] Compiling native Android Kotlin app..." -ForegroundColor Green
[System.Environment]::SetEnvironmentVariable('JAVA_HOME', 'C:\Program Files\Android\Android Studio\jbr', 'Process')
[System.Environment]::SetEnvironmentVariable('ANDROID_HOME', 'C:\Users\legion-5pro\AppData\Local\Android\Sdk', 'Process')

Set-Location $androidDir
cmd.exe /c gradlew.bat assembleDebug

$builtApk = "$androidDir\app\build\outputs\apk\debug\app-debug.apk"
if (!(Test-Path $builtApk)) {
    Write-Host "ERROR: Android build failed! APK not found at $builtApk" -ForegroundColor Red
    exit 1
}

# Step 4: Deploy APK directly to Desktop
Write-Host "[4/6] Deploying latest build to Desktop..." -ForegroundColor Green
Copy-Item $builtApk $desktopApk -Force
Write-Host " -> Saved latest APK to: $desktopApk" -ForegroundColor Cyan

# Create dedicated build directory on Desktop
New-Item -ItemType Directory -Path $desktopBuildDir -Force | Out-Null
Copy-Item $builtApk "$desktopBuildDir\Grovix-App.apk" -Force

# Create Web & Cloud Launchers on Desktop
$webServerLauncher = @"
@echo off
title Grovix Web Platform & API Server
color 0B
echo ========================================================================
echo   STARTING GROVIX WEB PLATFORM & CENTRAL API SERVER (PORT 5000)...
echo ========================================================================
cd /d "$serverDir"
start "" http://localhost:5000
node index.js
pause
"@
$webServerLauncher | Out-File -FilePath "$desktopDir\Start-Grovix-Web-Platform.bat" -Encoding ASCII
$webServerLauncher | Out-File -FilePath "$desktopBuildDir\Start-Grovix-Web-Platform.bat" -Encoding ASCII
Write-Host " -> Created Web Platform Launcher on Desktop: Start-Grovix-Web-Platform.bat" -ForegroundColor Cyan

# Copy Central Cloud Server Tunnel Launcher
$cloudServerScript = "$serverDir\Start-Grovix-Cloud-Server.bat"
if (Test-Path $cloudServerScript) {
    Copy-Item $cloudServerScript "$desktopDir\Start-Grovix-Cloud-Server.bat" -Force
    Copy-Item $cloudServerScript "$desktopBuildDir\Start-Grovix-Cloud-Server.bat" -Force
}

# Step 5: Create 1-click ADB installer script
Write-Host "[5/6] Generating 1-click device installer..." -ForegroundColor Green
$adbPath = "C:\Users\legion-5pro\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$installerBat = @"
@echo off
title Grovix Android App Installer
echo ====================================================
echo     GROVIX ANDROID APP - 1-CLICK ADB INSTALLER
echo ====================================================
echo.
echo Checking for connected Android devices or emulators...
"$adbPath" devices
echo.
echo Installing Grovix-App.apk...
"$adbPath" install -r "%~dp0Grovix-App.apk"
if %ERRORLEVEL% EQU 0 (
    echo.
    echo [SUCCESS] Grovix App installed successfully!
    echo Launching Grovix on your device...
    "$adbPath" shell am start -n com.example.grovix/.MainActivity
) else (
    echo.
    echo Note: If no device was found, connect your Android phone via USB
    echo with USB Debugging enabled, or start an Android Emulator.
    echo Alternatively, copy Grovix-App.apk directly to your phone and tap to install.
)
echo.
pause
"@
$installerBat | Out-File -FilePath "$desktopBuildDir\INSTALL-ON-DEVICE.bat" -Encoding ASCII

# Step 6: Write comprehensive README with multi-device access and cloud sync guide
Write-Host "[6/6] Writing documentation and credentials..." -ForegroundColor Green

$readme = @"
===================================================================
             GROVIX MULTI-DEVICE BUSINESS MANAGEMENT PLATFORM
        WEB APPLICATION (ANY DEVICE) + NATIVE ANDROID MOBILE APP
              WITH REAL-TIME 24/7 CLOUD SYNCHRONIZATION
===================================================================

LATEST BUILD LOCATIONS:
   Android APK File:  C:\Users\legion-5pro\Desktop\Grovix-App.apk
   Web & Build Pack:  C:\Users\legion-5pro\Desktop\Grovix App Android Build
   Web Launcher:      C:\Users\legion-5pro\Desktop\Start-Grovix-Web-Platform.bat
   Source Code:       C:\Users\legion-5pro\Documents\Grovix app

-------------------------------------------------------------------
HOW TO ACCESS ON ANY DEVICE:
-------------------------------------------------------------------
1. ANY PHONE, TABLET, LAPTOP, OR DESKTOP (WEB PLATFORM):
   - Access via any modern browser (Chrome, Safari, Edge, Firefox).
   - Works seamlessly on iPhones, iPads, Androids, MacBooks, and Windows PCs.
   - PWA Installable: On iPhone (Safari) or Android (Chrome), tap 
     "Add to Home Screen" to install Grovix as a native-feeling app!

2. NATIVE ANDROID APP (APK):
   - Send 'Grovix-App.apk' to your colleagues via WhatsApp, Drive, or email.
   - Tap the APK file to install it on any Android device.
   - Offline resilience: works offline and auto-syncs when online.

3. UNIFIED CLOUD SYNCHRONIZATION:
   - Both the Web Application and the Android App talk to the exact same
     central backend database!
   - Actions performed on an Android phone (creating a task, recording a
     sale in INR, logging an expense) update instantly on the website.
   - Updates made on the website reflect on all mobile devices in real time!

-------------------------------------------------------------------
24/7 CLOUD HOSTING (RECOMMENDED FOR PERMANENT REAL-TIME SYNC):
-------------------------------------------------------------------
To keep Grovix online 24/7 so that team members can access it anywhere
without your PC needing to stay on:

1. RENDER.COM (FREE 24/7 HOSTING):
   - Create a free account at https://render.com
   - Push this workspace to your GitHub repository.
   - On Render, click 'New +' -> 'Web Service' and select the repository.
   - Build Command: npm install && npm run build
   - Start Command: npm start
   - Render gives you a free permanent HTTPS URL (e.g. https://grovix.onrender.com).
   - Visiting that URL opens the website on ANY device!
   - On the Android App: tap the Cloud URL badge on the login screen or in
     Settings -> Central Cloud Sync and enter your Render URL.

2. LOCAL / DEV RUNNER:
   - Double-click 'Start-Grovix-Web-Platform.bat' to test locally on http://localhost:5000.
   - Double-click 'Start-Grovix-Cloud-Server.bat' for temporary public tunnel via localtunnel.

-------------------------------------------------------------------
DEFAULT PRE-SEEDED ACCOUNTS (Password: Grovix@2024):
-------------------------------------------------------------------
1. DIRECTORS / OWNERS (Power Level: 100 - Full Authority)
   - Shivesh Garg:  shivesh@grovix.com (or director1@grovix.com)
   - Sushant Singh: sushant@grovix.com (or director2@grovix.com)
   - Yuvik:         yuvik@grovix.com   (or director3@grovix.com)
   * Note: The 3 Founders are strictly protected and cannot deactivate or remove each other.

2. OPERATIONS MANAGER (Power Level: 75)
   - manager@grovix.com
   Abilities: Manage staff below them, create employee & guest accounts,
   assign tasks, manage sales & expenses in INR (₹), view inventory and analytics.

3. FULL-STACK DEVELOPER (Power Level: 50 - Dedicated Workspace)
   - developer@grovix.com (or dev@grovix.com)
   Abilities: Focused strictly on assigned tasks and deliverables.
   Can mark tasks In Progress, Delayed, or Completed.

4. STAFF ASSOCIATE (Power Level: 50)
   - employee@grovix.com

-------------------------------------------------------------------
KEY FEATURES:
-------------------------------------------------------------------
- Cross-platform: Responsive Web (any device) + Native Android APK.
- INR (₹) Currency across all sales, expenses, and financial reports.
- Real-time two-way synchronization between website and phones.
- 5 Time Range Filters: "Today", "This Week", "This Month", "3 Months", "6 Months".
- Task Details Modal with rich formatted Big Notes (Bold, Underline, Bullets).
- Dark Mode optimized interfaces.
- PWA support with "Add to Home Screen" on mobile web browsers.
"@

[System.IO.File]::WriteAllText("$desktopBuildDir\README.txt", $readme, [System.Text.Encoding]::UTF8)

Write-Host ""
Write-Host "============================================================" -ForegroundColor Green
Write-Host " BUILD & PACKAGING COMPLETE!" -ForegroundColor Green
Write-Host " Latest Android APK:  $desktopApk" -ForegroundColor Cyan
Write-Host " Web & Build Folder:  $desktopBuildDir" -ForegroundColor Cyan
Write-Host " Web Launcher:        $desktopDir\Start-Grovix-Web-Platform.bat" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Green
