4. SONNET HIGH

Sync2PC – Erstellung der technischen Dokumentation der Synchronisationsengine
Ziel

Erstelle eine umfassende, technisch korrekte und nachvollziehbare Dokumentation der Synchronisationsengine des Projekts Sync2PC.

Die Dokumentation wird unter folgendem Pfad angelegt:

docs/SYNC-ENGINE.md

Sie soll als zentrale technische Referenz für die Synchronisationslogik dienen und zukünftigen Claude-Code-Sessions ermöglichen, die Funktionsweise der Engine zu verstehen, ohne bei jeder Aufgabe den gesamten Synchronisationscode erneut analysieren zu müssen.

Besonders wichtig: Die Dokumentation muss den tatsächlich implementierten Zustand des Quellcodes beschreiben. Keine Annahmen, keine erfundenen Abläufe und keine Vermischung von geplanten und bereits implementierten Funktionen.

1. Quellen und Analysegrundlage

Untersuche den aktuellen Quellcode und berücksichtige insbesondere:

docs/DECISIONS.md
docs/ARCHITECTURE.md
README.md
Patchnotes
Alle relevanten Klassen und Dateien der Synchronisationsengine
Datenbankzugriffe und zugehörige Entities
SMB-Kommunikation
Dateiübertragungslogik
Hintergrundverarbeitung
Fehlerbehandlung und Logging
Vorhandene Tests

Der aktuelle Quellcode ist die maßgebliche Quelle für die Beschreibung des implementierten Verhaltens.

Verwende die Entscheidungen aus DECISIONS.md als zusätzliche Informationsquelle für technische Hintergründe und bewusst gewählte Verhaltensweisen.

Falls Dokumentation und Quellcode voneinander abweichen, dokumentiere den tatsächlichen Quellcodezustand und weise auf die Abweichung hin.

2. Überblick über die Synchronisationsengine

Beschreibe zunächst:

Zweck und Aufgaben der Synchronisationsengine.
Die wesentlichen Komponenten.
Verantwortlichkeiten der beteiligten Klassen.
Einstiegspunkte und Auslöser eines Synchronisationslaufs.
Zusammenspiel mit Datenbank, SMB-Kommunikation und Hintergrundverarbeitung.
Unterschiede zwischen den vorhandenen Synchronisationsprofilen.

Erstelle eine übersichtliche Darstellung der beteiligten Komponenten.

3. Synchronisationsablauf

Rekonstruiere den vollständigen Ablauf eines Synchronisationslaufs anhand des Quellcodes.

Beschreibe chronologisch:

Auslösen und Starten der Synchronisation.
Laden der relevanten Konfiguration.
Ermittlung des lokalen Dateibestands.
Ermittlung des entfernten Dateibestands.
Einlesen und Vergleichen vorhandener Metadaten.
Erkennung von Änderungen.
Ermittlung erforderlicher Synchronisationsaktionen.
Behandlung möglicher Konflikte.
Durchführung der Dateioperationen.
Aktualisierung des Datenbestands.
Abschluss und Statusverwaltung.

Berücksichtige auch Abweichungen zwischen den verschiedenen Synchronisationsprofilen.

Erstelle ein Mermaid-Ablaufdiagramm, das den tatsächlichen Synchronisationsablauf nachvollziehbar darstellt.

Vermeide dabei eine rein theoretische Darstellung. Jeder beschriebene Schritt muss durch die Implementierung nachvollziehbar sein.

4. 3-Wege-Abgleich

Dieser Abschnitt ist besonders wichtig.

Untersuche die tatsächliche Implementierung des 3-Wege-Abgleichs.

Beschreibe:

Bedeutung und Herkunft der drei Vergleichsstände.
Wie der gemeinsame Ausgangszustand ermittelt wird.
Wie lokale Änderungen erkannt werden.
Wie entfernte Änderungen erkannt werden.
Wie gleichzeitige Änderungen festgestellt werden.
Wie unveränderte Dateien behandelt werden.
Wie neue Dateien behandelt werden.
Wie geänderte Dateien behandelt werden.
Wie gelöschte Dateien erkannt werden.
Wie Konflikte aus dem Vergleich entstehen.

