# Build-Anleitung – LeoSync

## Kurzfassung

1. Einmalig **Android Studio** installieren und einmal starten. Damit sind JDK 17 und das Android SDK installiert.
2. **`build.bat`** doppelklicken.
3. Die fertige APK liegt unter `releases\LeoSync-<version>.apk`.

## Voraussetzungen

| Komponente | Version | Hinweis |
|---|---|---|
| JDK | 17 bis 25 | Java 8 reicht **nicht**. Seit 0.1.3 (Gradle 9.8, AGP 9.5) funktioniert auch das Java 25 von Android Studio. Nur bei neueren Versionen lädt der Builder ein portables JDK 21 nach `.build\jdk21`. |
| Android SDK | Platform 35, Build-Tools | Wird von Android Studio bzw. Gradle installiert. |
| Internet | beim ersten Build | Gradle, Abhängigkeiten, Wrapper, Länderdaten |

Der Builder sucht ein JDK 17–25 in dieser Reihenfolge: `JAVA_HOME`, `C:\Program Files\Android\Android Studio\jbr`, installierte JDKs unter `Program Files`, `.build\jdk21`. Findet er keines, lädt er Eclipse Temurin 21 herunter (einmalig, ca. 190 MB).

**Projekt in Android Studio öffnen:** Das mitgelieferte JDK (JBR, Java 25) funktioniert seit 0.1.3. Android Studio muss AGP 9.5 unterstützen; sonst meldet der Gradle-Sync eine zu neue Plugin-Version. Dann Android Studio aktualisieren.

**Werkzeug-Versionen (seit 0.1.3):** Gradle 9.8.2, Android Gradle Plugin 9.5.0 (Kotlin eingebaut, „built-in Kotlin“), Kotlin 2.4.21, KSP 2.3.13, Room 2.8.5.
Das SDK sucht er unter `ANDROID_HOME`, `ANDROID_SDK_ROOT` und `%LOCALAPPDATA%\Android\Sdk`.

## Was `build.bat` macht

