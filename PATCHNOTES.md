# Patchnotes – LeoSync

Versionierung: Beta `0.1.x` (nur Fehlerbehebungen). Neue Funktionen eröffnen `0.2.x`, `0.3.x` usw. bis zur Freigabe von `1.0.0`. Bis 0.0.13: `0.0.x`. Neueste Version oben.

## 0.16.45 (2026-11-12)

### Geändert
- **Texte:** Die Beschreibungen der Optionen «Unverschlüsselt» (SMB-Verschlüsselung) und «Nur WLAN-Name» (PC-Erkennung) erklären nur noch, was die Option ist, ohne Bezug auf frühere Versionen.

## 0.16.44 (2026-11-12)

### Neu
- **Papierkorb:** Bei jeder Datei auf dem Handy gibt es «Öffnen». Die Datei wird nur zum Ansehen mit der Standard-App geöffnet, im Papierkorb ändert sich nichts.

## 0.16.43 (2026-11-12)

### Behoben
- **Papierkorb:** Bei leerem Handy-Papierkorb und nicht erreichbarem PC lautet der Leertext «Auf dem Handy ist der Papierkorb leer.», sonst unverändert «Der Papierkorb ist leer.» (der PC-Papierkorb kann Einträge haben).

## 0.16.42 (2026-11-12)

### Behoben
- **Papierkorb:** In der Ordnerzeile stehen Name und Button «Neueste Version je Datei wählen» je in eigener Zeile; «(Hauptordner)» bricht nicht mehr mitten im Wort um, Ordnernamen brechen nur an Wortgrenzen um.

## 0.16.41 (2026-11-12)

### Geändert
- **Profilkachel:** Oben rechts zeigt die Kachel Symbole für WLAN (im hinterlegten WLAN), PC (Kennung bekannt, grüner Monitor) und Verschlüsselt, nur die zutreffenden, ohne Lücke und gleich gross. Der Text «im WLAN ✓» entfällt; die Textzeilen unter dem Namen bleiben.

## 0.16.40 (2026-11-12)

### Behoben
- **Papierkorb in der Galerie (Android 10):** Nach dem Anlegen der `.nomedia` löst die App einen gezielten Galerie-Scan aus (nur interner Speicher), damit bereits indexierte Papierkorb-Bilder wirklich aus der Galerie verschwinden.
- **Papierkorb:** Nach «Neu einlesen» lautet der Hinweis «Auswahl aufgehoben (Einträge nicht mehr vorhanden oder nicht erreichbar).»; nach einem Filterwechsel bleibt «… (Filter geändert).».

## 0.16.39 (2026-11-12)

### Behoben
- **Papierkorb:** Die Ordnerzeile ordnet Name und Button «Neueste Version je Datei wählen» untereinander an; der Filter-Zusatz im Button («Nur PC» usw.) entfällt, auch bei «Alles vom … auswählen».
- **Papierkorb:** Der Hinweis «Auswahl aufgehoben» steht als Zeile in der Liste statt als Toast, bis wieder etwas gewählt wird. Die Auswahlleiste ist kompakter.

## 0.16.38 (2026-11-12)

### Neu
- **Papierkorb:** Seitenfilter «Anzeigen» (Handy und PC / Nur Handy / Nur PC) über dem Tag. Die Auswahl wird bei Wechsel von Seitenfilter oder Tag aufgehoben, wenn sie ausgeblendete Einträge enthält. Auswahlleiste und Löschdialog nennen die betroffenen Seiten. Der Wiederherstellen-Dialog nennt das Ziel (Handy und PC), ersetzte und neue Dateien (nach Synchronisierungsstand) und bei aktivem Filter, dass immer auf beide Seiten geschrieben wird. «Wo?» beim Aufräumen ist passend zum Filter vorbelegt.

### Behoben
- **Papierkorb:** Die Schaltflächen «Papierkorb aufräumen…» und «Neu einlesen» stehen je in einer Zeile (bei Schrift 1,5/2,0 entstanden grosse Leerflächen).

## 0.16.37 (2026-11-12)

### Behoben
- **Update-Version:** Die installierte Version im Update-Vergleich wird zur Laufzeit aus dem Paket gelesen (kein veralteter Wert mehr nach inkrementellem Build).
- **Startseite:** Bei Pause und offenem Konflikt steht das Pausenende weiterhin sichtbar.
- **Konflikte:** «Alle: neuere» steht in der Inhaltsfläche statt in der Kopfzeile, der Titel bleibt bei grosser Schrift sichtbar.
- **Verlauf:** Filterchips brechen bei grosser Schrift nicht mehr mitten im Wort; «Alle» und «Fehler» teilen die Breite, «Übertragungen» steht in eigener Zeile.
- **Profil:** Der Name eines zum Löschen vorgemerkten Profils (Rückgängig-Frist) ist sofort wieder frei. Beim Speichern ohne WLAN-Namen steht der Hinweis fest über «Speichern». Verbindungstest bei Verschlüsselung «Aus»: ohne SMB-Version («Nicht verschlüsselt, wie eingestellt.»). Der Dialog «Zugangsdaten übernehmen» scrollt, der Titel «Erste Synchronisierung» wird bei sehr grosser Schrift nicht mehr getrennt, der Import-Dialog zeigt einen Scrollbalken.

## 0.16.36 (2026-11-12)

### Behoben
- **Handy-Papierkorb in der Galerie:** Bilder im Papierkorb `@LeoSync-Bin` eines Ordnerprofils erscheinen nicht mehr in der Galerie: Die App legt im Papierkorb eine leere Datei `.nomedia` an (nicht bei Download-Ordnern mit Nummern-Kennung, nie auf dem PC; Fehlschläge sind kein Fehler des Laufs).
- **Fotoprofil und Speicher freigeben:** Dateien in einem Papierkorb-Ordner (`@LeoSync-Bin` und Altnamen, an beliebiger Stelle des Pfads) werden nicht mehr gesichert und nicht mehr zum Freigeben angeboten.
- **Papierkorb-Übernahme:** Eine `.nomedia` im alten Papierkorb-Ordner gilt nicht als Inhalt und wird entfernt statt verschoben.

## 0.16.35 (2026-11-11)

### Behoben
- **Papierkorb-Merker:** Der Merker «PC lehnt `@LeoSync-Bin` ab» wird nach einer fehlgeschlagenen Übernahme nur noch bei «Name ungültig» gesetzt, nicht bei Kollision oder NOT_SUPPORTED eines einzelnen Verschiebens. Bekannte Einschränkungen: siehe `docs/SYNC-ENGINE.md` (Papierkorb).

## 0.16.34 (2026-11-11)

### Behoben
- **Papierkorb auf dem PC:** Lehnt der PC den Namen `@LeoSync-Bin` dauerhaft ab, merkt sich die App das je Profil und PC-Ziel (7 Tage): gleich der Ausweichname, eine einzige Warnung statt zwei je Lauf.

## 0.16.33 (2026-11-11)

### Geändert
- **Papierkorb heisst überall `@LeoSync-Bin`** (Handy und PC/NAS, alle Ordnertypen). Auf QNAP/Samba war `.LeoSync-Bin` ein versteckter Ordner, jetzt ist er sichtbar. Die Altnamen `.LeoSync-Bin`, `.LeoSync-Papierkorb` und `.Sync2PC-Papierkorb` werden weiter gelesen, aber nie synchronisiert.
- **Automatische Migration** beim nächsten Lauf: Umbenennen bzw. Datei für Datei verschieben (nie kopieren oder überschreiben, bei gleichem Namen wird die Uhrzeit angehängt, der alte Ordner nur gelöscht, wenn leer); wartet bei offener Wiederherstellung oder Leeren-Vormerkung. Eine Verlaufszeile je Seite.
- **PC-Rückfall:** Lehnt der Server den Namen ab, wird `.LeoSync-Bin` benutzt (Verlaufswarnung).
- **Wiederherstellen** findet eine Vormerkung mit Altnamen auch unter dem neuen Namen.

## 0.16.32 (2026-11-11)

### Geändert
- Gehört laut Development mit 0.16.33 zusammen (Papierkorb `@LeoSync-Bin` überall); Einzelheiten siehe 0.16.33.

## 0.16.31 (2026-11-11)

### Behoben
- **Download-Abbruch:** Ohne Fortschritt im Aufruf gilt ein zu kurzes Dateiende als Einzelfehler (Temp-Datei verworfen, der nächste Lauf beginnt neu) statt als Unterbrechung, damit nichts dauerhaft blockiert.

## 0.16.30 (2026-11-11)

### Behoben
- **Download-Abbruch:** Endet ein Download vor der erwarteten Grösse (z. B. Verbindungsabbruch mit Wiederverbinden), gilt das als Unterbrechung («Verbindung unterbrochen … Rest folgt»; die Temp-Datei bleibt, das Fortsetzen folgt im nächsten Lauf) statt als Fehler «Grösse nach Download stimmt nicht». Ändert sich die PC-Datei in der Zwischenzeit, bleibt es beim Fehler. (Der Fix steht laut Development erst in 0.16.31 vollständig.)

## 0.16.29 (2026-11-11)

### Geändert
- **WLAN-Name ist Pflicht** bei der PC-Erkennung «Nur WLAN-Name» und «Beides» (roter Hinweis in der Karte «WLAN», «Weiter» und «Speichern» gesperrt). Bestehende Profile ohne Namen in diesen Modi brauchen beim nächsten Speichern einen Namen. Entscheidung des Benutzers.

## 0.16.28 (2026-11-11)

### Geändert
- **Profilnamen einmalig:** Gross-/Kleinschreibung und Leerzeichen am Rand zählen nicht; Prüfung bei «Weiter», beim Speichern und beim Bearbeiten. Bestehende Doppelnamen werden nur gemeldet. Der Import «Hinzufügen» prüft Namen bewusst nicht.
- **Verbindungstest:** Bei Verschlüsselung «Aus» nennt «Zugriff verweigert» die Verschlüsselung als mögliche Ursache.
- **Import-Dialog:** Die Warnung zu «Ersetzen» steht oben, fest, in Fehlerfarbe.
- **«Verlauf löschen»:** Der Text nennt «Rückgängig».

## 0.16.27 (2026-11-11)

### Geändert
- **Datum und Uhrzeit** folgen den Systemeinstellungen des Handys (Region, 12 h/24 h), nicht der App-Sprache. Feste Formate bleiben bei Dateinamen, Sicherung/Export, Diagnose, Logs und Papierkorb-Tagesordnern.
- **Startseite:** «Letzte Synchronisierung» zeigt den letzten fehlerfreien Lauf (nicht den letzten Versuch); Leerzustand «Noch keine erfolgreiche Synchronisierung.».
- **Papierkorb:** Die Zeile zeigt «geändert …» (Änderungszeit der Datei) statt einer scheinbaren Löschzeit.
- **Dunkelmodus:** Aktionskarten der Startseite abgedunkelt lila.
- **Meldung «PC nicht erreichbar»** beim Entfernen gekürzt: «PC nicht erreichbar – nichts gelöscht. Die Dateien bleiben vorgemerkt.».
- **Handy-Ordner** mit nicht auflösbarer Kennung (`msd:`/`msf:`) zeigt «Handy-Ordner» statt «Download/690».

### Intern
- Stand bis 0.16.35: 600 Unit-Tests grün (Angabe Development), nichts am Gerät geprüft; QA-Prüfung (-241) und UX-Review (-242) waren offen.

## 0.16.25 (2026-11-11)

### Behoben
- **Handy-Papierkorb bei Download-Ordnern:** Bei Handy-Ordnern des Download-Anbieters mit Nummern-Kennung (`msd:`) heisst der Papierkorb «@LeoSync-Bin» statt «.LeoSync-Bin». Android indexiert Ordner mit Punkt am Anfang nicht; sie fehlten in der Ordnerliste, der Papierkorb war nicht auflistbar und zersplitterte in «(1)», «(2)». Der PC-Papierkorb bleibt unverändert.

## 0.16.24 (2026-11-11)

### Geändert
- **Konfliktkarte:** Die Knöpfe stehen untereinander und sind gleich breit.

## 0.16.23 (2026-11-11)

### Geändert
- **Info-Symbol:** Die Überschrift «Noch nichts eingerichtet» zeigt ein eigenes Info-Icon (Kreis mit i, blaue Fläche) statt des Material-Symbols.

## 0.16.20 (2026-11-11)

### Geändert
- **Profilkachel, Verschlüsselung:** Oben rechts erscheint ein Schild-mit-Schloss-Symbol, wenn die letzte Anmeldung des Profils tatsächlich SMB-verschlüsselt war (nicht bei der blossen Einstellung «Wenn möglich»). Nach Änderung von Einstellung, Freigabe oder Unterordner gibt es bis zur nächsten Verbindung kein Symbol. Der Zustand liegt in den SharedPreferences «enc_status» (kein DB-Schema).

## 0.16.19 (2026-11-11)

### Geändert
- **Begriff:** «Synchronisierungsstand» heisst deutsch «Abgleichstand», englisch «sync state» neu «sync status» (Ordnerwechsel-Dialog, Schutzbremsen-Text).
- **Profilkarte:** Symbol und Pfadanfang bleiben zusammen; Zeilenumbruch nur nach «\».

## 0.16.22 (2026-11-11)

### Geändert
- **Warn-Icon:** Die Überschrift der Statuskachel (z. B. «Keine Verbindung»), die Schutzbremsen-Karte und die Karte «Berechtigungen unvollständig» zeigen das eigene Warn-Icon statt des Material-Warndreiecks. Emojis kommen in den Texten nicht mehr vor; als Text bleiben bewusst → und ⏭.

## 0.16.17 (2026-11-11)

### Geändert
- **Deutscher Begriff:** «Synchronisierungsstatus» heisst jetzt «Abgleichstand» (Profil-Wartung, Profil löschen, Sicherung/Import). Englisch unverändert («sync status»).
- **Konfigurationsänderung:** Die App bleibt bei Änderung von Schriftgrösse oder Hell/Dunkel auf dem aktuellen Bildschirm. Der Assistent «Neues Profil» behält seine Eingaben, ausser dem Passwort (wird nicht gespeichert, neu eingeben); bestehende Profile werden neu aus der Datenbank geladen.
- **Profilkarte:** Der Zähler «offen» wird beim Zurückkehren in die App neu berechnet.
- **Meldung mit «Rückgängig»:** Die Aktion steht unter dem Text.

### Behoben
- Der Assistent speichert nie mit fehlender Pflichtangabe eines früheren Schritts.
## 0.16.16 (2026-11-11)

### Geändert
- **Englische App-Texte:** Das Gerät heisst auf Englisch «Phone» statt «Mobile» (11 Sprachdateien; «mobile data» und «mobile plan» unverändert).

## 0.16.15 (2026-11-11)

### Behoben
- **Speicher freigeben (Fotos):** Unmittelbar vor Papierkorb oder Löschen werden Grösse und Änderungsdatum der Datei erneut geprüft. Eine inzwischen bearbeitete Datei bleibt erhalten («inzwischen geändert»). Grenze: Eine Bearbeitung mit unveränderter Grösse und gleichem Datum wird nicht erkannt.
- **Ordner-Sync, Duplikate:** Vergibt der Anbieter beim Ersetzen einer nicht indizierten Handy-Datei einen anderen Namen («name (1).ext»), wird die Kopie verworfen und ein Fehler gemeldet.
- **Upload:** Ist die Handy-Quelle nicht lesbar, bleibt keine 0-Byte-`.s2ptmp` auf dem PC.

### Intern
- Neuer Test `DataIntegrityTest.speicherFreigeben_neupruefungVorLoeschenErkenntBearbeitung`. Nicht von QA bestätigt.

## 0.16.14 (2026-11-11)

### Geändert
- **Folgelauf für neue Dateien:** Am Laufende prüft LeoSync die Ordner auf Änderungen seit dem Einlesen. Sind während des Laufs Dateien neu entstanden, folgt ein automatischer Folgelauf nach 5 Minuten (höchstens einer pro Stunde). Prüfung durch QA offen (SYNC-20261111-214).

