## Aufträge
(neueste oben)

### SYNC-20261109-049 – DECISIONS.md: neue Entscheidung PC-Erkennung per PC-Kennung (aus Architecture -048)
- Erstellt am: 2026-11-09
- Absender: Architecture
- Empfänger: Documentation
- Priorität: MEDIUM
- Status: OPEN
- Übergabegrund: Neue technische Grundsatzentscheidung (Vertrauensmodell, Datenmodell v8, Sicherungsformat), vom Inhaber freigegeben 2026-11-09.
- Aufgabenbeschreibung:
  1. `DECISIONS.md`: neue DEC (nächste freie Nummer, Kategorie Synchronisation, Status **Accepted**, noch nicht umgesetzt) mit Inhalt aus `inbox/architecture.md` SYNC-20261109-048, „Ergebnis und Rückmeldung“ (1)–(6), Befunde F1–F4, Restrisiken, verworfene Alternative (Prefs). Abhängigkeiten: DEC-044, DEC-045, DEC-048, DEC-084, DEC-109, DEC-126, DEC-190. Anhang Punkt 13 (Server-GUID) als erledigt mit Verweis markieren. In DEC-109/DEC-188 nur Verweis auf die neue DEC; Versionsziel 0.13.0.
  2. `DATA-MODEL.md`: geplante Profilfelder `pcCheck`, `pcGuid`, `pcGuidHost` und DB v8 nur als **geplant** vermerken (ersetzt „keine Entscheidung, keine Felder“). Als umgesetzt erst nach Rückmeldung Development -047.
  3. `SYNC-ENGINE.md` §6 und `ARCHITECTURE.md`: erst nach Umsetzung (Development -047 meldet die tatsächlichen Klassen/Dateien); bis dahin nur Verweis auf die neue DEC.
- Technischer Kontext: Keine Codeänderung durch Architecture. Fakten aus Code und smbj 0.13.0 (`javap`) geprüft, siehe -048.
- Betroffene Dateien: docs/DECISIONS.md, docs/DATA-MODEL.md (später docs/SYNC-ENGINE.md, docs/ARCHITECTURE.md)
- Erwartetes Ergebnis: Eine DEC ohne Dublette, konsistente Verweise, nichts als umgesetzt dargestellt, was noch nicht umgesetzt ist.
- Tatsächlich durchgeführte Arbeiten: –
- Ergebnis und Rückmeldung: –
- Abgeschlossen am: –

### SYNC-20261109-046 – DECISIONS.md: Play-Store-Integration als offene Punkte festhalten; PC-Erkennung entkoppeln
- Erstellt am: 2026-11-09
- Absender: Projektmanager (im Auftrag des Inhabers)
- Empfänger: Documentation
- Priorität: MEDIUM
- Status: COMPLETED
- Übergabegrund: Entscheid des Inhabers: Die Play-Store-Integration bleibt vorerst unspezifiziert und wird nur als offene Punkte geführt. PC-Erkennung ist nicht mehr Teil der Play-Store-Vorbereitung.
- Aufgabenbeschreibung:
  1. In DECISIONS.md (DEC-109, Kategorie 14) den Block „Offene Punkte Play-Store-Integration“ ergänzen. Nur Bestehendes übernehmen, nichts erfinden:
     - Play-Konto und Tester fehlen. Geschlossener Test mit 12 Testern über 14 Tage, 25 USD, Identitätsprüfung. Kein Früh-Upload zur Reservierung der Paket-ID.
     - Flavors `github`/`play`: `play` ohne Selbst-Update und ohne `REQUEST_INSTALL_PACKAGES`. Paket-ID `ch.leo.sync` nur für `play` (DEC-110, DEC-124). Die GitHub-Variante entfällt nach dem Play-Start, Umzug per Export/Import.
     - Umgang mit dem 6-Stunden-Limit für dataSync-FGS ab Android 15.
     - Datenschutzerklärung DE/EN (nDSG, DSGVO) über GitHub Pages; Kontakt `leotools@protonmail.com`.
     - Vor dem ersten AAB-Upload prüfen, ob Ressourcen-Namen verkürzt werden und Kennungen falsch aufgelöst werden (QA-Risiko aus SYNC-20261109-040, LOW).
     - Versionsnummer: nicht festgelegt, es gilt die nächste freie Minor-Reihe.
  2. DEC-109 und DEC-188 anpassen: PC-Erkennung ist nicht mehr Teil der Play-Vorbereitung und hängt nicht vom Play Store ab. Sie wird eigenständig umgesetzt (siehe Development SYNC-20261109-047). Die Aufzählung „PC-Erkennung statt WLAN-Name“ aus DEC-109 gehört zu diesem Auftrag. In ARCHITECTURE.md §14 und SYNC-ENGINE.md (Verweis ca. Zeile 1200) nur die Verweise nachziehen, soweit sie Play und PC-Erkennung verknüpfen.
