# Patchnotes – Sync2PC

Versionierung: `0.0.x` bis zur Freigabe von `1.0.0`. Neueste Version oben.

## 0.0.7 (2026-11-01)

### Neu
- **Ordner synchronisieren:** neuer Profiltyp neben „Fotos & Videos sichern“. Ein beliebiger Handy-Ordner wird 1:1 mit einem PC-Ordner gespiegelt, mit Unterordnern und allen Dateitypen, ohne Jahresordner.
  - **Richtung pro Profil:** Handy → PC, PC → Handy oder beidseitig.
  - **Löschungen übernehmen** (optional, Standard aus): Gelöschte Dateien landen auf der Gegenseite im Ordner `.Sync2PC-Papierkorb\<Datum>`. Ist die Option aus, werden gelöschte Dateien wiederhergestellt.
  - **Konflikte** (beidseitig, auf beiden Seiten geändert): Standard ist, zu fragen (Handy-Version, PC-Version oder beide behalten). Optional pro Profil: immer die neuere Datei. Die verworfene Version kommt in den Papierkorb, nichts wird endgültig gelöscht.
  - **Prüfintervall** für Änderungen (Standard 60 Minuten, nur im Ziel-WLAN), zusätzlich sofort beim Verbinden mit dem WLAN.
  - **Speicherplatzprüfung:** Downloads, für die auf dem Handy nicht genug Platz ist (300 MB Reserve), werden übersprungen und gemeldet.
- **Profil-Assistent:** Beim Anlegen wählst du zuerst den Typ, danach zeigt der Editor nur die passenden Optionen.
- **Unterordner auf dem Share** pro Profil, mit Ordner-Browser („Durchsuchen“).
- **White- und Blacklist** (z. B. `*.pdf`, `Thumbs.db`, `Temp/**`) für beide Profiltypen.
- **Konflikt-Seite** mit „Alle: neuere“ sowie Benachrichtigung und Karte auf der Startseite.
- Die Vorschau zeigt bei Ordner-Profilen die geplante Aktion je Datei.

### Behoben
- Das Aufräumen alter Verlaufseinträge hat bei jedem Lauf den gesamten Verlauf gelöscht (falsche Zeitgrenze). Jetzt bleiben Verlaufseinträge 90 Tage und Detail-Logs 30 Tage.

### Build
- Der Builder schreibt die vollständige Gradle-Ausgabe zusätzlich nach `.build\last-build.log`.

### Technisch
- Datenbank Version 2 mit Migration; bestehende Profile bleiben erhalten und werden zu Bildordner-Profilen.
- Bildordner-Profile sind fest auf Handy → PC eingestellt. „Nach Sync löschen“ gibt es nur bei Bildordnern.

## 0.0.6 (2026-11-01)

### Neu
- **Zugangsdaten wiederverwenden:** Im Profil-Editor gibt es oben bei „Ziel“ eine Auswahl der anderen Profile. Ein Tippen übernimmt Benutzer, Passwort und Domäne, bei leerem Pfad auch den Freigabe-Pfad.

## 0.0.5 (2026-11-01)

### Geändert
- **Fortsetzen nach WLAN-Unterbruch:** Bricht ein Sync ab, weil das WLAN weg ist, wartet die App nicht mehr 30 Minuten. Sie setzt sofort fort, sobald das gemerkte WLAN wieder verbunden ist. Das gilt auch bei kurzen Aussetzern, die bisherige 60-Sekunden-Drosselung greift dann nicht.
- Die 30-Minuten-Prüfung gilt nur noch, wenn das Ziel-WLAN verbunden bleibt, der PC aber nicht erreichbar ist (z. B. ausgeschaltet).

## 0.0.4 (2026-11-01)

### Neu
- **PC-Erreichbarkeit im Ziel-WLAN:** Vor jedem Sync prüft die App mit einem kurzen Verbindungstest (Port 445, max. 3 Sekunden, ohne Anmeldung), ob der PC erreichbar ist.
  - Ist er nicht erreichbar, wird nach **30 Minuten** erneut geprüft, solange das Telefon im Ziel-WLAN ist. In der Zwischenzeit lösen auch neue Fotos keine Prüfung aus.
  - Eine neue WLAN-Verbindung und „Jetzt synchronisieren“ prüfen sofort.
  - Bricht die Verbindung während eines Laufs ab, gilt dieselbe 30-Minuten-Regel.
  - Die Startseite zeigt „PC nicht erreichbar – nächste Prüfung um HH:MM“.
- Verbindungsfehler im Automatikbetrieb erzeugen keine Fehler-Benachrichtigung mehr; die Wiederholung läuft still.

### Geändert
- Die automatische WorkManager-Wiederholung bei Fehlern (2 Minuten, exponentiell) ist durch die 30-Minuten-Regel ersetzt.

## 0.0.3 (2026-11-01)

