# Koordinationsrichtlinie – sechs Chat-Rollen

Verbindliche, zentrale Referenz für die Zusammenarbeit der Chat-Rollen. Dezentrales Markdown-System: **keine automatische Kommunikation, keine technische Sperre**. Jeder Chat liest und schreibt die Dateien selbst; Konflikte vermeidet nur die Einhaltung dieser Regeln. Es ersetzt weder Git noch die Freigabe durch den Benutzer (Inhaber).

## 0. Geltung und Regelhierarchie
1. `CLAUDE.md` – globale Projektregeln, Sicherheit, zentrale Berechtigungen.
2. Diese Richtlinie – Aufgaben, Status, Übergaben, Freigaben, parallele Arbeit, Konflikte.
3. Rollenprompts `.claude/roles/*.md` – fachliche Verantwortung und rollenspezifische Arbeitsweise.
4. Task-Dateien und Übergaben – der konkrete Auftrag.

Bei Widerspruch gilt die höhere Stufe. Rollenprompts schaffen keine Ausnahmen von zentralen Berechtigungen; Task-Dateien heben keine Rollengrenzen auf. Ist ein Widerspruch nicht eindeutig lösbar: dokumentieren (Task-Datei, „Offene Fragen“) und dem Benutzer vorlegen, keine eigenmächtige Regeländerung. Agents (`.claude/agents/`) sind Werkzeuge innerhalb einer Chat-Rolle, keine Rollen; sie haben höchstens die Rechte der aufrufenden Rolle.

## 1. Aufbau
```
coordination/
├── README.md        diese Richtlinie
├── TASK-INDEX.md    aktive Aufträge, eine Zeile je Auftrag, plus Zähler „Letzte vergebene Nummer“
├── TASK-HANDOVER.md nur Verweisdatei (Altbestand bis 2026-11-09)
├── inbox/           je Rolle ein Posteingang: nur Verweiszeilen auf aktive Aufträge
│   project-manager.md · architecture.md · development.md · qa-testing.md · documentation.md · ux-designer.md
├── tasks/           eine Markdown-Datei je Auftrag, nach Status sortiert
│   OPEN/ · IN_PROGRESS/ · BLOCKED/ · COMPLETED/   (COMPLETED enthält auch CANCELLED)
└── archive/         Sicherungen (vor-migration-2026-11-09, vor-refactoring-2026-11-10); nicht lesen
```
- **Die Task-Datei ist die einzige Quelle** für Inhalt und Status. Index und Posteingang enthalten nur Metadaten/Verweise.
- Gelesen wird bei Start/`/clear` und vor Übernahmen **nur der eigene Posteingang** und die dort verwiesenen Task-Dateien. Index nur bei Übergabe, Dublettenprüfung oder Statusprüfung. `tasks/COMPLETED/` und `archive/` nie standardmässig.
- Alte Verweise der Form `inbox/<rolle>.md SYNC-…` (vor 2026-11-09) bedeuten `tasks/COMPLETED/SYNC-….md`. Ältere Task-Dateien ohne die neuen Vorlagenfelder (§12) bleiben gültig und werden nicht migriert.

## 2. Rollen und Zuständigkeiten
Jeder Auftrag hat **genau eine verantwortliche Rolle: den Empfänger**. Andere Rollen wirken nur über eigene Aufträge mit (unterstützend oder prüfend).

| Rolle (Posteingang) | Entscheidet | Setzt um | Prüft |
|---|---|---|---|
| Project Manager (`project-manager`) | Aufgabenzerlegung, Rollenwahl, Reihenfolge, Priorität | Task-Dateien für Benutzeraufträge | – |
| Architecture (`architecture`) | technische Architektur, technische Konzepte und Risiken | Analysen, Konzepte; Code nur auf ausdrücklichen Umsetzungsauftrag des Benutzers | technische Umsetzbarkeit (auch von Designs) |
| Development (`development`) | Implementierungsdetails innerhalb freigegebener Vorgaben | Code, Tests, Ressourcen, Fehlerbehebung | eigene gezielte Tests |
| QA & Testing (`qa-testing`) | Prüfurteil (funktional) | Prüfberichte | funktionale Qualität, Regressionen, Datenintegrität |
| Documentation (`documentation`) | Struktur und Konsistenz der Doku | zentrale Dokumentation | Doku gegen Implementierung |
| UX-Specialist (`ux-designer`) | Designvorschläge (Freigabe: Benutzer) | Designvorgaben, Varianten, Vorschauen | Benutzerfreundlichkeit, UX-Review im Emulator |

