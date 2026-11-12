# Navigation durch LeoSync (UX und QA)

Wegbeschreibungen zu Zielfenstern der App für **blinde Navigation** per adb: Wer der Beschreibung folgt, braucht keine Screenshots, um sich zurechtzufinden. Pflege: UX-Specialist **und** QA & Testing (Pflicht für beide). Hier stehen nur Wegbeschreibungen, keine anderen Informationen.

## Regeln (diesen Kopf einmal je Sitzung lesen)
1. **Nicht vollständig einlesen:** den Abschnitt des Zielfensters per Grep nach seiner Überschrift suchen und nur diesen lesen.
2. **Ausgangslage einmal je Sitzung bestimmen:** Gerät/AVD und Auflösung (`adb shell wm size`) sowie Schriftgrösse (`adb shell settings get system font_scale`). Koordinaten gelten nur für Gerät **und** Schriftgrösse; Hell/Dunkel ändert sie nicht. Schriftgrösse wechseln: `adb shell settings put system font_scale <1.0|1.5|2.0>`; die App startet dabei neu, jeder Weg beginnt danach wieder auf der Startseite.
3. **Blind folgen:** Stehen Koordinaten für das aktuelle Gerät und die aktuelle Schriftgrösse da, die Schritte ohne Screenshot ausführen (`adb shell input tap x y`, Scrollen mit `adb shell input swipe x1 y1 x2 y2 300`).
4. **Ankunft billig prüfen:** nach dem letzten Schritt `uiautomator dump` und nur nach dem **Prüftext** des Zielfensters suchen (z. B. `Select-String`). Ein Screenshot nur, wenn diese Prüfung fehlschlägt oder ohnehin ein visueller Test ansteht.
5. **Fehlt etwas, sofort ergänzen:** Fehlen Koordinaten für die aktuelle Schriftgrösse, den Weg per uiautomator ermitteln (Mittelpunkt der `bounds`) und im selben Abschnitt ergänzen. Stimmt ein Schritt nicht mehr, nur diesen korrigieren und «Stand» aktualisieren. Fehlt ein Zielfenster, einen neuen Abschnitt nach dem Aufbau unten anlegen. Jeder neu ermittelte Weg wird vor Ende des Auftrags eingetragen.
6. Wer gleichzeitig mit einer anderen Rolle arbeitet: vor dem Eintragen den Abschnitt neu lesen und nur den eigenen Schritt ändern.
7. **Systemdialoge und Ordnerauswahlen** (Dateiauswahl, «Ordner wählen», Speichern-Dialog) nur bis zum Einstieg dokumentieren (wie man sie öffnet, woran man sie erkennt), nie deren Inhalt, Ordner oder Dateien.
8. **Scrollen:** «Wischer» = `swipe 30 1900 30 600 300` (am linken Rand, damit keine Schaltfläche getroffen wird); auf der Startseite «Ende» = 2× `swipe 540 1900 540 400 300`. Nach einem Fling schwanken Positionen um bis zu 200 px: bei Werten mit «ca.» die Karte kurz per uiautomator bestätigen.

## Aufbau eines Abschnitts

```text
## <Name des Zielfensters>
Stand: <App-Version> · Gerät: <AVD> (<Breite>x<Höhe>)
1. Start: Startseite
2. <Element> antippen (Scrollposition) – Schrift 1,0: x=…, y=… · 1,5: x=…, y=… · 2,0: x=…, y=…
3. <Element> antippen – Schrift 1,0: x=…, y=… · 1,5: ? · 2,0: ? -> Zielfenster (Prüftext: «<Text, der nur im Zielfenster steht>»)
```
Unbekannte Werte mit «?» kennzeichnen, nie raten. Ältere Abschnitte ohne Gerät oder Schriftgrösse: Koordinaten bei der nächsten Benutzung einmal per uiautomator bestätigen und dann im neuen Aufbau ergänzen.

---

## Kopfzeile der Startseite (Pause, Statistik, Verlauf, Einstellungen)
Stand: 0.16.35 · Gerät: LeoSync_API29 (1080x2280)
1. Start: Startseite (`am start -n com.sync2pc.app/.ui.MainActivity`); die vier Symbole liegen bei jeder Schriftgrösse auf y=171
2. Pause x=607 · Statistik x=739 · Verlauf x=871 · Einstellungen x=1003 (Schrift 1,0 · 1,5 · 2,0 identisch)
3. Prüftexte: Pause «Wie lange pausieren?» · Statistik «Pro Jahr» · Verlauf «Übertragungen» · Einstellungen «Sprache & Sicherung»

