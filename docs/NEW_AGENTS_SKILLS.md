6. Sonnet Medium

Sync2PC – Einrichtung des spezialisierten Claude-Code-Agent- und Skill-Systems
1. Zielsetzung

Erstelle für das Projekt Sync2PC ein vollständiges, effizientes und langfristig wartbares System aus vier spezialisierten Claude-Code-Agents und sechs Skills.

Die zentrale CLAUDE.md wird ausdrücklich erst im nächsten Einrichtungsschritt erstellt.

Ziele:

Geringer Tokenverbrauch.
Klare Zuständigkeiten.
Effiziente Agent-Delegation.
Wiederverwendbare Entwicklungsabläufe.
Unabhängiges Code-Review und QA.
Vermeidung von Regressionen und Datenverlust.
Langfristige Wartbarkeit.
2. Bestandsaufnahme

Untersuche vor jeder Änderung:

README.md
docs/DECISIONS.md
docs/ARCHITECTURE.md
docs/DATA-MODEL.md
docs/SYNC-ENGINE.md
docs/TESTING.md
.claude/
.claude/agents/
.claude/skills/
.claude/rules/, sofern vorhanden
Vorhandene Claude-Code-Einstellungen und Berechtigungen.

Prüfe außerdem:

Installierte Claude-Code-Version.
Unterstützte Agent- und Skill-Konfigurationen.
Gültige YAML-Frontmatter-Felder.
Modell- und Effort-Einstellungen.
Verfügbare Delegationsmöglichkeiten.
Berechtigungs- und Sandbox-Funktionen.
Git-Status und bestehende Änderungen.

Verwende ausschließlich tatsächlich vorhandene Pfade und Komponenten. Erfinde keine Dokumentationen, Agents oder Skills.

Vorhandene Änderungen dürfen nicht überschrieben werden.

3. Agents

Erstelle beziehungsweise vervollständige genau diese vier Agents:

sync-engine-specialist

Pfad: .claude/agents/sync-engine-specialist.md

Modell: Sonnet
Effort: High, sofern unterstützt.

Zuständigkeiten:

Synchronisationsengine.
3-Wege-Abgleich.
Konflikterkennung und Konfliktauflösung.
Lösch- und Wiederherstellungslogik.
Dateiübertragungen und SMB.
Synchronisationszustände.
Nebenläufigkeit und Fehlerbehandlung.
Wiederanlaufverhalten.

Wissensgrundlage:

Primär: docs/SYNC-ENGINE.md
Ergänzend: docs/ARCHITECTURE.md und docs/DECISIONS.md.
DATA-MODEL.md nur bei relevanten Datenbankbezügen.

Der Agent darf implementieren, wenn sein konkreter Auftrag dies vorsieht. Keine unnötigen Refactorings, Commits oder Pushes.

database-specialist

Pfad: .claude/agents/database-specialist.md

Modell: Sonnet
Effort: Medium.

Zuständigkeiten:

Room-Datenbank.
Entities und DAOs.
Repositorys.
Migrationen.
Datenintegrität.
Transaktionen.
Persistierung und Beziehungen.

Wissensgrundlage:

Primär: docs/DATA-MODEL.md
Ergänzend: docs/ARCHITECTURE.md und docs/DECISIONS.md.
SYNC-ENGINE.md bei synchronisationsrelevanten Daten.

Bei Schemaänderungen müssen Migrationen und Datenbestand berücksichtigt werden. Keine eigenständigen Commits oder Pushes.

code-reviewer

Pfad: .claude/agents/code-reviewer.md

Modell: Sonnet
Effort: Medium.

Zuständigkeiten:

Implementierungsqualität.
Architekturkonformität.
Funktionale Fehler.
Regressionen.
Nebenläufigkeit.
Fehlerbehandlung.
Datenverlustrisiken.
Unvollständige Implementierungen.
Fehlende Tests.

Der Agent arbeitet grundsätzlich lesend.

Er darf keine Produktionsdateien oder Testdateien verändern und keine Refactorings durchführen.

Er untersucht den tatsächlichen Git-Diff und die tatsächlichen Implementierungen unabhängig von der Zusammenfassung des implementierenden Agents.

Ergebnisse nach Schweregrad sortieren und nur nachvollziehbare Probleme melden.

testing-qa-specialist

Pfad: .claude/agents/testing-qa-specialist.md

Modell: Sonnet
Effort: High, sofern unterstützt.

Zuständigkeiten:

Funktionale Qualitätssicherung.
Testabdeckung.
Regressionen.
Grenzfälle.
Datenintegrität.
Synchronisationslogik.
Datenbankqualität.
Netzwerk- und SMB-Fehler.
Android-Lifecycle und WorkManager.
Wiederanlaufverhalten.
Datenverlustrisiken.

