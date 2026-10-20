@echo off
echo ====================================================
echo   Personal Gain Tracker - Auto Push to GitHub
echo ====================================================
git add -A
set commit_msg=%*
if "%commit_msg%"=="" set commit_msg=Auto-sync: %date% %time%
git commit -m "%commit_msg%"
git push origin main
echo ====================================================
echo   Push complete!
echo ====================================================
if "%1"=="" timeout /t 3
