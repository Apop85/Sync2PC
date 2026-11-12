# Koordinationsrichtlinie – sechs Chat-Rollen

Verbindliche, zentrale Referenz für die Zusammenarbeit der Chat-Rollen. Dezentrales Markdown-System ohne automatische Kommunikation zwischen den Chats. **Geschrieben wird ausschliesslich über `tools\Auftrag.ps1`** (§13; globale Sperre, atomares Schreiben, Sicherung, Protokoll, DEC-264); das Skript bildet diese Richtlinie ab und erzeugt Index und Posteingänge. Es ersetzt nicht die Freigabe durch den Benutzer (Inhaber).

## 0. Geltung und Regelhierarchie
1. `CLAUDE.md` – globale Projektregeln, Sicherheit, zentrale Berechtigungen.
2. Diese Richtlinie – Aufgaben, Status, Übergaben, Freigaben, parallele Arbeit, Konflikte.
3. Rollenprompts `.claude/roles/*.md` – fachliche Verantwortung und rollenspezifische Arbeitsweise.
4. Task-Dateien und Übergaben – der konkrete Auftrag.

Bei Widerspruch gilt die höhere Stufe. Rollenprompts schaffen keine Ausnahmen von zentralen Berechtigungen; Task-Dateien heben keine Rollengrenzen auf. Ist ein Widerspruch nicht eindeutig lösbar: dokumentieren (Task-Datei, „Offene Fragen“) und dem Benutzer vorlegen, keine eigenmächtige Regeländerung. Agents (`.claude/agents/`) sind Werkzeuge innerhalb einer Chat-Rolle, keine Rollen; sie haben höchstens die Rechte der aufrufenden Rolle.

## 1. Aufbau
```
coordination/
├── README.md        diese Richtlinie (Regelwerk)
├── TASK-INDEX.md    erzeugt: aktive Aufträge, eine Zeile je Auftrag, plus Zähler „Letzte vergebene Nummer“
├── TASK-HANDOVER.md nur Verweisdatei (Altbestand bis 2026-11-09)
├── inbox/           erzeugt: je Rolle ein Posteingang, nur Verweiszeilen auf aktive Aufträge
│   project-manager.md · architecture.md · development.md · qa-testing.md · documentation.md · ux-designer.md
├── tasks/           eine Markdown-Datei je Auftrag, nach Status sortiert
│   OPEN/ · IN_PROGRESS/ · BLOCKED/ · COMPLETED/   (COMPLETED enthält auch CANCELLED, schreibgeschützt)
├── .history/        Skript: Sicherung jeder Altfassung je ID (höchstens 20)
├── .audit.log       Skript: Protokoll jeder schreibenden Aktion (nur anhängen)
├── .lock            Skript: globale Sperre (offener Datei-Handle, 0 Byte)
└── archive/         Sicherungen (vor-migration-2026-11-09, vor-refactoring-2026-11-10, vor-umstellung-auftrag-2026-11-12); nicht lesen
```
- **Die Task-Datei ist die einzige Quelle** für Inhalt und Status. Index und Posteingang enthalten nur Metadaten/Verweise und werden bei jeder schreibenden Aktion neu erzeugt.
- **Schreiben nur über `tools\Auftrag.ps1`** (alle Rollen und Agents): Task-Dateien, `TASK-INDEX.md`, `inbox/`, `.history/`, `.audit.log` und `.lock` werden nie mit Edit/Write, Shell-Befehlen (`Set-Content`, `Out-File`, `Copy-Item`, `Move-Item`, `Rename-Item`, `Remove-Item`, Attribute …) oder von Hand geändert, auch nicht zur Reparatur (dafür `check -Fix`, §10). Technisch abgesichert (seit 2026-11-12, gegen Versehen, nicht gegen Absicht): `.claude/settings.json` sperrt Edit/Write/NotebookEdit auf diese Pfade (`permissions.deny`, wirkt auch auf von Claude Code erkannte Shell-Dateibefehle); der Hook `tools\Auftrag-guard.ps1` blockiert dieselben Werkzeuge und verlangt bei sonstigen Shell-Befehlen mit Schreibverb auf diese Pfade eine Rückfrage an den Benutzer (Aufrufe von `Auftrag.ps1` sind frei). Die Rückfrage nur zulassen, wenn der Benutzer den direkten Zugriff ausdrücklich will. `README.md` ist Regelwerk (`CLAUDE.md` §1); `TASK-HANDOVER.md` ist eine Verweisdatei und wird nur auf ausdrücklichen Auftrag des Benutzers geändert. Eingabedateien für `-Eingabe` legt jede Rolle im Scratchpad ausserhalb des Projekts an (§13).
- **Lesen:** Posteingang und Überblick über `liste` (§13), nicht über die Dateien. Direkt gelesen wird nur die Task-Datei des Auftrags, den man übernimmt oder bearbeitet, und zwar einmal (danach genügt die Skriptausgabe). Alles Übrige (Index, `inbox/`-Dateien, `tasks/COMPLETED/`, `.history/`, `.audit.log`, `archive/`) nur ausnahmsweise mit konkretem Grund, z. B. eine in „Abhängigkeiten“ genannte Vorgänger-Datei, deren Inhalt der Auftrag braucht, oder Fehlersuche nach Exit-Code 3, 5 oder 6.
- Alte Verweise der Form `inbox/<rolle>.md SYNC-…` (vor 2026-11-09) bedeuten `tasks/COMPLETED/SYNC-….md`. Ältere Task-Dateien ohne die neuen Vorlagenfelder (§12) bleiben gültig und werden nicht migriert.

