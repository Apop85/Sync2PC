# CLAUDE.md – LeoSync (Projektordner: Sync2PC)

Android-App (Kotlin, Room, SMB) zur Synchronisierung Handy ⇄ PC. Gilt für alle sechs Chat-Rollen (siehe 9). Sprache: Doku/Kommentare Deutsch, Schweizer Schreibweise (kein ß).

## 0. Rollen und Regelhierarchie
Jeder Chat hat genau eine der sechs Rollen (siehe 9). **Nach Chatstart oder `/clear`** ruft der Benutzer den Rollen-Skill auf (`/rolle-pm`, `/rolle-architecture`, `/rolle-development`, `/rolle-qa`, `/rolle-documentation`, `/rolle-ux`) oder nennt die Rolle: dann einmalig den Masterprompt (Tabelle in 9) und den eigenen Posteingang lesen (`coordination/README.md` §4). Ohne Rollenangabe zuerst nach der Rolle fragen.

**Regelhierarchie:** (1) diese `CLAUDE.md` → (2) `coordination/README.md` (Koordinationsrichtlinie) → (3) Rollenprompt → (4) Task-Datei/Übergabe. Bei Widerspruch gilt die höhere Stufe; Rollenprompts schaffen keine Ausnahmen von den Berechtigungen in 1, Task-Dateien heben keine Rollengrenzen auf. Nicht lösbare Widersprüche dem Benutzer vorlegen.

