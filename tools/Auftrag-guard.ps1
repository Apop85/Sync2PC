# Auftrag-guard.ps1 - PreToolUse-Hook fuer .claude/settings.json (SYNC-20261112-259, -263, -273; Konzept SYNC-20261112-253 Abschnitt 11)
#
# 1. PowerShell-Alias-Falle (CLAUDE.md 1, oberste Regel): Definiert ein PowerShell-Befehl oder eine geschriebene
#    .ps1/.psm1-Datei eine Funktion (function/filter), deren Name ein Alias oder Cmdlet ist, -> Exit 2 (blockiert).
#    Grund: Aliase haben Vorrang vor Funktionen; eine Funktion namens rd und danach "rd $p" ruft Remove-Item auf (Datenverlust).
# 2. coordination/ (tasks/**, inbox/**, TASK-INDEX.md, .history/**, .audit.log, .lock):
#    - Edit, Write, MultiEdit, NotebookEdit auf diese Pfade: Exit 2 (zusaetzlich zu permissions.deny, Stufe 1).
#    - PowerShell, Bash mit Schreibverb auf diese Pfade (ausser Aufrufen von tools\Auftrag.ps1 / Auftrag-Selbsttest.ps1):
#      Rueckfrage an den Benutzer (permissionDecision ask), weil die Erkennung heuristisch ist (Stufe 2).
# 3. Schreiborte (CLAUDE.md 1): Schreiben nur in den freigegebenen Orten ($Orte unten). Ausserhalb -> Rueckfrage (ask).
#    - Edit, Write, MultiEdit, NotebookEdit: Zielpfad.
#    - PowerShell, Bash mit Schreibverb: alle absoluten Pfade im Befehl (Umgebungsvariablen aufgeloest) und das
#      Arbeitsverzeichnis. Nicht als Ziel zaehlen nur Programme (.exe/.bat/.cmd/.com, z. B. adb.exe) und Werte von
#      Zuweisungen an Umgebungsvariablen ($env:JAVA_HOME = '...'). Schreiben im Android-SDK, unter Programme oder
#      Windows fragt also nach. Heuristisch: ein gelesener Pfad ausserhalb kann ebenfalls fragen.
#    Rollengrenzen innerhalb der Orte prueft der Waechter nicht (sie gelten trotzdem, CLAUDE.md 1).
# Lesen bleibt frei. Ein Fehler im Waechter blockiert nie (Exit 0).
# Grenze: haelt Versehen auf, keine absichtliche Umgehung. Reines ASCII (PowerShell 5.1).
# Selbsttest: tools\Auftrag-guard-Selbsttest.ps1

$ErrorActionPreference = 'Stop'
try {
    $raw = [Console]::In.ReadToEnd()
    if (-not $raw -or -not $raw.Trim()) { exit 0 }
    $j = $raw | ConvertFrom-Json
}
catch { exit 0 }

$Meldung = 'Gesperrt: coordination/ (Auftraege, Index, Posteingaenge, .history, .audit.log, .lock) nur ueber tools\Auftrag.ps1 aendern (CLAUDE.md Abschnitt 10, coordination/README.md Abschnitt 13). Lesen: Auftrag.ps1 liste.'

