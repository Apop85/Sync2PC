TASK-HANDOVER – Chat Coordination
1. Zweck

Diese Datei ist das gemeinsame Aufgaben- und Übergabeprotokoll der vier Claude-Code-Chats:

Development
Architecture & Complex Issues
QA & Testing
Documentation

Sie ermöglicht strukturierte Aufgabenübergaben und nachvollziehbare Statusverfolgung.

Sie stellt keine automatische Kommunikation zwischen Chats her.

Jeder Chat ist selbst dafür verantwortlich, relevante Aufträge zu lesen, anzunehmen, zu bearbeiten und den Status zu aktualisieren.

Die zentrale Steuerung erfolgt gemäß CLAUDE.md.

2. Verbindliche Übergabematrix
Absender	Empfänger	Typische Übergabe
Development	Architecture	Komplexe Architekturprobleme, grundlegende Designentscheidungen
Development	QA & Testing	Unabhängige funktionale Prüfung, Regressionstests
Development	Documentation	Aktualisierung technischer Dokumentationen
Architecture	Development	Freigegebene technische Lösungskonzepte zur Implementierung
Architecture	Documentation	Neue Architekturentscheidungen und Dokumentationsänderungen
QA & Testing	Development	Nachgewiesene Fehler, notwendige Korrekturen
QA & Testing	Documentation	Dokumentation von Testergebnissen oder Teststrategie
Documentation	Development	Fehlende Implementierungsinformationen, technische Rückfragen
Documentation	Architecture	Unklare Architekturentscheidungen, fehlende technische Grundlagen

Aufgaben dürfen nur an Rollen übergeben werden, die laut dieser Matrix zuständig sind.

3. Wann eine Übergabe erforderlich ist
Development

Übergabe an Architecture:

Komplexe Architekturprobleme.
Grundlegende Änderungen am Synchronisationskonzept.
Unklare technische Grundsatzentscheidungen.
Kritische Probleme mit mehreren möglichen Lösungsansätzen.

Übergabe an QA & Testing:

Änderungen an kritischer Synchronisationslogik.
Datenbankmigrationen.
Änderungen mit möglichem Datenverlust.
Kritische Änderungen an Nebenläufigkeit oder Wiederherstellung.
Wenn eine unabhängige funktionale Prüfung erforderlich ist.

Übergabe an Documentation:

Relevante Änderungen an Architektur, Datenmodell oder Synchronisationslogik.
Neue technische Entscheidungen.
Änderungen an der Teststrategie.
Release-Dokumentation.

Kleine Änderungen benötigen keine unnötigen Übergaben.

Architecture & Complex Issues

Übergabe an Development:

Nach Analyse und Freigabe eines technischen Lösungskonzepts.
Bei notwendigen Implementierungen.
Bei gezielten Fehlerkorrekturen.

Übergabe an Documentation:

Neue Architekturentscheidungen.
Geänderte Architekturgrundlagen.
Änderungen an technischen Dokumentationsvorgaben.

Größere Implementierungen dürfen erst nach ausdrücklicher Freigabe durch den Benutzer beginnen.

QA & Testing

Übergabe an Development:

Nachgewiesene Fehler.
Kritische Regressionen.
Fehlende funktionale Implementierungen.
Notwendige Korrekturen.
Unzureichende Fehlerbehandlung.

Übergabe an Documentation:

Änderungen an Teststrategie oder Testaufbau.
Dokumentationsbedarf aufgrund neuer Testergebnisse.
Relevante dokumentationswürdige Qualitätsrisiken.

QA führt notwendige Korrekturen nicht eigenständig durch.

Documentation

Übergabe an Development:

Fehlende Informationen zur tatsächlichen Implementierung.
Unklare Funktionsabläufe.
Nicht nachvollziehbare technische Änderungen.

Übergabe an Architecture:

Unklare Architekturentscheidungen.
Widersprüchliche technische Grundlagen.
Fehlende Begründungen grundlegender Designentscheidungen.

Documentation darf technische Sachverhalte nicht eigenständig erfinden oder ungeprüft interpretieren.

4. Wann keine Übergabe erforderlich ist

Keine Übergabe erstellen, wenn:

Die Aufgabe vollständig in der eigenen Rolle liegt.
Eine notwendige Prüfung bereits durchgeführt wurde.
Die Dokumentationsänderung direkt vorgenommen werden kann.
Ein spezialisierter Agent innerhalb des eigenen Chats ausreicht.
Eine Rückfrage ohne eigenständigen Arbeitsauftrag genügt.
Ein entsprechender Auftrag bereits existiert.

