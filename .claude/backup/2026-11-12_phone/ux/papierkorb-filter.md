# Designvorschlag: Papierkorb-Filter «Handy / PC / beides» (SYNC-20261112-251)

Stand: 2026-11-12 · App 0.16.35 (Ist-Zustand im Emulator und in `TrashScreen.kt` gelesen) · Status: **Vorschlag, Benutzerfreigabe offen** (README §8)

## 1. Ist-Zustand (beobachtet)
- Papierkorb-Ansicht pro Profil: Karte (Profilname, «Handy: n Dateien, x», «PC: m Dateien, y», Hinweis bei nicht erreichbarem PC, «Papierkorb aufräumen…», «Neu einlesen») · Hinweis · Wertfeld **«Tag»** (Dialog, öffnet Tagesauswahl) · optional «Alles vom … auswählen» · Ordnergruppen mit «Neueste Version je Datei wählen» · je Datei eine Karte, je Version eine Zeile mit Checkbox «geändert … · Handy|PC · Grösse».
- Handy- und PC-Einträge stehen gemischt in einer Liste; die Seite sieht man nur im Kleingedruckten jeder Zeile.
- Auswahlleiste unten («n ausgewählt · Grösse», «Wiederherstellen», «Löschen»). Löschen wirkt nur auf die markierten Einträge. **Wiederherstellen schreibt immer auf Handy und PC zurück** (Dialogtext), unabhängig von der Seite des markierten Eintrags.
- «Papierkorb aufräumen…» hat bereits eine eigene Wahl **«Wo?»** (Handy und PC / Nur Handy / Nur PC, Standard: beides) und wirkt unabhängig von der Liste.
- Bestehendes Muster für Filter: Wertfeld + Auswahldialog (seit 0.6.4 bewusst statt Chip-Reihe, wegen Platz).
- Bestehende Lücke: Die Auswahl bleibt beim Wechsel des Tag-Filters erhalten. Markierte, inzwischen ausgeblendete Einträge werden bei «Löschen»/«Wiederherstellen» trotzdem mitverarbeitet. Das wird mit einem Seitenfilter noch wahrscheinlicher.

## 2. Varianten

### Variante A (Empfehlung): Wertfeld «Anzeigen» wie «Tag»
Ein zweites Wertfeld direkt **über** «Tag» in der Liste (nicht in der Karte):
```text
Anzeigen   [ Handy und PC          v ]
Tag        [ Alle Tage             v ]
```
Tippen öffnet einen Dialog mit drei Radiozeilen (Muster «Wo?» im Aufräumen-Sheet, `ModeRow`): **Handy und PC** · **Nur Handy** · **Nur PC**. Die Beschriftungen gibt es schon (`sides_both`, `sides_phone_only`, `sides_pc_only`).
- Vorteile: gleiches Muster wie «Tag», eine Zeile auch bei Schrift 2,0, keine neue Komponente, Standardzustand = heutiges Verhalten.
- Nachteile: zwei Taps statt einem; Anzahlen stehen nur in der Karte darüber.

### Variante B: Die Zusammenfassungszeilen der Karte werden zu Filter-Chips
«Handy: 2 Dateien» und «PC: 7 Dateien» werden antippbare Chips (beide an = beides, mindestens einer an).
- Vorteile: Anzahl und Filter an einer Stelle, ein Tap.
- Nachteile: schlecht auffindbar (die Zeilen sehen nicht wie Filter aus), bei Schrift 2,0 brechen zwei Chips mit Zahlen um, «beide aus» muss verhindert werden, nicht erreichbarer PC braucht Sonderzustand im Chip. Neue Komponente.

**Empfehlung: A** (konsistent mit «Tag», robust bei Schrift 2,0, geringster Eingriff).

## 3. Verhalten (gilt für A; bei B sinngemäss)
1. **Standard** «Handy und PC». Der Filter gilt nur für die offene Ansicht (bleibt bei Drehen, wird beim Verlassen oder Profilwechsel zurückgesetzt). Kein dauerhaftes Merken: ein vergessener Filter würde einen scheinbar leeren Papierkorb zeigen.
2. **Karte oben** bleibt ungefiltert (Gesamtstand Handy/PC), damit man sieht, was der Filter verbirgt.
3. **Leerzustand** (Filter ergibt nichts), getrennt vom echten leeren Papierkorb:
   - DE «Keine Einträge auf dem Handy.» / «Keine Einträge auf dem PC.» (mit aktivem Tag: «… an diesem Tag.») + Schaltfläche **«Filter zurücksetzen»**
   - EN «No entries on the phone.» / «No entries on the PC.» + «Reset filter»
   - Echt leer bleibt «Der Papierkorb ist leer.»
