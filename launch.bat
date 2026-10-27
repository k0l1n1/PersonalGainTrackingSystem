@echo off
setlocal enabledelayedexpansion

echo ====================================================
echo   Personal Gain Tracking System - Launcher
echo ====================================================

rem 1. Locate valid Java 21+ executable
set "JAVA_EXE="

rem Check existing JAVA_HOME if valid
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" (
        set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
    )
)

rem Fallback to Adoptium JDK 21
if not defined JAVA_EXE (
    if exist "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot\bin\java.exe" (
        set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
        set "JAVA_EXE=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot\bin\java.exe"
    )
)

rem Fallback to standard Oracle / Eclipse Adoptium paths
if not defined JAVA_EXE (
    for /d %%D in ("C:\Program Files\Eclipse Adoptium\jdk-21*" "C:\Program Files\Java\jdk-21*") do (
        if exist "%%D\bin\java.exe" (
            set "JAVA_HOME=%%D"
            set "JAVA_EXE=%%D\bin\java.exe"
        )
    )
)

rem Fallback to PATH java
if not defined JAVA_EXE (
    for /f "tokens=*" %%i in ('where.exe java 2^>nul') do (
        if not defined JAVA_EXE (
            set "JAVA_EXE=%%i"
        )
    )
)

if not defined JAVA_EXE (
    echo [ERROR] Java runtime was not found.
    echo Please install Java 21 or set JAVA_HOME to a valid JDK directory.
    pause
    exit /b 1
)

rem 2. Verify Application JAR exists, build if missing
set "JAR_FILE=%~dp0target\personal-gain-tracking-system-1.0.0.jar"
if not exist "%JAR_FILE%" (
    echo Application package not found. Building with Maven...
    call "%~dp0mvnw.cmd" clean package -DskipTests
    if errorlevel 1 (
        echo [ERROR] Maven build failed.
        pause
        exit /b 1
    )
)

rem 3. Launch Application
echo Starting application...
start "" "%JAVA_EXE%" -jar "%JAR_FILE%"
exit /b 0