Die Verwendung interner Agents und Skills ersetzt nicht automatisch eine Chat-Übergabe.

5. Task-ID und Aufgabenstruktur

Jeder Auftrag erhält eine eindeutige Task-ID.

Format:

SYNC-YYYYMMDD-NNN

Beispiel:

SYNC-20261108-001

Die fortlaufende Nummer wird pro Erstellungsdatum vergeben.

Jeder Auftrag enthält:

Task-ID
Erstellungsdatum
Absender
Empfänger
Priorität
Status
Übergabegrund
Aufgabenbeschreibung
Technischer Kontext
Betroffene Dateien
Erwartetes Ergebnis
Tatsächlich durchgeführte Arbeiten
Abschlussdatum

Prioritäten:

CRITICAL
HIGH
MEDIUM
LOW
6. Statusmodell

Zulässige Statuswerte:

OPEN
IN_PROGRESS
BLOCKED
COMPLETED
CANCELLED

Bedeutung:

OPEN

Auftrag wurde erstellt, aber noch nicht ausdrücklich angenommen.

IN_PROGRESS

Empfangender Chat hat die Aufgabe ausdrücklich angenommen und arbeitet daran.

BLOCKED

Bearbeitung ist aufgrund fehlender Informationen, technischer Hindernisse oder Konflikte nicht möglich.

COMPLETED

Aufgabe wurde tatsächlich abgeschlossen und das Ergebnis dokumentiert.

CANCELLED

Auftrag wurde ausdrücklich aufgehoben.

Statuswechsel müssen nachvollziehbar bleiben.

Ein Auftrag darf nicht allein aufgrund einer begonnenen Bearbeitung als COMPLETED markiert werden.

7. Verbindlicher Ablauf einer Aufgabenübergabe
7.1 Erstellung

Der übergebende Chat muss:

Prüfen, ob ein entsprechender Auftrag bereits existiert.
Die Zuständigkeit anhand der Übergabematrix prüfen.
Den Übergabegrund dokumentieren.
Die konkrete Aufgabe beschreiben.
Relevante technische Informationen bereitstellen.
Betroffene Dateien benennen.
Das erwartete Ergebnis definieren.
Den Status auf OPEN setzen.
7.2 Annahme

Der empfangende Chat muss:

Die aktuelle TASK-HANDOVER.md lesen.
Prüfen, ob der Auftrag an seine Rolle adressiert ist.
Prüfen, ob alle notwendigen Informationen vorhanden sind.
Die Aufgabe ausdrücklich annehmen oder begründet ablehnen.
Bei Annahme den Status auf IN_PROGRESS setzen.
Bei fehlenden Informationen den Status auf BLOCKED setzen.

Eine Aufgabe gilt erst nach ausdrücklicher Annahme als übernommen.

7.3 Bearbeitung

Während der Bearbeitung:

Ausschließlich den zugewiesenen Auftrag bearbeiten.
Git-Status vor Änderungen prüfen.
Betroffene Dateien und bestehende Änderungen untersuchen.
Keine fremden Änderungen überschreiben.
Keine parallelen Schreibzugriffe auf dieselben Dateien zulassen.
Bei Konflikten die Bearbeitung stoppen und abstimmen.
7.4 Abschluss

Nach Abschluss muss der empfangende Chat dokumentieren:

Tatsächlich durchgeführte Arbeiten.
Geänderte Dateien.
Durchgeführte Tests und Ergebnisse.
Nicht durchgeführte Prüfungen.
Verbleibende Risiken.
Offene Punkte.
Abschlussdatum.

Erst danach darf der Status auf COMPLETED gesetzt werden.

Der übergebende Chat kann das Ergebnis anschließend prüfen und gegebenenfalls einen Folgeauftrag erstellen.

8. Regeln gegen doppelte Aufgaben

Vor jeder neuen Aufgabenübergabe muss geprüft werden:

Existiert bereits ein Auftrag mit demselben Ziel?
Ist ein entsprechender Auftrag bereits IN_PROGRESS?
Ist die Aufgabe bereits COMPLETED?
Existiert ein offener Auftrag mit identischen betroffenen Dateien und ähnlichem Änderungsumfang?

