@echo off
setlocal
echo ==================================================================
echo  Building Personal Gain Tracking System Windows Distribution
echo ==================================================================
powershell -ExecutionPolicy Bypass -File "%~dp0scripts\BuildWindowsDist.ps1"
if errorlevel 1 (
    echo.
    echo [ERROR] Build distribution failed.
    pause
    exit /b 1
)
echo.
echo Distribution successfully built on Desktop:
echo "%USERPROFILE%\Desktop\PersonalGainTrackingSystem-Windows.zip"
pause
