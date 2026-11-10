# Koordination der fünf Chat-Rollen – Bedienungsanleitung

Dezentrales, Markdown-basiertes System. **Keine automatische Kommunikation, keine technische Sperre:** Jeder Chat liest und schreibt die Dateien selbst; Konflikte vermeidet nur die Einhaltung dieser Regeln. Es ersetzt weder Git noch die Rückfrage beim Inhaber. Übergeordnete Regeln: `CLAUDE.md` §10.

## 1. Aufbau
```
coordination/
├── README.md        diese Anleitung
├── TASK-INDEX.md    aktive Aufträge, eine Zeile je Auftrag, plus Zähler „Letzte vergebene Nummer“
├── inbox/           je Rolle ein Posteingang: nur Verweiszeilen auf aktive Aufträge
│   project-manager.md · development.md · architecture.md · qa-testing.md · documentation.md
├── tasks/           eine Markdown-Datei je Auftrag, nach Status sortiert
│   OPEN/ · IN_PROGRESS/ · BLOCKED/ · COMPLETED/   (COMPLETED enthält auch CANCELLED)
└── archive/         Sicherung vor der Migration vom 2026-11-09 (Altbestand, nicht lesen)
```
- **Die Task-Datei ist die einzige Quelle** für Inhalt und Status. Index und Posteingang enthalten nur Metadaten/Verweise, keine Beschreibungen.
- Offene Aufträge finden = Posteingang lesen oder Ordner `tasks/OPEN` auflisten. `tasks/COMPLETED` wird nie standardmässig gelesen.
- Alte Verweise der Form `inbox/<rolle>.md SYNC-…` (vor 2026-11-09) bedeuten jetzt `tasks/COMPLETED/SYNC-….md`. Die Aufträge -002 bis -011 stammen aus der Durchsicht von `docs/DECISIONS.md` Anhang A/B; der alte Kopftext steht in `archive/vor-migration-2026-11-09/development.md`.

## 2. Rollen, Matrix, Zuständigkeiten
| Absender | Zulässige Empfänger |
|---|---|
| Project Manager | Development, Architecture, QA, Documentation |
| Development | Project Manager, Architecture, QA, Documentation |
| Architecture | Project Manager, Development, Documentation |
| QA & Testing | Project Manager, Development, Documentation |
| Documentation | Project Manager, Development, Architecture |

- **Project Manager:** bespricht mit dem Inhaber, formuliert und verteilt freigegebene Aufträge, prüft Duplikate, überwacht offene/blockierte Aufgaben. Ändert **keine Projektdateien**; Dateien unter `coordination/` sind keine Projektdateien und darf er bearbeiten.
- **Development:** Implementierung und Fehlerbehebung. **Architecture:** komplexe Analysen, Entscheidungen; grössere Umsetzung erst nach Freigabe des Inhabers. **QA:** unabhängige Prüfung, korrigiert nicht selbst. **Documentation:** technische Doku, erfindet nichts, fragt bei Development/Architecture nach.
- Übergeben nur, wenn die Aufgabe ausserhalb der eigenen Rolle liegt oder ein eigenständiges Ergebnis einer anderen Rolle nötig ist. Rein interne Aufgaben, blosse Rückfragen im Chat und bereits vorhandene Aufträge (stattdessen Task-ID verwenden/ergänzen) erzeugen keinen neuen Auftrag.

## 3. Chatstart und `/clear`
`CLAUDE.md` wird immer geladen, die Rolle aber nicht. Nach Start oder `/clear` den Skill der Rolle aufrufen: `/rolle-pm`, `/rolle-development`, `/rolle-architecture`, `/rolle-qa`, `/rolle-documentation`. Der Skill liest Masterprompt und eigenen Posteingang. Alternativ den Rollennamen nennen; dann gilt `CLAUDE.md` §0 und §10.

## 4. Neuen Auftrag erstellen (Absender)
1. `TASK-INDEX.md` und `tasks/OPEN|IN_PROGRESS|BLOCKED` auf Duplikate prüfen (gleiches Ziel/gleiche Dateien).
2. Empfänger per Matrix prüfen.
3. **ID:** `SYNC-YYYYMMDD-NNN`, NNN global fortlaufend = Maximum aus „Letzte vergebene Nummer“ im Index und den Dateinamen unter `tasks/` plus 1. Datei zuerst **ohne Überschreiben** anlegen (`New-Item` ohne `-Force`); existiert sie, Nummer erhöhen.
4. Task-Datei (Vorlage §9) in `tasks/OPEN/` füllen: vollständig, aber kompakt; keine Chatverläufe, keine Quellcodekopien.
5. Index: Zeile ergänzen und Zähler setzen.
6. Posteingang des Empfängers: Zeile ergänzen.
7. Im eigenen Chat melden: ID und Empfänger. Der Absender bleibt für Integration und Bewertung des Ergebnisses verantwortlich.