**Project Manager:** Ansprechpartner und Koordinator des Benutzers – kein fachlicher Vorgesetzter, keine Durchlaufstation. Er nimmt Benutzeraufträge entgegen, zerlegt und verteilt sie, priorisiert, erkennt Abhängigkeiten und holt Freigaben ein. Er bezieht nur die tatsächlich nötigen Rollen ein und gibt Aufgaben nicht pauschal an Development, wenn zuerst Architektur-, UX- oder Doku-Arbeit nötig ist. Über interne Fachübergaben wird er nicht informiert; erneut einbezogen wird er nur, wenn der Benutzer eine neue Koordinationsentscheidung braucht oder ihm einen Auftrag gibt.

**UX-Beteiligung** (entscheidet beim Erstellen der Absender, meist der PM; Feld „UX-Beteiligung“):
- **nötig** bei neuen oder wesentlich geänderten Oberflächen, Navigation/Menüführung, Benutzerinteraktionen, Dialogen und Bestätigungsabläufen, Sync-Status- und Fortschrittsanzeigen, Fehlermeldungen und Warnhinweisen, Benutzerführung bei Konflikten, Einstellungs- und Konfigurationsabläufen, wesentlichen Bedienvereinfachungen, Änderungen am Designsystem.
- **nicht nötig** bei internen Korrekturen, DB-Optimierungen, Performance, Refactorings und technischen Änderungen ohne sichtbare oder bedienbare Auswirkung. Bei Zweifel entscheidet der PM; kein zusätzlicher Freigabeschritt für rein technische Aufgaben.

## 3. Übergabematrix
| Absender | Zulässige Empfänger |
|---|---|
| Benutzer | jede Rolle (direkt im Chat der Rolle; Absender „Benutzer“) |
| Project Manager | Architecture, Development, QA, Documentation, UX |
| Architecture, Development, QA, Documentation, UX | jede andere Fachrolle; Project Manager **nur** bei Koordinationsbedarf |

**Koordinationsbedarf** (einziger Grund für Aufträge an den PM): neue oder geänderte Anforderung ausserhalb des Auftrags, Priorität/Reihenfolge mehrerer Aufträge, Planung über mehrere Rollen. **Kein** Grund: Ergebnisrückmeldung, Abschlussmeldung, Fehlerbericht, UX-Review, Doku-Bedarf, Benutzerfreigabe (diese legt die zuständige Rolle dem Benutzer direkt vor, §8).

Einen neuen Auftrag gibt es nur, wenn ein eigenständiges Ergebnis einer anderen Rolle nötig ist. Kleine Aufgaben der eigenen Rolle, blosse Rückfragen im Chat und bereits vorhandene Aufträge (dann Task-ID ergänzen) erzeugen keinen Auftrag. Interne Agents ersetzen keine Übergabe.

## 4. Chatstart und `/clear`
`CLAUDE.md` wird immer geladen, die Rolle nicht. Nach Start oder `/clear` den Rollen-Skill aufrufen: `/rolle-pm`, `/rolle-architecture`, `/rolle-development`, `/rolle-qa`, `/rolle-documentation`, `/rolle-ux`. Der Skill lädt Masterprompt und eigenen Posteingang und meldet offene Aufträge; keine Projektanalyse, keine Übernahme ohne Bestätigung. Alternativ nennt der Benutzer die Rolle; dann denselben Ablauf von Hand.

## 5. Neuen Auftrag erstellen (Absender)
1. `TASK-INDEX.md` und `tasks/OPEN|IN_PROGRESS|BLOCKED` auf Dubletten prüfen (gleiches Ziel/gleiche Dateien).
2. Empfänger nach Matrix (§3) und Zuständigkeit (§2) prüfen; UX-Beteiligung festlegen.
3. **ID:** `SYNC-YYYYMMDD-NNN`, NNN global fortlaufend = Maximum aus „Letzte vergebene Nummer“ im Index und den Dateinamen unter `tasks/` plus 1. Datei zuerst **ohne Überschreiben** anlegen (`New-Item` ohne `-Force`); existiert sie, Nummer erhöhen.
4. Task-Datei (Vorlage §12) in `tasks/OPEN/` füllen: vollständig, aber kompakt; keine Chatverläufe, keine Quellcodekopien, keine erfundenen Dateipfade (Unbekanntes kennzeichnen).
5. Index: Zeile ergänzen und Zähler setzen. 6. Posteingang des Empfängers: Zeile ergänzen.
7. Im eigenen Chat ID und Empfänger melden (der Benutzer überbringt sie; es gibt keine automatische Benachrichtigung).