## 2. Rollen und Zuständigkeiten
Jeder Auftrag hat **genau eine verantwortliche Rolle: den Empfänger**. Andere Rollen wirken nur über eigene Aufträge mit (unterstützend oder prüfend).

| Rolle (Posteingang) | Entscheidet | Setzt um | Prüft |
|---|---|---|---|
| Project Manager (`project-manager`) | Aufgabenzerlegung, Rollenwahl, Reihenfolge, Priorität | Aufträge für Benutzeraufträge (`erstellen`) | – |
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
`CLAUDE.md` wird immer geladen, die Rolle nicht. Nach Start oder `/clear` den Rollen-Skill aufrufen: `/rolle-pm`, `/rolle-architecture`, `/rolle-development`, `/rolle-qa`, `/rolle-documentation`, `/rolle-ux`. Der Skill lädt den Masterprompt, ruft den eigenen Posteingang mit `liste` ab (§13) und meldet offene Aufträge; keine Projektanalyse, keine Übernahme ohne Bestätigung. Alternativ nennt der Benutzer die Rolle; dann denselben Ablauf von Hand.

## 5. Neuen Auftrag erstellen (Absender)
1. Empfänger nach Matrix (§3) und Zuständigkeit (§2) prüfen; UX-Beteiligung festlegen. Gibt es schon einen aktiven Auftrag mit gleichem Ziel oder einen eigenen OPEN-Auftrag an dieselbe Rolle zum selben Stand (z. B. Doku- oder Prüfpunkte derselben Version), diesen ergänzen (`aendern -Feld Aufgabe -Anhaengen`) statt einen neuen anzulegen (`CLAUDE.md` §8).
2. `Auftrag.ps1 erstellen` aufrufen (§13). Das Skript vergibt die ID (`SYNC-YYYYMMDD-NNN`, global fortlaufend), legt die Datei nach Vorlage §12 in `tasks/OPEN/` an, erzeugt Index und Posteingänge und prüft Dubletten; Index und Posteingänge werden dafür nicht gelesen. Ähnlicher Titel mit mindestens zwei gemeinsamen Wörtern: Abbruch mit Exit-Code 3, nichts angelegt; dann den bestehenden Auftrag ergänzen (`aendern`) oder, wenn es wirklich ein eigener Auftrag ist, mit `-Trotzdem` anlegen. Nur Warnung: ähnlicher kurzer Titel, gleiche Datei in einem aktiven Auftrag, Auftrag an die eigene Rolle oder an den Project Manager. In `-Abhaengigkeiten` genannte Vorgänger sind von der Prüfung ausgenommen. Erweist sich ein angelegter Auftrag doch als Dublette, zieht der Absender ihn mit `status -Neu CANCELLED -Notiz "Dublette von <ID>"` zurück.
3. Inhalt vollständig, aber kompakt; keine Chatverläufe, keine Quellcodekopien, keine erfundenen Dateipfade (Unbekanntes kennzeichnen). Längere Berichte oder Konzepte als eigener Abschnitt (`aendern -Abschnitt`).
4. Im eigenen Chat ID und Empfänger melden (der Benutzer überbringt sie; es gibt keine automatische Benachrichtigung).