Reihenfolge immer: **Task-Datei → Index → Posteingang** (siehe §7).

## 5. Statusmodell
| Status | Bedeutung | Wechsel durch |
|---|---|---|
| OPEN | erstellt, nicht übernommen | Absender |
| IN_PROGRESS | Empfänger hat Übernahme ausdrücklich bestätigt | Empfänger |
| BLOCKED | Information/Hindernis fehlt; Frage unter „Offene Fragen“ | Empfänger |
| COMPLETED | Bearbeitung fertig und Ergebnis dokumentiert | Empfänger |
| CANCELLED | abgebrochen, mit Begründung | Absender oder Inhaber |

Zulässige Wechsel: OPEN → IN_PROGRESS · IN_PROGRESS ↔ BLOCKED · OPEN → BLOCKED · IN_PROGRESS/BLOCKED/OPEN → COMPLETED bzw. CANCELLED (COMPLETED nur aus IN_PROGRESS). Rücksprung aus COMPLETED gibt es nicht: neuer Folgeauftrag.

**Übernahme (Empfänger):** Posteingang lesen → nur adressierte Aufträge → Task-Datei lesen, Git-/Dateistand prüfen (§6) → Übernahme in der Task-Datei bestätigen (Datum, Rolle) → IN_PROGRESS. Fehlen Informationen: BLOCKED, Frage eintragen, Absender im Chat benennen. Nicht Zuständiges ablehnen (begründet, Absender informieren).

**Abschluss (Empfänger):** Arbeiten, Dateien, Tests/Ergebnisse, nicht durchgeführte Prüfungen, Restrisiken und Abschlussdatum in der Task-Datei eintragen → Status COMPLETED → Datei nach `tasks/COMPLETED/` → Zeile aus Index **und** Posteingang entfernen. Nachweislich nicht ausgeführte Tests nie als bestanden eintragen.

**Bearbeitungsverlauf** bleibt knapp: eine Zeile je Statuswechsel (Datum, Rolle, Status, Kurznotiz).

## 6. Parallele Chats und Git
- Vor Übernahme: Git-Status prüfen. Der Ordner ist **derzeit kein Git-Repository**; dann gilt der Dateistand (Änderungszeit, Inhalt) der betroffenen Dateien.
- Vor jeder Änderung die aktuelle Task-Datei und, bei gemeinsamen Dateien (Index, Posteingang, Doku), den aktuellen Stand erneut lesen und nur gezielt ändern (Zeile ergänzen/entfernen, nie ganze Datei überschreiben).
- Keine gleichzeitigen Schreibzugriffe auf dieselben Dateien; fremde Änderungen nicht überschreiben. Bei Konflikt stoppen und beim Inhaber abstimmen.
- Keine eigenen Commits oder Pushes.

## 7. Nicht atomare Änderungen und Wiederherstellung
Statuswechsel in dieser Reihenfolge: (1) Status-Zeile und Verlauf in der Task-Datei, (2) Datei in den passenden Statusordner verschieben, (3) Index, (4) Posteingang(e). Bei Abbruch ist der Zustand anhand der **Status-Zeile in der Task-Datei** wiederherstellbar:
- Ordner passt nicht zur Status-Zeile → Datei in den richtigen Ordner verschieben.
- Index/Posteingang weichen ab → aus der Task-Datei nachziehen.
- Datei ohne Index-Eintrag → Eintrag ergänzen; Index-Eintrag ohne Datei → Eintrag entfernen und dem Inhaber melden.
- Doppelte ID → Ältere behalten, Konflikt dem Inhaber melden.

