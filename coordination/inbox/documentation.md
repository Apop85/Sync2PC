## Aufträge
(neueste oben)

### SYNC-20261108-027 – QA-Ergebnisse 0.11.7 in TESTING.md und bekannte Verhaltenshinweise (aus QA -024)
- Erstellt am: 2026-11-08
- Absender: QA
- Empfänger: Documentation
- Priorität: LOW
- Status: COMPLETED
- Übergabegrund: Ergänzt -023 um tatsächlich ausgeführte Testzahlen (dort „nicht von mir“) und dokumentationswürdige Qualitätsrisiken.
- Aufgabenbeschreibung:
  1. `TESTING.md`: Stand QA-Lauf 2026-11-08 nach 0.11.7 (`testDebugUnitTest --rerun-tasks`, JDK 17): **20 Testklassen, 110 Tests, 0 Fehler, 0 übersprungen**; `NetworkRulesTest` 5 Tests, `BackupCodecTest` 9 Tests (inkl. `offWifiSwitchRoundTripsAsBoolean`, `backupWithoutOffWifiKeyLeavesItAbsent`). Die Gesamtzahl (-020: 109) wird nicht erklärt; Zahl nur als gemessen angeben. Testlücken: Orchestrierung `SyncWorker` (Wartestufen, `handleUnreachable` mit `midRun`/Schalter), `SyncScheduler.onOffWifiChanged`, Aufhebung der Wartezeit in `WifiNetworkReceiver`, `reportProblem`. Manuelle Gerätetests (Netzwechsel, Mail-App, Sicherung) bleiben beim Inhaber (nicht ausgeführt).
  2. Bekannte Verhaltenshinweise (aus QA -024, im Code belegt; Wortlaut/Ort nach eigenem Ermessen, z. B. SYNC-ENGINE.md/DEC-126): (a) Während der 2-h-Wartezeit kehren automatische Läufe (Foto-Auslöser, Sicherheitsnetz) still zurück (`SyncWorker` Zeile 171); neue Fotos warten bis zu 2 h, sofern keine WLAN-Verbindung, kein manueller Start und kein Schalterwechsel die Wartezeit aufhebt. (b) „Mobil/VPN“ = jedes aktive Netz mit Internet ausser WLAN (auch Ethernet/Tethering/Bluetooth), `WifiHelper.isOnMobileOrVpn`. (c) Geplante Fortsetzen-/Nachhol-Aufträge behalten nach Schalterwechsel die alte Netzbedingung: **Entscheid Development steht aus (-025)**, erst nach dessen Antwort dokumentieren.
- **Nachtrag Development 2026-11-08 (Antwort auf Punkt 2c und Meldung zu -026):**
  - **-025 / Punkt 2c, Entscheid Inhaber: so belassen.** Geplante `WORK_RESUME`/`WORK_FOLLOWUP` behalten nach einem Schalterwechsel ihre alte Netzbedingung (`SyncScheduler.autoConstraints`); `onOffWifiChanged` plant nur das Sicherheitsnetz neu und verwirft die Wartezeit. AUS→EIN: Die Aufträge warten weiter auf WLAN (`UNMETERED`), bis der nächste Auslöser kommt (Verzögerung, vom Inhaber akzeptiert, kein Datenverlust). EIN→AUS: Aufträge laufen an, der Worker beendet sich (harmlos). `enqueueResume`/`enqueueFollowUp` bleiben unverändert (bewusst, Fortsetzen-Pfad nicht angefasst). Bitte in DEC-126 den Vermerk „Entscheid steht aus“ durch diesen Stand ersetzen, dann -027 abschliessen.
  - **-026 (Code, Version bleibt 0.11.7):** `Diagnostics.kt` Zeile 288: Der Chooser-Titel beim Teilen des Verlaufs kam fest als „Verlauf teilen“ aus dem Code und erschien bei englischer App-Sprache deutsch; jetzt `str(R.string.log_share)` (EN „Share history“, DE „Verlauf teilen“, Ressource bestand schon, keine Textänderung). Bitte im PATCHNOTES-Eintrag 0.11.7 ergänzen. Nicht gebaut/getestet (Inhaberregel).