**Priorität:** CRITICAL = akute Gefahr für Datenintegrität, Datenverlust oder Grundfunktion · HIGH = wichtige Funktion, kritischer Fehler, nötige Architekturverbesserung · MEDIUM = reguläre Funktion/Verbesserung · LOW = optional, Komfort, Kleinigkeit. HIGH/CRITICAL kurz begründen (`-Begruendung`, Pflicht).

## 6. Statusmodell, Übernahme, Abschluss
| Status | Bedeutung | Wechsel durch |
|---|---|---|
| OPEN | erstellt, nicht übernommen | Absender |
| IN_PROGRESS | Empfänger hat Übernahme ausdrücklich bestätigt | Empfänger |
| BLOCKED | Information, Freigabe oder Vorarbeit fehlt; Frage unter „Offene Fragen“ | Empfänger |
| COMPLETED | Arbeit erledigt und dokumentiert | Empfänger |
| CANCELLED | abgebrochen, mit Begründung | Absender oder Benutzer |

Zulässige Wechsel: OPEN → IN_PROGRESS · OPEN → BLOCKED · IN_PROGRESS ↔ BLOCKED · IN_PROGRESS → COMPLETED · OPEN/IN_PROGRESS/BLOCKED → CANCELLED. Kein Rücksprung aus COMPLETED/CANCELLED: stattdessen Folgeauftrag. Weitere Statuswerte gibt es nicht; Zwischenstände (z. B. „Designvorschlag erstellt“) stehen im Bearbeitungsverlauf (`aendern -Feld Bearbeitungsverlauf`). Statuswechsel nur über `status` bzw. `abschliessen` (§13); das Skript prüft Wechsel und Rolle.

**Übernahme (Empfänger):** nur an die eigene Rolle adressierte Aufträge → Task-Datei einmal lesen, Dateistand der betroffenen Projektdateien prüfen (§9) → `status -Neu IN_PROGRESS` (setzt die Übernahmebestätigung mit Datum und Rolle). Fehlt etwas: `status -Neu BLOCKED -Frage "…"`. Nicht Zuständiges begründet ablehnen: `aendern -Feld Bearbeitungsverlauf -Wert "abgelehnt: <Grund>, zuständig: <Rolle>"`, Absender im Chat informieren; der Absender setzt CANCELLED oder erstellt den Auftrag an die richtige Rolle.

**Verantwortung für das Ergebnis:** allein beim Empfänger. Der Absender wertet das Ergebnis nur aus, wenn er es für eine eigene, noch offene Aufgabe braucht; der PM wertet Ergebnisse nicht standardmässig aus.

**Abschlussbedingungen (COMPLETED):**
- Durchgeführte Arbeiten, betroffene Dateien, **tatsächlich** ausgeführte Tests mit Ergebnis, nicht ausgeführte Tests/Prüfungen mit Grund, offene Punkte und Restrisiken sowie Abschlussdatum stehen in der Task-Datei. Nichts erfinden, nichts verschweigen.
- Eine nötige Benutzerfreigabe ist erteilt und vermerkt (§8), oder der Auftrag umfasste nur den Vorschlag.
- Nötige Folgeaufträge (§7) sind angelegt und unter „Folgeschritt“ genannt.
- Ein Designvorschlag ist keine freigegebene Designentscheidung. Eine abgeschlossene Implementierung gilt nicht als QA- oder UX-geprüft. Eine bestandene UX-Prüfung ersetzt keine QA und umgekehrt. Ein Prüfauftrag gilt nicht als bestanden, nur weil Development eine Korrektur als erledigt meldet.

