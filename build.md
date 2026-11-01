# Build-Anleitung – LeoSync

## Kurzfassung

1. Einmalig **Android Studio** installieren und einmal starten. Damit sind JDK 17 und das Android SDK installiert.
2. **`build.bat`** doppelklicken.
3. Die fertige APK liegt unter `releases\LeoSync-<version>.apk`.

## Voraussetzungen

| Komponente | Version | Hinweis |
|---|---|---|
| JDK | 17 bis 24 | Java 8 reicht **nicht**, Java 25+ wird von Gradle 8.11 nicht unterstützt. Neue Android-Studio-Versionen bringen Java 25 mit. Dann lädt der Builder automatisch ein portables JDK 21 nach `.build\jdk21`. |
| Android SDK | Platform 35, Build-Tools | Wird von Android Studio bzw. Gradle installiert. |
| Internet | beim ersten Build | Gradle, Abhängigkeiten, Wrapper, Länderdaten |

Der Builder sucht ein JDK 17–24 in dieser Reihenfolge: `JAVA_HOME`, `C:\Program Files\Android\Android Studio\jbr`, installierte JDKs unter `Program Files`, `.build\jdk21`. Findet er keines, lädt er Eclipse Temurin 21 herunter (einmalig, ca. 190 MB).

**Projekt in Android Studio öffnen:** Unter *Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK* ein JDK 17–24 wählen, z. B. `.build\jdk21\…`. Mit dem mitgelieferten Java 25 schlägt der Gradle-Sync fehl.
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
- Phase `0.0.x` bis zur Freigabe von `1.0.0` durch den Projektinhaber.

## Manuell bauen (ohne Builder)

```bash
gradlew.bat testDebugUnitTest assembleRelease
```

Alternativ das Projekt in Android Studio öffnen: *File → Open → Projektordner*.

## Release auf GitHub veröffentlichen (für Selbst-Updates)

Die App holt Updates aus dem **neuesten Release** des eingestellten GitHub-Repositorys.

1. Das Repository ist als Vorgabe in `version.properties` eingetragen (`UPDATE_REPO=Apop85/Sync2PC`) und in der App unter *Einstellungen → Updates* änderbar. Nach einer Änderung in `version.properties` mit `build.bat -Force` bauen, weil die Datei nicht zum Änderungs-Hash zählt.
2. Version bauen (`build.bat`).
3. Auf GitHub ein Release mit dem Tag `v<version>` anlegen (z. B. `v0.0.3`) und die Datei `releases\LeoSync-<version>.apk` anhängen. Der Dateiname muss auf `.apk` enden und darf nicht „debug“ enthalten. Markiere es nicht als „Pre-release“.
4. Der Release-Text erscheint in der App als Änderungsbeschreibung. Am einfachsten kopierst du den Abschnitt aus `PATCHNOTES.md`.

Mit der GitHub-CLI geht es in einem Schritt:

```bash
gh release create v0.0.12 releases\LeoSync-0.0.12.apk --title "LeoSync 0.0.12" --notes "Siehe PATCHNOTES.md"
```

**Wichtig:** Alle Versionen müssen mit demselben Schlüssel (`keystore\`) signiert sein, sonst lehnt Android das Update ab.

## Installation auf dem Telefon

- APK aufs Telefon kopieren und öffnen („Unbekannte Apps installieren“ für den Dateimanager erlauben), oder
- per USB-Debugging:

```bash
adb install -r releases\LeoSync-0.0.12.apk
```

## Fehlerbehebung

| Problem | Lösung |
|---|---|
| `What went wrong: 25.0.x` | Gradle läuft mit Java 25. Mit `build.bat` bauen (wählt das JDK selbst) bzw. in Android Studio das Gradle JDK auf 17–24 stellen |
| „Portables JDK konnte nicht eingerichtet werden“ | JDK 21 manuell installieren (z. B. Eclipse Temurin) und `JAVA_HOME` setzen |
| „Android SDK nicht gefunden“ | Android Studio einmal starten, SDK-Setup abschliessen |
| SDK-Lizenzen nicht akzeptiert | `"%LOCALAPPDATA%\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat" --licenses` |
| R8: „Missing class …“ | Inhalt von `app\build\outputs\mapping\release\missing_rules.txt` in `app\proguard-rules.pro` übernehmen (oder dem Agent schicken) |
| Release-APK stürzt ab, Debug nicht | Wahrscheinlich fehlt eine R8-Keep-Regel. Logcat-Ausgabe dem Agent schicken. |
| Länderdaten-Download schlägt fehl | Manuell `ne_50m_admin_0_countries.geojson` von Natural Earth nach `app\src\main\assets\geo\countries.geojson` kopieren |
