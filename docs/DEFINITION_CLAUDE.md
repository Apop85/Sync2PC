7. Sonnet Medium

Sync2PC – Neuerstellung der zentralen CLAUDE.md und Einrichtung der Chat-Koordinationsschnittstelle
1. Zielsetzung

Erstelle eine vollständige, kompakte und produktionsreife zentrale CLAUDE.md für Sync2PC.

Agents und Skills wurden im vorherigen Einrichtungsschritt erstellt. Verwende deren tatsächliche Konfiguration als Grundlage.

Zusätzlich wird die gemeinsame Koordinationsschnittstelle für vier spezialisierte Chats eingerichtet.

Ziele:

Geringer Tokenverbrauch.
Klare Verantwortlichkeiten.
Effiziente Skill- und Agent-Nutzung.
Einheitliche Entwicklungsabläufe.
Schutz vor Datenverlust.
Vermeidung von Regressionen.
Strukturierte Chat-Übergaben.

Es wird ausschließlich eine zentrale CLAUDE.md erstellt. Keine separaten CLAUDE.md-Dateien für Chat-Rollen.

2. Bestandsaufnahme

Untersuche:

README.md
Vorhandene CLAUDE.md.
docs/DECISIONS.md
docs/ARCHITECTURE.md
docs/SYNC-ENGINE.md
docs/DATA-MODEL.md
docs/TESTING.md
.claude/agents/
.claude/skills/
.claude/rules/, sofern vorhanden.
Weitere Claude-Code-Konfigurationen.
coordination/, sofern vorhanden.
coordination/TASK-HANDOVER.md, sofern vorhanden.

Prüfe den tatsächlichen Inhalt und die Namen aller Agents und Skills.

Prüfe den Git-Status.

Verwende ausschließlich tatsächlich vorhandene Komponenten. Erfinde keine Agents, Skills oder Dateipfade.

3. Übergeordnete Projektregeln

Die CLAUDE.md muss folgende Regeln enthalten:

Architektur
Bestehende Architekturentscheidungen einhalten.
Vorhandene Komponenten wiederverwenden.
Keine unnötigen Refactorings.
Keine parallelen Implementierungen bestehender Funktionen.
Änderungen möglichst lokal halten.
Schnittstellen nicht ohne Not verändern.
Implementierung
Betroffene Dateien vor Änderungen untersuchen.
Bestehende Implementierungen verstehen.
Keine Annahmen über nicht untersuchten Code treffen.
Fehlerbehandlung und Randfälle berücksichtigen.
Datenverlustrisiken besonders sorgfältig prüfen.
Vorhandene Tests berücksichtigen.
Dokumentation

Die technischen Dokumentationen bilden die zentrale Wissensbasis:

DECISIONS.md: Entscheidungen und Begründungen.
ARCHITECTURE.md: Gesamtarchitektur und Komponenten.
SYNC-ENGINE.md: Synchronisationslogik.
DATA-MODEL.md: Datenmodell und Persistierung.
TESTING.md: Teststrategie und Qualitätssicherung.
PATCHNOTES.md: Historisches Änderungsprotokoll.

Redundanzen vermeiden. Relevante Dokumentationen aktualisieren.

PATCHNOTES.md nicht standardmäßig laden.

4. Agents und Zuständigkeiten

Integriere ausschließlich die tatsächlich vorhandenen Agents.

Vorgesehene Zuständigkeiten:

sync-engine-specialist

Synchronisationsengine.
3-Wege-Abgleich.
Konflikte.
Lösch- und Wiederherstellungslogik.
SMB und Dateiübertragungen.

database-specialist

Room.
Entities und DAOs.
Repositorys.
Migrationen.
Datenintegrität und Persistierung.

code-reviewer

Unabhängiges, grundsätzlich lesendes Code-Review.
Architekturkonformität.
Implementierungsfehler.
Regressionen und Seiteneffekte.

testing-qa-specialist

Unabhängige funktionale QA.
Testabdeckung.
Grenzfälle.
Datenintegrität.
Regressionen und Risiken.

Der Hauptagent bleibt für die Gesamtaufgabe verantwortlich.

Agent-Auswahl:

Keine Agents für triviale Änderungen.
Nur bei tatsächlichem Mehrwert einsetzen.
Keine redundanten Analysen.
Keine unnötigen parallelen Schreibzugriffe.
Agent-Ergebnisse kritisch prüfen.
Bei kritischen Synchronisationsänderungen den Sync-Spezialisten bevorzugen.
Bei Datenbankänderungen den Database-Spezialisten bevorzugen.
Code-Review und QA nicht gleichsetzen.

