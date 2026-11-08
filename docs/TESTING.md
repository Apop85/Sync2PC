5. SONNET MEDIUM

Sync2PC – Erstellung der technischen Test- und Qualitätssicherungsdokumentation
Ziel

Erstelle eine vollständige, technisch korrekte und praxisorientierte Dokumentation der Teststrategie und Qualitätssicherung des Projekts Sync2PC.

Die Dokumentation wird unter folgendem Pfad angelegt:

docs/TESTING.md

Sie soll als zentrale Referenz für zukünftige Entwicklungsarbeiten dienen.

Das Ziel ist, dass Claude Code bei jeder Änderung schnell nachvollziehen kann:

Welche Tests bereits existieren.
Wie die Tests ausgeführt werden.
Welche Bereiche automatisiert getestet sind.
Welche Bereiche manuell geprüft werden müssen.
Welche Komponenten besonders regressionsanfällig sind.
Welche Tests bei welchen Änderungen ausgeführt werden sollten.
Wie die Qualität einer Implementierung zuverlässig überprüft wird.

Wichtig: Dokumentiere den tatsächlichen Test- und Build-Zustand des Projekts. Behaupte keine vorhandene Testabdeckung, die nicht nachgewiesen werden kann.

1. Quellen und Analysegrundlage

Untersuche das aktuelle Repository.

Berücksichtige insbesondere:

docs/ARCHITECTURE.md
docs/SYNC-ENGINE.md
docs/DATA-MODEL.md
docs/DECISIONS.md
README.md
Patchnotes
Gradle-Konfigurationen
Vorhandene Testverzeichnisse
Unit-Tests
Instrumentation-Tests
Integrationstests
Test-Fixtures und Testdaten
CI/CD-Konfigurationen, sofern vorhanden

Der tatsächliche Quellcode und die vorhandenen Testimplementierungen sind die maßgebliche Grundlage.

Unterscheide klar zwischen vorhandenen Tests, geplanten Tests und lediglich empfohlenen Tests.

2. Überblick über die Teststrategie

Beschreibe zunächst:

Welche Testarten im Projekt verwendet werden.
Welche Testframeworks eingesetzt werden.
Wie die Tests in die Projektstruktur eingebunden sind.
Welche Komponenten automatisiert getestet werden.
Welche Komponenten manuelle Prüfungen benötigen.
Welche Rolle Build-Prüfungen und statische Analysen spielen.

Erstelle eine Übersicht der vorhandenen Qualitätssicherungsmechanismen.

3. Build-System und Build-Befehle

Untersuche die Gradle-Konfiguration.

Dokumentiere:

Verwendete Gradle-Version.
Android Gradle Plugin.
Kotlin-Version.
Vorhandene Build-Varianten.
Debug- und Release-Konfiguration.
Relevante Build-Flavors, sofern vorhanden.
Build-Abhängigkeiten.
Vorhandene Build-Tasks.

Erstelle eine Übersicht der tatsächlich verwendbaren Befehle.

Berücksichtige insbesondere:

Vollständiger Debug-Build.
Release-Build.
Unit-Tests.
Instrumentation-Tests.
Einzelne Modul-Tests.
Relevante Prüf- und Analyseaufgaben.

Für jeden Befehl:

Konkreter Befehl.
Zweck.
Umfang.
Wann er ausgeführt werden sollte.

Verwende ausschließlich Befehle, die aus der vorhandenen Gradle-Konfiguration nachvollziehbar sind.

4. Vorhandene Teststruktur

Erfasse die vollständige relevante Testverzeichnisstruktur.

Dokumentiere:

Testmodule.
Testpakete.
Testklassen.
Verwendete Testframeworks.
Test-Fixtures.
Test-Hilfsklassen.
Mocking- und Fake-Implementierungen.
Gemeinsame Testressourcen.

Erstelle eine strukturierte Übersicht der vorhandenen Tests.

Für wichtige Testklassen beschreibe:

Dateipfad.
Getestete Komponente.
Testschwerpunkt.
Wesentliche überprüfte Szenarien.
Bedeutung für die Qualitätssicherung.

Keine Testabdeckung erfinden oder aus dem Namen einer Testklasse ableiten, ohne den Testinhalt zu untersuchen.

5. Unit-Tests

Dokumentiere die vorhandenen Unit-Tests.

Untersuche insbesondere Tests für:

Fachliche Anwendungslogik.
Synchronisationsentscheidungen.
Vergleichsalgorithmen.
Konflikterkennung.
Datenverarbeitung.
Metadaten.
Hilfsfunktionen.
Zustandsübergänge.
Fehlerbehandlung.

Beschreibe, welche Komponenten isoliert getestet werden können und welche Abhängigkeiten dafür benötigt werden.

