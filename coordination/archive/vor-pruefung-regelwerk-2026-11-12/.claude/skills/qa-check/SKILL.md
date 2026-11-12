---
name: qa-check
description: Risikobasierte QA-Prüfung der aktuellen Änderungen in LeoSync über den Agent testing-qa-specialist (Risikokategorie, passende Tests, Bewertung, Testlücken, Empfehlungen), ohne Dateien zu ändern. Verwenden nach Änderungen oder manuell per /qa-check.
argument-hint: "[Fokus oder Dateien, optional]"
---

# QA-Check

Fokus (optional): $ARGUMENTS

Dieser Skill ändert **keine** Produktions- oder Testdateien und behebt keine Fehler.

1. **Änderungen ermitteln**: Dateien aus Auftrag/Gespräch oder per `LastWriteTime` (Git ist nicht vorhanden, daher können keine Git-Befehle ausgeführt werden).
2. **Risikokategorie** bestimmen (höchste zutreffende gilt):
   - **Niedrig**: UI-Anpassungen, Textänderungen, kleine isolierte Änderungen.
   - **Mittel**: ViewModels, Repositorys, Geschäftslogik, Netzwerkkomponenten.
   - **Hoch**: Sync-Engine, 3-Wege-Abgleich, Konfliktauflösung, Lösch-/Wiederherstellungslogik, Datenbankmigrationen, Nebenläufigkeit, datenverlustgefährdete Änderungen.
3. **Doku gezielt**: `docs/TESTING.md` Abschnitt 9/11 plus nur nötige Abschnitte aus `docs/SYNC-ENGINE.md`, `docs/DATA-MODEL.md`, `docs/ARCHITECTURE.md`.
4. **Agent `testing-qa-specialist`** per Agent-Tool einsetzen mit: Dateiliste, Risikokategorie, relevante Doku-Abschnitte, erlaubte Tests (Niedrig: meist nur Prüfung ohne Lauf oder betroffene Testklasse; Mittel: betroffene Testklassen; Hoch: `testDebugUnitTest` komplett, Migration nur mit Gerät/Emulator). Kein Release-Build und kein `build.bat`; `assembleDebug` nur in der Rolle QA & Testing (`CLAUDE.md` §1).

   **Berechtigungsabgrenzung:** Der Agent erhält ausschliesslich die Berechtigungen der aufrufenden Rolle. Ein Aufruf ausserhalb der Rolle QA & Testing verleiht ihm insbesondere keine zusätzlichen Build-, Emulator- oder Testumgebungsberechtigungen. Diese Prüfung ersetzt keinen eigenständigen QA-Auftrag, wenn dafür die erweiterten Berechtigungen der QA-Rolle erforderlich sind.
5. **Bei hohem Risiko zusätzlich** unabhängiges Review über Skill `review-changes` (Agent `code-reviewer`) einplanen bzw. ausführen.
6. **Bewerten**: Befunde nach CRITICAL/HIGH/MEDIUM/LOW/INFO und Art (nachgewiesener Fehler, wahrscheinlicher Fehler, fehlender Test, nicht überprüfbar); Fehlalarme gegenprüfen.
7. **Bericht** (kurz): Risikokategorie mit Begründung, ausgeführte und nicht ausgeführte Tests, Befunde, Testlücken, Risiken, nummerierte Handlungsempfehlungen. Keine Selbstreparatur.