# Funktionsnamen, die PowerShell schon als Alias oder Cmdlet kennt (Get-Command statt eigener Liste, damit nichts fehlt)
function Get-AliasKonflikt([string]$code) {
    $res = New-Object System.Collections.Generic.List[string]
    if (-not $code -or $code -notmatch '(?i)\b(function|filter)\s') { return , $res }
    foreach ($m in [regex]::Matches($code, '(?i)(?<![\w$.-])(?:function|filter)\s+(?:global:|script:|local:|private:)?([A-Za-z][\w-]*)')) {
        $n = $m.Groups[1].Value
        $a = Get-Alias -Name $n -ErrorAction SilentlyContinue
        if ($a) { [void]$res.Add("$n (Alias fuer $($a.Definition))"); continue }
        if (Get-Command -Name $n -CommandType Cmdlet -ErrorAction SilentlyContinue) { [void]$res.Add("$n (Cmdlet)") }
    }
    return , $res
}
# Text in Anfuehrungszeichen (auch Here-Strings) entfernen: Ein Alias-Name als Suchmuster oder als Text eines Auftrags ist keine Definition.
# Nur sinnvoll, wenn der Text nicht ausgefuehrt wird (siehe Test-AusfuehrtText).
function Remove-AnfuehrungsText([string]$code) {
    return [regex]::Replace($code, '(?s)@''.*?''@|@".*?"@|''[^'']*''|"(?:[^"`]|`.)*"', "''")
}
# Wird Text als Code ausgefuehrt (-Command, iex, Skriptblock aus Text, -EncodedCommand)? Dann bleibt der Text Teil der Pruefung.
function Test-AusfuehrtText([string]$cmd) {
    return [bool]($cmd -match '(?i)(\s-c(ommand)?\s|\s-enc(odedcommand)?\s|\biex\b|Invoke-Expression|\[scriptblock\]|::Create\(|Add-Type\b)')
}
function Stop-AliasKonflikt($treffer) {
    [Console]::Error.WriteLine('Abgelehnt (PowerShell-Alias-Falle, CLAUDE.md 1): Funktionsname belegt: ' + (($treffer | Select-Object -Unique) -join ', ') +
        '. Aliase haben Vorrang vor Funktionen, ein Aufruf fuehrt das Cmdlet aus (z. B. rd = Remove-Item, Datenverlust). ' +
        'Eindeutigen Verb-Nomen-Namen waehlen (z. B. Read-FileText, Set-LsText) oder einen Skriptblock in einer Variablen nutzen ($teil = { ... }; & $teil).')
    exit 2
}
function Send-GuardFrage([string]$grund) {
    $o = [ordered]@{ hookSpecificOutput = [ordered]@{
            hookEventName            = 'PreToolUse'
            permissionDecision       = 'ask'
            permissionDecisionReason = $grund
        }
    }
    Write-Output (ConvertTo-Json $o -Depth 4 -Compress)
    exit 0
}
# Pfad vereinheitlichen: Anfuehrungszeichen weg, / -> \, /c/... -> c:\..., relativ -> absolut, Platzhalter abschneiden
function Resolve-GuardPfad([string]$p, [string]$cwd) {
    $p = $p.Trim().Trim('"', "'").Replace('/', '\')
    if ($p -match '^\\[A-Za-z]\\') { $p = $p.Substring(1, 1) + ':' + $p.Substring(2) }
    $p = ($p -split '[*?]')[0]
    if (-not $p) { return $null }
    try {
        if (-not [IO.Path]::IsPathRooted($p)) { $p = Join-Path $cwd $p }
        return [IO.Path]::GetFullPath($p).TrimEnd('\')
    }
    catch { return $null }
}
function Test-GuardOrt([string]$voll, $liste) {
    foreach ($o in $liste) {
        if ($voll.Equals($o, [StringComparison]::OrdinalIgnoreCase) -or $voll.StartsWith($o + '\', [StringComparison]::OrdinalIgnoreCase)) { return $true }
    }
    return $false
}

try {
    $tool = [string]$j.tool_name
    $ti = $j.tool_input
    $root = Split-Path -Parent $PSScriptRoot
    $base = [IO.Path]::GetFullPath((Join-Path $root 'coordination')).TrimEnd('\')
    $cwd = [string]$j.cwd; if (-not $cwd) { $cwd = $root }

    # Freigegebene Schreiborte (CLAUDE.md 1, Tabelle Schreibrechte; Aenderung nur mit dem Regelwerk)
    $memName = $root -replace '[:\\/]', '-'      # C:\Users\x\Sync2PC -> C--Users-x-Sync2PC (Ordnername von Claude Code)
    $Orte = New-Object System.Collections.Generic.List[string]
    foreach ($o in @(
            $root,                                                                  # Projektordner (Rollengrenzen: CLAUDE.md 1)
            'D:\testsync', 'X:\testsync',                                           # Testordner UX/QA
            (Join-Path $env:LOCALAPPDATA 'LeoSyncTestShare'),                       # venv SMB-Testserver (QA)
            'C:\Users\rbald\AppData\Local\Temp', (Join-Path $env:LOCALAPPDATA 'Temp'), # Temp fest (TMP kann im Hook auf einen Unterordner zeigen)
            [IO.Path]::GetTempPath(), $env:TMP, $env:TEMP,                          # %TMP% inkl. Scratchpad
            'C:\Users\rbald\Documents\GitHub\apop85.github.io\.claude\tasks.md',    # Website-Auftraege (Documentation)
            (Join-Path $env:USERPROFILE ('.claude\projects\' + $memName + '\memory')), # Memory von Claude Code
            (Join-Path $env:USERPROFILE '.claude\plans'))) {                        # Plaene von Claude Code
        if ($o) { try { [void]$Orte.Add([IO.Path]::GetFullPath($o).TrimEnd('\')) } catch {} }
    }
    $OrtHinweis = 'Freigegeben sind: Projektordner, D:\testsync, X:\testsync, %LOCALAPPDATA%\LeoSyncTestShare, %TMP%, Website-tasks.md, Memory/Plaene von Claude Code (CLAUDE.md 1). Nur zulassen, wenn der Auftrag diesen Ort wirklich braucht.'

    if (@('Edit', 'Write', 'MultiEdit', 'NotebookEdit') -contains $tool) {
        $p = [string]$ti.file_path
        if (-not $p) { $p = [string]$ti.notebook_path }
        if (-not $p) { exit 0 }
        # 1. Alias-Falle in PowerShell-Dateien (ueberall, auch Scratchpad)
        if ($p -match '(?i)\.psm?1$') {
            $code = [string]$ti.content
            if ($tool -eq 'Edit') { $code = [string]$ti.new_string }
            if ($tool -eq 'MultiEdit' -and $ti.edits) { $code = (@($ti.edits | ForEach-Object { [string]$_.new_string }) -join "`n") }
            $k = Get-AliasKonflikt $code
            if ($k.Count -gt 0) { Stop-AliasKonflikt $k }
        }
        $full = Resolve-GuardPfad $p $cwd
        if (-not $full) { exit 0 }
        # 2. coordination/
        if ($full.StartsWith($base + '\', [StringComparison]::OrdinalIgnoreCase)) {
            $rel = $full.Substring($base.Length + 1)
            if ($rel -match '^(?i)(tasks|inbox|\.history)(\\|$)' -or $rel -match '^(?i)(TASK-INDEX\.md|\.audit\.log|\.lock)$') {
                [Console]::Error.WriteLine($Meldung)
                exit 2
            }
        }
        # 3. Schreiborte
        if (-not (Test-GuardOrt $full $Orte)) {
            Send-GuardFrage ('Schreibzugriff ausserhalb der freigegebenen Orte: ' + $full + '. ' + $OrtHinweis)
        }
        exit 0
    }

    if (@('PowerShell', 'Bash') -contains $tool) {
        $cmd = [string]$ti.command
        if (-not $cmd) { exit 0 }
        # 1. Alias-Falle (nur PowerShell)
        $verben = @(
            '(?i)\b(Set-Content|Add-Content|Out-File|Clear-Content|New-Item|Remove-Item|Move-Item|Copy-Item|Rename-Item|Set-Item|Set-ItemProperty|Clear-Item|Tee-Object|Expand-Archive|Compress-Archive|Export-Csv|Export-Clixml)\b',
            '(?i)(^|[\s;|&(])(sc|ac|ni|ri|rm|rmdir|del|erase|rd|mi|mv|move|cpi|cp|copy|ren|rni|md|mkdir|attrib|icacls|tee|touch|truncate|xcopy|robocopy)(\s|$)',
            '(?i)\[(System\.)?IO\.(File|Directory)\]::(Write\w*|Append\w*|Delete|Move|Copy|Replace|Create\w*|SetAttributes|OpenWrite)',
            '(?i)\.(WriteAllText|WriteAllBytes|WriteAllLines|AppendAllText|MoveTo|CopyTo|Delete)\(',
            '(?i)(\s-OutFile\b|\bsed\s+-i|\bpull\s)',
            '(?<![-0-9=<])>{1,2}(?![&=])'
        )
        $schreibt = $false
        foreach ($v in $verben) { if ($cmd -match $v) { $schreibt = $true; break } }

        # 1. Alias-Falle (nur PowerShell). Fehlalarme (SYNC-20261112-269): Aufrufe von tools\Auftrag.ps1 (Text in -Aufgabe/-Kontext/-Wert)
        #    und reine Lesebefehle (Suchmuster) pruefen nur den Code ausserhalb von Anfuehrungszeichen. Wird der Text ausgefuehrt
        #    (-Command, iex ...) oder schreibt der Befehl, gilt weiter der ganze Befehlstext.
        if ($tool -eq 'PowerShell') {
            $pruef = $cmd
            if (($cmd -match '(?i)Auftrag(-Selbsttest|-guard-Selbsttest)?\.ps1' -or -not $schreibt) -and -not (Test-AusfuehrtText $cmd)) {
                $pruef = Remove-AnfuehrungsText $cmd
            }
            $k = Get-AliasKonflikt $pruef
            if ($k.Count -gt 0) { Stop-AliasKonflikt $k }
        }
        if (-not $schreibt) { exit 0 }

        # 2. coordination/
        $schutz = '(?i)(coordination[\\/]+(tasks|inbox|\.history)([\\/"''\s]|$)|TASK-INDEX\.md|\.audit\.log|coordination[\\/]+\.lock)'
        if ($cmd -notmatch '(?i)Auftrag(-Selbsttest)?\.ps1' -and $cmd -match $schutz) {
            Send-GuardFrage ('Befehl schreibt moeglicherweise direkt in coordination/. ' + $Meldung)
        }

        # 3. Schreiborte: Zuweisungen an Umgebungsvariablen ($env:JAVA_HOME = '...', $env:Path += ';...') sind kein
        #    Schreibziel und fallen weg; danach Umgebungsvariablen aufloesen und absolute Pfade sammeln
        $t = [regex]::Replace($cmd, '(?i)\$\{?env:[\w()]+\}?\s*[+]?=(?:"[^"]*"|''[^'']*''|[^;\r\n"''])*', ' ')
        #    Argumente von "adb ... shell" laufen auf dem Geraet (z. B. input text "\\server\share" tippt nur Text);
        #    sie fallen bis zum Anweisungsende weg, eine lokale Umleitung (> datei) oder Pipe bleibt erhalten
        $t = [regex]::Replace($t, '(?i)(?<![\w-])shell\s+(?:"[^"]*"|''[^'']*''|[^;\r\n"''>|])*', ' ')
        $t = [regex]::Replace($t,'(?i)\$\{?env:([A-Za-z_][\w()]*)\}?', [Text.RegularExpressions.MatchEvaluator] {
                param($m) $w = [Environment]::GetEnvironmentVariable($m.Groups[1].Value); if ($w) { $w } else { $m.Value } })
        $t = [regex]::Replace($t, '%([A-Za-z_][\w()]*)%', [Text.RegularExpressions.MatchEvaluator] {
                param($m) $w = [Environment]::GetEnvironmentVariable($m.Groups[1].Value); if ($w) { $w } else { $m.Value } })
        $t = [regex]::Replace($t, '(?i)(?<![\w$])(\$HOME\b|~(?=[\\/]))', $env:USERPROFILE.Replace('$', '$$'))
        $start = '(?:[A-Za-z]:[\\/]|\\\\[\w.$-]+\\)'
        $kand = New-Object System.Collections.Generic.List[string]
        $qPat = '(["''])(' + $start + '[^"''\r\n]*)\1'
        foreach ($m in [regex]::Matches($t, $qPat)) { [void]$kand.Add($m.Groups[2].Value) }
        $rest = [regex]::Replace($t, $qPat, ' ')
        foreach ($m in [regex]::Matches($rest, '(?<![\w"''$:])(' + $start + '[^\s"'';|<>(){},`]*)')) { [void]$kand.Add($m.Groups[1].Value) }

        $draussen = New-Object System.Collections.Generic.List[string]
        foreach ($c in $kand) {
            if ($c -match '(?i)\.(exe|bat|cmd|com)$') { continue }
            $full = Resolve-GuardPfad $c $cwd
            if (-not $full) { continue }
            if (-not (Test-GuardOrt $full $Orte) -and -not $draussen.Contains($full)) { [void]$draussen.Add($full) }
        }
        $cwdVoll = Resolve-GuardPfad $cwd $root
        if ($cwdVoll -and -not (Test-GuardOrt $cwdVoll $Orte)) { [void]$draussen.Add('Arbeitsverzeichnis ' + $cwdVoll) }
        if ($draussen.Count -gt 0) {
            Send-GuardFrage ('Befehl schreibt moeglicherweise ausserhalb der freigegebenen Orte: ' + ($draussen -join ', ') + '. ' + $OrtHinweis)
        }
        exit 0
    }
}
catch { exit 0 }
exit 0
