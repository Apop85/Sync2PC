# Navigation durch LeoSync (UX)

Wegbeschreibungen zu Zielfenstern der App. **Nicht vollständig einlesen:** gezielt nach der Überschrift des Zielfensters suchen und nur diesen Abschnitt lesen. Wenn eine Wegbeschreibung noch nicht vorhanden ist, merke dir den Weg und erfasse ihn in diesem Dokument. Wenn du einee Wegbeschreibung findest, welche keine Koordinaten aufweist, erstelle den Eintrag neu. Wenn du einen Eintrag mit allen Voraussetzungen findest, folge Blind der Anleitung und den Koordinaten. Wenn du am Schluss nicht am richtigen Ort gelandet bist, erstelle den Teil der Anleitung neu. Es dürfen keine anderen Informationen hier abgelegt werden ausser Wegbeschreibungen.

Pflege: UX-Specialist.

## Emulator-Hinweise (gelten für alle Prüfungen)
- **Hell-/Dunkelmodus wechseln:** nur über die Android-Einstellungen, nicht per `adb shell cmd uimode` (wirkt nicht): `adb shell am start -a android.settings.DISPLAY_SETTINGS` -> «Dark theme» (Schalter rechts, ca. x=972 y=657) antippen, 5 Sekunden warten, danach mit `adb shell cmd uimode night` prüfen («no» = hell, «yes» = dunkel). Die App folgt dem Systemmodus. Nach der Prüfung wieder auf den Ausgangszustand (dunkel) zurückstellen.
- Die App erzwingt Hochformat; Querformat ist nicht zu prüfen.
- Zahlenfelder zuverlässig leeren: Feld antippen, `KEYCODE_MOVE_END`, dann mehrmals `KEYCODE_DEL` mit kurzer Pause (je 250 ms), neuen Wert mit `input text` eintippen, Tastatur mit `keyevent 4` schliessen.
- Systemdialoge (Dateiauswahl) öffnen im zuletzt benutzten Ordner; Koordinaten vorher per `uiautomator dump` ermitteln, nicht blind tippen (ein Fehlgriff führte einmal in die Messages-App).

## Aufbau eines Abschnitts

```text
## <Name des Zielfensters>
Stand: <App-Version>
1. Start: Startseite
2. <Schritt>: Scrollposition, x-/y-Koordinaten für den nächsten Klick
3. <Schritt>: Scrollposition, x-/y-Koordinaten für den nächsten Klick -> Zielfenster
```r

---

## Profil bearbeiten
Stand: 0.14.6
1. Start: Startseite
2. Profilkarte in der Liste «Profile» antippen -> Profil bearbeiten (Zurück/Abbrechen -> Startseite; bei Änderungen Rückfrage)

## Neues Profil (Assistent)
Stand: 0.14.6
1. Start: Startseite
2. «Profil hinzufügen» (rechts neben «Profile») antippen
3. Typ wählen (Fotos & Videos sichern / Ordner synchronisieren) -> Assistent «Schritt 1 von 6»
4. Hinweis Test: «Zugangsdaten übernehmen…» in Schritt 3 füllt Benutzer, Passwort und Pfad aus einem vorhandenen Profil; Verlassen ohne Speichern: Zurück, dann «Verwerfen»

## Profil löschen (Dialog)
Stand: 0.14.6
1. Start: Startseite -> Profilkarte antippen (Profil bearbeiten)
2. Ganz nach unten scrollen, Karte «Wartung» antippen (aufklappen)
3. «Profil löschen» -> Dialog «Profil löschen?» (nur zum Ansehen: Abbrechen)

## Assistent: Schritt Filter und Schutzbremse
Stand: 0.14.7
1. Start: Startseite -> «Profil hinzufügen» -> Typ wählen -> Assistent «Schritt 1 von 6»
2. Schritte mit «Weiter» durchgehen (1 Name, 2 Quelle, 3 Ziel, 4 Synchronisierungsoptionen mit Papierkorb-Tagen und Prüfintervall); ab Schritt 5 gibt es zusätzlich «Speichern»
3. Schritt 5 = Filter (optional), Schritt 6 = Schutzbremse (optional)
4. Hinweis: Im Systemdialog «Ordner wählen» über «Show roots» -> «Downloads» -> «Testsync» wählen (liefert `raw:`-Kennung); Unterordner liefern `msd:`-Kennung