Reihenfolge immer: **Task-Datei → Index → Posteingang** (§10).

**Priorität:** CRITICAL = akute Gefahr für Datenintegrität, Datenverlust oder Grundfunktion · HIGH = wichtige Funktion, kritischer Fehler, nötige Architekturverbesserung · MEDIUM = reguläre Funktion/Verbesserung · LOW = optional, Komfort, Kleinigkeit. HIGH/CRITICAL kurz begründen.

## 6. Statusmodell, Übernahme, Abschluss
| Status | Bedeutung | Wechsel durch |
|---|---|---|
| OPEN | erstellt, nicht übernommen | Absender |
| IN_PROGRESS | Empfänger hat Übernahme ausdrücklich bestätigt | Empfänger |
| BLOCKED | Information, Freigabe oder Vorarbeit fehlt; Frage unter „Offene Fragen“ | Empfänger |
| COMPLETED | Arbeit erledigt und dokumentiert | Empfänger |
| CANCELLED | abgebrochen, mit Begründung | Absender oder Benutzer |

Zulässige Wechsel: OPEN → IN_PROGRESS · OPEN → BLOCKED · IN_PROGRESS ↔ BLOCKED · IN_PROGRESS → COMPLETED · OPEN/IN_PROGRESS/BLOCKED → CANCELLED. Kein Rücksprung aus COMPLETED/CANCELLED: stattdessen Folgeauftrag. Weitere Statuswerte gibt es nicht; Zwischenstände (z. B. „Designvorschlag erstellt“) stehen im Bearbeitungsverlauf.

**Übernahme (Empfänger):** nur an die eigene Rolle adressierte Aufträge → Task-Datei lesen, Datei-/Git-Stand prüfen (§9) → Übernahme in der Task-Datei bestätigen (Datum, Rolle) → IN_PROGRESS. Fehlt etwas: BLOCKED. Nicht Zuständiges begründet ablehnen: Grund und richtige Rolle in der Task-Datei vermerken, Absender im Chat informieren; der Absender setzt CANCELLED oder erstellt den Auftrag an die richtige Rolle.

**Verantwortung für das Ergebnis:** allein beim Empfänger. Der Absender wertet das Ergebnis nur aus, wenn er es für eine eigene, noch offene Aufgabe braucht; der PM wertet Ergebnisse nicht standardmässig aus.

**Abschlussbedingungen (COMPLETED):**
- Durchgeführte Arbeiten, betroffene Dateien, **tatsächlich** ausgeführte Tests mit Ergebnis, nicht ausgeführte Tests/Prüfungen mit Grund, offene Punkte und Restrisiken sowie Abschlussdatum stehen in der Task-Datei. Nichts erfinden, nichts verschweigen.
- Eine nötige Benutzerfreigabe ist erteilt und vermerkt (§8), oder der Auftrag umfasste nur den Vorschlag.
- Nötige Folgeaufträge (§7) sind angelegt und unter „Folgeschritt“ genannt.
- Ein Designvorschlag ist keine freigegebene Designentscheidung. Eine abgeschlossene Implementierung gilt nicht als QA- oder UX-geprüft. Eine bestandene UX-Prüfung ersetzt keine QA und umgekehrt. Ein Prüfauftrag gilt nicht als bestanden, nur weil Development eine Korrektur als erledigt meldet.

Danach: Status COMPLETED → Datei nach `tasks/COMPLETED/` → Zeile aus Index **und** Posteingang entfernen. Bearbeitungsverlauf knapp: eine Zeile je Statuswechsel (Datum, Rolle, Status, Kurznotiz).