- Technischer Kontext: QA -024 (`inbox/qa-testing.md`), Development -025, -026.
- Betroffene Dateien: `docs/TESTING.md`, ggf. `docs/SYNC-ENGINE.md`, `docs/DECISIONS.md` (DEC-126)
- Erwartetes Ergebnis: Doku passend zu den gemessenen Ergebnissen, ohne Aussagen zu erfinden.
- Tatsächlich durchgeführte Arbeiten: Übernahme bestätigt. `TESTING.md`: 20 Klassen/110 Tests (Abschnitte 1, 2, 13), `BackupCodecTest` 9, neue Zeile `NetworkRulesTest` 5, Testlücken und nicht ausgeführte Gerätetests (Punkt 7), Hinweis „Differenz zu 109 nicht erklärt“. `ARCHITECTURE.md` 4.7 Zahlen. `DECISIONS.md` DEC-126: bekannte Hinweise (a) Wartezeit blockiert Auslöser, (b) Definition „Mobil/VPN“. Hinweis (a): Zeilennummer nicht übernommen, Code `SyncWorker` Prüfung `shareRetryAt` gelesen und bestätigt; (b) aus -027 übernommen, `WifiHelper.isOnMobileOrVpn` nicht selbst gelesen.
- Ergebnis und Rückmeldung: (c) nach Entscheid Inhaber/Development -025 („so belassen“) in DEC-126 eingetragen. Zusätzlich aus Development -026: `PATCHNOTES.md` 0.11.7 um „Verlauf teilen“ (Chooser-Titel in App-Sprache) ergänzt. Nicht geprüft: Quelltext zu (c) und -026 (Angaben aus den Rückmeldungen).
- Abgeschlossen am: 2026-11-08

### SYNC-20261108-023 – Doku-Nachführung zu Version 0.11.7 (Netzwerkregel ausserhalb WLAN; Antwort auf -022 Punkt 1)
- Erstellt am: 2026-11-08
- Absender: Development
- Empfänger: Documentation
- Priorität: MEDIUM
- Status: COMPLETED
- Übergabegrund: Neue Entscheidung und geändertes Sync-Verhalten; Antwort für -022 Punkt 1.
- Aufgabenbeschreibung: Umsetzung von Development -019/-021 (Entscheid Inhaber 2026-11-08). (1) **DEC-126/DEC-048 präzisieren:** Mobile Daten/VPN gelten bei Schalter EIN **nicht** als Unterbrechung bis WLAN (Fortsetzen-Aufträge nutzen `CONNECTED`). Ausserhalb des WLAN gilt bei „PC nicht erreichbar“ und bei Verbindungsabbruch mitten im Lauf eine Wartezeit von **2 Stunden** (`NetworkRules.OFF_WIFI_RETRY_MINUTES`); im WLAN bleiben 30 min bzw. 2 min. Erreichbarkeitstest ausserhalb WLAN nur 2 Versuche (0/5 s). Die Wartezeit endet durch neue WLAN-Verbindung, manuellen Start oder einen erfolgreichen Lauf (auch von Hand); neue Auslöser (Fotos, Sicherheitsnetz, Ordner-Prüfung) ignorieren sie. Der Satz „Fortsetzen-Dienst und ‚Pausiert‘ bleiben WLAN-gebunden“ stimmt nur für `PauseService`/`WifiNetworkReceiver`, nicht für die Aufträge. Falls gewünscht eigene DEC (Folge von DEC-126). (2) **SYNC-ENGINE.md** Zeitregeln ergänzen. (3) **ARCHITECTURE.md:** neue Datei `sync/NetworkRules.kt` (reine Netzentscheidungen, von `WifiHelper`/`SyncWorker` genutzt), `SyncScheduler.onOffWifiChanged`. (4) **TESTING.md:** neue `NetworkRulesTest`, `BackupCodecTest` +2 Tests (Stand nach QA-Lauf noch nicht ausgeführt; Zahlen nicht von mir). (5) **Weitere Änderungen 0.11.7:** „Problem melden“ prüft vorab auf Mail-App (`queryIntentActivities`, `<queries>` im Manifest); Sicherung einspielen plant bei Wechsel von `syncOffWifi` neu und verwirft die Wartezeit (wie der Schalter). (6) **PATCHNOTES.md** 0.11.7. Danach -022 abschliessen.
- Technischer Kontext: Development -019, -021; QA -020; DEC-126, DEC-048.
- Betroffene Dateien: `docs/DECISIONS.md`, `docs/SYNC-ENGINE.md`, `docs/ARCHITECTURE.md`, `docs/TESTING.md`, `PATCHNOTES.md`
- Erwartetes Ergebnis: Doku passend zum Code (Quelle: `sync/NetworkRules.kt`, `SyncWorker.handleUnreachable`).
- Tatsächlich durchgeführte Arbeiten: Übernahme bestätigt. Gegen `NetworkRules.kt` und `SyncWorker` (Erreichbarkeitsversuche, `handleUnreachable`) geprüft. `DECISIONS.md`: DEC-126 (Präzisierung 0.11.7, Fortsetzen-Aufträge `CONNECTED`, Status/Komponenten), DEC-048 (Verweis auf 2 h). `SYNC-ENGINE.md`: Schritt 6, Schritt 9, Auslöser-Tabelle, Fehlertabelle (Zeitregeln). `ARCHITECTURE.md`: neue Zeile `NetworkRules.kt`, `WifiHelper`, `onOffWifiChanged`. `TESTING.md`: siehe -027. `PATCHNOTES.md`: 0.11.7. Keine eigene DEC angelegt (Präzisierung in DEC-126).
- Ergebnis und Rückmeldung: Nicht selbst geprüft: Mail-App-Prüfung (`queryIntentActivities` in `Diagnostics` Z. 319 gesehen, `<queries>` im Manifest nicht), `BackupManager`-Verwerfen der Wartezeit (nur Aufruf `onOffWifiChanged` gesehen), Wirkung der Wartezeit-Aufhebung in `WifiNetworkReceiver`. -022 Punkt 1 damit beantwortet.
- Abgeschlossen am: 2026-11-08

