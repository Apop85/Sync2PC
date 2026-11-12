---
name: investigate-bug
description: Strukturierte Fehlersuche in LeoSync mit nachgewiesener Ursache vor jeder Änderung. Verwenden bei Fehlermeldungen, unerwartetem Verhalten, Sync- oder Datenbankproblemen.
---

# Fehler untersuchen

Fehler: $ARGUMENTS

**Rollenberechtigung:** Dieser Skill dient primär der Fehleranalyse. Eine Fehlerbehebung erfolgt ausschliesslich im Rahmen der Berechtigungen der aufrufenden Rolle und eines entsprechenden Benutzerauftrags. Ein Analyseauftrag allein berechtigt nicht zur Implementierung.

1. **Beschreibung verstehen**: erwartetes vs. tatsächliches Verhalten, Reproduktion, Version, Logs/Verlauf (`.claude/LeoSync-Verlauf_*.txt`, `.claude/diag.txt` nur wenn relevant). Fehlendes als nummerierte Rückfrage.
2. **Komponenten eingrenzen** per Grep/Glob in `app/src/main/java/com/sync2pc/app/`.
3. **Doku gezielt laden** (nur Abschnitte): `docs/SYNC-ENGINE.md` bei Sync, `docs/DATA-MODEL.md` bei Datenbank, `docs/DECISIONS.md` nach DEC-ID/Stichwort (Verhalten kann gewollt sein!), `docs/TESTING.md` für Tests. Dateien mit Unterstrich sind Auftragstexte, keine Doku.
4. **Ursache untersuchen**: Code lesen, Datenfluss verfolgen. Komplexe Sync-Probleme -> Agent `sync-engine-specialist`; Datenbankprobleme -> Agent `database-specialist` (Analyse-Auftrag, keine Änderung).
5. **Ursache nachweisen**: konkrete Stelle (Datei:Zeile) und Ablauf, der den Fehler erzeugt; wenn möglich durch einen vorhandenen oder per Lesen nachvollzogenen Test belegen. Ist die Ursache nicht belegt, als Hypothese kennzeichnen und **nichts ändern**.
6. **Lösung**: vorschlagen oder (bei klarem Implementierungsauftrag und entsprechender Berechtigung) minimal umsetzen. Keine vorschnellen Änderungen, keine Nebenbei-Refactorings.
7. **Regressionstests**: Skill `run-targeted-tests`. Development erstellt keine Builds (`assemble*`, `build.bat`); der Inhaber baut selbst. Die eigenständige QA-Rolle darf Debug- und Test-Builds gemäss ihren Berechtigungen in `CLAUDE.md` erstellen.
8. **Auswirkungen prüfen**: weitere Aufrufer, Datenverlust-/Migrationsrisiko; bei Sync/DB-Änderung Skill `review-changes`.
9. **Dokumentieren** (kurz im Bericht): Symptom, Ursache (nachgewiesen/Hypothese), Lösung, Tests, Restrisiko. Dokumentationsbedarf und `PATCHNOTES.md` per direktem Auftrag an Documentation gemäss `coordination/README.md` übergeben (`tools\Auftrag.ps1 erstellen -An Documentation`, `CLAUDE.md` §10). Development ändert keine Dokumentationsdateien.
