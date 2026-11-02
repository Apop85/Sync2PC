# ============================================================================
#  LeoSync-Import
#  Sortiert Fotos und Videos aus einem PC-Ordner in den LeoSync-Share ein -
#  nach denselben Regeln wie die App:
#   - Datum: EXIF (Fotos) bzw. Metadaten (Videos, braucht ffprobe) -> Dateiname -> Aenderungsdatum
#   - Ziel:  <Share>\<Jahr>\<Monat>  bzw.  <Share>\<Jahr>\<Monat> - <Land>  (Heimatland ohne Land)
#   - Umbenannte Monatsordner: Rueckfrage (einmal pro Monat/Land)
#   - Keine Duplikate: gleicher Name (auch _2 ...), gleiche Groesse und Pruefsumme in allen
#     Ordnern desselben Monats -> wird uebersprungen
#   - Gleicher Name, anderer Inhalt -> name_2.jpg, name_3.jpg ...
#  Der Quellordner wird NICHT veraendert (es wird kopiert).
#
#  Start: Doppelklick auf tools\LeoSync-Import.bat
#  oder:  powershell -ExecutionPolicy Bypass -File tools\LeoSync-Import.ps1 -Source D:\Fotos -Target \\192.168.1.20\Fotos
#  Optionen: -HomeCountry CH   -Yes (ohne Bestaetigung kopieren)
# ============================================================================
param(
    [string]$Source,
    [string]$Target,
    [string]$HomeCountry,
    [switch]$Yes
)
$ErrorActionPreference = 'Stop'
try { [Console]::OutputEncoding = [Text.Encoding]::UTF8 } catch {}
$Root = Split-Path -Parent $PSScriptRoot