### SYNC-20261108-022 – Doku-Abgleich nach QA-Prüfung 0.11.5/0.11.6 (aus QA -020)
- Erstellt am: 2026-11-08
- Absender: QA
- Empfänger: Documentation
- Priorität: LOW
- Status: COMPLETED
- Übergabegrund: Unstimmigkeit Implementierung ↔ Doku und aktualisierte Teststrategie-Angaben.
- Aufgabenbeschreibung:
  1. `DECISIONS.md` DEC-126 (Konsequenzen): „Fortsetzen-Dienst und ‚Pausiert‘ bleiben WLAN-gebunden“ stimmt nicht mit dem Code überein. `SyncScheduler.autoConstraints` (auch für `enqueueResume`/`enqueueShareRetry`) nutzt bei Schalter EIN `NetworkType.CONNECTED`; `SyncWorker.shouldContinue` erkennt einen Wechsel auf Mobilfunk nicht als WLAN-Verlust. Aussage nach Klärung durch Development (QA -021 Punkt 1) präzisieren; nichts vorab festlegen. Ebenso „nicht gebaut/getestet“: Build 0.11.6 ist laut Inhaber erfolgreich.
  2. `TESTING.md`: Stand per QA-Lauf 2026-11-08 (JDK 17 aus Android Studio, `testDebugUnitTest`, alle Klassen): **19 Testklassen, 109 Tests, 0 Fehler, 0 übersprungen** (Abschnitt 1, 3, 13: bisher „18 Klassen, ca. 100“). `RemoteFolderNameTest` (4 Tests) wurde ausgeführt und ist bestanden: Hinweis „nicht ausgeführt“ in 4.2 entfällt. Hinweis ergänzen: Die Shell-Standard-`JAVA_HOME` zeigt auf Java 8, Gradle braucht JDK 17+ (z. B. `C:\Program Files\Android\Android Studio\jbr`). Lücken ergänzen: keine Tests für `WifiHelper`-Netzentscheid, `syncOffWifi`-Round-Trip, „Problem melden“.
- Technischer Kontext: QA -020, Folgeauftrag -021 an Development.
- Betroffene Dateien: `docs/DECISIONS.md` (DEC-126), `docs/TESTING.md`
- Erwartetes Ergebnis: Doku angepasst, ohne Aussagen zu erfinden.
- Tatsächlich durchgeführte Arbeiten: Punkt 2 vollständig: `TESTING.md` (Abschnitte 1, 2, 3, 4.2, Lücken, 13: 19 Klassen/109 Tests, JDK-17-Hinweis, `RemoteFolderNameTest` bestanden, neue Testlücken), `ARCHITECTURE.md` 4.7 (Testanzahl). Punkt 1 teilweise: in DEC-126 „nicht gebaut/getestet“ korrigiert; die Aussage „Fortsetzen-Dienst und ‚Pausiert‘ bleiben WLAN-gebunden“ nur als „von QA beanstandet, Klärung ausstehend“ markiert, nichts festgelegt.
- Ergebnis und Rückmeldung: Offen: Präzisierung von DEC-126 (und ggf. SYNC-ENGINE/ARCHITECTURE) nach Antwort von Development zu -021 Punkt 1. Danach Status auf COMPLETED. Die Zahl „19 Testklassen“ übernommen aus dem QA-Bericht; ob `TestTexts` darin mitgezählt ist, nicht geprüft.
  **Nachtrag 2026-11-08:** Punkt 1 durch Development -023 beantwortet und in DEC-126 präzisiert (Fortsetzen-Aufträge nutzen `CONNECTED`; Zahlen inzwischen 20 Klassen/110 Tests, siehe -027).
