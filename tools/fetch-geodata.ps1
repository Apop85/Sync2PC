# Laedt die Laendergrenzen (Natural Earth, Public Domain) fuer die Offline-Laendererkennung.
# Ziel: app/src/main/assets/geo/countries.geojson
# Aufruf: powershell -ExecutionPolicy Bypass -File tools\fetch-geodata.ps1 [-Force]
param(
    [string]$Root = (Split-Path -Parent $PSScriptRoot),
    [switch]$Force
)
$ErrorActionPreference = 'Stop'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$target = Join-Path $Root "app\src\main\assets\geo\countries.geojson"
if ((Test-Path $target) -and -not $Force) {
    Write-Host "Laenderdaten vorhanden: $target"
    exit 0
}
New-Item -ItemType Directory -Force (Split-Path $target) | Out-Null

# 1:50m = guter Kompromiss aus Genauigkeit an Grenzen und Groesse
$urls = @(
    "https://raw.githubusercontent.com/nvkelso/natural-earth-vector/v5.1.2/geojson/ne_50m_admin_0_countries.geojson",
    "https://raw.githubusercontent.com/nvkelso/natural-earth-vector/master/geojson/ne_50m_admin_0_countries.geojson"
)
$tmp = "$target.download"
foreach ($u in $urls) {
    try {
        Write-Host "Lade $u ..."
        Invoke-WebRequest -Uri $u -OutFile $tmp -UseBasicParsing
        $head = [IO.File]::ReadAllText($tmp, [Text.Encoding]::UTF8)
        if ($head -notmatch '"FeatureCollection"' -or $head -notmatch 'NAME_DE') { throw "Unerwarteter Inhalt" }
        Move-Item -Force $tmp $target
        $size = [math]::Round((Get-Item $target).Length / 1MB, 1)
        Write-Host "OK: $target ($size MB)"
        exit 0
    } catch {
        Write-Warning "Fehlgeschlagen: $($_.Exception.Message)"
        if (Test-Path $tmp) { Remove-Item -Force $tmp }
    }
}
Write-Error "Laenderdaten konnten nicht geladen werden. Die App funktioniert, legt aber keine Laenderordner an."
exit 1