## 0.16.13 (2026-11-11)

### Behoben
- **Handy-Datei ändert sich beim Upload:** Eine Datei, die sich vor oder während des Uploads ändert oder wächst, führt nicht mehr zu «Teilweise» oder einem Fehler, sondern wird zurückgestellt («· N folgen später», Folgelauf nach 5 Minuten). Auf dem PC bleibt nie eine halbe Datei.

## 0.16.12 (2026-11-11)

### Geändert
- **Icons statt Emojis:** Symbole und Emojis in der Oberfläche (Ordner, Kamera, Handy, PC, Pause, Haken, Fehler, Warnung …) sind durch eigene Icons ersetzt, in Hell und Dunkel. Benachrichtigungen und Diagnose-Export bleiben Text.

## 0.16.11 (2026-11-11)

### Intern
- Nur Testbarkeit, keine Änderung im sichtbaren Verhalten: `CredentialCrypto.alias` ist nur für Tests sichtbar; neuer Test `CredentialCryptoInstrumentedTest`.

## 0.16.10 (2026-11-11)

### Intern
- Nur Testbarkeit, keine Änderung im sichtbaren Verhalten: neue reine Entscheidungen in `SyncDecisions` und `OldFileRecovery`; `WorkerOutcomeDecisionsTest` (14 Tests), `ContentTriggerArmTest` (instrumentiert).

## 0.16.9 (2026-11-11)

### Behoben
- **Automatischer Lauf für neue Fotos:** Ein Lauf ohne neue Dateien verschiebt den automatischen Lauf für neue Fotos nicht mehr. Der Auslöser wird nach einem Lauf nur noch scharf gestellt, wenn keiner wartet; neue Medien verlängern die Frist weiterhin.

## 0.16.8 (2026-11-11)

### Geändert
- **Wartende Dateien:** Die Statuskarte nennt bei «Keine Verbindung» die Zahl wartender Dateien. Die Profilkarte eines Ordner-Profils zeigt zusätzlich «· N offen» (neue oder seit dem letzten Abgleich geänderte Dateien im Handy-Ordner, rein lokal gezählt; bei «PC → Handy» kein Zähler). Fotos-Profile unverändert.

## 0.16.7 (2026-11-11)

### Geändert
- **Statuskarte, WLAN-Zeile:** Ist der WLAN-Name nicht lesbar (kein Profil mit WLAN-Erkennung oder Standort-Berechtigung fehlt), bleibt die Zeile «WLAN» leer statt «WLAN: nicht verbunden / nicht lesbar»; die Karte behält ihre Höhe.
- **Statuskarte, Start nicht ausgeführt:** Wurde ein Start wegen PC/WLAN nicht ausgeführt und warten Dateien (ausstehende Dateien oder fälliger Sync-Auftrag), zeigt die Karte «Keine Verbindung» mit sitzendem Hund auf blauem Kreis statt des roten Fehler-Hundes. Ohne wartende Dateien bleibt der Fehler sichtbar.

### Intern
- `HomeStatusTest` +1 Test (gezielt grün, nicht von QA bestätigt). Der Text `home_wifi_none` ist unbenutzt und bleibt vorerst in den Sprachdateien. UX-Review offen (SYNC-20261111-194).

## 0.16.6 (2026-11-11)

### Geändert
- **Statuskarte:** Ein nicht ausgeführter Start zeigt «Keine Verbindung» statt «Bereit»; der manuelle Start heisst «Start nicht möglich» statt «Automatischer Start übersprungen». Ein nie gestartetes Profil verhindert «Alles gesichert». Die Zeile «WLAN» aktualisiert sich bei Netzwechsel.
- **Sicherung:** Export- und Importmeldungen zählen den Ordner-Sync-Stand mit (vorher nur Fotodateien). Der Ordner-Stand war schon vorher in der Sicherung enthalten und wurde wiederhergestellt; nur die angezeigte Zahl war zu klein.

### Intern
- `HomeStatusTest` +3 Tests. Stand 49 Klassen, 493 Tests, 0 Fehler (Angabe Development; nicht von QA bestätigt).

## 0.16.5 (2026-11-11)

### Intern
- Keine Verhaltensänderung, nur Testbarkeit: Das Wiederherstellen ist in `RestoreExecutor` ausgelagert, neue Schnittstellen `RemoteStore` und `LocalFiles`, Testabhängigkeit `work-testing` (nur androidTest), neue Tests (u. a. `TrashRemoteTest`, `RestoreExecutorTest`). Stand 49 Klassen, 490 Tests, 0 Fehler (Angabe Development; nicht von QA bestätigt). androidTest nur kompiliert (QA SYNC-20261111-189).

## 0.16.4 (2026-11-11)

### Behoben
- Kotlin-Warnung in `DeletionManager.kt` (überflüssiger Elvis-Operator) behoben, keine Verhaltensänderung.

## 0.16.3 (2026-11-11)

### Geändert
- **Dialoge mit drei oder mehr Knöpfen** sind gestapelt, volle Breite, einzeilig (bestätigende Aktion zuerst, Abbrechen zuletzt): «Änderungen speichern?», «Profil angelegt», Ordnerwechsel-Warnung, Import-Dialog (dieser behält seine Reihenfolge, DEC-246).
- **Scroll-Hinweis:** Überlaufender Dialogtext zeigt Scrollbalken und Verlauf am unteren Rand, auch in «Änderung bestätigen», «Alle: neuere» und «Alle übernehmen» (DEC-247).
- **Ordneranzeige:** Bei Download-Ordnern erscheint der vollständige Pfad ab dem Speicherstamm (ab Android 10; am Gerät belegbar).
- **Verschlüsselungskarte:** Bedienhilfe «Eingeklappt, Wert: …»; Verbindungstest zeigt «✓ Verschlüsselt (SMB 3.1.1)».
- **Aufräumen-Sheet:** Der Hinweis «Bitte Anzahl Tage eingeben» steht nur noch unter dem Feld.
- **Papierkorb-Dialog:** Punkt vor «Das lässt sich nicht rückgängig machen.».
- **Konfliktkarte:** «(neuer)» nur bei mindestens einer Sekunde Zeitunterschied, sonst «(gleich alt)» (DEC-248).
- **PC nicht erreichbar:** Beim Entfernen nach der Sicherung erscheint statt eines Toasts eine Snackbar (ca. 8 s) mit «Prüfe die Verbindung und versuche es erneut.».

### Intern
- Neue Bausteine `DialogButtonColumn`, `StackedButton`, `DialogScroll` in `ui/Common.kt`; `LocalTree.mediaStorePathLabel`. Stand 47 Klassen, 464 Tests, 0 Fehler (Angabe Development, JDK 17; nicht von QA oder Documentation ausgeführt).
- Offen: UX-Review am Gerät (SYNC-20261111-184), QA (SYNC-20261111-185); R1–R3 und R12 nur kompiliert geprüft.

## 0.16.2 (2026-11-11)

### Intern
- Testwerkzeug (QA-SMB-Testserver) an vier Abweichungen von impacket gegenüber einem echten Server angepasst. **Keine Änderung an der App.** Nur lokal gegen den impacket-Client geprüft; Emulatorlauf durch QA offen (SYNC-20261111-180).

## 0.16.1 (2026-11-11)

### Geändert
- **Startseite:** Statt «Alles gesichert» zeigt sie «↻ Noch nicht alles gesichert», solange Dateien auf den nächsten Sync warten (DEC-241).
- **Standort-Berechtigung:** Fehlt sie, erscheint ein Hinweis mit Knopf «Berechtigungen» (DEC-242).
- **Handy-Ordner:** Bei verlorenem Zugriff zeigt die Profilkarte «Zugriff verloren – tippen und neu wählen» (DEC-243).
- **Pausiert** ist eine eigene Überschrift mit Detailzeile (DEC-244).
- **PC nicht erreichbar** zeigt die Zeit der nächsten Prüfung nur noch einmal (DEC-245).

### Intern
- Neue Abfrage `pendingDeferredFlow`; `HomeStatusTest` erweitert. UX-Review offen (SYNC-20261111-181).

## 0.16.0 (2026-11-11)

### Behoben
- **«Beide behalten» überschreibt nie mehr** (SYNC-20261111-171, DEC-239): Die Konfliktkopie erhält einen freien Namen (`_conflict-phone`, `-2`, `-3` …), der vom System vergebene Name wird geprüft, der Upload überschreibt keine vorhandene PC-Datei, und die Wiederholung nach Verbindungsabbruch benennt nicht ein zweites Mal um. Vorher konnte eine vorhandene Konfliktkopie oder gleichnamige PC-Datei ohne Papierkorb überschrieben werden.
- **Konflikt-Rückgängig zuverlässig** (SYNC-20261111-172, DEC-232): Hat der Benutzer rückgängig gemacht, führt die Engine die Entscheidung auch bei laufendem Sync nicht mehr aus.
- **«Fortsetzen» und Wiederholung bei laufendem Sync:** Wird nachgeholt (`Result.retry()`, 30 s Backoff) statt in einen einfachen Nachlauf umgewandelt zu werden (DEC-233).
- **Unbekannter Verschlüsselungswert = «Erforderlich»** (fehlender = «Wenn möglich»; DEC-234).
- **Überschneidungswarnung:** Fotos ↔ Ordner wird datenträgerunabhängig verglichen (SD-Karte); ein Ordner auf der Speicherwurzel warnt (DEC-235).
- **Fehlermeldung «Verschlüsselung nicht verfügbar»** kommt bei automatischen Läufen nur einmal je Profil (DEC-237).

### Neu
- **Geänderte Dateien folgen per Folgelauf** (DEC-238): Dateien, die während des Laufs geändert wurden, holt LeoSync nach 5 Minuten nach (höchstens ein Folgelauf pro Stunde). Der Verlauf zeigt «· N folgen später»; der Lauf bleibt «OK». Datenbank Version 11 (`sync_runs.deferred`, Migration 10 → 11).

### Intern
- Neue Tests `ConflictNameTest`, `SkipFollowUpTest`; erweitert `SyncGateTest`, `ProfileOverlapTest`, `PcLoginTest`, `BackupCodecTest`. `MigrationTest` 10 → 11 nur als androidTest, nicht ausgeführt; keine Room-Tests der neuen DAO-Abfragen (Gerät nötig). Fehler beim Speichern des Nachlauf-Merkers nur im Diagnose-Log (DEC-236).
- Offen: QA-Prüfung am Gerät (SYNC-20261111-178).

## 0.15.5 (2026-11-11)

### Behoben
- **Ordner-Sync: Wiederholung nach Verbindungsabbruch** (SYNC-20261111-166): Bricht die Verbindung bei einer Übertragung mit Versionsschutz ab, nachdem der Lauf das Ziel (Handy- oder PC-Datei) schon in den Papierkorb verschoben hat, wird die Übertragung beim zweiten Versuch nicht mehr stillschweigend übersprungen. Vorher fehlte die Datei nach einem «erfolgreichen» Lauf, und der Status lautete «alles aktuell». Der Lauf merkt sich die selbst verschobenen Dateien, verschiebt sie nicht erneut und setzt an der angefangenen `.s2ptmp` fort. Ist am Ziel inzwischen wieder eine Datei entstanden, wird sie nie überschrieben (übersprungen, «folgt beim nächsten Sync»).

### Intern
- Neue Datei `mirror/TransferResume.kt`, Anpassung in `MirrorEngine.kt`. Neuer Test `TransferResumeTest` (8 Tests). Stand 45 Klassen, 442 Tests (Quelltext-Zählung; Angabe Development, nicht von Documentation oder QA ausgeführt).
- Offen: Statusanzeige bei übersprungenen Dateien (SYNC-20261111-167), Wiederholung bei «Beide behalten» (SYNC-20261111-168), QA-Prüfung (SYNC-20261111-169). UX-Prüfung nicht erfolgt.
## 0.15.1 (2026-11-11)

### Geändert
- **Verlauf:** Löschen mit Bestätigung und «Rückgängig» (die Einträge bleiben im Speicher, bis die Snackbar abläuft, DEC-226). Verlauf-Filter mit Haken und Rücksetzen beim Verlassen; Handlungsempfehlung bei Fehlereinträgen.
- **Papierkorb:** neue Beschriftungen («Papierkorb aufräumen…», «Neueste Version je Datei wählen», «Auswahl aufheben»), Datum mit Uhrzeit je Eintrag, Dateiname im Löschdialog bei einer Datei, Aufräumen-Sheet mit fester Fusszeile und Tage ≥ 1 (DEC-227).
- **Speicher freigeben:** Die Ergebnismeldung nennt gelöscht, übersprungen und freigegebenen Speicher; Hinweis ohne Papierkorb unter Android 10; Hilfetexte und Leerzustand.
- **Update-Seite:** Offline-Meldung mit «Erneut prüfen», «Jetzt prüfen», Changelog ohne Markdown mit Link-Knopf, Fortschritt in Prozent/MB mit «Abbrechen», Hinweis «Danach LeoSync wieder öffnen» statt automatischem Neustart (DEC-229).
- **Einstellungen:** Eingabeprüfung für «Tage ohne Sicherung» (1–365), Mindestalter (0–1440 Min.) und Heimatland (gültiger Code); Toast nach «Diagnosedaten kopieren»; ein Sprachwechsel bleibt auf der Seite.
- **Berechtigungen:** Dialog «Berechtigung gesperrt» mit App-Info-Knopf, wenn Android nicht mehr fragt.
- **Import-Dialog:** «Ersetzen» in Warnfarbe (DEC-228), Knöpfe brechen bei grosser Schrift um, Hinweis, dass die App-Sprache nicht zur Sicherung gehört (DEC-230). Die Rückfrage «richtiger Ordner» nennt Neuinstallation und Import.

