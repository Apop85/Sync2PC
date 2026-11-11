# LeoSync QA: lokale SMB-Testfreigabe fuer den Emulator (nur Testdaten).
#   tools\qa-smb-testshare.ps1            -> Start (Einrichtung beim ersten Mal)
#   tools\qa-smb-testshare.ps1 -Stop      -> Server beenden, adb reverse entfernen
#   tools\qa-smb-testshare.ps1 -Status    -> laeuft der Server, ist adb reverse aktiv
#   -ThrottleKBps <n>                     -> Datenrate auf n KB/s bremsen (Datei waehrend des Laufs aendern, Abbruch von Hand)
#   -DropAfterBytes <n> [-DropCount <k>]  -> Verbindung nach n Datenbytes hart trennen (die ersten k Verbindungen, Standard 1)
#   (Throttle/Drop gelten nur beim Start; laufenden Server vorher mit -Stop beenden)
# Ablauf: Python-venv mit impacket (einmalig, PyPI) -> Freigabeordner .build\qa-smb-share -> Python-SMB-Server auf
# 127.0.0.1:4445 -> "adb reverse tcp:445 tcp:4445". Im Emulator erreicht die App den Server dann als \\127.0.0.1\QA
# (oder \\localhost\QA: anderer Hostname, gleicher Server, geeignet zum Testen des Hostwechsels).
# Zugang im Profil: Benutzer qa, Passwort qa-test-1 (nur Testdaten). Windows belegt Port 445 selbst, deshalb 4445 + reverse.
# Hinweis: Datei ist ASCII (PowerShell 5.1 liest UTF-8 ohne BOM sonst falsch).
param(
    [switch]$Stop,
    [switch]$Status,
    [string]$Share = 'QA',
    [string]$User = 'qa',
    [string]$Password = 'qa-test-1',
    [int]$Port = 4445,
    [string]$Guid = '',
    [string]$Serial = '',
    [int]$ThrottleKBps = 0,
    [int]$DropAfterBytes = 0,
    [int]$DropCount = 1
)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$work = Join-Path $env:LOCALAPPDATA 'LeoSyncTestShare'
$venv = Join-Path $work 'venv'
$py = Join-Path $venv 'Scripts\python.exe'
$shareDir = Join-Path $root '.build\qa-smb-share'
$pidFile = Join-Path $root '.build\qa-smb-testshare.pid'
$log = Join-Path $root '.build\qa-smb-testshare.log'
$adb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
if (-not (Test-Path $adb)) { $adb = 'adb' }
$adbArgs = @(); if ($Serial) { $adbArgs = @('-s', $Serial) }

function Get-ServerProc {
    if (Test-Path $pidFile) {
        $id = [int](Get-Content $pidFile -Raw)
        return Get-Process -Id $id -ErrorAction SilentlyContinue
    }
    return $null
}

if ($Status) {
    $p = Get-ServerProc
    if ($p) { "Server laeuft (PID $($p.Id)), Freigabe $shareDir" } else { 'Server laeuft nicht' }
    & $adb @adbArgs reverse --list
    return
}

if ($Stop) {
    $p = Get-ServerProc
    if ($p) { Stop-Process -Id $p.Id -Force; 'Server beendet' } else { 'Server lief nicht' }
    Remove-Item $pidFile -ErrorAction SilentlyContinue
    & $adb @adbArgs reverse --remove tcp:445 2>$null
    return
}

# 1) venv + impacket (einmalig)
if (-not (Test-Path $py)) {
    Write-Host 'Erstelle venv und installiere impacket (einmalig, PyPI) ...'
    New-Item -ItemType Directory -Force $work | Out-Null
    python -m venv $venv
    & $py -m pip install --quiet impacket
}

# 2) Freigabeordner mit Testdateien (nur anlegen, nichts ueberschreiben)
New-Item -ItemType Directory -Force $shareDir | Out-Null
$hello = Join-Path $shareDir 'hallo.txt'
if (-not (Test-Path $hello)) { [IO.File]::WriteAllText($hello, "LeoSync Testfreigabe`r`n", [Text.UTF8Encoding]::new($false)) }

# 3) Server starten (falls nicht schon aktiv)
if (Get-ServerProc) {
    'Server laeuft bereits'
} else {
    $srvArgs = @((Join-Path $PSScriptRoot 'qa-smb-testshare.py'), '--dir', $shareDir, '--share', $Share, '--user', $User,
        '--password', $Password, '--port', $Port)
    if ($Guid) { $srvArgs += @('--guid', $Guid) }
    if ($ThrottleKBps -gt 0) { $srvArgs += @('--throttle-kbps', $ThrottleKBps) }
    if ($DropAfterBytes -gt 0) { $srvArgs += @('--drop-after-bytes', $DropAfterBytes, '--drop-count', $DropCount) }
    $proc = Start-Process -FilePath $py -ArgumentList $srvArgs -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput $log -RedirectStandardError "$log.err"
    [IO.File]::WriteAllText($pidFile, [string]$proc.Id)
    Start-Sleep -Seconds 2
    if ($proc.HasExited) { throw "Server beendet sich sofort, siehe $log.err" }
}

# 4) Emulator: 445 -> Host:4445
# Port 445 ist privilegiert: ohne Root verweigert adbd das Binden, deshalb bei Fehler "adb root" (nur Emulator-Images
# mit userdebug) und erneut versuchen.
& $adb @adbArgs reverse tcp:445 "tcp:$Port" 2>$null
if ($LASTEXITCODE -ne 0) {
    & $adb @adbArgs root | Out-Null
    Start-Sleep -Seconds 3
    & $adb @adbArgs wait-for-device
    & $adb @adbArgs reverse tcp:445 "tcp:$Port"
    if ($LASTEXITCODE -ne 0) { Write-Warning 'adb reverse auf Port 445 fehlgeschlagen (kein Root im Emulator-Image?)' }
}

"Bereit. Profil im Emulator: Freigabe-Pfad \\127.0.0.1\$($Share.ToUpper()), Benutzer $User, Passwort $Password"
"Freigabeordner: $shareDir"