## Pause (Startseite)
Stand: 0.16.35 · Gerät: LeoSync_API29 (1080x2280)
1. Start: Startseite -> Pausensymbol oben (x=607 y=171) -> Dialog (Prüftext: «Wie lange pausieren?»)
2. «Abbrechen» – Schrift 1,0: x=530, y=1644 · 1,5: x=381, y=1894 · 2,0: x=705, y=2008
3. Nur 1,0: «1 h» x=236 y=873 · «Für immer» x=420 y=1225 · «Pausieren» x=795 y=1643; 1,5 und 2,0: ? (nicht benötigt, bei Bedarf per uiautomator)
4. Achtung: Bei aktiver Pause öffnet dasselbe Symbol («▶») nicht den Dialog, sondern hebt die Pause auf

## Statistik
Stand: 0.16.35 · Gerät: LeoSync_API29 (1080x2280)
1. Start: Startseite
2. Symbol «Statistik» (x=739 y=171, alle Schriftgrössen) antippen -> Statistik (Prüftext: «Pro Jahr»)

## Verlauf (Protokoll)
Stand: 0.16.35 · Gerät: LeoSync_API29 (1080x2280)
1. Start: Startseite
2. Symbol «Verlauf» (x=871 y=171, alle Schriftgrössen) antippen, oder per Intent `am start -n com.sync2pc.app/.ui.MainActivity --es open log` -> Verlauf (Prüftext: «Übertragungen»)
3. Achtung: das Papierkorbsymbol rechts daneben (x=1003 y=171) öffnet «Gesamten Verlauf löschen?»

## Verlauf löschen (Dialog)
Stand: 0.16.35 · Gerät: LeoSync_API29 (1080x2280)
1. Start: Startseite -> «Verlauf» (x=871 y=171)
2. Papierkorbsymbol oben rechts (x=1003 y=171) -> Dialog (Prüftext: «Gesamten Verlauf löschen?»)
3. «Abbrechen» – Schrift 1,0: x=436, y=1368 · 1,5: x=736, y=1660 · 2,0: x=705, y=1752
4. «Verlauf löschen» (löscht wirklich) – 1,0: x=748, y=1368 · 1,5: ? · 2,0: x=626, y=1572

## Updates
Stand: 0.16.35 · Gerät: LeoSync_API29 (1080x2280)
1. Start: Startseite -> Zahnrad (x=1003 y=171) -> Einstellungsliste (Prüftext: «Sprache & Sicherung»)
2. «Updates» antippen (Schrift 1,0: x=755, y=854 · 1,5: x=592, y=854 · 2,0: x=494, y=854) -> Updates (Prüftext: «Jetzt prüfen»)
3. Alternativ per Intent: `am start -n com.sync2pc.app/.ui.MainActivity --es open update` (prüft beim Öffnen automatisch)

## Papierkorb
Stand: 0.16.35 · Gerät: LeoSync_API29 (1080x2280)
1. Start: Startseite
2. Ganz nach unten (2× `swipe 540 1900 540 400 300`)
3. «Papierkorb» antippen – Schrift 1,0: x=313, y=1844 · 1,5: x=366, y=1696 · 2,0: x=398, y=1512 -> Papierkorb (Prüftext: «Welchen Papierkorb möchtest du öffnen?»)

## Speicher freigeben
Stand: 0.16.35 · Gerät: LeoSync_API29 (1080x2280)
1. Start: Startseite
2. Ganz nach unten (2× `swipe 540 1900 540 400 300`)
3. «Speicher freigeben» antippen – Schrift 1,0: x=379, y=1584 · 1,5: x=472, y=1360 · 2,0: x=528, y=1028 -> Speicher freigeben (Prüftext: «Verschiebt Fotos und Videos»)

## Profil bearbeiten
Stand: 0.16.35 · Gerät: LeoSync_API29 (1080x2280)
1. Start: Startseite
2. Profilkarte antippen, hier «Folder» (Schrift 1,0: ohne Wischen x=498, y=2116 · 1,5: nach 1 kurzem Wisch `swipe 30 1900 30 1000 300` ca. x=498, y=1050 · 2,0: nach 1 kurzem Wisch ca. x=498, y=1660–1890, per uiautomator bestätigen) -> Profil bearbeiten (Prüftext: «Profilname»)
3. Zurück: Pfeil oben links (x=77 y=171, alle Schriftgrössen); ohne Änderungen kommt keine Rückfrage