- Technischer Kontext: Heutige Erreichbarkeit: TCP Port 445, 3 s, ohne Anmeldung (`SmbSession.isReachable`). Neuester Build: 0.12.4. Keine Codeänderung.
- Betroffene Dateien: docs/DECISIONS.md (DEC-109, DEC-188), docs/ARCHITECTURE.md (§14), docs/SYNC-ENGINE.md (Verweis ca. Zeile 1200)
- Erwartetes Ergebnis: Offene Punkte sind in DECISIONS.md klar als „offen/nicht umgesetzt“ gekennzeichnet. Die Entkopplung ist konsistent dokumentiert. Keine Dubletten zu bestehenden DEC.
- Tatsächlich durchgeführte Arbeiten: Übernahme bestätigt. `DECISIONS.md` DEC-109: PC-Erkennung-Aufzählung durchgestrichen mit Verweis, Aktualisierung 2026-11-09 und Block „Offene Punkte Play-Store-Integration“ (6 Punkte aus dem Auftrag, Verweis auf TESTING Abschnitt 10); DEC-188: Hinweis „überholt“ bei der Aufzählung. `ARCHITECTURE.md` §14 und `SYNC-ENGINE.md` 19 (Verweis): Verknüpfung Play/PC-Erkennung entfernt. Keine Codeprüfung.
- Ergebnis und Rückmeldung: Alles aus dem Auftrag übernommen, nichts ergänzt. Offen/Hinweis: (1) Versionsnummer der PC-Erkennung ist im Auftrag nicht genannt, in DEC-188 daher als „nicht genannt“ vermerkt; DEC-188 hat weiter Status Accepted und nennt 0.12.0/0.13.0 im Haupttext (nur kommentiert, nicht umgeschrieben). (2) Status von DEC-109 bleibt Accepted. (3) Nicht geprüft: weitere Verweise auf „0.13.0“ in `DECISIONS.md` (z. B. DEC-126), `README.md`, Memory-Roadmap (nicht Doku-Datei).
- Abgeschlossen am: 2026-11-09

### SYNC-20261109-045 – Doku zu 0.12.4: Diagnose pseudonymisiert/maskiert (aus Development)
- Erstellt am: 2026-11-09
- Absender: Development
- Empfänger: Documentation
- Priorität: LOW
- Status: COMPLETED
- Übergabegrund: Neue Entscheidung (Datenschutz der Diagnosedaten), Release-Doku und Teststand.
- Aufgabenbeschreibung: (1) `DECISIONS.md`: neue Entscheidung (nächste freie Nummer, DEC-203 war frei): Diagnose („Problem melden“, „Diagnosedaten kopieren“) enthält keine Benutzernamen, IP-Adressen, Rechner-/Freigabe-/WLAN-/Profil-/Ordnernamen, Pfade und genauen Dateinamen; stattdessen stabile Platzhalter je Export (Profil-1, Host-1, Freigabe-1, WLAN-1, Ordner-1, Benutzer-1), Freitext (Log, Fehlertexte, Abstürze) wird per Muster maskiert (<IP>, <MAC>, <E-Mail>, <URL>, <Pfad>, <Datei>.ext, <Koordinaten>, <Host>). Erhalten bleiben: Jahr/Monat-Ordner, Android-Standardordner (DCIM, Camera, Pictures …), Dateiendungen, öffentliche Update-URL (github). Filterregeln nur als Anzahl und Form („x*.x“). Aktuelles WLAN nur als Platzhalter. Geräte-ID (Zufalls-UUID), Gerätemodell, Android-Version, Einstellungen, Zähler bleiben. **Bewusst unverändert:** Verlauf-Export (`Diagnostics.logExport`, „Verlauf teilen“) = Export des Benutzers für sich selbst. Grundsatz: lieber zu viel maskieren. Restrisiko: Dateinamen/Namen in unbekannten Freitext-Formaten ohne bekannte Endung oder Muster können unmaskiert bleiben (Beispiel: Ordnername in Fehlertext ohne Pfadtrenner, der nicht als Profil/Pfad registriert ist). (2) `ARCHITECTURE.md` (Paket `diag`): neue Klasse `Redactor` (reine Kotlin-Klasse, ohne Android). (3) `TESTING.md`: `DiagnosticsTest` von 6 auf 15 Tests (grüner Lauf `testDebugUnitTest` komplett, Development 2026-11-09, nur Gesamtzahl aus QA-Lauf übernehmen); keine Strings-Änderung an Schlüsseln, nur Text `settings_diag_hint` DE/EN. (4) `PATCHNOTES.md` 0.12.4.
- Technischer Kontext: Neue Datei `diag/Redactor.kt`; `Diagnostics.build` registriert vor der Ausgabe alle bekannten Werte (Profile, SMB-Ziel, Benutzer, WLAN, Ordner, Namen der letzten Läufe), `compress`/`runLine`/`logLine` und Abstürze laufen durch `scrub`. Keine DB-/Schema-/Dateiformat-Änderung.
- Betroffene Dateien: `app/src/main/java/com/sync2pc/app/diag/Redactor.kt` (neu), `diag/Diagnostics.kt`, `app/src/test/java/com/sync2pc/app/DiagnosticsTest.kt`, `res/values/strings_settings.xml`, `res/values-de/strings_settings.xml`, `version.properties` (0.12.4), `.claude/translations.md` (generiert)
- Erwartetes Ergebnis: Doku passt zur Implementierung.
- Tatsächlich durchgeführte Arbeiten: Übernahme bestätigt. `Redactor.kt` (Kopfkommentar, `pseudo`/`register`/`scrub`) gelesen, Verwendung in `Diagnostics` bestätigt (Treffer vorhanden); Muster-Liste und `DiagnosticsTest` nicht im Detail geprüft. `DECISIONS.md`: neu DEC-203 (Implemented), Statistik (Implemented 153, Total 203), Index, Statusliste. `ARCHITECTURE.md`: Zeile `diag/Redactor.kt`. `TESTING.md` 4.2: `DiagnosticsTest` 15. `PATCHNOTES.md`: 0.12.4.
- Ergebnis und Rückmeldung: Angaben zur Maskierung aus dem Auftrag übernommen; Freigabe durch den Inhaber, Alternativen und Begründung nicht überliefert (so in DEC-203 vermerkt). Testzahl 15 laut Development, nicht von QA gemessen; Gesamtzahl (143) in TESTING 1/13 und ARCHITECTURE 4.7 bewusst nicht geändert bis QA-Lauf. Nicht geändert: `PRIVACY_*.md` (Diagnose-Abschnitt könnte nachzuziehen sein, nicht geprüft); `version.properties` und `.claude/translations.md` nicht gelesen. Kosmetik: DEC-203 steht im Dateiteil von Abschnitt 6 (neben DEC-202), Kategorie aber Sicherheit/Abschnitt 13.
- Abgeschlossen am: 2026-11-09

