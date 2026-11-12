# Auftrag-guard.ps1 - PreToolUse-Hook fuer .claude/settings.json (SYNC-20261112-259, Konzept SYNC-20261112-253 Abschnitt 11)
#
# Schuetzt coordination/tasks/**, inbox/**, TASK-INDEX.md, .history/**, .audit.log, .lock vor direktem Schreiben.
# - Edit, Write, MultiEdit, NotebookEdit auf diese Pfade: Exit 2 (blockiert, Meldung auf stderr).
#   Zusaetzlich sperrt permissions.deny in .claude/settings.json dieselben Pfade (Stufe 1).
# - PowerShell, Bash: Befehl nennt einen geschuetzten Pfad UND enthaelt ein Schreibverb -> Rueckfrage an den
#   Benutzer (permissionDecision ask), keine harte Sperre, weil die Erkennung heuristisch ist (Stufe 2).
#   Aufrufe von tools\Auftrag.ps1 und tools\Auftrag-Selbsttest.ps1 sind frei.
# - Lesen bleibt frei. Ein Fehler im Waechter blockiert nie (Exit 0).
# Grenze: haelt Versehen auf, keine absichtliche Umgehung (z. B. Schreiben aus einem anderen Skript heraus).
# Reines ASCII (PowerShell 5.1).

$ErrorActionPreference = 'Stop'
try {
    $raw = [Console]::In.ReadToEnd()
    if (-not $raw -or -not $raw.Trim()) { exit 0 }
    $j = $raw | ConvertFrom-Json
}
catch { exit 0 }

$Meldung = 'Gesperrt: coordination/ (Auftraege, Index, Posteingaenge, .history, .audit.log, .lock) nur ueber tools\Auftrag.ps1 aendern (CLAUDE.md Abschnitt 10, coordination/README.md Abschnitt 13). Lesen: Auftrag.ps1 liste.'

try {
    $tool = [string]$j.tool_name
    $ti = $j.tool_input
    $root = Split-Path -Parent $PSScriptRoot
    $base = [IO.Path]::GetFullPath((Join-Path $root 'coordination')).TrimEnd('\')

    if (@('Edit', 'Write', 'MultiEdit', 'NotebookEdit') -contains $tool) {
        $p = [string]$ti.file_path
        if (-not $p) { $p = [string]$ti.notebook_path }
        if (-not $p) { exit 0 }
        $p = $p.Replace('/', '\')
        if ($p -match '^\\[A-Za-z]\\') { $p = $p.Substring(1, 1) + ':' + $p.Substring(2) }   # /c/... -> c:\...
        if (-not [IO.Path]::IsPathRooted($p)) {
            $cwd = [string]$j.cwd; if (-not $cwd) { $cwd = $root }
            $p = Join-Path $cwd $p
        }
        $full = [IO.Path]::GetFullPath($p)
        if ($full.StartsWith($base + '\', [StringComparison]::OrdinalIgnoreCase)) {
            $rel = $full.Substring($base.Length + 1)
            if ($rel -match '^(?i)(tasks|inbox|\.history)(\\|$)' -or $rel -match '^(?i)(TASK-INDEX\.md|\.audit\.log|\.lock)$') {
                [Console]::Error.WriteLine($Meldung)
                exit 2
            }
        }
        exit 0
    }

    if (@('PowerShell', 'Bash') -contains $tool) {
        $cmd = [string]$ti.command
        if (-not $cmd) { exit 0 }
        if ($cmd -match '(?i)Auftrag(-Selbsttest)?\.ps1') { exit 0 }
        $schutz = '(?i)(coordination[\\/]+(tasks|inbox|\.history)([\\/"''\s]|$)|TASK-INDEX\.md|\.audit\.log|coordination[\\/]+\.lock)'
        if ($cmd -notmatch $schutz) { exit 0 }
        $verben = @(
            '(?i)\b(Set-Content|Add-Content|Out-File|Clear-Content|New-Item|Remove-Item|Move-Item|Copy-Item|Rename-Item|Set-Item|Set-ItemProperty|Clear-Item|Tee-Object|Expand-Archive)\b',
            '(?i)(^|[\s;|&(])(sc|ac|ni|ri|rm|rmdir|del|erase|rd|mi|mv|move|cpi|cp|copy|ren|rni|attrib|icacls|tee|touch|truncate|xcopy|robocopy)(\s|$)',
            '(?i)\[(System\.)?IO\.(File|Directory)\]::(Write\w*|Append\w*|Delete|Move|Copy|Replace|Create\w*|SetAttributes|OpenWrite)',
            '(?i)\.(WriteAllText|WriteAllBytes|WriteAllLines|AppendAllText|MoveTo|CopyTo|Delete)\(',
            '(?i)\bsed\s+-i',
            '(?<![-0-9=<])>{1,2}(?![&=])'
        )
        foreach ($v in $verben) {
            if ($cmd -match $v) {
                $o = [ordered]@{ hookSpecificOutput = [ordered]@{
                        hookEventName            = 'PreToolUse'
                        permissionDecision       = 'ask'
                        permissionDecisionReason = 'Befehl schreibt moeglicherweise direkt in coordination/. ' + $Meldung
                    }
                }
                Write-Output (ConvertTo-Json $o -Depth 4 -Compress)
                exit 0
            }
        }
        exit 0
    }
}
catch { exit 0 }
exit 0