## 7. Übergaben und Folgeaufträge
Fachliche Ergebnisse gehen **direkt** an die Rolle des nächsten Schritts, als Folgeauftrag mit „Abhängigkeiten: <Vorgänger-ID>“. Eine Übergabe enthält mindestens (Vorlagenfelder in Klammern): Task-Referenz (Abhängigkeiten) · bisheriger Stand, erledigte Arbeiten, relevante Erkenntnisse (Technischer Kontext) · offene Punkte (Offene Fragen) · erwarteter nächster Schritt (Erwartetes Ergebnis) · zuständige Folgerolle (Empfänger, Folgeschritt) · erforderliche Benutzerentscheidungen (Benutzerfreigabe).

Typische direkte Übergaben:
| Von | An | Wann |
|---|---|---|
| UX | Development | freigegebene Designvorgaben mit Akzeptanzkriterien |
| UX | Architecture | technische Umsetzbarkeit eines Designs unklar |
| Architecture | Development | freigegebenes technisches Konzept |
| Architecture | UX | technische Vorgaben mit Folgen für die Bedienung |
| Development | QA / UX | Implementierung zur funktionalen Prüfung / zum UX-Review (beide, falls beides nötig) |
| Development, Architecture, UX | Documentation | Änderung an dokumentiertem Verhalten, Entscheidung, Release (`PATCHNOTES.md`) |
| QA | Development | Fehler, Testlücken, nötige Korrekturen |
| QA | nächste zuständige Rolle | bestandene Prüfung, wenn ein weiterer Schritt folgt (sonst nur COMPLETED) |
| Documentation | Development / Architecture / UX / QA | fehlende oder widersprüchliche Angaben |

**Ablauf bei grösseren UI-Änderungen** (nur nötige Schritte): PM verteilt → UX analysiert und schlägt vor → Architecture bewertet bei Bedarf → Benutzer gibt Design frei → Development implementiert → Development übergibt an QA und UX → QA prüft funktional, UX prüft im Emulator → Documentation aktualisiert. Ist eine freigegebene UX-Vorgabe technisch nicht oder nur eingeschränkt umsetzbar, dokumentiert Development das und klärt es direkt mit UX (und bei Bedarf Architecture); freigegebene wesentliche Designentscheidungen werden nie stillschweigend geändert, eine Änderung braucht erneut die Benutzerfreigabe.

## 8. Benutzerfreigaben
Freigeben kann nur der Benutzer; keine Rolle ersetzt oder unterstellt eine Freigabe. Freigabepflichtig sind:
- grössere Architekturänderungen und grundlegende Technologieentscheidungen;
- wesentliche Designänderungen (Designvorschläge sind bis zur Freigabe nur Vorschläge);
- Änderungen an sicherheits- oder datenverlustkritischen Abläufen (Löschen, Ersetzen, Papierkorb, Wiederherstellen, Schutzbremse, Konflikte);
- potenziell datenverändernde oder destruktive Aktionen ausserhalb der Testdaten;
- Builds (`CLAUDE.md` §1) und Änderungen am Regelwerk (`CLAUDE.md`, diese Richtlinie, Rollenprompts, Skills, Agents);
- alles, wofür `CLAUDE.md` oder `docs/DECISIONS.md` eine Freigabe verlangt.

Die zuständige Fachrolle legt die Entscheidung dem Benutzer **direkt** in ihrem Chat vor (kompakt: Ziel, Lösung bzw. Varianten, Auswirkungen, Risiken, Empfehlung); der PM ist keine Zwischenstation. Vermerk im Feld „Benutzerfreigabe“: `erteilt <Datum>, <was>, <wo erteilt: Chat der Rolle>`. Wartet eine Aufgabe länger auf die Freigabe, BLOCKED mit der Frage. Ergebnisse dürfen vor der Freigabe weitergegeben werden (z. B. Analyse an UX); die freigabepflichtige **Umsetzung** beginnt erst danach.

**Entscheidungen melden:** Jede Rolle meldet getroffene bzw. freigegebene Entscheidungen (Architektur, Verhalten, Sicherheitsmodell, UI-Konzept, Standardwerte, Soll-Verhalten, Teststrategie, bewusst verworfene Alternativen) an Documentation – per Auftrag oder Ergänzung eines bestehenden Documentation-Auftrags – mit Entscheidung, Begründung, verworfenen Alternativen, betroffenen Komponenten, Datum und Quelle (Task-ID). Nur getroffene Entscheidungen, nie offene Fragen oder blosse Vorschläge. Freigegebene UX-Entscheidungen gehören nach `docs/DECISIONS.md` §10 (Benutzeroberfläche und UX).

