# Tests, Build und Qualitätssicherung – LeoSync (Sync2PC)

Stand: Grundstand Version 0.11.0, fortlaufend nachgeführt bis 0.15.5 (DB-Version 10). Grundlage: `build.gradle.kts`, `app/build.gradle.kts`, `gradle/wrapper/gradle-wrapper.properties`, `build.md` und alle Dateien unter `app/src/test` und `app/src/androidTest` (inhaltlich gelesen).

Diese Datei beschreibt **nur** Tests, Build und Qualitätssicherung. Fachliche Abläufe stehen in `docs/SYNC-ENGINE.md`, `docs/DATA-MODEL.md`, `docs/ARCHITECTURE.md`, Entscheidungen in `docs/DECISIONS.md`.
Kennzeichnung: **[vorhanden]** = im Repository nachweisbar, **[fehlt]** = nachweislich nicht vorhanden, **[manuell]** = nur per Checkliste prüfbar, **[Empfehlung]** = nicht umgesetzt.

---

## 1. Überblick

| Testart | Status | Framework | Ort | Läuft auf |
|---|---|---|---|---|
| JVM-Unit-Tests (reine Logik) | vorhanden: 45 Klassen mit `@Test`, 442 `@Test` laut Quelltext (Stand 0.15.5, Documentation 2026-11-11 per Zählung; neu seit 0.15.1 u. a. `TransferResumeTest` mit 8 Tests, SYNC-20261111-166/-170). Development meldet für 0.15.5 442 Tests; nicht von QA oder Documentation ausgeführt. **Letzter von QA ausgeführter Lauf:** 2026-11-11 01:07, JDK 17, Version 0.15.2, `gradlew.bat :app:testDebugUnitTest --rerun --no-build-cache`: 40 Klassen, 377 Tests, 0 Fehlschläge, 0 Fehler, 0 übersprungen (QA, SYNC-20261111-164). Frühere Stände: 0.15.0 373 Tests (Angabe Development, SYNC-20261111-142 bis -146, -152); 0.14.7 287 Tests (Gesamtlauf Development SYNC-20261110-119, 2026-11-10, JDK 17); QA -088 2026-11-09 nach 0.14.3: 32 Klassen, 272 Tests grün; 0.12.1: 25 Klassen, 143 Tests grün | JUnit 4.13.2, `org.json:json:20240303` | `app/src/test/java/com/sync2pc/app/` | PC (JVM), kein Gerät |
| Instrumentierte Tests | vorhanden: 3 Klassen, 18 `@Test` laut Quelltext (`MigrationTest` 6, `MsgInstrumentedTest` 6, `PcGuidDaoTest` 6; Documentation 2026-11-11 per Zählung). Letzter dokumentierter Lauf: QA 2026-11-09 auf Emulator `LeoSync_API29(AVD)` (Android 10), damals nur `MigrationTest` mit 3 Tests: 3/3 bestanden. QA 2026-11-09 (Stand 0.13.2, SYNC-20261109-053): `connectedDebugAndroidTest` 16/16 grün (`MigrationTest` 4, `MsgInstrumentedTest` 6, `PcGuidDaoTest` 6). **Nicht ausgeführt:** der aktuelle Stand mit 18 Tests, insbesondere `MigrationTest.folderLayoutFromVersion8` (8→9 am Emulator mit echten Daten geprüft, QA -088) `MigrationTest.smbEncryptionFromVersion9` (Migration 9→10, 0.15.0; laut Development nicht ausgeführt); `connectedDebugAndroidTest` wurde für 0.15.x insgesamt nicht ausgeführt (der QA-Lauf 2026-11-11 umfasste nur `testDebugUnitTest`) | AndroidJUnit4, `room-testing` (`MigrationTestHelper`) | `app/src/androidTest/java/com/sync2pc/app/` | Gerät/Emulator |
| Textparität DE/EN | vorhanden (als Unit-Test) | JUnit 4 | `StringsParityTest.kt` | PC (JVM) |
| Manuelle Release-Checkliste | vorhanden, dokumentiert | – | `build.md` (Abschnitte „Release-Checkliste“, „Test …“) | echtes Handy + PC/NAS |
| UI-/Compose-Tests | **[fehlt]** | – | – | – |
| SMB-/Netzwerk-Tests (echt oder Fake) | automatisiert **[fehlt]**; lokale SMB-Testfreigabe für Emulator-Prüfungen **[vorhanden]** (`tools\qa-smb-testshare.ps1`, siehe §14.3) | – | `tools/` | Emulator + PC |
| Engine-Tests (`SyncEngine`, `MirrorEngine`, Worker) | **[fehlt]** | – | – | – |
| CI/CD (z. B. `.github/`) | **[fehlt]**, kein Workflow vorhanden | – | – | – |
| Statische Analyse (detekt, ktlint, eigene Lint-Konfiguration) | **[fehlt]**, nichts konfiguriert | – | – | – |

Mocking-Bibliotheken (Mockito, MockK, Robolectric, Turbine …) sind **nicht** eingebunden. Fakes gibt es nur für Verzeichnis- und Dateiabfragen (`RemoteDirs`/`RemoteFiles` in `TargetResolverTest`, `RemoteFinderTest`) und für Verbindung und PC-Kennung (`PcTransport`/`PcLink` in `PcLoginTest`); es gibt keinen Fake für Schreib- und Übertragungsoperationen (Upload, Download, Löschen, Umbenennen). Die vorhandenen Tests prüfen ausschliesslich Klassen, die ohne Android-Abhängigkeit und ohne Netzwerk auskommen (im Quellcode als „rein“ gebaut, siehe `docs/ARCHITECTURE.md` 4.7).

### Qualitätssicherungsmechanismen im Überblick

| Mechanismus | Wirkung | Quelle |
|---|---|---|
| Unit-Tests beim Standard-Build | `build.bat` ruft `gradlew testDebugUnitTest assembleRelease` auf; ein Testfehler bricht den Build ab (ausser `-SkipTests`) | `build.md` |
| Kompilierung mit R8 im Release | `isMinifyEnabled`/`isShrinkResources` = true; deckt fehlende Keep-Regeln erst zur Laufzeit auf, nicht im Test | `app/build.gradle.kts` |
| Room-Schema-Export | `exportSchema = true`, Schemas `1.json`–`10.json` in `app/schemas/…/`; Grundlage für `MigrationTest` | `AppDatabase.kt`, `app/build.gradle.kts` |
| Strings-Parität | schlägt fehl, wenn DE/EN-Schlüssel, Platzhalter oder Plural-Formen abweichen | `StringsParityTest.kt` |
| Schutzbremse etc. zur Laufzeit | Fachliche Sicherungen der App (Löschschutz, Prüfstufen), keine Testmechanismen; Logik teilweise per Unit-Test abgesichert | `MirrorGuardTest`, `VerifyLevelTest` |
| Manuelle Release-Checkliste | Deckt Engine, SMB, Hintergrund, Migration auf echtem Gerät ab | `build.md` |

---

## 2. Build-System

| Komponente | Wert | Quelle |
|---|---|---|
| Gradle | 9.8.2 (Wrapper) | `gradle/wrapper/gradle-wrapper.properties` |
| Android Gradle Plugin | 9.5.0 (Kotlin eingebaut, kein `kotlin-android`-Plugin) | `build.gradle.kts` |
| Kotlin | 2.4.21 (`kotlin-gradle-plugin`, Compose-Compiler-Plugin gleich) | `build.gradle.kts` |
| KSP | 2.3.13 (Room-Compiler) | `build.gradle.kts` |
| Room | 2.8.5 | `app/build.gradle.kts` |
| Compose BOM | 2024.12.01 | `app/build.gradle.kts` |
| WorkManager | 2.10.0 | `app/build.gradle.kts` |
| SMB | `com.hierynomus:smbj:0.13.0`, `com.rapid7.client:dcerpc:0.12.13` | `app/build.gradle.kts` |
| SDK | compileSdk 36, targetSdk 36, minSdk 29 | `app/build.gradle.kts` |
| Java | Source/Target 17; Build läuft mit JDK 17–25 | `app/build.gradle.kts`, `build.md` |
| Modul | nur `:app` (Einzelmodul) | `settings.gradle.kts` |
| Flavors | **keine** | `app/build.gradle.kts` |
| Gradle-Optionen | `org.gradle.parallel=true`, `org.gradle.caching=true`, 3 GB Heap | `gradle.properties` |

**Build-Typen**

- `debug`: `applicationIdSuffix = ".debug"`, `versionNameSuffix = "-debug"` (parallel zur Release-App installierbar).
- `release`: R8 + Ressourcen-Shrinking, `proguard-rules.pro`; signiert nur, wenn `keystore.properties` existiert (wird vom Builder erzeugt, nicht in Git).
- Version aus `version.properties` (`versionCode = MAJOR*1_000_000 + MINOR*1_000 + PATCH`).

**Besonderheiten für Tests**

- `ksp { arg("room.schemaLocation", "$projectDir/schemas") }`: Schemas werden beim Build nach `app/schemas` geschrieben.
- `kspReleaseKotlin` läuft nach `kspDebugKotlin` (verhindert Schema-Race, siehe Kommentar in `app/build.gradle.kts`).
- `androidTest`-Assets enthalten `app/schemas` (für `MigrationTestHelper`).
- `TestTexts` (Unit-Test-Hilfsklasse) sucht `src/main/res` bzw. `app/src/main/res` relativ zum Arbeitsverzeichnis. Gradle startet Unit-Tests im Modulordner `app/`; ein Aufruf aus einer IDE mit anderem Arbeitsverzeichnis kann daran scheitern.

### Befehle

Alle Befehle aus dem Projektwurzelordner (Windows: `gradlew.bat`). Die Tasks sind Standard-AGP-Tasks; die Projektkonfiguration definiert **keine** eigenen Test- oder Prüf-Tasks. Der Hinweis „nicht ausgeführt“ gilt für diese Dokumentation: Befehle wurden hier aus der Konfiguration abgeleitet, nicht gestartet. **Berechtigungen:** Debug-/Test-Builds, Tests, Lint und Emulator-Installationen darf QA & Testing selbstständig ausführen; Release-Builds und `build.bat` nur der Inhaber; andere Rollen bauen nicht (`CLAUDE.md` §1).