## Profil bearbeiten: Karten am Formularende (Filter, Schutzbremse, Wartung)
Stand: 0.16.35 · Gerät: LeoSync_API29 (1080x2280)
1. Start: Profil bearbeiten (siehe oben), danach ans Formularende (8× Wischer; Wartung noch zugeklappt)
2. «Filter (optional)» (zugeklappt) – Schrift 1,0: x=507, y=772 · 1,5: x=507, y=340 · 2,0: nach 1 kurzem Rückwisch `swipe 30 700 30 1500 300`: x=507, y=1003
3. «Schutzbremse (optional)» – 1,0: x=507, y=1025 · 1,5: x=507, y=710 · 2,0: x=507, y=308
4. «Wartung» – 1,0: x=507, y=1729 · 1,5: x=507, y=1670 · 2,0: x=507, y=1513 (Prüftext nach Aufklappen: «Profil löschen»)
5. Aufgeklappter Filter (nur 1,0): «Nur diese Dateien» x=314 y=926 · «Diese Dateien ausschliessen» x=425 y=1124 (Prüftext: «Mehrere Muster mit Komma»); 1,5 und 2,0: ?

## Profil bearbeiten: Richtung und Änderungen
Stand: 0.16.35 · Gerät: LeoSync_API29 (1080x2280)
1. Start: Profil bearbeiten, ans Formularende (8× Wischer), dann Rückwisch `swipe 30 700 30 1900 300` – 1,0: 1×, 1,5: 2×, 2,0: 3×
2. Kartentitel «Richtung und Änderungen» – Schrift 1,0: x=352, y=655 · 1,5: x=466, y=1318 · 2,0: x=540, y=1330 (Prüftext: «In welche Richtung wird abgeglichen?»)
3. Radiobutton «Handy → PC» – 1,0: x=350, y=842 · 1,5: x=408, y=1542 · 2,0: x=452, y=1732
4. Radiobutton «Beidseitig» – 1,0: x=322, y=1150 · 1,5: x=368, y=1850 · 2,0: ? (liegt unter dem sichtbaren Bereich, ein weiterer kurzer Wisch nötig)
5. Speichern: Haken oben rechts (x=1003 y=171) nach einer Änderung -> Dialog «Änderung bestätigen» (Koordinaten der Dialogtasten: ?, nicht ausgelöst, um das Profil nicht zu ändern)

## Profil bearbeiten: WLAN-Karte, PC-Kennung, Verschlüsselung, Verbindung testen
Stand: 0.16.35 · Gerät: LeoSync_API29 (1080x2280)
1. Start: Profil bearbeiten (siehe oben); Reihenfolge von oben: Allgemein, Ordner auf dem Handy, Ziel (Freigabe, Benutzer, Passwort, «Verbindung testen»), PC-Kennung, Verschlüsselung (einklappbar), «WLAN (automatische Synchronisierung)» mit rotem Hinweis, Richtung und Änderungen, Filter, Schutzbremse, Wartung
2. «PC-Kennung» (Prüftext «PC-Kennung»): Schrift 1,5 nach Formularende und 3× Rückwisch x=269, y=1280; 1,0 und 2,0: ? (per uiautomator suchen)
3. Wischen am linken Rand (x=30), damit keine Schaltfläche getroffen wird; auf der Startseite löst ein Wisch über «Synchronisieren» sonst den Dialog «Erste Synchronisierung» aus

## Profil löschen (Dialog)
Stand: 0.16.35 · Gerät: LeoSync_API29 (1080x2280)
1. Start: Profil bearbeiten, ans Formularende (8× Wischer), «Wartung» antippen (Koordinaten siehe «Karten am Formularende»)
2. Danach bei 1,0 und 1,5 1×, bei 2,0 3× Wischer; «Profil löschen» antippen – Schrift 1,0: x=359, y=1806 · 1,5: x=426, y=1806 · 2,0: x=466, y=1784 -> Dialog (Prüftext: «Profil löschen?»)
3. «Abbrechen» – 1,0: x=556, y=1298 · 1,5: x=421, y=1440 · 2,0: x=705, y=1636
4. «Löschen» (löscht wirklich; der Dialog nennt den Profilnamen nicht, vorher oben im Formular den Namen prüfen) – 1,0: x=808, y=1298 · 1,5: x=768, y=1440 · 2,0: x=743, y=1456
5. «Abgleichstand zurücksetzen» (daneben in «Wartung», selbe Scrollposition) – 1,0: x=395, y=1542 · 1,5: x=532, y=1432 · 2,0: x=540, y=1189

