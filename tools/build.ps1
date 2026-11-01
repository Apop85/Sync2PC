# ============================================================================
#  LeoSync One-Click-Builder
#  - baut nur, wenn sich der Quellcode seit dem letzten Build geaendert hat
#  - erhoeht die Patch-Version automatisch, falls die aktuelle Version schon gebaut wurde
#  - legt die signierte APK unter releases\LeoSync-<version>.apk ab
#
#  Aufruf:  build.bat                (normal)
#           build.bat -Force         (immer bauen)
#           build.bat -SkipTests     (Unit-Tests ueberspringen)
#           build.bat -Debug         (Debug-APK statt Release)
# ============================================================================
param(
    [switch]$Force,
    [switch]$SkipTests,
    [switch]$Debug
)
$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root
$Utf8 = New-Object System.Text.UTF8Encoding $false
$StateDir = Join-Path $Root ".build"
New-Item -ItemType Directory -Force $StateDir | Out-Null
New-Item -ItemType Directory -Force (Join-Path $Root "releases") | Out-Null

function Step($t) { Write-Host ""; Write-Host "==> $t" -ForegroundColor Cyan }
function Fail($t) { Write-Host ""; Write-Host "FEHLER: $t" -ForegroundColor Red; exit 1 }

# ---------------------------------------------------------------------------
Step "Pruefe Java (JDK 17 bis 25)"
# Seit 0.1.3: Gradle 9.8 / AGP 9.5 laufen auch mit Java 25 (Android Studio JBR).
# Neuere Java-Versionen -> portables JDK 21 verwenden.
$MinJava = 17
$MaxJava = 25
function Get-JavaMajor($javaExe) {
    if (-not (Test-Path $javaExe)) { return 0 }
    $out = cmd /c "`"$javaExe`" -version 2>&1"
    $m = [regex]::Match(($out -join " "), 'version "(\d+)(\.(\d+))?')
    if (-not $m.Success) { return 0 }
    $major = [int]$m.Groups[1].Value
    if ($major -eq 1) { $major = [int]$m.Groups[3].Value }
    return $major
}
$candidates = @()
if ($env:JAVA_HOME) { $candidates += $env:JAVA_HOME }
$candidates += "$env:ProgramFiles\Android\Android Studio\jbr"
$candidates += "$env:LOCALAPPDATA\Programs\Android Studio\jbr"
$candidates += (Get-ChildItem "$env:ProgramFiles\Java", "$env:ProgramFiles\Eclipse Adoptium", "$env:ProgramFiles\Microsoft" -Directory -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -match 'jdk' } | Sort-Object Name -Descending | ForEach-Object { $_.FullName })
$localJdkRoot = Join-Path $StateDir "jdk21"
$candidates += (Get-ChildItem $localJdkRoot -Directory -ErrorAction SilentlyContinue | ForEach-Object { $_.FullName })
$javaHome = $null
foreach ($c in $candidates) {
    if (-not $c) { continue }
    $major = Get-JavaMajor (Join-Path $c "bin\java.exe")
    if ($major -ge $MinJava -and $major -le $MaxJava) { $javaHome = $c; break }
    if ($major -gt 0) { Write-Host "Uebersprungen (Java $major): $c" }
}
if (-not $javaHome) {
    Write-Host "Kein passendes JDK gefunden -> lade portables JDK 21 (Eclipse Temurin, ca. 190 MB) nach .build\jdk21"
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    $zip = Join-Path $StateDir "jdk21.zip"
    $u = "https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse?project=jdk"
    $oldPref = $ProgressPreference; $ProgressPreference = 'SilentlyContinue'  # sonst sehr langsam in PS 5.1
    try { Invoke-WebRequest -Uri $u -OutFile $zip -UseBasicParsing } finally { $ProgressPreference = $oldPref }
    if (Test-Path $localJdkRoot) { Remove-Item -Recurse -Force $localJdkRoot }
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    [System.IO.Compression.ZipFile]::ExtractToDirectory($zip, $localJdkRoot)
    Remove-Item -Force $zip
    $javaHome = Get-ChildItem $localJdkRoot -Directory | Select-Object -First 1 | ForEach-Object { $_.FullName }
    if (-not $javaHome -or (Get-JavaMajor (Join-Path $javaHome "bin\java.exe")) -lt $MinJava) {
        Fail "Portables JDK konnte nicht eingerichtet werden. Bitte JDK 21 manuell installieren und JAVA_HOME setzen. Details: build.md"
    }
}
$env:JAVA_HOME = $javaHome
Write-Host "JAVA_HOME = $javaHome"

# ---------------------------------------------------------------------------
Step "Pruefe Android SDK"
$sdk = $null
foreach ($c in @($env:ANDROID_HOME, $env:ANDROID_SDK_ROOT, "$env:LOCALAPPDATA\Android\Sdk")) {
    if ($c -and (Test-Path (Join-Path $c "platforms"))) { $sdk = $c; break }
}
if (-not $sdk) { Fail "Android SDK nicht gefunden. Android Studio installieren und einmal starten (SDK wird eingerichtet). Details: build.md" }
$localProps = Join-Path $Root "local.properties"
$sdkEscaped = $sdk.Replace('\', '\\').Replace(':', '\:')
[IO.File]::WriteAllText($localProps, "sdk.dir=$sdkEscaped`n", $Utf8)
Write-Host "SDK = $sdk"

# ---------------------------------------------------------------------------
Step "Pruefe Gradle Wrapper"
$wrapperJar = Join-Path $Root "gradle\wrapper\gradle-wrapper.jar"
if (-not (Test-Path $wrapperJar)) {
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    $u = "https://raw.githubusercontent.com/gradle/gradle/v9.8.2/gradle/wrapper/gradle-wrapper.jar"
    Write-Host "Lade gradle-wrapper.jar ($u)"
    Invoke-WebRequest -Uri $u -OutFile $wrapperJar -UseBasicParsing
    Write-Host ("SHA-256: " + (Get-FileHash $wrapperJar -Algorithm SHA256).Hash)
} else { Write-Host "vorhanden" }

# ---------------------------------------------------------------------------
Step "Pruefe Laenderdaten und Icons"
& powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "fetch-geodata.ps1") -Root $Root
if ($LASTEXITCODE -ne 0) { Write-Warning "Ohne Laenderdaten werden keine Laenderordner angelegt." }

