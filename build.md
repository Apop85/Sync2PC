# Build-Anleitung – LeoSync

## Kurzfassung

1. Einmalig **Android Studio** installieren und einmal starten. Damit sind JDK 17 und das Android SDK installiert.
2. **`build.bat`** doppelklicken.
3. Die fertige APK liegt unter `releases\LeoSync-<version>.apk`.

## Voraussetzungen

| Komponente | Version | Hinweis |
|---|---|---|
| JDK | 17 bis 25 | Java 8 reicht **nicht**. Seit 0.1.3 (Gradle 9.8, AGP 9.5) funktioniert auch das Java 25 von Android Studio. Nur bei neueren Versionen lädt der Builder ein portables JDK 21 nach `.build\jdk21`. |
| Android SDK | Platform 35, Build-Tools | Wird von Android Studio bzw. Gradle installiert. |
| Internet | beim ersten Build | Gradle, Abhängigkeiten, Wrapper, Länderdaten |

Der Builder sucht ein JDK 17–25 in dieser Reihenfolge: `JAVA_HOME`, `C:\Program Files\Android\Android Studio\jbr`, installierte JDKs unter `Program Files`, `.build\jdk21`. Findet er keines, lädt er Eclipse Temurin 21 herunter (einmalig, ca. 190 MB).

**Projekt in Android Studio öffnen:** Das mitgelieferte JDK (JBR, Java 25) funktioniert seit 0.1.3. Android Studio muss AGP 9.5 unterstützen; sonst meldet der Gradle-Sync eine zu neue Plugin-Version. Dann Android Studio aktualisieren.

**Werkzeug-Versionen (seit 0.1.3):** Gradle 9.8.2, Android Gradle Plugin 9.5.0 (Kotlin eingebaut, „built-in Kotlin“), Kotlin 2.4.21, KSP 2.3.13, Room 2.8.5.
Das SDK sucht er unter `ANDROID_HOME`, `ANDROID_SDK_ROOT` und `%LOCALAPPDATA%\Android\Sdk`.

## Was `build.bat` macht

