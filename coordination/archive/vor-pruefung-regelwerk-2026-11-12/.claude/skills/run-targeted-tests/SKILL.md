---
name: run-targeted-tests
description: Wählt und führt gezielt die zur Änderung passenden Tests von LeoSync aus (anhand docs/TESTING.md) und dokumentiert Ergebnisse sowie nicht ausgeführte Tests. Verwenden nach Codeänderungen statt vollständiger Testläufe.
---

# Gezielte Tests

Änderung/Bereich: $ARGUMENTS

1. **Geänderte Dateien** aus Auftrag/Gespräch oder per `LastWriteTime` ermitteln (Git ist nicht vorhanden, daher können keine Git-Befehle ausgeführt werden).
2. **Komponenten** bestimmen (Logikklasse, Entity/DB, Engine/SMB/Worker, Texte/UI, Gradle/Abhängigkeiten).
3. **`docs/TESTING.md` gezielt lesen**: Abschnitt 9 (Testauswahl), 11 (Empfehlungen je Aufgabe), bei Bedarf 4–7 für konkrete Testklassen.
4. **Auswahl** (Befehle aus dem Projektwurzelordner):
   - einzelne Klasse: `gradlew.bat testDebugUnitTest --tests "com.sync2pc.app.<Klasse>"`
   - alle JVM-Tests (`gradlew.bat testDebugUnitTest`) nur bei Sync-Engine, Textänderungen in `strings_*.xml`, Datenmodell oder wenn TESTING.md es verlangt
   - `gradlew.bat connectedDebugAndroidTest` (Migration) nur mit Gerät/Emulator und wenn Entity/Schema geändert wurde; ohne Gerät ausdrücklich als **nicht ausgeführt** melden
   - **Nie** `assembleRelease` oder `build.bat`: Der Inhaber baut selbst. `assembleDebug` (und `lintDebug`) nur in der Rolle QA & Testing (`CLAUDE.md` §1); alle anderen Rollen bauen nicht.
5. **Ausführen** nur das Nötige; bei Fehlschlag Ausgabe lesen und Ursache nennen, nicht raten, nichts reparieren (ausser der Auftrag verlangt es).
6. **Bericht**: ausgeführt (Befehl, Anzahl, Ergebnis), nicht ausgeführt (mit Grund), manuelle Prüfungen laut TESTING.md Abschnitt 8, Testlücken. Keine Ergebnisse erfinden.