| Befehl | Zweck | Umfang | Wann |
|---|---|---|---|
| `build.bat` | Standard-Build des Projekts: Hash-Prüfung, Versionserhöhung, Unit-Tests, Release-APK nach `releases\` | Debug-Unit-Tests + `assembleRelease` | **Nur Projektinhaber** (Release; ändert `version.properties`). Auch QA verwendet es nicht |
| `build.bat -SkipTests` / `-Force` / `-Debug` / `-NoPause` | Tests überspringen / immer bauen / Debug-APK / ohne Tastendruck am Ende | wie oben | Nur Inhaber, nach Absprache |
| `gradlew.bat testDebugUnitTest` | Alle JVM-Unit-Tests (Debug) | 45 Testklassen, kein Gerät; Gradle braucht JDK 17+ (die Shell-Standard-`JAVA_HOME` zeigt auf Java 8; z. B. `C:\Program Files\Android\Android Studio\jbr` verwenden) | Nach jeder Änderung an Logikklassen, Texten (`strings_*.xml`) oder Datenmodell-Klassen |
| `gradlew.bat testDebugUnitTest --tests "com.sync2pc.app.MirrorGuardTest"` | Eine Testklasse | 1 Klasse | gezielte Prüfung nach kleinem Eingriff |
| `gradlew.bat testDebugUnitTest --tests "com.sync2pc.app.MirrorGuardTest.massDeleteAbove20Percent"` | Eine Testmethode | 1 Test | Fehlersuche |
| `gradlew.bat testReleaseUnitTest` | Unit-Tests gegen die Release-Variante | wie oben | selten; nur bei Verdacht auf Varianten-Unterschiede |
| `gradlew.bat assembleDebug` | Debug-APK (kompiliert alles inkl. Compose/KSP), Ausgabe unter `app\build\outputs\apk\debug\` | kein Test | **QA darf selbstständig bauen** (`CLAUDE.md` §1); schneller Kompilier-Check, Emulator-Installation |
| `gradlew.bat assembleRelease` | Release-APK mit R8 | kein Test | **nur Inhaber**; nach Änderungen an Abhängigkeiten, Reflection, Room, smbj/dcerpc (R8-Keep-Regeln) |
| `gradlew.bat testDebugUnitTest assembleRelease` | Befehl, den `build.bat` ausführt; auch „Manuell bauen“ in `build.md` | Tests + Release | Vollprüfung vor Release |
| `gradlew.bat connectedDebugAndroidTest` | Instrumentierte Tests (`MigrationTest`, `MsgInstrumentedTest`, `PcGuidDaoTest`) | 18 `@Test` laut Quelltext, braucht Gerät/Emulator (API ≥ 29); QA darf ausführen | Bei jeder Änderung an `Entities.kt`, `AppDatabase.kt`, Schemas |
| `gradlew.bat lintDebug` | Android-Lint (AGP-Standardtask) | keine projektspezifische Lint-Konfiguration vorhanden | optional; QA darf ausführen; Ergebnis bisher nicht dokumentiert |

Reports: JUnit-HTML unter `app/build/reports/tests/testDebugUnitTest/`; Build-Log von `build.bat` unter `.build/last-build.log`.

---

## 3. Teststruktur

```
app/
  schemas/com.sync2pc.app.data.AppDatabase/1.json … 10.json   (Room-Schemas, Testgrundlage)
  src/
    test/java/com/sync2pc/app/          JVM-Unit-Tests (45 Klassen mit `@Test` laut Quelltext, zuzüglich Hilfsklasse TestTexts)
    androidTest/java/com/sync2pc/app/   MigrationTest.kt, MsgInstrumentedTest.kt, PcGuidDaoTest.kt
```

Es gibt keine `testFixtures`, keine Testressourcen-Ordner (`src/test/resources`), keine gemeinsamen Testdatendateien. Testdaten werden in den Tests selbst aufgebaut (Hilfsfunktionen wie `st()`, `e()`, `p()`, `item()`). Einzige Hilfsklasse: `TestTexts.kt` (lädt `strings*.xml` aus `src/main/res` und installiert einen deutschen `Texts.Resolver`, damit Texte in JVM-Tests aufgelöst werden; ohne Android-Context).

Testarten und Zuständigkeiten:

```mermaid
flowchart LR
  A[Logik-Klassen ohne Android] -->|JUnit, JVM| B[testDebugUnitTest]
  C[Room-Migrationen] -->|AndroidJUnit4| D[connectedDebugAndroidTest]
  E[SyncEngine / MirrorEngine / SMB / Worker / UI] -->|nur manuell| F[build.md Checkliste]
