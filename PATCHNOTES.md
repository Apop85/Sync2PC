# Patchnotes – LeoSync

Versionierung: Beta `0.1.x` (nur Fehlerbehebungen). Neue Funktionen eröffnen `0.2.x`, `0.3.x` usw. bis zur Freigabe von `1.0.0`. Bis 0.0.13: `0.0.x`. Neueste Version oben.

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