Wenn ein passender Auftrag existiert:

Keinen unnötigen neuen Auftrag erstellen.
Bestehende Task-ID verwenden.
Gegebenenfalls den bestehenden Auftrag ergänzen.
Bei bereits abgeschlossenen Aufgaben nur bei tatsächlich neuem Bedarf einen Folgeauftrag erstellen.
9. Schutz vor parallelen Änderungen

Vor jeder Dateiänderung durch einen empfangenden Chat:

TASK-HANDOVER.md erneut einlesen.
Git-Status prüfen.
Bestehende Änderungen untersuchen.
Prüfen, ob ein anderer Chat dieselben Dateien bearbeitet.
Bei konkurrierenden Änderungen nicht eigenständig fortfahren.

Es gilt:

Keine gleichzeitigen Schreibzugriffe auf dieselben Dateien.
Keine fremden Änderungen zurücksetzen.
Keine Änderungen ohne Abstimmung überschreiben.
Git ist die maßgebliche Versionsverwaltung.
TASK-HANDOVER.md ersetzt weder Git noch eine technische Konfliktlösung.

Bei nicht auflösbaren Konflikten wird der Auftrag auf BLOCKED gesetzt.

10. Aufgabenhistorie

Abgeschlossene und abgebrochene Aufträge bleiben zunächst nachvollziehbar dokumentiert.

Bestehende Aufgaben dürfen nicht stillschweigend gelöscht werden.

Die Datei soll trotzdem kompakt bleiben.

Bei sehr vielen abgeschlossenen Aufgaben kann eine spätere Archivierung vereinbart werden. Aktive und offene Aufgaben müssen dabei eindeutig nachvollziehbar bleiben.

11. Aufgabenübersicht

Neue Aufträge werden unter diesem Abschnitt eingetragen.

Aufträge nach Erstellungsdatum absteigend sortieren.

Offene und aktive Aufgaben sollen schnell erkennbar sein.

12. Auftragsvorlage
SYNC-YYYYMMDD-NNN – Kurzer Aufgabentitel
Erstellt am: YYYY-MM-DD
Absender: Development / Architecture / QA & Testing / Documentation
Empfänger: Development / Architecture / QA & Testing / Documentation
Priorität: LOW / MEDIUM / HIGH / CRITICAL
Status: OPEN

Übergabegrund:

Warum wird die Aufgabe an diese Rolle übergeben?

Aufgabenbeschreibung:

Was soll konkret untersucht, implementiert oder dokumentiert werden?

Technischer Kontext:

Welche technischen Informationen sind für die Bearbeitung notwendig?

Betroffene Dateien:

Pfad 1
Pfad 2

Erwartetes Ergebnis:

Welches konkrete Ergebnis wird erwartet?

Tatsächlich durchgeführte Arbeiten:

Wird bei Annahme und Bearbeitung ergänzt.

Testergebnisse:

Wird bei Bedarf ergänzt.

Verbleibende Risiken / offene Punkte:

Wird bei Bedarf ergänzt.

Abgeschlossen am:

YYYY-MM-DD

13. Beispielauftrag
SYNC-20261108-001 – Unabhängige Prüfung der Synchronisationsänderung
Erstellt am: 2026-11-08
Absender: Development
Empfänger: QA & Testing
Priorität: HIGH
Status: OPEN

Übergabegrund:

Die Synchronisationslogik wurde verändert. Aufgrund möglicher Auswirkungen auf Datenintegrität und Konfliktbehandlung ist eine unabhängige funktionale Prüfung erforderlich.

Aufgabenbeschreibung:

Prüfe die implementierten Änderungen auf funktionale Korrektheit, Regressionen und mögliche Datenverlustrisiken.

Technischer Kontext:

Die Änderungen betreffen den Synchronisationsablauf und müssen insbesondere hinsichtlich wiederholter Synchronisationsläufe und gleichzeitiger Änderungen geprüft werden.

Betroffene Dateien:

Hier die tatsächlichen geänderten Dateien eintragen.

Erwartetes Ergebnis:

Risikobewertung.
Übersicht der untersuchten Tests.
Tatsächliche Testergebnisse.
Identifizierte Fehler und Testlücken.
Verbleibende Risiken.
Konkrete Handlungsempfehlungen.

Tatsächlich durchgeführte Arbeiten:

Noch nicht begonnen.

Abgeschlossen am:

Noch offen.