4. **PC nicht erreichbar:** «Nur PC» bleibt wählbar (nicht ausgegraut, damit der Grund erklärbar ist). Die Liste zeigt statt «keine Einträge» den roten Hinweis der Karte als Leerzustand: DE «PC nicht erreichbar: Einträge aus dem PC-Papierkorb können nicht angezeigt werden.» / EN «PC not reachable: PC trash entries cannot be shown.» Nie behaupten, der PC-Papierkorb sei leer.
5. **Massenauswahl** («Alles vom … auswählen», «Neueste Version je Datei wählen») wirkt wie heute beim Tag-Filter **nur auf sichtbare Einträge**. Die Schaltflächen nennen den Filter, wenn er aktiv ist: «Neueste Version je Datei wählen (Nur Handy)».
6. **Auswahl beim Filterwechsel:** Wechselt Seitenfilter oder Tag, wird die Auswahl aufgehoben, wenn sie ausgeblendete Einträge enthielte; Hinweis einmalig «Auswahl aufgehoben (Filter geändert)». So betrifft «Löschen»/«Wiederherstellen» immer nur, was man sieht. Schliesst die bestehende Lücke beim Tag-Filter.
7. **Auswahlleiste und Dialoge** nennen die tatsächlich betroffenen Seiten aus der Auswahl: «3 ausgewählt (Handy 2, PC 1) · 45 B»; Löschdialog ergänzt «Betrifft: Handy 2, PC 1».
8. **Wiederherstellen:** Bei aktivem Seitenfilter ergänzt der Dialog: «Wiederherstellen schreibt immer auf Handy und PC, auch bei Filter „Nur Handy“.» (EN «Restoring always writes to phone and PC, even with the filter “Phone only”.»). Sonst suggeriert der Filter eine einseitige Wirkung.
9. **«Papierkorb aufräumen…»** bleibt von der Liste unabhängig (eigene Wahl «Wo?», eigene Bestätigung mit Anzahl und Seite). Vorschlag: «Wo?» wird passend zum Seitenfilter **vorausgewählt** (Nur Handy / Nur PC; bei «Handy und PC» wie heute), die Wahl bleibt änderbar und die Bestätigung nennt Seite und Anzahl wie bisher.
10. Schrift 2,0 und Dunkelmodus: Wertfeld und Radiozeilen sind bestehende Komponenten; Zeilen brechen um, keine feste Höhe.

## 4. Texte (neu)
| Schlüssel (Vorschlag) | DE | EN |
|---|---|---|
| `trash_view_label` | Anzeigen | Show |
| (Werte) | Handy und PC · Nur Handy · Nur PC | Phone and PC · Phone only · PC only (bestehende `sides_*`) |
| `trash_filter_empty_phone` | Keine Einträge auf dem Handy. | No entries on the phone. |
| `trash_filter_empty_pc` | Keine Einträge auf dem PC. | No entries on the PC. |
| `trash_filter_pc_unreachable` | PC nicht erreichbar: Einträge aus dem PC-Papierkorb können nicht angezeigt werden. | PC not reachable: PC trash entries cannot be shown. |
| `trash_filter_reset` | Filter zurücksetzen | Reset filter |
| `trash_selection_cleared` | Auswahl aufgehoben (Filter geändert). | Selection cleared (filter changed). |
| `trash_restore_filter_hint` | Wiederherstellen schreibt immer auf Handy und PC, auch bei Filter «%1$s». | Restoring always writes to phone and PC, even with the filter “%1$s”. |
Hinweis: Die EN-Texte des Projekts nutzen bei den Seiten «Phone» (z. B. `side_phone`), die Regel in `CLAUDE.md` nennt «Mobile». Development/Documentation klären, welche Schreibweise gilt; der Vorschlag folgt dem Bestand.