```

---

## 4. Vorhandene Tests im Detail

Pfade relativ zu `app/src/test/java/com/sync2pc/app/` (Ausnahme: `MigrationTest`). Zahl = `@Test`-Methoden.

### 4.1 Synchronisations-nahe Logik (Ordner-Sync, Entscheidungshilfen)

| Datei | Getestet | Geprüfte Szenarien (aus dem Testinhalt) | Tests |
|---|---|---|---|
| `MirrorGuardTest.kt` | `mirror/GuardPolicy`, `mirror/MirrorGuard` (Schutzbremse und Inhaltsvergleich) | Massenlöschung > 20 % (genau 20 % noch erlaubt); kleine Ordner ≤ 5 Dateien nur bei „alles weg“; ab 100 Löschungen immer Rückfrage; Prozentgrenze pro Profil, 100 % = nur feste Regeln; verschwundener Unterordner; Stichprobe begrenzt; oberste Verzeichnisse aus Schlüsseln; Abgleich Soll/Ist (alles da, fehlende Datei/Ordner = Abweichung, einzelne Grössenabweichungen toleriert, Grösse der geprüften Seite); gründlicher Vergleich (kleine Dateien komplett gehasht, grosse per Stichprobe, Änderung in der Mitte erkannt) | 14 |
| `TransferPlanTest.kt` | `mirror/TransferPlan` | kleine Dateien zuerst; Fortsetzen einer abgebrochenen Übertragung, wenn PC-Datei unverändert; Neustart, wenn PC-Datei nach Abbruch geändert, angefangene Datei leer/zu gross oder Zeitstempel unbekannt | 3 |
| `TransferResumeTest.kt` | `mirror/TransferResume` (Entscheidung vor einer Übertragung, auch bei Wiederholung nach Verbindungsabbruch; SYNC-20261111-166, seit 0.15.5) | erster Versuch wie bisher (mit Versionsschutz erst Papierkorb, dann übertragen; ohne Versionsschutz oder ohne Zieldatei direkt übertragen; geändertes Ziel wird übersprungen); Wiederholung nach eigenem Papierkorb-Verschieben setzt fort statt zu überspringen und verschiebt nie erneut; neu entstandene Zieldatei wird nie überschrieben; Invariante: nie «überspringen» bei fehlendem Ziel nach eigenem Papierkorb. Nur die Entscheidungsfunktion, nicht die Verdrahtung in `MirrorEngine` | 8 (laut Quelltext; Development meldet grün, nicht von QA oder Documentation ausgeführt) |
| `VerifyLevelTest.kt` | `VerifyLevel`/`Profile.verify()`, `PartialHash`, `BackupCodec` (Stufen) | Stufen Wenig/Mittel erzeugen dieselbe Prüfsumme wie früher; „Aus“ liest nichts; „Viel“ komplett bis 256 MB, dann Stichprobe; „Komplett“ deckt jedes Byte lückenlos ab; „Aus“ wird angehoben, wenn Dateien entfernt werden; unbekannter Code → Standard; Sicherung behält Stufe und trimmt Namen; alte Sicherung ohne Stufe nutzt `verifyUpload` | 7 |
| `FileFilterTest.kt` | `mirror/FileFilter` | leerer Filter akzeptiert alles ausser Temp-Dateien; Positivliste; Negativliste gewinnt; Namensmuster gilt in Unterordnern; seit 0.14.7 meldet `unbalancedPattern` Muster mit nicht geschlossener Klammer | 5 (laut Quelltext, vorher 4) |
| `TrashIndexTest.kt` | `mirror/TrashIndex` (Papierkorb-Index, Wiederherstellen) | Eintrag aus Papierkorb-Pfad (Datumsordner nötig); Versionsnamen mit Uhrzeit, Rückführung auf Originalnamen, Altformat `_2`, nummerierte Namen nur bei vorhandenem Basisnamen; neueste Version pro Datei; alles eines Tages; ganzer Ordner inkl. Unterordner; nur eine Version pro Datei auswählbar; Datums-Sortierung | 8 |
| `TrashPolicyTest.kt` | `mirror/TrashPolicy` | Ablauf alter Datumsordner (Stichtag selbst bleibt), fremde Ordnernamen laufen nie ab, Stichtagsberechnung | 3 |
| `TrashPurgeTest.kt` | `mirror/TrashPurge` | Regeln (nie fremde Ordner löschen), gleicher Stichtag wie Aufbewahrung, Kodierung/Dekodierung, Betroffenheit nach Seite, reservierte Einträge bleiben erhalten | 5 |
| `TrashPurgeConfirmedTest.kt` | `mirror/TrashPurge.planConfirmed` (bestätigter Stand beim Aufräumen, DEC-144) | ganzer Tag; neuere Version bleibt; geänderte Grösse; vorgemerkte bleiben; verschwundene Einträge/Tage unabhängig; `dateDir`; seit 0.11.10 | 6 |
| `InterventionRegistryTest.kt` | `mirror/InterventionRegistry` (Zustandslogik der Eingriffe beim laufenden Sync) | Warten/Laufen/Fertig; ein Eingriff je Profil; Abbrechen nur wartend; Neustart nach Abbrechen; Zustellung bzw. nicht zustellbares Ergebnis; Fehlertext; Zähler; Stand nach Development -036 | 8 |
| `FreeRulesTest.kt` | `sync/FreeRules` (Speicher freigeben) | Alter, Ordner inkl. Unterordner, Typen/Endungen, Grösse und Ausnahmen, Auswahl behält ungesicherte Favoriten und ganze Gruppen | 5 |
| `FolderNamesTest.kt` | `sync/FolderNames`, `sync/PartialHash` | Monat aus umbenannten Ordnern; Standardnamen; erzeugte Namen sind keine Kandidaten; Konfliktnamen ab „2“; gleicher Basisname; Bereinigung; Teilhash-Bereiche und Abhängigkeit von der Dateigrösse | 9 (laut Quelltext) |
| `FilenameDateParserTest.kt` | `media/FilenameDateParser`, `smb/SmbTarget` | gängige Datumsmuster im Dateinamen, Epoch-Millisekunden, kein Datum, SMB-Pfad-Parser | 4 |
| `LocalTreeLabelTest.kt` | `mirror/LocalTree` (nur Beschriftung) | Download-Picker-Pfade werden gekürzt | 1 |

### 4.2 Daten, Profile, Sicherung, Diagnose

| Datei | Getestet | Geprüfte Szenarien | Tests |
|---|---|---|---|
| `BackupCodecTest.kt` | `backup/BackupCodec`; zusätzlich `StaleCheck`, `SpaceFreer` | Round-Trip ohne Passwort; Passwörter verschlüsselt und wiederhergestellt; falsches/fehlendes Passwort importiert Profile trotzdem; ältere Sicherung bekommt Standardwerte; fremde und zukünftige Dateien werden abgelehnt; Erinnerung „veraltet“ alle drei Tage; Live-Photo-Paare bleiben zusammen; Schalter `syncOffWifi` im Round-Trip (Boolean) und fehlender Schlüssel bleibt fehlend (seit 0.11.7). Seit 0.13.0 drei Tests zur PC-Erkennung: Round-Trip von `pcCheck`/`pcGuid`/`pcGuidHost`; ältere Sicherung ohne diese Felder → `WIFI` und leer; Normalisierung beim Einlesen (Kleinbuchstaben/Host getrimmt; unbekannter Modus oder Null-UUID → `WIFI`/leer). Seit 0.14.3 (DEC-209) zusätzlich `folderLayout`: Round-Trip, fehlendes Feld → V1, unbekannter Code → Profil wird nicht importiert | 17 (laut Quelltext; seit 0.15.0 inkl. `smbEncryption`-Feld; Gesamtlauf Development 373 grün) |
| `ProfileOverlapTest.kt` | `data/ProfileOverlap` (seit 0.15.0 typübergreifend Fotos ↔ Ordner-Profil und kanonischer Handy-Ordner, DEC-092/-134) | gleiche Quelle trotz anderer Schreibweise; zusätzlicher Ordner = teilweise; Oberordner = teilweise; anderes Ziel/andere Ordner = ok; Bearbeiten desselben Profils ist kein Konflikt; Ordner-Profile vergleichen lokalen Ordner; verschachtelte Ordner-Profile warnen. Seit 0.14.3 zusätzlich Ordnerstruktur (DEC-209): zweites Fotoprofil mit gleichem Ziel und anderer Struktur wird blockiert, verschachteltes Ziel warnt | 19 (laut Quelltext, vorher 13; 0.15.0 laut Development grün) |
| `ProfileWizardTest.kt` | `data/ProfileWizard` | Schritte je Profiltyp (Schutzbremse nur bei Ordner-Profilen, zuletzt); Pflichtfelder (WLAN optional); Speichern ab erstem optionalem Schritt. Seit 0.14.3/0.14.4 zusätzlich Vorlagenwahl im Assistenten (Eigentümer-Vorlage nur Anzeige, `effectiveLayout`, DEC-209) | 6 (laut Quelltext, vorher 3) |
| `TargetResolverTest.kt` | `sync/TargetResolver` (mit Fakes für `RemoteDirs`/`MappingStore`; DEC-209) | je Vorlage V1/V2/V3: neue Ordner, vorhandener Standardname auch in anderer App-Sprache, Heimatland/ohne GPS, Rückfrage bei umbenanntem Ordner (V3: eine Gruppe je Monat), gespeicherte Zuordnung mit 1 bzw. 2 Segmenten, Bereinigung jedes Segments, verschwundener Zuordnungsordner wird neu entschieden, Simulation schreibt keine Zuordnungen, Live-Foto-Video im selben Ordner, `monthFolders` (a/b/c, für jede Vorlage gleich, Cache und `folderCreated`), offline listet nichts, `decisionSegments` je Vorlage | 27 (laut Quelltext; Lauf QA -088 grün) |
| `RemoteFinderTest.kt` | `sync/MonthFolderSearch` (`locate`, `monthOfLocation`), `SpaceFreer.RemoteFinder` (DEC-136, DEC-209) | Pfad zerlegen (letztes Jahressegment, 1–2 Ordner, unbekannte Form → null); Fund je Vorlage in Monats-, Länder- und Unterordnern, nach Handverschiebung, nie in anderen Monaten; gleicher Name und gleiche Grösse mit anderem Inhalt gilt nicht als gefunden; nicht lesbare Handy-Datei bleibt; Prüfung des Entscheidungspfads (`validateDecisionPath`) | 17 (laut Quelltext; Lauf QA -088 grün) |
| `FolderLayoutGuardTest.kt` | `data/FolderLayoutGuard`, `FolderLayout` (DEC-209) | gesperrtes Profil behält gespeicherte Vorlage, übrige Felder werden gespeichert; ungesperrtes Profil darf wechseln, offene Rückfragen gehören dann zur alten Vorlage; unbekannter Code wird auf einen gültigen normalisiert (nie ungültig in die DB); Codes stabil, `parse` streng | 6 (laut Quelltext; Lauf QA -088 grün) |
| `NumberInputTest.kt` | `data/NumberInput` (Zahlenfelder Profilformular, DEC-217) | leer/nicht numerisch ist nie ein Wert; Grenzen Papierkorb-Tage, Prüfintervall, Schutzbremse; Zwischenwerte beim Tippen ergeben nie eine falsche Zahl; Tippfilter lässt nur Ziffern zu und begrenzt die Länge | 6 |
| `ChangeConfirmTest.kt` | `data/ChangeConfirm` (Bestätigung bei Richtung/Löschungen, DEC-216) | keine Änderung = kein Dialog; Richtungswechsel fragt; Einschalten von «Löschungen übernehmen» fragt, Ausschalten nicht; beide Auslöser ergeben zwei Absätze; neues Profil, Fotos-Profil und fehlender Ausgangsstand fragen nie | 5 |
| `CredentialGroupsTest.kt` | `data/CredentialGroups` | gleicher Benutzer/Server/Passwort = ein Eintrag; Abweichungen getrennt; Profile ohne Benutzer ignoriert | 3 |
| `RunSummaryTest.kt` | `sync/RunSummary`, `data/RunGrouping` | Zusammenfassung zeigt nur Geschehenes; Nullwerte entfallen; aufeinanderfolgende ruhige Läufe gruppiert; einzelner ruhiger Lauf bleibt einzeln; Filter Fehler/Übertragung; „gefunden auf PC/Papierkorb“ ist nicht ruhig; Altformat bis 0.3.3 | 6 |
| `DiagnosticsTest.kt` | `diag/Diagnostics`, `VendorHints`, `CrashReporter` | Log-Verdichtung (neues/altes Format, Einzelzeile); Herstellererkennung; Absturzprotokoll behält neueste drei; Trace gekürzt; seit 0.12.4 Maskierung/Pseudonymisierung über `Redactor` (DEC-203); seit 0.14.6 `pcStatusShowsOnlyStateWord` (PC-Status nur als Zustandswort) | 17 (`@Test` im Quelltext gezählt 2026-11-10; Stand 2026-11-09 laut Development 15, grüner Gesamtlauf; Gesamtlauf 2026-11-10 nach 0.14.7 287/287 grün, enthält die Klasse) |
| `RemoteFolderNameTest.kt` | `invalidFolderName` (Ordnername in der PC-Auswahl) | Namensprüfung (leer, Länge, verbotene Zeichen, „.“/„..“, Ende mit Punkt/Leerzeichen, Dublette); seit 0.11.3; im QA-Lauf 2026-11-08 ausgeführt und bestanden | 4 |
| `NetworkRulesTest.kt` | `sync/NetworkRules` (reine Netzentscheidungen, DEC-126) | Tabelle erlaubte Netzart (`onAllowedNetwork`); Schalter + mobile Daten/VPN nötig (`offWifiSyncActive`); Weiterlaufen nach SSID bzw. Netzart (`mayContinue`); Wartezeiten (`retryMinutes`); Erreichbarkeitsversuche (`reachabilityWaits`); seit 0.11.7. Seit 0.13.0 (DEC-204) zusätzlich je Modus `WIFI`/`GUID`/`BOTH`: `needsWifiName`, `eligible`, `mayContinue(mode, …)`, `wifiTriggerMatches` | 12 (laut Quelltext, 6 neu seit 0.13.0; im Gesamtlauf 287/287 grün, QA -053 und Development -119) |
| `PcIdentityTest.kt` | `sync/PcIdentity`, `PcIdentityException` (DEC-204) | Normalisierung der Kennung (Kleinbuchstaben, Null-UUID/leer ungültig); `PcCheckMode.parse` fällt auf `WIFI` zurück; gespeicherte Kennung gilt nur für denselben Host (ohne Gross-/Kleinschreibung); Tabelle `decide` je Modus: `WIFI` blockiert nie, `GUID`/`BOTH` ohne Kennung nie anmelden, ohne gespeicherte Kennung nur bei `USER` lernen, mit gespeicherter Kennung gleich/abweichend; nur `ALLOW`/`ALLOW_AND_LEARN` dürfen anmelden; `PcIdentityException` ist kein Netzwerkfehler (auch verschachtelt) und keine `IOException`, echte Netzwerkfehler werden weiter erkannt; Meldungen lesbar mit Host. Neu seit 0.13.0 | 19 (laut Quelltext; im Gesamtlauf grün, QA -053 und Development -119) |
| `FolderFreerTest.kt` | `mirror/FolderFreer` (`isProtectedDir`, `emptyDirCandidates`; DEC-147) | Profil-Wurzel immer geschützt; `DCIM/Camera` geschützt (handgeschriebene Labels); andere Ordner nicht geschützt; Kandidaten tiefster Ordner zuerst ohne geschützte; geschützte übersprungen, andere bleiben; seit 0.11.8 | 5 |
| `RestoreRetryPolicyTest.kt` | `mirror/RestoreRetryPolicy` (DEC-146) | ab dem 5. Fehlversuch erschöpft; Zähler je Wiederherstellungs-ID; ID-Präfix wird nicht verwechselt (1/12); seit 0.11.8 | 3 |
| `MsgTest.kt` | `l10n/Msg` (Kennung), `data/RunTexts`, `RunGrouping.isIdle`, `RunSummary.NOT_IDLE_KEYS` (DEC-159/DEC-202) | Code rendert wie der Originaltext; Ressourcen-Namen statt IDs gespeichert; Mehrzahl und Verschachtelung überstehen die Speicherung; wörtlicher Text ohne Code; Rückfalltext bei unbrauchbarem Code; unbekannte Felder ignoriert, Tiefe begrenzt; Anzeige in der aktiven statt der Schreibsprache; `isIdle` über Schlüssel bzw. Merkmal-Wörter ohne Code; Log-Zeilen aus Code oder Rückfalltext; `NOT_IDLE_KEYS` sind echte Ressourcen; seit 0.12.0 | 11 (QA-Lauf 2026-11-09 bestanden) |
| `PcLoginTest.kt` | `smb/PcLogin` (Fake-Verbindung, DEC-204/-205) | siehe §7 «PC-Erkennung»: kein `authenticate` bei UNKNOWN/CHANGED/NO_ID/WRONG_NET, Reihenfolge connect → Kennung → Entscheidung → authenticate, Lernen/Neuvertrauen nur nach Commit; seit 0.15.0 zusätzlich Verschlüsselungs-Pflichtprüfung vor/nach `authenticate()` (DEC-222) | 31 (laut Quelltext; vorher 19) |
| `EtaTest.kt` | `ui/etaSeconds` (Restzeitschätzung) | keine Schätzung vor Mindestdauer/ohne Mindestmenge/bei Fertig oder Überschreitung/über Obergrenze; Schätzung bei belastbarer Rate; Obergrenze genau | 6 (laut Quelltext; im Gesamtlauf grün) |
| `PendingTrashedErrorTest.kt` | `isPendingOrTrashed`, `LocalItemNotReadyException` | siehe §7 «Android-spezifisch» | 4 (Development -050 gezielt grün; im Gesamtlauf enthalten) |
| `DataIntegrityTest.kt` | `ContentCompare`/`PartialHash.comparison`, `PresenceCheck`, Löschfreigabe und PC-Prüfung (DEC-223, R-I1 bis R-I4) | Vollhash über 32 MB, Lesefehler = nicht gleich, Stufe ≥ «Mittel» bei «bereits vorhanden», PC-Kopie vor dem Löschen (laut Auftrag SYNC-20261111-142; Einzelfälle nicht im Quelltext geprüft) | 25 (laut Quelltext; seit 0.15.0, Development: grün) |
| `SyncGateTest.kt` | `sync/SyncGate` (DEC-221) | Sperre, Nachlauf-Merker, Nachlaufart (manuell/automatisch), kein Verlust zwischen Prüfung und Freigabe (laut Auftrag SYNC-20261111-143) | 15 (laut Quelltext; seit 0.15.0, Development: grün) |
| `SmbConfigTest.kt` | `smbConfigFor` (DEC-222; Testfall S1 aus SYNC-20261110-129) | Abbildung der Verschlüsselungsstufe auf die smbj-Konfiguration, reine Funktion ohne Verbindung | 6 (laut Quelltext; seit 0.15.0, Development: grün) |
| `HomeStatusTest.kt` | Statuskarte (SYNC-20261110-113 A1/A2/A9) | gut = «alle aktuell»; offener Konflikt, keine Verbindung, Fehler im letzten Lauf nie «aktuell»; ohne/deaktivierte Profile nie «gesichert»; Vorrang «läuft»; Pause; Verbindungsermittlung | 11 (laut Quelltext; seit 0.15.0, Development: grün) |
| `ScreenLogicTest.kt` | reine Logik von Vorschau, Ordnerauswahl und Konflikten (SYNC-20261110-135 V4/O1, -136 K1) | laut Kopfkommentar der Testklasse; Einzelfälle nicht im Quelltext geprüft | 8 (laut Quelltext; seit 0.15.0, Development: grün) |
| `StringsParityTest.kt` | `res/values*/strings*.xml` über `TestTexts` | gleiche Schlüssel, Plural-Formen `one`/`other`, gleiche Platzhalter, Formate funktionieren, Apostrophe maskiert | 5 |

### 4.3 Instrumentation

`app/src/androidTest/java/com/sync2pc/app/MigrationTest.kt` (`@RunWith(AndroidJUnit4)`, `MigrationTestHelper`, braucht Gerät/Emulator):

- `fromVersion1ToLatest`: Profil der DB-Version 1 durchläuft alle Migrationen (`MIGRATION_1_2` … `MIGRATION_9_10`) und `runMigrationsAndValidate(..., 10, true, …)` (seit 0.15.0; zuvor 9 seit 0.14.3, 8 seit 0.13.0); prüft Schema und eine übernommene Spalte (Prüfung an → „Wenig“).
- `verifyLevelFromVersion5`: `MIGRATION_5_6` mit Datenzeilen (KEEP/Prüfung aus → „Aus“; TRASH/Prüfung aus → „Wenig“; Prüfung an → „Wenig“).
- `runCodesFromVersion6` (seit 0.12.0): `MIGRATION_6_7`; bestehender Lauf und bestehende Log-Zeile behalten ihren Text, die neuen Spalten `messageCode`, `triggerCode`, `code` sind `NULL`; eine Kennung lässt sich danach schreiben. Ausgeführt durch QA 2026-11-09 (Emulator API 29): bestanden.
- `pcCheckFromVersion7` (seit 0.13.0, DEC-204): `MIGRATION_7_8`; ein bestehendes Profil behält Name, Pfad, verschlüsseltes Passwort und WLAN-Liste, die neuen Spalten stehen auf `pcCheck = 'WIFI'`, `pcGuid = ''`, `pcGuidHost = ''`. **Nicht ausgeführt.**
- `folderLayoutFromVersion8` (seit 0.14.3, DEC-209): `MIGRATION_8_9`; bestehende Profile erhalten `folderLayout = 'MONTH_SUFFIX'`. **Nicht ausgeführt** (QA -088); die Migration wurde stattdessen am Emulator mit echten Daten (0.13.14 → 0.14.3) erfolgreich geprüft. Die Zahl „3“ in der Zählung älterer Läufe gilt nur für den ausgeführten Stand 0.12.1.
- `smbEncryptionFromVersion9` (seit 0.15.0, DEC-222): `MIGRATION_9_10`; liest nach der Migration Name, `smbPath`, `smbPasswordEnc`, `wifiSsids`, `folderLayout` und `smbEncryption` eines bestehenden Profils (Schema gegen `10.json`). **Nicht ausgeführt.**

`app/src/androidTest/java/com/sync2pc/app/MsgInstrumentedTest.kt` (seit 0.12.2, 6 Tests laut Development -043/-039): Namen-Round-Trip mit echtem `AndroidResolver`, Plural/verschachtelt, Rückfall, DE/EN, `triggerData/triggerOf`; Sprachwechsel nur bis Android 12. **Ausgeführt:** 6/6 grün (QA 2026-11-09, Emulator API 29, Stand 0.13.2, SYNC-20261109-053); seither nicht wiederholt. Zusätzlicher Gerätetest beim Inhaber (nicht ausgeführt): Hinweise für LeoSync aus → Eingriff → Ansicht öffnen (Ergebnis bleibt stehen).

Einzelne Migrationen 1→2, 2→3, 3→4, 4→5 werden **nur als Teil der Kette** gegen das Schema validiert, nicht mit eigenen Datenprüfungen (nachweisbar nur die oben genannten Datenprüfungen).

---

## 5. Datenbank- und Migrationstests

| Aspekt | Stand |
|---|---|
| Migrationskette 1→10 | **[vorhanden]** `MigrationTest.fromVersion1ToLatest` (Gerät/Emulator, validiert gegen Version 10); bis Version 8 am 2026-11-09 auf Emulator bestanden (QA -053, Stand 0.13.2); die Erweiterung auf 9 und 10 ist als Test **nicht ausgeführt** (Migration 8→9 am Emulator mit echten Daten geprüft, QA -088) |
| Datenerhalt bei 7→8 (`pcCheck`, `pcGuid`, `pcGuidHost`) | **[vorhanden]** `MigrationTest.pcCheckFromVersion7` (QA 2026-11-09, Emulator API 29: bestanden, Schema gegen `8.json` validiert) |
| Datenerhalt bei 9→10 (`smbEncryption`, 0.15.0) | **[vorhanden]** `MigrationTest.smbEncryptionFromVersion9` (**nicht ausgeführt**) |
| Datenerhalt bei 5→6 (`verifyLevel`) | **[vorhanden]** `MigrationTest.verifyLevelFromVersion5` |
| Datenerhalt bei 6→7 (Kennungsspalten) | **[vorhanden]** `MigrationTest.runCodesFromVersion6` (QA 2026-11-09 bestanden) |
| Datenerhalt bei 1→2, 2→3, 3→4, 4→5 mit eigenen Daten | **[fehlt]** (nur Schema-Validierung innerhalb der Kette) |
| Spalten `mirrorState`, `syncedFile`, `folderMapping`, `pendingRestore` usw. nach Migration | **[fehlt]** (nicht geprüft; nur eine Spalte des Profils) |
| DAO-Tests (`SyncDao`) | **[fehlt]** |
| Repository-/Entity-Tests, Transaktionen, Fremdschlüssel, Eindeutigkeit | **[fehlt]** |
| Persistierung von Sync-Ständen (`MirrorState`, `SyncedFile`) | **[fehlt]** als Datenbanktest; `MirrorState` wird nur als Datenobjekt in `MirrorGuardTest`/`BackupCodecTest` verwendet |
| Duplikaterkennung über Datenbank | **[fehlt]** |
| Backup/Restore der Profil-Daten (JSON, kein Room) | **[vorhanden]** `BackupCodecTest`, `VerifyLevelTest` |
| Schema-Datei zur aktuellen DB-Version vorhanden | `1.json`–`10.json` vorhanden (DB-Version 10); der Test prüft die Schemas nur, wenn `connectedDebugAndroidTest` läuft |

Wichtig: Das Gradle-Setup führt `MigrationTest` **nicht** beim Standard-Build (`build.bat`) aus. Ohne manuellen Lauf mit Gerät/Emulator ist eine Datenbankänderung beim Build ungeprüft. `build.md` verlangt zusätzlich einen Update-Test über die bestehende Installation.

---

## 6. Synchronisationsengine (Details: `docs/SYNC-ENGINE.md`)

`SyncEngine` (Bildordner), `MirrorEngine` (Ordner, 3-Wege-Abgleich, `plan()`), `SyncWorker`, `SyncRunner`, `DeletionManager`, `RestoreManager`, `TrashCleaner`, `FolderFreer` haben **keine direkten Tests** (`FolderFreerTest` prüft nur die Hilfsfunktionen `isProtectedDir`/`emptyDirCandidates`; `verifyAndDelete` ist ungetestet). Getestet sind nur ausgelagerte reine Hilfsklassen, seit 0.15.0 zusätzlich `SyncGate` (Sperre und Nachlauf-Entscheidung), `ContentCompare`/`PresenceCheck` und `smbConfigFor`, seit 0.15.5 `TransferResume` (Entscheidung vor/bei Wiederholung einer Übertragung).

| Szenario | Vorhandener Test | Testdatei | Bewertung |
|---|---|---|---|
| Neue Dateien erkennen (`plan()`) | – | – | **fehlt** |
| Geänderte Dateien erkennen | – | – | **fehlt** |
| Gelöschte Dateien erkennen | – | – | **fehlt** |
| 3-Wege-Abgleich / Entscheidungsbaum `plan()` | – | – | **fehlt** |
| Gleichzeitige Änderungen / Konflikterkennung | – | – | **fehlt** |
| Konfliktbehandlung (`ConflictMode`, `MirrorConflict`) | – | – | **fehlt** |
| Massenlösch-Schutzbremse (Entscheidungsfunktion) | `massDeleteAbove20Percent` u. a. | `MirrorGuardTest.kt` | getestet (reine Funktion `GuardPolicy`; Einbindung in den Lauf nicht getestet) |
| Soll/Ist-Abgleich nach verschwundenem PC-Ordner | `vanishedSubfolder`, `matchesWhenEverythingIsThere`, `missingFileOrDirIsMismatch` | `MirrorGuardTest.kt` | getestet (Funktion, nicht der Lauf) |
| Gründliche Inhaltsprüfung (Hash/Stichprobe) | `thorough*`, `fullCoversEveryByteWithoutGaps` | `MirrorGuardTest.kt`, `VerifyLevelTest.kt` | getestet |
| Reihenfolge der Übertragung | `smallFilesFirst` | `TransferPlanTest.kt` | getestet |
| Abgebrochene Übertragung fortsetzen / neu starten | `resumeWhenPcFileUnchanged`, `restartWhenPcFileChangedOrUnclear` | `TransferPlanTest.kt` | Entscheidungslogik getestet; die tatsächliche Dateiübertragung (`writeFileResumable`, `SmbSession.upload/download`) **fehlt** |
| Wiederholung nach Verbindungsabbruch: kein stilles Überspringen nach eigenem Papierkorb-Verschieben (-166, 0.15.5) | `retryAfterOwnTrashContinuesInsteadOfSkipping`, `retryNeverTrashesAgain`, `retryWithNewFileAtTargetIsNeverOverwritten`, `skipNeverHappensForAbsentTargetAfterOwnTrash` | `TransferResumeTest.kt` | Entscheidungsfunktion getestet; Verdrahtung in `MirrorEngine.executeTransfer`/`transferWorker` (`trashedByRun`, Wiederholung mit neuer Verbindung) und das Fortsetzen an der `.s2ptmp` **fehlen** (QA-Prüfung SYNC-20261111-169 offen) |
| Wiederholungsversuche, Fehlerbehandlung (`Errors.kt`) | – | – | **fehlt** |
| Wiederanlauf nach App-Abbruch / Neustart | – | – | **fehlt** (nur manuell, siehe Abschnitt 8) |
| Sichere Dateiersetzung (`replaceWith`, Temp-Datei `.s2ptmp`) | – | – | **fehlt** |
| Löschmodi / Papierkorb-Zuordnung | `entryFromTrashPath`, `versionNamesWithTime` u. a. | `TrashIndexTest.kt` | Namens- und Indexlogik getestet; Verschieben/Löschen selbst **fehlt** |
| Aufbewahrungsfrist Papierkorb | `olderFoldersExpire`, `cutoffIsDaysBack` | `TrashPolicyTest.kt` | getestet |
| Papierkorb leeren (Regeln, Vormerkung) | `rules`, `reservedEntriesAreKept` | `TrashPurgeTest.kt` | Regeln getestet; Ausführung **fehlt** |
| Wiederherstellen (Auswahl der Version) | `onlyOneVersionPerFileSelected`, `latestVersionPerFile` | `TrashIndexTest.kt` | Auswahl getestet; `RestoreManager`/`PendingRestore` **fehlt** |
| Speicher freigeben (Auswahlregeln) | `FreeRulesTest` (5) | `FreeRulesTest.kt` | Regeln getestet; `SpaceFreer`/`FolderFreer`-Ablauf **fehlt** |
| Bildordner: Datumsermittlung aus Dateiname | `commonPatterns`, `epochMillis` | `FilenameDateParserTest.kt` | getestet |
| Bildordner: Zielordnernamen, Konfliktnamen | `FolderNamesTest` | `FolderNamesTest.kt` | getestet |
| Bildordner: EXIF, Länderzuordnung (`MediaAnalyzer`, `CountryLookup`), Live-Photo-Paare | Live-Photo-Logik via `livePhotoPairs...` | `BackupCodecTest.kt` | Live-Photo-Test enthält lokale Hilfsklasse `F`; ob er produktiven Code aufruft, ist im Test nur über `SpaceFreer` erkennbar; EXIF/Land **fehlt** |
| Synchronisationsprofile (Überschneidung, Wizard, Zugangsdaten-Gruppen) | `ProfileOverlapTest`, `ProfileWizardTest`, `CredentialGroupsTest` | siehe 4.2 | getestet |
| Ein Lauf für alle Profile, Mutex, Pausen, WLAN-/PC-Bedingungen (`SyncWorker`) | – | – | **fehlt** |
| Simulation (`dryRun`) | – | – | **fehlt** |

---

## 7. SMB-, Netzwerk- und Android-Tests

### SMB / Netzwerk (`smb/SmbSession.kt`, `sync/WifiHelper.kt`)

Nachweislich **keine** Tests für Verbindungsaufbau, Authentifizierung, Freigabenliste (`listShares`), Verzeichnisoperationen, Upload/Download, `isReachable` (TCP 445), Timeouts, Wiederverbindung oder Netzwerkfehler. Es gibt weder echte SMB-Tests noch Fake-/Mock-Implementierungen. Das Einzige, was aus der SMB-Schicht getestet ist: der Pfad-Parser `SmbTarget` (in `FilenameDateParserTest.smbPaths`) und der Teilhash-Berechnungsteil (`PartialHash`, in `FolderNamesTest`/`MirrorGuardTest`/`VerifyLevelTest`) mit lokalen Byte-Arrays.

### PC-Erkennung (DEC-204, DEC-205; Stand 2026-11-09, Code 0.13.0; APK 0.13.0 und 0.13.1 in `releases\` vorhanden; zu den Tests siehe jeweils «ausgeführt/nicht ausgeführt»)

- **Unit (vorhanden, ausgeführt: QA -053 2026-11-09 grün, Gesamtlauf 2026-11-10 287/287 grün):** `PcIdentityTest`, `NetworkRulesTest` (je Modus), `BackupCodecTest` (3 neue Tests), Instrumentation `MigrationTest` 7→8 (Abschnitte 4.2, 4.3, 5). Ebenfalls grün: `StringsParityTest` (neue Texte `profile_pc_*`, `home_profile_pc_*`, `skip_pc_not_recognised`, `smb_err_pc_*`).
- **Neu/erweitert in 0.13.1 (vorhanden, ausgeführt: QA -053 grün; Development SYNC-20261109-055):** `PcLoginTest` (Fake-Verbindung: kein `authenticate` bei UNKNOWN/CHANGED/NO_ID/WRONG_NET, Reihenfolge connect → Kennung → Entscheidung → authenticate, Lernen/Neuvertrauen nur nach Commit, strengerer Modus, Grundsatz V7, Host-Zwischenspeicher, Host-Prüfung); `PcIdentityTest` erweitert (Modus × Zugriffsart × `learnNetOk`, V7, `PcEditorRules`, `WRONG_NET`-Meldung); `NetworkRulesTest` (`learnNetOk`); `DiagnosticsTest` (PC-Kennung/Host pseudonymisiert; die Zeilen in `Diagnostics.build` selbst nicht JVM-testbar); androidTest `PcGuidDaoTest` (SQL `learnPcGuid`/`trustNewPcGuid`/`clearPcGuid` inkl. Moduswächter, In-Memory-Room). `PcGuidDaoTest` 6/6 auf Emulator grün (QA -053, Stand 0.13.2).
- **Neu in 0.13.14 (Development SYNC-20261109-079):** `PcIdentityTest.hostChangeAsksOnlyForExistingGuidProfilesWithNewUnconfirmedHost` (`PcEditorRules.needsHostConfirm`, DEC-208). Gezielter Lauf `testDebugUnitTest` (`PcIdentityTest`, `MsgTest`) am 2026-11-09 grün (JDK 17) laut Development; nicht von Documentation ausgeführt. Nicht abgedeckt: UI-Verdrahtung (`confirmHost`).
- **Lücken:** keine Tests für `SyncWorker.precheckPc` und die Oberfläche (`ProfileScreen`, `HomeParts`; nur die ausgelagerte reine Regel `PcEditorRules` ist per JVM-Test abgedeckt).
- **Gerätetest (SYNC-20261109-053, G1–G15 und U1/U2 belegt, Emulator mit eigenem Testserver durch QA bzw. am echten Gerät/PC durch den Inhaber):** u. a. ein fremder PC unter gleicher IP erhält keinen SESSION_SETUP; Verhalten bei Kennungswechsel. **Offen:** G7 Akku bei `GUID` in fremden WLANs (Inhaber); Stabilität der Kennung bei Samba/NAS (ungeprüft).
- **Gerätetest (DEC-205, durch QA am Emulator belegt, SYNC-20261109-053 G11/G12, Stand 0.13.11):** Speichern und «Verbindung testen» in fremdem WLAN bei `BOTH` ohne Kennung → kein SESSION_SETUP; manueller Sync ohne Kennung → keine Anmeldung (Hinweis «Profil öffnen → Verbindung testen»). Zur Kompilierung unsicher laut Development (inzwischen gebaut, APK 0.13.1 vorhanden): `ConnectionContext.getServerGuid()`, Compose `collectAsState` in `createdId?.let`.

### Android-spezifisch

| Bereich | Stand |
|---|---|
| Activity/Lifecycle (`MainActivity`) | **[fehlt]** |
| ViewModels | im Projekt nicht als eigene Klassen erfasst; **keine** ViewModel-Tests |
| Compose-UI / Navigation | **[fehlt]** (keine `compose-ui-test`-Abhängigkeit) |
| Berechtigungen (`Permissions.kt`, `PermissionRows.kt`) | **[fehlt]**, nur manuell |
| Speicherzugriff (SAF, `LocalTree`, MediaStore) | **[fehlt]**; nur Label-Funktion getestet. `PendingTrashedErrorTest` (4 Tests, **[vorhanden]**) prüft nur die Fehlererkennung (`isPendingOrTrashed`, `LocalItemNotReadyException`, keine Verwechslung mit Netzwerkfehlern); reale Verifikation des MediaStore-Verhaltens (`IS_TRASHED`-Filter, Überspringen) auf dem Gerät fehlt |
| WorkManager (`SyncWorker`, `TriggerWorker`, `UpdateWorker`, `SyncScheduler`) | **[fehlt]** (kein `work-testing`; seit 0.15.0 nur die reine Klasse `SyncGate` per `SyncGateTest`) |
| Benachrichtigungen (`Notifications.kt`) | **[fehlt]** |
| Receivers (`Receivers.kt`), `PauseService` | **[fehlt]** |
| Selbst-Update (`UpdateChecker`, `UpdateInstaller`) | **[fehlt]** |
| Verschlüsselung (`CredentialCrypto`, Android Keystore) | **[fehlt]**; Passwort-Round-Trip in `BackupCodecTest` betrifft den Sicherungs-Codec, nicht den Keystore |
| Room-Migration | einziger Instrumentation-Test (Abschnitt 5) |

Instrumentation (Gerät/Emulator nötig): `MigrationTest`, `MsgInstrumentedTest`, `PcGuidDaoTest`. Alle anderen automatisierten Tests laufen auf der JVM.

---

## 8. Manuelle Tests und reale Geräte

Die Quelle ist `build.md`; sie wird hier nicht kopiert. Ergänzend die Einordnung.

**Zwingend (laut `build.md` Release-Checkliste, nur manuell möglich):**

1. Update über bestehende Version installieren; Profile, Verlauf, Sync-Stand vorhanden (Migration auf echtem Datenbestand).
2. Bildordner-Profil: Simulieren und Synchronisieren, richtige Monats-/Länderordner, keine Duplikate.
3. Ordner-Profil beidseitig: Änderung auf Handy und PC wird abgeglichen („Test Ordner-Sync“).
4. WLAN während des Syncs trennen → „Pausiert“; wieder verbinden → Fortsetzen (inkl. „fortgesetzt ab … MB“).
5. Schutzbremse (PC-Ordner umbenannt, > 20 % gelöscht, ganzer Unterordner) – „Test Schutzbremse“.
6. Papierkorb/Wiederherstellen/Aufräumen, auch bei ausgeschaltetem PC (Vormerkung) – „Test Versionsschutz …“, „Test Papierkorb aufräumen“.
7. Speicher freigeben (Fotos und Ordner) – „Test Speicher freigeben …“.
8. Sicherung Export/Import mit und ohne Passwort.
9. Sprache DE/EN auf allen Seiten.
10. Selbst-Update nach Release auf einem Gerät mit Vorversion.

**Ergänzend (empfohlen, in `build.md` nicht oder nur teilweise geregelt) [Empfehlung]:**

- Unterschiedliche NAS-/SMB-Implementierungen und Windows-Versionen (Samba, Synology, Windows-Freigabe).
- App-Abbruch (Prozess beenden) mitten in Upload, Download und Papierkorb-Aktion.
- Neustart des Geräts; Akkuoptimierung/Hintergrundbeschränkung der Hersteller (siehe `VendorHints`).
- Berechtigungen entziehen während Betrieb (Foto-, Speicher-, Benachrichtigungs-, Akku-Berechtigung).
- Sehr viele Dateien, sehr grosse Dateien, zu wenig Speicherplatz auf Handy oder PC.
- Mehrere Android-Versionen (minSdk 29 bis targetSdk 36), v. a. Speicherzugriff und Hintergrundlimits.
- Lokaler Ordner mit Sonderzeichen/langen Pfaden; Zeitzonen- und Uhrenabweichung zwischen Handy und PC.

---

## 9. Regressionsanfälligkeit und Testauswahl

| Änderung an … | Mindestens ausführen | Zusätzlich manuell |
|---|---|---|
| `GuardPolicy`/`MirrorGuard`, Schutzbremse | `testDebugUnitTest --tests "*MirrorGuardTest"` | „Test Schutzbremse“ |
| Prüfstufen, `PartialHash`, Hashing | `*VerifyLevelTest`, `*MirrorGuardTest`, `*FolderNamesTest` | Sync mit Prüfstufe „Viel“/„Komplett“ |
| Übertragung (Reihenfolge, Fortsetzen, Wiederholung) | `*TransferPlanTest`, `*TransferResumeTest` | Test Ordner-Sync inkl. WLAN-Trennung; Engine-Verhalten ist nicht automatisiert abgesichert |
| `MirrorEngine.plan()`, 3-Wege-Abgleich, Konflikte | **keine automatisierten Tests vorhanden**; sicherheitshalber komplette Unit-Tests | Test Ordner-Sync und Schutzbremse, Konflikt-Szenario mit gleichzeitiger Änderung |
| Löschen, Papierkorb, Wiederherstellen | `*TrashIndexTest`, `*TrashPolicyTest`, `*TrashPurgeTest` | „Test Versionsschutz“, „Test Papierkorb aufräumen“ |
| Speicher freigeben | `*FreeRulesTest`, `*BackupCodecTest` (`livePhotoPairs…`) | „Test Speicher freigeben“ |
| Datenmodell/Entity/Migration | `testDebugUnitTest` + `connectedDebugAndroidTest` (neues Schema `N.json`, neue Migration in `MigrationTest.all` aufnehmen) | Update über bestehende App |
| Sicherung/Backup-Format | `*BackupCodecTest`, `*VerifyLevelTest` | Export/Import |
| Profile, Wizard, Überschneidung | `*ProfileWizardTest`, `*ProfileOverlapTest`, `*CredentialGroupsTest` | Profil anlegen/bearbeiten |
| Texte/Strings | `*StringsParityTest` (alle `strings_*.xml` synchron) | Sprachumschaltung |
| Verlauf/Zusammenfassung | `*RunSummaryTest` | Verlauf-Ansicht |
| Dateifilter | `*FileFilterTest` | – |
| SMB-Zugriff | **keine Tests** | Test Ordner-Sync und Bildordner mit echtem PC; `assembleRelease` (R8) |
| WorkManager/Hintergrund/Berechtigungen | **keine Tests** | WLAN-Trennung, Neustart, Akku-Einschränkung, Berechtigungen |
| UI (Compose) | `assembleDebug` | Sichtprüfung auf Gerät |
| Abhängigkeiten, Gradle, Proguard | `testDebugUnitTest` + `assembleRelease` | Release-APK starten (R8-Abstürze nur zur Laufzeit sichtbar) |

Empfohlener Prüfablauf:

```mermaid
flowchart TD
  A[Änderung] --> B{Art}
  B -->|Reine Logik| C[Gezielte Testklasse] --> D[testDebugUnitTest]
  B -->|Entity/DB| E[testDebugUnitTest + connectedDebugAndroidTest]
  B -->|Engine/SMB/Worker| F[testDebugUnitTest + manuelle Checkliste]
  B -->|UI/Texte| G[assembleDebug, StringsParityTest]
  D --> H[Ergebnis ehrlich berichten]
  E --> H
  F --> H
  G --> H
