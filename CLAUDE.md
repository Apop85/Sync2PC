# CLAUDE.md – LeoSync (Projektordner: Sync2PC)

Android-App (Kotlin, Room, SMB) zur Synchronisierung Handy ⇄ PC. Gilt für alle vier Chat-Rollen (siehe 9). Sprache: Doku/Kommentare Deutsch, Schweizer Schreibweise (kein ß).

## 0. Rollen
Du hast eine der vier möglichen Rollen. Dein master Prompt für deine Rolle findest du hier:
Architecture & Complex Issues: .claude\roles\ARCHITECTURE-Specialist.md
Documentation: .claude\roles\DOCUMENTATION-Specialist.md
QA & Testing: .claude\roles\QA-Specialist.md
Development: .claude\roles\DEVELOPMENT-Specialist.md
Projektmanager: .claude\roles\Project-manager.md
Lese den für dich bestimmten Masterprompt einmalig ein. 

## 1. Verbindliche Inhaberregeln
Diese Regeln sind hier abschliessend festgehalten (`.claude/README.md` entfällt). Sie gelten verbindlich:
- **Nichts bauen** (`build.bat`, APK): macht der Inhaber, ausser er erlaubt es ausdrücklich.
- Jede Code-Änderung: PATCH in `version.properties` erhöhen. Neue Funktion = nächste Minor-Reihe.
- Sichtbare Texte nur in beide Sprachdateien (`res/values/strings_*.xml` EN, `res/values-de/` DE), nie fest im Code; danach `tools/make-translations.ps1`. DE: „Handy“, „Freigabe“, „Synchronisierung“; EN: „Mobile“, „Share“. Nie Whitelist/Blacklist.
- Jede Antwort, die eine Änderung abschliesst, endet mit der Versionsnummer in eigener Zeile (z. B. `Version 0.11.0`).
- Geheimnisse (Keystore) nie ins Repo; `.gitignore` aktuell halten; vor dem Anlegen einer Datei prüfen, ob sie existiert.
- Jeder Chat führt nur Aufgaben aus, die zu seiner Rolle passen; andere Aufgaben werden an den jeweiligen Spezialisten delegiert (siehe 10).
- Dokumentationen (inkl. `PATCHNOTES.md`) übernimmt der Dokumentationsspezialist; alle Dokumentationsaufgaben werden an ihn übergeben (siehe 11).

## 2. Architektur
- Bestehende Entscheidungen (`docs/DECISIONS.md`) einhalten; vorhandene Komponenten wiederverwenden.
- Keine unnötigen Refactorings, keine parallelen Implementierungen bestehender Funktionen.
- Änderungen lokal halten; Schnittstellen, DB-Schema und Dateiformate nicht ohne Not ändern.

## 3. Implementierung
- Betroffene Dateien zuerst lesen; keine Annahmen über ungelesenen Code.
- Fehlerbehandlung und Randfälle (Netzwerkabbruch, leere/verschwundene Ordner, Wiederanlauf, Nebenläufigkeit) bedenken.
- **Datenverlust hat höchste Priorität**: Löschen, Ersetzen, Papierkorb, Wiederherstellen, Schutzbremse besonders sorgfältig prüfen.
- Vorhandene Tests beachten und bei Verhaltensänderung anpassen/ergänzen. Neue DB-Version → `MigrationTest` ergänzen.

## 4. Dokumentation (zentrale Wissensbasis, gezielt laden)
| Datei | Inhalt |
|---|---|
| `docs/DECISIONS.md` | Entscheidungen und Begründungen |
| `docs/ARCHITECTURE.md` | Gesamtarchitektur, Komponenten |
| `docs/SYNC-ENGINE.md` | Synchronisationslogik |
| `docs/DATA-MODEL.md` | Datenmodell, Persistierung |
| `docs/TESTING.md` | Teststrategie, Qualitätssicherung |
| `PATCHNOTES.md` (Wurzel) | Änderungsprotokoll – **nicht standardmässig laden**, nur ergänzen |

Massgeblich sind die Namen mit **Bindestrich**; die Dateien mit Unterstrich (`SYNC_ENGINE.md`, `DATA_MODEL.md`) und `docs/NEW_*.md`, `docs/*_CHAT.md` sind Entwürfe/Einrichtungsvorlagen und werden nicht geladen. Redundanz vermeiden, nur betroffene Doku aktualisieren. Weitere Dateien: `build.md` (Build), `.claude/translations.md` (generiert).

## 5. Agents (`.claude/agents/`, alle Sonnet)
| Agent | Zuständigkeit | Schreibt? |
|---|---|---|
| `sync-engine-specialist` | Sync-Engine, 3-Wege-Abgleich, Konflikte, Löschen/Wiederherstellen, SMB, Wiederanlauf | ja |
| `database-specialist` | Room, Entities, DAOs, Repositorys, Migrationen, Integrität (nicht Sync-Logik) | ja |
| `code-reviewer` | Unabhängiges Review: Architektur, Fehler, Regressionen, Seiteneffekte | nein |
| `testing-qa-specialist` | Unabhängige QA: Tests ausführen, Abdeckung, Grenzfälle, Datenintegrität | nein |

Regeln: Hauptagent bleibt für die Gesamtaufgabe verantwortlich und prüft Agent-Ergebnisse kritisch. Keine Agents für triviale Änderungen, keine redundanten Analysen, keine parallelen Schreibzugriffe auf dieselben Dateien. Kritische Sync-Änderung → Sync-Spezialist; DB-Änderung → Database-Spezialist. Review und QA sind verschieden; bei besonders kritischen Änderungen beide, unabhängig voneinander. Kontext knapp mitgeben.