## 5. Akzeptanzkriterien
- Standard ist «Handy und PC»; Verhalten ohne Änderung des Filters identisch zu heute.
- Filter «Nur Handy»/«Nur PC» zeigt ausschliesslich Einträge dieser Seite; Karte oben bleibt ungefiltert.
- Leerer Filter zeigt den passenden Text und «Filter zurücksetzen»; echt leerer Papierkorb zeigt unverändert «Der Papierkorb ist leer.»
- Bei nicht erreichbarem PC und Filter «Nur PC» erscheint der Unerreichbar-Hinweis, nie «leer».
- Filter- oder Tageswechsel hebt eine Auswahl mit ausgeblendeten Einträgen auf und meldet es.
- Auswahlleiste und Löschdialog nennen die betroffenen Seiten; der Wiederherstellen-Dialog nennt bei aktivem Filter, dass beide Seiten betroffen sind.
- «Aufräumen» schlägt «Wo?» passend zum Filter vor, ohne die Bestätigung zu verkürzen.
- Schrift 1,0/1,5/2,0 und Hell/Dunkel: keine abgeschnittenen Texte, keine Leerflächen.

## 6. Risiken
- **Datenverlust:** Löschen auf einer vom Filter verborgenen Auswahl (durch Punkt 6 behoben); falsche Annahme, Wiederherstellen wirke nur auf die gefilterte Seite (Punkt 8); «Aufräumen» mit anderer Seite als erwartet (Punkt 9).
- Umsetzung berührt dieselbe Datei wie SYNC-20261112-249 (Handy-Papierkorb): Dateistand vor Umsetzung prüfen.
- Die Karte oben verhält sich bei Schrift 2,0 derzeit fehlerhaft (siehe SYNC-20261112-274 P3); das sollte vor oder mit dem Filter behoben werden, sonst sitzt das neue Feld unter einer zu grossen Karte.
- Technische Umsetzbarkeit: reiner UI-Filter über `data.entries` (`side`), keine Datenquellenänderung erkennbar; Architecture nur bei Bedarf.

## 7. Ergänzung zu Punkt 4 (Hinweis beim Wiederherstellen), Stand 2026-11-12
Anregung des Benutzers: Der Dialog soll sagen, welche Datei wohin wiederhergestellt wird und ob sie auf der anderen Seite noch vorhanden ist.
Ist-Zustand (`strings_trash.xml`, `trash_restore_text`): «Die aktuellen Versionen kommen zuerst in den Papierkorb. Die gewählten Versionen werden auf Handy und PC zurückgeschrieben.» Der Dialog nennt weder Dateien noch Seiten.

Variante 4A (Empfehlung): **Zusammenfassung mit Zählern**, ergänzt um den Filterhinweis aus Punkt 8
- Titel unverändert («3 Dateien wiederherstellen?»).
- Zeile «Wird zurückgeschrieben auf: Handy (3) und PC (3).»
- Zeile nur wenn zutreffend: «Ersetzt 2 aktuelle Dateien auf dem Handy und 1 auf dem PC. Sie kommen vorher in den Papierkorb.»
- Zeile nur wenn zutreffend: «Noch nicht vorhanden: 1 Datei auf dem Handy.» (Datei fehlt dort aktuell und wird neu angelegt.)
- Bei höchstens drei Dateien stehen die Dateinamen darunter, sonst nur die Zähler.
- Vorteile: kurz, auch bei Schrift 2,0 lesbar, verhindert die falsche Annahme «nur Handy». Nachteil: keine Einzelaufstellung.

Variante 4B: **Einzelaufstellung je Datei** (scrollbare Liste im Dialog), z. B. «b.txt: Handy ersetzt (aktuelle Version geht in den Papierkorb), PC neu angelegt».
- Vorteile: maximal genau. Nachteile: bei vielen Dateien lang, bei Schrift 2,0 sehr gross, mehr Text zum Übersetzen und Prüfen, Datenquelle muss pro Datei den Zustand beider Seiten liefern.

Variante 4C: nur der Filterhinweis aus Punkt 8 (kleinste Änderung, keine Zähler).

Empfehlung: 4A. Die Einschätzung stützt sich auf `RestoreManager.request` und `TrashScreen.kt` (Zeilen 359–377); ob die Anzahl der ersetzten Dateien pro Seite schon vor dem Bestätigen verfügbar ist (`data.existing`), klärt Development bei der Umsetzung, bei Bedarf Architecture. Texte DE/EN folgen nach Freigabe.
