# CLAUDE.md – LeoSync (Projektordner: Sync2PC)

Android-App (Kotlin, Room, SMB) zur Synchronisierung Handy ⇄ PC. Gilt für alle sechs Chat-Rollen (siehe 9). Sprache: Doku/Kommentare Deutsch, Schweizer Schreibweise (kein ß).

## 0. Rollen und Regelhierarchie
Jeder Chat hat genau eine der sechs Rollen (siehe 9). **Nach Chatstart oder `/clear`** ruft der Benutzer den Rollen-Skill auf (`/rolle-pm`, `/rolle-architecture`, `/rolle-development`, `/rolle-qa`, `/rolle-documentation`, `/rolle-ux`) oder nennt die Rolle: dann einmalig den Masterprompt (Tabelle in 9) lesen und den eigenen Posteingang mit `liste` abrufen (siehe 10, `coordination/README.md` §4). Ohne Rollenangabe zuerst nach der Rolle fragen.

**Regelhierarchie:** (1) diese `CLAUDE.md` → (2) `coordination/README.md` (Koordinationsrichtlinie) → (3) Rollenprompt → (4) Task-Datei/Übergabe. Bei Widerspruch gilt die höhere Stufe; Rollenprompts schaffen keine Ausnahmen von den Berechtigungen in 1, Task-Dateien heben keine Rollengrenzen auf. Nicht lösbare Widersprüche dem Benutzer vorlegen.