Erkläre die Vergleichslogik anhand konkreter Beispiele.

Verwende Tabellen, um die verschiedenen Änderungskombinationen darzustellen.

Unterscheide ausdrücklich zwischen dem theoretischen 3-Wege-Abgleich und den tatsächlich im Projekt implementierten Regeln.

5. Konflikterkennung und Konfliktbehandlung

Dokumentiere sämtliche tatsächlich implementierten Konfliktarten.

Untersuche:

Wie ein Konflikt erkannt wird.
Welche Kriterien zur Konflikterkennung verwendet werden.
Wie gleichzeitige Änderungen behandelt werden.
Wie unterschiedliche Dateiinhalte bewertet werden.
Welche Rolle Zeitstempel und Metadaten spielen.
Welche Konfliktlösungsstrategien existieren.
Welche Entscheidungen automatisch getroffen werden.
Welche Entscheidungen Benutzereingriffe erfordern.
Wie Konfliktzustände gespeichert und weiterverarbeitet werden.

Beschreibe auch Sonderfälle und mögliche Grenzsituationen.

Dokumentiere die Auswirkungen auf lokale und entfernte Dateien.

6. Dateioperationen und Übertragung

Beschreibe sämtliche wesentlichen Dateioperationen:

Neue Dateien übertragen.
Vorhandene Dateien aktualisieren.
Dateien ersetzen.
Dateien kopieren.
Dateien verschieben.
Dateien löschen.
Dateien wiederherstellen.
Dateiübertragungen wiederholen.

Untersuche insbesondere:

Wie Dateiübertragungen gestartet werden.
Wie der Fortschritt verarbeitet wird.
Wie unvollständige Übertragungen behandelt werden.
Wie Fehler erkannt werden.
Wie die Konsistenz zwischen lokaler und entfernter Datei sichergestellt wird.
Welche Sicherheitsmechanismen gegen unbeabsichtigten Datenverlust existieren.

Beschreibe die tatsächliche Implementierung und nicht nur die vorgesehenen Abläufe.

7. Löschlogik und Wiederherstellung

Dieser Bereich muss besonders sorgfältig untersucht werden.

Dokumentiere:

Wie lokale Löschungen erkannt werden.
Wie entfernte Löschungen erkannt werden.
Wie Löschungen im 3-Wege-Abgleich berücksichtigt werden.
Wie Löschinformationen gespeichert werden.
Welche Rolle der Papierkorb spielt.
Wie Wiederherstellungen funktionieren.
Wie verhindert wird, dass gelöschte Dateien unbeabsichtigt erneut synchronisiert werden.
Welche Sonderfälle auftreten können.

Untersuche insbesondere, wie die Engine mit unterschiedlichen Löschzuständen auf beiden Seiten umgeht.

Dokumentiere mögliche Risiken für unbeabsichtigten Datenverlust.

8. Metadaten und Dateivergleich

Beschreibe die für die Synchronisation verwendeten Metadaten.

Untersuche:

Dateinamen.
Dateipfade.
Dateigrößen.
Zeitstempel.
Hashwerte, sofern verwendet.
EXIF- und weitere Metadaten, sofern relevant.
Identifikatoren.
Versions- oder Änderungsinformationen.
Sonstige Vergleichskriterien.

Erkläre, welche Metadaten für welche Entscheidungen verwendet werden.

Dokumentiere mögliche Probleme durch abweichende Zeitstempel, Dateisystemeigenschaften oder unvollständige Metadaten.

9. Synchronisationsprofile

Untersuche alle vorhandenen Synchronisationsprofile.

Beschreibe für jedes Profil:

Zweck.
Funktionsweise.
Unterschiede zu anderen Profilen.
Erlaubte Dateioperationen.
Verhalten bei Konflikten.
Verhalten bei Löschungen.
Besondere Einschränkungen.

