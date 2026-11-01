# LeoSync

Teil der Leo-Tool-Familie.

Android-App, die Fotos und Videos vom Telefon automatisch auf eine Windows-Freigabe (SMB) überträgt und dort nach **Jahr, Monat und Reiseland** sortiert. Ausserdem synchronisiert sie beliebige Ordner in beide Richtungen.

```
\\fs01.local\Fotos\2026\06\IMG_20260614_101530.jpg
\\fs01.local\Fotos\2026\06 - Ägypten\IMG_20260620_083012.jpg
\\fs01.local\Fotos\2026\06 - Ägypten Hurghada\...   ← vom Benutzer umbenannt, wird weiter genutzt
```

## Funktionen

Es gibt zwei Profiltypen:

| | Fotos & Videos sichern | Ordner synchronisieren |
|---|---|---|
| Ablage auf dem PC | `Jahr\Monat - Land\` | 1:1 wie auf dem Handy |
| Richtung | Handy → PC | Handy → PC, PC → Handy oder beidseitig |
| Dateitypen | Fotos, Videos | alle |
| Löschen | optional nach geprüftem Upload | optional Löschungen übernehmen (Papierkorb) |

- **Automatisch im Heim-WLAN.** Sobald das Telefon mit einem eingetragenen WLAN verbunden ist, startet der Sync. Neue Fotos werden direkt nachgeladen. Ist der PC aus, prüft die App alle 30 Minuten erneut. Nach einem WLAN-Unterbruch setzt sie sofort beim Wiederverbinden fort.
- **Sortierung nach Aufnahmedatum.** Die App liest EXIF bzw. Video-Metadaten, sonst das Datum im Dateinamen, sonst das Änderungsdatum.
- **Länderordner aus GPS, offline.** Es gibt keine Online-Abfrage. Fotos aus dem Heimatland (Standard: Schweiz) bekommen keinen Länderzusatz.
- **Eigene Ordnernamen.** Monatsordner dürfen umbenannt werden, solange die Monatszahl erhalten bleibt (`06 - Ägypten mit Familie`). Bei Mehrdeutigkeit fragt die App einmal pro Monat und Land, nicht pro Bild.
- **Keine Duplikate.** Die App gleicht mit einer lokalen Datenbank und dem Share ab (Grösse plus Teil-Prüfsumme). Ein gleicher Name mit anderem Inhalt wird zu `bild_2.jpg`.
- **Robust und schnell.** Abgebrochene Uploads werden fortgesetzt. Erst nach vollständigem Upload wird umbenannt, es entstehen keine halben Dateien. Bis zu 4 Übertragungen laufen parallel.
- **Original-Datum.** Die Dateien auf dem PC behalten das Änderungsdatum vom Telefon.
- **Statistik:** Anzahl und Speicher pro Jahr und Land.
- **Optional aufräumen.** Nach geprüftem Upload kann die App Dateien in den Papierkorb verschieben oder löschen. Standard ist behalten.
- **Akkuschonend.** Es gibt keinen Dauerdienst. Die App nutzt System-Callbacks und WorkManager und hat eine Option „Nur beim Laden“.
- **Fortschritt** erscheint in der Benachrichtigung und auf dem Sperrbildschirm.
- **Profile** für mehrere Ziele, eine **Vorschau** (Trockenlauf) und einen **Verlauf** mit Log.
- **Selbst-Update** über GitHub Releases: Die App prüft täglich, ein Tippen installiert, danach startet sie neu.
- **Ordner-Sync:** 3-Wege-Abgleich (Handy, PC, letzter Stand) mit Konflikterkennung. Ersetzte und gelöschte Dateien landen im `.LeoSync-Papierkorb`, nicht im Nirgendwo. Der Papierkorb leert sich nach einer pro Profil einstellbaren Frist (Standard 30 Tage, 0 = nie).
- **Fehlersuche:** Der Verlauf lässt sich über das Android-Teilen-Menü exportieren. *Einstellungen → Beta / Diagnose* kopiert Diagnosedaten (ohne Passwörter) mit einer zufälligen Geräte-ID in die Zwischenablage.
- **Filter:** White- und Blacklist mit Mustern wie `*.pdf` oder `Temp/**`.

## Download

Status: **Beta (0.1.x)** auf dem Weg zu 1.0.0. Die neueste APK findest du unter [Releases](https://github.com/Apop85/Sync2PC/releases/latest). Danach aktualisiert sich die App selbst.

## Voraussetzungen

- Android 10 oder neuer
- Windows-PC mit freigegebenem Ordner (Windows-Ordnerfreigabe, SMB2/SMB3) und einem Benutzerkonto mit Schreibrecht

## Einrichtung

1. App installieren (siehe [build.md](build.md)).
2. Berechtigungen erteilen. Die Startseite führt durch alle nötigen Schritte:
   - **Fotos & Videos** und **Standort in Fotos** (für Länderordner)
   - **Standort „Immer zulassen“**: Android gibt den WLAN-Namen nur mit Standortberechtigung heraus. Die App nutzt den Standort **nicht**.
   - **Benachrichtigungen** und **Akku-Optimierung aus**
3. Profil anlegen: Freigabe (`\\server\freigabe`), Benutzer und Passwort, WLAN und Quellordner. Die Quellordner wählst du über die Android-Dateiauswahl, z. B. `DCIM › Camera`.
4. Mit **Verbindung testen** und **Vorschau** prüfen und dann **Jetzt synchronisieren**.

## Bestehende Fotos vom PC einsortieren

`tools\LeoSync-Import.bat` per Doppelklick starten. Das Werkzeug sortiert einen Ordner auf dem PC nach denselben Regeln wie die App in den Share ein:

1. Quellordner wählen und Ziel-Share angeben (z. B. `\\192.168.1.20\Fotos`; wird gemerkt).
2. Bei umbenannten Monatsordnern fragt es einmal pro Monat bzw. Land nach.
3. Eine Vorschau zeigt, was in welchen Ordner kommt und was schon vorhanden ist.
4. Kopiert wird erst nach Bestätigung mit „j“. Der Quellordner bleibt unverändert.

Voraussetzungen: Windows mit PowerShell 5.1 (vorinstalliert), für Videodaten `ffprobe` (Teil von ffmpeg). Ohne ffprobe gelten Dateiname bzw. Änderungsdatum. Die Ländererkennung nutzt `app\src\main\assets\geo\countries.geojson`; das Werkzeug muss deshalb im Projektordner liegen.

Einschränkung: Bei HEIC-Fotos kann Windows ohne Zusatz das EXIF-Datum und GPS nicht lesen. Dann gelten Dateiname bzw. Änderungsdatum, und es gibt keinen Länderordner.

## Projektstruktur

```
app/src/main/java/com/sync2pc/app/
  data/    Room-Datenbank, Profile, Einstellungen, Passwort-Verschlüsselung (Android Keystore)
  media/   MediaStore-Scan, EXIF-/Video-Metadaten, Datum aus Dateinamen
  geo/     Offline-Ländererkennung (Punkt-in-Polygon, Natural Earth)
  smb/     SMB-Verbindung (smbj), Upload mit Fortsetzung, Teil-Hash
  sync/    Sync-Engine, Zielordner-Logik, Worker, WLAN-Trigger, Benachrichtigungen, Löschen
  mirror/  Ordner-Spiegelung (Handy-Ordner via Android-Ordnerfreigabe, 3-Wege-Abgleich, Filter)
  update/  Selbst-Update (GitHub Releases, PackageInstaller)
  diag/    Diagnosedaten und Verlauf-Export (Beta)
  ui/      Jetpack Compose Oberfläche
tools/     Build-Skripte (One-Click-Builder, Icons, Animation, Länderdaten) und LeoSync-Import (PC → Share)
```

## Bauen

Siehe [build.md](build.md). Kurz: `build.bat` doppelklicken.

## Technik

Kotlin · Jetpack Compose · WorkManager · Room · [smbj](https://github.com/hierynomus/smbj) · AndroidX ExifInterface · Natural-Earth-Länderdaten (Public Domain)

## Lizenz

[MIT](LICENSE). Länderdaten: [Natural Earth](https://www.naturalearthdata.com/) (Public Domain).