**Abschluss:** zuerst nötige Folgeaufträge erstellen, dann `abschliessen` mit `-Arbeiten` (durchgeführte Arbeiten und betroffene Dateien), `-Tests` (tatsächlich ausgeführte Tests mit Ergebnis, nicht ausgeführte mit Grund), bei Bedarf `-Risiken` (offene Punkte, Restrisiken) und `-Folgeschritt` (IDs und Empfänger der Folgeaufträge). Das Skript setzt Status und „Abgeschlossen am“, verschiebt nach `tasks/COMPLETED/`, setzt den Schreibschutz und erzeugt Index und Posteingänge. Bearbeitungsverlauf knapp: das Skript schreibt je Statuswechsel eine Zeile (Datum, Rolle, Status, `-Notiz`). Danach sind nur noch Ergänzungen durch Anhängen möglich (§13).

## 7. Übergaben und Folgeaufträge
Fachliche Ergebnisse gehen **direkt** an die Rolle des nächsten Schritts, als Folgeauftrag mit „Abhängigkeiten: <Vorgänger-ID>“. Der Folgeauftrag ist in sich vollständig: die für den nächsten Schritt nötigen Erkenntnisse stehen im „Technischen Kontext“, damit der Empfänger die Vorgänger-Datei nicht lesen muss (Token); die Vorgänger-ID dient nur als Beleg. Eine Übergabe enthält mindestens (Vorlagenfelder in Klammern): Task-Referenz (Abhängigkeiten) · bisheriger Stand, erledigte Arbeiten, relevante Erkenntnisse (Technischer Kontext) · offene Punkte (Offene Fragen) · erwarteter nächster Schritt (Erwartetes Ergebnis) · zuständige Folgerolle (Empfänger, Folgeschritt) · erforderliche Benutzerentscheidungen (Benutzerfreigabe).

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
- Builds (`CLAUDE.md` §1; Ausnahme: Debug-/Test-Builds, Tests und Emulator-Installationen durch QA & Testing sind ohne Einzelfreigabe erlaubt, Release-Builds nicht) und Änderungen am Regelwerk (`CLAUDE.md`, diese Richtlinie, Rollenprompts, Skills, Agents);
- alles, wofür `CLAUDE.md` oder `docs/DECISIONS.md` eine Freigabe verlangt.

Die zuständige Fachrolle legt die Entscheidung dem Benutzer **direkt** in ihrem Chat vor (kompakt: Ziel, Lösung bzw. Varianten, Auswirkungen, Risiken, Empfehlung); der PM ist keine Zwischenstation. Vermerk im Feld „Benutzerfreigabe“: `erteilt <Datum>, <was>, <wo erteilt: Chat der Rolle>` (beim Erstellen `-Freigabe "…"`, später `aendern -Feld Benutzerfreigabe -Wert "…"`; nicht verwechseln mit dem Parameter `-Benutzerfreigabe`, §13). Wartet eine Aufgabe länger auf die Freigabe, BLOCKED mit der Frage. Ergebnisse dürfen vor der Freigabe weitergegeben werden (z. B. Analyse an UX); die freigabepflichtige **Umsetzung** beginnt erst danach.

**Entscheidungen melden:** Jede Rolle meldet getroffene bzw. freigegebene Entscheidungen (Architektur, Verhalten, Sicherheitsmodell, UI-Konzept, Standardwerte, Soll-Verhalten, Teststrategie, bewusst verworfene Alternativen) an Documentation – per Auftrag oder Ergänzung eines bestehenden Documentation-Auftrags – mit Entscheidung, Begründung, verworfenen Alternativen, betroffenen Komponenten, Datum und Quelle (Task-ID). Nur getroffene Entscheidungen, nie offene Fragen oder blosse Vorschläge. Freigegebene UX-Entscheidungen gehören nach `docs/DECISIONS.md` §10 (Benutzeroberfläche und UX).