## 6. Skills (`.claude/skills/`, Ablauf steht im jeweiligen `SKILL.md`)
| Zweck | Skill |
|---|---|
| Neue Funktion/Änderung | `implement-feature` |
| Fehlersuche | `investigate-bug` |
| Codeprüfung | `review-changes` (nutzt `code-reviewer`) |
| Funktionale QA | `qa-check` (nutzt `testing-qa-specialist`) |
| Gezielte Tests | `run-targeted-tests` (nach `docs/TESTING.md`) |
| Dokumentation | `update-docs` |

Keiner der Skills ist auf manuellen Aufruf beschränkt (kein `disable-model-invocation`); einsetzen, wenn der Ablauf sinnvoll ist. Skill-Inhalte nicht hier duplizieren.

## 7. Abläufe
- **Klein**: Dateien lesen → ändern → gezielte Tests → kurzer Bericht. Keine Agents.
- **Normal**: `implement-feature` → Komponenten prüfen → Doku gezielt laden → bei Bedarf Spezialagent → implementieren → Tests → Doku.
- **Fehler**: `investigate-bug` → Ursache nachweisen → beheben → Regressionstests → Ergebnis dokumentieren.
- **Kritisch** (3-Wege-Abgleich, Konflikte, Löschen/Wiederherstellen, Migrationen, Nebenläufigkeit, möglicher Datenverlust): Risiken analysieren → Doku laden → Spezialagent → implementieren → gezielte Tests → `review-changes` → `qa-check` → Restrisiken dokumentieren → Doku aktualisieren. Nur tatsächlich nötige Schritte.
- Ein erfolgreicher Build ersetzt weder Review noch QA.

## 8. Token, Modelle, Git
- Nur relevante Quelldateien und Doku laden; gezielte Tests statt Volltests; keine Chatverläufe rekonstruieren.
- Sonnet als Standard; Opus nur für aussergewöhnlich komplexe Architektur-/Analyseaufgaben. Qualität und Datensicherheit gehen vor.
- **Git**: Der Ordner ist derzeit **kein Git-Repository** (Stand 2026-11-08, `.git` fehlt) – ohne Repository gilt: vor Änderungen an gemeinsam genutzten Dateien den aktuellen Dateistand lesen, nichts überschreiben, was ein anderer Chat geändert hat. Mit Repository: Status vor grösseren Änderungen prüfen, fremde Änderungen schützen. **Nie selbst committen oder pushen.** Gemeinsame Dateien nicht gleichzeitig bearbeiten.

## 9. Vier Chat-Rollen (dieselbe CLAUDE.md, dieselben Agents/Skills)
| Rolle | Modell / Effort | Auftrag |
|---|---|---|
| Development | Sonnet / Medium | Entwicklung, Fehlerbehebung, Integration, gezielte Tests, Doku-Übergaben |
| Architecture & Complex Issues | Opus / High | Analysen, Architekturentscheidungen, Alternativen, technische Vorgaben; grössere Implementierung erst nach Freigabe des Benutzers |
| QA & Testing | Sonnet / Medium | Unabhängige Prüfung, Abdeckung, Regressionen, Risikobewertung; **ändert keine Produktions- oder Testdateien** |
| Documentation | Sonnet / Low | Pflege der Doku und Konsistenz; Aussagen müssen zur Implementierung passen, nichts erfinden |

Modell und Effort stellt der Benutzer pro Chat ein; die Rolle nennt der Benutzer zu Beginn des Chats.

## 10. Koordination: `coordination/TASK-HANDOVER.md`
Verbindliches Übergabeprotokoll zwischen den Rollen (keine automatische Kommunikation). Matrix, Statusmodell, Vorlage und Ablauf stehen **dort** und gelten verbindlich.
- Übergaben nur laut Matrix: Development → Architecture, QA & Testing, Documentation · Architecture → Development, Documentation · QA & Testing → Development, Documentation · Documentation → Development, Architecture.
- Übergabepflicht, wenn die Aufgabe ausserhalb der eigenen Rolle liegt oder ein eigenständiges Ergebnis einer anderen Rolle nötig ist. Kleine Aufgaben der eigenen Rolle ohne Übergabe erledigen; interne Agents ersetzen keine Übergabe.
- Vor Übergabe Datei lesen und Dubletten prüfen; Auftrag mit Task-ID `SYNC-YYYYMMDD-NNN`, Status `OPEN`, im eigenen Chat mitteilen.
- Empfänger bestätigt die Übernahme ausdrücklich (→ `IN_PROGRESS`), setzt bei Lücken `BLOCKED`, `COMPLETED` erst nach dokumentierter Arbeit. Nur an die eigene Rolle adressierte, offene Aufträge lesen; erledigte nicht neu analysieren.
- Vor Änderungen Datei und Dateistand erneut lesen. Der übergebende Chat bleibt für Integration und Bewertung verantwortlich. Keine Chatverläufe oder Quellcodekopien eintragen.

## 11. Doku-Übergaben
Entscheidung → `DECISIONS.md` · Architektur → `ARCHITECTURE.md` · Sync → `SYNC-ENGINE.md` · Datenmodell → `DATA-MODEL.md` · Tests → `TESTING.md` · Release → `PATCHNOTES.md`. Selbst ändern oder an Documentation übergeben (mit technischem Kontext und betroffenen Dateien).

## 12. Abschlussbericht (kompakt)
Änderungen · betroffene Dateien · Tests und Ergebnisse · Doku-Änderungen · Übergaben · offene Punkte/Risiken · Versionsnummer.