$iconSrc = Join-Path $Root ".claude\icon.png"
$iconHashFile = Join-Path $StateDir "icon.hash"
if (Test-Path $iconSrc) {
    $ih = (Get-FileHash $iconSrc -Algorithm SHA256).Hash
    $old = if (Test-Path $iconHashFile) { (Get-Content $iconHashFile -Raw).Trim() } else { "" }
    $fg = Join-Path $Root "app\src\main\res\mipmap-xxxhdpi\ic_launcher_foreground.png"
    if ($ih -ne $old -or -not (Test-Path $fg)) {
        & powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "make-icons.ps1") -Root $Root
        [IO.File]::WriteAllText($iconHashFile, $ih, $Utf8)
    } else { Write-Host "Icons aktuell" }
}

# Animation: bevorzugt aus dem Video (.claude\new_animation.mp4, braucht ffmpeg),
# sonst aus der Bildvorlage (.claude\gif_frames.png). Nur neu erzeugen, wenn sich die Quelle geaendert hat.
$animVideo = Join-Path $Root ".claude\new_animation.mp4"
$animSheet = Join-Path $Root ".claude\gif_frames.png"
$animHashFile = Join-Path $StateDir "anim.hash"
$animSrc = if (Test-Path $animVideo) { $animVideo } elseif (Test-Path $animSheet) { $animSheet } else { $null }
if ($animSrc) {
    # Kennung der Erzeugungs-Einstellungen anhaengen: Aendert sie sich (z. B. Bildgroesse), wird neu erzeugt
    $ah = (Get-FileHash $animSrc -Algorithm SHA256).Hash + "|v2-320px"
    $oldA = if (Test-Path $animHashFile) { (Get-Content $animHashFile -Raw).Trim() } else { "" }
    $firstFrame = Join-Path $Root "app\src\main\res\drawable-nodpi\anim_dog_01.png"
    if ($ah -ne $oldA -or -not (Test-Path $firstFrame)) {
        if ($animSrc -eq $animVideo) {
            if (Get-Command ffmpeg -ErrorAction SilentlyContinue) {
                & powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "make-anim-video.ps1") -Root $Root
                if ($LASTEXITCODE -eq 0) { [IO.File]::WriteAllText($animHashFile, $ah, $Utf8) }
            } else { Write-Warning "ffmpeg nicht gefunden - Animation bleibt unveraendert" }
        } else {
            & powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "make-anim.ps1") -Root $Root
            [IO.File]::WriteAllText($animHashFile, $ah, $Utf8)
        }
    } else { Write-Host "Animation aktuell" }
}

