# Sync2PC

Android-App, die Fotos und Videos vom Telefon automatisch auf eine Windows-Freigabe (SMB) überträgt und dort nach **Jahr, Monat und Reiseland** sortiert.

```
\\fs01.local\Fotos\2026\06\IMG_20260614_101530.jpg
\\fs01.local\Fotos\2026\06 - Ägypten\IMG_20260620_083012.jpg
\\fs01.local\Fotos\2026\06 - Ägypten Hurghada\...   ← vom Benutzer umbenannt, wird weiter genutzt
```

## Funktionen

- **Automatisch im Heim-WLAN.** Sobald das Telefon mit einem eingetragenen WLAN verbunden ist, startet der Sync. Neue Fotos werden direkt nachgeladen.
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

## Download

Die neueste APK findest du unter [Releases](https://github.com/Apop85/Sync2PC/releases/latest). Danach aktualisiert sich die App selbst.

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

## Projektstruktur

```
app/src/main/java/com/sync2pc/app/
  data/    Room-Datenbank, Profile, Einstellungen, Passwort-Verschlüsselung (Android Keystore)
  media/   MediaStore-Scan, EXIF-/Video-Metadaten, Datum aus Dateinamen
  geo/     Offline-Ländererkennung (Punkt-in-Polygon, Natural Earth)
  smb/     SMB-Verbindung (smbj), Upload mit Fortsetzung, Teil-Hash
  sync/    Sync-Engine, Zielordner-Logik, Worker, WLAN-Trigger, Benachrichtigungen, Löschen
  update/  Selbst-Update (GitHub Releases, PackageInstaller)
  ui/      Jetpack Compose Oberfläche
tools/     Build-Skripte (One-Click-Builder, Icons, Länderdaten)
```

## Bauen

Siehe [build.md](build.md). Kurz: `build.bat` doppelklicken.

## Technik

Kotlin · Jetpack Compose · WorkManager · Room · [smbj](https://github.com/hierynomus/smbj) · AndroidX ExifInterface · Natural-Earth-Länderdaten (Public Domain)

## Lizenz

[MIT](LICENSE). Länderdaten: [Natural Earth](https://www.naturalearthdata.com/) (Public Domain).
