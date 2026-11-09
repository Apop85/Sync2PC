# Prueft vor einer Veroeffentlichung, ob Quellen persoenliche Daten enthalten (SYNC-20261109-069).
# Sucht: Windows-Benutzername, absolute Pfade C:\Users\..., IPv4-Adressen ausserhalb der Dokumentationsbereiche
# (RFC 5737: 192.0.2.x, 198.51.100.x, 203.0.113.x) und ausserhalb der Beispieladressen 192.168.1.x / 192.168.0.x / 10.0.0.x.
# Aufruf: powershell -ExecutionPolicy Bypass -File tools\check-public-data.ps1   (Exit 1 bei Treffern)
# Hinweis: Nur ASCII in dieser Datei (PowerShell 5.1 liest ohne BOM als ANSI).
param([string]$Root = (Split-Path -Parent $PSScriptRoot))
$skip = '\\(\.build|\.git|\.gradle|build|releases|keystore|AppScreenshots|node_modules)\\'
$ext = '*.kt','*.xml','*.ps1','*.py','*.md','*.bat','*.kts','*.gradle','*.properties','*.json','*.cs'
$user = [regex]::Escape($env:USERNAME)
$rules = @(
    @{ Name = 'Benutzername'; Rx = "(?i)\b$user\b" },
    @{ Name = 'Pfad C:\Users'; Rx = '(?i)[A-Z]:\\+Users\\+[^\\\s"'']+' },
    @{ Name = 'IPv4 (nicht Beispiel)'; Rx = '\b(?!192\.0\.2\.|198\.51\.100\.|203\.0\.113\.|192\.168\.1\.|192\.168\.0\.|10\.0\.0\.|127\.|0\.0\.0\.0)(?:(?:25[0-5]|2[0-4]\d|1?\d?\d)\.){3}(?:25[0-5]|2[0-4]\d|1?\d?\d)\b' }
)
$hits = 0
Get-ChildItem $Root -Recurse -File -Include $ext | Where-Object { $_.FullName -notmatch $skip -and $_.Name -ne 'local.properties' -and $_.Name -ne 'check-public-data.ps1' } | ForEach-Object {
    $f = $_
    $i = 0
    foreach ($line in [IO.File]::ReadLines($f.FullName)) {
        $i++
        foreach ($r in $rules) {
            if ($line -match $r.Rx) { $hits++; '{0}:{1}  [{2}]  {3}' -f $f.FullName.Substring($Root.Length + 1), $i, $r.Name, $line.Trim() }
        }
    }
}
if ($hits) { "$hits Treffer"; exit 1 } else { 'Keine Treffer'; exit 0 }
