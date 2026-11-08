## Aufträge
(neueste oben)

Hinweis: Die Aufträge -002 bis -011 stammen aus der Durchsicht von `docs/DECISIONS.md` Anhang A/B (Antworten des Inhabers vom 2026-11-08). Umsetzung ist vom Inhaber freigegeben, ausser wo „prüfen“ steht. Jede Umsetzung: Doku gemäss `CLAUDE.md` §11 an Documentation melden.

### SYNC-20261108-019 – Mobile Daten/VPN: Wiederholungen und Erreichbarkeitstest begrenzen (Review -003, Befund 1)
- Erstellt am: 2026-11-08
- Absender: Development (Folgeauftrag aus Review zu -003, vom Inhaber bestätigt)
- Empfänger: Development
- Priorität: LOW
- Status: OPEN
- Übergabegrund: Optionale Nachbesserung aus dem Review von 0.11.5; eigenständiger Folgeauftrag.
- Aufgabenbeschreibung: Mit Schalter `syncOffWifi` und ohne erreichbaren PC (kein VPN) verursacht jeder automatische Lauf über Mobilfunk bis zu ca. 60 s Verbindungsversuche (`waits` in `SyncWorker`, ~Z. 177) und danach alle 30 Minuten eine Wiederholung (`enqueueShareRetry`). Ausserhalb des WLAN: Erreichbarkeitstest auf 1–2 Versuche kürzen und/oder Wiederholungsabstand vergrössern (Wert mit Inhaber klären). Verhalten im WLAN unverändert lassen.
- Technischer Kontext: Review 0.11.5 (MEDIUM 1), DEC-126, DEC-045, DEC-048.
- Betroffene Dateien: `sync/SyncWorker.kt` (`work`, `handleUnreachable`), `sync/SyncScheduler.kt`
- Erwartetes Ergebnis: Umsetzung, Doku-Hinweis (SYNC-ENGINE Zeitregeln).
- Tatsächlich durchgeführte Arbeiten: –
- Ergebnis und Rückmeldung: –
- Abgeschlossen am: –

### SYNC-20261108-013 – Nicht verwendete Texte aufräumen (Anhang A Punkt 25)
- Erstellt am: 2026-11-08
- Absender: Documentation
- Empfänger: Development
- Priorität: LOW
- Status: OPEN
- Übergabegrund: Entscheid des Inhabers: aufräumen.
- Aufgabenbeschreibung: Ungenutzte Strings in beiden Sprachdateien entfernen: `profile_subfolder`, `profile_subfolder_example_mirror`, `profile_subfolder_example_photos`, `profile_browse`, `settings_battery_off`, `profile_delete_cd`. Vor dem Löschen erneut auf Verwendung prüfen (Kotlin, XML, `TestTexts`, `StringsParityTest`); `profile_subfolders*` wird verwendet und bleibt. Danach `tools/make-translations.ps1`.
- Technischer Kontext: Per Suche in `app/src` nur Definitionen gefunden (Stand 2026-11-08, nicht gebaut).
- Betroffene Dateien: `res/values/strings_profile.xml`, `res/values/strings_settings.xml`, `res/values-de/` (gleiche Dateien)
- Erwartetes Ergebnis: Entfernte Texte, Testlauf `StringsParityTest`.
- Tatsächlich durchgeführte Arbeiten: –
- Ergebnis und Rückmeldung: –
- Abgeschlossen am: –

### SYNC-20261108-012 – Eingriff beim laufenden Sync vormerken (DEC-144)
- Erstellt am: 2026-11-08
- Absender: Documentation
- Empfänger: Development
- Priorität: MEDIUM
- Status: OPEN
- Übergabegrund: Entscheid des Inhabers (Anhang A Punkt 17): umsetzen.
- Aufgabenbeschreibung: Verlässt man die Ansicht, während Aufräumen, Endgültig löschen oder Speicher freigeben (Ordner) auf den laufenden Sync wartet, soll der Eingriff vorgemerkt und danach ausgeführt werden, statt zu unterbleiben. Datenverlust beachten: vorgemerkter Eingriff darf nicht mit veralteten Daten laufen (Regeln neu prüfen, geschützte Versionen, Abbruch/Neustart der App). Ähnlich vorhandener Vormerkung `PurgeStore`.
- Hinweis Development (2026-11-08): Wartet auf Entscheidung Architecture, siehe SYNC-20261108-014 (`inbox/architecture.md`).
- Technischer Kontext: DEC-144 (Alternative „vormerken“ wurde damals nicht gewählt), DEC-147, Risiko D29.
- Betroffene Dateien: `mirror/MirrorLock.kt`, `TrashCleaner`, `FolderFreer`, `ui/TrashScreen.kt`, `ui/FreeSpaceScreen.kt`
- Erwartetes Ergebnis: Umsetzung, Tests, Doku-Hinweis. Review und QA empfohlen.
- Tatsächlich durchgeführte Arbeiten: –
- Ergebnis und Rückmeldung: –
- Abgeschlossen am: –