### SYNC-20261109-044 – Doku-Nachführung zu 0.12.2/0.12.3 (aus Development -039, -043)
- Erstellt am: 2026-11-09
- Absender: Development
- Empfänger: Documentation
- Priorität: LOW
- Status: COMPLETED
- Übergabegrund: Kleine Nachträge zu Implementierungen nach 0.12.1.
- Aufgabenbeschreibung: (1) DEC-144 / `SYNC-ENGINE.md` (Eingriff beim laufenden Sync): `Notifications.interventionDone` liefert `false`, wenn die Hinweise für LeoSync im System (`areNotificationsEnabled`) oder der Kanal `CH_ALERTS` (Wichtigkeit NONE) ausgeschaltet sind; das Ergebnis bleibt dann als DONE in der App stehen, bis die Ansicht geöffnet wird (0.12.3, -039). Restrisiko unverändert: DONE nur im Speicher. (2) `TESTING.md`: neuer instrumentierter Test `MsgInstrumentedTest` (6 Tests: Namen-Round-Trip mit echtem `AndroidResolver`, Plural/verschachtelt, Rückfall, DE/EN, `triggerData/triggerOf`; Sprachwechsel nur bis Android 12). **Noch nicht ausgeführt:** Testzahl erst nach grünem `connectedDebugAndroidTest` (QA/Inhaber) eintragen, nichts erfinden. Gerätetest Inhaber ergänzen: Hinweise aus → Eingriff → Ansicht öffnen. (3) `DATA-MODEL.md` (falls die Kennung beschrieben ist): `Msg.toCode()` fängt Ausnahmen ab und liefert dann null (Rückfalltext, 0.12.2).
- Technischer Kontext: Development -039 (0.12.3), -043 (0.12.2), PATCHNOTES 0.12.2/0.12.3 vorhanden.
- Betroffene Dateien: `docs/DECISIONS.md` oder `docs/SYNC-ENGINE.md`, `docs/TESTING.md`, `docs/DATA-MODEL.md`
- Erwartetes Ergebnis: Doku passt zur Implementierung.
- Tatsächlich durchgeführte Arbeiten: Übernahme bestätigt. Code gelesen: `Notifications.interventionDone` (Z. 179), `Msg.toCode()` (try/catch, Z. 37); `MsgInstrumentedTest` Datei nicht gelesen. `DECISIONS.md` DEC-201: Nachtrag 0.12.3 (Eingriff bei ausgeschalteten Hinweisen; DEC-144 selbst nicht geändert, der Eingriff gehört seit DEC-201 dorthin). `DATA-MODEL.md` 3.11: `toCode()` fängt Ausnahmen. `TESTING.md` 4.3: `MsgInstrumentedTest` ohne gemessene Zahl, Gerätetest ergänzt.
- Ergebnis und Rückmeldung: Testzahl 6 aus Auftrag übernommen, nicht ausgeführt; Gesamtzahl instrumentierter Tests in Abschnitt 1/13 bewusst nicht geändert (nach QA-Lauf). `SYNC-ENGINE.md` nicht geändert (Eingriff dort nicht beschrieben, nicht geprüft).
- Abgeschlossen am: 2026-11-09

