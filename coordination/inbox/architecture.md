## Aufträge
(neueste oben)

### SYNC-20261108-015 – Entscheidung vor Umsetzung: Verlauf mit Kennungen (DEC-159, Auftrag -009)
- Erstellt am: 2026-11-08
- Absender: Development
- Empfänger: Architecture
- Priorität: LOW
- Status: COMPLETED
- Übergabegrund: Änderung berührt Datenbankschema bzw. Sicherungsformat (`BackupCodec`); Lösungsansatz soll vor der Umsetzung festgelegt werden (Inhaber hat der Rückfrage an Architecture zugestimmt).
- Aufgabenbeschreibung: Festlegen, wie Verlaufseinträge (`RunSummary`, Verlauf/Log) statt fertiger Texte mit Kennungen (Schlüssel plus Parameter) gespeichert werden, sodass sie in der aktiven Sprache erscheinen. Offen: (a) Speicherform (neue Spalten oder kodierter Text in vorhandener Spalte), (b) Umgang mit bestehenden Einträgen (Fallback: alter Text unverändert anzeigen), (c) `isIdle` ohne Merkmal-Wörter (Risiko D35), (d) Rückwärts- und Vorwärtskompatibilität der Sicherungsdatei, (e) Export des Verlaufs über das Teilen-Menü (Sprache beim Export).
- Technischer Kontext: DEC-159, Anhang A Punkt 19 (Inhaber: umsetzen), Risiko D35. Entwicklungsauftrag: SYNC-20261108-009 (Development, wartet auf diese Entscheidung).
- Betroffene Dateien: `sync/RunSummary.kt`, `data/*` (Entities, AppDatabase, Migration), `ui/LogScreen.kt`, `backup/*`
- Erwartetes Ergebnis: Kurze Entscheidungsvorgabe (gewählte Speicherform, Migration ja/nein, Sicherungsformat-Regel); Eintrag in `DECISIONS.md` über Documentation.
- Tatsächlich durchgeführte Arbeiten: Code gelesen: `RunSummary`, `RunGrouping`, `Entities` (`SyncRun`, `LogEntry`), Schreibstellen in `MirrorEngine`/`SyncEngine` (47 `log`-Aufrufe), `SyncScheduler` (Auslöser), `SyncDao.closeDanglingRuns`, Leser (`LogScreen`, `HomeParts`, `PreviewScreen`, `SyncWorker`, `Diagnostics`), `backup/*`. Vorgabe vom Inhaber freigegeben (2026-11-08). Nichts am Code geändert.
- Ergebnis und Rückmeldung: **Vorgabe:**
  - (a) Neue nullable Spalten `log_entries.code`, `sync_runs.messageCode`, `sync_runs.triggerCode` (TEXT); DB-Version 7, `MIGRATION_6_7` nur `ADD COLUMN` (DEC-084), Schema-Export, `MigrationTest` 6→7. Code = kleiner JSON-Baum (org.json): Ressourcen-**Name** (nie R-ID), optional Pluralzahl, Argumente (Text, Zahl oder verschachtelter Code), Liste für Verkettung. Dateinamen und Fehlertexte bleiben wörtliche Argumente. `message`/`trigger` bleiben als Rückfalltext in der Sprache beim Schreiben. Ein zentraler Renderer (reiner Teil unit-getestet) zeigt den Code; unbekannter Schlüssel, Parse- oder Formatfehler → Rückfalltext. Verworfen: Kodierung in `message` (jeder vergessene Leser zeigt JSON, Rückfalltext und Diagnose-Regex gehen verloren); nur Schlüssel ohne Rückfall (umbenannte Texte → leere Einträge).
  - (b) Alte Einträge (Code NULL) zeigen den gespeicherten Text; keine Umrechnung. Sie verschwinden mit der Aufbewahrung (Logs 30, Läufe 90 Tage).
  - (c) `isIdle` über Schlüssel im Code (`run_trashed`, `run_found`, `run_conflicts`, `run_space_full`, Konstante neben `RunSummary`, Unit-Test); Merkmal-Wörter nur noch Rückfall für Einträge ohne Code. `RunSummary` liefert einen strukturierten Wert statt String.
  - (d) Sicherung unberührt: `backup/*` enthält weder Läufe noch Logs; Formatversion bleibt 1. Regel für später: Code-Felder optional, Leser ignorieren unbekannte Felder.
  - (e) Verlauf-Export (`Diagnostics.logExport`, Teilen-Menü) und alle Anzeigen (`LogScreen`, `HomeParts:280`, `PreviewScreen:88`, Fehlermeldung `SyncWorker:278`) rendern in der aktiven Sprache. Diagnose (`build`, `compress`) bleibt beim gespeicherten Text (DEC-160).
  - Auslöser: Kennung zusätzlich in die Auftragsdaten (`SyncScheduler`); fehlt sie (bereits eingeplante Aufträge), gilt der Text.
  - Umsetzung in zwei Stufen: 1. Lauf-Kurztext + Auslöser, 2. Log-Zeilen. `TestTexts` muss Namen auflösen können (Risiko D33).
  - **Nebenbefund:** `Diagnostics.compress` (Regex `ALREADY_COUNT`/`ALREADY_FOLDER`, Diagnostics.kt:336–337) erkennt „bereits vorhanden“ nur deutsch; in Englisch falsche Zählung in der Diagnose. Kann in Stufe 2 über den Schlüssel `eng_already_in` gelöst werden.
  - Restrisiken: vergessener Leser zeigt nur den Rückfalltext (harmlos); Platzhalter DE/EN müssen gleich bleiben (`StringsParityTest`). Folgeaufträge: Development -009 (Hinweis ergänzt), Documentation -033.