## 8. BLOCKED
Der Empfänger setzt BLOCKED und schreibt die konkrete Frage ins Feld „Offene Fragen“. Die Antwort trägt der Absender (oder die angefragte Rolle) in der Task-Datei ein und meldet sie im Chat; der Empfänger setzt danach IN_PROGRESS. Ein BLOCKED-Auftrag ist weder erledigt noch verloren; der Project Manager überwacht ihn. Rückfragen, die eine andere Rolle beantworten muss, werden als eigener Auftrag erstellt (Beispiel §10) und im blockierten Auftrag unter „Abhängigkeiten“ genannt.

## 9. Vorlage Task-Datei
```
# SYNC-YYYYMMDD-NNN – Kurztitel
- Erstellt am: YYYY-MM-DD
- Absender: <Rolle>
- Empfänger: <Rolle>
- Priorität: LOW | MEDIUM | HIGH | CRITICAL
- Status: OPEN
- Übergabegrund:
- Aufgabenbeschreibung:
- Technischer Kontext:
- Betroffene Dateien:
- Dokumentationsverweise:
- Erwartetes Ergebnis:
- Abhängigkeiten: (andere Task-IDs oder –)
- Bearbeitungsverlauf: (je Zeile: Datum, Rolle, Status, Notiz)
- Übernahmebestätigung: (Empfänger: Datum, Rolle)
- Durchgeführte Arbeiten:
- Tests und Ergebnisse: (nur tatsächlich ausgeführte)
- Offene Fragen:
- Verbleibende Risiken:
- Abgeschlossen am:
```

## 10. Beispiele (nur Illustration, keine echten Aufträge)
**A) Project Manager → Development** (`tasks/OPEN/SYNC-20261201-050.md`)
```
# SYNC-20261201-050 – Beispiel: Konfliktkarte zeigt Dateigrösse
- Erstellt am: 2026-12-01
- Absender: Project Manager
- Empfänger: Development
- Priorität: LOW
- Status: OPEN
- Übergabegrund: Inhaber wünscht die Grösse in der Konfliktkarte; kleine, isolierte Änderung.
- Aufgabenbeschreibung: Dateigrösse beider Versionen in der Konfliktkarte anzeigen.
- Technischer Kontext: Anzeige nur; keine Änderung an Sync-Logik oder DB.
- Betroffene Dateien: (Development ermittelt; nicht erfunden)
- Dokumentationsverweise: docs/DECISIONS.md (Konflikte)
- Erwartetes Ergebnis: Anzeige DE/EN, gezielte Tests, Rückmeldung mit Version.
- Abhängigkeiten: –
- Bearbeitungsverlauf: 2026-12-01 PM OPEN
- Übernahmebestätigung: –
- Durchgeführte Arbeiten: –
- Tests und Ergebnisse: –
- Offene Fragen: –
- Verbleibende Risiken: –
- Abgeschlossen am: –
```
Dazu: Zeile in `TASK-INDEX.md` (Zähler 050) und in `inbox/development.md`:
`| SYNC-20261201-050 | Project Manager | Konfliktkarte zeigt Dateigrösse | LOW | OPEN | ../tasks/OPEN/SYNC-20261201-050.md |`

**B) Rückfrage QA → Development** (QA ist in -051 blockiert, weil das erwartete Verhalten unklar ist)
```
# SYNC-20261201-052 – Beispiel: Rückfrage Verhalten bei abgebrochener Übertragung
- Erstellt am: 2026-12-01
- Absender: QA
- Empfänger: Development
- Priorität: MEDIUM
- Status: OPEN
- Übergabegrund: Prüfung -051 kann nicht abgeschlossen werden: Sollverhalten unklar.
- Aufgabenbeschreibung: Frage: Wird eine abgebrochene Teildatei beim nächsten Lauf neu übertragen oder fortgesetzt? Bitte Antwort mit Verweis auf die Code-Stelle.
- Technischer Kontext: Beobachtet im Test (kein nachgewiesener Fehler).
- Betroffene Dateien: (Development nennt die Stelle)
- Dokumentationsverweise: docs/SYNC-ENGINE.md
- Erwartetes Ergebnis: Antwort in „Durchgeführte Arbeiten“; keine Codeänderung nötig.
- Abhängigkeiten: blockiert SYNC-20261201-051 (QA)
- Bearbeitungsverlauf: 2026-12-01 QA OPEN
- ...
```
QA setzt -051 auf BLOCKED (Datei nach `tasks/BLOCKED/`, Index, Posteingang) und verweist auf -052. Nach Antwort: Development COMPLETED -052, QA → IN_PROGRESS -051.
