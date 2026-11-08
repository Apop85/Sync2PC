3. SONNET MEDIUM

Sync2PC – Erstellung der technischen Datenmodelldokumentation
Ziel

Erstelle eine vollständige, technisch korrekte und nachvollziehbare Dokumentation des Datenmodells von Sync2PC.

Die Dokumentation wird unter folgendem Pfad angelegt:

docs/DATA-MODEL.md

Sie soll als zentrale technische Referenz für sämtliche persistenten Datenstrukturen und deren Zusammenhänge dienen.

Das Ziel ist, dass zukünftige Claude-Code-Sessions schnell nachvollziehen können:

Welche Daten im Projekt gespeichert werden.
Wie die Daten strukturiert sind.
Welche Entities und Datenbanktabellen existieren.
Wie die einzelnen Datenobjekte zusammenhängen.
Welche Felder welche Bedeutung haben.
Welche Statuswerte und Zustände verwendet werden.
Welche Integritätsregeln eingehalten werden müssen.
Wie Datenbankmigrationen durchgeführt werden.
Welche Datenstrukturen für die Synchronisationsengine besonders wichtig sind.

Wichtig: Dokumentiere ausschließlich das tatsächlich implementierte Datenmodell. Keine erfundenen Tabellen, Felder, Beziehungen oder Integritätsregeln.

1. Quellen und Analysegrundlage

Untersuche den aktuellen Quellcode.

Berücksichtige insbesondere:

docs/ARCHITECTURE.md
docs/SYNC-ENGINE.md
docs/DECISIONS.md
README.md
Patchnotes
Room-Datenbankdefinitionen
Entity-Klassen
DAO-Klassen
Database-Klassen
Datenbankmigrationen
Repository-Klassen
Datenbankbezogene Services und Hilfsklassen
Tests für Datenbank und Datenmodell

Der aktuelle Quellcode ist die maßgebliche Quelle.

Berücksichtige auch historische Entscheidungen, sofern sie für das heutige Datenmodell relevant sind.

Unterscheide zwischen:

Aktuell implementierten Datenstrukturen.
Historischen Datenbankstrukturen.
Geplanten Änderungen.
Nicht mehr verwendeten Datenstrukturen.
2. Überblick über das Datenmodell

Beschreibe zunächst:

Welche Datenbanktechnologie verwendet wird.
Welche Datenbank(en) existieren.
Welche Aufgaben die einzelnen Datenbanken übernehmen.
Wie das Datenmodell in die Gesamtarchitektur eingebunden ist.
Welche Komponenten auf die Daten zugreifen.
Welche Daten für die Synchronisation besonders wichtig sind.

Erstelle eine übersichtliche Gesamtbeschreibung.

3. Datenbankstruktur und Tabellenübersicht

Erfasse sämtliche tatsächlich implementierten relevanten Datenbanktabellen.

Erstelle für jede Tabelle einen eigenen Abschnitt.

Dokumentiere:

Tabellenname

Zweck: Welche Aufgabe erfüllt die Tabelle?

Entity-Klasse: Welche Kotlin-Klasse repräsentiert die Tabelle?

Felder: Erstelle eine Tabelle mit folgenden Spalten:

Feldname.
Datentyp.
Nullable / Non-Nullable.
Standardwert, sofern vorhanden.
Primärschlüssel / Fremdschlüssel.
Bedeutung und Verwendung.

Indizes: Welche Indizes existieren und welchem Zweck dienen sie?

Beziehungen: Welche anderen Tabellen sind mit dieser Tabelle verbunden?

Besonderheiten: Welche technischen oder fachlichen Regeln sind zu beachten?

Dokumentiere auch Tabellen, die nicht direkt zur Synchronisationsengine gehören, aber dauerhaft wichtige Anwendungsdaten speichern.

Lasse temporäre oder ausschließlich generierte Datenstrukturen weg, sofern sie nicht architekturrelevant sind.

4. Entity-Klassen

Untersuche die vorhandenen Entity-Klassen im Detail.

Beschreibe:

Verantwortlichkeit der jeweiligen Entity.
Bedeutung der wichtigsten Eigenschaften.
Beziehungen zu anderen Entities.
Verwendete Annotationen.
Besondere Konstruktoren oder Datenkonvertierungen.
Abweichungen zwischen Kotlin-Datentypen und Datenbanktypen.

Dokumentiere auch wichtige Datenklassen, die nicht direkt Room-Entities sind, aber als fachliche Datenmodelle verwendet werden.

Unterscheide klar zwischen persistenten Entities und temporären Datenobjekten.

5. Beziehungen zwischen den Datenobjekten

Untersuche sämtliche relevanten Beziehungen.

Berücksichtige:

One-to-One.
One-to-Many.
Many-to-Many.
Fremdschlüssel.
Junction-Tabellen.
Room-Relation-Definitionen.
Indirekte Beziehungen über Identifikatoren.

Erkläre, wie die Beziehungen technisch umgesetzt sind.

Erstelle ein Mermaid-ER-Diagramm mit den wichtigsten Tabellen und deren Beziehungen.

Das Diagramm muss dem tatsächlichen Datenbankschema entsprechen.

Erfinde keine Beziehungen, die lediglich fachlich sinnvoll erscheinen, aber nicht implementiert sind.

6. DAO- und Repository-Struktur

Dokumentiere die Datenzugriffsschicht.

Untersuche:

Welche DAO-Klassen existieren.
Welche Tabellen sie verwalten.
Welche wichtigen Abfragen vorhanden sind.
Welche Insert-, Update- und Delete-Operationen existieren.
Welche Transaktionen verwendet werden.
Welche Abfragen für die Synchronisationsengine besonders relevant sind.
Welche Repository-Klassen existieren.
Wie die Datenzugriffe zwischen den Komponenten organisiert sind.

Erstelle eine Übersicht der wichtigsten Datenzugriffspfade.

Beschreibe insbesondere, welche Komponenten direkt auf DAOs zugreifen und welche Repository-Abstraktionen verwenden.

7. Synchronisationsrelevante Daten

Dieser Abschnitt ist besonders wichtig.

Untersuche sämtliche Datenstrukturen, die für den Synchronisationsprozess benötigt werden.

Dokumentiere insbesondere:

Speicherung des bekannten Dateibestands.
Lokale und entfernte Dateiidentifikatoren.
Informationen zum gemeinsamen Ausgangszustand des 3-Wege-Abgleichs.
Änderungsinformationen.
Zeitstempel.
Hashwerte, sofern vorhanden.
Synchronisationsstatus.
Konfliktinformationen.
Löschinformationen.
Papierkorbinformationen.
Übertragungsstatus.
Fehlerzustände.
Profilzuordnungen.

Erkläre die fachliche Bedeutung dieser Daten und wie die Synchronisationsengine sie verwendet.

Beschreibe, welche Informationen für die korrekte Erkennung von Änderungen und Konflikten unverzichtbar sind.

Verweise für die vollständigen Synchronisationsabläufe auf docs/SYNC-ENGINE.md.

8. Statuswerte und Zustandsmodelle

Identifiziere sämtliche wichtigen Statusfelder, Enums und Zustandswerte.

Für jeden relevanten Status:

Name.
Technischer Wert.
Fachliche Bedeutung.
Mögliche Vorgängerzustände.
Mögliche Folgezustände.
Welche Komponente den Status verändert.
Welche Auswirkungen der Status auf andere Komponenten hat.

Erstelle bei komplexeren Zustandsmodellen ein Mermaid-Zustandsdiagramm.

Dokumentiere auch implizite Zustände, sofern sie durch Kombination mehrerer Felder entstehen.

Unterscheide zwischen Datenbankstatus, temporärem Programmstatus und UI-Anzeigestatus.

9. Datenintegrität und Konsistenzregeln

Dieser Abschnitt ist besonders kritisch.

Untersuche alle implementierten Integritätsregeln.

Berücksichtige:

Primärschlüssel.
Fremdschlüssel.
Eindeutigkeitsbedingungen.
NOT-NULL-Bedingungen.
Indizes.
Transaktionen.
Kaskadierende Löschungen.
Vermeidung von Duplikaten.
Konsistenz zwischen zusammengehörigen Datensätzen.
Synchronisationsrelevante Datenbankbedingungen.

Beschreibe für jede wichtige Regel:

Was muss eingehalten werden?
Warum ist die Regel notwendig?
Wo wird sie technisch abgesichert?
Welche Folgen hätte eine Verletzung?

Unterscheide ausdrücklich zwischen:

Durch die Datenbank erzwungenen Regeln.
Durch Anwendungscode abgesicherten Regeln.
Ausschließlich fachlich vorausgesetzten Regeln.

Kennzeichne fehlende oder nicht eindeutig abgesicherte Integritätsbedingungen.

10. Datenbankmigrationen und Versionshistorie

Untersuche sämtliche vorhandenen Datenbankmigrationen.

Dokumentiere:

Aktuelle Datenbankversion.
Frühere Datenbankversionen.
Vorhandene Migrationen.
Änderungen am Datenbankschema.
Neue und entfernte Tabellen.
Hinzugefügte oder entfernte Felder.
Änderungen an Indizes und Beziehungen.
Datenkonvertierungen.
Besonderheiten bei bestehenden Installationen.

Erstelle eine chronologische Migrationsübersicht.

Beschreibe, welche Migrationen für den aktuellen Datenbankstand relevant sind.

Untersuche auch, ob Destruktionsmigrationen oder andere potenziell datenverlustbehaftete Verfahren verwendet werden.

Erfinde keine historischen Migrationsschritte, die nicht aus dem Quellcode oder der Versionshistorie nachvollziehbar sind.

11. TypeConverters und Serialisierung

Untersuche alle vorhandenen Datenkonvertierungen.

Berücksichtige:

Room TypeConverters.
Enum-Konvertierungen.
Datums- und Zeitkonvertierungen.
Listen und Collections.
JSON- oder andere Serialisierungsverfahren.
Speicherung komplexer Datenstrukturen.

Dokumentiere:

Ausgangstyp.
Gespeicherter Datentyp.
Konvertierungsmechanismus.
Besonderheiten und Einschränkungen.
12. Datenlebenszyklus

Beschreibe den Lebenszyklus wichtiger Datenobjekte.

Untersuche:

Erstellung.
Speicherung.
Aktualisierung.
Verwendung.
Archivierung, sofern vorhanden.
Löschung.
Wiederherstellung.

Beschreibe insbesondere den Lebenszyklus synchronisationsrelevanter Dateieinträge.

Erkläre, wie Änderungen im Datenmodell mit tatsächlichen Dateioperationen zusammenhängen.

Verweise bei Bedarf auf SYNC-ENGINE.md.

13. Datenbankzugriffe und Nebenläufigkeit

Untersuche:

Transaktionsverwendung.
Gleichzeitige Datenbankzugriffe.
Coroutine-basierte Datenbankoperationen.
Schreib- und Lesezugriffe während einer Synchronisation.
Konsistenz bei abgebrochenen Vorgängen.
Umgang mit konkurrierenden Änderungen.
Besondere Schutzmechanismen.

Dokumentiere bekannte Einschränkungen und mögliche Risiken.

Unterscheide zwischen tatsächlich implementierten Schutzmechanismen und lediglich theoretisch möglichen Lösungen.

14. Tests des Datenmodells

Untersuche die vorhandenen Tests.

Dokumentiere:

Entity-Tests.
DAO-Tests.
Datenbanktests.
Migrationstests.
Tests für Integritätsbedingungen.
Tests für Statusübergänge.
Tests für Duplikaterkennung.
Tests für synchronisationsrelevante Daten.

Erstelle eine Übersicht der vorhandenen Testabdeckung.

Benenne wichtige Datenmodellbereiche, die noch nicht ausreichend automatisiert getestet sind.

Führe keine umfangreichen Build- oder Testläufe durch. Analysiere zunächst die vorhandenen Tests.

15. Kritische Regeln für zukünftige Änderungen

