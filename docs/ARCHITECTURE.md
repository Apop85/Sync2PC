2. SONNET MEDIUM

Sync2PC – Erstellung der technischen Architekturdokumentation
Ziel

Erstelle eine vollständige, technisch korrekte und gut strukturierte Architekturdokumentation für das Projekt Sync2PC.

Die Dokumentation wird unter folgendem Pfad angelegt:

docs/ARCHITECTURE.md

Sie soll sowohl als technische Referenz für Entwickler als auch als dauerhaftes Kontextwissen für zukünftige Claude-Code-Sessions dienen.

Das wichtigste Ziel ist, dass Claude bei zukünftigen Entwicklungsaufgaben schnell versteht:

Wie das Projekt aufgebaut ist.
Welche Komponenten existieren.
Welche Aufgaben diese Komponenten übernehmen.
Welche Dateien und Klassen für bestimmte Funktionen zuständig sind.
Wie die Komponenten miteinander kommunizieren.
Wo neue Funktionen sinnvoll implementiert werden.
Welche architektonischen Regeln und Abhängigkeiten zu beachten sind.
1. Quellen und Analysegrundlage

Untersuche das aktuelle Repository.

Berücksichtige insbesondere:

Den vollständigen relevanten Quellcode.
Die bestehende README.md.
Die Patchnotes.
docs/DECISIONS.md.
Gradle- und Build-Konfigurationen.
Android-Konfigurationen und Manifest-Dateien.
Bestehende technische Dokumentation.

Wichtig:

Der tatsächlich vorhandene Quellcode ist die maßgebliche Quelle für die Beschreibung der aktuellen Architektur.

Die DECISIONS.md dient als zusätzliche Informationsquelle für Hintergründe und getroffene Entscheidungen.

Unterscheide zwischen:

Aktuell implementierter Architektur.
Geplanten oder vorgeschlagenen Änderungen.
Historischen Architekturentscheidungen.

Beschreibe niemals geplante Funktionen als bereits implementiert.

2. Projektüberblick

Beschreibe:

Zweck und grundlegende Funktion von Sync2PC.
Technologische Grundlagen.
Verwendete Programmiersprachen und Frameworks.
Android-Architektur.
Wichtige externe Bibliotheken.
Wesentliche technische Integrationen.
3. Gesamtarchitektur

Erstelle eine verständliche Übersicht der Gesamtarchitektur.

Beschreibe:

Die wichtigsten Systemkomponenten.
Deren Verantwortlichkeiten.
Beziehungen und Abhängigkeiten.
Kommunikationswege zwischen den Komponenten.
Zentrale technische Schnittstellen.

Erstelle ein Mermaid-Architekturdiagramm, sofern die tatsächliche Architektur ausreichend nachvollziehbar ist.

Das Diagramm soll die wesentlichen Komponenten und deren Beziehungen zeigen und nicht mit Implementierungsdetails überladen werden.

4. Verzeichnis- und Modulstruktur

Erstelle eine übersichtliche Darstellung der relevanten Projektverzeichnisse.

Erkläre die Funktion der einzelnen Verzeichnisse und Module.

Berücksichtige dabei insbesondere:

App-Module.
Kotlin-Quellcode.
UI-Komponenten.
Datenbank.
Synchronisationskomponenten.
Netzwerk- und SMB-Komponenten.
Hintergrundprozesse.
Ressourcen.
Tests.
Konfigurationsdateien.

Generierte Dateien, Build-Artefakte und temporäre Dateien müssen nicht einzeln dokumentiert werden.

5. Datei- und Klassenübersicht

Dies ist ein besonders wichtiger Abschnitt.

Erstelle einen strukturierten Index der relevanten Quelldateien.

Für jede relevante Datei sollen folgende Informationen enthalten sein:

Dateipfad: Relativer Pfad innerhalb des Repositorys.

Verantwortlichkeit: Welche konkrete Aufgabe erfüllt die Datei?

Inhalt: Welche wichtigen Klassen, Funktionen oder Komponenten befinden sich darin?

Abhängigkeiten: Mit welchen anderen wichtigen Komponenten arbeitet sie zusammen?

Relevanz: Welche Bedeutung hat sie für das Gesamtsystem?

Berücksichtige alle relevanten Kotlin-Dateien und weitere architekturrelevante Dateien.

Unterscheide zwischen zentralen Dateien und unterstützenden Komponenten.

Erstelle keine künstlich aufgeblähten Beschreibungen für triviale Dateien.

Ziel ist eine möglichst vollständige Orientierung über den tatsächlich vorhandenen Quellcode.

6. Komponenten und Verantwortlichkeiten

Beschreibe die wesentlichen Systemkomponenten einzeln.

Untersuche insbesondere:

Benutzeroberfläche.
Anwendungslogik.
Synchronisationssteuerung.
Synchronisationsprofile.
SMB-Kommunikation.
Datenbankzugriff.
Datenverarbeitung.
Metadaten- und EXIF-Auswertung.
Hintergrundverarbeitung.
WorkManager.
Einstellungen und Konfiguration.
Diagnose und Logging.
Update-Mechanismen.

Berücksichtige nur Komponenten, die tatsächlich im Projekt existieren.

Erkläre, welche Aufgaben jeweils in welcher Komponente liegen und welche Verantwortlichkeiten bewusst getrennt sind.

7. Abhängigkeiten und Kommunikationswege

Beschreibe:

Welche Komponenten andere Komponenten aufrufen.
Welche Schnittstellen verwendet werden.
Wie Daten zwischen Komponenten übertragen werden.
Welche Abhängigkeiten besonders wichtig sind.
Wo potenziell problematische Kopplungen bestehen.