### Intern
- Neue Datei `ReleaseNotes.kt`; 4 neue Abfragen in `SyncDao`, kein Schema. Neuer Test `ReleaseNotesTest`. Stand 40 Klassen, 377 Tests (Quelltext-Zählung); Gesamtlauf laut Development 0 Fehler (nicht von Documentation ausgeführt).
- QA- und UX-Prüfung offen (SYNC-20261111-160/-161); ein APK 0.15.1 lag beim Abfassen in `releases\` nicht vor (0.15.0 ist gebaut).
## 0.15.0 (2026-11-11, gebaut)

### Neu
- **SMB-Verschlüsselung pro Profil** (DEC-222): Stufen «Aus», «Wenn möglich» (Standard, auch für bestehende Profile) und «Erforderlich». Bei «Erforderlich» wird ohne Verschlüsselung keine Verbindung aufgebaut und kein Passwort gesendet. Eigene, standardmässig eingeklappte Karte «Verschlüsselung» im Profilformular (DEC-225); «Verbindung testen» zeigt SMB-Version und Verschlüsselung. Datenbank Version 10 (Spalte `smbEncryption`, Migration 9 → 10); Sicherung enthält das Feld optional.

### Geändert
- **Datenintegrität** (DEC-223): Der Ordner-Sync vergleicht Dateien über 32 MB vollständig statt per Stichprobe. Vor dem Löschen nach der Sicherung wird die PC-Kopie erneut geprüft; ist der PC nicht erreichbar, wird nichts gelöscht, und fehlt die Kopie, bleibt das Original und wird neu gesichert. «Speicher freigeben» (Fotos) prüft zusätzlich Teil-Prüfsumme und Änderungsdatum. «Bereits vorhanden» erteilt bei Löschmodus ≠ «Behalten» eine Löschfreigabe nur nach Prüfung mindestens auf Stufe «Mittel».
- **Start während eines laufenden Syncs** wird nicht mehr verworfen, sondern genau einmal nachgeholt; die Sperre wird in jedem Fall freigegeben (DEC-221). Hinweis per Meldung und Zeile «Danach: …» in der Statuskarte (DEC-224); «Pausieren» und «Abbrechen» verwerfen den Nachlauf.
- **Überschneidungswarnung** gilt typübergreifend: Fotoprofil und Ordner-Profil warnen bei gleichem oder verschachteltem PC-Ziel, und bei ineinanderliegenden Handy-Ordnern, wenn das Ordner-Profil aufs Handy schreibt (DEC-092/DEC-134 aktualisiert).
- **Oberfläche:** Home-Status, Konflikt-Knöpfe (Rückgängig), Ordnerauswahl und Vorschau überarbeitet (laut Development SYNC-20261111-155; Einzelheiten siehe Hilfe).

### Intern
- Neue Klassen `SyncGate`, `ContentCompare`, `PresenceCheck`; gemeinsame Verbindungskonfiguration `smbConfigFor`; `SyncDao.deleteSynced`; `partialHash` wird immer als LOW-Teil-Prüfsumme abgelegt.
- Neue Tests `DataIntegrityTest`, `SyncGateTest`, `SmbConfigTest`, `HomeStatusTest`, `ScreenLogicTest`; erweitert `PcLoginTest`, `ProfileOverlapTest`, `BackupCodecTest`, `MigrationTest` (9 → 10, nicht ausgeführt). Gesamtlauf `testDebugUnitTest`: 373 Tests, 0 Fehler (Angabe Development).
- Nicht geprüft: Gerätetests gegen echte Windows-/NAS-Server, Durchsatz mit Verschlüsselung, UX-Review.

## 0.14.8 (2026-11-11)

### Geändert
- **Profil-Assistent:** «Weiter» lässt einen Schritt mit leerem oder ungültigem Zahlenfeld (Papierkorb-Tage 0–3650, Prüfintervall 15–1440 Minuten, Schutzbremse 1–100 %) nicht verlassen; «Speichern» im Assistenten springt zum ersten fehlerhaften Schritt (DEC-217).
- **Dialoge im Profilformular** scrollen bei grosser Systemschrift.
- **Ordneranzeige** zeigt für Ordner des Download-Anbieters den ganzen Pfad, sofern der Anbieter ihn liefert.
- **Filter:** Fehlertext vereinfacht.
## 0.14.7 (2026-11-10)

### Geändert
- **Zahlenfelder im Profilformular** (Papierkorb-Tage 0–3650, Prüfintervall 15–1440 Minuten, Schutzbremse 1–100 %): Zwischenwerte werden nicht gespeichert und nichts wird mehr still korrigiert. Ein ungültiger Wert zeigt einen Fehlertext unter dem Feld und blockiert das Speichern (DEC-217).
- **Bestätigungsdialog «Änderung bestätigen»** beim Ändern der Richtung oder beim Einschalten von «Löschungen übernehmen» an bestehenden Ordner-Profilen (DEC-216).
- **Filter:** Filtermuster mit nicht geschlossener Klammer werden abgelehnt. Die Filterkarte zeigt eine Kurzfassung, die Profilkarte «Filter aktiv».
- **Verbindung testen** ohne Pfad zeigt eine verständliche Meldung.
- **Ordneranzeige** über «Downloads» gewählter Ordner zeigt den Ordner statt des Anzeigenamens (z. B. «Download/12»). Der Ablehnungsgrund bei Fotos-Ordnern erscheint als Text; angenommen werden nur Ordner im internen Speicher (DEC-218).
- **Ordnerwechsel-Dialog:** Erläuterung der Optionen ergänzt.
- **Überschneidungsmeldung** nennt, ob der PC- oder der Handy-Ordner betroffen ist.
- **Profil löschen:** einheitlicher Text («Handy und PC»).

### Intern
- Neue Dateien `data/NumberInput.kt` und `data/ChangeConfirm.kt`; keine Änderung an Schema, Dateiformaten oder Sync-Logik.
- Neue Tests `NumberInputTest` (6), `ChangeConfirmTest` (5), `FileFilterTest.unbalancedBracketsAreReported`. Gesamtlauf `testDebugUnitTest` am 2026-11-10: 34 Klassen, 287 Tests, 0 Fehler (Angabe Development). Die UI-Verdrahtung ist nicht abgedeckt.

## 0.14.6 (2026-11-10)

### Geändert
- **Diagnose** enthält zusätzlich: Datenbankversion, je Profil den «PC-Status» (erkannt / noch nicht erkannt / nicht relevant bei nur WLAN; gleiche Logik wie die Startseite), bei Fotoprofilen den Zusatz «(gesperrt)» bei der Ordnerstruktur, sobald sie nach der ersten Synchronisierung gesperrt ist, sowie die Einstellungen `notifySummary`, `notifyUpdate`, `notifyHints` und `previewLimit`. «Kennung weicht ab» / «anderer PC» entsteht erst beim Anmelden und steht weiterhin im Lauftext und Log.

### Intern
- Keine Änderung an Schema, Dateiformaten oder Sync-Verhalten. `DiagnosticsTest` um `pcStatusShowsOnlyStateWord` erweitert.

## 0.14.5 (2026-11-09, gebaut)

### Behoben
- **Rückfrage-Dialog (Ordnerauswahl):** Bei «Eigenen Name…» wurde die vorbelegte Vorgabe (z. B. `Frankreich\08 - `) nicht übernommen, «Übernehmen» blieb gesperrt. Anzeige, Fehlertext und Übernahme nutzen jetzt denselben Wert; die Vorgabe gilt als gültiger Wert (QA -092: bestanden).

### Geändert
- **Typ-Text im Profil** (Bearbeiten und Assistent): neutral «sortiert nach Jahr und Monat» (EN «sorted by year and month»), da er sonst der gewählten Ordnerstruktur (V2/V3) widersprach.

## 0.14.4 (2026-11-09)

### Behoben
- **Ordnerstruktur im Assistenten (DEC-209):** Beim Tippen des Zielpfads ging die gewählte Vorlage verloren, wenn ein anderes Fotoprofil dasselbe Ziel hat. Jetzt gilt die Vorlage des bestehenden Profils nur als Anzeige (Auswahl gesperrt); die eigene Wahl wird getrennt gemerkt und kehrt zurück, wenn das Ziel wechselt (ohne eigene Wahl V1).
- **Rückfrage zu umbenannten Ordnern:** Der Dialog zeigt den Fehlertext passend zur Vorlage; Bestätigen ist erst möglich, wenn die Vorlage geladen ist.

## 0.14.3 (2026-11-09)

### Neu
- **Ordnerstruktur pro Fotoprofil wählbar (DEC-209):** V1 `Jahr\MM[ - Land]` (Standard, bisheriges Verhalten), V2 `Jahr\[Land\]MM`, V3 `Jahr\MM\[Land]`. Änderbar bis zur ersten Synchronisierung des Profils, danach gesperrt. Ein zweites Fotoprofil mit gleichem Ziel und anderer Struktur wird blockiert, bei verschachteltem Ziel gibt es eine Warnung.
- **Duplikatsuche** für alle Strukturen auch in Länder- und Unterordnern desselben Monats; «Speicher freigeben» sucht entsprechend.
- **Sicherung:** enthält die Ordnerstruktur; eine Sicherung mit unbekannter Struktur importiert das Profil nicht, «Ersetzen» bricht dann ab, bevor etwas gelöscht wird.

### Intern
- Datenbank Version 9 (`profiles.folderLayout`, Migration 8→9; bestehende Profile = V1). Neue Tests: `TargetResolverTest`, `RemoteFinderTest`, `FolderLayoutGuardTest` sowie Erweiterungen von `ProfileOverlapTest`, `BackupCodecTest`, `ProfileWizardTest`, `MigrationTest`; 272 Unit-Tests grün (QA -088), `MigrationTest` 8→9 nicht ausgeführt, Migration am Emulator mit echten Daten erfolgreich.
- Das PC-Import-Skript (`tools/LeoSync-Import.*`) wird nicht mehr gepflegt und kennt nur V1.

## 0.13.14 (2026-11-09, gebaut, Sicherheits-QA bestanden, Restrisiken siehe -081)

### Geändert
- **Rückfrage beim Hostwechsel (DEC-208):** Ändert man im Editor eines bestehenden Profils (Modus `GUID`/`BOTH`) den Host auf einen noch nicht bestätigten PC, fragt die App vor dem Senden der Zugangsdaten «Anderen PC verwenden?» (vor Speichern, «Verbindung testen» und «Freigaben/Ordner wählen»). Ersteinrichtung und `WIFI` fragen nicht. Ohne Bestätigung keine Anmeldung und keine Speicherung.
- **Meldung «anderer PC»:** Papierkorb und «Speicher freigeben» zeigen bei fremdem oder unbekanntem PC eine rote Meldung; Datenverhalten unverändert.
- **Fehlermeldungen in Rot (DEC-207, Nachtrag):** nicht erreichbarer PC wird rot dargestellt; die Offline-Vorschau zeigt eine übersetzte Meldung statt des englischen Ausnahmetexts.

### Intern
- Neuer Test in `PcIdentityTest` (`PcEditorRules.needsHostConfirm`). UI-Verdrahtung nicht getestet.

## 0.13.13 (2026-11-09, ungebaut)

### Behoben
- **Kommentare in `PcLogin.kt`:** zehn Kommentarzeilen mit doppelt kodierten Umlauten korrigiert, UTF-8-BOM entfernt (auch bei `tools/refine-anim-outline.py` und `version.properties`). Code und Logik unverändert.

## 0.13.12 (2026-11-09)

### Geändert
- **Fehlermeldungen im UI in Rotton:** zentraler `ErrorText` (`ui/Common.kt`, Theme-Farbe `colorScheme.error`) für «Verbindung testen», «Speicher freigeben» und die Vorschau (Simulation); Verlauf und Statuskachel bleiben unverändert. Umsetzung teilweise geprüft (Sichtprüfung am Emulator: «Verbindung testen» rot/normal; weitere Punkte offen, Auftrag -071).

## 0.13.11 (2026-11-09, gebaut)

### Geändert
- **Laufanimation von Leo:** Tempo wieder 100 % (42 ms je Bild, feste Konstante `FRAME_MS` in `ui/DogStatus.kt`; `LOOP_MS` steuert nur noch den Statusbogen), Benachrichtigung `notif_dog_flipper.xml` mit 42 ms.

## 0.13.10 (2026-11-09, ungebaut)

### Geändert
- **Laufanimation von Leo:** nur noch die Bilder 1–16 (`anim_dog_01…16`, vorher 37), nachbearbeitet mit `tools/refine-anim-outline.py` (schwarze Reste transparent, Aussenlinie konstant 2.6 px, Einrisse geschlossen). Entscheid des Inhabers.

## 0.13.9 (2026-11-09, ungebaut)

### Geändert
- **Laufanimation von Leo:** Änderungen aus 0.13.7 und 0.13.8 verworfen, Stand von 0.13.6 (37 Bilder, 42 ms) wiederhergestellt. `tools/refine-anim-outline.py` bleibt als ungenutztes Archivwerkzeug.

## 0.13.8 (2026-11-09, ungebaut, verworfen mit 0.13.9)

### Geändert
- **Laufanimation von Leo:** Versuch mit dem sauberen Schrittzyklus (Bilder 17–24, 4× wiederholt = 32 Bilder, 48 ms); durch 0.13.9 verworfen.

## 0.13.7 (2026-11-09, ungebaut)

### Geändert
- **Laufanimation von Leo:** geschlossene Aussenlinie (konstant 2.6 px) und entfernte graue Hintergrundreste, neues Werkzeug `tools/refine-anim-outline.py`.

## 0.13.6 (2026-11-09, ungebaut)

### Behoben
- **Restzeit-Anzeige:** Hochrechnung nur noch bei mindestens 60 s Messdauer, 2 MiB übertragen und höchstens 72 h; sonst «Restzeit wird berechnet …» (`home_eta_calculating`, beide Sprachen). Vorher absurde Werte bei winzigen Mengen (`etaSeconds()` in `HomeParts.kt`).
- **Lintfehler:** `NewApi` in `SyncWorker.kt` (`stopReasonCompat()`), `profile_guard_percent` mit `formatted="false"`, `app_name` mit `translatable="false"`, Einrückung in `ProfileCards.kt`. `lintDebug`: 0 Fehler. Verhalten bei Abbruch unter API 29 nicht am Gerät geprüft.

### Intern
- Neuer Test `EtaTest` (6 Tests).

## 0.13.5 (2026-11-09, ungebaut)

### Geändert
- **Laufanimation von Leo:** neu freigestellt (Fell über Farbsättigung statt Helligkeit erkannt, weicher Alpha-Rand), 37 Bilder (`anim_dog_01…37`, 42 ms), Bilder fest eingefroren: der Bau erzeugt sie nicht mehr (DEC-206), ffmpeg ist keine Bau-Voraussetzung mehr.

### Intern
- Neue Werkzeuge `tools/export-anim-frames.py` (Einmal-Export) und `tools/check-anim-alpha.py` (Alpha-Messung); `make-anim-video.ps1` und `make-anim.ps1` entfernt.

## 0.13.4 (2026-11-09, ungebaut)

### Behoben
- **Laufanimation von Leo:** ohne graue Flächen und «Geisterbeine» zwischen den Beinen und ohne Löcher im Fell (36 Bilder neu erzeugt).

### Intern
- Animation wird aus dem Video `.claude\images\new_animation.mp4` mit `tools\make-anim-video.ps1` erzeugt (eingeschlossene dunkle Lücken `-GapErode 12` entfernt, kleine umschlossene Löcher `-HoleMax 3000` wieder deckend); Quellbilder liegen unter `.claude\images\`. Neuerzeugung beim nächsten Bau (Hash-Kennung `v3-320px-alpha`). `build.md` angepasst.

## 0.13.2 (2026-11-09, ungebaut, Sichtprüfung offen)

### Geändert
- **Statuskachel:** hinter Leo ein farbiger Kreis zum Status (grün schlafend/laufend, hellblau sitzend, grau pausiert, rot Aufmerksamkeit); bei «laufend» mit drehendem Bogen; im Dunkelmodus helle Kontur beim pausierten Zustand. Rein visuell, keine Änderung an Synchronisierung, Datenbank oder Dateiformaten (`ui/DogStatus.kt`).

## 0.13.1 (2026-11-09, ungebaut und ungetestet)

### Geändert
- **PC-Erkennung, Nachbesserungen (DEC-205):** Die PC-Kennung wird bei `GUID`/`BOTH` nur nach dem Speichern des Profils und in «Verbindung testen» gelernt, nach erfolgreicher Anmeldung (neue Zugriffsart `SETUP`, Netzbedingung `learnNetOk`, Entscheidung `WRONG_NET`). Manueller Sync und automatische Läufe lernen nie («PC unbekannt», Hinweis «Profil öffnen → Verbindung testen»). Eine bei `WIFI` still gelernte Kennung erzeugt nie Vertrauen. Kennungswechsel: aushandeln, Verbindung schliessen, fragen, neu verbinden, bestätigte Kennung prüfen, dann anmelden. Kein automatischer Test mehr beim Moduswechsel.
- **Wartezeit:** abgewiesene Profile und Fehler der Vorab-Prüfung setzen keinen Timer (eine Meldung je Profil); Timer für nicht erreichbare PCs nur, wenn kein Profil lief oder die Verbindung mitten im Lauf abbrach. Vorab-Prüfung je Host und Lauf einmal (nur Speicher, keine Schemaänderung).

### Intern
- Neu: `smb/PcLogin.kt`, `sync/PcLearner.kt`, `PcEditorRules`. Neue/erweiterte Tests: `PcLoginTest`, `PcIdentityTest`, `NetworkRulesTest`, `DiagnosticsTest`, androidTest `PcGuidDaoTest`. Nicht ausgeführt, nichts im Gerät geprüft.

## 0.13.0 (2026-11-09, ungebaut und ungetestet)

### Neu
- **PC-Erkennung per PC-Kennung (SMB-Server-GUID, DEC-204):** Pro Profil wählbar: nur WLAN-Name (Standard für bestehende Profile, bisheriges Verhalten), nur PC-Kennung oder beides. Die Kennung wird vor dem Senden der Zugangsdaten geprüft; bei unbekanntem oder anderem PC meldet sich die App nicht an, überspringt das Profil (`skip_pc_not_recognised`), benachrichtigt einmal je Profil und wartet wie bei «nicht erreichbar». Bei Kennungswechsel (z. B. Neuinstallation von Windows) nur über «Verbindung testen» mit Rückfrage neu vertrauen. Oberfläche: Karte «PC-Kennung» im Profil, Status «PC erkannt / noch nicht erkannt» auf der Startseite. Funktioniert ohne Play Store und ohne WLAN-Namen (Standortfreigabe nur bei WLAN-Modus).
- **Datenbank Version 8:** neue Profilfelder `pcCheck`, `pcGuid`, `pcGuidHost` (`MIGRATION_7_8`, nur `ADD COLUMN`). **Sicherung:** die drei Felder im Profil-JSON, Format-Version 1 bleibt; ältere Sicherungen ergeben «nur WLAN-Name».

### Intern
- Neue Klasse `sync/PcIdentity.kt`, `smb/PcStore.kt`; einziger Anmeldeweg `SmbSession.connectChecked`. Neue/erweiterte Tests: `PcIdentityTest`, `NetworkRulesTest`, `BackupCodecTest`, `MigrationTest` (7→8). Nicht ausgeführt.
- Beschlossen, noch nicht umgesetzt (DEC-205): Die Kennung wird nur beim Speichern des Profils und in «Verbindung testen» gelernt, nicht mehr beim manuellen Sync.

## 0.12.5 (2026-11-09)

### Behoben
- **Foto-Scan: Einträge im Papierkorb oder noch in Bearbeitung:** Der Scan ignoriert zusätzlich Einträge im Papierkorb (`IS_TRASHED = 0`, ab Android 11). Wechselt ein Eintrag zwischen Scan und Lesen auf pending/trashed, wird die Datei still übersprungen (kein Fehler, kein Ausschluss) und im nächsten Lauf erneut versucht.

### Intern
- Neue Testklasse `PendingTrashedErrorTest` (4 Tests). Nicht auf dem Gerät getestet.

## 0.12.4 (2026-11-09)

### Geändert
- **Diagnose pseudonymisiert und maskiert (DEC-203):** „Problem melden“ und „Diagnosedaten kopieren“ enthalten keine Benutzernamen, IP-Adressen, Rechner-, Freigabe-, WLAN-, Profil- und Ordnernamen, Pfade und genauen Dateinamen mehr; stattdessen stabile Platzhalter (Profil-1, Host-1 …) bzw. Maskierung im Freitext. Der Verlauf-Export („Verlauf teilen“) bleibt unverändert. Text `settings_diag_hint` DE/EN angepasst.

### Intern
- Neue Klasse `diag/Redactor.kt`; `DiagnosticsTest` erweitert (6 auf 15 Tests laut Development).

## 0.12.3 (2026-11-09)

### Behoben
- **Eingriff beim laufenden Sync (QA -039):** Sind die Hinweise für LeoSync im System oder der Kanal „Hinweise“ ausgeschaltet, gilt das Ergebnis nicht mehr als zugestellt. Es bleibt dann in der App stehen, bis die Ansicht geöffnet wird (`Notifications.interventionDone`, Prüfung `areNotificationsEnabled` und Kanal-Wichtigkeit).

## 0.12.2 (2026-11-09)

### Intern
- `Msg.toCode()` fängt Ausnahmen ab (ein ungültiges Zahlenargument bricht keinen Lauf mehr ab, es gilt der Rückfalltext). Neuer instrumentierter Test `MsgInstrumentedTest` (Kennungen mit echten Android-Ressourcen, DE/EN, `triggerData/triggerOf`). Keine Funktionsänderung.

## 0.12.1 (2026-11-09)

### Neu
- **Verlauf mit Kennungen, Stufe 2 (DEC-159):** Die Zeilen des Detail-Logs (Start, Zähler, Papierkorb, Konflikte, Wiederherstellen, Fehler, Ende …) werden ebenfalls als Kennung gespeichert (`log_entries.code`) und im aufgeklappten Lauf sowie im Verlauf-Export in der aktiven Sprache angezeigt. Auslöser und Richtung in der Startzeile, Gründe einer Unterbrechung und die Schutzbremse-Überschrift stecken als verschachtelte Kennung darin. Dateinamen, Pfade, technische Fehlertexte und Symbolzeilen (↑ ↓ ✗) bleiben wörtlich. Die Diagnose bleibt beim gespeicherten Text (DEC-160). Keine weitere Datenbankänderung.
## 0.12.0 (2026-11-08)

### Neu
- **Verlauf mit Kennungen, Stufe 1 (DEC-159):** Kurztext eines Laufs und Auslöser werden zusätzlich als Kennung gespeichert (Ressourcen-Name, Zahlen, Verkettung; `Msg`) und im Verlauf, auf der Startseite, in der Simulation und im Verlauf-Export in der aktiven Sprache angezeigt. Der Verlauf ist damit nach einem Sprachwechsel nicht mehr gemischtsprachig. Alte Einträge zeigen weiter ihren gespeicherten Text. „Ruhige“ Läufe (`isIdle`) erkennt die App über Schlüssel statt über Merkmal-Wörter (Risiko D35); die Wörter gelten nur noch für Einträge ohne Kennung.
- Datenbank Version 7 (`MIGRATION_6_7`, nur drei neue, nullable Spalten: `sync_runs.messageCode`, `sync_runs.triggerCode`, `log_entries.code`). Sicherung (`BackupCodec`) unverändert, Formatversion 1. Die Log-Zeilen folgen in Stufe 2 (0.12.1).

### Intern
- Neu `l10n/Msg.kt`, `data/RunTexts.kt`; `Texts.Resolver` löst Ressourcen per Name auf. `RunSummary` liefert eine Kennung statt Text. Neue Tests `MsgTest`, `MigrationTest` 6→7.
## 0.11.10 (2026-11-08)

### Behoben
- **Papierkorb (ersetzte Versionen):** Steht nach einem Ordnerwechsel die Stichprobe aus, wurde das vorgemerkte PC-Leeren übersprungen, der Lauf legte aber ersetzte Versionen in den PC-Papierkorb, und der nächste Lauf löschte sie bei „Alles“/„Tage“ ungesehen mit. Jetzt läuft das vorgemerkte Leeren unmittelbar nach bestandener Stichprobe (`MirrorEngine.runInner`, nur Leeren, keine Wiederherstellungen; nicht im Trockenlauf).
- **Aufräumen:** führt nur noch den bestätigten Stand aus. Unter der Sperre wird neu eingelesen; gelöscht wird nur, was noch mit gleicher Grösse da und nicht zum Wiederherstellen vorgemerkt ist. Ein Datumsordner wird nur ganz gelöscht, wenn sein aktueller Inhalt vollständig bestätigt war (`TrashPurge.planConfirmed`).

### Neu
- **Eingriff beim laufenden Sync (DEC-144):** Aufräumen, Endgültig löschen und Speicher freigeben (Ordner) warten auf den laufenden Sync und laufen danach auch dann, wenn die Ansicht verlassen wird (prozessweit, `PendingIntervention`, nicht gespeichert: Prozessende = nichts gelöscht). Anzeige „Wird nach der laufenden Synchronisierung ausgeführt“ mit „Abbrechen“ (nur wartend); pro Profil nur ein Eingriff gleichzeitig; Ergebnis in der Ansicht oder als Hinweis. Keine Datenbankänderung.
- **Eingriff beim laufenden Sync (QA -035):** Das Ergebnis geht nicht mehr still verloren: Die Benachrichtigung hängt nicht mehr vom Schalter „Hinweise“ ab, hat je Profil eine eigene Kennung, und ist sie nicht zustellbar (keine Berechtigung), bleibt das Ergebnis in der App stehen, bis die Ansicht geöffnet wird. Fehlt die Handy-Berechtigung, meldet Aufräumen/Endgültig löschen einen Fehler, bevor etwas gelöscht wird; bei 0 Treffern erscheint „Nichts gelöscht …“ statt „gelöscht“. „Abbrechen“ und Start schliessen sich gegenseitig aus (gemeinsame Sperre, Kennung je Start), ein abgebrochener Eingriff kann einen neuen nicht mehr aus der Anzeige entfernen. Die Zustandslogik (`InterventionRegistry`) ist ohne Android testbar.

## 0.11.9 (2026-11-08)

### Behoben
- **Speicher freigeben (leere Ordner):** Vor dem Löschen eines Ordners werden alle Einträge gezählt, auch solche ohne Anzeigenamen; gelöscht wird nur bei 0 (`LocalTree.deleteDirIfEmpty`, `rawChildCount`). Vorher hätte ein Ordner nur mit solchen Einträgen als leer gegolten.
- **Wiederherstellen:** Der Versuchszähler (`RestoreFailures`) wird unter einer Sperre geschrieben.

## 0.11.8 (2026-11-08)

### Neu
- **Speicher freigeben (Ordner-Profile):** Leere Ordner werden nach dem Löschen entfernt (nicht die Profil-Wurzel, den Papierkorb und `DCIM/Camera`); das Ergebnis zeigt „X leere Ordner entfernt“.
- **Wiederherstellen:** Nach 5 Fehlversuchen wird nicht mehr automatisch wiederholt. Vormerkung und Papierkorb-Version bleiben erhalten; auf der Startseite „Erneut versuchen“ oder „Verwerfen“.

### Intern
- Sechs unbenutzte Texte entfernt (`profile_subfolder`, `profile_subfolder_example_mirror`, `profile_subfolder_example_photos`, `profile_browse`, `settings_battery_off`, `profile_delete_cd`). Neue Tests `RestoreRetryPolicyTest`, `FolderFreerTest`.

## 0.11.7 (2026-11-08)

### Geändert
- **Mobile Daten/VPN:** Ausserhalb des WLAN wartet die App nach „PC nicht erreichbar“ und nach Verbindungsabbruch mitten im Lauf 2 Stunden (im WLAN weiter 30 bzw. 2 Minuten). Der Erreichbarkeitstest macht dort nur 2 Versuche. Die Wartezeit endet durch neue WLAN-Verbindung, manuellen Start oder einen erfolgreichen Lauf.
- **Sicherung einspielen:** Ändert sich dabei der Schalter für mobile Daten/VPN, wird neu geplant und die Wartezeit verworfen (wie beim Umschalten).
- **Problem melden:** Prüft vorab, ob eine Mail-App vorhanden ist.

### Behoben
- **Verlauf teilen:** Der Titel des Auswahldialogs folgt jetzt der App-Sprache (war fest deutsch; `Diagnostics.kt`).

### Intern
- Neue Datei `sync/NetworkRules.kt` (reine Netzentscheidungen), `SyncScheduler.onOffWifiChanged`, `<queries>` im Manifest; neue `NetworkRulesTest`, `BackupCodecTest` +2 Tests.

## 0.11.6 (2026-11-08)

### Geändert
- **Mobile Daten/VPN:** Beim Umschalten des Schalters wird die Wartezeit „PC nicht erreichbar“ verworfen (`cancelShareRetry`). Der Hinweistext nennt jetzt mögliche Kosten beim Mobilfunkabo (z. B. im Ausland).

## 0.11.5 (2026-11-08)

### Neu
- **Mobile Daten/VPN:** Schalter in Einstellungen › Übertragung, mit dem die automatische Synchronisierung auch ausserhalb des WLAN läuft (Standard aus, mit Hinweis auf den Datenverbrauch).
- **Problem melden:** Knopf in Einstellungen › Diagnose öffnet die Mail-App mit vorbereitetem Text und der Diagnose als Anhang (`leotools@protonmail.com`). Gesendet wird erst vom Benutzer.

### Intern
- `WifiHelper.onAllowedNetwork`, `SyncScheduler.autoConstraints`, 8 Strings EN/DE.

## 0.11.4 (2026-11-08)

### Intern
- Typfehler `collectAsState` in `HomeParts.kt` behoben (Build-Fehler von 0.11.3).

## 0.11.3 (2026-11-08)

### Neu
- **Ordner anlegen:** In der PC-Ordnerauswahl lässt sich unterhalb einer Freigabe direkt ein neuer Ordner anlegen.

### Intern
- `invalidFolderName`, 6 neue Strings EN/DE, `RemoteFolderNameTest`. Nicht gebaut, da 0.11.3 am Typfehler scheiterte (siehe 0.11.4).

## 0.11.2 (2026-11-08)

### Neu
- **Ordner-Profile:** Die Profilkarte zeigt „X Dateien synchronisiert“ (Stand des letzten Abgleichs) und, falls vorhanden, „Y Konflikte offen“. Beides wird lokal gelesen, ohne PC-Zugriff.

### Intern
- `SyncDao.mirrorStateCountFlow`, `SyncDao.openConflictCountFlow` (reine Abfragen, keine Schema-Änderung).

## 0.11.1 (2026-11-08)

### Neu
- **Gesichert und offen:** Bei Fotos-Profilen zeigt die Profilkarte „X gesichert · Y offen“ (rein lokal berechnet, ohne PC-Zugriff). Bei Ordner-Profilen erscheint noch keine Anzeige.

### Geändert
- **Profil löschen:** Während der Rückgängig-Frist erscheinen keine Konfliktkarten dieses Profils mehr (auf Startseite und in der Konfliktliste). Die Konflikte bleiben gespeichert und sind nach „Rückgängig“ wieder da.

### Intern
- `ProfileCard` liest `SyncDao.syncedCount` und `PendingCheck.readyFiles(…, 0)`. Keine Datenbank-Änderung. Aufträge SYNC-20261108-002 und -007.

## 0.11.0 (2026-11-08)

### Neu
- **Ordner auf dem PC wählen:** Im Profil genügt der PC (z. B. `\\192.168.1.20`) mit Benutzer und Passwort. „Ordner auf dem PC wählen…“ zeigt die Freigaben des PCs und darin die Ordner. Das separate Feld „Unterordner“ entfällt; bestehende Profile behalten ihr Ziel.
- **Profil löschen rückgängig machen:** Nach dem Löschen erscheint „Profil gelöscht – Rückgängig“. Endgültig gelöscht wird erst, wenn der Hinweis verschwindet.
- **Restzeit:** Während einer Synchronisierung zeigt die Statuskachel nach etwa einer Minute die geschätzte Restzeit.
- **Hilfe & Produktseite** im Zahnrad-Menü (https://apop85.github.io/leosync/).

### Geändert
- **Verständliche Fehlermeldungen** mit nächstem Schritt (falsches Passwort, Freigabe oder Ordner nicht gefunden, kein Zugriff, PC nicht erreichbar, Speicher voll). Technische Details lassen sich beim Verbindungstest und in der Ordnerauswahl aufklappen und stehen im Verlauf.
- **Berechtigungen erst bei Bedarf:** Medienzugriff beim Anlegen eines Fotos-Profils, Standort beim WLAN, Benachrichtigungen und Akku nach dem ersten Profil. Ohne Profil fragt die Startseite nichts.
- **Meldung nach jeder Synchronisierung** ist bei neuen Installationen aus. Bestehende Einstellungen bleiben.
- **Aufgeräumte Hinweise:** Lange Erklärungen zeigen den ersten Satz, der Rest steht hinter „Mehr dazu“.
- **Screenreader:** Start und Ende einer Synchronisierung werden angesagt; die Richtungspfeile der Profilkarte werden als Wort vorgelesen.
- **Tipp** zum Herunterziehen, einmalig unter der Statuskachel.

### Intern
- Neue Bibliothek `com.rapid7.client:dcerpc:0.12.13` (Freigaben auflisten über `srvsvc`), APK +0,4 MB. R8-Regeln ergänzt.
- `friendlyError`/`userError` (sync/Errors.kt), `PermNeeds`, `PendingProfileDelete`, `HintMore`/`ErrorDetails`, `SmbSession.listShares`, `SmbTarget.hostOf`. Keine Datenbank-Änderung.
- Hilfe-Texte für die Produktseite: `docs/hilfe/hilfe-de.md`, `docs/hilfe/help-en.md`.

## 0.10.0 (2026-11-04)

### Neu
- **Nach unten ziehen zum Synchronisieren:** Steht die Startseite ganz oben, startet weiteres Herunterziehen eine Synchronisierung, wie „Neu laden“ im Browser. Es gelten dieselben Regeln wie beim Knopf (Hinweis zum Ladegerät bei der ersten Synchronisierung). Ohne Profil oder während einer laufenden Synchronisierung passiert nichts.

### Geändert
- **Zugangsdaten übernehmen:** Statt eines Knopfs pro Profil gibt es im Profil einen einzigen Knopf „Zugangsdaten übernehmen…“. Er öffnet eine Liste, in der gleiche Zugangsdaten (Benutzer, Domäne, Server, Passwort) nur einmal erscheinen, mit den Profilen, die sie verwenden.

### Intern
- `data/CredentialGroups.kt` mit Unit-Test `CredentialGroupsTest`. Keine Datenbank-Änderung.

## 0.9.5 (2026-11-04)

### Geändert
- **Android 16 als Ziel-Version** (targetSdk/compileSdk 36), wie es der Play Store verlangt. Auf Geräten bis Android 15 ändert sich nichts. Getestet im Emulator mit Android 17: Zurück-Geste im Assistenten und Rückfrage bei ungespeicherten Änderungen funktionieren.
- **Speicher freigeben:** Steht die Prüfung nach dem Hochladen auf „Aus“, erklärt ein Hinweis, warum keine Fotos angeboten werden.

### Intern
- Startseite liest die Einstellungen nur noch neu, wenn sich der Zustand eines Hintergrundauftrags ändert, nicht bei jedem Fortschritts-Update.
- Profil bearbeiten: Optionen-Karten für Ordner- und Fotos-Profile in `ui/ProfileCards.kt` ausgelagert.
- Synchronisierung mit einer Testfreigabe im Emulator geprüft: Hoch- und Herunterladen (inkl. 40 MB), Änderungen auf beiden Seiten mit alter Version im Papierkorb, Konflikt bei beidseitiger Änderung, gelöschte Datei wird ohne „Löschungen übernehmen“ wiederhergestellt.

## 0.9.4 (2026-11-04)

### Intern
- **Migrationstest:** Neuer instrumentierter Test `MigrationTest` prüft die Datenbank-Migrationen von Version 1 bis 6 gegen die exportierten Schemas und dass Profile richtig übernommen werden (Prüfstufe, Profilname). Er läuft auf einem Gerät oder Emulator mit `gradlew connectedDebugAndroidTest`, nicht im Builder.
- **Unit-Tests für die Prüfstufen** (`VerifyLevelTest`): gelesene Bereiche je Stufe, „Wenig“ = bisherige Prüfsumme, „Aus“ wird beim Entfernen von Fotos angehoben, Sicherung mit und ohne `verifyLevel`.
- **Lauf-Zustände als Konstanten** (`RunStatus.OK` usw.) statt freier Texte im Code. In der Datenbank steht derselbe Text, keine Migration.
- **Startseite aufgeteilt:** Karten und Dialoge stehen in `ui/HomeParts.kt`, `HomeScreen.kt` enthält nur noch den Aufbau der Seite.
- **`build.bat -NoPause`** beendet ohne „Taste drücken“.

## 0.9.3 (2026-11-04)

### Behoben
- **Statuskachel:** Bild, WLAN-Zeile und Knopf stehen jetzt in jedem Zustand an derselben Stelle. Die Zeile für „✓ Alles gesichert und aktuell“ samt Abstand ist immer reserviert, der Bereich darunter hat eine feste Mindesthöhe. Grösser wird die Kachel nur noch bei langen Fehlermeldungen.

### Geändert
- **„Sync“ ausgeschrieben:** In allen deutschen Texten steht jetzt „Synchronisierung“ bzw. „Synchronisieren“ (z. B. „Letzte Synchronisierung“, „Synchronisierungsstatus zurücksetzen“). Der App-Name LeoSync bleibt.

## 0.9.2 (2026-11-03)

### Behoben
- **Statuskachel während des Syncs:** Die Höhe ändert sich nicht mehr. Die Fortschrittszeile hat immer zwei Zeilen, der aktuelle Dateiname immer genau eine (lange Namen enden mit „…“), auch bevor der erste Dateiname bekannt ist.

## 0.9.1 (2026-11-03)

### Geändert
- **Sprache & Sicherung:** Die Länderordner (Heimatland) stehen jetzt direkt unter der Sprache, die Sicherung kommt danach.

### Intern
- Kotlin-Warnung in `SyncWorker` behoben (überflüssiger Elvis-Operator bei der Fehlermeldung).

## 0.9.0 (2026-11-03)

### Neu
- **Prüfung nach dem Hochladen in Stufen** (Fotos-Profile): Aus, Wenig, Mittel, Viel, Komplett. „Mittel“ ist Standard für neue Profile und liest bis 32 MB die ganze Datei zurück, darüber 32 Stichproben. „Aus“ (nur Grösse) geht nur, solange die Fotos auf dem Handy bleiben; wer „In den Papierkorb“ oder „Endgültig löschen“ wählt, wird automatisch auf „Mittel“ gestellt. Bei „Komplett“ gibt es einen Hinweis zu Zeit und Akku. Bestehende Profile behalten ihr Verhalten („Wenig“ bzw. „Aus“).
- **App-Berechtigungen mit Status:** Jeder Punkt zeigt „✓ erledigt“ oder einen Knopf, wie die Karte auf der Startseite.

### Geändert
- **Pause-Symbol oben:** Läuft die Automatik, steht dort ⏸ (pausieren), ist sie pausiert, ▶ (fortsetzen). Bisher war es umgekehrt.
- **Pause-Dialog:** Dauer zuerst auswählen (sichtbar markiert), dann „Pausieren“.
- **Statuskachel** springt nicht mehr in der Grösse, wenn sich der Zustand ändert. Unter „✓ Alles gesichert und aktuell“ ist etwas Abstand. Ohne Profil steht beim ausgegrauten „Synchronisieren“, warum.
- **Profilkarte:** Handy- und PC-Ordner je auf einer eigenen Zeile; „im WLAN ✓“ ist ein neutrales Etikett statt in Linkfarbe.
- **Statistik:** neues Balken-Symbol oben; die Balken sind beschriftet („Balkenlänge = Anzahl Dateien“).
- **Profil bearbeiten:** „Speichern“ und „Abbrechen“ stehen fest unten. „Profil löschen“ ist von oben rechts in den Bereich „Wartung“ gewandert. „Filter“, „Schutzbremse“ und „Wartung“ sind zugeklappt, solange darin nichts eingestellt ist. Das Feld „Unterordner auf der Freigabe“ nutzt die ganze Breite, „Durchsuchen“ steht darunter. Bei „Endgültig löschen“ erscheint eine Warnung.
- **Profil-Assistent:** Schritt 1 ohne Überschrift „Allgemein“ und ohne „Profil aktiv“.
- **Speicher freigeben:** „Vorschau“ ist der Hauptknopf, Entfernen ist umrandet und rot.
- **Auswahlfelder** sehen in „Speicher freigeben“ und im Papierkorb gleich aus.
- **Verlauf:** „OK“ steht nicht mehr auf jeder Karte, nur abweichende Zustände sind markiert.
- **Einstellungen:** „App-Einstellungen“ heisst „Sprache & Sicherung“ und enthält jetzt auch Export/Import (vorher unter „Diagnose“). Bei „Benachrichtigungen“ fällt die doppelte Überschrift weg.
- **Begriffe:** Deutsch „Handy“ und „Freigabe“ (statt „Telefon“/„Share“), Englisch „Mobile“ und „Share“. Filter heissen „Nur diese Dateien“ und „Diese Dateien ausschliessen“ (ohne Whitelist/Blacklist).
- Leerzeichen am Anfang und Ende von Profilnamen werden entfernt (vorher z. B. „(OrdnerTest )“).

### Intern
- **DB-Version 6** (`MIGRATION_5_6`): `profiles.verifyLevel INTEGER NOT NULL DEFAULT 2`, bestehende Profile = 1 (Prüfung an) bzw. 0 (aus, nur ohne Löschen); Profilnamen per `TRIM`. `6.json` entsteht beim Build und muss committet werden.
- `VerifyLevel` (Entities), `Profile.verify()`, `PartialHash.levelRegions/forLevel`, `SmbSession.levelHash`. „Wenig“ ist identisch mit der bisherigen Prüfung. Sicherung: `verifyLevel`, ältere Sicherungen über `verifyUpload`.
- Neu: `ui/PermissionRows.kt`, `CollapsibleCard` und `bottomBar` in `ScreenScaffold`, `AppIcons.BarChart`.

## 0.8.0 (2026-11-03)

### Neu
- **App auf Englisch:** Alle Texte gibt es jetzt auf Deutsch und Englisch, auch Benachrichtigungen, Verlauf und Fehlermeldungen. Ist das Handy auf Deutsch eingestellt (Schweiz, Deutschland, Österreich …), erscheint Deutsch, sonst Englisch.
- **Sprache wählen:** *Einstellungen → App-Einstellungen → Sprache* mit „Systemsprache“, „Deutsch“ und „English“. Ab Android 13 auch in den Android-Einstellungen der App.
- **Länderordner in der App-Sprache:** Neue Ordner heissen z. B. „06 - Egypt“ statt „06 - Ägypten“, wenn die App auf Englisch läuft. Bestehende Ordner in der anderen Sprache werden erkannt und weiterverwendet; es entsteht kein zweiter Ordner und keine Rückfrage. Statistik und Ordnerauswahl zeigen die Ländernamen ebenfalls in der App-Sprache.

### Geändert
- **Papierkorb heisst `.LeoSync-Bin`** (vorher `.LeoSync-Papierkorb`). Der alte Ordner wird weiter vom Sync ausgeschlossen, im Papierkorb angezeigt, kann wiederhergestellt und geleert werden.
- **„Beide Versionen behalten“** bei Konflikten benennt die Handy-Datei jetzt `…_conflict-phone` (vorher `…_Konflikt-Handy`).
- **Datum** auf Englisch als `2026-11-03`, auf Deutsch wie bisher `03.11.2026`.
- Verlaufseinträge werden in der Sprache gespeichert, die beim Lauf aktiv war. Ältere Einträge bleiben deutsch.

### Intern
- Texte in `res/values/strings_*.xml` (Englisch, Grundsprache) und `res/values-de/strings_*.xml` (Deutsch). Zugriff über `l10n/Texts.kt` (`str`, `plural`), Sprachwahl in `l10n/AppLanguage.kt`, `res/xml/locales_config.xml`.
- Neuer Unit-Test `StringsParityTest`: Jeder Text muss in beiden Sprachen mit denselben Platzhaltern vorhanden sein und sich formatieren lassen. Bestehende Tests prüfen weiterhin die deutschen Texte (`TestTexts`).
- `tools/make-translations.ps1` erzeugt `.claude/translations.md` (alle Texte Deutsch | Englisch mit Zeilennummern).
- Keine Datenbank-Änderung.

## 0.7.1 (2026-11-03)

### Behoben
- **Papierkorb:** „Wiederherstellen“ und „Endgültig löschen“ in der Auswahlleiste haben jetzt je eine eigene Zeile. Nebeneinander wurden die Texte abgeschnitten.

## 0.7.0 (2026-11-03)

### Neu
- **Speicher freigeben auch für Ordner-Profile:** Funktioniert jetzt auch bei Ordner-Profilen mit Richtung Handy → PC, wenn „Löschungen übernehmen“ ausgeschaltet ist.
  - Jede Datei wird zuerst auf dem PC geprüft: vorhanden, gleiche Grösse, gleicher Inhalt. Erst dann wird sie auf dem Handy direkt gelöscht.
  - Auf dem PC bleibt sie erhalten, auch wenn später „Löschungen übernehmen“ eingeschaltet oder der Sync-Status zurückgesetzt wird.
  - Infrage kommen nur Dateien, die schon synchronisiert wurden und seither unverändert sind.
- **Neue Auswahl in Zeilen** (Fotos- und Ordner-Profile). Jede Zeile zeigt ihren Wert; Tippen öffnet die Auswahl:
  - **Profil**, **Älter als** (Tage, Wochen, Monate, Jahre oder egal; Fotos nach Aufnahmedatum, Ordner nach Änderungsdatum) und **Ordner** (inklusive Unterordner).
  - Unter **„Weitere Kriterien“**: Dateitypen (Bilder, Videos, Audio, Dokumente, Archive, Andere, eigene Endungen), Grösse ab, Ausnehmen (Muster wie beim Filter) und bei Fotos „Favoriten behalten“.
  - Eine laufende Anzeige „Passend: N Dateien · Grösse“ zeigt das Ergebnis. In der **Vorschau** lassen sich einzelne Dateien oder ganze Ordner abwählen.
- **Kachel „Speicher freigeben“:** Sie ist ausgegraut, wenn kein passendes Profil vorhanden ist.

### Geändert
- **Papierkorb, eindeutige Namen:** Liegt am selben Tag schon eine gleichnamige Datei im Papierkorb, heisst die nächste jetzt `Foto (14-32-05).jpg` (Uhrzeit) statt `Foto_2.jpg`. Ältere Einträge mit „_2“ werden weiter wie bisher erkannt.
- **Hinweise auf der Startseite:** Wartet ein Profil auf eine Entscheidung der Schutzbremse, sagen „Wiederherstellung vorgemerkt“ und „Papierkorb leeren vorgemerkt“ das jetzt.

### Behoben
- **Aufräumen während eines Syncs:** Aufräumen, Endgültig löschen und Speicher freigeben (Ordner) warten jetzt, bis ein laufender Ordner-Sync fertig ist. Vorher konnte „Alles leeren“ eine gerade ersetzte Version sofort mitlöschen.
- **Vorgemerkte Wiederherstellungen sind geschützt:** Aufräumen, Endgültig löschen und das automatische Leeren nach Ablauf der Frist lassen diese Versionen liegen. Die Rückfrage nennt sie, im Papierkorb sind sie als „zum Wiederherstellen vorgemerkt“ markiert.
- **Wiederherstellen rollt bei Fehlern zurück:** Bricht das Wiederherstellen ab, kommen die aktuellen Versionen an ihren Platz zurück, und die Vormerkung bleibt. Die Startseite zeigt „Wiederherstellen fehlgeschlagen“ mit „Erneut versuchen“ und „Verwerfen“. Vorher konnte die Datei auf Handy und PC fehlen.
- **Wiederhergestellte Dateien:** Sie werden beim nächsten Sync nicht mehr unnötig neu geprüft. Bei manchen Speicherorten, z. B. „Downloads“, wurde vorher ein falscher Zeitstempel gespeichert.

## 0.6.4 (2026-11-03)

### Geändert
- **Papierkorb, Datumsauswahl:** Statt einer Knopfreihe mit einem Knopf pro Tag gibt es ein Feld „Tag“ mit dem aktuellen Wert, z. B. „Alle Tage“ oder „03.11.2026“. Ein Tippen öffnet eine Liste aller Tage, neueste zuerst, mit Anzahl und Grösse pro Tag. Unter „Aufräumen… → Einzelne Tage“ wählst du in derselben Liste mehrere Tage mit Häkchen; dazu gibt es „Alle“ und „Keine“.
- **Hochformat:** LeoSync bleibt immer im Hochformat. Drehen ins Querformat ist abgeschaltet.

## 0.6.3 (2026-11-03)

### Geändert
- **Papierkorb:** „Wiederherstellen“ und „Papierkorb leeren“ sind jetzt ein Tool namens „Papierkorb“.
  - Oben zeigt eine Übersicht den belegten Platz auf Handy und PC. Der Knopf **„Aufräumen…“** öffnet die Regeln zum Leeren (älter als … Tage, einzelne Tage, alles; Handy, PC oder beide).
  - Darunter steht der Inhalt. Sobald Einträge markiert sind, erscheint unten eine Leiste mit **„Wiederherstellen“** und **„Endgültig löschen“**.
  - Bei mehreren Ordner-Profilen lässt sich das Profil oben rechts wechseln.
  - Der Papierkorb gilt jetzt für alle Ordner-Profile, auch ohne „Alte Versionen behalten“. Gelöschte Dateien liegen ja trotzdem dort.
- **Tools:** „Speicher freigeben“ und „Papierkorb“ sind immer sichtbar. Ohne passendes Profil sind sie ausgegraut, und ein Tippen erklärt, wofür sie gedacht sind.

## 0.6.2 (2026-11-03)

### Behoben
- Build-Fehler aus 0.6.1: Das Pause-Symbol oben rechts fehlte, weil die Symbol-Datei überschrieben wurde. Es ist wieder da, das Smartphone-Symbol bleibt.

## 0.6.1 (2026-11-03)

### Geändert
- **Tools:** Jedes Tool hat jetzt eine eigene Zeile, links das Symbol, rechts Titel und Beschreibung.
- **Speicher freigeben:** zeigt ein Smartphone-Symbol statt des alten Telefonhörers.

## 0.6.0 (2026-11-03)

### Neu
- **Papierkorb leeren:** Neue Kachel unter Tools. Sie zeigt, wie viel Platz der Papierkorb zuletzt belegt hat (Handy und PC). Zuerst wählst du das Ordner-Profil (bei nur einem öffnet es direkt), dann:
  - **Was:** älter als … Tage (Standard 7), einzelne Tage oder alles
  - **Wo:** Handy und PC (Standard), nur Handy oder nur PC
  - Vor dem Löschen zeigt LeoSync Anzahl und Grösse und fragt nach. Endgültig gelöschte Dateien lassen sich nicht wiederherstellen.
  - Ist der PC nicht erreichbar, wird das Handy sofort geleert und der PC beim nächsten Sync mit PC.
- **Endgültig löschen** in „Papierkorb & alte Versionen“: markierte Einträge direkt löschen statt wiederherstellen.
- Fotos-Profile haben keinen eigenen Papierkorb – ein Hinweis erklärt, dass entfernte Fotos im Papierkorb der Galerie liegen.

## 0.5.2 (2026-11-03)

### Behoben
- **Warnung „Erste Synchronisierung“ kam immer wieder:** Waren die Ordner eines Profils auf beiden Seiten leer, hielt LeoSync jeden Start für die erste Synchronisierung. Jetzt gilt ein Profil nach dem ersten erfolgreichen Lauf nicht mehr als neu, auch wenn es nichts zu übertragen gab.

## 0.5.1 (2026-11-03)

### Behoben
- **Ordner-Sync: Unveränderte Datei wurde zurückgeschickt.** Direkt nach einem Download vom PC hielt LeoSync die Datei auf dem Handy beim nächsten Lauf für geändert, lud sie wieder hoch und legte mit dem Versionsschutz eine unveränderte Kopie in den PC-Papierkorb. Betroffen waren vor allem Ordner unter „Downloads“, deren Zeitstempel je nach Abfrage anders ausfallen. Jetzt:
  - Nach dem Download wird der Stand genau so gelesen wie beim späteren Einlesen.
  - Ist eine Datei nur scheinbar geändert (gleiche Grösse, gleicher Inhalt auf beiden Seiten), merkt sich LeoSync nur den neuen Stand, statt zu übertragen.
  - Die Prüfung „während des Laufs geändert?“ vor einem Download liest die Handy-Datei ebenfalls wie beim Einlesen. Sonst hätte LeoSync auf solchen Speicherorten Änderungen vom PC fälschlich überspringen können.

## 0.5.0 (2026-11-03)

### Neu
- **Alte Versionen behalten (Ordner-Profile):** Wird eine Datei auf Handy oder PC überschrieben, kommt die alte Version in den Ordner „.LeoSync-Papierkorb“, statt verloren zu gehen. Schalter „Alte Versionen im Papierkorb behalten“ im Profil, Standard an. Gelöscht wird erst nach der Papierkorb-Frist.
- **Papierkorb & alte Versionen:** Gelöschte Dateien und ältere Versionen zurückholen – einzeln, einen ganzen Ordner oder alles von einem bestimmten Tag. Zuerst wählst du das Profil (bei nur einem öffnet es direkt). Beim Wiederherstellen kommt die aktuelle Version zuerst in den Papierkorb, die gewählte Version wird auf Handy und PC zurückgeschrieben. Ist der PC nicht erreichbar, wird die Wiederherstellung vorgemerkt und beim nächsten Sync mit PC ausgeführt.
- **Startseite – Tools:** Unter den Profilen gibt es neu Kacheln für „Speicher freigeben“ und „Wiederherstellen“.
- **Profil-Assistent:** Neue Profile werden Schritt für Schritt eingerichtet: Profilname → Quelle → Ziel → Sync-Optionen → Filter (optional) → Schutzbremse (optional, nur Ordner). Jeder Schritt erklärt, was man dort einstellt. Ab den optionalen Schritten lässt sich direkt speichern.

### Geändert
- **Profil bearbeiten:** Die Karten stehen in derselben Reihenfolge wie die Schritte des Assistenten. Die Erklärungen sind präziser, z. B. beim WLAN: Ohne WLAN-Namen synchronisiert ein Profil nur von Hand (praktisch für VPN).

### Technik
- Datenbank-Version 5 (Versionsschutz, vorgemerkte Wiederherstellungen). Die Umstellung passiert beim ersten Start automatisch.

## 0.4.2 (2026-11-03)

### Neu
- **Schutzbremse einstellbar:** Im Ordner-Profil gibt es ganz unten „Schutzbremse (optional)“. Dort legst du fest, ab wie viel Prozent gelöschter Dateien LeoSync nachfragt (Standard 20 %).
- **Ganzer Unterordner verschwunden:** Würde ein ganzer Unterordner mit mehr als 5 Dateien in den Papierkorb kommen (z. B. „Steuern 2024“), fragt LeoSync nach, auch wenn das nur ein kleiner Teil des ganzen Profils ist.
- **Ab 100 Löschungen immer nachfragen:** Unabhängig von der Prozent-Grenze hält der Sync an, wenn auf einmal 100 oder mehr Dateien in den Papierkorb kämen.

### Geändert
- **Schutzbremse bei kleinen Ordnern:** Hat ein Ordner-Profil höchstens 5 synchronisierte Dateien, fragt LeoSync nur noch nach, wenn **alle** Dateien gelöscht würden. Einzelne Löschungen laufen ohne Rückfrage durch.

### Technik
- Datenbank-Version 4 (neue Profil-Einstellung). Die Umstellung passiert beim ersten Start automatisch, Profile und Verlauf bleiben erhalten.
- Build: Die beiden Übersetzungsschritte für das Datenbank-Schema (Debug und Release) laufen nacheinander. Gleichzeitig konnte einer eine halb geschriebene Schema-Datei lesen und abbrechen.

## 0.4.1 (2026-11-03)

### Sicherheit: Schutz vor Datenverlust beim Ordner-Sync
- **Schutzbremse:** Fehlt der PC-Ordner oder der Handy-Ordner eines Profils, das schon synchronisiert hat (z. B. umbenannt oder verschoben), hält der Sync an, statt alles als „gelöscht“ anzusehen. Ein leerer PC-Ordner wird nicht mehr neu angelegt. Auf der Startseite wählst du: **Neuen Ordner wählen**, **Erneut prüfen** oder **Neu anlegen und abgleichen** (ohne Löschen).
- **Viele Löschungen:** Würden mehr als 20 % der synchronisierten Dateien in den Papierkorb kommen, hält der Sync vor dem Löschen an. Du wählst **Nicht löschen** (Dateien bleiben bzw. werden wiederhergestellt) oder **Trotzdem in den Papierkorb**.
- **Ordnerwechsel:** Änderst du im Profil den Handy- oder PC-Ordner, prüft LeoSync stichprobenartig, ob der neue Ordner zum bisherigen Stand passt. Passt er nicht, fragt es: „Ist das wirklich der richtige Ordner?“ mit **Anderen Ordner wählen**, **Trotzdem übernehmen**, **Als neuen Ordner behandeln** oder **Abbrechen**. Ist der PC gerade nicht erreichbar, prüft der nächste Sync.
- Ein Profil, das auf dich wartet, ruht. Alle anderen Profile laufen normal weiter. Es gibt einen Hinweis, und im Verlauf steht „Wartet auf dich“.
- **Import:** Importierte Ordner-Profile starten erst, wenn du sie einmal von Hand synchronisierst. Mit Sync-Status importierte Ordner werden beim ersten Lauf zusätzlich stichprobenartig geprüft.
- **Ersetzen sicherer:**
  - Scheitert eine Übertragung, nachdem die alte Version in den Papierkorb kam, überträgt der nächste Sync neu, statt die andere Seite zu löschen.
  - Wird eine Datei während eines Laufs geändert, überschreibt LeoSync sie nicht mehr; der nächste Sync entscheidet.
  - Auf dem Handy wird die alte Version erst gelöscht, wenn die neue fertig an ihrem Platz ist (Zwischenname `.s2pold`). Bricht die App mittendrin ab, räumt der nächste Sync auf.
- **Genauerer Vergleich:** Beim ersten Abgleich und bei auf beiden Seiten geänderten Dateien vergleicht LeoSync Dateien bis 32 MB vollständig, grössere an 32 Stellen statt an 3.
- **Doppelte Ordner-Profile:** Warnung, wenn ein Ordner-Profil denselben oder einen verschachtelten PC- bzw. Handy-Ordner wie ein anderes Profil nutzt.

### Sicherheit: Fotos und „Speicher freigeben“
- **Fortgesetzte Uploads** (alle Profile): Eine angefangene Datei auf dem PC wird nur weitergeschrieben, wenn sie zur aktuellen Datei passt, und vor dem Fertigstellen immer geprüft. Sonst beginnt der Upload neu.
- **Speicher freigeben:** Wurde ein Foto auf dem PC in einen anderen Monatsordner verschoben, genügen Name und Grösse nicht mehr – zusätzlich muss der Inhalt (Teil-Hash) übereinstimmen.

### Neu
- **Neues Zustandsbild:** Ein stehender Hund zeigt an, dass LeoSync auf eine Entscheidung von dir wartet (Schutzbremse, Ordnerauswahl, Konflikte, Dateien entfernen).

## 0.4.0 (2026-11-02)

### Neu
- **Einstellungen neu gegliedert:** Das Zahnrad öffnet ein Menü mit sechs Rubriken. Jede ist eine eigene Seite:
  1. **Benachrichtigungen:** Schalter, Erinnerung „lange nicht gesichert“
  2. **Übertragung:** Automatik, parallele Übertragungen
  3. **App-Berechtigungen:** Hintergrund, Akku-Optimierung, Hersteller-Hinweise, „Anleitung im Browser“
  4. **App-Einstellungen:** Länderordner
  5. **Updates:** prüft beim Öffnen sofort, ohne zusätzlichen Knopf, mit der täglichen Prüfung
  6. **Diagnose:** Sicherung, Diagnosedaten
- **Benachrichtigungen einzeln abschaltbar:** „Meldung nach jedem Sync“, „Update verfügbar“ und „Hinweise“ (Ordnerauswahl, Konflikte, Dateien entfernen, Fehler – stehen auch ohne Benachrichtigung auf der Startseite). Dazu ein Knopf zu den Android-Einstellungen für Benachrichtigungen. Die Fortschrittsanzeige und „Pausiert – wartet auf WLAN“ bleiben bewusst immer an.

### Geändert
- **Update-Quelle fest:** Das GitHub-Repository lässt sich nicht mehr ändern. Die App lädt Updates immer aus dem eingebauten Repository; ein früher geänderter Wert wird ignoriert, und eine importierte Sicherung setzt es nicht mehr.
- **Play Store:** Die Vorbereitung für den Play Store ist **nicht** Teil von 0.4.0, sondern folgt in einer späteren Version.

## 0.3.7 (2026-11-02)

### Neu
- **Neue Profile starten erst nach deiner Freigabe:** Nach dem Anlegen fragt die App „Wie weiter?“ mit **Simulieren**, **Synchronisieren** oder **Später**. Bis du das Profil einmal von Hand synchronisierst, startet es nicht automatisch; die Profilkarte zeigt „Wartet auf den ersten Start“. Importierte Profile laufen wie bisher sofort.
- **Simulation pro Profil:** Der Knopf „Simulieren“ ist jetzt im Profil (Abschnitt „Simulation“) statt auf der Startseite. Du wählst „Nur die ersten [Anzahl] Dateien“ (Anzahl frei wählbar, wird gemerkt) oder „Alle Dateien“.
- **Warnung vor der ersten Synchronisierung:** Ist das Telefon nicht am Ladegerät und synchronisiert ein Profil zum ersten Mal, weist die App darauf hin, dass das lange dauern und viel Akku brauchen kann („Trotzdem starten“ / „Abbrechen“).

### Geändert
- **Startseite:** „Jetzt synchronisieren“ heisst jetzt **„Synchronisieren“** und bricht nicht mehr um. „Vorschau“ heisst neu „Simulieren“ und ist ins Profil umgezogen.

## 0.3.6 (2026-11-02)

### Geändert
- **Verlauf:** Der Knopf **„Alle“** ist zurück, neben „Fehler“ und „Übertragungen“. „Alle“ ist aktiv, solange kein Filter eingeschaltet ist, und setzt beim Antippen beide Filter zurück.

## 0.3.5 (2026-11-02)

### Neu
- **„Für immer (nur manuell)“** im Pause-Dialog: schaltet die Automatik ganz ab. Es gibt dann keine Syncs bei WLAN-Verbindung, bei neuen Fotos, durch das Sicherheitsnetz oder die Ordner-Prüfung, und auch keine Erinnerung. Synchronisiert wird nur noch mit „Jetzt synchronisieren“, ohne dass das die Einstellung aufhebt. Die Startseite zeigt „⏸ Automatik aus – nur manuelle Syncs“. ⏸ oben rechts schaltet die Automatik wieder ein.

### Geändert
- **Filter im Verlauf:** Statt „Alle“ und „Nur Übertragungen und Probleme“ gibt es zwei kurze Knöpfe, **„Fehler“** und **„Übertragungen“**. Beide lassen sich einzeln oder zusammen einschalten. Ist keiner aktiv, erscheinen alle Läufe.
  - „Fehler“: fehlgeschlagene oder teilweise erfolgreiche Läufe und Läufe mit Datei-Fehlern
  - „Übertragungen“: Läufe, die mindestens eine Datei übertragen haben
  - Beide aktiv: Läufe mit Fehlern oder Übertragungen

## 0.3.4 (2026-11-02)

### Verbessert
- **Kürzere Zusammenfassungen** in Verlauf und Benachrichtigung: nur noch, was passiert ist, z. B. „3 übertragen“ oder „3 übertragen, 12 bereits vorhanden“ (Dateien, die die Duplikatsuche schon auf dem PC gefunden hat). Ohne Übertragung steht „Nichts zu tun“. Beim Ordner-Sync erscheinen nur die Teile, die nicht 0 sind („↓ 3 heruntergeladen“). Das Detail-Log beim Aufklappen enthält weiterhin alle Zahlen.
- **Verlauf übersichtlicher:**
  - Aufeinanderfolgende Läufe ohne Übertragung erscheinen als **ein** Eintrag („12× geprüft – nichts zu tun“ mit Zeitraum und Profilen). Antippen zeigt die einzelnen Läufe.
  - Neuer Filter oben: „Alle“ oder „Nur Übertragungen und Probleme“. Die Auswahl bleibt gespeichert.
  - Der Verlauf zeigt jetzt alle Läufe der letzten 90 Tage statt nur der letzten 100.
- **Akku:**
  - Gibt es bei einem Fotos-Profil keine neuen Fotos, wird der PC gar nicht erst kontaktiert (kein Erreichbarkeitstest, keine Verbindung).
  - Neue Fotos lösen den Sync erst aus, wenn sie das Mindestalter erreicht haben. So reicht ein Lauf statt zwei.
  - Das **Sicherheitsnetz** (zusätzliche Prüfung, falls ein Auslöser verloren geht) läuft jetzt **6 Stunden nach dem letzten Sync** statt in einem starren 6-Stunden-Rhythmus. Jeder Sync schiebt es nach hinten.

## 0.3.3 (2026-11-02)

### Behoben
- **Statusanzeige:** „Sync eingeplant – wartet auf passendes WLAN“ erschien auch dann, wenn das Heim-WLAN verbunden war und nur eine zeitversetzte Prüfung geplant war (z. B. der Nachhol-Sync für gerade erst aufgenommene Fotos). Jetzt gilt:
  - Wartet ein Sync wirklich, nennt die Startseite den Grund: „Wartet auf das Heim-WLAN“, „Wartet auf das Ladegerät“ oder „Sync startet gleich…“.
  - Ist nur eine spätere Prüfung geplant, zeigt sie den letzten Lauf und darunter „Nächste Prüfung um …“. Ist alles gesichert, erscheint der schlafende Hund.
  - Ein Lauf ohne Übertragung erscheint als „Nichts zu tun – alles aktuell“.
- **PC-Import-Werkzeug** (`tools\LeoSync-Import.bat`): Bei mehr als ca. 2 GB zu kopierender Daten brach das Werkzeug vor der ersten Datei mit „Der Wert für einen Int32 war zu groß“ ab (Fortschrittsanzeige). Es wurde dabei nichts kopiert.

## 0.3.2 (2026-11-01)

### Verbessert
- **Eigenes Bild für „Pausiert“:** Ist der Dienst vom Benutzer pausiert, zeigt die Startseite den Hund auf dem Rücken statt des sitzenden Hundes.

## 0.3.1 (2026-11-01)

### Neu
- **Dienst pausieren:** Oben rechts auf der Startseite zeigt ein Symbol den Zustand: **▶** = automatische Syncs laufen, **⏸** = pausiert.
  - Antippen bei ▶ fragt „Wie lange pausieren?“ (1 / 4 / 24 Stunden oder eine eigene Anzahl bis 168). Ein laufender Sync wird angehalten, und bis zum Ende der Pause startet nichts automatisch. Danach läuft alles von selbst wieder an.
  - Antippen bei ⏸ oder „Fortsetzen“ beendet die Pause sofort. „Jetzt synchronisieren“ beendet sie ebenfalls.
  - Die Startseite zeigt „⏸ Pausiert bis …“. Die Erinnerung „lange nicht gesichert“ läuft auch während der Pause.
- **Nachfrage bei ungespeicherten Änderungen:** Wer ein Profil mit Änderungen verlässt, wird gefragt: Speichern, Verwerfen oder Abbrechen.
- **Speichern-Häkchen** oben in der Titelleiste des Profils, ohne nach unten scrollen zu müssen.

### Geändert
- Der Knopf „Abbrechen“ während eines Syncs heisst jetzt **„Pausieren“**, in der App und in der Benachrichtigung. Er hält nur den laufenden Sync an, beim nächsten automatischen Start geht es weiter. Im Verlauf steht „Pausiert“.

### Behoben
- Über *Downloads* gewählte Handy-Ordner erscheinen als „Download/Test“ statt „SD-Karte: /storage/emulated/0/…“.
- Vier Build-Warnungen aufgeräumt.

## 0.3.0 (2026-11-01)

### Neu
- **Ordner-Sync überträgt parallel:** Statt einer Datei nach der anderen laufen jetzt so viele Übertragungen gleichzeitig, wie unter *Einstellungen → Parallele Übertragungen* eingestellt sind (dieselbe Einstellung wie beim Fotos-Sync, Standard 3). Bei vielen kleinen Dateien ist das deutlich schneller.
- **Kleine Dateien zuerst:** Der Fortschritt kommt schneller voran, und bei einem Abbruch sind möglichst viele Dateien schon fertig.
- **Abgebrochene Downloads setzen fort:** Bricht ein Download aufs Handy ab, lädt der nächste Lauf nur noch den Rest. Wurde die Datei auf dem PC inzwischen geändert, beginnt der Download neu. Uploads zum PC konnten das schon bisher.

### Verbessert
- Bricht die Verbindung kurz ab, wird sie neu aufgebaut und die Datei wiederholt. Ist das Netz weg, stoppen alle Übertragungen sauber, und der Lauf gilt als „Unterbrochen“.
- Neue Ordner auf dem Handy werden auch bei parallelen Übertragungen nur einmal angelegt, es entsteht kein „Ordner (1)“.
- Am Fotos-Sync ändert sich nichts.

## 0.2.1 (2026-11-01)

### Neu
- **Keine doppelten Profile:**
  - Ein Profil mit **gleichem Ziel und denselben Quellen** wie ein bestehendes wird nicht gespeichert. Beim Ordner-Profil zählen der gleiche Handy-Ordner und das gleiche Ziel.
  - Bei **teilweiser Überschneidung** (gleiches Ziel, mindestens ein gemeinsamer Quellordner) erscheint eine Warnung mit „Trotzdem speichern“.
  - Gross- und Kleinschreibung sowie `/` und `\` spielen beim Vergleich keine Rolle. Deaktivierte Profile zählen mit.
- **Import mit „Hinzufügen“:** Identische Profile werden übersprungen, teilweise überschneidende importiert und im Ergebnis als Hinweis aufgeführt. Das gilt auch für doppelte Profile innerhalb derselben Sicherungsdatei.

## 0.2.0 (2026-11-01)

### Neu
- **Sicherung** (*Einstellungen → Sicherung*): Profile, Einstellungen, Ordner-Zuordnungen und auf Wunsch der Sync-Status lassen sich in eine Datei exportieren und wieder importieren, z. B. vor einem Handywechsel.
  - **Optionales Export-Passwort:** Ist eines gesetzt, sind die SMB-Passwörter in der Datei verschlüsselt (AES-256-GCM, Schlüssel per PBKDF2 aus dem Export-Passwort). Ohne Export-Passwort enthält die Datei keine Passwörter.
  - Der Import zeigt zuerst eine Vorschau. Danach kannst du die bisherigen Profile **ersetzen** oder die neuen **hinzufügen**. Bei falschem oder fehlendem Export-Passwort werden die Profile trotzdem übernommen, nur ohne SMB-Passwort.
  - Mit Sync-Status weiss die App nach dem Import sofort, welche Dateien schon auf dem PC sind.
- **Erinnerung „lange nicht gesichert“:** Gibt es seit 7 Tagen (einstellbar) keine erfolgreiche Sicherung, obwohl neue Fotos oder Videos warten, erscheint eine Benachrichtigung. Sie wiederholt sich alle 3 Tage und verschwindet nach der nächsten erfolgreichen Sicherung. Abschaltbar unter *Einstellungen → Erinnerung*.
- **Speicher freigeben** (Startseite, unter den Profilen): Fotos und Videos, die älter als eine selbst gewählte Anzahl Monate sind und nachweislich auf dem PC liegen, kommen in den Android-Papierkorb. Dort bleiben sie 30 Tage wiederherstellbar.
  - Vor dem Verschieben prüft die App **jede Datei auf dem PC** (vorhanden und gleich gross, auch wenn sie in einen anderen Ordner desselben Monats verschoben wurde). Was fehlt oder abweicht, bleibt auf dem Handy.
  - Optional bleiben Favoriten erhalten (ab Android 11).
  - Live-Foto-Paare (Bild und Video) werden nur gemeinsam verschoben.

### Technik
- Neue Unit-Tests für das Sicherungsformat, die Verschlüsselung mit dem Export-Passwort, ältere Sicherungen, die Wiederholung der Erinnerung und die Live-Foto-Paare.
- Keine Datenbank-Änderung.

## 0.1.6 (2026-11-01)

### Behoben
- **Läufe, die beim Beenden der App offen blieben**, zum Beispiel durch ein Update während eines Syncs, stehen im Verlauf jetzt als „Unterbrochen – App wurde beendet (z. B. Update), Rest folgt beim nächsten Sync“. Bisher stand dort ein leeres „Abgebrochen“. Als Endzeit gilt der letzte Log-Eintrag des Laufs, und im Log steht ein entsprechender Hinweis.

## 0.1.5 (2026-11-01)

### Verbessert
- **Grösserer laufender Hund:** In 0.1.4 wurden die Bilder in ihrer Pixelgrösse gezeichnet statt in der vorgesehenen Höhe. Der laufende Hund war dadurch nur etwa halb so hoch wie der sitzende. Jetzt erhalten alle Bilder eine feste Höhe: Der laufende Hund ist etwa gleich hoch wie der sitzende, und der sitzende und der schlafende Hund bleiben so gross wie bisher.
- **Kein Springen mehr:** Der Bildbereich ist in allen Zuständen gleich hoch. Die Startseite verschiebt sich nicht, wenn ein Sync beginnt oder endet.
- **Schärfere Animation:** Die Einzelbilder haben jetzt 320 px statt 160 px. Davon profitiert auch die Benachrichtigung. Die APK wird dadurch etwas grösser.

## 0.1.4 (2026-11-01)

### Verbessert
- **Hund auf der Startseite zeigt immer den Zustand:**
  - Sync läuft → **laufender Hund** (dieselbe Animation wie in der Benachrichtigung)
  - Alles gesichert → **schlafender Hund**
  - Sonst (wartet auf WLAN oder PC, Ordnerauswahl offen, unterbrochen …) → **sitzender Hund**
- Die Animation läuft nur, solange die App sichtbar ist, und braucht im Hintergrund keinen Akku. Sind Animationen in den Android-Einstellungen ausgeschaltet, bleibt das Bild stehen.

## 0.1.3 (2026-11-01)

### Verbessert
- **Absturzberichte:** Stürzt die App ab, speichert sie den Fehlerbericht lokal auf dem Handy (die letzten 3). Die Diagnose zeigt ihn im neuen Abschnitt „Abstürze“. Übertragen wird nichts automatisch.

### Technik
- Build auf **Gradle 9.8.2** und **Android Gradle Plugin 9.5.0** umgestellt (Kotlin 2.4.21 eingebaut, KSP 2.3.13, Room 2.8.5). Der Builder funktioniert jetzt auch mit dem Java 25 von Android Studio, ein zusätzliches JDK 21 ist nicht mehr nötig.
- Neue Unit-Tests für die Diagnose (Zusammenfassung der Log-Zeilen, Hersteller-Erkennung, Absturzberichte).

## 0.1.2 (2026-11-01)

### Behoben
- **„Unterbrochen“ statt „Abgebrochen“:** Stoppt Android einen Lauf, weil das WLAN getrennt wurde oder aus einem anderen Systemgrund, steht im Verlauf jetzt „Unterbrochen (WLAN getrennt) – wird automatisch fortgesetzt“. „Abgebrochen“ erscheint nur noch, wenn du selbst auf *Abbrechen* tippst.
- **Diagnose:** Die Zusammenfassung zählt jetzt Dateien statt Log-Zeilen („= 775 Dateien bereits vorhanden (76 Einträge: …)“).
- **Diagnose:** Die Warnung vor Hersteller-Killern nennt den Zeitpunkt des letzten Vorfalls. So sieht man, ob alte Fälle noch nachwirken oder ob es wieder passiert.

## 0.1.1 (2026-11-01)

### Verbessert
- **Übersichtlicherer Verlauf:** Bereits vorhandene Dateien werden nicht mehr einzeln protokolliert, sondern pro Monatsordner zusammengefasst (z. B. „= 37 Dateien bereits in 2014\04 vorhanden“).
- **Laufnummer im Verlauf:** Jede Karte zeigt die Nummer des Laufs (z. B. „#25“), passend zu Diagnose und Export.
- **Diagnose:** Sie zeigt zuerst alle Warnungen und Fehler der letzten Läufe und danach ein gekürztes Log. Gleichartige „bereits vorhanden“-Zeilen werden dabei zusammengefasst. Beendet ein Hersteller-Stromsparer die App (z. B. „o-kill“ bei OnePlus), weist die Diagnose darauf hin.
- **Hinweise je Hersteller** unter *Einstellungen → Hintergrund* für OnePlus/Oppo/Realme, Samsung, Xiaomi und Huawei/Honor. Dazu gibt es einen Link zu dontkillmyapp.com.

### Behoben
- Nach dem erneuten Verbinden mit dem WLAN blieb intern „PC nicht erreichbar“ stehen, bis der nächste Lauf fertig war.

## 0.1.0 (2026-11-01) – Beta

### Neu
- **Beta-Phase:** Ab hier gibt es nur noch Fehlerbehebungen (0.1.x). Kommen neue Funktionen dazu, geht es mit 0.2.x weiter.
- **Diagnosedaten kopieren** (*Einstellungen → Beta / Diagnose*): kopiert Angaben zu Gerät, Akku- und Hintergrund-Einschränkungen, Berechtigungen, WLAN, Einstellungen, Profilen, geplanten Aufträgen, den letzten Prozess-Enden und den letzten Läufen in die Zwischenablage. Passwörter sind nie enthalten.
- **Geräte-ID:** Eine zufällige ID (keine Hardware-Kennung) steht in jeder Diagnose und jedem Verlauf-Export, damit sich Logs einem Gerät zuordnen lassen.
- **Verlauf teilen:** Im Verlauf exportiert ein neuer Knopf alle Läufe samt Log als Textdatei über das Android-Teilen-Menü.
- **Papierkorb leert sich selbst** (Ordner-Sync): Datumsordner im `.LeoSync-Papierkorb` (und im alten `.Sync2PC-Papierkorb`), die älter als die Frist sind, werden auf Handy und PC endgültig gelöscht. Die Frist ist pro Profil einstellbar, Standard 30 Tage, 0 = nie.

### Technik
- Datenbank-Version 3 (neue Spalte `trashRetentionDays`, Migration 2→3). Die bisherigen Daten bleiben erhalten.
- Release-Checkliste in `build.md`.

## 0.0.13 (2026-11-01)

### Neu
- **Keine Duplikate nach Umsortieren:** Vor jedem Upload sucht die App in **allen Ordnern desselben Monats** (z. B. `10`, `10 - Ägypten`, `10 - Ferien`) nach derselben Datei: gleicher Name (auch `bild_2.jpg` …), gleiche Grösse und gleiche Prüfsumme. Wird sie fündig, gilt die Datei als gesichert und wird nicht nochmals übertragen. Das gilt auch nach „Sync-Status zurücksetzen“ oder einer Neuinstallation.
- **PC-Import-Werkzeug** `tools\LeoSync-Import.bat`: sortiert Fotos und Videos aus einem Ordner auf dem PC nach denselben Regeln wie die App in den Share ein.
  - Gleiche Regeln: Jahr\Monat - Land, Heimatland ohne Land, Rückfrage bei umbenannten Ordnern, gleiche Duplikatprüfung, `_2`-Namen.
  - Ablauf: Quellordner wählen, Vorschau ansehen, bestätigen. Kopiert wird nur nach ausdrücklichem „j“.
  - Der Quellordner bleibt unverändert. Das Änderungsdatum bleibt erhalten. Jede Kopie wird geprüft. Ein Protokoll liegt unter `%LOCALAPPDATA%\LeoSync\import-logs`.
- **Startbildschirm:** Beim Öffnen der App erscheinen der sitzende Hund und der Schriftzug „LeoSync“ (ca. 1,3 Sekunden, dann weiche Überblendung).
- Das App-Grundthema folgt jetzt dem hellen bzw. dunklen Modus des Systems, ohne weisses Aufblitzen beim Start im Dunkelmodus.

## 0.0.12 (2026-11-01)

### Geändert
- **Neuer Name: LeoSync** (Teil der „Leo“-Tool-Familie). Startbildschirm, Benachrichtigungen, Texte und APK-Dateiname (`LeoSync-<version>.apk`) sind umbenannt.
- Unverändert bleiben die interne App-ID (`com.sync2pc.app`), Profile, Verlauf, gespeicherte Passwörter, der Signatur-Schlüssel und die Update-Quelle (`Apop85/Sync2PC`). Das Update läuft also normal über die installierte App.
- Der Papierkorb beim Ordner-Sync heisst jetzt `.LeoSync-Papierkorb`. Ein vorhandener `.Sync2PC-Papierkorb` wird weiterhin nie synchronisiert.

## 0.0.11 (2026-11-01)

### Behoben
- **Tausende Fehler „DiskShare has already been closed“:** Die Verbindung, über die die App Zielordner nachschlägt, blieb während langer Uploads ungenutzt. Windows trennt solche Verbindungen nach etwa 15 Minuten, danach scheiterte jede weitere Datei. Jetzt prüft die App jede Verbindung vor der Nutzung und baut sie bei Bedarf neu auf.
- **Verbindungsabbrüche sind keine Fehler mehr:**
  - Bricht die Verbindung während einer Datei ab, baut die App sie neu auf und setzt den Upload an der Abbruchstelle fort.
  - Klappt das nicht, endet der Lauf als „Unterbrochen“, ohne Fehlerzähler und ohne Benachrichtigung. Die offenen Dateien folgen beim nächsten Sync.
  - Bleibt das WLAN dabei verbunden, folgt der nächste Versuch nach 2 Minuten statt nach 30.
- Fehlermeldungen im Verlauf sind kurz (nur die eigentliche Ursache statt der ganzen Fehlerkette).

### Neu
- **WLAN-Sperre während der Übertragung:** Das Handy hält das WLAN im Standby bzw. bei gesperrtem Bildschirm wach, solange ein Sync läuft.
- **Neue Animation** aus `new_animation.mp4`: 32 Bilder eines gehenden Hundes, Hintergrund und Wasserzeichen entfernt, nahtlose Schleife.
- **Zustandsbilder:**
  - Sitzender Hund = wartet auf Fortsetzung („Pausiert“-Anzeige und Startseite).
  - Schlafender Hund = alles gesichert und aktuell (Startseite und „fertig“-Meldung).

### Build
- `tools/make-anim-video.ps1`: Video → Animationsbilder (ffmpeg); der Builder nimmt das Video bevorzugt vor `gif_frames.png`.
- `tools/make-state-images.ps1`: bereitet `wait_cleaned.png` und `idle_cleaned.png` auf. Beide Skripte laufen automatisch, wenn sich die Vorlagen ändern.

## 0.0.10 (2026-11-01)

### Geändert
- **„Pausiert“ statt Unterbruch-Hinweis:** Nach einem WLAN-Unterbruch erscheint keine eigene Benachrichtigung mehr. Die Statusleiste zeigt stattdessen **„Pausiert – wartet auf WLAN …“** mit dem bisherigen Fortschritt und einem stehenden Hund, bis das Heim-WLAN wieder verbunden ist. Danach wechselt die Anzeige direkt zum normalen Fortschritt.
- Die Pausiert-Anzeige läuft als Vordergrunddienst. Er hält die App wach und lauscht selbst auf das WLAN. Grund: Im Feldtest von 0.0.9 wurde die App nach dem Wegwischen der Benachrichtigung so gründlich beendet, dass weder der WLAN-Broadcast noch der Aufgabenplaner sie weckten. Der Dienst wartet nur und braucht praktisch keinen Akku.
  - Der Fortsetzen-Auftrag beim Aufgabenplaner bleibt als Rückfallebene, z. B. wenn Android den Dienst nicht erlaubt oder (ab Android 15) nach 6 Stunden beendet.
- Die Startseite zeigt „⏸ Pausiert – wartet auf das Heim-WLAN“.

## 0.0.9 (2026-11-01)

### Behoben
- **Kein Fortsetzen nach WLAN-Unterbruch, wenn die Benachrichtigung entfernt wurde:** Nach einem Abbruch blieb die Fortschrittsanzeige stehen und hielt die App am Leben. Wurde sie weggewischt oder angetippt, beendete Android die App. Das Wecken beim Wiederverbinden war dann auf manchen Geräten blockiert.
  - Neu hinterlegt die App nach einem Unterbruch zusätzlich einen Auftrag beim Android-Aufgabenplaner („fortsetzen, sobald WLAN da ist“). Dieser startet die App auch, wenn sie beendet wurde. Ist es das falsche WLAN, schaut er alle 15 Minuten erneut nach, höchstens 24 Stunden lang.
  - Die Fortschrittsanzeige wird nach jedem Lauf zuverlässig entfernt und nach einem Stopp nicht mehr neu erzeugt.
- „Abbrechen“ durch dich gilt nicht als Unterbruch; der Sync wird dann nicht automatisch fortgesetzt.

### Neu
- Hinweis-Benachrichtigung „Sync unterbrochen – wird automatisch fortgesetzt …“. Sie darf weggewischt werden und verschwindet von selbst, sobald der Sync weiterläuft.

## 0.0.8 (2026-11-01)

### Behoben
- **Kein Sync nach dem WLAN-Verbinden:** Direkt nach dem Verbinden ist das Netz oft noch nicht bereit. Bisher galt der PC dann nach einem einzigen Versuch von 3 Sekunden als „nicht erreichbar“, und die App wartete 30 Minuten. Jetzt gilt:
  - Die App versucht es etwa eine Minute lang mehrmals (nach 5, 10, 15 und 30 Sekunden). Die Benachrichtigung zeigt dabei „Warte auf PC…“.
  - Liefert Android den WLAN-Namen noch nicht, liest die App ihn bis zu 15 Sekunden lang nach.

### Neu
- **Grund bei übersprungenem Auto-Start:** Die Startseite zeigt, warum ein automatischer Start nicht synchronisiert hat, z. B. „WLAN-Name nicht lesbar“, „WLAN ist in keinem Profil eingetragen“ oder „PC nicht erreichbar“.
- **Laufender Hund** in der Fortschritts-Benachrichtigung, auch auf dem Sperrbildschirm. Er besteht aus 16 Einzelbildern (Benachrichtigungen können keine GIFs abspielen) und pausiert bei ausgeschaltetem Bildschirm.

### Build
- `tools/make-anim.ps1` zerlegt `.claude/gif_frames.png` (4×4) in Einzelbilder und entfernt den halbtransparenten Schleier. Der Builder ruft es automatisch auf, wenn sich die Vorlage ändert.

## 0.0.7 (2026-11-01)

### Neu
- **Ordner synchronisieren:** neuer Profiltyp neben „Fotos & Videos sichern“. Ein beliebiger Handy-Ordner wird 1:1 mit einem PC-Ordner gespiegelt, mit Unterordnern und allen Dateitypen, ohne Jahresordner.
  - **Richtung pro Profil:** Handy → PC, PC → Handy oder beidseitig.
  - **Löschungen übernehmen** (optional, Standard aus): Gelöschte Dateien landen auf der Gegenseite im Ordner `.Sync2PC-Papierkorb\<Datum>`. Ist die Option aus, werden gelöschte Dateien wiederhergestellt.
  - **Konflikte** (beidseitig, auf beiden Seiten geändert): Standard ist, zu fragen (Handy-Version, PC-Version oder beide behalten). Optional pro Profil: immer die neuere Datei. Die verworfene Version kommt in den Papierkorb, nichts wird endgültig gelöscht.
  - **Prüfintervall** für Änderungen (Standard 60 Minuten, nur im Ziel-WLAN), zusätzlich sofort beim Verbinden mit dem WLAN.
  - **Speicherplatzprüfung:** Downloads, für die auf dem Handy nicht genug Platz ist (300 MB Reserve), werden übersprungen und gemeldet.
- **Profil-Assistent:** Beim Anlegen wählst du zuerst den Typ, danach zeigt der Editor nur die passenden Optionen.
- **Unterordner auf dem Share** pro Profil, mit Ordner-Browser („Durchsuchen“).
- **White- und Blacklist** (z. B. `*.pdf`, `Thumbs.db`, `Temp/**`) für beide Profiltypen.
- **Konflikt-Seite** mit „Alle: neuere“ sowie Benachrichtigung und Karte auf der Startseite.
- Die Vorschau zeigt bei Ordner-Profilen die geplante Aktion je Datei.

### Behoben
- Das Aufräumen alter Verlaufseinträge hat bei jedem Lauf den gesamten Verlauf gelöscht (falsche Zeitgrenze). Jetzt bleiben Verlaufseinträge 90 Tage und Detail-Logs 30 Tage.

### Build
- Der Builder schreibt die vollständige Gradle-Ausgabe zusätzlich nach `.build\last-build.log`.

### Technisch
- Datenbank Version 2 mit Migration; bestehende Profile bleiben erhalten und werden zu Bildordner-Profilen.
- Bildordner-Profile sind fest auf Handy → PC eingestellt. „Nach Sync löschen“ gibt es nur bei Bildordnern.

## 0.0.6 (2026-11-01)

### Neu
- **Zugangsdaten wiederverwenden:** Im Profil-Editor gibt es oben bei „Ziel“ eine Auswahl der anderen Profile. Ein Tippen übernimmt Benutzer, Passwort und Domäne, bei leerem Pfad auch den Freigabe-Pfad.

## 0.0.5 (2026-11-01)

### Geändert
- **Fortsetzen nach WLAN-Unterbruch:** Bricht ein Sync ab, weil das WLAN weg ist, wartet die App nicht mehr 30 Minuten. Sie setzt sofort fort, sobald das gemerkte WLAN wieder verbunden ist. Das gilt auch bei kurzen Aussetzern, die bisherige 60-Sekunden-Drosselung greift dann nicht.
- Die 30-Minuten-Prüfung gilt nur noch, wenn das Ziel-WLAN verbunden bleibt, der PC aber nicht erreichbar ist (z. B. ausgeschaltet).

## 0.0.4 (2026-11-01)

### Neu
- **PC-Erreichbarkeit im Ziel-WLAN:** Vor jedem Sync prüft die App mit einem kurzen Verbindungstest (Port 445, max. 3 Sekunden, ohne Anmeldung), ob der PC erreichbar ist.
  - Ist er nicht erreichbar, wird nach **30 Minuten** erneut geprüft, solange das Telefon im Ziel-WLAN ist. In der Zwischenzeit lösen auch neue Fotos keine Prüfung aus.
  - Eine neue WLAN-Verbindung und „Jetzt synchronisieren“ prüfen sofort.
  - Bricht die Verbindung während eines Laufs ab, gilt dieselbe 30-Minuten-Regel.
  - Die Startseite zeigt „PC nicht erreichbar – nächste Prüfung um HH:MM“.
- Verbindungsfehler im Automatikbetrieb erzeugen keine Fehler-Benachrichtigung mehr; die Wiederholung läuft still.

### Geändert
- Die automatische WorkManager-Wiederholung bei Fehlern (2 Minuten, exponentiell) ist durch die 30-Minuten-Regel ersetzt.

## 0.0.3 (2026-11-01)

### Neu
- **Selbst-Update über GitHub Releases:** Die App prüft täglich im WLAN (abschaltbar) und meldet eine neue Version per Benachrichtigung. Ein Tippen lädt sie herunter und startet die Installation. Android fragt einmal nach der Erlaubnis „Apps installieren“ und bei jedem Update nach Bestätigung.
  - Vor der Installation prüft die App die heruntergeladene Datei (gleiche App, höhere Version). Die Signatur prüft Android.
  - Nach dem Update startet die App neu. Lässt Android das nicht zu, kommt die Benachrichtigung „Update installiert – tippen zum Öffnen“.
  - Quelle: [github.com/Apop85/Sync2PC](https://github.com/Apop85/Sync2PC). Das ist voreingestellt und in den Einstellungen unter „Updates“ änderbar.
- **Benachrichtigungs-Symbol** aus dem Hunde-Logo: einfarbige Silhouette mit Augen, Nase und Halsband, statt des generischen Symbols.

### Geändert
- **Ordnerauswahl** über den Android-Systemdialog („Dateien“) statt einer eigenen Liste.

## 0.0.2 (2026-11-01)

### Neu
- **Parallele Übertragungen:** Standard sind 3 gleichzeitige Uploads mit je eigener SMB-Verbindung, einstellbar von 1 bis 4. Das bringt mehr Tempo bei vielen kleinen Dateien.
- **Original-Änderungsdatum:** Die Datei auf dem PC übernimmt das Änderungsdatum vom Telefon (Erstell- und Änderungsdatum) statt des Kopierzeitpunkts.
- **Statistik:** Anzahl Dateien und Speicher pro Jahr und pro Land (aus GPS), Fotos und Videos getrennt gezählt.
- **Lizenz:** MIT.

### Geändert
- Der Release-Build ist mit R8 verkleinert (Minify und Shrink Resources). Die Keep-Regeln für smbj und BouncyCastle stehen in `app/proguard-rules.pro`.
- Gleichzeitiges Anlegen desselben Ordners durch mehrere Verbindungen wird toleriert.

### Build
- Der Builder verwendet nur noch Java 17–24. Neuere Android-Studio-Versionen bringen Java 25 mit, und damit bricht Gradle 8.11 ab (`What went wrong: 25.0.4`). In diesem Fall lädt der Builder einmalig ein portables JDK 21 nach `.build\jdk21`.

## 0.0.1 (2026-11-01)

Erste Version.

### Sync
- Überträgt Fotos und Videos per SMB2/SMB3 auf eine Windows-Freigabe (Benutzer + Passwort).
- Ordnerstruktur `<Jahr>\<Monat>` bzw. `<Jahr>\<Monat> - <Land>`, z. B. `2026\06 - Ägypten`.
- Datum: EXIF (Fotos) bzw. Container-Metadaten (Videos) → Datum im Dateinamen → Änderungsdatum.
- Land offline aus GPS-Koordinaten (Natural Earth). Heimatland (Standard: Schweiz) ohne Länderzusatz.
- Jedes Land bekommt einen eigenen Monatsordner.
- Live-Fotos/Bewegtbilder: Ein Video mit gleichem Namen wie ein Foto landet im selben Ordner.
- Umbenannte Monatsordner werden erkannt (nur die Monatszahl muss bleiben). Bei Mehrdeutigkeit fragt die App einmal pro Gruppe (Monat + Land), nicht pro Datei. „Alle übernehmen“ erledigt alles mit einem Klick.
- Gleicher Name, anderer Inhalt: `bild_2.jpg`, `bild_3.jpg`, …
- Duplikaterkennung: lokale Datenbank plus Abgleich mit dem Share (Grösse, dann Teil-Hash über Anfang, Mitte und Ende).
- Abgebrochene Uploads werden fortgesetzt (Temp-Datei `.s2ptmp`, danach Umbenennung).
- Optionale Prüfung nach dem Upload. Pflicht, wenn Dateien entfernt werden sollen.
- Nach dem Sync: behalten (Standard), Papierkorb oder endgültig löschen. Android 11+ fragt einmal pro Durchgang nach.
- Mindestalter für Dateien (Standard: 5 Minuten) mit automatischem Nachhol-Sync.

### Hintergrund und Akku
- Startet automatisch, wenn ein Heim-WLAN verbunden wird (System-Callback, die App muss nicht laufen).
- Neue Fotos werden gebündelt erkannt und hochgeladen, sobald das Heim-WLAN verbunden ist.
- Sicherheitsnetz: Prüfung alle 6 Stunden (nur WLAN).
- Option „Nur beim Laden“.
- Fortschritt als Benachrichtigung, auch auf dem Sperrbildschirm, mit „Abbrechen“.

### Oberfläche
- Profile: mehrere Ziele mit eigenen Zugangsdaten, Quellordnern, WLANs und Löschoptionen.
- Verbindungstest, Ordnerauswahl mit Dateianzahl, Vorschau (Trockenlauf), Verlauf mit Log.
- Einrichtungshilfe für alle nötigen Berechtigungen.

### Projekt
- One-Click-Builder (`build.bat`): baut nur bei Änderungen, erhöht die Patch-Version automatisch, signiert die APK.
- Unit-Tests für Ordnernamen, Dateinamen-Datum, SMB-Pfade und Teil-Hash.