# Zustandsbilder (wartet / alles aktuell / pausiert)
$stateSrcs = @((Join-Path $Root ".claude\wait_cleaned.png"), (Join-Path $Root ".claude\idle_cleaned.png"), (Join-Path $Root ".claude\pause.png")) | Where-Object { Test-Path $_ }
if ($stateSrcs.Count -gt 0) {
    $sh = ($stateSrcs | ForEach-Object { (Get-FileHash $_ -Algorithm SHA256).Hash }) -join ""
    $stateHashFile = Join-Path $StateDir "state-images.hash"
    $oldS = if (Test-Path $stateHashFile) { (Get-Content $stateHashFile -Raw).Trim() } else { "" }
    $waitOut = Join-Path $Root "app\src\main\res\drawable-nodpi\dog_wait.png"
    $pauseOut = Join-Path $Root "app\src\main\res\drawable-nodpi\dog_pause.png"
    if ($sh -ne $oldS -or -not (Test-Path $waitOut) -or -not (Test-Path $pauseOut)) {
        & powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "make-state-images.ps1") -Root $Root
        [IO.File]::WriteAllText($stateHashFile, $sh, $Utf8)
    } else { Write-Host "Zustandsbilder aktuell" }
}

# ---------------------------------------------------------------------------
Step "Pruefe auf Quellcode-Aenderungen"
$inputs = @("app\src", "app\build.gradle.kts", "app\proguard-rules.pro", "build.gradle.kts",
            "settings.gradle.kts", "gradle.properties", "gradle\wrapper\gradle-wrapper.properties")
$files = foreach ($i in $inputs) {
    $p = Join-Path $Root $i
    if (Test-Path $p -PathType Container) { Get-ChildItem $p -Recurse -File } elseif (Test-Path $p) { Get-Item $p }
}
$sb = New-Object System.Text.StringBuilder
foreach ($f in ($files | Sort-Object FullName)) {
    $rel = $f.FullName.Substring($Root.Length + 1)
    [void]$sb.Append($rel).Append('|').Append((Get-FileHash $f.FullName -Algorithm SHA256).Hash).Append("`n")
}
$sha = [System.Security.Cryptography.SHA256]::Create()
$srcHash = [BitConverter]::ToString($sha.ComputeHash($Utf8.GetBytes($sb.ToString()))).Replace("-", "")
$hashFile = Join-Path $StateDir "last-build.hash"
$lastHash = if (Test-Path $hashFile) { (Get-Content $hashFile -Raw).Trim() } else { "" }

$verFile = Join-Path $Root "version.properties"
function Read-Version {
    $t = [IO.File]::ReadAllText($verFile, $Utf8)
    $g = { param($k) [int]([regex]::Match($t, "(?m)^$k=(\d+)").Groups[1].Value) }
    return @((& $g "VERSION_MAJOR"), (& $g "VERSION_MINOR"), (& $g "VERSION_PATCH"))
}
$v = Read-Version
$version = "$($v[0]).$($v[1]).$($v[2])"
$suffix = if ($Debug) { "-debug" } else { "" }
$apkOut = Join-Path $Root "releases\LeoSync-$version$suffix.apk"

if (-not $Force -and $srcHash -eq $lastHash -and (Test-Path $apkOut)) {
    Write-Host "Keine Aenderungen seit dem letzten Build ($version). Nichts zu tun." -ForegroundColor Green
    Write-Host "Erzwingen mit: build.bat -Force"
    exit 0
}