### Neu
- **Selbst-Update über GitHub Releases:** Die App prüft täglich im WLAN (abschaltbar) und meldet eine neue Version per Benachrichtigung. Ein Tippen lädt sie herunter und startet die Installation. Android fragt einmal nach der Erlaubnis „Apps installieren“ und bei jedem Update nach Bestätigung.
  - Vor der Installation prüft die App die heruntergeladene Datei (gleiche App, höhere Version). Die Signatur prüft Android.
  - Nach dem Update startet die App neu. Lässt Android das nicht zu, kommt die Benachrichtigung „Update installiert – tippen zum Öffnen“.
  - Quelle: [github.com/Apop85/Sync2PC](https://github.com/Apop85/Sync2PC). Das ist voreingestellt und in den Einstellungen unter „Updates“ änderbar.
- **Benachrichtigungs-Symbol** aus dem Hunde-Logo: einfarbige Silhouette mit Augen, Nase und Halsband, statt des generischen Symbols.

### Geändert
- **Ordnerauswahl** über den Android-Systemdialog („Dateien“) statt einer eigenen Liste.

## 0.0.2 (2026-11-01)

### Neu
- **Parallele Übertragungen:** Standard sind 3 gleichzeitige Uploads mit je eigener SMB-Verbindung, einstellbar von 1 bis 4. Das bringt mehr Tempo bei vielen kleinen Dateien.
- **Original-Änderungsdatum:** Die Datei auf dem PC übernimmt das Änderungsdatum vom Telefon (Erstell- und Änderungsdatum) statt des Kopierzeitpunkts.
- **Statistik:** Anzahl Dateien und Speicher pro Jahr und pro Land (aus GPS), Fotos und Videos getrennt gezählt.
- **Lizenz:** MIT.

### Geändert
- Der Release-Build ist mit R8 verkleinert (Minify und Shrink Resources). Die Keep-Regeln für smbj und BouncyCastle stehen in `app/proguard-rules.pro`.
- Gleichzeitiges Anlegen desselben Ordners durch mehrere Verbindungen wird toleriert.

### Build
- Der Builder verwendet nur noch Java 17–24. Neuere Android-Studio-Versionen bringen Java 25 mit, und damit bricht Gradle 8.11 ab (`What went wrong: 25.0.4`). In diesem Fall lädt der Builder einmalig ein portables JDK 21 nach `.build\jdk21`.

## 0.0.1 (2026-11-01)

Erste Version.

### Sync
- Überträgt Fotos und Videos per SMB2/SMB3 auf eine Windows-Freigabe (Benutzer + Passwort).
- Ordnerstruktur `<Jahr>\<Monat>` bzw. `<Jahr>\<Monat> - <Land>`, z. B. `2026\06 - Ägypten`.
- Datum: EXIF (Fotos) bzw. Container-Metadaten (Videos) → Datum im Dateinamen → Änderungsdatum.
- Land offline aus GPS-Koordinaten (Natural Earth). Heimatland (Standard: Schweiz) ohne Länderzusatz.
- Jedes Land bekommt einen eigenen Monatsordner.
- Live-Fotos/Bewegtbilder: Ein Video mit gleichem Namen wie ein Foto landet im selben Ordner.
- Umbenannte Monatsordner werden erkannt (nur die Monatszahl muss bleiben). Bei Mehrdeutigkeit fragt die App einmal pro Gruppe (Monat + Land), nicht pro Datei. „Alle übernehmen“ erledigt alles mit einem Klick.
- Gleicher Name, anderer Inhalt: `bild_2.jpg`, `bild_3.jpg`, …
- Duplikaterkennung: lokale Datenbank plus Abgleich mit dem Share (Grösse, dann Teil-Hash über Anfang, Mitte und Ende).
- Abgebrochene Uploads werden fortgesetzt (Temp-Datei `.s2ptmp`, danach Umbenennung).
- Optionale Prüfung nach dem Upload. Pflicht, wenn Dateien entfernt werden sollen.
- Nach dem Sync: behalten (Standard), Papierkorb oder endgültig löschen. Android 11+ fragt einmal pro Durchgang nach.
- Mindestalter für Dateien (Standard: 5 Minuten) mit automatischem Nachhol-Sync.

### Hintergrund und Akku
- Startet automatisch, wenn ein Heim-WLAN verbunden wird (System-Callback, die App muss nicht laufen).
- Neue Fotos werden gebündelt erkannt und hochgeladen, sobald das Heim-WLAN verbunden ist.
- Sicherheitsnetz: Prüfung alle 6 Stunden (nur WLAN).
- Option „Nur beim Laden“.
- Fortschritt als Benachrichtigung, auch auf dem Sperrbildschirm, mit „Abbrechen“.

### Oberfläche
- Profile: mehrere Ziele mit eigenen Zugangsdaten, Quellordnern, WLANs und Löschoptionen.
- Verbindungstest, Ordnerauswahl mit Dateianzahl, Vorschau (Trockenlauf), Verlauf mit Log.
- Einrichtungshilfe für alle nötigen Berechtigungen.

### Projekt
- One-Click-Builder (`build.bat`): baut nur bei Änderungen, erhöht die Patch-Version automatisch, signiert die APK.
- Unit-Tests für Ordnernamen, Dateinamen-Datum, SMB-Pfade und Teil-Hash.