## 9. Parallele Sitzungen und Wiederaufnahme
- **Vor Übernahme und vor jeder Änderung an Projektdateien** den Dateistand prüfen (Änderungszeit, Inhalt; Git ist nicht vorhanden, daher können keine Git-Befehle ausgeführt werden). Gemeinsame Projektdateien (z. B. Doku) unmittelbar vorher erneut lesen und nur gezielt ändern. Für `coordination/` übernimmt das Skript Sperre und Konflikterkennung: Task-Datei, Index und Posteingänge **nicht** vor jedem Aufruf neu lesen.
- **Laufende Arbeit erkennen:** IN_PROGRESS heisst „in Arbeit bei der Empfängerrolle“. Betroffene Dateien eines laufenden Auftrags fasst keine andere Rolle an; Abhängigkeiten stehen im Feld „Abhängigkeiten“. Prüfen über `liste -Text <Datei oder Stichwort>`; `erstellen` warnt zusätzlich bei gleichen Dateien.
- **Dateizuständigkeit:** je Dateibereich schreibt nur die in `CLAUDE.md` §1 (Berechtigungen) genannte Rolle; `coordination/` ändern alle Rollen ausschliesslich über `tools\Auftrag.ps1` (§1, §13). Dieselbe Datei nie gleichzeitig in zwei Chats bearbeiten.
- **Konkurrierende Änderung:** an einer Projektdatei (seit dem Lesen geändert oder Teil eines laufenden Auftrags): stoppen, nichts überschreiben, Stand erneut lesen; ist keine gezielte Ergänzung möglich, mit dem Benutzer abstimmen. An einer Task-Datei meldet das Skript Exit-Code 3 und schreibt nichts (§13).
- **Abgebrochene Sitzung:** Ein IN_PROGRESS-Auftrag ohne aktive Sitzung wird von derselben Rolle nach `/clear` über den Posteingang wieder aufgenommen: Task-Datei (Bearbeitungsverlauf) und Dateistand lesen, Bearbeitungsstand prüfen, `aendern -Feld Bearbeitungsverlauf -Wert "wieder aufgenommen"`. Bereits dokumentierte Arbeit nicht wiederholen, fehlende Belege nicht erfinden.
- Keine eigenen Commits oder Pushes.

## 10. Nicht atomare Änderungen und Wiederherstellung
Das Skript schreibt in fester Reihenfolge: (1) Task-Datei atomar ersetzen (vorher Sicherung in `.history/`), (2) in den passenden Statusordner verschieben, (3) bei COMPLETED/CANCELLED Schreibschutz, (4) Index, (5) Posteingänge, (6) Audit-Log. Bei Abbruch gilt die **Status-Zeile der Task-Datei**; Wiederherstellung nur mit `check` (meldet) und `check -Fix -Rolle <Rolle>` (repariert), nie von Hand:
- `-Fix` behebt: Ordner passt nicht zur Status-Zeile, fehlender Schreibschutz, BOM, verwaiste Temp-Dateien, abweichender Index oder Posteingang.
- Nur gemeldet, dem Benutzer vorlegen: doppelte Nummer (ältere behalten), leere oder unvollständige Datei, mehrdeutige Datei, Empfänger nicht zuordenbar, fehlendes Pflichtfeld, fremde Datei unter `tasks/`.
- Meldet ein Aufruf „ist geschrieben, aber Übersicht/Audit fehlgeschlagen“: **nicht wiederholen**, sondern `check -Fix`.

**BLOCKED:** Der Empfänger setzt `status -Neu BLOCKED -Frage "<konkrete Frage>"` (das Skript trägt sie unter „Offene Fragen“ ein). Muss eine andere Rolle antworten, wird ein eigener Auftrag an sie erstellt und im blockierten Auftrag unter „Abhängigkeiten“ genannt (`aendern -Feld Abhängigkeiten -Wert "…" -Anhaengen`, ohne `-Anhaengen` würde der bisherige Wert ersetzt); fehlt eine Benutzerentscheidung, wird sie dem Benutzer direkt vorgelegt. Die Antwort wird mit `aendern -Feld "Offene Fragen" -Wert "<Datum> Antwort: …"` eingetragen; der Empfänger setzt danach `status -Neu IN_PROGRESS`. Überblick über blockierte Aufträge: `liste -Status BLOCKED`; der PM meldet sie dem Benutzer nur, wenn dieser einen Überblick verlangt.

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
Die Vorlage erzeugt `erstellen` (Kopfzeile `# <ID> – <Titel>`, Felder in der Reihenfolge unten, leere Felder entfallen). Nicht zutreffende optionale Felder dürfen entfallen. Pflichtfelder sind: ID/Titel, Erstellt am, Absender, Empfänger, Priorität, Status, Aufgabenbeschreibung, Erwartetes Ergebnis, Bearbeitungsverlauf und Übernahmebestätigung; „Erstellt am“, Absender, Status, Verlauf und Übernahmebestätigung setzt das Skript.