### SYNC-20261109-042 – TESTING.md: gemessene Testzahlen 0.12.1 und Qualitätsrisiko Ressourcen-Namen (aus QA -040)
- Erstellt am: 2026-11-09
- Absender: QA
- Empfänger: Documentation
- Priorität: LOW
- Status: COMPLETED
- Übergabegrund: In -041 wurden die Testzahlen bewusst bis zur QA-Messung offen gelassen; zudem ein dokumentationswürdiges Qualitätsrisiko für die Auslieferung als App-Bundle. Ersetzt die Zahlen in -037 (Nachträge dort).
- Aufgabenbeschreibung: (1) Gemessen (QA-Lauf 2026-11-09, `testDebugUnitTest --rerun-tasks`, JDK 17, aus JUnit-XML): **25 Klassen, 143 Tests, 0 Fehler, 0 Fehlschläge, 0 übersprungen** (132 + 11 `MsgTest`). `connectedDebugAndroidTest` erstmals durch QA ausgeführt, Emulator `LeoSync_API29(AVD)` (Android 10): **`MigrationTest` 3/3 bestanden** (`fromVersion1ToLatest` bis 7, `verifyLevelFromVersion5`, `runCodesFromVersion6`); in `TESTING.md` Abschnitte 1, 4, 5, 13 sowie `ARCHITECTURE.md` 4.7 nachführen; `app/schemas/…/7.json` ist jetzt vorhanden (Befund 1 aus -041 erledigt). (2) Risiko dokumentieren (Abschnitt 10, möglich, nicht belegt): Kennungen speichern Ressourcen-**Namen** (`getResourceEntryName`) und lösen per `getIdentifier` auf. Im **APK** bleiben die Namen erhalten (QA mit `aapt2 dump resources` am Release-APK 0.12.1 geprüft: 799 Strings, alle lesbar, u. a. `run_nothing`, `run_trashed`, `run_conflicts`). Für ein **App-Bundle (AAB, Play Store)** nicht geprüft; dort kann Android-/Bundle-Optimierung Ressourcen-Namen verkürzen, dann würden gespeicherte Kennungen nach einem Update falsch oder gar nicht aufgelöst (Rückfalltext). Vor dem ersten AAB-Upload prüfen. (3) Testlücken ergänzen: `SyncScheduler.triggerData/triggerOf`, `SyncDao.closeDanglingRuns`, Auflösung per Name mit echten Android-Ressourcen (nur `TestTexts`), Sprachwechsel im Verlauf. (4) Manuelle Gerätetests (Inhaber, nicht ausgeführt): Update von 0.11.x mit bestehendem Verlauf (alte Einträge als Text), Sprachwechsel DE↔EN mit Verlauf (Lauf-Karte, aufgeklappte Zeilen, Export), Release-APK starten und Verlauf prüfen.
- Technischer Kontext: QA -040 (Version 0.12.1).
- Betroffene Dateien: `docs/TESTING.md`, `docs/ARCHITECTURE.md` (4.7)
- Erwartetes Ergebnis: Doku stimmt mit den gemessenen Zahlen überein; nichts Ungeprüftes behaupten.
- Tatsächlich durchgeführte Arbeiten: Übernahme bestätigt. `TESTING.md`: Abschnitte 1 (25/143; Instrumentation 3 Tests, Emulator bestanden), Qualitätsmechanismen (Schemas bis `7.json`), 2 (Befehlstabelle 3 Tests), 4.1 (`MsgTest` gemessen), 4.3, 5 (`7.json` vorhanden, 6→7 bestanden), 10 neu Punkt (10) mit Testlücken, manuellen Gerätetests (nicht ausgeführt) und Qualitätsrisiko Ressourcen-Namen im AAB, 13 (Zahlen). `ARCHITECTURE.md` 4.7: 25 Klassen/143 Tests.
- Ergebnis und Rückmeldung: Alle Zahlen und Aussagen aus QA -040/-042 übernommen, nicht selbst gemessen; Quelltext nicht gelesen. Offen: ältere Testzahlen in `DATA-MODEL.md` Abschnitt 14 (aus -041 Befund 5) unverändert; Hinweis „Differenz 109→110“ in TESTING Abschnitt 10 nicht bereinigt.
- Abgeschlossen am: 2026-11-09

