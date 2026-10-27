@echo off
setlocal

set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
set "JAR_FILE=%~dp0target\personal-gain-tracking-system-1.0.0.jar"

if not exist "%JAVA_EXE%" (
    echo ERROR: Java 21 was not found.
    pause
    exit /b 1
)

if not exist "%JAR_FILE%" (
    echo ERROR: Application JAR was not found:
    echo "%JAR_FILE%"
    pause
    exit /b 1
)

echo Starting Personal Gain Tracking System...
"%JAVA_EXE%" -jar "%JAR_FILE%"

if errorlevel 1 (
    echo.
    echo The application exited with an error.
    pause
)