## 1. Verbindliche Inhaberregeln
Diese Regeln sind hier abschliessend festgehalten (`.claude/README.md` gibt es nicht mehr). Sie gelten verbindlich für alle Rollen:
- **Nichts bauen** (`build.bat`, `assemble*`, APK/Bundle): macht der Inhaber, ausser er erlaubt es ausdrücklich pro Lauf (DEC-184). Gezielte Tests (`gradlew.bat testDebugUnitTest …`, Skill `run-targeted-tests`) sind kein Bauen. **Ausnahme nur für die Rolle QA & Testing (Benutzerauftrag 2026-11-10):** QA darf selbstständig Debug-/Test-Builds mit den vorhandenen Gradle-Tasks erstellen (`gradlew.bat assembleDebug`, `testDebugUnitTest`, `connectedDebugAndroidTest`, `lintDebug`), Debug-APKs auf Emulatoren und vom Inhaber ausdrücklich freigegebenen Testgeräten installieren und Builds nach Fehlerbehebungen wiederholen, ohne Einzelerlaubnis. Nicht erlaubt bleiben für QA: `build.bat` (erhöht Version, schreibt `releases\`), `assembleRelease`/`bundle*`, Signierung, Veröffentlichung, jede Änderung an Code, Gradle- und Build-Dateien. Entwicklung, UX, Documentation, Architecture und PM erhalten dadurch **keine** Build-Rechte.
- Jede Code-Änderung: PATCH in `version.properties` erhöhen (durch die Rolle, die den Code ändert). Neue Funktion = nächste Minor-Reihe.
- Sichtbare Texte nur in beide Sprachdateien (`res/values/strings_*.xml` EN, `res/values-de/` DE), nie fest im Code; danach `tools/make-translations.ps1`. DE: „Handy“, „Freigabe“, „Synchronisierung“; EN: „Mobile“, „Share“. Nie Whitelist/Blacklist.
- Jede Antwort, die eine Änderung abschliesst, endet mit der Versionsnummer in eigener Zeile (z. B. `Version 0.11.0`).
- **Kodierung:** Alle Textdateien sind UTF-8 ohne BOM. In PowerShell 5.1 nie `Set-Content`, `Out-File` oder `Add-Content` ohne `-Encoding` verwenden, und auch `-Encoding utf8` schreibt dort ein BOM. Stattdessen `[IO.File]::WriteAllText(pfad, text, [Text.UTF8Encoding]::new($false))` nutzen. Beim Lesen immer `Get-Content -Encoding UTF8` angeben, sonst entstehen kaputte Umlaute.
- **PowerShell-Alias-Falle (Datenverlust, mehrfach vorgekommen):** Eigene Funktionen und Variablen in PowerShell nie nach eingebauten Aliasen benennen. Besonders gefährlich: `rd`, `ri`, `del`, `rm`, `erase` (alle = `Remove-Item`, löschen Dateien/Ordner), ausserdem `r` (`Invoke-History`), `sc`, `sl`, `gc`, `ac`, `cp`, `mv`, `cat`, `echo`. Hilfsfunktionen immer mit Verb-Nomen- oder eindeutigen Namen (z. B. `Read-FileText`, `Rep-Text`) benennen; vor dem Aufruf einer neuen Funktion mit `Get-Command <Name>` prüfen, dass sie nicht auf einen Alias oder ein Cmdlet trifft. Vor jedem Löschen und Überschreiben von Dateien den Inhalt gesichert haben oder lesen; zuerst lesen und prüfen, dann schreiben, nie `Remove-Item` nach einem ungeprüften Schreibvorgang.
- Geheimnisse (Keystore) nie ins Repo; `.gitignore` aktuell halten; vor dem Anlegen einer Datei prüfen, ob sie existiert. Echte Zugangsdaten und produktive Passwörter gehören nie in Repo, Rollenprompts oder Doku. **Ausnahme (Benutzerauftrag 2026-11-10):** `qa-test-1` ist ein bewusst **öffentliches Testpasswort** und kein Geheimnis. Es gehört ausschliesslich zur lokalen SMB-Testumgebung (Benutzer `qa`, `tools\qa-smb-testshare.ps1`), darf in Rollenprompts und Testdokumentation stehen und darf nie für produktive Konten oder externe Systeme verwendet werden.
- **Nie selbst committen oder pushen** (alle Rollen).
- Jeder Chat führt nur Aufgaben seiner Rolle aus und übernimmt keine Berechtigungen anderer Rollen; Fremdes wird direkt an die zuständige Rolle übergeben (siehe 10).
- **Benutzerfreigabe** (nur der Benutzer, keine Rolle ersetzt sie) vor: grösseren Architektur- und wesentlichen Designänderungen, Änderungen an sicherheits- oder datenverlustkritischen Abläufen, destruktiven Aktionen ausserhalb der Testdaten, Builds und Änderungen am Regelwerk (diese Datei, `coordination/README.md`, `.claude/roles|skills|agents/`). Ein Vorschlag ist keine Freigabe. Details: `coordination/README.md` §8.
- **Emulator:** UX und QA starten ihn selbstständig und bedienen ihn für ihre Prüfaufträge ohne Einzelerlaubnis; sie dürfen in der App alle dafür nötigen Einstellungen vornehmen und entfernen (DEC-212). Werden Profile angelegt, bearbeitet oder entfernt, legen sie **vorher einen Export** an. Nach den Tests **löschen sie alle von ihnen angelegten Testprofile** und stellen den ursprünglichen Zustand **per Profilexport (Import)** wieder her (Benutzerauftrag 2026-11-10). Dateioperationen (erstellen, ändern, verschieben, löschen usw.) sind nur in den Testordnern `D:\testsync` und `X:\testsync` erlaubt, wo die Shares der Testprofile liegen. **Zusätzlich nur für QA:** der lokale SMB-Testbereich `.build/qa-smb-share` (Testdateien erstellen, ändern, überschreiben, löschen; Testdaten vorbereiten und zurücksetzen) samt den Betriebsdateien des SMB-Testservers `.build/qa-smb-testshare.pid` und `.build/qa-smb-testshare.log` (anlegen, ändern, löschen) sowie das Python-venv des Testservers unter `%LOCALAPPDATA%\LeoSyncTestShare` (einrichten, aktualisieren, zurücksetzen/löschen; Benutzerauftrag 2026-11-10). Das ist keine allgemeine Schreibberechtigung für `.build/` oder `%LOCALAPPDATA%`; alles übrige unter `.build/` (u. a. `last-build.log`) und alle Build-Konfigurationen bleiben gesperrt. Andere Rollen: Emulator nur mit Erlaubnis pro Lauf (DEC-184). Profile mit Zugangsdaten richtet der Inhaber ein; Builds nur mit Erlaubnis pro Lauf (Ausnahme: Debug-/Test-Builds durch QA, siehe oben). Physische Geräte: QA nur solche, die der Inhaber ausdrücklich als Testgerät benennt; sein persönliches Handy, seine produktiven Profile und Daten nie. Manuelle Benutzertests bleiben beim Inhaber: QA bereitet sie vor und nimmt Ergebnisse entgegen, markiert sie aber nie selbst als bestanden.

**Schreibrechte je Rolle** (Lesen ist allen erlaubt; `coordination/` bearbeiten alle Rollen nach `coordination/README.md`):
| Rolle | Darf bearbeiten | Darf nicht bearbeiten |
|---|---|---|
| Project Manager | nur `coordination/` | alle Projektdateien |
| Architecture | Code nur auf ausdrücklichen Umsetzungsauftrag des Benutzers; sonst nichts ausser `coordination/` | Dokumentation |
| Development | Code, Tests, Ressourcen (`app/`), `version.properties`, `tools/`, Gradle-Dateien, `.claude/translations.md` (nur per Skript) | Dokumentation |
| QA & Testing | Screenshots unter `.claude/AppScreenshots/`; Dateien in den Testordnern `D:\testsync`, `X:\testsync`; Testdaten im SMB-Testbereich `.build/qa-smb-share` sowie `.build/qa-smb-testshare.pid` und `.build/qa-smb-testshare.log` sowie `%LOCALAPPDATA%\LeoSyncTestShare` (nur diese, siehe §1 Emulator); Build-Ausgaben (`app/build/`, `.gradle/`) durch Debug-/Test-Builds nach §1 | Produktions-, Test- und Doku-Dateien, Gradle-/Build-Dateien und -Skripte, `version.properties`, übrige Dateien unter `.build/` |
| Documentation | `docs/` (ohne Dateien mit Unterstrich), `docs/hilfe/`, `README.md`, `PATCHNOTES.md`, `build.md`, `PRIVACY_*.md` | Code, Tests, Ressourcen |
| UX-Specialist | Screenshots und Designvorschauen unter `.claude/AppScreenshots/`; alle Dateien unter `.claude/ux/` (u. a. `navigation.md`); Dateien in den Testordnern `D:\testsync`, `X:\testsync` | Code, Layouts/Compose, Ressourcen inkl. Texte, Gradle, Dokumentation |

Regelwerk-Dateien (`CLAUDE.md`, `coordination/README.md`, `.claude/roles|skills|agents/`) ändert nur die Rolle, der der Benutzer dies ausdrücklich aufträgt.

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
| `docs/DECISIONS.md` | Entscheidungen und Begründungen (§10: Benutzeroberfläche und UX) |
| `docs/ARCHITECTURE.md` | Gesamtarchitektur, Komponenten |
| `docs/SYNC-ENGINE.md` | Synchronisationslogik |
| `docs/DATA-MODEL.md` | Datenmodell, Persistierung |
| `docs/TESTING.md` | Teststrategie, Qualitätssicherung |
| `PATCHNOTES.md` (Wurzel) | Änderungsprotokoll – **nicht standardmässig laden**, nur ergänzen |

Massgeblich sind die Namen mit **Bindestrich**; Dateien mit Unterstrich in `docs/` (z. B. `DEFINITION_*.md`) sind Einrichtungsvorlagen und werden weder geladen noch geändert. Redundanz vermeiden, nur betroffene Doku aktualisieren. Weitere Dateien: `build.md` (Build), `docs/hilfe/` (Benutzerhilfe), `.claude/translations.md` (generiert), `.claude/AppScreenshots/` (App-Screenshots).

## 5. Agents (`.claude/agents/`, alle Sonnet)
| Agent | Zuständigkeit | Schreibt? |
|---|---|---|
| `sync-engine-specialist` | Sync-Engine, 3-Wege-Abgleich, Konflikte, Löschen/Wiederherstellen, SMB, Wiederanlauf | ja |
| `database-specialist` | Room, Entities, DAOs, Repositorys, Migrationen, Integrität (nicht Sync-Logik) | ja |
| `code-reviewer` | Unabhängiges Review: Architektur, Fehler, Regressionen, Seiteneffekte | nein |
| `testing-qa-specialist` | Unabhängige QA: Tests ausführen, Abdeckung, Grenzfälle, Datenintegrität | nein |

Regeln: Agents sind Werkzeuge einer Chat-Rolle, keine Rollen, und haben höchstens deren Schreibrechte (schreibende Agents nur dort, wo die Rolle Code ändern darf; sonst nur Analyseaufträge). Hauptagent bleibt für die Gesamtaufgabe verantwortlich und prüft Agent-Ergebnisse kritisch. Keine Agents für triviale Änderungen, keine redundanten Analysen, keine parallelen Schreibzugriffe auf dieselben Dateien. Kritische Sync-Änderung → Sync-Spezialist; DB-Änderung → Database-Spezialist. Review und QA sind verschieden; bei besonders kritischen Änderungen beide, unabhängig voneinander. Kontext knapp mitgeben.

## 6. Skills (`.claude/skills/`, Ablauf steht im jeweiligen `SKILL.md`)
| Zweck | Skill |
|---|---|
| Neue Funktion/Änderung | `implement-feature` |
| Fehlersuche | `investigate-bug` |
| Codeprüfung | `review-changes` (nutzt `code-reviewer`) |
| Funktionale QA | `qa-check` (nutzt `testing-qa-specialist`) |
| Gezielte Tests | `run-targeted-tests` (nach `docs/TESTING.md`) |
| Dokumentation (nur Documentation) | `update-docs` |

Keiner dieser Skills ist auf manuellen Aufruf beschränkt (kein `disable-model-invocation`); einsetzen, wenn der Ablauf sinnvoll ist und die Rolle die nötigen Rechte hat. Skill-Inhalte nicht hier duplizieren.

**Rollen-Skills** (nur manuell durch den Benutzer, `disable-model-invocation`): `rolle-pm`, `rolle-architecture`, `rolle-development`, `rolle-qa`, `rolle-documentation`, `rolle-ux`. Sie laden nach Chatstart/`/clear` Masterprompt und Posteingang der Rolle.

## 7. Abläufe (Development)
- **Klein**: Dateien lesen → ändern → gezielte Tests → kurzer Bericht. Keine Agents.
- **Normal**: `implement-feature` → Komponenten prüfen → Doku gezielt laden → bei Bedarf Spezialagent → implementieren → Tests → Doku-Übergabe.
- **Fehler**: `investigate-bug` → Ursache nachweisen → beheben → Regressionstests → Ergebnis dokumentieren.
- **Kritisch** (3-Wege-Abgleich, Konflikte, Löschen/Wiederherstellen, Migrationen, Nebenläufigkeit, möglicher Datenverlust): Risiken analysieren → Doku laden → Spezialagent → implementieren → gezielte Tests → `review-changes` → `qa-check` → Restrisiken dokumentieren → Doku-Übergabe. Nur tatsächlich nötige Schritte.
- Ein erfolgreicher Build ersetzt weder Review noch QA.

## 8. Token, Modelle, Git
- Nur relevante Quelldateien und Doku laden; gezielte Tests statt Volltests; keine Chatverläufe rekonstruieren; vorhandene Analyseergebnisse (Task-Dateien) wiederverwenden; keine vollständigen Projektanalysen bei kleinen Aufgaben.
- Sonnet als Standard; Opus nur für aussergewöhnlich komplexe Architektur-/Analyseaufgaben. Qualität und Datensicherheit gehen vor.
- **Git**: Der Ordner ist derzeit **kein Git-Repository** (`.git` fehlt) – dann vor Änderungen an gemeinsam genutzten Dateien den aktuellen Dateistand lesen und nichts überschreiben, was ein anderer Chat geändert hat. Mit Repository: Status vor grösseren Änderungen prüfen, fremde Änderungen schützen. Gemeinsame Dateien nicht gleichzeitig bearbeiten.

## 9. Sechs Chat-Rollen (dieselbe CLAUDE.md, dieselben Agents/Skills)
| Rolle | Masterprompt (`.claude/roles/`) | Modell / Effort | Auftrag |
|---|---|---|---|
| Project Manager | `Project-manager.md` | Sonnet | Ansprechpartner und Koordinator des Benutzers: Anforderungen klären, zerlegen, priorisieren, an die nötigen Rollen verteilen, Freigaben einholen; keine Zwischenstation für Fachübergaben |
| Architecture & Complex Issues | `ARCHITECTURE-Specialist.md` | Opus / High | Analysen, Architekturentscheidungen, Alternativen, technische Vorgaben und Risiken |
| Development | `DEVELOPMENT-Specialist.md` | Sonnet / Medium | Implementierung, Fehlerbehebung, Integration, gezielte Tests, Doku-Übergaben |
| QA & Testing | `QA-Specialist.md` | Sonnet / Medium | Unabhängige funktionale Prüfung, Abdeckung, Regressionen, Datenintegrität, Risikobewertung |
| Documentation | `DOCUMENTATION-Specialist.md` | Sonnet / Low | Zentrale Dokumentation und ihre Konsistenz; nichts erfinden |
| UX-Specialist | `UX-Specialist.md` | Sonnet / Medium | Benutzerfreundlichkeit, UI-Design, Navigation, Designsystem, Designvorschläge, UX-Review im Emulator |

Modell und Effort stellt der Benutzer pro Chat ein.

## 10. Koordination: `coordination/` (Richtlinie: `coordination/README.md`)
Verbindliches, dezentrales Übergabesystem: `TASK-INDEX.md` (aktive Aufträge) · `inbox/<rolle>.md` (`project-manager`, `architecture`, `development`, `qa-testing`, `documentation`, `ux-designer`) · `tasks/{OPEN,IN_PROGRESS,BLOCKED,COMPLETED}/<ID>.md` (einzige Quelle für Inhalt und Status). Matrix, Status, Übergaben, Freigaben, parallele Sitzungen und Konflikte regelt **nur** die Richtlinie. Grundsätze:
- Bei Start/`/clear` und vor Übernahmen nur den eigenen Posteingang und die dort verwiesenen Task-Dateien lesen; `tasks/COMPLETED/` und `archive/` nie standardmässig.
- Fachrollen übergeben Ergebnisse **direkt** an die Rolle des nächsten Schritts und legen Benutzerentscheidungen dem Benutzer direkt vor. Der Project Manager ist keine Rückgabe-, Freigabe- oder Abschlussinstanz.

## 11. Doku-Übergaben
Entscheidung → `DECISIONS.md` · Architektur → `ARCHITECTURE.md` · Sync → `SYNC-ENGINE.md` · Datenmodell → `DATA-MODEL.md` · Tests → `TESTING.md` · Release → `PATCHNOTES.md` · sichtbares Verhalten → `docs/hilfe/`. Die Änderung übernimmt Documentation: die auslösende Rolle übergibt per Auftrag mit technischem Kontext, betroffenen Dateien und ggf. Version.

## 12. Abschlussbericht (kompakt)
Änderungen · betroffene Dateien · Tests und Ergebnisse (nicht ausgeführte kennzeichnen) · Übergaben · offene Punkte/Risiken · Versionsnummer.