## 1. Verbindliche Inhaberregeln
Diese Regeln sind hier abschliessend festgehalten (`.claude/README.md` gibt es nicht mehr). Sie gelten verbindlich für alle Rollen:
- **Nichts bauen** (`build.bat`, `assemble*`, APK/Bundle): macht der Inhaber, ausser er erlaubt es ausdrücklich pro Lauf (DEC-184). Gezielte Tests (`gradlew.bat testDebugUnitTest …`, Skill `run-targeted-tests`) sind kein Bauen. **Einzige Ausnahme: QA & Testing** (Benutzerauftrag 2026-11-10) erstellt und wiederholt Debug-/Test-Builds (`gradlew.bat assembleDebug`, `testDebugUnitTest`, `connectedDebugAndroidTest`, `lintDebug`) ohne Einzelerlaubnis und installiert Debug-APKs auf Emulatoren und vom Inhaber ausdrücklich freigegebenen Testgeräten; nie `build.bat`, `assembleRelease`/`bundle*`, Signierung, Veröffentlichung oder Änderungen an Code-, Gradle- und Build-Dateien (Details: QA-Prompt §3). Die übrigen Rollen erhalten dadurch **keine** Build-Rechte.
- Jede Code-Änderung: PATCH in `version.properties` erhöhen (durch die Rolle, die den Code ändert). Neue Funktion = nächste Minor-Reihe.
- Sichtbare Texte nur in beide Sprachdateien (`res/values/strings_*.xml` EN, `res/values-de/` DE), nie fest im Code; danach `tools/make-translations.ps1`. DE: „Handy“, „Freigabe“, „Synchronisierung“; EN: „Mobile“, „Share“. Nie Whitelist/Blacklist.
- Jede Antwort, die eine Änderung abschliesst, endet mit der Versionsnummer in eigener Zeile (z. B. `Version 0.11.0`).
- **Kodierung:** Alle Textdateien sind UTF-8 ohne BOM. In PowerShell 5.1 nie `Set-Content`, `Out-File` oder `Add-Content` ohne `-Encoding` verwenden, und auch `-Encoding utf8` schreibt dort ein BOM. Stattdessen `[IO.File]::WriteAllText(pfad, text, [Text.UTF8Encoding]::new($false))` nutzen. Beim Lesen immer `Get-Content -Encoding UTF8` angeben, sonst entstehen kaputte Umlaute.
- **PowerShell-Alias-Falle (Datenverlust, mehrfach vorgekommen):** Eigene Funktionen und Variablen in PowerShell nie nach eingebauten Aliasen benennen. Besonders gefährlich: `rd`, `ri`, `del`, `rm`, `erase` (alle = `Remove-Item`, löschen Dateien/Ordner), ausserdem `r` (`Invoke-History`), `sc`, `sl`, `gc`, `ac`, `cp`, `mv`, `cat`, `echo`. Hilfsfunktionen immer mit Verb-Nomen- oder eindeutigen Namen (z. B. `Read-FileText`, `Rep-Text`) benennen; vor dem Aufruf einer neuen Funktion mit `Get-Command <Name>` prüfen, dass sie nicht auf einen Alias oder ein Cmdlet trifft. Vor jedem Löschen und Überschreiben von Dateien den Inhalt gesichert haben oder lesen; zuerst lesen und prüfen, dann schreiben, nie `Remove-Item` nach einem ungeprüften Schreibvorgang.
- Geheimnisse (Keystore) nie ins Repo; `.gitignore` aktuell halten; vor dem Anlegen einer Datei prüfen, ob sie existiert. Echte Zugangsdaten und produktive Passwörter gehören nie in Repo, Rollenprompts oder Doku. **Ausnahme (Benutzerauftrag 2026-11-10):** `qa-test-1` ist ein bewusst **öffentliches Testpasswort** nur der lokalen SMB-Testumgebung (QA-Prompt §3); es darf in Rollenprompts und Testdokumentation stehen, nie für produktive Konten oder externe Systeme.
- **Nie selbst committen oder pushen** (alle Rollen).
- Jeder Chat führt nur Aufgaben seiner Rolle aus und übernimmt keine Berechtigungen anderer Rollen; Fremdes wird direkt an die zuständige Rolle übergeben (siehe 10).
- **Benutzerfreigabe** (nur der Benutzer, keine Rolle ersetzt sie) vor: grösseren Architektur- und wesentlichen Designänderungen, Änderungen an sicherheits- oder datenverlustkritischen Abläufen, destruktiven Aktionen ausserhalb der Testdaten, Builds und Änderungen am Regelwerk (diese Datei, `coordination/README.md`, `.claude/roles|skills|agents/`). Ein Vorschlag ist keine Freigabe. Details: `coordination/README.md` §8.
- **Emulator:** UX und QA starten und bedienen ihn für ihre Prüfaufträge ohne Einzelerlaubnis und dürfen in der App alle dafür nötigen Einstellungen vornehmen und entfernen (DEC-212). Profile nur nach **vorherigem Export** anlegen, bearbeiten oder entfernen; nach den Tests **alle selbst angelegten Testprofile löschen** und den Ursprungszustand **per Import** wiederherstellen (Benutzerauftrag 2026-11-10). Dateioperationen nur in Testordnern: UX in `D:\testsync` und `X:\testsync`; QA zusätzlich im SMB-Testbereich `.build\qa-smb-share`, an den Betriebsdateien `.build\qa-smb-testshare.pid`, `.log`, `.log.err` und am venv `%LOCALAPPDATA%\LeoSyncTestShare` (Details: QA-Prompt §3, UX-Prompt §4). Keine allgemeine Schreibberechtigung für `.build/` (u. a. `last-build.log`) oder `%LOCALAPPDATA%`; Build-Konfigurationen bleiben gesperrt. Andere Rollen: Emulator nur mit Erlaubnis pro Lauf (DEC-184). Profile mit Zugangsdaten richtet der Inhaber ein. Physische Geräte: QA nur vom Inhaber ausdrücklich benannte Testgeräte; persönliches Handy, produktive Profile und Daten nie. Manuelle Benutzertests bleiben beim Inhaber; QA bereitet sie vor, markiert sie nie selbst als bestanden.

