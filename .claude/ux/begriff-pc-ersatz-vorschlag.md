# Begriffsvorschlag: «PC» ersetzen (PC und NAS) – SYNC-20261111-236

Stand: 2026-11-11, UX-Specialist. Status: **Vorschlag, wartet auf Freigabe des Benutzers** (wesentliche Designänderung, README §8). Keine Änderung an Code, Ressourcen oder Doku.

## Ausgangslage
- Der Begriff «PC» steckt in ca. 160 DE-Texten (plus EN, Hilfe, README). Er bezeichnet **die Gegenseite der Synchronisierung**: das Gerät mit der Freigabe, auf das das Handy zugreift (Windows-PC oder NAS).
- Die Seite wird in sieben Mustern genannt: Richtung («Handy → PC»), Zustand («PC nicht erreichbar», «PC erkannt»), Ort («auf dem PC», «PC-Ordner», «PC-Papierkorb»), Zusammensetzung («PC-Kennung», «PC-Erkennung», «PC-Name»), Vertrauen («anderer PC», «Neuem PC vertrauen»), Zähler («Handy: 3 Dateien, PC: 5 Dateien») und Zeitform («Synchronisierung mit erreichbarem PC»).
- Bereits vorhanden: «Server» (Textstelle `profile_host_change_body`, Icon `icon_server`), «Dein PC oder NAS» (`enc_test_warn`, `enc_test_required_fail`). Die App benutzt die Begriffe also schon uneinheitlich.
- Feste Begriffe bleiben: «Handy», «Freigabe» (EN «Mobile», «Share»), «Synchronisierung», nie Whitelist/Blacklist. Technische Bezeichner und `com.sync2pc` bleiben unverändert.

## Die drei Varianten

| | A «Server» (Empfehlung) | B «Zielgerät» | C «PC/NAS» |
|---|---|---|---|
| DE | der Server | das Zielgerät | der PC/NAS (nur als Etikett) |
| EN | server | target device | PC/NAS |
| Kurzform Karte | «Server erkannt» | «Zielgerät erkannt» | «PC/NAS erkannt» |
| Länge | 6 Zeichen | 9 Zeichen | 6 Zeichen |
| Genus | maskulin wie «PC»: alle Artikel und Endungen bleiben gleich | **neutral**: «ein anderes», «dem», jede Endung ändert sich | unklar (NAS ist m./n./f.), Sätze müssen umgangen werden |
| Passt für PC **und** NAS | ja, bei Windows-PC mit Erklärung | ja | ja, wörtlich |
| Passt zur Richtung | ja («Handy → Server», «Server → Handy») | **nein**: «Zielgerät → Handy» ist falsch, wenn das Gerät die Quelle ist | ja |
| Technische Hürde | mittel («Server» kennen die meisten, ein PC ist für viele aber kein Server) | niedrig | niedrig |
| Kollision | «Server» steht schon an einer Stelle und als Icon-Name, nur vereinheitlichen | «Ziel/Zielordner» ist schon belegt («Zielordner», «Ziel» im Assistenten) | keine |
| Umsetzungsrisiko | gering (fast 1:1 ersetzen) | hoch (Grammatik in ca. 160 Stellen) | mittel (klobig in langen Sätzen) |

### Kurzbewertung
- **A «Server»:** kurz, kippt die Grammatik nicht, passt für Richtung und Zustand und deckt NAS und PC ab. Schwäche: Wer sein Windows-Notebook als Gegenstelle benutzt, denkt nicht «Server». Gegenmittel: beim ersten Auftreten die Erklärung «Server (dein PC oder NAS)» (Assistent Schritt 3, Typwahl, Hilfe), danach nur «Server».
- **B «Zielgerät»:** verständlich, aber falsch, sobald die Daten vom Gerät zum Handy fliessen («Zielgerät → Handy»), und der Genuswechsel birgt viele Fehler.
- **C «PC/NAS»:** wörtlich und für alle klar, aber in Fliesstext schwerfällig («Der PC/NAS ist nicht erreichbar»). Gut als **Erklärung** an wenigen Stellen, nicht als durchgängiger Begriff.

## Beispieltexte der wichtigsten Stellen
(DE aktuell → A / B / C)