## Neues Profil (Assistent)
Stand: 0.16.35 · Gerät: LeoSync_API29 (1080x2280)
1. Start: Startseite
2. «Profil hinzufügen» (rechts neben «Profile») – Schrift 1,0: x=858, y=1514 · 1,5: x=776, y=1628 · 2,0: x=726, y=1772 -> Typwahl (Prüftext: «Was möchtest du synchronisieren?»)
3. «Ordner synchronisieren» – 1,0: x=360, y=914 · 1,5: x=476, y=1286 · 2,0: x=540, y=1833 -> Assistent (Prüftext: «Schritt 1 von 6: Profilname»); «Fotos & Videos sichern» liegt darüber (1,0: y=490)
4. Namensfeld antippen (Schritt 1) – 1,0: x=540, y=743 · 1,5: x=540, y=1037 · 2,0: x=540, y=1149; Name mit `adb shell input text <Name>`, danach `input keyevent 111` (Tastatur schliessen); Achtung: das Feld ist mit «Ordner» vorbelegt, getippter Text wird angehängt (Profilname «Ordner<Text>»)
5. «Weiter» (Schritt 1) – 1,0: x=916, y=1084 · 1,5: x=884, y=1468 · 2,0: x=864, y=1740 -> Schritt 2 (Prüftext: «Schritt 2 von 6: Quelle»)
6. «Ordner wählen…» (Schritt 2) – 1,0: x=290, y=876 · 1,5: x=368, y=1138 · 2,0: x=416, y=1376 -> Systemdialog (Prüftext: «Show roots»; weiter nicht beschrieben)
7. Verlassen ohne Speichern: Systemtaste Zurück 1× je Schritt, im Schritt 1 nochmals -> Dialog «Änderungen speichern?» (Prüftext); «Verwerfen» – 1,0: x=540, y=1270 · 1,5: x=540, y=1404 · 2,0: x=540, y=1456 (1,0: «Speichern» x=540 y=1138, «Abbrechen» x=540 y=1402)
8. Hinweis Test: «Zugangsdaten übernehmen…» in Schritt 3 füllt Benutzer, Passwort und Domäne aus einem vorhandenen Profil und den Pfad, wenn er leer ist

## Assistent: Schritt Filter und Schutzbremse
Stand: 0.16.35 · Gerät: LeoSync_API29 (1080x2280)
1. Start: Neues Profil (Assistent) bis Schritt 2; Schritt 2 verlangt einen Ordner (nur Einstieg der Auswahl dokumentiert); die folgenden Schritte nur bei Schrift 1,0 gemessen, 1,5 und 2,0: ? (Grund: erst nach Ordnerauswahl erreichbar)
2. Schritt 3 «Ziel»: «Zugangsdaten übernehmen…» (1,0: x=401, y=864), danach ans Seitenende (1× Wischer): «Weiter» x=916, y=2038 (Prüftext: «Schritt 3 von 6: Ziel»)
3. Schritt 4 (Optionen, Papierkorb-Tage, Prüfintervall): ans Seitenende (4× Wischer), «Weiter» x=916, y=2038 (Prüftext: «Papierkorb leeren nach … Tagen»)
4. Schritt 5 «Filter (optional)»: «Weiter» x=592, y=1404 · «Speichern» x=885, y=1404 (Prüftext: «Schritt 5 von 6: Filter (optional)»)
5. Schritt 6 «Schutzbremse (optional)»: «Speichern» x=885, y=1240 · «Zurück» x=170, y=1240 · Haken oben rechts x=1003, y=171 (Prüftext: «Schritt 6 von 6: Schutzbremse (optional)»)

