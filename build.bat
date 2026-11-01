@echo off
rem Sync2PC One-Click-Builder - Doppelklick genuegt. Optionen: -Force -SkipTests -Debug
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\build.ps1" %*
set RC=%ERRORLEVEL%
echo.
pause
exit /b %RC%