Weitere Felder bei Bedarf ergänzen:
- Übergabegrund
- Technischer Kontext
- Betroffene Dateien
- Dokumentationsverweise
- UX-Beteiligung
- Benutzerfreigabe
- Abhängigkeiten
- Folgeschritt
- Durchgeführte Arbeiten
- Tests und Ergebnisse
- Offene Fragen
- Verbleibende Risiken
- Abgeschlossen am

## 13. Bedienung `tools\Auftrag.ps1`
Vollständige Referenz; das Skript muss dafür nicht gelesen werden. Kurzfassung für den Alltag: `CLAUDE.md` §10. Parameter und Beispiele zeigt auch das Skript selbst: `Auftrag.ps1 -Help` (Übersicht) bzw. `Auftrag.ps1 <Befehl> -Help` (Alias `-Hilfe`).

**Aufruf** (PowerShell-Werkzeug, Projektwurzel): `powershell -NoProfile -ExecutionPolicy Bypass -File tools\Auftrag.ps1 <Befehl> -Rolle <eigene Rolle> …`. In den Beispielen steht `Auftrag.ps1` für diesen ganzen Aufruf.
- `-Rolle` (Pflicht bei schreibenden Befehlen): `PM`, `Architecture`, `Development`, `QA`, `Documentation`, `UX`, `Benutzer` (Langformen wie „QA & Testing“ gehen auch). Immer die Rolle des eigenen Chats; Agents verwenden die Rolle des aufrufenden Chats.
- `-Id`: volle ID oder nur die Nummer (`-Id 259`).
- `-WhatIf`: zeigt die geplante Änderung und Warnungen (bei `erstellen` auch den ganzen Inhalt), schreibt nichts. `-Json`: Ausgabe als JSON, nur für maschinelle Auswertung (länger).
- Ausgabe: eine Erfolgszeile, ggf. `WARNUNG:`- und `FEHLER:`-Zeilen. **Nach Exit-Code 0 nichts nachlesen** (weder Task-Datei noch Index noch Posteingang); Warnungen beachten.
- Einzeilige Werte als Parameter in `"…"` (Umlaute gehen). Mehrzeiliges oder Text mit `"`, `` ` ``, `$` → `-Eingabe <Datei>` (unten). Parameter haben Vorrang vor der Datei.

| Befehl | Zweck | Parameter (Pflicht **fett**) | Wer |
|---|---|---|---|
| `erstellen` | neuer Auftrag (§5) | **`-An`**, **`-Titel`**, **`-Prio`** LOW/MEDIUM/HIGH/CRITICAL, **`-Aufgabe`**, **`-Ergebnis`**; **`-Begruendung`** bei HIGH/CRITICAL; optional `-Uebergabegrund`, `-Kontext`, `-Dateien`, `-Abhaengigkeiten`, `-UX`, `-Freigabe`, `-Folgeschritt`, `-Trotzdem` (mögliche Dublette bewusst anlegen, §5) | Absender |
| `status` | Statuswechsel ausser Abschluss (§6) | **`-Id`**, **`-Neu`** `IN_PROGRESS` (Übernahme bzw. zurück aus BLOCKED) · `BLOCKED` mit **`-Frage`** · `CANCELLED` mit **`-Notiz`** (Begründung); optional `-Notiz` | IN_PROGRESS/BLOCKED: Empfänger; CANCELLED: Absender oder Benutzer |
| `abschliessen` | IN_PROGRESS → COMPLETED (§6) | **`-Id`**, **`-Arbeiten`**, **`-Tests`** (beide Pflicht, sofern nicht schon in der Datei); optional `-Risiken`, `-Folgeschritt`, `-Notiz` | Empfänger |
| `aendern` | Feld setzen/ergänzen oder Abschnitt anhängen | **`-Id`** und entweder **`-Feld "<Feldname>"`** mit `-Wert "…"` oder `-Eingabe` (optional `-Anhaengen`, `-Notiz`) oder **`-Abschnitt "<Titel>"`** mit **`-Eingabe`** | Empfänger für die eigene Arbeit; andere Rollen nur für Ergänzungen nach dieser Richtlinie (Antwort auf Offene Fragen, Freigabe-Vermerk, Ergänzung statt Dublette §3, Vermerk von Documentation) |
| `liste` | Überblick, nur lesend | mit `-Rolle` (ohne `-An`): eigener Posteingang, Standard OPEN,IN_PROGRESS,BLOCKED; ohne `-Rolle` (oder `-Rolle Benutzer`) bzw. mit `-An`: alle bzw. die genannte Rolle, Standard OPEN,IN_PROGRESS (BLOCKED gezählt). Filter `-An`, `-Von`, `-Status` (mehrere mit Komma; `Alle`), `-Prio`, `-Text` (Volltext), `-IdVon`/`-IdBis` (Nummern), `-Seit`/`-Bis` (Erstellt am, `yyyy-MM-dd`); `-MitDateien` zeigt zusätzlich „Betroffene Dateien“ (Gruppieren ohne Task-Dateien zu lesen) | alle |
| `check` | Konsistenzprüfung, nur lesend; `-Fix` repariert (§10) | `-Fix` braucht `-Rolle`; Fehler einzeln, Hinweise (Altformat, ohne Handlungsbedarf) nur gezählt, einzeln mit `-Details` | alle |

**Regeln für `aendern`:** Jede Änderung schreibt automatisch eine Verlaufszeile. Ersetzt wird der Feldwert (fehlt das Feld, wird es eingefügt); immer angehängt werden Bearbeitungsverlauf, Durchgeführte Arbeiten, Tests und Ergebnisse, Offene Fragen; `-Anhaengen` hängt auch an andere Felder an. Nicht änderbar: ID/Titel, Status, Absender, Empfänger, Erstellt am, Übernahmebestätigung, Abgeschlossen am. Bei COMPLETED/CANCELLED nur Anhängen (`-Anhaengen` oder Anhängefelder) und `-Abschnitt`. `-Feld Bearbeitungsverlauf -Wert "x"` ergibt die Verlaufszeile „<Datum> <Rolle>: x“. Feldnamen wie in §12 oder die Kurznamen `Aufgabe`, `Ergebnis`, `Dateien`, `Kontext`, `UX`, `Freigabe`, `Prio`, `Arbeiten`, `Risiken`, `Fragen`, `Verlauf`; Umlaute oder ae/oe/ue, Gross-/Kleinschreibung egal.

**`-Benutzerfreigabe "<Grund>"`** ist kein Feld, sondern die protokollierte Ausnahme von der Rollenprüfung (Exit 2), z. B. wenn der Benutzer im Chat ausdrücklich erlaubt, einen Auftrag einer anderen Rolle zu übernehmen oder abzubrechen. Nie ohne diese ausdrückliche Erlaubnis verwenden. Den Freigabe-Vermerk eines Auftrags setzt `-Freigabe` bzw. `aendern -Feld Benutzerfreigabe` (§8).

**`-Eingabe <Datei>`:** UTF-8-Datei (mit dem Write-Werkzeug) im Scratchpad des Chats, nie unter `coordination/`; nach Gebrauch darf sie liegen bleiben. Format wie die Task-Datei:
```
- An: Development
- Titel: Kurzer Titel
- Prio: HIGH
- Begruendung: warum HIGH
- Aufgabe: erste Zeile
  Folgezeilen immer mit zwei Leerzeichen einrücken,
  - auch Aufzählungen: eingerückt
