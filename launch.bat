@echo off
set JAR_FILE=%~dp0target\personal-gain-tracking-system-1.0.0.jar

if not exist "%JAR_FILE%" (
    echo Building application package...
    call "%~dp0mvnw.cmd" package -DskipTests
)

start javaw -jar "%JAR_FILE%"
exit /b 0