Zeige, welche Komponenten möglichst unabhängig bleiben sollten.

8. Datenflüsse und Verarbeitungsabläufe

Beschreibe die wesentlichen Datenflüsse auf Architekturebene.

Beispielsweise:

Start einer Synchronisation.
Kommunikation mit dem entfernten Speicher.
Übergabe von Daten an die Verarbeitungskomponenten.
Persistierung in der Datenbank.
Hintergrundverarbeitung.
Aktualisierung der Benutzeroberfläche.

Verwende bei Bedarf Mermaid-Diagramme.

Die detaillierte fachliche Funktionsweise der Synchronisationsengine gehört jedoch in die separate Datei docs/SYNC-ENGINE.md.

Die vollständige Datenbankstruktur gehört in docs/DATA-MODEL.md.

Vermeide daher unnötige Doppelungen.

9. Externe Bibliotheken und Schnittstellen

Dokumentiere die wesentlichen externen Abhängigkeiten.

Für wichtige Bibliotheken:

Name.
Verwendungszweck.
Betroffene Komponenten.
Bedeutung für die Architektur.

Berücksichtige insbesondere Netzwerk-, SMB-, Datenbank-, Android- und Hintergrundverarbeitungsbibliotheken.

Verwende die tatsächlichen Abhängigkeiten aus den Build-Dateien und erfinde keine Bibliotheken.

10. Android-spezifische Architektur

Beschreibe die wesentlichen Android-spezifischen Aspekte:

Application Lifecycle.
Activities und Services.
ViewModels, sofern vorhanden.
Navigation.
Hintergrundprozesse.
WorkManager.
Berechtigungen.
Benachrichtigungen.
Speicherzugriffe.
Relevante Android-Kompatibilitätsanforderungen.

Dokumentiere nur tatsächlich implementierte Mechanismen.

11. Einstiegspunkte und Erweiterungspunkte

Erkläre, wo zukünftige Entwickler bestimmte Änderungen durchführen sollten.

Beispiele:

Neue UI-Funktion hinzufügen.
Neue Einstellung ergänzen.
Synchronisationsverhalten verändern.
Neue Datenbankfunktion implementieren.
Neue SMB-Funktion integrieren.
Hintergrundaufgabe ergänzen.
Diagnosefunktion erweitern.

Nenne dabei möglichst konkrete Dateien und Klassen.

Dieser Abschnitt soll Claude helfen, bei zukünftigen Aufgaben direkt an der richtigen Stelle anzusetzen.

12. Architekturregeln und Einschränkungen

Dokumentiere wichtige Regeln, die bei zukünftigen Änderungen eingehalten werden müssen.

Berücksichtige:

Verantwortlichkeitstrennung.
Abhängigkeitsregeln.
Datenbankzugriffe.
Threading und Nebenläufigkeit.
Android Lifecycle.
Ressourcenmanagement.
Fehlerbehandlung.
Kompatibilität.
Besonders sensible Komponenten.

Übernimm relevante, weiterhin gültige Architekturentscheidungen aus DECISIONS.md.

13. Verweise auf weitere Dokumentation

Verweise auf die vorgesehenen ergänzenden Dokumente:

docs/DECISIONS.md
docs/SYNC-ENGINE.md
docs/DATA-MODEL.md
docs/TESTING.md

Falls einzelne Dateien noch nicht existieren, dürfen sie als geplante Dokumentation gekennzeichnet werden.

Erkläre kurz, welche Informationen jeweils dort zu finden sein sollen.

14. Qualitätsanforderungen

Die Dokumentation muss:

Technisch korrekt sein.
Auf dem tatsächlichen Quellcode basieren.
Klar und nachvollziehbar formuliert sein.
Eine sinnvolle Überschriftenstruktur besitzen.
Relative Dateipfade verwenden.
Keine veralteten oder nicht implementierten Funktionen als aktuell darstellen.
Keine unnötigen Codeblöcke enthalten.
Keine umfangreichen Quellcodepassagen kopieren.
Keine redundanten Informationen aus anderen Dokumenten wiederholen.

Bei unklaren Sachverhalten ausdrücklich darauf hinweisen, statt zu spekulieren.

15. Arbeitsweise und Einschränkungen
Arbeite systematisch und analysiere die relevanten Dateien.
Vermeide unnötiges mehrfaches Einlesen derselben Dateien.
Keine vollständige Ausgabe von Quelldateien im Chat.
Keine unnötigen Build- oder Testläufe.
Keine Änderungen am Programmcode.
Keine Änderungen an README.md, Patchnotes oder DECISIONS.md.
Ändere ausschließlich docs/ARCHITECTURE.md.

Falls die Datei bereits existiert, prüfe ihren Inhalt und aktualisiere sie sinnvoll, anstatt Informationen unüberlegt zu überschreiben.

16. Abschlusskontrolle

Prüfe vor Abschluss:

Sind alle relevanten Module erfasst?
Sind die wichtigen Quelldateien im Index enthalten?
Sind die Verantwortlichkeiten korrekt?
Stimmen die beschriebenen Abhängigkeiten mit dem Quellcode überein?
Sind die Architekturdiagramme korrekt?
Sind geplante und implementierte Funktionen sauber getrennt?
Sind die Verweise auf weitere Dokumente korrekt?
17. Abschlussbericht

Gib im Chat ausschließlich eine kurze Zusammenfassung aus:

Datei erstellt oder aktualisiert.
Untersuchte Module.
Anzahl der dokumentierten relevanten Quelldateien.
Erstellte Architekturdiagramme.
Noch offene Fragen oder nicht eindeutig nachvollziehbare Architekturdetails.

Gib nicht den vollständigen Inhalt der ARCHITECTURE.md im Chat aus.