```

---

## 10. Abdeckung und Lücken

| Bereich | Automatisierte Tests | Manuelle Tests | Erkannte Lücken |
|---|---|---|---|
| Schutzbremse / Löschschutz | Entscheidungslogik (`MirrorGuardTest`) | ja | Einbindung in den Lauf nicht getestet |
| Dateifilter, Namen, Datum | ja | teilweise | EXIF/Land (`MediaAnalyzer`, `CountryLookup`) ungetestet |
| Papierkorb, Versionen, Wiederherstellen | Index-/Regel-Logik | ja | Ausführung (`TrashCleaner`, `RestoreManager`, `DeletionManager`) ungetestet |
| Speicher freigeben | Auswahlregeln | ja | Ausführung ungetestet |
| Prüfstufen/Hashing | ja | ja | Vergleich mit echter SMB-Datei ungetestet |
| Übertragungsplanung | ja | ja | Echte Übertragung und Abbruch ungetestet; Wiederholungs-Entscheidung (`TransferResume`) getestet, ihre Verdrahtung in `MirrorEngine` nicht |
| Sync-Engine Ordner (`MirrorEngine`, 3-Wege) | **keine** | ja | **Kritisch:** `plan()`, Konflikte, Zustandsfortschreibung (S) ohne Test |
| Sync-Engine Bilder (`SyncEngine`) | **keine** (nur Hilfsklassen) | ja | **Kritisch:** Zielzuordnung, Duplikate, Live-Photo-Ablauf |
| SMB (`SmbSession`) | **keine** | ja | **Kritisch:** kein Fake, kein Integrationstest |
| Datenbank | nur Migrationstest (Gerät) | ja (Update) | DAO, Constraints, Datenerhalt der Zwischenmigrationen |
| WorkManager/Receiver/Pause | **keine** | ja | Kein `work-testing`, keine Worker-/Receiver-/`PauseService`-Tests; nur `SyncGate` (seit 0.15.0) geprüft; Pausen-, Wartebedingungslogik ungetestet |
| UI/Compose, Navigation | **keine** | ja | Keine UI-Tests |
| Berechtigungen, Benachrichtigungen | **keine** | ja | nur manuell |
| Verschlüsselung (`CredentialCrypto`) | **keine** | indirekt | Keystore-Verhalten ungetestet |
| Selbst-Update | **keine** | ja | nur manuell |
| Sicherung/Import | ja | ja | – |
| Texte DE/EN | ja | ja | – |
| CI / statische Analyse | **keine** | – | Kein automatischer Lauf ausserhalb von `build.bat` |

**Testkonzepte:** Die Lückenmatrix und die priorisierten Testfälle (B/P/N) stehen in den Berichten SYNC-20261110-127, -128 und -129 (`coordination/tasks/COMPLETED/`) und werden hier nicht kopiert. Die Zeilen zu R-N1/R-BG4 (Testfälle T1/T5) sind seit 0.15.0 als Logik durch `SyncGateTest` getestet; die Verdrahtung im Worker (`SyncWorker`, `SyncScheduler`) ist es nicht.

**Nicht eindeutig feststellbar** (Quellcode der betreffenden Klassen wurde für diese Datei nicht auf versteckte Testbarkeit geprüft): ob `livePhotoPairsStayTogether` produktiven Code oder nur lokale Testlogik prüft; ob weitere Funktionen von `Diagnostics`/`LocalTree` getestet sind, als die in 4.1/4.2 genannten. Es wurde keine Coverage-Messung (JaCoCo) durchgeführt; Zahlen zur Zeilenabdeckung sind unbekannt.

Testlücken mit höchster Priorität (ableitbar, [Empfehlung], nicht umgesetzt): (1) `MirrorEngine.plan()` mit Entscheidungstabellen aus `docs/SYNC-ENGINE.md` als Unit-Test, dafür reine Funktion ohne SMB nötig; (2) Fake für `SmbSession` hinter einer Schnittstelle; (3) Migrations-Datenprüfungen für alle Schritte und alle Tabellen; (4) DAO-Tests mit In-Memory-Room; (5) `work-testing` für `SyncWorker`; (6) CI-Lauf von `testDebugUnitTest` auf GitHub; (7) Nach 0.11.7 sind Netzregeln (`NetworkRulesTest`) und `syncOffWifi`-Round-Trip getestet. Weiter ohne Test (QA 2026-11-08): Orchestrierung in `SyncWorker` (Wartestufen, `handleUnreachable` mit `midRun`/Schalter), `SyncScheduler.onOffWifiChanged`, Aufhebung der Wartezeit in `WifiNetworkReceiver`, „Problem melden“ (`reportProblem`). Manuelle Gerätetests (Netzwechsel, Mail-App, Sicherung) bleiben beim Inhaber und wurden nicht ausgeführt. Die Gesamtzahl: 109 → 110 (0.11.7) nicht erklärt; 118 = 110 + 5 + 3 (0.11.8, laut QA -029).
(8) Ohne Test nach 0.11.8/0.11.9 (QA -029; `deleteDirIfEmpty` mit `rawChildCount` und `RestoreFailures` sind nicht JVM-testbar): `FolderFreer.verifyAndDelete`, `LocalTree.deleteDirIfEmpty` (SAF), `RestoreFailures` (Prefs), `runRestores`-Filter und Rollback, Kartenarithmetik „vorgemerkt − fehlgeschlagen“ auf der Startseite, `RestoreManager.retryFailed`; `isProtectedDir` nur mit handgeschriebenen Labels, nicht mit der echten Ausgabe von `LocalTree.label`; Verhalten von `deleteDocument` auf Ordnern am echten Anbieter nicht automatisierbar. Manuelle Gerätetests beim Inhaber (nicht ausgeführt): Ordner leeren (Unterordner, `DCIM/Camera`, SD-Karte, `.nomedia`, Netzabbruch) und 5 Fehlversuche provozieren (Erneut versuchen, Verwerfen, Papierkorb leeren).
(9) Ohne Test nach 0.11.10 (QA -035/-038): `TrashCleaner.purgeConfirmed`/`deleteEntries` mit I/O; `FolderFreer.verifyAndDelete` mit `onStart`; `MirrorEngine` Leeren nach Stichprobe (Development -034); `Notifications`; Abbrechen genau zwischen Sperrerwerb und `onStart`. Die Zustandslogik der Eingriffe (`InterventionRegistry`, früher `PendingIntervention`) ist seit -036 getestet. Manuelle Gerätetests beim Inhaber (nicht ausgeführt): Aufräumen während laufendem Sync (Wartezustand, Ansicht verlassen, Ergebnis als Hinweis); „Abbrechen“ im Wartezustand, auch kurz vor Sync-Ende wiederholt; PC aus → „Alles“ → Ordnerwechsel → Sync mit Ersetzungen → nächster Sync (ersetzte Versionen bleiben); Speicher freigeben während Sync; Prozess beenden im Wartezustand (nichts gelöscht); System-Hinweise für LeoSync aus → Eingriff → Ansicht schliessen → Ergebnis prüfen.
(10) Ohne Test nach 0.12.1 (QA -040): `SyncScheduler.triggerData`/`triggerOf`, `SyncDao.closeDanglingRuns`, Auflösung der Kennungen per Name mit echten Android-Ressourcen (`MsgTest` nutzt nur `TestTexts`), Sprachwechsel im Verlauf. Manuelle Gerätetests beim Inhaber (nicht ausgeführt): Update von 0.11.x mit bestehendem Verlauf (alte Einträge als Text); Sprachwechsel DE↔EN mit Verlauf (Lauf-Karte, aufgeklappte Zeilen, Export); Release-APK starten und Verlauf prüfen.
**Qualitätsrisiko (möglich, nicht belegt): Ressourcen-Namen im App-Bundle.** Kennungen speichern Ressourcen-**Namen** (`getResourceEntryName`) und lösen per `getIdentifier` auf. Im APK bleiben die Namen erhalten (QA: `aapt2 dump resources` am Release-APK 0.12.1, 799 Strings lesbar, u. a. `run_nothing`, `run_trashed`, `run_conflicts`). Für ein App-Bundle (AAB, Play Store) nicht geprüft; Bundle-Optimierung kann Namen verkürzen, dann würden gespeicherte Kennungen nach einem Update falsch oder nicht aufgelöst (Rückfalltext). Vor dem ersten AAB-Upload prüfen.

---

## 11. Empfehlungen je Aufgabe

| Aufgabe | Vorgehen |
|---|---|
| Kleine UI-Änderung | `assembleDebug`; bei Textänderung `StringsParityTest`; Sichtprüfung auf Gerät, falls möglich. Kein vollständiger Testlauf nötig |
| Entity/Datenbank | Version in `AppDatabase` erhöhen, Migration schreiben, Schema `N.json` committen, `MigrationTest` erweitern (Daten prüfen!), `testDebugUnitTest`, `connectedDebugAndroidTest`, Update-Test auf Gerät. Ohne Gerät: ausdrücklich als nicht ausgeführt melden |
| Sync-Engine | `testDebugUnitTest` komplett; Logik wenn möglich in reine Funktion auslagern und Test ergänzen; manuell „Test Ordner-Sync“ und „Test Schutzbremse“ |
| SMB-Zugriff | `assembleRelease` (R8), manueller Test gegen echten PC; ohne Tests Restrisiko benennen |
| WorkManager/Hintergrund | keine automatisierten Tests; manuell WLAN-Trennung, Neustart, Hintergrundbeschränkung; Risiko benennen |
| Backup-Format | `BackupCodecTest`, `VerifyLevelTest`; Altformat-Test ergänzen |
| Neue reine Logikklasse | Test in `app/src/test/java/com/sync2pc/app/` im Stil der bestehenden Tests (JUnit 4, Hilfsfunktionen, deutsche Kommentare) |

---

## 12. Qualitätsregeln für Claude Code

1. Vor Abschluss die zur Änderung passenden Tests aus Abschnitt 9 ausführen.
2. Testergebnisse nie erfinden; nur berichten, was tatsächlich gelaufen ist.
3. Fehlgeschlagene Tests und Build-Fehler immer nennen, auch wenn sie nicht zur Änderung gehören.
4. Build-Erfolg ist keine Aussage über funktionale Korrektheit (besonders Engine, SMB, Worker).
5. Bei Änderungen an `MirrorEngine`, `SyncEngine`, Löschlogik, Datenmodell und Migrationen gezielte Regressionstests und die passenden manuellen Szenarien aus `build.md` nennen.
6. Nicht ausgeführte Tests ausdrücklich als „nicht ausgeführt“ kennzeichnen (z. B. `connectedDebugAndroidTest` ohne Gerät).
7. Bei fehlenden Tests das verbleibende Risiko benennen.
8. Bei kleinen Änderungen gezielte Tests statt Vollläufen.
9. Projektregel (`CLAUDE.md` §1): Release-Builds und `build.bat` löst nur der Projektinhaber aus. Einzige Ausnahme: Die Rolle QA & Testing erstellt Debug-/Test-Builds (`assembleDebug`) selbstständig; Development, UX, Documentation, Architecture und PM bauen nicht.
10. Neue oder geänderte Texte immer in beiden `strings_*.xml` pflegen (sonst scheitert `StringsParityTest`).
11. Bei DB-Änderungen Schema-Datei und Migration committen; `MigrationTest.all` ergänzen.

---

## 13. Pflege dieser Datei

Bei neuen Testklassen: Abschnitt 4 und die Tabellen in 6, 9, 10 aktualisieren. Anzahl und Pfade der Tests sind in `docs/ARCHITECTURE.md` 4.7 ebenfalls erwähnt; bei Änderungen dort mitführen. Die Testanzahl (Quelltext-Zählung Documentation 2026-11-11, Stand 0.15.5: 45 Klassen/442 `@Test` JVM, 3 Klassen/18 `@Test` instrumentiert; zuletzt von QA ausgeführter Gesamtlauf: 2026-11-11, Version 0.15.2, 40 Klassen/377 Tests grün) ist bei jeder Änderung neu zu prüfen. Aussagen zu Zahlen in den Abschnitten 4 und 10 stammen teils aus älteren Läufen; massgeblich ist der jeweils jüngste QA-Lauf.

---

## 14. Eigenständige QA-Durchführung (seit 2026-11-10)

### 14.1 Ablauf eines Testauftrags (QA & Testing)
1. **Umgebung prüfen** (nur Nötiges, Ergebnis im Bericht): Java (`gradlew.bat` braucht JDK 17+; `JAVA_HOME` zeigt in der Shell auf Java 8, daher für den Aufruf `$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'` setzen), SDK laut `local.properties` (Build-Tools 34.0.0 und 36.0.0 vorhanden), `adb.exe` unter `%LOCALAPPDATA%\Android\Sdk\platform-tools` (nicht im PATH), `emulator.exe -list-avds` (Stand 2026-11-10: `LeoSync_API29`, `Medium_Phone_API_37.0`), `adb devices` (Android-Version per `getprop ro.build.version.release`/`sdk`), Erreichbarkeit der SMB-Testfreigabe (§14.3). Fehlendes dokumentieren, nichts umkonfigurieren; zusätzliche Software nur mit Erlaubnis des Inhabers.
2. **Tests wählen** nach §9 anhand der tatsächlichen Änderung; gültige Ergebnisse desselben Stands wiederverwenden, nur Nötiges neu bauen/ausführen.
3. **Bauen und installieren:** `gradlew.bat assembleDebug` → `adb install -r app\build\outputs\apk\debug\<apk>`. Debug-App hat die Kennung `….debug` und läuft parallel zur Release-App; Release-Installation des Inhabers nie anfassen.
4. **Ausführen:** `testDebugUnitTest [--tests …]`, `connectedDebugAndroidTest` (Emulator läuft), optional `lintDebug`; Emulator-Prüfung nach `CLAUDE.md` §1 (Profile vor Änderung exportieren; nach dem Test alle angelegten Testprofile löschen und den Ursprungszustand per Import des Exports wiederherstellen; Dateioperationen nur in `D:\testsync`/`X:\testsync` und im SMB-Testbereich `.build\qa-smb-share`). App-Daten für isolierte Tests: `adb shell pm clear <Paket>` (nur beim Debug-Paket und nur im Emulator/Testgerät).
5. **Berichten:** Befehl, Anzahl, Ergebnis; nicht Ausgeführtes mit Grund; Fehler als Fehlerbericht (§14.4).