Primäre Wissensgrundlage: docs/TESTING.md.

Ergänzend werden nur die für die konkrete Aufgabe notwendigen Abschnitte aus den anderen technischen Dokumentationen geladen.

Der Agent darf vorhandene Tests und Builds ausführen, aber keine Produktions- oder Testdateien eigenständig verändern.

Er darf keine neuen Tests implementieren.

Klassifizierung:

CRITICAL
HIGH
MEDIUM
LOW
INFO

Unterscheide zwischen nachgewiesenem Fehler, wahrscheinlichem Fehler, fehlendem Test und nicht überprüfbarem Sachverhalt.

Abschlussbericht mit untersuchten Komponenten, ausgeführten Tests, Ergebnissen, Testlücken und Risiken.

Berechtigungen: Prüfe, ob eine echte technische Schreibsperre bei gleichzeitig möglicher Testausführung gewährleistet werden kann. Allgemeine Bash-Berechtigungen stellen keine Schreibsperre dar. Dokumentiere verbleibende Einschränkungen ausdrücklich.

4. Skills

Erstelle beziehungsweise vervollständige genau diese sechs Skills:

implement-feature

Pfad: .claude/skills/implement-feature/SKILL.md

Standardablauf:

Aufgabenstellung analysieren.
Betroffene Komponenten identifizieren.
Relevante Dokumentation laden.
Bestehende Implementierung untersuchen.
Änderungsplan erstellen.
Bei Bedarf Spezialagent einsetzen.
Implementierung durchführen.
Gezielte Tests ausführen.
Bei Bedarf Code-Review durchführen.
Dokumentation aktualisieren.
Abschlussbericht erstellen.

Agent-Auswahl:

Synchronisation: sync-engine-specialist.
Datenbank: database-specialist.
Code-Review: code-reviewer.

Kleine Aufgaben erhalten einen verkürzten Ablauf.

investigate-bug

Pfad: .claude/skills/investigate-bug/SKILL.md

Standardablauf:

Fehlerbeschreibung verstehen.
Betroffene Komponenten identifizieren.
Relevante Dokumentation laden.
Fehlerursache untersuchen.
Ursache nachvollziehbar nachweisen.
Lösung vorschlagen oder implementieren.
Regressionstests durchführen.
Auswirkungen prüfen.
Ergebnisse dokumentieren.

Bei komplexen Synchronisationsproblemen sync-engine-specialist einsetzen. Bei Datenbankproblemen database-specialist einsetzen.

Keine vorschnellen Änderungen ohne nachvollziehbare Fehlerursache.

review-changes

Pfad: .claude/skills/review-changes/SKILL.md

Aufgabe:

Git-Diff untersuchen.
Betroffene Komponenten identifizieren.
Architekturkonformität prüfen.
Kritische Seiteneffekte untersuchen.
Vorhandene Tests berücksichtigen.
code-reviewer einsetzen.
Ergebnisse nach Schweregrad zusammenfassen.

Keine eigenen Codeänderungen.

update-docs

Pfad: .claude/skills/update-docs/SKILL.md

Dokumentationszuordnung:

Entscheidungen: DECISIONS.md.
Architektur: ARCHITECTURE.md.
Synchronisation: SYNC-ENGINE.md.
Datenbank: DATA-MODEL.md.
Tests: TESTING.md.
Releases: PATCHNOTES.md.

Nur tatsächlich notwendige Änderungen durchführen. Bestehende Strukturen und DEC-IDs erhalten. PATCHNOTES.md nicht bei jeder gewöhnlichen Entwicklungsaufgabe aktualisieren.

run-targeted-tests

Pfad: .claude/skills/run-targeted-tests/SKILL.md

Aufgabe:

Geänderte Dateien identifizieren.
Betroffene Komponenten bestimmen.
TESTING.md gezielt konsultieren.
Passende Tests auswählen.
Nur notwendige Tests ausführen.
Ergebnisse und nicht ausgeführte Tests dokumentieren.

Keine unnötigen vollständigen Testläufe. Keine Testergebnisse erfinden.

qa-check

Pfad: .claude/skills/qa-check/SKILL.md

Aufgabe:

Git-Diff untersuchen.
Risikokategorie bestimmen.
Relevante Dokumentation laden.
testing-qa-specialist einsetzen.
Geeignete Tests bestimmen und ausführen lassen.
Ergebnisse bewerten.
Testlücken und Risiken dokumentieren.
Handlungsempfehlungen erstellen.

Risikokategorien:

Niedrig:

UI-Anpassungen.
Textänderungen.
Kleine isolierte Änderungen.

Mittel:

ViewModels.
Repositorys.
Geschäftslogik.
Netzwerkkomponenten.

Hoch:

Synchronisationsengine.
3-Wege-Abgleich.
Konfliktauflösung.
Lösch- und Wiederherstellungslogik.
Datenbankmigrationen.
Nebenläufigkeit.
Datenverlustgefährdete Änderungen.

Bei hohem Risiko zusätzlich ein unabhängiges Code-Review vorsehen.

Der Skill darf keine Produktions- oder Testdateien verändern und keine Fehler selbstständig beheben.

5. Skill- und Agent-Zusammenspiel

Grundprinzip:

Skills definieren Arbeitsabläufe.
Agents übernehmen spezialisierte Aufgaben.
Technische Dokumentationen bilden die Wissensbasis.
Die zentrale CLAUDE.md wird im nächsten Schritt erstellt.

Die Skills sollen über die tatsächlich unterstützte Claude-Code-Skill-Funktion aufrufbar sein.

Der Hauptagent soll Skills bei passender Aufgabenstellung verwenden, ohne unnötige Mehrfachaufrufe.

Der Agent-Aufruf erfolgt über die tatsächlich verfügbare Agent-Delegation.

Erfinde keine Toolnamen oder Aufrufmechanismen.

Manuelle und automatische Aufrufbarkeit

Alle sechs Skills sollen für den Hauptagenten verwendbar sein.

qa-check muss zusätzlich über /qa-check manuell aufrufbar sein.

Prüfe die tatsächlich unterstützten Frontmatter-Optionen. Verwende keine nicht unterstützten Felder.

Verwechsle manuelle Aufrufbarkeit nicht mit einer Beschränkung auf manuelle Aufrufe.

6. Tokenoptimierung
Nur notwendige Dokumentationen laden.
Agents nur bei tatsächlichem Mehrwert einsetzen.
Keine vollständigen Dokumentationen pauschal übergeben.
Kurze strukturierte Agent-Ergebnisse bevorzugen.
Keine redundanten Analysen.
Keine unnötigen parallelen Schreibzugriffe.
Sonnet als Standardmodell.
Opus nur für außergewöhnlich komplexe Architekturfragen.

Qualität und Datensicherheit haben immer Vorrang.

7. Technische Validierung

Unterscheide zwischen:

Syntaktischer Validierung.
Erkennung durch Claude Code.
Tatsächlichem Funktionstest.

Prüfe:

Vier Agents vorhanden.
Sechs Skills vorhanden.
Gültiges Frontmatter.
Gültige Modellangaben.
Effort-Einstellungen, sofern unterstützt.
Agent-Beschreibungen.
Skill-Beschreibungen.
Manuelle Aufrufbarkeit von /qa-check.
Berechtigungen.
Delegationsmöglichkeiten.
Korrekte Dokumentationspfade.
Keine widersprüchlichen Zuständigkeiten.

Behaupte keine erfolgreiche Funktion, die nicht tatsächlich getestet wurde.

8. Arbeitsablauf

Phase 1: Bestandsaufnahme und Git-Status.

Phase 2: Kurzer Umsetzungsplan.

Phase 3: Agents erstellen beziehungsweise vervollständigen.

Phase 4: Skills erstellen beziehungsweise vervollständigen.

Phase 5: Konsistenzprüfung.

Phase 6: Technische Validierung.

Phase 7: Git-Diff und Abschlusskontrolle.

9. Einschränkungen

Keine Programmdateien verändern.

Keine technischen Dokumentationen verändern.

Keine zentrale CLAUDE.md vorwegnehmen.

Keine unnötigen Rules, Hooks oder MCP-Integrationen hinzufügen.

Keine externen Abhängigkeiten installieren.

Keine bestehenden Änderungen überschreiben.

Keine Commits oder Pushes.

Erlaubte Änderungen:

.claude/agents/
.claude/skills/
Weitere .claude/-Konfigurationen nur, wenn technisch zwingend erforderlich und begründet.
10. Abschlussbericht

Kompakt berichten:

Anzahl und Namen der Agents.
Anzahl und Namen der Skills.
Modelle und Effort.
Berechtigungsprüfung.
Aufrufmöglichkeiten.
Ergebnis der Konfigurationsprüfung.
Ergebnis der Erkennungsprüfung.
Geänderte Dateien.
Offene Punkte und Risiken.

Keine vollständigen Agent- oder Skill-Dateien im Chat wiederholen.

Wichtig: Die zentrale CLAUDE.md und die vier Chat-Rollen werden ausdrücklich erst im nächsten Schritt eingerichtet.