- Abgeschlossen am: 2026-11-08

### SYNC-20261108-018 – Doku-Nachführung zu Version 0.11.5 (Schalter mobile Daten/VPN, „Problem melden“)
- Erstellt am: 2026-11-08
- Absender: Development
- Empfänger: Documentation
- Priorität: LOW
- Status: COMPLETED
- Übergabegrund: Dokumentationsänderungen liegen ausschliesslich bei Documentation.
- Aufgabenbeschreibung: (1) DEC-126 von Accepted auf Implemented (0.11.5) und Anhang A Punkt 14; Verhalten: Schalter `syncOffWifi` (Standard aus, Einstellungen › Übertragung, in Sicherung und Diagnose); ausserhalb des WLAN keine SSID-Prüfung, alle aktiven Profile berechtigt; im WLAN (auch VPN über WLAN) bleibt die SSID-Regel (DEC-044); Netzwerkbedingung der automatischen Aufträge `CONNECTED` statt `UNMETERED`; PC-Erreichbarkeit wie bisher; Auslöser ausserhalb des WLAN: neue Medien, Sicherheitsnetz (6 h), Ordner-Prüfung (kein WLAN-Broadcast, kein Auslöser „mobile Daten verbunden“); Fortsetzen-Dienst/„Pausiert“ bleibt WLAN-gebunden; Datenverbrauch-Hinweis in der App. Weiter ARCHITECTURE/SYNC-ENGINE (Netzwerkregeln) und DATA-MODEL (Einstellung `syncOffWifi`, Sicherungsfeld, abwärtskompatibel). (2) DEC-121/DEC-123 auf Implemented (0.11.5) und Anhang A Punkt 18: Knopf „Problem melden“ in Einstellungen › Diagnose; `Diagnostics.reportProblem` öffnet per `ACTION_SENDTO mailto:` die Mail-App (`leotools@protonmail.com`, Betreff mit Version, Hinweistext, Diagnose als Anhang `LeoSync-Diagnose_<Zeit>.txt`), Benutzer sendet selbst; ohne Mail-App Adresse in Zwischenablage. Knopf nicht ausgeblendet (Adresse fest hinterlegt; DEC-121 nannte Ausblenden ohne Adresse). `PRIVACY_*.md` Abschnitte 6/7 passen zur Funktion; Platzhalter (Name Z. 14, E-Mail Z. 16 und 230) stehen noch, E-Mail wäre `leotools@protonmail.com`, Name Sache des Inhabers. (3) `PATCHNOTES.md` 0.11.5 (Neu: Schalter mobile Daten/VPN, Problem melden; Intern: `WifiHelper.onAllowedNetwork`, `SyncScheduler.autoConstraints`, 8 Strings EN/DE) und 0.11.4 (Intern: Typfehler `collectAsState` in `HomeParts.kt` behoben).
- Technischer Kontext: Aufträge SYNC-20261108-003 und -008 (COMPLETED, Details in `inbox/development.md`). Nicht gebaut/getestet. Nachtrag 0.11.6 nach Review: Beim Umschalten des Schalters wird zusätzlich die Wartezeit „PC nicht erreichbar“ verworfen (`cancelShareRetry`); Hinweistext nennt jetzt mögliche Kosten beim Mobilfunkabo (z. B. Ausland). PATCHNOTES 0.11.6 entsprechend. Offener Folgeauftrag SYNC-20261108-019 (Wiederholungen über Mobilfunk begrenzen).
- Betroffene Dateien: `docs/DECISIONS.md`, `docs/ARCHITECTURE.md`, `docs/SYNC-ENGINE.md`, `docs/DATA-MODEL.md`, `PATCHNOTES.md`
- Erwartetes Ergebnis: Doku passt zum Ist-Stand; Rückmeldung mit geänderten Stellen.
- Tatsächlich durchgeführte Arbeiten: `DECISIONS.md`: DEC-126 (Implemented, Verhalten), DEC-121 und DEC-123 (Implemented), Anhang A Punkte 14 und 18. `ARCHITECTURE.md`: WorkManager-Netzwerkbedingung (`autoConstraints`, `CONNECTED`), Sequenzdiagramm. `SYNC-ENGINE.md`: Schritt 5 (SSID-Prüfung) und Auslöser-Tabelle. `DATA-MODEL.md`: Schlüssel `syncOffWifi` in `global`, Sicherung. `PATCHNOTES.md`: Einträge 0.11.3 bis 0.11.6 ergänzt (0.11.3/0.11.4 aus -017, 0.11.5/0.11.6 aus diesem Auftrag). Alle Angaben aus Developments Beschreibung, Code nicht selbst geprüft.
- Ergebnis und Rückmeldung: Offen/Hinweise: (1) Anzahl der exportierten Schlüssel in DATA-MODEL (14 + `syncOffWifi`) nicht nachgezählt; bei Bedarf Development fragen. (2) `PRIVACY_*.md`-Platzhalter (Name Z. 14, E-Mail Z. 16 und 230) sind Sache des Inhabers bzw. Development; E-Mail wäre `leotools@protonmail.com`; in DEC-123 als offen vermerkt. (3) Folgeauftrag SYNC-20261108-019 (Wiederholungen über Mobilfunk begrenzen) bei Abschluss nachführen. (4) Die in DATA-MODEL/TESTING genannten Testanzahlen sind nicht aktualisiert.
- Abgeschlossen am: 2026-11-08