- Abgeschlossen am: 2026-11-08

### SYNC-20261108-014 – Entscheidung vor Umsetzung: Eingriff beim laufenden Sync vormerken (DEC-144, Auftrag -012)
- Erstellt am: 2026-11-08
- Absender: Development
- Empfänger: Architecture
- Priorität: MEDIUM
- Status: COMPLETED
- Übergabegrund: Nebenläufigkeit und Datenverlustrisiko (Löschen im Papierkorb, „Speicher freigeben“ Ordner); übergeordnete Entscheidung nötig, bevor Development umsetzt (Inhaber hat der Rückfrage an Architecture zugestimmt).
- Aufgabenbeschreibung: Festlegen des Vormerkungs-Modells für Aufräumen, Endgültig löschen und Speicher freigeben (Ordner), wenn ein Sync läuft und die Ansicht verlassen wird. Offen: (a) Wo wird vorgemerkt (Prefs wie `PurgeStore` oder Datenbank), (b) Gültigkeit: muss der Eingriff beim Ausführen neu gegen aktuelle Daten geprüft werden (Regeln, geschützte Versionen, Auswahl nach Alter/Ordner/Typ), (c) Verhalten bei App-Neustart/Abbruch, (d) Ausführung durch wen und wann (nach Sync-Ende in `SyncWorker`/`MirrorLock`-Freigabe oder eigener Worker), (e) Anzeige/Widerruf der Vormerkung, (f) Verhalten bei Konflikt mit dem Wiederanlauf.
- Technischer Kontext: DEC-144 (Alternative „vormerken“ wurde nicht gewählt), DEC-147, Risiko D29. Entwicklungsauftrag: SYNC-20261108-012 (wartet auf diese Entscheidung). Review und QA nach Umsetzung vorgesehen.
- Betroffene Dateien: `mirror/MirrorLock.kt`, `TrashCleaner`, `FolderFreer`, `ui/TrashScreen.kt`, `ui/FreeSpaceScreen.kt`, `PurgeStore`
- Erwartetes Ergebnis: Kurze Entscheidungsvorgabe zu (a) bis (f); Eintrag in `DECISIONS.md`/`SYNC-ENGINE.md` über Documentation.
- Tatsächlich durchgeführte Arbeiten: Code gelesen: `MirrorLock`, `TrashCleaner`, `TrashPurge`/`PurgeStore`, `FolderFreer`, `RestoreManager`, `MirrorEngine.run`/`runPendingPurges`/`purgeTrash`, `LocalTree.purgeTrash`, `TrashPolicy`, `ui/TrashScreen.kt`, `ui/FreeSpaceScreen.kt`. Vorgabe vom Inhaber freigegeben (2026-11-08). Nichts am Code geändert.
- Ergebnis und Rückmeldung: **Befunde:** (F1) Ursache: Eingriffe laufen in `rememberCoroutineScope` der Ansicht (TrashScreen.kt:156, FreeSpaceScreen.kt:128) und werden beim Verlassen abgebrochen. (F2) `MirrorLock` gilt je Profil-Lauf (MirrorEngine.kt:143), `Mutex` ist fair → wartender Eingriff kommt direkt nach dem laufenden Profil. (F3) **Bestehende Lücke:** Aufräumen zeigt die Anzahl aus dem eingelesenen Stand (TrashScreen.kt:383/445), führt aber die Regel aus; bei „Alles“ und „Tage“ mit heutigem Tag werden Versionen mitgelöscht, die ein Sync inzwischen in den Papierkorb gelegt hat (nach dem Warten oder wenn zwischen Einlesen und Bestätigen ein Sync lief). „Älter als“ ist sicher. (F4) „Endgültig löschen“ (konkrete Pfade) und `FolderFreer.verifyAndDelete` (volle Neuprüfung) sind robust. (F5) Restlücke `PurgeStore` bei ausstehender Stichprobe, siehe unten.
  **Vorgabe:**
  - (a) Nicht dauerhaft vormerken (keine Prefs, keine DB): prozessweites Objekt (Muster `PendingProfileDelete`, `SupervisorJob`) startet die bestehenden Funktionen; Verlassen der Ansicht bricht nicht ab. Verlorene Vormerkung = nichts gelöscht (sichere Richtung).
  - (b) Ausgeführt wird nur der bestätigte Stand, **immer** (behebt F3): Aufräumen übergibt die betroffenen Einträge (Seite, Pfad, Grösse); ein Datumsordner wird nur ganz gelöscht, wenn sein aktueller Inhalt vollständig darin enthalten ist, sonst nur die bestätigten Dateien. Übersprungen: geänderte Grösse, inzwischen zum Wiederherstellen vorgemerkt (Prüfung bei Ausführung). PC beim Einlesen unbekannt → Regel in `PurgeStore` wie bisher. Endgültig löschen und Speicher freigeben unverändert; Auswahlkriterien werden nicht neu ausgewertet.
  - (c) Neustart/Prozessende: Vormerkung entfällt, nichts gelöscht. Ergebnis als Benachrichtigung (bestehender Hinweis-Schalter) bzw. Meldung in der offenen Ansicht.
  - (d) Ausführung durch den vorgemerkten Eingriff selbst, sobald er `MirrorLock` erhält; keine Kopplung an `SyncWorker`/`MirrorEngine`, kein eigener Worker (10-Minuten-Grenze beim Warten, Sperre nur im Prozess).
  - (e) Hinweis „Wird nach dem laufenden Sync ausgeführt“ mit „Abbrechen“ (nur solange wartend) in Papierkorb und Speicher freigeben; weitere Lösch-Eingriffe für dasselbe Profil gesperrt, solange einer ansteht. Texte DE/EN.
  - (f) WLAN-Abbruch gibt die Sperre frei → Eingriff läuft; PC-Teile gehen in `PurgeStore`, Speicher freigeben meldet „PC nicht erreichbar“. Keine Verklemmung (Eingriff hält nur `MirrorLock`). Wiederherstellen hat Vorrang vor wartendem Löschen.
  - Verworfen: Prefs/DB (dauerhafte Löschabsichten, Haken im Worker, Migration), Worker, Ausführung in `MirrorEngine` am Laufende.
  - Restrisiken: Hersteller-Killer beendet den Prozess während des Wartens → Eingriff unterbleibt still (D14); Sperre bleibt global (D29). Kritische Änderung → Review und QA nach Umsetzung.
  - (F5) Bei ausstehender Stichprobe überspringt der Lauf das vorgemerkte PC-Leeren (MirrorEngine.kt:194), legt aber Versionen in den Papierkorb; der nächste Lauf leert sie mit. Lösung (nur `runPendingPurges` nach bestandener Stichprobe, Z. 216) vom Inhaber freigegeben → Development -034.
  - Folgeaufträge: Development -012 (Hinweis ergänzt), Documentation -033.
- Abgeschlossen am: 2026-11-08