## 9. Parallele Sitzungen und Wiederaufnahme
- **Vor Übernahme und vor jeder Änderung** Git-Status prüfen; der Ordner ist derzeit **kein Git-Repository**, dann gilt der Dateistand (Änderungszeit, Inhalt). Die aktuelle Task-Datei und gemeinsame Dateien (Index, Posteingänge, Doku) unmittelbar vorher erneut lesen und nur gezielt ändern (Zeile ergänzen/entfernen, nie ganze Datei überschreiben).
- **Laufende Arbeit erkennen:** IN_PROGRESS heisst „in Arbeit bei der Empfängerrolle“. Betroffene Dateien eines laufenden Auftrags fasst keine andere Rolle an; Abhängigkeiten stehen im Feld „Abhängigkeiten“.
- **Dateizuständigkeit:** je Dateibereich schreibt nur die in `CLAUDE.md` §1 (Berechtigungen) genannte Rolle; `coordination/` bearbeiten alle Rollen nach dieser Richtlinie. Dieselbe Datei nie gleichzeitig in zwei Chats bearbeiten.
- **Konkurrierende Änderung** (Datei hat sich seit dem Lesen geändert oder gehört einem laufenden Auftrag): stoppen, nichts überschreiben, Stand erneut lesen; ist keine gezielte Ergänzung möglich, mit dem Benutzer abstimmen.
- **Abgebrochene Sitzung:** Ein IN_PROGRESS-Auftrag ohne aktive Sitzung wird von derselben Rolle nach `/clear` über den Posteingang wieder aufgenommen: Bearbeitungsverlauf und Dateistand lesen, Bearbeitungsstand prüfen, eine Verlaufszeile „wieder aufgenommen“ ergänzen. Bereits dokumentierte Arbeit nicht wiederholen, fehlende Belege nicht erfinden.
- Keine eigenen Commits oder Pushes.

## 10. Nicht atomare Änderungen und Wiederherstellung
Statuswechsel in dieser Reihenfolge: (1) Status-Zeile und Verlauf in der Task-Datei, (2) Datei in den passenden Statusordner verschieben, (3) Index, (4) Posteingang(e). Bei Abbruch gilt die **Status-Zeile der Task-Datei**:
- Ordner passt nicht zur Status-Zeile → Datei verschieben.
- Index/Posteingang weichen ab → aus der Task-Datei nachziehen.
- Datei ohne Index-Eintrag → Eintrag ergänzen; Index-Eintrag ohne Datei → Eintrag entfernen und dem Benutzer melden.
- Doppelte ID → ältere behalten, Konflikt dem Benutzer melden.

**BLOCKED:** Der Empfänger setzt BLOCKED und schreibt die konkrete Frage unter „Offene Fragen“. Muss eine andere Rolle antworten, wird ein eigener Auftrag an sie erstellt und im blockierten Auftrag unter „Abhängigkeiten“ genannt; fehlt eine Benutzerentscheidung, wird sie dem Benutzer direkt vorgelegt. Die Antwort wird in der Task-Datei eingetragen; der Empfänger setzt danach IN_PROGRESS. Überblick über blockierte Aufträge gibt der Index; der PM meldet sie dem Benutzer nur, wenn dieser einen Überblick verlangt.

## 11. Konfliktbehandlung
Fachliche Konflikte klären die betroffenen Rollen zuerst **direkt** (Folgeauftrag oder Ergänzung der Task-Datei). Innerhalb ihres Bereichs entscheidet die fachlich zuständige Rolle: Architektur → Architecture, Design → UX (bei wesentlicher Änderung mit Benutzerfreigabe), Implementierungsdetail → Development, Prüfurteil → QA, Doku-Struktur → Documentation. Keine Einigung oder Benutzerentscheidung nötig → beide Positionen kurz dem Benutzer vorlegen. Der PM ist keine Pflichtstation.
| Fall | Vorgehen |
|---|---|
| UX und Development uneinig über Designumsetzung | Development dokumentiert die Einschränkung, Klärung mit UX (ggf. Architecture); wesentliche Abweichung → Benutzer |
| Architecture lehnt technische Umsetzung ab | Begründung in der Task-Datei; Alternative an Development bzw. UX; grössere Änderung → Benutzer |
| QA findet Fehler nach „fertig“ | neuer Auftrag QA → Development (mit Vorgänger-ID); kein Rücksprung aus COMPLETED |
| Documentation findet widersprüchliche Angaben | Folgeauftrag an Development/Architecture/UX; bis zur Klärung nichts dokumentieren |
| Zwei Sitzungen an derselben Datei | §9: stoppen, neu lesen, nichts überschreiben; sonst Benutzer |