### SYNC-20261108-017 – Doku-Nachführung zu Version 0.11.3 («Ordner anlegen», DEC-192)
- Erstellt am: 2026-11-08
- Absender: Development
- Empfänger: Documentation
- Priorität: LOW
- Status: COMPLETED
- Übergabegrund: Dokumentationsänderungen (DECISIONS, ARCHITECTURE, TESTING, PATCHNOTES) liegen ausschliesslich bei Documentation.
- Aufgabenbeschreibung: (1) `DECISIONS.md`: DEC-192 (Ordner anlegen in der PC-Ordnerauswahl) als umgesetzt in 0.11.3 nachführen; Anhang A Punkt 24 entsprechend. Verhalten: Zeile «Neuer Ordner» nur unterhalb einer Freigabe (keine Freigaben anlegen); Namensprüfung lokal (leer, über 255 Zeichen, `\ / : * ? " < > |`, Steuerzeichen, «.»/«..», Punkt/Leerzeichen am Ende; Dublette ohne Gross-/Kleinschreibung); Anlegen über `SmbSession.ensureDir` auf eigener Verbindung, Erfolg nur wenn der Ordner danach existiert; Fehler (Rechte, Netz) im Dialog, Eingabe bleibt; nach Erfolg wechselt die Auswahl in den neuen Ordner. Restrisiko: Windows-reservierte Namen (CON, NUL …) nicht lokal abgefangen, Serverfehler wird angezeigt. (2) `ARCHITECTURE.md`: Beschreibung von `RemoteFolderPicker` um «Ordner anlegen» ergänzen. (3) `TESTING.md`: neue Testklasse `RemoteFolderNameTest` (Namensprüfung `invalidFolderName`); nicht ausgeführt, nur erstellt (Inhaberregel «nichts bauen»). (4) `PATCHNOTES.md`: Eintrag 0.11.3 (Neu: Ordner auf dem PC direkt in der Ordnerauswahl anlegen; Intern: `invalidFolderName`, 6 neue Strings EN/DE, `RemoteFolderNameTest`).
- Technischer Kontext: Auftrag SYNC-20261108-006 (COMPLETED, Details in `inbox/development.md`). Keine Schema-, DAO- oder `SmbSession`-Änderung. `version.properties` steht inzwischen auf 0.11.4. Nachtrag: 0.11.3 scheiterte im Build an einem Typfehler in `ui/HomeParts.kt:411` (`collectAsState`, Profilkarte aus 0.11.2); behoben in 0.11.4 (Intern-Eintrag in PATCHNOTES), 0.11.4 gebaut, Inhaber bestätigt erfolgreichen Build.
- Betroffene Dateien: `docs/DECISIONS.md`, `docs/ARCHITECTURE.md`, `docs/TESTING.md`, `PATCHNOTES.md`
- Erwartetes Ergebnis: Doku passt zum Ist-Stand; Rückmeldung mit geänderten Stellen.
- Tatsächlich durchgeführte Arbeiten: `DECISIONS.md`: DEC-192 (Nachtrag 0.11.3, Konsequenz angepasst), Anhang A Punkt 24 erledigt. `ARCHITECTURE.md`: `RemoteFolderPicker` und Abschnitt 14 (Eintrag „nicht entschieden“ entfernt). `TESTING.md`: Zeile `RemoteFolderNameTest`. `PATCHNOTES.md`: 0.11.3 und 0.11.4.
- Ergebnis und Rückmeldung: Testanzahl von `RemoteFolderNameTest` unbekannt (Spalte „–“); Test nicht ausgeführt, nur erstellt. Angaben aus Developments Beschreibung, Code nicht selbst geprüft.
- Abgeschlossen am: 2026-11-08

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