| Stelle | aktuell | A «Server» | B «Zielgerät» | C «PC/NAS» |
|---|---|---|---|---|
| Richtung | Handy → PC | Handy → Server | Handy → Zielgerät | Handy → PC/NAS |
| Richtung (zurück) | PC → Handy | Server → Handy | **Zielgerät → Handy** (falsch) | PC/NAS → Handy |
| nicht erreichbar | PC nicht erreichbar (%1$s) | Server nicht erreichbar (%1$s) | Zielgerät nicht erreichbar (%1$s) | PC/NAS nicht erreichbar (%1$s) |
| Hinweis dazu | Ist der PC eingeschaltet und die Freigabe aktiv? | Ist der Server eingeschaltet und die Freigabe aktiv? | Ist das Zielgerät eingeschaltet und die Freigabe aktiv? | Ist der PC/NAS eingeschaltet und die Freigabe aktiv? |
| Karte | PC erkannt | Server erkannt | Zielgerät erkannt | PC/NAS erkannt |
| Kennung | PC-Kennung / Nur PC-Kennung | Server-Kennung / Nur Server-Kennung | Gerätekennung / Nur Gerätekennung | PC/NAS-Kennung (zu lang, besser «Gerätekennung») |
| Fremdes Gerät | Unter %1$s antwortet ein anderer PC als bisher. | Unter %1$s antwortet ein anderer Server als bisher. | Unter %1$s antwortet ein anderes Zielgerät als bisher. | Unter %1$s antwortet ein anderer PC/NAS als bisher. |
| Vertrauen | Neuem PC vertrauen | Neuem Server vertrauen | Neuem Zielgerät vertrauen | Neuem PC/NAS vertrauen |
| Löschen verschoben | PC nicht erreichbar – nichts gelöscht. Die Dateien bleiben vorgemerkt. | Server nicht erreichbar – nichts gelöscht. Die Dateien bleiben vorgemerkt. | Zielgerät nicht erreichbar – nichts gelöscht. … | PC/NAS nicht erreichbar – nichts gelöscht. … |
| Papierkorb | PC-Papierkorb geleert | Server-Papierkorb geleert | Papierkorb des Zielgeräts geleert | Papierkorb auf PC/NAS geleert |
| Zähler | Handy: 3 Dateien, PC: 5 Dateien | Handy: 3 Dateien, Server: 5 Dateien | Handy: 3 Dateien, Zielgerät: 5 Dateien | Handy: 3 Dateien, PC/NAS: 5 Dateien |
| Assistent Schritt 3 | Wohin auf dem PC? Gib die Windows-Freigabe an … | Wohin auf dem Server (PC oder NAS)? Gib die Freigabe an … | Wohin auf dem Zielgerät (PC oder NAS)? … | Wohin auf dem PC/NAS? … |
| Verschlüsselung | Dein PC oder NAS kann nicht verschlüsseln … | Dein Server kann nicht verschlüsseln … | Dein Zielgerät kann nicht verschlüsseln … | Dein PC/NAS kann nicht verschlüsseln … |

EN (A): «Phone → Server», «Server not reachable», «Server ID», «A different server answers at %1$s than before», «Trust the new server», «Server recycle bin emptied». EN (B): «target device». EN (C): «PC/NAS».

## Kritische Meldungen (Punkt c)
- Meldungen zu **Nicht erreichbar**, **Löschen verschoben**, **Fremdes Gerät**, **Wiederherstellen** bleiben bei A sinngleich verständlich: es ändert sich nur das Nomen, nicht die Satzstruktur und nicht die Handlungsanweisung.
- Die **Vertrauensfrage** («Anderem Server vertrauen?») ist sicherheitskritisch: Der Text «Nur fortfahren, wenn du den Server neu installiert oder ersetzt hast» ist für ein NAS gut, für einen PC weniger. Empfehlung: dort einmalig «Server (PC oder NAS)» schreiben.
- Die Regel «keine Änderung am Verhalten» gilt: Es wird kein Text inhaltlich umgeschrieben, nur der Begriff getauscht; Fehlertexte mit `Eigenschaften → Freigabe` (Windows-spezifisch) bleiben.

## Empfehlung
**Variante A «Server»**, mit dem Doppelnamen «Server (PC oder NAS)» an **vier Erklärstellen**: Typwahl-Texte, Assistent Schritt 3, Vertrauensfrage, Hilfe/README. Überall sonst nur «Server».

Unterbegriffe: «Serverkennung» wird zu «Server-Kennung» (wie bisher mit Bindestrich: «PC-Kennung»); «PC-Name» in `profile_domain` → «Servername»; «Synchronisierung mit erreichbarem PC» → «… mit erreichbarem Server»; `sides_pc_only` → «Nur Server»; Icon `icon_pc` (💻) auf den Server-Icon (🖧, `icon_server`) umstellen, damit das Symbol zum Wort passt (Prüfung durch Development, wo `💻` benutzt wird).

## Umsetzung (nach Freigabe)
1. Reihenfolge: **nach** den Aufträgen -233, -234, -235 (gleiche Textdateien).
2. Development, in dieser Reihenfolge: (a) `strings_engine.xml` und `strings_home.xml` (Fehler- und Zustandsmeldungen), (b) `strings_profile.xml` (Kennung, Vertrauen), (c) `strings_free.xml`, `strings_trash.xml`, `strings_screens.xml`, `strings_mirror.xml`, `strings_notif.xml`, `strings_settings.xml`, `strings_data.xml`, `strings_log_backup.xml`; je DE und EN, danach `tools/make-translations.ps1`; Icon `icon_pc` anpassen. Aufteilung in drei Aufträge möglich (Fehlermeldungen zuerst).
3. QA: Texte und Meldungen (Zeichenlängen), UX-Review bei Schrift 2,0 (neue Wortlängen: «Server erkannt» auf Karten, «Handy → Server» in Chips, «Server-Papierkorb» in Dialogen).
4. Documentation: Hilfe, README, PATCHNOTES, `DECISIONS.md` §10 (Begriff, Glossar), Webseite (Ablageort dem Benutzer unbekannt, nicht ändern ohne Auftrag).
5. Nicht Teil: Pakete, Variablen, Dateiformate, Datenfelder, Tests.

## Risiken
- Wer ein Windows-Notebook als Gegenstelle benutzt, kennt es nicht als «Server» (Gegenmittel: Doppelname an vier Stellen).
- Textlänge bei Schrift 2,0 (Server ist 4 Zeichen länger als PC): Karten und Dialoge neu prüfen.
- Einzelne Stellen mit «PC» als Windows-Begriff (z. B. «Windows-Benutzer», «Eigenschaften → Freigabe») bleiben absichtlich.
- Vollständigkeit: mechanisches Ersetzen ist nicht sicher; jede Stelle muss gelesen werden (Beispiel: «Server» bedeutet in `profile_host_change_body` bisher die Adresse).

## Offene Entscheidungen für den Benutzer
1. Variante A, B oder C (oder eigener Begriff).
2. Soll der Doppelname «(PC oder NAS)» an den vier Erklärstellen stehen?
3. Icon `icon_pc` auf das Server-Icon umstellen?
