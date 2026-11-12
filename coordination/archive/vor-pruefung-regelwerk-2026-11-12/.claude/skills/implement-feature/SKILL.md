---
name: implement-feature
description: Standardablauf zum Implementieren einer Funktion oder Änderung in LeoSync (Analyse, Doku gezielt laden, Plan, Umsetzung, gezielte Tests, optional Review, Doku). Verwenden bei Aufträgen, die Code ändern oder ergänzen.
---

# Funktion implementieren

Ablauf für Aufgabe: $ARGUMENTS

**Rollenberechtigung:** Dieser Skill ist für die Rolle Development vorgesehen. Andere Rollen verwenden ihn nicht zur eigenständigen Implementierung. Für Architecture gelten weiterhin die ausdrücklichen Umsetzungsberechtigungen gemäss `CLAUDE.md` und Architecture-Rollenprompt.

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
7. **Gezielte Tests**: Skill `run-targeted-tests`. Development erstellt keine Builds (`assemble*`, `build.bat`); der Inhaber baut selbst. Die eigenständige QA-Rolle darf Debug- und Test-Builds gemäss ihren Berechtigungen in `CLAUDE.md` erstellen.
8. **Review** bei mittlerem/hohem Risiko (Sync-Engine, Löschlogik, Migration, Nebenläufigkeit): Skill `review-changes` (Agent `code-reviewer`). Bei hohem Risiko zusätzlich `qa-check`.
9. **Version und Folgeaufträge**:
   - PATCH in `version.properties` erhöht der Hauptagent gemäss `CLAUDE.md` §1; Spezialagenten tun das nicht.
   - Dokumentationsbedarf und `PATCHNOTES.md`-Eintrag (Version, Änderungen, Dateien) gemäss `coordination/README.md` §7 direkt an Documentation übergeben (Folgeauftrag `tools\Auftrag.ps1 erstellen -An Documentation`, `CLAUDE.md` §10).
   - Development ändert keine Dokumentationsdateien.
   - Bei Bedarf QA- und UX-Review als direkte Folgeaufträge einplanen (ebenfalls `erstellen`).
10. **Abschlussbericht** (kurz): Änderungen (Dateien), Tests (gelaufen/nicht gelaufen), Review-Ergebnis, Folgeaufträge, Risiken, offene nummerierte Fragen.

Git ist nicht vorhanden, daher können keine Git-Befehle ausgeführt werden (auch keine Commits oder Pushes).