## Profil testweise anlegen und löschen
Stand: 0.16.35 · Gerät: LeoSync_API29 (1080x2280)
1. Anlegen: siehe «Neues Profil (Assistent)» und «Assistent: Schritt Filter und Schutzbremse»; vorher Sicherung exportieren (Pflicht). Schritt 3: unerreichbarer Host z. B. \\192.168.3.250\Fotos für «PC nicht erreichbar»; Schritt 5 oder 6 «Speichern» -> Dialog «Profil gespeichert» (Koordinaten: ?, nicht ausgelöst, um keine Testprofile zu erzeugen)
2. Löschen: Profilkarte -> ganz nach oben scrollen und Namen prüfen -> ganz nach unten, «Wartung» -> «Profil löschen» -> «Löschen»: Koordinaten siehe «Profil löschen (Dialog)»

## Sicherung exportieren (Dialog)
Stand: 0.16.35 · Gerät: LeoSync_API29 (1080x2280)
1. Start: Startseite -> Zahnrad (x=1003 y=171) -> «Sprache & Sicherung» (Schrift 1,0: x=860, y=722 · 1,5: x=758, y=722 · 2,0: x=697, y=722; Prüftext: «Länderordner»)
2. «Exportieren…» – Schrift 1,0: x=540, y=1910 (ohne Wischer) · 1,5: x=540, y=1840 (nach 1 Wischer) · 2,0: x=540, y=1818 (nach 1 Wischer) -> Dialog (Prüftext: «Sicherung exportieren»)
3. «Datei wählen…» – 1,0: x=758, y=1500 · 1,5: x=688, y=1738 · 2,0: x=646, y=1828 -> Systemdialog zum Speichern (Prüftext: «SAVE»; weiter nicht beschrieben)
4. «Abbrechen» (im Dialog) – 1,0: x=454, y=1500 · 1,5: x=736, y=1904 · 2,0: x=705, y=2008
5. Achtung: Wird im Dialog ein Export-Passwort getippt, kommt ein Wiederholungsfeld hinzu und die Schaltflächen rutschen nach unten; die obigen Werte gelten dann nicht

## Sicherung importieren (Dialog)
Stand: 0.16.35 · Gerät: LeoSync_API29 (1080x2280)
1. Start: Startseite -> Zahnrad -> «Sprache & Sicherung» (siehe Export)
2. «Importieren…» – Schrift 1,0: x=540, y=2064 (ohne Wischer) · 1,5: x=540, y=1994 (nach 1 Wischer) · 2,0: x=540, y=1986 (nach 1 Wischer) -> Systemdialog zur Dateiauswahl (Prüftext: «Show roots», Symbol links oben x=99 y=171); weiter nicht beschrieben
3. Nach der Dateiwahl erscheint der App-Dialog «Sicherung importieren» (Prüftext: «Hinzufügen»); «Abbrechen» – 1,0: x=540, y=1394 · 1,5: ? · 2,0: ? (nur nach Dateiwahl erreichbar, bewusst nicht dokumentiert); «Ersetzen» löscht alle Profile

## Konflikte (Konfliktkarten, Dialog «Alle: neuere»)
Stand: 0.16.37 · Gerät: LeoSync_API29 (1080x2280)
1. Konflikt herstellen: Datei in Download/Testsync (Handy) und X:\testsync (PC) nach dem ersten Abgleich auf beiden Seiten mit unterschiedlichem Inhalt ändern; Handy-Datei danach `am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Download/Testsync/<Datei>`; gleicher Zeitstempel = «(gleich alt)»
2. Startseite: «Synchronisieren» (unter dem Statuskasten) – Schrift 1,0: x=540, y=1304 · 1,5: x=540, y=1570 · 2,0: x=540, y=2048 -> Karte «Konflikte bei der Ordner-Synchronisierung» -> «Entscheiden» -> Liste «Konflikte»
3. «Entscheiden» (Karte oben auf der Startseite, ohne Wischen) – Schrift 1,0: x=258, y=656 · 1,5: x=318, y=932 · 2,0: x=354, y=1160 -> Liste (Prüftext: «Konflikte»)
4. «Alle: neuere» (Schaltfläche unter dem Hinweis, nicht mehr in der Kopfzeile) – 1,0: x=540, y=612 · 1,5: x=540, y=853 · 2,0: x=540, y=1278 -> Dialog (Prüftext: «Fortfahren?»); «Abbrechen» im Dialog – 1,0: x=540, y=1380 · 1,5: ? · 2,0: x=540, y=1583
5. Auflösen ohne Entscheidung (Testzustand beenden): Testdatei auf beiden Seiten löschen und «Synchronisieren» antippen