Erstelle eine Vergleichstabelle.

Beschreibe nur tatsächlich implementierte Unterschiede.

10. SMB-Kommunikation

Dokumentiere die Rolle der SMB-Kommunikation innerhalb der Synchronisationsengine.

Untersuche:

Verbindungsaufbau.
Authentifizierung.
Zugriff auf entfernte Verzeichnisse.
Dateiauflistung.
Lesen und Schreiben von Dateien.
Löschen und Verschieben.
Fehlerbehandlung.
Verbindungsabbrüche.
Wiederverbindungsmechanismen.
Ressourcenfreigabe.

Beschreibe die Schnittstellen zwischen Synchronisationslogik und SMB-Komponenten.

Die vollständige Beschreibung der externen Bibliotheken gehört in ARCHITECTURE.md. Hier soll der konkrete Einsatz innerhalb der Synchronisation dokumentiert werden.

11. Nebenläufigkeit und Hintergrundverarbeitung

Untersuche:

Wie Synchronisationsläufe gestartet und ausgeführt werden.
Welche Rolle WorkManager spielt.
Wie parallele Dateioperationen behandelt werden.
Wie gleichzeitige Synchronisationsläufe verhindert oder koordiniert werden.
Wie Abbrüche behandelt werden.
Wie laufende Operationen auf Unterbrechungen reagieren.
Welche Ressourcenbeschränkungen berücksichtigt werden.

Achte besonders auf mögliche Race Conditions und inkonsistente Zustände.

12. Fehlerbehandlung und Wiederanlauf

Dokumentiere die Fehlerbehandlung der Synchronisationsengine.

Berücksichtige:

Netzwerkfehler.
SMB-Fehler.
Berechtigungsprobleme.
Dateioperationen, die fehlschlagen.
Datenbankfehler.
Unterbrochene Synchronisationsläufe.
App-Abstürze.
Wiederaufnahme nach Unterbrechungen.
Wiederholungsmechanismen.
Fehlerprotokollierung.

Unterscheide zwischen:

Fehlern, die automatisch behoben werden.
Fehlern, die einen erneuten Versuch erfordern.
Fehlern, die den Synchronisationslauf abbrechen.
Fehlern, die Benutzereingriffe erfordern.

Beschreibe auch, welche Garantien die Implementierung tatsächlich bietet und welche nicht.

13. Datenbank und Synchronisationszustand

Beschreibe, wie die Synchronisationsengine mit der Datenbank zusammenarbeitet.

Untersuche:

Welche Daten vor einem Synchronisationslauf geladen werden.
Welche Zustände während der Synchronisation gespeichert werden.
Wann Datenbankeinträge aktualisiert werden.
Wie Änderungen persistiert werden.
Wie Synchronisationszustände wiederhergestellt werden.
Welche Abhängigkeiten zwischen Datenbank und Dateioperationen bestehen.

Die vollständige Tabellen- und Entity-Dokumentation gehört in DATA-MODEL.md.

Hier liegt der Schwerpunkt auf der Verwendung des Datenmodells durch die Synchronisationsengine.

14. Kritische Invarianten und Schutzmechanismen

Erstelle einen eigenen Abschnitt mit allen wichtigen Regeln, die während einer Synchronisation niemals verletzt werden dürfen.

Beispiele für zu untersuchende Bereiche:

Keine unbeabsichtigte Datenlöschung.
Keine unvollständige Datei als erfolgreich übertragen markieren.
Keine falsche Fortschreibung des gemeinsamen Ausgangszustands.
Keine unkontrollierten parallelen Synchronisationsläufe.
Keine inkonsistenten Datenbankzustände.
Keine unbeabsichtigte Wiederbelebung gelöschter Dateien.

Wichtig: Diese Beispiele sind Untersuchungsfragen und keine Behauptung, dass entsprechende Schutzmechanismen bereits existieren.

Dokumentiere für jede tatsächlich vorhandene Invariante:

Die Regel.
Ihre technische Bedeutung.
Die betroffenen Komponenten.
Wie sie im Code abgesichert wird.

Kennzeichne fehlende oder nicht eindeutig nachweisbare Schutzmechanismen ausdrücklich.

15. Tests und bekannte Einschränkungen

Untersuche die vorhandenen Tests zur Synchronisationsengine.

Dokumentiere:

Vorhandene Testklassen.
Abgedeckte Synchronisationsszenarien.
Getestete Konfliktfälle.
Getestete Löschfälle.
Fehler- und Wiederanlaufszenarien.
Nicht ausreichend getestete Bereiche.

Erstelle eine Übersicht kritischer Szenarien, die noch nicht ausreichend durch automatisierte Tests abgedeckt sind.

Führe keine umfangreichen Tests oder Build-Prozesse aus. Die vorhandenen Tests und deren Inhalt sollen zunächst analysiert werden.

16. Diagramme und Beispiele

Verwende geeignete Mermaid-Diagramme für:

Den vollständigen Synchronisationsablauf.
Den 3-Wege-Abgleich.
Die Konfliktbehandlung.
Wesentliche Zustandsübergänge.

Erstelle konkrete Beispiele für typische Synchronisationssituationen.

Die Diagramme müssen die tatsächliche Implementierung widerspiegeln.

17. Abgrenzung zu anderen Dokumenten

Vermeide unnötige Doppelungen mit:

docs/ARCHITECTURE.md
docs/DECISIONS.md
docs/DATA-MODEL.md
docs/TESTING.md

Diese Datei soll ausschließlich die Synchronisationsengine und deren fachliche und technische Abläufe behandeln.

Verweise bei Bedarf auf die jeweils zuständige Dokumentation.

18. Qualitätsanforderungen
Vollständige und nachvollziehbare Analyse des relevanten Quellcodes.
Technisch korrekte Beschreibungen.
Keine erfundenen Abläufe oder Schutzmechanismen.
Implementierte und geplante Funktionen klar unterscheiden.
Relevante Klassen und Dateipfade nennen.
Komplexe Abläufe anhand konkreter Beispiele erklären.
Kritische Sonderfälle nicht auslassen.
Keine umfangreichen Quellcodepassagen kopieren.
Offene Fragen und Unsicherheiten ausdrücklich kennzeichnen.
19. Arbeitsweise und Einschränkungen
Analysiere systematisch die relevanten Implementierungen.
Vermeide unnötiges mehrfaches Einlesen derselben Dateien.
Keine Änderungen am Programmcode.
Keine Änderungen an anderen Dokumentationsdateien.
Keine Refactorings.
Keine neuen Architekturentscheidungen treffen.
Ändere ausschließlich docs/SYNC-ENGINE.md.
Falls die Datei bereits existiert, aktualisiere sie unter Erhalt relevanter bestehender Informationen.
20. Abschlusskontrolle

Prüfe vor Abschluss:

Sind alle wesentlichen Synchronisationskomponenten berücksichtigt?
Ist der tatsächliche Ablauf korrekt beschrieben?
Ist der 3-Wege-Abgleich nachvollziehbar dokumentiert?
Sind Konflikt- und Löschlogik ausreichend beschrieben?
Sind Fehlerbehandlung und Wiederanlauf berücksichtigt?
Sind kritische Invarianten nachvollziehbar?
Sind alle relevanten Dateipfade korrekt?
Sind die Diagramme konsistent mit dem Quellcode?
Sind offene Fragen und fehlende Tests gekennzeichnet?
21. Abschlussbericht

Gib im Chat ausschließlich eine kurze Zusammenfassung aus:

Datei erstellt oder aktualisiert.
Untersuchte Komponenten.
Dokumentierte Synchronisationsabläufe.
Erstellte Diagramme.
Identifizierte technische Risiken.
Nicht eindeutig nachvollziehbare Sachverhalte.
Noch offene Dokumentationspunkte.

Gib nicht den vollständigen Inhalt der SYNC-ENGINE.md im Chat aus.