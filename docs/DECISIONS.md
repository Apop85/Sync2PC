# Entscheidungen – Sync2PC / LeoSync

## Auswertungen

> **Session 1** (Projektstart 0.0.1 – 0.0.13, Entscheidungen vom 2026-11-01): DEC-001 bis DEC-076.
> **Session 2** („Beta 0.1.x“, 0.1.0 – 0.4.0, Entscheidungen vom 2026-11-01 bis 2026-11-02): DEC-077 bis DEC-118; dazu Status- und Hinweis-Aktualisierungen an DEC-007, DEC-010, DEC-012, DEC-043, DEC-056, DEC-060, DEC-064, DEC-073, DEC-074, DEC-076.
> **Session 3** („Beta 0.4.x / 0.5.x“, 0.4.1 – 0.7.1, Entscheidungen vom 2026-11-02 bis 2026-11-03): DEC-119 bis DEC-154; dazu Aktualisierungen an DEC-013, DEC-036, DEC-083, DEC-091, DEC-092, DEC-105, DEC-109, DEC-110. Der genaue Tag (02. oder 03.11.) ist bei einigen Entscheidungen nicht sicher bestimmbar; angegeben ist dann 2026-11-03.
>
> **Grundlage:** ausschliesslich der jeweilige Gesprächsverlauf. Das Feld „Datum“ enthält das Erfassungsdatum und in Klammern das Entscheidungsdatum.
>
> **„Implemented“** heisst: im Code der jeweiligen Session umgesetzt.
> - Session 1: gebaut und getestet nur bis **0.0.10**; 0.0.11 bis 0.0.13 blieben dort ungebaut (in Session 2 als Teil von 0.1.x gebaut).
> - Session 2: 0.1.0 bis 0.4.0 wurden vom Inhaber gebaut; auf dem Gerät bestätigt sind ausdrücklich: Diagnose, OnePlus-Hinweise, Export/Import, Speicher freigeben, doppelte Profile, Ordner-Sync, Pause, Hund-Anzeige, Statusanzeige. 0.4.0 wurde gebaut und auf GitHub veröffentlicht; Gerätetests der 0.4.0-Neuerungen sind im Verlauf nicht bestätigt.
> - Session 3: 0.4.1 und 0.4.2 (nach Build-Fix) gebaut; 0.5.0 gebaut und im Feldtest genutzt (deckte den Fehler aus DEC-137 auf). Für 0.5.1 bis 0.6.4 ist im Verlauf nur der Build-Fehler von 0.6.1 (behoben in 0.6.2) belegt, kein Gerätetest. 0.7.0 scheiterte zuerst am Build (Kommentar-Falle, behoben ohne neue Version) und lief danach auf dem Gerät (Screenshot der Papierkorb-Ansicht). 0.7.1 blieb ungebaut.
>
> **Session 4** („Lokalisierung“, 0.8.0, Entscheidungen vom 2026-11-03): DEC-155 bis DEC-166; dazu Aktualisierungen an DEC-001, DEC-018, DEC-019, DEC-036, DEC-037, DEC-099 und DEC-125.
> - Session 4: 0.8.0 umgesetzt. Der erste Build scheiterte nur an der Kompilierung der Unit-Tests (Kommentar-Falle in `TestTexts.kt`), behoben ohne neue Version (DEC-120). Ein erfolgreicher Build und ein Gerätetest sind im Verlauf **nicht** belegt.
>
> **Session 5** („UX-Prüfung und Design-Skills“, 0.9.0 – 0.11.0, Entscheidungen vom 2026-11-03 bis 2026-11-08): DEC-167 bis DEC-205 (DEC-201 und DEC-202 am 2026-11-08; DEC-202 seit 0.12.0/0.12.1 Implemented, DEC-201 Accepted; DEC-203 Implemented; DEC-204 und DEC-205 am 2026-11-09, seit 0.13.0/0.13.1 Implemented); dazu Aktualisierungen an DEC-001, DEC-006, DEC-013, DEC-024, DEC-039, DEC-040, DEC-108, DEC-109, DEC-113, DEC-165.
> - Session 5: 0.9.0 bis 0.9.3 vom Inhaber gebaut (0.9.0 und 0.9.1 auf GitHub veröffentlicht); 0.9.4 bis 0.11.0 mit ausdrücklicher Erlaubnis vom Agent gebaut, Unit-Tests grün, `MigrationTest` im Emulator grün. Im Emulator (Android 10 und 17) mit einer Testfreigabe des Inhabers geprüft; die Statuskachel hat der Inhaber auf dem Gerät bestätigt (0.9.3).
> - Regelwerk-Refactoring (2026-11-10, kein Code): DEC-210 bis DEC-215 (Rollen, Koordination, Emulator, Doku, QA, UX-Navigation), alle Accepted; DEC-184 und DEC-185 durch DEC-212 ergänzt.
> - Versionen 0.16.0 bis 0.16.3 (2026-11-11): DEC-231 bis DEC-248 (Sync-Sperre und Folgelauf, «Beide behalten», Konflikt-Rückgängig, Verschlüsselungswert, Startseite und Dialoge, Teststruktur); Status Implemented laut Development, DEC-240 Accepted; QA- und UX-Prüfung offen.
> - Versionen 0.16.8 bis 0.16.35 (2026-11-11): DEC-249 bis DEC-264 (wartende Dateien, Icon-Stil, Zurückstellen geänderter Handy-Dateien, EN «Phone»); Status Implemented laut Development; QA-Prüfung offen.
> - Version 0.15.1 (2026-11-11): DEC-226 bis DEC-230 (Verlauf löschen, Papierkorb, Import «Ersetzen», Update-Hinweis, Sprache ausserhalb der Sicherung), Status Implemented laut Development.
> - Version 0.15.0 (2026-11-11): DEC-221 bis DEC-225 (Nachlauf, SMB-Verschlüsselung, Gleichheit vor Löschen, UX Nachlauf-Hinweis und Karte «Verschlüsselung»), Status Implemented laut Development; Aktualisierungen an DEC-021, DEC-052, DEC-092, DEC-134, DEC-217, DEC-219.
> - Version 0.14.7 (2026-11-10): DEC-216 bis DEC-218 (Profilformular), Status Implemented laut Development; DEC-219 (Überschneidung, 2026-11-10).
>
> **Hinweis:** Die frühere Notiz „Änderungen aus späteren Sessions sind nicht erfasst“ (Statistik-Symbol 0.9.0, Begriffe „Mobile/Share“, Roadmap ab 0.10.0) ist mit Session 5 erledigt.

### Statistik

| Status | Anzahl |
|---|---|
| Accepted | 24 |
| Implemented | 170 |
| Proposed | 3 |
| Rejected | 15 |
| Superseded | 18 |
| **Total** | **230** |

---

## Inhaltsübersicht

Die Entscheidungen sind nach Themen gruppiert (innerhalb eines Themas nach DEC-ID). Inhalt, IDs und Status sind unverändert; jede Entscheidung steht genau einmal vollständig im Dokument. Abgelehnte (Rejected) und überholte (Superseded) Entscheidungen stehen gesammelt in Abschnitt 16; der Index am Ende dieser Übersicht nennt zu jeder ID Thema und Abschnitt. Die Anhänge A bis D (offene Punkte, Änderungsbedarf, Dokumentationsbedarf, Risiken) stehen unverändert am Dateiende.

### Themen

1. [Architektur und Projektstruktur](#1-architektur-und-projektstruktur) (7) – Plattform, Profilmodell, Namen, Lizenz und Codeaufbau.
2. [Synchronisationsengine und 3-Wege-Abgleich](#2-synchronisationsengine-und-3-wege-abgleich) (8) – Richtung, Abgleichsregeln, Filter, Start neuer Profile, Sperre und Eingriffe beim laufenden Sync.
3. [Konflikterkennung und Konfliktbehandlung](#3-konflikterkennung-und-konfliktbehandlung) (5) – Rückfragen bei Konflikten, Änderungen während des Laufs, Konflikt-Zusatz.
4. [Dateiübertragung und SMB](#4-dateiübertragung-und-smb) (9) – SMB-Zugriff, Upload/Download, Fortsetzen, Zeitstempel, Verbindungsabbrüche, Zielauswahl auf dem PC.
5. [Foto-Sortierung (Bildordner)](#5-foto-sortierung-bildordner) (9) – Zielstruktur Jahr/Monat/Land, Datums- und Ländererkennung, Live-Fotos, Mindestalter, PC-Import-Skript.
6. [Datenbank und Datenmodell](#6-datenbank-und-datenmodell) (7) – Room, Migrationen, DB-Versionen, gespeicherte Namen und Texte.
7. [Datenintegrität und Duplikaterkennung](#7-datenintegrität-und-duplikaterkennung) (11) – Prüfsummen, Prüfstufen, Duplikatsuche, Namenskonflikte, sichere Vergleiche, doppelte Profile.
8. [Lösch- und Wiederherstellungslogik](#8-lösch--und-wiederherstellungslogik) (17) – Löschen nach dem Sync, Papierkorb, Versionsschutz, Wiederherstellen, Schutzbremse, Speicher freigeben.
9. [Android und Hintergrundprozesse](#9-android-und-hintergrundprozesse) (27) – WorkManager, Auslöser, WLAN-Erkennung, PC-Kennung, Pause, Vordergrunddienst, Akku, Berechtigungen, Zielversion.
10. [Benutzeroberfläche und UX](#10-benutzeroberfläche-und-ux) (48) – Startseite, Profil-Editor, Statusanzeigen, Verlauf, Einstellungen, Hinweise, Barrierefreiheit.
11. [Lokalisierung und Sprache](#11-lokalisierung-und-sprache) (6) – Zweisprachigkeit, Textzugriff, Sprachwahl, Übersetzungsregeln.
12. [Performance und Optimierungen](#12-performance-und-optimierungen) (6) – R8, Parallelität, Protokollierung, Symbole, Neuzeichnen.
13. [Sicherheit, Datenschutz und Diagnose](#13-sicherheit-datenschutz-und-diagnose) (6) – Passwortschutz, Diagnosedaten, Absturzbericht, Sicherung/Export, Sicherheitsprüfung.
14. [Verteilung: Updates, Play Store und Support](#14-verteilung-updates-play-store-und-support) (10) – Selbst-Update, Play-Store-Ziel, Paket-ID, Play Protect, Feedback, Kontakt, Produktseite.
15. [Build, Tests, Versionierung und Arbeitsregeln](#15-build-tests-versionierung-und-arbeitsregeln) (14) – Builder, Versionsplan, Arbeitsregeln für Agents, Testumgebung.
16. [Verworfene und überholte Lösungsansätze](#16-verworfene-und-überholte-lösungsansätze) (33) – Alle Entscheidungen mit Status Rejected oder Superseded, mit Verweis auf die ersetzende Entscheidung.

Anhänge: [A. Offene Entscheidungen](#a-offene-entscheidungen) · [B. Änderungsbedarf](#b-änderungsbedarf-an-bestehenden-entscheidungen) · [C. Dokumentationsbedarf](#c-dokumentationsbedarf) · [D. Technische Risiken](#d-technische-risiken)

### Entscheidungen je Thema

**1. Architektur und Projektstruktur**

- DEC-001: Native Android-App mit Kotlin und Jetpack Compose
- DEC-005: Profile mit eigenem Ziel, Zugangsdaten, Quellen und WLANs
- DEC-008: Sichtbarer Name LeoSync, Repo bleibt Sync2PC
- DEC-009: Lizenz MIT
- DEC-033: Zwei Profiltypen mit Assistent
- DEC-179: Lauf-Zustände als Konstanten statt freier Texte
- DEC-181: Grosse Bildschirme in Teile aufgeteilt

**2. Synchronisationsengine und 3-Wege-Abgleich**

- DEC-034: Sync-Richtung pro Profil, Bildordner nur Handy → PC
- DEC-035: 3-Wege-Abgleich zwischen Handy, PC und letztem Stand
- DEC-040: Einschluss- und Ausschlussfilter (White- und Blacklist)
- DEC-103: Neue Profile starten erst nach dem ersten manuellen Sync
- DEC-135: Importierte Ordner-Profile warten auf den ersten Start und eine Stichprobe
- DEC-144: Sperre zwischen Ordner-Sync und Eingriffen von Hand
- DEC-201: Eingriffe beim laufenden Ordner-Sync prozessweit vormerken
- DEC-238: Während des Laufs geänderte Dateien: Zähler `deferred` und begrenzter Folgelauf

**3. Konflikterkennung und Konfliktbehandlung**

- DEC-037: Konflikte fragen, optional pro Profil „immer neuere Datei“
- DEC-133: Nichts überschreiben, was sich während des Laufs geändert hat
- DEC-164: Konflikt-Zusatz `_conflict-phone`
- DEC-232: Konflikt-Rückgängig und Anwenden schliessen sich über bedingte DB-Abfragen aus
- DEC-239: «Beide behalten»: freier Konfliktname, nie überschreibend, wiederholbar

**4. Dateiübertragung und SMB**

- DEC-002: SMB-Zugriff über die Bibliothek smbj mit Benutzer und Passwort
- DEC-023: Upload über Temp-Datei mit Fortsetzen ab Abbruchstelle
- DEC-028: Ursprüngliches Änderungsdatum auf dem PC erhalten
- DEC-041: Speicherplatzprüfung vor Downloads
- DEC-053: Verbindungsabbrüche zählen nicht als Fehler, Verbindung wird neu aufgebaut
- DEC-094: Abgebrochene Downloads aufs Handy fortsetzen
- DEC-131: Upload nur mit Hash-Prüfung fortsetzen
- DEC-192: Ziel auf dem PC auswählen – Freigaben auflisten
- DEC-234: Gespeicherter Verschlüsselungswert: fehlend = «Bevorzugt», unbekannt = «Erforderlich»

**5. Foto-Sortierung (Bildordner)**

- DEC-016: Zielstruktur `Jahr\Monat` bzw. `Jahr\Monat - Land`
- DEC-017: Reihenfolge der Datumsermittlung
- DEC-018: Offline-Ländererkennung mit Natural Earth
- DEC-019: Umbenannte Monatsordner erkennen und einmal pro Gruppe fragen
- DEC-026: Live-Foto-Paare im selben Ordner halten
- DEC-027: Mindestalter für Dateien mit Nachhol-Sync
- DEC-068: PC-Import-Skript mit den Regeln der App
- DEC-161: Länderordner in der App-Sprache, bestehende Ordner aller App-Sprachen weiterverwenden
- DEC-209: Ordnerstruktur pro Fotoprofil wählbar (V1 bis V3)

**6. Datenbank und Datenmodell**

- DEC-003: Room-Datenbank als zentraler Zustandsspeicher
- DEC-084: Migrationsregeln – nie destruktiv, neue Spalten mit DEFAULT
- DEC-159: Verlaufstexte in der aktiven Sprache speichern
- DEC-202: Verlauf mit Kennungen (Code-Spalten mit Rückfalltext)
- DEC-203: Diagnose pseudonymisiert und maskiert (Abschnitt 13)
- DEC-162: Ordnername aus dem Länderkürzel, Cache-Name nur als Kennzeichen
- DEC-172: DB-Version 6 – Prüfstufe und bereinigte Profilnamen
- DEC-173: Profilnamen ohne Leerzeichen am Rand

**7. Datenintegrität und Duplikaterkennung**

- DEC-020: Namenskonflikte als `name_2`, `name_3` …
- DEC-021: Zweistufige Duplikaterkennung mit Teil-Prüfsumme
- DEC-022: Duplikatsuche in allen Ordnern desselben Monats
- DEC-092: Doppelte Profile blockieren, Überschneidungen warnen
- DEC-219: Derselbe Handy-Ordner mit mehreren PC-Zielen ist zulässig
- DEC-223: Gleichheit vor Löschen, Freigeben und «synchron» wird inhaltlich geprüft (präzisiert DEC-021)
- DEC-129: Stichprobe nach einem Ordnerwechsel
- DEC-130: Handy-Dateien sicher ersetzen (`.s2pold`)
- DEC-132: Gründlicher Inhaltsvergleich – bis 32 MB vollständig, darüber 32 Stichproben
- DEC-134: Warnung bei gleichem oder verschachteltem Ordner zweier Ordner-Profile
- DEC-136: Speicher freigeben – Fund in anderem Monatsordner nur mit gleicher Prüfsumme
- DEC-137: SAF-Werte aus `stat()` und `children()` nie mischen; inhaltsgleich heisst nur Stand merken
- DEC-171: Prüfstufen nach dem Hochladen

**8. Lösch- und Wiederherstellungslogik**

- DEC-025: Löschen nach dem Sync (behalten, Papierkorb, löschen)
- DEC-036: Löschungen optional übernehmen, Ersetztes immer in den Papierkorb
- DEC-083: Papierkorb mit Frist pro Profil automatisch leeren
- DEC-091: Speicher freigeben nur nach Prüfung jeder Datei auf dem PC
- DEC-127: Schutzbremse – Ordner-Sync hält an und fragt nach
- DEC-128: Schwellen für Massenlöschungen
- DEC-138: Versionsschutz pro Ordner-Profil
- DEC-139: Wiederherstellungen vormerken, der nächste Sync mit PC führt sie aus
- DEC-140: Papierkorb von Hand leeren
- DEC-141: Ein Tool „Papierkorb“ statt „Wiederherstellen“ und „Papierkorb leeren“
- DEC-142: Papierkorb für alle Ordner-Profile, auch ohne Versionsschutz
- DEC-143: Eindeutige Namen im Papierkorb mit Uhrzeit
- DEC-145: Vorgemerkte Wiederherstellungen werden nie gelöscht
- DEC-146: Wiederherstellen rollt bei Fehlern zurück, die Vormerkung bleibt
- DEC-147: Speicher freigeben auch für Ordner-Profile Handy → PC
- DEC-163: Papierkorb heisst `.LeoSync-Bin`, alte Namen bleiben erkannt
- DEC-231: Löschen über den Android-Systemdialog: Prüfung der PC-Kopie nur unmittelbar davor

**9. Android und Hintergrundprozesse**

- DEC-030: Medienzugriff über MediaStore mit unzensierten GPS-Daten
- DEC-038: Ordner-Spiegelung über SAF mit dauerhafter Berechtigung
- DEC-042: Prüfintervall für Ordner-Profile
- DEC-043: WorkManager-basierte Auslöser-Architektur
- DEC-044: WLAN-Erkennung über SSID mit Standortberechtigung
- DEC-045: Erreichbarkeitsprüfung des PCs mit Wiederholungen nach dem Verbinden
- DEC-048: 30 Minuten warten, wenn der PC im Ziel-WLAN nicht erreichbar ist
- DEC-049: Nach WLAN-Unterbruch sofort beim Wiederverbinden fortsetzen
- DEC-050: Zusätzlicher Fortsetzen-Auftrag beim Aufgabenplaner
- DEC-052: Anzeige „Pausiert“ als Vordergrunddienst bis zur Rückkehr des WLANs
- DEC-054: WLAN- und CPU-Sperre während der Übertragung
- DEC-055: Ausnahme von der Akku-Optimierung anfordern
- DEC-082: Hersteller-Hinweise gegen App-Killer und deren Erkennung
- DEC-086: „Unterbrochen“, „Pausiert“ und offene Läufe im Verlauf unterscheiden
- DEC-090: Erinnerung „lange nicht gesichert“
- DEC-095: Benutzer-Pause befristet oder „für immer (nur manuell)“
- DEC-101: PC nur bei Bedarf kontaktieren, Foto-Auslöser wartet das Mindestalter ab
- DEC-102: Sicherheitsnetz 6 Stunden nach dem letzten Sync
- DEC-126: Option „auch über mobile Daten bzw. VPN synchronisieren“
- DEC-204: PC-Erkennung per PC-Kennung (SMB-Server-GUID), Modus je Profil
- DEC-205: PC-Kennung nur beim Speichern und in „Verbindung testen“ lernen (ändert DEC-204 Punkte 1 und 5)
- DEC-206: Laufanimation: Einzelbilder als feste Quell-Assets (ersetzt DEC-060)
- DEC-207: Fehlermeldungen im UI in einem zentralen Fehler-Rotton
- DEC-208: Rückfrage beim Hostwechsel eines bestehenden Profils (schliesst R1 aus DEC-205 für diesen Fall)
- DEC-221: Verworfener Start während eines laufenden Syncs wird einmal nachgeholt
- DEC-152: App nur im Hochformat
- DEC-183: Ziel-Version Android 16 (API 36) vorgezogen
- DEC-190: Berechtigungen erst bei Bedarf
- DEC-233: Sonderaufträge bei belegter Sync-Sperre: Wiederholung statt Nachlauf-Merker
- DEC-236: Fehler beim Speichern des Nachlauf-Merkers: nur Diagnose-Log
- DEC-237: «Verschlüsselung nicht verfügbar» bei automatischen Läufen: einmal je Profil bis zum nächsten Erfolg

**10. Benutzeroberfläche und UX**

- DEC-031: Quellordner über den Android-Systemdialog wählen
- DEC-060: Animation in der Fortschrittsanzeige als Einzelbilder aus dem Video
- DEC-063: Einfarbiges Statusleisten-Symbol aus dem Hunde-Logo
- DEC-064: Zustandsbilder „wartet“ und „alles aktuell“
- DEC-065: Startbildschirm mit sitzendem Hund und „LeoSync“
- DEC-066: Statistik nach Jahr und Land
- DEC-067: Grund für übersprungenen Auto-Start auf der Startseite anzeigen
- DEC-072: Vollständigkeitsbilanz pro Profil *(Proposed)*
- DEC-088: Zustandsbild auf der Startseite immer sichtbar, mit Laufanimation
- DEC-096: Knopf „Abbrechen“ heisst „Pausieren“
- DEC-097: Nachfrage bei ungespeicherten Profiländerungen, Speichern oben
- DEC-098: Statusanzeige unterscheidet fällige von zeitversetzten Aufträgen
- DEC-099: Kurze Lauf-Zusammenfassungen ohne „übersprungen“
- DEC-100: Verlauf gruppiert ruhige Läufe, Filter „Alle / Fehler / Übertragungen“
- DEC-104: Simulation pro Profil mit wählbarer Dateianzahl
- DEC-105: Warnung vor der ersten Synchronisierung ohne Ladegerät
- DEC-106: Einstellungen in sechs Rubriken, „Updates“ prüft sofort
- DEC-108: Benachrichtigungen einzeln abschaltbar, Fortschritt und „Pausiert“ nicht
- DEC-148: Auswahl in Zeilen mit Wertfeld und Vorschau, gleiche Ansicht für Fotos
- DEC-149: Tools-Sektion – eine Zeile pro Tool, immer sichtbar, ausgegraut mit Hinweis
- DEC-150: Profil-Assistent in Schritten
- DEC-151: Wertfeld mit Auswahlliste statt Knopfreihen
- DEC-167: Pause-Symbol zeigt die Aktion, nicht den Zustand
- DEC-168: Statuskachel mit festen Plätzen in jedem Zustand
- DEC-169: Einheitliche Begriffe in der Oberfläche
- DEC-170: „Sync“ in deutschen Texten ausschreiben
- DEC-174: Profil bearbeiten – Knöpfe unten, Löschen in „Wartung“, optionale Bereiche zugeklappt
- DEC-175: Speicher freigeben – Vorschau als Hauptschritt
- DEC-176: Rubrik „Sprache & Sicherung“
- DEC-177: Seite „App-Berechtigungen“ mit Status je Punkt
- DEC-178: Kleinere Vereinheitlichungen der Oberfläche
- DEC-186: Herunterziehen startet eine Synchronisierung
- DEC-187: Zugangsdaten über einen Knopf mit zusammengefasster Liste übernehmen
- DEC-189: Verständliche Fehlermeldungen mit nächstem Schritt
- DEC-191: Erfolgsmeldung nach jeder Synchronisierung bei Neuinstallation aus
- DEC-193: Screenreader – Statuswechsel ansagen, Richtungspfeile als Wort
- DEC-194: Lange Hinweise mit „Mehr dazu“
- DEC-195: Geschätzte Restzeit ab einer Minute
- DEC-196: Profil löschen mit „Rückgängig“
- DEC-216: Bestätigung beim Ändern von Richtung oder «Löschungen übernehmen»
- DEC-217: Zahlenfelder im Profilformular prüfen statt still korrigieren
- DEC-218: Fotos-Ordner über «Downloads»: nur interner Speicher
- DEC-224: Nachlauf-Hinweis: Snackbar und Zeile «Danach: …» in der Statuskarte
- DEC-225: Karte «Verschlüsselung» im Profilformular
- DEC-226: Verlauf löschen mit Bestätigung und «Rückgängig»
- DEC-227: Papierkorb: nur Texte und Uhrzeit
- DEC-228: Import «Ersetzen» in Warnfarbe, ohne zusätzliche Bestätigung
- DEC-229: Update: Hinweis statt automatischem Neustart
- DEC-230: Die App-Sprache bleibt ausserhalb der Sicherung
- DEC-235: Überschneidungswarnung Fotos ↔ Ordner: datenträgerunabhängig, Speicherwurzel warnt
- DEC-241: Statuskarte: neuer Zustand «↻ Noch nicht alles gesichert»
- DEC-242: Startseite: Hinweis bei fehlender Standort-Berechtigung (R4)
- DEC-243: Profilkarte: Zeile bei verlorenem Handy-Ordner-Zugriff (R5)
- DEC-244: Startseite: «⏸ Pausiert» als Überschrift (R6)
- DEC-245: «Keine Verbindung»: «PC nicht erreichbar» ohne doppelte Zeitangabe (R7)
- DEC-246: Dialoge mit drei oder mehr Knöpfen sind gestapelt
- DEC-247: Überlaufender Dialogtext zeigt Scrollbalken und Verlauf
- DEC-248: Konfliktkarte: «(neuer)» erst ab einer Sekunde Unterschied, sonst «(gleich alt)»
- DEC-249: Wartende Dateien nur lokal gezählt; leere WLAN-Zeile bleibt als Leerzeile
- DEC-250: Icon-Stil «Variante C Duotone» statt Emojis
- DEC-251: Geänderte Handy-Datei beim Upload wird zurückgestellt statt als Fehler gezählt
- DEC-252: Englisch nennt das Gerät «Phone» statt «Mobile»
- DEC-253: Deutscher Begriff «Abgleichstand» statt «Synchronisierungsstatus»
- DEC-254: Handy-Papierkorb heisst bei `msd:`-Ordnern «@LeoSync-Bin»
- DEC-255: Verschlüsselungs-Icon zeigt den tatsächlichen Zustand der letzten Anmeldung
- DEC-256: Datum und Uhrzeit folgen dem Systemformat des Handys
- DEC-257: «Letzte Synchronisierung» ist der letzte fehlerfreie Lauf
- DEC-258: Profilnamen sind einmalig; Import «Hinzufügen» prüft sie bewusst nicht
- DEC-259: WLAN-Name ist Pflicht bei «Nur WLAN-Name» und «Beides»
- DEC-260: Papierkorb heisst überall `@LeoSync-Bin`; Migration und PC-Rückfall
- DEC-261: Zu kurzes Dateiende beim Download ist eine Unterbrechung, nur mit Fortschritt
- DEC-262: SMB-Zeitlimits bleiben bei 60 s und 90 s
- DEC-263: Handy-Papierkorb und Galerie: Ausschluss und `.nomedia`
- DEC-264: Auftragsverwaltung über ein zentrales Skript
- DEC-265: Doppelte Felder in Altdateien werden toleriert
- DEC-266: Technische Schreibsperre für `coordination/` und Alias-Sperre
- DEC-267: Dublettenprüfung bei `erstellen` bricht vor dem Schreiben ab
- DEC-268: Ablaufregeln zur Token-Einsparung und Regelwerk-Klarstellungen
- DEC-269: Papierkorb-Ansicht: Seitenfilter «Anzeigen» und erweiterter Wiederherstellen-Dialog

**11. Lokalisierung und Sprache**

- DEC-155: App zweisprachig, Englisch als Grundsprache
- DEC-156: Zentraler Textzugriff `str()`/`plural()` statt `stringResource`
- DEC-157: Unit-Test für Vollständigkeit der Übersetzungen
- DEC-158: Sprachwahl in den App-Einstellungen
- DEC-160: Diagnose und PC-Import-Skript nicht übersetzen
- DEC-166: Projektregel 15 – Übersetzungen aktuell halten, Tabelle mit Zeilennummern

**12. Performance und Optimierungen**

- DEC-014: R8-Minify mit Keep-Regeln aktivieren
- DEC-029: Parallele Uploads mit eigener Verbindung je Worker
- DEC-087: Bereits vorhandene Dateien pro Ordner zusammengefasst protokollieren
- DEC-093: Paralleler Ordner-Sync in zwei Phasen, kleine Dateien zuerst
- DEC-153: Eigene Symbole statt material-icons-extended
- DEC-182: Startseite liest Einstellungen nur bei Zustandswechseln

**13. Sicherheit, Datenschutz und Diagnose**

- DEC-004: SMB-Passwörter mit dem Android-Keystore verschlüsseln
- DEC-079: Diagnosedaten in die Zwischenablage, mit zufälliger Geräte-ID
- DEC-080: Verlauf als Textdatei über das Android-Teilen-Menü exportieren
- DEC-081: Lokaler Absturzbericht in der Diagnose
- DEC-089: Sicherung von Profilen, Einstellungen und Sync-Status (Export/Import)
- DEC-200: Sicherheitsprüfung mit claude-security *(Proposed)*
- DEC-222: SMB3-Verschlüsselung pro Profil (Aus / Wenn möglich / Erforderlich)

**14. Verteilung: Updates, Play Store und Support**

- DEC-056: Selbst-Update über GitHub Releases mit Installation per Tippen
- DEC-059: Neustart nach dem Update mit Benachrichtigung als Rückfall
- DEC-107: Update-Quelle fest eingebaut
- DEC-109: Ziel Play Store; Vorbereitung beschlossen, aber nicht in 0.4.0
- DEC-110: Neue Paket-ID `ch.leo.sync`, Schema `ch.leo.<appname>`
- DEC-111: Play-Protect-Sperre per Einspruch klären, nicht umgehen
- DEC-121: Feedback in der Testphase per E-Mail-Knopf „Problem melden“
- DEC-123: Kontaktadresse `leotools@protonmail.com`
- DEC-124: Nur die Play-Variante erhält `ch.leo.sync`
- DEC-197: Hilfe & Produktseite

**15. Build, Tests, Versionierung und Arbeitsregeln**

- DEC-011: One-Click-Builder, der nur bei Quelländerungen baut
- DEC-013: Arbeitsregeln für Agents (Agent baut nie, GO-Regel)
- DEC-075: `release.bat` mit GitHub-CLI *(Proposed)*
- DEC-077: Beta-Versionierung ab 0.1.0, neue Funktionen eröffnen die nächste Reihe
- DEC-078: Antworten nach einer Änderung enden mit der Versionsnummer
- DEC-085: Build-Werkzeuge auf Gradle 9 / AGP 9 mit eingebautem Kotlin
- DEC-119: Kurzfassung vor dem GO nur auf Aufforderung
- DEC-120: Build-Fehler einer nie gebauten Version ohne neue Versionsnummer beheben
- DEC-154: KSP-Release wartet auf KSP-Debug
- DEC-180: Instrumentierter Migrationstest vorgezogen
- DEC-184: Builds und Emulator-Tests durch den Agent nur mit Erlaubnis pro Lauf
- DEC-185: Testumgebung mit Emulator und Testfreigabe
- DEC-188: Versionsplan 0.10.0 bis 0.13.0
- DEC-210: Sechs Chat-Rollen und zentrale Koordinationsrichtlinie
- DEC-211: Direkte Übergaben zwischen Fachrollen, Project Manager als Koordinator
- DEC-212: Emulator für UX und QA ohne Einzelerlaubnis (ergänzt DEC-184)
- DEC-213: Dokumentation nur durch Documentation, UX-Entscheidungen in §10
- DEC-214: QA führt keine Builds aus
- DEC-215: UX prüft selbstständig und führt eine Navigationsdatei
- DEC-220: QA baut Debug-/Test-Builds selbstständig (ergänzt DEC-214)
- DEC-240: Teststruktur für Engine-Abläufe: `RemoteStore`, `LocalFiles`, ausgelagertes Wiederherstellen

**16. Verworfene und überholte Lösungsansätze**

- DEC-006: Zugangsdaten aus einem anderen Profil übernehmen *(Superseded)*
- DEC-007: Interne App-ID `com.sync2pc.app` beibehalten *(Superseded)*
- DEC-010: Versionierung 0.0.x bis zur Freigabe von 1.0.0 *(Superseded)*
- DEC-012: Build nur mit Java 17–24, sonst portables JDK 21 *(Superseded)*
- DEC-015: Minify deaktiviert lassen *(Superseded)*
- DEC-024: Prüfung nach dem Upload, Pflicht vor dem Löschen *(Superseded)*
- DEC-032: Eigene Ordnerliste aus dem MediaStore *(Superseded)*
- DEC-039: Unterordner auf dem Share pro Profil mit Ordner-Browser *(Superseded)*
- DEC-046: Einzelner 3-Sekunden-Erreichbarkeitstest *(Superseded)*
- DEC-047: Wiederholung über WorkManager-Backoff *(Superseded)*
- DEC-051: Eigene Benachrichtigung „Sync unterbrochen“ *(Superseded)*
- DEC-057: Update-Quelle SMB-Ordner *(Rejected)*
- DEC-058: Automatische Update-Installation im Hintergrund *(Rejected)*
- DEC-061: Animation aus dem Sprite-Sheet `gif_frames.png` *(Superseded)*
- DEC-062: Animiertes GIF direkt in der Benachrichtigung *(Rejected)*
- DEC-069: Verschieben statt Kopieren im Import *(Rejected)*
- DEC-070: Feste Regel „Fotos ohne GPS eines Monats in den Länderordner“ *(Rejected)*
- DEC-071: Beidseitiger Sync für Bildordner *(Rejected)*
- DEC-073: Papierkorb automatisch leeren (z. B. nach 30 Tagen) *(Superseded)*
- DEC-074: Parallele Übertragung im Ordner-Sync *(Superseded)*
- DEC-076: Upgrade auf Gradle 9 bzw. AGP 9 (Java-25-tauglich) *(Superseded)*
- DEC-112: R8-Verschleierung abschalten gegen die Play-Protect-Sperre *(Rejected)*
- DEC-113: Instrumentierter Migrationstest erst vor 1.0.0 *(Superseded)*
- DEC-114: Schnellzugriff (Kachel oder Widget) „Jetzt synchronisieren“ *(Rejected)*
- DEC-115: „Pausiert“-Anzeige nach WLAN-Abbruch kürzer als 24 Stunden halten *(Rejected)*
- DEC-116: Prüfintervall der Ordner-Profile seltener bzw. nur beim Laden *(Rejected)*
- DEC-117: Bearbeitete Fotos ersetzen die ältere Version auf dem PC *(Rejected)*
- DEC-118: Dateiattribute (z. B. Windows-Archiv-Bit) auswerten *(Rejected)*
- DEC-122: Feedback über einen Discord-Webhook *(Rejected)*
- DEC-125: Versionsplan 0.6.0 bis 0.9.0 *(Superseded)*
- DEC-165: Versionsplan mit Lokalisierung als 0.8.0 *(Superseded)*
- DEC-198: Englische Texte behalten „sync“ *(Rejected)*
- DEC-199: Statuskachel ganz fest mit gekürzten Meldungen *(Rejected)*

### Index nach DEC-ID

| ID | Status | Abschnitt |
|---|---|---|
| DEC-001 | Implemented | 1. Architektur und Projektstruktur |
| DEC-002 | Implemented | 4. Dateiübertragung und SMB |
| DEC-003 | Implemented | 6. Datenbank und Datenmodell |
| DEC-004 | Implemented | 13. Sicherheit, Datenschutz und Diagnose |
| DEC-005 | Implemented | 1. Architektur und Projektstruktur |
| DEC-006 | Superseded | 16. Verworfene und überholte Lösungsansätze |
| DEC-007 | Superseded | 16. Verworfene und überholte Lösungsansätze |
| DEC-008 | Implemented | 1. Architektur und Projektstruktur |
| DEC-009 | Implemented | 1. Architektur und Projektstruktur |
| DEC-010 | Superseded | 16. Verworfene und überholte Lösungsansätze |
| DEC-011 | Implemented | 15. Build, Tests, Versionierung und Arbeitsregeln |
| DEC-012 | Superseded | 16. Verworfene und überholte Lösungsansätze |
| DEC-013 | Accepted | 15. Build, Tests, Versionierung und Arbeitsregeln |
| DEC-014 | Implemented | 12. Performance und Optimierungen |
| DEC-015 | Superseded | 16. Verworfene und überholte Lösungsansätze |
| DEC-016 | Implemented | 5. Foto-Sortierung (Bildordner) |
| DEC-017 | Implemented | 5. Foto-Sortierung (Bildordner) |
| DEC-018 | Implemented | 5. Foto-Sortierung (Bildordner) |
| DEC-019 | Implemented | 5. Foto-Sortierung (Bildordner) |
| DEC-020 | Implemented | 7. Datenintegrität und Duplikaterkennung |
| DEC-021 | Implemented | 7. Datenintegrität und Duplikaterkennung |
| DEC-022 | Implemented | 7. Datenintegrität und Duplikaterkennung |
| DEC-023 | Implemented | 4. Dateiübertragung und SMB |
| DEC-024 | Superseded | 16. Verworfene und überholte Lösungsansätze |
| DEC-025 | Implemented | 8. Lösch- und Wiederherstellungslogik |
| DEC-026 | Implemented | 5. Foto-Sortierung (Bildordner) |
| DEC-027 | Implemented | 5. Foto-Sortierung (Bildordner) |
| DEC-028 | Implemented | 4. Dateiübertragung und SMB |
| DEC-029 | Implemented | 12. Performance und Optimierungen |
| DEC-030 | Implemented | 9. Android und Hintergrundprozesse |
| DEC-031 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-032 | Superseded | 16. Verworfene und überholte Lösungsansätze |
| DEC-033 | Implemented | 1. Architektur und Projektstruktur |
| DEC-034 | Implemented | 2. Synchronisationsengine und 3-Wege-Abgleich |
| DEC-035 | Implemented | 2. Synchronisationsengine und 3-Wege-Abgleich |
| DEC-036 | Implemented | 8. Lösch- und Wiederherstellungslogik |
| DEC-037 | Implemented | 3. Konflikterkennung und Konfliktbehandlung |
| DEC-038 | Implemented | 9. Android und Hintergrundprozesse |
| DEC-039 | Superseded | 16. Verworfene und überholte Lösungsansätze |
| DEC-040 | Implemented | 2. Synchronisationsengine und 3-Wege-Abgleich |
| DEC-041 | Implemented | 4. Dateiübertragung und SMB |
| DEC-042 | Implemented | 9. Android und Hintergrundprozesse |
| DEC-043 | Implemented | 9. Android und Hintergrundprozesse |
| DEC-044 | Implemented | 9. Android und Hintergrundprozesse |
| DEC-045 | Implemented | 9. Android und Hintergrundprozesse |
| DEC-046 | Superseded | 16. Verworfene und überholte Lösungsansätze |
| DEC-047 | Superseded | 16. Verworfene und überholte Lösungsansätze |
| DEC-048 | Implemented | 9. Android und Hintergrundprozesse |
| DEC-049 | Implemented | 9. Android und Hintergrundprozesse |
| DEC-050 | Implemented | 9. Android und Hintergrundprozesse |
| DEC-051 | Superseded | 16. Verworfene und überholte Lösungsansätze |
| DEC-052 | Implemented | 9. Android und Hintergrundprozesse |
| DEC-053 | Implemented | 4. Dateiübertragung und SMB |
| DEC-054 | Implemented | 9. Android und Hintergrundprozesse |
| DEC-055 | Implemented | 9. Android und Hintergrundprozesse |
| DEC-056 | Implemented | 14. Verteilung: Updates, Play Store und Support |
| DEC-057 | Rejected | 16. Verworfene und überholte Lösungsansätze |
| DEC-058 | Rejected | 16. Verworfene und überholte Lösungsansätze |
| DEC-059 | Implemented | 14. Verteilung: Updates, Play Store und Support |
| DEC-060 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-061 | Superseded | 16. Verworfene und überholte Lösungsansätze |
| DEC-062 | Rejected | 16. Verworfene und überholte Lösungsansätze |
| DEC-063 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-064 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-065 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-066 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-067 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-068 | Implemented | 5. Foto-Sortierung (Bildordner) |
| DEC-069 | Rejected | 16. Verworfene und überholte Lösungsansätze |
| DEC-070 | Rejected | 16. Verworfene und überholte Lösungsansätze |
| DEC-071 | Rejected | 16. Verworfene und überholte Lösungsansätze |
| DEC-072 | Proposed | 10. Benutzeroberfläche und UX |
| DEC-073 | Superseded | 16. Verworfene und überholte Lösungsansätze |
| DEC-074 | Superseded | 16. Verworfene und überholte Lösungsansätze |
| DEC-075 | Proposed | 15. Build, Tests, Versionierung und Arbeitsregeln |
| DEC-076 | Superseded | 16. Verworfene und überholte Lösungsansätze |
| DEC-077 | Implemented | 15. Build, Tests, Versionierung und Arbeitsregeln |
| DEC-078 | Accepted | 15. Build, Tests, Versionierung und Arbeitsregeln |
| DEC-079 | Implemented | 13. Sicherheit, Datenschutz und Diagnose |
| DEC-080 | Implemented | 13. Sicherheit, Datenschutz und Diagnose |
| DEC-081 | Implemented | 13. Sicherheit, Datenschutz und Diagnose |
| DEC-082 | Implemented | 9. Android und Hintergrundprozesse |
| DEC-083 | Implemented | 8. Lösch- und Wiederherstellungslogik |
| DEC-084 | Implemented | 6. Datenbank und Datenmodell |
| DEC-085 | Implemented | 15. Build, Tests, Versionierung und Arbeitsregeln |
| DEC-086 | Implemented | 9. Android und Hintergrundprozesse |
| DEC-087 | Implemented | 12. Performance und Optimierungen |
| DEC-088 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-089 | Implemented | 13. Sicherheit, Datenschutz und Diagnose |
| DEC-090 | Implemented | 9. Android und Hintergrundprozesse |
| DEC-091 | Implemented | 8. Lösch- und Wiederherstellungslogik |
| DEC-092 | Implemented | 7. Datenintegrität und Duplikaterkennung |
| DEC-093 | Implemented | 12. Performance und Optimierungen |
| DEC-094 | Implemented | 4. Dateiübertragung und SMB |
| DEC-095 | Implemented | 9. Android und Hintergrundprozesse |
| DEC-096 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-097 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-098 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-099 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-100 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-101 | Implemented | 9. Android und Hintergrundprozesse |
| DEC-102 | Implemented | 9. Android und Hintergrundprozesse |
| DEC-103 | Implemented | 2. Synchronisationsengine und 3-Wege-Abgleich |
| DEC-104 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-105 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-106 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-107 | Implemented | 14. Verteilung: Updates, Play Store und Support |
| DEC-108 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-109 | Accepted | 14. Verteilung: Updates, Play Store und Support |
| DEC-110 | Accepted | 14. Verteilung: Updates, Play Store und Support |
| DEC-111 | Accepted | 14. Verteilung: Updates, Play Store und Support |
| DEC-112 | Rejected | 16. Verworfene und überholte Lösungsansätze |
| DEC-113 | Superseded | 16. Verworfene und überholte Lösungsansätze |
| DEC-114 | Rejected | 16. Verworfene und überholte Lösungsansätze |
| DEC-115 | Rejected | 16. Verworfene und überholte Lösungsansätze |
| DEC-116 | Rejected | 16. Verworfene und überholte Lösungsansätze |
| DEC-117 | Rejected | 16. Verworfene und überholte Lösungsansätze |
| DEC-118 | Rejected | 16. Verworfene und überholte Lösungsansätze |
| DEC-119 | Accepted | 15. Build, Tests, Versionierung und Arbeitsregeln |
| DEC-120 | Accepted | 15. Build, Tests, Versionierung und Arbeitsregeln |
| DEC-121 | Accepted | 14. Verteilung: Updates, Play Store und Support |
| DEC-122 | Rejected | 16. Verworfene und überholte Lösungsansätze |
| DEC-123 | Accepted | 14. Verteilung: Updates, Play Store und Support |
| DEC-124 | Accepted | 14. Verteilung: Updates, Play Store und Support |
| DEC-125 | Superseded | 16. Verworfene und überholte Lösungsansätze |
| DEC-126 | Accepted | 9. Android und Hintergrundprozesse |
| DEC-127 | Implemented | 8. Lösch- und Wiederherstellungslogik |
| DEC-128 | Implemented | 8. Lösch- und Wiederherstellungslogik |
| DEC-129 | Implemented | 7. Datenintegrität und Duplikaterkennung |
| DEC-130 | Implemented | 7. Datenintegrität und Duplikaterkennung |
| DEC-131 | Implemented | 4. Dateiübertragung und SMB |
| DEC-132 | Implemented | 7. Datenintegrität und Duplikaterkennung |
| DEC-133 | Implemented | 3. Konflikterkennung und Konfliktbehandlung |
| DEC-134 | Implemented | 7. Datenintegrität und Duplikaterkennung |
| DEC-135 | Implemented | 2. Synchronisationsengine und 3-Wege-Abgleich |
| DEC-136 | Implemented | 7. Datenintegrität und Duplikaterkennung |
| DEC-137 | Implemented | 7. Datenintegrität und Duplikaterkennung |
| DEC-138 | Implemented | 8. Lösch- und Wiederherstellungslogik |
| DEC-139 | Implemented | 8. Lösch- und Wiederherstellungslogik |
| DEC-140 | Implemented | 8. Lösch- und Wiederherstellungslogik |
| DEC-141 | Implemented | 8. Lösch- und Wiederherstellungslogik |
| DEC-142 | Implemented | 8. Lösch- und Wiederherstellungslogik |
| DEC-143 | Implemented | 8. Lösch- und Wiederherstellungslogik |
| DEC-144 | Implemented | 2. Synchronisationsengine und 3-Wege-Abgleich |
| DEC-145 | Implemented | 8. Lösch- und Wiederherstellungslogik |
| DEC-146 | Implemented | 8. Lösch- und Wiederherstellungslogik |
| DEC-147 | Implemented | 8. Lösch- und Wiederherstellungslogik |
| DEC-148 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-149 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-150 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-151 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-152 | Implemented | 9. Android und Hintergrundprozesse |
| DEC-153 | Implemented | 12. Performance und Optimierungen |
| DEC-154 | Implemented | 15. Build, Tests, Versionierung und Arbeitsregeln |
| DEC-155 | Implemented | 11. Lokalisierung und Sprache |
| DEC-156 | Implemented | 11. Lokalisierung und Sprache |
| DEC-157 | Implemented | 11. Lokalisierung und Sprache |
| DEC-158 | Implemented | 11. Lokalisierung und Sprache |
| DEC-159 | Implemented | 6. Datenbank und Datenmodell |
| DEC-160 | Implemented | 11. Lokalisierung und Sprache |
| DEC-161 | Implemented | 5. Foto-Sortierung (Bildordner) |
| DEC-162 | Implemented | 6. Datenbank und Datenmodell |
| DEC-163 | Implemented | 8. Lösch- und Wiederherstellungslogik |
| DEC-164 | Implemented | 3. Konflikterkennung und Konfliktbehandlung |
| DEC-165 | Superseded | 16. Verworfene und überholte Lösungsansätze |
| DEC-166 | Implemented | 11. Lokalisierung und Sprache |
| DEC-167 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-168 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-169 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-170 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-171 | Implemented | 7. Datenintegrität und Duplikaterkennung |
| DEC-172 | Implemented | 6. Datenbank und Datenmodell |
| DEC-173 | Implemented | 6. Datenbank und Datenmodell |
| DEC-174 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-175 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-176 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-177 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-178 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-179 | Implemented | 1. Architektur und Projektstruktur |
| DEC-180 | Implemented | 15. Build, Tests, Versionierung und Arbeitsregeln |
| DEC-181 | Implemented | 1. Architektur und Projektstruktur |
| DEC-182 | Implemented | 12. Performance und Optimierungen |
| DEC-183 | Implemented | 9. Android und Hintergrundprozesse |
| DEC-184 | Implemented | 15. Build, Tests, Versionierung und Arbeitsregeln |
| DEC-185 | Accepted | 15. Build, Tests, Versionierung und Arbeitsregeln |
| DEC-186 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-187 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-188 | Accepted | 15. Build, Tests, Versionierung und Arbeitsregeln |
| DEC-189 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-190 | Implemented | 9. Android und Hintergrundprozesse |
| DEC-191 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-192 | Implemented | 4. Dateiübertragung und SMB |
| DEC-193 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-194 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-195 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-196 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-197 | Implemented | 14. Verteilung: Updates, Play Store und Support |
| DEC-198 | Rejected | 16. Verworfene und überholte Lösungsansätze |
| DEC-199 | Rejected | 16. Verworfene und überholte Lösungsansätze |
| DEC-200 | Proposed | 13. Sicherheit, Datenschutz und Diagnose |
| DEC-201 | Accepted | 2. Synchronisationsengine und 3-Wege-Abgleich |
| DEC-202 | Implemented | 6. Datenbank und Datenmodell |
| DEC-203 | Implemented | 13. Sicherheit, Datenschutz und Diagnose |
| DEC-204 | Implemented | 9. Android und Hintergrundprozesse |
| DEC-205 | Implemented | 9. Android und Hintergrundprozesse |
| DEC-206 | Accepted | 10. Benutzeroberfläche und UX |
| DEC-207 | Accepted | 10. Benutzeroberfläche und UX |
| DEC-208 | Accepted | 9. Android und Hintergrundprozesse |
| DEC-209 | Implemented | 5. Foto-Sortierung (Bildordner) |
| DEC-210 | Accepted | 15. Build, Tests, Versionierung und Arbeitsregeln |
| DEC-211 | Accepted | 15. Build, Tests, Versionierung und Arbeitsregeln |
| DEC-212 | Accepted | 15. Build, Tests, Versionierung und Arbeitsregeln |
| DEC-213 | Accepted | 15. Build, Tests, Versionierung und Arbeitsregeln |
| DEC-214 | Accepted | 15. Build, Tests, Versionierung und Arbeitsregeln |
| DEC-220 | Accepted | 15. Build, Tests, Versionierung und Arbeitsregeln |
| DEC-221 | Implemented | 9. Android und Hintergrundprozesse |
| DEC-222 | Implemented | 13. Sicherheit, Datenschutz und Diagnose |
| DEC-223 | Implemented | 8. Lösch- und Wiederherstellungslogik |
| DEC-224 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-225 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-226 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-227 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-228 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-229 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-230 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-215 | Accepted | 15. Build, Tests, Versionierung und Arbeitsregeln |
| DEC-216 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-217 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-218 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-219 | Implemented | 7. Datenintegrität und Duplikaterkennung |
| DEC-231 | Implemented | 8. Lösch- und Wiederherstellungslogik |
| DEC-232 | Implemented | 3. Konflikterkennung und Konfliktbehandlung |
| DEC-233 | Implemented | 9. Android und Hintergrundprozesse |
| DEC-234 | Implemented | 4. Dateiübertragung und SMB |
| DEC-235 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-236 | Implemented | 9. Android und Hintergrundprozesse |
| DEC-237 | Implemented | 9. Android und Hintergrundprozesse |
| DEC-238 | Implemented | 2. Synchronisationsengine und 3-Wege-Abgleich |
| DEC-239 | Implemented | 3. Konflikterkennung und Konfliktbehandlung |
| DEC-240 | Accepted | 15. Build, Tests, Versionierung und Arbeitsregeln |
| DEC-241 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-242 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-243 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-244 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-245 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-246 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-247 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-248 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-249 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-250 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-251 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-252 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-253 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-254 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-255 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-256 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-257 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-258 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-259 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-260 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-261 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-262 | Accepted | 10. Benutzeroberfläche und UX |
| DEC-263 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-264 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-265 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-266 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-267 | Implemented | 10. Benutzeroberfläche und UX |
| DEC-268 | Accepted | 10. Benutzeroberfläche und UX |
| DEC-269 | Implemented | 10. Benutzeroberfläche und UX |

---

## 1. Architektur und Projektstruktur

### DEC-001: Native Android-App mit Kotlin und Jetpack Compose
- **Status:** Implemented
- **Datum:** 2026-11-08 (Entscheidung 2026-11-01)
- **Kategorie:** Architektur
- **Entscheidung:** Native Android-App in Kotlin mit Jetpack-Compose-Oberfläche; minSdk 29 (Android 10), targetSdk/compileSdk 35; App-Sprache nur Deutsch.
- **Begründung:** Der Inhaber hat Android ≥ 10 und „nur Deutsch“ ausdrücklich bestätigt. Hintergrundbetrieb, MediaStore und WLAN-Erkennung erfordern native APIs.
- **Alternativen:** keine diskutiert.
- **Konsequenzen:** Scoped Storage ab Android 10 bestimmt den Dateizugriff; Android-12+-Einschränkungen für Vordergrunddienste müssen behandelt werden.
- **Aktualisierung (Session 4):** „App-Sprache nur Deutsch“ gilt nicht mehr: Seit 0.8.0 ist die App zweisprachig, Englisch als Grundsprache (DEC-155). Der übrige Inhalt bleibt gültig.
- **Aktualisierung (Session 5):** targetSdk und compileSdk sind seit 0.9.5 auf 36 (Android 16), DEC-183.
- **Betroffene Komponenten:** `app/build.gradle.kts`, `ui/*`
- **Abhängigkeiten:** DEC-030, DEC-043

### DEC-005: Profile mit eigenem Ziel, Zugangsdaten, Quellen und WLANs
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Architektur
- **Entscheidung:** Mehrere Profile. Jedes hat einen eigenen Share-Pfad, eigene Zugangsdaten, Quellordner, erlaubte WLANs und eigene Optionen. Global bleiben: Auto-Sync, Mindestalter, Heimatland, „nur beim Laden“, parallele Uploads.
- **Begründung:** Vorschlag 9 (Profile) vom Inhaber angenommen.
- **Alternativen:** Ein Einzelprofil wurde verworfen.
- **Konsequenzen:** Duplikat-, Lösch- und Abgleichslogik arbeiten pro Profil. Löschen nach Sync berücksichtigt alle Profile (DEC-025).
- **Betroffene Komponenten:** `Profile`, `ui/ProfileScreen.kt`, `ui/HomeScreen.kt`
- **Abhängigkeiten:** DEC-025, DEC-033

### DEC-008: Sichtbarer Name LeoSync, Repo bleibt Sync2PC
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Sonstiges
- **Entscheidung:** App-Name, Texte, Benachrichtigungen und APK-Name (`LeoSync-<version>.apk`) heissen „LeoSync“ (Teil der „Leo“-Tool-Familie). Das GitHub-Repo `Apop85/Sync2PC`, der Projektordner und interne Kennungen bleiben.
- **Begründung:** Entscheidung des Inhabers; die Repo-Umbenennung hat er ausdrücklich abgelehnt.
- **Alternativen:** Repo in `Apop85/LeoSync` umbenennen (abgelehnt); App-ID ändern (abgelehnt, DEC-007).
- **Konsequenzen:** Der Papierkorb heisst neu `.LeoSync-Papierkorb`; der alte `.Sync2PC-Papierkorb` bleibt dauerhaft vom Sync ausgeschlossen.
- **Betroffene Komponenten:** `strings.xml`, `themes.xml`, `Notifications.kt`, `MirrorEngine.TRASH_DIR/OLD_TRASH_DIR`, `FileFilter`, `tools/build.ps1`
- **Abhängigkeiten:** DEC-007, DEC-036

### DEC-009: Lizenz MIT
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Sonstiges
- **Entscheidung:** Open Source unter MIT, Copyright „LeoSync contributors“.
- **Begründung:** Antwort des Inhabers auf die Rückfrage.
- **Alternativen:** GPL-3.0 wurde genannt und nicht gewählt.
- **Konsequenzen:** Die Natural-Earth-Daten (Public Domain) sind damit vereinbar.
- **Betroffene Komponenten:** `LICENSE`, `README.md`
- **Abhängigkeiten:** —

### DEC-033: Zwei Profiltypen mit Assistent
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Architektur
- **Entscheidung:**
  - Profiltypen: PHOTOS (Bildordner, sortiert) und MIRROR (normaler Ordner, 1:1 mit Unterordnern).
  - Beim Anlegen fragt ein Assistent nach dem Typ.
  - Gemeinsame Schnittstelle `SyncRunner` für `SyncEngine` und `MirrorEngine`.
  - Ein Bildordner-Profil darf mehrere Quellordner haben, ein Ordner-Profil genau ein Paar.
- **Begründung:** Wunsch „Jahresordner nur bei Bildordnern“; Antworten 4 und 5; Vorschlag 1 (Assistent) angenommen.
- **Alternativen:** mehrere Paare pro Ordner-Profil (abgelehnt).
- **Konsequenzen:** Statistik und Löschen nach Sync gelten nur für Bildordner.
- **Betroffene Komponenten:** `ProfileType`, `sync/SyncRunner.kt`, `mirror/MirrorEngine.kt`, `ui/ProfileParts.kt`
- **Abhängigkeiten:** DEC-034, DEC-035

### DEC-179: Lauf-Zustände als Konstanten statt freier Texte
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-04)
- **Kategorie:** Architektur
- **Entscheidung:** `object RunStatus` mit `const val` (`RUNNING`, `OK`, `PARTIAL`, `FAILED`, `CANCELLED`, `INTERRUPTED`, `WAITING`) ersetzt die Textliterale im Kotlin-Code.
- **Begründung:** Befund des Code-Reviews (Tippfehler fielen erst zur Laufzeit auf).
- **Alternativen:** Enum (nicht gewählt: die DB speichert Text, eine Enum hätte Konvertierungen bzw. eine Migration gebraucht).
- **Konsequenzen:** In der DB und in SQL-Abfragen stehen weiterhin dieselben Texte; beide müssen übereinstimmen.
- **Betroffene Komponenten:** `data/Entities.kt`, `RunGrouping`, `SyncDao`, `MirrorEngine`, `SyncEngine`, `SyncWorker`, `HomeScreen`, `LogScreen`
- **Abhängigkeiten:** DEC-086

### DEC-181: Grosse Bildschirme in Teile aufgeteilt
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-04)
- **Kategorie:** Architektur
- **Entscheidung:** Karten und Dialoge der Startseite liegen in `ui/HomeParts.kt` (`internal`); die Optionen-Karten des Profils in `ui/ProfileCards.kt`. Diese erhalten den Profil-Zustand als `MutableState`, damit ihr Code unverändert verschoben werden konnte.
- **Begründung:** Befund der Prüfung technischer Schulden (Dateien mit 750–800 Zeilen).
- **Alternativen:** Umbau mit Zustands-Callbacks (nicht gewählt: höheres Risiko ohne Compiler).
- **Konsequenzen:** `ProfileScreen.kt` ist weiterhin gross (ca. 750 Zeilen); `MirrorEngine` bewusst nicht umgebaut (nur mit Tests).
- **Betroffene Komponenten:** `ui/HomeScreen.kt`, `ui/HomeParts.kt`, `ui/ProfileScreen.kt`, `ui/ProfileCards.kt`
- **Abhängigkeiten:** —

---

## 2. Synchronisationsengine und 3-Wege-Abgleich

### DEC-034: Sync-Richtung pro Profil, Bildordner nur Handy → PC
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Synchronisation
- **Entscheidung:** Ordner-Profile wählen UPLOAD, DOWNLOAD oder BOTH. Bildordner sind fest auf UPLOAD (`Profile.dir()`).
- **Begründung:** Antworten 1 und 4.
- **Alternativen:** beidseitige Bildordner (abgelehnt, DEC-071).
- **Konsequenzen:** Die Abgleichsregeln hängen von der Richtung ab (DEC-035).
- **Betroffene Komponenten:** `SyncDirection`, `Profile.dir()`
- **Abhängigkeiten:** DEC-035

### DEC-035: 3-Wege-Abgleich zwischen Handy, PC und letztem Stand
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Synchronisation
- **Entscheidung:** Pro Datei werden der Handy-Stand (L), der PC-Stand (R) und der letzte gemeinsame Stand (S aus `mirror_state`) verglichen:
  - Nur eine Seite geändert oder neu: in Sync-Richtung kopieren.
  - Beide geändert oder erstmals beide vorhanden: vollständig gleich, dann nur den Stand merken; sonst ein Konflikt (bei Einweg-Richtung gewinnt die Quelle, die Gegenseite wandert in den Papierkorb).
  - Gelöscht: gemäss DEC-036.
  - Schlüssel ist der relative Pfad in Kleinbuchstaben.
- **Begründung:** Erkennung von Änderungen, Löschungen und Konflikten ohne Benutzereingriff.
- **Alternativen:** einfacher Zeitstempel-Vergleich (nicht diskutiert).
- **Konsequenzen:** Lokal kann per SAF kein Änderungsdatum gesetzt werden; deshalb speichert die App die tatsächlich gelesenen Werte. Der Ordner-Sync läuft sequenziell.
- **Betroffene Komponenten:** `MirrorEngine.plan/execute`, `MirrorState`, `SyncDao`
- **Abhängigkeiten:** DEC-036, DEC-037, DEC-038

### DEC-040: Einschluss- und Ausschlussfilter (White- und Blacklist)
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Synchronisation
- **Entscheidung:**
  - Glob-Muster (`*`, `?`, `**`); Muster mit `/` gelten für den Pfad, sonst für den Namen.
  - Die Ausschlussliste hat Vorrang.
  - `*.s2ptmp` und die Papierkörbe sind immer ausgeschlossen.
  - Gilt für beide Profiltypen.
- **Begründung:** Vorschlag 2 „ja white und blacklist integrieren“.
- **Alternativen:** —
- **Konsequenzen:** In Kotlin-Blockkommentaren darf `/**` nicht vorkommen (Build-Fehler „Unclosed comment“).
- **Aktualisierung (Session 5):** In der Oberfläche heissen die Filter seit 0.9.0 „Nur diese Dateien“ und „Diese Dateien ausschliessen“ (keine Begriffe Whitelist/Blacklist, DEC-169). Die Logik ist unverändert.
- **Betroffene Komponenten:** `mirror/FileFilter.kt`, `SyncEngine`, `MirrorEngine`
- **Abhängigkeiten:** DEC-169

### DEC-103: Neue Profile starten erst nach dem ersten manuellen Sync
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-02)
- **Kategorie:** Synchronisation
- **Entscheidung:** Nach dem Anlegen fragt die App „Simulieren / Synchronisieren / Später“. Bis zum ersten manuellen Lauf (Status ≠ FAILED) lässt der Worker das Profil bei automatischen Läufen aus; die Profilkarte zeigt „Wartet auf den ersten Start“. Die Markierung wird vor `setupAll` gesetzt. Importierte Profile sind ausgenommen.
- **Begründung:** Hinweis des Inhabers: Nach dem Einrichten startete sofort die Übertragung, Testen war nicht möglich (Ursache: der WLAN-Callback meldet ein bereits verbundenes Netz sofort).
- **Alternativen:** Gilt auch für Importe (vom Inhaber abgelehnt).
- **Konsequenzen:** Speicherung ohne DB-Migration in `GlobalSettings` (StringSet).
- **Betroffene Komponenten:** `GlobalSettings.awaitingFirstStart`, `ui/ProfileScreen.kt`, `SyncWorker`, `HomeScreen.ProfileCard`
- **Abhängigkeiten:** DEC-084, DEC-104, DEC-105

### DEC-135: Importierte Ordner-Profile warten auf den ersten Start und eine Stichprobe
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-02)
- **Kategorie:** Synchronisation
- **Entscheidung:** Importierte Ordner-Profile werden als „wartet auf den ersten Start“ markiert; mit importiertem Sync-Stand steht auf beiden Seiten eine Stichprobe aus. „Ersetzen“ verwirft Schutzbremsen, Wiederherstellungen und Leerungen der alten Profile.
- **Begründung:** Nach Import zeigt die SAF-Freigabe evtl. auf einen anderen Ordner; ohne Prüfung könnte der Sync alles als gelöscht werten.
- **Alternativen:** Ausnahme für Importe wie in DEC-103 (hier bewusst aufgehoben für Ordner-Profile).
- **Konsequenzen:** —
- **Betroffene Komponenten:** `backup/BackupManager.kt`
- **Abhängigkeiten:** DEC-089, DEC-103, DEC-129

### DEC-144: Sperre zwischen Ordner-Sync und Eingriffen von Hand
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-03)
- **Kategorie:** Architektur
- **Entscheidung:** Ein prozessweiter Mutex (`MirrorLock`) wird von jedem echten Ordner-Sync gehalten. Aufräumen, Endgültig löschen und Speicher freigeben (Ordner) warten darauf; die Ansicht zeigt „Wartet, bis der laufende Sync fertig ist…“. Die Simulation sperrt nicht.
- **Begründung:** Logikprüfung: „Alles leeren“ während eines Syncs hätte eine gerade ersetzte Version sofort mitgelöscht.
- **Alternativen:** Eingriff vormerken und vom Sync ausführen lassen (genannt, nicht gewählt).
- **Konsequenzen:** Verlässt man die Ansicht während des Wartens, unterbleibt der Eingriff. *(Wird durch DEC-201 abgelöst, sobald umgesetzt: der Eingriff wird dann prozessweit vorgemerkt und läuft nach dem Sync; bis dahin gilt der Satz.)*
- **Betroffene Komponenten:** `mirror/MirrorLock.kt`, `MirrorEngine.run`, `TrashCleaner`, `FolderFreer`, `ui/TrashScreen.kt`, `ui/FreeSpaceScreen.kt`
- **Abhängigkeiten:** DEC-140, DEC-147, DEC-201

### DEC-201: Eingriffe beim laufenden Ordner-Sync prozessweit vormerken, nur bestätigter Stand wird gelöscht
- **Status:** Accepted *(vom Inhaber am 2026-11-08 freigegeben; bis zur Umsetzung durch Development -012 nicht implementiert)*
- **Datum:** 2026-11-08
- **Kategorie:** Architektur
- **Entscheidung:** Aufräumen, Endgültig löschen und Speicher freigeben (Ordner) werden bei laufendem Sync nicht dauerhaft vorgemerkt, sondern von einem prozessweiten Objekt gehalten (Muster `PendingProfileDelete`, `SupervisorJob`), das die bestehenden Funktionen startet; Verlassen der Ansicht bricht nicht ab.
  - (a) Keine Prefs, keine Datenbank. Eine verlorene Vormerkung bedeutet: nichts wird gelöscht (sichere Richtung).
  - (b) Ausgeführt wird immer nur der bestätigte Stand: Aufräumen übergibt die betroffenen Einträge (Seite, Pfad, Grösse). Ein Datumsordner wird nur ganz gelöscht, wenn sein aktueller Inhalt vollständig darin enthalten ist, sonst nur die bestätigten Dateien. Übersprungen werden Einträge mit geänderter Grösse oder inzwischen zum Wiederherstellen vorgemerkte (Prüfung bei der Ausführung). Ist der PC beim Einlesen unbekannt, gilt die Regel in `PurgeStore` wie bisher. Endgültig löschen und Speicher freigeben bleiben unverändert; ihre Auswahlkriterien werden nicht neu ausgewertet.
  - (c) Bei Neustart oder Prozessende entfällt die Vormerkung, nichts wird gelöscht. Das Ergebnis erscheint als Benachrichtigung (bestehender Hinweis-Schalter) bzw. als Meldung in der offenen Ansicht.
  - (d) Der vorgemerkte Eingriff führt sich selbst aus, sobald er `MirrorLock` erhält; keine Kopplung an `SyncWorker`/`MirrorEngine`, kein eigener Worker (10-Minuten-Grenze beim Warten, Sperre gilt nur im Prozess).
  - (e) Anzeige „Wird nach dem laufenden Sync ausgeführt“ mit „Abbrechen“ (nur solange wartend) in Papierkorb und Speicher freigeben. Solange ein Eingriff für ein Profil ansteht, sind weitere Lösch-Eingriffe für dasselbe Profil gesperrt. Texte DE/EN.
  - (f) Bricht das WLAN ab, gibt der Lauf die Sperre frei und der Eingriff läuft; PC-Teile gehen in `PurgeStore`, Speicher freigeben meldet „PC nicht erreichbar“. Keine Verklemmung (der Eingriff hält nur `MirrorLock`). Wiederherstellen hat Vorrang vor wartendem Löschen.
- **Begründung:** Befund F1: Die Eingriffe laufen im `rememberCoroutineScope` der Ansicht (`TrashScreen`, `FreeSpaceScreen`) und werden beim Verlassen abgebrochen. Befund F3 (siehe D47): Aufräumen löscht bei „Alles“ und „Tage“ mit heutigem Tag mehr als angezeigt. Befund F4: „Endgültig löschen“ (konkrete Pfade) und `FolderFreer.verifyAndDelete` (volle Neuprüfung) sind robust.
- **Alternativen:** Dauerhaft in Prefs oder Datenbank vormerken (dauerhafte Löschabsichten, Haken im Worker, Migration); eigener Worker; Ausführung in `MirrorEngine` am Laufende (alle verworfen).
- **Konsequenzen:** Löst die Konsequenz „Verlässt man die Ansicht … unterbleibt der Eingriff“ aus DEC-144 ab, sobald umgesetzt. Restrisiken: Beendet ein Hersteller-Killer den Prozess während des Wartens, unterbleibt der Eingriff still (D14); die Sperre bleibt global (D29). Kritische Änderung: Review und QA nach der Umsetzung.
- **Nachtrag 0.12.3 (Development -039, im Code gelesen 2026-11-09):** `Notifications.interventionDone` liefert `false`, wenn die Hinweise für LeoSync im System (`areNotificationsEnabled`) oder der Kanal `CH_ALERTS` (Wichtigkeit NONE) ausgeschaltet sind. Das Ergebnis bleibt dann als DONE in der App stehen, bis die Ansicht geöffnet wird. Restrisiko unverändert: DONE nur im Speicher.
- **Betroffene Komponenten:** `mirror/MirrorLock.kt`, `TrashCleaner`, `TrashPurge`, `PurgeStore`, `FolderFreer`, `ui/TrashScreen.kt`, `ui/FreeSpaceScreen.kt`
- **Abhängigkeiten:** DEC-140, DEC-144, DEC-147, DEC-196 (Muster `PendingProfileDelete`)

### DEC-238: Während des Laufs geänderte Dateien: Zähler `deferred` und begrenzter Folgelauf
- **Status:** Implemented (0.16.0 laut Development, SYNC-20261111-173; Startseiten-Anzeige 0.16.1, DEC-241; QA-/UX-Prüfung offen)
- **Datum:** 2026-11-11 (Quelle SYNC-20261111-167, -173; Benutzerfreigabe erteilt, Chat Architecture)
- **Kategorie:** Sync
- **Entscheidung:** Dateien, die während des Laufs auf Handy oder PC geändert wurden (`skipChanged`), werden im Lauf gezählt (`SyncRun.deferred`, DB 11, Migration 10 → 11). Der Lauf bleibt `OK`; die Zusammenfassung nennt «· N folgen später», die Startseite zeigt nicht «aktuell». Nach einem echten Lauf mit `deferred > 0` plant `SyncWorker` einen Folgelauf nach 5 Minuten (eigener Auftragsname `sync-skip-followup`, Bedingungen wie automatische Läufe), höchstens einen pro Stunde (`lastSkipFollowUpAt`); danach gelten die regulären Auslöser. Pause und «nur manuell» werden beachtet.
- **Begründung:** Vorher wurde nur eine Warnung geloggt, es gab keinen Folgelauf, und die Startseite zeigte «Alles gesichert und aktuell»; ohne Ordnerprüfung holte erst das Sicherheitsnetz (6 h) nach (SYNC-20261111-173, Technischer Kontext). Die Begrenzung verhindert Endlosschleifen bei dauernd geänderten Dateien.
- **Alternativen:** Wiederverwendung von `skipped` (verworfen: bei Fotoprofilen bedeutet es «bereits vorhanden/unverändert»); Statusmerker in den Einstellungen statt in der Datenbank (verworfen).
- **Konsequenzen:** DB-Version 11; neuer Auftrag in `ALL_SYNC_WORK`; Details `docs/SYNC-ENGINE.md` 6.7, `docs/DATA-MODEL.md` 3.9.
- **Betroffene Komponenten:** `mirror/MirrorEngine.kt`, `data/Entities.kt` (`SyncRun`), `data/AppDatabase.kt`, `sync/SyncWorker.kt`, `sync/SyncScheduler.kt`, `sync/RunSummary.kt`, `data/GlobalSettings.kt`
- **Abhängigkeiten:** DEC-233, DEC-241

---

## 3. Konflikterkennung und Konfliktbehandlung

### DEC-037: Konflikte fragen, optional pro Profil „immer neuere Datei“
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Synchronisation
- **Entscheidung:**
  - Standard ist eine Rückfrage auf einer eigenen Seite: Handy-Version, PC-Version oder beide behalten. Dazu gibt es „Alle: neuere“.
  - Pro Profil wählbar: „Immer die neuere Datei übernehmen“.
  - Die verworfene Version wandert in den Papierkorb.
  - „Beide behalten“ benennt die Handy-Datei in `*_Konflikt-Handy.*` um.
  - Die Entscheidung wird beim nächsten Lauf angewendet.
- **Begründung:** Antwort 3 des Inhabers.
- **Alternativen:** immer beide behalten (vorgeschlagen, nicht als Standard gewählt).
- **Konsequenzen:** Offene Konflikte blockieren nur die betroffenen Dateien.
- **Aktualisierung (Session 4):** „Beide behalten“ benennt seit 0.8.0 in `*_conflict-phone.*` um statt `*_Konflikt-Handy.*` (DEC-164).
- **Betroffene Komponenten:** `ConflictMode`, `MirrorConflict`, `ConflictResolution`, `ui/ConflictScreen.kt`
- **Abhängigkeiten:** DEC-035

### DEC-133: Nichts überschreiben, was sich während des Laufs geändert hat
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-02)
- **Kategorie:** Synchronisation
- **Entscheidung:** Vor dem Ersetzen prüft die App, ob die Zielseite noch dem eingelesenen Stand entspricht; sonst wird die Datei übersprungen („folgt beim nächsten Sync“). Ein Download, dessen Ziel inzwischen neu entstanden ist, bricht ab (`ChangedDuringRunException`). Vor einer Übertragung mit Papierkorb der Gegenseite wird der Sync-Stand zuerst gelöscht, damit ein Fehlschlag nicht als Löschung gilt.
- **Begründung:** Massnahmen aus der Datenverlust-Prüfung.
- **Alternativen:** —
- **Konsequenzen:** —
- **Betroffene Komponenten:** `MirrorEngine.executeTransfer/unchanged/skipChanged`, `LocalTree.writeFileResumable(expectNew)`
- **Abhängigkeiten:** DEC-035, DEC-137

### DEC-164: Konflikt-Zusatz `_conflict-phone`
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-03)
- **Kategorie:** Synchronisation
- **Entscheidung:** „Beide Versionen behalten“ benennt die Handy-Datei in `name_conflict-phone.ext` um (`MirrorEngine.CONFLICT_SUFFIX`), sprachunabhängig.
- **Begründung:** Gleicher Entscheid wie DEC-163 (englische Standardnamen, da noch kaum genutzt).
- **Alternativen:** `_Konflikt-Handy` beibehalten.
- **Konsequenzen:** Bereits umbenannte `_Konflikt-Handy`-Dateien bleiben normale Dateien.
- **Betroffene Komponenten:** `mirror/MirrorEngine.kt`
- **Abhängigkeiten:** DEC-037

### DEC-232: Konflikt-Rückgängig und Anwenden schliessen sich über bedingte DB-Abfragen aus
- **Status:** Implemented (0.16.0 laut Development, SYNC-20261111-172; QA-Prüfung offen, SYNC-20261111-178)
- **Datum:** 2026-11-11 (Quelle SYNC-20261111-156 Punkt 3, -172; Benutzerfreigabe erteilt, Chat Architecture)
- **Kategorie:** Sync / Konflikte
- **Entscheidung:** Eine Konfliktentscheidung wird über `undoResolution(profileId, relPath, expected)` nur rückgängig gemacht, wenn die Entscheidung unverändert ist; die Engine löscht die Zeile für Aktionen aus einer Entscheidung über `deleteConflictIf(…, expected)` und führt die Aktion nicht aus, wenn das 0 liefert. Entweder gilt «rückgängig» oder «zu spät». Der Sync startet weiterhin sofort.
- **Begründung:** Zeitfenster: Die Engine plant, der Benutzer drückt Rückgängig (Zeile noch da), die Engine löscht die Zeile unbedingt und führt die Entscheidung trotzdem aus; zusätzlich konnte `upsertConflict` eine gelöschte Zeile mit altem Stand wiederbeleben (SYNC-20261111-172, Technischer Kontext).
- **Alternativen:** Start erst nach Ablauf der Rückgängig-Frist (verworfen: Aufwand, und der Start ginge beim Verlassen des Bildschirms verloren).
- **Konsequenzen:** `ConflictScreen` zählt nur Ergebnis 1 als rückgängig gemacht; Konflikte, bei denen eine Seite fehlt, werden weiterhin unbedingt gelöscht. Keine Room-In-Memory-Tests der Abfragen (Gerät nötig).
- **Betroffene Komponenten:** `data/SyncDao.kt`, `ui/ConflictScreen.kt`, `ui/ScreenLogic.kt`, `mirror/MirrorEngine.kt`
- **Abhängigkeiten:** DEC-037

### DEC-239: «Beide behalten»: freier Konfliktname, nie überschreibend, wiederholbar
- **Status:** Implemented (0.16.0 laut Development, SYNC-20261111-171; QA-Prüfung offen, SYNC-20261111-178)
- **Datum:** 2026-11-11 (Quelle SYNC-20261111-168, -171; Benutzerfreigabe erteilt, Chat Architecture)
- **Kategorie:** Sync / Konflikte
- **Entscheidung:** Die Konfliktkopie erhält einen auf Handy und PC freien Namen (`_conflict-phone`, `-2`, `-3` … bis 100, danach Fehler ohne Änderung). Nach dem Umbenennen wird der vom Anbieter vergebene Name geprüft (Abweichung: keine Übertragung, nächster Lauf überträgt als neu). Der Upload überschreibt nie (`replaceExisting = false`). Das Umbenennen geschieht einmal je Lauf (Merker `renamedByRun`/`uploadedByRun`), die Wiederholung nach Verbindungsabbruch setzt fort.
- **Begründung:** Vorher konnte eine vorhandene Konfliktkopie (der Speicher-Anbieter hängt « (1)» an) oder eine gleichnamige PC-Datei ohne Papierkorb überschrieben werden; die Wiederholung nach Abbruch scheiterte, weil die alte Dokument-ID nach dem Umbenennen ungültig war (SYNC-20261111-171, Technischer Kontext).
- **Alternativen:** — (in der Quelle nicht genannt).
- **Konsequenzen:** Details `docs/SYNC-ENGINE.md` 5.6. Die Engine-Fehlerpfade (Umbenennen, Anbieter-Name, Wiederholung) sind nur am Gerät prüfbar.
- **Betroffene Komponenten:** `mirror/MirrorEngine.kt` (`executeTransfer`, `transferWorker`), `mirror/ConflictName`
- **Abhängigkeiten:** DEC-037, DEC-232

---

## 4. Dateiübertragung und SMB

### DEC-002: SMB-Zugriff über die Bibliothek smbj mit Benutzer und Passwort
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Architektur
- **Entscheidung:** SMB2/SMB3 über smbj (0.13.0); Anmeldung mit Benutzer, Passwort und optionaler Domäne; Ziel ist eine Windows-11-Ordnerfreigabe, angesprochen über die IP-Adresse (kein `.local`).
- **Begründung:** Der Inhaber nutzt die Windows-Ordnerfreigabe mit Benutzer und Passwort und kennt die SMB-Version nicht. smbj verhandelt die Version selbst. `.local`-Namen löst Android nicht zuverlässig auf.
- **Alternativen:** Gastzugang wurde gefragt und nicht gewählt.
- **Konsequenzen:** smbj nutzt Reflection, was die Keep-Regeln für R8 nötig macht (DEC-014). Windows trennt unbenutzte Verbindungen (DEC-053).
- **Betroffene Komponenten:** `smb/SmbSession.kt`, `SmbTarget`
- **Abhängigkeiten:** DEC-014, DEC-053

### DEC-023: Upload über Temp-Datei mit Fortsetzen ab Abbruchstelle
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Synchronisation
- **Entscheidung:** Der Upload schreibt zuerst nach `<name>.s2ptmp` und benennt danach um. Eine angefangene Temp-Datei wird ab ihrer Grösse fortgesetzt. Temp-Dateien werden von Ordnerlisten und Filtern ausgeschlossen.
- **Begründung:** Vorschlag 3 („ja unbedingt“): keine halben Dateien auf dem Share.
- **Alternativen:** —
- **Konsequenzen:** Liegengebliebene `.s2ptmp`-Dateien können auf dem Share bleiben, wenn sich der Name ändert.
- **Betroffene Komponenten:** `SmbSession.upload`, `LocalTree.writeFile`
- **Abhängigkeiten:** DEC-053

### DEC-028: Ursprüngliches Änderungsdatum auf dem PC erhalten
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Synchronisation
- **Entscheidung:** Erstell- und Änderungsdatum der Zieldatei werden vor dem Umbenennen auf das Änderungsdatum der Quelle gesetzt. Fehler dabei brechen den Upload nicht ab.
- **Begründung:** Antwort „ja“ auf die Rückfrage.
- **Alternativen:** Kopierzeitpunkt (verworfen).
- **Konsequenzen:** Beim Ordner-Sync liest die App die tatsächlichen Werte nach dem Upload zurück.
- **Betroffene Komponenten:** `SmbSession.upload(modifiedMillis)`
- **Abhängigkeiten:** DEC-023

### DEC-041: Speicherplatzprüfung vor Downloads
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Android
- **Entscheidung:** Downloads, die die Reserve von 300 MB unterschreiten würden, werden übersprungen und gemeldet.
- **Begründung:** Vorschlag 3 angenommen.
- **Alternativen:** —
- **Konsequenzen:** Der freie Speicher wird über den Hauptspeicher (StatFs) ermittelt; für SD-Karten ist das ungenau.
- **Betroffene Komponenten:** `MirrorEngine.freeSpace`
- **Abhängigkeiten:** DEC-035

### DEC-053: Verbindungsabbrüche zählen nicht als Fehler, Verbindung wird neu aufgebaut
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Synchronisation
- **Entscheidung:**
  - Netzwerkfehler (`isNetworkError`) zählen nicht als Dateifehler.
  - Verbindungen werden vor der Nutzung auf Gültigkeit geprüft (`isAlive`) und bei Bedarf neu aufgebaut; das betrifft auch die Nachschlage-Verbindung des Produzenten.
  - Ein Worker wiederholt eine Datei einmal mit neuer Verbindung, danach endet der Lauf mit Status `INTERRUPTED`, ohne Benachrichtigung.
  - Fehlertexte werden gekürzt (`shortError`).
- **Begründung:** Feldtest mit 2943 Fehlern „DiskShare has already been closed“ (Windows-Leerlauftrennung) und „Software caused connection abort“; Wunsch des Inhabers, erwartbare Abbrüche abzufangen.
- **Alternativen:** Lauf bei jedem Fehler abbrechen bzw. alles als Fehler zählen (vorher).
- **Konsequenzen:** Abgebrochene Dateien folgen beim nächsten Lauf ab der Abbruchstelle (DEC-023).
- **Betroffene Komponenten:** `sync/Errors.kt`, `SmbSession.isAlive`, `SyncEngine` (Produzent, Worker), `MirrorEngine`, `TargetResolver` (Provider), `ui/LogScreen.kt`
- **Abhängigkeiten:** DEC-023, DEC-029, DEC-048

### DEC-094: Abgebrochene Downloads aufs Handy fortsetzen
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Synchronisation
- **Entscheidung:** Eine vorhandene `.s2ptmp` auf dem Handy wird mit Modus „wa“ weitergeschrieben, wenn sie kleiner als das Original ist und die PC-Datei seit dem letzten Schreiben nicht geändert wurde; bei unbekannten Zeitstempeln wird neu begonnen. Danach Grössenprüfung; stimmt sie nicht (Anbieter ignoriert Anhängen), wird die Temp-Datei verworfen und der nächste Lauf beginnt neu.
- **Begründung:** Antwort 6: „ja“; grosse Dateien und WLAN-Abbrüche.
- **Alternativen:** immer neu beginnen (bisher).
- **Konsequenzen:** Abhängig vom SAF-Anbieter; der Selbstheilungsmechanismus fängt Anbieter ohne „wa“ ab.
- **Betroffene Komponenten:** `LocalTree.writeFileResumable`, `TransferPlan.resumeOffset`, `SmbSession.download(startOffset)`
- **Abhängigkeiten:** DEC-023, DEC-093

### DEC-131: Upload nur mit Hash-Prüfung fortsetzen
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-02)
- **Kategorie:** Synchronisation
- **Entscheidung:** Eine angefangene `.s2ptmp` auf dem PC wird nur fortgesetzt, wenn ein erwarteter Hash der Handy-Datei vorliegt; vor dem Umbenennen wird er geprüft, sonst wird die Temp-Datei verworfen.
- **Begründung:** Massnahme 7: Eine Temp-Datei aus einem anderen Stand der Datei darf nicht ergänzt werden.
- **Alternativen:** Fortsetzen nur nach Grösse (bisher).
- **Konsequenzen:** Fotos- und Ordner-Sync übergeben `expectedHash`.
- **Betroffene Komponenten:** `SmbSession.upload(expectedHash)`, `TransferPlan.resumeOffset`, `SyncEngine`, `MirrorEngine.upload`
- **Abhängigkeiten:** DEC-023, DEC-132

### DEC-192: Ziel auf dem PC auswählen – Freigaben auflisten
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-08)
- **Kategorie:** Architektur
- **Entscheidung:** Im Profil genügt der PC (`\\192.168.1.20`) mit Zugangsdaten; „Ordner auf dem PC wählen…“ listet die Freigaben (nur Datei-Freigaben, ohne `$`-Freigaben) und darin die Ordner. Das Feld „Unterordner“ entfällt; ein vorhandener Unterordner wird beim Öffnen in den Pfad übernommen (gleiches Ziel). Für das Auflisten wird `com.rapid7.client:dcerpc:0.12.13` (Dienst `srvsvc`) mit der vorhandenen smbj-Verbindung genutzt.
- **Begründung:** Design-Prüfung (Pfad tippen ist Arbeit, die das System übernehmen kann); der Inhaber: Unterordner-Feld „kann somit weg“. smbj allein kann keine Freigaben auflisten.
- **Alternativen:** eigene RPC-Umsetzung (zu aufwendig); Freigabe weiter von Hand tippen (Eintippen bleibt möglich).
- **Konsequenzen:** Zusatzbibliotheken (Guava, Commons IO/Lang); smbj und BouncyCastle aus dcerpc ausgeschlossen; R8-Regeln ergänzt; APK +0,4 MB. Seit 0.11.3 kann die Auswahl Ordner anlegen (Nachtrag unten).
- **Betroffene Komponenten:** `smb/SmbSession.kt` (`listShares`, `SmbTarget.hostOf`), `ui/ProfileParts.kt` (`RemoteFolderPicker`), `ui/ProfileScreen.kt`, `app/build.gradle.kts`, `app/proguard-rules.pro`
- **Abhängigkeiten:** DEC-039 (ersetzt), DEC-002, DEC-014
- **Nachtrag 0.11.3 („Ordner anlegen“, Inhaberentscheid 2026-11-08, SYNC-20261108-006):**
  - Zeile „Neuer Ordner“ nur unterhalb einer Freigabe; Freigaben werden nicht angelegt.
  - Namensprüfung lokal (`invalidFolderName`): leer, über 255 Zeichen, `\ / : * ? " < > |`, Steuerzeichen, „.“/„..“, Punkt oder Leerzeichen am Ende, Dublette (ohne Gross-/Kleinschreibung).
  - Anlegen über `SmbSession.ensureDir` auf eigener Verbindung; Erfolg nur, wenn der Ordner danach existiert. Fehler (Rechte, Netz) erscheinen im Dialog, die Eingabe bleibt; nach Erfolg wechselt die Auswahl in den neuen Ordner.
  - Restrisiko: Windows-reservierte Namen (CON, NUL …) werden nicht lokal abgefangen; der Serverfehler wird angezeigt.
  - Quelle: Rückmeldung Development (SYNC-20261108-017), Code von Documentation nicht selbst geprüft.

### DEC-234: Gespeicherter Verschlüsselungswert: fehlend = «Bevorzugt», unbekannt = «Erforderlich»
- **Status:** Implemented (0.16.0 laut Development, SYNC-20261111-172)
- **Datum:** 2026-11-11 (Quelle SYNC-20261111-156 Punkt 4, -172)
- **Kategorie:** SMB / Sicherheit
- **Entscheidung:** `SmbEncryption.parse` liefert bei leerem oder fehlendem Wert `PREFERRED` (wie bisher), bei einem vorhandenen, unbekannten Wert `REQUIRED` (sicherer Ausfall). Gilt für den Datenbankwert und die Sicherung (`BackupCodec`).
- **Begründung:** Sicherer Ausfall (SYNC-20261111-175: «unbekannt → Erforderlich (sicherer Ausfall)»).
- **Alternativen:** — (in der Quelle nicht genannt).
- **Konsequenzen:** Ein unbekannter Wert führt nie zu einer unverschlüsselten Verbindung.
- **Betroffene Komponenten:** `data/Entities.kt` (`SmbEncryption.parse`), `backup/BackupCodec.kt`
- **Abhängigkeiten:** DEC-222

---

## 5. Foto-Sortierung (Bildordner)

### DEC-016: Zielstruktur `Jahr\Monat` bzw. `Jahr\Monat - Land`
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Synchronisation
- **Entscheidung:**
  - Zielordner ist `<Basis>\<JJJJ>\<MM>`; mit GPS `<MM> - <Ländername DE>`.
  - Das Heimatland (Standard CH, einstellbar) bekommt keinen Zusatz.
  - Jedes Land erhält einen eigenen Monatsordner.
  - Ungültige Windows-Zeichen werden ersetzt.
- **Begründung:** Vorgabe des Inhabers; Rückfrage 6: „Jedes Land ein eigener Ordner“.
- **Alternativen:** —
- **Konsequenzen:** Ein Monat kann mehrere Ordner haben; Fotos ohne GPS landen im reinen Monatsordner.
- **Aktualisierung (2026-11-09):** Diese Struktur ist seit 0.14.3 Vorlage V1 (Standard) von drei wählbaren Ordnerstrukturen pro Fotoprofil (DEC-209).
- **Betroffene Komponenten:** `sync/FolderNames.kt`, `sync/TargetResolver.kt`, `GlobalSettings.homeCountryIso`
- **Abhängigkeiten:** DEC-017, DEC-018, DEC-019, DEC-209

### DEC-017: Reihenfolge der Datumsermittlung
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Synchronisation
- **Entscheidung:**
  - Fotos: EXIF (DateTimeOriginal, dann Digitized, dann DateTime).
  - Videos: Container-Datum (UTC, in Lokalzeit umgerechnet).
  - Danach das Datum im Dateinamen (Muster und Unix-ms), zuletzt das Änderungsdatum.
- **Begründung:** Vorgabe des Inhabers; Rückfrage 8: „Nimm das Änderungsdatum“ als letzte Stufe.
- **Alternativen:** Ordner „Unsortiert“ (vorgeschlagen, nicht gewählt).
- **Konsequenzen:** Die Analyse wird in `media_info` zwischengespeichert, damit Dateien nicht erneut geöffnet werden.
- **Betroffene Komponenten:** `media/MediaAnalyzer.kt`, `media/FilenameDateParser.kt`, `MediaInfo`
- **Abhängigkeiten:** DEC-016

### DEC-018: Offline-Ländererkennung mit Natural Earth
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Synchronisation
- **Entscheidung:**
  - Grundlage ist Natural Earth 1:50m (GeoJSON) mit deutschen Namen (NAME_DE).
  - Die Datei wird per Streaming geparst; dann Bounding-Box, Punkt-in-Polygon und als Rückfall das nächste Land innerhalb von ca. 25 km; dazu ein LRU-Cache.
  - Die Datei liegt in den Assets, der Builder lädt sie bei Bedarf.
- **Begründung:** Rückfrage 5: „ja gerne offline“ (Datenschutz, kein Netzzugriff nötig).
- **Alternativen:** Online-Geocoding (verworfen).
- **Konsequenzen:** An Grenzen und Küsten kann die Zuordnung ungenau sein. Die Datei macht die APK grösser.
- **Aktualisierung (Session 4):** Seit 0.8.0 werden statt nur `NAME_DE` die Namen aller App-Sprachen gelesen (`NAME_DE`, `NAME_EN`, Rückfall `NAME`); der Ordnername folgt der App-Sprache (DEC-161).
- **Betroffene Komponenten:** `geo/CountryLookup.kt`, `tools/fetch-geodata.ps1`, `assets/geo/countries.geojson`
- **Abhängigkeiten:** DEC-016, DEC-068

### DEC-019: Umbenannte Monatsordner erkennen und einmal pro Gruppe fragen
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Synchronisation
- **Entscheidung:**
  - Die Monatszahl (01–12) muss im Ordnernamen erhalten bleiben.
  - Namen wie „MM“ oder „MM - <Land>“ gelten als von der App erzeugt und lösen keine Rückfrage aus.
  - Bei umbenannten Kandidaten fragt die App einmal pro Gruppe (Jahr, Monat, Land), mit Vorschaubildern und „Alle übernehmen“.
  - Die Wahl wird als `FolderMapping` gespeichert; das Flag `seen` erkennt einen später verschwundenen Ordner und fragt dann neu.
- **Begründung:** Rückfrage 7: „Frage den User … nicht 1000 Mal klicken“.
- **Alternativen:** automatische Zuordnung (verworfen).
- **Konsequenzen:** Dateien wartender Gruppen werden übersprungen, bis entschieden ist.
- **Aktualisierung (Session 4):** „Von der App erzeugt“ umfasst seit 0.8.0 Ländernamen in allen App-Sprachen; ein bestehender Standardordner in der anderen Sprache wird ohne Rückfrage weiterverwendet (DEC-161).
- **Aktualisierung (2026-11-09):** Rückfrage nur für Ordner mit Monatszahl; bei V2/V3 gilt jeder Ordner ausser „MM“ als umbenannt, bei V3 eine Rückfrage je Monat (DEC-209).
- **Betroffene Komponenten:** `TargetResolver.kt`, `FolderNames.isGeneratedName`, `PendingDecision`, `FolderMapping`, `ui/DecisionScreen.kt`
- **Abhängigkeiten:** DEC-016, DEC-209

### DEC-026: Live-Foto-Paare im selben Ordner halten
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Synchronisation
- **Entscheidung:** Ein Video mit gleichem Basisnamen wie ein Foto im selben Quellordner übernimmt dessen Datum und Land.
- **Begründung:** Vorschlag 5 angenommen.
- **Alternativen:** —
- **Konsequenzen:** Das Video erhält keinen eigenen `media_info`-Eintrag; in der Statistik zählt es als „ohne Standort“.
- **Betroffene Komponenten:** `SyncEngine` (`imageByBase`), `tools/LeoSync-Import.ps1`
- **Abhängigkeiten:** DEC-017

### DEC-027: Mindestalter für Dateien mit Nachhol-Sync
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Synchronisation
- **Entscheidung:** Dateien werden erst ab einem Mindestalter übertragen (Standard 5 Minuten). Zu junge Dateien und während eines Laufs neu entstandene lösen einen verzögerten Nachhol-Sync aus.
- **Begründung:** Vorschlag 10 angenommen (Fotos werden evtl. noch bearbeitet).
- **Alternativen:** —
- **Konsequenzen:** Der Content-Trigger feuert früher, als übertragen wird; deshalb ist ein eigener Follow-up-Work nötig.
- **Betroffene Komponenten:** `GlobalSettings.minFileAgeMinutes`, `SyncScheduler.enqueueFollowUp`, `MediaScanner.hasAddedSince`
- **Abhängigkeiten:** DEC-043

### DEC-068: PC-Import-Skript mit den Regeln der App
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Sonstiges
- **Entscheidung:**
  - `tools/LeoSync-Import.bat`, `.ps1` und `LeoSyncImport.cs` (C# 5 über Add-Type, nur ASCII) liegen im Ordner `tools`.
  - Ablauf: Doppelklick, Ordner im Dialog wählen, Ziel eingeben, Vorschau ansehen; kopiert wird nur nach ausdrücklichem „j“.
  - Gleiche Regeln wie die App: Datum, Ländererkennung über dieselbe GeoJSON, Heimatland, Rückfrage bei umbenannten Ordnern, Duplikatsuche über alle Monatsordner mit gleicher Teil-Prüfsumme, `_2`-Namen.
  - Kopiert wird über `.s2ptmp`, danach geprüft und umbenannt; das Änderungsdatum bleibt erhalten.
- **Begründung:** Wunsch des Inhabers; Antworten: kopieren, im Skript fragen, Doppelklick mit Abfrage; Ablage in `tools`.
- **Alternativen:** reiner Kommandozeilenaufruf (bleibt als Option möglich).
- **Konsequenzen:** Muss im Projektordner liegen (GeoJSON). HEIC-EXIF und GPS sind ohne Zusatz nicht lesbar. Videodaten brauchen `ffprobe`.
- **Aktualisierung (2026-11-09):** Das Skript wird nicht mehr gepflegt und nicht an Benutzer verteilt; die drei Dateien stehen in `.gitignore` und gelangen nicht ins Git (Entscheid des Inhabers, Quelle SYNC-20261109-086 abgebrochen; README-Abschnitt entfernt). Es kennt nur die Ordnerstruktur V1 (DEC-209); nicht gegen Ziele mit V2/V3 verwenden.
- **Betroffene Komponenten:** `tools/LeoSync-Import.ps1`, `tools/LeoSyncImport.cs`, `tools/LeoSync-Import.bat`
- **Abhängigkeiten:** DEC-018, DEC-021, DEC-022, DEC-019, DEC-209

### DEC-161: Länderordner in der App-Sprache, bestehende Ordner aller App-Sprachen weiterverwenden
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-03)
- **Kategorie:** Synchronisation
- **Entscheidung:**
  - Neue Länderordner heissen in der App-Sprache („06 - Egypt“ bzw. „06 - Ägypten“).
  - `TargetResolver` sucht zusätzlich (Schritt 2b) nach dem Standardnamen in jeder anderen App-Sprache und verwendet einen gefundenen Ordner ohne Rückfrage weiter.
  - `allCountryNames` (Erkennung „unveränderter“ Ordner) enthält die Namen aller App-Sprachen.
  - Erkannt werden nur die Sprachen der App (`AppLanguage.SUPPORTED`), nicht alle 25 Sprachen der Geodaten.
- **Begründung:** Vorschlag zu Rückfrage 2, vom Inhaber angenommen; seine Sorge um Mehraufwand bei vielen Sprachen wurde mit „Namen kommen kostenlos aus Natural Earth“ beantwortet („ok“). Die Beschränkung auf App-Sprachen hat der Agent bei der Umsetzung festgelegt und mitgeteilt: Sonst gälte ein selbst umbenannter Ordner wie „06 - Italia“ plötzlich als Standardordner.
- **Alternativen:** Ländernamen immer deutsch; alle Geodaten-Sprachen erkennen (verworfen, siehe oben).
- **Konsequenzen:** Ein Sprachwechsel erzeugt keine doppelten Monatsordner. Ein selbst gewählter Name, der zufällig dem Ländernamen der anderen App-Sprache entspricht, gilt als Standardordner.
- **Betroffene Komponenten:** `geo/CountryLookup.kt` (`Country.names`, `nameFor`, `namesOf`, `allCountryNames`), `sync/TargetResolver.kt` (`namesOfCountry`), `sync/SyncEngine.kt`
- **Abhängigkeiten:** DEC-018, DEC-019, DEC-162

### DEC-209: Ordnerstruktur pro Fotoprofil wählbar (V1 bis V3)
- **Status:** Implemented (App seit 0.14.3, Assistent-Anpassung 0.14.4; Ausnahme: PC-Import-Skript, siehe DEC-068)
- **Datum:** 2026-11-09 (2026-11-09)
- **Kategorie:** Synchronisation
- **Entscheidung:**
  - Pro Fotoprofil wählbar aus fester Liste (`FolderLayout`): **V1** `Jahr\MM[ - Land]` (`MONTH_SUFFIX`, bisher DEC-016, Standard und Wert aller bestehenden Profile), **V2** `Jahr\[Land\]MM` (`COUNTRY_FOLDER`), **V3** `Jahr\MM\[Land]` (`COUNTRY_SUBFOLDER`).
  - Heimatland ohne Länderebene bzw. -zusatz; jedes Land ein eigener Ordner (DEC-016 bleibt). Monat zweistellig. Fotos liegen direkt in der letzten Ebene.
  - Änderbar bis zur ersten Synchronisierung des Profils, danach gesperrt; die Sperre sichert die Datenbankschicht ab (`FolderLayoutGuard` in `SyncDao.updateProfile`), zusätzlich sperrt die Oberfläche die Auswahl.
  - Ein zweites Fotoprofil mit gleichem Ziel und anderer Struktur wird blockiert, bei verschachteltem Ziel gibt es eine Warnung (DEC-092). Im Assistenten gilt bei gleichem Ziel die Vorlage des Eigentümers nur als Anzeige; die eigene Wahl wird getrennt gemerkt und kehrt zurück, wenn das Ziel wechselt (ohne eigene Wahl V1; 0.14.4).
  - Duplikatsuche für alle Vorlagen vorlagenunabhängig auch in Länder- und Unterordnern desselben Monats (`MonthFolderSearch`, DEC-022).
  - Rückfrage bei umbenannten Ordnern nur für Ordner mit Monatszahl (DEC-019); Länderordner werden nur am Standardnamen erkannt. Bei V2/V3 gilt jeder Ordner ausser „MM“ als umbenannt und löst die Rückfrage aus. V3: eine Rückfrage je Monat, gespeichert wird nur der Monatsordner. Der Rückfrage-Dialog zeigt den Fehlertext je Vorlage (`FolderNames.decisionPathProblem`); Bestätigen ist erst bei geladener Vorlage möglich.
  - Sicherung (DEC-089): optionales Feld `folderLayout` im Format VERSION 1; unbekannter Code → Profil wird nicht importiert, „Ersetzen“ bricht ab, wenn die Sicherung nicht importierbare Profile enthält. Zuordnungspfade werden beim Lesen bereinigt (kein `..`, höchstens 2 Segmente).
  - Speicher freigeben (DEC-136): `SpaceFreer.RemoteFinder` sucht nur bei zerlegbarem Pfad.
- **Begründung:** Wunsch des Inhabers (pro Profil wählbar, nachträglich nicht änderbar, keine Dateimigration). Sperre bis zum ersten Sync und Blockieren gleicher Ziele: Entscheid des Inhabers (abweichend von der Empfehlung „ab erstem Speichern“ bzw. „nur warnen“). Die vereinigte Duplikatsuche schützt vor Doppelten bei Neuanlage mit anderer Struktur und bei Handverschiebungen. Wahl im Assistenten getrennt gemerkt, weil stilles Überschreiben beim Tippen des Zielpfads die Benutzerwahl verlor.
- **Alternativen:** `Land\Jahr\Monat` (Wurzel mischt Jahre und Länder), Tagesebene (Tageszahl nicht von Monatszahl unterscheidbar, viele Ordner), Monatsnamen (sprachabhängig → doppelte Ordner bei Sprachwechsel, Kollision mit „MM - Land“), frei editierbares Muster (Testaufwand) – verworfen; einstufig `JJJJ-MM[ - Land]` und nur `Jahr[ - Land]` – zurückgestellt; nachträglicher Strukturwechsel bzw. Verschieben vorhandener Dateien – nur nach separatem Entscheid; Länderliste zur Erkennung generierter Namen bei V2/V3 und Löschen vor Melden beim Import – verworfen; dauerhafte Übernahme der Eigentümer-Vorlage in `p.folderLayout` – verworfen (0.14.4).
- **Konsequenzen:** Die Struktur ist nach dem ersten Sync fest; ein Wechsel erfordert ein neues Profil. Das PC-Import-Skript kennt nur V1 und darf nicht gegen Ziele mit V2/V3 verwendet werden.
- **Betroffene Komponenten:** `Profile.folderLayout` (DB v9), `FolderLayout`, `FolderLayoutGuard`, `ProfileOverlap`, `TargetResolver`, `MonthFolderSearch`, `SyncEngine`, `SpaceFreer.RemoteFinder`, `DecisionScreen`, `ProfileScreen`/`ProfileWizard`, `FolderNames`, `BackupCodec`
- **Quelle:** SYNC-20261109-070 (Konzept), -083/-084/-085 (Umsetzung), -089 (0.14.4), -086 (abgebrochen: Skript nicht mehr gepflegt), -087.
- **Abhängigkeiten:** DEC-016, DEC-019, DEC-022, DEC-068, DEC-089, DEC-092, DEC-136

---

## 6. Datenbank und Datenmodell

### DEC-003: Room-Datenbank als zentraler Zustandsspeicher
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Datenbank
- **Entscheidung:** Der Zustand liegt in Room:
  - Tabellen: `profiles`, `synced_files`, `folder_mappings`, `pending_decisions`, `media_info` (Analyse-Cache), `sync_runs`, `log_entries`, ab DB-Version 2 zusätzlich `mirror_state` und `mirror_conflicts`.
  - Migrationen werden manuell geschrieben (`MIGRATION_1_2`); die Schemas werden exportiert.
  - Aufbewahrung: Verlauf 90 Tage, Detail-Logs 30 Tage.
- **Begründung:** Schnelles Überspringen bekannter Dateien ohne Netzwerkzugriff; Profile und Verlauf müssen Updates überstehen.
- **Alternativen:** `fallbackToDestructiveMigration` wurde implizit verworfen, um Profile und Sync-Status zu erhalten.
- **Konsequenzen:** Jede Schemaänderung braucht eine Migration. Die Aufbewahrungsfrist der Läufe war bis 0.0.6 fehlerhaft und löschte den ganzen Verlauf; korrigiert in 0.0.7.
- **Betroffene Komponenten:** `data/Entities.kt`, `data/SyncDao.kt`, `data/AppDatabase.kt`
- **Abhängigkeiten:** DEC-021, DEC-035

### DEC-084: Migrationsregeln – nie destruktiv, neue Spalten mit DEFAULT
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Datenbank
- **Entscheidung:**
  - Kein `fallbackToDestructiveMigration`: Fehlt eine Migration, soll die App lieber abstürzen als still Daten löschen.
  - Neue Spalten per `ALTER TABLE … DEFAULT …`; die Entity erhält denselben Kotlin-Standardwert, aber kein `@ColumnInfo(defaultValue)`.
  - DB-Version 3 (`MIGRATION_2_3`, Spalte `trashRetentionDays`); das exportierte Schema wird committet.
  - Das Schema 2.json wurde vor der Änderung mit den Entities abgeglichen.
- **Begründung:** Vorschlag 6 („Room-Schema prüfen und Migrationen absichern“) angenommen; ab der Beta darf ein Update keine Daten verlieren.
- **Alternativen:** —
- **Konsequenzen:** Jede neue Persistenz, die ohne Migration auskommen soll, wird in `GlobalSettings` abgelegt (z. B. DEC-103).
- **Betroffene Komponenten:** `data/AppDatabase.kt`, `data/Entities.kt`, `app/schemas/…/3.json`
- **Abhängigkeiten:** DEC-003, DEC-083, DEC-113

### DEC-159: Verlaufstexte in der aktiven Sprache speichern
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-03)
- **Kategorie:** Datenbank
- **Entscheidung:**
  - Log-Einträge, Lauf-Kurztexte und Auslöser werden als fertiger Text in der Sprache gespeichert, die beim Lauf aktiv war. Ältere Einträge bleiben deutsch.
  - `RunGrouping.isIdle` erkennt die Merkmale („Papierkorb“/„trash“, „bereits vorhanden“/„already present“ …) in beiden Sprachen.
- **Begründung:** Rückfrage 1: „erstmal einfache variante, für später vormerken“.
- **Alternativen:** Kennungen speichern und erst beim Anzeigen übersetzen (vorgemerkt, siehe A. Offene Entscheidungen; Lösungsansatz in DEC-202 festgelegt).
- **Konsequenzen:** Nach einem Sprachwechsel ist der Verlauf gemischtsprachig. Periodische Aufträge behalten den Auslöser-Text vom Zeitpunkt der Einplanung. *(Abgelöst durch DEC-202, umgesetzt in 0.12.0/0.12.1: neue Einträge werden in der aktiven Sprache angezeigt; alte Einträge ohne Kennung bleiben in ihrer Sprache.)*
- **Betroffene Komponenten:** `sync/RunSummary.kt`, `data/RunGrouping.kt`, `SyncEngine`, `MirrorEngine`, `SyncScheduler`, `data/SyncDao.closeDanglingRuns`
- **Abhängigkeiten:** DEC-099, DEC-100, DEC-202

### DEC-202: Verlauf mit Kennungen – Code-Spalten mit Rückfalltext
- **Status:** Implemented *(freigegeben vom Inhaber 2026-11-08; umgesetzt durch Development -009: 0.12.0 Stufe 1 Lauf-Kurztext und Auslöser, 0.12.1 Stufe 2 Log-Zeilen; Nachtrag am Ende)*
- **Datum:** 2026-11-08
- **Kategorie:** Datenbank
- **Entscheidung:**
  - (a) Neue nullable Spalten `log_entries.code`, `sync_runs.messageCode`, `sync_runs.triggerCode` (TEXT); DB-Version 7, `MIGRATION_6_7` nur mit `ADD COLUMN` (DEC-084), Schema-Export, `MigrationTest` 6→7. Der Code ist ein kleiner JSON-Baum (org.json): Ressourcen-Name (nie R-ID), optional Pluralzahl, Argumente (Text, Zahl oder verschachtelter Code), Liste für Verkettung. Dateinamen und Fehlertexte bleiben wörtliche Argumente. `message`/`trigger` bleiben als Rückfalltext in der Sprache beim Schreiben. Ein zentraler Renderer (reiner Teil unit-getestet) zeigt den Code an; unbekannter Schlüssel sowie Parse- oder Formatfehler führen zum Rückfalltext.
  - (b) Alte Einträge (Code NULL) zeigen den gespeicherten Text, ohne Umrechnung; sie verschwinden mit der Aufbewahrung (Logs 30, Läufe 90 Tage).
  - (c) `RunGrouping.isIdle` prüft Schlüssel im Code (`run_trashed`, `run_found`, `run_conflicts`, `run_space_full`; Konstante neben `RunSummary`, Unit-Test); die Merkmal-Wörter bleiben nur Rückfall für Einträge ohne Code. `RunSummary` liefert einen strukturierten Wert statt String.
  - (d) Die Sicherung bleibt unberührt: `backup/*` enthält weder Läufe noch Logs, Formatversion bleibt 1. Regel für später: Code-Felder optional, Leser ignorieren unbekannte Felder.
  - (e) Verlauf-Export (`Diagnostics.logExport`, Teilen-Menü) und alle Anzeigen (`LogScreen`, `HomeParts`, `PreviewScreen`, Fehlermeldung in `SyncWorker`) rendern in der aktiven Sprache. Diagnose (`build`, `compress`) bleibt beim gespeicherten Text (DEC-160).
  - Auslöser: Die Kennung kommt zusätzlich in die Auftragsdaten (`SyncScheduler`); fehlt sie (bereits eingeplante Aufträge), gilt der Text.
  - Umsetzung in zwei Stufen: 1. Lauf-Kurztext und Auslöser, 2. Log-Zeilen. `TestTexts` muss Namen auflösen können (D33).
- **Begründung:** Beseitigt den gemischtsprachigen Verlauf (DEC-159, D35) und die Abhängigkeit von Merkmal-Wörtern.
- **Alternativen:** Kodierung in `message` (jeder vergessene Leser zeigt JSON, Rückfalltext und Diagnose-Regex gehen verloren); nur Schlüssel ohne Rückfall (umbenannte Texte ergäben leere Einträge) – beide verworfen.
- **Konsequenzen:** Nebenbefund: `Diagnostics.compress` (Regex `ALREADY_COUNT`/`ALREADY_FOLDER`) erkennt „bereits vorhanden“ nur deutsch, in Englisch ist die Zählung in der Diagnose falsch; lösbar in Stufe 2 über den Schlüssel `eng_already_in`. Restrisiken: Ein vergessener Leser zeigt nur den Rückfalltext (harmlos); Platzhalter DE/EN müssen gleich bleiben (`StringsParityTest`).
- **Betroffene Komponenten:** `sync/RunSummary.kt`, `data/RunGrouping.kt`, `data/Entities.kt` (`SyncRun`, `LogEntry`), `AppDatabase`, `SyncScheduler`, `ui/LogScreen.kt`, `diag/Diagnostics.kt`, `l10n/Msg.kt`, `l10n/Texts.kt`
- **Abhängigkeiten:** DEC-084, DEC-159, DEC-160
- **Nachtrag Umsetzung (0.12.0/0.12.1, im Code gelesen 2026-11-09):** `l10n/Msg.kt` (JSON-Kennung, Formen `t`, `k`+`a`, `k`+`n`+`a`, `j`+`p`, Tiefe höchstens 12, Rückfalltext bei jedem Fehler); `Texts.Resolver` löst Ressourcen-Namen auf (`entryName`, `stringByName`, `pluralByName`), `TestTexts` liefert ihn für Unit-Tests (D33); `MIGRATION_6_7` (DB-Version 7, drei `ADD COLUMN`); `MirrorEngine` und `SyncEngine` schreiben Text und Code aus derselben `Msg` (wörtliche Texte über `Msg.lit`); der Auslöser liegt zusätzlich als `KEY_TRIGGER_CODE` in den Auftragsdaten (`SyncScheduler`, Rückfall auf den Text); `RunGrouping.isIdle` prüft bei vorhandenem Code `RunSummary.NOT_IDLE_KEYS`, sonst die Merkmal-Wörter; `Diagnostics.logExport` rendert in der aktiven Sprache, die Diagnose bleibt beim gespeicherten Text. Abweichung/offen: Der Regex `ALREADY_COUNT` in `Diagnostics.compress` ist weiterhin nur deutsch (Nebenbefund oben nicht behoben). Das Schema `7.json` wird erst beim nächsten Build exportiert; `MigrationTest` 6→7 (`runCodesFromVersion6`) ist instrumentiert; später am 2026-11-09 auf dem Emulator ausgeführt und bestanden (QA, `docs/TESTING.md` §4.3). Vollständigkeit der Umstellung aller Log-Aufrufe nicht einzeln geprüft.

### DEC-203: Diagnose pseudonymisiert und maskiert
- **Status:** Implemented *(0.12.4, Auftrag Development an Documentation SYNC-20261109-045; Freigabe durch den Inhaber im Auftrag nicht genannt)*
- **Datum:** 2026-11-09
- **Kategorie:** Sicherheit, Datenschutz und Diagnose
- **Entscheidung:**
  - (a) Die Diagnose („Problem melden“, „Diagnosedaten kopieren“) enthält keine Benutzernamen, IP-Adressen, Rechner-, Freigabe-, WLAN-, Profil- und Ordnernamen, keine Pfade und keine genauen Dateinamen.
  - (b) Bekannte Werte werden durch stabile Platzhalter je Export ersetzt (Profil-1, Host-1, Freigabe-1, WLAN-1, Ordner-1, Benutzer-1). Freitext (Log, Fehlertexte, Abstürze) wird per Muster maskiert (<IP>, <MAC>, <E-Mail>, <URL>, <Pfad>, <Datei>.ext, <Koordinaten>, <Host>).
  - (c) Erhalten bleiben: Jahr/Monat-Ordner, Android-Standardordner (DCIM, Camera, Pictures …), Dateiendungen, öffentliche Update-URL (github), Geräte-ID (Zufalls-UUID), Gerätemodell, Android-Version, Einstellungen, Zähler. Filterregeln nur als Anzahl und Form („x*.x“). Aktuelles WLAN nur als Platzhalter.
  - (d) Bewusst unverändert: Verlauf-Export (`Diagnostics.logExport`, „Verlauf teilen“) als Export des Benutzers für sich selbst.
  - Grundsatz: lieber zu viel maskieren.
- **Begründung:** Diagnosedaten werden per Mail an Dritte gesendet; sie sollen keine personenbezogenen oder Umgebungsdaten enthalten (Begründung aus dem Auftrag abgeleitet).
- **Alternativen:** nicht überliefert.
- **Konsequenzen:** Restrisiko (laut Development): Namen oder Dateinamen in unbekannten Freitext-Formaten ohne bekannte Endung oder Muster können unmaskiert bleiben (z. B. Ordnername in Fehlertext ohne Pfadtrenner, der nicht als Profil/Pfad registriert ist). Der Text `settings_diag_hint` (DE/EN) wurde angepasst.
- **Betroffene Komponenten:** `diag/Redactor.kt` (neu), `diag/Diagnostics.kt` (`build` registriert vor der Ausgabe alle bekannten Werte; `compress`, `runLine`, `logLine` und Abstürze laufen durch `scrub`)
- **Abhängigkeiten:** DEC-160, DEC-123

### DEC-162: Ordnername aus dem Länderkürzel, Cache-Name nur als Kennzeichen
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-03)
- **Kategorie:** Datenbank
- **Entscheidung:**
  - Der Ordnername wird aus `media_info.countryIso` in der App-Sprache bestimmt (`CountryLookup.nameFor`); der gespeicherte `countryName` dient nur noch als Kennzeichen „Land bestimmt“ und als Rückfall.
  - Statistik und Ordnerauswahl zeigen Ländernamen über `CountryLookup.displayName(iso, fallback)` in der App-Sprache; `statRows` liefert dafür zusätzlich `countryIso`.
- **Begründung:** Vorschlag 2.3, vom Inhaber bestätigt („Einverstanden“).
- **Alternativen:** Namen im Cache in beiden Sprachen speichern (bräuchte eine Migration).
- **Konsequenzen:** Keine DB-Migration nötig (`StatRow` ist nur ein Abfrage-Objekt).
- **Betroffene Komponenten:** `sync/SyncEngine.kt`, `geo/CountryLookup.kt`, `data/SyncDao.statRows`, `data/Entities.kt` (`StatRow`), `ui/StatsScreen.kt`, `ui/DecisionScreen.kt`
- **Abhängigkeiten:** DEC-161, DEC-066

### DEC-172: DB-Version 6 – Prüfstufe und bereinigte Profilnamen
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-03)
- **Kategorie:** Datenbank
- **Entscheidung:** `MIGRATION_5_6`: `profiles.verifyLevel INTEGER NOT NULL DEFAULT 2`; bestehende Profile: `verifyUpload = 0 AND deleteMode = 'KEEP'` → 0 (Aus), sonst 1 (Wenig, bisheriges Verhalten); zusätzlich `UPDATE profiles SET name = TRIM(name)`. `verifyUpload` bleibt als Spalte und wird nur noch gespiegelt.
- **Begründung:** Bestehende Profile sollen ihr Verhalten behalten; die Migrationsregeln (DEC-084) verlangen `ALTER TABLE … DEFAULT`.
- **Alternativen:** Spalte `verifyUpload` entfernen (nicht gemacht: braucht Tabellen-Neuaufbau).
- **Konsequenzen:** `6.json` muss committet werden. Sicherungen schreiben `verifyLevel`; ältere Sicherungen werden über `verifyUpload` eingelesen (an → Wenig, aus → Aus).
- **Betroffene Komponenten:** `data/AppDatabase.kt`, `Profile`, `backup/BackupCodec.kt`, `app/schemas/.../6.json`
- **Abhängigkeiten:** DEC-084, DEC-171, DEC-173

### DEC-173: Profilnamen ohne Leerzeichen am Rand
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-03)
- **Kategorie:** Sonstiges
- **Entscheidung:** Profilnamen werden beim Speichern, beim Import und per Migration getrimmt; Anzeigen trimmen zusätzlich die in Läufen gespeicherten Namen.
- **Begründung:** Befund der UX-Prüfung („(OrdnerTest )“, „OrdnerTest , Fotos“).
- **Alternativen:** —
- **Konsequenzen:** Alte Läufe behalten den ungetrimmten Namen in der DB.
- **Betroffene Komponenten:** `ProfileScreen.save`, `BackupCodec`, `MIGRATION_5_6`, `HomeScreen`, `LogScreen`
- **Abhängigkeiten:** DEC-172

---

## 7. Datenintegrität und Duplikaterkennung

### DEC-020: Namenskonflikte als `name_2`, `name_3` …
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Synchronisation
- **Entscheidung:** Bei gleichem Namen und anderem Inhalt erhält die zweite Datei `_2` (nicht `_1`), dann `_3` usw.
- **Begründung:** Rückfrage 4.
- **Alternativen:** Überspringen oder Überschreiben (nicht gewählt).
- **Konsequenzen:** Die Duplikatsuche muss Namensvarianten mit `_N` mitprüfen (DEC-022).
- **Betroffene Komponenten:** `FolderNames.conflictName`, `SyncEngine.transfer`
- **Abhängigkeiten:** DEC-021, DEC-022

### DEC-021: Zweistufige Duplikaterkennung mit Teil-Prüfsumme
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Synchronisation
- **Entscheidung:**
  1. **Lokal:** Eine Datei mit gleichem Pfad, gleicher Grösse und gleichem Änderungsdatum in `synced_files` wird ohne Netzzugriff übersprungen.
  2. **Auf dem Share:** Liegt dort eine Datei mit gleichem Namen, wird zuerst die Grösse verglichen; bei anderer Grösse wird nicht gehasht. Bei gleicher Grösse folgt die Teil-Prüfsumme: SHA-256 über die Grösse (8 Byte, Big Endian) sowie je 64 KB von Anfang, Mitte und Ende; Dateien bis 192 KB vollständig.
- **Begründung:** Rückfrage 3 („beides“) und Vorschlag 4 des Inhabers (nur Ausschnitte hashen, bei anderer Grösse überspringen).
- **Alternativen:** Prüfsumme über die ganze Datei (abgelehnt: zu viele Daten).
- **Konsequenzen:** Kein vollständiger Inhaltsbeweis; das PC-Import-Skript muss denselben Algorithmus verwenden.
- **Aktualisierung (2026-11-11, DEC-223, 0.15.0):** Bei Löschmodus ≠ KEEP erteilt «bereits vorhanden» eine Löschfreigabe nur nach Prüfung mindestens auf Stufe «Mittel» (`PresenceCheck`); bei KEEP gilt «bereits vorhanden» weiter als gesichert. Für Dateien über 32 MB vergleicht der Ordner-Sync vollständig statt per Stichprobe.
- **Betroffene Komponenten:** `sync/PartialHash.kt`, `SyncEngine.transfer`, `SyncDao.syncedLite`, `tools/LeoSyncImport.cs`
- **Abhängigkeiten:** DEC-022, DEC-024, DEC-068

### DEC-022: Duplikatsuche in allen Ordnern desselben Monats
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Synchronisation
- **Entscheidung:** Vor jedem Upload werden alle Ordner desselben Monats nach gleichem Namen (inkl. `_N`), gleicher Grösse und gleicher Prüfsumme durchsucht; bei einem Treffer gilt die Datei als gesichert.
- **Begründung:** Der Inhaber verschiebt auf dem PC von Hand (z. B. von `10` nach `10 - Ägypten`). Nach dem Zurücksetzen des Sync-Status oder einer Neuinstallation sollen keine Duplikate entstehen.
- **Alternativen:** Suche nur im Zielordner (bisheriges Verhalten).
- **Konsequenzen:** Zusätzliche Ordnerlisten pro Lauf (zwischengespeichert); gehasht wird nur bei Treffern auf Name und Grösse.
- **Aktualisierung (2026-11-09):** Seit 0.14.3 gilt die Suche vorlagenunabhängig auch in Länder- und Unterordnern desselben Monats, für alle drei Vorlagen inkl. V1 (`MonthFolderSearch`, DEC-209).
- **Betroffene Komponenten:** `SyncEngine.Job.siblingFolders`, `TargetResolver.monthFolders`, `FolderNames.sameBaseName`
- **Abhängigkeiten:** DEC-021, DEC-020, DEC-209

### DEC-092: Doppelte Profile blockieren, Überschneidungen warnen
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** UI
- **Entscheidung:**
  - Identisch (gleiches Ziel und dieselben Quellen bzw. derselbe Handy-Ordner) wird nicht gespeichert.
  - Teilweise Überschneidung (gleiches Ziel, gemeinsamer Quellordner, auch verschachtelt mit Unterordnern) zeigt eine Warnung mit „Trotzdem speichern“.
  - Vergleich ohne Gross-/Kleinschreibung und Trenner-Unterschiede; deaktivierte Profile zählen mit.
  - Import mit „Hinzufügen“ überspringt identische Profile und meldet Überschneidungen.
- **Begründung:** Wunsch des Inhabers; Antwort 1: „gleiches Ziel mit Warnung zulassen“.
- **Alternativen:** Überschneidungen ebenfalls blockieren (Empfehlung, nicht gewählt).
- **Konsequenzen:** PC-Name und IP desselben PCs werden nicht als gleich erkannt.
- **Aktualisierung (Session 3):** Für Ordner-Profile warnt die App seit 0.4.1 auch bei gleichem oder verschachteltem PC- bzw. Handy-Ordner (`Hit.nested`, DEC-134).
- **Aktualisierung (2026-11-11, Entscheid Inhaber im Architecture-Chat, Quelle SYNC-20261111-151/-152, 0.15.0):** Die Überschneidungsprüfung gilt typübergreifend. Fotoprofil und Ordner-Profil warnen («Trotzdem speichern»), wenn das Fotos-Ziel gleich dem PC-Ordner des Ordner-Profils ist, darin liegt oder ihn enthält (immer), sowie wenn ein Fotos-Quellordner und der Handy-Ordner ineinander liegen und das Ordner-Profil aufs Handy schreibt (Richtung «Beide» oder «PC → Handy»). Handy-Ordner werden kanonisch aufgelöst (SAF-Kennungen `primary:`/`raw:`, `msd:` per `findDocumentPath`), nicht auflösbare per Text verglichen; nur Warnung, nicht blockieren. Begründung: Datenverlustpfad über «Löschungen übertragen» bei Fotosicherungen im Ordner-Profil, Doppelungen. Verworfen: blockieren; Handy-Seite immer warnen; kanonischen Pfad in der DB speichern. Offen: Spike-Ergebnis zu `msd:` am Gerät, SD-Karte im Fotoprofil (relative Pfade ohne Datenträger, mögliche falsche Warnung).
- **Aktualisierung (2026-11-09):** Ein zweites Fotoprofil mit gleichem Ziel und anderer Ordnerstruktur wird blockiert, bei verschachteltem Ziel gibt es eine Warnung (DEC-209).
- **Betroffene Komponenten:** `data/ProfileOverlap.kt`, `ui/ProfileScreen.kt`, `BackupManager.import`
- **Abhängigkeiten:** DEC-005, DEC-089, DEC-209

### DEC-129: Stichprobe nach einem Ordnerwechsel
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-02)
- **Kategorie:** Synchronisation
- **Entscheidung:** Wird der Handy- oder PC-Ordner eines synchronisierten Profils geändert, prüft die App rund 20 Dateien und die obersten Ordner des Sync-Stands im neuen Ordner. Passt er nicht, kommt die Warnung „Ist das wirklich der richtige Ordner?“ mit der Möglichkeit, den Pfad nochmals zu wechseln. Die Prüfung läuft beim Speichern und, falls ausstehend, im nächsten Lauf; bis dahin werden keine Wiederherstellungen oder Leerungen ausgeführt.
- **Begründung:** Vorgabe des Inhabers („stichprobenartig prüfen … Option, den Ordnerpfad nochmals zu wechseln“).
- **Alternativen:** Sync-Stand bei Ordnerwechsel immer zurücksetzen.
- **Konsequenzen:** —
- **Betroffene Komponenten:** `FolderCheck`, `GuardPolicy.sample/topDirs/matches`, `MirrorEngine.folderMatches`, `ui/ProfileScreen.kt`
- **Abhängigkeiten:** DEC-127

### DEC-130: Handy-Dateien sicher ersetzen (`.s2pold`)
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-02)
- **Kategorie:** Android
- **Entscheidung:** Beim Ersetzen auf dem Handy wird die alte Datei zuerst in `<name>.s2pold` umbenannt, dann die Temp-Datei auf den Namen gesetzt und erst danach `.s2pold` gelöscht. Bei einem Fehler wird zurückgerollt; liegengebliebene `.s2pold` werden beim nächsten Einlesen aufgeräumt bzw. zurückgeholt. `*.s2pold` ist immer vom Sync ausgeschlossen.
- **Begründung:** Massnahme gegen Datenverlust bei Abbruch mitten im Ersetzen (SAF kennt kein atomares Umbenennen über bestehende Dateien).
- **Alternativen:** alte Datei vorher löschen (bisher).
- **Konsequenzen:** —
- **Betroffene Komponenten:** `LocalTree.replaceWith/listRecursive(recover)`, `OLD_SUFFIX`, `FileFilter`
- **Abhängigkeiten:** DEC-038, DEC-094

### DEC-132: Gründlicher Inhaltsvergleich – bis 32 MB vollständig, darüber 32 Stichproben
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-02)
- **Kategorie:** Performance
- **Entscheidung:** Für Inhaltsvergleiche im Ordner-Sync gilt `PartialHash.thorough`: Dateien bis 32 MB werden vollständig gehasht, grössere über 32 verteilte Stichproben. Die Teil-Prüfsumme (3 × 64 KB) bleibt für den Fotos-Sync und das Import-Skript.
- **Begründung:** Antwort des Inhabers („kostet wahrscheinlich deutlich mehr Akku … würden 32 MB auch schon reichen?“).
- **Alternativen:** immer die ganze Datei hashen (verworfen, Akku); nur 3 × 64 KB (zu schwach).
- **Konsequenzen:** Grosse Dateien sind nicht vollständig bewiesen; Kosten nur bei gleicher Grösse.
- **Betroffene Komponenten:** `sync/PartialHash.kt` (`FULL_LIMIT`, `SAMPLES`, `thorough`), `LocalTree.contentHash`, `SmbSession.contentHash`
- **Abhängigkeiten:** DEC-021

### DEC-134: Warnung bei gleichem oder verschachteltem Ordner zweier Ordner-Profile
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-02)
- **Kategorie:** UI
- **Entscheidung:** Teilen sich zwei Ordner-Profile denselben PC-Ordner oder liegt einer im anderen (PC oder Handy), warnt die App beim Speichern.
- **Begründung:** Massnahme aus der Datenverlust-Prüfung; zwei Profile könnten sich gegenseitig Löschungen übertragen.
- **Alternativen:** blockieren (nicht gewählt).
- **Konsequenzen:** —
- **Aktualisierung (2026-11-11):** Typübergreifende Prüfung Fotos ↔ Ordner-Profil siehe DEC-092 (`Hit.cross`, `ProfileOverlap.canonicalTree`, `LocalTree.canonical`).
- **Abhängigkeiten:** DEC-092

### DEC-136: Speicher freigeben – Fund in anderem Monatsordner nur mit gleicher Prüfsumme
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-02)
- **Kategorie:** Sicherheit
- **Entscheidung:** Wird ein Foto nicht am gespeicherten Pfad gefunden, genügen in anderen Ordnern desselben Monats Name und Grösse nicht mehr; zusätzlich muss die Teil-Prüfsumme stimmen. Ist die Handy-Datei nicht lesbar, bleibt sie.
- **Begründung:** Massnahme aus der Datenverlust-Prüfung.
- **Alternativen:** —
- **Konsequenzen:** —
- **Aktualisierung (2026-11-09):** `RemoteFinder` sucht nur bei zerlegbarem Pfad (Ordnerstruktur V1 bis V3, DEC-209).
- **Betroffene Komponenten:** `SpaceFreer.RemoteFinder.exists`, `SpaceFreer.Candidate.hash`
- **Abhängigkeiten:** DEC-091, DEC-209

### DEC-137: SAF-Werte aus `stat()` und `children()` nie mischen; inhaltsgleich heisst nur Stand merken
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-03)
- **Kategorie:** Android
- **Entscheidung:**
  - Der Sync-Stand nach Download, Wiederherstellen und „Beide behalten“ wird per Ordnerliste (`tree.find`) gelesen, nicht per Einzelabfrage.
  - Ist eine Seite einseitig geändert, die Grösse aber gleich und der Inhalt identisch, wird nur der Stand gemerkt statt übertragen (nur wenn sonst übertragen würde).
- **Begründung:** Feldtest 0.5.0 (Profil im Ordner „Downloads“): Der Anbieter liefert in `stat()` andere Zeitstempel als in `children()`; die eben heruntergeladene Datei galt als geändert, wurde wieder hochgeladen und die unveränderte PC-Kopie landete im Papierkorb.
- **Alternativen:** —
- **Konsequenzen:** Einweg-Profile hashen nicht bei jedem Lauf.
- **Betroffene Komponenten:** `MirrorEngine.download/plan/runRestores/executeTransfer`, `LocalTree.find`
- **Abhängigkeiten:** DEC-035, DEC-132

### DEC-171: Prüfstufen nach dem Hochladen
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-03)
- **Kategorie:** Synchronisation
- **Entscheidung:** Statt „Upload prüfen“ (an/aus) gibt es für Fotos-Profile fünf Stufen:
  - Aus (nur Grösse), Wenig (Anfang/Mitte/Ende, 192 KB, = bisherige Prüfung), Mittel (bis 32 MB ganz, darüber 32 Stichproben; Standard für neue Profile), Viel (bis 256 MB ganz, darüber 128 Stichproben), Komplett (ganze Datei).
  - „Aus“ ist nur erlaubt, solange die Fotos auf dem Handy bleiben; beim Wechsel auf „Papierkorb“/„Löschen“ springt „Aus“ auf „Mittel“ mit Hinweis.
  - „Komplett“ zeigt einen Hinweis zu Akku und Zeit und fragt einmal nach.
  - „Aus“ gilt nicht als verifiziert.
- **Begründung:** Vorschlag des Inhabers (wenig, mittel, viel, komplett); „Aus“ nur bei behaltenen Fotos und „Mittel“ als Standard vom Inhaber festgelegt.
- **Alternativen:** Ein/Aus wie bisher (DEC-024).
- **Konsequenzen:** Neue DB-Spalte (DEC-172). Höhere Stufen lesen zusätzlich die lokale Datei; „Komplett“ verdoppelt bei Videos etwa die Übertragungszeit. Fotos mit „Aus“ erscheinen nicht in „Speicher freigeben“ (Hinweis seit 0.9.5).
- **Betroffene Komponenten:** `VerifyLevel` (data/Entities.kt), `Profile.verify()/mustVerify()`, `PartialHash.levelRegions/forLevel`, `SmbSession.levelHash`, `SyncEngine.transfer/localLevelHash`, `BackupCodec`, `ui/ProfileCards.kt`
- **Abhängigkeiten:** DEC-024 (ersetzt), DEC-132, DEC-172

### DEC-219: Derselbe Handy-Ordner mit mehreren PC-Zielen ist zulässig
- **Status:** Implemented (Verhalten laut UX im Emulator 0.14.7 beobachtet; `ProfileOverlap.mirrorHit` von Documentation im Code gelesen)
- **Datum:** 2026-11-10 (Entscheid Inhaber im UX-Chat, Quelle SYNC-20261110-121, -122, -123)
- **Kategorie:** Datenintegrität
- **Entscheidung:** Zwei Ordner-Profile dürfen denselben Handy-Ordner mit verschiedenen PC-Zielen verwenden. Die Überschneidungsprüfung (`ProfileOverlap`) meldet das bewusst nicht. Eine Meldung entsteht nur bei gleichem PC-Ziel (identisch bzw. gemeinsamer Ordner) oder wenn PC-Ordner oder Handy-Ordner ineinander verschachtelt sind.
- **Begründung:** Entscheid des Inhabers; im Emulator wurden zwei Profile mit gleichem Handy-Ordner und den PC-Zielen `…\qaanders` und `…\qaanders2` ohne Meldung gespeichert (gewollt).
- **Alternativen:** — (in der Quelle nicht genannt).
- **Konsequenzen:** Dieselbe Quelle wird zu mehreren PC-Zielen gesichert; verschachtelte oder gleiche PC-Ziele warnen weiterhin (DEC-092). Keine Code- und keine Hilfeänderung nötig.
- **Betroffene Komponenten:** `data/ProfileOverlap.kt` (`mirrorHit`)
- **Abhängigkeiten:** DEC-092


### DEC-223: Gleichheit vor Löschen, Freigeben und «synchron» wird inhaltlich geprüft
- **Status:** Implemented (0.15.0 laut Development, SYNC-20261111-142; Tests `DataIntegrityTest`, `RemoteFinderTest`; Integrationstests für `plan()`/`FolderFreer` über 32 MB und `DeletionManager` fehlen)
- **Datum:** 2026-11-11 (Entscheid Inhaber im Architecture-Chat; Quellen SYNC-20261110-125, -141, -142)
- **Kategorie:** Synchronisation
- **Entscheidung:** (1) Der Ordner-Sync vergleicht Dateien über 32 MB per vollständigem Hash beider Seiten statt per Stichprobe (abbrechbar, Lesefehler = nicht gleich); dasselbe gilt für «unverändert» bei «Speicher freigeben» (Ordner). (2) «Speicher freigeben» (Fotos) verlangt am gespeicherten Pfad zusätzlich den Teil-Hash, `photoItems` zusätzlich das Änderungsdatum. (3) Vor dem Löschen nach der Sicherung wird die PC-Kopie erneut geprüft; ohne erreichbaren PC wird nichts gelöscht; fehlt die Kopie, bleibt das Original und wird neu gesichert. (4) «Bereits vorhanden» erteilt bei Löschmodus ≠ KEEP eine Löschfreigabe nur nach Prüfung mindestens auf Stufe «Mittel» (präzisiert DEC-021).
- **Begründung:** R-I1 bis R-I3 und R-F1/R-I4: Datenverlustpfade durch Gleichheit auf Basis von Grösse oder Stichprobe.
- **Alternativen:** Konflikt oder Übertragung statt Vollhash über 32 MB (verworfen, Konfliktflut beim Erstabgleich).
- **Konsequenzen:** Längere Prüfzeiten bei grossen Dateien; `ContentCompare`, `PresenceCheck`, `SyncDao.deleteSynced`; Toast «PC nicht erreichbar – nichts gelöscht». Restrisiken siehe `docs/SYNC-ENGINE.md` 17 (R-I1, R-I3, R-I5 bis R-I7).
- **Betroffene Komponenten:** `mirror/MirrorEngine.kt`, `mirror/FolderFreer.kt`, `sync/SpaceFreer.kt`, `sync/DeletionManager.kt`, `sync/SyncEngine.kt`, `sync/PartialHash.kt`
- **Abhängigkeiten:** DEC-021, DEC-091, DEC-131, DEC-135

---

## 8. Lösch- und Wiederherstellungslogik

### DEC-025: Löschen nach dem Sync (behalten, Papierkorb, löschen)
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Synchronisation
- **Entscheidung:**
  - Optionen pro Profil: behalten (Standard), Papierkorb, endgültig löschen.
  - Freigegeben wird eine Datei erst, wenn alle aktiven Profile, die sie abdecken, sie geprüft gesichert haben.
  - Android 10: direktes Löschen. Ab Android 11: gesammelter Systemdialog (`createTrashRequest`/`createDeleteRequest`).
  - Nur für Bildordner-Profile.
- **Begründung:** Rückfrage 1 (Optionsmenü, Standard behalten); Frage 8 der Sync-Richtungsrunde blieb offen, deshalb galt die Empfehlung „nur Bildordner“.
- **Alternativen:** MANAGE_MEDIA ohne Bestätigungsdialog (erwogen, nicht umgesetzt).
- **Konsequenzen:** Unter Android 11+ ist eine Interaktion des Benutzers nötig (Benachrichtigung bzw. Karte auf der Startseite).
- **Betroffene Komponenten:** `sync/DeletionManager.kt`, `DeleteState`, `MainActivity` (Lösch-Launcher)
- **Abhängigkeiten:** DEC-024, DEC-005

### DEC-036: Löschungen optional übernehmen, Ersetztes immer in den Papierkorb
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Synchronisation
- **Entscheidung:**
  - „Löschungen übernehmen“ ist Standard aus. Ist es aus, wird eine gelöschte Datei wiederhergestellt; bei Einweg-Richtung wird nur der Stand verworfen.
  - Gelöschte und ersetzte Dateien landen in `.LeoSync-Papierkorb\<Datum>\…` der jeweiligen Seite, nie endgültig gelöscht.
- **Begründung:** Antwort 2: „default nein, Option lassen“.
- **Alternativen:** endgültiges Löschen (verworfen).
- **Konsequenzen:** Der Papierkorb wächst unbegrenzt (DEC-073 vorgeschlagen). Der alte `.Sync2PC-Papierkorb` bleibt ausgeschlossen.
- **Aktualisierung (Session 3):** Gleichnamige Dateien am selben Tag heissen im Papierkorb seit 0.7.0 `name (HH-mm-ss).ext` statt `name_2.ext` (DEC-143). Ersetzte Versionen landen nur noch bei eingeschaltetem Versionsschutz im Papierkorb (DEC-138).
- **Aktualisierung (Session 4):** Der Papierkorb heisst seit 0.8.0 `.LeoSync-Bin`; `.LeoSync-Papierkorb` und `.Sync2PC-Papierkorb` werden weiter erkannt (DEC-163).
- **Betroffene Komponenten:** `MirrorEngine.trashRemote`, `LocalTree.moveToTrash`, `FileFilter.ALWAYS_EXCLUDED`
- **Abhängigkeiten:** DEC-035, DEC-008

### DEC-083: Papierkorb mit Frist pro Profil automatisch leeren
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Synchronisation
- **Entscheidung:** Pro Ordner-Profil eine Frist `trashRetentionDays` (Standard 30, 0 = nie). Beim Lauf werden Datumsordner `yyyy-MM-dd` vor dem Stichtag in `.LeoSync-Papierkorb` und `.Sync2PC-Papierkorb` auf Handy und PC endgültig gelöscht; andere Ordnernamen nie. Nicht in der Simulation.
- **Begründung:** Vorschlag 8 angenommen; Rückfrage 3: „pro Profil“.
- **Alternativen:** globale Frist (vorgeschlagen, nicht gewählt).
- **Konsequenzen:** Fehler beim Leeren werden nur protokolliert; Netzfehler auf dem PC brechen den Lauf wie üblich ab.
- **Aktualisierung (Session 3):** Seit 0.6.0 gibt es zusätzlich das Leeren von Hand (DEC-140). Seit 0.7.0 nutzt die Frist dieselbe Regel (`PurgeRule.OLDER`) und lässt zum Wiederherstellen vorgemerkte Versionen liegen (DEC-145).
- **Betroffene Komponenten:** `mirror/TrashPolicy.kt`, `MirrorEngine.purgeTrash`, `LocalTree.purgeTrash`, `SmbSession.deleteDir`, `Profile.trashRetentionDays`, `ui/ProfileScreen.kt`
- **Abhängigkeiten:** ersetzt DEC-073; DEC-036, DEC-084

### DEC-091: Speicher freigeben nur nach Prüfung jeder Datei auf dem PC
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Sicherheit
- **Entscheidung:**
  - Eigener Bildschirm: Fotos/Videos älter als frei wählbare Monate (Aufnahmedatum, sonst Änderungsdatum), die als geprüft gesichert gelten.
  - Optional „Favoriten behalten“ (ab Android 11).
  - Vor dem Löschen wird jede Datei auf dem PC gesucht (gespeicherter Pfad, sonst alle Ordner desselben Monats) und die Grösse verglichen; fehlende oder abweichende bleiben.
  - Live-Foto-Paare (Ordner + Basisname) nur gemeinsam.
  - Ab Android 11 in den Android-Papierkorb (`createTrashRequest`, Blöcke à 500); Android 10 endgültig nach Rückfrage.
- **Begründung:** Vorschlag 7; Antworten 4 und 5 (Papierkorb, Monate frei, Favoriten wählbar). Der Inhaber löscht auf dem PC gelegentlich von Hand, die Datenbank allein reicht nicht.
- **Alternativen:** sofortiges endgültiges Löschen (verworfen).
- **Konsequenzen:** Der PC muss erreichbar sein. Vom Inhaber erfolgreich getestet.
- **Aktualisierung (Session 3):** Die Suche in anderen Monatsordnern verlangt seit 0.4.1 zusätzlich die gleiche Teil-Prüfsumme (DEC-136). Seit 0.7.0 gilt das Tool auch für Ordner-Profile (DEC-147) und hat eine neue Auswahl mit Kriterien und Vorschau (DEC-148); `SpaceFreer.scan` wurde durch `photoItems` ersetzt.
- **Betroffene Komponenten:** `sync/SpaceFreer.kt`, `ui/FreeSpaceScreen.kt`, `GlobalSettings.freeSpace*`
- **Abhängigkeiten:** DEC-021, DEC-022, DEC-025, DEC-026

### DEC-127: Schutzbremse – Ordner-Sync hält an und fragt nach
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-02)
- **Kategorie:** Synchronisation
- **Entscheidung:**
  - Arten (`GuardKind`): PC-Ordner fehlt (`REMOTE_MISSING`), Handy-Ordner fehlt (`LOCAL_MISSING`), zu viele Löschungen (`MASS_DELETE`), Ordner nach Wechsel prüfen (`CHECK_FOLDER`).
  - Ein blockierter Lauf endet mit Status `WAITING`; das Profil ruht, bis der Inhaber entscheidet (Karte auf der Startseite, Benachrichtigung, Zustandsbild „wartet auf dich“ `dog_attention`).
  - Antworten: „Trotzdem löschen“, „Nicht löschen“ (dieser Lauf übernimmt keine Löschungen), „Ordner passt“, „Als neu abgleichen“, Ordner neu wählen.
  - Fehlt der PC-Ordner, wird er nur beim allerersten Abgleich angelegt. Erledigte Konflikte werden erst nach der Prüfung gelöscht.
  - Gespeichert in SharedPreferences „guard“ (ohne DB-Migration).
- **Begründung:** Vorgabe des Inhabers „es darf nicht zu ungewollten Datenverlusten führen“; Massnahmen 1 bis 11 der Datenverlust-Prüfung angenommen.
- **Alternativen:** fehlenden Ordner leer neu anlegen (bisher; hätte alles als gelöscht gewertet).
- **Konsequenzen:** Vorgemerkte Wiederherstellungen und Leerungen warten ebenfalls (DEC-139, DEC-140; Hinweis auf der Startseite seit 0.7.0).
- **Betroffene Komponenten:** `mirror/MirrorGuard.kt` (`GuardStore`, `GuardPolicy`, `MirrorGuard.resolve`), `MirrorEngine.block`, `SyncWorker`, `SyncRunner.blocked`, `Notifications.guardNeeded`, `HomeScreen.GuardCard`, `ui/DogStatus.kt`, `tools/make-state-images.ps1`, `MirrorGuardTest`
- **Abhängigkeiten:** DEC-035, DEC-036, DEC-128, DEC-129

### DEC-128: Schwellen für Massenlöschungen
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-02)
- **Kategorie:** Synchronisation
- **Entscheidung:** Die Schutzbremse greift bei mehr als X % (pro Profil einstellbar, Standard 20) der synchronisierten Dateien, immer ab 100 Löschungen, wenn ein ganzer Unterordner mit mehr als 5 Dateien verschwindet, und bei Profilen mit höchstens 5 Dateien nur, wenn alle gelöscht würden. Die Simulation zeigt die Warnung, blockiert aber nicht.
- **Begründung:** Antworten des Inhabers (20 %, „ab 100 Dateien“, kleine Ordner nur bei vollständigem Entfernen); keine Mengenangaben in der Warnung („wieviel von was interessiert nicht“).
- **Alternativen:** feste Prozentgrenze ohne Ausnahme für kleine Ordner.
- **Konsequenzen:** DB-Version 4 (`massDeletePercent`, `MIGRATION_3_4`); Einstellung im Profil-Assistenten als optionaler letzter Schritt.
- **Betroffene Komponenten:** `GuardPolicy.massDelete/vanishedFolder`, `Profile.massDeletePercent`, `AppDatabase`, `BackupCodec`
- **Abhängigkeiten:** DEC-084, DEC-127, DEC-150

### DEC-138: Versionsschutz pro Ordner-Profil
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-02)
- **Kategorie:** Synchronisation
- **Entscheidung:** Schalter „Alte Versionen im Papierkorb behalten“ (`keepVersions`, Standard an): Bei einseitig geänderten Dateien wandert die überschriebene Version der Gegenseite in den Papierkorb (beide Seiten, bestehende Frist).
- **Begründung:** Wunsch des Inhabers, alte Versionen auf Quelle und Ziel wiederherstellen zu können.
- **Alternativen:** —
- **Konsequenzen:** DB-Version 5 (`MIGRATION_4_5`).
- **Betroffene Komponenten:** `Profile.keepVersions`, `MirrorEngine.plan`, `AppDatabase`, `BackupCodec`, `ui/ProfileScreen.kt`
- **Abhängigkeiten:** DEC-036, DEC-139

### DEC-139: Wiederherstellungen vormerken, der nächste Sync mit PC führt sie aus
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-02)
- **Kategorie:** Synchronisation
- **Entscheidung:**
  - Wiederherstellungen kommen immer in die Tabelle `pending_restores` und werden im nächsten Sync vor dem Einlesen ausgeführt; ist der PC aus, warten sie (Hinweis auf der Startseite).
  - Ablauf: aktuelle Versionen beider Seiten in den Papierkorb, alte Version an den Ursprungsort, dann auf die andere Seite kopieren und Stand setzen.
  - Auswahl profilbezogen: zuerst das Profil (bei einem direkt), dann Tage, „Alles vom … auswählen“, „Ganzer Ordner“, pro Datei höchstens eine Version.
  - Originalnamen nummerierter Einträge (`_N`) werden nur zurückgerechnet, wenn der Grundname im selben Ordner liegt.
- **Begründung:** Vorgaben des Inhabers (Punkt 7: „vormerken statt blockieren“; profilsensitiv).
- **Alternativen:** Wiederherstellen blockieren, solange der PC fehlt (verworfen).
- **Konsequenzen:** DB-Version 5 (`pending_restores`).
- **Betroffene Komponenten:** `PendingRestore`, `SyncDao`, `mirror/RestoreManager.kt`, `mirror/TrashIndex.kt`, `MirrorEngine.runRestores`, `TrashIndexTest`
- **Abhängigkeiten:** DEC-138, DEC-141, DEC-146

### DEC-140: Papierkorb von Hand leeren
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-03)
- **Kategorie:** Synchronisation
- **Entscheidung:** Regeln: älter als X Tage (Standard 7), einzelne Tage oder alles; Seiten: beide (Standard), nur Handy, nur PC. Rückfrage mit Anzahl und Grösse. Das Handy wird sofort geleert; der PC sofort, wenn erreichbar, sonst vorgemerkt (`PurgeStore`, Prefs „purge“) und im nächsten Sync nach den Wiederherstellungen erledigt. Markierte Einträge lassen sich einzeln endgültig löschen. Nur Ordner-Profile; Fotos verweisen auf den Galerie-Papierkorb.
- **Begründung:** Neues Tool laut Inhaber; Antworten „alles genannte, lass den User auswählen“.
- **Alternativen:** —
- **Konsequenzen:** Keine DB-Änderung. Die Kachel zeigt die zuletzt gemessene Grösse. Bekannte Lücke F3 bis zur Umsetzung von DEC-201: Aufräumen zeigt die Anzahl aus dem eingelesenen Stand, führt aber die Regel aus (D47).
- **Betroffene Komponenten:** `mirror/TrashPurge.kt`, `mirror/TrashCleaner.kt`, `LocalTree.purgeTrash/deleteTrashFile`, `MirrorEngine.runPendingPurges`, `TrashPurgeTest`
- **Abhängigkeiten:** DEC-083, DEC-141, DEC-145

### DEC-141: Ein Tool „Papierkorb“ statt „Wiederherstellen“ und „Papierkorb leeren“
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-03)
- **Kategorie:** UI
- **Entscheidung:** Variante 3: oben eine Übersicht (belegter Platz Handy/PC, Knopf „Aufräumen…“ öffnet die Regeln als Bottom-Sheet), darunter der Inhalt; sobald etwas markiert ist, erscheint unten eine Leiste mit „Wiederherstellen“ und „Endgültig löschen“ (seit 0.7.1 untereinander, weil abgeschnitten). Profilwechsel über ein Menü in der Titelleiste.
- **Begründung:** Wunsch des Inhabers; Empfehlung angenommen.
- **Alternativen:** Variante mit zwei Reitern „Durchsuchen / Aufräumen“ (verworfen).
- **Konsequenzen:** `RestoreScreen`/`PurgeScreen` und `Screen.Restore`/`Screen.Purge` entfallen.
- **Betroffene Komponenten:** `ui/TrashScreen.kt`, `Screen.Trash`, `ui/HomeScreen.kt`
- **Abhängigkeiten:** DEC-139, DEC-140, DEC-142, DEC-151

### DEC-142: Papierkorb für alle Ordner-Profile, auch ohne Versionsschutz
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-03)
- **Kategorie:** Synchronisation
- **Entscheidung:** Wiederherstellen ist bei allen Ordner-Profilen möglich, nicht nur mit `keepVersions`.
- **Begründung:** Gelöschte Dateien landen auch ohne Versionsschutz im Papierkorb; Antwort „einverstanden“.
- **Alternativen:** nur Profile mit Versionsschutz (0.5.0 bis 0.6.2, ersetzt).
- **Konsequenzen:** Die Wiederherstellungs-Logik hängt nicht von `keepVersions` ab.
- **Betroffene Komponenten:** `trashProfiles` in `ui/TrashScreen.kt`
- **Abhängigkeiten:** DEC-138, DEC-141

### DEC-143: Eindeutige Namen im Papierkorb mit Uhrzeit
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-03)
- **Kategorie:** Synchronisation
- **Entscheidung:** Liegt am selben Tag schon eine gleichnamige Datei im Papierkorb, heisst die nächste `name (HH-mm-ss).ext`, bei gleicher Sekunde `name (HH-mm-ss-2).ext`. Solche Namen werden immer auf den Originalnamen zurückgeführt; Altbestand mit `_N` weiter nach der Geschwister-Regel.
- **Begründung:** Eine echte Datei `Foto_2.jpg` wurde als ältere Version von `Foto.jpg` verwechselt; die Uhrzeit ist eindeutig und beim Stöbern auf dem PC verständlich (Empfehlung angenommen).
- **Alternativen:** Kennung im Namen (`Foto.LeoSync-2.jpg`), Unterordner pro Version, Zuordnungsliste pro Tag, Zuordnung nur in der App-Datenbank (alle verworfen; die letzte wegen Verlust nach Neuinstallation).
- **Konsequenzen:** Keine DB-Änderung. `moveToTrash`/`trashRemote` liefern den Papierkorb-Pfad zurück.
- **Betroffene Komponenten:** `TrashIndex.versionName/uniqueTrashName/timeNow/originalName`, `LocalTree.moveToTrash`, `MirrorEngine.trashRemote`, `TrashIndexTest`
- **Abhängigkeiten:** DEC-036, DEC-139

### DEC-145: Vorgemerkte Wiederherstellungen werden nie gelöscht
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-03)
- **Kategorie:** Synchronisation
- **Entscheidung:** Aufräumen, Endgültig löschen, vorgemerktes PC-Leeren und die automatische Frist lassen vorgemerkte Versionen liegen (übrige Dateien des Tages werden einzeln gelöscht). Die Rückfrage nennt sie, in der Liste sind sie markiert und nicht wählbar.
- **Begründung:** Logikprüfung: Sonst meldete der Sync nur „nicht mehr vorhanden“, die Datei kam nie zurück.
- **Alternativen:** —
- **Konsequenzen:** —
- **Betroffene Komponenten:** `TrashPurge.key/reservedKeys/reservedPaths/protectedHits`, `LocalTree.purgeTrash(rule, keep)`, `TrashCleaner.purgeRemote(…, keep)`, `RestoreManager.Loaded.reserved`, `TrashPurgeTest`
- **Abhängigkeiten:** DEC-083, DEC-139, DEC-140

### DEC-146: Wiederherstellen rollt bei Fehlern zurück, die Vormerkung bleibt
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-03)
- **Kategorie:** Synchronisation
- **Entscheidung:** Scheitert eine Wiederherstellung, kommt die alte Version zurück in den Papierkorb und die aktuellen Versionen an ihren Platz; bei vollständigem Rollback wird auch der alte Sync-Stand zurückgeschrieben. Die Vormerkung bleibt; eine rote Karte auf der Startseite bietet „Erneut versuchen“ und „Verwerfen“ (Markierung in Prefs „restore“).
- **Begründung:** Logikprüfung: Vorher konnte die Datei auf Handy und PC fehlen (nur im Papierkorb).
- **Alternativen:** Vormerkung bei Fehler löschen (bisher).
- **Konsequenzen:** Bis 0.11.7: ohne „Verwerfen“ wird bei jedem Sync erneut versucht.
- **Nachtrag 0.11.8 (Entscheid Inhaber 2026-11-08, Development -005; Angaben aus Auftrag -028, Fundstellen im Code stichprobenartig geprüft):** Pro Wiederherstellung zählt `RestoreFailures` Fehlversuche (Prefs „restore“, Schlüssel `attempts`, Format `restoreId:n`, nicht in der Sicherung). Netzwerkfehler zählen nicht (Vormerkung bleibt, Lauf wird unterbrochen). Ab dem 5. Fehlversuch (`RestoreRetryPolicy.MAX_ATTEMPTS`) überspringt `MirrorEngine.runRestores` den Eintrag. Die Vormerkung bleibt in der Datenbank, damit bleibt die Papierkorb-Version vor dem Löschen geschützt (`TrashPurge.reserved*`, unverändert). Die Karte „Wiederherstellen fehlgeschlagen“ zeigt zusätzlich einen Text für aufgegebene Einträge; „Erneut versuchen“ setzt die Zähler zurück (`RestoreManager.retryFailed`) und startet einen Sync, „Verwerfen“ löscht Vormerkung und Zähler. Zähler werden auch bei Erfolg und Profillöschung (`forget`) entfernt. Verlaufseintrag `eng_restore_gave_up` beim 5. Fehlversuch, nur bei vollständigem Zurückrollen (sonst bleibt der Text „siehe Papierkorb“). Restrisiko: Zähler nur in Prefs, bei „Daten löschen“ der App beginnen die 5 Versuche neu. Risiko D27 entschärft.
- **Betroffene Komponenten:** `MirrorEngine.runRestores/rollbackRestore`, `RestoreFailures`, `RestoreRetryPolicy`, `RestoreManager.discardFailed/retryFailed`, `ui/HomeScreen.kt`
- **Abhängigkeiten:** DEC-139

### DEC-147: Speicher freigeben auch für Ordner-Profile Handy → PC
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-03)
- **Kategorie:** Synchronisation
- **Entscheidung:**
  - Nur Ordner-Profile mit Richtung Handy → PC und ausgeschaltetem „Löschungen übernehmen“; nur Dateien, deren Sync-Stand zur Datei passt.
  - Jede Datei wird auf dem PC geprüft (vorhanden, gleiche Grösse, gleicher Inhalt per `contentHash`), danach auf dem Handy direkt gelöscht; ihr Sync-Stand wird vorher entfernt.
- **Begründung:** Wunsch des Inhabers; Bedingung „keine Auswirkungen beim Löschen des Sync-Status und erneuter Synchronisierung“. Im Code geprüft: Bei Handy → PC ohne Löschungen übernehmen verwirft der Sync fehlende Handy-Dateien nur (DROP); ohne Stand passiert nichts.
- **Alternativen:** Merker „freigegeben“ im Sync-Stand (unnötig); LeoSync-Papierkorb auf dem Handy (gibt erst nach der Frist Platz frei); auch bei „Löschungen übernehmen“ (verworfen, würde PC-Kopien löschen); beidseitige Profile (ausgeschlossen).
- **Konsequenzen:** Endgültiges Löschen ohne Papierkorb. Bis 0.11.7 blieben leere Ordner stehen.
- **Nachtrag 0.11.8 (Entscheid Inhaber 2026-11-08, Development -004; Angaben aus Auftrag -028, Fundstellen im Code stichprobenartig geprüft):** Nach `FolderFreer.verifyAndDelete` werden die Elternordner der tatsächlich gelöschten Dateien (tiefster zuerst) entfernt, aber nur, wenn sie beim Prüfen wirklich leer sind (`LocalTree.deleteDirIfEmpty` liest unmittelbar vorher neu ein). Nie entfernt werden Profil-Wurzel, Papierkorb-Ordner und `DCIM/Camera` (`FolderFreer.isProtectedDir`, gilt auch, wenn der gewählte Ordner `DCIM` oder `DCIM/Camera` ist). Fehler beim Aufräumen ändern das Ergebnis nicht. Anzeige „X leere Ordner entfernt“ im Ergebnis (`Result.foldersRemoved`). Nur Profile Handy → PC ohne „Löschungen übernehmen“ (wie bisher). Risiko D26 teilweise entschärft.
  - **Befunde QA -029 (nur Quelltextanalyse, nicht auf dem Gerät belegt) und Behebung 0.11.9 (Development -030/-031):** (A1) `LocalTree.children()` überspringt Einträge ohne Anzeigenamen; besteht ein Ordner nur daraus, galt er als leer und wäre per `deleteDocument` (bei vielen Anbietern rekursiv) gelöscht worden. **Behoben in 0.11.9:** `deleteDirIfEmpty` zählt vorher alle Einträge (`rawChildCount`, auch ohne Anzeigenamen) und löscht nur bei 0. (A2) **Restrisiko, vom Entwickler akzeptiert (Risiko D26):** Zwischen Zählung und `deleteDocument` kann ein anderer Prozess eine Datei anlegen (Zeitfenster Millisekunden); sie ginge verloren, weil Android kein nicht-rekursives Löschen kennt und `MirrorLock` nur vor der eigenen Engine schützt. Weitere Systemordner (z. B. `Pictures/Screenshots`, `Download`) sind nicht geschützt und werden bei Leere entfernt; `DCIM/Camera/Unterordner` ist nicht geschützt (gewollt).
- **Betroffene Komponenten:** `mirror/FolderFreer.kt`, `mirror/LocalTree.kt` (`deleteDirIfEmpty`), `ui/FreeSpaceScreen.kt` (`freeProfiles`, `FREE_HINT`)
- **Abhängigkeiten:** DEC-036, DEC-091, DEC-144, DEC-148

### DEC-163: Papierkorb heisst `.LeoSync-Bin`, alte Namen bleiben erkannt
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-03)
- **Kategorie:** Synchronisation
- **Entscheidung:**
  - Neuer Papierkorb-Ordner `.LeoSync-Bin` (`MirrorEngine.TRASH_DIR`).
  - `.LeoSync-Papierkorb` (`LEGACY_TRASH_DIR`) und `.Sync2PC-Papierkorb` (`OLD_TRASH_DIR`) werden weiter vom Sync ausgeschlossen, im Papierkorb angezeigt, wiederhergestellt und geleert (`TRASH_DIRS`, `isTrashDir`).
- **Begründung:** Inhaber: „gib den Ordnern eine englische Bezeichnung als default“, Name „.LeoSync-Bin“ von ihm vorgegeben; der alte Ordner existiert vermutlich schon vom Feldtest 0.5.1.
- **Alternativen:** feste Namen nicht übersetzen (Vorschlag des Agents, vom Inhaber verworfen); Namen je App-Sprache (verworfen, die App fände nach einem Sprachwechsel ihren Papierkorb nicht mehr).
- **Konsequenzen:** Bestehende Papierkörbe bleiben nutzbar; neue Einträge landen nur im neuen Ordner.
- **Betroffene Komponenten:** `mirror/MirrorEngine.kt`, `mirror/FileFilter.kt` (`ALWAYS_EXCLUDED`), `mirror/LocalTree.kt`, `mirror/TrashCleaner.kt`, `mirror/RestoreManager.kt`, `mirror/FolderFreer.kt`
- **Abhängigkeiten:** DEC-036, DEC-141

### DEC-231: Löschen über den Android-Systemdialog: Prüfung der PC-Kopie nur unmittelbar davor
- **Status:** Implemented (unverändert umgesetzt, keine Änderung in 0.16.0; Restrisiko akzeptiert)
- **Datum:** 2026-11-11 (Quelle SYNC-20261111-156 Punkt 1, -172; Chat Architecture)
- **Kategorie:** Löschen
- **Entscheidung:** Die volle Prüfung der PC-Kopie läuft unmittelbar vor `createDeleteRequest`/`createTrashRequest`. Eine Prüfung nach der Bestätigung im Systemdialog ist nicht möglich. Das Restrisiko ist akzeptiert.
- **Begründung:** Das System führt das Löschen bei Bestätigung selbst aus; ein Eingriff dazwischen ist technisch nicht möglich (SYNC-20261111-172, Technischer Kontext).
- **Alternativen:** Prüfung «nach Bestätigung, vor dem Löschen» (technisch nicht möglich).
- **Konsequenzen:** Das Zeitfenster zwischen Prüfung und Bestätigung bleibt als Restrisiko.
- **Betroffene Komponenten:** Löschweg der Fotos (`DeletionManager`, `SpaceFreer`)
- **Abhängigkeiten:** DEC-223

---

## 9. Android und Hintergrundprozesse

### DEC-030: Medienzugriff über MediaStore mit unzensierten GPS-Daten
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Android
- **Entscheidung:** Bildordner werden über MediaStore-Abfragen gelesen (`RELATIVE_PATH`), nicht über das Dateisystem. Für GPS und den Upload wird `setRequireOriginal` verwendet (`ACCESS_MEDIA_LOCATION`).
- **Begründung:** Performance; ohne das Original entfernt Android die GPS-Daten aus der Kopie.
- **Alternativen:** SAF für Bildordner (verworfen, langsamer).
- **Konsequenzen:** Bildordner erfassen nur Medien; für andere Dateitypen braucht es SAF (DEC-038).
- **Betroffene Komponenten:** `media/MediaScanner.kt`, `MediaAnalyzer`, `SyncEngine.readUri`
- **Abhängigkeiten:** DEC-031, DEC-038

### DEC-038: Ordner-Spiegelung über SAF mit dauerhafter Berechtigung
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Android
- **Entscheidung:** Der Handy-Ordner wird per `takePersistableUriPermission` freigegeben. Der Zugriff läuft über direkte `DocumentsContract`-Abfragen (nicht DocumentFile). Geschrieben wird über eine Temp-Datei, dann wird die alte Version ersetzt und umbenannt.
- **Begründung:** Der MediaStore liefert ab Android 11 keine Nicht-Medien-Dateien; direkte Abfragen sind schneller.
- **Alternativen:** `MANAGE_EXTERNAL_STORAGE` (nicht gewählt).
- **Konsequenzen:** Geht die Berechtigung verloren, scheitert der Lauf mit Hinweis. Für SAF-Bäume gibt es keinen Änderungs-Trigger.
- **Betroffene Komponenten:** `mirror/LocalTree.kt`
- **Abhängigkeiten:** DEC-035, DEC-042

### DEC-042: Prüfintervall für Ordner-Profile
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Android
- **Entscheidung:** Ein periodischer Trigger mit dem kürzesten Intervall aller aktiven Ordner-Profile (Standard 60 Minuten, mindestens 15), nur im WLAN, zusätzlich sofort beim WLAN-Verbinden.
- **Begründung:** Antwort 7 „ok“; der PC meldet Änderungen nicht von selbst.
- **Alternativen:** —
- **Konsequenzen:** Das Intervall wird beim Speichern und Löschen von Profilen neu berechnet.
- **Betroffene Komponenten:** `SyncScheduler.scheduleRemotePoll/updateRemotePoll`, `GlobalSettings.remotePollMinutes`
- **Abhängigkeiten:** DEC-043

### DEC-043: WorkManager-basierte Auslöser-Architektur
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Android
- **Entscheidung:**
  - Auslöser: WLAN-Callback per PendingIntent (die App muss nicht laufen), Content-URI-Trigger für neue Medien, Sicherheitsnetz alle 6 Stunden, manueller Start, Nachhol-Sync.
  - Ein Mutex verhindert parallele Läufe.
  - Vordergrunddienst über `setForeground` (Typ dataSync), mit Rückfall auf eine normale Benachrichtigung.
  - Option „nur beim Laden“.
- **Begründung:** Vorgaben Hintergrundbetrieb und Akku; Vorschläge 1 und 2 angenommen.
- **Alternativen:** ein dauerhafter Dienst (verworfen, Akku).
- **Konsequenzen:** Der Vordergrundstart aus dem Hintergrund kann ab Android 12 scheitern; der Rückfall ist nötig.
- **Aktualisierung (Session 2):** Das Sicherheitsnetz läuft seit 0.3.4 nicht mehr starr alle 6 Stunden, sondern 6 Stunden nach dem letzten Sync (DEC-102). Der Content-Trigger wartet das Mindestalter ab (DEC-101). Benutzer-Pause und „nur manuell“ unterdrücken die Auslöser (DEC-095).
- **Betroffene Komponenten:** `SyncScheduler`, `SyncWorker`, `TriggerWorker`, `Receivers.kt`, `WifiHelper`
- **Abhängigkeiten:** DEC-044, DEC-045, DEC-049, DEC-095, DEC-101, DEC-102

### DEC-044: WLAN-Erkennung über SSID mit Standortberechtigung
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Android
- **Entscheidung:** Ein automatischer Sync startet nur, wenn die SSID zu einem Profil passt. Dafür sind Standort, „Immer zulassen“ und ein eingeschalteter Standortdienst nötig. Direkt nach dem Verbinden liest die App die SSID bis zu 15 Sekunden lang nach. Der manuelle Sync ignoriert die SSID.
- **Begründung:** Android gibt die SSID nur mit Standortberechtigung heraus.
- **Alternativen:** —
- **Konsequenzen:** Fehlt eine Berechtigung, gibt es keinen Auto-Sync; der Grund wird angezeigt (DEC-067).
- **Betroffene Komponenten:** `WifiHelper.currentSsid`, `SyncWorker`, `ui/Permissions.kt`
- **Abhängigkeiten:** DEC-043

### DEC-045: Erreichbarkeitsprüfung des PCs mit Wiederholungen nach dem Verbinden
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Synchronisation
- **Entscheidung:** Vor dem Sync prüft die App per TCP auf Port 445 (3 Sekunden, ohne Anmeldung), ob der PC erreichbar ist. Bei automatischen Läufen folgen bis zu etwa 1 Minute weitere Versuche (nach 5, 10, 15 und 30 Sekunden), beim manuellen Lauf 2 Versuche.
- **Begründung:** Feldtest: Das WLAN-Signal kam, bevor der Share erreichbar war.
- **Alternativen:** ein einzelner 3-Sekunden-Test (DEC-046).
- **Konsequenzen:** Der automatische Start kann sich um bis zu etwa 1 Minute verzögern.
- **Betroffene Komponenten:** `SmbSession.isReachable`, `SyncWorker`
- **Abhängigkeiten:** ersetzt DEC-046; DEC-048

### DEC-048: 30 Minuten warten, wenn der PC im Ziel-WLAN nicht erreichbar ist
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Synchronisation
- **Entscheidung:** Ist das Ziel-WLAN verbunden, der PC aber nicht erreichbar, folgt der nächste Versuch nach 30 Minuten; in der Zwischenzeit ignorieren automatische Läufe die Auslöser. Eine neue WLAN-Verbindung und der manuelle Start prüfen sofort. Bricht die Verbindung mitten im Lauf ab, folgt der nächste Versuch nach 2 Minuten (0.0.11).
- **Begründung:** Wunsch des Inhabers (0.0.4); die 2-Minuten-Regel für kurze Aussetzer kam in 0.0.11 dazu.
- **Alternativen:** Backoff (DEC-047).
- **Konsequenzen:** Die Startseite zeigt „PC nicht erreichbar – nächste Prüfung um …“. Seit 0.11.7 gilt bei Schalter EIN ausserhalb des WLAN (nur mobile Daten/VPN) stattdessen eine Wartezeit von 2 Stunden (DEC-126).
- **Betroffene Komponenten:** `GlobalSettings.shareRetryAt`, `SyncScheduler.enqueueShareRetry`, `SyncWorker.handleUnreachable`
- **Abhängigkeiten:** DEC-045, DEC-049

### DEC-049: Nach WLAN-Unterbruch sofort beim Wiederverbinden fortsetzen
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Synchronisation
- **Entscheidung:** Bricht ein Lauf wegen WLAN-Verlust ab, startet die App keinen Timer. Sie setzt `resumeOnWifi`, und der WLAN-Receiver startet ohne die 60-Sekunden-Drosselung sofort neu. Ein Abbrechen durch den Benutzer zählt nicht als Unterbruch.
- **Begründung:** Wunsch des Inhabers (0.0.5).
- **Alternativen:** 30-Minuten-Timer auch hier (verworfen).
- **Konsequenzen:** Das funktioniert nur, wenn das Gerät die App wecken kann (siehe DEC-050 bis DEC-052).
- **Betroffene Komponenten:** `GlobalSettings.resumeOnWifi`, `WifiNetworkReceiver`, `SyncWorker.markInterrupted`
- **Abhängigkeiten:** DEC-050, DEC-052

### DEC-050: Zusätzlicher Fortsetzen-Auftrag beim Aufgabenplaner
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Android
- **Entscheidung:** Nach einem Unterbruch stellt die App einen WorkManager-Auftrag `sync-resume` ein (nur im WLAN). Bei falschem WLAN plant er sich nach 15 Minuten neu ein, höchstens 24 Stunden lang. Aus dem laufenden Auftrag heraus wird mit `APPEND_OR_REPLACE` eingeplant.
- **Begründung:** Feldtest: Wurde die Benachrichtigung weggewischt, weckte der Broadcast die App nicht mehr.
- **Alternativen:** —
- **Konsequenzen:** Laut Feldtest von 0.0.9 reichte das allein auf dem Gerät des Inhabers nicht.
- **Betroffene Komponenten:** `SyncScheduler.enqueueResume`, `SyncWorker`
- **Abhängigkeiten:** DEC-049, DEC-052

### DEC-052: Anzeige „Pausiert“ als Vordergrunddienst bis zur Rückkehr des WLANs
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Android
- **Entscheidung:** `PauseService` ist ein dataSync-Vordergrunddienst mit der Anzeige „Pausiert – wartet auf WLAN …“ und eigener ID 1008. Er registriert einen eigenen NetworkCallback und startet den Sync, sobald das Ziel-WLAN zurück ist. Rückfall ist eine normale Benachrichtigung; ab Android 15 beendet ihn `onTimeout` (im Code bei 0.14.7 nur `stopSelf()`, ohne Rückfall-Benachrichtigung; Ist-Stand nach Architecture SYNC-20261110-124/-141, R-BG3).
- **Begründung:** Wunsch des Inhabers. Der Feldtest zeigte, dass das Gerät die App ohne sichtbare Anzeige zu stark einschränkt.
- **Alternativen:** reine Benachrichtigung (DEC-051); nur WorkManager (DEC-050).
- **Konsequenzen:** Ab Android 15 gilt ein Zeitlimit von 6 Stunden pro Tag für dataSync; danach bleibt nur noch der Rückfall.
- **Betroffene Komponenten:** `sync/PauseService.kt`, `Notifications.pausedNotification/showPaused`, `AndroidManifest.xml`
- **Abhängigkeiten:** DEC-049, DEC-050

### DEC-054: WLAN- und CPU-Sperre während der Übertragung
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Android
- **Entscheidung:** WifiLocks (HIGH_PERF und LOW_LATENCY) und ein Partial-WakeLock (höchstens 4 Stunden), nur solange ein Lauf aktiv ist.
- **Begründung:** Frage des Inhabers, ob im Sperrbildschirm das WLAN abbricht.
- **Alternativen:** —
- **Konsequenzen:** Etwas höherer Akkuverbrauch während der Übertragung. HIGH_PERF ist in neueren Android-Versionen veraltet; die Wirkung ist ungewiss.
- **Betroffene Komponenten:** `sync/TransferLocks.kt`, `SyncWorker`
- **Abhängigkeiten:** DEC-055

### DEC-055: Ausnahme von der Akku-Optimierung anfordern
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Android
- **Entscheidung:** Die Einrichtungshilfe fordert `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` sowie alle nötigen Berechtigungen.
- **Begründung:** Ohne die Ausnahme ist ein Vordergrundstart aus dem Hintergrund (Android 12+) nicht erlaubt, und Doze kann das Netz sperren.
- **Alternativen:** —
- **Konsequenzen:** Nicht konform mit den Play-Richtlinien ohne Begründung (für den Play Store relevant, siehe B).
- **Betroffene Komponenten:** `ui/Permissions.kt`, `HomeScreen.PermissionCard`, `SettingsScreen`
- **Abhängigkeiten:** DEC-043, DEC-052

### DEC-082: Hersteller-Hinweise gegen App-Killer und deren Erkennung
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Android
- **Entscheidung:**
  - Schritte je Hersteller (OnePlus/Oppo/Realme, Samsung, Xiaomi, Huawei/Honor) erscheinen in den Einstellungen (seit 0.4.0 *App-Berechtigungen*), dazu ein Knopf „Anleitung im Browser“ (dontkillmyapp.com).
  - Die Diagnose erkennt Hersteller-Killer in `ApplicationExitInfo` (Grund OTHER bzw. „kill“ in der Beschreibung) und nennt Anzahl und letzten Zeitpunkt.
- **Begründung:** Diagnose des Inhabers zeigte `o-kill(6)` mit Wichtigkeit 125: Der Oplus-Killer beendete die App während des Vordergrunddienstes; das erklärte die früheren Fortsetzungsprobleme. Nach „Hintergrundaktivität zulassen“ traten keine neuen Kills mehr auf.
- **Alternativen:** —
- **Konsequenzen:** Die Bezeichnungen können je nach Software-Stand abweichen (Hinweis in der App).
- **Betroffene Komponenten:** `diag/VendorHints.kt`, `Diagnostics`, `ui/SettingsScreen.kt`
- **Abhängigkeiten:** DEC-052, DEC-079

### DEC-086: „Unterbrochen“, „Pausiert“ und offene Läufe im Verlauf unterscheiden
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Android
- **Entscheidung:**
  - Stoppt Android einen Lauf, wertet der Worker `stopReason` aus: `CANCELLED_BY_APP` gilt als Stopp durch den Benutzer, alles andere als `INTERRUPTED` mit Grund (z. B. „WLAN getrennt“).
  - Läufe, die beim Beenden der App offen blieben (z. B. Update), werden beim nächsten Start als `INTERRUPTED` mit Hinweis abgeschlossen; Endzeit = letzter Log-Eintrag. Simulations-Läufe sind ausgenommen.
  - Seit 0.3.1 heisst der Stopp durch den Benutzer im Verlauf „Pausiert“ (DEC-096).
- **Begründung:** Vorschläge 7 und 6 (0.1.2, 0.1.6) angenommen; Feldtest zeigte irreführendes „Abgebrochen“.
- **Alternativen:** —
- **Konsequenzen:** Das Fortsetzen selbst ist unverändert.
- **Betroffene Komponenten:** `SyncWorker.interruptReason`, `SyncEngine`, `MirrorEngine`, `SyncDao.closeDanglingRuns`, `ui/LogScreen.kt`
- **Abhängigkeiten:** DEC-049, DEC-053, DEC-096

### DEC-090: Erinnerung „lange nicht gesichert“
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Android
- **Entscheidung:** Täglicher Check (`stale-check`, ohne Bedingungen): Hatte ein aktives Profil seit N Tagen (Standard 7) keinen Lauf mit Status OK oder PARTIAL und warten bei Bildordnern übertragungsbereite Dateien, kommt eine Benachrichtigung; Wiederholung alle 3 Tage; nach erfolgreichem Lauf wird sie entfernt. Abschaltbar; bei „nur manuell“ aus.
- **Begründung:** Vorschlag 2 mit Ergänzung „in den Einstellungen ausschaltbar“; Antwort 3: 7 Tage.
- **Alternativen:** tägliche Wiederholung (verworfen); einmalige Meldung (offen gelassen, 3 Tage gewählt).
- **Konsequenzen:** Ohne erfolgreiche Läufe in der DB (z. B. nach 90 Tagen Löschfrist) dient der erste Check als Basis.
- **Betroffene Komponenten:** `sync/StaleCheck.kt`, `sync/PendingCheck.kt`, `Notifications.staleWarning`, `GlobalSettings.stale*`
- **Abhängigkeiten:** DEC-095, DEC-108

### DEC-095: Benutzer-Pause befristet oder „für immer (nur manuell)“
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01; „für immer“ 2026-11-02)
- **Kategorie:** Android
- **Entscheidung:**
  - Symbol oben rechts: ▶ = Automatik aktiv, ⏸ = pausiert (eigenes Pause-Icon, da im Basis-Iconset nicht vorhanden).
  - Pausieren fragt nach Stunden (1/4/24 oder 1–168); ein Auftrag `pause-end` setzt danach automatisch fort.
  - „Für immer (nur manuell)“ (`manualOnly`) schaltet alle Hintergrund-Auslöser ab (WLAN-Callback, Foto-Trigger, Sicherheitsnetz, Ordner-Prüfung, Erinnerung).
  - „Synchronisieren“ hebt eine befristete Pause auf, nicht aber „für immer“; nur ⏸ beendet „für immer“.
  - Eigenes Zustandsbild „pausiert“ (DEC-088).
- **Begründung:** Wunsch des Inhabers („beides“, Anzahl Stunden abfragen, „für immer“ = nur manuell).
- **Alternativen:** Pause ohne Begrenzung (verworfen, Sicherheitsnetz gegen vergessene Pause).
- **Konsequenzen:** Nicht zu verwechseln mit der WLAN-Pause `resumeOnWifi` (DEC-052).
- **Betroffene Komponenten:** `GlobalSettings.pausedUntil/manualOnly`, `SyncScheduler.pause/pauseForever/resume/clearPause`, `SyncWorker`, `WifiNetworkReceiver`, `StaleCheck`, `ui/HomeScreen.kt`, `ui/AppIcons.kt`
- **Abhängigkeiten:** DEC-043, DEC-096

### DEC-101: PC nur bei Bedarf kontaktieren, Foto-Auslöser wartet das Mindestalter ab
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-02)
- **Kategorie:** Performance
- **Entscheidung:**
  - Fotos-Profile ohne übertragungsbereite Dateien (lokal geprüft) überspringen Erreichbarkeitstest und Verbindung; Ordner-Profile brauchen den PC immer.
  - Der Content-Trigger feuert erst nach Mindestalter + 30 s, damit ein Lauf statt zwei genügt.
- **Begründung:** Akku-Vorschläge 1 und 2 angenommen; Ausgangswert 2,22 % Akku/Tag bei 4 Netzwechseln.
- **Alternativen:** —
- **Konsequenzen:** Ein Lauf ohne neue Fotos kostet praktisch kein Netzwerk.
- **Betroffene Komponenten:** `sync/PendingCheck.kt`, `SyncWorker`, `SyncScheduler.armContentTrigger`
- **Abhängigkeiten:** DEC-027, DEC-045, DEC-102

### DEC-102: Sicherheitsnetz 6 Stunden nach dem letzten Sync
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-02)
- **Kategorie:** Android
- **Entscheidung:** Statt eines starren 6-Stunden-Rhythmus ein einmaliger Auftrag `trigger-safety` mit 6 Stunden Verzögerung, den jeder Sync-Lauf neu startet (REPLACE); beim App-Start nur angelegt, falls keiner existiert (KEEP). Der alte periodische Auftrag wird entfernt.
- **Begründung:** Vorgabe des Inhabers: „immer 6h nach dem letzten Sync, immer weiter nach hinten schieben“.
- **Alternativen:** 12 Stunden oder Ausfall bei kurz vorher erfolgreicher Sicherung (vorgeschlagen, nicht gewählt).
- **Konsequenzen:** —
- **Betroffene Komponenten:** `SyncScheduler.scheduleSafetyNet`, `SyncWorker.doWork` (`finally`)
- **Abhängigkeiten:** DEC-043

### DEC-126: Option „auch über mobile Daten bzw. VPN synchronisieren“
- **Status:** Implemented (0.11.5, Nachträge 0.11.6, 0.11.7)
- **Datum:** 2026-11-08 (2026-11-02)
- **Kategorie:** Synchronisation
- **Entscheidung:** Vorgemerkt zusammen mit der PC-Erkennung: eine Option, über mobile Daten bzw. VPN zu synchronisieren (VPN über Mobilfunk gilt als gemessen; Hinweis auf Datenverbrauch).
- **Begründung:** Frage des Inhabers, ob der WLAN-Name für VPN-Nutzer Pflicht sein muss; „das kannst du dir vormerken“.
- **Alternativen:** —
- **Konsequenzen:** Umgesetzt als Schalter (Inhaberentscheid 2026-11-08, SYNC-20261108-003; Quelle: Rückmeldung Development, Code nicht selbst geprüft; Build 0.11.6 laut Inhaber erfolgreich, Unit-Tests im QA-Lauf bestanden):
  - Schalter `syncOffWifi` (Standard aus), Einstellungen › Übertragung; in Sicherung und Diagnose enthalten.
  - Ausserhalb des WLAN keine SSID-Prüfung, alle aktiven Profile berechtigt. Im WLAN (auch VPN über WLAN) bleibt die SSID-Regel (DEC-044).
  - Netzwerkbedingung der automatischen Aufträge: `CONNECTED` statt `UNMETERED`. PC-Erreichbarkeit wie bisher.
  - Auslöser ausserhalb des WLAN: neue Medien, Sicherheitsnetz (6 h), Ordner-Prüfung. Kein WLAN-Broadcast und kein Auslöser „mobile Daten verbunden“. Nur `PauseService` und `WifiNetworkReceiver` („Pausiert“) bleiben WLAN-gebunden; die Fortsetzen-/Wiederholungsaufträge (`enqueueResume`, `enqueueShareRetry`) nutzen bei Schalter EIN `CONNECTED` (`autoConstraints`). Mobile Daten/VPN gelten also **nicht** als Unterbrechung bis zum nächsten WLAN (Klärung Development -021/-023; bestätigt durch QA -020).
  - **Präzisierung 0.11.7 (Entscheid Inhaber 2026-11-08, Development -019/-021/-023; Quelle `sync/NetworkRules.kt`, `SyncWorker.handleUnreachable`):** Ausserhalb des WLAN (nur mobile Daten/VPN) gilt bei „PC nicht erreichbar“ und bei Verbindungsabbruch mitten im Lauf eine Wartezeit von **2 Stunden** (`NetworkRules.OFF_WIFI_RETRY_MINUTES` = 120); im WLAN bleiben 30 min bzw. 2 min (DEC-048). Der Erreichbarkeitstest umfasst ausserhalb des WLAN nur 2 Versuche (0/5 s; im WLAN automatisch 5). Die Wartezeit endet durch eine neue WLAN-Verbindung, den manuellen Start oder einen erfolgreichen Lauf (auch von Hand); neue Auslöser (Fotos, Sicherheitsnetz, Ordner-Prüfung) ignorieren sie. Beim Umschalten des Schalters und beim Einspielen einer Sicherung mit geändertem `syncOffWifi` wird neu geplant und die Wartezeit verworfen (`SyncScheduler.onOffWifiChanged`). „Mobil/VPN“ = jedes aktive Netz mit Internet ausser WLAN (auch Ethernet, Tethering, Bluetooth; `WifiHelper.isOnMobileOrVpn`).
  - **Bekannter Hinweis (QA -024):** Während der 2-h-Wartezeit kehren automatische Läufe (Foto-Auslöser, Sicherheitsnetz) still zurück (`SyncWorker`, Prüfung `shareRetryAt`); neue Fotos warten bis zu 2 h, sofern keine WLAN-Verbindung, kein manueller Start und kein Schalterwechsel die Wartezeit aufhebt.
  - **Entscheid Inhaber 2026-11-08 (Development -025, „so belassen“):** Bereits geplante `WORK_RESUME`/`WORK_FOLLOWUP` behalten nach einem Schalterwechsel ihre alte Netzbedingung (`onOffWifiChanged` plant nur das Sicherheitsnetz neu und verwirft die Wartezeit). AUS→EIN: Aufträge warten weiter auf WLAN (`UNMETERED`), bis der nächste Auslöser kommt (Verzögerung, vom Inhaber akzeptiert). EIN→AUS: harmlos (Auftrag läuft an, Worker beendet sich). Kein Datenverlust; `enqueueResume`/`enqueueFollowUp` bleiben unverändert. Quelle: Rückmeldung Development/QA, nur Quelltextanalyse, nicht auf dem Gerät belegt.
  - Hinweis auf Datenverbrauch in der App; 0.11.6 nennt zusätzlich mögliche Kosten beim Mobilfunkabo (z. B. Ausland) und verwirft beim Umschalten die Wartezeit „PC nicht erreichbar“ (`cancelShareRetry`).
  - Folgeauftrag SYNC-20261108-019 (Wiederholungen über Mobilfunk begrenzen) umgesetzt in 0.11.7 (siehe Präzisierung 0.11.7 oben).
- **Betroffene Komponenten:** `sync/NetworkRules.kt` (0.11.7), `sync/WifiHelper.kt` (`onAllowedNetwork`), `sync/SyncScheduler.kt` (`autoConstraints`), `sync/SyncWorker.kt`, `GlobalSettings.syncOffWifi`
- **Abhängigkeiten:** DEC-044, DEC-109, DEC-204

### DEC-204: PC-Erkennung per PC-Kennung (SMB-Server-GUID), Modus je Profil
- **Status:** Implemented (im Code umgesetzt in 0.13.0, Development SYNC-20261109-047; APK 0.13.0 vorhanden, Unit-Tests grün (Gesamtlauf 2026-11-10 287/287), `MigrationTest.pcCheckFromVersion7` und Emulator-/Gerätetests G1–G15 durch QA bzw. Inhaber belegt (SYNC-20261109-053); Punkt 1 „Lernen“ und Punkt 5 „manueller Sync“ wurden durch DEC-205 geändert und in 0.13.1 umgesetzt. „Implemented“ heisst nur „im Code umgesetzt“. Offen als Prüfung: G7 (Akku bei `GUID` in fremden WLANs, Inhaber, SYNC-20261109-072), Stabilität der PC-Kennung bei Samba/NAS (ungeprüft))
- **Datum:** 2026-11-09 (2026-11-09, Freigabe Inhaber; Vorgabe Architecture SYNC-20261109-048)
- **Kategorie:** Synchronisation
- **Entscheidung:** Zusätzlich zur WLAN-Erkennung gibt es die Erkennung des PCs über seine SMB-Server-GUID („PC-Kennung“), geprüft **vor** dem Senden der Zugangsdaten. Der Benutzer wählt je Profil den Modus `pcCheck` = `WIFI` (nur WLAN-Name) | `GUID` (nur PC-Kennung) | `BOTH` (beides muss erfüllt sein, nicht „eines von beiden“).
  1. **Modus und Lernen** *(„Lernen“ und der Test beim Moduswechsel werden durch DEC-205 ersetzt; Umsetzung offen, bis dahin gilt der folgende Text wie im Code)*: Bestehende Profile → `WIFI` (heutiges Verhalten, keine Verhaltensänderung durch die Migration). Neue Profile → `BOTH` bei eingetragenem WLAN-Namen, sonst `GUID` (gefahrlos wegen `awaitingFirstStart`, DEC-103: der erste Start von Hand lernt die Kennung). Gelernt wird nur nach erfolgreicher Anmeldung und `connectShare`, nur wenn für den aktuellen Host keine Kennung gespeichert ist, nie still überschreibend. `WIFI`: still bei jeder erfolgreichen Verbindung. `GUID`/`BOTH`: nur bei Zugriffsart `USER` (Verbindung testen, manueller Sync, Freigaben durchsuchen, Papierkorb/Wiederherstellen/Speicher freigeben aus der Ansicht); `AUTO` ohne bekannte Kennung meldet sich nicht an. Ein Wechsel auf `GUID`/`BOTH` im Profil startet sofort „Verbindung testen“.
  2. **Speicherort (DB v8):** drei neue Profilfelder, `MIGRATION_7_8` nur `ADD COLUMN` (DEC-084): `pcCheck TEXT NOT NULL DEFAULT 'WIFI'`, `pcGuid TEXT NOT NULL DEFAULT ''`, `pcGuidHost TEXT NOT NULL DEFAULT ''` (Host kleingeschrieben beim Lernen); Schema-Export und `MigrationTest` 7→8. Die Kennung gilt nur, wenn `pcGuidHost` dem aktuellen Host entspricht, sonst „unbekannt“. **Sicherung:** die drei Felder im Profil-JSON, `BackupCodec.VERSION` bleibt 1; fehlend oder unbekannt → `WIFI` und leer.
  3. **Kennungswechsel (z. B. Windows-Neuinstallation):** `AUTO` meldet sich nicht an, überspringt das Profil (`noteSkip` „Unter <Host> antwortet ein anderer PC“), benachrichtigt einmal je Profil bis zur Klärung, wartet wie bei „nicht erreichbar“ (30 min bzw. 2 h, DEC-048, DEC-126), kein FAILED-Lauf. Manueller Sync bricht das Profil mit Hinweis „Verbindung testen“ ab. Neu vertrauen nur in „Verbindung testen“ mit Rückfrage **vor** `authenticate`; die neue Kennung gilt für alle Profile mit gleichem Host und gleicher alter Kennung. Keine Kennung (null/Null-UUID) → bei `GUID`/`BOTH` nie anmelden, Hinweis „nur WLAN-Namen verwenden“. Instabile Kennung: kein Automatismus, nur Hinweistext.
  4. **Ort der Prüfung:** zentral. Reine Regel `PcIdentity.decide(mode, pcGuid, pcGuidHost, host, seenGuid, access)` → ALLOW / ALLOW_AND_LEARN / UNKNOWN / CHANGED / NO_ID. `SmbSession`: ein privater Weg connect → prüfen → authenticate für `open` und `listShares`; Standard `AUTO`; kein ungeprüfter öffentlicher Weg. Geprüft wird gegen den aktuellen DB-Stand (Profil per ID neu lesen; ungespeichertes Profil im Editor: Kopie); Lernen per bedingtem UPDATE. Eigene `PcIdentityException`, von `isNetworkError` ausdrücklich **nicht** als Netzwerkfehler gewertet. `SyncWorker` prüft nach dem Port-445-Test vorab (nur Aushandeln, je Host einmal) für `GUID`/`BOTH`; die Prüfung in `open` bleibt zweite Sicherung. Vorschau: Offline-Vorschau mit Meldung.
  5. **Eignung je Modus** *(der Satz „Manueller Sync … unbekannt → lernen“ wird durch DEC-205 ersetzt, Umsetzung offen)*: `SyncWorker` (Filter `p.ssids()`), `mayContinue`, `handleUnreachable`, `WifiNetworkReceiver`, `PauseService`: `WIFI`/`BOTH` wie heute (unlesbarer WLAN-Name = nicht erfüllt, nie stillschweigend erfüllt); `GUID` jedes WLAN bzw. erlaubte Netzart. Mobile Daten/VPN mit Schalter (DEC-126): `WIFI` wie heute, `BOTH`/`GUID` Kennung Pflicht. Das 15-s-Warten (DEC-044) nur, wenn ein Profil den Namen braucht; Standortberechtigung nur bei `WIFI`/`BOTH` mit Namen (DEC-190). Manueller Sync: `WIFI` ohne Prüfung, `GUID`/`BOTH` prüfen (unbekannt → lernen). DEC-126 bleibt unverändert (Wartezeiten, Versuche, Netzbedingungen).
  6. **Oberfläche:** Profil „PC-Erkennung“ (DE „PC-Kennung“, EN „PC ID“), drei Optionen mit je einem Satz Sicherheitsaussage, Status „PC erkannt / noch nicht erkannt / anderer PC“; WLAN-Liste nur bei `WIFI`/`BOTH`; Profilkarte zeigt den Status; die Kennung erscheint nur pseudonymisiert in der Diagnose (DEC-203).
  - **Tests:** Unit `PcIdentity` (Tabelle), `NetworkRules` je Modus, `BackupCodec` Rundlauf und alte Sicherung, `MigrationTest` 7→8, `isNetworkError(PcIdentityException) == false`; Gerätetest QA: ein fremder PC unter gleicher IP erhält keinen SESSION_SETUP. Version: neue Funktion → **0.13.0** (DEC-188, DEC-077).
- **Begründung:** Der WLAN-Name schützt heute nur automatische Läufe davor, Zugangsdaten an einen fremden Server mit gleicher IP zu senden; manueller Sync, Test, Freigaben durchsuchen, Papierkorb und Wiederherstellen senden ungeprüft (Befund F2). Ohne lesbaren WLAN-Namen (keine Standortfreigabe) versagen `PauseService` (Fortsetzen unterbleibt), das 15-s-Warten und `handleUnreachable` (Befund F3). Die Kennung liegt nach dem Aushandeln vor, vor `authenticate` (Befund F1: smbj 0.13.0, `connection.getConnectionContext().getServerGuid()`, per `javap` geprüft; nicht `getConnectionInfo()`).
- **Alternativen:** Speicherung in Prefs/Einstellungen ohne Schema-Änderung – verworfen: Zustand ausserhalb des Profils, verwaist beim Löschen des Profils, beim Einspielen einer Sicherung wäre eine Bindung an die Profil-ID nötig.
- **Konsequenzen:** DB v8 und eine erweiterte Sicherung (abwärtskompatibel, `VERSION` 1). Befund F4: Die Kennung ist nicht geheim (unverschlüsselt in der Aushandlung); sie schützt zuverlässig gegen einen fremden Server in fremdem Netz, nicht gegen einen gezielten Angreifer, der sie zuvor im eigenen Netz las (dieselbe Schwäche wie beim WLAN-Namen); angemeldet wird per NTLM (offline knackbare Antwort, kein Klartextpasswort). Sicherheitsaussage je Modus: `WIFI` behält die bekannte Schwäche (fremdes Netz mit gleichem Namen und gleicher IP), `GUID` und `BOTH` prüfen vor dem Senden der Zugangsdaten. **Restrisiken:** Akku bei `GUID` in fremden WLANs (Test ca. 1 min je Verbindung, dann alle 30 min); Stabilität der Kennung bei Samba/NAS ungeprüft. Kritische Änderung → Review und QA (Datenverlust/Sicherheit). Quelle: Rückmeldung Architecture, Code und smbj-Bibliothek von Architecture geprüft, von Documentation nicht selbst nachgeprüft.
- **Betroffene Komponenten:** `smb/SmbSession.kt` (mit `PcStore`), `sync/PcIdentity.kt` (inkl. `PcIdentityException`), `sync/SyncWorker.kt`, `sync/NetworkRules.kt`, `sync/Receivers.kt`, `sync/PauseService.kt`, `ui/Permissions.kt`, `data/*` (`Profile`, `AppDatabase.MIGRATION_7_8`, `SyncDao`), `backup/BackupCodec`, `diag/Diagnostics`, `ui/ProfileScreen.kt`, `ui/ProfileParts.kt`, `ui/HomeParts.kt`
- **Abhängigkeiten:** DEC-044, DEC-045, DEC-048, DEC-084, DEC-109, DEC-126, DEC-190; geändert durch DEC-205

### DEC-205: PC-Kennung nur beim Speichern und in „Verbindung testen“ lernen
- **Status:** Implemented (im Code umgesetzt in 0.13.1, Development SYNC-20261109-055 Punkt 6; APK 0.13.1 vorhanden, `PcLoginTest`/`PcGuidDaoTest` grün, Gerätetests G11/G12 durch QA belegt (SYNC-20261109-053). „Implemented“ heisst nur „im Code umgesetzt“; offen als Prüfung: G7, Stabilität der Kennung bei Samba/NAS)
- **Datum:** 2026-11-09 (2026-11-09, Freigabe Inhaber, Empfehlung B; Konzept Architecture SYNC-20261109-058, Anlass QA-Fund F1 aus SYNC-20261109-053 / -056)
- **Kategorie:** Synchronisation
- **Entscheidung:** Ersetzt DEC-204 Punkt 1 „Lernen“ und Punkt 5 „manueller Sync“. Bei `GUID`/`BOTH` lernt die App die PC-Kennung nur nach dem Speichern des Profils und in „Verbindung testen“, jeweils nach erfolgreicher Anmeldung und `connectShare`. Ein manueller Sync und alle Ansichten lernen nie.
  1. **Zugriffsarten:** `PcAccess` erhält `SETUP` (Profil-Editor: „Verbindung testen“, Freigaben/Ordner wählen, Lernen nach dem Speichern). `USER` (manueller Sync, Ansichten) und `AUTO` lernen nie: unbekannte Kennung = `UNKNOWN`, keine Anmeldung, Hinweis „Profil öffnen → Verbindung testen“. `SyncWorker.precheckPc` verliert den Lernzweig.
  2. **Regel:** `PcIdentity.decide(…, access, learnNetOk)`. Bei `GUID`/`BOTH` ohne gespeicherte Kennung für den Host: `SETUP` und `learnNetOk` → `ALLOW_AND_LEARN`; `SETUP` ohne `learnNetOk` → neue Entscheidung `WRONG_NET` (nicht anmelden, eigener Text); sonst `UNKNOWN`. Mit bekannter Kennung unverändert (gleich → `ALLOW`, abweichend → `CHANGED`). `WIFI` unverändert.
  3. **WLAN-Bedingung beim Lernen (`learnNetOk`, reine Funktion in `NetworkRules`):** `BOTH` → aktueller WLAN-Name lesbar und in der Liste des Profils (gespeicherter bzw. gerade gespeicherter Stand); unlesbar (keine Standortfreigabe) = nicht erfüllt, der Text weist auf die Standortfreigabe oder „nur PC-Kennung“ hin. `GUID` → immer erfüllt. Ermittelt im Aufrufer, nicht in `SmbSession`; gilt bei `BOTH` auch für ungespeicherte neue Profile.
  4. **Lernpunkte:** nur (a) nach dem Speichern und (b) „Verbindung testen“, beide nur bei gespeichertem Profil, nur wenn für den Host noch keine Kennung gespeichert ist (bedingtes UPDATE). „Freigaben/Ordner wählen“ meldet sich unter `SETUP`-Regeln an, lernt aber nicht.
  5. **Lernen nach dem Speichern:** nur wenn das gespeicherte Profil `GUID`/`BOTH` ist, ein Host gesetzt ist und keine gültige Kennung für diesen Host vorliegt. Läuft nach dem Schreiben in der Datenbank in einem app-weiten Scope (nicht im Bildschirm-Scope), Zeitlimit wie `checkPc` (30 s). Speichern gelingt immer; bei Fehlschlag (Port 445 zu, kein Netz, falsches WLAN, Anmeldung abgelehnt) Status „noch nicht erkannt“, bei abweichender Kennung „anderer PC“ ohne Rückfrage (Neuvertrauen nur im Test). Assistent (neues Profil): kurzes Lernen mit Anzeige „PC wird erkannt …“, damit ein sofortiger manueller Start nicht an `UNKNOWN` scheitert.
  6. **Moduswechsel:** Der automatische Test beim Wechsel auf `GUID`/`BOTH` (`setPcMode`) entfällt, das Lernen nach dem Speichern ersetzt ihn (keine Zugangsdaten bloss durch Antippen einer Option). Das sofortige Löschen der Kennung in `setPcMode` bleibt (verringert nur Vertrauen).
  7. **Grundsatz „still gelernte `WIFI`-Kennung erzeugt nie Vertrauen“ zentral:** `currentIdentity` wertet die gespeicherte Kennung als leer, wenn der DB-Modus `WIFI` ist und der wirksame Modus `GUID`/`BOTH`; `save()` verwirft sie zusätzlich bei diesem Wechsel.
  8. **Kennungswechsel** unverändert nach DEC-204 Punkt 3, nur in „Verbindung testen“ (`SETUP`); Ablauf nach SYNC-20261109-055 Punkt 3 (aushandeln, Verbindung schliessen, fragen, neu verbinden, exakte Kennung prüfen, anmelden, vertrauen).
  - **Tests:** `PcIdentityTest` (Tabelle Modus × Zugriffsart `AUTO`/`USER`/`SETUP` × `learnNetOk` × gespeichert/gesehen), `NetworkRulesTest` für `learnNetOk` (`BOTH` unlesbar/fremd/in Liste, `GUID`), Test zum Grundsatz in Punkt 7, Entscheidung „nach dem Speichern lernen ja/nein“ als reine Funktion; QA-Gerätetest: Speichern und Test in fremdem WLAN bei `BOTH` ohne Kennung → kein SESSION_SETUP, manueller Sync ohne Kennung → keine Anmeldung.
- **Begründung:** QA-Fund F1: Bei `GUID`/`BOTH` lernte nach DEC-204 auch ein manueller Sync die Kennung ohne Rückfrage und ohne WLAN-Prüfung; bei `BOTH` ohne bekannte Kennung konnte so ein fremder PC mit gleicher IP dauerhaft Vertrauen und Zugangsdaten erhalten. Nur die erfolgreiche Anmeldung samt `connectShare` belegt, dass der antwortende Server der eigene PC ist (ein fremder PC lehnt die Zugangsdaten fast immer ab); eine bloss ausgehandelte Kennung wäre unbestätigt. Lernen ist damit auf die bewusste Einrichtung im Editor beschränkt, in einem einzigen Mechanismus.
- **Alternativen:** (A) Lernen beim Speichern nur per Aushandlung ohne Anmeldung – verworfen (Kennung unbestätigt, bei `GUID` kann ein fremder PC dauerhaft Vertrauen erhalten). (A+B gemischt: `BOTH` per Aushandlung, `GUID` per Anmeldung) – nicht empfohlen (zwei Wege, mehr Code und Tests, kaum Gewinn). (C) DEC-204 belassen und nur beim manuellen Sync die WLAN-Bedingung für `BOTH` ergänzen – nicht empfohlen (`GUID` lernt weiter beim beiläufigen Sync). (D) Zentrale Host-Tabelle (DB v9, Kennung je Host mit Herkunft „still/bestätigt“) – später, nur bei Bedarf (Migration, `MigrationTest`, Sicherungsformat, doppelter Zustand); Inhaber-Vorgabe: keine Schemaänderung vorerst.
- **Konsequenzen:** Keine Schemaänderung. Manueller Sync und wiederhergestellte Sicherungen lernen nicht mehr; der Benutzer muss das Profil speichern oder testen. **Restrisiken:** (R1) Speichern oder Test bei `GUID` in einem fremden Netz mit gleicher IP sendet einmal die NTLM-Antwort; kein dauerhaftes Vertrauen, ausser der fremde Server akzeptiert beliebige Anmeldungen (Gast, Samba „map to guest“) und hat eine Freigabe gleichen Namens. (R2) Die Kennung ist nicht geheim und in der Aushandlung fälschbar (unverändert, DEC-204). (R3) Häufiger „noch nicht erkannt“, Hinweistexte nötig. (R4) `BOTH` ohne Standortfreigabe kann nicht lernen. (R5) Lernen nach dem Speichern läuft nebenläufig; ein sofortiger manueller Start kann es knapp verpassen (im Assistenten gemildert, Punkt 5). (R6) Nichts im Gerät geprüft. Quelle: Rückmeldung Architecture (SYNC-20261109-058), von Documentation nicht selbst nachgeprüft.
- **Umsetzungsentscheidungen (Development SYNC-20261109-055, mitgeteilt via -059):** (a) Das Lernen im Modus `WIFI` bleibt still, erzeugt aber nie Vertrauen (Punkt 7). (b) Hostwechsel im Editor: kein Lernen und kein Neuvertrauen, solange der Host ungespeichert ist (Lernen/Neuvertrauen nur, wenn der Host der Verbindung dem gespeicherten Host des Profils entspricht). (c) Einrichtungsausnahme: unter `SETUP` wird bei `GUID` ohne bekannte Kennung in jedem Netz angemeldet (Restrisiko R1 aus -058), bei `BOTH` nur im eingetragenen WLAN. (d) Abgewiesene Profile (und Fehler der Vorab-Prüfung) warten ohne Timer (eine Meldung je Profil); nicht erreichbare PCs setzen den Timer nur, wenn kein Profil lief bzw. bei Verbindungsabbruch mitten im Lauf (DEC-126/DEC-048 unverändert). **Verworfen:** Host-Tabelle (DB v9); Rückfrage bei jedem Hostwechsel (offen, Inhaberentscheid) – für bestehende Profile inzwischen entschieden, siehe DEC-208. Quelle: -055 «Durchgeführte Arbeiten», nicht im Code nachgeprüft.
- **Betroffene Komponenten:** `sync/PcIdentity.kt` (`PcAccess.SETUP`, `WRONG_NET`), `sync/NetworkRules.kt` (`learnNetOk`), `smb/SmbSession.kt` (`currentIdentity`), `sync/SyncWorker.kt` (`precheckPc`), `ui/ProfileScreen.kt` (`save`, `runTest`, `setPcMode`), `ui/ProfileParts.kt`
- **Abhängigkeiten:** DEC-204 (ändert Punkte 1 und 5), DEC-103, DEC-126

### DEC-206: Laufanimation: Einzelbilder als feste Quell-Assets
- **Status:** Accepted (im Code laut Development SYNC-20261109-066, Version 0.13.11, ungebaut)
- **Datum:** 2026-11-09 (Entscheid Inhaber im PM-Chat, Quelle SYNC-20261109-066/-067)
- **Kategorie:** UI
- **Entscheidung:** Die Einzelbilder der Laufanimation (`anim_dog_*.png`, `notif_dog_flipper.xml`) werden einmalig aus dem Video exportiert, von Hand veredelt und als feste Quell-Assets eingefroren. Der Build erzeugt sie nie neu; ffmpeg ist keine Build-Voraussetzung mehr. Endstand: nur die ersten 16 Bilder; Frames aus der Sicherung `.build/anim_backup_0.13.6` mit `tools/refine-anim-outline.py` nachbearbeitet (Aussenlinie neu gezeichnet, konstant 2.6 px, Einrisse geschlossen, dunkle Schattenreste transparent; Python, numpy, scipy, Pillow; nur manuelles Archivwerkzeug, nicht im Bau, überschreibt die Frames). `tools/export-anim-frames.py` bleibt Archivwerkzeug für den Einmal-Export (37 Bilder). Tempo: feste Konstante `FRAME_MS` = 42 ms je Bild in `ui/DogStatus.kt` und `flipInterval` 42 in `notif_dog_flipper.xml`; `LOOP_MS` (1542 ms) steuert nur noch den Statusbogen; eine Schleife dauert 672 ms. Referenzbilder: `.claude/images/animation_frames/leo_frame_01-16.png`.
- **Begründung:** Die automatische Freistellung per Helligkeits-Schwelle lieferte auch in 0.13.4 fehlerhafte Transparenz; Handnacharbeit würde bei jedem Neuexport überschrieben.
- **Alternativen:** Schwellenlogik weiter nachschärfen (verworfen); höhere Auflösung (nicht nötig, bleibt 320 px); 32-Bilder-Zyklus (Bilder 17–24 ×4) und 37 Bilder (verworfen: Hinterbein-Wechsel im Quellvideo uneinheitlich bzw. Entscheid Inhaber).
- **Konsequenzen:** Änderungen an der Animation erfolgen von Hand an den Assets; der Bau prüft und erzeugt sie nicht. Quelle: Nachtrag Development in -067, von Documentation nicht im Code nachgeprüft (`tools/build.ps1` Z. 119–120 bestätigt «Bau erzeugt nie neu»).
- **Betroffene Komponenten:** `tools/build.ps1`, `tools/export-anim-frames.py`, `tools/refine-anim-outline.py`, `res/drawable-nodpi/anim_dog_*`, `layout/notif_dog_flipper.xml`, `ui/DogStatus.kt`
- **Abhängigkeiten:** ersetzt DEC-060 (Erzeugung im Build); DEC-088

### DEC-207: Fehlermeldungen im UI in einem zentralen Fehler-Rotton
- **Status:** Accepted (im Code umgesetzt laut Development SYNC-20261109-071, Version 0.13.12; Punkt „nicht erreichbarer PC“ und Offline-Vorschau in 0.13.14, gebaut; Sichtprüfung durch QA nur teilweise, SYNC-20261109-075)
- **Datum:** 2026-11-09 (Entscheid Inhaber im PM-Chat, Quellen SYNC-20261109-071, -076, -077)
- **Kategorie:** UI
- **Entscheidung:** Fehlermeldungen, die im UI erscheinen, werden über einen zentralen `ErrorText` (`ui/Common.kt`, Theme-Farbe `colorScheme.error`, kein fester Farbwert) in Rotton dargestellt. Ausgenommen: Verlauf und Statuskachel auf dem Startbildschirm. Auch ein **nicht erreichbarer PC** gilt als Fehler (rot; „nicht erreichbar“ heisst „nichts verifiziert“). Roher englischer Ausnahmetext wird nicht im UI angezeigt (Ersatz durch übersetzte Meldung, z. B. in der Offline-Vorschau). Angepasst laut -071: „Verbindung testen“ (`ProfileScreen`), Ergebniskarte „Speicher freigeben“ (`FreeSpaceScreen`), Statuszeile der Simulation bei `RunStatus.FAILED` (`PreviewScreen`); Erfolgsmeldungen bleiben in normaler Textfarbe.
- **Begründung:** Meldungen, z. B. bei „Verbindung testen“, fielen kaum auf.
- **Alternativen:** —  (nicht in den Quellen genannt).
- **Konsequenzen:** Nicht färbbar bleiben Toasts (Backup, Log-Export, Diagnose, Update) und der Backup-Ergebnisdialog; ob dafür ein eigener Auftrag nötig ist, liegt beim Project Manager (-075). Keine neuen Texte, keine Logik- oder DB-Änderung (ausser der übersetzten Ersatzmeldung). Quelle: Angaben aus -071/-075/-076, von Documentation nicht im Code nachgeprüft.
- **Betroffene Komponenten:** `ui/Common.kt`, `ui/ProfileScreen.kt`, `ui/FreeSpaceScreen.kt`, `ui/PreviewScreen.kt`
- **Abhängigkeiten:** —

### DEC-208: Rückfrage beim Hostwechsel eines bestehenden Profils
- **Status:** Accepted (im Code umgesetzt laut Development SYNC-20261109-079, Version 0.13.14, gebaut; Test `PcIdentityTest` gezielt grün, UI-Verdrahtung nicht durch Unit-Tests abgedeckt; Sicherheits-QA -081 bestanden nach Code und Emulator-Gerätetest am 2026-11-09, Restrisiken: Meldung „Speicher freigeben“ bei fremdem PC, EN, Dunkelmodus, Fremd-WLAN)
- **Datum:** 2026-11-09 (Entscheid Inhaber im PM-Chat, Quellen SYNC-20261109-079, -082)
- **Kategorie:** Synchronisation
- **Entscheidung:** Präzisiert die Einrichtungsausnahme aus DEC-205 (Umsetzungsentscheidung c): Beim **Ersteinrichten** eines Profils bleibt die Anmeldung bei unbekanntem PC ohne Rückfrage. Ändert der Benutzer im Editor eines **bestehenden** Profils den Host, erscheint vor dem Senden der Zugangsdaten die Rückfrage „Anderen PC verwenden?“ (Host alt → neu). Regel `PcEditorRules.needsHostConfirm` (`sync/PcIdentity.kt`): nur bei bestehendem Profil, Modus `GUID`/`BOTH`, Hostwechsel gegenüber dem gespeicherten Stand und in dieser Sitzung noch nicht bestätigtem Host. Sie greift vor Speichern, „Verbindung testen“ und „Freigaben/Ordner wählen“; ohne Bestätigung weder Anmeldung noch Speicherung. `WIFI` und gleicher Host fragen nicht. Zusätzlich (-078): Bei fremdem oder unbekanntem PC (`PcIdentityException`) zeigen Papierkorb und „Speicher freigeben“ eine rote „anderer PC“-Meldung (vorhandene Texte `smb_err_pc_*`, `free_other_pc_hosts`); das Datenverhalten bleibt unverändert.
- **Begründung:** Zugangsdaten nicht an einen falschen oder fremden Rechner senden (verkleinert Restrisiko R1 aus DEC-205 für bestehende Profile).
- **Alternativen:** Keine Rückfrage (Restrisiko akzeptieren) – verworfen; Rückfrage auch bei Ersteinrichtung – bewusst ausgenommen (-058); neue Texte für den Papierkorb – verworfen (vorhandene wiederverwendet).
- **Konsequenzen:** R1 ist nur für bestehende Profile mit Hostwechsel geschlossen; bei Ersteinrichtung mit `GUID` unverändert offen. Die frühere Annahme, der Picker könne den Host über `onPick` setzen, widerlegte QA (-081): `RemoteFolderPicker` bleibt auf dem Host des Profils, `onPick` übernimmt nur den Pfad. Quelle: -079/-082, von Documentation nicht im Code nachgeprüft.
- **Betroffene Komponenten:** `sync/PcIdentity.kt` (`PcEditorRules.needsHostConfirm`), `ui/ProfileScreen.kt` (`confirmHost`, `saveNow`), Texte `profile_host_change_*`
- **Abhängigkeiten:** DEC-204, DEC-205

### DEC-152: App nur im Hochformat
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-03)
- **Kategorie:** Android
- **Entscheidung:** `MainActivity` mit `screenOrientation="portrait"`; Drehen ins Querformat ist abgeschaltet.
- **Begründung:** Vorgabe des Inhabers.
- **Alternativen:** —
- **Konsequenzen:** Mit targetSdk 36 (Android 16) ignoriert Android die Sperre auf grossen Bildschirmen (≥ 600 dp).
- **Betroffene Komponenten:** `AndroidManifest.xml`
- **Abhängigkeiten:** DEC-109

### DEC-183: Ziel-Version Android 16 (API 36) vorgezogen
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-04)
- **Kategorie:** Android
- **Entscheidung:** targetSdk und compileSdk 36 seit 0.9.5 (Teil der Play-Store-Vorbereitung, vorgezogen).
- **Begründung:** Der Play Store verlangt eine aktuelle Ziel-Version; geringer Aufwand; der Inhaber stimmte nach Erläuterung der Folgen zu.
- **Alternativen:** erst mit der Play-Store-Vorbereitung.
- **Konsequenzen:** Wirkt nur auf Android 16+: Hochformat-Sperre auf Tablets wirkungslos, Zurück-Geste mit Vorschau (im Emulator Android 17 geprüft), strengere Hintergrund-Kontingente (nicht geprüft). Die nächste Ziel-Version (37) dürfte eine Berechtigung für den Heimnetz-Zugriff verlangen.
- **Betroffene Komponenten:** `app/build.gradle.kts`
- **Abhängigkeiten:** DEC-001, DEC-109, DEC-152

### DEC-190: Berechtigungen erst bei Bedarf
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-08)
- **Kategorie:** Android
- **Entscheidung:** Die Startseite fragt nur, was vorhandene Profile brauchen (ohne Profil nichts): Medienzugriff bei Fotos-Profilen (im Assistenten direkt in der Quell-Karte), Standort nur bei Profilen mit WLAN-Namen (in der WLAN-Karte), Benachrichtigungen und Akku sobald es Profile gibt.
- **Begründung:** Design-Prüfung; der Inhaber: Bereich wirke sonst „überfordernd und zu fordernd“; entspricht den Play-Store-Empfehlungen.
- **Alternativen:** alle Berechtigungen beim ersten Start (bisher).
- **Konsequenzen:** Ein Profil ohne WLAN-Namen verlangt keinen Standort.
- **Betroffene Komponenten:** `ui/Permissions.kt` (`PermNeeds`, `missing`), `ui/PermissionRows.kt`, `ui/HomeScreen.kt`, `ui/ProfileScreen.kt`
- **Abhängigkeiten:** DEC-055, DEC-177


### DEC-221: Verworfener Start während eines laufenden Syncs wird einmal nachgeholt
- **Status:** Implemented (0.15.0 laut Development, SYNC-20261111-143; APK 0.15.0 vorhanden; Gesamtlauf 373 Tests grün laut Development; Gerätetest nicht dokumentiert)
- **Datum:** 2026-11-11 (Entscheid Inhaber im Architecture-Chat; Quellen SYNC-20261110-124, -141, -143)
- **Kategorie:** Android
- **Entscheidung:** Ein Start (manuell oder automatisch), der während eines laufenden Syncs verworfen würde, wird als Nachlauf vorgemerkt und nach Ende des laufenden Syncs genau einmal nachgeholt; ein manueller Start bleibt manuell. Die Sperre wird in jedem Fall freigegeben (auch bei einer Ausnahme in `markInterrupted`/`cancelProgress`). Der Merker liegt im Speicher und persistent; Setzen und Prüfen laufen unter einer gemeinsamen kurzen Sperre (`SyncGate`).
- **Begründung:** R-N1 (alle fünf Sync-Namen betroffen, Wirkung von `manual` ging verloren, Oberfläche zeigte nichts) sowie R-BG4/R-BG5 (Sperre konnte hängen bleiben oder den Folgeauftrag verwerfen).
- **Alternativen:** `APPEND` für alle Sync-Aufträge; Warten auf die Sperre im Worker (beide verworfen).
- **Konsequenzen:** Neue reine Klasse `SyncGate`, `SyncScheduler.enqueueDeferred`, `GlobalSettings.deferredStart`; «Pausieren» und «Abbrechen» verwerfen den Merker; Details in `docs/SYNC-ENGINE.md` 11.2. Die Anzeige regelt DEC-224. Offen (keine Entscheidung): `PauseService`/`dataSync`-Budget (R-BG1/R-BG3).
- **Betroffene Komponenten:** `sync/SyncGate.kt`, `sync/SyncWorker.kt`, `sync/SyncScheduler.kt`, `data/GlobalSettings`, `SyncGateTest`
- **Abhängigkeiten:** DEC-052, DEC-103

### DEC-233: Sonderaufträge bei belegter Sync-Sperre: Wiederholung statt Nachlauf-Merker
- **Status:** Implemented (0.16.0 laut Development, SYNC-20261111-172; QA-Prüfung offen, SYNC-20261111-178)
- **Datum:** 2026-11-11 (Quelle SYNC-20261111-156 Punkt 6, -172; Benutzerfreigabe erteilt, Chat Architecture)
- **Kategorie:** Hintergrund
- **Entscheidung:** Aufträge «Fortsetzen» (`KEY_RESUME`), «PC-Wiederholung» (`KEY_RETRY`) und «Folgelauf übersprungener Dateien» nehmen die Sperre über `SyncGate.tryAcquire()` ohne Merker; ist sie belegt, endet der Worker mit `Result.retry()` (linearer Backoff 30 s). Normale AUTO/MANUAL-Aufträge bleiben unverändert (DEC-221).
- **Begründung:** Als Nachlauf-Merker verloren die Aufträge Daten und die 24-h-Grenze: `markInterrupted` reiht `WORK_RESUME` ein, während die Sperre noch gehalten wird; startet er sofort, wurde er zum AUTO-Merker ohne `KEY_RESUME` (kein 15-Minuten-Nachfassen, keine 24-h-Grenze, «Pausiert» konnte stehen bleiben).
- **Alternativen:** — (in der Quelle nicht genannt).
- **Konsequenzen:** Daten und 24-h-Grenze bleiben erhalten. Details `docs/SYNC-ENGINE.md` 3.1.
- **Betroffene Komponenten:** `sync/SyncGate.kt`, `sync/SyncWorker.kt`, `sync/SyncScheduler.kt`
- **Abhängigkeiten:** DEC-221, DEC-238

### DEC-236: Fehler beim Speichern des Nachlauf-Merkers: nur Diagnose-Log
- **Status:** Implemented (0.16.0 laut Development, SYNC-20261111-172)
- **Datum:** 2026-11-11 (Quelle SYNC-20261111-156 Punkt 2, -172)
- **Kategorie:** Hintergrund / Diagnose
- **Entscheidung:** `DeferredStore.write` liefert das Ergebnis von `commit()`; bei `false` oder Ausnahme gibt es einen Eintrag im Diagnose-Log (Logcat), keinen Verlaufseintrag. Der Merker im Speicher bleibt.
- **Begründung:** In der Quelle nicht genannt.
- **Alternativen:** Verlaufseintrag (nicht gewählt).
- **Konsequenzen:** —
- **Betroffene Komponenten:** `data/GlobalSettings.kt` (`DeferredStore`)
- **Abhängigkeiten:** DEC-221

### DEC-237: «Verschlüsselung nicht verfügbar» bei automatischen Läufen: einmal je Profil bis zum nächsten Erfolg
- **Status:** Implemented (0.16.0 laut Development, SYNC-20261111-172)
- **Datum:** 2026-11-11 (Quelle SYNC-20261111-156 Punkt 7, -172)
- **Kategorie:** Hintergrund / Benachrichtigung
- **Entscheidung:** Die Engine meldet den Fall über ein Flag (`encryptionUnavailable`, ohne GUID-Vorabtest); der Verlauf bleibt `FAILED`. `SyncWorker` meldet bei automatischen Läufen die Fehlermeldung je Profil nur einmal (Merker `encNotified`) und setzt ihn nach einem erfolgreichen Lauf des Profils zurück (auch für WLAN-Profile). Ein manueller Start meldet immer.
- **Begründung:** Jeder automatische Lauf (Ordnerprüfung ab 15 Minuten) meldete den Fall sonst neu, weil die Engine bis `PcLogin` lief und scheiterte (SYNC-20261111-172, Technischer Kontext).
- **Alternativen:** — (in der Quelle nicht genannt).
- **Konsequenzen:** Umsetzung mit eigenem Merker-Set `encNotified` statt `pcNotified` (Hinweis Development).
- **Betroffene Komponenten:** `mirror/MirrorEngine.kt`, `sync/SyncWorker.kt`, `data/GlobalSettings.kt`
- **Abhängigkeiten:** DEC-222

---

## 10. Benutzeroberfläche und UX

### DEC-031: Quellordner über den Android-Systemdialog wählen
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** UI
- **Entscheidung:** Der Systemdialog `OpenDocumentTree` liefert den Ordner; die App rechnet ihn in einen MediaStore-Pfad um (`primary:DCIM/Camera` wird zu `DCIM/Camera/`). Eine dauerhafte Berechtigung ist dafür nicht nötig.
- **Begründung:** Der Inhaber fand die eigene Liste „schrecklich“.
- **Alternativen:** eigene MediaStore-Ordnerliste (DEC-032).
- **Konsequenzen:** Speicher-Wurzel und `Download`-Wurzel sind ab Android 11 nicht wählbar.
- **Betroffene Komponenten:** `ui/ProfileScreen.kt` (`treeUriToRelativePath`)
- **Abhängigkeiten:** ersetzt DEC-032

### DEC-060: Animation in der Fortschrittsanzeige als Einzelbilder aus dem Video
- **Status:** Implemented (Erzeugung im Build seit 2026-11-09 durch DEC-206 abgelöst: Bilder sind feste Quell-Assets; der Rest bleibt gültig)
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** UI
- **Entscheidung:**
  - Die Fortschrittsbenachrichtigung nutzt eine eigene Ansicht (`DecoratedCustomViewStyle`); ein ViewFlipper blättert die Einzelbilder durch.
  - Quelle ist `new_animation.mp4`: Ausschnitt Bild 13–49 (nahtlose Schleife), quadratisch zugeschnitten, Wasserzeichen und Hintergrund per Flutfüllung entfernt, Duplikate gestrichen; ergibt 32 Bilder à 160 px, 48 ms pro Bild.
- **Begründung:** Wunsch des Inhabers. Das ganze Video (85 oder 121 Bilder) wurde wegen des Speicherbedarfs in SystemUI verworfen.
- **Alternativen:** GIF direkt (DEC-062), Sprite-Sheet (DEC-061).
- **Konsequenzen:** Etwa 1,3 MB mehr in der APK. Die Animation pausiert bei ausgeschaltetem Bildschirm. Braucht ffmpeg im Build-Werkzeug.
- **Aktualisierung (Session 2):** Seit 0.1.5 werden die Einzelbilder in **320 px** erzeugt (schärfer, auch für die Startseite, DEC-088). Der Animations-Hash im Builder trägt das Suffix `|v2-320px`, damit eine Änderung der Erzeugungsparameter eine Neuerzeugung auslöst.
- **Betroffene Komponenten:** `tools/make-anim-video.ps1`, `tools/build.ps1`, `layout/notif_dog_flipper.xml`, `notification_progress_small/big.xml`, `Notifications.progress`
- **Abhängigkeiten:** ersetzt DEC-061; DEC-088

### DEC-063: Einfarbiges Statusleisten-Symbol aus dem Hunde-Logo
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** UI
- **Entscheidung:** Silhouette mit Helligkeitsschwelle 70: helles Fell weiss, dunkle Konturen transparent. PNG je Dichte, vom Builder bei Änderung von `icon.png` neu erzeugt.
- **Begründung:** Wunsch des Inhabers; Android erlaubt in der Statusleiste nur einfarbige Symbole.
- **Alternativen:** generisches Vektor-Symbol (vorher).
- **Konsequenzen:** —
- **Betroffene Komponenten:** `tools/make-icons.ps1`, `drawable-*/ic_notification.png`
- **Abhängigkeiten:** DEC-011

### DEC-064: Zustandsbilder „wartet“ und „alles aktuell“
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** UI
- **Entscheidung:** Der sitzende Hund (`dog_wait`) erscheint in der Pausiert-Anzeige und auf der Startseite, solange ein Sync auf die Fortsetzung wartet. Der schlafende Hund (`dog_idle`) erscheint auf der Startseite mit „Alles gesichert und aktuell“ und in der Fertig-Meldung.
- **Begründung:** Vorgabe des Inhabers zur Bedeutung der beiden Bilder.
- **Alternativen:** —
- **Konsequenzen:** „Aktuell“ wird heuristisch bestimmt: letzter Lauf OK, nichts eingeplant oder übersprungen, keine Entscheidungen, Konflikte oder Löschungen offen.
- **Aktualisierung (Session 2):** Seit 0.1.4 zeigt die Startseite immer ein Bild (laufend, sitzend, schlafend, seit 0.3.2 zusätzlich „pausiert“); zeitversetzt geplante Aufträge verhindern „aktuell“ seit 0.3.3 nicht mehr. Details in DEC-088 und DEC-098.
- **Betroffene Komponenten:** `tools/make-state-images.ps1`, `HomeScreen.StatusCard`, `notification_paused.xml`, `Notifications.summary`
- **Abhängigkeiten:** DEC-052, DEC-088, DEC-098

### DEC-065: Startbildschirm mit sitzendem Hund und „LeoSync“
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** UI
- **Entscheidung:** Ein Compose-Startbildschirm für 1,3 Sekunden mit Überblendung, nur beim Kaltstart. Das Theme-Parent ist `Theme.DeviceDefault.DayNight`, damit der System-Splash ab Android 12 farblich passt.
- **Begründung:** Wunsch des Inhabers.
- **Alternativen:** SplashScreen-API mit eigenem Symbol (erwogen, wegen Zuschnitt und Masken nicht gewählt).
- **Konsequenzen:** Ab Android 12 erscheint davor immer kurz der System-Splash mit dem App-Icon.
- **Betroffene Komponenten:** `ui/SplashScreen.kt`, `MainActivity`, `values/themes.xml`
- **Abhängigkeiten:** DEC-064

### DEC-066: Statistik nach Jahr und Land
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** UI
- **Entscheidung:** Anzahl und Speicher pro Jahr und pro Land (aus GPS), Fotos und Videos getrennt; nur für Bildordner-Profile.
- **Begründung:** Vorschlag angenommen.
- **Alternativen:** —
- **Konsequenzen:** Videos aus Live-Paaren zählen als „ohne Standortdaten“.
- **Betroffene Komponenten:** `ui/StatsScreen.kt`, `SyncDao.statRows`
- **Abhängigkeiten:** DEC-026

### DEC-067: Grund für übersprungenen Auto-Start auf der Startseite anzeigen
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** UI
- **Entscheidung:** `noteSkip` speichert den letzten Grund (SSID nicht lesbar, WLAN in keinem Profil, PC nicht erreichbar); die Startseite zeigt ihn bis zu 24 Stunden lang.
- **Begründung:** Diagnose bei Feldtests, weil stille Abbrüche nicht nachvollziehbar waren.
- **Alternativen:** —
- **Konsequenzen:** —
- **Betroffene Komponenten:** `GlobalSettings.noteSkip`, `SyncWorker`, `HomeScreen`
- **Abhängigkeiten:** DEC-044, DEC-045

### DEC-072: Vollständigkeitsbilanz pro Profil
- **Status:** Implemented (eingeschränkt, 0.11.1/0.11.2)
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** UI
- **Entscheidung:** Anzeige „X von Y Dateien gesichert, Z offen“ mit Liste und Grund. Freigabe des Inhabers (2026-11-08) nur für die Anzahl gesicherter und offener Dateien unter dem Profil, ohne Liste mit Gründen.
- **Begründung:** Frage des Inhabers nach einer Vollständigkeitsprüfung.
- **Alternativen:** —
- **Konsequenzen:**
  - 0.11.1: Fotos-Profile zeigen „X gesichert · Y offen“. „Gesichert“ zählt alle Einträge in `synced_files` (auch per „Speicher freigeben“ entfernte Dateien); „offen“ ist der lokale MediaStore-Abgleich ohne Mindestalter. Keine Liste mit Gründen.
  - 0.11.2: Ordner-Profile zeigen „X Dateien synchronisiert“ (Zeilen in `mirror_state`, Stand des letzten Abgleichs) und „Y Konflikte offen“ (`mirror_conflicts`, `resolution IS NULL`). Kein „offen“ im Sinn nicht übertragener Dateien, weil dafür ein PC-Listing nötig wäre; vom Inhaber als sinnvolle Ergänzung bestätigt. Neue DAO-Abfragen `mirrorStateCountFlow`, `openConflictCountFlow` (kein Schemaeintrag).
  - Quelle: Rückmeldung Development zu SYNC-20261108-002 und -016; Code von Documentation nicht selbst geprüft.
- **Betroffene Komponenten:** `ui/HomeScreen.kt`, `ui/ProfileCards.kt`, `data/SyncDao.kt`
- **Abhängigkeiten:** DEC-021

### DEC-088: Zustandsbild auf der Startseite immer sichtbar, mit Laufanimation
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** UI
- **Entscheidung:**
  - Sync läuft: laufender Hund (Einzelbilder der Benachrichtigungs-Animation); alles gesichert: schlafender Hund; vom Benutzer pausiert: Hund auf dem Rücken (`dog_pause` aus `.claude/pause.png`); sonst sitzender Hund.
  - Feste Höhen in dp (laufend 112, sitzend 96, schlafend 50, pausiert 72), fester Bildbereich gegen Springen.
  - Die Animation läuft nur, solange die App sichtbar ist (`repeatOnLifecycle(STARTED)`), und steht still, wenn Android-Animationen aus sind.
- **Begründung:** Wünsche des Inhabers (0.1.4, 0.1.5, 0.3.2). Bug in 0.1.4: Bilder wurden in Pixelgrösse statt dp gezeichnet, weil nur die Höhe gesetzt war.
- **Alternativen:** —
- **Konsequenzen:** Bilder müssen mit Höhe **und** Breite (`fillMaxWidth`) skaliert werden.
- **Betroffene Komponenten:** `ui/DogStatus.kt`, `HomeScreen.StatusCard`, `tools/make-state-images.ps1`, `tools/build.ps1`
- **Abhängigkeiten:** DEC-060, DEC-064, DEC-095

### DEC-096: Knopf „Abbrechen“ heisst „Pausieren“
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** UI
- **Entscheidung:** Der Knopf während eines Syncs (App und Benachrichtigung) heisst „Pausieren“; er hält nur den laufenden Lauf an, der nächste automatische Start setzt fort.
- **Begründung:** Hinweis des Inhabers, „Abbrechen“ sei irreführend.
- **Alternativen:** —
Pausiert angezeigt.
- **Betroffene Komponenten:** `HomeScreen.StatusCard`„Pausiert“ angezeigt.
- **Betroffene Komponenten:** `HomeScreen.StatusCard`, `Notifications.progress`, `LogScreen`
- **Abhängigkeiten:** DEC-086, DEC-095

### DEC-097: Nachfrage bei ungespeicherten Profiländerungen, Speichern oben
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** UI
- **Entscheidung:** Beim Verlassen eines geänderten Profils (Pfeil, Zurück-Geste, „Abbrechen“) fragt die App „Speichern / Verwerfen / Abbrechen“; zusätzlich ein Häkchen zum Speichern in der Titelleiste.
- **Begründung:** Der Inhaber hatte „Löschungen übernehmen“ eingeschaltet, aber nicht gespeichert, und hielt das Verhalten für einen Fehler.
- **Alternativen:** Hinweistext unter dem Schalter (vom Inhaber als unnötig verworfen).
- **Konsequenzen:** —
- **Betroffene Komponenten:** `ui/ProfileScreen.kt`
- **Abhängigkeiten:** —

### DEC-098: Statusanzeige unterscheidet fällige von zeitversetzten Aufträgen
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-02)
- **Kategorie:** UI
- **Entscheidung:** Nur fällige Aufträge (Startzeit erreicht) gelten als „wartet“, mit konkretem Grund (Heim-WLAN, Ladegerät, „startet gleich“). Zeitversetzte Aufträge (Nachhol-Sync, erneute PC-Prüfung) erscheinen nur als „Nächste Prüfung um …“ und verhindern den schlafenden Hund nicht. Ein Lauf ohne Übertragung heisst „Nichts zu tun – alles aktuell“.
- **Begründung:** Meldung des Inhabers: „wartet auf WLAN“ obwohl verbunden.
- **Alternativen:** —
- **Konsequenzen:** Nutzt `WorkInfo.nextScheduleTimeMillis`.
- **Betroffene Komponenten:** `ui/HomeScreen.kt`
- **Abhängigkeiten:** DEC-064, DEC-088

### DEC-099: Kurze Lauf-Zusammenfassungen ohne „übersprungen“
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-02)
- **Kategorie:** UI
- **Entscheidung:** Verlauf und Benachrichtigung zeigen nur, was passiert ist: „3 übertragen“, „12 bereits vorhanden“ (Funde der Duplikatsuche), Fehler, wartende Ordnerauswahlen, sonst „Nichts zu tun“; beim Ordner-Sync nur Werte ≠ 0. Das Detail-Log behält alle Zahlen.
- **Begründung:** Wunsch des Inhabers; Formulierung „bereits vorhanden“ statt „waren schon auf dem PC“ vom Inhaber vorgegeben.
- **Alternativen:** Anzahl der unveränderten Dateien weiter anzeigen (verworfen).
- **Konsequenzen:** Gespeichert wird der fertige Text; die Gruppierung (DEC-100) wertet ihn aus.
- **Aktualisierung (Session 4):** Die Texte stehen seit 0.8.0 in den Sprachdateien und werden in der beim Lauf aktiven Sprache gespeichert; `RunGrouping.isIdle` prüft Merkmale beider Sprachen (DEC-159).
- **Betroffene Komponenten:** `sync/RunSummary.kt`, `SyncEngine`, `MirrorEngine`
- **Abhängigkeiten:** DEC-087, DEC-100

### DEC-100: Verlauf gruppiert ruhige Läufe, Filter „Alle / Fehler / Übertragungen“
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-02)
- **Kategorie:** UI
- **Entscheidung:**
  - Aufeinanderfolgende Läufe ohne Übertragung, Fehler oder offene Punkte erscheinen als ein Eintrag („12× geprüft – nichts zu tun“), aufklappbar; nur in der Anzeige, jeder Lauf bleibt gespeichert.
  - Der Verlauf lädt alle Läufe der letzten 90 Tage (höchstens 2000).
  - Filter: „Alle“, „Fehler“ (FAILED, PARTIAL, Datei-Fehler) und „Übertragungen“ (≥ 1 übertragen); die beiden letzten sind kombinierbar (ODER), „Alle“ setzt zurück; die Auswahl bleibt gespeichert.
  - Laufnummer „#id“ in jeder Karte.
- **Begründung:** Wünsche des Inhabers; der Zwischenstand „Nur Übertragungen und Probleme“ (0.3.4) war zu lang und wurde ersetzt; „Alle“ kam auf Wunsch zurück (0.3.6).
- **Alternativen:** Läufe in der Datenbank zusammenführen (verworfen, bräuchte eine Migration).
- **Konsequenzen:** —
- **Betroffene Komponenten:** `data/RunGrouping.kt`, `ui/LogScreen.kt`, `GlobalSettings.logFilter*`
- **Abhängigkeiten:** DEC-099

### DEC-104: Simulation pro Profil mit wählbarer Dateianzahl
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-02)
- **Kategorie:** UI
- **Entscheidung:** „Simulieren“ (vorher „Vorschau“) steht im Profil statt auf der Startseite und simuliert nur dieses Profil; wählbar „Nur die ersten N Dateien“ (N frei, gemerkt) oder „Alle“. Die Engines analysieren im Trockenlauf höchstens N neue Dateien bzw. Einträge. Die Startseite hat nur noch „Synchronisieren“.
- **Begründung:** Antworten des Inhabers (profilspezifisch, Anzahl selbst bestimmen, Knopf aus der Statusanzeige entfernen).
- **Alternativen:** Simulation aller Profile auf einmal (bisher).
- **Konsequenzen:** Simuliert wird der gespeicherte Stand; bei ungespeicherten Änderungen ist der Knopf gesperrt.
- **Betroffene Komponenten:** `ui/PreviewScreen.kt`, `Screen.Preview(profileId)`, `SyncEngine`/`MirrorEngine` (`previewLimit`), `GlobalSettings.previewLimit`
- **Abhängigkeiten:** DEC-103

### DEC-105: Warnung vor der ersten Synchronisierung ohne Ladegerät
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-02)
- **Kategorie:** UI
- **Entscheidung:** Ist das Telefon nicht am Strom und synchronisiert ein Profil zum ersten Mal (noch nichts gesichert bzw. abgeglichen), erscheint vor dem Start ein Hinweis mit „Trotzdem starten“ / „Abbrechen“.
- **Begründung:** Wunsch des Inhabers.
- **Alternativen:** —
- **Konsequenzen:** Hinweis: Der Dateistand zeigt eine spätere Verfeinerung (zusätzlich „nie erfolgreicher Lauf“, 0.5.2), die nicht aus dieser Session stammt.
- **Aktualisierung (Session 3):** Die Verfeinerung stammt aus Session 3 (0.5.2): Als „erste Synchronisierung“ gilt ein Profil nur, wenn nichts gesichert ist **und** es noch keinen erfolgreichen Lauf gab (`dao.lastSuccessAt == null`). Grund: Bei leeren Ordnern auf beiden Seiten erschien die Warnung bei jedem Start. Läufe werden nach 90 Tagen gelöscht; danach zählt wieder nur der Sync-Stand.
- **Betroffene Komponenten:** `ui/FirstSync.kt`, `HomeScreen`, `ProfileScreen`
- **Abhängigkeiten:** DEC-103

### DEC-106: Einstellungen in sechs Rubriken, „Updates“ prüft sofort
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-02)
- **Kategorie:** UI
- **Entscheidung:** Das Zahnrad öffnet ein Menü mit Benachrichtigungen, Übertragung, App-Berechtigungen, App-Einstellungen, Updates, Diagnose; jede Rubrik ist eine eigene Seite. „Updates“ öffnet direkt die Update-Seite, die beim Öffnen prüft und den Schalter „Täglich nach Updates suchen“ enthält.
- **Begründung:** Vorgabe des Inhabers inkl. Zuordnung der Inhalte.
- **Alternativen:** eine lange Einstellungsseite (bisher).
- **Konsequenzen:** —
- **Betroffene Komponenten:** `ui/SettingsScreen.kt` (`SettingsSection`), `Screen.Settings(section)`, `update/UpdateScreen.kt`, `HomeScreen`
- **Abhängigkeiten:** DEC-107, DEC-108

### DEC-108: Benachrichtigungen einzeln abschaltbar, Fortschritt und „Pausiert“ nicht
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-02)
- **Kategorie:** UI
- **Entscheidung:** Schalter „Meldung nach jedem Sync“, „Update verfügbar“ und „Hinweise“ (Ordnerauswahl, Konflikte, Dateien entfernen, Fehler); dazu ein Knopf zu den Android-Benachrichtigungseinstellungen. Fortschrittsanzeige und „Pausiert – wartet auf WLAN“ bleiben immer an.
- **Begründung:** Wunsch des Inhabers; der Hinweis-Schalter ist nur erlaubt, weil die Hinweise auch über Karten auf der Startseite erreichbar sind. Antwort 2: „Pausiert“ nicht abschaltbar.
- **Alternativen:** „Pausiert“ abschaltbar (verworfen, sichert das Fortsetzen auf dem OnePlus ab).
- **Konsequenzen:** —
- **Aktualisierung (Session 5):** „Meldung nach jedem Sync“ ist bei Neuinstallationen seit 0.11.0 standardmässig aus (DEC-191). Die Seite „Benachrichtigungen“ hat seit 0.9.0 keine doppelte Überschrift mehr.
- **Betroffene Komponenten:** `GlobalSettings.notify*`, `sync/Notifications.kt`, `ui/SettingsScreen.kt`
- **Abhängigkeiten:** DEC-052, DEC-106, DEC-191

### DEC-148: Auswahl in Zeilen mit Wertfeld und Vorschau, gleiche Ansicht für Fotos
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-03)
- **Kategorie:** UI
- **Entscheidung:**
  - Zeilen (Tippen öffnet die Auswahl): Profil, Älter als (Tage/Wochen/Monate/Jahre oder egal), Ordner (inkl. Unterordner); unter „Weitere Kriterien“ zugeklappt: Dateitypen (Gruppen plus eigene Endungen), Grösse ab, Ausnehmen (Filtermuster), bei Fotos „Favoriten behalten“. Geänderte Zeilen bleiben sichtbar.
  - Laufend „Passend: N Dateien · Grösse“; Vorschau nach Ordnern mit Abwählen; Live-Foto-Paare nur gemeinsam.
  - „Älter als“: Fotos nach Aufnahmedatum, Ordner-Dateien nach Änderungsdatum.
- **Begründung:** Vorgabe „so viele Möglichkeiten wie möglich, aber übersichtlich“; Antworten 1 bis 3 („macht Sinn“, „einverstanden“).
- **Alternativen:** separater Knopf neben einem Textfeld (Idee des Inhabers, als eine Fläche umgesetzt).
- **Konsequenzen:** —
- **Betroffene Komponenten:** `sync/FreeRules.kt`, `SpaceFreer.photoItems`, `ui/FreeSpaceScreen.kt`, `FreeRulesTest`
- **Abhängigkeiten:** DEC-147, DEC-151

### DEC-149: Tools-Sektion – eine Zeile pro Tool, immer sichtbar, ausgegraut mit Hinweis
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-02 bis 2026-11-03)
- **Kategorie:** UI
- **Entscheidung:** Unter „Profile“ eine Sektion „Tools“ (0.5.0). Seit 0.6.1 ein Eintrag pro Zeile (Symbol links, Titel und Beschreibung rechts) statt zwei Kacheln pro Zeile. Seit 0.6.3 immer sichtbar; ohne passendes Profil ausgegraut, Tippen zeigt, wofür das Tool gedacht ist.
- **Begründung:** Wünsche des Inhabers („schöner als einfach nur Links“, „pro Tool eine Zeile“, „sichtbar aber ausgegraut“).
- **Alternativen:** Textlinks; zwei Kacheln pro Zeile (0.5.0 bis 0.6.0, ersetzt); Tools ausblenden (bis 0.6.2, ersetzt).
- **Konsequenzen:** —
- **Betroffene Komponenten:** `ui/HomeScreen.kt` (`ToolTile`, `ToolCard`)
- **Abhängigkeiten:** DEC-141, DEC-147, DEC-153

### DEC-150: Profil-Assistent in Schritten
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-02)
- **Kategorie:** UI
- **Entscheidung:** Neue Profile in einzelnen Schritten: Name, Quelle, Ziel, Sync-Optionen, Filter (optional), Schutzbremse (optional, nur Ordner-Profile). Ab dem ersten optionalen Schritt lässt sich speichern; Zurück geht einen Schritt zurück. Die Karten der Profilansicht folgen derselben Reihenfolge.
- **Begründung:** Vorgaben des Inhabers während der Umsetzung von 0.5.0.
- **Alternativen:** eine lange Seite (bisher).
- **Konsequenzen:** —
- **Betroffene Komponenten:** `data/ProfileWizard.kt`, `ui/ProfileScreen.kt`, `ProfileWizardTest`
- **Abhängigkeiten:** DEC-033, DEC-128

### DEC-151: Wertfeld mit Auswahlliste statt Knopfreihen
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-03)
- **Kategorie:** UI
- **Entscheidung:** Auswahlen mit vielen Werten (z. B. Tage im Papierkorb) erscheinen als ein Feld mit aktuellem Wert; Tippen öffnet eine Liste (neueste zuerst, mit Anzahl und Grösse; einfach oder mit Häkchen). Dasselbe Feld nutzt die Auswahl von „Speicher freigeben“.
- **Begründung:** Meldung des Inhabers: Ein Knopf pro Datum wird schnell unübersichtlich.
- **Alternativen:** Chip-Reihe pro Tag (0.6.0 bis 0.6.3, ersetzt).
- **Konsequenzen:** —
- **Betroffene Komponenten:** `ValueField`, `DayPickerDialog` in `ui/TrashScreen.kt`
- **Abhängigkeiten:** DEC-141, DEC-148

### DEC-167: Pause-Symbol zeigt die Aktion, nicht den Zustand
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-03)
- **Kategorie:** UI
- **Entscheidung:** Läuft die Automatik, steht oben ⏸ (tippen = pausieren); ist sie pausiert, steht ▶ (tippen = fortsetzen). Bis 0.8.0 zeigte ▶ „läuft“.
- **Begründung:** Befund der UX-Prüfung: ▶ wird allgemein als „starten“ gelesen; der Inhaber stimmte zu („Stimmt. Bitte ändern“).
- **Alternativen:** —
- **Konsequenzen:** Texte, die auf das Symbol verweisen („über ▶ oben rechts wieder einschalten“), wurden angepasst.
- **Betroffene Komponenten:** `ui/HomeScreen.kt` (TopBar), `AppIcons.Pause`, `strings_home.xml`
- **Abhängigkeiten:** DEC-095

### DEC-168: Statuskachel mit festen Plätzen in jedem Zustand
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-03, endgültig 2026-11-04)
- **Kategorie:** UI
- **Entscheidung:** Die Statuskachel ändert ihre Höhe beim Zustandswechsel nicht:
  - Überschrift-Zeile („✓ Alles gesichert und aktuell“) mit einer Zeile Abstand ist immer reserviert, auch wenn sie leer ist.
  - WLAN-Zeile einzeilig.
  - Zustandsbereich mit Mindesthöhe (2 × bodyLarge + 2 × bodySmall + 24 dp); während des Laufs feste Zeilenzahl (Fortschritt 2 Zeilen, Dateiname 1 Zeile mit „…“).
  - Grösser wird die Kachel nur bei langen Fehlermeldungen.
- **Begründung:** Wunsch des Inhabers („kachel vergrössert und verkleinert sich“). 0.9.0 (nur Mindesthöhe) und 0.9.2 (feste Zeilen beim Lauf) reichten nicht; der Inhaber bestätigte 0.9.3 auf dem Gerät („kachelgrösse passt jetzt“).
- **Alternativen:** ganz feste Höhe mit gekürzten Meldungen (verworfen, DEC-199).
- **Konsequenzen:** Im Ruhezustand ohne Profil bleibt Leerraum in der Kachel.
- **Betroffene Komponenten:** `StatusCard` (heute `ui/HomeParts.kt`)
- **Abhängigkeiten:** DEC-088, DEC-199

### DEC-169: Einheitliche Begriffe in der Oberfläche
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-03)
- **Kategorie:** UI
- **Entscheidung:** Deutsch „Handy“ (nie „Telefon“) und „Freigabe“ (nie „Share“); Englisch „Mobile“ (nie „phone“) und „Share“ (grossgeschrieben). Filter heissen „Nur diese Dateien“ / „Diese Dateien ausschliessen“ ohne Whitelist/Blacklist.
- **Begründung:** Befund der UX-Prüfung; die Begriffe hat der Inhaber festgelegt.
- **Alternativen:** —
- **Konsequenzen:** Alle neuen Texte müssen diese Begriffe verwenden (im Memory als Regel festgehalten). Die Schlüsselnamen in `strings_*.xml` (z. B. `sides_phone_only`) blieben unverändert.
- **Betroffene Komponenten:** `res/values*/strings_*.xml`
- **Abhängigkeiten:** DEC-040, DEC-155, DEC-170

### DEC-170: „Sync“ in deutschen Texten ausschreiben
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-04)
- **Kategorie:** UI
- **Entscheidung:** In allen deutschen Texten steht „Synchronisierung“ bzw. „Synchronisieren“ statt „Sync“ (z. B. „Letzte Synchronisierung“, „Synchronisierungsstatus“). Ausgenommen sind App-Name und Titel (LeoSync).
- **Begründung:** Wunsch des Inhabers.
- **Alternativen:** auch Englisch ausschreiben (verworfen, DEC-198).
- **Konsequenzen:** Artikel und Endungen wechseln („der Sync“ → „die Synchronisierung“); Ersetzungen müssen als ganze Wendungen erfolgen.
- **Betroffene Komponenten:** `res/values-de/strings_*.xml`
- **Abhängigkeiten:** DEC-169, DEC-198

### DEC-174: Profil bearbeiten – Knöpfe unten, Löschen in „Wartung“, optionale Bereiche zugeklappt
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-03)
- **Kategorie:** UI
- **Entscheidung:**
  - „Speichern“/„Abbrechen“ stehen fest unten (bottomBar); das Häkchen oben bleibt.
  - „Profil löschen“ ist von oben rechts in den Bereich „Wartung“ gewandert (neben „Synchronisierungsstatus zurücksetzen“).
  - „Filter“, „Schutzbremse“ und „Wartung“ sind beim Bearbeiten zugeklappt, solange nichts eingestellt ist; im Assistenten normale Karten.
  - Assistent Schritt 1 ohne Überschrift „Allgemein“ und ohne „Profil aktiv“.
  - Bei „Endgültig löschen“ erscheint eine rote Warnung.
- **Begründung:** Befunde der UX-Prüfung (Löschen neben Speichern, Seite sehr lang); vom Inhaber angenommen.
- **Alternativen:** —
- **Konsequenzen:** `ScreenScaffold` hat `bottomBar`; neues `CollapsibleCard`.
- **Betroffene Komponenten:** `ui/ProfileScreen.kt`, `ui/Common.kt` (`ScreenScaffold`, `CollapsibleCard`)
- **Abhängigkeiten:** DEC-097, DEC-150, DEC-196

### DEC-175: Speicher freigeben – Vorschau als Hauptschritt
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-03; Hinweis 2026-11-04)
- **Kategorie:** UI
- **Entscheidung:** „Vorschau“ ist der Hauptknopf; der Knopf zum Entfernen ist umrandet und rot. Seit 0.9.5 erklärt ein Hinweis, dass bei Prüfstufe „Aus“ keine Fotos angeboten werden.
- **Begründung:** Befund der UX-Prüfung (die Voreinstellung traf 6037 Dateien/85 GB über einen grossen Hauptknopf); Hinweis aus dem Code-Review.
- **Alternativen:** —
- **Konsequenzen:** —
- **Betroffene Komponenten:** `ui/FreeSpaceScreen.kt`, `strings_free.xml`
- **Abhängigkeiten:** DEC-091, DEC-148, DEC-171

### DEC-176: Rubrik „Sprache & Sicherung“
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-03)
- **Kategorie:** UI
- **Entscheidung:** „App-Einstellungen“ heisst „Sprache & Sicherung“ und enthält (seit 0.9.1 in dieser Reihenfolge) Sprache, Länderordner (Heimatland) und die Sicherung (Export/Import), die vorher unter „Diagnose“ lag.
- **Begründung:** UX-Prüfung (ähnliche Menünamen, Sicherung schwer zu finden; wichtig für den Umzug auf die Play-Variante); Reihenfolge vom Inhaber gewünscht.
- **Alternativen:** —
- **Konsequenzen:** Doku und Testanleitung verweisen auf den neuen Ort.
- **Betroffene Komponenten:** `ui/SettingsScreen.kt` (`appPage`, `diagnosePage`), `strings_settings.xml`, `build.md`
- **Abhängigkeiten:** DEC-089, DEC-106, DEC-158

### DEC-177: Seite „App-Berechtigungen“ mit Status je Punkt
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-03)
- **Kategorie:** UI
- **Entscheidung:** Jede Berechtigung zeigt „✓ erledigt“ oder einen Knopf; dieselben Zeilen wie die Karte auf der Startseite (`PermissionRows`). Ein Knopf „App-Info öffnen“ bleibt.
- **Begründung:** Befund der UX-Prüfung (Knopf hiess wie die Seite, kein Status sichtbar).
- **Alternativen:** —
- **Konsequenzen:** Grundlage für die Berechtigungen bei Bedarf (DEC-190).
- **Betroffene Komponenten:** `ui/PermissionRows.kt`, `ui/SettingsScreen.kt`, `ui/HomeParts.kt`
- **Abhängigkeiten:** DEC-055, DEC-190

### DEC-178: Kleinere Vereinheitlichungen der Oberfläche
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-03)
- **Kategorie:** UI
- **Entscheidung:**
  - Verlauf: „OK“ nicht mehr auf jeder Karte, nur abweichende Zustände.
  - Statistik: Balken-Symbol oben (`AppIcons.BarChart`) statt Kalender; Beschriftung „Balkenlänge = Anzahl Dateien“.
  - Auswahlfelder (`ValueField`) mit transparentem Hintergrund, überall gleich.
  - Profilkarte: Handy- und PC-Ordner je eigene Zeile; „im WLAN ✓“ als neutrales Etikett.
  - Pause-Dialog: Dauer erst wählen (markiert), dann „Pausieren“.
  - Ohne Profil steht beim ausgegrauten „Synchronisieren“ der Grund.
- **Begründung:** Befunde der UX-Prüfung, vom Inhaber angenommen.
- **Alternativen:** —
- **Konsequenzen:** —
- **Betroffene Komponenten:** `ui/LogScreen.kt`, `ui/StatsScreen.kt`, `ui/AppIcons.kt`, `ui/TrashScreen.kt` (`ValueField`), `ui/HomeParts.kt`
- **Abhängigkeiten:** DEC-100, DEC-151, DEC-153

### DEC-186: Herunterziehen startet eine Synchronisierung
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-04; Tipp 2026-11-08)
- **Kategorie:** UI
- **Entscheidung:** Ganz oben auf der Startseite weiter nach unten ziehen startet eine Synchronisierung wie der Knopf (gleiche Regeln inkl. Ladegerät-Warnung); ohne Profil oder bei laufendem Auftrag passiert nichts. Seit 0.11.0 erscheint einmalig ein Tipp unter der Statuskachel (weg nach dem ersten Ziehen oder Antippen).
- **Begründung:** Wunsch des Inhabers (analog „Neu laden“ im Browser); Tipp aus der Design-Prüfung.
- **Alternativen:** —
- **Konsequenzen:** Neue Funktion → Reihe 0.10.0 (DEC-077). `PullToRefreshBox` ist experimentell (OptIn).
- **Betroffene Komponenten:** `ui/HomeScreen.kt` (`startSync`, `PullToRefreshBox`), `GlobalSettings.pullTipDone`
- **Abhängigkeiten:** DEC-077, DEC-105

### DEC-187: Zugangsdaten über einen Knopf mit zusammengefasster Liste übernehmen
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-04)
- **Kategorie:** UI
- **Entscheidung:** Statt eines Chips pro Profil gibt es einen Knopf „Zugangsdaten übernehmen…“ (immer sichtbar, sobald andere Profile Zugangsdaten haben). Die Liste fasst gleiche Zugangsdaten zusammen (Benutzer, Domäne, Server, Passwort) und nennt die Profile, die sie verwenden.
- **Begründung:** Hinweis des Inhabers („je mehr Profile, umso unübersichtlicher“); „immer“ sichtbar vom Inhaber gewählt.
- **Alternativen:** Knopf nur, solange kein Benutzer eingetragen ist (nicht gewählt).
- **Konsequenzen:** Passwörter werden für den Vergleich entschlüsselt (nur im Speicher).
- **Betroffene Komponenten:** `data/CredentialGroups.kt`, `ui/ProfileScreen.kt`, Test `CredentialGroupsTest`
- **Abhängigkeiten:** DEC-006 (ersetzt), DEC-004

### DEC-189: Verständliche Fehlermeldungen mit nächstem Schritt
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-08)
- **Kategorie:** UI
- **Entscheidung:** Häufige Fehler (falsches Passwort/Konto, Freigabe fehlt, Ordner fehlt, kein Zugriff, PC voll, Handy voll, Name unbekannt, PC nicht erreichbar) werden in klare Sätze übersetzt. Lauf-Meldungen (FAILED) und Anzeigen nutzen sie; der technische Text bleibt in den Log-Zeilen des Verlaufs und ist beim Verbindungstest und in der Ordnerauswahl aufklappbar.
- **Begründung:** Design-Prüfung (Heuristik „Fehler erkennen und beheben“); vom Inhaber angenommen.
- **Alternativen:** —
- **Konsequenzen:** Die Erkennung beruht auf Ausnahmetypen und SMB-Statusnamen im Text; unbekannte Fehler erscheinen weiter technisch.
- **Betroffene Komponenten:** `sync/Errors.kt` (`friendlyError`, `userError`), `SyncEngine`, `MirrorEngine`, `ui/Common.kt` (`ErrorDetails`), `ui/ProfileScreen.kt`, `ui/ProfileParts.kt`
- **Abhängigkeiten:** —

### DEC-191: Erfolgsmeldung nach jeder Synchronisierung bei Neuinstallation aus
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-08)
- **Kategorie:** Android
- **Entscheidung:** `notifySummary` ist für neue Installationen standardmässig aus; bestehende Installationen (erkannt an vorhandenen Einstellungen) behalten „an“. Hinweise und Fehler bleiben.
- **Begründung:** LeoSync arbeitet im Hintergrund; der Inhaber: „niemand mag zu viele Benachrichtigungen“.
- **Alternativen:** Standard für alle ändern (nicht gewählt).
- **Konsequenzen:** Die Erkennung „neu“ hängt davon ab, dass vor `applyInstallDefaults()` nichts in die Einstellungen schreibt.
- **Betroffene Komponenten:** `GlobalSettings.applyInstallDefaults`, `App.onCreate`
- **Abhängigkeiten:** DEC-108

### DEC-193: Screenreader – Statuswechsel ansagen, Richtungspfeile als Wort
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-08)
- **Kategorie:** UI
- **Entscheidung:** Die Statuszeile ist ein Live-Bereich (Start und Ende einer Synchronisierung werden angesagt); die Pfeile ⇄/→ der Profilkarte werden als Richtungstext vorgelesen.
- **Begründung:** Design-Prüfung (Barrierefreiheit), angenommen.
- **Alternativen:** —
- **Konsequenzen:** Mit TalkBack nicht geprüft.
- **Betroffene Komponenten:** `ui/HomeParts.kt` (`StatusCard`, `ProfileCard`)
- **Abhängigkeiten:** —

### DEC-194: Lange Hinweise mit „Mehr dazu“
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-08)
- **Kategorie:** UI
- **Entscheidung:** Hinweise über 120 Zeichen zeigen nur den ersten Satz, der Rest steht hinter „Mehr dazu“ (Profil- und Einstellungsseiten). Die Satzerkennung ignoriert Abkürzungen wie „z. B.“ und „e.g.“.
- **Begründung:** Inhaber: „Das UI soll aufgeräumt wirken.“
- **Alternativen:** —
- **Konsequenzen:** Wichtige Informationen im zweiten Satz sind zunächst verborgen.
- **Betroffene Komponenten:** `ui/Common.kt` (`HintMore`), `ui/ProfileScreen.kt`, `ui/ProfileCards.kt`, `ui/SettingsScreen.kt`
- **Abhängigkeiten:** —

### DEC-195: Geschätzte Restzeit ab einer Minute
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-08)
- **Kategorie:** UI
- **Entscheidung:** Während einer Synchronisierung zeigt die Statuskachel nach 60 Sekunden „noch ca. …“ vor dem Dateinamen, berechnet aus dem Tempo seit Beginn des Laufs.
- **Begründung:** Design-Prüfung (Sichtbarkeit des Systemzustands), angenommen.
- **Alternativen:** —
- **Konsequenzen:** Schätzung über den ganzen Lauf, nicht pro Datei; nicht im echten Lauf geprüft.
- **Betroffene Komponenten:** `ui/HomeScreen.kt`, `ui/HomeParts.kt` (`formatEta`)
- **Abhängigkeiten:** DEC-168

### DEC-196: Profil löschen mit „Rückgängig“
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-08)
- **Kategorie:** UI
- **Entscheidung:** Nach dem Löschen wird das Profil nur vorgemerkt (ausgeblendet, wird nicht synchronisiert). Die Startseite zeigt „Profil gelöscht – Rückgängig“; endgültig gelöscht wird, wenn der Hinweis verschwindet, spätestens nach 15 Sekunden. Wird die App vorher beendet, bleibt das Profil erhalten.
- **Begründung:** Design-Prüfung (Rückgängig statt nur Rückfrage), angenommen.
- **Alternativen:** nur die bisherige Rückfrage.
- **Konsequenzen:** Die Vormerkung ist nur im Speicher. Seit 0.11.1 sind die Konfliktkarten des Profils während der Frist ausgeblendet (`HomeScreen`, `ConflictScreen` filtern nach `PendingProfileDelete.pending`; nur Anzeige, Konflikte bleiben in `mirror_conflicts` gespeichert, „Alle: neuere Version“ wirkt nur auf sichtbare Konflikte). Die Benachrichtigung „Konflikte“ zählt weiterhin alle offenen Konflikte (`dao.openConflictCount()`).
- **Betroffene Komponenten:** `ui/PendingProfileDelete.kt`, `ui/HomeScreen.kt`, `ui/ProfileScreen.kt`, `sync/SyncWorker.kt`, `ScreenScaffold(snackbarHost)`
- **Abhängigkeiten:** DEC-174

### DEC-216: Bestätigung beim Ändern von Richtung oder «Löschungen übernehmen»
- **Status:** Implemented (Umsetzung in 0.14.7 laut Development, SYNC-20261110-115/-119; `data/ChangeConfirm.kt` und `ChangeConfirmTest` von Documentation geprüft, die UI-Verdrahtung nicht)
- **Datum:** 2026-11-10 (Freigabe Inhaber im UX-Chat, Quelle SYNC-20261110-099, -116)
- **Kategorie:** UI
- **Entscheidung:** Beim Speichern eines bestehenden Ordner-Profils mit geänderter Richtung oder neu eingeschaltetem «Löschungen übernehmen» erscheint der Dialog «Änderung bestätigen» mit alter und neuer Einstellung und der Folge; Schaltflächen nur «Abbrechen» und «Speichern». Ausschalten von «Löschungen übernehmen», neue Profile und Fotos-Profile lösen ihn nicht aus. Bei Einweg-Richtungen beschreibt der Text, dass abweichende Dateien der Zielseite ersetzt werden und die ersetzte Version in den Papierkorb kommen kann (nach der Planung der `MirrorEngine`). Die Bestätigung erscheint je Änderung nur einmal, auch wenn das Speichern wegen anderer Dialoge neu angestossen wird.
- **Begründung:** Diese Einstellungen verändern Löschverhalten und Synchronisierungsrichtung und wurden bisher ohne Rückfrage wirksam (am Bildschirm bestätigt); Schutz vor versehentlicher Änderung, Datenverlust hat höchste Priorität.
- **Alternativen:** (1) dauerhafte Warnkarte im Formular (verlängert das Formular, bleibt unbeachtet) – verworfen; (2) Schaltfläche «Zuerst simulieren» im Dialog (die Simulation prüft nur den gespeicherten Stand, daher irreführend) – verworfen.
- **Konsequenzen:** Neue Texte `profile_confirm_*` in beiden Sprachdateien. UI-Verdrahtung (Dialogfolge) ist nicht unit-getestet.
- **Betroffene Komponenten:** `data/ChangeConfirm.kt`, `ui/ProfileScreen.kt`, `strings_profile.xml`
- **Abhängigkeiten:** DEC-207 (Dialogmuster), SYNC-20261110-099, -115

### DEC-217: Zahlenfelder im Profilformular prüfen statt still korrigieren
- **Status:** Implemented (0.14.7 laut Development, SYNC-20261110-114/-115/-119; `data/NumberInput.kt` und `NumberInputTest` geprüft, die UI-Verdrahtung nicht)
- **Datum:** 2026-11-10 (Quelle SYNC-20261110-114, -115, -119)
- **Kategorie:** UI
- **Entscheidung:** Die Zahlenfelder des Profilformulars speichern keine Zwischenwerte und korrigieren nicht mehr still. Ein leerer oder ausserhalb des Bereichs liegender Wert zeigt einen Fehlertext unter dem Feld und blockiert das Speichern. Bereiche: Papierkorb-Tage 0–3650, Prüfintervall 15–1440 Minuten, Schutzbremse 1–100 % (nicht 0). Alle Felder nutzen eine einheitliche Bereichsmeldung («Bitte eine Zahl von X bis Y eingeben») statt eigener Texte je Feld.
- **Begründung:** Stilles Korrigieren verschleierte, welcher Wert tatsächlich gespeichert wird; bei der Schutzbremse wäre 0 gleichbedeutend mit «aus».
- **Alternativen:** — (in den Quellen nicht genannt).
- **Konsequenzen:** Neue Hilfsklasse `NumberInput`; Text `profile_number_range` in beiden Sprachdateien.
- **Aktualisierung (0.14.8, 2026-11-11, Quelle SYNC-20261110-120/-150):** Sperre statt stilles Korrigieren (Sollverhalten Inhaber 2026-11-10): Im Profil-Assistenten lässt «Weiter» einen Schritt mit leerem oder ungültigem Zahlenfeld nicht verlassen, «Speichern» springt zum ersten fehlerhaften Schritt. Dialogtexte im Profilformular scrollen bei grosser Systemschrift; der Filter-Fehlertext ist vereinfacht.
- **Betroffene Komponenten:** `data/NumberInput.kt`, `ui/ProfileCards.kt`, `ui/ProfileScreen.kt`, `strings_profile.xml`
- **Abhängigkeiten:** SYNC-20261110-114, -115

### DEC-218: Fotos-Ordner über «Downloads»: nur interner Speicher
- **Status:** Implemented (0.14.7 laut Development, SYNC-20261110-119; nicht im Code nachgeprüft)
- **Datum:** 2026-11-10 (Quelle SYNC-20261110-119)
- **Kategorie:** UI
- **Entscheidung:** Bei der Ordnerauswahl über «Downloads» akzeptiert ein Fotos-Profil nur `raw:`-Kennungen (interner Speicher). `msf:`-Kennungen bleiben abgelehnt; der Ablehnungsgrund erscheint als Text. Angezeigt wird der gewählte Ordner statt des Anzeigenamens (z. B. «Download/12»).
- **Begründung:** Von Development bei der Umsetzung festgelegt; eine Begründung nennt die Quelle nicht.
- **Alternativen:** `msf:`-Kennungen ebenfalls zulassen (bleibt abgelehnt).
- **Konsequenzen:** —
- **Betroffene Komponenten:** `mirror/LocalTree.kt`, `ui/ProfileScreen.kt`
- **Abhängigkeiten:** —


### DEC-224: Nachlauf-Hinweis: Snackbar und Zeile «Danach: …» in der Statuskarte
- **Status:** Implemented (0.15.0 laut Development, SYNC-20261111-146; UX-Review in SYNC-20261111-154 offen)
- **Datum:** 2026-11-11 (Freigabe Inhaber im UX-Chat; Quellen SYNC-20261111-145, -146, -147)
- **Kategorie:** UI
- **Entscheidung:** Ein vorgemerkter Start während eines laufenden Syncs wird beim manuellen Start per Snackbar bestätigt («Wird nach der laufenden Synchronisierung gestartet.») und als Zeile in der Statuskarte gezeigt, innerhalb der Mindesthöhe (die Karte springt nicht). Die Zeile lautet manuell «Danach: alle Profile», automatisch «Danach folgt eine weitere Synchronisierung.» (die Variante «Danach: <Profilname>» entfällt, es gibt keinen Start für ein einzelnes Profil). «Pausieren» und «Abbrechen» verwerfen den Nachlauf. Bei Start von anderer Seite (Benachrichtigung/WLAN-Auslöser) nur die Zeile, keine Snackbar.
- **Begründung:** Der Benutzer soll sehen, dass sein Start angenommen ist (vorher sah er nur «Sync läuft»).
- **Alternativen:** nur Snackbar (A1), nur Zeile (A2); gewählt A3 beides.
- **Konsequenzen:** Zeile im Live-Bereich (polite) für Screenreader; Texte DE/EN in `strings_home.xml`.
- **Betroffene Komponenten:** `ui/HomeScreen.kt`, `ui/HomeParts.kt`
- **Abhängigkeiten:** DEC-221

### DEC-225: Karte «Verschlüsselung» im Profilformular
- **Status:** Implemented (0.15.0 laut Development, SYNC-20261111-146; UX-Review in SYNC-20261111-154 offen; Hilfeabschnitt siehe Hilfe, Anker mit Development abzustimmen)
- **Datum:** 2026-11-11 (Freigabe Inhaber im UX-Chat; Quellen SYNC-20261111-145, -146, -147)
- **Kategorie:** UI
- **Entscheidung:** Eigene Karte «Verschlüsselung» direkt unter «PC-Kennung», standardmässig eingeklappt (Begründung des Inhabers: betrifft die meisten Benutzer nicht); eingeklappt zeigt die Kopfzeile den Wert («Verschlüsselung · Wenn möglich»). Drei Optionen als Radio-Liste mit Erklärsatz (Aus / Wenn möglich (empfohlen) / Erforderlich). Kein zusätzlicher Bestätigungsdialog beim Wechsel auf «Aus» (der Optionstext warnt); die Wahl wirkt wie die übrigen Felder erst mit «Speichern». «Verbindung testen» zeigt eine eigene Zeile zu Verschlüsselung und SMB-Version; Hilfeverweis statt Inline-Anleitung; kein Element «Zertifikatsprüfung ignorieren» (bei SMB gibt es keine Zertifikate).
- **Begründung:** Wunsch des Inhabers; Bedienmuster wie bei «PC-Kennung».
- **Alternativen:** unter «Erweitert» (B), Dropdown in «Verbindung» (C); gewählt A (eigene Karte).
- **Konsequenzen:** `CollapsibleCard` um `titleSuffix` und `openSignal` erweitert; Hilfeknopf öffnet bis zum Hilfeabschnitt `HELP_URL`.
- **Betroffene Komponenten:** `ui/ProfileScreen.kt`, `ui/ProfileParts.kt`, `ui/Common.kt`, `strings_profile.xml`
- **Abhängigkeiten:** DEC-222, DEC-204


### DEC-226: Verlauf löschen mit Bestätigung und «Rückgängig» (Variante A)
- **Status:** Implemented (0.15.1 laut Development, SYNC-20261111-162; QA-/UX-Prüfung offen, SYNC-20261111-160/-161)
- **Datum:** 2026-11-11 (Quelle SYNC-20261110-139, -162)
- **Kategorie:** UI
- **Entscheidung:** Verlauf löschen verlangt eine Bestätigung und bietet danach «Rückgängig». Die Einträge werden im Speicher zurückgehalten, bis die Snackbar abläuft.
- **Begründung:** Freigabe «Variante A» (Dialog + Rückgängig) in SYNC-20261110-139; weitere Begründung nennt die Quelle nicht.
- **Alternativen:** andere Varianten laut SYNC-20261110-139 (hier nicht übernommen).
- **Konsequenzen:** Erst nach Ablauf der Snackbar wird endgültig gelöscht.
- **Betroffene Komponenten:** ui/LogScreen.kt, SyncDao.kt (4 neue Abfragen, kein Schema)
- **Abhängigkeiten:** —

### DEC-227: Papierkorb: nur Texte und Uhrzeit (Variante A)
- **Status:** Implemented (0.15.1 laut Development, SYNC-20261111-162; QA-/UX-Prüfung offen, SYNC-20261111-160/-161)
- **Datum:** 2026-11-11 (Quelle SYNC-20261110-139, -162)
- **Kategorie:** UI
- **Entscheidung:** Der Papierkorb erhält neue Beschriftungen («Papierkorb aufräumen…», «Neueste Version je Datei wählen», «Auswahl aufheben»), Datum mit Uhrzeit je Eintrag, den Dateinamen im Löschdialog bei einer Datei sowie ein Aufräumen-Sheet mit fester Fusszeile und Tage ≥ 1. Keine weiteren Funktionsänderungen.
- **Begründung:** Freigabe «Variante A» (nur Texte und Uhrzeit) in SYNC-20261110-139; weitere Begründung nennt die Quelle nicht.
- **Alternativen:** andere Varianten laut SYNC-20261110-139 (hier nicht übernommen).
- **Konsequenzen:** —
- **Betroffene Komponenten:** ui/TrashScreen.kt
- **Abhängigkeiten:** —

### DEC-228: Import «Ersetzen» in Warnfarbe, ohne zusätzliche Bestätigung (Variante A)
- **Status:** Implemented (0.15.1 laut Development, SYNC-20261111-162; QA-/UX-Prüfung offen, SYNC-20261111-160/-161)
- **Datum:** 2026-11-11 (Quelle SYNC-20261110-134, -162)
- **Kategorie:** UI
- **Entscheidung:** Im Import-Dialog ist «Ersetzen» als destruktive Aktion in Warnfarbe hervorgehoben, ohne zusätzliche Bestätigung. Die Knöpfe brechen bei grosser Schrift um; ein Hinweis nennt, dass die App-Sprache nicht zur Sicherung gehört.
- **Begründung:** Freigabe «Variante A» laut SYNC-20261110-134; weitere Begründung nennt die Quelle nicht.
- **Alternativen:** zusätzliche Bestätigung (nicht gewählt).
- **Konsequenzen:** —
- **Betroffene Komponenten:** ui/BackupSection.kt
- **Abhängigkeiten:** DEC-230

### DEC-229: Update: Hinweis statt automatischem Neustart
- **Status:** Implemented (0.15.1 laut Development, SYNC-20261111-162; QA-/UX-Prüfung offen, SYNC-20261111-160/-161)
- **Datum:** 2026-11-11 (Quelle SYNC-20261110-140, -162)
- **Kategorie:** UI
- **Entscheidung:** Nach der Installation eines Updates zeigt die Update-Seite den Hinweis «Danach LeoSync wieder öffnen», statt die App automatisch neu zu starten.
- **Begründung:** Die Paketinstallation beendet die App, ein Neustart wäre nicht zuverlässig.
- **Alternativen:** automatischer Neustart (verworfen, nicht zuverlässig).
- **Konsequenzen:** Weitere Änderungen der Update-Seite (Offline-Meldung mit «Erneut prüfen», «Jetzt prüfen», Changelog ohne Markdown mit Link-Knopf, Fortschritt in Prozent/MB mit «Abbrechen») siehe PATCHNOTES 0.15.1.
- **Betroffene Komponenten:** ui/UpdateScreen.kt, ReleaseNotes.kt
- **Abhängigkeiten:** —

### DEC-230: Die App-Sprache bleibt ausserhalb der Sicherung
- **Status:** Implemented (0.15.1 laut Development, SYNC-20261111-162; QA-/UX-Prüfung offen, SYNC-20261111-160/-161)
- **Datum:** 2026-11-11 (Quelle SYNC-20261110-137, -162)
- **Kategorie:** UI
- **Entscheidung:** Die App-Sprache gehört nicht zur Sicherung (Dateiformat unverändert); der Import-Dialog weist darauf hin.
- **Begründung:** Das Dateiformat bleibt unverändert (SYNC-20261111-162); weitere Begründung nennt die Quelle nicht.
- **Alternativen:** — (in der Quelle nicht genannt).
- **Konsequenzen:** Nach Neuinstallation/Import bleibt die Sprache wie auf dem Gerät eingestellt.
- **Betroffene Komponenten:** ackup/BackupCodec.kt (unverändert), ui/BackupSection.kt
- **Abhängigkeiten:** DEC-228

### DEC-235: Überschneidungswarnung Fotos ↔ Ordner: datenträgerunabhängig, Speicherwurzel warnt
- **Status:** Implemented (0.16.0 laut Development, SYNC-20261111-172)
- **Datum:** 2026-11-11 (Quelle SYNC-20261111-156 Punkt 5, -172)
- **Kategorie:** UI / Profile
- **Entscheidung:** `phoneOverlap` vergleicht Handy-Pfade datenträgerunabhängig (interner Speicher, SD-Karte). Ein Ordner-Profil auf der Speicherwurzel warnt. Reine Warnung, kein Schema.
- **Begründung:** Fotoprofile erfassen über `VOLUME_EXTERNAL` gleiche relative Pfade auf allen Datenträgern (SYNC-20261111-172, Technischer Kontext).
- **Alternativen:** — (in der Quelle nicht genannt).
- **Konsequenzen:** Restrisiko: Kollision zweier gleichnamiger Dateien auf zwei Datenträgern; das Löschen bleibt durch die volle Hash-Prüfung geschützt.
- **Betroffene Komponenten:** `data/ProfileOverlap.kt`
- **Abhängigkeiten:** DEC-219, DEC-134

### DEC-241: Statuskarte: neuer Zustand «↻ Noch nicht alles gesichert»
- **Status:** Implemented (0.16.1 laut Development, SYNC-20261111-177; UX-Review offen, SYNC-20261111-181)
- **Datum:** 2026-11-11 (Quelle SYNC-20261111-174, -177 UX-Specialist; -182)
- **Kategorie:** UI / Startseite
- **Entscheidung:** Hat der letzte Lauf (OK) N ≥ 1 ausstehende Dateien (`SyncRun.deferred`), steht die Überschrift «↻ Noch nicht alles gesichert» (EN «↻ Not everything backed up yet»; Symbol als Text, normale Textfarbe, nicht ⚠/✓), der Hund sitzt (`DogState.WAITING`), und der Hinweis nennt die Zahl («N Dateien wurden während des Laufs geändert und folgen beim nächsten Sync.»). Ist kein Folgelauf geplant, entfällt die Zeile «Nächste Prüfung», und der Hinweis lautet «… folgen mit der nächsten Prüfung.». Rangfolge der Überschrift: läuft > nichts eingerichtet > wartet auf dich > pausiert > keine Verbindung > Fehler > ausstehende Dateien > gesichert > neutral. Der Verlauf zeigt «· N folgen später», der Lauf bleibt OK. Kehrt ein Lauf ohne ausstehende Dateien zurück, erscheint wieder «✓ Alles gesichert und aktuell».
- **Begründung:** Die Startseite soll ehrlich und ohne Fehleranmutung zeigen, dass nicht alles gesichert ist; es wird nichts von der Person verlangt (daher sitzender Hund).
- **Alternativen:** — (in der Quelle nicht genannt).
- **Konsequenzen:** Zahl aus `pendingDeferredFlow` (letzter echter, erfolgreicher Lauf je aktivem Profil). Abweichung zur Vorgabe: «Folgelauf geplant» wird an der bestehenden Zeile «Nächste Prüfung» erkannt, nicht an einem eigenen Merker. **Ergänzung 0.16.6 (SYNC-20261111-188/-192):** Ein nicht ausgeführter Start (z. B. PC/WLAN nicht verfügbar) zählt als «Keine Verbindung» und steht vor «ausstehende Dateien»; der manuelle Start heisst dann «Start nicht möglich» statt «Automatischer Start übersprungen». «Alles gesichert» steht nur, wenn jedes aktive Profil schon einmal erfolgreich von Hand gestartet wurde. Rangfolge: läuft > nichts eingerichtet > wartet auf dich > pausiert > keine Verbindung > Fehler > Start nicht ausgeführt (als «Keine Verbindung») > ausstehende Dateien > gesichert > neutral. Die Zeile «WLAN» aktualisiert sich bei Netzwechsel. **Ergänzung 0.16.7 (SYNC-20261111-193):** Ein nicht ausgeführter Start (`recentSkip`) mit Queue (`pendingFiles > 0` oder fälliger Sync-Auftrag) zählt als «Keine Verbindung» (sitzender Hund auf blauem Kreis) und steht **vor** «Fehler»; bisher erschien bei fehlgeschlagenem letztem Lauf der rote Fehler-Hund. Ohne Queue bleibt der Fehler sichtbar und die Reihenfolge wie in 0.16.6. Ist der WLAN-Name nicht lesbar (kein Profil mit WLAN-Erkennung oder Standort-Berechtigung fehlt), bleibt die Zeile «WLAN» leer (gleiche Höhe) statt «WLAN: nicht verbunden / nicht lesbar». UX-Review in SYNC-20261111-194.
- **Betroffene Komponenten:** `ui/HomeStatus.kt`, `ui/HomeParts.kt`, `ui/HomeScreen.kt`
- **Abhängigkeiten:** DEC-238

### DEC-242: Startseite: Hinweis bei fehlender Standort-Berechtigung (R4)
- **Status:** Implemented (0.16.1 laut Development, SYNC-20261111-176 R4; UX-Review offen, SYNC-20261111-181)
- **Datum:** 2026-11-11 (Quelle SYNC-20261111-176 UX-Specialist, -182)
- **Kategorie:** UI / Startseite
- **Entscheidung:** Fehlt die Standort-Berechtigung, zeigt die Statuskarte «⚠ Standort-Berechtigung fehlt – der WLAN-Name ist nicht lesbar.» mit dem Textknopf «Berechtigungen» (öffnet Einstellungen → App-Berechtigungen). Die Überschrift bleibt neutral und nie «Alles gesichert» (Rangfolge nach «Fehler»).
- **Begründung:** Nach Entzug der Berechtigung stand «✓ Alles gesichert und aktuell» ohne Hinweis und ohne Weg zur Lösung, obwohl die automatische Synchronisierung im Heim-WLAN still gestört ist (UX-Review SYNC-20261111-149/-154/-161).
- **Alternativen:** — (in der Quelle nicht genannt).
- **Konsequenzen:** Reine Anzeige, keine Sicherheitsabfrage entfällt. Ergänzung 0.16.6 (SYNC-20261111-192): Der Hinweis erscheint nur, wenn ein Profil den WLAN-Namen braucht (Erkennungsart WLAN oder Beides mit Namen).
- **Betroffene Komponenten:** `ui/HomeStatus.kt`, `ui/HomeParts.kt`, `ui/HomeScreen.kt`
- **Abhängigkeiten:** DEC-241

### DEC-243: Profilkarte: Zeile bei verlorenem Handy-Ordner-Zugriff (R5)
- **Status:** Implemented (0.16.1 laut Development, SYNC-20261111-176 R5; UX-Review offen, SYNC-20261111-181)
- **Datum:** 2026-11-11 (Quelle SYNC-20261111-176, -182; der Inhaber hat der Richtung zugestimmt)
- **Kategorie:** UI / Startseite
- **Entscheidung:** Fehlt der Zugriff auf den Handy-Ordner, zeigt die Profilkarte «⚠ Handy-Ordner: Zugriff verloren – tippen und neu wählen» (Fehlerfarbe plus Symbol); Tippen öffnet das Profilformular. Geprüft wird `LocalTree.hasPermission` auf der Profilkarte.
- **Begründung:** Nach einem Import zeigte die Karte «0 Dateien synchronisiert / Wartet auf den ersten Start», obwohl der Zugriff fehlte; «Zugriff verloren» stand nur im Profilformular.
- **Alternativen:** — (in der Quelle nicht genannt).
- **Konsequenzen:** Reine Anzeige.
- **Betroffene Komponenten:** `ui/HomeParts.kt`, `ui/HomeScreen.kt`, `mirror/LocalTree.kt`
- **Abhängigkeiten:** —

### DEC-244: Startseite: «⏸ Pausiert» als Überschrift (R6)
- **Status:** Implemented (0.16.1 laut Development, SYNC-20261111-176 R6; UX-Review offen, SYNC-20261111-181)
- **Datum:** 2026-11-11 (Quelle SYNC-20261111-154/-176 UX-Specialist, -182)
- **Kategorie:** UI / Startseite
- **Entscheidung:** Der Pausiert-Zustand hat die Überschrift «⏸ Pausiert» (EN «⏸ Paused»; titleSmall, Symbol plus Text) und darunter die Detailzeile «Bis 06:23.» bzw. bei «für immer» «Nur manuell synchronisieren.» (EN «Until 06:23.» / «Manual sync only.»), statt «⏸ Pausiert bis 06:23».
- **Begründung:** Gestaltungsfrage aus dem UX-Review SYNC-20261111-154, entschieden; gleiches Muster wie «✓ Alles gesichert» und «⚠ Keine Verbindung».
- **Alternativen:** — (in der Quelle nicht genannt).
- **Konsequenzen:** —
- **Betroffene Komponenten:** `ui/HomeStatus.kt`, `ui/HomeParts.kt`
- **Abhängigkeiten:** DEC-241

### DEC-245: «Keine Verbindung»: «PC nicht erreichbar» ohne doppelte Zeitangabe (R7)
- **Status:** Implemented (0.16.1 laut Development, SYNC-20261111-176 R7; UX-Review offen, SYNC-20261111-181)
- **Datum:** 2026-11-11 (Quelle SYNC-20261111-176, -182)
- **Kategorie:** UI / Startseite
- **Entscheidung:** Der Fehlertext lautet nur «PC nicht erreichbar (Folder)»; die Zeit steht allein in der Zeile «Nächste Prüfung um HH:MM».
- **Begründung:** Die Angabe «nächste Prüfung um 05:08» stand vorher doppelt (im Fehlertext und in der Zeile darunter).
- **Alternativen:** Die Zeile statt des Fehlertexts entfernen (in der Quelle nicht gewählt; vorgeschlagen war das Kürzen des Fehlertexts).
- **Konsequenzen:** —
- **Betroffene Komponenten:** `ui/HomeStatus.kt`, `ui/HomeParts.kt`
- **Abhängigkeiten:** —

### DEC-246: Dialoge mit drei oder mehr Knöpfen sind gestapelt
- **Status:** Implemented (0.16.3 laut Development, SYNC-20261111-176 R1; UX-Review offen, SYNC-20261111-184)
- **Datum:** 2026-11-11 (Quelle SYNC-20261111-176 UX-Specialist, -186)
- **Kategorie:** UI / Dialoge
- **Entscheidung:** Dialoge mit drei oder mehr Knöpfen zeigen die Knöpfe gestapelt, volle Breite, einzeilig, von oben: bestätigende Aktion zuerst, Abbrechen zuletzt (z. B. «Speichern», «Verwerfen», «Abbrechen»). Der Import-Dialog behält seine Reihenfolge Abbrechen, Hinzufügen, Ersetzen (Ersetzen in Warnfarbe, DEC-228).
- **Begründung:** Bei Schrift 2,0 brach «Verwerfen» mitten im Wort um, «Speichern» stand rechts oben, «Abbrechen» und «Verwerfen» nebeneinander (UX-Review).
- **Alternativen:** — (in der Quelle nicht genannt).
- **Konsequenzen:** Bausteine `DialogButtonColumn`/`StackedButton` in `ui/Common.kt`; betroffen u. a. «Änderungen speichern?», «Profil angelegt», Ordnerwechsel-Warnung (vier Knöpfe, jetzt volle Breite), Import-Dialog.
- **Betroffene Komponenten:** `ui/Common.kt`, `ui/ProfileScreen.kt`, `ui/BackupSection.kt`
- **Abhängigkeiten:** DEC-228

### DEC-247: Überlaufender Dialogtext zeigt Scrollbalken und Verlauf
- **Status:** Implemented (0.16.3 laut Development, SYNC-20261111-176 R2; UX-Review offen, SYNC-20261111-184)
- **Datum:** 2026-11-11 (Quelle SYNC-20261111-176 UX-Specialist, -186)
- **Kategorie:** UI / Dialoge
- **Entscheidung:** Läuft der Text eines Dialogs über, zeigt er einen Scrollbalken und einen Verlauf am unteren Rand (`DialogScroll` in `ui/Common.kt`), auch in den Dialogen «Änderung bestätigen», «Alle: neuere» und «Alle übernehmen».
- **Begründung:** Der Text scrollte bei Schrift 2,0, aber nichts zeigte es an; der wichtigste Satz stand ganz unten, mitten im Satz abgeschnitten.
- **Alternativen:** — (in der Quelle nicht genannt).
- **Konsequenzen:** —
- **Betroffene Komponenten:** `ui/Common.kt`, `ui/ConflictScreen.kt`, `ui/DecisionScreen.kt`
- **Abhängigkeiten:** DEC-246

### DEC-248: Konfliktkarte: «(neuer)» erst ab einer Sekunde Unterschied, sonst «(gleich alt)»
- **Status:** Implemented (0.16.3 laut Development, SYNC-20261111-176 R11; UX-Review offen, SYNC-20261111-184)
- **Datum:** 2026-11-11 (Quelle SYNC-20261111-176 UX-Specialist, -186)
- **Kategorie:** UI / Konflikte
- **Entscheidung:** «(neuer)» steht nur bei einem Zeitunterschied von mindestens einer Sekunde, sonst «(gleich alt)» (Text `conflict_same_age`). Die Entscheidung «Alle: neuere» bei Gleichstand bleibt unverändert: Handy.
- **Begründung:** «(neuer)» erschien auch bei gleichem Zeitstempel (beide 05:28:40), damit sah «Alle: neuere» scheinbar begründet aus.
- **Alternativen:** — (in der Quelle nicht genannt).
- **Konsequenzen:** —
- **Betroffene Komponenten:** `ui/ConflictScreen.kt`
- **Abhängigkeiten:** DEC-037

### DEC-249: Wartende Dateien nur lokal gezählt; leere WLAN-Zeile bleibt als Leerzeile
- **Status:** Implemented (0.16.8 laut Development)
- **Datum:** 2026-11-11 (Quelle SYNC-20261111-199, -201)
- **Kategorie:** UI / Startseite
- **Entscheidung:** Die Zahl wartender Dateien wird nur lokal ermittelt (kein Netzwerk) und auf der Statuskarte nur bei «Keine Verbindung» genannt. Die Profilkarte eines Ordner-Profils zeigt zusätzlich «· N offen» (neue oder seit dem letzten Abgleich geänderte Dateien im Handy-Ordner; bei «PC → Handy» kein Zähler; Fotos-Profile unverändert). Die leere WLAN-Zeile bleibt als Leerzeile stehen.
- **Begründung:** Ohne Verbindung ist ein Netzwerkabgleich nicht möglich. Die Karte behält eine feste Position des übrigen Inhalts (Benutzerauftrag 0.16.7).
- **Alternativen:** Die leere WLAN-Zeile weglassen (Vorschlag UX): verworfen, weil der Benutzer gleichbleibende Position wünscht.
- **Konsequenzen:** Die Zahl «offen» ist eine lokale Schätzung, kein Abgleich mit dem PC.
- **Betroffene Komponenten:** Startseite (Statuskarte), Profilkarte
- **Abhängigkeiten:** DEC-241

### DEC-250: Icon-Stil «Variante C Duotone» statt Emojis
- **Status:** Implemented (0.16.12 laut Development)
- **Datum:** 2026-11-11 (Quelle SYNC-20261111-195, Entscheidung des Benutzers)
- **Kategorie:** UI / Designsystem
- **Entscheidung:** Symbole und Emojis in der Oberfläche sind durch eigene Icons im Stil «Variante C Duotone» ersetzt (Hell und Dunkel). Die Umsetzung läuft über eine zentrale Textkomponente. Bewusst Text geblieben: → in Beschriftungen, ⏭, ⤼, ≤. Benachrichtigungen und Diagnose-Export bleiben Text.
- **Begründung:** — (in der Quelle nicht genannt; Auswahl des Benutzers aus den Varianten).
- **Alternativen:** Weitere Varianten in der Designvorschau (SYNC-20261111-195).
- **Konsequenzen:** Die Hilfe nennt teils noch Emoji-Symbole im Text (z. B. «⏸», «↻»).
- **Betroffene Komponenten:** UI (zentrale Textkomponente)
- **Abhängigkeiten:** —

### DEC-251: Geänderte Handy-Datei beim Upload wird zurückgestellt statt als Fehler gezählt
- **Status:** Implemented (0.16.13 laut Development)
- **Datum:** 2026-11-11 (Quelle SYNC-20261111-202)
- **Kategorie:** Sync / Übertragung
- **Entscheidung:** Ändert oder wächst eine Handy-Datei vor oder während des Uploads, gilt sie als «folgt später» (`deferred`) statt als Fehler oder «Teilweise». Die `.s2ptmp` wird gelöscht, kein Sync-Stand für die abgebrochene Version, Folgelauf nach 5 Minuten (DEC-238). Am Laufende plant ein Änderungsstempel-Vergleich der Ordner einen Folgelauf für nach dem Einlesen neu entstandene Dateien (0.16.14, SYNC-20261111-213, Entscheidung des Benutzers).
- **Begründung:** Eine sich ändernde Quelldatei ist kein Fehler; auf dem PC soll nie eine halbe Datei bleiben.
- **Alternativen:** — (in der Quelle nicht genannt).
- **Konsequenzen:** Siehe SYNC-ENGINE 6.7. QA-Prüfung des Folgelaufs offen (SYNC-20261111-214).
- **Betroffene Komponenten:** `MirrorEngine`, `SmbSession.upload`, `mirror/UploadVerdict.kt`, `SyncWorker`
- **Abhängigkeiten:** DEC-238

### DEC-252: Englisch nennt das Gerät «Phone» statt «Mobile»
- **Status:** Implemented (0.16.16 laut Development, SYNC-20261111-215)
- **Datum:** 2026-11-11 (Benutzerauftrag)
- **Kategorie:** UI / Lokalisierung
- **Entscheidung:** In den englischen App-Texten und der englischen Hilfe heisst das Gerät «Phone». «Mobile data» und «mobile plan» (Mobilfunk) bleiben. Das ersetzt die frühere Vorgabe «Mobile».
- **Begründung:** — (in der Quelle nicht genannt; Benutzerwunsch).
- **Alternativen:** — (in der Quelle nicht genannt).
- **Konsequenzen:** `CLAUDE.md` §1 und die Memory «LeoSync UI-Begriffe» nennen für EN noch «Mobile»; eine Änderung am Regelwerk braucht den ausdrücklichen Auftrag des Benutzers.
- **Betroffene Komponenten:** `res/values/strings_*.xml`, `docs/hilfe/help-en.md`
- **Abhängigkeiten:** DEC-155

### DEC-253: Deutscher Begriff «Abgleichstand» statt «Synchronisierungsstatus»
- **Status:** Implemented (0.16.17 laut Development, SYNC-20261111-218)
- **Datum:** 2026-11-11
- **Kategorie:** UI / Lokalisierung
- **Entscheidung:** Der deutsche UI-Begriff «Synchronisierungsstatus» heisst «Abgleichstand» (Profil-Wartung, Profil löschen, Sicherung/Import). Englisch bleibt «sync status». Die App bleibt zudem bei Konfigurationsänderungen (Schriftgrösse, Hell/Dunkel) auf dem aktuellen Bildschirm; der Assistent «Neues Profil» behält Eingaben ausser dem Passwort.
- **Begründung:** — (in der Quelle nicht genannt).
- **Alternativen:** — (in der Quelle nicht genannt).
- **Konsequenzen:** Ergänzt DEC-155 (Wortwahl «Synchronisierung»); ältere Einträge nennen weiter «Synchronisierungsstatus».
- **Betroffene Komponenten:** `res/values-de/strings_*.xml`, Profil-Assistent
- **Abhängigkeiten:** DEC-155

### DEC-254: Handy-Papierkorb heisst bei `msd:`-Ordnern «@LeoSync-Bin»
- **Status:** Implemented (0.16.25 laut Development, SYNC-20261111-229)
- **Datum:** 2026-11-11
- **Kategorie:** Sync / Papierkorb
- **Entscheidung:** Bei Handy-Ordnern des Download-Anbieters mit Nummern-Kennung (`msd:`) heisst der Papierkorb «@LeoSync-Bin» statt «.LeoSync-Bin». Der PC-Papierkorb bleibt «.LeoSync-Bin».
- **Begründung:** MediaStore indexiert Ordner mit Punkt am Anfang nicht, sie fehlen in der Ordnerliste. Folge: Der Papierkorb liess sich nicht auflisten und zersplitterte in «(1)», «(2)».
- **Alternativen:** Name ohne Sonderzeichen («LeoSync-Bin»): verworfen; «@» sortiert oben und wird nicht gefiltert.
- **Konsequenzen:** Der Papierkorbname hängt von der Ordnerart ab; sichtbar in der Download-App. Siehe SYNC-ENGINE 7.5. **Teilweise überholt durch DEC-260** (Papierkorb heisst seit 0.16.33 überall `@LeoSync-Bin`); Galerie-Wirkung: DEC-263.
- **Betroffene Komponenten:** `mirror/MirrorEngine.kt`, `mirror/FileFilter.kt`, `mirror/LocalTree.kt`
- **Abhängigkeiten:** DEC-094

### DEC-255: Verschlüsselungs-Icon zeigt den tatsächlichen Zustand der letzten Anmeldung
- **Status:** Implemented (0.16.20 laut Development, SYNC-20261111-225)
- **Datum:** 2026-11-11
- **Kategorie:** UI / Sicherheit
- **Entscheidung:** Die Profilkachel zeigt oben rechts ein Schild-mit-Schloss-Icon, wenn die letzte Anmeldung tatsächlich SMB-verschlüsselt war, nicht bei der blossen Einstellung «Wenn möglich». Der Zustand wird in den SharedPreferences «enc_status» gespeichert (`smb/EncryptionStatus.kt`), ohne DB-Schema. Nach Änderung von Einstellung, Freigabe oder Unterordner gibt es bis zur nächsten Verbindung kein Icon.
- **Begründung:** — (in der Quelle nicht genannt).
- **Alternativen:** — (in der Quelle nicht genannt).
- **Konsequenzen:** `enc_status` gehört nicht zum Datenmodell der Datenbank und ist hier, nicht in `DATA-MODEL.md`, festgehalten.
- **Betroffene Komponenten:** `smb/EncryptionStatus.kt`, `smb/SmbSession.kt`, `ui/HomeParts.kt`, `ui/Common.kt`
- **Abhängigkeiten:** DEC-222

### DEC-256: Datum und Uhrzeit folgen dem Systemformat des Handys
- **Status:** Implemented (0.16.27 laut Development, SYNC-20261111-233)
- **Datum:** 2026-11-11 (Entscheidung des Benutzers im Chat Development)
- **Kategorie:** UI / Lokalisierung
- **Entscheidung:** Alle Anzeigen von Datum und Uhrzeit folgen den Systemeinstellungen des Handys (Region/Sprache des Systems, 12 h/24 h), nicht der App-Sprache; zentrale Hilfe `l10n/TimeFormat.kt`. Weicht die App-Sprache vom System ab, kann das Datum nach System und der Text nach App-Sprache erscheinen; das ist gewollt und kein Fehler. Bewusst feste Formate: Backup-Dateiname, Diagnose/Logs/Crash, Papierkorb-Tagesordner und Uhrzeit-Anhang, EXIF-Parsing. Ohne Android (Unit-Tests) gilt `yyyy-MM-dd HH:mm`. Die Texte `fmt_date_time`/`fmt_day_month` entfallen.
- **Begründung:** Der Benutzer soll Zeit und Datum in dem Format sehen, das er gewohnt ist (Beispiel: System mit US-Format und AM/PM, App auf Deutsch).
- **Alternativen:** — (in der Quelle nicht genannt).
- **Konsequenzen:** Mischung aus System-Datumsformat und App-Sprache ist möglich.
- **Betroffene Komponenten:** `l10n/TimeFormat.kt`, `ui/Common.kt`
- **Abhängigkeiten:** DEC-155

### DEC-257: «Letzte Synchronisierung» ist der letzte fehlerfreie Lauf
- **Status:** Implemented (0.16.27 laut Development, SYNC-20261111-234)
- **Datum:** 2026-11-11 (Entscheidung des Benutzers, PM-Annahme bestätigt)
- **Kategorie:** UI / Startseite
- **Entscheidung:** Die Startseite zeigt «Letzte Synchronisierung: Zeit (Profil)» für den letzten Lauf mit Status OK und 0 Fehlern (kein Vorschau-Lauf, `SyncDao.lastCleanRunFlow`), nicht den letzten Versuch. Läufe mit Fehlern, Teilerfolg oder Abbruch ändern die Zeit nicht. Ein fehlerfreier Lauf, bei dem nichts zu übertragen war, zählt. Leerzustand: «Noch keine erfolgreiche Synchronisierung.» (Vorschlag Development, UX prüft).
- **Begründung:** Die Zeit soll sagen, wann zuletzt wirklich gesichert wurde, nicht wann zuletzt etwas versucht wurde.
- **Alternativen:** — (in der Quelle nicht genannt).
- **Konsequenzen:** Quelle ist die Datenbank, kein Schema geändert; «Alles aktuell» unverändert.
- **Betroffene Komponenten:** `data/SyncDao.kt`, `ui/HomeParts.kt`, `ui/HomeScreen.kt`
- **Abhängigkeiten:** DEC-241

### DEC-258: Profilnamen sind einmalig; Import «Hinzufügen» prüft sie bewusst nicht
- **Status:** Implemented (0.16.28 laut Development, SYNC-20261111-235)
- **Datum:** 2026-11-11 (Entscheidung des Benutzers)
- **Kategorie:** Profile / Eingabe
- **Entscheidung:** Profilnamen sind einmalig: Gross-/Kleinschreibung wird ignoriert, Leer-, Steuer- und unsichtbare Zeichen am Rand werden entfernt. Geprüft wird im Assistenten bei «Weiter» (Namensschritt), beim Speichern und beim Bearbeiten (das eigene Profil ausgenommen). Bestehende Doppelnamen werden nicht umbenannt, beim Bearbeiten am Namensfeld gemeldet; Speichern anderer Änderungen bleibt möglich, das Umbenennen auf einen belegten Namen ist gesperrt. Kein Unique-Index, kein Schema. Der Import «Hinzufügen» prüft Namen bewusst nicht (Doppelnamen möglich, Meldung erst beim Bearbeiten).
- **Begründung:** — (in der Quelle nicht genannt).
- **Alternativen:** Unique-Index: nicht gewählt (kein Schema).
- **Konsequenzen:** Erweitert DEC-173. Logik in `data/ProfileNames.kt`, Test `ProfileNamesTest`.
- **Betroffene Komponenten:** `data/ProfileNames.kt`, `data/ProfileWizard.kt`, `ui/ProfileScreen.kt`
- **Abhängigkeiten:** DEC-173

### DEC-259: WLAN-Name ist Pflicht bei «Nur WLAN-Name» und «Beides»
- **Status:** Implemented (0.16.29 laut Development, SYNC-20261111-232 Punkt 5)
- **Datum:** 2026-11-11 (Entscheidung des Benutzers im Chat)
- **Kategorie:** Profile / Eingabe
- **Entscheidung:** Bei der PC-Erkennung «Nur WLAN-Name» und «Beides» ist der WLAN-Name Pflicht: Das Formular blockiert «Weiter» und «Speichern», in der Karte «WLAN» steht ein roter Hinweis; `profile_wifi_hint` heisst nicht mehr «Optional».
- **Begründung:** — (in der Quelle nicht genannt).
- **Alternativen:** — (in der Quelle nicht genannt).
- **Konsequenzen:** Bestehende Profile ohne Namen in diesen Modi brauchen beim nächsten Speichern einen Namen.
- **Betroffene Komponenten:** `ui/ProfileScreen.kt`, `strings_data.xml`, `strings_profile.xml`
- **Abhängigkeiten:** DEC-219

### DEC-260: Papierkorb heisst überall `@LeoSync-Bin`; Migration und PC-Rückfall
- **Status:** Implemented (0.16.33 laut Development, SYNC-20261111-244; ergänzt 0.16.34/0.16.35; QA und UX offen)
- **Datum:** 2026-11-11 (Entscheidung des Benutzers im UX- und im Development-Chat, Konzept freigegeben: Stufe A und B zusammen, Migration automatisch ohne Rückfrage)
- **Kategorie:** Sync / Papierkorb
- **Entscheidung:** Der Papierkorb heisst auf Handy und PC/NAS in allen Ordnertypen `@LeoSync-Bin` (`TRASH_DIR`). Altnamen (`.LeoSync-Bin`, `.LeoSync-Papierkorb`, `.Sync2PC-Papierkorb`) werden weiter gelesen und nie synchronisiert. Eine automatische Migration (`mirror/TrashMigration.kt`) benennt den Altordner um bzw. verschiebt Datei für Datei (nie kopieren, nie überschreiben, Uhrzeit-Anhang bei Namensgleichheit, alter Ordner nur löschen, wenn leer; wiederholbar; wartet bei offener Wiederherstellung, Leeren-Vormerkung oder unbestätigtem Ordner; nie im Vorschau-Lauf). Lehnt der PC den Namen mit einem Namens-Statuscode ab, gilt der Rückfall `.LeoSync-Bin` (`mirror/PcTrashMover.kt`, Verlaufswarnung); die Ablehnung merkt sich `PcTrashNameMemory` 7 Tage (nur bei «Name ungültig»).
- **Begründung:** MediaStore indexiert Punkt-Ordner nicht (DEC-254); auf QNAP/Samba war `.LeoSync-Bin` ein versteckter Ordner. Ein einheitlicher, sichtbarer Name vermeidet zwei Namen je Seite.
- **Alternativen:** `.LeoSync-Bin` auf dem PC behalten: verworfen (zwei Namen).
- **Konsequenzen:** Überholt DEC-254 teilweise (Handy-Name nur bei `msd:`). Bekannte Einschränkungen und Restpunkte: `SYNC-ENGINE.md` (Papierkorbnamen). Die Galerie-Wirkung regelt DEC-263.
- **Betroffene Komponenten:** `mirror/TrashMigration.kt`, `mirror/TrashMigrationSides.kt`, `mirror/PcTrashMover.kt`, `mirror/PcTrashNameMemory.kt`, `mirror/MirrorEngine.kt`
- **Abhängigkeiten:** DEC-254, DEC-094

### DEC-261: Zu kurzes Dateiende beim Download ist eine Unterbrechung, nur mit Fortschritt
- **Status:** Implemented (0.16.30/0.16.31 laut Development, SYNC-20261111-232 Punkt 2)
- **Datum:** 2026-11-11
- **Kategorie:** Sync / Übertragung
- **Entscheidung:** Endet ein Download vor der erwarteten Grösse, gilt das als unterbrochen («Verbindung unterbrochen … Rest folgt»), die `.s2ptmp` bleibt und das Fortsetzen folgt im nächsten Lauf. Nur wenn im Aufruf Bytes gelesen wurden; ohne Fortschritt gilt es als Einzelfehler (Temp-Datei verworfen, nächster Lauf beginnt neu). Ändert sich die PC-Datei in der Zwischenzeit, bleibt es beim Fehler «Grösse nach Download stimmt nicht».
- **Begründung:** QA-Befund -232: Ein Verbindungsabbruch mit Wiederverbinden endete fälschlich als Fehler. Ohne Fortschritt würde Unterbrechung dauerhaft blockieren (Review).
- **Alternativen:** — (in der Quelle nicht genannt).
- **Konsequenzen:** `smb/DownloadEnd.kt` (`classify`), `DownloadSizeException`, Test `DownloadEndTest`.
- **Betroffene Komponenten:** `smb/DownloadEnd.kt`, `smb/SmbSession.kt`
- **Abhängigkeiten:** DEC-131

### DEC-262: SMB-Zeitlimits bleiben bei 60 s und 90 s
- **Status:** Accepted (Architecture, SYNC-20261111-243)
- **Datum:** 2026-11-11
- **Kategorie:** Architektur / SMB
- **Entscheidung:** Die SMB-Zeitlimits bleiben unverändert: `SmbSession.open` mit `smbConfigFor(enc, 60, 90)`, also smbj-Zeitlimit 60 s (Lese-, Schreib- und Transaktionsanfragen) und Socket-Zeitlimit 90 s. Schnellmodus (Lernen nach dem Speichern) 30/30 s, `checkPc`/`listShares` 30 s. Kein Fortschrittswächter, keine Änderung der Oberfläche.
- **Begründung:** Ein vom PC geschlossener oder zurückgesetzter TCP-Kanal (FIN/RST) wird von smbj sofort erkannt (geprüft im Bytecode von smbj 0.13.0, `PacketReader.run` → `Connection.handleError`). Das Zeitlimit greift nur bei stummer Gegenstelle (PC ohne Strom, WLAN ausser Reichweite): Erkennung nach etwa 60 s, höchstens etwa 90 s. Seit 0.16.30 endet so ein Abbruch korrekt als «Verbindung unterbrochen … Rest folgt»; es kostet nur Zeit, keine Daten.
- **Alternativen:** (a) Zeitlimit auf 30 s senken: smbj zählt die Zeit je Anfrage bis 1 MB, damit scheitert jede Verbindung unter etwa 35 KB/s; dazu anlaufende NAS-Platten und lange Close-/Rename-Vorgänge mit Virenprüfung: Fehlalarme mit Neuverbinden für nur etwa 30 s Gewinn. (b) Fortschrittswächter: smbj meldet innerhalb einer Anfrage keinen Fortschritt; der Wächter müsste aus einem zweiten Thread schliessen (auch beim Umbenennen und bei «Beide behalten»): neuer Nebenläufigkeitspfad, zweiter Zeitlimit-Mechanismus. Das Socket-Zeitlimit (90 s) nicht senken, weil smbj jede Funkstille darüber als Verbindungsfehler behandelt.
- **Konsequenzen:** Harter Verbindungsabbruch kostet etwa 63 s plus Neuversuch (QA-Messung).
- **Betroffene Komponenten:** `smb/SmbSession.kt` (`smbConfigFor`, `open`), `sync/Errors.kt` (`isNetworkError`)
- **Abhängigkeiten:** DEC-261

### DEC-263: Handy-Papierkorb und Galerie: Ausschluss und `.nomedia`
- **Status:** Implemented *(0.16.36, laut Development SYNC-20261112-272, 611 Tests grün; von Documentation nur per Stichprobe geprüft)* (Architecture, SYNC-20261111-247, Benutzerfreigabe 2026-11-12)
- **Datum:** 2026-11-12
- **Kategorie:** Sync / Papierkorb
- **Entscheidung:** (1) Fotoprofil und «Speicher freigeben» schliessen Dateien aus, deren `RELATIVE_PATH` an beliebiger Stelle ein Papierkorb-Segment (`MirrorEngine.TRASH_DIRS`) enthält. (2) Der Handy-Papierkorb `@LeoSync-Bin` eines Ordnerprofils erhält eine leere `.nomedia` (bestmöglich, Fehlschlag ist kein Fehler), ausser bei `msd:`-Ordnern; nie auf dem PC, nie in Altnamen-Ordnern; die Datei ist kein Papierkorb-Inhalt. (3) Der Filter der Ordnerprofile bleibt unverändert.
- **Begründung:** Seit dem einheitlichen Namen `@LeoSync-Bin` (DEC-260) indexiert der MediaProvider den Handy-Papierkorb (nur Punkt-Ordner und Ordner mit `.nomedia` gelten als versteckt). Gelöschte oder ersetzte Bilder erschienen in der Galerie, und ein Fotoprofil mit überlappendem Quellordner hätte sie als neue Fotos hochgeladen. Fotoprofil und «Speicher freigeben» lesen nur den MediaStore mit Pfaden relativ zum Speicher, daher griff der bisherige Ausschluss nur an der Wurzel.
- **Alternativen:** (a) nur `.nomedia`: deckt `msd:` nicht ab. (b) Ausschluss auf jeder Ebene auch für Ordnerprofile: kritische Sync-Änderung an bestehendem Zustand. (d) auf den Gerätetest warten: Der Ausschluss ist unabhängig davon richtig. `.LeoSync-Bin` für `msd:` (Benutzeridee): würde DEC-254 rückgängig machen. Zentraler Handy-Papierkorb im App-Speicher (Benutzeridee): Kopieren + Löschen statt Verschieben über Anbietergrenzen, Verlust bei Deinstallation oder «Speicher löschen», im Dateimanager nicht sichtbar, neue Ablage und zweite Migration.
- **Konsequenzen:** Restrisiko: Bei `msd:`-Ordnern bleiben Papierkorb-Bilder in der Galerie sichtbar (dort ist nur auflistbar, was im MediaStore steht). Der Papierkorb-Ordner bleibt nach dem Leeren mit der `.nomedia` stehen. Ergänzt DEC-254.
  **Umsetzung 0.16.36 (laut Development, SYNC-20261112-272):** (1) Segment-Filter `MirrorEngine.isInTrash` (ganze Segmente, alle `TRASH_DIRS`, Gross-/Kleinschreibung egal) direkt in `MediaScanner.scan` und `SpaceFreer.query`, damit auch `PendingCheck` erfasst ist. (2) `.nomedia` nur auf dem Handy, nicht bei `msd:` und zusätzlich nicht bei `msf:` (`TrashNoMedia.applies`); angelegt vor der ersten Datei (`LocalTree.moveToTrash`, `PhoneTrashSide.makeDir`), einmal je Lauf (`MirrorEngine` nach `migrateTrash`, nie im Vorschau-Lauf) und nach der Migration. (3) «Alles löschen» entfernt weiter den ganzen Papierkorb-Ordner samt `.nomedia`; sie wird vor dem nächsten Verschieben neu angelegt. (4) Fehlschlag: nur `Log.i`, ein Versuch je Lauf, kein App-Log-Eintrag. (5) `TrashMigration`: Eine `.nomedia` direkt im Altnamen-Ordner wird über `TrashSide.deleteNoMedia` entfernt (Handy und PC), nicht verschoben.
- **Betroffene Komponenten:** `media/MediaScanner.kt`/`sync/SyncEngine.kt`, `sync/SpaceFreer.kt`, `mirror/MirrorEngine.kt`, `mirror/LocalTree.kt`, `mirror/TrashMigration.kt`
- **Abhängigkeiten:** DEC-254, DEC-260

### DEC-264: Auftragsverwaltung über ein zentrales Skript
- **Status:** Implemented *(Skript umgesetzt SYNC-20261112-254; Regelwerk umgestellt SYNC-20261112-259; Hook `tools/Auftrag-guard.ps1` umgesetzt, siehe DEC-266; Quelle: Auftrag SYNC-20261112-260, Architecture)* (Entscheidung: Architecture, SYNC-20261112-253, Benutzerfreigabe 2026-11-12)
- **Datum:** 2026-11-12
- **Kategorie:** Koordination / Werkzeuge
- **Entscheidung:** Alle Schreibzugriffe auf `coordination/tasks`, `TASK-INDEX.md` und `inbox/` laufen über ein zentrales PowerShell-5.1-Skript in `tools/` mit `erstellen`, `status`, `aendern`, `abschliessen`, `liste`, `check [-Fix]`. Task-Dateien sind die einzige Quelle; Index und Posteingänge werden daraus erzeugt. Globale Sperre über einen offen gehaltenen Datei-Handle (`coordination/.lock`, `FileShare.None`), Nummernvergabe mit `FileMode.CreateNew` (Maximum aus Dateinamen und `.history`-Ordnernamen), atomares Schreiben über Temp-Datei und `File.Replace` mit Hash-Vergleich, Sicherung jeder Altfassung in `.history/<ID>/` (höchstens 20), Audit-Log `.audit.log` (nur anhängen), keine Löschfunktion (Ausnahme: älteste Sicherung in `.history`), abgeschlossene Aufträge schreibgeschützt und nur durch Anhängen ergänzbar. Hook gegen Edit/Write auf diese Dateien erst nach bestandenem Test, zuerst nur für Edit/Write.
- **Begründung:** Wiederholte Datenverluste und Beschädigungen durch manuelle Bearbeitung (überschriebene Task-Datei -245, fehlende Index-Zeilen, defekte Tabelle). Ein offen gehaltener Handle kann nach einem Absturz nicht verwaist zurückbleiben; erzeugte Übersichten können nicht auseinanderlaufen.
- **Alternativen:** Sperrdatei, deren blosse Existenz die Sperre bedeutet (verwaist nach Absturz); zeilenweises Bearbeiten von Index und Posteingängen (Drift); Wiedereröffnen und Weiterleiten (vom Inhaber gestrichen); Hook auch für Shell-Befehle in Stufe 1 (heuristisch, Fehlalarme).
- **Konsequenzen:** Grenzen: `-Rolle` und `-Benutzerfreigabe` sind Konvention und Protokoll, keine Authentifizierung; Schreibschutz und Hook verhindern nur Versehen. Die Umstellung von `coordination/README.md` und Regelwerk hat Architecture auf Benutzerauftrag vom 2026-11-12 vorgenommen (Freigabe in SYNC-20261112-259); die frühere Vorgabe «ändert der Project Manager» ist überholt. Die verbindliche Bedienung steht in `coordination/README.md` §13 (Kurzfassung `CLAUDE.md` §10, Hilfe im Skript mit `-Help`); hier nicht dupliziert.
- **Betroffene Komponenten:** `tools/Auftrag.ps1`, `coordination/` (`.lock`, `.history/`, `.audit.log`, erzeugte Index-/Posteingangsdateien), `.gitignore`
- **Abhängigkeiten:** DEC-210
- **Skripterweiterungen 2026-11-12** (Architecture mit einmaliger Development-Befugnis, SYNC-20261112-259): `-Help`/`-Hilfe`/`hilfe`; `-Eingabe` bei `erstellen`/`status`/`abschliessen` lehnt unbekannte Schlüssel, `#`-Überschriften und Vorlauftext mit Exit 1 ab (vorher still verworfen = Datenverlust); Dublettenprüfung erkennt Pfade mit `*` und entfernt Markdown-Sternchen; `aendern -Feld` akzeptiert Kurznamen und nennt bei Fehler die gültigen Namen; `check` fasst Hinweise zusammen (`-Details` für Einzelzeilen); `liste -Rolle` zeigt standardmässig auch BLOCKED, neu `-MitDateien` (SYNC-20261112-262).

### DEC-265: Doppelte Felder in Altdateien werden toleriert
- **Status:** Implemented *(laut Auftrag SYNC-20261112-260, Architecture; im Skript nicht von Documentation nachgeprüft)*
- **Datum:** 2026-11-12
- **Kategorie:** Koordination / Werkzeuge
- **Entscheidung:** In Task-Dateien im Altformat (22 Dateien) mit mehrfach vorkommenden Feldern gilt bei `Bearbeitungsverlauf`, `Übernahmebestätigung` und `Abgeschlossen am` das letzte Vorkommen, bei den übrigen das erste. Exit 3 gibt es nur bei doppeltem `Status`, `Absender`, `Empfänger`, `Priorität` oder `Erstellt am`; `check` meldet einen Hinweis.
- **Begründung:** Sonst wären Ergänzungen an diesen abgeschlossenen Dateien unmöglich; die Kernfelder bleiben eindeutig.
- **Alternativen:** Exit 3 für jedes doppelte Feld (Konzept SYNC-20261112-253, Abschnitt 3): verworfen.
- **Konsequenzen:** Abweichung vom Konzept -253. Quelle: Architecture, SYNC-20261112-259.
- **Betroffene Komponenten:** `tools/Auftrag.ps1`
- **Abhängigkeiten:** DEC-264

### DEC-266: Technische Schreibsperre für `coordination/` und Alias-Sperre
- **Status:** Implemented *(laut Auftrag SYNC-20261112-260; Architecture meldet «live geprüft»: Edit gesperrt, `Remove-Item` auf Auftragspfad hart gesperrt, .NET-Schreibweg löst Rückfrage aus)*
- **Datum:** 2026-11-12
- **Kategorie:** Koordination / Werkzeuge
- **Entscheidung:** Benutzerfreigabe 2026-11-12 (Chat Architecture, Stufe 1+2): `.claude/settings.json` mit `permissions.deny` für Edit auf `coordination/tasks/**`, `inbox/**`, `TASK-INDEX.md`, `.history/**`, `.audit.log`, `.lock` und PreToolUse-Hook `tools/Auftrag-guard.ps1`: Edit/Write/NotebookEdit auf diese Pfade Exit 2; PowerShell/Bash mit Schreibverb auf diese Pfade: Rückfrage (ask); `Auftrag.ps1`-Aufrufe frei; Lesen frei. Zusätzlich (Benutzerauftrag, SYNC-20261112-263): Die Alias-Falle ist oberste Regel (`CLAUDE.md` §1) und technisch gesperrt: der Hook lehnt PowerShell-Befehle und `.ps1`-Dateien mit `function`/`filter` unter Alias- oder Cmdlet-Namen mit Exit 2 ab.
- **Begründung:** Wiederholter Datenverlust trotz Regel (u. a. gelöschte Task-Dateien durch `function rd`).
- **Alternativen:** Harte Sperre auch für die Shell-Heuristik (Fehlalarme); kein Hook, nur Regelwerk: beide verworfen.
- **Konsequenzen:** Grenze: hält Versehen auf, keine Absicht. Ausnahme Documentation: `apop85.github.io/.claude/tasks.md` für Aufträge an den Web Developer, von Hand.
- **Betroffene Komponenten:** `.claude/settings.json`, `tools/Auftrag-guard.ps1`
- **Abhängigkeiten:** DEC-264

### DEC-267: Dublettenprüfung bei `erstellen` bricht vor dem Schreiben ab
- **Status:** Implemented *(laut Auftrag SYNC-20261112-260, aus SYNC-20261112-262; im Skript nicht nachgeprüft)*
- **Datum:** 2026-11-12
- **Kategorie:** Koordination / Werkzeuge
- **Entscheidung:** Ein ähnlicher Titel mit mindestens zwei gemeinsamen Wörtern bricht vor dem Schreiben ab (Exit 3); `-Trotzdem` legt bewusst an; Vorgänger unter `-Abhängigkeiten` sind ausgenommen, sonst nur Warnung.
- **Begründung:** Die Warnung kam bisher erst nach dem Anlegen.
- **Alternativen:** Abbruch schon bei einem gemeinsamen Wort (Fehlalarme bei kurzen Titeln): verworfen.
- **Konsequenzen:** Ersetzt «Dublettenwarnung, keine Sperre» aus Konzept SYNC-20261112-253.
- **Betroffene Komponenten:** `tools/Auftrag.ps1`
- **Abhängigkeiten:** DEC-264

### DEC-268: Ablaufregeln zur Token-Einsparung und Regelwerk-Klarstellungen
- **Status:** Accepted *(laut Auftrag SYNC-20261112-260, Benutzerentscheide 2026-11-12 laut -263/-264; Regeltexte stehen in `CLAUDE.md`)*
- **Datum:** 2026-11-12
- **Kategorie:** Koordination / Regelwerk
- **Entscheidung:** (1) Posteingang/Überblick nur über `liste`; Task-Datei nur des bearbeiteten Auftrags einmal lesen; nach Exit 0 nichts nachlesen. (2) `CLAUDE.md` §1 verschlankt (Rechte unverändert, Details in QA-/UX-Prompt); UX-Testordner nur `D:\testsync`, `X:\testsync`. (3) Folgeaufträge in sich vollständig; Chat-Abschlussbericht kurz mit Verweis auf die Task-Datei. (4) Git ist nicht vorhanden, keine Git-Befehle. (5) Änderungen an `tools/` und am Regelwerk ändern die Version nicht. (6) Rückfragen und Vorschläge an den Benutzer immer nummeriert. (7) Gebündelt arbeiten, Umgebungsprüfung und Build einmal je Sitzung, Ausgaben gefiltert, Folgeaufträge an eigenen OPEN-Auftrag anhängen; QA/UX prüfen seitenweise gebündelt. (8) `PATCHNOTES.md` pflegt Documentation, nicht die Agents. (9) Ergänzung 2026-11-12 (Benutzerauftrag, SYNC-20261112-276; `CLAUDE.md` §11): Doku-Aufträge erstellt oder ergänzt jede Rolle direkt ohne Rückfrage beim Benutzer; bestehende OPEN-Aufträge an Documentation zum selben Stand werden ergänzt statt neu angelegt.
- **Begründung:** Token-Einsparung und Vermeidung von Widersprüchen im Regelwerk.
- **Alternativen:** In den Quellen nicht genannt.
- **Konsequenzen:** Verbindlich ist der Wortlaut in `CLAUDE.md` und `coordination/README.md`.
- **Betroffene Komponenten:** `CLAUDE.md`, `coordination/README.md`, `.claude/`
- **Abhängigkeiten:** DEC-264

### DEC-269: Papierkorb-Ansicht: Seitenfilter «Anzeigen» und erweiterter Wiederherstellen-Dialog
- **Status:** Implemented *(0.16.38, laut Auftrag SYNC-20261112-278 und Development, -277; Quelle der Wahrheit: `.claude/ux/papierkorb-filter.md`; Review/QA/UX-Review-Freigabe von Documentation nicht geprüft)*
- **Datum:** 2026-11-12
- **Kategorie:** UI / Papierkorb
- **Entscheidung:** Benutzerfreigabe 2026-11-12 (Chat UX), Variante A, Punkte 1, 2, 3, 5, 6 und 4A. (1) Wertfeld **«Anzeigen»** (Handy und PC / Nur Handy / Nur PC) über «Tag»; Standard beides; gilt nur für die offene Ansicht. (2) Die Karte oben bleibt ungefiltert. (3) Massenauswahl wirkt nur auf sichtbare Einträge. (4) Wechselt Seitenfilter oder Tag, wird die Auswahl aufgehoben, wenn sie ausgeblendete Einträge enthält (auch nach «Neu einlesen» bereinigt). (5) Auswahlleiste und Löschdialog nennen die betroffenen Seiten. (6) Der Wiederherstellen-Dialog nennt, dass immer auf Handy und PC zurückgeschrieben wird, die Zahl ersetzter und neu anzulegender Dateien (neutral nach Synchronisierungsstand: «Laut Synchronisierungsstand vorhanden/nicht vorhanden: n Dateien», nicht je Seite) und bei aktivem Filter einen Hinweis. (7) «Wo?» im Aufräumen-Sheet ist passend zum Filter vorbelegt, die Bestätigung bleibt unverändert. (8) Die Schaltflächen «Papierkorb aufräumen…» und «Neu einlesen» stehen je in einer Zeile (SYNC-20261112-275).
- **Begründung:** Handy- und PC-Einträge standen gemischt; Wiederherstellen wirkt immer auf beide Seiten, was der Filter sonst verschleiern würde; die Auswahl blieb beim Tag-Wechsel erhalten, sodass ausgeblendete Einträge mitverarbeitet werden konnten (Datenverlustrisiko).
- **Alternativen:** Variante B (Chips in der Karte): schlecht auffindbar, bei Schrift 2,0 umbrechend. Wiederherstellen-Dialog 4B (Einzelaufstellung je Datei) und 4C (nur Filterhinweis): verworfen zugunsten 4A. Dauerhaftes Merken des Filters: verworfen (scheinbar leerer Papierkorb).
- **Konsequenzen:** Umsetzung `ui/TrashScreen.kt`, `mirror/TrashViewRules.kt`, `strings_trash.xml`, `TrashViewRulesTest.kt`. EN-Seitenbegriff ist «Phone»: Der Benutzer hat das am 2026-11-12 bestätigt, `CLAUDE.md` wurde entsprechend geändert (vgl. DEC-249 bis DEC-264, EN «Phone»).
- **Betroffene Komponenten:** `ui/TrashScreen.kt`, `mirror/TrashViewRules.kt`, `RestoreManager`
- **Abhängigkeiten:** DEC-215, DEC-263

---

## 11. Lokalisierung und Sprache

### DEC-155: App zweisprachig, Englisch als Grundsprache
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-03)
- **Kategorie:** Architektur
- **Entscheidung:**
  - Die App gibt es auf Deutsch und Englisch. Läuft das System nicht auf Deutsch, erscheint Englisch.
  - Englisch steht in `res/values/strings_*.xml` (Grundsprache), Deutsch in `res/values-de/strings_*.xml`; je Bereich eine Datei.
  - Alle sichtbaren Texte inkl. Benachrichtigungen, Verlauf und Fehlermeldungen; Einzahl/Mehrzahl als `plurals`.
- **Begründung:** Wunsch des Inhabers („zumindest schon mal auf englisch … wenn das system nicht auf deutsch läuft“); Englisch als Grundsprache macht den Rückfall für alle Nicht-Deutsch-Systeme automatisch.
- **Alternativen:** Deutsch als Grundsprache mit `values-en` (dann sähen Nutzer anderer Sprachen Deutsch).
- **Konsequenzen:** Rund 716 Texte und 60 Mehrzahl-Texte; jede neue Sprache braucht eine eigene `values-xx`. Neue Texte nie fest im Code (DEC-166).
- **Betroffene Komponenten:** `res/values*/strings_*.xml`, praktisch alle Dateien in `ui/`, `sync/`, `mirror/`, `update/`, `backup/`, `data/`
- **Abhängigkeiten:** DEC-001, DEC-156, DEC-166

### DEC-156: Zentraler Textzugriff `str()`/`plural()` statt `stringResource`
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-03)
- **Kategorie:** Architektur
- **Entscheidung:**
  - Alle Texte laufen über `l10n/Texts.kt` (`str(R.string.x, …)`, `plural(R.plurals.x, n, …)`), auch in Workern, Engines und reiner Logik. `Texts.init` in `App.onCreate` und `onConfigurationChanged`.
  - Enums mit Text (`DeleteMode`, `ProfileType`, `SyncDirection`, `ConflictMode`, `FileKind`, `AgeUnit`, `PurgeSides`, `ProfileWizard.Step`, `SettingsSection`) halten String-IDs; `label`/`title` sind Getter.
  - Unit-Tests ohne Android: Ist nichts initialisiert, lädt `Texts` per Reflection `TestTexts` (src/test), das die deutschen XML-Texte liest. Bestehende Tests prüfen so unverändert Deutsch.
  - Platzhalter immer positional (`%1$s`); `%` in Texten mit Platzhaltern als `%%`.
- **Begründung:** Viele Texte entstehen ausserhalb von Compose (Benachrichtigungen, Log, reine Logik mit Tests); ein einheitlicher Zugriff vermeidet Kompilierfehler bei `stringResource` ausserhalb von Composables.
- **Alternativen:** `stringResource` in Compose und `Context.getString` sonst; reine Logik auf Kennungen umbauen (beides mehr Umbau ohne Compiler).
- **Konsequenzen:** Compose rekomponiert nicht von selbst bei Sprachwechsel; dafür wird die Activity neu aufgebaut (DEC-158). Texte vor `App.onCreate` (z. B. ContentProvider) wären nicht verfügbar.
- **Betroffene Komponenten:** `l10n/Texts.kt`, `App.kt`, `test/TestTexts.kt`
- **Abhängigkeiten:** DEC-155, DEC-157

### DEC-157: Unit-Test für Vollständigkeit der Übersetzungen
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-03)
- **Kategorie:** Sonstiges
- **Entscheidung:** `StringsParityTest` prüft: gleiche Schlüssel in beiden Sprachen, gleiche Platzhalter, `one` und `other` bei Mehrzahl, Formatierbarkeit, maskierte Apostrophe.
- **Begründung:** Vorschlag 2 („Unit-Test …, ob jeder Text in beiden Sprachen vorhanden ist“), vom Inhaber angenommen („mach das“). Lint meldet fehlende Übersetzungen nur als Warnung; falsche `%` stürzen erst zur Laufzeit ab.
- **Alternativen:** nur Lint.
- **Konsequenzen:** Der Build schlägt fehl, wenn eine Übersetzung fehlt.
- **Betroffene Komponenten:** `test/StringsParityTest.kt`, `test/TestTexts.kt`
- **Abhängigkeiten:** DEC-155, DEC-166

### DEC-158: Sprachwahl in den App-Einstellungen
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-03)
- **Kategorie:** Android
- **Entscheidung:**
  - *Einstellungen → App-Einstellungen → Sprache* mit „Systemsprache“, „Deutsch“, „English“ (Sprachnamen in der eigenen Sprache).
  - Ab Android 13 über `LocaleManager` mit `res/xml/locales_config.xml` (auch in den Android-Einstellungen der App wählbar); Android 10–12 über eigene Prefs „lang“ und `createConfigurationContext` (`MainActivity.attachBaseContext`) plus `recreate()`.
  - Die Wahl ist gerätebezogen und nicht Teil von Export/Import.
  - Unterstützte Sprachen zentral in `AppLanguage.SUPPORTED` (de, en).
- **Begründung:** Vorschlag 1 und Anschlussfrage 3, vom Inhaber bestätigt („Füge dies unter App-Einstellungen ein“, „passt“).
- **Alternativen:** AppCompat `setApplicationLocales` (nicht genutzt, die App hat keine AppCompat-Abhängigkeit).
- **Konsequenzen:** Kanalnamen der Benachrichtigungen werden nach einem Wechsel neu gesetzt. Neue Sprache: `SUPPORTED`, `locales_config.xml` und `values-xx` ergänzen.
- **Betroffene Komponenten:** `l10n/AppLanguage.kt`, `ui/SettingsScreen.kt` (`LanguageCard`), `ui/MainActivity.kt`, `AndroidManifest.xml`, `res/xml/locales_config.xml`
- **Abhängigkeiten:** DEC-106, DEC-155

### DEC-160: Diagnose und PC-Import-Skript nicht übersetzen
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-03)
- **Kategorie:** Sonstiges
- **Entscheidung:** Der Diagnosetext (`diag/Diagnostics.kt`) bleibt Deutsch; das PC-Werkzeug `tools/LeoSync-Import.ps1` wird nicht übersetzt; Entwickler-Logs (`android.util.Log`) ebenfalls nicht.
- **Begründung:** Rückfragen 4 und 5: Diagnose ist für den Inhaber gedacht („einverstanden“); das Import-Skript „war nur für mich“.
- **Alternativen:** Diagnose auf Englisch umstellen oder übersetzen.
- **Konsequenzen:** Diagnosedaten bleiben einheitlich lesbar; Hersteller-Hinweise in der Diagnose können je nach Sprache englisch erscheinen, weil sie aus `VendorHints` stammen.
- **Betroffene Komponenten:** `diag/Diagnostics.kt`, `tools/LeoSync-Import.ps1`
- **Abhängigkeiten:** DEC-079, DEC-068

### DEC-166: Projektregel 15 – Übersetzungen aktuell halten, Tabelle mit Zeilennummern
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-03)
- **Kategorie:** Sonstiges
- **Entscheidung:**
  - Jeder neue oder geänderte Text kommt in beide Sprachdateien, nie fest in den Code.
  - `tools/make-translations.ps1` erzeugt `.claude/translations.md`: Tabelle „Nr. | Schlüssel | Deutsch | Englisch“, nach Bereichen gegliedert. Der Inhaber meldet Korrekturen per Zeilennummer, der Agent übernimmt sie in die XML.
- **Begründung:** Rückfrage 7 („tabelle mit deutsch und deinen englischen vorschlägen … nummeriere die zeilen“) und Anweisung „ergänze projektregeln: übersetzungen aktuell halten“.
- **Alternativen:** Tabelle direkt im Chat (bei rund 800 Zeilen verworfen).
- **Konsequenzen:** Zeilennummern verschieben sich bei neuen Texten; Korrekturen beziehen sich auf den jeweils aktuellen Stand der Tabelle.
- **Betroffene Komponenten:** `.claude/README.md` (Regel 15), `tools/make-translations.ps1`, `.claude/translations.md`
- **Abhängigkeiten:** DEC-013, DEC-155, DEC-157

---

## 12. Performance und Optimierungen

### DEC-014: R8-Minify mit Keep-Regeln aktivieren
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Performance
- **Entscheidung:** Release mit `isMinifyEnabled` und `isShrinkResources`. smbj, mbassador und BouncyCastle werden vollständig behalten; optionale Abhängigkeiten sind per `-dontwarn` abgedeckt.
- **Begründung:** Vorschlag (kleinere APK) angenommen.
- **Alternativen:** Minify aus (DEC-015).
- **Konsequenzen:** Fehlende Keep-Regeln zeigen sich erst im Release-Build oder zur Laufzeit.
- **Betroffene Komponenten:** `app/build.gradle.kts`, `app/proguard-rules.pro`
- **Abhängigkeiten:** DEC-002; ersetzt DEC-015

### DEC-029: Parallele Uploads mit eigener Verbindung je Worker
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Performance
- **Entscheidung:**
  - Standard sind 3 parallele Uploads (einstellbar 1–4), jeder mit eigener SMB-Verbindung.
  - Ein Produzent analysiert die Dateien und stellt Aufträge in einen Kanal.
  - Die Namensvergabe läuft unter einer Sperre mit reservierten Namen.
  - Gleichzeitiges `mkdir` wird toleriert.
- **Begründung:** Vorschlag angenommen; schneller bei vielen kleinen Dateien.
- **Alternativen:** eine einzige Verbindung (vorher).
- **Konsequenzen:** Die Verbindung des Produzenten ist lange ungenutzt; Windows trennt sie dann nach dem Leerlauf (Fehler aus dem Feldtest, gelöst in DEC-053).
- **Betroffene Komponenten:** `SyncEngine` (`Shared`, `worker`, `nameLock`), `GlobalSettings.parallelUploads`
- **Abhängigkeiten:** DEC-053

### DEC-087: Bereits vorhandene Dateien pro Ordner zusammengefasst protokollieren
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Performance
- **Entscheidung:** Statt einer Log-Zeile pro bereits vorhandener Datei schreibt der Fotos-Sync „= N Dateien bereits in <Ordner> vorhanden“, spätestens alle 250 Treffer sowie bei Lauf-Ende und Abbruch.
- **Begründung:** Vorschlag 6 („6 ist ok“): Verlauf lesbar, Datenbank klein.
- **Alternativen:** Einzelzeilen beibehalten.
- **Konsequenzen:** Bei einem harten Prozess-Ende geht höchstens die laufende Zählung verloren.
- **Betroffene Komponenten:** `SyncEngine.flushAlready`
- **Abhängigkeiten:** DEC-099

### DEC-093: Paralleler Ordner-Sync in zwei Phasen, kleine Dateien zuerst
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Performance
- **Entscheidung:**
  - Phase A (nacheinander, Haupt-Verbindung): Papierkorb, Stand merken, Konflikte, Aufräumen.
  - Phase B (parallel): Übertragungen mit so vielen Workern wie „Parallele Übertragungen“ (gleiche Einstellung wie Fotos), je eigene SMB-Verbindung, kleine Dateien zuerst.
  - Ordner auf dem Handy werden nur nacheinander angelegt (`ensureDir` synchronisiert), sonst entstünde per SAF „Ordner (1)“.
  - Ein Wiederholversuch bei Netzfehler, danach Unterbruch des Laufs; gemeinsames Speicherbudget; Fortschritt inkl. laufender Übertragungen.
  - Der Fotos-Sync bleibt unverändert.
- **Begründung:** Plan 0.3.0 vom Inhaber freigegeben (Antworten 5 und 7).
- **Alternativen:** eigene Einstellung für den Ordner-Sync (verworfen).
- **Konsequenzen:** Zähler sind atomar, Fehler-Flags `@Volatile`. Vom Inhaber mit einem Test-Profil erfolgreich getestet.
- **Betroffene Komponenten:** `mirror/MirrorEngine.kt` (`executeAdmin`, `runTransfers`, `transferWorker`), `mirror/TransferPlan.kt`, `mirror/LocalTree.kt`
- **Abhängigkeiten:** ersetzt DEC-074; DEC-029, DEC-035, DEC-094

### DEC-153: Eigene Symbole statt material-icons-extended
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-03)
- **Kategorie:** Performance
- **Entscheidung:** Fehlende Material-Symbole (Pause, Smartphone) werden als `ImageVector` in `AppIcons` nachgezeichnet; das grosse Paket `material-icons-extended` wird nicht eingebunden.
- **Begründung:** Wunsch nach Smartphone- statt Telefonhörer-Symbol; das Paket würde die App unnötig vergrössern.
- **Alternativen:** `material-icons-extended` (verworfen).
- **Konsequenzen:** Vor dem Anlegen prüfen, ob `AppIcons.kt` existiert (das Überschreiben verursachte den Build-Fehler von 0.6.1).
- **Betroffene Komponenten:** `ui/AppIcons.kt`
- **Abhängigkeiten:** DEC-095, DEC-149

### DEC-182: Startseite liest Einstellungen nur bei Zustandswechseln
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-04)
- **Kategorie:** Performance
- **Entscheidung:** Abgeleitete Werte werden mit dem Schlüssel `workKey` (Auftrags-ID, Zustand, Versuch) statt der kompletten Auftragsliste neu berechnet; eine einzige `GlobalSettings`-Instanz.
- **Begründung:** Befund des Code-Reviews: Fortschritts-Updates lösten mehrmals pro Sekunde je acht Einstellungs-Lesevorgänge aus.
- **Alternativen:** —
- **Konsequenzen:** Werte, die sich ohne Zustandswechsel ändern, aktualisieren sich erst beim nächsten Wechsel oder onResume.
- **Betroffene Komponenten:** `ui/HomeScreen.kt`
- **Abhängigkeiten:** —

---

## 13. Sicherheit, Datenschutz und Diagnose

### DEC-004: SMB-Passwörter mit dem Android-Keystore verschlüsseln
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Sicherheit
- **Entscheidung:** AES-256-GCM mit einem nicht exportierbaren Keystore-Schlüssel (Alias `sync2pc_credentials`); in der DB liegt der verschlüsselte Wert (Base64).
- **Begründung:** Keine Passwörter im Klartext auf dem Gerät.
- **Alternativen:** EncryptedSharedPreferences wurde erwogen und nicht genutzt.
- **Konsequenzen:** Der Alias darf nicht umbenannt werden, sonst sind alle Passwörter verloren (bei der Umbenennung in DEC-008 bewusst beibehalten).
- **Betroffene Komponenten:** `data/CredentialCrypto.kt`
- **Abhängigkeiten:** DEC-008

### DEC-079: Diagnosedaten in die Zwischenablage, mit zufälliger Geräte-ID
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Sonstiges
- **Entscheidung:**
  - Ein Knopf „Diagnosedaten kopieren“ (seit 0.4.0 unter *Einstellungen → Diagnose*) sammelt Gerät, Akku- und Hintergrund-Einschränkungen (Standby-Bucket, `isBackgroundRestricted`), Berechtigungen, WLAN, Einstellungen, Profile, WorkManager-Aufträge, aktive Benachrichtigungen, `ApplicationExitInfo` (Android 11+), Abstürze und die letzten Läufe.
  - Keine Passwörter; beim Benutzernamen nur „gesetzt“.
  - Eine zufällige 12-stellige Geräte-ID (keine Hardware-Kennung) ordnet Diagnosen und Logs einem Gerät zu.
  - Der Log-Teil zeigt zuerst alle Warnungen und Fehler, danach ein gekürztes Log; gleichartige „bereits vorhanden“-Zeilen werden mit Dateizahl zusammengefasst.
  - Der Knopf bleibt auch nach 1.0.0.
- **Begründung:** Wunsch des Inhabers (Vorschlag 7: „Button Diagnosedaten kopieren“; Rückfragen 1 und 2: eindeutige ID statt persönlicher Daten, Knopf bleibt).
- **Alternativen:** persönliche Angaben verdecken (unnötig, da nicht enthalten).
- **Konsequenzen:** Die ID entsteht nach einer Neuinstallation neu (`allowBackup=false`). Die Diagnose deckte den OnePlus-Killer auf (DEC-082).
- **Betroffene Komponenten:** `diag/Diagnostics.kt`, `GlobalSettings.deviceId`, `SyncDao` (Diagnose-Abfragen), `ui/SettingsScreen.kt`
- **Abhängigkeiten:** DEC-080, DEC-081, DEC-082

### DEC-080: Verlauf als Textdatei über das Android-Teilen-Menü exportieren
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Android
- **Entscheidung:** Ein Teilen-Knopf im Verlauf schreibt alle Läufe mit Log (Kopf mit Geräte-ID und Version) nach `cacheDir/share/` und teilt die Datei über einen FileProvider (`${applicationId}.fileprovider`). Der Export enthält jeden Lauf einzeln, auch wenn die Anzeige zusammenfasst (DEC-100).
- **Begründung:** Rückfrage 4: „ja über das android teilen menü“; grosse Logs passen nicht zuverlässig in `EXTRA_TEXT`.
- **Alternativen:** Text direkt im Intent (verworfen, Grössenlimit).
- **Konsequenzen:** Exportierte Dateien enthalten IP-Adresse und Dateinamen; `.claude/*.txt` und `LeoSync-Verlauf_*.txt` sind per `.gitignore` ausgeschlossen.
- **Betroffene Komponenten:** `Diagnostics.logExport/shareAsFile`, `ui/LogScreen.kt`, `AndroidManifest.xml`, `res/xml/file_paths.xml`, `.gitignore`
- **Abhängigkeiten:** DEC-079

### DEC-081: Lokaler Absturzbericht in der Diagnose
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Sonstiges
- **Entscheidung:** Ein `UncaughtExceptionHandler` (installiert als Erstes in `App.onCreate`) speichert die letzten 3 Abstürze mit gekürztem Stacktrace (max. 6000 Zeichen) lokal; die Diagnose zeigt sie im Abschnitt „Abstürze“. Danach übernimmt der normale Android-Handler.
- **Begründung:** Vorschlag 1 („umsetzen“): Abstürze ohne USB/Logcat sichtbar machen.
- **Alternativen:** Crash-Reporting-Dienst (nicht diskutiert; widerspräche „keine Server“).
- **Konsequenzen:** Nichts wird automatisch übertragen.
- **Betroffene Komponenten:** `diag/CrashReporter.kt`, `App.kt`, `Diagnostics`
- **Abhängigkeiten:** DEC-079

### DEC-089: Sicherung von Profilen, Einstellungen und Sync-Status (Export/Import)
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Sicherheit
- **Entscheidung:**
  - JSON-Datei (`leosync-backup`, Formatversion 1) mit Profilen, Einstellungen, Ordner-Zuordnungen und optional (vorausgewählt) dem Sync-Status (`synced_files`, `mirror_state`); der Lösch-Zustand wird bewusst nicht exportiert.
  - Optionales Export-Passwort: SMB-Passwörter mit AES-256-GCM verschlüsselt, Schlüssel per PBKDF2-HMAC-SHA256 (210 000 Runden, 16-Byte-Salt), Prüfwert erkennt ein falsches Passwort. Ohne Export-Passwort keine Passwörter in der Datei.
  - Import mit Vorschau; „Ersetzen“ (alles löschen, Einstellungen übernehmen) oder „Hinzufügen“ (neue IDs, Einstellungen unverändert); bei falschem Passwort werden Profile ohne Passwort übernommen.
  - Spätere App-Versionen müssen ältere Formatversionen lesen können; fehlende Felder erhalten Standardwerte.
- **Begründung:** Vorschlag 5 und Rückfragen 1/2 (optionales Export-Passwort, Sync-Status mitexportieren); Voraussetzung für den Paketwechsel (DEC-110). Der Keystore-Schlüssel ist nicht exportierbar.
- **Alternativen:** immer ohne Passwort exportieren (nicht gewählt).
- **Konsequenzen:** Ordner-Profile verlieren die SAF-Freigabe (Hinweis nach dem Import). Vom Inhaber erfolgreich getestet.
- **Betroffene Komponenten:** `backup/BackupCodec.kt`, `backup/BackupManager.kt`, `ui/BackupSection.kt`, `GlobalSettings.exportable/importFrom`, `SyncDao`
- **Abhängigkeiten:** DEC-004, DEC-092, DEC-110

### DEC-200: Sicherheitsprüfung mit claude-security
- **Status:** Proposed
- **Datum:** 2026-11-08 (2026-11-04 bis 2026-11-08)
- **Kategorie:** Sicherheit
- **Entscheidung:** Der Inhaber möchte LeoSync mit dem Werkzeug claude-security prüfen lassen (Schwerpunkte: Zugangsdaten, Sicherung, SMB-Pfade, Selbst-Update, exportierte Komponenten). Der Lauf fand nicht statt: Das Werkzeug verlangt die ausdrückliche Zustimmung zu den Kosten und die Einstellung „Dynamic workflows“; als Unteragent ohne Workflow-Werkzeug startet es nicht.
- **Begründung:** Wunsch des Inhabers.
- **Alternativen:** eigene Durchsicht ohne Prüfgremium (vom Werkzeug bewusst nicht geliefert).
- **Konsequenzen:** Der Scan legt einen Ordner `CLAUDE-SECURITY-<Zeitstempel>` im Projekt an (mit eigener `.gitignore`); Patches nur auf Auftrag und mit Git-Checkout.
- **Betroffene Komponenten:** —
- **Abhängigkeiten:** —


### DEC-222: SMB3-Verschlüsselung pro Profil (Aus / Wenn möglich / Erforderlich)
- **Status:** Implemented (0.15.0 laut Development, SYNC-20261111-144; Schema 10; Tests laut Development; Prüfung gegen echte Server und Durchsatzmessung offen)
- **Datum:** 2026-11-11 (Entscheid Inhaber im Architecture-Chat; Quellen SYNC-20261110-126, -141, -144)
- **Kategorie:** Sicherheit
- **Entscheidung:** Je Profil drei Stufen: «Aus» (wie bisher), «Wenn möglich» (Standard, auch für bestehende Profile), «Erforderlich» (ohne Verschlüsselung keine Anmeldung, Zugangsdaten werden nicht gesendet; nur SMB 3.x). Kein Schalter für Zertifikate: SMB3 über TCP verwendet keine Zertifikate, smbj unterstützt kein SMB über QUIC. Signieren wird nicht erzwungen (offen bis Leistungsmessung).
- **Begründung:** R-SMB1 bis R-SMB3 und Benutzerwunsch.
- **Alternativen:** Verschlüsselung immer erzwingen (bricht Server ohne SMB3-Verschlüsselung); einfacher Ein/Aus-Schalter (kein Schutz gegen stille Herabstufung); Schalter «Zertifikatsprüfung ignorieren» (bei SMB nicht anwendbar).
- **Konsequenzen:** Neues Profilfeld `smbEncryption` (Room 9 → 10, `MIGRATION_9_10`), Backup-Feld optional (fehlend/unbekannt = «Wenn möglich»); gemeinsame Fabrik `smbConfigFor`; Pflichtprüfung in `PcLogin` vor `authenticate()`; eigene Ausnahme statt `PcDecision`; «Verbindung testen» zeigt Dialekt und Verschlüsselung. Offen (keine Entscheidung): Signieren erzwingen, Sicherheitsanbieter (BouncyCastle gegen JCE). Verhalten und Ist-Stand: `docs/SYNC-ENGINE.md` 10.2.
- **Betroffene Komponenten:** `smb/SmbSession.kt`, `smb/PcLogin.kt`, `data/Entities.kt`, `data/AppDatabase.kt`, `backup/BackupCodec.kt`, `SmbConfigTest`
- **Abhängigkeiten:** DEC-204, DEC-205

---

## 14. Verteilung: Updates, Play Store und Support

### DEC-056: Selbst-Update über GitHub Releases mit Installation per Tippen
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Sicherheit
- **Entscheidung:**
  - Die App fragt täglich im WLAN `releases/latest` von `Apop85/Sync2PC` ab (keine Entwürfe, keine Vorabversionen) und nimmt die erste `.apk` ohne „debug“ im Namen.
  - Bei einer neueren Version meldet sie sich per Benachrichtigung; ein Tippen lädt herunter und installiert über den `PackageInstaller`.
  - Vor der Installation prüft sie Paketname und versionCode; die Signatur prüft Android.
- **Begründung:** Antworten des Inhabers: Quelle „GitHub Releases“, Installation „Benachrichtigung, Tippen installiert“.
- **Alternativen:** SMB-Ordner als Quelle (DEC-057), automatische Hintergrundinstallation (DEC-058).
- **Konsequenzen:** Das Repo muss öffentlich sein; alle Versionen brauchen denselben Keystore. GitHub-Releases dürfen nicht als „Pre-release“ markiert werden (sonst unsichtbar für `releases/latest`).
- **Aktualisierung (Session 2):** Die Update-Quelle ist seit 0.4.0 fest eingebaut und in der App nicht mehr änderbar (DEC-107). Die Update-Seite prüft beim Öffnen sofort (DEC-106). Play Protect blockiert die App inzwischen als „schädlich“; das Selbst-Update ist ein vermuteter Mit-Auslöser (DEC-111).
- **Betroffene Komponenten:** `update/UpdateChecker.kt`, `UpdateInstaller.kt`, `UpdateWorker.kt`, `UpdateScreen.kt`, `MainActivity.handleInstallStatus`, `version.properties` (`UPDATE_REPO`)
- **Abhängigkeiten:** DEC-007, DEC-011, DEC-106, DEC-107, DEC-111

### DEC-059: Neustart nach dem Update mit Benachrichtigung als Rückfall
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Android
- **Entscheidung:** Bei `MY_PACKAGE_REPLACED` (mit Merker `updateInstalling`) versucht die App `startActivity` und zeigt zusätzlich „Update installiert – tippen zum Öffnen“. Der Bestätigungsdialog wird aus der `MainActivity` im eigenen Task gestartet.
- **Begründung:** Wunsch des Inhabers. Android blockiert Starts aus dem Hintergrund häufig.
- **Alternativen:** —
- **Konsequenzen:** Ob der automatische Neustart klappt, ist geräteabhängig.
- **Betroffene Komponenten:** `BootReceiver`, `UpdateInstaller.install`, `Notifications.updateInstalled`
- **Abhängigkeiten:** DEC-056

### DEC-107: Update-Quelle fest eingebaut
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-02)
- **Kategorie:** Sicherheit
- **Entscheidung:** Das GitHub-Repository für Updates ist nicht mehr änderbar; `GlobalSettings.updateRepo` liefert immer `BuildConfig.UPDATE_REPO`. Früher gespeicherte Werte werden ignoriert, Sicherungen exportieren bzw. importieren es nicht mehr.
- **Begründung:** Vorgabe des Inhabers.
- **Alternativen:** —
- **Konsequenzen:** Ein Wechsel der Quelle geht nur über `version.properties` und einen neuen Build.
- **Betroffene Komponenten:** `GlobalSettings`, `UpdateScreen`, `version.properties`
- **Abhängigkeiten:** DEC-056

### DEC-109: Ziel Play Store; Vorbereitung beschlossen, aber nicht in 0.4.0
- **Status:** Accepted
- **Datum:** 2026-11-08 (2026-11-01; Verschiebung 2026-11-02)
- **Kategorie:** Architektur
- **Entscheidung:** LeoSync soll offiziell in den Play Store. Inhalt der Vorbereitung:
  - ~~PC-Erkennung statt WLAN-Name: jedes ungemessene WLAN, Port-445-Test, Prüfung der SMB-Server-GUID **vor** dem Senden der Zugangsdaten; WLAN-Name nur optional mit freiwilliger Standortfreigabe; Bedingung: genauso zuverlässig wie heute.~~ *(Seit 2026-11-09 nicht mehr Teil der Play-Vorbereitung, siehe Aktualisierung unten; Inhalt bleibt als Vorgabe für die eigenständige PC-Erkennung erhalten.)*
  - Zwei App-Varianten (`github` mit Selbst-Update, `play` ohne `REQUEST_INSTALL_PACKAGES`).
  - Aktuelles `targetSdk`, Umgang mit dem 6-Stunden-Limit für dataSync ab Android 15.
  - Datenschutzerklärung DE/EN (nDSG, DSGVO) über GitHub Pages; AGB sind nicht nötig.
  - Ursprünglich als 0.4.0 geplant; der Inhaber hat am 2026-11-02 entschieden, dass sie nicht in 0.4.0 kommt („wird, was es halt wird“); die Versionsnummer ist offen.
- **Begründung:** Wunsch des Inhabers, „eine App offiziell releasen“; „Standort immer zulassen“ ist die grösste Play-Hürde.
- **Alternativen:** weiter nur über GitHub (kein Play Store).
- **Konsequenzen:** Geschlossener Test mit 12 Testern über 14 Tage, 25 USD, Identitätsprüfung. Paketwechsel nötig (DEC-110).
- **Aktualisierung (Session 3):** Feedback per E-Mail-Knopf „Problem melden“ (DEC-121), Kontakt `leotools@protonmail.com` (DEC-123), nur die Play-Variante bekommt die neue Paket-ID (DEC-124). Die Versionsnummer wurde zuerst 0.7.0, dann 0.8.0 (DEC-125); laut Memory hat eine spätere Session die Reihenfolge erneut geändert.
- **Aktualisierung (Session 5):** Das aktuelle targetSdk (36) ist vorgezogen und seit 0.9.5 umgesetzt (DEC-183). Die Play-Store-Vorbereitung ist jetzt Version 0.12.0 (DEC-188).
- **Aktualisierung (2026-11-09, Entscheid Inhaber, SYNC-20261109-046):** Die Play-Store-Integration bleibt vorerst unspezifiziert und wird nur als offene Punkte geführt (unten). Die PC-Erkennung ist nicht mehr Teil der Play-Vorbereitung und hängt nicht vom Play Store ab; sie wird eigenständig umgesetzt (Development SYNC-20261109-047); verbindliche Vorgabe: DEC-204 (Version 0.13.0). Heutige Erreichbarkeit: TCP-Port 445, 3 s, ohne Anmeldung (`SmbSession.isReachable`).
- **Offene Punkte Play-Store-Integration:**
  - Play-Konto und Tester fehlen. Geschlossener Test mit 12 Testern über 14 Tage, 25 USD, Identitätsprüfung. Kein Früh-Upload zur Reservierung der Paket-ID.
  - Flavors `github`/`play`: `play` ohne Selbst-Update und ohne `REQUEST_INSTALL_PACKAGES`. Paket-ID `ch.leo.sync` nur für `play` (DEC-110, DEC-124). Die GitHub-Variante entfällt nach dem Play-Start, Umzug per Export/Import.
  - Umgang mit dem 6-Stunden-Limit für dataSync-FGS ab Android 15.
  - Datenschutzerklärung DE/EN (nDSG, DSGVO) über GitHub Pages; Kontakt `leotools@protonmail.com`.
  - Vor dem ersten AAB-Upload prüfen, ob Ressourcen-Namen verkürzt werden und Kennungen falsch aufgelöst werden (QA-Risiko aus SYNC-20261109-040, LOW; siehe `TESTING.md` Abschnitt 10).
  - Versionsnummer: nicht festgelegt, es gilt die nächste freie Minor-Reihe.
- **Betroffene Komponenten:** geplant: `WifiHelper`, `SyncWorker`, `SmbSession`, `app/build.gradle.kts` (Flavors), `update/*`
- **Abhängigkeiten:** DEC-044, DEC-052, DEC-056, DEC-110, DEC-113

### DEC-110: Neue Paket-ID `ch.leo.sync`, Schema `ch.leo.<appname>`
- **Status:** Accepted
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Android
- **Entscheidung:** Mit der Play-Store-Vorbereitung wechselt die Paket-ID auf `ch.leo.sync` (Schema `ch.leo.<appname>` für die Leo-Tool-Familie). Der Umzug erfolgt per Export, Neuinstallation, Import, Deinstallation der alten App.
- **Begründung:** Die ID ist im Play Store dauerhaft; `com.sync2pc.app` passt nicht zu LeoSync. Die ID war am 2026-11-01 frei (reserviert erst beim ersten Upload); eine echte Domain ist nicht nötig.
- **Alternativen:** `com.sync2pc.app` behalten (DEC-007); eindeutigere ID wie `ch.leotools.sync` (genannt, nicht gewählt).
- **Konsequenzen:** Für Android eine neue App; das Selbst-Update der alten App kann nicht auf die neue umsteigen. Keystore-Alias darf neu sein, Passwörter kommen über den Import.
- **Aktualisierung (Session 3):** Präzisiert durch DEC-124: Nur die Play-Variante erhält `ch.leo.sync`; die GitHub-Variante bleibt bis zum Play-Start `com.sync2pc.app` und wird danach nicht mehr unterstützt.
- **Betroffene Komponenten:** geplant: `app/build.gradle.kts`, Paketordner, FileProvider-Authority
- **Abhängigkeiten:** ersetzt DEC-007; DEC-089, DEC-109

### DEC-111: Play-Protect-Sperre per Einspruch klären, nicht umgehen
- **Status:** Accepted
- **Datum:** 2026-11-08 (2026-11-02)
- **Kategorie:** Sicherheit
- **Entscheidung:** Auf „Schädliche App blockiert“ (Play Protect, betrifft rückwirkend auch 0.2.0–0.3.3, gleiches Signaturzertifikat SHA-256 `9f8132574d2cdb43ffbef021af6091fa4a454df0f9742c3513b127bb22f971bd`) wird mit dem offiziellen Einspruchsformular reagiert (SHA-256 der APK, Begründungstext). Kein neuer Schlüssel bzw. kein neues Paket nur zum Ausweichen.
- **Begründung:** Die Einstufung gilt der App als Ganzes (Paket bzw. Zertifikat), nicht einem Build. Ausweichen entspräche dem Verhalten von Schadsoftware.
- **Alternativen:** neuer Schlüssel/neues Paket (verworfen); R8-Verschleierung abschalten (DEC-112, verworfen).
- **Konsequenzen:** Google antwortet laut Formular nicht; der Einspruch gilt nur für die eingereichte Datei. Langfristig soll der Play-Store-Vertrieb helfen. Ob der Einspruch eingereicht wurde, ist im Verlauf nicht bestätigt.
- **Betroffene Komponenten:** —
- **Abhängigkeiten:** DEC-056, DEC-109, DEC-112

### DEC-121: Feedback in der Testphase per E-Mail-Knopf „Problem melden“
- **Status:** Implemented (0.11.5)
- **Datum:** 2026-11-08 (2026-11-02)
- **Kategorie:** Sicherheit
- **Entscheidung:** Ein Knopf „Problem melden“ öffnet die Mail-App mit vorbereitetem Text und Diagnose; der Tester sendet selbst. Dazu das Play-Feedback der Testversion. Der Knopf bleibt nach 1.0.0. Er wird ausgeblendet, solange keine Adresse hinterlegt ist.
- **Begründung:** Keine Server und keine Geheimnisse in der App; der Nutzer sieht, was er sendet.
- **Alternativen:** Discord-Webhook (DEC-122, verworfen).
- **Konsequenzen:** Umgesetzt in 0.11.5 (SYNC-20261108-008): Knopf in Einstellungen › Diagnose; `Diagnostics.reportProblem` öffnet per `ACTION_SENDTO mailto:` die Mail-App (`leotools@protonmail.com`, Betreff mit Version, Hinweistext, Diagnose als Anhang `LeoSync-Diagnose_<Zeit>.txt`); der Benutzer sendet selbst. Ohne Mail-App landet die Adresse in der Zwischenablage. Abweichung: Der Knopf wird nicht ausgeblendet, weil die Adresse fest hinterlegt ist. Quelle: Rückmeldung Development, Code nicht selbst geprüft.
- **Betroffene Komponenten:** `diag/Diagnostics.kt` (`reportProblem`), Einstellungen „Diagnose“
- **Abhängigkeiten:** DEC-079, DEC-109, DEC-123

### DEC-123: Kontaktadresse `leotools@protonmail.com`
- **Status:** Implemented (0.11.5, „Problem melden“)
- **Datum:** 2026-11-08 (2026-11-03)
- **Kategorie:** Sonstiges
- **Entscheidung:** Die Adresse dient für „Problem melden“, als Kontakt in der Datenschutzerklärung und im Play Store.
- **Begründung:** Vom Inhaber eingerichtet und bestätigt („1. ja“).
- **Alternativen:** `leo@` bzw. `leosoft@protonmail` (bereits vergeben).
- **Konsequenzen:** In „Problem melden“ fest hinterlegt (DEC-121). In `PRIVACY_*.md` stehen laut Development noch Platzhalter (Name Z. 14, E-Mail Z. 16 und 230); die E-Mail wäre `leotools@protonmail.com`, der Name ist Sache des Inhabers (offen).
- **Betroffene Komponenten:** „Problem melden“ (`diag/Diagnostics.kt`), Datenschutzerklärung
- **Abhängigkeiten:** DEC-109, DEC-121

### DEC-124: Nur die Play-Variante erhält `ch.leo.sync`
- **Status:** Accepted
- **Datum:** 2026-11-08 (2026-11-03)
- **Kategorie:** Android
- **Entscheidung:** Die Play-Variante bekommt `ch.leo.sync`; die GitHub-Variante behält bis zum Play-Start `com.sync2pc.app`. Sobald LeoSync im Play Store ist, wird die GitHub-Variante nicht mehr unterstützt. Kein Früh-Upload zum Reservieren der ID.
- **Begründung:** Antworten des Inhabers (u. a. „sollte es im Playstore erscheinen, wird die Githubvariante nicht mehr unterstützt“).
- **Alternativen:** Beide Varianten mit neuer ID.
- **Konsequenzen:** Der Inhaber zieht einmal per Export/Import um (DEC-089).
- **Betroffene Komponenten:** geplant: `app/build.gradle.kts` (Flavors)
- **Abhängigkeiten:** DEC-109, DEC-110

### DEC-197: Hilfe & Produktseite
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-08)
- **Kategorie:** Sonstiges
- **Entscheidung:** Im Zahnrad-Menü öffnet „Hilfe & Produktseite“ https://apop85.github.io/leosync/. Der Agent hat Hilfe-Texte (DE/EN) für die Produktseite geschrieben.
- **Begründung:** Design-Prüfung (keine Hilfe vorhanden); Produktseite und Hilfetexte vom Inhaber gewünscht.
- **Alternativen:** eigene Hilfe-Seite in der App (nicht gewählt).
- **Konsequenzen:** Die Texte muss der Inhaber auf der Produktseite einbinden und bei Änderungen nachführen.
- **Betroffene Komponenten:** `ui/HomeScreen.kt`, `HELP_URL` (ui/HomeParts.kt), `docs/hilfe/hilfe-de.md`, `docs/hilfe/help-en.md`
- **Abhängigkeiten:** DEC-123

---

## 15. Build, Tests, Versionierung und Arbeitsregeln

### DEC-011: One-Click-Builder, der nur bei Quelländerungen baut
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Sonstiges
- **Entscheidung:** `build.bat` startet `tools/build.ps1`. Der Builder:
  - bildet einen Hash über die Quellen und baut nur bei Änderungen;
  - erzeugt Keystore und `keystore.properties` beim ersten Release-Build;
  - lädt Gradle-Wrapper und Länderdaten bei Bedarf;
  - erzeugt Icons, Animation und Zustandsbilder neu, wenn sich die Vorlagen ändern;
  - führt die Unit-Tests aus und baut die Release-APK nach `releases/`;
  - schreibt die vollständige Gradle-Ausgabe nach `.build/last-build.log`.
- **Begründung:** Regel 4; das Protokoll lässt den Agent Fehler ohne Kopieren analysieren.
- **Alternativen:** Bauen über Android Studio (dokumentiert, nicht Standard).
- **Konsequenzen:** Keystore und Passwortdatei sind per `.gitignore` ausgeschlossen und müssen gesichert werden. `.ps1`-Dateien müssen ASCII sein (PowerShell 5.1).
- **Betroffene Komponenten:** `build.bat`, `tools/build.ps1`, `tools/make-icons.ps1`, `tools/make-anim*.ps1`, `tools/make-state-images.ps1`, `tools/fetch-geodata.ps1`
- **Abhängigkeiten:** DEC-010, DEC-012

### DEC-013: Arbeitsregeln für Agents (Agent baut nie, GO-Regel)
- **Status:** Accepted
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Sonstiges
- **Entscheidung:** Der Agent startet keine Builds. In neuen Chats sammelt er zuerst Ideen und setzt erst nach „GO“ um. Rückfragen werden zuerst beantwortet. Fragen und Vorschläge werden nummeriert. `.claude/README.md` dient als Notiz für spätere Agents.
- **Begründung:** Vorgabe des Inhabers.
- **Alternativen:** —
- **Konsequenzen:** Fehler zeigen sich erst im Build des Inhabers; es wird über `last-build.log` analysiert.
- **Aktualisierung (Session 3):** Die Kurzfassung vor dem GO gibt es nur noch auf Aufforderung des Inhabers; der Agent fragt nicht von sich aus nach GO (DEC-119).
- **Aktualisierung (Session 5):** Der Inhaber erlaubt Builds und Emulator-Tests durch den Agent fallweise pro Lauf; die Grundregel „Agent baut nicht“ bleibt (DEC-184).
- **Betroffene Komponenten:** `.claude/README.md`, Memory
- **Abhängigkeiten:** DEC-011, DEC-119

### DEC-075: `release.bat` mit GitHub-CLI
- **Status:** Proposed
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Sonstiges
- **Entscheidung:** Ein Skript würde das GitHub-Release anlegen und die APK hochladen.
- **Begründung:** Vereinfacht den Ablauf pro Version.
- **Alternativen:** —
- **Konsequenzen:** Setzt eine installierte GitHub-CLI voraus.
- **Betroffene Komponenten:** —
- **Abhängigkeiten:** DEC-056

### DEC-077: Beta-Versionierung ab 0.1.0, neue Funktionen eröffnen die nächste Reihe
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01; Ergänzung 2026-11-02)
- **Kategorie:** Sonstiges
- **Entscheidung:**
  - Ab 0.1.0 Beta: `0.1.x` enthält nur Fehlerbehebungen (PATCH +1 pro Änderung).
  - Eine neue Funktion eröffnet die nächste Reihe (0.2.0, 0.3.0, 0.4.0 …); 1.0.0 gibt nur der Inhaber frei.
  - GitHub-Releases sind normale Releases, keine „Pre-releases“.
  - Die Umgliederung der Einstellungen samt Benachrichtigungs-Schaltern wurde auf Wunsch des Inhabers als 0.4.0 geführt; die Play-Store-Vorbereitung ist damit nicht mehr 0.4.0 (DEC-109).
- **Begründung:** Antworten des Inhabers zu Rückfrage 1/2 („sollten neue Funktionen dazu kommen soll 0.1.x zu 0.2.x gewechselt werden“) und „mach 0.4.0“. Pre-releases erscheinen nicht unter `releases/latest`.
- **Alternativen:** Pre-releases für die Beta (verworfen, weil der UpdateChecker sie nicht sieht); 0.3.8 für die Einstellungs-Umgliederung (vorgeschlagen, Inhaber wählte 0.4.0).
- **Konsequenzen:** Der Builder erhöht PATCH weiterhin automatisch, wenn die APK der aktuellen Version existiert; der Agent muss vor Abschluss prüfen, ob die Version schon gebaut wurde (Fall 0.3.6/0.3.7).
- **Betroffene Komponenten:** `version.properties`, `PATCHNOTES.md`, `build.md`, `.claude/README.md`
- **Abhängigkeiten:** ersetzt DEC-010; DEC-109

### DEC-078: Antworten nach einer Änderung enden mit der Versionsnummer
- **Status:** Accepted
- **Datum:** 2026-11-08 (2026-11-02)
- **Kategorie:** Sonstiges
- **Entscheidung:** Jede Antwort, die eine Änderung abschliesst, endet mit der aktuellen Versionsnummer in eigener Zeile (Regel 14).
- **Begründung:** Der Inhaber nutzt sie als Commit-Kommentar beim Push.
- **Alternativen:** —
- **Konsequenzen:** —
- **Betroffene Komponenten:** `.claude/README.md` (Regel 14), Memory
- **Abhängigkeiten:** DEC-013

### DEC-085: Build-Werkzeuge auf Gradle 9 / AGP 9 mit eingebautem Kotlin
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Sonstiges
- **Entscheidung:**
  - Gradle 9.8.2, Android Gradle Plugin 9.5.0 mit „built-in Kotlin“ (kein `org.jetbrains.kotlin.android`, kein `kotlinOptions`; jvmTarget folgt `compileOptions`).
  - Kotlin 2.4.21 über `buildscript.classpath` im Root; das Compose-Compiler-Plugin hat dieselbe Version.
  - KSP 2.3.13 (nur KSP2), Room 2.8.5.
  - Der Builder akzeptiert Java 17–25 (das JBR von Android Studio).
  - Die Versionen wurden vor der Umsetzung online geprüft (Maven-Metadaten, services.gradle.org, kotlinlang.org).
- **Begründung:** Vorschlag 9 vom Inhaber angenommen; der JDK-21-Workaround entfällt.
- **Alternativen:** Opt-out aus built-in Kotlin (`android.builtInKotlin=false`, nur vorübergehend dokumentiert); Room 2.7.2 (verworfen zugunsten der neuesten stabilen Version).
- **Konsequenzen:** Erster Build 0.1.4 erfolgreich (2 min 9 s, Tests grün, keine Warnungen). Für Unit-Tests mit `org.json` ist `testImplementation("org.json:json")` nötig (DEC-089).
- **Betroffene Komponenten:** `build.gradle.kts`, `app/build.gradle.kts`, `gradle/wrapper/gradle-wrapper.properties`, `tools/build.ps1`, `build.md`
- **Abhängigkeiten:** ersetzt DEC-012 und DEC-076

### DEC-119: Kurzfassung vor dem GO nur auf Aufforderung
- **Status:** Accepted
- **Datum:** 2026-11-08 (2026-11-03)
- **Kategorie:** Sonstiges
- **Entscheidung:** Der Agent gibt die Zusammenfassung vor einem GO nur, wenn der Inhaber sie verlangt. Er fragt nicht von sich aus nach GO und bietet keine GO-Knöpfe an. Ausdrückliche Aufträge („setze 0.6.3 um“, „du kannst direkt loslegen“) gelten als Freigabe.
- **Begründung:** Vorgabe des Inhabers („gib mir die Zusammenfassung vor dem Go erst auf meine Aufforderung und nicht nach jedem Gespräch“).
- **Alternativen:** Kurzfassung nach jedem Gespräch (bisher, verworfen).
- **Konsequenzen:** Rückfragen bleiben nummeriert; ohne Freigabe werden keine Dateien geändert.
- **Betroffene Komponenten:** `.claude/README.md`, Memory `sync2pc-workflow-rules`
- **Abhängigkeiten:** DEC-013

### DEC-120: Build-Fehler einer nie gebauten Version ohne neue Versionsnummer beheben
- **Status:** Accepted
- **Datum:** 2026-11-08 (2026-11-03)
- **Kategorie:** Sonstiges
- **Entscheidung:** Scheitert der erste Build einer neuen Version, wird der Fehler ohne PATCH-Erhöhung und ohne eigenen Patchnotes-Eintrag behoben (Fall 0.7.0). Ist eine Version schon gebaut, erhöht jede weitere Änderung PATCH (Fall 0.7.1).
- **Begründung:** Anweisung des Inhabers: „die version soll 0.7.0 bleiben“.
- **Alternativen:** Jede Korrektur als neue Version (bisherige Regel; bei 0.6.1 → 0.6.2 noch so gehandhabt).
- **Konsequenzen:** Der Agent muss wissen, ob die Version schon erfolgreich gebaut wurde. **Ergänzung 2026-11-12 (SYNC-20261112-276):** Die Regel steht seit der Freigabe des Inhabers vom 2026-11-12 in der PATCH-Regel von `CLAUDE.md` §1 (bisher nur als Claude-Erinnerung, Inhaber 2026-11-03 bei 0.7.0): Die Behebung eines Build-Fehlers der aktuellen, noch nicht erfolgreich gebauten Version erhöht PATCH nicht und erhält keinen eigenen Patchnotes-Eintrag.
- **Betroffene Komponenten:** `version.properties`, `PATCHNOTES.md`, `CLAUDE.md` §1
- **Abhängigkeiten:** DEC-077

### DEC-154: KSP-Release wartet auf KSP-Debug
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-02)
- **Kategorie:** Sonstiges
- **Entscheidung:** `kspReleaseKotlin` läuft nach `kspDebugKotlin` (`mustRunAfter`).
- **Begründung:** Build-Fehler 0.4.2 („Expected start of the object '{', but had 'EOF'“): Beide Tasks lasen bzw. schrieben gleichzeitig das neue Room-Schema `4.json`.
- **Alternativen:** —
- **Konsequenzen:** Etwas weniger Parallelität im Build.
- **Betroffene Komponenten:** `app/build.gradle.kts`
- **Abhängigkeiten:** DEC-085

### DEC-180: Instrumentierter Migrationstest vorgezogen
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-04)
- **Kategorie:** Datenbank
- **Entscheidung:** `MigrationTest` (MigrationTestHelper) prüft die Migrationen 1 → 6 gegen die exportierten Schemas und die Übernahme der Daten (Profilname, Prüfstufe; 5 → 6 mit drei Fällen). Er läuft auf Gerät/Emulator mit `gradlew connectedDebugAndroidTest`, nicht im Builder. Die Schemas sind androidTest-Assets.
- **Begründung:** Ein Emulator stand zur Verfügung und der Inhaber erlaubte Builds; zuvor nur „vor 1.0.0“ geplant.
- **Alternativen:** bis vor 1.0.0 warten (DEC-113).
- **Konsequenzen:** Jede neue DB-Version muss im Test ergänzt werden. Neue Test-Abhängigkeiten: room-testing 2.8.5, androidx.test.ext:junit 1.2.1, androidx.test:runner 1.6.2.
- **Betroffene Komponenten:** `app/src/androidTest/.../MigrationTest.kt`, `app/build.gradle.kts`
- **Abhängigkeiten:** DEC-084, DEC-113 (ersetzt), DEC-172

### DEC-184: Builds und Emulator-Tests durch den Agent nur mit Erlaubnis pro Lauf
- **Status:** Implemented
- **Datum:** 2026-11-08 (2026-11-03 bis 2026-11-08)
- **Kategorie:** Sonstiges
- **Entscheidung:** Der Inhaber erlaubt Builds (`build.bat`) und Emulator-Tests fallweise; Regel 9 bleibt sonst gültig. `build.bat -NoPause` beendet ohne Tastendruck. Der Agent leitet die Build-Ausgabe in eine Datei um.
- **Begründung:** Der Inhaber erteilte die Erlaubnis jeweils ausdrücklich; beim automatischen Aufruf hing der Prozess (der Gradle-Daemon hält eine Pipe offen).
- **Alternativen:** —
- **Konsequenzen:** Existiert die APK der aktuellen Version schon, zählt der Builder hoch; für einen erneuten Build derselben Version muss die APK vorher gelöscht werden.
- **Betroffene Komponenten:** `build.bat`, `tools/build.ps1`, `build.md`, `CLAUDE.md` §1 *(früher `.claude/README.md`, entfällt)*
- **Aktualisierung (2026-11-10):** Die Emulator-Regel ist teilweise ersetzt durch DEC-212: UX und QA bedienen den Emulator für ihre Prüfaufträge ohne Einzelerlaubnis; andere Rollen weiterhin nur mit Erlaubnis pro Lauf. Builds bleiben unverändert nur mit Erlaubnis pro Lauf.
- **Abhängigkeiten:** DEC-011, DEC-013, DEC-212 (ergänzt)

### DEC-185: Testumgebung mit Emulator und Testfreigabe
- **Status:** Accepted
- **Datum:** 2026-11-08 (2026-11-03 bis 2026-11-04)
- **Kategorie:** Sonstiges
- **Entscheidung:** Emulatoren `LeoSync_API29` (Android 10 = minSdk) und `Medium_Phone_API_37.0` (Android 17). Der Inhaber richtet Profile mit seinen Zugangsdaten selbst ein (Ordner `Download/Testsync` ⇄ `\\192.168.3.3\Fotos\testsync`, Fotos → `\\192.168.3.3\Fotos\testfotos`, auf dem PC `X:\…`). Der Agent darf nur in diesen Testordnern Dateien anlegen, ändern und löschen.
- **Begründung:** Erlaubnis des Inhabers; Zugangsdaten gibt nur er ein.
- **Alternativen:** nur Screenshots des Inhabers.
- **Konsequenzen:** Ohne Profile des Inhabers lassen sich Synchronisierung und Prüfstufen nicht testen. Neuinstallations-Fälle brauchen eine Deinstallation (Profile weg).
- **Betroffene Komponenten:** `CLAUDE.md` §1 (Emulator, Testumgebung) *(früher `.claude/README.md`, entfällt)*
- **Abhängigkeiten:** DEC-184, DEC-212

### DEC-188: Versionsplan 0.10.0 bis 0.13.0
- **Status:** Accepted
- **Datum:** 2026-11-08 (2026-11-03 bis 2026-11-08)
- **Kategorie:** Sonstiges
- **Entscheidung:** 0.9.x UX-Überarbeitung und Prüfstufen; 0.10.0 Herunterziehen und Zugangsdaten-Auswahl; 0.11.0 Umsetzung der Design-Prüfung; **0.12.0** Play-Store-Vorbereitung; **0.13.0** PC-Erkennung inkl. mobile Daten/VPN. *(Überholt, Stand 2026-11-09: 0.12.0 ist durch den Verlauf mit Kennungen belegt; die Play-Store-Integration ist nur noch als offene Punkte geführt und ohne Versionsnummer (DEC-109); die PC-Erkennung ist davon entkoppelt und wird eigenständig umgesetzt, ihre Versionsnummer ist 0.13.0, festgelegt in DEC-204. Mobile Daten/VPN ist seit 0.11.5 umgesetzt, DEC-126.)*
- **Begründung:** Neue Funktionen eröffnen jeweils eine neue Reihe (DEC-077); vom Inhaber jeweils bestätigt.
- **Alternativen:** Play Store als 0.10.0 bzw. 0.11.0 (verschoben).
- **Konsequenzen:** —
- **Betroffene Komponenten:** Roadmap (früher `.claude/README.md`, entfällt), Memory
- **Abhängigkeiten:** DEC-165 (ersetzt), DEC-109, DEC-126, DEC-204

### DEC-210: Sechs Chat-Rollen und zentrale Koordinationsrichtlinie
- **Status:** Accepted
- **Datum:** 2026-11-10
- **Kategorie:** Sonstiges
- **Entscheidung:** Es gibt sechs Chat-Rollen: Project Manager, Architecture, Development, QA & Testing, Documentation und UX-Specialist (neu). Die UX-Rolle hat den Masterprompt `.claude/roles/UX-Specialist.md`, den Posteingang `coordination/inbox/ux-designer.md` und den Skill `/rolle-ux`. Die Zusammenarbeit regelt die zentrale Koordinationsrichtlinie `coordination/README.md`. Bei Widerspruch gilt die Regelhierarchie `CLAUDE.md` → Richtlinie → Rollenprompt → Task-Datei.
- **Begründung:** Beschluss des Inhabers im Refactoring des Rollen- und Koordinationssystems; eine einzige Rangfolge verhindert widersprüchliche Regeln in Rollenprompts und Task-Dateien.
- **Alternativen:** —
- **Konsequenzen:** Rollenprompts schaffen keine Ausnahmen von den Berechtigungen in `CLAUDE.md` §1; Task-Dateien heben keine Rollengrenzen auf. Der Vorzustand ist unter `coordination/archive/vor-refactoring-2026-11-10/` gesichert. Reines Regelwerk, keine neue App-Version.
- **Betroffene Komponenten:** `CLAUDE.md`, `coordination/README.md`, `coordination/TASK-HANDOVER.md`, `.claude/roles/`, `.claude/skills/rolle-*`, `coordination/inbox/ux-designer.md`
- **Abhängigkeiten:** DEC-211, DEC-212, DEC-213

### DEC-211: Direkte Übergaben zwischen Fachrollen, Project Manager als Koordinator
- **Status:** Accepted
- **Datum:** 2026-11-10
- **Kategorie:** Sonstiges
- **Entscheidung:** Fachrollen geben sich gegenseitig direkt Aufträge. An den Project Manager geht etwas nur bei Koordinationsbedarf. Der Project Manager ist Ansprechpartner und Koordinator des Inhabers, aber keine Rückgabe-, Freigabe- oder Abschlussinstanz. Verantwortlich für einen Auftrag ist allein der Empfänger.
- **Begründung:** Freigabe des Inhabers am 2026-11-10; die Umwege über den Project Manager kosteten Zeit und verteilten die Verantwortung unklar.
- **Alternativen:** Die bisherige eingeschränkte Übergabematrix mit dem Project Manager als Rückmeldestelle (verworfen).
- **Konsequenzen:** Rückfragen gehen direkt an die Rolle, die sie beantworten kann; Benutzerentscheidungen legen die Fachrollen dem Inhaber direkt vor. Details in `coordination/README.md` §2, §3 und §8.
- **Betroffene Komponenten:** `coordination/README.md`, `CLAUDE.md` §10, Rollenprompts
- **Abhängigkeiten:** DEC-210

### DEC-212: Emulator für UX und QA ohne Einzelerlaubnis (ergänzt DEC-184)
- **Status:** Accepted
- **Datum:** 2026-11-10
- **Kategorie:** Sonstiges
- **Entscheidung:** UX und QA starten den Emulator selbstständig und bedienen ihn für ihre Prüfaufträge ohne Einzelerlaubnis. Sie dürfen in der App alle nötigen Einstellungen vornehmen und entfernen. Werden Profile bearbeitet oder entfernt, legen sie zuerst einen Export an und stellen den ursprünglichen Zustand nach den Tests per Import wieder her. Die Shares der Testprofile liegen unter `D:\testsync` und `X:\testsync`; dort dürfen beide Rollen alle nötigen Dateioperationen ausführen (erstellen, ändern, verschieben, löschen usw.). Andere Rollen brauchen weiterhin die Erlaubnis pro Lauf; Builds bleiben unverändert nur mit Erlaubnis pro Lauf. Profile mit Zugangsdaten richtet weiterhin der Inhaber ein.
- **Begründung:** Freigabe des Inhabers am 2026-11-10 (erste Fassung) und Erweiterung am selben Tag; UX- und QA-Prüfaufträge sind ohne selbstständigen Emulator und ohne Einstellungs- und Dateizugriff nicht ausführbar. Export vor jeder Profiländerung und Import danach schützen die vorhandenen Profile des Inhabers.
- **Alternativen:** Erlaubnis pro Lauf auch für UX und QA (verworfen).
- **Konsequenzen:** DEC-184 gilt für den Emulator nur noch für die übrigen Rollen (teilweise ersetzt); die Regel zu Builds bleibt. Die Testordner in DEC-185 (Download/Testsync ⇄ `\\192.168.3.3\Fotos\testsync`) sind für UX und QA durch `D:\testsync` und `X:\testsync` ersetzt.
- **Betroffene Komponenten:** `CLAUDE.md` §1 (Emulator, Schreibrechte), `.claude/roles/UX-Specialist.md`, `.claude/roles/QA-Specialist.md`
- **Abhängigkeiten:** DEC-184 (ergänzt), DEC-185 (ergänzt), DEC-210

### DEC-213: Dokumentation nur durch Documentation, UX-Entscheidungen in §10
- **Status:** Accepted
- **Datum:** 2026-11-10
- **Kategorie:** Sonstiges
- **Entscheidung:** Doku-Dateien ändert nur die Rolle Documentation. Development liefert die technischen Angaben und den PATCHNOTES-Auftrag, ändert aber keine Doku-Dateien. Freigegebene UX-Entscheidungen stehen in `docs/DECISIONS.md` §10; es gibt keine eigene Designsystem-Datei.
- **Begründung:** Freigabe des Inhabers am 2026-11-10; eine Stelle für die Doku hält sie konsistent und redundanzfrei.
- **Alternativen:** `docs/DESIGN-SYSTEM.md`, von UX gepflegt (verworfen); kleine Doku-Änderungen durch Development (verworfen).
- **Konsequenzen:** Doku-Änderungen laufen als Auftrag an Documentation mit technischem Kontext (`CLAUDE.md` §11).
- **Betroffene Komponenten:** `CLAUDE.md` §1 und §11, `.claude/skills/update-docs`, `.claude/roles/DOCUMENTATION-Specialist.md`
- **Abhängigkeiten:** DEC-210

### DEC-214: QA führt keine Builds aus
- **Status:** Accepted
- **Datum:** 2026-11-10
- **Kategorie:** Sonstiges
- **Entscheidung:** Die Rolle QA & Testing führt keine Builds aus. Der Widerspruch im bisherigen QA-Prompt ist behoben. Gezielte Tests bleiben erlaubt und gelten nicht als Bauen.
- **Begründung:** Builds machen nur der Inhaber oder die Rollen mit seiner ausdrücklichen Erlaubnis pro Lauf (DEC-184).
- **Alternativen:** —
- **Konsequenzen:** QA prüft mit vorhandenen Builds bzw. Tests und im Emulator (DEC-212).
- **Betroffene Komponenten:** `.claude/roles/QA-Specialist.md`
- **Abhängigkeiten:** DEC-184, DEC-212
- **Hinweis (2026-11-10):** Für Debug-/Test-Builds durch DEC-220 teilweise überholt; Release-Builds und `build.bat` bleiben ausgeschlossen.

### DEC-220: QA baut Debug-/Test-Builds selbstständig
- **Status:** Accepted
- **Datum:** 2026-11-10
- **Kategorie:** Sonstiges
- **Entscheidung:** Die Rolle QA & Testing darf ohne Einzelerlaubnis Debug-/Test-Builds (`gradlew.bat assembleDebug`), Unit-, instrumentierte und Lint-Tests ausführen, Debug-APKs auf dem Emulator und auf vom Inhaber ausdrücklich benannten Testgeräten installieren und Builds nach Fehlerbehebungen wiederholen. Weiterhin ausgeschlossen: `build.bat`, Release-Builds, Signierung, Veröffentlichung, Änderungen an Code, Gradle-/Build-Dateien und `version.properties`. Andere Rollen erhalten keine Build-Rechte. Manuelle Benutzertests (persönliches Handy, produktive Profile/Daten) bleiben beim Inhaber; QA markiert sie nie selbst als bestanden.
- **Begründung:** Auftrag des Inhabers am 2026-11-10 (Chat Documentation): QA soll möglichst unabhängig von Development und Inhaber prüfen können. `build.bat` ist ausgenommen, weil es die Version erhöht und Release-Artefakte erzeugt.
- **Alternativen:** Builds weiter nur durch den Inhaber (DEC-214, für Debug-Builds verworfen); `build.bat -Debug` für QA (verworfen, ändert `version.properties`).
- **Konsequenzen:** `CLAUDE.md` §1, `coordination/README.md` §8, QA-Prompt, Agent `testing-qa-specialist`, Skills `qa-check`/`run-targeted-tests` und `docs/TESTING.md` §14 angepasst.
- **Betroffene Komponenten:** `CLAUDE.md`, `coordination/README.md`, `.claude/roles/QA-Specialist.md`, `.claude/agents/testing-qa-specialist.md`, `.claude/skills/`, `docs/TESTING.md`
- **Abhängigkeiten:** DEC-184, DEC-212, DEC-214

### DEC-215: UX prüft selbstständig und führt eine Navigationsdatei
- **Status:** Accepted
- **Datum:** 2026-11-10
- **Kategorie:** UI
- **Entscheidung:** Der UX-Specialist prüft alles, was er selbst im Emulator prüfen kann, selbstständig und ohne Rückfrage. Er pflegt `.claude/ux/navigation.md`: ein Abschnitt je Zielfenster mit der Wegbeschreibung ab der Startseite. Die Datei wird nie vollständig eingelesen; bei Orientierungsbedarf sucht er gezielt nach dem Zielfenster und liest nur dessen Abschnitt.
- **Begründung:** Anweisung des Inhabers am 2026-11-10; spart Rückfragen und vermeidet, dass jede Prüfung den Weg durch die App neu suchen muss.
- **Alternativen:** Vollständiges Einlesen der Datei bei jeder Prüfung (verworfen, Tokenaufwand).
- **Konsequenzen:** UX darf im Verzeichnis `.claude/ux/` schreiben (`CLAUDE.md` §1). Fehlt ein Weg, ergänzt UX die Datei eigenständig, sobald feststeht, wie man zum Zielfenster navigiert. Ändert sich ein Weg, passt UX nur den betroffenen Abschnitt an.
- **Betroffene Komponenten:** `.claude/ux/navigation.md`, `CLAUDE.md` §1, `.claude/roles/UX-Specialist.md`
- **Abhängigkeiten:** DEC-212
- **Ergänzung 2026-11-12 (Benutzerfreigabe, SYNC-20261112-276): Emulatorprüfungen UX und QA bündeln primär nach Schriftgrösse, dann nach App-Seite.** Zuerst alles in 1,0, danach aufsteigend 1,5 und 2,0; jede Schriftgrösse nur einmal einstellen, am Ende die Ausgangsgrösse wiederherstellen; innerhalb einer Schriftgrösse wie bisher seitenweise (bei UX darin Hell/Dunkel je Seite). Grund: Jeder Schriftgrössenwechsel startet die App neu, die Navigation beginnt wieder auf der Startseite. Ersetzt die bisherige Regel «Seitenweise bündeln». **Ausnahme:** Prüfungen, deren Zustand erst reproduziert werden muss (Fehlermeldungen, Toasts, Warnungen, Konflikte, Abbrüche), erledigen UX und QA direkt nach dem Herstellen in allen Schriftgrössen, solange der Zustand besteht (nur das Auslösen wiederholen, nicht die Vorbereitung); danach weiter in der bisherigen Schriftgrösse. Nebenpunkte (laut Auftrag, Wortlaut in den Rollenmodulen): QA-Screenshots wie bei UX (Orientierungsscreenshots löschen, geprüfte Elemente unter `.claude/AppScreenshots/archive` mit sprechendem Namen); Dunkelmodus auf `LeoSync_API29` über die Display-Einstellungen, `cmd uimode night` wirkt dort nicht. Verbindlicher Wortlaut: `.claude/roles/ux/pruefung.md`, `.claude/roles/qa/emulator.md`, `.claude/ux/navigation.md`.

### DEC-240: Teststruktur für Engine-Abläufe: `RemoteStore`, `LocalFiles`, ausgelagertes Wiederherstellen
- **Status:** Accepted (Benutzerfreigabe erteilt 2026-11-11; Umsetzung durch Development in SYNC-20261111-163 Stufe 2, Stand der Umsetzung nicht bestätigt)
- **Datum:** 2026-11-11 (Quelle SYNC-20261111-183, Chat Architecture)
- **Kategorie:** Tests / Architektur
- **Entscheidung:** `RemoteFiles` bleibt nur lesend (Löschweg Fotos ohne Schreibrechte). Neue Schnittstelle `RemoteStore : RemoteFiles` für Schreibzugriffe (u. a. `dirExists`, `listRecursive`, `ensureDir`, `move`, `delete`, `deleteDir`, Hash-Lesen sowie die Upload-/Download-Aufrufe des Wiederherstellens); `SmbSession` implementiert sie mit den vorhandenen Methoden. Schmale Schnittstelle `LocalFiles` nur für das Wiederherstellen (`LocalTree` implementiert sie 1:1). Das Wiederherstellen wird verhaltensgleich aus der `MirrorEngine` in eine eigene Klasse ausgelagert (z. B. `mirror/RestoreExecutor`, Absturz-Tests). `androidx.work:work-testing` nur als `androidTestImplementation` (kein Robolectric). Gemeinsamer `FakeShare` unter `app/src/test/java/com/sync2pc/app/` (bestehende Klasse erweitern). Die Protokolle `SmbSession.upload` und `LocalTree.writeFileResumable` bleiben; nur ihre Entscheidungen werden als reine Funktionen testbar.
- **Begründung:** Geringste Rechte für den Löschweg Fotos; Fehlerinjektion je Schritt des Wiederherstellens (datenverlustkritisch) wird testbar; der `FakeShare` besteht schon und wird von `DataIntegrityTest` genutzt.
- **Alternativen:** `RemoteFiles` um Schreiboperationen erweitern (verworfen: Löschweg Fotos bekäme Schreibrechte); allgemeine Schnittstelle für `LocalTree` / End-zu-End-Test der `MirrorEngine` auf der JVM (verworfen: sehr grosser Umbau wegen Room, Context, SAF); `testFixtures` (verworfen: Gradle-Aufwand ohne Nutzen); Robolectric (verworfen: WorkManager-Testtreiber braucht einen echten Context).
- **Konsequenzen:** B1/B5 laufen nur mit `connectedDebugAndroidTest`. Restrisiko: Die Auslagerung kann trotz «verhaltensgleich» Fehler einführen; abgesichert durch Spezialagent, Review, QA und neue Tests. Eintrag in `docs/TESTING.md` nach der Umsetzung (Übergabe Development aus SYNC-20261111-163 offen).
- **Betroffene Komponenten:** `smb/SmbSession.kt`, `mirror/LocalTree.kt`, `mirror/MirrorEngine.kt`, `app/src/test`, `app/build.gradle.kts`
- **Abhängigkeiten:** DEC-223

---

## 16. Verworfene und überholte Lösungsansätze

*Abgelehnt (Rejected)*

### DEC-057: Update-Quelle SMB-Ordner
- **Status:** Rejected
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Architektur
- **Entscheidung:** Abgelehnt; der Inhaber wählte GitHub Releases.
- **Begründung:** Wahl des Inhabers.
- **Alternativen:** „Beides“ (ebenfalls nicht gewählt).
- **Konsequenzen:** Updates brauchen Internet.
- **Betroffene Komponenten:** —
- **Abhängigkeiten:** DEC-056

### DEC-058: Automatische Update-Installation im Hintergrund
- **Status:** Rejected
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Android
- **Entscheidung:** Abgelehnt zugunsten von „Tippen installiert“.
- **Begründung:** Wahl des Inhabers.
- **Alternativen:** —
- **Konsequenzen:** Bei jedem Update ist eine Bestätigung nötig.
- **Betroffene Komponenten:** —
- **Abhängigkeiten:** DEC-056

### DEC-062: Animiertes GIF direkt in der Benachrichtigung
- **Status:** Rejected
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** UI
- **Entscheidung:** Abgelehnt, weil technisch nicht möglich.
- **Begründung:** Android-Benachrichtigungen spielen keine GIFs oder Videos ab.
- **Alternativen:** ViewFlipper (DEC-060).
- **Konsequenzen:** —
- **Betroffene Komponenten:** —
- **Abhängigkeiten:** DEC-060

### DEC-069: Verschieben statt Kopieren im Import
- **Status:** Rejected
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Sonstiges
- **Entscheidung:** Abgelehnt; der Quellordner bleibt unverändert.
- **Begründung:** Wahl des Inhabers („Kopieren“).
- **Alternativen:** —
- **Konsequenzen:** Den Quellordner löscht der Inhaber selbst.
- **Betroffene Komponenten:** —
- **Abhängigkeiten:** DEC-068

### DEC-070: Feste Regel „Fotos ohne GPS eines Monats in den Länderordner“
- **Status:** Rejected
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Synchronisation
- **Entscheidung:** Abgelehnt: „nein, vorerst nicht“.
- **Begründung:** Antwort des Inhabers.
- **Alternativen:** —
- **Konsequenzen:** Fotos ohne GPS landen weiterhin im reinen Monatsordner.
- **Betroffene Komponenten:** —
- **Abhängigkeiten:** DEC-016, DEC-019

### DEC-071: Beidseitiger Sync für Bildordner
- **Status:** Rejected
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Synchronisation
- **Entscheidung:** Bildordner bleiben Handy → PC.
- **Begründung:** Antwort 4 „einverstanden“: Die Sortierung nach Jahr, Monat und Land ergibt nur in eine Richtung Sinn.
- **Alternativen:** Bilder vom PC flach in den Handy-Ordner (vorgeschlagen, nicht gewählt).
- **Konsequenzen:** —
- **Betroffene Komponenten:** `Profile.dir()`
- **Abhängigkeiten:** DEC-034

### DEC-112: R8-Verschleierung abschalten gegen die Play-Protect-Sperre
- **Status:** Rejected
- **Datum:** 2026-11-08 (2026-11-02)
- **Kategorie:** Sicherheit
- **Entscheidung:** Zurückgezogen, bevor der Inhaber entschied.
- **Begründung:** Die Sperre betrifft auch alte Builds; sie hängt nicht am Code-Inhalt eines Builds.
- **Alternativen:** Einspruch (DEC-111).
- **Konsequenzen:** Minify und Verschleierung bleiben (DEC-014).
- **Betroffene Komponenten:** —
- **Abhängigkeiten:** DEC-014, DEC-111

### DEC-114: Schnellzugriff (Kachel oder Widget) „Jetzt synchronisieren“
- **Status:** Rejected
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** UI
- **Entscheidung:** Abgelehnt („nein“).
- **Begründung:** Antwort des Inhabers.
- **Alternativen:** —
- **Konsequenzen:** —
- **Betroffene Komponenten:** —
- **Abhängigkeiten:** —

### DEC-115: „Pausiert“-Anzeige nach WLAN-Abbruch kürzer als 24 Stunden halten
- **Status:** Rejected
- **Datum:** 2026-11-08 (2026-11-02)
- **Kategorie:** Performance
- **Entscheidung:** Abgelehnt („nicht umsetzen“); die 24 Stunden bleiben.
- **Begründung:** Würde das Fortsetzen auf dem OnePlus gefährden.
- **Alternativen:** —
- **Konsequenzen:** —
- **Betroffene Komponenten:** `PauseService`
- **Abhängigkeiten:** DEC-052

### DEC-116: Prüfintervall der Ordner-Profile seltener bzw. nur beim Laden
- **Status:** Rejected
- **Datum:** 2026-11-08 (2026-11-02)
- **Kategorie:** Performance
- **Entscheidung:** „Erstmal nicht umsetzen“ – vorerst verworfen.
- **Begründung:** Antwort des Inhabers.
- **Alternativen:** Test-Profil deaktivieren, wenn nicht gebraucht.
- **Konsequenzen:** Aktive Ordner-Profile lesen weiterhin standardmässig stündlich den PC-Ordner ein.
- **Betroffene Komponenten:** `SyncScheduler.scheduleRemotePoll`
- **Abhängigkeiten:** DEC-042

### DEC-117: Bearbeitete Fotos ersetzen die ältere Version auf dem PC
- **Status:** Rejected
- **Datum:** 2026-11-08 (2026-11-02)
- **Kategorie:** Synchronisation
- **Entscheidung:** Abgelehnt („nein, ist ok“); beim Fotos-Sync bleibt `bild_2.jpg` neben dem alten Foto.
- **Begründung:** Gleiche Namen können verschiedene Fotos sein; nie überschreiben. Beim Ordner-Sync überschreibt eine geänderte Datei ohnehin die andere Seite (DEC-035, DEC-037).
- **Alternativen:** —
- **Konsequenzen:** —
- **Betroffene Komponenten:** —
- **Abhängigkeiten:** DEC-020

### DEC-118: Dateiattribute (z. B. Windows-Archiv-Bit) auswerten
- **Status:** Rejected
- **Datum:** 2026-11-08 (2026-11-02)
- **Kategorie:** Synchronisation
- **Entscheidung:** Keine Auswertung von Attributen; Änderungen werden über Grösse, Änderungsdatum und Teil-Prüfsumme erkannt.
- **Begründung:** Android kennt keine solchen Attribute; Windows setzt das Archiv-Bit für neue Dateien selbst. Keine Überschneidung mit Google Backup (`allowBackup=false`) oder Google Fotos.
- **Alternativen:** —
- **Konsequenzen:** Risiko mit Google Fotos „Speicherplatz freigeben“ siehe D.
- **Betroffene Komponenten:** —
- **Abhängigkeiten:** DEC-021, DEC-035

### DEC-122: Feedback über einen Discord-Webhook
- **Status:** Rejected
- **Datum:** 2026-11-08 (2026-11-02)
- **Kategorie:** Sicherheit
- **Entscheidung:** Verworfen („ja dann ohne discord“).
- **Begründung:** Eine Webhook-URL in der APK ist für jeden auslesbar und kann missbraucht werden; sicher ginge es nur mit eigenem Server.
- **Alternativen:** E-Mail-Knopf (DEC-121).
- **Konsequenzen:** —
- **Betroffene Komponenten:** —
- **Abhängigkeiten:** DEC-121

### DEC-198: Englische Texte behalten „sync“
- **Status:** Rejected
- **Datum:** 2026-11-08 (2026-11-04)
- **Kategorie:** UI
- **Entscheidung:** Das Ausschreiben von „sync“ zu „synchronization“ im Englischen wurde verworfen.
- **Begründung:** Inhaber: „Englisch so lassen“; „sync“ ist im Englischen üblich.
- **Alternativen:** —
- **Konsequenzen:** Unterschiedliche Kürzungsregeln je Sprache.
- **Betroffene Komponenten:** `res/values/strings_*.xml`
- **Abhängigkeiten:** DEC-170

### DEC-199: Statuskachel ganz fest mit gekürzten Meldungen
- **Status:** Rejected
- **Datum:** 2026-11-08 (2026-11-03)
- **Kategorie:** UI
- **Entscheidung:** Eine vollständig feste Kachelhöhe, bei der lange Meldungen mit „…“ gekürzt werden, wurde nicht gewählt.
- **Begründung:** Der Inhaber wählte die Mindesthöhe, bei der die Kachel in seltenen Fällen wachsen darf.
- **Alternativen:** Mindesthöhe (DEC-168).
- **Konsequenzen:** —
- **Betroffene Komponenten:** —
- **Abhängigkeiten:** DEC-168

*Überholt (Superseded)*

### DEC-006: Zugangsdaten aus einem anderen Profil übernehmen
- **Status:** Superseded (durch DEC-187: ein Knopf mit zusammengefasster Liste statt eines Chips pro Profil; die Übernahmeregeln gelten weiter)
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** UI
- **Entscheidung:** Im Profil-Editor übernimmt ein Tippen auf ein anderes Profil Benutzer, Passwort und Domäne; den Share-Pfad nur, wenn er noch leer ist.
- **Begründung:** Wunsch des Inhabers (0.0.6).
- **Alternativen:** keine.
- **Konsequenzen:** Das Passwort wird entschlüsselt kopiert und beim Speichern neu verschlüsselt.
- **Betroffene Komponenten:** `ui/ProfileScreen.kt`
- **Abhängigkeiten:** DEC-004

### DEC-007: Interne App-ID `com.sync2pc.app` beibehalten
- **Status:** Superseded (durch DEC-110; der Wechsel ist beschlossen, aber noch nicht umgesetzt – bis dahin gilt `com.sync2pc.app`)
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Android
- **Entscheidung:** applicationId und Paketname bleiben `com.sync2pc.app`; der Debug-Build trägt das Suffix `.debug`.
- **Begründung:** Die ID war im Play Store frei. Beim Umbenennen (0.0.12) wollte der Inhaber nur den sichtbaren Namen ändern, damit Updates, Profile und Daten erhalten bleiben.
- **Alternativen:** `ch.sync2pc.app` (vorgeschlagen, nicht gewählt); eine neue ID wie `ch.leo.sync` wurde ausdrücklich abgelehnt (Neuinstallation und Datenverlust).
- **Konsequenzen:** Interne Namen (`sync2pc.db`, Keystore-Alias) bleiben unverändert.
- **Betroffene Komponenten:** `app/build.gradle.kts`
- **Abhängigkeiten:** DEC-008; siehe B (spätere Roadmap)

### DEC-010: Versionierung 0.0.x bis zur Freigabe von 1.0.0
- **Status:** Superseded (durch DEC-077; Formel für versionCode, PATCH-Erhöhung und Auto-Erhöhung des Builders gelten weiter)
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Sonstiges
- **Entscheidung:**
  - Die Version steht in `version.properties`; versionCode = MAJOR·1 000 000 + MINOR·1 000 + PATCH.
  - Bei jeder Code-Änderung wird PATCH erhöht und `PATCHNOTES.md` nachgeführt.
  - Der Builder erhöht PATCH automatisch, wenn die APK der aktuellen Version schon existiert.
- **Begründung:** Vorgabe des Inhabers (Regeln 5 und 6).
- **Alternativen:** —
- **Konsequenzen:** `version.properties` zählt nicht zum Quell-Hash; nach einer Änderung von `UPDATE_REPO` ist deshalb `-Force` nötig.
- **Betroffene Komponenten:** `version.properties`, `app/build.gradle.kts`, `tools/build.ps1`
- **Abhängigkeiten:** DEC-011

### DEC-012: Build nur mit Java 17–24, sonst portables JDK 21
- **Status:** Superseded (durch DEC-085: seit 0.1.3 Java 17–25)
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Sonstiges
- **Entscheidung:** Der Builder akzeptiert nur JDK 17 bis 24. Fehlt ein passendes, lädt er Temurin 21 nach `.build/jdk21`.
- **Begründung:** Android Studio brachte JDK 25 mit; Gradle 8.11 bricht damit ab („What went wrong: 25.0.4“).
- **Alternativen:** Upgrade auf Gradle 9/AGP 9 (als langfristige Option genannt, DEC-076).
- **Konsequenzen:** Zum Öffnen des Projekts in Android Studio muss dort das Gradle-JDK umgestellt werden.
- **Betroffene Komponenten:** `tools/build.ps1`, `build.md`
- **Abhängigkeiten:** DEC-076; siehe B

### DEC-015: Minify deaktiviert lassen
- **Status:** Superseded (durch DEC-014)
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Performance
- **Entscheidung:** In 0.0.1 war Minify aus.
- **Begründung:** Reflection in smbj und BouncyCastle.
- **Alternativen:** R8 mit Keep-Regeln.
- **Konsequenzen:** —
- **Betroffene Komponenten:** `app/build.gradle.kts`
- **Abhängigkeiten:** DEC-014

### DEC-024: Prüfung nach dem Upload, Pflicht vor dem Löschen
- **Status:** Superseded (durch DEC-171: Prüfstufen statt Ein/Aus; die bisherige Prüfung entspricht der Stufe „Wenig“, Pflicht beim Löschen bleibt)
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Synchronisation
- **Entscheidung:** Nach dem Upload werden die Grösse und die Teil-Prüfsumme der Zieldatei verglichen. Die Prüfung ist optional (Standard an) und Pflicht, wenn nach dem Sync gelöscht werden soll. Schlägt sie fehl, wird die Zieldatei gelöscht und die Datei neu versucht.
- **Begründung:** Vorschlag 4: „wenn löschen ausgewählt ist, sicher verifizieren“.
- **Alternativen:** —
- **Konsequenzen:** Zusätzlich werden pro Datei 192 KB vom Share gelesen.
- **Betroffene Komponenten:** `Profile.mustVerify`, `SyncEngine.transfer`
- **Abhängigkeiten:** DEC-021, DEC-025

### DEC-032: Eigene Ordnerliste aus dem MediaStore
- **Status:** Superseded (durch DEC-031)
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** UI
- **Entscheidung:** Auswahldialog mit allen MediaStore-Ordnern und Dateizahl (0.0.1–0.0.2).
- **Begründung:** —
- **Alternativen:** Systemdialog.
- **Konsequenzen:** `MediaScanner.listFolders` ist noch vorhanden.
- **Betroffene Komponenten:** `MediaScanner.listFolders`
- **Abhängigkeiten:** DEC-031

### DEC-039: Unterordner auf dem Share pro Profil mit Ordner-Browser
- **Status:** Superseded (durch DEC-192: kein eigenes Feld mehr, Freigabe und Ordner werden im PC-Browser gewählt; `remoteSubfolder` bleibt in der DB und wird beim Öffnen in den Pfad übernommen)
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** UI
- **Entscheidung:** Das Feld „Unterordner“ wird an den Basispfad angehängt und lässt sich über einen Share-Browser wählen.
- **Begründung:** Antwort 6: „Option, in einen Unterordner des Shares zu syncen“.
- **Alternativen:** —
- **Konsequenzen:** Gilt für beide Profiltypen; `SmbSession.targetOf` berücksichtigt den Unterordner überall.
- **Betroffene Komponenten:** `Profile.remoteSubfolder`, `SmbSession.targetOf`, `ui/ProfileParts.RemoteFolderPicker`
- **Abhängigkeiten:** —

### DEC-046: Einzelner 3-Sekunden-Erreichbarkeitstest
- **Status:** Superseded (durch DEC-045)
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Synchronisation
- **Entscheidung:** In 0.0.4 gab es genau einen Test; danach folgte direkt die 30-Minuten-Wartezeit.
- **Begründung:** —
- **Alternativen:** Wiederholungen.
- **Konsequenzen:** Er war die Ursache für ausbleibende Syncs nach dem WLAN-Verbinden.
- **Betroffene Komponenten:** `SyncWorker`
- **Abhängigkeiten:** DEC-045

### DEC-047: Wiederholung über WorkManager-Backoff
- **Status:** Superseded (durch DEC-048 und DEC-053)
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Android
- **Entscheidung:** `Result.retry()` mit exponentiellem Backoff (ab 2 Minuten, 3 Versuche) bis 0.0.3.
- **Begründung:** —
- **Alternativen:** feste Wiederholungslogik.
- **Konsequenzen:** —
- **Betroffene Komponenten:** `SyncWorker`, `SyncScheduler`
- **Abhängigkeiten:** DEC-048

### DEC-051: Eigene Benachrichtigung „Sync unterbrochen“
- **Status:** Superseded (durch DEC-052)
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** UI
- **Entscheidung:** Eine wegwischbare Hinweis-Benachrichtigung bei WLAN-Unterbruch (0.0.9).
- **Begründung:** —
- **Alternativen:** Anzeige „Pausiert“.
- **Konsequenzen:** Der Inhaber: „Eine Benachrichtigung bei WLAN-Unterbruch braucht es nicht, das ist erwartbares Verhalten.“
- **Betroffene Komponenten:** `Notifications.cancelInterrupted` (Aufräumen alter Hinweise)
- **Abhängigkeiten:** DEC-052

### DEC-061: Animation aus dem Sprite-Sheet `gif_frames.png`
- **Status:** Superseded (durch DEC-060)
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** UI
- **Entscheidung:** 4×4 Einzelbilder mit Alpha-Bereinigung (0.0.8).
- **Begründung:** —
- **Alternativen:** —
- **Konsequenzen:** Bleibt als Rückfall im Builder, falls kein Video vorhanden ist.
- **Betroffene Komponenten:** `tools/make-anim.ps1`
- **Abhängigkeiten:** DEC-060

### DEC-073: Papierkorb automatisch leeren (z. B. nach 30 Tagen)
- **Status:** Superseded (durch DEC-083, in 0.1.0 umgesetzt)
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Synchronisation
- **Entscheidung:** Der Papierkorb des Ordner-Syncs würde nach einer einstellbaren Frist automatisch geleert.
- **Begründung:** Der Papierkorb wächst sonst unbegrenzt.
- **Alternativen:** —
- **Konsequenzen:** —
- **Betroffene Komponenten:** —
- **Abhängigkeiten:** DEC-036

### DEC-074: Parallele Übertragung im Ordner-Sync
- **Status:** Superseded (durch DEC-093, in 0.3.0 umgesetzt)
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Performance
- **Entscheidung:** Der Ordner-Sync würde wie der Bildordner-Sync mehrere Dateien gleichzeitig übertragen.
- **Begründung:** Gleiche Beschleunigung wie bei den Fotos.
- **Alternativen:** —
- **Konsequenzen:** —
- **Betroffene Komponenten:** —
- **Abhängigkeiten:** DEC-029, DEC-035

### DEC-076: Upgrade auf Gradle 9 bzw. AGP 9 (Java-25-tauglich)
- **Status:** Superseded (durch DEC-085, in 0.1.3 umgesetzt)
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Sonstiges
- **Entscheidung:** Als langfristige Alternative zum JDK-Workaround genannt.
- **Begründung:** Der Workaround aus DEC-012 wäre damit überflüssig.
- **Alternativen:** —
- **Konsequenzen:** —
- **Betroffene Komponenten:** —
- **Abhängigkeiten:** DEC-012 (siehe B)

### DEC-113: Instrumentierter Migrationstest erst vor 1.0.0
- **Status:** Superseded (durch DEC-180: in 0.9.4 vorgezogen und umgesetzt)
- **Datum:** 2026-11-08 (2026-11-01)
- **Kategorie:** Datenbank
- **Entscheidung:** Ein Test mit `MigrationTestHelper` (v1→v2→v3 …) wird vor 1.0.0 umgesetzt, nicht in der Beta.
- **Begründung:** Antwort des Inhabers: „könnte man später machen, die User werden erst ab 1.0.0 versorgt“.
- **Alternativen:** sofort umsetzen (vorgeschlagen).
- **Konsequenzen:** Braucht ein Gerät; der Builder führt ihn nicht aus.
- **Betroffene Komponenten:** geplant: `app/src/androidTest`
- **Abhängigkeiten:** DEC-084

### DEC-125: Versionsplan 0.6.0 bis 0.9.0
- **Status:** Superseded durch DEC-165 (Session 4: Lokalisierung als 0.8.0); laut Memory später erneut geändert (0.10.0 Pull-to-Sync, 0.11.0 Play Store, 0.12.0 PC-Erkennung; nicht aus diesen Verläufen geprüft)
- **Datum:** 2026-11-08 (2026-11-03)
- **Kategorie:** Sonstiges
- **Entscheidung:** 0.6.0 Papierkorb leeren; 0.6.x Papierkorb-Zusammenlegung; 0.7.0 Speicher freigeben für Ordner-Profile plus Fixes; 0.8.0 Play-Store-Vorbereitung; 0.9.0 PC-Erkennung plus mobile Daten/VPN.
- **Begründung:** Antworten des Inhabers („0.7.0 ist ok“, „3. ok“).
- **Alternativen:** Play-Store-Vorbereitung als 0.7.0 (ursprünglicher Plan).
- **Konsequenzen:** —
- **Betroffene Komponenten:** `.claude/README.md` (Roadmap)
- **Abhängigkeiten:** DEC-109

### DEC-165: Versionsplan mit Lokalisierung als 0.8.0
- **Status:** Superseded (durch DEC-188, Session 5: 0.9.x UX-Überarbeitung, 0.10.0 Herunterziehen, 0.11.0 Design-Prüfung, 0.12.0 Play Store, 0.13.0 PC-Erkennung)
- **Datum:** 2026-11-08 (2026-11-03)
- **Kategorie:** Sonstiges
- **Entscheidung:** 0.8.0 Lokalisierung; Play-Store-Vorbereitung rückt auf 0.9.0, PC-Erkennung auf 0.10.0.
- **Begründung:** Rückfrage 6: „einverstanden“.
- **Alternativen:** Lokalisierung als Teil der Play-Store-Vorbereitung.
- **Konsequenzen:** —
- **Betroffene Komponenten:** `.claude/README.md` (Roadmap), `version.properties`
- **Abhängigkeiten:** DEC-125 (ersetzt)

---

## A. Offene Entscheidungen

1. **Vollständigkeitsbilanz** (DEC-072): Inhaber 2026-11-08: Anzahl gesicherter und offener Dateien unter dem Profil anzeigen. Umgesetzt in 0.11.1/0.11.2 (SYNC-20261108-002), eingeschränkt: siehe DEC-072.
2. **`release.bat`** (DEC-075): Inhaber 2026-11-08: abgelehnt, Pushes führt er selbst aus. *(Papierkorb-Aufräumen und paralleler Ordner-Sync sind seit Session 2 umgesetzt: DEC-083, DEC-093.)*
3. ~~Gradle/AGP-Upgrade~~ – erledigt in Session 2 (DEC-085).
4. **Dateianzahl in der Ordnerauswahl anzeigen** (Vorschlag aus 0.0.3): keine Antwort.
5. **NDK-Strip-Warnung stummschalten:** angeboten, keine Antwort; Inhaber fragte 2026-11-08 nach der Bedeutung. Im Repository nicht weiter belegt. Inhaber 2026-11-08: vorerst pending.
6. **Wirksamkeit des Fortsetzens auf dem Gerät des Inhabers:** In Session 2 geklärt: Gerät OnePlus 9 Pro (OxygenOS 14); der Oplus-Killer beendete die App auch im Vordergrunddienst. Nach „Hintergrundaktivität zulassen“ keine neuen Kills; Fortsetzen nach WLAN-Trennung im Feldtest bestätigt. Ob „Automatischer Start“ zusätzlich gesetzt wurde, ist offen.
7. ~~**HEIC im Import-Skript:**~~ – Inhaber 2026-11-08: Das Skript war nur für den Inhaber, Punkt wird ignoriert.
8. **Verhalten nach dem 6-Stunden-Limit des dataSync-Dienstes (Android 15+):** Rückfall vorhanden, Wirksamkeit offen; Teil der Play-Store-Vorbereitung (DEC-109).
9. ~~**Versionsnummer der Play-Store-Vorbereitung**~~ – erledigt: DEC-125 (Session 3), später durch DEC-188 ersetzt (0.12.0 Play Store, 0.13.0 PC-Erkennung).
10. ~~**Kontakt-E-Mail**~~ – erledigt: `leotools@protonmail.com` (DEC-123).
11. **Play-Protect-Einspruch** (DEC-111): Text vorbereitet; ob eingereicht und mit welchem Ergebnis, ist offen.
12. **Wirksamkeit der Erinnerung „lange nicht gesichert“** (DEC-090): auf dem Gerät noch nicht beobachtet.
13. ~~**Verwendung der Server-GUID** (DEC-109)~~ – erledigt: Inhaber 2026-11-09 Vorgabe freigegeben, als DEC-204 festgelegt (Ablauf bei Kennungswechsel dort, Punkt 3); Umsetzung offen (Development SYNC-20261109-047).

*Session 3:*

14. **WLAN-Name Pflicht oder optional** (Frage des Inhabers für VPN-Nutzer): nur als Option „mobile Daten/VPN“ vorgemerkt (DEC-126); die Antwort im Verlauf ist nicht eindeutig belegt. Inhaber 2026-11-08: Schalter für automatische Synchronisierung auch ausserhalb des WLAN; umgesetzt in 0.11.5/0.11.6 (SYNC-20261108-003, siehe DEC-126). Offen: Wiederholungen über Mobilfunk begrenzen (SYNC-20261108-019).
15. **Leere Ordner nach „Speicher freigeben“** (DEC-147): bis 0.11.7 blieben sie stehen. Inhaber 2026-11-08: aufräumen, keine leeren Ordner zurücklassen, Ausnahme `DCIM\Camera` (SYNC-20261108-004). **Umgesetzt in 0.11.8.**
16. **Fehlgeschlagene Wiederherstellungen** (DEC-146): bis 0.11.7 bei jedem Sync erneut versucht. Inhaber 2026-11-08: 5 Versuche, danach Meldung an den Benutzer (SYNC-20261108-005). **Umgesetzt in 0.11.8.**
17. **Warten auf den laufenden Sync** (DEC-144): Verlässt man die Ansicht, unterbleibt der Eingriff; ob er stattdessen vorgemerkt werden soll, ist offen. Inhaber 2026-11-08: Eingriff vormerken und später ausführen, Umsetzung freigegeben (SYNC-20261108-012); Vorgabe als DEC-201 festgehalten.
18. **Umsetzung von „Problem melden“ und Datenschutzerklärung** (DEC-121, DEC-123): Inhaber 2026-11-08: „Problem melden“ umgesetzt in 0.11.5 (Kontakt `leotools@protonmail.com`, SYNC-20261108-008, siehe DEC-121); Datenschutzerklärung ist umgesetzt (`PRIVACY_GER.md`, `PRIVACY_EN.md`).

*Session 4:*

19. ~~**Verlauf mit Kennungen statt fertiger Texte** (DEC-159)~~ – erledigt: Inhaber 2026-11-08 Umsetzung freigegeben, Vorgabe als DEC-202; umgesetzt in 0.12.0 (Stufe 1) und 0.12.1 (Stufe 2), SYNC-20261108-009.
20. **Englische Übersetzungen prüfen** (DEC-166): Tabelle liegt vor; ob und welche Korrekturen der Inhaber meldet, ist offen.
21. **Sprachwechsel auf Android 10–12** (DEC-158): eigener Weg über `createConfigurationContext`; auf dem Gerät nicht getestet.

*Session 5:*

22. ~~**Sicherheitsprüfung mit claude-security** (DEC-200)~~ – Inhaber 2026-11-08: wird nicht umgesetzt, verworfen. (Status von DEC-200 selbst unverändert; Anpassung ggf. durch Architecture.)
23. **Ungeprüfte Teile von 0.9.5–0.11.0:** Restzeit im echten Lauf, Ansagen mit TalkBack, Berechtigungen bei einer Neuinstallation, strengere Hintergrund-Kontingente unter Android 16 bei langen Erstsicherungen.
24. **Ordner anlegen in der PC-Ordnerauswahl** (DEC-192): heute muss der Zielordner auf dem PC existieren; ob die Auswahl Ordner anlegen können soll, war nicht entschieden. ~~erledigt~~: Inhaber 2026-11-08 „Ordner anlegen“, umgesetzt in 0.11.3 (SYNC-20261108-006, DEC-192 Nachtrag).
25. **Nicht mehr verwendete Texte** (z. B. `profile_subfolder*`, `profile_browse`, `settings_battery_off`, `profile_delete_cd`): aufräumen oder behalten ist nicht entschieden. Stand 2026-11-08: Die Texte stehen in beiden Sprachdateien, werden im Kotlin-Code aber nicht verwendet (geprüft per Suche in `app/src`); Inhaber 2026-11-08: aufräumen (SYNC-20261108-013). **Umgesetzt in 0.11.8:** entfernt (EN/DE) `profile_subfolder`, `profile_subfolder_example_mirror`, `profile_subfolder_example_photos`, `profile_browse`, `settings_battery_off`, `profile_delete_cd`; vorher per Suche in `app/src` nur Definitionen gefunden. `profile_subfolders*` bleibt.
26. ~~**Konfliktkarten eines Profils, das gerade gelöscht wird** (DEC-196)~~ – erledigt: Inhaber 2026-11-08 ausblenden, umgesetzt in 0.11.1 (SYNC-20261108-007).
27. **Hilfe-Texte auf der Produktseite** (DEC-197): Einbindung durch den Inhaber; Inhaber 2026-11-08: pending.
28. **Heimnetz-Berechtigung ab targetSdk 37** (DEC-183): voraussichtlich nötig für die Verbindung zur Freigabe; Inhaber 2026-11-08: bleibt offen (frühestens mit der nächsten Ziel-Version).

## B. Änderungsbedarf an bestehenden Entscheidungen

Die Punkte 1 bis 4 beruhen nicht auf dieser Session, sondern auf dem aktuellen Dateistand und dem Memory; sie sind zu prüfen:

1. ~~**DEC-012 und DEC-076:** Gradle 9 / AGP 9~~ – erledigt: als DEC-085 erfasst, DEC-012 und DEC-076 sind Superseded.
2. ~~**DEC-007 (App-ID):**~~ – erledigt: Beschluss `ch.leo.sync` als DEC-110 erfasst, DEC-007 ist Superseded (Umsetzung steht aus).
3. **DEC-001 (nur Deutsch) und DEC-040 (Begriffe White- und Blacklist):** ~~DEC-001~~ erledigt in Session 4 (DEC-155). DEC-040: Das Memory nennt „keine Whitelist/Blacklist“ und die Begriffe „Mobile/Share“; in Session 4 hiessen die Filter noch Whitelist/Blacklist. Das stammt aus einer späteren Session und ist dort zu erfassen.
4. **DEC-021 und DEC-024 (Teil-Prüfsumme):** `PartialHash` enthält inzwischen gründliche Vergleiche und Prüfstufen (0.4.1/0.9.0). Der Algorithmus „Wenig“ muss mit dem Import-Skript (`LeoSyncImport.cs`) identisch bleiben.
5. ~~**DEC-013 (Arbeitsregeln):**~~ – erledigt: „Kurzfassung nur auf Aufforderung“ als DEC-119 erfasst, DEC-013 aktualisiert.
6. **DEC-048:** Innerhalb der Session teilweise angepasst; bei Abbruch mitten im Lauf gilt 2 statt 30 Minuten. Geprüft (SYNC-20261108-010, Development): DEC-048 und `SYNC-ENGINE.md` stimmen mit dem Code überein.
7. ~~**DEC-092 (doppelte Profile):**~~ – erledigt: verschachtelte Ordner-Profile als DEC-134 erfasst.
8. ~~**Mehrsprachigkeit:**~~ – erledigt: Session 4 (DEC-155 bis DEC-166); DEC-001, DEC-099 aktualisiert.
9. ~~**DEC-125 (Versionsplan):**~~ – erledigt in Session 5: DEC-188 (0.12.0 Play Store, 0.13.0 PC-Erkennung); DEC-165 verweist darauf.
10. ~~**DEC-149 und DEC-153:**~~ – erledigt in Session 5: Statistik-Symbol und Änderungen an `FreeSpaceScreen` als DEC-175 und DEC-178 erfasst. *(Unklar bleibt, ob `FolderFreer`/`TrashCleaner` in Session 5 geändert wurden; im Verlauf nicht belegt.)*
11. ~~**DEC-040 (Begriffe):**~~ – erledigt in Session 5 (DEC-169); Punkt 3 damit abgeschlossen.
12. **DEC-021 und DEC-024 (Teil-Prüfsumme):** Prüfstufen als DEC-171 erfasst. Weiterhin gilt: Der Algorithmus „Wenig“ muss mit dem Import-Skript (`LeoSyncImport.cs`) identisch bleiben (Punkt 4); geprüft, siehe Punkt 4. Geprüft (SYNC-20261108-011, Development): „Wenig“ in `PartialHash.kt` und `LeoSyncImport.cs` identisch; DEC-021/-171 korrekt, kein Änderungsbedarf.

## C. Dokumentationsbedarf

- **ARCHITECTURE.md:**
  - Zwei Engines hinter `SyncRunner`, Worker, Auslöser-Kette (Receiver, WorkManager, PauseService) und Mutex.
  - Rollen von `TransferLocks`, `PauseService` und den Retry- und Resume-Aufträgen.
  - Bewusst unveränderte interne Kennungen (Paket, DB, Keystore-Alias).
- **SYNC-ENGINE.md:**
  - Ablauf der Ordnerauflösung (Mapping, Standardname, Kandidaten, neu), Regel für erzeugte Namen, `seen`-Flag.
  - Duplikatsuche über alle Monatsordner.
  - Spezifikation der Teil-Prüfsumme (Big-Endian-Grösse, Bereiche), Temp-Datei und Fortsetzen.
  - Entscheidungstabelle des 3-Wege-Abgleichs (L/R/S × Richtung × `propagateDeletes` × `ConflictMode`), Papierkorb-Regeln.
  - Fehlerklassen (Netz gegenüber Datei), Status `INTERRUPTED`, Zeitregeln (1 Minute, 2 Minuten, 30 Minuten, Resume 15 Minuten, höchstens 24 Stunden).
- **DATA-MODEL.md:**
  - Alle Tabellen und Schlüssel; `DeleteState`-Werte (0/1/2/3/4); `ConflictResolution`.
  - `MIGRATION_1_2`; Aufbewahrungsfristen.
  - `GlobalSettings`-Schlüssel (`resumeOnWifi`, `shareRetryAt`, `noteSkip`, `updateInstalling` …).
- **TESTING.md:**
  - Vorhandene Unit-Tests (FolderNames, FilenameDateParser, SmbTarget, PartialHash, FileFilter).
  - Manuelle Feldtests: WLAN-Unterbruch mit und ohne Wegwischen, Standby, Windows-Leerlauftrennung, Update-Pfad v→v+1.
  - Test der Migration v1→v2, R8-Release-Test, Probelauf des Import-Skripts mit Testordnern.
  - *Session 2:* Unit-Tests TrashPolicy, Diagnostics (compress, VendorHints, CrashReporter), BackupCodec (inkl. Export-Passwort, Altformate), ProfileOverlap, TransferPlan, LocalTreeLabel, RunSummary/RunGrouping; `org.json` als Test-Abhängigkeit.
  - Release-Checkliste in `build.md` und Testanleitung „Test Ordner-Sync“; Migrationstest vor 1.0.0 (DEC-113).
- *Session 2 zusätzlich:*
  - **ARCHITECTURE.md:** Paket `diag/` (Diagnose, Absturzbericht, Hersteller-Hinweise), `backup/`; Navigation `Screen.Settings(section)`, `Screen.Preview(profileId)`; Zustände Benutzer-Pause (`pausedUntil`, `manualOnly`) gegenüber WLAN-Pause (`resumeOnWifi`); Auslöser `trigger-safety`, `pause-end`, `stale-check`; Benachrichtigungs-Schalter.
  - **SYNC-ENGINE.md:** Zwei Phasen des Ordner-Syncs, Reihenfolge, Download-Fortsetzung (`resumeOffset`), Papierkorb-Frist; `PendingCheck` vor dem PC-Kontakt; `stopReason`-Auswertung; Lauf-Zusammenfassungen und `isIdle`-Regel; `previewLimit` im Trockenlauf; Freigabe neuer Profile.
  - **DATA-MODEL.md:** DB-Version 3 (`trashRetentionDays`, `MIGRATION_2_3`), Migrationsregeln (DEC-084); neue `GlobalSettings`-Schlüssel (`deviceId`, `stale*`, `freeSpace*`, `pausedUntil`, `manualOnly`, `awaitingFirstStart`, `previewLimit`, `logFilter*`, `notify*`); Sicherungsformat `leosync-backup` v1.
- *Session 3 zusätzlich:*
  - **ARCHITECTURE.md:** `MirrorLock` (Sperre Sync ↔ Eingriffe von Hand); Tools `Screen.Trash` und `Screen.FreeSpace`; Paket-Zuordnung `mirror/FolderFreer`, `mirror/TrashCleaner`, `mirror/TrashPurge`, `sync/FreeRules`.
  - **SYNC-ENGINE.md:** Reihenfolge eines Ordner-Laufs: offene Schutzbremse → Handy-Ordner prüfen (`LOCAL_MISSING`) → SMB öffnen, PC-Ordner prüfen (`REMOTE_MISSING`) → vorgemerkte Wiederherstellungen → vorgemerktes PC-Leeren → Einlesen mit `.s2pold`-Recovery → Stichprobe nach Ordnerwechsel → „Nicht löschen“ → Papierkorb-Frist → Planung → Massenlösch-Prüfung → Phase A/B. Dazu Schwellen der Schutzbremse, Versionsschutz (`trashOther`), Rollback beim Wiederherstellen, Namensschema im Papierkorb, Regel „stat() und children() nie mischen“, Sicherheitsargument für „Speicher freigeben“ bei Ordnern (DROP statt TRASH).
  - **DATA-MODEL.md:** DB-Version 4 (`massDeletePercent`, `MIGRATION_3_4`) und 5 (`keepVersions`, Tabelle `pending_restores`, `MIGRATION_4_5`); DAO-Abfrage `pendingRestoreProfiles`; SharedPreferences „guard“, „purge“ (Regeln, Dateien, Grössen) und „restore“ (fehlgeschlagene Wiederherstellungen) – nicht in der Sicherung.
  - **TESTING.md:** Unit-Tests `MirrorGuardTest`, `TrashIndexTest` (inkl. Uhrzeit-Namen), `ProfileWizardTest`, `TrashPurgeTest` (inkl. Schutz vorgemerkter Einträge), `FreeRulesTest`; manuelle Testabschnitte in `build.md` (Schutzbremse, Versionsschutz/Wiederherstellen, Papierkorb aufräumen, Profil-Assistent, Speicher freigeben für Ordner); Migrationstest vor 1.0.0 muss v3→v4→v5 abdecken.

- *Session 4 zusätzlich:*
  - **ARCHITECTURE.md:** Paket `l10n/` (`Texts`, `AppLanguage`), Initialisierung in `App.onCreate`/`onConfigurationChanged`, Sprachwahl je Android-Version, Aufbau der `strings_*.xml` je Bereich, Regel „keine Texte im Code“.
  - **SYNC-ENGINE.md:** Ordnerauflösung Schritt 2b (Standardname in anderer App-Sprache), Ordnername aus ISO-Kürzel, Papierkorb-Namen `TRASH_DIRS`, Konflikt-Zusatz `_conflict-phone`, Verlaufstexte in aktiver Sprache und `isIdle`-Merkmale beider Sprachen.
  - **DATA-MODEL.md:** `StatRow.countryIso`; Bedeutung von `media_info.countryName` (nur Kennzeichen); SharedPreferences „lang“ (nicht in der Sicherung).
  - **TESTING.md:** `StringsParityTest`, Reflection-Rückfall `TestTexts` (deutsche Texte in allen Tests), Ergänzung `FileFilterTest` (`.LeoSync-Bin`); manueller Sprachtest (Release-Checkliste 10a in `build.md`).

- *Session 5 zusätzlich:*
  - **ARCHITECTURE.md:** Aufteilung `HomeScreen`/`HomeParts` und `ProfileScreen`/`ProfileCards` (MutableState-Übergabe); `PermissionRows` + `PermNeeds`; `PendingProfileDelete` (prozessweit, Snackbar); `ScreenScaffold` mit `bottomBar`/`snackbarHost`; Bibliothek dcerpc (`listShares`) und ihre R8-Regeln; `RunStatus`-Konstanten.
  - **SYNC-ENGINE.md:** Prüfstufen (`PartialHash.levelRegions`: LOW = `regions`, MEDIUM = `thoroughRegions`, HIGH = 256 MB/128 Stichproben, FULL = ganze Datei), Pflicht bei Löschen, „Aus“ prüft nur die Grösse und gilt nicht als verifiziert; `friendlyError`/`userError` (Lauf-Meldung verständlich, Log technisch); vorgemerkte Profillöschung wird vom `SyncWorker` übersprungen.
  - **DATA-MODEL.md:** DB-Version 6 (`verifyLevel`, Mapping aus `verifyUpload`/`deleteMode`, `TRIM(name)`); `remoteSubfolder` wird beim Öffnen in `smbPath` übernommen und leer gespeichert; Sicherung mit `verifyLevel` (Rückfall `verifyUpload`); neue `GlobalSettings`-Schlüssel `pullTipDone` und Neuinstallations-Standard für `notifySummary`.
  - **TESTING.md:** `VerifyLevelTest`, `CredentialGroupsTest`, instrumentierter `MigrationTest` (`gradlew connectedDebugAndroidTest`, Schemas als androidTest-Assets); Emulator-Testumgebung (`LeoSync_API29`, `Medium_Phone_API_37.0`) mit Testfreigabe und Bedienung per `adb`/`uiautomator`; Testabschnitt „UX-Überarbeitung und Prüfstufen“ in `build.md`.

## D. Technische Risiken

1. **Ungebaute Stände:** 0.0.11 bis 0.0.13 wurden in der Session nie kompiliert. Ein kompatibles Zusammenspiel von KSP und Kotlin war nicht bestätigt.
2. **Hersteller-Stromsparfunktionen:** Das Wecken im Hintergrund ist auf dem Gerät des Inhabers unzuverlässig; die Lösung hängt am Vordergrunddienst (Zeitlimit ab Android 15).
3. **Keystore-Verlust:** Ohne `keystore/` und `keystore.properties` sind keine Updates mehr möglich.
4. **Teil-Prüfsumme:** Sie liest nur 192 KB; inhaltsgleiche Anfangs-, Mitten- und Endbereiche bei unterschiedlichem Inhalt werden nicht erkannt. Das Löschen nach dem Sync verlässt sich darauf.
5. **R8:** Fehlende Keep-Regeln (smbj, mbassador, BouncyCastle) zeigen sich erst zur Laufzeit.
6. **Interne Namen:** Eine Änderung von Keystore-Alias, DB-Name oder App-ID führt zu Datenverlust bzw. einer neuen App.
7. **SAF-Einschränkungen:** Das lokale Änderungsdatum ist nicht setzbar; eine verlorene dauerhafte Berechtigung lässt den Lauf scheitern; bei Anbietern ohne `moveDocument` wird kopiert und gelöscht.
8. **Unbegrenzter Papierkorb** beim Ordner-Sync.
9. **Ländererkennung:** An Grenzen und Küsten ungenau (1:50m); HEIC ohne GPS im PC-Import.
10. **Kotlin-Fallen:** `/**` in Blockkommentaren (Build-Fehler). In PowerShell liefert `$null -notmatch` `$null`, was im Import-Skript fast zu Kopien ohne Bestätigung führte.
11. **Doppelte Logik:** Teil-Prüfsumme, Ordnerregeln und Länderregeln existieren doppelt (Kotlin und C#) und müssen synchron bleiben.
12. **Play-Store-Konformität:** Hintergrund-Standort, Akku-Ausnahme, `REQUEST_INSTALL_PACKAGES` und der dataSync-Vordergrunddienst brauchen für eine Veröffentlichung Begründungen.

*Session 2 zusätzlich:*

13. **Play-Protect-Sperre:** Die App ist als „schädlich“ eingestuft (alle Versionen mit demselben Zertifikat). Neue Nutzer würden blockiert; jede neue APK kann neu bewertet werden.
14. **Hersteller-Killer:** Auf Oplus-Geräten beendet `o-kill` die App auch im Vordergrunddienst; Zuverlässigkeit hängt von Einstellungen des Nutzers ab (DEC-082).
15. **Sicherungsdatei:** Ohne Export-Passwort enthält sie keine Passwörter, aber Pfade, IP-Adressen und den Sync-Status; das Format muss rückwärtskompatibel bleiben (Formatversion 1).
16. **Download-Fortsetzung:** Hängt davon ab, dass der SAF-Anbieter „wa“ unterstützt; sonst wird nach der Grössenprüfung neu begonnen.
17. **Zustände in `GlobalSettings`:** `awaitingFirstStart`, `manualOnly`, `pausedUntil` und Benachrichtigungs-Schalter liegen ausserhalb der Datenbank und werden (bis auf einzelne Einstellungen) nicht exportiert; bei Neuinstallation gehen sie verloren.
18. **Ausgeschaltete Hinweise:** Mit `notifyHints = false` fallen offene Ordnerauswahlen, Konflikte und Fehler nur noch auf der Startseite auf.
19. **Google Fotos „Speicherplatz freigeben“:** Löscht Google Fotos Originale, bevor LeoSync sie gesichert hat, landen sie nie auf dem PC.
20. **Versionsfalle:** Werden Änderungen unter einer bereits gebauten Version eingetragen, zählt der Builder automatisch hoch (Fall 0.3.6/0.3.7); Patchnotes müssen dann korrigiert werden.
21. **PowerShell-Falle:** `[math]::Max/Min` mit Int-Literal und `long` wählt die Int32-Überladung und scheitert ab ca. 2 GB (Import-Skript, behoben in 0.3.3).
22. **AGP 9 / built-in Kotlin:** KGP-Version und Compose-Compiler-Plugin müssen identisch bleiben; KSP 2.3 kennt nur KSP2 (Room ≥ 2.7 nötig).
23. **Paketwechsel `ch.leo.sync`:** Für Android eine neue App; ohne Export und Import gehen Profile und Sync-Status verloren, das Selbst-Update kann nicht umsteigen.

*Session 3 zusätzlich:*

24. **SAF-Anbieter liefern je nach Abfrage andere Zeitstempel** (`stat()` gegenüber `children()`, z. B. „Downloads“). Jeder neue Vergleich mit dem Sync-Stand muss dieselbe Quelle wie das Einlesen verwenden (DEC-137).
25. **Zustände in SharedPreferences:** Schutzbremse, vorgemerktes PC-Leeren und fehlgeschlagene Wiederherstellungen liegen ausserhalb der Datenbank und der Sicherung; bei Neuinstallation gehen sie verloren.
26. **„Speicher freigeben“ bei Ordnern löscht endgültig:** Die Sicherheit hängt davon ab, dass der Sync bei Handy → PC ohne „Löschungen übernehmen“ fehlende Handy-Dateien nie auf den PC überträgt. Jede Änderung an `MirrorEngine.plan` für diesen Fall muss das berücksichtigen (DEC-147). Seit 0.11.8 werden danach auch leere Elternordner entfernt (ausser Profil-Wurzel, Papierkorb, `DCIM/Camera`), Teilentschärfung von D26.
27. **Wiederherstellen kann unvollständig zurückrollen:** Scheitert der Schritt nach einem erfolgreichen Upload bzw. Download, bleibt die aktuelle Version im Papierkorb (Hinweis im Verlauf). Ein Download, der wegen einer inzwischen entstandenen Handy-Datei übersprungen wird, gilt als erfolgreich wiederhergestellt.
28. **Namensschema im Papierkorb:** Eine echte Datei, die zufällig `name (HH-mm-ss).ext` heisst, wird als Version von `name.ext` angezeigt.
29. **`MirrorLock` und lange Läufe:** Eingriffe warten bis zum Ende eines grossen Syncs; die Sperre ist nur prozessweit und schützt nicht vor anderen Apps. Bleibt auch mit DEC-201 bestehen (die Sperre bleibt global).
30. **Agent-Fallen:** Vorhandene Dateien beim Anlegen überschrieben (`AppIcons.kt`, Build-Fehler 0.6.1); `/**` in KDoc erneut in 0.7.0 (`FreeRules.kt`).
31. **Hochformat-Sperre:** Ab targetSdk 36 auf Tablets und Faltgeräten wirkungslos (DEC-152).

*Session 4 zusätzlich:*

32. **Texte vor `App.onCreate`:** `str()` wirft, solange `Texts.init` nicht gelaufen ist (z. B. in ContentProvidern oder frühen Initialisierungen).
33. **Reflection-Rückfall in Tests:** `Texts` lädt `com.sync2pc.app.TestTexts` per Namen; wird die Klasse umbenannt oder verschoben, scheitern alle Tests mit Texten. Seit 0.12.0 muss der Test-`Resolver` in `TestTexts` zusätzlich Ressourcen-**Namen** auflösen können (`entryName`, `stringByName`, `pluralByName`; DEC-202).
34. **Formatfehler zur Laufzeit:** Ein `%` ohne `%%` in Texten mit Platzhaltern oder abweichende Platzhalter zwischen den Sprachen stürzen erst beim Anzeigen ab; nur `StringsParityTest` fängt das ab.
35. **Gemischtsprachiger Verlauf:** Gespeicherte Texte lassen sich nachträglich nicht übersetzen; `isIdle` hängt an Merkmal-Wörtern beider Sprachen. Ändert sich die Formulierung in `RunSummary`, müssen die Merkmale nachgezogen werden. **Behoben seit 0.12.0/0.12.1 (DEC-202)** für neue Einträge: `isIdle` prüft Schlüssel (`RunSummary.NOT_IDLE_KEYS`); alte Einträge ohne Code bleiben bis zum Ablauf der Aufbewahrung (Logs 30, Läufe 90 Tage) gemischtsprachig und nutzen weiter die Merkmal-Wörter. Die Diagnose bleibt beim gespeicherten Text (DEC-160); `ALREADY_COUNT` dort ist nur deutsch.
36. **Kommentar-Falle zum dritten Mal:** `values-de/*.xml` in einem KDoc (`TestTexts.kt`) brach den Test-Build (0.8.0).
37. **Erkennung „erzeugter“ Ordnernamen:** Mit mehr App-Sprachen gelten mehr Namen als Standardordner; ein selbst gewählter Name in einer App-Sprache löst dann keine Rückfrage mehr aus (DEC-161).

*Session 5 zusätzlich:*

38. **Bibliothek dcerpc** (DEC-192): bringt Guava und Apache Commons mit; smbj und BouncyCastle sind ausgeschlossen, d. h. dcerpc läuft gegen die eigene smbj-Version (0.13.0 statt der vorgesehenen 0.12.2). Ein smbj-Update oder fehlende R8-Regeln können das Auflisten der Freigaben brechen, ohne dass Unit-Tests es bemerken.
39. **Prüfstufe „Komplett“** liest jede Datei vollständig zurück: bei grossen Videos doppelte Dauer und mehr Akku; bei der Erstsicherung zusätzlich das 6-Stunden-Limit des dataSync-Dienstes (Android 15+).
40. **Fehlererkennung über Texte:** `friendlyError` erkennt SMB-Fehler an Statusnamen in Ausnahmetexten; andere smbj-Versionen oder Formulierungen fallen auf den technischen Text zurück.
41. **`RunStatus` und SQL:** Die Konstanten und die Textwerte in DAO-Abfragen (`'OK'`, `'PARTIAL'` …) müssen identisch bleiben.
42. **Neuinstallations-Erkennung** (DEC-191): beruht auf leeren Einstellungen; schreibt künftig Code vor `applyInstallDefaults()` in `global`, gelten bestehende Nutzer als neu (oder umgekehrt).
43. **Pfad-Übernahme beim Öffnen** (DEC-192): Ein Profil mit `remoteSubfolder` wird beim nächsten Speichern mit zusammengeführtem Pfad gespeichert; Code, der `remoteSubfolder` separat erwartet, muss beide Formen akzeptieren (`SmbSession.targetOf`, `ProfileOverlap.target` tun das).
44. **Vorgemerkte Profillöschung nur im Speicher** (DEC-196): Prozess-Ende während der Frist lässt das Profil bestehen (gewollt); ein zweites Löschen während der Frist löscht das erste sofort.
45. **Android 16+** (DEC-183): Hochformat-Sperre auf Tablets wirkungslos, Querformat ungetestet; strengere Kontingente für Hintergrundaufträge.
46. **Agent-Fallen beim Bauen/Testen:** PowerShell-`Set-Location` gilt nicht für `cmd /c`; Gradle-Ausgabe in eine Pipe lässt den Aufruf hängen; die Sandbox blockiert Befehle, deren Text wie ein geschützter Pfad aussieht; PowerShell-Hashtables sind bei Schlüsseln nicht gross-/kleinschreibungssensitiv; gestaffelte Textersetzungen können bereits ersetzte Stellen erneut treffen („Synchronisierunghronisierung“).

*Nachtrag 2026-11-08 (Architecture -014):*

47. **Aufräumen löscht mehr als angezeigt (Befund F3, bis zur Umsetzung von Development -012):** Die Papierkorb-Ansicht zeigt Anzahl und Grösse aus dem eingelesenen Stand (`TrashScreen`), führt aber die Regel aus. Bei „Alles“ und „Tage“ mit heutigem Tag werden Versionen mitgelöscht, die ein Sync inzwischen in den Papierkorb gelegt hat (nach dem Warten auf `MirrorLock` oder wenn zwischen Einlesen und Bestätigen ein Sync lief). „Älter als“ ist sicher. Behebung: DEC-201 (b), nur der bestätigte Stand wird gelöscht. Datenverlustrisiko, bis umgesetzt.