Bei besonders kritischen Änderungen beide unabhängig voneinander einsetzen.

5. Skills und Verwendung

Integriere ausschließlich die tatsächlich vorhandenen Skills.

Vorgesehene Zuordnung:

Neue Funktionen: implement-feature
Fehlerbehebungen: investigate-bug
Codeprüfung: review-changes
Dokumentation: update-docs
Gezielte Tests: run-targeted-tests
Funktionale QA: qa-check

Der Hauptagent verwendet den passenden Skill, wenn dessen Arbeitsablauf sinnvoll ist.

Die tatsächlichen Skill-Dateien sind maßgeblich. Keine Skill-Anweisungen unnötig in der CLAUDE.md duplizieren.

Manuell beschränkte Skills dürfen nicht automatisch gestartet werden. Prüfe hierzu die tatsächliche Skill-Konfiguration.

6. Entwicklungsabläufe
Kleine Änderungen
Dateien untersuchen.
Änderung durchführen.
Gezielte Tests.
Kurzer Bericht.

Keine unnötigen Agents.

Normale Änderungen
implement-feature verwenden.
Betroffene Komponenten untersuchen.
Dokumentation gezielt laden.
Bei Bedarf Spezialagent einsetzen.
Implementieren.
Tests ausführen.
Dokumentation aktualisieren.
Fehlerbehebungen
investigate-bug verwenden.
Ursache nachvollziehbar untersuchen.
Fehler beheben.
Regressionstests durchführen.
Ergebnisse dokumentieren.
Kritische Änderungen

Beispiele:

3-Wege-Abgleich.
Konfliktauflösung.
Lösch- und Wiederherstellungslogik.
Datenbankmigrationen.
Nebenläufigkeit.
Möglicher Datenverlust.

Ablauf:

Risiken analysieren.
Dokumentation laden.
Spezialagent einsetzen.
Implementieren.
Gezielte Tests.
review-changes.
qa-check.
Verbleibende Risiken dokumentieren.
Dokumentation aktualisieren.

Nur tatsächlich notwendige Schritte durchführen.

7. Code-Review und QA

Code-Review untersucht:

Implementierungsqualität.
Architekturkonformität.
Programmierfehler.
Seiteneffekte.
Wartbarkeit.

QA untersucht:

Funktionale Korrektheit.
Testabdeckung.
Grenzfälle.
Regressionen.
Datenintegrität.
Fehlerverhalten.

Ein erfolgreicher Build ersetzt weder Code-Review noch QA.

8. Tokenoptimierung
Nur relevante Quelldateien laden.
Dokumentationen gezielt einlesen.
PATCHNOTES.md nicht standardmäßig laden.
Nur offene und relevante Koordinationsaufgaben berücksichtigen.
Agents knapp mit Kontext versorgen.
Keine vollständigen Chatverläufe rekonstruieren.
Gezielte Tests bevorzugen.
Sonnet als Standardmodell.
Opus für außergewöhnlich komplexe Architektur- und Analyseaufgaben.

Qualität und Datensicherheit haben Vorrang.

9. Git-Regeln
Vor größeren Änderungen Git-Status prüfen.
Vorhandene Änderungen schützen.
Keine fremden Änderungen überschreiben.
Keine eigenständigen Commits oder Pushes.
Gemeinsam genutzte Dateien nicht gleichzeitig bearbeiten.
Vor Änderungen an gemeinsam genutzten Dateien den aktuellen Stand prüfen.
10. Vier Chat-Rollen

Alle vier Chats verwenden dieselbe CLAUDE.md, dieselben Agents und Skills.

Development

Modell: Sonnet
Effort: Medium

Reguläre Entwicklung, Fehlerbehebungen, Integration, gezielte Tests und Dokumentationsübergaben.

Architecture & Complex Issues

Modell: Opus
Effort: High

Komplexe Analysen, Architekturentscheidungen, Lösungsalternativen und technische Vorgaben.

Größere Implementierungen erst nach Freigabe durch den Benutzer.

QA & Testing

Modell: Sonnet
Effort: Medium

Unabhängige funktionale Prüfung, Testabdeckung, Regressionen und Risikobewertung.

Keine eigenständigen Änderungen an Produktions- oder Testdateien.

Documentation

Modell: Sonnet
Effort: Low

Pflege der technischen Dokumentationen und Prüfung der Konsistenz.

Technische Aussagen müssen mit der tatsächlichen Implementierung übereinstimmen.

