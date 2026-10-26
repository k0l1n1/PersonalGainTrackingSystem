@echo off
setlocal enabledelayedexpansion

rem Validate current JAVA_HOME if set
if defined JAVA_HOME (
    if not exist "%JAVA_HOME%\bin\java.exe" (
        set "JAVA_HOME="
    )
)

rem If JAVA_HOME is invalid or not set, search known installed JDK locations
if not defined JAVA_HOME (
    if exist "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot\bin\java.exe" (
        set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
    )
)

rem If still not found, check javac/java from PATH
if not defined JAVA_HOME (
    for /f "tokens=*" %%i in ('where.exe java 2^>nul') do (
        if not defined JAVA_HOME (
            if exist "%%~dpi..\bin\javac.exe" (
                set "JAVA_HOME=%%~dpi.."
            )
        )
    )
)

set "JAR_FILE=%~dp0target\personal-gain-tracking-system-1.0.0.jar"

if not exist "%JAR_FILE%" (
    echo Building application package...
    call "%~dp0mvnw.cmd" package -DskipTests
    if errorlevel 1 (
        echo.
        echo [ERROR] Maven build failed. Please check the logs above.
        pause
        exit /b 1
    )
)

if not exist "%JAR_FILE%" (
    echo.
    echo [ERROR] Application jar file was not found at: "%JAR_FILE%"
    pause
    exit /b 1
)

echo Starting Personal Gain Tracking System...
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\javaw.exe" (
        start "" "%JAVA_HOME%\bin\javaw.exe" -jar "%JAR_FILE%"
        exit /b 0
    )
    if exist "%JAVA_HOME%\bin\java.exe" (
        start "" "%JAVA_HOME%\bin\java.exe" -jar "%JAR_FILE%"
        exit /b 0
    )
)

start "" javaw -jar "%JAR_FILE%"
exit /b 0