Add-Type -AssemblyName System.Drawing, System.Windows.Forms, System.Web.Extensions
Add-Type -TypeDefinition ([IO.File]::ReadAllText((Join-Path $PSScriptRoot "LeoSyncImport.cs"))) `
    -ReferencedAssemblies System.Drawing, System.Web.Extensions

$ImageExt = @('.jpg', '.jpeg', '.png', '.heic', '.heif', '.gif', '.webp', '.dng', '.tif', '.tiff', '.bmp', '.cr2', '.nef', '.arw')
$VideoExt = @('.mp4', '.mov', '.m4v', '.3gp', '.avi', '.mkv', '.mts', '.m2ts', '.wmv')

function Fmt([long]$b) {
    if ($b -ge 1GB) { return "{0:N1} GB" -f ($b / 1GB) }
    if ($b -ge 1MB) { return "{0:N1} MB" -f ($b / 1MB) }
    return "{0:N0} KB" -f ($b / 1KB)
}

# ---- Einstellungen merken (letztes Ziel, Heimatland) -------------------------
$cfgDir = Join-Path $env:APPDATA "LeoSync"
$cfgFile = Join-Path $cfgDir "import.json"
$cfg = @{ target = ""; home = "CH"; lastSource = "" }
if (Test-Path $cfgFile) {
    try { $j = Get-Content $cfgFile -Raw | ConvertFrom-Json; foreach ($k in @("target", "home", "lastSource")) { if ($j.$k) { $cfg[$k] = $j.$k } } } catch {}
}

Write-Host ""
Write-Host "  LeoSync-Import - Fotos und Videos in den Share einsortieren" -ForegroundColor Cyan
Write-Host "  ---------------------------------------------------------"
Write-Host ""

# ---- Quelle -------------------------------------------------------------------
if (-not $Source) {
    Write-Host "Bitte den Quellordner waehlen (Dialog oeffnet sich, evtl. hinter diesem Fenster) ..."
    $dlg = New-Object System.Windows.Forms.FolderBrowserDialog
    $dlg.Description = "LeoSync-Import: Quellordner mit Fotos/Videos waehlen"
    $dlg.ShowNewFolderButton = $false
    if ($cfg.lastSource -and (Test-Path $cfg.lastSource)) { $dlg.SelectedPath = $cfg.lastSource }
    if ($dlg.ShowDialog() -ne [System.Windows.Forms.DialogResult]::OK) { Write-Host "Abgebrochen."; exit 1 }
    $Source = $dlg.SelectedPath
}
if (-not (Test-Path -LiteralPath $Source -PathType Container)) { Write-Host "Quellordner nicht gefunden: $Source" -ForegroundColor Red; exit 1 }
Write-Host "Quelle: $Source"

# ---- Ziel -----------------------------------------------------------------------
if (-not $Target) {
    $hint = if ($cfg.target) { " [Enter = $($cfg.target)]" } else { " (z. B. \\192.168.1.20\Fotos)" }
    $inp = Read-Host "Ziel-Share$hint"
    $Target = if ($inp) { $inp.Trim('"', ' ') } else { $cfg.target }
}
if (-not $Target) { Write-Host "Kein Ziel angegeben." -ForegroundColor Red; exit 1 }
$Target = $Target.TrimEnd('\')
if (-not (Test-Path -LiteralPath $Target -PathType Container)) {
    Write-Host "Ziel nicht erreichbar: $Target" -ForegroundColor Red
    Write-Host "Tipp: Den Pfad einmal im Explorer oeffnen (Anmeldung am Share), dann erneut starten."
    exit 1
}
if (-not $HomeCountry) { $HomeCountry = $cfg.home }
$HomeCountry = $HomeCountry.ToUpper()
Write-Host "Ziel:   $Target   (Heimatland ohne Laenderzusatz: $HomeCountry)"

New-Item -ItemType Directory -Force $cfgDir | Out-Null
@{ target = $Target; home = $HomeCountry; lastSource = $Source } | ConvertTo-Json | Set-Content -Path $cfgFile -Encoding UTF8

# ---- Protokoll -------------------------------------------------------------------
$logDir = Join-Path $env:LOCALAPPDATA "LeoSync\import-logs"
New-Item -ItemType Directory -Force $logDir | Out-Null
$logFile = Join-Path $logDir ("import-" + (Get-Date -Format "yyyyMMdd-HHmmss") + ".log")
function Log([string]$s) { Add-Content -LiteralPath $logFile -Value ((Get-Date -Format "HH:mm:ss") + "  " + $s) -Encoding UTF8 }
Log "Quelle: $Source"; Log "Ziel: $Target"

# ---- Hilfen: Laenderdaten, ffprobe ------------------------------------------------
$geoPath = Join-Path $Root "app\src\main\assets\geo\countries.geojson"
$geo = New-Object LeoGeo $geoPath
if (-not $geo.Available) { Write-Host "Hinweis: Laenderdaten fehlen ($geoPath) - Ordner ohne Laenderzusatz." -ForegroundColor Yellow }
$ffprobe = (Get-Command ffprobe -ErrorAction SilentlyContinue).Source
if (-not $ffprobe) { Write-Host "Hinweis: ffprobe nicht gefunden - Videos werden nach Dateiname/Aenderungsdatum sortiert." -ForegroundColor Yellow }

function Get-VideoMeta([string]$path) {
    if (-not $ffprobe) { return $null }
    $ErrorActionPreference = 'Continue'
    try {
        $out = & $ffprobe -v quiet -print_format json -show_entries format_tags $path | Out-String
        $tags = ($out | ConvertFrom-Json).format.tags
        if (-not $tags) { return $null }
        $r = @{ Ym = $null; Lat = [double]::NaN; Lon = [double]::NaN }
        if ($tags.creation_time) {
            $dt = [DateTime]::Parse($tags.creation_time, [Globalization.CultureInfo]::InvariantCulture, [Globalization.DateTimeStyles]::RoundtripKind).ToLocalTime()
            if ($dt.Year -ge 1980) { $r.Ym = $dt.ToString("yyyy-MM") }
        }
        foreach ($p in $tags.PSObject.Properties) {
            if ($p.Name -like "*location*" -and "$($p.Value)" -match '([+-]\d+(?:\.\d+)?)([+-]\d+(?:\.\d+)?)') {
                $la = [double]::Parse($Matches[1], [Globalization.CultureInfo]::InvariantCulture)
                $lo = [double]::Parse($Matches[2], [Globalization.CultureInfo]::InvariantCulture)
                if (-not ($la -eq 0 -and $lo -eq 0)) { $r.Lat = $la; $r.Lon = $lo }
                break
            }
        }
        return $r
    } catch { return $null }
}

# ---- 1. Dateien suchen und analysieren -------------------------------------------
Write-Host ""
Write-Host "Suche Dateien ..."
$allExt = $ImageExt + $VideoExt
$files = @(Get-ChildItem -LiteralPath $Source -Recurse -File -ErrorAction SilentlyContinue |
    Where-Object { $allExt -contains $_.Extension.ToLower() })
if ($files.Count -eq 0) { Write-Host "Keine Fotos/Videos gefunden."; exit 0 }
Write-Host "$($files.Count) Fotos/Videos gefunden. Analysiere Datum und Ort ..."

$items = New-Object System.Collections.Generic.List[object]
$n = 0
foreach ($f in $files) {
    $n++
    if ($n % 25 -eq 0) { Write-Progress -Activity "Analysiere" -Status "$n / $($files.Count)" -PercentComplete (100 * $n / $files.Count) }
    $isVideo = $VideoExt -contains $f.Extension.ToLower()
    $ym = $null; $src = ""; $lat = [double]::NaN; $lon = [double]::NaN
    if ($isVideo) {
        $m = Get-VideoMeta $f.FullName
        if ($m) { $ym = $m.Ym; $lat = $m.Lat; $lon = $m.Lon; if ($ym) { $src = "META" } }
    } else {
        $la = 0.0; $lo = 0.0
        $ym = [LeoMedia]::ReadExif($f.FullName, [ref]$la, [ref]$lo)
        if ($ym) { $src = "EXIF" }
        $lat = $la; $lon = $lo
    }
    if (-not $ym) { $ym = [LeoMedia]::DateFromName($f.Name); if ($ym) { $src = "DATEINAME" } }
    if (-not $ym) { $ym = $f.LastWriteTime.ToString("yyyy-MM"); $src = "AENDERUNGSDATUM" }
    $iso = $null; $cname = $null
    if (-not [double]::IsNaN($lat) -and $geo.Available) {
        $c = $geo.Lookup($lat, $lon)
        if ($c) { $iso = $c[0]; $cname = $c[1] }
    }
    $items.Add([pscustomobject]@{
        File = $f; IsVideo = $isVideo; Year = [int]$ym.Substring(0, 4); Month = [int]$ym.Substring(5, 2)
        DateSource = $src; Iso = $iso; Country = $cname
    })
}
Write-Progress -Activity "Analysiere" -Completed

# Live-Fotos/Bewegtbilder: Video mit gleichem Namen wie ein Foto im selben Ordner -> gleicher Zielordner
$imgByBase = @{}
foreach ($it in $items) { if (-not $it.IsVideo) { $imgByBase[($it.File.DirectoryName + "\" + $it.File.BaseName).ToLower()] = $it } }
foreach ($it in $items) {
    if ($it.IsVideo) {
        $p = $imgByBase[($it.File.DirectoryName + "\" + $it.File.BaseName).ToLower()]
        if ($p) { $it.Year = $p.Year; $it.Month = $p.Month; $it.Iso = $p.Iso; $it.Country = $p.Country }
    }
}
# Heimatland ohne Laenderzusatz
foreach ($it in $items) { if ($it.Iso -and $it.Iso -eq $HomeCountry) { $it.Country = $null; $it.Iso = $null } }

# ---- 2. Zielordner bestimmen (Rueckfrage bei umbenannten Ordnern) ------------------
$yearFolders = @{}
function Get-YearFolders([int]$year) {
    if (-not $yearFolders.ContainsKey($year)) {
        $p = Join-Path $Target $year
        $list = New-Object System.Collections.ArrayList
        if (Test-Path -LiteralPath $p) { Get-ChildItem -LiteralPath $p -Directory | ForEach-Object { [void]$list.Add($_.Name) } }
        $yearFolders[$year] = $list
    }
    return , $yearFolders[$year]
}

$groupFolder = @{}
$groups = $items | Group-Object { "{0:D4}|{1:D2}|{2}" -f $_.Year, $_.Month, $_.Country } | Sort-Object Name
foreach ($g in $groups) {
    $first = $g.Group[0]
    $year = $first.Year; $month = $first.Month; $country = $first.Country
    $folders = Get-YearFolders $year
    $default = [LeoNames]::DefaultName($month, $country)
    $existing = $folders | Where-Object { $_ -ieq $default } | Select-Object -First 1
    if ($existing) { $groupFolder[$g.Name] = $existing; continue }
    $cands = @($folders | Where-Object { [LeoNames]::MonthOf($_) -eq $month -and -not [LeoNames]::IsGenerated($_, $geo.Names) } | Sort-Object)
    if ($cands.Count -eq 0) {
        $groupFolder[$g.Name] = $default
        [void]$folders.Add($default)
        continue
    }
    Write-Host ""
    $label = "{0}\{1:D2}" -f $year, $month
    if ($country) { $label += " ($country)" }
    Write-Host "$label - $($g.Count) Dateien. Fuer diesen Monat gibt es umbenannte Ordner. Wohin?" -ForegroundColor Yellow
    $opts = @($cands) + @($default)
    for ($k = 0; $k -lt $opts.Count; $k++) {
        $txt = if ($k -eq $opts.Count - 1) { "Neuer Ordner: $year\$($opts[$k])" } else { "$year\$($opts[$k])" }
        Write-Host ("  [{0}] {1}" -f ($k + 1), $txt)
    }
    $a = ""
    do {
        $a = Read-Host "Auswahl 1-$($opts.Count)"
        if ($null -eq $a) { Write-Host "Keine Eingabe moeglich - abgebrochen, es wurde nichts kopiert."; exit 1 }
        $a = "$a".Trim()
    } until ($a -match '^\d+$' -and [int]$a -ge 1 -and [int]$a -le $opts.Count)
    $choice = $opts[[int]$a - 1]
    $groupFolder[$g.Name] = $choice
    if (-not ($folders | Where-Object { $_ -ieq $choice })) { [void]$folders.Add($choice) }
}

# ---- 3. Duplikate pruefen und Dateinamen festlegen ----------------------------------
Write-Host ""
Write-Host "Pruefe auf Duplikate im Share ..."
$listings = @{}   # Ordnerpfad (klein) -> Liste vorhandener Dateien
function Get-Listing([string]$dir) {
    $key = $dir.ToLower()
    if (-not $listings.ContainsKey($key)) {
        $list = New-Object System.Collections.ArrayList
        if (Test-Path -LiteralPath $dir) {
            Get-ChildItem -LiteralPath $dir -File | Where-Object { $_.Extension -ne ".s2ptmp" } |
                ForEach-Object { [void]$list.Add([pscustomobject]@{ Name = $_.Name; Size = $_.Length; Path = $_.FullName }) }
        }
        $listings[$key] = $list
    }
    return , $listings[$key]
}
$hashCache = @{}
function Get-Hash([string]$path, [long]$size) {
    if (-not $hashCache.ContainsKey($path)) { $hashCache[$path] = [LeoMedia]::PartialHash($path, $size) }
    return $hashCache[$path]
}

$plan = New-Object System.Collections.Generic.List[object]
$n = 0
foreach ($it in ($items | Sort-Object { $_.File.FullName })) {
    $n++
    if ($n % 25 -eq 0) { Write-Progress -Activity "Pruefe Duplikate" -Status "$n / $($items.Count)" -PercentComplete (100 * $n / $items.Count) }
    $key = "{0:D4}|{1:D2}|{2}" -f $it.Year, $it.Month, $it.Country
    $folder = $groupFolder[$key]
    $yearDir = Join-Path $Target $it.Year
    $targetDir = Join-Path $yearDir $folder
    $name = [LeoNames]::Sanitize($it.File.Name)
    $size = $it.File.Length
    $dup = $null
    # in allen Ordnern desselben Monats (inkl. bereits geplanter Kopien) nach gleicher Datei suchen
    $monthDirs = @((Get-YearFolders $it.Year) | Where-Object { [LeoNames]::MonthOf($_) -eq $it.Month } | ForEach-Object { Join-Path $yearDir $_ })
    if ($monthDirs -notcontains $targetDir) { $monthDirs += $targetDir }
    foreach ($d in $monthDirs) {
        foreach ($e in (Get-Listing $d)) {
            if ($e.Size -ne $size -or -not [LeoNames]::SameBaseName($e.Name, $name)) { continue }
            if ((Get-Hash $e.Path $e.Size) -eq (Get-Hash $it.File.FullName $size)) { $dup = $e.Path; break }
        }
        if ($dup) { break }
    }
    if ($dup) {
        $plan.Add([pscustomobject]@{ Item = $it; Action = "VORHANDEN"; Dest = $dup; Folder = "$($it.Year)\$folder" })
        continue
    }
    # freien Namen im Zielordner finden: bild.jpg, bild_2.jpg, ...
    $listing = Get-Listing $targetDir
    $c = $name; $k = 1
    while ($listing | Where-Object { $_.Name -ieq $c }) { $k++; $c = [LeoNames]::ConflictName($name, $k) }
    $dest = Join-Path $targetDir $c
    # Als geplant eintragen (fuer Duplikate innerhalb der Quelle und die Namensvergabe)
    [void]$listing.Add([pscustomobject]@{ Name = $c; Size = $size; Path = $it.File.FullName })
    $plan.Add([pscustomobject]@{ Item = $it; Action = "KOPIEREN"; Dest = $dest; Folder = "$($it.Year)\$folder" })
}
Write-Progress -Activity "Pruefe Duplikate" -Completed

# ---- 4. Vorschau ------------------------------------------------------------------
$toCopy = @($plan | Where-Object { $_.Action -eq "KOPIEREN" })
$skip = @($plan | Where-Object { $_.Action -eq "VORHANDEN" })
$bytes = ($toCopy | ForEach-Object { $_.Item.File.Length } | Measure-Object -Sum).Sum
if (-not $bytes) { $bytes = 0 }
Write-Host ""
Write-Host "Vorschau:" -ForegroundColor Cyan
$plan | Group-Object Folder | Sort-Object Name | ForEach-Object {
    $new = @($_.Group | Where-Object { $_.Action -eq "KOPIEREN" }).Count
    $old = @($_.Group | Where-Object { $_.Action -eq "VORHANDEN" }).Count
    $line = "  {0,-40} {1,6} neu" -f $_.Name, $new
    if ($old -gt 0) { $line += ", $old bereits vorhanden" }
    Write-Host $line
}
$bySrc = $items | Group-Object DateSource | ForEach-Object { "$($_.Name): $($_.Count)" }
Write-Host ""
Write-Host ("Datum ermittelt aus: " + ($bySrc -join ", "))
Write-Host ("Zu kopieren: {0} Dateien ({1}), uebersprungen (bereits im Share): {2}" -f $toCopy.Count, (Fmt $bytes), $skip.Count)
foreach ($p in $plan) { Log ("{0}  {1}  ->  {2}" -f $p.Action, $p.Item.File.FullName, $p.Dest) }
if ($toCopy.Count -eq 0) { Write-Host "Nichts zu kopieren." -ForegroundColor Green; Write-Host "Protokoll: $logFile"; exit 0 }

if (-not $Yes) {
    Write-Host ""
    $a = Read-Host "Jetzt kopieren? (j/n)"
    # Nur ein ausdrueckliches "j"/"ja" startet das Kopieren (keine Eingabe = abbrechen)
    if ("$a".Trim() -notmatch '^(?i)(j|ja|y|yes)$') { Write-Host "Abgebrochen. Es wurde nichts kopiert."; exit 0 }
}

# ---- 5. Kopieren (Temp-Datei -> pruefen -> umbenennen) --------------------------------
$done = 0; $failed = 0; [long]$doneBytes = 0
foreach ($p in $toCopy) {
    $src = $p.Item.File
    Write-Progress -Activity "Kopiere nach $Target" -Status ("{0} / {1}  ({2} von {3})  {4}" -f ($done + $failed + 1), $toCopy.Count, (Fmt $doneBytes), (Fmt $bytes), $src.Name) `
        -PercentComplete ([int][math]::Min([double]100, [double]100 * $doneBytes / [math]::Max([double]1, [double]$bytes)))
    # Hinweis: [math]::Max(1, $bytes) waehlt die Int32-Variante und scheitert ab ca. 2 GB -> immer [double]
    $tmp = $p.Dest + ".s2ptmp"
    try {
        New-Item -ItemType Directory -Force (Split-Path $p.Dest) | Out-Null
        [IO.File]::Copy($src.FullName, $tmp, $true)
        $fi = Get-Item -LiteralPath $tmp
        $fi.CreationTime = $src.LastWriteTime
        $fi.LastWriteTime = $src.LastWriteTime
        if ($fi.Length -ne $src.Length -or [LeoMedia]::PartialHash($tmp, $fi.Length) -ne (Get-Hash $src.FullName $src.Length)) {
            throw "Pruefung nach dem Kopieren fehlgeschlagen"
        }
        if (Test-Path -LiteralPath $p.Dest) { throw "Zieldatei existiert inzwischen: $($p.Dest)" }
        [IO.File]::Move($tmp, $p.Dest)
        $done++; $doneBytes += $src.Length
        Log "OK  $($src.FullName)  ->  $($p.Dest)"
    } catch {
        $failed++
        Log "FEHLER  $($src.FullName): $($_.Exception.Message)"
        Write-Host "Fehler: $($src.Name): $($_.Exception.Message)" -ForegroundColor Red
        try { if (Test-Path -LiteralPath $tmp) { Remove-Item -LiteralPath $tmp -Force } } catch {}
    }
}
Write-Progress -Activity "Kopiere" -Completed

Write-Host ""
$color = if ($failed -gt 0) { "Yellow" } else { "Green" }
Write-Host ("Fertig: {0} kopiert ({1}), {2} bereits vorhanden, {3} Fehler." -f $done, (Fmt $doneBytes), $skip.Count, $failed) -ForegroundColor $color
Write-Host "Der Quellordner wurde nicht veraendert."
Write-Host "Protokoll: $logFile"
Log ("Fertig: {0} kopiert, {1} vorhanden, {2} Fehler" -f $done, $skip.Count, $failed)