11. Koordinationsschnittstelle

Gemeinsame Datei:

coordination/TASK-HANDOVER.md

Sie dient als verbindliches Aufgaben- und Übergabeprotokoll zwischen den vier Chat-Rollen. Sie stellt keine automatische Chat-Kommunikation her.

Übergaberichtungen
Development → Architecture, QA & Testing, Documentation.
Architecture → Development, Documentation.
QA & Testing → Development, Documentation.
Documentation → Development, Architecture.

Übergaben sind ausschließlich innerhalb dieser Zuständigkeiten zulässig.

Verbindliche Übergabepflichten

Jeder Chat muss die Koordinationsschnittstelle verwenden, wenn eine Aufgabe außerhalb seiner eigenen Zuständigkeit liegt oder eine andere Chat-Rolle ein eigenständiges Arbeitsergebnis liefern muss.

Insbesondere gelten folgende Regeln:

Development übergibt komplexe Architekturfragen an Architecture, erforderliche unabhängige Prüfungen an QA & Testing und ausgelagerte Dokumentationsarbeiten an Documentation.
Architecture übergibt freigegebene Implementierungsaufträge an Development und notwendige Dokumentationsarbeiten an Documentation.
QA & Testing übergibt festgestellte Implementierungsfehler an Development und notwendigen Dokumentationsbedarf an Documentation.
Documentation fordert bei fehlenden technischen Informationen oder ungeklärten Sachverhalten Informationen von Development oder Architecture an.

Kleine Aufgaben innerhalb der eigenen Zuständigkeit werden ohne unnötige Übergabe selbstständig bearbeitet.

Aufgabenstruktur

Jeder Auftrag enthält:

Task-ID.
Erstellungsdatum.
Absender.
Empfänger.
Priorität.
Status.
Aufgabenbeschreibung.
Grund der Übergabe.
Technischen Kontext.
Betroffene Dateien.
Erwartetes Ergebnis.
Tatsächlich durchgeführte Arbeiten.
Abschlussdatum.
Ergebnis und Rückmeldung.
Statusmodell

Zulässige Statuswerte:

OPEN, IN_PROGRESS, BLOCKED, COMPLETED, CANCELLED.

OPEN: Auftrag erstellt, noch nicht übernommen.
IN_PROGRESS: Übernahme ausdrücklich bestätigt, Bearbeitung begonnen.
BLOCKED: Bearbeitung aufgrund fehlender Informationen oder Hindernisse nicht möglich.
COMPLETED: Bearbeitung abgeschlossen und Ergebnis dokumentiert.
CANCELLED: Auftrag nachvollziehbar abgebrochen.

Ein Auftrag darf erst nach tatsächlicher Bearbeitung und dokumentiertem Ergebnis auf COMPLETED gesetzt werden.

Verbindlicher Ablauf einer Aufgabenübergabe

Vor jeder relevanten Übergabe muss der übergebende Chat:

Die aktuelle TASK-HANDOVER.md einlesen.
Prüfen, ob bereits ein entsprechender Auftrag existiert.
Die Zuständigkeit des vorgesehenen Empfängers anhand der Übergabematrix prüfen.
Einen konkreten Auftrag mit eindeutiger Aufgabenbeschreibung erstellen.
Den Grund der Übergabe und das erwartete Ergebnis beschreiben.
Betroffene Dateien und relevante technische Zusammenhänge benennen.
Den Auftrag mit einer eindeutigen Task-ID und dem Status OPEN eintragen.
Die Übergabe im eigenen Chat ausdrücklich mitteilen.

Der empfangende Chat muss:

Den Auftrag und seinen technischen Kontext prüfen.
Die eigene Zuständigkeit und verfügbare Informationen überprüfen.
Die Übernahme ausdrücklich bestätigen.
Den Status auf IN_PROGRESS setzen, sobald die Bearbeitung beginnt.
Bei fehlenden Informationen den Auftrag auf BLOCKED setzen und den Klärungsbedarf dokumentieren.
Nach Abschluss die tatsächlich durchgeführten Arbeiten und das Ergebnis eintragen.
Den Status erst danach auf COMPLETED setzen.

Ein Auftrag gilt nicht allein durch seine Eintragung als übernommen. Eine ausdrückliche Bestätigung des Empfängers ist erforderlich.

