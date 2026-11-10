---
name: implement-feature
description: Standardablauf zum Implementieren einer Funktion oder Änderung in LeoSync (Analyse, Doku gezielt laden, Plan, Umsetzung, gezielte Tests, optional Review, Doku). Verwenden bei Aufträgen, die Code ändern oder ergänzen.
---

# Funktion implementieren

Ablauf für Aufgabe: $ARGUMENTS

1. **Analysieren**: Ziel, Akzeptanzkriterien, Datenverlustrisiko klären. Unklares als nummerierte Rückfragen stellen.
2. **Komponenten bestimmen**: betroffene Dateien/Schichten unter `app/src/main/java/com/sync2pc/app/` per Grep/Glob finden.
3. **Doku gezielt laden** (per Grep nur relevante Abschnitte, nie ganze Dateien; `docs/DECISIONS.md` nach DEC-ID/Stichwort): Sync -> `docs/SYNC-ENGINE.md`; Datenbank -> `docs/DATA-MODEL.md`; Struktur -> `docs/ARCHITECTURE.md`; Tests -> `docs/TESTING.md`. Dateien mit Unterstrich in `docs/` sind Auftragstexte, keine Doku.
4. **Bestehende Implementierung lesen**, bevor etwas geändert wird.
5. **Änderungsplan** (kurz: Dateien, Schritte, Tests). Bei grösseren oder datenverlustnahen Änderungen vor der Umsetzung mit dem Inhaber abstimmen.
6. **Umsetzung**:
   - Sync-Engine (Abgleich, Konflikte, Löschen, SMB, Worker): Agent `sync-engine-specialist` per Agent-Tool (`subagent_type`).
   - Room/Entities/DAOs/Migrationen: Agent `database-specialist`.
   - Sonst selbst umsetzen. Kleine Aufgaben (UI-Text, isolierte Änderung): direkt selbst, ohne Agent, Schritte 5 und 9 verkürzen.
   - Bei Aufträgen, die beide Bereiche betreffen: nacheinander, nie parallel auf dieselben Dateien.
7. **Gezielte Tests**: Skill `run-targeted-tests`. Nicht bauen (`assemble*`, `build.bat`): Der Inhaber baut selbst.
8. **Review** bei mittlerem/hohem Risiko (Sync-Engine, Löschlogik, Migration, Nebenläufigkeit): Skill `review-changes` (Agent `code-reviewer`). Bei hohem Risiko zusätzlich `qa-check`.
9. **Version**: PATCH in `version.properties` erhöht der Hauptagent (`CLAUDE.md` §1); die Spezialagenten tun das nicht.
10. **Folgeaufträge** (`coordination/README.md` §7): Doku und `PATCHNOTES.md`-Eintrag (Version, Änderungen, Dateien) an Documentation – Development ändert keine Doku-Dateien; bei Bedarf QA und UX-Review.
11. **Abschlussbericht** (kurz): Änderungen (Dateien), Tests (gelaufen/nicht gelaufen), Review-Ergebnis, Folgeaufträge, Risiken, offene nummerierte Fragen.

Keine Commits oder Pushes.