- Ergebnis: …
```
- Schlüssel: die Parameternamen ohne `-` (`An`, `Titel`, `Prio`, `Aufgabe`, `Ergebnis`, `Begruendung`, `Uebergabegrund`, `Kontext`, `Dateien`, `Abhaengigkeiten`, `UX`, `Freigabe`, `Folgeschritt`; bei `abschliessen` `Arbeiten`, `Tests`, `Risiken`, `Folgeschritt`, `Notiz`; bei `status` `Neu`, `Notiz`, `Frage`) oder die Feldnamen aus §12.
- Eine Zeile in Spalte 0 der Form `- Name:` beginnt immer ein neues Feld. Unbekannte Namen, `#`-Überschriften und Text vor dem ersten Feld lehnen `erstellen`, `status` und `abschliessen` mit Exit-Code 1 ab; Abschnitte kommen nach dem Anlegen mit `aendern -Abschnitt`.
- `aendern -Abschnitt "<Titel>" -Eingabe <Datei>`: der ganze Dateiinhalt wird als `## <Titel>` ans Dateiende gehängt (Leerraum am Rand und Zeilenenden werden angeglichen) (Überschriften, Listen, Tabellen erlaubt). `aendern -Feld X -Eingabe <Datei>`: enthält die Datei ein Feld `- X:`, gilt dessen Wert, sonst der ganze Inhalt.