**Schreibrechte je Rolle** (Lesen ist allen erlaubt; Aufträge in `coordination/` ändern alle Rollen **ausschliesslich über `tools\Auftrag.ps1`**, nie direkt, siehe 10):
| Rolle | Darf bearbeiten | Darf nicht bearbeiten |
|---|---|---|
| Project Manager | nur Aufträge über `tools\Auftrag.ps1` | alle Projektdateien |
| Architecture | Code nur auf ausdrücklichen Umsetzungsauftrag des Benutzers; sonst nur Aufträge über `tools\Auftrag.ps1` | Dokumentation |
| Development | Code, Tests, Ressourcen (`app/`), `version.properties`, `tools/`, Gradle-Dateien, `.claude/translations.md` (nur per Skript) | Dokumentation |
| QA & Testing | Screenshots unter `.claude/AppScreenshots/`; Testordner und SMB-Testbetrieb nach „Emulator“ oben (nur diese); Build-Ausgaben (`app/build/`, `.gradle/`) durch Debug-/Test-Builds | Produktions-, Test- und Doku-Dateien, Gradle-/Build-Dateien und -Skripte, `version.properties`, übrige Dateien unter `.build/` und alle Dateien unter `.claude/ux/` (u. a. `navigation.md`) |
| Documentation | `docs/` (ohne Dateien mit Unterstrich), `docs/hilfe/`, `README.md`, `PATCHNOTES.md`, `build.md`, `PRIVACY_*.md` | Code, Tests, Ressourcen |
| UX-Specialist | Screenshots und Designvorschauen unter `.claude/AppScreenshots/`; alle Dateien unter `.claude/ux/` (u. a. `navigation.md`); Dateien in den Testordnern `D:\testsync`, `X:\testsync` | Code, Layouts/Compose, Ressourcen inkl. Texte, Gradle, Dokumentation |

Alle Rollen dürfen Eingabedateien für `tools\Auftrag.ps1` im Scratchpad (ausserhalb des Projekts) anlegen. Regelwerk-Dateien (`CLAUDE.md`, `coordination/README.md`, `.claude/roles|skills|agents/`) ändert nur die Rolle, der der Benutzer dies ausdrücklich aufträgt.

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

Regeln: Agents sind Werkzeuge einer Chat-Rolle, keine Rollen, und haben höchstens deren Schreibrechte; für `coordination/` gilt auch für sie 10 (nur `tools\Auftrag.ps1`, `-Rolle` des aufrufenden Chats) (schreibende Agents nur dort, wo die Rolle Code ändern darf; sonst nur Analyseaufträge). Hauptagent bleibt für die Gesamtaufgabe verantwortlich und prüft Agent-Ergebnisse kritisch. Keine Agents für triviale Änderungen, keine redundanten Analysen, keine parallelen Schreibzugriffe auf dieselben Dateien. Kritische Sync-Änderung → Sync-Spezialist; DB-Änderung → Database-Spezialist. Review und QA sind verschieden; bei besonders kritischen Änderungen beide, unabhängig voneinander. Kontext knapp mitgeben.

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
- **Git**: Git ist nicht vorhanden, daher können keine Git-Befehle ausgeführt werden. Vor Änderungen an gemeinsam genutzten Dateien den aktuellen Dateistand lesen und nichts überschreiben, was ein anderer Chat geändert hat. Gemeinsame Dateien nicht gleichzeitig bearbeiten.

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
Verbindliches, dezentrales Übergabesystem: `tasks/{OPEN,IN_PROGRESS,BLOCKED,COMPLETED}/<ID>.md` (einzige Quelle für Inhalt und Status) · daraus erzeugt `TASK-INDEX.md` und `inbox/<rolle>.md` (`project-manager`, `architecture`, `development`, `qa-testing`, `documentation`, `ux-designer`). Matrix, Status, Übergaben, Freigaben, parallele Sitzungen und Konflikte regelt **nur** die Richtlinie. Grundsätze:
- **Schreiben nur über `tools\Auftrag.ps1`** (alle Rollen und Agents). Nie Edit/Write oder Shell-Befehle auf Task-Dateien, Index, Posteingänge, `.history/`, `.audit.log`, `.lock`, auch nicht zur Reparatur (`check -Fix`). Technisch gesperrt über `.claude/settings.json` und den Hook `tools\Auftrag-guard.ps1` (README §1).
- **Lesen:** Posteingang und Überblick mit `liste`. Direkt gelesen wird nur die Task-Datei des Auftrags, den man übernimmt oder bearbeitet, einmal. Index, `inbox/`-Dateien, `tasks/COMPLETED/`, `.history/`, `archive/` nur ausnahmsweise mit konkretem Grund (README §1).
- Fachrollen übergeben Ergebnisse **direkt** an die Rolle des nächsten Schritts (Folgeauftrag mit `erstellen`) und legen Benutzerentscheidungen dem Benutzer direkt vor. Der Project Manager ist keine Rückgabe-, Freigabe- oder Abschlussinstanz.