Verboten für QA bleiben: Code-, Gradle- und Skriptänderungen, `version.properties`, `build.bat`, Release-Build, Signierung, Veröffentlichung, produktive Daten.

### 14.2 Testdaten und Testprofile
- Getrennt von produktiven Daten: Testprofile des Emulators (Profil «test» nur mit Export/Rückstellung, eigene Profile wie «QA-Host» danach immer löschen; Ursprungszustand per Profilexport wiederherstellen), Testordner `D:\testsync` und `X:\testsync` (vorhanden am 2026-11-10; Unterordner u. a. `vom_pc`), Freigabeordner `.build\qa-smb-share`. Eine zentrale Testdatendatei/Fixture gibt es **nicht** (siehe §3); der Datenbestand wird je Szenario angelegt.
- Abzudeckende Szenarien, soweit der Auftrag sie braucht: Dateien verschiedener Grösse, verschachtelte Ordner, Sonderzeichen, lange Namen, gleicher Name mit anderem Inhalt, unterschiedliche Änderungszeiten, Konflikt (gleichzeitige Änderung), Löschen/Wiederherstellen, abgebrochene Übertragung (WLAN trennen/Prozess beenden), unvollständiger Lauf. **[Empfehlung]** Die Anlage dieser Daten bisher manuell/ad hoc; ein Skript dafür wäre ein Development-Auftrag, QA darf es nicht schreiben.

