@echo off
rem LeoSync One-Click-Builder - Doppelklick genuegt. Optionen: -Force -SkipTests -Debug -NoPause
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\build.ps1" %*
set RC=%ERRORLEVEL%
echo.
rem -NoPause: ohne "Taste druecken" beenden (fuer automatische Aufrufe)
echo %* | find /i "-NoPause" >nul && exit /b %RC%
pause
exit /b %RC%