6. Datenbank- und Migrationstests

Untersuche sämtliche vorhandenen Datenbanktests.

Berücksichtige:

Entity-Tests.
DAO-Tests.
Repository-Tests.
Datenbankoperationen.
Transaktionen.
Fremdschlüsselbeziehungen.
Eindeutigkeitsbedingungen.
Datenintegrität.
Datenbankmigrationen.
Duplikaterkennung.
Persistierung synchronisationsrelevanter Informationen.

Beschreibe insbesondere, welche Tests sicherstellen, dass bestehende Daten bei Datenbankänderungen erhalten bleiben.

7. Tests der Synchronisationsengine

Dieser Abschnitt ist besonders wichtig.

Untersuche die vorhandenen Tests zur Synchronisationslogik.

Berücksichtige insbesondere:

Erkennung neuer Dateien.
Erkennung geänderter Dateien.
Erkennung gelöschter Dateien.
3-Wege-Abgleich.
Gleichzeitige Änderungen.
Konflikterkennung.
Konfliktbehandlung.
Synchronisationsprofile.
Dateiübertragung.
Abgebrochene Übertragungen.
Wiederholungsversuche.
Fehlerbehandlung.
Wiederherstellung nach Unterbrechungen.

Erstelle eine Übersicht:

Szenario	Vorhandener Test	Testdatei	Bewertung

Bewerte ausschließlich anhand tatsächlich vorhandener Tests.

Kennzeichne nicht vorhandene Tests als fehlend und nicht lediglich als unzureichend.

Verweise für die fachlichen Abläufe auf SYNC-ENGINE.md.

8. SMB- und Netzwerk-Tests

Untersuche die Tests der Netzwerk- und SMB-Komponenten.

Dokumentiere:

Verbindungsaufbau.
Authentifizierung.
Verzeichniszugriffe.
Dateioperationen.
Netzwerkfehler.
Verbindungsabbrüche.
Wiederverbindung.
Timeout-Verhalten.
Fehlerbehandlung.
Test-Doubles für entfernte Dateisysteme.

Unterscheide zwischen echten Netzwerk- und SMB-Tests sowie isolierten Tests mit Mock- oder Fake-Implementierungen.

9. Android-spezifische Tests

Untersuche die vorhandenen Android-Tests.

Berücksichtige:

Activity- und Lifecycle-Verhalten.
ViewModels.
UI-Tests.
Navigation.
Berechtigungen.
Speicherzugriffe.
WorkManager.
Hintergrundprozesse.
Benachrichtigungen.
Android-spezifische Dienste.

Beschreibe, welche Tests einen Emulator oder ein physisches Gerät benötigen.

Kennzeichne Unterschiede zwischen Unit-Tests und Instrumentation-Tests.

10. Manuelle Tests und reale Geräte

Identifiziere Bereiche, die sich nicht ausreichend automatisiert testen lassen oder bei denen reale Geräteprüfungen wichtig sind.

Berücksichtige insbesondere:

Tatsächliche SMB-Verbindungen.
Unterschiedliche NAS-Implementierungen.
Netzwerkunterbrechungen.
WLAN-Wechsel.
App-Abbrüche während einer Synchronisation.
Android-Hintergrundbeschränkungen.
Neustart des Geräts.
Berechtigungsänderungen.
Größere Dateimengen.
Speicherplatzmangel.
Unterschiedliche Android-Versionen.

Erstelle eine Liste sinnvoller manueller Testszenarien.

Unterscheide zwischen zwingend erforderlichen und ergänzenden Prüfungen.

11. Regressionstests

Erstelle eine Übersicht der besonders wichtigen Regressionstests.

Berücksichtige insbesondere:

Änderungen an der Synchronisationsengine.
Änderungen am 3-Wege-Abgleich.
Änderungen an der Konflikterkennung.
Änderungen an Lösch- und Wiederherstellungslogik.
Änderungen am Datenmodell.
Datenbankmigrationen.
Änderungen an Dateiübertragungen.
Änderungen an Hintergrundprozessen.

Beschreibe, welche Tests nach welchen Arten von Änderungen ausgeführt werden sollten.

Ziel ist eine möglichst effiziente Testauswahl, ohne kritische Bereiche unkontrolliert zu lassen.

12. Testabdeckung und Qualitätslücken

Bewerte den vorhandenen Teststand.

Erstelle eine Übersicht:

Bereich	Automatisierte Tests	Manuelle Tests	Erkannte Lücken

Untersuche insbesondere:

Nicht getestete kritische Komponenten.
Fehlende Grenzfalltests.
Fehlende Fehler- und Wiederanlauftests.
Unzureichende Migrationstests.
Fehlende Regressionstests.
Bereiche, die ausschließlich durch manuelle Tests geprüft werden.