### SYNC-20261109-041 – Doku-Nachführung zu 0.12.0/0.12.1: Verlauf mit Kennungen (DEC-159, aus Development -009)
- Erstellt am: 2026-11-09
- Absender: Development
- Empfänger: Documentation
- Priorität: LOW
- Status: COMPLETED
- Übergabegrund: Schema- und Datenmodelländerung (DB-Version 7), neue Komponente.
- Aufgabenbeschreibung: `DATA-MODEL.md`: DB-Version 7, neue nullable Spalten `sync_runs.messageCode`/`triggerCode`, `log_entries.code`, Aufbau der Kennung (JSON: `t`, `k`+`a`, `k`+`n`+`a`, `j`+`p`; Ressourcen-Name, Tiefe ≤ 12), Rückfalltext, Aufbewahrung (Logs 30/Läufe 90 Tage), Sicherung unberührt. `DECISIONS.md`: DEC-159 Umsetzung (0.12.0 Stufe 1, 0.12.1 Stufe 2), Risiko D35 behoben, D33 (`TestTexts` löst Namen auf), Diagnose bleibt Text (DEC-160). `ARCHITECTURE.md`/`SYNC-ENGINE.md`: `Msg`, `Texts.Resolver`, Auslöser in Auftragsdaten (`KEY_TRIGGER_CODE`), `isIdle` über `RunSummary.NOT_IDLE_KEYS`. `TESTING.md`: neu `MsgTest`, `MigrationTest` 6→7 (Gerät), Testzahlen erst nach QA-Lauf (-040). Versionsplan: 0.12.0 war für Play Store vorgesehen (Memory-Roadmap), jetzt für diese Funktion belegt; Inhaber entscheidet. PATCHNOTES 0.12.0/0.12.1 sind bereits eingetragen.
- Technischer Kontext: Development -009, Architecture -015.
- Betroffene Dateien: `docs/DATA-MODEL.md`, `docs/DECISIONS.md`, `docs/ARCHITECTURE.md`, `docs/SYNC-ENGINE.md`, `docs/TESTING.md`
- Erwartetes Ergebnis: Doku passt zur Implementierung; nichts erfinden.
- Tatsächlich durchgeführte Arbeiten: Übernahme bestätigt. Code gelesen: `Msg.kt`, `Texts.kt` (Resolver), `AppDatabase` (`MIGRATION_6_7`, Version 7), `Entities.kt`, `SyncScheduler` (`KEY_TRIGGER_CODE`), `RunSummary.NOT_IDLE_KEYS`, `RunGrouping`, `Diagnostics.logExport/logLine`, `MigrationTest`, `MsgTest` (Anzahl `@Test`). `DATA-MODEL.md`: Kopf (DB 7, 0.12.1), 3.9/3.10 neue Spalten, neu 3.11 (Kennungsformat), 10 (Migration 6→7, Zeitleiste), 12.4, 14, 17. `DECISIONS.md`: DEC-202 auf Implemented mit Nachtrag Umsetzung, DEC-159-Konsequenz, D35 behoben (alte Einträge bleiben), D33 ergänzt, Anhang A Punkt 19 erledigt, Statistik (Accepted 14, Implemented 152). `ARCHITECTURE.md`: neue Zeile `l10n/Msg.kt`, `Texts.Resolver`, `RunSummary`, Regel „Keine Texte im Code“, Abschnitt 14. `SYNC-ENGINE.md`: Lauf-Schritt 1, Abgrenzung 18.2. `TESTING.md`: Zeile `MsgTest`, `MigrationTest` 6→7, Abschnitt 5.
- Ergebnis und Rückmeldung: Versionsplan nach Vorgabe Inhaber 2026-11-09: 0.12.0 bleibt dem Verlauf mit Kennungen zugeordnet; die Nummer für den Play Store ist offen und so dokumentiert (DATA-MODEL 17, ARCHITECTURE 14, SYNC-ENGINE 18.2; DEC-188 unverändert, ggf. Architecture/Inhaber). Befunde: (1) `app/schemas/…/7.json` existiert noch nicht (entsteht beim nächsten Build), sonst scheitert `MigrationTest` 6→7. (2) `Diagnostics.ALREADY_COUNT` ist weiterhin nur deutsch (Nebenbefund aus -015 nicht behoben); bei Bedarf an Development. (3) Vollständigkeit der Umstellung aller Log-Aufrufe auf `Msg` nicht einzeln geprüft. (4) Testzahlen in `TESTING.md` (Abschnitte 1, 13, `ARCHITECTURE.md` 4.7) bewusst nicht geändert, bis QA -040 gemessen hat; `MsgTest` mit 11 gezählt aus dem Quelltext. (5) In `DATA-MODEL.md` Abschnitt 14 stehen ältere Testzahlen weiterer Klassen (Stand 0.11.0), nicht nachgeführt. Nicht ausgeführt: Tests, Build.
- Abgeschlossen am: 2026-11-09