1. Prüft JDK und SDK und schreibt `local.properties`.
2. Lädt `gradle\wrapper\gradle-wrapper.jar`, falls es fehlt.
3. Lädt die Länderdaten (`tools\fetch-geodata.ps1`), falls sie fehlen.
4. Erzeugt die App-Icons und das einfarbige Benachrichtigungs-Symbol aus `.claude\icon.png` neu, wenn sich das Bild geändert hat (`tools\make-icons.ps1`). Eine grosse Vorschau des Benachrichtigungs-Symbols liegt dann unter `.build\ic_notification_preview.png`.
5. Zerlegt `.claude\gif_frames.png` in die Animationsbilder für die Benachrichtigung, wenn sich die Vorlage geändert hat (`tools\make-anim.ps1`).
6. Berechnet einen Hash über alle Quelldateien. **Ohne Änderung wird nicht gebaut.**
7. Existiert die APK der aktuellen Version schon, wird `VERSION_PATCH` in `version.properties` um 1 erhöht und in `PATCHNOTES.md` ein Platzhalter-Eintrag angelegt.
8. Beim ersten Release-Build wird ein Signatur-Schlüssel erstellt (`keystore\`, `keystore.properties`).
9. Führt die Unit-Tests aus und baut die APK (`gradlew testDebugUnitTest assembleRelease`).
10. Kopiert die APK nach `releases\`.

Die vollständige Gradle-Ausgabe jedes Builds steht zusätzlich in `.build\last-build.log`. Bei Fehlern genügt es, dem Agent zu sagen, dass der Build fehlgeschlagen ist; er liest das Protokoll selbst.

### Optionen

```bash
build.bat -Force
```
Baut immer, auch ohne Änderungen.

```bash
build.bat -SkipTests
```
Überspringt die Unit-Tests.

```bash
build.bat -Debug
```
Baut eine Debug-APK (`com.sync2pc.app.debug`, parallel installierbar).

## Signatur-Schlüssel sichern

`keystore\sync2pc-release.jks` und `keystore.properties` sind **nicht** in Git (Geheimnisse).
Geht der Schlüssel verloren, kann die installierte App nicht mehr aktualisiert werden. Sie muss dann deinstalliert werden, und ihre Daten gehen verloren.
**Beide Dateien sicher aufbewahren.**

## Versionierung

- `version.properties` enthält `MAJOR.MINOR.PATCH`.
- `versionCode = MAJOR*1000000 + MINOR*1000 + PATCH`
- Bis 0.0.13: Phase `0.0.x`.
- Seit 0.1.0: **Beta**. `0.1.x` enthält nur Fehlerbehebungen (PATCH +1 pro Änderung). Kommt eine neue Funktion dazu, folgt `0.2.0`, dann `0.3.0` usw.
- `1.0.0` gibt nur der Projektinhaber frei.

## Release-Checkliste (Beta)

Vor jedem GitHub-Release:

1. `build.bat` läuft ohne Fehler durch (Unit-Tests und R8 inklusive).
2. Falls sich die Datenbank geändert hat: Es gibt eine neue Datei `app\schemas\…\<Version>.json` und eine passende Migration in `AppDatabase`. Die Schema-Datei wird committet.
3. Update über die bestehende Version installieren, **nicht** neu installieren. Die App startet, Profile und Verlauf sind noch da (prüft die Migration).
4. Bildordner-Profil: *Simulieren* und *Synchronisieren* klappen, neue Fotos landen im richtigen Monats- bzw. Länderordner, und es entstehen keine Duplikate.
5. Ordner-Profil: Je eine Datei auf dem Handy und auf dem PC ändern → beide werden abgeglichen.
6. WLAN während eines Syncs trennen → „Pausiert“ erscheint. WLAN wieder verbinden → der Sync setzt fort.
7. *Einstellungen → Diagnose → Diagnosedaten kopieren* und *Verlauf → Teilen* funktionieren.
8. *Einstellungen → Diagnose → Sicherung*: Export mit und ohne Export-Passwort. Import mit „Hinzufügen“ ergibt doppelte Profile, die danach gelöscht werden.
9. *Speicher freigeben* mit grosser Monatszahl (z. B. 120) und kleiner Auswahl: Die Prüfung auf dem PC läuft, der Android-Dialog erscheint, und im Papierkorb der Galerie sind die Dateien wiederherstellbar.
10. Ordner-Sync (siehe „Test Ordner-Sync“ und „Test Schutzbremse“ unten), wenn sich daran etwas geändert hat.
11. `PATCHNOTES.md` ist ergänzt. Release `v<version>` mit der APK anlegen (siehe unten).
12. Nach dem Release: Das Selbst-Update auf einem Gerät mit der Vorversion prüfen.

### Test Ordner-Sync

Vorbereitung auf dem PC: In der Freigabe einen Testordner anlegen, z. B. `\\192.168.3.3\Fotos\_Test-Ordnersync`, und darin:
- einige hundert kleine Dateien (z. B. Textdateien oder kleine PDFs, gern in Unterordnern)
- 2–3 grosse Dateien (je einige hundert MB, z. B. ein Video)

Auf dem Handy:
1. Neues Profil → **Ordner synchronisieren**. Gleiche Freigabe und Zugangsdaten wie beim Fotos-Profil (über die Chips übernehmen), als Unterordner `_Test-Ordnersync` wählen.
2. Handy-Ordner: einen **neuen, leeren** Ordner wählen (z. B. `Documents/LeoSync-Test`), Richtung **Beidseitig**.
3. Nach dem Speichern im Dialog *Simulieren* wählen (oder später im Profil, Abschnitt „Simulation“) → alle Dateien erscheinen als „↓ PC → Handy“. Bis zum ersten manuellen Sync startet das Profil nicht automatisch.
4. *Synchronisieren*: Im Verlauf steht „Übertrage … mit 3 parallelen Verbindungen“. Die kleinen Dateien kommen zuerst, die grossen am Schluss.
5. **Fortsetzen testen:** Während eine grosse Datei lädt, das WLAN kurz trennen und wieder verbinden. Im Verlauf erscheint danach „↓ … (fortgesetzt ab … MB)“.
6. **Rückweg:** Auf dem Handy eine Datei ändern oder neu anlegen → nächster Sync lädt sie hoch („↑ …“).
7. Es gibt auf dem Handy keine doppelten Ordner wie „Ordner (1)“.
8. Zum Schluss das Test-Profil löschen. Die Dateien auf PC und Handy bleiben erhalten und können von Hand entfernt werden.

### Test Schutzbremse (seit 0.4.1)

Mit dem Test-Profil von oben, **Löschungen übernehmen** eingeschaltet, nach einem erfolgreichen Sync:
1. **PC-Ordner umbenannt:** `_Test-Ordnersync` auf dem PC umbenennen → *Synchronisieren*. Auf der Startseite erscheint „PC-Ordner nicht gefunden“ mit dem stehenden Hund, auf dem Handy wird **nichts** gelöscht, auf dem PC kein leerer Ordner angelegt. Ordner zurückbenennen → *Erneut prüfen* → der Sync läuft normal.
2. **Neuer Ordner gewählt:** Im Profil einen anderen, ungleichen PC-Unterordner wählen und speichern → „Ist das wirklich der richtige Ordner?“. *Abbrechen* lässt alles, wie es war.
3. **Viele Löschungen:** Auf dem PC mehr als 20 % der Testdateien löschen → *Synchronisieren* → „Viele Dateien würden gelöscht“. *Nicht löschen* → die Dateien kommen wieder auf den PC. Nochmals löschen, *Trotzdem in den Papierkorb* → sie landen auf dem Handy in `.LeoSync-Papierkorb`.
4. Im Verlauf steht bei den angehaltenen Läufen „Wartet auf dich“. Andere Profile laufen in der Zeit normal weiter.
5. **Ganzer Unterordner (seit 0.4.2):** Auf dem PC einen Unterordner mit mehr als 5 Dateien löschen (aber weniger als 20 % aller Dateien) → „Viele Dateien würden gelöscht“ nennt den Ordner.
6. **Grenze im Profil:** „Schutzbremse (optional)“ z. B. auf 50 % stellen → 30 % Löschungen laufen ohne Rückfrage durch.
7. **Update 0.4.1 → 0.4.2** über die bestehende App installieren (DB-Version 3 → 4): Profile, Verlauf und Sync-Stand sind noch da, die Schutzbremse steht auf 20 %.

### Test Versionsschutz und Wiederherstellen (seit 0.5.0)

1. **Update 0.4.2 → 0.5.0** über die bestehende App (DB-Version 4 → 5): Profile und Verlauf sind noch da, im Ordner-Profil ist „Alte Versionen im Papierkorb behalten“ eingeschaltet.
2. Im Test-Ordner auf dem PC eine Textdatei ändern → *Synchronisieren*. Auf dem Handy liegt die alte Version in `.LeoSync-Papierkorb/<heute>/…`. Umgekehrt (auf dem Handy ändern) landet die alte Version im PC-Papierkorb.
3. Startseite → Tools → **Papierkorb** (seit 0.6.3). Bei mehreren Ordner-Profilen zuerst das Profil wählen. Die Datei erscheint mit „ältere Version“, eine gelöschte mit „gelöscht“.
4. Eine Version wählen → in der Leiste unten *Wiederherstellen* → der Sync läuft. Danach ist die alte Version auf Handy **und** PC, die bisherige liegt im Papierkorb.
5. Feld **„Tag“** antippen → Liste aller Tage mit Anzahl und Grösse → einen Tag wählen. „Alles vom … auswählen“ und „Ganzer Ordner“ ausprobieren: pro Datei wird nur eine Version gewählt. „Alle Tage“ zeigt wieder alles.
6. **PC aus:** PC ausschalten, eine Wiederherstellung auslösen → Startseite zeigt „Wiederherstellung vorgemerkt“. PC einschalten → nächster Sync führt sie aus.

### Test Papierkorb aufräumen (seit 0.6.0, seit 0.6.3 im Tool „Papierkorb“)

1. Startseite → Tools → **Papierkorb** → **Aufräumen…** (Fenster von unten). Die Kachel zeigt nach dem ersten Öffnen die belegte Grösse.
2. „Älter als 0 Tage“, „Nur Handy“ → Anzahl und Grösse stimmen mit dem Handy-Papierkorb überein; nach dem Löschen ist `.LeoSync-Papierkorb` auf dem Handy leer, auf dem PC unverändert.
3. „Einzelne Tage“ → Feld „Tage“ antippen → einen Tag ankreuzen → *Übernehmen*, „Handy und PC“ → nur dieser Tagesordner verschwindet auf beiden Seiten.
4. **PC aus:** „Alles“, „Nur PC“ → Startseite zeigt „Papierkorb leeren vorgemerkt“. PC an → nächster Sync leert ihn (Verlauf: „PC-Papierkorb geleert (vorgemerkt)“).
5. Im **Papierkorb** eine Version markieren → in der Leiste unten *Endgültig löschen* → sie ist weg.
6. Mehrere Ordner-Profile: oben rechts über den Profilnamen wechseln → die Liste lädt das andere Profil.
7. Ordner-Profil mit „Alte Versionen behalten“ **aus**: eine Datei auf dem PC löschen (mit „Löschungen übernehmen“) → sie erscheint trotzdem im Papierkorb und lässt sich wiederherstellen.
8. Nur Fotos-Profile (oder gar keine): Kachel „Papierkorb“ ist ausgegraut; Tippen zeigt „Den Papierkorb gibt es nur bei Ordner-Profilen.“ Ohne Fotos-Profil ist „Speicher freigeben“ ausgegraut.
9. **Hochformat (seit 0.6.4):** Automatisches Drehen einschalten, Handy quer halten → LeoSync bleibt im Hochformat.

### Test Speicher freigeben für Ordner und Papierkorb-Fixes (seit 0.7.0)

1. **Kachel:** Nur ein Ordner-Profil mit „Löschungen übernehmen“ an (oder Richtung beidseitig) → „Speicher freigeben“ ist ausgegraut, Tippen zeigt den Hinweis. Ordner-Profil Handy → PC ohne „Löschungen übernehmen“ → Kachel aktiv.
2. **Auswahl:** Profil öffnen → „Älter als“ auf „Alter egal“, „Ordner“ auf einen Unterordner → „Passend: N Dateien“ stimmt. „Weitere Kriterien…“ → Dateitypen, Grösse ab, Ausnehmen (`*.pdf`) ausprobieren; zuklappen → geänderte Zeilen bleiben sichtbar.
3. **Vorschau:** einzelne Dateien und einen ganzen Ordner abwählen → die Zahl sinkt.
4. **Löschen:** *Auf dem PC prüfen und löschen* → Rückfrage → Dateien sind auf dem Handy weg, auf dem PC noch da. Danach *Synchronisieren*: nichts wird gelöscht oder zurückgeholt (Verlauf).
5. **Sicherheit:** Eine Datei auf dem PC vorher ändern → sie bleibt auf dem Handy („… Dateien bleiben“). Danach im Profil „Löschungen übernehmen“ einschalten und synchronisieren → die freigegebenen Dateien bleiben auf dem PC.
6. **Fotos:** Dieselbe Ansicht mit Fotos-Profil; Favoriten behalten; Galerie-Papierkorb wie bisher.
7. **Sperre:** Grossen Sync starten, währenddessen Papierkorb → Aufräumen… → „Alles“ → Anzeige „Wartet, bis der laufende Sync fertig ist…“, danach wird geleert.
8. **Vorgemerkte Wiederherstellung geschützt:** PC aus, eine PC-Version wiederherstellen (vorgemerkt) – oder eine Handy-Version, dann sofort Aufräumen… „Alles“ → Rückfrage nennt „1 Datei(en) … bleiben“, der Eintrag ist als „zum Wiederherstellen vorgemerkt“ markiert. PC an → Wiederherstellung klappt.
9. **Namen:** Dieselbe Datei am selben Tag zweimal ersetzen → im Papierkorb `Name (HH-mm-ss).ext`; in der App erscheinen beide als Versionen derselben Datei.
10. **Schutzbremse:** Bei vorgemerkter Wiederherstellung die Schutzbremse auslösen (z. B. PC-Ordner umbenennen) → die Karte sagt „Wartet auf deine Entscheidung (Schutzbremse)“.

### Test Profil-Assistent (seit 0.5.0)

1. *Profil hinzufügen* → Typ wählen → Schritt 1–4 erscheinen einzeln mit Fortschrittsbalken. „Weiter“ ohne Pflichtangabe (Name, Ordner, Freigabe-Pfad) zeigt einen roten Hinweis.
2. Ab „Filter“ gibt es „Speichern“ direkt; die Zurück-Geste geht einen Schritt zurück.
3. Nach dem Speichern erscheint „Wie weiter?“. *Profil bearbeiten* zeigt alle Karten auf einer Seite in derselben Reihenfolge.

## Manuell bauen (ohne Builder)

```bash
gradlew.bat testDebugUnitTest assembleRelease
```

Alternativ das Projekt in Android Studio öffnen: *File → Open → Projektordner*.

## Release auf GitHub veröffentlichen (für Selbst-Updates)

Die App holt Updates aus dem **neuesten Release** des eingestellten GitHub-Repositorys.

1. Das Repository ist in `version.properties` eingetragen (`UPDATE_REPO=Apop85/Sync2PC`). Seit 0.4.0 ist es fest in die App eingebaut und dort nicht mehr änderbar. Nach einer Änderung in `version.properties` mit `build.bat -Force` bauen, weil die Datei nicht zum Änderungs-Hash zählt.
2. Version bauen (`build.bat`).
3. Auf GitHub ein Release mit dem Tag `v<version>` anlegen (z. B. `v0.0.3`) und die Datei `releases\LeoSync-<version>.apk` anhängen. Der Dateiname muss auf `.apk` enden und darf nicht „debug“ enthalten. Markiere es nicht als „Pre-release“, auch nicht in der Beta: Die App fragt nur `releases/latest` ab, und dort erscheinen Pre-releases nicht.
4. Der Release-Text erscheint in der App als Änderungsbeschreibung. Am einfachsten kopierst du den Abschnitt aus `PATCHNOTES.md`.

Mit der GitHub-CLI geht es in einem Schritt:

```bash
gh release create v0.1.0 releases\LeoSync-0.1.0.apk --title "LeoSync 0.1.0" --notes "Siehe PATCHNOTES.md"
```

**Wichtig:** Alle Versionen müssen mit demselben Schlüssel (`keystore\`) signiert sein, sonst lehnt Android das Update ab.

## Installation auf dem Telefon

- APK aufs Telefon kopieren und öffnen („Unbekannte Apps installieren“ für den Dateimanager erlauben), oder
- per USB-Debugging:

```bash
adb install -r releases\LeoSync-0.1.0.apk
```

## Fehlerbehebung

| Problem | Lösung |
|---|---|
| `What went wrong: 2x.0.x` (Java-Version) | Gradle läuft mit einem zu neuen Java. Mit `build.bat` bauen (wählt das JDK selbst) bzw. in Android Studio das Gradle JDK auf 17–25 stellen |
| `The 'org.jetbrains.kotlin.android' plugin is no longer required` | Seit AGP 9 ist Kotlin eingebaut. Das Plugin darf in keiner `build.gradle.kts` mehr stehen |
| „Portables JDK konnte nicht eingerichtet werden“ | JDK 21 manuell installieren (z. B. Eclipse Temurin) und `JAVA_HOME` setzen |
| „Android SDK nicht gefunden“ | Android Studio einmal starten, SDK-Setup abschliessen |
| SDK-Lizenzen nicht akzeptiert | `"%LOCALAPPDATA%\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat" --licenses` |
| R8: „Missing class …“ | Inhalt von `app\build\outputs\mapping\release\missing_rules.txt` in `app\proguard-rules.pro` übernehmen (oder dem Agent schicken) |
| Release-APK stürzt ab, Debug nicht | Wahrscheinlich fehlt eine R8-Keep-Regel. Logcat-Ausgabe dem Agent schicken. |
| Länderdaten-Download schlägt fehl | Manuell `ne_50m_admin_0_countries.geojson` von Natural Earth nach `app\src\main\assets\geo\countries.geojson` kopieren |
