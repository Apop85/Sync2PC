# Erzeugt .claude/translations.md: alle App-Texte Deutsch | Englisch mit fortlaufender Nummer (seit 0.8.0).
# Quelle: app/src/main/res/values-de/strings_*.xml (Deutsch) und app/src/main/res/values/strings_*.xml (Englisch).
# Aufruf: powershell -ExecutionPolicy Bypass -File tools\make-translations.ps1
# Hinweis: Nur ASCII in dieser Datei (PowerShell 5.1 liest ohne BOM als ANSI).
param([string]$Root = (Split-Path -Parent $PSScriptRoot))
$ErrorActionPreference = 'Stop'
$res = Join-Path $Root 'app\src\main\res'
$out = Join-Path $Root '.claude\translations.md'

# Reihenfolge der Abschnitte (Datei -> Ueberschrift)
$sections = [ordered]@{
    'strings_common.xml'      = 'Allgemein'
    'strings_home.xml'        = 'Startseite, Schutzbremse, Berechtigungen'
    'strings_settings.xml'    = 'Einstellungen'
    'strings_profile.xml'     = 'Profil und Assistent'
    'strings_data.xml'        = 'Profil-Optionen, doppelte Profile, Typwahl'
    'strings_free.xml'        = 'Speicher freigeben'
    'strings_trash.xml'       = 'Papierkorb'
    'strings_mirror.xml'      = 'Ordner-Sync: Seiten'
    'strings_log_backup.xml'  = 'Verlauf und Sicherung'
    'strings_screens.xml'     = 'Ordnerauswahl, Konflikte, Simulation, Statistik, Updates'
    'strings_notif.xml'       = 'Benachrichtigungen'
    'strings_engine.xml'      = 'Sync-Ablauf, Verlauf-Texte, Fehlermeldungen'
    'strings_vendor.xml'      = 'Hersteller-Hinweise'
}

function Read-Res([string]$path) {
    $map = [ordered]@{}
    if (-not (Test-Path $path)) { return $map }
    [xml]$x = [IO.File]::ReadAllText($path, [Text.Encoding]::UTF8)
    foreach ($n in $x.resources.ChildNodes) {
        if ($n.NodeType -ne 'Element') { continue }
        if ($n.LocalName -eq 'string') { $map[$n.GetAttribute('name')] = $n.InnerText }
        elseif ($n.LocalName -eq 'plurals') {
            foreach ($i in $n.item) { $map[$n.GetAttribute('name') + ' (' + $i.quantity + ')'] = $i.InnerText }
        }
    }
    return $map
}

function Cell([string]$t) {
    if ($null -eq $t) { return '' }
    $t = $t.Replace("\'", "'").Replace('\"', '"').Replace('\n', ' / ').Replace('\\', '\')
    return $t.Replace('|', '\|')
}

$sb = New-Object Text.StringBuilder
[void]$sb.AppendLine('# LeoSync - Uebersetzungen Deutsch / Englisch'.Replace('Ue', [string][char]0xDC))
[void]$sb.AppendLine('')
[void]$sb.AppendLine(('Erzeugt mit `tools/make-translations.ps1` aus `res/values-de` (Deutsch) und `res/values` (Englisch). ' +
    'Korrekturen bitte mit Zeilennummer melden. Platzhalter wie `%1$s` werden in der App ersetzt (Zahl, Name, Datum ...). ' +
    'Zeilen mit `(one)` / `(other)` sind Einzahl / Mehrzahl.'))
[void]$sb.AppendLine('')
$nr = 0
foreach ($file in $sections.Keys) {
    $de = Read-Res (Join-Path $res "values-de\$file")
    $en = Read-Res (Join-Path $res "values\$file")
    if ($de.Count -eq 0 -and $en.Count -eq 0) { continue }
    [void]$sb.AppendLine('## ' + $sections[$file])
    [void]$sb.AppendLine('')
    [void]$sb.AppendLine('| Nr. | Schl' + [char]0xFC + 'ssel | Deutsch | Englisch |')
    [void]$sb.AppendLine('|---:|---|---|---|')
    $keys = @($de.Keys) + @($en.Keys | Where-Object { -not $de.Contains($_) })
    foreach ($k in $keys) {
        $nr++
        [void]$sb.AppendLine("| $nr | ``$k`` | $(Cell $de[$k]) | $(Cell $en[$k]) |")
    }
    [void]$sb.AppendLine('')
}
[IO.File]::WriteAllText($out, $sb.ToString(), (New-Object Text.UTF8Encoding($false)))
Write-Host "OK: $out ($nr Zeilen)"