### 14.3 SMB-Testumgebung
- **Vorhanden:** `tools\qa-smb-testshare.ps1`/`.py` (Python + impacket, venv unter `%LOCALAPPDATA%\LeoSyncTestShare`): Server auf `127.0.0.1:4445`, `adb reverse tcp:445 tcp:4445`, Freigabe `\\127.0.0.1\QA`. Start/Status/Stop und App-Einstellungen: `.claude/roles/QA-Specialist.md` §3 «Werkzeug». Zugang: Benutzer `qa`, Passwort `qa-test-1`. Das ist ein bewusst **öffentliches Testpasswort** (kein Geheimnis, `CLAUDE.md` §1): es gilt nur für diese lokale SMB-Testumgebung, darf in Rollenprompts und Testdoku stehen und nie für produktive Konten oder externe Systeme verwendet werden. Echte Zugangsdaten und produktive Passwörter gehören nie in Repo, Rollenprompts oder Doku. Schreibbereich von QA hier: Testdaten in `.build\qa-smb-share` sowie `.build\qa-smb-testshare.pid` und `.build\qa-smb-testshare.log` sowie das venv unter `%LOCALAPPDATA%\LeoSyncTestShare` (sonst keine Schreibberechtigung für `.build\` oder `%LOCALAPPDATA%`).
- **Abdeckbar damit (manuell im Emulator):** Verbindungsaufbau, Anmeldung, Dateiliste, Upload/Download, Überschreiben, Konflikt, Löschen/Papierkorb, Hostwechsel-Dialog, PC-Kennung; Verbindungsabbruch/Wiederverbindung durch Stoppen und Neustarten des Servers; nicht erreichbarer Server (Server gestoppt).
- **Grenzen:** App verbindet fest auf Port 445 (daher Umleitung); keine Samba-/NAS-/Windows-Freigabe-Eigenheiten; automatisierte SMB-Tests **[fehlt]** (kein Fake hinter einer Schnittstelle, siehe §7/§10). Eine echte dedizierte Test-Freigabe auf einem PC/NAS gibt es nicht; wäre eine Bereitstellung nötig, entscheidet der Inhaber.

### 14.4 Fehlerbericht
Task-Referenz · Funktion · App-Version und Build-Typ · Android-Version/API · Gerät/Emulator · Testfall · Ausgangslage · Schritte · erwartet · tatsächlich · Logauszüge (gekürzt) · Screenshot nur bei Nutzen · Reproduzierbarkeit · Ursache: **beobachtet** und **Vermutung** getrennt. Übergabe direkt an Development (`coordination/README.md` §7).

### 14.5 Manuelle Benutzertests
Weiterhin beim Inhaber (§8, `build.md`): persönliches Handy, produktive Profile und Daten, Entscheidungen mit persönlichem Bezug, Selbst-Update, verschiedene NAS-/Windows-Freigaben. QA bereitet vor, dokumentiert offene Punkte, nimmt Ergebnisse entgegen und markiert nie selbst «bestanden». Erinnerungen an offene Tests bleiben beim Project Manager.

### 14.6 Bekannte Einschränkungen und Lücken
Kein CI, keine UI-/Compose-Tests, keine Engine-/SMB-/Worker-Tests (§6, §7, §10), keine JaCoCo-Messung, kein Test über das echte Netzwerk ausser der lokalen Testfreigabe, nur Emulator-API 29 und 37 vorhanden (kein Android 11–15 dazwischen), persönliches Handy für QA nicht freigegeben. Release-Build/R8-Verhalten kann QA nicht prüfen (Inhaber).

**Lücken seit 0.15.0 (laut Development SYNC-20261111-142 bis -146, -157 und Architecture SYNC-20261110-124/-125/-126):** Worker-Ebene (`SyncWorker`, `SyncScheduler`, Receiver, `PauseService`) ohne Test (kein `work-testing`, R-BG7); kein Integrationstest für `MirrorEngine.plan()`/`FolderFreer` über 32 MB und `DeletionManager`; `MigrationTest` (auch 9→10) läuft nur als androidTest und wurde zu 0.15.0 nicht ausgeführt; Verschlüsselung nicht gegen echte Windows-/NAS-Server geprüft (ob der lokale QA-Testserver SMB3-Verschlüsselung unterstützt, ist ungeklärt), keine Durchsatzmessung; `msd:`-Auflösung der Überschneidungsprüfung nur am Gerät prüfbar; UX-Review 0.15.0 ausstehend (SYNC-20261111-154).
