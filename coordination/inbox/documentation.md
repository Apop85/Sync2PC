## Aufträge
(neueste oben)

### SYNC-20261108-018 – Doku-Nachführung zu Version 0.11.5 (Schalter mobile Daten/VPN, „Problem melden“)
- Erstellt am: 2026-11-08
- Absender: Development
- Empfänger: Documentation
- Priorität: LOW
- Status: OPEN
- Übergabegrund: Dokumentationsänderungen liegen ausschliesslich bei Documentation.
- Aufgabenbeschreibung: (1) DEC-126 von Accepted auf Implemented (0.11.5) und Anhang A Punkt 14; Verhalten: Schalter `syncOffWifi` (Standard aus, Einstellungen › Übertragung, in Sicherung und Diagnose); ausserhalb des WLAN keine SSID-Prüfung, alle aktiven Profile berechtigt; im WLAN (auch VPN über WLAN) bleibt die SSID-Regel (DEC-044); Netzwerkbedingung der automatischen Aufträge `CONNECTED` statt `UNMETERED`; PC-Erreichbarkeit wie bisher; Auslöser ausserhalb des WLAN: neue Medien, Sicherheitsnetz (6 h), Ordner-Prüfung (kein WLAN-Broadcast, kein Auslöser „mobile Daten verbunden“); Fortsetzen-Dienst/„Pausiert“ bleibt WLAN-gebunden; Datenverbrauch-Hinweis in der App. Weiter ARCHITECTURE/SYNC-ENGINE (Netzwerkregeln) und DATA-MODEL (Einstellung `syncOffWifi`, Sicherungsfeld, abwärtskompatibel). (2) DEC-121/DEC-123 auf Implemented (0.11.5) und Anhang A Punkt 18: Knopf „Problem melden“ in Einstellungen › Diagnose; `Diagnostics.reportProblem` öffnet per `ACTION_SENDTO mailto:` die Mail-App (`leotools@protonmail.com`, Betreff mit Version, Hinweistext, Diagnose als Anhang `LeoSync-Diagnose_<Zeit>.txt`), Benutzer sendet selbst; ohne Mail-App Adresse in Zwischenablage. Knopf nicht ausgeblendet (Adresse fest hinterlegt; DEC-121 nannte Ausblenden ohne Adresse). `PRIVACY_*.md` Abschnitte 6/7 passen zur Funktion; Platzhalter (Name Z. 14, E-Mail Z. 16 und 230) stehen noch, E-Mail wäre `leotools@protonmail.com`, Name Sache des Inhabers. (3) `PATCHNOTES.md` 0.11.5 (Neu: Schalter mobile Daten/VPN, Problem melden; Intern: `WifiHelper.onAllowedNetwork`, `SyncScheduler.autoConstraints`, 8 Strings EN/DE) und 0.11.4 (Intern: Typfehler `collectAsState` in `HomeParts.kt` behoben).
- Technischer Kontext: Aufträge SYNC-20261108-003 und -008 (COMPLETED, Details in `inbox/development.md`). Nicht gebaut/getestet. Nachtrag 0.11.6 nach Review: Beim Umschalten des Schalters wird zusätzlich die Wartezeit „PC nicht erreichbar“ verworfen (`cancelShareRetry`); Hinweistext nennt jetzt mögliche Kosten beim Mobilfunkabo (z. B. Ausland). PATCHNOTES 0.11.6 entsprechend. Offener Folgeauftrag SYNC-20261108-019 (Wiederholungen über Mobilfunk begrenzen).
- Betroffene Dateien: `docs/DECISIONS.md`, `docs/ARCHITECTURE.md`, `docs/SYNC-ENGINE.md`, `docs/DATA-MODEL.md`, `PATCHNOTES.md`
- Erwartetes Ergebnis: Doku passt zum Ist-Stand; Rückmeldung mit geänderten Stellen.
- Tatsächlich durchgeführte Arbeiten: –
- Ergebnis und Rückmeldung: –
- Abgeschlossen am: –