Erstelle eine kompakte Übersicht der wichtigsten Regeln, die bei Änderungen am Datenmodell eingehalten werden müssen.

Beispiele für zu untersuchende Bereiche:

Bestehende Identifikatoren nicht unbedacht verändern.
Synchronisationsrelevante Metadaten vollständig erhalten.
Migrationen für bestehende Installationen berücksichtigen.
Fremdschlüsselbeziehungen nicht unbeabsichtigt verletzen.
Duplikaterkennung und Eindeutigkeitsbedingungen berücksichtigen.
Statusübergänge konsistent halten.
Datenbankänderungen mit den betroffenen DAOs und Repositorys abstimmen.

Diese Beispiele sind Untersuchungsfragen und keine Behauptung, dass alle entsprechenden Regeln bereits implementiert sind.

Verweise auf die jeweils relevanten Entscheidungen in DECISIONS.md.

16. Diagramme und Übersichten

Verwende geeignete Diagramme und Tabellen.

Mindestens vorgesehen:

ER-Diagramm des Datenbankschemas.
Tabellenübersicht.
Übersicht der wichtigsten Entity-Beziehungen.
Migrationsübersicht.
Zustandsdiagramme für komplexe Statusmodelle.

Die Diagramme müssen dem tatsächlichen Implementierungsstand entsprechen.

17. Abgrenzung zu anderen Dokumenten

Vermeide unnötige Doppelungen mit:

docs/ARCHITECTURE.md
docs/SYNC-ENGINE.md
docs/DECISIONS.md
docs/TESTING.md

Der Schwerpunkt dieser Datei liegt ausschließlich auf dem Datenmodell und dessen technischer und fachlicher Bedeutung.

18. Qualitätsanforderungen
Vollständige Analyse der relevanten Datenbankstrukturen.
Technisch korrekte Feld- und Typbeschreibungen.
Tatsächliche Beziehungen dokumentieren.
Implementierte und geplante Strukturen klar unterscheiden.
Relative Dateipfade verwenden.
Keine erfundenen Datenbankregeln.
Keine veralteten Strukturen als aktuell darstellen.
Keine umfangreichen Quellcodepassagen kopieren.
Offene Fragen ausdrücklich kennzeichnen.

Bei Unklarheiten nicht spekulieren.

19. Arbeitsweise und Einschränkungen
Analysiere systematisch alle relevanten Datenbankdateien.
Vermeide unnötiges mehrfaches Einlesen derselben Dateien.
Keine Änderungen am Programmcode.
Keine Änderungen an anderen Dokumentationsdateien.
Keine Refactorings.
Keine neuen Architekturentscheidungen treffen.
Ändere ausschließlich docs/DATA-MODEL.md.
Falls die Datei bereits existiert, aktualisiere sie unter Erhalt aller weiterhin relevanten Informationen.
20. Abschlusskontrolle

Prüfe vor Abschluss:

Sind alle relevanten Tabellen erfasst?
Sind alle wichtigen Entities dokumentiert?
Sind die Felddefinitionen korrekt?
Stimmen die Beziehungen mit dem Quellcode überein?
Sind DAO- und Repository-Verantwortlichkeiten nachvollziehbar?
Sind synchronisationsrelevante Daten ausreichend beschrieben?
Sind Statuswerte und Zustandsübergänge korrekt?
Sind Migrationen vollständig erfasst?
Sind die Diagramme korrekt?
Sind Datenintegritätsregeln nachvollziehbar?
Sind offene Fragen und Testlücken gekennzeichnet?
21. Abschlussbericht

Gib im Chat ausschließlich eine kurze Zusammenfassung aus:

Datei erstellt oder aktualisiert.
Anzahl der dokumentierten Tabellen.
Anzahl der dokumentierten Entities.
Anzahl der untersuchten DAOs.
Dokumentierte Migrationen.
Erstellte Diagramme.
Identifizierte Datenintegritätsrisiken.
Noch offene Fragen.

Gib nicht den vollständigen Inhalt der DATA-MODEL.md im Chat aus.