## Profil bearbeiten: Filter, Richtung und Löschungen
Stand: 0.14.7
1. Start: Startseite -> Profilkarte antippen (Profil bearbeiten)
2. Richtung und Löschungen: nach unten scrollen bis zur Karte «Richtung und Änderungen» (Radiobuttons, Schalter); Änderung + oben rechts Haken («Speichern») -> Dialog «Änderung bestätigen» (V1)
3. Filter: weiter nach unten, Karte «Filter (optional)» antippen (aufklappen) -> «Nur diese Dateien» / «Diese Dateien ausschliessen»
4. Schriftgrösse: `adb shell settings put system font_scale 2.0` (Formular schliesst dabei, ungespeicherte Änderungen gehen verloren)

## Papierkorb
Stand: 0.14.7
1. Start: Startseite
2. Einmal nach oben wischen (swipe 540,1900 -> 540,600)
3. «Papierkorb» antippen (ca. x=313 y=1943, dunkel/Schrift 1,0; bei anderer Schriftgrösse per uiautomator suchen) -> Papierkorb

## Speicher freigeben
Stand: 0.14.7
1. Start: Startseite
2. Einmal nach oben wischen (swipe 540,1900 -> 540,600)
3. «Speicher freigeben» antippen (ca. x=379 y=1683, dunkel/Schrift 1,0) -> Speicher freigeben

## Verlauf (Protokoll)
Stand: 0.14.7
1. Start: Startseite
2. Symbol «Verlauf» oben rechts antippen (x=871 y=171), oder per Intent: `am start -n com.sync2pc.app/.ui.MainActivity --es open log` -> Verlauf. Achtung: das Papierkorb-Symbol rechts daneben (x=1003 y=171) löscht ohne Rückfrage.

## Statistik
Stand: 0.14.7
1. Start: Startseite
2. Symbol «Statistik» oben (x=739 y=171) antippen -> Statistik

## Updates
Stand: 0.14.7
1. Start: Startseite
2. Per Intent: `am start -n com.sync2pc.app/.ui.MainActivity --es open update` -> Updates (prüft beim Öffnen automatisch)
## Sicherung importieren (Dialog)
Stand: 0.16.3
1. Start: Startseite -> Zahnrad oben rechts (x=1003 y=171) -> «Sprache & Sicherung» (y~721)
2. «Importieren…» (x~643 y~1910) -> Systemdialog: Burger links oben (x=99 y=171, «Show roots») -> «Downloads» -> Datei «LeoSync-Sicherung_…json» wählen -> Dialog «Sicherung importieren» (nur ansehen: «Abbrechen»; «Ersetzen» löscht alle Profile)

## Konflikte (Konfliktkarten, Dialog «Alle: neuere»)
Stand: 0.16.3
1. Konflikt herstellen: Datei in Download/Testsync (Handy) und X:\testsync (PC) nach dem ersten Abgleich auf beiden Seiten mit unterschiedlichem Inhalt ändern; Handy-Datei danach `am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Download/Testsync/<Datei>`; gleicher Zeitstempel = «(gleich alt)»
2. Startseite: «Synchronisieren» antippen (Karte «Folder») -> Karte «Konflikte bei der Ordner-Synchronisierung» -> «Entscheiden» -> Liste «Konflikte»; «Alle: neuere» oben rechts (x~840 y=171) -> Dialog

## Pause (Startseite)
Stand: 0.16.3
1. Start: Startseite -> Pausensymbol oben (x=606 y=171) -> Dialog «Wie lange pausieren?» (1 h x=236 y=873, «Für immer» x=420 y=1225, «Pausieren» x=795 y=1643)
2. Achtung: Bei aktiver Pause öffnet dasselbe Symbol («▶») nicht den Dialog, sondern hebt die Pause auf

## Profil testweise anlegen und löschen
Stand: 0.16.3
1. Start: Startseite -> «Profil hinzufügen» -> «Ordner synchronisieren» (x=540 y=1030) -> Schritt 1 Name; Schritt 2 «Ordner wählen…» (Systemdialog: Ordner antippen, «Diesen Ordner verwenden» x=540 y=2082, «ALLOW» x=870 y=1309); Schritt 3 Pfad/Benutzer/Passwort von Hand (unerreichbarer Host z. B. \\192.168.3.250\Fotos für «PC nicht erreichbar»); Schritt 4 Richtung wählen, «Weiter»; Schritt 5 «Speichern» -> Dialog «Profil gespeichert»
2. Löschen: Profilkarte -> ganz nach oben scrollen und Namen prüfen -> ganz nach unten, «Wartung» -> «Profil löschen» -> «Löschen» (Dialog nennt den Profilnamen nicht)