# ---------------------------------------------------------------------------
if (Test-Path $apkOut) {
    Step "Version $version wurde bereits gebaut -> Patch-Version erhoehen"
    $v[2] = $v[2] + 1
    $version = "$($v[0]).$($v[1]).$($v[2])"
    $t = [IO.File]::ReadAllText($verFile, $Utf8)
    $t = [regex]::Replace($t, "(?m)^VERSION_PATCH=\d+", "VERSION_PATCH=$($v[2])")
    [IO.File]::WriteAllText($verFile, $t, $Utf8)
    $apkOut = Join-Path $Root "releases\LeoSync-$version$suffix.apk"

    $pn = Join-Path $Root "PATCHNOTES.md"
    if (Test-Path $pn) {
        $notes = [IO.File]::ReadAllText($pn, $Utf8)
        if ($notes -notmatch [regex]::Escape("## $version")) {
            $date = Get-Date -Format "yyyy-MM-dd"
            $entry = "## $version ($date)`n`n- Automatischer Build: Quellcode geaendert, Patchnotes bitte ergaenzen.`n`n"
            $idx = $notes.IndexOf("## ")
            if ($idx -ge 0) { $notes = $notes.Insert($idx, $entry) } else { $notes += "`n$entry" }
            [IO.File]::WriteAllText($pn, $notes, $Utf8)
        }
    }
}
Write-Host "Version: $version"

# ---------------------------------------------------------------------------
if (-not $Debug) {
    Step "Pruefe Signatur-Schluessel"
    $ksProps = Join-Path $Root "keystore.properties"
    if (-not (Test-Path $ksProps)) {
        $ksDir = Join-Path $Root "keystore"
        New-Item -ItemType Directory -Force $ksDir | Out-Null
        $ks = Join-Path $ksDir "sync2pc-release.jks"
        $pw = -join ((48..57) + (65..90) + (97..122) | Get-Random -Count 32 | ForEach-Object { [char]$_ })
        $keytool = Join-Path $javaHome "bin\keytool.exe"
        & $keytool -genkeypair -v -keystore $ks -storetype PKCS12 -alias sync2pc -keyalg RSA -keysize 4096 `
            -validity 36500 -storepass $pw -keypass $pw -dname "CN=LeoSync, O=Leo" | Out-Null
        if ($LASTEXITCODE -ne 0) { Fail "Keystore konnte nicht erstellt werden" }
        $content = "storeFile=keystore/sync2pc-release.jks`nstorePassword=$pw`nkeyAlias=sync2pc`nkeyPassword=$pw`n"
        [IO.File]::WriteAllText($ksProps, $content, $Utf8)
        Write-Host "Neuer Signatur-Schluessel erstellt: keystore\sync2pc-release.jks" -ForegroundColor Yellow
        Write-Host "WICHTIG: keystore\ und keystore.properties sichern! Ohne sie sind keine Updates der installierten App moeglich." -ForegroundColor Yellow
    } else { Write-Host "vorhanden" }
}

# ---------------------------------------------------------------------------
$tasks = @()
if (-not $SkipTests) { $tasks += "testDebugUnitTest" }
if ($Debug) { $tasks += "assembleDebug" } else { $tasks += "assembleRelease" }
Step ("Gradle: " + ($tasks -join " "))
# Ausgabe zusaetzlich in .build\last-build.log schreiben (zum Weitergeben an den Agent)
$logFile = Join-Path $StateDir "last-build.log"
$writer = New-Object System.IO.StreamWriter($logFile, $false, $Utf8)
$writer.WriteLine("LeoSync Build $version - " + (Get-Date -Format "yyyy-MM-dd HH:mm:ss") + " - Tasks: " + ($tasks -join " "))
$prevEap = $ErrorActionPreference
$ErrorActionPreference = 'Continue'   # stderr von Gradle darf das Skript nicht abbrechen
try {
    & (Join-Path $Root "gradlew.bat") @tasks --console=plain 2>&1 | ForEach-Object {
        $line = "$_"
        Write-Host $line
        $writer.WriteLine($line)
    }
    $gradleExit = $LASTEXITCODE
} finally {
    $ErrorActionPreference = $prevEap
    $writer.Close()
}
if ($gradleExit -ne 0) { Fail "Gradle-Build fehlgeschlagen. Vollstaendiges Protokoll: $logFile" }

$built = if ($Debug) { "app\build\outputs\apk\debug\app-debug.apk" } else { "app\build\outputs\apk\release\app-release.apk" }
$built = Join-Path $Root $built
if (-not (Test-Path $built)) { Fail "APK nicht gefunden: $built" }
Copy-Item -Force $built $apkOut
[IO.File]::WriteAllText($hashFile, $srcHash, $Utf8)

Step "Fertig"
$mb = [math]::Round((Get-Item $apkOut).Length / 1MB, 1)
Write-Host "APK: $apkOut ($mb MB)" -ForegroundColor Green
Write-Host "Installation: APK aufs Telefon kopieren und oeffnen, oder: adb install -r `"$apkOut`""