### SYNC-20261108-011 – Prüfung: Teil-Prüfsumme in App und Import-Skript identisch
- Erstellt am: 2026-11-08
- Absender: Documentation
- Empfänger: Development
- Priorität: LOW
- Status: COMPLETED
- Übergabegrund: Fehlende Implementierungsinformation; Documentation darf nicht ungeprüft dokumentieren.
- Aufgabenbeschreibung: Prüfen, ob `PartialHash` (Stufe „Wenig“ / `regions`) und `tools/LeoSyncImport.cs` denselben Algorithmus verwenden, und ob DEC-021/DEC-024/DEC-171 den Ist-Stand korrekt beschreiben. Abweichungen melden, nichts ändern ohne Rückfrage.
- Technischer Kontext: DECISIONS Anhang B Punkte 4 und 12; Risiko D11 (doppelte Logik Kotlin/C#). Der Inhaber nutzt das Import-Skript nur selbst (A7 HEIC verworfen).
- Betroffene Dateien: `sync/PartialHash.kt`, `tools/LeoSyncImport.cs`, `docs/DECISIONS.md`
- Erwartetes Ergebnis: Kurze Aussage „identisch“ oder Liste der Abweichungen mit Fundstelle.
- Tatsächlich durchgeführte Arbeiten: Übernommen von Development. Quelltexte und DEC-021/-024/-171 gelesen und verglichen; nichts geändert, nichts gebaut.
- Ergebnis und Rückmeldung: **Identisch.** `PartialHash.regions/compute/hash` (PartialHash.kt:16–25, 84–91) und `LeoSyncImport.PartialHash` (LeoSyncImport.cs:162–178) stimmen überein: SHA-256 über Grösse (8 Byte Big Endian), bis 3 × 64 KB ganze Datei, sonst 64 KB bei 0, bei `size/2 − 32 KB` und bei `size − 64 KB`; Ausgabe Hex klein. DEC-021 (Text, 192 KB, 3 × 64 KB) und DEC-171 („Wenig“ = bisherige Prüfung, `levelRegions(LEVEL_LOW)` = `regions`) beschreiben den Ist-Stand korrekt. DEC-024 ist zu Recht „Superseded“. Das Skript kennt die Stufen Mittel/Viel/Komplett und `thorough` nicht; das ist unkritisch, solange es nur „Wenig“ vergleichen muss. Nicht geprüft: Randfall Dateigrösse 0 im C#-Skript (kein Lauf, nur Quelltext gelesen); Risiko D11 (doppelte Logik) bleibt, da kein automatischer Vergleichstest besteht. Kein Doku-Änderungsbedarf.
- Abgeschlossen am: 2026-11-08

### SYNC-20261108-010 – Prüfung: DEC-048 gegen Code (2 statt 30 Minuten)
- Erstellt am: 2026-11-08
- Absender: Documentation
- Empfänger: Development
- Priorität: LOW
- Status: COMPLETED
- Übergabegrund: Fehlende Implementierungsinformation.
- Aufgabenbeschreibung: Prüfen, welche Zeitregeln für „Datei zu frisch / Änderung während des Laufs“ tatsächlich gelten (insbesondere 2 Minuten bei Abbruch mitten im Lauf statt 30 Minuten) und ob DEC-048 und `SYNC-ENGINE.md` (Zeitregeln) damit übereinstimmen.
- Technischer Kontext: DECISIONS Anhang B Punkt 6.
- Betroffene Dateien: `docs/DECISIONS.md` (DEC-048), `docs/SYNC-ENGINE.md`, zugehöriger Code (`GlobalSettings.minFileAgeMinutes`, `SyncScheduler`, `MirrorEngine`)
- Erwartetes Ergebnis: Ist-Werte mit Fundstelle im Code.
- Tatsächlich durchgeführte Arbeiten: Übernommen von Development. Code und Doku gelesen und verglichen; nichts geändert, nichts gebaut.
- Ergebnis und Rückmeldung: **DEC-048 und SYNC-ENGINE.md stimmen mit dem Code überein.** Ist-Werte: (1) PC im Ziel-WLAN nicht erreichbar: 30 min, `SyncScheduler.SHARE_RETRY_MINUTES` (SyncScheduler.kt:57), gesetzt in `SyncWorker.handleUnreachable` (SyncWorker.kt:349). (2) Abbruch mitten im Lauf bei bestehendem WLAN: 2 min (`if (midRun) 2L`, SyncWorker.kt:349), gilt auch bei manuellem Start (Z. 339); die 2 min sind im Code fest eingetragen, keine Konstante. (3) WLAN verloren: kein Timer, sofort beim Wiederverbinden. (4) Mindestalter „zu frisch“: `minFileAgeMinutes`, Standard 5, 0–1440 (GlobalSettings.kt:27–29), Prüfung SyncEngine.kt:198–207 (nicht bei `dryRun`). (5) Während des Laufs neu entstandene Medien: Nachhol-Sync nach Mindestalter (SyncWorker.kt:307–313). (6) Auslöser-Verzögerung: `max(30 s, Mindestalter + 30 s)` (SyncScheduler.kt:128–129), maximal `max(5 min, +2 min)` (Z. 135). (7) Erreichbarkeitsprüfung vor dem Lauf: automatisch 5 Versuche über ca. 60 s (0/5/10/15/30 s), manuell 2 Versuche (0/3 s) (SyncWorker.kt:176). (8) WorkManager-Backoff exponentiell ab 2 min (SyncScheduler.kt:89). Doku: SYNC-ENGINE.md Z. 152, 156, 187, 224, 929, 953 und DEC-048 nennen 30/2 min und Mindestalter 5 korrekt. Lücke (kein Fehler): Punkte (6)–(8) stehen nicht oder nur teilweise in der Doku; Documentation kann das bei Bedarf ergänzen. Nicht geprüft: Verhalten auf einem Gerät, `MirrorEngine` (keine eigene Zeitregel gefunden, Suche nach `minFileAge` nur in Foto-`SyncEngine`, `PendingCheck`, `StaleCheck`, `SyncWorker`, `SyncScheduler`).
- Abgeschlossen am: 2026-11-08

### SYNC-20261108-009 – Verlauf mit Kennungen statt fertiger Texte (DEC-159)
- Erstellt am: 2026-11-08
- Absender: Documentation
- Empfänger: Development
- Priorität: LOW
- Status: OPEN
- Übergabegrund: Umsetzung vom Inhaber freigegeben (Anhang A Punkt 19).
- Aufgabenbeschreibung: Verlaufseinträge nicht als fertige Texte, sondern mit Kennungen speichern, sodass sie in der aktiven Sprache angezeigt werden. Löst auch das Risiko D35 (gemischtsprachiger Verlauf, `isIdle` über Merkmal-Wörter). Bei Schema- oder Sicherungsformat-Änderung: Migration, `MigrationTest`, Rückwärtskompatibilität der Sicherung prüfen; bei Bedarf vorher Rückfrage an Architecture.
- Hinweis Development (2026-11-08): Wartet auf Entscheidung Architecture, siehe SYNC-20261108-015 (`inbox/architecture.md`).
- Technischer Kontext: DEC-159, `RunSummary`, `isIdle`.
- Betroffene Dateien: `sync/RunSummary.kt`, `data/*`, `ui/LogScreen.kt`, `backup/*`
- Erwartetes Ergebnis: Umsetzung, Tests, Hinweis auf nötige Doku-Änderungen (DATA-MODEL, DECISIONS).
- Tatsächlich durchgeführte Arbeiten: –
- Ergebnis und Rückmeldung: –
- Abgeschlossen am: –

### SYNC-20261108-008 – „Problem melden“ umsetzen (DEC-121, DEC-123)
- Erstellt am: 2026-11-08
- Absender: Documentation
- Empfänger: Development
- Priorität: LOW
- Status: COMPLETED
- Übergabegrund: Umsetzung vom Inhaber freigegeben (Anhang A Punkt 18).
- Aufgabenbeschreibung: Funktion „Problem melden“ umsetzen. Kontakt: `leotools@protonmail.com`. Die Datenschutzerklärung ist bereits umgesetzt (`PRIVACY_GER.md` / `PRIVACY_EN.md`); sie ist nur zu verlinken bzw. auf Konsistenz mit der Funktion zu prüfen.
- Technischer Kontext: DEC-121, DEC-123; Memory-Roadmap (Version laut Versionsplan klären, nächste Minor-Reihe).
- Betroffene Dateien: `ui/SettingsScreen.kt` (voraussichtlich), `res/values*/strings_*.xml`, `PRIVACY_*.md`
- Erwartetes Ergebnis: Umsetzung, Tests, Doku-Hinweis.
- Tatsächlich durchgeführte Arbeiten: Einstellungen › Diagnose: neuer Knopf „Problem melden“ mit Hinweistext. `Diagnostics.reportProblem` erzeugt die Diagnose (`build`), legt sie als `LeoSync-Diagnose_<Zeit>.txt` im Cache-Ordner `share` ab (bestehender FileProvider) und öffnet per `ACTION_SENDTO mailto:` die Mail-App mit Adresse `Diagnostics.SUPPORT_EMAIL`, Betreff mit Version, Hinweistext und Diagnose als Anhang. Der Benutzer sendet selbst. Ohne Mail-App: Adresse in die Zwischenablage, Hinweis. Neue Strings `settings_report*`, `report_*` (EN/DE), `make-translations.ps1` ausgeführt. Der Knopf ist nicht ausgeblendet, da die Adresse fest hinterlegt ist. Version 0.11.5.
- Ergebnis und Rückmeldung: Nicht gebaut, keine Tests ausgeführt (Inhaberregel „nichts bauen“); kein UI-Test vorhanden. Datenschutz-Konsistenz geprüft: `PRIVACY_*.md` Abschnitte 6/7 (freiwilliger Versand per E-Mail, Inhalt, UUID) passen zur Funktion. Offen/Inhaber: In beiden Dateien stehen noch Platzhalter (Z. 14 Name, Z. 16 und Z. 230 E-Mail); Name kenne ich nicht, E-Mail wäre `leotools@protonmail.com` (nicht selbst geändert, Documentation). Verlinkung der Datenschutzerklärung in der App nicht umgesetzt: es gibt noch keine öffentliche URL (Play-Store-Vorbereitung 0.12.0). Der Anhang enthält die vollständige Diagnose (u. a. WLAN-Namen, Serveradressen, Dateinamen), der Hinweistext in der App nennt das. Restrisiken: Mail-Apps behandeln den Anhang unterschiedlich; nicht auf einem Gerät geprüft.
- Abgeschlossen am: 2026-11-08

### SYNC-20261108-007 – Konfliktkarten eines in Löschung befindlichen Profils ausblenden (DEC-196)
- Erstellt am: 2026-11-08
- Absender: Documentation
- Empfänger: Development
- Priorität: LOW
- Status: COMPLETED
- Übergabegrund: Entscheid des Inhabers: ausblenden (Anhang A Punkt 26).
- Aufgabenbeschreibung: Während der Rückgängig-Frist einer vorgemerkten Profillöschung keine Konfliktkarten dieses Profils anzeigen. Konflikte selbst nicht löschen (bei „Rückgängig“ müssen sie wieder erscheinen).
- Technischer Kontext: DEC-196, Risiko D44 (Vormerkung nur im Speicher).
- Betroffene Dateien: `ui/HomeScreen.kt`, `ui/ConflictScreen.kt`
- Erwartetes Ergebnis: Umsetzung, Test, Doku-Hinweis.
- Tatsächlich durchgeführte Arbeiten: In `HomeScreen` und `ConflictScreen` wird die Liste der offenen Konflikte nach `PendingProfileDelete.pending` gefiltert (reine Anzeige; `mirror_conflicts` unverändert). Der Sammelknopf „Alle: neuere Version“ wirkt nur auf die sichtbaren Konflikte. Version 0.11.1.
- Ergebnis und Rückmeldung: Nicht gebaut, kein Test ausgeführt (Inhaberregel „nichts bauen“); kein UI-Test vorhanden. Restrisiko: Die Benachrichtigung „Konflikte“ (`Notifications.conflictsNeeded`, Zähler `dao.openConflictCount()`) zählt weiter alle offenen Konflikte inklusive des vorgemerkten Profils. Bei Löschen bleibt die Vormerkung nur im Speicher (Risiko D44 unverändert). Doku-Hinweis: DECISIONS DEC-196 (Umsetzung 0.11.1).
- Abgeschlossen am: 2026-11-08

### SYNC-20261108-006 – „Ordner anlegen“ in der PC-Ordnerauswahl (DEC-192)
- Erstellt am: 2026-11-08
- Absender: Documentation
- Empfänger: Development
- Priorität: MEDIUM
- Status: COMPLETED
- Übergabegrund: Entscheid des Inhabers: umsetzen (Anhang A Punkt 24).
- Aufgabenbeschreibung: In `RemoteFolderPicker` neuen Ordner auf der Freigabe anlegen können. Fehlerfälle (fehlende Rechte, ungültige Namen, Netzabbruch) bedenken.
- Technischer Kontext: DEC-192; heute muss der Zielordner auf dem PC existieren.
- Betroffene Dateien: `ui/ProfileParts.kt` (`RemoteFolderPicker`), `smb/SmbSession.kt`, `res/values*/strings_*.xml`
- Erwartetes Ergebnis: Umsetzung, Tests, Doku-Hinweis (DECISIONS, ARCHITECTURE).
- Tatsächlich durchgeführte Arbeiten: In `RemoteFolderPicker` erste Listenzeile „➕ Neuer Ordner“ (nur unterhalb einer Freigabe; Freigaben selbst werden nicht angelegt). Dialog mit Namensfeld; Prüfung `invalidFolderName` (leer, > 255 Zeichen, `\ / : * ? " < > |`, Steuerzeichen, „.“/„..“, Punkt/Leerzeichen am Ende) und Dublette (ohne Gross-/Kleinschreibung) vor dem Anlegen. Anlegen über bestehendes `SmbSession.ensureDir` auf eigener Verbindung (keine Änderung an `SmbSession`); Erfolg nur, wenn `dirExists` danach wahr; Fehler (Rechte, Netz) via `userError` im Dialog, Eingabe bleibt erhalten. Nach Erfolg wechselt der Picker in den neuen Ordner. Neue Strings `remote_picker_new_folder*`, `remote_picker_create`, `remote_picker_name_*`, `remote_picker_create_failed` (EN/DE), `make-translations.ps1` ausgeführt. Version 0.11.3.
- Ergebnis und Rückmeldung: Nicht gebaut, keine Tests ausgeführt (Inhaberregel „nichts bauen“); kein UI-Test vorhanden, `invalidFolderName` ohne Unit-Test. Restrisiken: Windows-reservierte Namen (CON, NUL …) werden nicht lokal abgefangen, der Server-Fehler wird angezeigt; Syntax von Hand geprüft. Bei Abbruch mitten im Anlegen bleibt höchstens ein leerer Ordner, kein Datenverlust. Doku-Hinweis: DECISIONS DEC-192 (Umsetzung 0.11.3), ARCHITECTURE (Picker), PATCHNOTES 0.11.3 (nicht von mir ergänzt, Documentation).
- Abgeschlossen am: 2026-11-08

### SYNC-20261108-005 – Wiederherstellung: höchstens 5 Versuche, danach Meldung (DEC-146)
- Erstellt am: 2026-11-08
- Absender: Documentation
- Empfänger: Development
- Priorität: MEDIUM
- Status: OPEN
- Übergabegrund: Entscheid des Inhabers (Anhang A Punkt 16): 5 Versuche, danach Meldung an den Benutzer.
- Aufgabenbeschreibung: Fehlgeschlagene Wiederherstellung nach 5 Versuchen nicht mehr automatisch wiederholen und dem Benutzer melden. Vormerkung und Papierkorb-Version dürfen dabei nicht verloren gehen (Datenverlust-Priorität); Verhalten von „Erneut versuchen“/„Verwerfen“ festlegen und dokumentieren. Zähler ausserhalb der Sicherung (Prefs „restore“) bedenken.
- Technischer Kontext: DEC-146, Risiko D27.
- Betroffene Dateien: `MirrorEngine.runRestores/rollbackRestore`, `RestoreFailures`, `RestoreManager`, `ui/HomeScreen.kt`
- Erwartetes Ergebnis: Umsetzung, Tests (`TrashIndexTest` o. ä.), Hinweis für SYNC-ENGINE/DECISIONS. Wegen Datenverlustrisiko Review/QA empfohlen.
- Tatsächlich durchgeführte Arbeiten: –
- Ergebnis und Rückmeldung: –
- Abgeschlossen am: –

### SYNC-20261108-004 – „Speicher freigeben“: leere Ordner aufräumen (DEC-147)
- Erstellt am: 2026-11-08
- Absender: Documentation
- Empfänger: Development
- Priorität: MEDIUM
- Status: OPEN
- Übergabegrund: Entscheid des Inhabers (Anhang A Punkt 15): leere Ordner aufräumen, ausser `DCIM\Camera`.
- Aufgabenbeschreibung: Nach „Speicher freigeben“ (Ordner-Profile) leer gewordene Ordner auf dem Handy entfernen; `DCIM\Camera` bleibt immer bestehen. Der Inhaber schrieb „DICM\Camera“, gemeint ist vermutlich `DCIM/Camera` (bitte bestätigen). Nur Ordner, die tatsächlich leer sind (kein Löschen von Inhalt), auch nicht Profil-Wurzelordner ohne Rückfrage.
- Hinweis Development (2026-11-08): Inhaber bestätigt: „DICM“ war ein Schreibfehler; gemeint ist der tatsächliche Ordnername `DCIM/Camera`.
- Technischer Kontext: DEC-147, Risiko D26.
- Betroffene Dateien: `mirror/FolderFreer.kt`, `ui/FreeSpaceScreen.kt`
- Erwartetes Ergebnis: Umsetzung, Tests, Doku-Hinweis. Wegen Löschlogik Review/QA empfohlen.
- Tatsächlich durchgeführte Arbeiten: –
- Ergebnis und Rückmeldung: –
- Abgeschlossen am: –

### SYNC-20261108-003 – Schalter: automatische Synchronisierung auch ausserhalb des WLAN (DEC-126)
- Erstellt am: 2026-11-08
- Absender: Documentation
- Empfänger: Development
- Priorität: MEDIUM
- Status: COMPLETED
- Übergabegrund: Entscheid des Inhabers (Anhang A Punkt 14): umsetzen.
- Aufgabenbeschreibung: Schalter, mit dem die automatische Synchronisierung auch über mobile Daten bzw. VPN läuft; mit Hinweis auf Datenverbrauch (siehe DEC-126). Zusammenspiel mit WLAN-Name/SSID-Prüfung, Standort-Berechtigung und PC-Erreichbarkeit klären.
- Technischer Kontext: DEC-126, DEC-044; PC-Erkennung (DEC-109) bleibt vorerst offen.
- Betroffene Dateien: `sync/WifiHelper.kt`, `sync/SyncWorker.kt`, `ui/SettingsScreen.kt` bzw. `ProfileScreen.kt`, `res/values*/strings_*.xml`
- Erwartetes Ergebnis: Umsetzung, Tests, Doku-Hinweis (DECISIONS, ARCHITECTURE).
- Tatsächlich durchgeführte Arbeiten: Neue Einstellung `GlobalSettings.syncOffWifi` (Standard aus, in der Sicherung enthalten, in der Diagnose). Schalter in Einstellungen › Übertragung mit Datenverbrauchs-Hinweis. `WifiHelper`: `isOnMobileOrVpn` (aktives Netz mit Internet, ohne WLAN-Transport), `onAllowedNetwork` (WLAN oder, mit Schalter, mobile Daten/VPN), `offWifiSyncActive`. `SyncScheduler.autoConstraints`: Netzwerkbedingung `CONNECTED` statt `UNMETERED`, wenn der Schalter an ist; beim Umschalten wird das Sicherheitsnetz neu geplant (Ordner-Prüfung wird über `setupAll` aktualisiert). `SyncWorker`: ausserhalb des WLAN (Schalter an) sind alle aktiven Profile berechtigt (keine SSID-Prüfung); die drei Stellen „im WLAN geblieben?“ (Erreichbarkeits-Wiederholung, `shouldContinue` während der Übertragung, `handleUnreachable`) nutzen `onAllowedNetwork`. Regeln bleiben: im WLAN (auch VPN über WLAN) gilt weiter die SSID-Regel inkl. Standort-Berechtigung; PC-Erreichbarkeit (TCP 445) wird wie bisher vor dem Lauf geprüft, ohne Erreichbarkeit (z. B. VPN aus) gilt „PC nicht erreichbar“ (30 min). Auslöser ausserhalb des WLAN: neue Medien, Sicherheitsnetz (6 h), Ordner-Prüfung; kein eigener Auslöser „mobile Daten verbunden“. Version 0.11.5.
- Ergebnis und Rückmeldung: Nicht gebaut, keine Tests ausgeführt (Inhaberregel „nichts bauen“); keine Unit-Tests ergänzt (Netzwerklogik braucht Android-Umgebung). Restrisiken: (1) Mit Schalter an kann der Hintergrund-Sync mobile Daten verbrauchen (Hinweis in der App); keine Datenbegrenzung. (2) Eine unterbrochene Übertragung ausserhalb des WLAN wird über den Fortsetzen-Auftrag (nur Bedingung `CONNECTED`) fortgesetzt; `WifiNetworkReceiver` und `PauseService` bleiben WLAN-gebunden. (3) „Pausiert“-Dienst wartet weiterhin auf ein Ziel-WLAN. (4) Netz-Eigenschaften von VPN-Typen sind je nach Gerät verschieden, nicht auf einem Gerät geprüft. Doku-Hinweis: DECISIONS DEC-126 (Accepted → Implemented 0.11.5), Anhang A Punkt 14, ARCHITECTURE/SYNC-ENGINE (Netzwerkregeln), DATA-MODEL (neue Einstellung `syncOffWifi`, Sicherung). Review und QA empfohlen (Netzbedingung, Wiederanlauf).
- Abgeschlossen am: 2026-11-08

### SYNC-20261108-002 – Anzeige gesicherter und offener Dateien unter dem Profil (DEC-072)
- Erstellt am: 2026-11-08
- Absender: Documentation
- Empfänger: Development
- Priorität: LOW
- Status: COMPLETED
- Übergabegrund: Entscheid des Inhabers (Anhang A Punkt 1): umsetzen.
- Aufgabenbeschreibung: Unterhalb des Profils die Anzahl gesicherter und noch offener Dateien anzeigen (Umfang laut Inhaber; die in DEC-072 genannte Liste mit Gründen war nicht Teil der Freigabe, bei Bedarf rückfragen).
- Technischer Kontext: DEC-072 (Proposed), DEC-021.
- Betroffene Dateien: `ui/HomeScreen.kt`, `ui/ProfileCards.kt`, `data/SyncDao.kt`
- Erwartetes Ergebnis: Umsetzung, Tests, Doku-Hinweis.
- Tatsächlich durchgeführte Arbeiten: `ProfileCard` (tatsächlich in `ui/HomeParts.kt`) zeigt bei Fotos-Profilen „X gesichert · Y offen“: X = `SyncDao.syncedCount` (Flow), Y = `PendingCheck.readyFiles(ctx, p, 0)` (lokaler MediaStore-Abgleich, Mindestalter 0, also inkl. zu junger Dateien), 800 ms entprellt, bei Fehler (z. B. Medienzugriff fehlt) ausgeblendet. Neuer String `home_profile_counts` (EN/DE), `tools/make-translations.ps1` ausgeführt. Version 0.11.1, PATCHNOTES ergänzt. Keine DAO- oder Schema-Änderung. **Nachtrag 0.11.2 (Inhaber-Freigabe):** Ordner-Profile zeigen „X Dateien synchronisiert“ (`SyncDao.mirrorStateCountFlow`) und, falls > 0, „Y Konflikte offen“ (`openConflictCountFlow`); Plurals `home_profile_mirror_synced`, `home_profile_open_conflicts`. Zwei neue reine Abfragen, kein Schema-Eingriff. Doku-Nachtrag in SYNC-20261108-016.
- Ergebnis und Rückmeldung: Nicht gebaut, keine Tests ausgeführt (Inhaberregel „nichts bauen“). Nicht kompiliert: Syntax von Hand geprüft, Build durch den Inhaber nötig. Bewusst begrenzt: Ordner-Profile zeigen nichts, da „offen“ dort ein PC-Listing bräuchte (offene Frage an den Inhaber). „Gesichert“ zählt alle Einträge in `synced_files`, auch Dateien, die per „Speicher freigeben“ vom Handy entfernt wurden. Keine Liste mit Gründen (nicht freigegeben). Doku-Hinweis: DEC-072 von Proposed auf Implemented (eingeschränkt), DECISIONS Anhang A Punkt 1.
- Abgeschlossen am: 2026-11-08