Verbindliche Regeln für alle Chats
Jeder Chat prüft die Koordinationsdatei vor relevanten Übergaben und bei der Übernahme adressierter Aufgaben.
Jeder Chat bearbeitet ausschließlich Aufgaben, die an seine Rolle adressiert sind.
Bereits erledigte oder vorhandene Aufgaben dürfen nicht unnötig erneut erstellt oder ausgeführt werden.
Abgeschlossene Aufgaben bleiben zunächst nachvollziehbar dokumentiert.
Der übergebende Chat bleibt für die Integration und Bewertung des Ergebnisses innerhalb seiner ursprünglichen Aufgabe verantwortlich.
Notwendige Folgeaufgaben werden als eigenständige Aufträge dokumentiert.
Keine unnötigen Übergaben oder redundanten Aufgaben erstellen.
Keine vollständigen Chatverläufe oder umfangreichen Quellcodekopien übertragen.
Zusammenarbeit und parallele Änderungen
Vor der Bearbeitung eines Auftrags den aktuellen Git-Status prüfen.
Die aktuelle TASK-HANDOVER.md unmittelbar vor Änderungen erneut einlesen.
Vor Änderungen an gemeinsam genutzten Dateien bestehende Änderungen überprüfen.
Keine parallelen Schreibzugriffe auf dieselben Projektdateien.
Bei konkurrierenden Änderungen die Bearbeitung stoppen und abstimmen.
Keine fremden Änderungen überschreiben oder zurücksetzen.
Git und die Koordinationsdatei sind getrennte Systeme. Die Koordinationsdatei ersetzt weder Versionsverwaltung noch Synchronisierung zwischen getrennten Projektständen.
Tokenoptimierung
Nur offene und für die eigene Rolle relevante Aufgaben berücksichtigen.
Abgeschlossene Aufgaben nicht bei jeder Session erneut analysieren.
Aufgabenbeschreibungen knapp und vollständig halten.
Keine redundanten Informationen oder umfangreichen technischen Analysen in die Koordinationsdatei übertragen.

Die detaillierte Bedienungsanleitung, Aufgabenstruktur, Übergabematrix und Beispielaufträge sind in coordination/TASK-HANDOVER.md verbindlich festgelegt. Alle vier Chat-Rollen müssen diese Regeln einhalten.

12. Dokumentationsübergaben

Relevante Dokumentationsänderungen selbst durchführen oder an Documentation übergeben.

Zuordnung:

Entscheidung: DECISIONS.md.
Architektur: ARCHITECTURE.md.
Synchronisation: SYNC-ENGINE.md.
Datenmodell: DATA-MODEL.md.
Teststrategie: TESTING.md.
Release: PATCHNOTES.md.

Übergaben müssen technischen Kontext und betroffene Dateien enthalten.

13. Abschlussbericht

Nach Entwicklungsaufgaben kompakt berichten:

Änderungen.
Betroffene Dateien.
Durchgeführte Tests und Ergebnisse.
Dokumentationsänderungen.
Aufgabenübergaben.
Offene Punkte und Risiken.
14. Umfang

Zielgröße: möglichst unter 200 Zeilen.

Keine vollständigen Agent- oder Skill-Anweisungen übernehmen.

Falls erforderlich, notwendige Regeln in .claude/rules/ auslagern, aber nur wenn dies mit der installierten Claude-Code-Version kompatibel ist.

15. Arbeitsablauf
Bestandsaufnahme.
Kurzen Plan erstellen.
CLAUDE.md konsolidieren.
TASK-HANDOVER.md erstellen beziehungsweise aktualisieren.
Konsistenz prüfen.
Agent- und Skill-Referenzen überprüfen.
Pfade und Konfigurationen validieren.
Git-Diff kontrollieren.

Bestehende Aufgaben in der Koordinationsdatei erhalten.

16. Einschränkungen

Keine Programmdateien verändern.

Keine technischen Dokumentationen verändern.

Keine Agents oder Skills neu erstellen.

Keine bestehenden Agents oder Skills ohne ausdrücklichen Grund verändern.

Keine unnötigen Dateien erstellen.

Keine Commits oder Pushes.

Erlaubte Änderungen:

CLAUDE.md
coordination/TASK-HANDOVER.md
Notwendige .claude/rules/, sofern begründet.

17. Abschlussbericht

Kompakt berichten:

CLAUDE.md erstellt beziehungsweise überarbeitet.
Zeilenzahl.
Eingebundene Agents und Skills.
Vier Chat-Rollen integriert.
Koordinationsschnittstelle erstellt beziehungsweise aktualisiert.
Unterstützte Übergaberichtungen.
Ergebnis der Konsistenzprüfung.
Offene Punkte und Konflikte.

Keine vollständigen Dateien im Chat wiederholen.