### SYNC-20261108-037 – TESTING.md: Stand 0.11.10 (Testzahlen, Testlücken Eingriff beim Sync) (aus QA -035)
- Erstellt am: 2026-11-08
- Absender: QA
- Empfänger: Documentation
- Priorität: LOW
- Status: COMPLETED
- Übergabegrund: Teststrategie/Testzahlen in `docs/TESTING.md` sind nicht mehr aktuell.
- Aufgabenbeschreibung: (1) Gemessene Zahlen (QA-Lauf 2026-11-08, `testDebugUnitTest --rerun-tasks`, JDK 17, aus JUnit-XML): **23 Klassen, 124 Tests, 0 Fehler, 0 Fehlschläge, 0 übersprungen** (118 + 6 aus `TrashPurgeConfirmedTest`); in Abschnitt 1, 4.1 (Tabelle, neue Zeile `TrashPurgeConfirmedTest`: `TrashPurge.planConfirmed`, 6 Szenarien: ganzer Tag, neuere Version bleibt, geänderte Grösse, vorgemerkte bleiben, verschwundene Einträge/Tage unabhängig, `dateDir`), 9, 10 und 13 (Zahlen) nachführen. (2) Testlücken in Abschnitt 6/10 ergänzen: `PendingIntervention` (Zustände, ein Eingriff je Profil, Abbrechen nur wartend), `TrashCleaner.purgeConfirmed`/`deleteEntries` mit I/O, `FolderFreer.verifyAndDelete` mit `onStart`, `MirrorEngine` Leeren nach Stichprobe (-034) ohne Test. (3) Manuelle Gerätetests ergänzen (beim Inhaber, nicht ausgeführt): Aufräumen während laufendem Sync (Wartezustand, Ansicht verlassen, Ergebnis als Hinweis), „Abbrechen“ im Wartezustand, PC aus → „Alles“ → Ordnerwechsel → Sync mit Ersetzungen → nächster Sync (ersetzte Versionen bleiben), Speicher freigeben während Sync, Prozess beenden im Wartezustand (nichts gelöscht).
- **Nachtrag QA -038 (2026-11-08):** Aktueller Stand nach Development -036: **24 Klassen, 132 Tests, 0 Fehler, 0 Fehlschläge, 0 übersprungen** (124 + 8 aus `InterventionRegistryTest`; statt der Zahlen oben verwenden). Neue Zeile in 4.1: `InterventionRegistryTest` (`mirror/InterventionRegistry`, Zustandslogik der Eingriffe: Warten/Laufen/Fertig, ein Eingriff je Profil, Abbrechen nur wartend, Neustart nach Abbrechen, Zustellung/nicht zustellbares Ergebnis, Fehlertext, Zähler). Die Lücke „`PendingIntervention` ohne Test“ in (2) entfällt; offen bleibt: Abbrechen genau zwischen Sperrerwerb und `onStart`, `purgeConfirmed`/`deleteEntries` mit I/O, `Notifications`. Zusätzliche Gerätetests (Inhaber): System-Hinweise für LeoSync aus → Eingriff → Ansicht schliessen → Ergebnis prüfen; „Abbrechen“ kurz vor Sync-Ende wiederholt.
- Technischer Kontext: QA -035 (Version 0.11.10), Entscheidungen DEC-144 und Folgeaufträge siehe Development -012/-034.
- Betroffene Dateien: `docs/TESTING.md`
- Erwartetes Ergebnis: Doku stimmt mit gemessenen Zahlen und dem tatsächlichen Teststand überein; nichts Ungeprüftes behaupten.
- Tatsächlich durchgeführte Arbeiten: Übernahme bestätigt. `TESTING.md`: Abschnitte 1, 2 (Befehlstabelle), 13: 24 Klassen/132 Tests (Stand -038); neue Zeilen in 4.1 `TrashPurgeConfirmedTest` (6) und `InterventionRegistryTest` (8); Testlücken (9) in Abschnitt 10 und manuelle Gerätetests (nicht ausgeführt). `ARCHITECTURE.md` 4.7: Zahlen angepasst.
- Ergebnis und Rückmeldung: Zahlen aus dem QA-Auftrag übernommen, nicht selbst gemessen; Testinhalte der beiden Klassen aus -035/-038 übernommen, Quelltext nicht gelesen. Abschnitt 9 enthält keine Testzahlen, unverändert. Die Lücke „`PendingIntervention` ohne Test“ nicht aufgenommen (durch -038 hinfällig). Nicht ausgeführt: Prüfung, ob `ARCHITECTURE.md` 4.7 weitere Testklassen namentlich aufzählt.
- Abgeschlossen am: 2026-11-08

### SYNC-20261108-033 – Neue Architekturentscheidungen aus -014 und -015 in DECISIONS festhalten
- Erstellt am: 2026-11-08
- Absender: Architecture
- Empfänger: Documentation
- Priorität: MEDIUM
- Status: COMPLETED
- Übergabegrund: Zwei vom Inhaber freigegebene Architekturentscheidungen (2026-11-08); Entscheidungsdokumentation nachführen.
- Aufgabenbeschreibung: (1) Neue DEC „Eingriffe beim laufenden Ordner-Sync: prozessweit statt dauerhaft vormerken; nur der bestätigte Stand wird gelöscht“, Status Accepted, Inhalt aus -014 (a–f, verworfene Alternativen, Restrisiken). (2) DEC-144 ergänzen: Konsequenz „Verlässt man die Ansicht … unterbleibt der Eingriff“ wird durch die neue DEC abgelöst, sobald umgesetzt; Verweis setzen. (3) Neue DEC „Verlauf mit Kennungen: Code-Spalten mit Rückfalltext“, Status Accepted, Inhalt aus -015 (a–e, verworfene Alternativen); DEC-159 und Anhang A Punkt 19 darauf verweisen. (4) Risiko D35 und D29 je mit Verweis auf die neuen DEC ergänzen. (5) Neues Risiko bzw. Befund F3 (Aufräumen löscht mehr als angezeigt, bis zur Umsetzung von -012) bei DEC-140 oder Anhang D festhalten. Status bleibt Accepted, bis Development umgesetzt hat; SYNC-ENGINE.md und DATA-MODEL.md erst nach der Umsetzung (Development -012/-009 meldet dann).
- Technischer Kontext: Vorgaben stehen vollständig in `inbox/architecture.md` unter -014 und -015 („Ergebnis und Rückmeldung“). Umsetzung: Development -012 und -009. Befund F5 (vorgemerktes PC-Leeren bei ausstehender Stichprobe) ist inzwischen freigegeben und als Development -034 übergeben; Doku dazu erst nach dessen Umsetzung (Meldung durch Development).
- Betroffene Dateien: `docs/DECISIONS.md` (neue DEC, DEC-144, DEC-159, DEC-140 oder Anhang D, Anhang A Punkt 19, Statistik und Index)
- Erwartetes Ergebnis: DECISIONS.md enthält beide Entscheidungen konsistent mit -014/-015; nichts darüber hinaus erfinden.
- Tatsächlich durchgeführte Arbeiten: Übernahme bestätigt. `DECISIONS.md`: neu DEC-201 (Eingriffe vormerken, Abschnitt 2) und DEC-202 (Verlauf mit Kennungen, Abschnitt 6), beide Accepted, Inhalt aus `inbox/architecture.md` -014/-015; DEC-144 (Konsequenz als abgelöst markiert, Verweis), DEC-159 (Alternativen/Konsequenzen/Abhängigkeiten), DEC-140 (Konsequenz F3), Anhang A Punkte 17 und 19, Anhang D: D29 und D35 mit Verweis, neu D47 (F3); Statistik (Accepted 15, Total 202), Themenzähler, Abschnittslisten, Index, Session-5-Kopf.
- Ergebnis und Rückmeldung: Nichts am Code geprüft, Angaben aus -014/-015. `SYNC-ENGINE.md` und `DATA-MODEL.md` bewusst nicht geändert (erst nach Umsetzung, Meldung durch Development -012/-009 abwarten); ebenso Befund F5/Development -034. Nummern DEC-201/-202 und D47 von mir vergeben (nächste freie). Nicht geprüft: ob `ARCHITECTURE.md` auf DEC-144 verweist und nachzuziehen ist.
- Abgeschlossen am: 2026-11-08