## 12. Vorlage Task-Datei
```
# SYNC-YYYYMMDD-NNN – Kurztitel
- Erstellt am: YYYY-MM-DD
- Absender: <Rolle oder Benutzer>
- Empfänger: <Rolle>
- Priorität: LOW | MEDIUM | HIGH | CRITICAL
- Status: OPEN
- Übergabegrund:
- Aufgabenbeschreibung:
- Technischer Kontext: (bisheriger Stand, erledigte Arbeiten, Erkenntnisse)
- Betroffene Dateien:
- Dokumentationsverweise:
- Erwartetes Ergebnis:
- UX-Beteiligung: nötig | nicht nötig – Kurzgrund
- Benutzerfreigabe: nicht nötig | ausstehend: <was> | erteilt <Datum>, <was>, <wo>
- Abhängigkeiten: (andere Task-IDs oder –)
- Folgeschritt: (Rolle und Zweck des nächsten Auftrags oder –)
- Bearbeitungsverlauf: (je Zeile: Datum, Rolle, Status, Notiz)
- Übernahmebestätigung: (Empfänger: Datum, Rolle)
- Durchgeführte Arbeiten:
- Tests und Ergebnisse: (nur tatsächlich ausgeführte; nicht ausgeführte mit Grund)
- Offene Fragen:
- Verbleibende Risiken:
- Abgeschlossen am:
```

## 13. Beispiele (nur Illustration, keine echten Aufträge)
**A) Project Manager → UX** (`tasks/OPEN/SYNC-20261201-050.md`)
```
# SYNC-20261201-050 – Beispiel: Konfliktkarte zeigt Dateigrösse
- Erstellt am: 2026-12-01
- Absender: Project Manager
- Empfänger: UX-Specialist
- Priorität: LOW
- Status: OPEN
- Übergabegrund: Inhaber wünscht die Grösse in der Konfliktkarte.
- Aufgabenbeschreibung: Darstellung der Dateigrösse beider Versionen in der Konfliktkarte vorschlagen.
- Technischer Kontext: Anzeige nur; keine Änderung an Sync-Logik oder DB.
- Betroffene Dateien: (Development ermittelt; nicht erfunden)
- Dokumentationsverweise: docs/DECISIONS.md §10
- Erwartetes Ergebnis: Vorschlag, Freigabe des Inhabers, Designvorgabe direkt an Development.
- UX-Beteiligung: nötig – Benutzerführung bei Konflikten
- Benutzerfreigabe: ausstehend: Darstellungsvariante
- Abhängigkeiten: –
- Folgeschritt: Development (Umsetzung), danach QA und UX-Review
- Bearbeitungsverlauf: 2026-12-01 PM OPEN
- (übrige Felder: –)
```
Dazu: Zeile in `TASK-INDEX.md` (Zähler 050) und in `inbox/ux-designer.md`:
`| SYNC-20261201-050 | Project Manager | Konfliktkarte zeigt Dateigrösse | LOW | OPEN | ../tasks/OPEN/SYNC-20261201-050.md |`

**B) Rückfrage QA → Development** (QA ist in -051 blockiert, weil das Sollverhalten unklar ist): QA erstellt -052 an Development („Wird eine abgebrochene Teildatei neu übertragen oder fortgesetzt? Antwort mit Code-Stelle“, Abhängigkeiten: blockiert -051), setzt -051 auf BLOCKED (Datei nach `tasks/BLOCKED/`, Index, Posteingang). Nach der Antwort: Development COMPLETED -052, QA setzt -051 auf IN_PROGRESS. Der PM ist nicht beteiligt.
