# Auftrag-guard-Selbsttest.ps1 - prueft tools\Auftrag-guard.ps1 mit simulierten Hook-Eingaben (SYNC-20261112-273)
# Aufruf: powershell -NoProfile -ExecutionPolicy Bypass -File tools\Auftrag-guard-Selbsttest.ps1
# Schreibt nichts; jeder Fall startet den Waechter mit einer JSON-Eingabe und vergleicht das Ergebnis:
#   frei = Exit 0 ohne Ausgabe, frage = Rueckfrage (ask), sperre = Exit 2. Reines ASCII (PowerShell 5.1).

$guard = Join-Path $PSScriptRoot 'Auftrag-guard.ps1'
$root = Split-Path -Parent $PSScriptRoot
$tmp = [IO.Path]::GetTempPath().TrimEnd('\')
$prof = $env:USERPROFILE
$mem = Join-Path $prof ('.claude\projects\' + ($root -replace '[:\\/]', '-') + '\memory')
$adb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
$fn = 'func' + 'tion'          # Schluesselwort zerlegt, damit der Waechter diese Datei nicht selbst sperrt
$ok = 0; $fehler = 0

$pruefe = {
    param([string]$name, [string]$tool, [string]$wert, [string]$erwartet, [string]$cwd)
    if (-not $cwd) { $cwd = $root }
    $ti = @{}
    if ($tool -eq 'PowerShell' -or $tool -eq 'Bash') { $ti.command = $wert } else { $ti.file_path = $wert; $ti.content = 'x' }
    $json = ConvertTo-Json @{ tool_name = $tool; tool_input = $ti; cwd = $cwd } -Depth 4 -Compress
    $aus = ($json | powershell -NoProfile -ExecutionPolicy Bypass -File $guard 2>&1 | Out-String)
    $code = $LASTEXITCODE
    $ist = 'frei'
    if ($code -eq 2) { $ist = 'sperre' }
    elseif ($aus -match '"permissionDecision":"ask"') { $ist = 'frage' }
    elseif ($code -ne 0 -or $aus.Trim()) { $ist = "unerwartet (Exit $code)" }
    if ($ist -eq $erwartet) { $script:ok++ ; Write-Output ("OK      " + $name) }
    else { $script:fehler++; Write-Output ("FEHLER  " + $name + ": erwartet " + $erwartet + ", ist " + $ist + " | " + $aus.Trim()) }
}

# Datei-Werkzeuge
& $pruefe 'Write im Projekt'                 'Write' (Join-Path $root 'docs\x.md') 'frei'
& $pruefe 'Write tief im Projekt'            'Write' (Join-Path $root 'app\src\main\java\com\sync2pc\app\ui\X.kt') 'frei'
& $pruefe 'Write Punkt-Ordner im Projekt'    'Write' (Join-Path $root '.claude\roles\ux\neu\a.md') 'frei'
& $pruefe 'Write coordination/archive'       'Write' (Join-Path $root 'coordination\archive\a\b.md') 'frei'
& $pruefe 'Write Nachbarordner Sync2PC-alt'  'Write' ($root + '-alt\a.md') 'frage'
& $pruefe 'Write in %TMP%'                 'Write' (Join-Path $tmp 'claude\scratch\a.md') 'frei'
& $pruefe 'Write Memory'                     'Write' (Join-Path $mem 'x.md') 'frei'
& $pruefe 'Write Website-tasks.md'           'Write' 'C:\Users\rbald\Documents\GitHub\apop85.github.io\.claude\tasks.md' 'frei'
& $pruefe 'Write Website sonst'              'Write' 'C:\Users\rbald\Documents\GitHub\apop85.github.io\index.html' 'frage'
& $pruefe 'Write Desktop'                    'Write' (Join-Path $prof 'Desktop\x.txt') 'frage'
& $pruefe 'Write D:\testsync'                'Write' 'D:\testsync\a.txt' 'frei'
& $pruefe 'Write D:\anderes'                 'Write' 'D:\anderes\a.txt' 'frage'
& $pruefe 'Write /c/-Pfad ausserhalb'        'Write' '/c/Users/rbald/Neu/a.txt' 'frage'
& $pruefe 'Write coordination/tasks'         'Write' (Join-Path $root 'coordination\tasks\OPEN\x.md') 'sperre'
$altTmp = $env:TMP; $altTemp = $env:TEMP
$env:TMP = Join-Path $altTmp 'claude\sitzung'; $env:TEMP = $env:TMP      # wie im Hook: TMP zeigt auf einen Unterordner
& $pruefe 'Temp fest, TMP=Unterordner'       'Write' 'C:\Users\rbald\AppData\Local\Temp\a\b.txt' 'frei'
& $pruefe 'Temp per Shell, TMP=Unterordner'  'PowerShell' 'New-Item -ItemType Directory C:\Users\rbald\AppData\Local\Temp\neu' 'frei'
& $pruefe 'LOCALAPPDATA\Temp-alt'            'Write' 'C:\Users\rbald\AppData\Local\Temp-alt\a.txt' 'frage'
$env:TMP = $altTmp; $env:TEMP = $altTemp
& $pruefe 'Write .ps1 in %TMP%' 'Write' (Join-Path $tmp 'a.ps1') 'frei'

# PowerShell
& $pruefe 'Lesen ausserhalb'                 'PowerShell' 'Get-Content -Encoding UTF8 C:\Windows\win.ini' 'frei'
& $pruefe 'New-Item im Projekt (relativ)'    'PowerShell' 'New-Item -ItemType Directory .claude\ux\neu' 'frei'
& $pruefe 'New-Item ausserhalb'              'PowerShell' 'New-Item -ItemType Directory C:\Users\rbald\Neu' 'frage'
& $pruefe 'Copy-Item tief im Projekt (abs.)' 'PowerShell' ('Copy-Item a.txt "' + $root + '\app\src\test\java\x\a.txt"') 'frei'
& $pruefe 'Remove-Item .gradle (relativ)'    'PowerShell' 'Remove-Item .gradle\x -Recurse' 'frei'
& $pruefe 'mkdir ausserhalb'               'PowerShell' 'mkdir C:\Neu' 'frage'
& $pruefe 'relativ, cwd ausserhalb'          'PowerShell' 'New-Item x.txt' 'frage' (Join-Path $prof 'Desktop')
& $pruefe 'adb-Screenshot ins Projekt'       'PowerShell' ('& "' + $adb + '" exec-out screencap -p > .claude\AppScreenshots\a.png') 'frei'
& $pruefe 'adb pull ins Projekt'             'PowerShell' ('& "' + $adb + '" pull /sdcard/Download/a.txt .claude\AppScreenshots\a.txt') 'frei'
& $pruefe 'adb pull auf Desktop'             'PowerShell' ('& "' + $adb + '" pull /sdcard/Download/a.txt C:\Users\rbald\Desktop\a.txt') 'frage'
& $pruefe 'Remove-Item im Android-SDK'       'PowerShell' 'Remove-Item "$env:LOCALAPPDATA\Android\Sdk\platform-tools\x" -Recurse' 'frage'
& $pruefe 'New-Item unter Programme'         'PowerShell' 'New-Item -ItemType Directory "C:\Program Files\Neu"' 'frage'
& $pruefe 'Copy-Item nach C:\Windows'        'PowerShell' 'Copy-Item a.txt C:\Windows\Temp\a.txt' 'frage'
& $pruefe 'Write im Android-SDK'             'Write' (Join-Path $env:LOCALAPPDATA 'Android\Sdk\a.txt') 'frage'
& $pruefe '$env:Path erweitern + Umleitung'  'PowerShell' ('$env:Path = "' + (Split-Path $adb) + ';" + $env:Path; adb exec-out screencap -p > .claude\AppScreenshots\a.png') 'frei'
& $pruefe 'adb shell input text UNC'         'PowerShell' ('$a = "' + $adb + '"; Remove-Item ''' + $root + '\.claude\AppScreenshots\_d.png'' -ErrorAction SilentlyContinue; & $a shell "i=0; while [ `$i -lt 3 ]; do input keyevent 67; done"; & $a shell ''input text "\\\\server\\Fotos"''') 'frei'
& $pruefe 'adb shell mit Geraete-Umleitung'  'PowerShell' ('& "' + $adb + '" shell "rm /sdcard/x; echo 1 > /sdcard/y"') 'frei'
& $pruefe 'adb shell, lokal auf Desktop'     'PowerShell' ('& "' + $adb + '" shell screencap -p > C:\Users\rbald\Desktop\a.png') 'frage'
& $pruefe 'JAVA_HOME + gradlew'              'PowerShell' '$env:JAVA_HOME = ''C:\Program Files\Android\Android Studio\jbr''; .\gradlew.bat assembleDebug *> $null' 'frei'
& $pruefe 'WriteAllText $env:TEMP'           'PowerShell' '[IO.File]::WriteAllText("$env:TEMP\x.txt", ''a'', [Text.UTF8Encoding]::new($false))' 'frei'
& $pruefe 'WriteAllText %USERPROFILE%'       'PowerShell' '[IO.File]::WriteAllText("$env:USERPROFILE\x.txt", ''a'')' 'frage'
& $pruefe 'New-Item ~\Desktop'               'PowerShell' 'New-Item ~\Desktop\x.txt' 'frage'
& $pruefe 'venv LeoSyncTestShare'            'PowerShell' 'New-Item -ItemType Directory "$env:LOCALAPPDATA\LeoSyncTestShare\venv"' 'frei'
& $pruefe 'LOCALAPPDATA sonst'               'PowerShell' 'New-Item -ItemType Directory "$env:LOCALAPPDATA\Anderes"' 'frage'
& $pruefe 'Copy-Item nach X:\testsync'       'PowerShell' 'Copy-Item a.txt X:\testsync\a.txt' 'frei'
& $pruefe 'Copy-Item nach X:\anderes'        'PowerShell' 'Copy-Item a.txt X:\anderes\a.txt' 'frage'
& $pruefe 'Remove-Item mit Platzhalter'      'PowerShell' 'Remove-Item D:\testsync\*.tmp' 'frei'
& $pruefe 'UNC-Ziel'                         'PowerShell' 'Copy-Item a.txt \\server\share\a.txt' 'frage'
& $pruefe 'Download ins Projekt'             'PowerShell' 'Invoke-WebRequest https://example.org/a -OutFile .build\a' 'frei'
& $pruefe 'Auftrag.ps1 mit -Eingabe'         'PowerShell' ('powershell -NoProfile -File tools\Auftrag.ps1 erstellen -Rolle QA -Eingabe ' + $tmp + '\a.md') 'frei'
& $pruefe 'coordination direkt'              'PowerShell' 'Remove-Item coordination\tasks\OPEN\x.md' 'frage'
& $pruefe 'Alias-Funktion'                   'PowerShell' ($fn + ' gc { 1 }') 'sperre'
& $pruefe 'Verb-Nomen-Funktion'              'PowerShell' ($fn + ' Read-FileText { 1 }') 'frei'
# Fehlalarme (SYNC-20261112-269): Alias-Name nur als Text, nicht als Definition
& $pruefe 'Auftrag.ps1, Text einfach quotiert' 'PowerShell' ('powershell -NoProfile -File tools\Auftrag.ps1 erstellen -Rolle QA -Aufgabe ''Beispiel: ' + $fn + ' rd { }'' -An Development') 'frei'
& $pruefe 'Auftrag.ps1, Text doppelt quotiert' 'PowerShell' ('powershell -NoProfile -File tools\Auftrag.ps1 aendern -Rolle QA -Id 1 -Feld Aufgabe -Wert "Bericht: ' + $fn + ' rd { }"') 'frei'
& $pruefe 'Auftrag.ps1, Here-String'         'PowerShell' ('powershell -NoProfile -File tools\Auftrag.ps1 erstellen -Rolle QA -Aufgabe @''' + "`n" + $fn + ' rd { }' + "`n" + '''@') 'frei'
& $pruefe 'Suchmuster in Lesebefehl'         'PowerShell' ('Select-String -Path a.txt -Pattern ''' + $fn + ' rd''') 'frei'
# Echte Faelle bleiben gesperrt
& $pruefe 'Alias-Funktion mit Aufruf'        'PowerShell' ($fn + ' rd { Remove-Item $args }; rd x') 'sperre'
& $pruefe 'Auftrag.ps1 plus echte Funktion'  'PowerShell' ('powershell -File tools\Auftrag.ps1 liste -Rolle QA; ' + $fn + ' rd { 1 }') 'sperre'
& $pruefe 'Funktion in -Command-Text'        'PowerShell' ('powershell -Command "' + $fn + ' rd { 1 }"') 'sperre'
& $pruefe 'Funktion in iex-Text'             'PowerShell' ('iex ''' + $fn + ' rd { 1 }''') 'sperre'
& $pruefe 'Schreiben von Alias-Funktion'     'PowerShell' ('Set-Content a.txt ''' + $fn + ' rd { 1 }''') 'sperre'

Write-Output ''
Write-Output ("Ergebnis: " + $ok + " OK, " + $fehler + " Fehler")
if ($fehler -gt 0) { exit 1 }
exit 0