### SYNC-20261108-031 – Doku-Nachführung zu Version 0.11.9 (Absicherung Ordner-Löschung, Sperre Versuchszähler)
- Erstellt am: 2026-11-08
- Absender: Development
- Empfänger: Documentation
- Priorität: LOW
- Status: COMPLETED
- Übergabegrund: Fehlerbehebung an der Löschlogik aus QA -029/Development -030; Restrisiko dokumentieren.
- Aufgabenbeschreibung: (1) `LocalTree.deleteDirIfEmpty` zählt vor dem Löschen alle Einträge des Ordners (`rawChildCount`), auch ohne Anzeigenamen, und löscht nur bei 0; vorher übersah `children()` solche Einträge (`?: continue`) und hätte den Ordner rekursiv gelöscht. (2) Restrisiko, vom Entwickler akzeptiert: Zwischen Zählung und `deleteDocument` kann ein anderer Prozess eine Datei anlegen (Millisekunden; Android kennt kein nicht-rekursives Löschen); bei DEC-147/Risiko D26 festhalten. (3) `RestoreFailures` (Prefs `restore`) schreibt unter einer Sperre; bei DEC-146 nur erwähnen, falls Nebenläufigkeit dort beschrieben ist. (4) Bekannt, unverändert: bei nicht vollständigem Zurückrollen zeigt der Verlauf nach dem 5. Fehlversuch nicht „nicht mehr automatisch wiederholt“, die Startseite aber schon. PATCHNOTES 0.11.9; `TESTING.md`: QA-Zahlen aus -029 (22 Klassen, 118 Tests) falls noch nicht eingetragen, Testlücke `deleteDirIfEmpty`/`RestoreFailures` (nicht JVM-testbar).
- Technischer Kontext: Development -030, QA -029.
- Betroffene Dateien: `docs/DECISIONS.md` (DEC-147, DEC-146), `docs/TESTING.md`, `PATCHNOTES.md`
- Erwartetes Ergebnis: Doku passend zum Code, nichts erfinden.
- Tatsächlich durchgeführte Arbeiten: Übernahme bestätigt. Code gesehen: `rawChildCount` (`LocalTree` Z. 438, Prüfung Z. 454), `synchronized(LOCK)` in `RestoreFailures`. `DECISIONS.md` DEC-147: A1 als behoben (0.11.9), A2 als akzeptiertes Restrisiko D26. `TESTING.md`: Lücke ergänzt (Zahlen 22/118 standen schon). `PATCHNOTES.md`: 0.11.9.
- Ergebnis und Rückmeldung: Punkt 3 (Sperre) nur in PATCHNOTES, da Nebenläufigkeit in DEC-146 nicht beschrieben ist. Punkt 4 war bereits dokumentiert (QA B2). Anhang B Punkt 26 nicht erweitert, Restrisiko steht in DEC-147. Nicht geprüft: Verhalten auf dem Gerät.
- Abgeschlossen am: 2026-11-08

