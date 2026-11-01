@echo off
rem LeoSync-Import: Fotos/Videos aus einem PC-Ordner in den Share einsortieren (Doppelklick genuegt)
chcp 65001 >nul
powershell -NoProfile -ExecutionPolicy Bypass -STA -File "%~dp0LeoSync-Import.ps1" %*
echo.
pause