### SYNC-20261108-017 – Doku-Nachführung zu Version 0.11.3 («Ordner anlegen», DEC-192)
- Erstellt am: 2026-11-08
- Absender: Development
- Empfänger: Documentation
- Priorität: LOW
- Status: OPEN
- Übergabegrund: Dokumentationsänderungen (DECISIONS, ARCHITECTURE, TESTING, PATCHNOTES) liegen ausschliesslich bei Documentation.
- Aufgabenbeschreibung: (1) `DECISIONS.md`: DEC-192 (Ordner anlegen in der PC-Ordnerauswahl) als umgesetzt in 0.11.3 nachführen; Anhang A Punkt 24 entsprechend. Verhalten: Zeile «Neuer Ordner» nur unterhalb einer Freigabe (keine Freigaben anlegen); Namensprüfung lokal (leer, über 255 Zeichen, `\ / : * ? " < > |`, Steuerzeichen, «.»/«..», Punkt/Leerzeichen am Ende; Dublette ohne Gross-/Kleinschreibung); Anlegen über `SmbSession.ensureDir` auf eigener Verbindung, Erfolg nur wenn der Ordner danach existiert; Fehler (Rechte, Netz) im Dialog, Eingabe bleibt; nach Erfolg wechselt die Auswahl in den neuen Ordner. Restrisiko: Windows-reservierte Namen (CON, NUL …) nicht lokal abgefangen, Serverfehler wird angezeigt. (2) `ARCHITECTURE.md`: Beschreibung von `RemoteFolderPicker` um «Ordner anlegen» ergänzen. (3) `TESTING.md`: neue Testklasse `RemoteFolderNameTest` (Namensprüfung `invalidFolderName`); nicht ausgeführt, nur erstellt (Inhaberregel «nichts bauen»). (4) `PATCHNOTES.md`: Eintrag 0.11.3 (Neu: Ordner auf dem PC direkt in der Ordnerauswahl anlegen; Intern: `invalidFolderName`, 6 neue Strings EN/DE, `RemoteFolderNameTest`).
- Technischer Kontext: Auftrag SYNC-20261108-006 (COMPLETED, Details in `inbox/development.md`). Keine Schema-, DAO- oder `SmbSession`-Änderung. `version.properties` steht inzwischen auf 0.11.4. Nachtrag: 0.11.3 scheiterte im Build an einem Typfehler in `ui/HomeParts.kt:411` (`collectAsState`, Profilkarte aus 0.11.2); behoben in 0.11.4 (Intern-Eintrag in PATCHNOTES), 0.11.4 gebaut, Inhaber bestätigt erfolgreichen Build.
- Betroffene Dateien: `docs/DECISIONS.md`, `docs/ARCHITECTURE.md`, `docs/TESTING.md`, `PATCHNOTES.md`
- Erwartetes Ergebnis: Doku passt zum Ist-Stand; Rückmeldung mit geänderten Stellen.
- Tatsächlich durchgeführte Arbeiten: –
- Ergebnis und Rückmeldung: –
- Abgeschlossen am: –

### SYNC-20261108-016 – Doku-Nachführung zu Version 0.11.1 und Prüfergebnissen -010/-011
- Erstellt am: 2026-11-08
- Absender: Development
- Empfänger: Documentation
- Priorität: LOW
- Status: COMPLETED
- Übergabegrund: Dokumentationsänderungen (DECISIONS, SYNC-ENGINE) liegen laut Inhaber ausschliesslich bei Documentation.
- Aufgabenbeschreibung: (1) DEC-072 (Vollständigkeitsbilanz): Status von Proposed auf Implemented, eingeschränkt: Anzeige „X gesichert · Y offen“ nur bei Fotos-Profilen, keine Liste mit Gründen, Ordner-Profile zeigen noch nichts; „gesichert“ zählt alle Einträge in `synced_files` (auch per „Speicher freigeben“ entfernte Dateien); „offen“ = lokaler MediaStore-Abgleich ohne Mindestalter. Anhang A Punkt 1 entsprechend nachführen. (2) DEC-196 (Konfliktkarten bei vorgemerkter Profillöschung): Umsetzung 0.11.1 nachführen. Nur Anzeige gefiltert, Konflikte bleiben gespeichert; Benachrichtigung „Konflikte“ zählt weiterhin alle offenen Konflikte; Risiko D44 unverändert. (3) Ergebnis SYNC-20261108-011: Teil-Prüfsumme „Wenig“ in `PartialHash.kt` und `LeoSyncImport.cs` identisch; DEC-021/-171 korrekt, kein Änderungsbedarf; Anhang B Punkte 4 und 12 als geprüft markieren. (4) Ergebnis SYNC-20261108-010: DEC-048 und `SYNC-ENGINE.md` stimmen mit dem Code überein; Anhang B Punkt 6 als geprüft markieren. (5) Nachtrag 0.11.2 zu DEC-072: Ordner-Profile zeigen „X Dateien synchronisiert“ (Zeilen in `mirror_state`, Stand letzter Abgleich) und „Y Konflikte offen“ (`mirror_conflicts`, resolution IS NULL); kein „offen“ im Sinn von nicht übertragenen Dateien, weil dafür ein PC-Listing nötig wäre; Inhaber hat das als sinnvolle Ergänzung bestätigt. Neue DAO-Abfragen `mirrorStateCountFlow`, `openConflictCountFlow` (kein Schema-Eintrag in DATA-MODEL nötig, nur Abfragen). Optionale Ergänzung in `SYNC-ENGINE.md`: Erreichbarkeitsprüfung vor dem Lauf (automatisch 5 Versuche über ca. 60 s: 0/5/10/15/30 s; manuell 2 Versuche: 0/3 s), Auslöser-Verzögerung `max(30 s, Mindestalter + 30 s)`, WorkManager-Backoff exponentiell ab 2 min.
- Technischer Kontext: Aufträge SYNC-20261108-002, -007, -010, -011 (Development, Details und Fundstellen im Code stehen in `inbox/development.md`). Version 0.11.1; `PATCHNOTES.md` und `version.properties` hat Development bereits gemäss Inhaberregel angepasst.
- Betroffene Dateien: `docs/DECISIONS.md` (DEC-072, DEC-196, Anhang A Punkt 1, Anhang B Punkte 4, 6, 12), optional `docs/SYNC-ENGINE.md` (Zeitregeln)
- Erwartetes Ergebnis: Doku passt zum Ist-Stand; Rückmeldung mit geänderten Stellen.
- Tatsächlich durchgeführte Arbeiten: `DECISIONS.md`: DEC-072 (Implemented, eingeschränkt, inkl. 0.11.2-Nachtrag), DEC-196 (Konsequenzen), Anhang A Punkte 1 und 26, Anhang B Punkte 4, 6, 12 als geprüft markiert. Angaben aus der Rückmeldung von Development übernommen, Code nicht selbst geprüft.
- Ergebnis und Rückmeldung: Nicht ausgeführt: optionale Ergänzung in `SYNC-ENGINE.md` (Erreichbarkeitsprüfung, Auslöser-Verzögerung, Backoff), da Werte nicht von Documentation am Code verifiziert; bei Bedarf Bestätigung durch Development (Fundstellen) anfordern. `PATCHNOTES.md` nicht geprüft/geändert.
- Abgeschlossen am: 2026-11-08