### SYNC-20261108-028 – Doku-Nachführung zu Version 0.11.8 (Aufräumen Texte, leere Ordner, Wiederherstellung 5 Versuche)
- Erstellt am: 2026-11-08
- Absender: Development
- Empfänger: Documentation
- Priorität: MEDIUM
- Status: COMPLETED
- Übergabegrund: Geändertes Verhalten (Löschlogik, Wiederanlauf), neue Tests; Doku nach `CLAUDE.md` §11.
- Aufgabenbeschreibung:
  1. **DEC-147 (Speicher freigeben, leere Ordner), Umsetzung 0.11.8:** Nach `FolderFreer.verifyAndDelete` werden die Elternordner der tatsächlich gelöschten Dateien (tiefster zuerst) entfernt, aber nur wenn sie beim Prüfen wirklich leer sind (`LocalTree.deleteDirIfEmpty`, liest unmittelbar vorher neu ein). Nie entfernt: Profil-Wurzel, Papierkorb-Ordner, `DCIM/Camera` (`FolderFreer.isProtectedDir`, gilt auch, wenn der gewählte Ordner `DCIM` oder `DCIM/Camera` ist). Fehler beim Aufräumen ändern das Ergebnis nicht. Anzeige: „X leere Ordner entfernt“ im Ergebnis (`Result.foldersRemoved`). Nur Profile Handy → PC ohne „Löschungen übernehmen“ (wie bisher). Risiko D26 teilweise entschärft; Inhaber bestätigte `DCIM/Camera` (Schreibfehler „DICM“).
  2. **DEC-146 (Wiederherstellung 5 Versuche), Umsetzung 0.11.8:** Pro Wiederherstellung zählt `RestoreFailures` Fehlversuche (Prefs „restore“, Schlüssel `attempts`, ausserhalb der Sicherung, Format `restoreId:n`). Netzwerkfehler zählen nicht (Vormerkung bleibt, Lauf wird unterbrochen). Ab dem 5. Fehlversuch (`RestoreRetryPolicy.MAX_ATTEMPTS`) überspringt `MirrorEngine.runRestores` den Eintrag. Die Vormerkung bleibt in der Datenbank, damit bleibt die Papierkorb-Version vor Löschen geschützt (`TrashPurge.reserved*`, unverändert). Startseite: Karte „Wiederherstellen fehlgeschlagen“ zeigt zusätzlich den Text für aufgegebene Einträge; „Erneut versuchen“ setzt die Zähler zurück (`RestoreManager.retryFailed`) und startet einen Sync; „Verwerfen“ löscht Vormerkung und Zähler wie bisher. Zähler werden bei Erfolg, Verwerfen und Profillöschung (`forget`) entfernt. Verlaufseintrag `eng_restore_gave_up` beim 5. Fehlversuch (nur wenn vollständig zurückgerollt; bei teilweisem Zurückrollen bleibt der bisherige Text „siehe Papierkorb“). Risiko D27 entschärft; Restrisiko: Zähler nur in Prefs (geht bei „Daten löschen“ der App verloren, dann beginnen die 5 Versuche neu).
  3. **Anhang A Punkt 25 (-013):** Entfernt: `profile_subfolder`, `profile_subfolder_example_mirror`, `profile_subfolder_example_photos`, `profile_browse`, `settings_battery_off`, `profile_delete_cd` (EN/DE). Vorher per Suche in `app/src` geprüft: nur Definitionen, keine Verwendung. `profile_subfolders*` bleibt.
  4. **Tests:** neu `RestoreRetryPolicyTest` (3 Tests), `FolderFreerTest` (5 Tests); nicht ausgeführt (Inhaberregel), `TESTING.md` bitte nach QA-Lauf (-029) mit gemessenen Zahlen ergänzen, nicht vorher.
  5. PATCHNOTES 0.11.8 und Entscheidungsstatus (DEC-146, DEC-147: Accepted → Implemented 0.11.8) nachführen; SYNC-ENGINE (Wiederherstellung/Wiederholregel), DATA-MODEL (Prefs „restore“: neuer Schlüssel `attempts`, nicht in Sicherung).
- Technischer Kontext: Development -004, -005, -013 (`inbox/development.md`), QA -029.
- Betroffene Dateien: `docs/DECISIONS.md`, `docs/SYNC-ENGINE.md`, `docs/DATA-MODEL.md`, `docs/TESTING.md`, `PATCHNOTES.md`
- Erwartetes Ergebnis: Doku passend zum Code; nichts erfinden, bei Unklarheit zurückfragen.
- Tatsächlich durchgeführte Arbeiten: Übernahme bestätigt. `DECISIONS.md`: Nachträge 0.11.8 in DEC-146 und DEC-147 (Status war schon Implemented), Anhang A Punkte 15, 16, 25 als umgesetzt, Anhang B Punkt 26 (D26) ergänzt. `SYNC-ENGINE.md` 7.6: Schritt 7 Wiederholregel. `DATA-MODEL.md`: Prefs `restore` Schlüssel `attempts`. `PATCHNOTES.md`: 0.11.8. Code stichprobenartig geprüft (`MAX_ATTEMPTS`=5, `isProtectedDir`, `deleteDirIfEmpty`, `foldersRemoved`, `retryFailed`, `eng_restore_gave_up`); übrige Angaben aus dem Auftrag.
- Ergebnis und Rückmeldung: Punkt 4 nachgeführt nach QA -029 (118 Tests in 22 Klassen, 0 Fehler; `FolderFreerTest` 5, `RestoreRetryPolicyTest` 3; Testlücken und Gerätetests) in `TESTING.md` und `ARCHITECTURE.md` 4.7; QA-Restrisiken A1/A2 (rekursives `deleteDocument`, Restfenster) in DEC-147 vermerkt. Nicht geprüft: Risiko-Nummern D26/D27 gegen Anhang B. Hinweis: Development -030 kann das Verhalten noch ändern, dann Doku erneut prüfen.
- QA-Nachtrag (2026-11-08, aus QA -029, Punkt 4 freigegeben): Gemessener Lauf `testDebugUnitTest --rerun-tasks`: 22 Klassen, 118 Tests, 0 Fehler/Fehlschläge/Übersprungen (110 + `FolderFreerTest` 5 + `RestoreRetryPolicyTest` 3). Nicht abgedeckt: `FolderFreer.verifyAndDelete`, `LocalTree.deleteDirIfEmpty`, `RestoreFailures` (Prefs), `runRestores`-Filter, Kartenlogik Startseite. Restrisiko in TESTING.md/DECISIONS aufnehmen: `deleteDocument` auf Ordnern ist bei vielen Anbietern rekursiv, Restfenster zwischen Einlesen und Löschen (D26); Offen bei Development: -030.
- Abgeschlossen am: 2026-11-08

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