**Kurzbedienung** (vollständig: README §13; `… -Help` zeigt Parameter, das Skript muss nicht gelesen werden). Jede Zeile beginnt mit `powershell -NoProfile -ExecutionPolicy Bypass -File tools\Auftrag.ps1`; `-Rolle` = eigene Rolle (`PM`, `Architecture`, `Development`, `QA`, `Documentation`, `UX`), `-Id` = volle ID oder Nummer:
```
liste -Rolle QA [-MitDateien]                                          # eigener Posteingang inkl. BLOCKED (ohne -Rolle: alle)
erstellen -Rolle QA -An Development -Prio MEDIUM -Titel "…" -Aufgabe "…" -Ergebnis "…" [-Abhaengigkeiten "<ID>"] [-Dateien "…"] [-Kontext "…"] [-Begruendung "…" bei HIGH/CRITICAL]
status -Rolle QA -Id 260 -Neu IN_PROGRESS                               # Übernahme; BLOCKED mit -Frage "…"; CANCELLED (Absender) mit -Notiz "…"
aendern -Rolle QA -Id 260 -Feld Bearbeitungsverlauf -Wert "…"           # Feld setzen/anhängen; -Abschnitt "<Titel>" -Eingabe <Datei> für Berichte
abschliessen -Rolle QA -Id 260 -Arbeiten "…" -Tests "…" [-Risiken "…"] [-Folgeschritt "<ID> an <Rolle>"]
```
Mehrzeiliges über `-Eingabe <Datei im Scratchpad>` (`- Schlüssel: Wert`, Folgezeilen zwei Leerzeichen eingerückt). Exit 0: nichts nachlesen, `WARNUNG:` beachten; 1 korrigieren; 2 nicht umgehen; 3 Stand prüfen (bei `erstellen`: mögliche Dublette, bestehenden Auftrag ergänzen oder bewusst `-Trotzdem`); 4 wiederholen; 5/6 README §13. Folgeaufträge so vollständig formulieren (Erkenntnisse in `-Kontext`), dass der Empfänger die Vorgänger-Datei nicht lesen muss.

## 11. Doku-Übergaben
Entscheidung → `DECISIONS.md` · Architektur → `ARCHITECTURE.md` · Sync → `SYNC-ENGINE.md` · Datenmodell → `DATA-MODEL.md` · Tests → `TESTING.md` · Release → `PATCHNOTES.md` · sichtbares Verhalten → `docs/hilfe/`. Die Änderung übernimmt Documentation: die auslösende Rolle übergibt per Auftrag (`erstellen -An Documentation`) mit technischem Kontext, betroffenen Dateien und ggf. Version.

## 12. Abschlussbericht (kompakt)
Änderungen · betroffene Dateien · Tests und Ergebnisse (nicht ausgeführte kennzeichnen) · Übergaben · offene Punkte/Risiken · Versionsnummer. Was schon in der Task-Datei steht (Arbeiten, Tests, Risiken, Folgeschritt), im Chat nur kurz zusammenfassen und auf die Task-ID verweisen, nicht wiederholen.