1. Prüft JDK und SDK und schreibt `local.properties`.
2. Lädt `gradle\wrapper\gradle-wrapper.jar`, falls es fehlt.
3. Lädt die Länderdaten (`tools\fetch-geodata.ps1`), falls sie fehlen.
4. Erzeugt die App-Icons und das einfarbige Benachrichtigungs-Symbol aus `.claude\icon.png` neu, wenn sich das Bild geändert hat (`tools\make-icons.ps1`). Eine grosse Vorschau des Benachrichtigungs-Symbols liegt dann unter `.build\ic_notification_preview.png`.
5. Zerlegt `.claude\gif_frames.png` in die Animationsbilder für die Benachrichtigung, wenn sich die Vorlage geändert hat (`tools\make-anim.ps1`).
6. Berechnet einen Hash über alle Quelldateien. **Ohne Änderung wird nicht gebaut.**
7. Existiert die APK der aktuellen Version schon, wird `VERSION_PATCH` in `version.properties` um 1 erhöht und in `PATCHNOTES.md` ein Platzhalter-Eintrag angelegt.
8. Beim ersten Release-Build wird ein Signatur-Schlüssel erstellt (`keystore\`, `keystore.properties`).
9. Führt die Unit-Tests aus und baut die APK (`gradlew testDebugUnitTest assembleRelease`).
10. Kopiert die APK nach `releases\`.

Die vollständige Gradle-Ausgabe jedes Builds steht zusätzlich in `.build\last-build.log`. Bei Fehlern genügt es, dem Agent zu sagen, dass der Build fehlgeschlagen ist; er liest das Protokoll selbst.

### Optionen

```bash
build.bat -Force
```
Baut immer, auch ohne Änderungen.

```bash
build.bat -SkipTests
```
Überspringt die Unit-Tests.

```bash
build.bat -Debug
```
Baut eine Debug-APK (`com.sync2pc.app.debug`, parallel installierbar).

## Signatur-Schlüssel sichern

`keystore\sync2pc-release.jks` und `keystore.properties` sind **nicht** in Git (Geheimnisse).
Geht der Schlüssel verloren, kann die installierte App nicht mehr aktualisiert werden. Sie muss dann deinstalliert werden, und ihre Daten gehen verloren.
**Beide Dateien sicher aufbewahren.**

## Versionierung

- `version.properties` enthält `MAJOR.MINOR.PATCH`.
- `versionCode = MAJOR*1000000 + MINOR*1000 + PATCH`
- Bis 0.0.13: Phase `0.0.x`.
- Seit 0.1.0: **Beta**. `0.1.x` enthält nur Fehlerbehebungen (PATCH +1 pro Änderung). Kommt eine neue Funktion dazu, folgt `0.2.0`, dann `0.3.0` usw.
- `1.0.0` gibt nur der Projektinhaber frei.

## Release-Checkliste (Beta)

Vor jedem GitHub-Release:

1. `build.bat` läuft ohne Fehler durch (Unit-Tests und R8 inklusive).
2. Falls sich die Datenbank geändert hat: Es gibt eine neue Datei `app\schemas\…\<Version>.json` und eine passende Migration in `AppDatabase`. Die Schema-Datei wird committet.
3. Update über die bestehende Version installieren, **nicht** neu installieren. Die App startet, Profile und Verlauf sind noch da (prüft die Migration).
4. Bildordner-Profil: *Vorschau* und *Jetzt synchronisieren* klappen, neue Fotos landen im richtigen Monats- bzw. Länderordner, und es entstehen keine Duplikate.
5. Ordner-Profil: Je eine Datei auf dem Handy und auf dem PC ändern → beide werden abgeglichen.
6. WLAN während eines Syncs trennen → „Pausiert“ erscheint. WLAN wieder verbinden → der Sync setzt fort.
7. *Einstellungen → Diagnosedaten kopieren* und *Verlauf → Teilen* funktionieren.
8. *Einstellungen → Sicherung*: Export mit und ohne Export-Passwort. Import mit „Hinzufügen“ ergibt doppelte Profile, die danach gelöscht werden.
9. *Speicher freigeben* mit grosser Monatszahl (z. B. 120) und kleiner Auswahl: Die Prüfung auf dem PC läuft, der Android-Dialog erscheint, und im Papierkorb der Galerie sind die Dateien wiederherstellbar.
10. Ordner-Sync (siehe „Test Ordner-Sync“ unten), wenn sich daran etwas geändert hat.
11. `PATCHNOTES.md` ist ergänzt. Release `v<version>` mit der APK anlegen (siehe unten).
12. Nach dem Release: Das Selbst-Update auf einem Gerät mit der Vorversion prüfen.

### Test Ordner-Sync

Vorbereitung auf dem PC: In der Freigabe einen Testordner anlegen, z. B. `\\192.168.3.3\Fotos\_Test-Ordnersync`, und darin:
- einige hundert kleine Dateien (z. B. Textdateien oder kleine PDFs, gern in Unterordnern)
- 2–3 grosse Dateien (je einige hundert MB, z. B. ein Video)

Auf dem Handy:
1. Neues Profil → **Ordner synchronisieren**. Gleiche Freigabe und Zugangsdaten wie beim Fotos-Profil (über die Chips übernehmen), als Unterordner `_Test-Ordnersync` wählen.
2. Handy-Ordner: einen **neuen, leeren** Ordner wählen (z. B. `Documents/LeoSync-Test`), Richtung **Beidseitig**.
3. *Vorschau* → alle Dateien erscheinen als „↓ PC → Handy“.
4. *Jetzt synchronisieren*: Im Verlauf steht „Übertrage … mit 3 parallelen Verbindungen“. Die kleinen Dateien kommen zuerst, die grossen am Schluss.
5. **Fortsetzen testen:** Während eine grosse Datei lädt, das WLAN kurz trennen und wieder verbinden. Im Verlauf erscheint danach „↓ … (fortgesetzt ab … MB)“.
6. **Rückweg:** Auf dem Handy eine Datei ändern oder neu anlegen → nächster Sync lädt sie hoch („↑ …“).
7. Es gibt auf dem Handy keine doppelten Ordner wie „Ordner (1)“.
8. Zum Schluss das Test-Profil löschen. Die Dateien auf PC und Handy bleiben erhalten und können von Hand entfernt werden.

## Manuell bauen (ohne Builder)

```bash
gradlew.bat testDebugUnitTest assembleRelease
```

Alternativ das Projekt in Android Studio öffnen: *File → Open → Projektordner*.

## Release auf GitHub veröffentlichen (für Selbst-Updates)

Die App holt Updates aus dem **neuesten Release** des eingestellten GitHub-Repositorys.

1. Das Repository ist als Vorgabe in `version.properties` eingetragen (`UPDATE_REPO=Apop85/Sync2PC`) und in der App unter *Einstellungen → Updates* änderbar. Nach einer Änderung in `version.properties` mit `build.bat -Force` bauen, weil die Datei nicht zum Änderungs-Hash zählt.
2. Version bauen (`build.bat`).
3. Auf GitHub ein Release mit dem Tag `v<version>` anlegen (z. B. `v0.0.3`) und die Datei `releases\LeoSync-<version>.apk` anhängen. Der Dateiname muss auf `.apk` enden und darf nicht „debug“ enthalten. Markiere es nicht als „Pre-release“, auch nicht in der Beta: Die App fragt nur `releases/latest` ab, und dort erscheinen Pre-releases nicht.
4. Der Release-Text erscheint in der App als Änderungsbeschreibung. Am einfachsten kopierst du den Abschnitt aus `PATCHNOTES.md`.

Mit der GitHub-CLI geht es in einem Schritt:

```bash
gh release create v0.1.0 releases\LeoSync-0.1.0.apk --title "LeoSync 0.1.0" --notes "Siehe PATCHNOTES.md"
```

**Wichtig:** Alle Versionen müssen mit demselben Schlüssel (`keystore\`) signiert sein, sonst lehnt Android das Update ab.

## Installation auf dem Telefon

- APK aufs Telefon kopieren und öffnen („Unbekannte Apps installieren“ für den Dateimanager erlauben), oder
- per USB-Debugging:

```bash
adb install -r releases\LeoSync-0.1.0.apk
```

## Fehlerbehebung

| Problem | Lösung |
|---|---|
| `What went wrong: 2x.0.x` (Java-Version) | Gradle läuft mit einem zu neuen Java. Mit `build.bat` bauen (wählt das JDK selbst) bzw. in Android Studio das Gradle JDK auf 17–25 stellen |
| `The 'org.jetbrains.kotlin.android' plugin is no longer required` | Seit AGP 9 ist Kotlin eingebaut. Das Plugin darf in keiner `build.gradle.kts` mehr stehen |
| „Portables JDK konnte nicht eingerichtet werden“ | JDK 21 manuell installieren (z. B. Eclipse Temurin) und `JAVA_HOME` setzen |
| „Android SDK nicht gefunden“ | Android Studio einmal starten, SDK-Setup abschliessen |
| SDK-Lizenzen nicht akzeptiert | `"%LOCALAPPDATA%\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat" --licenses` |
| R8: „Missing class …“ | Inhalt von `app\build\outputs\mapping\release\missing_rules.txt` in `app\proguard-rules.pro` übernehmen (oder dem Agent schicken) |
| Release-APK stürzt ab, Debug nicht | Wahrscheinlich fehlt eine R8-Keep-Regel. Logcat-Ausgabe dem Agent schicken. |
| Länderdaten-Download schlägt fehl | Manuell `ne_50m_admin_0_countries.geojson` von Natural Earth nach `app\src\main\assets\geo\countries.geojson` kopieren |
