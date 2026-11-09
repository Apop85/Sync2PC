# TASK-HANDOVER – Koordination der vier Chat-Rollen

Gemeinsames Aufgaben- und Übergabeprotokoll für **Development**, **Architecture** (Architecture & Complex Issues), **QA** (QA & Testing) und **Documentation**. Es stellt keine automatische Kommunikation her: Jeder Chat liest, übernimmt, bearbeitet und aktualisiert Aufträge selbst. Es ersetzt weder Git noch eine Konfliktlösung. Steuerung gemäss `CLAUDE.md`.

## 1. Übergabematrix (nur diese Richtungen)
| Von → An | Typische Übergabe |
|---|---|
| Development → Architecture | Komplexe Architekturprobleme, Grundsatzentscheidungen, mehrere Lösungsansätze |
| Development → QA | Unabhängige Prüfung: kritische Sync-Logik, Migrationen, Datenverlustrisiko, Nebenläufigkeit, Wiederherstellung |
| Development → Documentation | Änderungen an Architektur, Datenmodell, Sync, Teststrategie, Release-Doku |
| Architecture → Development | Freigegebene Lösungskonzepte, gezielte Korrekturen (grössere Umsetzung erst nach Freigabe durch den Benutzer) |
| Architecture → Documentation | Neue Entscheidungen, geänderte Architekturgrundlagen |
| QA → Development | Nachgewiesene Fehler, Regressionen, fehlende Implementierung, unzureichende Fehlerbehandlung (QA korrigiert nicht selbst) |
| QA → Documentation | Änderungen an Teststrategie/Testaufbau, dokumentationswürdige Qualitätsrisiken |
| Documentation → Development | Fehlende Implementierungsinformationen, unklare Abläufe |
| Documentation → Architecture | Unklare oder widersprüchliche Entscheidungen, fehlende Begründungen |

Documentation erfindet oder interpretiert nichts ungeprüft.

## 2. Wann (nicht) übergeben
- **Übergeben**, wenn die Aufgabe ausserhalb der eigenen Rolle liegt oder eine andere Rolle ein eigenständiges Ergebnis liefern muss.
- **Nicht übergeben**, wenn: Aufgabe vollständig in der eigenen Rolle liegt · Prüfung schon erfolgt · Doku direkt änderbar · interner Agent/Skill genügt · blosse Rückfrage · passender Auftrag existiert bereits (dessen Task-ID verwenden/ergänzen).

## 3. Auftrag
- **Task-ID** `SYNC-YYYYMMDD-NNN` (NNN fortlaufend pro Erstellungsdatum). **Priorität**: LOW / MEDIUM / HIGH / CRITICAL.
- **Status**: `OPEN` (erstellt, nicht übernommen) · `IN_PROGRESS` (Übernahme ausdrücklich bestätigt) · `BLOCKED` (Info/Hindernis fehlt, Klärungsbedarf eintragen) · `COMPLETED` (bearbeitet, Ergebnis dokumentiert) · `CANCELLED` (nachvollziehbar abgebrochen).
- Ein Auftrag ist erst nach ausdrücklicher Bestätigung des Empfängers übernommen und erst nach dokumentiertem Ergebnis `COMPLETED`.

## 4. Ablauf
**Absender:** diese Datei lesen → Dublette prüfen (gleiches Ziel, gleiche Dateien, schon IN_PROGRESS/COMPLETED?) → Zuständigkeit per Matrix prüfen → Auftrag mit Vorlage unter „Aufträge“ eintragen (neueste oben, Status OPEN) → Übergabe im eigenen Chat mitteilen. Der Absender bleibt für Integration und Bewertung des Ergebnisses verantwortlich; Folgeaufgaben werden eigene Aufträge.

**Empfänger:** Datei lesen → prüfen, ob an die eigene Rolle adressiert und Information ausreicht → Übernahme ausdrücklich bestätigen (oder begründet ablehnen) → `IN_PROGRESS` → bei Lücken `BLOCKED` → nach Abschluss durchgeführte Arbeiten, geänderte Dateien, Tests/Ergebnisse, nicht durchgeführte Prüfungen, Restrisiken, Abschlussdatum eintragen → erst dann `COMPLETED`.

**Parallele Änderungen:** Vor jeder Dateiänderung diese Datei erneut lesen und den Dateistand der betroffenen Dateien prüfen (der Ordner ist derzeit kein Git-Repository; mit Repository zusätzlich Git-Status). Keine gleichzeitigen Schreibzugriffe auf dieselben Dateien, keine fremden Änderungen überschreiben; bei Konflikt stoppen, abstimmen, nötigenfalls `BLOCKED`.

**Token:** Nur offene, an die eigene Rolle adressierte Aufträge lesen. Erledigte nicht neu analysieren. Knapp schreiben; keine Chatverläufe, Analysen oder Quellcodekopien eintragen. Aufträge werden nicht stillschweigend gelöscht; bei sehr vielen erledigten kann eine Archivierung vereinbart werden.

## 5. Vorlage
```
### SYNC-YYYYMMDD-NNN – Kurztitel
- Erstellt am: YYYY-MM-DD
- Absender: Development | Architecture | QA | Documentation
- Empfänger: Development | Architecture | QA | Documentation
- Priorität: LOW | MEDIUM | HIGH | CRITICAL
- Status: OPEN
- Übergabegrund:
- Aufgabenbeschreibung:
- Technischer Kontext:
- Betroffene Dateien:
- Erwartetes Ergebnis:
- Tatsächlich durchgeführte Arbeiten: (Empfänger)
- Ergebnis und Rückmeldung: (Empfänger; Tests, Restrisiken, offene Punkte)
- Abgeschlossen am:
```

## 6. Beispiel (nur Illustration, kein echter Auftrag)
```
### SYNC-20261108-001 – Unabhängige Prüfung Änderung am Papierkorb-Restore
- Erstellt am: 2026-11-08
- Absender: Development
- Empfänger: QA
- Priorität: HIGH
- Status: OPEN
- Übergabegrund: Änderung an Wiederherstellungslogik mit Datenverlustrisiko, unabhängige Prüfung nötig.
- Aufgabenbeschreibung: Funktionale Korrektheit, Regressionen und Datenverlustrisiken prüfen; wiederholte Läufe und Abbruch mitten im Restore.
- Technischer Kontext: Rollback in MirrorEngine.runRestores angepasst.
- Betroffene Dateien: (tatsächlich geänderte Dateien eintragen)
- Erwartetes Ergebnis: Risikobewertung, ausgeführte Tests mit Ergebnis, Fehler/Testlücken, Empfehlungen.
- Tatsächlich durchgeführte Arbeiten: –
- Ergebnis und Rückmeldung: –
- Abgeschlossen am: –
```

## 7. Speicherorte Posteingänge zur Übergabe der Tasks
Architecture & Complex Issues: coordination\inbox\architecture.md
Development: coordination\inbox\development.md
Documentation: coordination\inbox\documentation.md
QA & Testing: coordination\inbox\qa-testing.md