### SYNC-20261108-001 – Veraltete Verweise auf entfernte Unterstrich-Dateien bereinigen
- Erstellt am: 2026-11-08
- Absender: Architecture
- Empfänger: Documentation
- Priorität: LOW
- Status: COMPLETED
- Übergabegrund: Reine Doku-Konsistenz, keine Architekturentscheidung; liegt in der Rolle Documentation.
- Aufgabenbeschreibung: Der Inhaber hat `docs/SYNC_ENGINE.md` und `docs/DATA_MODEL.md` (Unterstrich) entfernt. Hinweise, die diese Dateien als vorhanden beschreiben oder auf sie verweisen, an den Ist-Stand anpassen bzw. streichen. Inhaltlich sonst nichts ändern.
- Technischer Kontext: Massgeblich sind nur noch `docs/SYNC-ENGINE.md` und `docs/DATA-MODEL.md` (Bindestrich). `docs/ARCHITECTURE.md` ist bereits bereinigt (kein Verweis mehr auf Unterstrich-Namen).
- Betroffene Dateien:
  - `docs/DATA-MODEL.md` Z. 7 (Kopfhinweis: behauptet, `SYNC-ENGINE.md` fehle und `ARCHITECTURE.md` verweise auf `DATA_MODEL.md` – beides falsch) und Z. 916 (Verweis-Tabelle)
  - `docs/TESTING.md` Z. 5 (Hinweis auf zusätzliche Unterstrich-Dateien)
  - `docs/SYNC-ENGINE.md` Z. 65, 75 (Abweichung A4), 1195, 1218 (Unterstrich-Datei als Quelle bzw. Arbeitsauftrag, Verweis auf `ARCHITECTURE.md` §13)
  - optional `docs/DECISIONS.md` Anhang C (ab Z. 3063): Unterstrich-Namen als historische Session-Einträge; Umstellen auf Bindestrich-Namen nur zur Vereinheitlichung, Inhalt nicht ändern
  - Zeilennummern Stand 2026-11-08; vor dem Ändern per Suche nach `SYNC_ENGINE`/`DATA_MODEL` neu ermitteln.
- Erwartetes Ergebnis: Keine Doku-Datei beschreibt die Unterstrich-Dateien mehr als vorhanden; Suche nach `SYNC_ENGINE`/`DATA_MODEL` in `docs/` liefert höchstens bewusst belassene historische Einträge (Entscheid zu DECISIONS Anhang C in der Rückmeldung nennen).
- Tatsächlich durchgeführte Arbeiten: Verweise/Hinweise auf Unterstrich-Dateien entfernt bzw. angepasst in `DATA-MODEL.md` (Kopfhinweis, Verweis-Tabelle), `TESTING.md` (Z. 5), `SYNC-ENGINE.md` (1.2, A4 als erledigt markiert, 18.1, 19). Zusätzlich in `DATA-MODEL.md` zwei veraltete Hinweise „SYNC-ENGINE.md noch nicht vorhanden“ (Z. ~518, ~917) korrigiert (gleiche Ursache).
- Ergebnis und Rückmeldung: `docs/` enthält ausser DECISIONS Anhang C keine `SYNC_ENGINE`/`DATA_MODEL`-Treffer mehr. DECISIONS Anhang C auf Wunsch des Inhabers nachträglich auf Bindestrich-Namen umgestellt (nur Dateinamen, Inhalt unverändert). A4 in SYNC-ENGINE blieb als Zeile erhalten (Nummerierung A5 ff. stabil). Keine Tests nötig; Quellcode nicht berührt, keine Version/PATCHNOTES-Änderung.
- Abgeschlossen am: 2026-11-08