## Dunkelmodus im Emulator umschalten
Stand: 0.16.35 · Gerät: LeoSync_API29 (1080x2280)
1. `adb shell cmd uimode night yes` wirkt hier nicht (System meldet «NightModeLocked», Stand bleibt «no»).
2. Stattdessen: `adb shell am start -a android.settings.DISPLAY_SETTINGS` (Prüftext: «Dark theme») -> Schalter «Dark theme» antippen – Schrift 1,0: x=972, y=657 · 1,5: x=972, y=755 · 2,0: x=972, y=852 -> `adb shell cmd uimode night` zur Kontrolle.

## Papierkorb: Filter «Anzeigen» und «Tag»
Stand: 0.16.38 · Gerät: LeoSync_API29 (1080x2280)
1. Start: Startseite -> ganz nach unten (2× `swipe 540 1900 540 400 300`; mit offenem Konflikt 5×) -> «Papierkorb» (siehe Abschnitt «Papierkorb») -> Profil «Folder» antippen (x=154 y=457 bei 1,0; 1,5: x=180 y=494; 2,0: x=200 y=614)
2. Feld «Anzeigen» (Wert «Handy und PC», unter dem Hinweis, ohne Wischen) – Schrift 1,0: x=228, y=1190 · 1,5: x=295, y=1489 · 2,0: x=343, y=1894 -> Dialog (Prüftext: «Anzeigen» mit den Zeilen «Handy und PC», «Nur Handy», «Nur PC»)
3. Dialogzeilen (Schrift 1,0): «Handy und PC» x=423 y=950 · «Nur Handy» x=392 y=1082 · «Nur PC» x=361 y=1214 · «Abbrechen» x=790 y=1412; 1,5 und 2,0: ? (per uiautomator, Zeilen untereinander)
4. Feld «Tag» (Wert «Alle Tage») – 1,0: x=178, y=1384 · 1,5 und 2,0: ? (je nach Wisch) -> Dialog «Tag wählen»; Leerzustand: «Nur PC» mit Tag «11/10/2026» zeigt «Keine Einträge auf dem PC an diesem Tag.» und «Filter zurücksetzen» (1,0: x=540 y=1634 · 1,5: x=540 y=1746 · 2,0: x=540 y=1562)
5. Auswahl: Checkbox einer Version antippen -> unten erscheint die Leiste (Prüftext: «ausgewählt»); «Wiederherstellen» (1,0: x=540 y=1906) und «Endgültig löschen» (1,0: x=540 y=2054) öffnen Dialoge, «Abbrechen» im Dialog 1,0: Wiederherstellen x=416 y=1638, Löschen x=396 y=1380
6. «Papierkorb aufräumen…» (Karte oben, ohne Wischen) – 1,0: x=540 y=678 · 1,5: x=540 y=764 · 2,0: x=540 y=886 -> Sheet (Prüftext: «Wo?»)
7. Ergänzt 0.16.39 (Gerät LeoSync_API29, 1080x2280; Zeilen können je nach Profilzahl wandern, Ankunft per uiautomator bestätigen): bei mehreren Ordnerprofilen zeigt «Papierkorb» zuerst «Welchen Papierkorb möchtest du öffnen?» (Profilzeile 1,0: x=154 y=457 für das erste Profil, 1,5: x=180 y=494, 2,0: x=300 y=614); ganz nach unten wischen: 1,0 und 1,5 je 3×, 2,0 bis 6× `swipe 540 1900 540 400 300`, «Papierkorb»-Karte 1,5: x=295 y=1696, 2,0: x=310 y=1512 (mit vier Profilen). Profilwechsel im Papierkorb: Auswahlfeld oben rechts (1,5: x=790 y=150; Einträge 1,5: erstes Profil y=325, zweites y=457). «Anzeigen»-Dialog 1,5: «Handy und PC» y=957, «Nur Handy» y=1090, «Nur PC» y=1222 (x≈450), «Abbrechen» x=790 y=1420; 2,0: «Nur PC» x=422 y=1230. Auswahlleiste unten: «Wiederherstellen» 1,0: x=540 y=1927, «Endgültig löschen» 1,0: x=540 y=2065 (sie öffnen Dialoge; bei 2,0 scrollt der Wiederherstellen-Dialog, Prüftext «Wird zurückgeschrieben auf» bzw. «Will be written back to»). Zurück-Taste im Papierkorb geht zur Startseite, ein zweites Zurück verlässt die App.