**Exit-Codes und Reaktion:** 0 ok (Warnungen möglich) · 1 Eingabe/Pflichtfeld: Meldung lesen, korrigieren, erneut aufrufen · 2 Berechtigung: falsche Rolle oder fremder Auftrag; nicht umgehen, zuständige Rolle bzw. Benutzer fragen · 3 Zustandskonflikt (unzulässiger Wechsel, mehrdeutige Datei, seit dem Lesen geändert) oder bei `erstellen` mögliche Dublette (Meldung nennt die ID): `liste -Text <ID> -Status Alle` bzw. Task-Datei lesen, dann entscheiden (Dublette: bestehenden Auftrag ergänzen oder `-Trotzdem`); mehrdeutige Datei dem Benutzer melden · 4 Sperre: nach einigen Sekunden wiederholen · 5 `check` fand Fehler: `check -Fix -Rolle <Rolle>`, Rest dem Benutzer melden · 6 E/A-Fehler: bei „ist geschrieben, aber …“ nicht wiederholen, sondern `check -Fix`; sonst Meldung dem Benutzer vorlegen.

**Beispiele** (Rolle jeweils die eigene):
```
Auftrag.ps1 liste -Rolle QA                                            # eigener Posteingang inkl. BLOCKED
Auftrag.ps1 liste -Rolle QA -MitDateien                                # dazu Betroffene Dateien (Rollenstart, Gruppieren)
Auftrag.ps1 liste -Status BLOCKED                                      # alle blockierten
Auftrag.ps1 liste -Text "SyncEngine.kt"                                # laufende Arbeit an einer Datei
Auftrag.ps1 erstellen -Rolle Development -An QA -Prio MEDIUM -Titel "Prüfung Papierkorb-Filter (aus -249)" -Aufgabe "Filter funktional prüfen" -Ergebnis "Prüfbericht" -Abhaengigkeiten "SYNC-20261112-249" -WhatIf
Auftrag.ps1 erstellen -Rolle Architecture -Eingabe <Scratchpad>\auftrag.md
Auftrag.ps1 status -Rolle QA -Id 260 -Neu IN_PROGRESS
Auftrag.ps1 status -Rolle QA -Id 260 -Neu BLOCKED -Frage "Welche App-Version ist installiert?"
Auftrag.ps1 aendern -Rolle Development -Id 260 -Feld "Offene Fragen" -Wert "2026-11-12 Antwort: 0.16.35"
Auftrag.ps1 status -Rolle QA -Id 260 -Neu IN_PROGRESS -Notiz "Antwort erhalten"
Auftrag.ps1 aendern -Rolle UX -Id 251 -Feld Bearbeitungsverlauf -Wert "Designvorschlag erstellt"
Auftrag.ps1 aendern -Rolle UX -Id 251 -Abschnitt "Designvorschlag" -Eingabe <Scratchpad>\vorschlag.md
Auftrag.ps1 aendern -Rolle UX -Id 251 -Feld Benutzerfreigabe -Wert "erteilt 2026-11-12, Variante B, Chat UX"
Auftrag.ps1 abschliessen -Rolle QA -Id 260 -Arbeiten "Filter geprüft (PapierkorbScreen)" -Tests "TrashFilterTest bestanden; Emulator nicht geprüft: kein Gerät" -Folgeschritt "SYNC-20261112-261 an Development"
Auftrag.ps1 status -Rolle Development -Id 260 -Neu CANCELLED -Notiz "Dublette von SYNC-20261112-255"
Auftrag.ps1 aendern -Rolle Documentation -Id 255 -Feld "Durchgeführte Arbeiten" -Wert "DEC-265 ergänzt"   # auch nach Abschluss
Auftrag.ps1 check
```
Tests auf einer Kopie (nur Selbsttest/QA): `$env:AUFTRAG_TESTMODUS='1'` und `-Wurzel <Kopie von coordination>`; `-Wurzel` geht nur im Testmodus. Selbsttest: `tools\Auftrag-Selbsttest.ps1`.