Unterscheide zwischen nachweisbaren Testlücken und Bereichen, deren Testabdeckung nicht eindeutig festgestellt werden kann.

13. Empfohlene Teststrategie für zukünftige Änderungen

Erstelle konkrete Empfehlungen für zukünftige Claude-Code-Arbeiten.

Definiere, welche Prüfungen bei unterschiedlichen Aufgaben sinnvoll sind.

Beispiele:

Kleine UI-Änderung: Welche Tests und Build-Prüfungen sind notwendig?

Änderung an einer Datenbank-Entity: Welche Datenbank- und Migrationstests müssen berücksichtigt werden?

Änderung an der Synchronisationsengine: Welche Synchronisations- und Regressionstests sind erforderlich?

Änderung am SMB-Zugriff: Welche Netzwerk- und Dateioperationstests sind notwendig?

Änderung an WorkManager oder Hintergrundprozessen: Welche Android-spezifischen Tests sollten durchgeführt werden?

Die Empfehlungen müssen sich an den tatsächlichen Testmöglichkeiten des Projekts orientieren.

14. Qualitätsregeln für Claude Code

Formuliere verbindliche Regeln für zukünftige Entwicklungsarbeiten:

Relevante Tests vor Abschluss einer Implementierung ausführen.
Testergebnisse nicht erfinden.
Fehlgeschlagene Tests nicht verschweigen.
Build-Erfolg nicht mit funktionaler Korrektheit gleichsetzen.
Bei Änderungen an kritischen Komponenten gezielte Regressionstests durchführen.
Nicht ausgeführte Tests ausdrücklich als nicht ausgeführt kennzeichnen.
Bei fehlenden Tests die verbleibenden Risiken benennen.
Keine unnötigen vollständigen Testläufe bei kleinen Änderungen durchführen, sofern gezielte Tests ausreichen.
15. Diagramme und Übersichten

Erstelle geeignete Tabellen und Diagramme, sofern sie die Übersicht verbessern.

Beispielsweise:

Teststruktur.
Testarten und Zuständigkeiten.
Testabdeckung nach Komponenten.
Empfohlener Prüfablauf bei Änderungen.

Keine unnötigen Diagramme erstellen.

16. Abgrenzung zu anderen Dokumenten

Vermeide unnötige Doppelungen mit:

docs/ARCHITECTURE.md
docs/SYNC-ENGINE.md
docs/DATA-MODEL.md
docs/DECISIONS.md

Diese Datei konzentriert sich auf Tests, Build-Prozess und Qualitätssicherung.

17. Qualitätsanforderungen
Tatsächliche Tests untersuchen.
Vorhandene Testabdeckung korrekt beschreiben.
Keine nicht existierenden Tests behaupten.
Build-Befehle aus der tatsächlichen Konfiguration ableiten.
Relative Dateipfade verwenden.
Testlücken klar kennzeichnen.
Automatisierte und manuelle Tests unterscheiden.
Implementierte und empfohlene Testfälle trennen.
Keine umfangreichen Quellcodepassagen kopieren.

Bei unklaren Sachverhalten ausdrücklich darauf hinweisen.

18. Arbeitsweise und Einschränkungen
Analysiere systematisch die relevanten Test- und Build-Dateien.
Vermeide unnötiges mehrfaches Einlesen derselben Dateien.
Keine Änderungen am Programmcode.
Keine Änderungen an anderen Dokumentationsdateien.
Keine neuen Tests implementieren.
Keine Refactorings.
Keine umfangreichen Build- oder Testläufe durchführen.
Ändere ausschließlich docs/TESTING.md.
Falls die Datei bereits existiert, aktualisiere sie unter Erhalt weiterhin relevanter Informationen.
19. Abschlusskontrolle

Prüfe vor Abschluss:

Sind alle relevanten Testmodule erfasst?
Sind die wichtigsten Testklassen dokumentiert?
Sind die Build- und Testbefehle korrekt?
Ist die Testabdeckung realistisch dargestellt?
Sind Synchronisations- und Datenbanktests ausreichend berücksichtigt?
Sind manuelle Tests von automatisierten Tests getrennt?
Sind kritische Testlücken erkennbar?
Sind die Empfehlungen für zukünftige Änderungen nachvollziehbar?
20. Abschlussbericht

Gib im Chat ausschließlich eine kurze Zusammenfassung aus:

Datei erstellt oder aktualisiert.
Anzahl untersuchter Testklassen.
Vorhandene Testarten.
Identifizierte Testlücken.
Dokumentierte Build- und Testbefehle.
Besonders kritische, noch unzureichend getestete Bereiche.

Gib nicht den vollständigen Inhalt der TESTING.md im Chat aus.