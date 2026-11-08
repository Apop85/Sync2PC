## Aufträge
(neueste oben)

### SYNC-20261108-015 – Entscheidung vor Umsetzung: Verlauf mit Kennungen (DEC-159, Auftrag -009)
- Erstellt am: 2026-11-08
- Absender: Development
- Empfänger: Architecture
- Priorität: LOW
- Status: OPEN
- Übergabegrund: Änderung berührt Datenbankschema bzw. Sicherungsformat (`BackupCodec`); Lösungsansatz soll vor der Umsetzung festgelegt werden (Inhaber hat der Rückfrage an Architecture zugestimmt).
- Aufgabenbeschreibung: Festlegen, wie Verlaufseinträge (`RunSummary`, Verlauf/Log) statt fertiger Texte mit Kennungen (Schlüssel plus Parameter) gespeichert werden, sodass sie in der aktiven Sprache erscheinen. Offen: (a) Speicherform (neue Spalten oder kodierter Text in vorhandener Spalte), (b) Umgang mit bestehenden Einträgen (Fallback: alter Text unverändert anzeigen), (c) `isIdle` ohne Merkmal-Wörter (Risiko D35), (d) Rückwärts- und Vorwärtskompatibilität der Sicherungsdatei, (e) Export des Verlaufs über das Teilen-Menü (Sprache beim Export).
- Technischer Kontext: DEC-159, Anhang A Punkt 19 (Inhaber: umsetzen), Risiko D35. Entwicklungsauftrag: SYNC-20261108-009 (Development, wartet auf diese Entscheidung).
- Betroffene Dateien: `sync/RunSummary.kt`, `data/*` (Entities, AppDatabase, Migration), `ui/LogScreen.kt`, `backup/*`
- Erwartetes Ergebnis: Kurze Entscheidungsvorgabe (gewählte Speicherform, Migration ja/nein, Sicherungsformat-Regel); Eintrag in `DECISIONS.md` über Documentation.
- Tatsächlich durchgeführte Arbeiten: –
- Ergebnis und Rückmeldung: –
- Abgeschlossen am: –

### SYNC-20261108-014 – Entscheidung vor Umsetzung: Eingriff beim laufenden Sync vormerken (DEC-144, Auftrag -012)
- Erstellt am: 2026-11-08
- Absender: Development
- Empfänger: Architecture
- Priorität: MEDIUM
- Status: OPEN
- Übergabegrund: Nebenläufigkeit und Datenverlustrisiko (Löschen im Papierkorb, „Speicher freigeben“ Ordner); übergeordnete Entscheidung nötig, bevor Development umsetzt (Inhaber hat der Rückfrage an Architecture zugestimmt).
- Aufgabenbeschreibung: Festlegen des Vormerkungs-Modells für Aufräumen, Endgültig löschen und Speicher freigeben (Ordner), wenn ein Sync läuft und die Ansicht verlassen wird. Offen: (a) Wo wird vorgemerkt (Prefs wie `PurgeStore` oder Datenbank), (b) Gültigkeit: muss der Eingriff beim Ausführen neu gegen aktuelle Daten geprüft werden (Regeln, geschützte Versionen, Auswahl nach Alter/Ordner/Typ), (c) Verhalten bei App-Neustart/Abbruch, (d) Ausführung durch wen und wann (nach Sync-Ende in `SyncWorker`/`MirrorLock`-Freigabe oder eigener Worker), (e) Anzeige/Widerruf der Vormerkung, (f) Verhalten bei Konflikt mit dem Wiederanlauf.
- Technischer Kontext: DEC-144 (Alternative „vormerken“ wurde nicht gewählt), DEC-147, Risiko D29. Entwicklungsauftrag: SYNC-20261108-012 (wartet auf diese Entscheidung). Review und QA nach Umsetzung vorgesehen.
- Betroffene Dateien: `mirror/MirrorLock.kt`, `TrashCleaner`, `FolderFreer`, `ui/TrashScreen.kt`, `ui/FreeSpaceScreen.kt`, `PurgeStore`
- Erwartetes Ergebnis: Kurze Entscheidungsvorgabe zu (a) bis (f); Eintrag in `DECISIONS.md`/`SYNC-ENGINE.md` über Documentation.
- Tatsächlich durchgeführte Arbeiten: –
- Ergebnis und Rückmeldung: –
- Abgeschlossen am: –
