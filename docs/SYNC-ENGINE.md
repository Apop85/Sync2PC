# Synchronisationsengine – LeoSync (Sync2PC)

> **Stand:** Grundstand Version 0.11.0 (DB-Version 6), einzelne Abschnitte bis 0.14.x nachgeführt (aktuell `version.properties` 0.14.7, DB-Version 9). **Massgebliche Quelle ist der Quellcode** unter
> `app/src/main/java/com/sync2pc/app/`. Dieses Dokument beschreibt ausschliesslich den **implementierten** Zustand.
> Geplantes steht nur in Abschnitt 18 und ist dort als „nicht umgesetzt“ gekennzeichnet.
>
> **Kennzeichnung im Text**
>
> | Marke | Bedeutung |
> |---|---|
> | **[IMPL]** | im Code nachweisbar umgesetzt |
> | **[RISIKO]** | umgesetzt, aber mit einer Lücke oder einem Datenverlust-Risiko, das hier benannt wird |
> | **[FEHLT]** | ein Schutz, den man erwarten könnte, existiert im Code nicht |
> | **[OFFEN]** | aus dem Code nicht eindeutig nachvollziehbar oder nicht geprüft |
>
> **Zuständigkeit dieser Datei:** Abläufe und Regeln der Synchronisation. Bibliotheken, Modulstruktur und Android-Details
> → `docs/ARCHITECTURE.md`; Tabellen und Spalten → `docs/DATA-MODEL.md`; Entscheidungsgründe (DEC-xxx) →
> `docs/DECISIONS.md`; Build und Testbefehle → `docs/TESTING.md`.

---

## Inhaltsverzeichnis

1. [Quellen und Analysegrundlage](#1-quellen-und-analysegrundlage)
2. [Überblick](#2-überblick)
3. [Synchronisationsablauf](#3-synchronisationsablauf)
4. [3-Wege-Abgleich (Ordner-Profile)](#4-3-wege-abgleich-ordner-profile)
5. [Konflikterkennung und Konfliktbehandlung](#5-konflikterkennung-und-konfliktbehandlung)
6. [Dateioperationen und Übertragung](#6-dateioperationen-und-übertragung)
7. [Löschlogik und Wiederherstellung](#7-löschlogik-und-wiederherstellung)
8. [Metadaten und Dateivergleich](#8-metadaten-und-dateivergleich)
9. [Synchronisationsprofile](#9-synchronisationsprofile)
10. [SMB-Kommunikation](#10-smb-kommunikation)
11. [Nebenläufigkeit und Hintergrundverarbeitung](#11-nebenläufigkeit-und-hintergrundverarbeitung)
12. [Fehlerbehandlung und Wiederanlauf](#12-fehlerbehandlung-und-wiederanlauf)
13. [Datenbank und Synchronisationszustand](#13-datenbank-und-synchronisationszustand)
14. [Kritische Invarianten und Schutzmechanismen](#14-kritische-invarianten-und-schutzmechanismen)
15. [Tests und bekannte Einschränkungen](#15-tests-und-bekannte-einschränkungen)
16. [Konkrete Beispiele](#16-konkrete-beispiele)
17. [Risikoregister](#17-risikoregister)
18. [Offene Fragen, Nicht-Geprüftes, Geplantes](#18-offene-fragen-nicht-geprüftes-geplantes)
19. [Abgrenzung und Verweise](#19-abgrenzung-und-verweise)

---

## 1. Quellen und Analysegrundlage

### 1.1 Tatsächlich gelesen (Zeile für Zeile)

| Bereich | Dateien (alle unter `app/src/main/java/com/sync2pc/app/`) |
|---|---|
| Engines | `sync/SyncEngine.kt` (Fotos), `mirror/MirrorEngine.kt` (Ordner), `sync/SyncRunner.kt` |
| Hintergrund | `sync/SyncWorker.kt`, `sync/SyncScheduler.kt`, `sync/TriggerWorker.kt`, `sync/Receivers.kt`, `sync/TransferLocks.kt`, `sync/PendingCheck.kt` |
| SMB / lokal | `smb/SmbSession.kt`, `mirror/LocalTree.kt`, `media/MediaScanner.kt` |
| Regeln | `mirror/MirrorGuard.kt`, `mirror/TransferPlan.kt`, `mirror/FileFilter.kt`, `sync/PartialHash.kt`, `sync/FolderNames.kt`, `sync/TargetResolver.kt`, `sync/Errors.kt` |
| Papierkorb | `mirror/TrashIndex.kt`, `mirror/TrashPolicy.kt`, `mirror/TrashPurge.kt`, `mirror/TrashCleaner.kt`, `mirror/RestoreManager.kt`, `mirror/MirrorLock.kt`, `mirror/FolderFreer.kt` |
| Löschen Fotos | `sync/DeletionManager.kt`, `sync/SpaceFreer.kt` (nur die ersten 120 Zeilen) |
| Daten | `data/Entities.kt`, `data/SyncDao.kt`, `data/AppDatabase.kt` (Migrationen), `data/GlobalSettings.kt` (nur `minFileAgeMinutes`, `parallelUploads`) |
| Tests | `MirrorGuardTest`, `TransferPlanTest`, `VerifyLevelTest`, `TrashPurgeTest`, `MigrationTest` (Inhalt gelesen); `TrashIndexTest` (Ausschnitt); übrige Testklassen nur über Testnamen und Grep nach Engine-Klassen |
| Doku | `docs/DECISIONS.md` (Abschnitte 2, 3, 4 teilweise, 7 teilweise, 8), `docs/ARCHITECTURE.md` (§7, §12–14), `docs/DATA-MODEL.md` (nur Überschriften), `PATCHNOTES.md` (Kopf), `version.properties` |

### 1.2 Nicht oder nur oberflächlich gelesen

`media/MediaAnalyzer.kt`, `geo/CountryLookup.kt`, `sync/WifiHelper.kt`, `sync/PauseService.kt`, `sync/Notifications.kt`,
`sync/StaleCheck.kt`, `backup/*`, `ui/*` (ausser `ConflictScreen`-Ausschnitt), `README.md`,
der Rest von `sync/SpaceFreer.kt`. Aussagen dazu sind als **[OFFEN]** gekennzeichnet oder verweisen auf DECISIONS.

### 1.3 Abweichungen zwischen Dokumentation und Code

| # | Dokumentation sagt | Code sagt | Hier gilt |
|---|---|---|---|
| A1 | `DECISIONS.md` DEC-035: „Der Ordner-Sync läuft sequenziell.“ | `MirrorEngine.runTransfers` führt Übertragungen mit 1–4 parallelen Workern aus (`MirrorEngine.kt:472`, seit 0.3.0 laut Kommentar). Nur Phase A (Verwaltung) ist sequenziell. | Code |
| A2 | Klassenkommentar `MirrorEngine` (Z. 47–56): „Ersetzte oder gelöschte Dateien landen immer in `.LeoSync-Papierkorb/<Datum>/…`“ | Der Papierkorb heisst seit 0.8.0 `.LeoSync-Bin` (`MirrorEngine.TRASH_DIR`). Ersetztes landet nicht „immer“ im Papierkorb: bei einseitiger Änderung nur mit `keepVersions` (Abschnitt 6.5). | Code |
| A3 | `DECISIONS.md` DEC-036: „… nie endgültig gelöscht“ | Der Papierkorb wird nach `trashRetentionDays` automatisch und von Hand geleert (`purgeTrash`, `TrashCleaner`). „Nie“ gilt nur für den Sync selbst. | Code |
| A4 | *(erledigt 2026-11-08)* Frühere Verweise auf die Unterstrich-Datei `docs/SYNC_ENGINE.md` (Arbeitsauftrag) | Datei entfernt; `ARCHITECTURE.md` §13 verweist auf `docs/SYNC-ENGINE.md`. | Diese Datei |
| A5 | `DECISIONS.md` DEC-037: „Beide behalten“ → `*_Konflikt-Handy.*` | Seit 0.8.0 `_conflict-phone` (`MirrorEngine.CONFLICT_SUFFIX`); DEC-037 selbst enthält den Nachtrag (DEC-164). | Code |
| A6 | `MirrorEngine`-Kommentar (Z. 52): „auf einer Seite gelöscht → optional übernehmen …, sonst wiederherstellen“ | Zutreffend für `BOTH`, `UPLOAD` und `DOWNLOAD` nur teilweise; bei der Richtung, die nicht zurückschreiben darf, wird nur der Sync-Stand verworfen (`DROP`), nicht wiederhergestellt (Abschnitt 4.5). | Code |

---

## 2. Überblick

### 2.1 Zweck

Die Engine hält Dateien zwischen einem Android-Handy und einem Windows-SMB-Share (PC/NAS) abgeglichen. Es gibt **zwei
voneinander unabhängige Engines**, die dieselbe Schnittstelle `SyncRunner` implementieren (`sync/SyncRunner.kt`):

| Engine | Profiltyp | Prinzip | Klasse |
|---|---|---|---|
| **Foto-Engine** | `PHOTOS` („Bildordner“) | Einweg Handy → PC, sortiert nach der gewählten Ordnerstruktur (V1 `Jahr\Monat[ - Land]`, V2 `Jahr\[Land\]Monat`, V3 `Jahr\Monat\[Land]`, DEC-209); Wiederholung erkannt über die Tabelle `synced_files`; **kein** 3-Wege-Abgleich | `sync/SyncEngine.kt` |
| **Ordner-Engine** | `MIRROR` („Ordner“) | 1:1-Spiegelung eines SAF-Ordners mit einem PC-Ordner, 3-Wege-Abgleich über `mirror_state` | `mirror/MirrorEngine.kt` |

Der **3-Wege-Abgleich existiert nur in der Ordner-Engine**. Die Foto-Engine erkennt weder Löschungen noch Änderungen auf dem
PC und kennt keine Konflikte (Abschnitt 3.2).

### 2.2 Komponenten und Verantwortlichkeiten

| Komponente | Datei | Aufgabe |
|---|---|---|
| `SyncScheduler` | `sync/SyncScheduler.kt` | Plant WorkManager-Aufträge (Auto, Manuell, Nachhol, Wiederholung, Fortsetzen, Sicherheitsnetz, PC-Abfrage, Pause) |
| `TriggerWorker` | `sync/TriggerWorker.kt` | Leichter Auslöser: ruft nur `enqueueAuto` |
| `WifiNetworkReceiver`, `BootReceiver` | `sync/Receivers.kt` | WLAN-Ereignis → `enqueueAuto`; Neustart/Update → `setupAll` |
| `SyncWorker` | `sync/SyncWorker.kt` | Eigentlicher Lauf-Koordinator: wählt Profile, prüft WLAN und PC-Erreichbarkeit, startet je Profil die passende Engine, Benachrichtigungen, Wiederholungen |
| `SyncEngine` | `sync/SyncEngine.kt` | Foto-Lauf eines Profils |
| `MirrorEngine` | `mirror/MirrorEngine.kt` | Ordner-Lauf eines Profils (Planung + Ausführung) |
| `GuardPolicy`, `GuardStore`, `MirrorGuard`, `FolderCheck` | `mirror/MirrorGuard.kt` | Schutzbremse (reine Regeln, Speicher in SharedPreferences, Benutzerantworten, Stichprobe bei Ordnerwechsel) |
| `SmbSession` | `smb/SmbSession.kt` | Alle SMB-Operationen (Auflisten, Hoch-/Herunterladen, Verschieben, Hashes) |
| `LocalTree` | `mirror/LocalTree.kt` | Alle Operationen im Handy-Ordner über das Storage Access Framework |
| `MediaScanner` | `media/MediaScanner.kt` | Fotos/Videos aus dem MediaStore (nur Foto-Engine) |
| `TargetResolver` | `sync/TargetResolver.kt` | Zielordner je Ordnerstruktur (V1–V3, DEC-209) der Foto-Engine |
| `MonthFolderSearch` | `sync/MonthFolderSearch.kt` | Ordner des Monats für die Duplikatsuche, Pfad zerlegen (`locate`) |
| `PartialHash`, `TransferPlan`, `FileFilter`, `FolderNames`, `TrashIndex`, `TrashPolicy`, `TrashPurge` | `sync/`, `mirror/` | Reine Regelobjekte (teilweise getestet, Abschnitt 15) |
| `RestoreManager`, `TrashCleaner`, `FolderFreer`, `SpaceFreer`, `DeletionManager` | `mirror/`, `sync/` | Eingriffe ausserhalb des Syncs (Wiederherstellen, Leeren, Speicher freigeben, Löschen nach Sync) |
| `MirrorLock` | `mirror/MirrorLock.kt` | Mutex zwischen Ordner-Lauf und Handeingriffen |
| `AppDatabase`, `SyncDao`, `Entities.kt` | `data/` | Room-Persistenz (Details → `DATA-MODEL.md`) |

```mermaid
flowchart TD
    subgraph Ausloeser["Auslöser"]
        W["WLAN-Ereignis\nWifiNetworkReceiver"]
        C["Neue Medien\nContent-Trigger"]
        S["Sicherheitsnetz 6 h / PC-Abfrage / Nachhol / Wiederholung"]
        M["Manuell (App)"]
        U["Entscheidung des Benutzers\nKonflikt, Schutzbremse, Wiederherstellung"]
    end
    W --> T["TriggerWorker / SyncScheduler.enqueueAuto"]
    C --> T
    S --> T
    M --> SM["SyncScheduler.enqueueManual"]
    U --> SM
    T --> SW["SyncWorker.doWork"]
    SM --> SW
    SW -->|"Profiltyp PHOTOS"| SE["SyncEngine"]
    SW -->|"Profiltyp MIRROR"| ME["MirrorEngine"]
    SE --> SMB["SmbSession"]
    ME --> SMB
    ME --> LT["LocalTree (SAF)"]
    SE --> MS["MediaScanner (MediaStore)"]
    SE --> DB[("Room")]
    ME --> DB
    ME --> SP[("SharedPreferences\nguard / purge / restore")]
    SW --> N["Notifications"]
    SW --> SS["SyncScheduler\nFolgeaufträge"]
```

### 2.3 Einstiegspunkte und Auslöser

Alle automatischen Läufe laufen über **einen** Worker-Typ (`SyncWorker`); `TriggerWorker` reiht nur ein.

| Auslöser | Einstieg | Verhalten |
|---|---|---|
| WLAN verbunden | `WifiNetworkReceiver.onReceive` → `enqueueAuto` | Drosselung 60 s, ausser `resumeOnWifi` gesetzt; nur wenn ein aktives Profil zur SSID passt (oder SSID unlesbar) **[IMPL]** |
| Neue Fotos/Videos | `armContentTrigger` (Content-URI-Trigger) → `TriggerWorker` | Wartet Mindestalter ab (`minFileAgeMinutes`, Standard 5); wird nach einem Lauf nur dann neu scharf gestellt, wenn keiner wartet (`KEEP`, seit 0.16.9; ein wartender Trigger behält seine Frist, neue Medien verlängern sie selbst; Einstellungsänderungen stellen ihn mit `REPLACE` neu); mit `syncOffWifi` auch ausserhalb des WLAN |
| Sicherheitsnetz | `scheduleSafetyNet` | 6 h nach dem letzten Lauf, wird bei jedem Lauf nach hinten geschoben; mit `syncOffWifi` auch ausserhalb des WLAN |
| PC-Abfrage (Ordner-Profile) | `scheduleRemotePoll` | Periodisch, mindestens 15 min (WorkManager-Minimum), kürzestes Intervall aller aktiven Ordner-Profile |
| Nachhol-Sync | `enqueueFollowUp` | Für zu junge Dateien, frühestens nach 30 s |
| PC nicht erreichbar | `enqueueShareRetry` | Nach 30 min (bei Abbruch mitten im Lauf nach 2 min; ausserhalb des WLAN, nur mobile Daten/VPN, nach 2 h; seit 0.11.7) |
| WLAN-Unterbruch | `enqueueResume` + Receiver | Fortsetzen sobald WLAN da; gibt nach 24 h auf |
| Manuell | `enqueueManual` | Hebt eine Pause auf; keine WLAN-Bedingung (nur `CONNECTED`) |
| Benutzerentscheidung | `ConflictScreen.resolve` → `enqueueAuto`; `MirrorGuard.resolve`, `RestoreManager.request` → `enqueueManual` | Entscheidung wird erst im nächsten Lauf angewendet |
| Ende einer Pause | `pause()` → Auftrag `pause-end` | Löst `enqueueAuto` aus |
| Simulation (Trockenlauf) | `ui/PreviewScreen.kt` erzeugt die Engine **direkt** mit `dryRun = true` | Läuft **nicht** über `SyncWorker` und nimmt weder dessen Mutex noch `MirrorLock` |

### 2.4 Unterschiede der Profile (Kurzfassung)

Vollständige Vergleichstabelle in Abschnitt 9. Es gibt effektiv vier Verhaltensvarianten:
`PHOTOS` (immer Handy → PC), `MIRROR` mit Richtung `UPLOAD`, `DOWNLOAD` oder `BOTH`. Zusätzlich beeinflussen
`propagateDeletes`, `conflictMode`, `keepVersions`, `trashRetentionDays`, `massDeletePercent` und die Filter das Ordner-Verhalten;
`deleteMode` und `verifyLevel` das Foto-Verhalten.

---

## 3. Synchronisationsablauf

### 3.1 Koordination durch `SyncWorker` (beide Profiltypen)

`SyncWorker.doWork` → `work()`. Reihenfolge:

1. **Sperre:** `SyncGate` (seit 0.15.0, reine Klasse; ersetzt den früheren `SyncWorker.mutex`, Sperre und Nachlauf-Merker unter einer gemeinsamen `synchronized`-Sperre). Ist bereits ein Lauf aktiv, endet der Worker sofort mit `Result.success()` (Ausgabedaten `deferred = true`), die Anfrage wird aber als **Nachlauf vorgemerkt** und nach dem laufenden Sync genau einmal nachgeholt (Abschnitt 11.2, DEC-221). **Ausnahme (seit 0.16.0, DEC-233):** Aufträge «Fortsetzen» (`KEY_RESUME`), «PC-Wiederholung» (`KEY_RETRY`) und der Folgelauf nach übersprungenen Dateien (6.7) nehmen die Sperre über `SyncGate.tryAcquire()` ohne Merker; ist sie belegt, endet der Worker mit `Result.retry()` (WorkManager behält Auftrag und Eingabedaten, linearer Backoff 30 s), statt in den Nachlauf-Merker umgewandelt zu werden. Daten und die 24-h-Grenze bleiben so erhalten. AUTO/MANUAL-Aufträge sind unverändert.
2. `dao.closeDanglingRuns()`: Läufe mit Status `RUNNING` (App wurde währenddessen beendet) werden auf `INTERRUPTED` gesetzt **[IMPL]**.
3. **Pause:** Nicht-manuell und `settings.isPaused()` → Lauf entfällt (Grund wird notiert).
4. **Profilauswahl:** aktiv (`enabled`), konfiguriert (`isConfigured`), bei automatischen Läufen nicht in `awaitingFirstStart` (neue Profile warten auf ersten manuellen Start, DEC-103), **nicht durch die Schutzbremse blockiert** (`GuardStore.blockedIds`), nicht gerade zum Löschen vorgemerkt (`PendingProfileDelete`).
5. **WLAN:** Automatisch nur Profile, deren Ziel-SSID zur aktuellen SSID passt; bei noch unlesbarer SSID bis zu 3× 5 s nachlesen. Manuell: alle. Mit Schalter `syncOffWifi` (seit 0.11.5, DEC-126) entfällt die SSID-Prüfung ausserhalb des WLAN: alle aktiven Profile sind berechtigt; im WLAN (auch VPN über WLAN) gilt weiter die SSID-Regel.
6. **PC-Erreichbarkeit** (`SmbSession.isReachable`, TCP Port 445, 3 s): automatisch im WLAN bis 5 Versuche (0, 5, 10, 15, 30 s), manuell 2 (0, 3 s), ausserhalb des WLAN (nur mobile Daten/VPN, seit 0.11.7) 2 (0, 5 s) (`NetworkRules.reachabilityWaits`). **Ausnahme:** Foto-Profile ohne übertragungsbereite Dateien (`PendingCheck.readyFiles == 0`) überspringen den Test. Danach folgt bei Profilen mit `GUID`/`BOTH` die Vorab-Prüfung der PC-Kennung (Abschnitt 6.0, seit 0.13.0).
7. **Vordergrunddienst:** `setForeground(...)`; scheitert das (Android 12+), normale Benachrichtigung. `TransferLocks.acquire` hält WLAN- und CPU-Sperre (max. 4 h).
8. **Schleife über alle lauffähigen Profile, nacheinander** (nicht parallel): Engine erzeugen (`MIRROR` → `MirrorEngine`, sonst `SyncEngine`), `engine.run()`, danach Flags auswerten:
   `engine.blocked` → Benachrichtigung „Entscheidung nötig“; `wifiLost && !manual` → `markInterrupted()`; `connectionFailed` → Profil zählt als „nicht erreichbar“ und der Lauf als mitten unterbrochen; `encryptionUnavailable` (seit 0.16.0, DEC-237; Verschlüsselung «Erforderlich», Server bietet sie nicht an) → Verlauf `FAILED`, die Fehlermeldung kommt bei automatischen Läufen je Profil nur einmal (Merker `encNotified`, zurückgesetzt nach einem erfolgreichen Lauf des Profils, auch für WLAN-Profile), bei manuellem Start immer; `FAILED` → harter Fehler.
9. **Abschluss:** Sperren freigeben, Zusammenfassungs-Benachrichtigungen (Fehler, Übertragen, Entscheidungen, Konflikte, Speichermangel, Löschfreigaben), Nachhol-Sync für während des Laufs entstandene Medien (`MediaScanner.hasAddedSince`), `handleUnreachable` (Wiederholung bzw. `markInterrupted`; Wartezeit nach `NetworkRules.retryMinutes`: 30 min, mitten im Lauf 2 min, ausserhalb des WLAN immer 2 h; die Wartezeit endet durch neue WLAN-Verbindung, manuellen Start oder erfolgreichen Lauf, neue Auslöser (Fotos, Sicherheitsnetz, Ordner-Prüfung) ignorieren sie, DEC-126).
10. `finally` in `doWork`: Fortschritt entfernen; wenn von Android gestoppt (nicht vom Benutzer) und nicht manuell → `markInterrupted()`; Sperre (`SyncGate`) in jedem Fall freigeben (verschachteltes `finally`, auch wenn `markInterrupted`/`cancelProgress` eine Ausnahme werfen); danach genau einen vorgemerkten Nachlauf einreihen (Abschnitt 11.2); `armContentTrigger` (mit `KEEP`: ein bereits wartender Trigger behält seine Frist, seit 0.16.9); Sicherheitsnetz neu starten.

`doWork` gibt in allen regulären Pfaden `Result.success()` zurück (Engine-Ausnahmen werden in den Engines abgefangen); es gibt kein `Result.retry()` und kein `Result.failure()` im Code **[IMPL]**. Wiederholungen werden ausschliesslich über eigene Folgeaufträge erzeugt (Abschnitt 12).

```mermaid
flowchart TD
    A["doWork"] --> B{"Mutex frei?"}
    B -->|"nein"| Z["Result.success – Anfrage verworfen"]
    B -->|"ja"| C["closeDanglingRuns"]
    C --> D{"Pause aktiv und nicht manuell?"}
    D -->|"ja"| Z2["Abbruch, Grund notieren"]
    D -->|"nein"| E["Profile filtern:\naktiv, konfiguriert, nicht wartend auf Erststart,\nnicht blockiert, nicht in Löschung"]
    E --> F{"Ziel-WLAN passt?\nmanuell: immer"}
    F -->|"nein"| Z3["Ende, ggf. Fortsetzen-Auftrag in 15 min"]
    F -->|"ja"| G["PC erreichbar? TCP 445 mit Wiederholungen"]
    G -->|"nein"| H["handleUnreachable:\nRetry 30 min oder markInterrupted"]
    G -->|"ja"| I["Vordergrunddienst + WLAN/CPU-Sperre"]
    I --> J{"Pro Profil nacheinander"}
    J -->|"PHOTOS"| K["SyncEngine.run"]
    J -->|"MIRROR"| L["MirrorEngine.run unter MirrorLock"]
    K --> M["Flags auswerten"]
    L --> M
    M --> J
    J -->|"fertig"| N["Benachrichtigungen,\nNachhol-Sync, Sicherheitsnetz, Content-Trigger"]
```

### 3.2 Foto-Lauf (`SyncEngine.run`)

Kein Vergleich mit dem Zustand des PCs; der Lauf entscheidet allein aus dem Handy-Bestand und der Tabelle `synced_files`.

| # | Schritt | Details / Code |
|---|---|---|
| 1 | Lauf anlegen | `dao.insertRun(SyncRun(...))`; Status `RUNNING`; Logs älter 30 Tage und Läufe älter 90 Tage werden gelöscht (nicht bei `dryRun`). Seit 0.12.0 trägt der Lauf den Auslöser zusätzlich als Kennung (`triggerCode`, aus den Auftragsdaten `KEY_TRIGGER_CODE`, Rückfall auf den Text) und beim Abschluss den Kurztext als `messageCode`; `log()` schreibt Text und `code` aus derselben `Msg` (siehe `DATA-MODEL.md` 3.11). Anzeige und `RunGrouping.isIdle` lesen die Kennung (`RunSummary.NOT_IDLE_KEYS`), die Sync-Entscheidungen ändern sich nicht |
| 2 | Lokalen Bestand ermitteln | `MediaScanner.scan(...)` über MediaStore (`RELATIVE_PATH LIKE`, `IS_PENDING = 0`, `size > 0`; ab Android 11/API 30 zusätzlich `IS_TRASHED = 0`, minSdk 29) |
| 3 | Filter | `FileFilter(includePatterns, excludePatterns)` auf `relativePath + displayName` |
| 4 | Bekanntes aussortieren | `dao.syncedLite(profileId)` → Map `sourceKey`; **bekannt** = gleiche `size` **und** gleiches `dateModified` (Sekunden) → übersprungen ohne I/O |
| 5 | Mindestalter | `age = jetzt − max(dateAdded, dateModified) < minFileAgeMinutes` → „zu jung“, frühester Nachholzeitpunkt in `nextYoungFileAt` (nicht bei `dryRun`) |
| 6 | Nichts zu tun? | `todo.isEmpty()` → `finish(OK, NOTHING)` **ohne Netzwerkverbindung** |
| 7 | SMB öffnen | `SmbSession.open(profile)`; bei `dryRun` und Fehler → Offline-Vorschau |
| 8 | Analyse + Zielordner (Produzent, sequenziell) | `mediaInfo()` (Cache `media_info`, gültig bei gleicher `size`/`dateModified`; sonst `MediaAnalyzer`), `TargetResolver.resolve` → `Folder` oder `Ask` |
| 9 | Entscheidungsbedarf | `Ask` → Datei wird **nicht** übertragen, Gruppe wird gezählt; am Lauf-Ende als `PendingDecision` gespeichert (Abschnitt 13) |
| 10 | Übertragung (parallel) | `Channel<Job>(32)`, `parallelUploads` (1–4, Standard 3) Worker mit je eigener `SmbSession` |
| 11 | Pro Datei (`transfer`) | Namensvergabe unter `nameLock`, Duplikatprüfung, Upload mit Temp-Datei, Prüfung nach `verifyLevel`, erst dann `record()`. Wechselt ein Eintrag zwischen Scan und Lesen auf pending/trashed (`IllegalStateException` „Only owner is able to interact with pending/trashed item“, in `openLocal` als `LocalItemNotReadyException` gekapselt), wird die Datei still übersprungen (kein ✗, kein Fehlerzähler, kein Ausschluss); der nächste Lauf versucht sie erneut |
| 12 | Abschluss | Offene Entscheidungen speichern, `DeletionManager.sweep`, Status bestimmen, `finish(...)` |

Statusbestimmung: Standard `OK`; `failed > 0` → `PARTIAL`; WLAN weg → `PARTIAL`; Abbruch durch Android → `INTERRUPTED`; durch Benutzer →
`CANCELLED`; Verbindungsfehler → `INTERRUPTED` (+ `connectionFailed`); sonstiger Fehler → `FAILED`.

**Zielordner (`TargetResolver`, Reihenfolge)** **[IMPL]**: (1) gespeicherte Benutzerentscheidung `folder_mappings` (nur gültig, solange der Ordner existiert oder noch nie gesehen wurde; Pfad wird beim Lesen bereinigt: kein `..`, höchstens 2 Segmente, sonst ignoriert), (2) Standardname existiert bereits (auch in anderer App-Sprache), (3) umbenannte Ordner mit gleicher Monatszahl → Benutzer fragen, (4) Standardname neu anlegen. Die Schritte laufen je nach Ordnerstruktur des Profils (`Profile.folderLayout`, DEC-209) auf anderen Ebenen unterhalb des Jahresordners:

| Vorlage | Ziel (relativ zum Jahr) | Standardname / Länderebene | Rückfrage (Schritt 3) |
|---|---|---|---|
| V1 `MONTH_SUFFIX` (Standard, bisher) | `MM` bzw. `MM - Land` | Land im Monatsnamen; Heimatland ohne Zusatz | Gruppe je Jahr, Monat, Land; Ordner „MM“ und „MM - <Land>“ gelten als erzeugt (Ländernamen aller App-Sprachen) |
| V2 `COUNTRY_FOLDER` | `[Land\]MM` | Länderordner im Jahr (nur am Standardnamen erkannt, auch in anderer App-Sprache), darin `MM`; Heimatland: `MM` direkt im Jahr | Gruppe je Jahr, Monat, Land; im Länderordner gilt jeder Ordner mit Monatszahl ausser „MM“ als umbenannt; gespeichert werden bis zu 2 Segmente |
| V3 `COUNTRY_SUBFOLDER` | `MM\[Land]` | Monatsordner `MM`, darin der Länderordner (Standardname); Heimatland: Fotos direkt in `MM` | **eine** Gruppe je Jahr und Monat (nicht je Land); jeder Ordner mit Monatszahl ausser „MM“ gilt als umbenannt; gespeichert wird nur der Monatsordner, der Länderunterordner kommt automatisch dazu |

Fotos liegen immer direkt in der letzten Ebene. Bei V2/V3 gibt es keine Länderliste zur Erkennung erzeugter Namen: Länderordner werden nur am Standardnamen wiedererkannt, jeder andere Ordner mit Monatszahl löst die Rückfrage aus (DEC-019, DEC-209). Die Struktur ist bis zur ersten Synchronisierung des Profils änderbar, danach gesperrt (DEC-209, `FolderLayoutGuard` in `SyncDao.updateProfile`; `FolderLayoutGuard.changed` erkennt einen Vorlagenwechsel, weil offene Ordner-Rückfragen dann zur alten Vorlage gehören).

**Duplikatsuche über Monatsordner (`MonthFolderSearch.related`, vorlagenunabhängig, auch V1)** **[IMPL]**: `TargetResolver.monthFolders(jahr, monat)` liefert alle Ordner des Monats, in denen eine Datei schon liegen könnte: (a) Ordner im Jahr mit der Monatszahl, (b) deren Unterordner (V3: Länderordner), (c) Ordner mit der Monatszahl in Unterordnern ohne Monatszahl (V2: Monatsordner im Länderordner). Ob wirklich ein Duplikat vorliegt, entscheiden Name, Grösse und Teil-Hash (Schritt 3 unten). `SpaceFreer.RemoteFinder` zerlegt den gespeicherten Pfad mit `MonthFolderSearch.locate` (Jahr = letztes vierstelliges Segment, darunter 1–2 Ordner); bei unbekannter Form wird nicht gesucht und die Datei gilt als „nicht gefunden“ (bleibt auf dem Handy; DEC-136, DEC-209).

**Duplikat- und Namenslogik in `transfer()`** **[IMPL]** (Zeilenverweis `SyncEngine.kt:533` seit 0.14.3 veraltet, Funktion `transfer` in `SyncEngine.kt`):

1. Name = `FolderNames.sanitize(displayName)` (Windows-ungültige Zeichen → `_`).
2. Unter `sh.nameLock`: Ordnerliste des Ziels (gecacht je Lauf). Existiert der Name: gleiche Grösse **und** gleicher Teil-Hash (`PartialHash.compute`, Stufe „Wenig“) → „bereits vorhanden“, kein Upload. Seit 0.15.0 (R-F1/R-I4, `PresenceCheck`): bei `deleteMode ≠ KEEP` gilt die vorhandene Zieldatei nur dann als gesichert (`verified = true`), wenn sie mit der Profilstufe, mindestens „Mittel“, auf beiden Seiten übereinstimmt, sonst `verified = false` (keine Löschfreigabe); bei `KEEP` unverändert (DEC-021). Sonst nächster Name `bild_2.jpg`, `bild_3.jpg`, … (`FolderNames.conflictName`).
3. Danach Duplikatsuche in den **anderen Ordnern desselben Monats** (gleicher Grundname, gleiche Grösse, gleicher Teil-Hash); die Ordnerliste liefert `MonthFolderSearch` (auch Länder- und Unterordner, siehe oben).
4. Name wird in `sh.reserved` eingetragen (gegen gleichzeitige Worker), Ordner wird angelegt.
5. Upload (Abschnitt 6.1), danach Grössen- und Inhaltsprüfung (Abschnitt 6.4), dann `record()`.

> **[RISIKO] R-F1 (bis 0.14.7; seit 0.15.0 für `deleteMode ≠ KEEP` behoben, siehe oben):** Bei „bereits vorhanden“ wurde `verified = true` gesetzt, **unabhängig von der gewählten Prüfstufe** des Profils – die Gleichheit stützt sich nur auf Name, Grösse und Teil-Hash (3 × 64 KB). Ist `deleteMode ≠ KEEP`, kann daraus die Freigabe zum Löschen des Handy-Originals entstehen (Abschnitt 7.5). Das Verhalten ist bewusst (DEC-021), umgeht aber die Stufen „Mittel/Viel/Alles“.

> **Konsequenz bei bearbeiteten Fotos:** Ändern sich Grösse oder Änderungsdatum einer bereits gesicherten Datei, gilt sie als „nicht bekannt“. Ist der Inhalt auf dem PC verschieden, wird sie als `name_2.ext` zusätzlich hochgeladen; die Zeile in `synced_files` (Schlüssel `profileId + sourceKey`) wird überschrieben und zeigt danach auf die neue Datei. Die alte PC-Datei bleibt unangetastet liegen.

### 3.3 Ordner-Lauf (`MirrorEngine.runInner`)

`run()` umschliesst `runInner()` mit `MirrorLock.mutex.withLock` – ausser bei der Simulation (`MirrorEngine.kt:143`).

| # | Schritt | Details / Code |
|---|---|---|
| 1 | Lauf anlegen, alte Logs/Läufe löschen | wie Foto-Lauf |
| 2 | **Offene Schutzbremse?** | `guards.get(profileId) != null` → `block()` ohne weitere Arbeit |
| 3 | Berechtigung prüfen | `LocalTree.hasPermission` sonst `IOException` (`err_local_access_lost`) |
| 4 | Handy-Wurzel prüfen | `tree.stat(rootDocId) != null`; fehlt sie: mit vorhandenem Sync-Stand → `Guard(LOCAL_MISSING)`, ohne Stand → Fehler |
| 5 | SMB öffnen, PC-Basisordner prüfen | fehlt der Ordner: mit Sync-Stand → `Guard(REMOTE_MISSING)`; ohne Stand (Erstabgleich, nicht `dryRun`) → Ordner anlegen |
| 6 | Vorgemerkte Wiederherstellungen ausführen | `runRestores` (nur wenn keine Stichprobe ansteht); danach `runPendingPurges` (Abschnitt 7) |
| 7 | **Lokalen Bestand lesen** | `tree.listRecursive(skipTrash, recover = !dryRun)` – ausserdem `.s2pold`-Wiederherstellung (Abschnitt 6.3), dann `FileFilter`, Schlüssel = relativer Pfad **in Kleinbuchstaben** |
| 8 | **Entfernten Bestand lesen** | `s.listRecursive(s.path(), skipTrash)` mit demselben Filter |
| 9 | Konflikte laden | `dao.conflicts(profileId)` (alle, auch entschiedene) |
| 10 | **Stichprobe nach Ordnerwechsel** | `guards.pendingCheck` nicht leer: passt `folderMatches` nicht → `Guard(CHECK_FOLDER)`; sonst Vormerkung löschen |
| 11 | „Nicht löschen“-Einmalfreigabe | `guards.takeKeepFiles` → `propagate = false` für diesen Lauf |
| 12 | Papierkorb-Frist anwenden | `purgeTrash()` (nicht `dryRun`) |
| 13 | **Planen** | Schlüssel = Vereinigung aus lokal, entfernt und `mirror_state`; je Schlüssel Konfliktprüfung oder `plan(...)` → Liste von `Action` |
| 14 | **Schutzbremse für Löschungen** | `GuardPolicy.massDelete` / `vanishedFolder` auf die Löschaktionen; einmalige Freigabe `takeAllowDelete` wird **immer** verbraucht |
| 15 | Erledigte Konflikte entfernen | `dao.deleteConflict` – erst nach der Schutzbremse |
| 16 | **Phase A (sequenziell)** | `executeAdmin`: Papierkorb-Moves, `RECORD`, `DROP`, `CONFLICT` anlegen. Alle 10 Aktionen `shouldContinue()` prüfen |
| 17 | **Phase B (parallel)** | `runTransfers`: kleine Dateien zuerst, 1–4 Worker mit eigener `SmbSession`, Speicherbudget für Downloads |
| 18 | Abschluss | Status: `failed > 0` oder `spaceShortage` → `PARTIAL`; `finish(...)` |

```mermaid
flowchart TD
    S["runInner"] --> G0{"Offene Schutzbremse\nin GuardStore?"}
    G0 -->|"ja"| BL["block: Status WAITING"]
    G0 -->|"nein"| P1{"Handy-Wurzel lesbar?"}
    P1 -->|"nein, Sync-Stand vorhanden"| BL
    P1 -->|"nein, kein Stand"| ERR["IOException, Status FAILED"]
    P1 -->|"ja"| P2{"PC-Basisordner vorhanden?"}
    P2 -->|"nein, Sync-Stand vorhanden"| BL
    P2 -->|"nein, kein Stand"| MK["Ordner anlegen"]
    P2 -->|"ja"| RS["Wiederherstellungen und vorgemerktes Leeren"]
    MK --> RS
    RS --> LS["Listen: Handy via SAF, PC via SMB\nFilter, Schlüssel klein geschrieben"]
    LS --> CK{"Stichprobe ausstehend\nund passt nicht?"}
    CK -->|"ja"| BL
    CK -->|"nein"| PU["Papierkorb-Frist anwenden"]
    PU --> PL["Planen: plan() je Pfad,\nKonflikte aus DB berücksichtigen"]
    PL --> MD{"Massenlöschung oder\nUnterordner verschwunden\nund keine Freigabe?"}
    MD -->|"ja, echter Lauf"| BL
    MD -->|"nein"| PA["Phase A sequenziell:\nPapierkorb, RECORD, DROP, CONFLICT"]
    PA --> PB["Phase B parallel:\nUPLOAD, DOWNLOAD, KEEP_BOTH"]
    PB --> FN["finish: OK oder PARTIAL"]
```

### 3.4 Simulation (`dryRun = true`)

Gemeinsam: Es wird weder übertragen noch gelöscht; `log()` schreibt nichts; ein `SyncRun`-Datensatz mit `dryRun = true` wird trotzdem angelegt (Abfragen für Verlauf filtern `dryRun = 0`).

| Verhalten | Foto-Engine | Ordner-Engine |
|---|---|---|
| PC nicht erreichbar | Offline-Vorschau | Offline-Vorschau (`remote` leer; `sameContent` liefert `false`) |
| Nur die ersten N Einträge | `previewLimit` (EXIF/GPS lesen kostet Zeit) | `previewLimit` |
| Schutzbremse | – | Warnung im Ergebnistext, **kein** Blockieren (`massWarn`) |
| Papierkorb leeren, Wiederherstellen, `.s2pold`-Recovery, Konflikte speichern | – | werden **nicht** ausgeführt |
| Sperre | – | `MirrorLock` wird nicht genommen |

### 3.5 Laufstatus (`RunStatus`, `data/Entities.kt`)

```mermaid
stateDiagram-v2
    [*] --> RUNNING: insertRun
    RUNNING --> OK: alles erledigt
    RUNNING --> PARTIAL: Dateifehler, WLAN weg, Speichermangel
    RUNNING --> FAILED: nicht-Netzwerk-Ausnahme
    RUNNING --> INTERRUPTED: Netzwerkfehler, Android-Stopp, closeDanglingRuns
    RUNNING --> CANCELLED: Abbruch durch Benutzer
    RUNNING --> WAITING: Schutzbremse hat angehalten
    OK --> [*]
    PARTIAL --> [*]
    FAILED --> [*]
    INTERRUPTED --> [*]
    CANCELLED --> [*]
    WAITING --> [*]
```

`WAITING` entsteht nur in der Ordner-Engine. `INTERRUPTED` wegen toter App entsteht ausschliesslich über `closeDanglingRuns`
im **nächsten** `SyncWorker`-Lauf.

---

## 4. 3-Wege-Abgleich (Ordner-Profile)

### 4.1 Die drei Vergleichsstände

| Stand | Bedeutung | Herkunft im Code |
|---|---|---|
| **L** (lokal) | aktueller Zustand der Datei im Handy-Ordner | `LocalTree.listRecursive` → `LocalEntry(size, modified)`; Map `local` |
| **R** (remote) | aktueller Zustand der Datei auf dem PC | `SmbSession.listRecursive` → `RemoteEntry(size, modified)`; Map `remote` |
| **S** (Stand) | Zustand beider Seiten beim **letzten erfolgreichen** Abgleich dieser Datei | Tabelle `mirror_state`: `localSize/localModified/remoteSize/remoteModified`; Map `states` |

**Wichtig – abweichend vom Lehrbuch:** S speichert **zwei getrennte Zustände** (einen je Seite), nicht einen einzigen gemeinsamen Ausgangsinhalt. Eine Seite wird immer **nur mit ihrem eigenen früheren Zustand** verglichen (`L` gegen `S.local*`, `R` gegen `S.remote*`). Zeitstempel und Grössen der beiden Seiten werden **nicht gegeneinander** verglichen – ausser (a) bei `ConflictMode.NEWER` (Abschnitt 5.3) und (b) bei der Grössengleichheit vor dem Inhaltsvergleich. Das macht den Vergleich robust gegen die Tatsache, dass das Handy per SAF kein Änderungsdatum setzen kann (DEC-035/137).

Es gibt **keinen** gespeicherten gemeinsamen Inhalts-Hash. „Gleicher Inhalt“ wird bei Bedarf frisch berechnet (Abschnitt 8.3).

### 4.2 Wie S fortgeschrieben wird (und wann nicht)

`record(key, l, r)` (`MirrorEngine.kt:805`) schreibt `MirrorState` (Upsert). Aufrufstellen:

| Wo | Wann |
|---|---|
| `executeAdmin(RECORD)` | Inhalt auf beiden Seiten gleich (Hash) – nur Stand merken, keine Übertragung |
| `upload()` | nach erfolgreichem Upload: Handy-Eintrag `l` aus der Liste, PC-Eintrag per `s.stat` **nach** dem Umbenennen |
| `download()` | nach erfolgreichem Download: Handy-Eintrag per `tree.find` (Ordnerliste), PC-Eintrag `r` aus der Liste |

S wird **gelöscht** (`deleteMirrorState`) bei: `DROP`, `TRASH_LOCAL`, `TRASH_REMOTE`, **vor** jeder Übertragung mit Papierkorb der Gegenseite
(`trashOther`), vor `KEEP_BOTH`, zu Beginn einer Wiederherstellung, durch `FolderFreer` (Speicher freigeben) und durch „Als neu abgleichen“
(`clearMirrorState`).

S wird **nie** fortgeschrieben, wenn eine Übertragung übersprungen (`skipChanged`), fehlgeschlagen oder wegen Speichermangel ausgelassen wurde **[IMPL]**.
Ein offener Konflikt lässt S unverändert.

### 4.3 Änderungserkennung

```text
changedL = (S fehlt) ODER L.size != S.localSize ODER L.modified != S.localModified
changedR = (S fehlt) ODER R.size != S.remoteSize ODER R.modified != S.remoteModified
```

Vergleich ist **exakte Gleichheit** (`MirrorEngine.kt:356`). Es gibt keine Toleranz für Zeitstempel-Granularität (Abschnitt 8.4).
Gelöscht ist eine Datei, wenn sie in der eigenen Seitenliste fehlt, aber S existiert.
Neu ist eine Datei, die auf einer Seite vorkommt, während S fehlt.

### 4.4 Entscheidungsbaum von `plan()` (`MirrorEngine.plan`)

```mermaid
flowchart TD
    A["plan: l, r, st"] --> B{"l und r fehlen?"}
    B -->|"ja, st vorhanden"| DROP1["DROP: Stand löschen"]
    B -->|"ja, st fehlt"| NONE0["nichts"]
    B -->|"nein"| C{"nur l vorhanden?"}
    C -->|"ja"| D{"st fehlt?"}
    D -->|"ja"| D1{"dir.up?"}
    D1 -->|"ja"| UP1["UPLOAD"]
    D1 -->|"nein"| NONE1["nichts"]
    D -->|"nein: PC hat gelöscht"| E{"Richtung?"}
    E -->|"UPLOAD"| UP2["UPLOAD: auf PC wiederherstellen"]
    E -->|"anders"| E2{"propagate?"}
    E2 -->|"ja"| TL["TRASH_LOCAL"]
    E2 -->|"nein"| E3{"BOTH?"}
    E3 -->|"ja"| UP3["UPLOAD: wiederherstellen"]
    E3 -->|"nein, DOWNLOAD"| DROP2["DROP: Handy-Kopie bleibt"]
    C -->|"nein"| F{"nur r vorhanden?"}
    F -->|"ja"| G{"st fehlt?"}
    G -->|"ja"| G1{"dir.down?"}
    G1 -->|"ja"| DN1["DOWNLOAD"]
    G1 -->|"nein"| NONE2["nichts"]
    G -->|"nein: Handy hat gelöscht"| H{"Richtung?"}
    H -->|"DOWNLOAD"| DN2["DOWNLOAD: aufs Handy wiederherstellen"]
    H -->|"anders"| H2{"propagate?"}
    H2 -->|"ja"| TR["TRASH_REMOTE"]
    H2 -->|"nein"| H3{"BOTH?"}
    H3 -->|"ja"| DN3["DOWNLOAD: wiederherstellen"]
    H3 -->|"nein, UPLOAD"| DROP3["DROP: PC-Kopie bleibt"]
    F -->|"nein: beide vorhanden"| I{"changedL und changedR\nbeide false?"}
    I -->|"ja"| NONE3["nichts"]
    I -->|"nein"| J{"st fehlt ODER\nbeide geändert?"}
    J -->|"ja"| K{"sameContent?\nGrösse gleich + gründlicher Hash"}
    K -->|"gleich"| REC["RECORD: nur Stand merken"]
    K -->|"verschieden"| L{"Richtung?"}
    L -->|"UPLOAD"| UPT["UPLOAD + PC-Version in Papierkorb"]
    L -->|"DOWNLOAD"| DNT["DOWNLOAD + Handy-Version in Papierkorb"]
    L -->|"BOTH"| M{"conflictMode"}
    M -->|"NEWER"| N{"l.modified >= r.modified?"}
    N -->|"ja"| UPT
    N -->|"nein"| DNT
    M -->|"ASK"| CF["CONFLICT: Eintrag in mirror_conflicts"]
    J -->|"nein: nur eine Seite"| O{"wouldTransfer und Grösse gleich\nund sameContent?"}
    O -->|"ja"| REC
    O -->|"nein"| P{"welche Seite geändert?"}
    P -->|"links"| Q{"dir.up?"}
    Q -->|"ja"| UPK["UPLOAD, Papierkorb nur bei keepVersions"]
    Q -->|"nein"| NONE4["nichts"]
    P -->|"rechts"| R2{"dir.down?"}
    R2 -->|"ja"| DNK["DOWNLOAD, Papierkorb nur bei keepVersions"]
    R2 -->|"nein"| NONE5["nichts"]
```

`dir.up` = Richtung ≠ `DOWNLOAD`; `dir.down` = Richtung ≠ `UPLOAD` (`SyncDirection`, `Entities.kt`).

### 4.5 Entscheidungstabellen

**Tabelle A – beide Seiten vorhanden** (S = Stand vorhanden/fehlt; „geändert“ siehe 4.3)

| S | L geändert | R geändert | Ergebnis |
|---|---|---|---|
| vorhanden | nein | nein | keine Aktion |
| vorhanden | ja | nein | Ist `dir.up`, Grösse gleich und Inhalt gleich → `RECORD`. Sonst `dir.up` → `UPLOAD` (PC-Version in Papierkorb **nur mit `keepVersions`**); sonst keine Aktion |
| vorhanden | nein | ja | Spiegelbildlich mit `dir.down` → `RECORD` / `DOWNLOAD` (Handy-Version in Papierkorb nur mit `keepVersions`) / keine Aktion |
| vorhanden | ja | ja | Inhalt gleich → `RECORD`. Sonst nach Richtung (Tabelle C) |
| fehlt | (ja) | (ja) | Erstabgleich dieser Datei: wie „beide geändert“ |

**Tabelle B – eine Seite fehlt**

| L | R | S | UPLOAD | DOWNLOAD | BOTH |
|---|---|---|---|---|---|
| ja | nein | fehlt (neu auf Handy) | `UPLOAD` | keine Aktion | `UPLOAD` |
| nein | ja | fehlt (neu auf PC) | keine Aktion | `DOWNLOAD` | `DOWNLOAD` |
| ja | nein | vorhanden (**PC hat gelöscht**) | `UPLOAD` (wiederherstellen, auch mit `propagate`) | `propagate` → `TRASH_LOCAL`, sonst `DROP` | `propagate` → `TRASH_LOCAL`, sonst `UPLOAD` |
| nein | ja | vorhanden (**Handy hat gelöscht**) | `propagate` → `TRASH_REMOTE`, sonst `DROP` | `DOWNLOAD` (wiederherstellen) | `propagate` → `TRASH_REMOTE`, sonst `DOWNLOAD` |
| nein | nein | vorhanden | `DROP` | `DROP` | `DROP` |

`propagate` = `Profile.propagateDeletes` (Standard **aus**), kann pro Lauf durch „Nicht löschen“ (`takeKeepFiles`) auf `false` gesetzt werden.

**Tabelle C – Inhalt verschieden, beide Seiten betroffen (Erstabgleich oder beide geändert)**

| Richtung | `conflictMode` | Ergebnis |
|---|---|---|
| UPLOAD | egal | Handy gewinnt: `UPLOAD`, PC-Version **immer** in den PC-Papierkorb (`trashOther = true`, unabhängig von `keepVersions`) |
| DOWNLOAD | egal | PC gewinnt: `DOWNLOAD`, Handy-Version **immer** in den Handy-Papierkorb |
| BOTH | `NEWER` | `l.modified >= r.modified` → `UPLOAD`, sonst `DOWNLOAD`; die unterlegene Version immer in den Papierkorb. Bei Gleichstand gewinnt das **Handy** |
| BOTH | `ASK` | `CONFLICT` – Eintrag in `mirror_conflicts`, Datei wartet (Abschnitt 5) |

### 4.6 Theorie gegen Implementierung

| Aspekt | Theoretischer 3-Wege-Merge | Tatsächlich implementiert |
|---|---|---|
| Basisstand | ein gemeinsamer Ausgangsinhalt | zwei seitenspezifische Metadaten-Stände (Grösse + Änderungszeit), kein Inhalt |
| Konflikt | beide ≠ Basis und L ≠ R | beide ≠ S **und** Inhalt (Hash) verschieden; Gleichheit wird nur bei gleicher Grösse geprüft |
| Löschen gegen Ändern | Konflikt | **kein Konflikt** – die Löschung gewinnt, die geänderte Version landet im Papierkorb (7.3, **R-D1**) |
| Umbenennen/Verschieben | wird oft erkannt | **nicht erkannt** – Löschung + Neuanlage (7.4, **R-D2**) |
| Textmerge | optional | nicht vorhanden – nur ganze Dateien |
| Ordner | Teil des Baums | nur Dateien; leere Ordner und Ordnerlöschungen werden nicht synchronisiert |
| Gleichheit trotz Änderung | – | `RECORD`: Zeitstempel-Verschiebung ohne Inhaltsänderung erzeugt keine Übertragung |
| Einweg-Richtungen | – | die „andere“ Seite wird bei einseitiger Änderung ignoriert (nur Wiederherstellen bzw. `DROP`) |

---

## 5. Konflikterkennung und Konfliktbehandlung

### 5.1 Konfliktarten

Implementiert sind folgende Konfliktfälle:

| Art | Bedingung | Ergebnis |
|---|---|---|
| **K1 – Beidseitige Änderung** (Ordner, `BOTH`, `ASK`) | `changedL && changedR` (oder Erstabgleich), Inhalt verschieden | `Action.CONFLICT`, Eintrag `mirror_conflicts` mit `resolution = NULL` |
| **K2 – Beidseitige Änderung** (`BOTH`, `NEWER`) | wie K1 | automatisch: neuere Datei gewinnt, Unterlegene in Papierkorb |
| **K3 – Beidseitige Änderung, Einweg-Profil** | wie K1, Richtung `UPLOAD`/`DOWNLOAD` | kein Konflikt: die Quelle gewinnt, Gegenseite in Papierkorb |
| **K4 – Änderung während des Laufs** | Zieldatei hat sich zwischen Einlesen und Schreiben verändert | Datei wird übersprungen (`skipChanged`, Log-Warnung), nächster Lauf sieht beide Stände |
| **K5 – Ziel entstand während des Laufs** | Download: Handy-Datei gab es beim Einlesen nicht, ist aber inzwischen da (`ChangedDuringRunException`) | Download wird abgebrochen und als „geändert“ übersprungen |
| **K6 – Namenskollision Fotos** | gleicher Zielname, anderer Inhalt | kein Konflikt-Dialog: `name_2.ext`, `name_3.ext` … |

Es gibt **keine** Konflikterkennung für: Löschen-gegen-Ändern (Abschnitt 7.3), Umbenennen gegen Ändern, Ordnerkonflikte,
unterschiedliche Gross-/Kleinschreibung (Abschnitt 8.5).

### 5.2 Kriterien

- **Auslöser:** beide Seiten seit S verändert (4.3).
- **Inhaltsvergleich** (`MirrorEngine.sameContent`): ungleiche Grösse → sofort „verschieden“, **ohne** Hash; gleiche Grösse → Inhaltshash auf beiden Seiten: ≤ 32 MB ganze Datei; **seit 0.15.0 (E1, R-I3) darüber ebenfalls vollständiger Hash beider Seiten** (`ContentCompare`, `PartialHash.comparison`), abbrechbar und mit Fortschritt; Lesefehler oder Abbruch → „nicht gleich“. Bis 0.14.7 galten darüber 32 Stichproben à 64 KB, deren Treffer in `plan()` als `RECORD` gewertet wurde (Änderung zwischen den Stichproben blieb unentdeckt). Ohne SMB-Verbindung → `false`.
- **Zeitstempel** entscheiden nur bei `NEWER` (und im Dialog „Alle: neuere“). Sie stammen aus **verschiedenen Uhren/Anbietern** (SAF `lastModified` gegen SMB `lastWriteTime`).

> **[RISIKO] R-K1:** `NEWER` vergleicht `l.modified >= r.modified` über zwei unabhängige Quellen. Uhrenabweichung zwischen Handy und PC oder ein SAF-Anbieter, der beim Download die Schreibzeit statt der Originalzeit liefert, kann die „falsche“ Datei gewinnen lassen. Die unterlegene Version wird immer in den Papierkorb gelegt, ist also wiederherstellbar.

### 5.3 Konfliktlösungsstrategien

| Strategie | Wo gewählt | Wirkung im nächsten Lauf (`MirrorEngine.kt:236`) |
|---|---|---|
| **Handy behalten** (`KEEP_LOCAL` = `"LOCAL"`) | `ConflictScreen` | `UPLOAD`, PC-Version in Papierkorb |
| **PC behalten** (`KEEP_REMOTE` = `"REMOTE"`) | `ConflictScreen` | `DOWNLOAD`, Handy-Version in Papierkorb |
| **Beide behalten** (`KEEP_BOTH` = `"BOTH"`) | `ConflictScreen` | Handy-Datei → `<name>_conflict-phone.<ext>` (bzw. `_conflict-phone-2` … , siehe 5.6), diese hochladen, PC-Version unter Originalnamen herunterladen |
| **Alle: neuere** | `ConflictScreen` (Knopf oben) | je Konflikt `KEEP_LOCAL`, wenn gespeichertes `localModified >= remoteModified`, sonst `KEEP_REMOTE` (Werte vom **Erkennungszeitpunkt**) |
| **Immer neuere** | Profil-Option `ConflictMode.NEWER` | automatisch ohne Rückfrage (Tabelle C) |

Vollautomatisch entschieden wird: K2, K3, K4, K5. Benutzereingriff erfordert: K1 (`ASK`).
Ein offener Konflikt (`resolution == NULL`) lässt die betroffene Datei **unberührt** (`continue`), alle anderen Dateien laufen weiter.

### 5.4 Speicherung und Weiterverarbeitung

```mermaid
stateDiagram-v2
    [*] --> Offen: Phase A legt Konflikt an, resolution leer
    Offen --> Entschieden: ConflictScreen setzt resolution LOCAL, REMOTE oder BOTH
    Entschieden --> Planung: nächster Lauf, beide Seiten noch vorhanden
    Planung --> Geloescht: deleteConflict (nach Schutzbremse, vor Ausführung)
    Offen --> Geloescht: eine Seite ist weg, normal weiterplanen
    Geloescht --> [*]
```

- Tabelle `mirror_conflicts` (Schlüssel `profileId + relPath` in Originalschreibweise); Abgleich mit Dateien über `relPath.lowercase()`.
- Eine Entscheidung stösst per `SyncScheduler.enqueueAuto` einen Lauf an; ausgeführt wird sie dort, nicht im Dialog.
- `dao.deleteConflict` läuft **vor** der Ausführung der Übertragung (`MirrorEngine.kt:266`).
- **Rückgängig atomar (seit 0.16.0, DEC-232):** `ConflictScreen` macht eine Entscheidung nur über `undoResolution(profileId, relPath, expected)` rückgängig (`UPDATE … SET resolution = NULL … AND resolution = :expected`); gezählt wird nur Ergebnis 1, die Zeile wird nie wiederbelebt (kein `upsertConflict` mehr). Die Engine löscht die Zeile für Aktionen aus einer Konfliktentscheidung über `deleteConflictIf(profileId, relPath, expected)` (`DELETE … AND resolution = :expected`); liefert das 0 (der Benutzer hat rückgängig gemacht), entfernt sie die Aktion aus `actions` und führt sie nicht aus. Konflikte, bei denen eine Seite fehlt, werden weiterhin unbedingt gelöscht. Der Sync startet nach einer Entscheidung weiterhin sofort.

> **[RISIKO] R-K2:** Wird nach dem Löschen der Konfliktzeile und vor/während der Übertragung der Lauf unterbrochen (Netz weg, App beendet), ist die Entscheidung des Benutzers **verloren**. Beim nächsten Lauf gilt die Datei weiterhin als „beide geändert“ und wird erneut als Konflikt gemeldet. Es entsteht kein Datenverlust, aber der Benutzer muss erneut entscheiden.

> **[RISIKO] R-K3:** Der Benutzer sieht Grösse/Datum vom Erkennungszeitpunkt. Wird eine Seite danach nochmals geändert, gilt seine Wahl für die **aktuelle** Version dieser Seite. Die verworfene Seite landet in jedem Fall im Papierkorb (`trashOther = true`).

### 5.5 Auswirkungen auf Dateien

| Entscheidung | Handy-Datei | PC-Datei |
|---|---|---|
| Handy behalten / Handy neuer | bleibt | wird überschrieben; alte Version unter `.LeoSync-Bin\<Datum>\…` |
| PC behalten / PC neuer | wird ersetzt; alte Version in `.LeoSync-Bin/<Datum>/…` | bleibt |
| Beide behalten | Original wird zu `*_conflict-phone.*` bzw. `*_conflict-phone-2.*` … (neue Datei im Sync-Stand), Originalname bekommt PC-Inhalt | Konfliktkopie neu hochgeladen (nie überschreibend), Originalname bleibt |

### 5.6 Sonderfälle

- Eine der beiden Seiten fehlt, obwohl ein Konflikt offen ist → Konfliktzeile wird verworfen, die Datei normal geplant (`MirrorEngine.kt:247`).
- `KEEP_BOTH` prüft **nicht**, ob sich Handy- oder PC-Datei seit dem Einlesen geändert haben.
- **`KEEP_BOTH` seit 0.16.0 (DEC-239, SYNC-20261111-171) [IMPL]:** (1) *Freier Name:* `ConflictName` wählt `name_conflict-phone.ext`, sonst `-2`, `-3` … bis 100 (danach Fehler ohne Änderung); der Name darf weder auf dem Handy (`tree.find`) noch auf dem PC (`stat`) noch in den eingelesenen Listen vorkommen. (2) *Anbieter-Name prüfen:* Nach `tree.rename` wird der Name des zurückgegebenen Dokuments gelesen; weicht er vom geplanten ab (der Speicher-Anbieter hängt « (1)» an), wird nicht übertragen, die Datei bleibt umbenannt, und der nächste Lauf überträgt sie als neu. Hochgeladen wird nur die Datei aus dem Ergebnis des Umbenennens. (3) *Nie überschreibend:* Upload mit `replaceExisting = false`; eine vorhandene PC-Datei führt zu einem Fehler statt zum Überschreiben (vorher konnte eine gleichnamige PC-Datei ohne Papierkorb überschrieben werden). (4) *Sync-Stand* des Originalschlüssels wird sofort nach dem Umbenennen vergessen. (5) *Wiederholbar:* Die Merker `renamedByRun` und `uploadedByRun` (nur für den laufenden Lauf, wie `trashedByRun`) verhindern bei der Wiederholung nach Verbindungsabbruch ein zweites Umbenennen und einen bereits fertigen Upload; fortgesetzt wird mit dem gemerkten Namen und dem Download der PC-Version unter dem Originalnamen.
- `KEEP_BOTH`: Scheitert der Download der PC-Version nach dem Umbenennen und gibt es keine Wiederholung, fehlt der Originalname auf dem Handy. Der Stand für den Originalschlüssel wurde vorher gelöscht; im nächsten Lauf gilt die PC-Datei als „neu auf dem PC“ und wird (bei `dir.down`, hier immer `BOTH`) geladen.
- Konflikte können nur in `BOTH` entstehen; in Einweg-Profilen nie.
- `MirrorConflict.localModified/remoteModified` sind nur Anzeigedaten (Dialog, „Alle: neuere“), keine Beweislage für die Ausführung.

---

## 6. Dateioperationen und Übertragung

### 6.0 PC-Erkennung vor dem Anmelden (DEC-204, DEC-205; im Code seit 0.13.0, Lernen überarbeitet in 0.13.1; APK 0.13.0 und 0.13.1 in `releases\` vorhanden, Gerätetest ausstehend, Stand 2026-11-09)

Je Profil wählt `Profile.pcCheck` (`PcCheckMode`) die Erkennung: `WIFI` (nur WLAN-Name, Standard und heutiges Verhalten), `GUID` (nur PC-Kennung = SMB-Server-GUID) oder `BOTH` (beides muss erfüllt sein). Die Kennung wird nach dem Aushandeln gelesen, **vor** `authenticate`; Zugangsdaten gehen nur bei einer zulässigen Entscheidung an den Server.

**Ablauf (`SyncWorker`, `SmbSession`):**
1. TCP-Port 445 (`SmbSession.isReachable`, wie bisher, Abschnitt 2 Schritt 6).
2. **Vorab-Prüfung** nur bei `GUID`/`BOTH` (`SyncWorker.precheckPc` → `SmbSession.checkPc`): nur aushandeln und entscheiden, keine Anmeldung, **kein Lernzweig** (seit 0.13.1). Ergebnis: lauffähig / nicht erreichbar (Netzwerkfehler) / abgewiesen (`PcIdentityException`-Entscheidung) / sonstiger Fehler (wird wie «abgewiesen» behandelt, siehe unten). `WIFI` überspringt die Vorab-Prüfung. Je Host wird einmal je Lauf ausgehandelt (Host-Zwischenspeicher `PcSeen`, nur im Speicher, keine Datenbankänderung); die Entscheidung fällt je Profil mit dessen eigener gespeicherter Kennung.
3. **Anmeldeweg:** der einzige `authenticate`-Aufruf steht in `smb/PcLogin.kt` (Anmeldelogik hinter den Schnittstellen `PcLink`/`PcTransport`; `SmbSession` ist nur noch der smbj-Adapter, `SmbSession.connectChecked` bleibt der Zugang für `open` und `listShares`): `connect` → Kennung lesen (`getConnectionContext().getServerGuid()`) → `PcIdentity.decide` gegen den aktuellen DB-Stand (Profil per ID neu gelesen, der strengere Modus von Kopie und Datenbank, `currentIdentity`; ungespeichertes Profil: Werte der Kopie, ohne Lernen) → erst bei zulässiger Entscheidung `authenticate`. Sonst wird die Verbindung geschlossen und `PcIdentityException` geworfen (keine Zugangsdaten gesendet). Die Datenbank erreicht `SmbSession` über die Schnittstelle `PcStore` (`RoomPcStore`, gesetzt in `App.kt`).
4. **Lernen (nur Zugriffsart `SETUP`, DEC-205):** erst nach erfolgreicher Anmeldung **und** `connectShare` (bzw. Freigabenliste) schreibt der Commit die Kennung per bedingtem UPDATE (`SyncDao.learnPcGuid`: nur bei leerer Kennung oder anderem Host, mit Moduswächter `pcCheck = :mode`; nie still überschreibend). Gelernt wird nur an zwei Stellen: (a) nach dem Speichern des Profils (`PcLearner.learnAfterSave`, app-weiter Scope, nach dem Schreiben in der Datenbank, 30 s je SMB-Vorgang; Speichern gelingt immer; im Dialog «Wie weiter?» erscheint «PC wird erkannt …»), (b) in «Verbindung testen» (`SmbSession.test` mit `SETUP`). «Freigaben/Ordner wählen» meldet sich unter `SETUP` an, lernt aber nie. Ein automatischer Test beim Moduswechsel entfällt. Lernen und Neuvertrauen nur, wenn der Host der Verbindung dem gespeicherten Host des Profils entspricht.
5. **Kennungswechsel (Neuvertrauen, nur `SETUP`):** aushandeln, Kennung lesen, Verbindung **schliessen**, Benutzer fragen (`onChangedGuid`, Host, alte/neue Kennung), bei «Ja» neu verbinden und prüfen, dass die gesehene Kennung **genau der bestätigten** entspricht (sonst abbrechen, nicht anmelden), erst dann `authenticate`, danach `trustNewPcGuid` (gilt für alle Profile mit gleichem Host und gleicher alter Kennung). «Nein»/Zeitüberschreitung = nicht vertrauen.

**Grundsatz «`WIFI`-Kennung erzeugt nie Vertrauen» (V7):** eine im Modus `WIFI` still gelernte Kennung gilt bei `GUID`/`BOTH` als leer. Zentral in `currentIdentity` (`trustedStored`: gespeicherte Kennung zählt nur, wenn der DB-Modus nicht `WIFI` ist, wenn der wirksame Modus `GUID`/`BOTH` ist) und im Editor in `save()` über `PcEditorRules.keepStoredOnSave` (Kennung bleibt nur, wenn Host und Modus unverändert; sonst verworfen). Das Lernen im Modus `WIFI` bleibt still (Meldung aus -055), erzeugt aber nie Vertrauen.

**Regel `PcIdentity.decide(mode, pcGuid, pcGuidHost, host, seenGuid, access, learnNetOk)`** (reine Funktion; `PcAccess.AUTO` = automatisch, `USER` = vom Benutzer ausgelöst (manueller Sync, Ansichten), `SETUP` = Profil-Editor):

| Entscheidung | Bedingung |
|---|---|
| `ALLOW` | `WIFI` (mit bekannter oder fehlender Kennung); oder `GUID`/`BOTH` mit gespeicherter, gleicher Kennung |
| `ALLOW_AND_LEARN` | `GUID`/`BOTH` ohne gespeicherte Kennung für den Host, nur bei `SETUP` **und** `learnNetOk`; `WIFI` lernt weiterhin still (ohne Vertrauenswirkung, siehe Grundsatz) |
| `WRONG_NET` | wie `ALLOW_AND_LEARN`, aber `learnNetOk` nicht erfüllt: nicht anmelden (eigener Text `smb_err_pc_wrong_net`) |
| `UNKNOWN` | `GUID`/`BOTH` ohne gespeicherte Kennung bei `AUTO`/`USER`: nicht anmelden (Hinweis «Profil öffnen → Verbindung testen», `smb_err_pc_unknown`) |
| `CHANGED` | gespeicherte Kennung weicht von der gesehenen ab: nicht anmelden; Neuvertrauen nur durch den Benutzer unter `SETUP` (Ablauf Schritt 5, nur bei gespeichertem Profil) |
| `NO_ID` | `GUID`/`BOTH`, der Server liefert keine brauchbare Kennung (leer oder Null-UUID): nie anmelden |

**Netzbedingung `learnNetOk`** (reine Funktion `NetworkRules.learnNetOk`, im Aufrufer ermittelt): `BOTH` → WLAN-Name lesbar und in der Liste des Profils (unlesbar = nicht erfüllt); `GUID` → immer erfüllt. Einrichtungsausnahme (Restrisiko R1, DEC-205): unter `SETUP` wird bei `GUID` ohne bekannte Kennung in jedem Netz angemeldet, bei `BOTH` nur im eingetragenen WLAN. Für **bestehende** Profile mit Hostwechsel ist R1 durch eine Rückfrage vor dem Senden der Zugangsdaten geschlossen (DEC-208, `PcEditorRules.needsHostConfirm`: Modus `GUID`/`BOTH`, Host gegenüber gespeichertem Stand geändert, in der Sitzung noch nicht bestätigt; greift vor Speichern, 
Eine gespeicherte Kennung gilt nur, wenn `pcGuidHost` dem aktuellen Host entspricht (ohne Gross-/Kleinschreibung), sonst gilt sie als unbekannt.

**`PcIdentityException`** ist bewusst keine `IOException` und kein Netzwerkfehler (`isNetworkError` wertet sie als `false`): ein Wiederholen ändert nichts, es wurden keine Zugangsdaten gesendet. Die Vorschau (`SyncEngine`) fängt sie sicher als Offline ab.

**Verhalten bei Abweisung (`SyncWorker`, seit 0.13.1):** das Profil wird übersprungen (`noteSkip` mit `skip_pc_not_recognised`), **kein** FAILED-Lauf. Das gilt auch für Fehler der Vorab-Aushandlung (`PcPre.Failed`, z. B. nur SMB1, unerwarteter Status, Kennung nicht lesbar): eine Meldung je Profil (`GlobalSettings.pcNotified`; der Merker wird gelöscht, sobald das Profil wieder lauffähig ist), keine Dauerbenachrichtigung, kein stilles Überspringen; manuell: Benachrichtigung mit Hinweis. **Kein Timer und kein `shareRetryAt`** für abgewiesene Profile (Port 445 antwortet, die Abweisung ändert nur der Benutzer); sie werden beim nächsten ohnehin stattfindenden Lauf mit einer einzelnen Aushandlung neu bewertet. Die mehrstufige Wartezeit `reachabilityWaits` betrifft nur Port 445 (nicht erreichbar). Der Timer für nicht erreichbare PCs (`handleUnreachable`: 30 min, mitten im Lauf 2 min, ausserhalb des WLAN 2 h) wird nur gesetzt, wenn **kein** Profil lief (frühe Rückkehr) bzw. wenn die Verbindung mitten im Lauf abbrach; ein erreichbares Profil blockiert die anderen nicht (DEC-126/DEC-048 unverändert). Ein Fortsetzen-Auftrag mit nur abgewiesenen Profilen räumt «Pausiert» auf. Die Trigger sind global (Content-Trigger, periodischer Poll, WLAN-Empfänger), nicht je Profil; die Logik fügt keine hinzu, nur eine Aushandlung je Host und Lauf.

**Eignung je Modus:** `NetworkRules.needsWifiName(mode)` (nur `GUID` braucht keinen Namen), `eligible`, `mayContinue(mode, …)` (bei `GUID` nur die erlaubte Netzart) und `wifiTriggerMatches` (`GUID` = jedes WLAN; Parameter `unreadableMatches` für den Fall ohne lesbaren Namen). Genutzt von `SyncWorker` (das 15-s-Warten auf den WLAN-Namen nur, wenn ein Profil ihn braucht), `Receivers` (`WifiNetworkReceiver`, `unreadableMatches = true`) und `PauseService` (Fortsetzen, `unreadableMatches = false`). Bei `WIFI`/`BOTH` gilt ein unlesbarer Name als nicht erfüllt, nie stillschweigend als erfüllt (Ausnahme laut Code: `WifiNetworkReceiver` wertet einen unlesbaren Namen als Treffer, `unreadableMatches = true`). Der Standortzugriff (`Permissions`) wird nur bei `WIFI`/`BOTH` mit WLAN-Namen verlangt. Manueller Sync: `WIFI` ohne Prüfung, `GUID`/`BOTH` mit Vorab-Prüfung; er lernt **nie** (seit 0.13.1, DEC-205): ohne bekannte Kennung `UNKNOWN`, keine Anmeldung.

**Nicht über die Vorab-Prüfung:** Papierkorb, Wiederherstellen, Speicher freigeben (`TrashCleaner`, `RestoreManager`, `FolderFreer`, `SpaceFreer`, `MirrorGuard`) und die Engines nutzen `AUTO`; ohne bekannte Kennung melden sie sich bei `GUID`/`BOTH` nicht an. `MirrorGuard`/`MirrorEngine` und `SyncEngine` laufen über denselben Anmeldeweg.

**Sicherheitsaussage:** Die Kennung ist nicht geheim (unverschlüsselt in der Aushandlung). `GUID`/`BOTH` schützen vor einem fremden Server in einem fremden Netz mit gleicher IP, nicht vor einem gezielten Angreifer, der die Kennung zuvor im eigenen Netz las. `WIFI` behält die bekannte Schwäche (fremdes Netz mit gleichem Namen). Restrisiken: Akku bei `GUID` in fremden WLANs (Prüfung alle 30 min), Stabilität der Kennung bei Samba/NAS (ungeprüft).

> **Umgesetzt in 0.13.1 (DEC-205; APK 0.13.1 in `releases\` vorhanden, Gerätetest nicht dokumentiert):** Beschreibung oben = Stand des Codes laut Development SYNC-20261109-055. Offen: Gerätetest (QA). Bekannte Restpunkte: Ein sofortiger manueller Start direkt nach dem Speichern kann das Lernen knapp verpassen (R5, einmalig «PC unbekannt»); manueller Sync und wiederhergestellte Sicherungen lernen nicht mehr (R3); das Zeitlimit von 30 s gilt je SMB-Vorgang, nicht gesamt; die Einrichtungsausnahme gilt bei Ersteinrichtung weiterhin ohne Rückfrage; bei Hostwechsel eines bestehenden Profils fragt die App seit 0.13.14 (DEC-208, gebaut; Sicherheits-QA -081 bestanden nach Code und Emulator-Gerätetest, Restrisiken: „Speicher freigeben“ bei fremdem PC, EN, Dunkelmodus, Fremd-WLAN; UI-Verdrahtung ohne Unit-Test; der Picker bleibt auf dem Host des Profils, `onPick` übernimmt nur den Pfad).

### 6.1 Hochladen (`SmbSession.upload`) **[IMPL]**

1. Zieldatei `<finalPath>.s2ptmp` (Konstante `SmbSession.TEMP_SUFFIX`).
2. Vorhandene Temp-Datei: Fortsetzen nur wenn `TransferPlan.resumeOffset(tmpSize, tmpModified, size, modifiedMillis)` > 0 (Temp kleiner als Original **und** Original-Änderungszeit ≤ Temp-Änderungszeit; unbekannte Zeiten → 0) **und** ein `expectedHash` übergeben wurde; sonst `offset = 0` und `FILE_OVERWRITE_IF`.
3. Quelle ab `offset` öffnen, in 1-MB-Blöcken lesen/schreiben; vor jedem Block `checkCancel()`.
4. `offset != size` → `IOException` (unvollständig).
5. Nach Fortsetzen: Teil-Hash der Temp-Datei muss `expectedHash()` entsprechen, sonst `deleteOnClose` + `IOException` (nächster Versuch beginnt neu).
6. Änderungs-/Erstelldatum der Temp-Datei auf das Originaldatum setzen (Fehler ignoriert: „nice to have“).
7. `f.rename(finalPath, replaceExisting)` – Foto: `false` (kein Überschreiben), Ordner: `true`.
8. Ist die Handy-Quelle nicht lesbar (seit 0.16.15, -203), wird die `.s2ptmp` entfernt, statt als 0-Byte-Datei liegen zu bleiben.

Zugriffe: Foto-Engine `openAt(item, offset)` über `ContentResolver` (mit `setRequireOriginal` für GPS), Ordner-Engine `tree.openInput(docId, offset)`.

### 6.2 Herunterladen (`LocalTree.writeFileResumable`, und `SmbSession.download`) **[IMPL]**

1. Elternordner anlegen (`ensureDir`, `@Synchronized`), Geschwisterliste lesen.
2. `expectNew` (Datei gab es beim Einlesen nicht) und Datei existiert inzwischen → `ChangedDuringRunException`.
3. Vorhandene `<name>.s2ptmp` auf dem Handy: `TransferPlan.resumeOffset(...)`; >0 → Anhängen im Modus `"wa"` (Anbieter ohne Unterstützung: Temp löschen, neu beginnen); sonst Temp löschen und neu.
4. Schreiben in die Temp-Datei, danach **Grössenprüfung** (`stat(...).size == expectedSize`); Abweichung → Temp löschen, `IOException`.
5. `replaceWith(...)`: siehe 6.3.

> **[RISIKO] R-T1:** Das Fortsetzen von **Downloads** prüft keinen Inhalt – nur Grösse und Zeitstempelheuristik (`resumeOffset`). Beim **Upload** dagegen ist ein Hash Pflicht (DEC-131). Eine Rückdatierung ist dafür nicht nötig (R-I6): ändert sich die PC-Datei während des Downloads und bricht dieser ab, genügt es, dass die Änderungszeit der PC-Datei nicht nach der der Temp-Datei liegt und die Temp-Datei kleiner ist; verglichen werden dabei die Uhren von PC und Handy. Der Anfang käme dann aus der alten, der Rest aus der neuen Version, danach folgt nur die Grössenprüfung. Ein Test existiert nicht.

### 6.3 Datei sicher ersetzen auf dem Handy (`LocalTree.replaceWith`) **[IMPL]**

```
alte Datei  → "<name>.s2pold"   (umbenennen)
Temp-Datei  → "<name>"          (umbenennen)
"<name>.s2pold" löschen
```

Scheitert das Umbenennen der Temp-Datei, wird die alte Version zurückbenannt (Temp bleibt liegen, wird nie synchronisiert).
**Recovery nach Absturz** in `LocalTree.listRecursive(recover = true)` (nur echte Läufe): Existiert `<name>.s2pold`, aber `<name>` nicht → zurückbenennen (alte Version gilt); existiert beides → `.s2pold` löschen.
Auf dem **PC** gibt es kein `.s2pold`: dort ersetzt `rename(replace = true)` die Datei serverseitig in einem Schritt.
**Namensschutz (seit 0.16.15, -203):** Vergibt der Anbieter beim Umbenennen einen anderen Namen (nicht indizierte Datei, «name (1).ext»), wird die Kopie verworfen und ein Fehler gemeldet (Text `tree_err_name_taken`), statt ein Duplikat zu hinterlassen.

### 6.4 Prüfung und Konsistenz

**Foto-Engine** (`SyncEngine.transfer`, `:602`): nach dem Upload immer `smb.fileSize(finalPath) == item.size`; zusätzlich je `verifyLevel`:

| Stufe | Code | Prüfung der Zieldatei |
|---|---|---|
| Aus | 0 | nur Grösse; gilt **nicht** als verifiziert |
| Wenig | 1 | Teil-Hash (3 × 64 KB, ≤ 192 KB ganz) |
| Mittel (Standard) | 2 | ≤ 32 MB ganze Datei, darüber 32 Stichproben à 64 KB |
| Viel | 3 | ≤ 256 MB ganz, darüber 128 Stichproben à 64 KB |
| Alles | 4 | ganze Datei |

Weicht Grösse oder Hash ab, wird die eben hochgeladene Zieldatei gelöscht (`smb.delete`), `IOException(eng_err_verify)` geworfen, die Datei zählt als `failed`, und `record()` wird **nicht** aufgerufen **[IMPL]**. Wer löscht (`deleteMode ≠ KEEP`), prüft mindestens auf „Mittel“ (`Profile.verify()`); `OFF` wird dann angehoben.

**Ordner-Engine:** Nach Upload nur `s.stat(path)` – Datei muss existieren und die Grösse stimmen. Nach Download Grössenprüfung gegen `r.size` (im `writeFileResumable` und in `download()`).
**Ein Inhalts-Hash nach der Übertragung wird in der Ordner-Engine nicht berechnet.** **[FEHLT]** (Risiko R-T2: Bit-Fehler bei gleicher Grösse bliebe unentdeckt; die Übertragung läuft über TCP/SMB mit eigener Integrität, ein zusätzlicher Schutz ist aber nicht vorhanden.)

### 6.5 Überblick der Operationen (Ordner-Engine)

| Operation | Aktion (`Kind`) | Ablauf |
|---|---|---|
| Neue Datei übertragen | `UPLOAD` / `DOWNLOAD` | Zielordner anlegen, Temp, Umbenennen, `record` |
| Vorhandene aktualisieren (einseitig) | `UPLOAD`/`DOWNLOAD` mit `trashOther = keepVersions` | mit `keepVersions` zuerst Gegenseite in den Papierkorb |
| Ersetzen (beidseitig, Erstabgleich, Entscheidung) | `UPLOAD`/`DOWNLOAD` mit `trashOther = true` | **immer** zuerst Gegenseite in den Papierkorb |
| Kopieren | – | es gibt kein eigenständiges „Kopieren“; Kopie = Neuanlage |
| Verschieben/Umbenennen | – | nicht erkannt (7.4); nur interne Moves in den Papierkorb, bei Wiederherstellung und `KEEP_BOTH` |
| In den Papierkorb | `TRASH_LOCAL`, `TRASH_REMOTE` | Abschnitt 7 |
| Löschen (endgültig) | – | nur durch Frist/Handeingriff am Papierkorb, nie durch den Sync selbst |
| Wiederherstellen | `runRestores` | Abschnitt 7.6 |
| Wiederholen | – | Wiederholung ist implizit: jeder Lauf plant aus dem Ist-Zustand neu; fehlgeschlagene Dateien haben keinen Stand und werden erneut versucht |

**Reihenfolge der Ausführung in Phase B:** nach Grösse aufsteigend (`TransferPlan.smallFirst`) – bei Abbruch sind möglichst viele Dateien fertig.

**Fortschritt:** `emitShared` höchstens alle 700 ms (ausser Schluss); Bytes = fertige + laufende (`inflight`); gemeldet über `onProgress` an `SyncWorker` → Benachrichtigung und `setProgressAsync`.

**Speicherbudget Downloads:** `freeSpace() − 300 MB`; gemeinsamer Zähler, jeder Download (und `KEEP_BOTH`) reserviert die PC-Dateigrösse; reicht das Budget nicht, wird die Datei übersprungen und `spaceShortage = true` gesetzt (Status `PARTIAL`).

**Sicherheitsmechanismen gegen Datenverlust (Zusammenfassung, Details Abschnitt 14):** Temp-Datei + Umbenennen, `.s2pold`, Papierkorb vor Überschreiben, Stand zuerst löschen, Prüfung auf Änderung während des Laufs, Schutzbremse, Stichprobe, Hash-Pflicht beim Fortsetzen von Uploads.

### 6.6 Wiederholung einer Übertragung nach Verbindungsabbruch (seit 0.15.5, SYNC-20261111-166) **[IMPL]**

Bei einem Netzwerkfehler während einer Übertragung (`UPLOAD`/`DOWNLOAD`, Phase B) schliesst der Transfer-Worker die Verbindung und versucht es **einmal** nach 2 s mit einer neuen Verbindung erneut (Abschnitt 10); `.s2ptmp` wird dabei fortgesetzt (6.1, 6.2). Die Entscheidung vor jedem Versuch trifft die reine Funktion `TransferResume.step` (`mirror/TransferResume.kt`):

| Lage | Schritt |
|---|---|
| Der Lauf hat das Ziel (Handy- oder PC-Datei) schon selbst in den Papierkorb verschoben (`trashedByRun`) und am Ziel liegt keine Datei | `TRANSFER`: nicht erneut verschieben, an der angefangenen `.s2ptmp` fortsetzen |
| Der Lauf hat das Ziel selbst in den Papierkorb verschoben, aber am Ziel ist inzwischen wieder eine Datei entstanden | `SKIP_CHANGED`: wird **nie überschrieben**, Log «folgt beim nächsten Sync» |
| Erster Versuch, Ziel seit dem Einlesen geändert | `SKIP_CHANGED` (wie bisher) |
| Erster Versuch, Versionsschutz (`trashOther`) und Zieldatei vorhanden | `TRASH_THEN_TRANSFER`: Sync-Stand vergessen, Ziel in den Papierkorb, Merken in `trashedByRun`, dann übertragen |
| Sonst | `TRANSFER` |

`trashedByRun` gilt nur für den laufenden Lauf (Menge der Dateischlüssel in der Engine-Instanz, nicht persistent).

**Behobener Fehler (-166):** Der erste Versuch verschob die alte Version am Ziel in den Papierkorb und brach dann ab. Beim zweiten Versuch fehlte das Ziel, die Prüfung «Ziel unverändert» schlug an, und die Übertragung wurde ohne Fehler übersprungen. Folge: Die Datei fehlte nach einem «erfolgreichen» Lauf, der Status lautete «alles aktuell».

**Stand:** Die Anzeige übersprungener Dateien (6.7, -167/-173) und die Wiederholung von `KEEP_BOTH` (5.6, -168/-171) sind seit 0.16.0 umgesetzt. **Offen:** Gerätetest der Wiederholungspfade (QA SYNC-20261111-178). Getestet ist nur die Entscheidungsfunktion (`TransferResumeTest`, 8 Tests), nicht die Verdrahtung in `MirrorEngine` und kein Verbindungsabbruch im Gerätetest.

### 6.7 Während des Laufs geänderte Dateien: Zähler `deferred` und Folgelauf (seit 0.16.0, DEC-238) **[IMPL]**

- **Zähler:** `MirrorEngine.skipChanged` (Datei wurde während des Laufs auf Handy oder PC geändert bzw. ist neu entstanden; bisher nur eine Warnung im Log) zählt in einem `AtomicInteger deferred`. `finish()` schreibt ihn in `sync_runs.deferred` (DB 11, siehe `DATA-MODEL.md` 3.9). Bewusst nicht `skipped`: Dort heisst der Wert bei Fotoprofilen «bereits vorhanden/unverändert».
- **Status:** Der Lauf bleibt `OK`. Die Verlauf-Zusammenfassung (`RunSummary.mirror`) ergänzt «· N folgen später» (EN «· N follow later»). Die Startseite zeigt bei ausstehenden Dateien nicht «Alles gesichert und aktuell» (`allUpToDate` ist bei `deferred > 0` falsch; Anzeige siehe DEC-241). Die Zahl für die Startseite liefert die Abfrage `pendingDeferredFlow`: Summe `deferred` aus dem jeweils letzten echten (keine Simulation), erfolgreichen Lauf je aktivem Profil.
- **Folgelauf:** Nach einem echten Lauf mit `deferred > 0` plant `SyncWorker` einen automatischen Folgelauf in **5 Minuten** (Auftragsname `sync-skip-followup`, `APPEND_OR_REPLACE`, Bedingungen wie bei automatischen Läufen, Auslöser-Text «Nachholen geänderter Dateien»; Teil von `ALL_SYNC_WORK`, damit «Pausieren/Abbrechen» ihn entfernen). **Begrenzung:** höchstens **einer pro Stunde** (Zeitstempel `lastSkipFollowUpAt` in `GlobalSettings`; reine Funktion, getestet in `SkipFollowUpTest`); danach übernehmen die regulären Auslöser (Ordnerprüfung, WLAN, Sicherheitsnetz 6 h). Pause und «nur manuell» werden wie bei jedem automatischen Lauf beachtet. Bei belegter Sperre gilt `Result.retry()` (3.1, Punkt 1). Ein bestehender `WORK_FOLLOWUP` (junge Fotos) wird nicht ersetzt, deshalb der eigene Name.
- **Handy-Datei ändert sich vor oder während des Uploads (seit 0.16.13, -202):** `MirrorEngine` prüft die Handy-Datei vor dem Start (Ordnerliste, Grösse und `mtime`) und vor dem Umbenennen der `.s2ptmp`. Eine Änderung zählt als «folgt später» (`deferred`), nicht mehr als Fehler oder «Teilweise»; die `.s2ptmp` wird gelöscht, für die abgebrochene Version wird kein Sync-Stand geschrieben, auf dem PC bleibt nie eine halbe Datei. Bausteine: `mirror/UploadVerdict.kt`, `SourceChangedException`, Parameter `sourceChanged` in `SmbSession.upload`. Fotoprofile unverändert.
- **Folgelauf für nach dem Einlesen neu entstandene Dateien (seit 0.16.14, -213):** Am Laufende prüft `LocalTree.changedSinceListing` die Änderungsstempel der Ordner (`mirror/FolderChange.kt`, höchstens 300 Ordner; keine Aussage bei eigenen Schreibzugriffen des Laufs oder ohne Stempel). Bei einer Änderung plant `SyncWorker` über `SkipFollowUp.mayPlan(…, changedDuringRun)` den Folgelauf (5 Minuten, höchstens einer pro Stunde, wie oben). Der Benutzer hat diesen Folgelauf entschieden; QA-Prüfung in -214 offen.

### 6.8 Download-Abschluss und «letzte Synchronisierung» (seit 0.16.30 bis 0.16.31, 0.16.27) **[IMPL]**

- **Zu kurzes Dateiende (`smb/DownloadEnd.kt`, `classify`):** Endet ein Download vor der erwarteten Grösse (Dateiende trotz fehlender Bytes, z. B. nach Verbindungsabbruch mit Wiederverbinden), gilt das als **unterbrochen** («Verbindung unterbrochen … Rest folgt», nicht als Fehler); die `.s2ptmp` bleibt, das Fortsetzen folgt im nächsten Lauf (Text `smb_err_download_cut`). **Nur wenn im Aufruf Bytes gelesen wurden** (Fortschritt); sonst gilt es als Einzelfehler (`DownloadSizeException`, Temp-Datei verworfen, der nächste Lauf beginnt neu), damit nichts dauerhaft blockiert. Ändert sich die PC-Datei in der Zwischenzeit (kleiner), bleibt es beim Fehler «Grösse nach Download stimmt nicht». Test: `DownloadEndTest` (5 Fälle).
- **«Letzte Synchronisierung» (Startseite):** Letzter Lauf mit Status `OK` und 0 Fehlern, kein Vorschau-Lauf (`SyncDao.lastCleanRunFlow`); Läufe mit Fehlern, Teilerfolg oder Abbruch ändern die Zeit nicht. Ein fehlerfreier Lauf ohne Übertragung zählt (Entscheidung des Benutzers, DEC-257). Quelle ist die Datenbank (kein Schema).
- **SMB-Zeitlimits (DEC-262):** `SmbSession.open` mit `smbConfigFor(enc, 60, 90)` (smbj-Zeitlimit 60 s je Anfrage, Socket 90 s); Schnellmodus 30/30 s, `checkPc`/`listShares` 30 s. Ein hartes Schliessen durch den PC wird sofort erkannt; das Limit greift nur bei stummer Gegenstelle (ca. 60 s, höchstens ca. 90 s).
---

## 7. Löschlogik und Wiederherstellung

### 7.1 Wie Löschungen erkannt werden

| Profiltyp | Erkennung |
|---|---|
| **Ordner** | Datei fehlt in der Seitenliste, `mirror_state` existiert (Tabelle B). **Nur die Existenz von S zählt**, nicht der Zeitpunkt. |
| **Fotos** | **Keine Löschungserkennung.** Weder das Löschen auf dem Handy noch auf dem PC wird bemerkt oder propagiert. `synced_files`-Zeilen bleiben bestehen. |

Verlass auf vollständige Listen: Ist eine Seitenliste unvollständig, sähe die Engine Löschungen. Schutz dagegen:

- `LocalTree.children` wirft bei `null`-Cursor einen Fehler statt „leer“ zu liefern (`LocalTree.kt:81`) **[IMPL]**.
- Fehlende Basisordner beider Seiten → Schutzbremse `LOCAL_MISSING`/`REMOTE_MISSING`.
- Massenlöschung und verschwundene Unterordner → `MASS_DELETE`.
- `SmbSession.listDir` liefert für **nicht existierende** Unterordner eine **leere Liste** statt eines Fehlers (`SmbSession.kt:156`). Ein vollständig fehlender Unterordner wird so als „alle Dateien gelöscht“ gesehen und von `vanishedFolder`/`massDelete` abgefangen, **nicht** aber, wenn er höchstens 5 Dateien enthält und das Profil grösser ist **[OFFEN/RISIKO]**.

### 7.2 Aktionen bei erkannter Löschung

| Aktion | Ablauf |
|---|---|
| `TRASH_LOCAL` | `tree.moveToTrash(rel, l, today)` → `.LeoSync-Bin/<yyyy-MM-dd>/<Unterordner>/<Name>`, dann `deleteMirrorState` |
| `TRASH_REMOTE` | `trashRemote` → `<Basis>\.LeoSync-Bin\<Datum>\<Unterordner>\<Name>` (`SmbSession.move`), dann `deleteMirrorState` |
| `DROP` | nur `deleteMirrorState`; die verbliebene Kopie bleibt unverändert und wird später als „neu“ ohne Stand behandelt |
| Wiederherstellung (Löschung rückgängig, `UPLOAD`/`DOWNLOAD` aus Tabelle B) | normale Übertragung; S fehlt → entsteht neu durch `record` |

Namenskollision im Papierkorb am selben Tag: `TrashIndex.uniqueTrashName` → `a (14-32-05).txt`, bei gleicher Sekunde `a (14-32-05-2).txt`. Bis 0.6.x hiessen Duplikate `a_2.txt`; `TrashIndex.originalName` erkennt beide Schemata.
Papierkorbnamen: aktuell überall `@LeoSync-Bin` (seit 0.16.33, DEC-260; vorher `.LeoSync-Bin`, auf dem Handy bei `msd:`-Ordnern seit 0.16.25 `@LeoSync-Bin`, DEC-254). `TRASH_DIR = "@LeoSync-Bin"`; Altnamen in `MirrorEngine.TRASH_DIRS`: `.LeoSync-Bin`, `.LeoSync-Papierkorb` (0.0.12–0.7.x), `.Sync2PC-Papierkorb` (≤ 0.0.11). Sie werden weiter gelesen, gelistet und geleert und sind per `FileFilter.ALWAYS_EXCLUDED` sowie `skipTrash` von der Synchronisation ausgeschlossen; `MEDIASTORE_TRASH_DIR` entfällt.

**Migration (seit 0.16.33, `mirror/TrashMigration.kt`, `TrashMigrationSides.kt`):** je Seite (Handy, PC) einmal pro Lauf, unter `MirrorLock`, vor dem Einlesen, nie im Vorschau-/Simulationslauf; sie wartet bei offener Wiederherstellung, Leeren-Vormerkung oder unbestätigtem Ordner. Gibt es nur den Altnamen, wird der Ordner umbenannt; gibt es beide Namen, werden die Dateien einzeln verschoben (nie kopiert, nie überschrieben, bei Namensgleichheit wird die Uhrzeit angehängt), der alte Ordner wird nur gelöscht, wenn er leer ist. Wiederholbar nach Abbruch. Fehler → Verlaufswarnung, Netzwerkfehler → unterbrochen; «Anbieter kann nicht verschieben» und «unvollständig» stehen nur im Logcat. Erfolg: eine Verlaufszeile je Seite (`eng_trash_migrated_phone/pc`).

**PC-Rückfall (`mirror/PcTrashMover.kt`):** Lehnt der Server den Ordnernamen mit einem Namens-Statuscode ab, wird `.LeoSync-Bin` benutzt (Verlaufswarnung `eng_trash_pc_fallback`). Zugriff verweigert, Quota und Netzwerkfehler lösen keinen Rückfall aus. Seit 0.16.34 merkt sich `mirror/PcTrashNameMemory.kt` (SharedPreferences `trashname`, kein Schema) eine dauerhafte Ablehnung je Profil und PC-Ziel für 7 Tage: gleich der Ausweichname, keine Übernahme, eine einzige Warnung statt zwei je Lauf. Seit 0.16.35 wird der Merker nur bei «Name ungültig» (`PcTrashMover.nameInvalid`) gesetzt, nicht bei Kollision oder NOT_SUPPORTED eines einzelnen Verschiebens. **Wiederherstellen** findet eine Vormerkung mit Altnamen auch unter dem neuen Namen und umgekehrt.

**`.nomedia` und Segment-Filter (seit 0.16.36, DEC-263, laut Development SYNC-20261112-272):** Der Handy-Papierkorb erhält eine leere `.nomedia` (`mirror/TrashNoMedia.kt`, `LocalTree.ensureTrashNoMedia`), damit die Galerie ihn nicht anzeigt; nicht bei `msd:` und `msf:`, nie auf dem PC. Angelegt vor der ersten Datei, einmal je Lauf nach `migrateTrash` (nie im Vorschau-Lauf) und nach der Migration; ein Fehlschlag ist kein Fehler (nur `Log.i`). Das Leeren «Alles» im Lauf (`LocalTree.purgeTrash`) löscht den ganzen Ordner samt `.nomedia`, sie wird vor dem nächsten Verschieben neu angelegt; das Aufräumen in der Ansicht (`TrashCleaner`) löscht nur Datumsordner und Dateien, der Ordner `@LeoSync-Bin` und die `.nomedia` bleiben stehen (Klarstellung Development, SYNC-20261112-283/-272). Seit 0.16.40 löst `ensureTrashNoMedia` nach dem Anlegen einen gezielten `MediaScannerConnection.scanFile` (`.nomedia` und Papierkorb-Ordner, nur `primary:`, nie ein Fehler) aus, weil unter Android 10 bereits indexierte Zeilen sonst bis zum nächsten Systemscan stehen bleiben (am API-29-Emulator bestanden, SYNC-20261112-284; Android 11+ ungeprüft). Die Migration entfernt eine `.nomedia` im Altnamen-Ordner (`TrashSide.deleteNoMedia`), statt sie zu verschieben. `MirrorEngine.isInTrash` (ganze Pfadsegmente, alle `TRASH_DIRS`, Gross-/Kleinschreibung egal) filtert in `MediaScanner.scan` und `SpaceFreer.query`: Papierkorb-Dateien werden von Fotoprofil nicht gesichert und bei «Speicher freigeben» nicht angeboten.

**Bekannte Einschränkungen (kein Fehler, kein Datenverlust):** (a) Bei `msd:`-Ordnern bleiben Papierkorb-Bilder in der Galerie sichtbar (kein `.nomedia`, siehe DEC-263). (b) Handy-Fehler der Migration warnen in jedem Lauf (nur der PC hat den Merker). (c) Der Merker wird beim Löschen eines Profils oder beim Import nicht entfernt (folgenlos, verfällt nach 7 Tagen; bei Tausch des PCs unter gleichem Namen gilt er bis zu 7 Tage). (d) Jeder Lauf listet zusätzlich die Profilwurzel auf Handy und PC, um einen Altordner zu erkennen. (e) Die Handy-Migration listet je Datei den Quellordner (bei sehr grossen Datumsordnern langsam, abbrechbar). (f) Verschachtelte Profile (PC-Ordner B in PC-Ordner A): `@LeoSync-Bin` von B erscheint bei A als Inhalt (galt schon für den Punkt-Namen). (g) Review L1 bis L4: u. a. blockieren erschöpfte Wiederherstellungen die Migration ohne Hinweis; Samba mit «delete veto files».

### 7.3 Löschen gegen Ändern

`plan()` betrachtet bei fehlender Gegenseite **nicht**, ob die vorhandene Seite seit S geändert wurde.

> **[RISIKO] R-D1 – Löschung überstimmt Änderung.** Fall: PC löscht `a.txt`, Handy hat `a.txt` danach bearbeitet. Mit `propagate = true` (Richtung ≠ `UPLOAD`) ergibt sich `TRASH_LOCAL`: die **bearbeitete** Handy-Version wird in den Handy-Papierkorb verschoben, kein Konflikt. Spiegelbildlich für `TRASH_REMOTE`. Ohne `propagate` und Richtung `BOTH`/`UPLOAD` wird die bearbeitete Datei stattdessen wiederhergestellt (sicher). Die Datei bleibt im Papierkorb wiederherstellbar (Frist `trashRetentionDays`, Standard 30 Tage). Es existiert kein Test für diesen Fall.

### 7.4 Umbenennen und Verschieben

> **[RISIKO] R-D2:** Eine Umbenennung ist für die Engine „alter Pfad gelöscht + neuer Pfad neu“. Mit `propagate = false` (Standard) wird der alte Pfad auf der Gegenseite **wiederhergestellt** (BOTH/DOWNLOAD bzw. UPLOAD) → die Datei liegt danach unter beiden Namen. Mit `propagate = true` wird die alte Datei der Gegenseite in den Papierkorb verschoben und die neue übertragen (Daten bleiben erhalten, ein zusätzlicher Transfer entsteht). Es gibt keine Umbenennungserkennung (Hash, Datei-ID). Gleiches gilt für Ordnerumbenennungen: der ganze Inhalt wird neu übertragen und – mit `propagate` – die alte Struktur in den Papierkorb verschoben (Schutzbremse: `vanishedFolder`).

### 7.5 Löschen nach dem Sync (nur Fotos)

`DeletionManager.sweep` (am Ende jedes echten Foto-Laufs), Zustandsautomat `DeleteState`:

```mermaid
stateDiagram-v2
    [*] --> NONE: record, deleteMode KEEP oder unverifiziert
    [*] --> WANTED: record, verifiziert und deleteMode ungleich KEEP
    WANTED --> WANTED: nicht alle abdeckenden Profile haben verifiziert gesichert
    WANTED --> QUEUED_TRASH: Android 11 plus, mindestens ein Profil wünscht Papierkorb
    WANTED --> QUEUED_DELETE: Android 11 plus, nur endgültiges Löschen
    WANTED --> DONE: Android 10, direkt gelöscht oder Datei verändert bzw. weg
    QUEUED_TRASH --> DONE: Systemdialog bestätigt oder Datei verändert bzw. weg
    QUEUED_DELETE --> DONE: Systemdialog bestätigt oder Datei verändert bzw. weg
    DONE --> [*]
```

Regeln **[IMPL]**:

- Eine Datei wird nur freigegeben, wenn **alle aktiven Foto-Profile, die ihren Ordner abdecken** (`covers`: Ordner, Unterordner-Option, Bild/Video-Typ), sie `verified` gesichert haben (`syncedAllProfiles`).
- „Papierkorb gewinnt“, wenn irgendein abdeckendes Profil nur Papierkorb wünscht.
- Android 11+ (API 30): nie ohne **Systemdialog** (`MediaStore.createTrashRequest`/`createDeleteRequest`, Pakete à 500). Android 10: direkt, vorher `stillSame` (Grösse gleich, sonst `DONE` ohne Löschung).
- `stillSame` vergleicht seit 0.15.0 Grösse **und** `DATE_MODIFIED` mit der DB-Zeile (bis 0.14.7 nur die Grösse, R-I1). Vor dem Löschen (Android 10: `sweep`; ab Android 11: `buildRequests` vor dem Systemdialog) wird die **PC-Kopie erneut geprüft** (Datei vorhanden, gleiche Grösse, gleicher LOW-Teil-Hash gegen die aktuelle Handy-Datei; gemeinsame Funktion mit `SpaceFreer`). Fehlt die Kopie oder weicht sie ab: das Original bleibt, die Zeile wird mit `SyncDao.deleteSynced` entfernt und die Datei beim nächsten Lauf neu gesichert (Toast und Verlaufszeile). PC nicht erreichbar oder von der PC-Erkennung abgelehnt: **nichts löschen**, die Zeilen bleiben vorgemerkt. `syncedBy` zählt eine Zeile nur bei passender Grösse und passendem Änderungsdatum. **[RISIKO]** Android 11+: nach dem Systemdialog keine erneute Prüfung (Zeitfenster).
- Zusammenhang mit R-F1: Eine als „bereits vorhanden“ erkannte Datei erhält bei `deleteMode ≠ KEEP` seit 0.15.0 `verified = true` nur nach Prüfung mindestens auf Stufe „Mittel“ (bis 0.14.7 nach Teil-Hash, unabhängig von der Stufe).

`SpaceFreer` („Speicher freigeben“, Fotos) prüft vor dem Entfernen jede Datei erneut auf dem PC: am gespeicherten Pfad (`RemoteFinder.existsAtPath`) seit 0.15.0 Grösse **und** LOW-Teil-Hash (bis 0.14.7 nur Grösse, R-I2), sonst in den anderen Ordnern des Monats (Name, Grösse, Teil-Hash); als „gesichert“ gilt eine Foto-Zeile nur, wenn Grösse **und** `dateModified` zur aktuellen Datei passen (`photoItems`). Details in `DECISIONS.md` DEC-091/136; der Rest der Datei wurde hier nicht gelesen (**[OFFEN]**). Seit 0.16.15 (-205) prüft `SpaceFreer.recheckUnchanged` unmittelbar vor Papierkorb/Löschen Grösse und Änderungsdatum erneut; eine inzwischen geänderte Datei bleibt, Meldung «inzwischen geändert» (Text `free_changed`). **Bekannte Grenze:** Eine Bearbeitung mit unveränderter Grösse und gleichem Datum wird nicht erkannt. `SpaceFreer` nimmt keinen `MirrorLock` und keinen Worker-Mutex (nur `ensureActive` im Prüfablauf).

### 7.6 Papierkorb, Frist und Wiederherstellung (Ordner-Profile)

**Speicherort:** je Seite `<Profilordner>/.LeoSync-Bin/<yyyy-MM-dd>/<ursprünglicher Unterpfad>/<Name>`.

**Automatische Frist** (`MirrorEngine.purgeTrash`): `trashRetentionDays > 0` → `PurgeRule(OLDER, before = cutoff)` – löscht ganze Datumsordner, deren Name `yyyy-MM-dd` **vor** dem Stichtag liegt; andere Namen werden nie gelöscht (`TrashPolicy.isExpired`). Vorgemerkte Wiederherstellungen bleiben liegen (`TrashPurge.reservedPaths`). Fehler lokal → nur Warnung; Netzwerkfehler auf dem PC → Lauf wird unterbrochen. Läuft bei jedem echten Lauf vor dem Planen.

**Von Hand** (`TrashCleaner`): nimmt `MirrorLock`; Handy sofort, PC sofort wenn erreichbar, sonst als Vormerkung in `PurgeStore` (SharedPreferences `purge`), vom nächsten Lauf ausgeführt (`runPendingPurges`, nach dem Wiederherstellen).

**Wiederherstellung** (`RestoreManager.request` → Tabelle `pending_restores` → `MirrorEngine.runRestores`):

1. Vormerken (kein sofortiger Eingriff); ein Lauf wird angestossen. Ausgeführt wird nur bei erreichbarem PC und keiner ausstehenden Stichprobe.
2. Pro Eintrag: Quelle (Handy- oder PC-Papierkorb) prüfen – fehlt sie, Eintrag verwerfen (Warnung).
3. `deleteMirrorState`, aktuelle Handy-Version und PC-Version (falls vorhanden) **in den Papierkorb**.
4. Alte Version an den ursprünglichen Ort verschieben (`tree.moveFile` bzw. `s.move`), danach auf die andere Seite kopieren (`upload()` bzw. `download()`, setzt den Stand).
5. Eintrag und eventuellen Konflikt löschen.
6. Fehler: `rollbackRestore` stellt die vorherige Lage wieder her (alte Version zurück in den Papierkorb, aktuelle Versionen an ihren Platz) und schreibt bei Erfolg den alten Stand zurück; die Vormerkung bleibt (Fehlermarke und Zähler `attempts` in `RestoreFailures`, SharedPreferences `restore`). Netzwerkfehler: Rollback-Versuch, dann Lauf wird unterbrochen; Vormerkung bleibt, der Fehlversuch zählt nicht.
7. **Wiederholregel (seit 0.11.8, DEC-146):** Ab dem 5. Fehlversuch (`RestoreRetryPolicy.MAX_ATTEMPTS`) überspringt `runRestores` den Eintrag; die Vormerkung bleibt in der Datenbank und schützt die Papierkorb-Version vor dem Löschen. „Erneut versuchen“ (`RestoreManager.retryFailed`) setzt die Zähler zurück und startet einen Sync; „Verwerfen“, Erfolg und Profillöschung entfernen die Zähler. Beim 5. Fehlversuch Verlaufseintrag `eng_restore_gave_up` (nur bei vollständigem Rollback).

**Seit 0.16.5 (SYNC-20261111-190, DEC-240):** `runRestores`/`rollbackRestore` liegen in `mirror/RestoreExecutor` (hinter `RemoteStore`, `LocalFiles` und `RestoreHost`); der Ablauf ist unverändert. **IST-Verhalten (nicht Soll; Klärung in SYNC-20261110-125), durch `RestoreExecutorTest` mit `IST_`-Tests festgehalten:** Bei Prozess- oder Benutzerabbruch gibt es keinen Rollback. Bricht der Lauf nach dem Verschieben der Quelle und vor der Übertragung ab, meldet der nächste Lauf «Quelle fehlt», die Vormerkung wird gelöscht und die Kopie auf die Gegenseite entfällt; Datenverlust entsteht dabei nicht. Bei einem Fehler nach der Übertragung ist das Zurückrollen unvollständig: Der alte Sync-Stand kommt nicht zurück, und die Statistik `trashed` wird nicht zurückgezählt.

> **[OFFEN/RISIKO] R-D3:** Die Wiederherstellung ist **nicht atomar** über Dateisysteme und Datenbank. Bricht der **Prozess** (nicht nur das Netz) mittendrin ab, gibt es kein Rollback; `pending_restores` wird erst am Ende gelöscht. Beim Neuversuch kann die Quelle schon verschoben sein (`restore_gone` → Eintrag wird verworfen) und der Zustand wird dann durch die normale Planung geglättet (neue Datei ohne Stand). Dieses Absturzfenster ist nicht getestet; das Endergebnis wurde hier nicht für alle Richtungen durchgespielt.

### 7.7 Verhinderung ungewollter Wiederbelebung

| Mechanismus | Wirkung |
|---|---|
| Stand `S` bleibt bis zur Ausführung der Löschung erhalten | Fehlschlag der Löschung ⇒ nächster Lauf plant neu |
| `DROP` bei nicht zurückschreibender Richtung | verworfener Stand ⇒ die verbliebene Kopie gilt später als „neu ohne Stand“ und wird, wo `dir` es nicht erlaubt, ignoriert |
| Löschen von `S` **vor** `trashOther`-Übertragung | Fehlschlag ⇒ Datei gilt als neu (wird erneut übertragen) statt als „auf der Gegenseite gelöscht“ (sonst würde auch das Original gelöscht) |
| Papierkorb-Ordner vom Sync ausgeschlossen | Papierinhalt kehrt nie zurück |
| Fotos: `synced_files` | gelöschte Fotos werden nicht wiederbelebt, solange die Zeile mit gleicher Grösse + `dateModified` existiert; verschwundene PC-Dateien werden **nicht** erneut hochgeladen |
| Ordner mit `propagate = false` und Richtung `BOTH` | gelöschte Dateien werden **absichtlich wiederhergestellt** (Datenerhalt hat Vorrang vor Löschung) – das ist das Standardverhalten und keine Panne |

### 7.8 Unterschiedliche Löschzustände beider Seiten

| Zustand | Ergebnis |
|---|---|
| Beide haben gelöscht (L, R fehlen, S vorhanden) | `DROP` – Stand verschwindet, nichts weiter |
| Eine Seite gelöscht, andere unverändert | Tabelle B |
| Eine Seite gelöscht, andere geändert | Tabelle B – Änderung wird **nicht** berücksichtigt (R-D1) |
| Datei im Papierkorb **und** neu angelegt | Papierkorb wird nicht gescannt; die neue Datei ist normale „neue Datei“ |
| Papierkorb-Eintrag zur Wiederherstellung vorgemerkt und gleichzeitig Frist abgelaufen | Eintrag bleibt liegen (`reserved`) |

---

## 8. Metadaten und Dateivergleich

### 8.1 Verwendete Metadaten

| Metadatum | Foto-Engine | Ordner-Engine | Quelle |
|---|---|---|---|
| **Pfad / Name** | `sourceKey = RELATIVE_PATH + DISPLAY_NAME` (Gross-/Kleinschreibung beachtet) | `relPath` mit `/`, **Schlüssel in Kleinbuchstaben**, Anzeige in Originalschreibweise | MediaStore bzw. SAF/SMB-Listing |
| **Grösse** | `size` (Bytes); Filter `> 0` | `localSize`/`remoteSize` | MediaStore `SIZE`; SAF `COLUMN_SIZE`; SMB `endOfFile` |
| **Änderungszeit** | `dateModifiedSec` (Sekunden) | `localModified`/`remoteModified` (ms) | MediaStore `DATE_MODIFIED`; SAF `COLUMN_LAST_MODIFIED`; SMB `lastWriteTime` |
| **Hinzugefügt am** | `dateAddedSec` (nur für Mindestalter) | – | MediaStore `DATE_ADDED` |
| **Hash** | Teil-Hash (`partialHash`) in `synced_files`; Prüfstufen | nicht gespeichert; bei Bedarf frisch (`thorough`) | `PartialHash` |
| **EXIF/GPS/Datum** | `MediaInfo` (`year`, `month`, `dateSource`, `latitude`, `longitude`, Land) | – | `MediaAnalyzer` (nicht gelesen; Reihenfolge nach `MediaInfo`-Kommentar: EXIF, META, FILENAME, MODIFIED; DEC-017) |
| **Identifikatoren** | MediaStore-`id` (nur als Schlüssel für Laufzeit-Fortschritt), `mediaUri` | SAF-`docId` nur innerhalb eines Laufs | – |
| **Versionsinformation** | keine | keine (nur Papierkorb-Datumsordner) | – |
| **Weitere** | `deleteState`, `verified`, `remotePath` | `syncedAt` (nur informativ) | – |

### 8.2 Welche Metadaten für welche Entscheidung

| Entscheidung | Verwendet |
|---|---|
| „Foto schon gesichert?“ | `sourceKey` + `size` + `dateModifiedSec` aus `synced_files` |
| „Foto-Duplikat auf dem PC?“ | gleicher Name (auch `name_N`), `size`, Teil-Hash |
| „Foto-Zielordner“ | EXIF/Metadaten-Datum, GPS → Land (`MediaInfo`, Cache gültig bei gleicher `size` und `dateModified`) |
| „Ordner-Datei geändert?“ | `size` + `modified` je Seite gegen `mirror_state` |
| „Gleicher Inhalt?“ | `size` und `PartialHash.thorough` |
| „Wer gewinnt (NEWER)?“ | `modified` beider Seiten (seitenübergreifend) |
| „Download fortsetzen?“ | `tmpSize`, `tmpModified`, `remoteSize`, `remoteModified` |
| „Datei während des Laufs geändert?“ | `size` + `modified` jeweils gegen die Eintragswerte der Listing-Phase |

### 8.3 Hashverfahren (`PartialHash`, `sync/PartialHash.kt`)

SHA-256 über `[Grösse (8 Byte)] + Bereiche`. Bereiche:

- **Wenig/„compute“:** ≤ 192 KB ganz, sonst 3 × 64 KB (Anfang, Mitte, Ende).
- **Mittel/„thorough“:** ≤ 32 MB ganz (1-MB-Blöcke), sonst 32 gleichmässig verteilte Stichproben à 64 KB. Für den **Ordner-Sync-Vergleich** und „Speicher freigeben“ (Ordner) gilt seit 0.15.0 über 32 MB ein vollständiger Hash beider Seiten (`ContentCompare`); die Stichprobe dient dort nicht mehr als Gleichheitsbeweis. In `synced_files` wird der LOW-Teil-Hash seit 0.15.0 immer abgelegt (`SyncEngine.transfer`), damit `partialHash` ab Stufe „Mittel“ nicht `null` bleibt.
- **Viel:** ≤ 256 MB ganz, sonst 128 Stichproben.
- **Alles:** ganz.

Stichproben-Hashes können Änderungen **zwischen** den Stichproben nicht entdecken (Test `thoroughDetectsChangeInTheMiddle` belegt das für den „Wenig“-Hash bei einer 1-MB-Datei; „Mittel“ liest dort die ganze Datei).

### 8.4 Probleme durch Zeitstempel und Dateisystem

| Thema | Verhalten im Code | Bewertung |
|---|---|---|
| SAF kann `lastModified` nicht setzen | nach Download enthält der Handy-Stand die tatsächlich gelesene Zeit (nicht die PC-Zeit); beide Seiten werden getrennt verglichen | **[IMPL]** Kernidee von DEC-035/137 |
| `stat()` und `children()` liefern bei manchen Anbietern (z. B. „Downloads“) verschiedene Zeitstempel | Stände werden immer per Ordnerliste (`tree.find`) gelesen (`download`, `executeTransfer`, `runRestores`) | **[IMPL]** Behebung eines Feldfehlers (DEC-137) |
| Zeitstempel verschoben, Inhalt gleich | `RECORD` statt Übertragung (nur wenn `wouldTransfer` und Grösse gleich) | **[IMPL]** |
| Exakte Gleichheit der Zeit | keine Toleranz (z. B. FAT/exFAT mit 2 s Granularität, Rundung bei Server) | **[OFFEN]** – für `PC`-Seite wird nach dem Upload per `stat` der real gespeicherte Wert gelesen, was Rundung abfängt; für SD-Karten-SAF-Anbieter ungeprüft |
| Zeit `0` (Anbieter liefert `null`) | `LocalTree` setzt `modified = 0`; Änderungen mit gleicher Grösse wären dann unsichtbar | **[RISIKO]** – ob reale Anbieter das tun, ist nicht geprüft; `resumeOffset` behandelt `0` als „unbekannt“ |
| Uhrenabweichung Handy/PC | nur bei `NEWER` relevant (R-K1) | **[RISIKO]** |
| Zeitzone | SMB `FileTime` und SAF-ms sind UTC-Epochenwerte; die Papierkorb-Datumsordner nutzen die lokale Gerätezeit (`SimpleDateFormat(..., Locale.GERMANY)`) | keine Zeitzonen-Vergleiche zwischen Seiten |
| Mehrdeutigkeit Gross-/Kleinschreibung | Schlüssel in Kleinbuchstaben; zwei Dateien `A.txt` und `a.txt` im selben Handy-Ordner kollidieren – die zweite überschreibt die erste in der `local`-Map (`associate`) | **[RISIKO]** Randfall, nicht abgefangen |
| Windows-ungültige Namen (`: ? * " < > |`, Endpunkt/-leerzeichen, reservierte Namen) | `FolderNames.sanitize` wirkt **nur in der Foto-Engine**. Ordner-Dateien mit solchen Namen schlagen beim SMB-Upload fehl und zählen in jedem Lauf als Dateifehler | **[RISIKO]** nicht abgefangen |
| Sehr lange Pfade | im Code nicht behandelt | **[OFFEN]** |

### 8.5 Filter

`FileFilter` (`mirror/FileFilter.kt`): Glob (`*`, `?`, `**`), Gross-/Kleinschreibung egal; Muster mit `/` gelten für den relativen Pfad, sonst für den Dateinamen; Ausschluss hat Vorrang; leere Einschlussliste = alles. Immer ausgeschlossen: `*.s2ptmp`, `*.s2pold`, die drei Papierkorb-Namen. Gilt für beide Engines. Ein Filter wirkt auf beide Seiten gleich; bereits gesicherte Dateien, die neu ausgeschlossen werden, verschwinden aus beiden Listen ⇒ `DROP` des Stands (keine Löschung der Dateien).

---

## 9. Synchronisationsprofile

Es gibt zwei Profiltypen (`ProfileType`) und für Ordner drei Richtungen (`SyncDirection`).

### 9.1 Vergleichstabelle

| Eigenschaft | **Fotos** (`PHOTOS`) | **Ordner Handy → PC** (`MIRROR`, `UPLOAD`) | **Ordner PC → Handy** (`MIRROR`, `DOWNLOAD`) | **Ordner beidseitig** (`MIRROR`, `BOTH`) |
|---|---|---|---|---|
| Engine | `SyncEngine` | `MirrorEngine` | `MirrorEngine` | `MirrorEngine` |
| Richtung | fest Handy → PC (`Profile.dir()`) | Handy → PC | PC → Handy | beide |
| Quelle Handy | MediaStore (`MediaScanner`) | SAF-Ordner (`LocalTree`) | SAF-Ordner | SAF-Ordner |
| Zielstruktur | `Jahr\Monat[ - Land]` (V1) oder V2/V3, pro Profil wählbar (DEC-209) | 1:1 mit Unterordnern | 1:1 | 1:1 |
| Zustandstabelle | `synced_files` (+`media_info`, `folder_mappings`, `pending_decisions`) | `mirror_state` (+`mirror_conflicts`, `pending_restores`) | gleiche | gleiche |
| Änderungserkennung | nur Handy: `size` + `dateModified` | beide Seiten gegen `mirror_state` | beide | beide |
| Neue Datei | hochladen (ggf. `name_N`) | hochladen | herunterladen | beide Richtungen |
| Einseitig geändert | Handy: erneut als neu/`name_N`; PC: nicht beobachtet | Handy-Änderung → Upload; PC-Änderung ignoriert | PC-Änderung → Download; Handy-Änderung ignoriert | beide → jeweils übertragen |
| Beidseitig geändert | – | Handy gewinnt, PC in Papierkorb | PC gewinnt, Handy in Papierkorb | `ASK` → Konflikt, `NEWER` → neuere gewinnt |
| Konflikte | keine (nur `name_N`) | keine | keine | ja (`ConflictMode`) |
| Löschen auf Handy | nicht erkannt | `propagate` → `TRASH_REMOTE`, sonst `DROP` (PC-Kopie bleibt) | wiederherstellen (Download) | `propagate` → `TRASH_REMOTE`, sonst wiederherstellen |
| Löschen auf PC | nicht erkannt | wiederherstellen (Upload) | `propagate` → `TRASH_LOCAL`, sonst `DROP` | `propagate` → `TRASH_LOCAL`, sonst wiederherstellen |
| Löschen nach Sync | `deleteMode` KEEP/TRASH/DELETE nach verifiziertem Upload | nein (`Profile.delete()` = KEEP) | nein | nein |
| Papierkorb | – (Android-Papierkorb via Systemdialog) | `.LeoSync-Bin` beider Seiten | wie links | wie links |
| Versionsschutz | – | `keepVersions` | `keepVersions` | `keepVersions` |
| Schutzbremse | – | ja | ja | ja |
| Filter | ja | ja | ja | ja |
| Prüfung nach Übertragung | `verifyLevel` (Aus … Alles) | nur Grösse | nur Grösse | nur Grösse |
| Mindestalter | `minFileAgeMinutes` (Standard 5) | nein | nein | nein |
| Erster Lauf | wartet auf manuellen Start (DEC-103) | wie links | wie links | wie links |
| Speicher freigeben | `SpaceFreer` | `FolderFreer` (nur `UPLOAD` ohne `propagate`) | nein | nein |
| Besonderheit | Live-Foto-Paar im selben Ordner; Ordnerfragen an den Benutzer | – | Speicherbudget (300 MB Reserve) | – |

### 9.2 Einschränkungen und Wechselwirkungen

- `FolderFreer.eligible` verlangt Richtung `UPLOAD` **und** `propagateDeletes = false`; sonst würde das Fehlen auf dem Handy zur Löschung auf dem PC propagiert.
- Profile mit überlappenden Ordnern oder Zielen: `ProfileOverlap` blockiert identische Profile und warnt bei Überschneidung (DEC-092/134) – nur auf Eingabeebene, nicht zur Laufzeit **[OFFEN]**.
- Die Wirkung von Konfigurationsänderungen (z. B. `propagateDeletes` einschalten) auf bestehende Stände ist rein regelbasiert (`plan()`); eine Migration der Stände findet nicht statt.

---

## 10. SMB-Kommunikation

Bibliotheken und Abhängigkeiten → `ARCHITECTURE.md` §8. Hier der konkrete Einsatz.

### 10.1 Schnittstelle `SmbSession`

Eine `SmbSession` kapselt Verbindung, Anmeldung und **eine** Share (`DiskShare`). Alle Pfade sind relativ zur Share-Wurzel mit `\`; die Engines
bauen Pfade über `SmbSession.path(...)` (Basisordner des Profils + Teile). Ordner-Engine rechnet `/` ↔ `\` in `remotePath`.

| Funktion | Aufgabe | Aufrufer |
|---|---|---|
| `SmbSession.open(profile)` | Verbinden, anmelden, Share öffnen | beide Engines, Worker, `FolderCheck`, `RestoreManager`, `TrashCleaner`, `FolderFreer`, `SpaceFreer` |
| `isAlive()` | `share.isConnected && connection.isConnected` | vor jeder Nutzung durch Worker/Produzent |
| `isReachable(host)` | TCP-Connect Port 445, 3 s, **ohne Anmeldung** | `SyncWorker`, `FolderCheck`, `RestoreManager`, `TrashCleaner` |
| `listDir`, `listRecursive` | Auflisten (Dateien: Name, Grösse, `lastWriteTime`) | Planung, Duplikatsuche, Papierkorb |
| `stat`, `fileSize`, `dirExists` | Existenz/Grösse/Zeit | `executeTransfer`, Verifikation |
| `upload`, `download` | Übertragen (1-MB-Puffer) | Engines |
| `move` | Umbenennen/Verschieben (`DELETE`+`READ`, `rename(to, false)`) | Papierkorb, Wiederherstellung |
| `ensureDir` | Ordnerkette anlegen; verträgt Parallelität (ignoriert Fehler, wenn der Ordner inzwischen existiert) | Upload, Papierkorb |
| `delete`, `deleteDir` | Datei bzw. Ordner **rekursiv** endgültig löschen | Verifikationsfehler (Foto), Papierkorb leeren |
| `partialHash`, `levelHash`, `contentHash` | Hashes durch Lesen von Bereichen | Verifikation, `sameContent`, Duplikatsuche |
| `listShares` | Freigaben über `srvsvc` auflisten | Profilauswahl (nicht Teil des Sync-Laufs) |
| `close()` | Share, Session, Verbindung, Client schliessen (je mit eigenem try/catch) | `finally` in Engines/Workern |

### 10.2 Verbindungsaufbau und Authentifizierung

`SmbSession.open` : `SmbConfig` über die gemeinsame Fabrik `smbConfigFor` (verwendet von `open`, `checkPc` und der Freigabenliste) mit Timeout 60 s, Socket-Timeout 90 s, `multiProtocolNegotiate = true`; `client.connect(host)`; `AuthenticationContext(user, password, domain|null)` mit dem **entschlüsselten** Passwort (`CredentialCrypto`, Android-Keystore); `connectShare(share) as? DiskShare`. Bei jedem Fehler werden `connection` und `client` geschlossen und die Ausnahme weitergeworfen. Verschlüsselung je Profil (`smbEncryption`, seit 0.15.0, DEC-222): «Aus» = wie bisher (`withEncryptData(false)`); «Wenn möglich» (Standard, auch für bestehende Profile) = `withEncryptData(true)`: smbj verschlüsselt automatisch (AES-128-GCM/CCM), sobald der Server SMB3-Verschlüsselung kann, sonst läuft die Verbindung unverschlüsselt weiter; «Erforderlich» = zusätzlich nur Dialekte SMB 3.1.1/3.0.2/3.0 und Pflichtprüfung in `PcLogin` **vor** `authenticate()` (ohne ausgehandelte Verschlüsselung Abbruch ohne Senden der Zugangsdaten, `SmbEncryptionUnavailableException`) und nach der Anmeldung (Sitzung muss einen Verschlüsselungsschlüssel haben). «Verbindung testen» zeigt Dialekt und Verschlüsselungsstatus (`SmbSession.testDetailed`). Es gibt keinen Zertifikatsschalter (SMB3 über TCP nutzt keine Zertifikate, smbj unterstützt kein SMB über QUIC). **Ist-Stand smbj 0.13.0 (Architecture SYNC-20261110-126, Bytecode-Prüfung):** Signieren wird nicht verlangt (`withSigningRequired(false)`), Dialekte 3.1.1, 3.0.2, 3.0, 2.1, 2.0.2, nur NTLMv2/SPNEGO, Pre-Auth-Integrity für 3.1.1, **keine** `FSCTL_VALIDATE_NEGOTIATE_INFO`-Prüfung für 3.0.x, nur TCP (keine Zertifikate, kein QUIC); Sicherheitsanbieter BouncyCastle. Offen/nicht entschieden: Signieren erzwingen, Sicherheitsanbieter (Risiken R-SMB2 bis R-SMB5, Abschnitt 17).

### 10.3 Fehlerbehandlung, Abbrüche, Wiederverbindung

- **Klassifikation:** `isNetworkError(e)` (`sync/Errors.kt`) – `SocketException`, `SocketTimeoutException`, `UnknownHostException`, `EOFException`, Klassennamen mit `TransportException`, Meldungen „already been closed“ / „not connected“, bis Tiefe 8. Alles andere ist ein Datei-/Logikfehler (`failed`).
- **Wiederverbindung:** `SmbSession` verbindet **nie** selbst neu. Die Aufrufer tun es:
  - Foto-Produzent (`producerSmb`) und alle Worker prüfen vor Gebrauch `isAlive()` und öffnen bei Bedarf neu (Windows trennt unbenutzte Verbindungen nach ca. 15 min).
  - Bei einem Netzwerkfehler in einer Datei: Verbindung schliessen, **ein** Wiederholungsversuch nach 2 s mit neuer Verbindung (Upload/Download setzt an der Abbruchstelle fort); beim zweiten Fehler wird der **ganze Lauf** unterbrochen (`INTERRUPTED`). Wiederholungsentscheidung bei bereits verschobenem Ziel: Abschnitt 6.6.
  - Phase A der Ordner-Engine und `purgeTrash` nutzen die **Hauptverbindung** ohne `isAlive()`-Prüfung; ein Netzwerkfehler dort unterbricht den Lauf sofort ohne Wiederholungsversuch.
- **Berechtigungs-/Anmeldefehler** (`STATUS_LOGON_FAILURE`, `STATUS_ACCESS_DENIED` …) sind keine Netzwerkfehler: Lauf endet als `FAILED` mit verständlicher Meldung (`friendlyError`), bzw. bei Dateien als Dateifehler.
- **Ressourcenfreigabe:** siehe `close()`-Aufrufe; Worker schliessen ihre Sitzung im `finally`; der Hauptlauf `smb?.close()` im `finally` von `run`/`runInner`.

### 10.4 Lokale Gegenstelle `LocalTree`

Alle Handy-Zugriffe laufen über `DocumentsContract`/`ContentResolver` mit einer dauerhaften Baum-Berechtigung. Ordner-IDs werden pro Lauf in einer `ConcurrentHashMap` gecacht (`dirIds`, Kleinbuchstaben-Schlüssel); `ensureDir` ist `@Synchronized`. Wichtig für die Engine: Listen liefern `LocalEntry(docId, name, size, modified, isDir)`; fehlende Berechtigung wird vor dem Lauf geprüft, ein Verlust während des Laufs führt zu einer Ausnahme der jeweiligen Operation.

---

## 11. Nebenläufigkeit und Hintergrundverarbeitung

### 11.1 Ebenen der Koordination

| Ebene | Mechanismus | Zweck |
|---|---|---|
| WorkManager (Einreihen) | eindeutige Aufträge je Art: `sync-auto` (`KEEP`), `sync-manual` (`KEEP`), `sync-followup`, `sync-share-retry`, `sync-resume` (`REPLACE`/`APPEND_OR_REPLACE`), `trigger-content`, `trigger-safety`, `trigger-remote-poll`, `pause-end` | verhindert Mehrfach-Einträge derselben Art |
| Sync-Sperre | `SyncGate` (prozessweit, seit 0.15.0; vorher `SyncWorker.mutex` mit `tryLock`): Sperre und persistenter Nachlauf-Merker (`GlobalSettings.deferredStart`, `commit`) unter einer gemeinsamen kurzen `synchronized`-Sperre | höchstens **ein** Lauf in der ganzen App; ein währenddessen verworfener Start wird genau einmal nachgeholt |
| `MirrorLock` | `Mutex` um `MirrorEngine.run` (echt) | Handeingriffe (Papierkorb leeren, endgültig löschen, Speicher freigeben Ordner) warten auf den Lauf |
| Pro Foto-Lauf | `Shared.nameLock` (Mutex), `reserved`, `listings`, atomare Zähler, `Channel(32)` | eindeutige Zielnamen bei parallelen Uploads |
| Pro Ordner-Lauf | `ConcurrentLinkedQueue`, atomare Zähler, geteiltes `Shared.budget`, `stop`-Flag | parallele Übertragungen |

### 11.2 Verhalten und Grenzen

- **Parallelität:** `parallelUploads` 1–4 (Standard 3) Worker mit **je eigener** SMB-Verbindung; Profile laufen **nacheinander**.
- **Gleichzeitige Läufe:** durch `SyncGate` ausgeschlossen. `WORK_AUTO` und `WORK_MANUAL` sind verschiedene Auftragsnamen und könnten sich überschneiden; der zweite Worker beendet sich sofort (Ausgabe `deferred = true`) und hinterlässt einen Nachlauf-Merker.\n- **Nachlauf (seit 0.15.0, DEC-221):** Der Inhaber der Sperre prüft nach der Freigabe den Merker und reiht genau einen Nachlauf ein (`SyncScheduler.enqueueDeferred`, Namen `sync-deferred` und `sync-deferred-manual`, `APPEND_OR_REPLACE`, Auslöser «nachgeholt»); manuell bleibt manuell. Keine Schleife: ein weiterer Nachlauf entsteht nur, wenn während seiner Laufzeit erneut ein Start verworfen wurde. `SyncScheduler.setupAll` holt einen Merker nach Prozess-Kill nach. «Pausieren» und «Abbrechen» (`cancelAll`) verwerfen den Merker. Schnittstelle für die Oberfläche: `enqueueManual`/`enqueueAuto` liefern `StartResult`, `deferredState()` als StateFlow.

> **[RISIKO] R-N1 (bis 0.14.7; seit 0.15.0 durch den Nachlauf behoben, DEC-221):** Wurde ein Start gestartet, während ein Lauf aktiv war, beendete sich der Worker sofort mit `success` und tat **nichts**; die Anfrage wurde nicht gemerkt. Das betraf alle fünf Sync-Namen (`sync-auto`, `sync-manual`, `sync-followup`, `sync-share-retry`, `sync-resume`). Die Oberfläche zeigte nichts an (Home wertet nur RUNNING/ENQUEUED). Daten gingen nicht verloren (Entscheidungen, Konfliktwahl, Wiederherstellungs-Vormerkungen und Guard-Freigaben sind vorher in DB/`GuardStore` persistiert), verloren ging nur die Wirkung von `manual` (u. a. Erststart neuer Profile, Umgehung von WLAN-Regel und Wartezeit). Aufgefangen wurde sie durch WLAN-Ereignis, Medien-Trigger, Poll oder das Sicherheitsnetz (6 h, wird nach jedem Lauf neu gesetzt) – **nicht** bei „Für immer pausiert“ (Sicherheitsnetz aus).

- **Simulation** läuft ausserhalb von Worker-Mutex und `MirrorLock`; sie liest, schreibt aber `SyncRun`/Cache-Daten (z. B. `media_info`-Einträge werden nur bei `!dryRun` aktualisiert).
- **Abbrüche:** `CancellationException` wird in beiden Engines gefangen, in `withContext(NonCancellable)` mit Status `INTERRUPTED` (Android hat gestoppt, mit Grund) oder `CANCELLED` (Benutzer) abgeschlossen und **erneut geworfen**. Übertragungsschleifen rufen `ensureActive()` pro 1-MB-Block und pro Datei. Temp-Dateien bleiben liegen (Fortsetzen).
- **Stopp durch Android** (`isStopped` ≠ `STOP_REASON_CANCELLED_BY_APP`): `markInterrupted()` setzt `resumeOnWifi`, plant `enqueueResume` und zeigt „Pausiert“ (`PauseService`).
- **Ressourcen:** `TransferLocks` (WLAN `FULL_HIGH_PERF` + `FULL_LOW_LATENCY`, Teil-Wakelock, Obergrenze 4 h), Vordergrunddienst, Speicherbudget für Downloads, `parallelUploads ≤ 4`, 1-MB-Puffer je Verbindung, `Channel`-Kapazität 32 für Foto-Jobs.
- **Race Conditions im Ordner-Lauf:**
  - *Zeitfenster* zwischen Einlesen (Schritt 7/8) und Ausführung: dateibezogene Prüfung `unchanged(...)` vor `UPLOAD` (PC) und `DOWNLOAD` (Handy); nicht bei `TRASH_*`, `RECORD`, `DROP`, `KEEP_BOTH`.
  - *Handy-Seite beim Upload:* es wird **nicht** erneut geprüft, ob sich die Handy-Datei während des Lesens ändert; eine Änderung würde beim nächsten Lauf (Zustand ≠ `S`) erneut hochgeladen.
  - *TOCTOU* PC: zwischen `unchanged(...)` und `rename(replace)` kann sich die PC-Datei ändern. Mit `trashOther` wird die dann vorhandene Datei zuerst in den Papierkorb verschoben (kein Verlust); ohne Versionsschutz überschreibt `rename(replace)` direkt **[RISIKO]** gering.
  - *Gleiche Datei in zwei Läufen:* durch Mutex ausgeschlossen. Simulation während eines Echtlaufs liest und kann inkonsistente Vorschauen zeigen (kein Schaden).

### 11.3 Inkonsistente Zustände

- `closeDanglingRuns()` räumt nur Lauf-Datensätze auf. Datei-Zustände werden über Temp-Dateien (`.s2ptmp`, `.s2pold`) und die Neuplanung eines jeden Laufs geglättet.
- Die Aufrufstelle `dao.closeDanglingRuns()` ist nur sicher, weil sie unter dem Worker-Mutex steht; Simulationen sind per `dryRun = 0` ausgeschlossen.

---

## 12. Fehlerbehandlung und Wiederanlauf

### 12.1 Klassifikation

| Fehlerklasse | Beispiele | Reaktion | Kategorie |
|---|---|---|---|
| **Netzwerk** | WLAN weg, PC im Ruhezustand, Leerlauf-Trennung, Timeout | ein Wiederholungsversuch pro Datei (2 s), danach `INTERRUPTED`, kein Dateifehler; Folgeauftrag (2 min bei bestehendem WLAN mitten im Lauf, 30 min bei nicht erreichbarem PC, sofort beim Wiederverbinden bei WLAN-Verlust; ausserhalb des WLAN jeweils 2 h) | automatisch + erneuter Versuch |
| **Verbindung beim Start** | `isReachable` scheitert | im WLAN bis 5 Versuche über ca. 1 min, danach 30-min-Wartezeit; ausserhalb des WLAN 2 Versuche, danach 2 h | automatisch |
| **Anmeldung/Rechte** | `STATUS_LOGON_FAILURE`, `STATUS_ACCESS_DENIED` | Lauf `FAILED`, Fehlertext, Benachrichtigung | **Benutzereingriff** |
| **Dateifehler** | Name ungültig, Datei gesperrt, PC-Platte voll (`STATUS_DISK_FULL`), Handy-Speicher | `failed`++ , Log `E`, Lauf läuft weiter, Status `PARTIAL`; Datei hat keinen Stand und wird im nächsten Lauf erneut versucht | erneuter Versuch (ggf. Benutzer) |
| **Speichermangel Download** | Budget (frei − 300 MB) überschritten | Datei übersprungen, `spaceShortage`, `PARTIAL`, Benachrichtigung | Benutzer (Platz schaffen) |
| **Prüffehler Foto** | Grösse/Hash verschieden | Zieldatei löschen, `failed`, kein DB-Eintrag | erneuter Versuch |
| **Geändert während des Laufs** | Zieldatei verändert | `skipChanged`, Log `W` (kein `failed`) | automatisch im nächsten Lauf |
| **Schutzbremse** | fehlender Ordner, Massenlöschung, falscher Ordner | Status `WAITING`, Profil ruht bis zur Entscheidung | **Benutzereingriff** |
| **Fehlende Berechtigung SAF** | Baumrecht entzogen | `IOException`, `FAILED` | Benutzer (Ordner neu wählen) |
| **Ordner-Entscheidung Fotos** | umbenannte Monatsordner | Datei wartet (`PendingDecision`), Benachrichtigung | Benutzer |
| **App-Absturz / Prozess beendet** | Hersteller-Killer, Update | `closeDanglingRuns` markiert `INTERRUPTED`; Temp-Dateien bleiben; Wiederaufnahme durch nächsten Auslöser | automatisch |
| **Datenbankfehler** | Room-Ausnahme | wird wie jede andere Ausnahme behandelt (`FAILED` mit `shortError`); keine Sonderbehandlung | Benutzer / Entwickler |
| **Fehlende DB-Migration** | Schemaversion ohne Migration | **absichtlich Absturz** statt stiller Datenlöschung (`AppDatabase`: kein `fallbackToDestructiveMigration`) | Entwickler |

`isNetworkError` wird auch in der Produzentenphase (Foto-Analyse mit Ordnerliste) angewendet: dort bricht ein Netzwerkfehler den gesamten Lauf ab, jeder andere Fehler zählt als Analysefehler der Datei.

### 12.2 Wiederaufnahme

| Mechanismus | Wirkung |
|---|---|
| Neuplanung je Lauf | Eine Ordner-Datei ohne Stand wird erneut übertragen; eine Foto-Datei ohne Zeile in `synced_files` ebenso |
| `.s2ptmp` (PC und Handy) | Fortsetzen ab Abbruchstelle (Upload mit Hash-Pflicht, Download mit Zeit-/Grössenheuristik) |
| `.s2pold` (Handy) | Rückbenennung/Löschung beim nächsten `listRecursive(recover = true)` |
| `resumeOnWifi` + `enqueueResume` + Receiver | Fortsetzen nach WLAN-Verlust; nach 24 h Aufgabe |
| `shareRetryAt` + `enqueueShareRetry` | 30-min-Wiederholung bei unerreichbarem PC |
| `pending_restores`, `PurgeStore` | Handeingriffe warten auf den nächsten Lauf mit erreichbarem PC |
| `GuardStore` | Entscheidungen überleben Neustart (SharedPreferences) |
| Sicherheitsnetz 6 h | Fängt verlorene Auslöser |

### 12.3 Fehlerprotokollierung

Tabellen `sync_runs` (Status, Zähler, Nachricht) und `log_entries` (Ebenen `I`, `W`, `E`); im Dry-Run keine Logeinträge. Aufbewahrung Logs 30 Tage, Läufe 90 Tage. `shortError` (innerste Ursache), `friendlyError` (bekannte `STATUS_*` und Netzwerkklassen → übersetzte Meldung mit nächstem Schritt). Absturzprotokoll separat (`diag/CrashReporter.kt`, nicht Teil der Engine).

### 12.4 Garantien

**Die Implementierung bietet:**
- *Keine unvollständige Zieldatei unter dem Zielnamen:* Temp + Umbenennen auf PC und Handy.
- *Keine Fortschreibung des Stands ohne abgeschlossene Dateioperation* (Ordner) bzw. ohne Verifikation (Foto).
- *Papierkorb vor Überschreiben* in allen Fällen mit `trashOther` und bei jeder Löschung durch den Sync.
- *Selbstheilung durch Neuplanung:* jeder Lauf berechnet aus Ist-Zustand + `S` neu; Wiederholungen sind idempotent.
- *Konvergenz (best effort):* wiederholte Läufe ohne Eingriffe führen beide Seiten zusammen.

**Die Implementierung bietet nicht:**
- *Transaktionale Atomarität* zwischen Dateisystem, SMB und Room (kein `@Transaction`/`withTransaction` im Sync-Pfad, nur im Backup-Import). Zwischenzustände nach Prozessabbruch werden durch die Neuplanung korrigiert, nicht durch Rollback. (Ausnahme: `rollbackRestore` bei Wiederherstellungsfehlern, ohne Prozessabbruch.)
- *Erkennung von Umbenennen/Verschieben, von Löschen-gegen-Ändern und von Inhaltsbeschädigung nach Übertragung in der Ordner-Engine.*
- *Exactly-once* oder garantierte Reihenfolge der Dateien.
- *Cross-Profil-Koordination* bei überlappenden Ordnern (nur Warnung bei der Eingabe).

---

## 13. Datenbank und Synchronisationszustand

Schema, Spalten, Migrationen → `DATA-MODEL.md`. Hier nur die Nutzung durch die Engines.

### 13.1 Vor dem Lauf geladen

| Daten | Engine | Quelle |
|---|---|---|
| Profil | beide | Parameter des Konstruktors (Objekt zum Aufrufzeitpunkt, kein erneutes Lesen) |
| `synced_files` (kompakt: `sourceKey`, `size`, `dateModified`) | Foto | `dao.syncedLite` |
| `media_info` | Foto | `dao.mediaInfo(sourceKey)` je Datei |
| `folder_mappings` | Foto | `dao.mapping` im `TargetResolver` |
| `mirror_state` | Ordner | `dao.mirrorStates` (Map über `relPath`) |
| `mirror_conflicts` | Ordner | `dao.conflicts(profileId)` |
| `pending_restores` | Ordner | `dao.pendingRestores(profileId)` |
| Nicht-DB-Zustand | Ordner | `GuardStore` (`guard`), `PurgeStore` (`purge`), `RestoreFailures` (`restore`), `GlobalSettings` |

### 13.2 Während und nach dem Lauf geschrieben

| Zeitpunkt | Schreibvorgang |
|---|---|
| Laufstart | `insertRun` (RUNNING) |
| Foto: jede erfolgreiche/erkannte Datei | `upsertSynced` **nach** Verifikation bzw. bei erkanntem Duplikat |
| Foto: Analyse | `upsertMediaInfo` |
| Foto: Ende des Produzenten | `deleteDecisionsOfProfile` zu Beginn, `upsertDecision` für offene Gruppen am Ende |
| Ordner: Phase A | `deleteMirrorState` (TRASH/DROP), `upsertMirrorState` (RECORD), `upsertConflict` |
| Ordner: Phase B | `deleteMirrorState` vor dem Papierkorb-Schritt, `upsertMirrorState` nach erfolgreicher Übertragung |
| Ordner: Planung | `deleteConflict` (erledigte) – vor der Ausführung |
| Lauf-Ende | `updateRun` mit Status/Zählern (`finish`) |
| Log | `insertLog` pro Ereignis (nicht Dry-Run) |

### 13.3 Abhängigkeiten zwischen Datenbank und Dateioperationen

- **Reihenfolge Datei → DB:** Der Stand wird **nach** der Dateioperation geschrieben (Upload/Download/Verifikation). Folge: Stürzt der Prozess nach der Dateioperation und vor dem `upsert` ab, gilt die Datei im nächsten Lauf als geändert/neu und wird erneut gesichtet. Das ist sicher, kostet aber Übertragung bzw. Hashing.
- **Reihenfolge DB → Datei bei Gefahr:** Vor einer überschreibenden Übertragung wird der Stand **zuerst gelöscht** (`executeTransfer`, `KEEP_BOTH`, `runRestores`). Ein Abbruch danach macht die Datei zu „neu“ statt zu „gelöscht“.
- **Keine Transaktionen:** jede DAO-Operation ist für sich atomar. Es gibt keinen Mechanismus, der Stand und Datei zusammen bindet.
- **Zustandswiederherstellung:** nach Abbruch nur über Neuplanung und die Resteinträge (Temp-Dateien, `pending_*`, SharedPreferences). `closeDanglingRuns` erzeugt dafür einen sauberen Verlaufseintrag.
- **Zustand ausserhalb der DB:** `GuardStore.set(...)` und die Einmal-Freigaben (`allowDelete_*`, `keepFiles_*`) nutzen `SharedPreferences.apply()` (asynchron). Dass diese Werte einen sofortigen Prozess-Kill überleben, ist nicht garantiert **[OFFEN]**; die Entscheidung wäre dann erneut zu treffen, die Schutzbremse greift erneut (sicher).
- **Backup/Import:** `BackupManager` setzt Sync-Stände im Import in `db.withTransaction`; importierte Ordner-Profile warten auf den ersten Start und eine Stichprobe (DEC-135).

---

## 14. Kritische Invarianten und Schutzmechanismen

Legende: **[IMPL]** vorhanden, **[RISIKO]** vorhanden mit Lücke, **[FEHLT]** nicht vorhanden, **[OFFEN]** nicht nachweisbar.

| # | Invariante | Technische Bedeutung | Komponenten | Absicherung im Code | Status |
|---|---|---|---|---|---|
| I-01 | Zieldatei erscheint nie unvollständig unter ihrem Namen | Upload/Download in `.s2ptmp`, danach Umbenennen | `SmbSession.upload`, `LocalTree.writeFileResumable/replaceWith` | `TEMP_SUFFIX`, `rename`, Grössenprüfung (`offset != size`, `written != expectedSize`) | **[IMPL]** |
| I-02 | Alte Handy-Version geht beim Ersetzen nie verloren | `.s2pold`-Wechsel | `LocalTree.replaceWith`, `listRecursive(recover)` | alte → `.s2pold`; bei Fehler zurück; Absturz-Recovery | **[IMPL]** |
| I-03 | Keine Fortschreibung des Stands ohne Erfolg | `mirror_state`/`synced_files` bilden nur Abgeschlossenes ab | `MirrorEngine.record`, `SyncEngine.record` | `record` nur nach `upload`/`download`/Verifikation; nicht bei `skipChanged`/Fehler | **[IMPL]** |
| I-04 | Fotos gelten nur nach Prüfung als gesichert | `verified`/`WANTED` hängen an Verifikation | `SyncEngine.transfer`, `Profile.verify()` | Grösse + Hash nach Stufe; bei Fehler Zieldatei löschen (best-effort, R-I4; ein Fehler wird seit 0.15.0 im Verlauf gemeldet), kein `record`; Löschen erzwingt ≥ „Mittel“ (auch bei „bereits vorhanden“ seit 0.15.0, `PresenceCheck`) | **[IMPL]**, Rest R-I4 (best-effort) |
| I-05 | Eine Überschreibung hinterlässt eine wiederherstellbare Vorversion | `trashOther` | `MirrorEngine.plan/executeTransfer/trashRemote`, `LocalTree.moveToTrash` | `trashOther = true` bei beidseitiger Änderung/Erstabgleich; bei einseitiger nur mit `keepVersions` | **[RISIKO]** (`keepVersions = false`) |
| I-06 | Fehlschlag einer Überschreibung wird nie als Löschung gewertet | Stand vor der Operation entfernen | `executeTransfer`, `KEEP_BOTH`, `runRestores` | `deleteMirrorState` vor `trashRemote/moveToTrash` | **[IMPL]** (DEC-133) |
| I-07 | Nichts wird überschrieben, was sich seit dem Einlesen geändert hat | Prüfung der Gegenseite vor dem Schreiben | `executeTransfer`, `unchanged`, `writeFileResumable(expectNew)` | Vergleich `size` + `modified`; `ChangedDuringRunException` | **[RISIKO]** nicht bei `KEEP_BOTH`, `TRASH_*`; kleines TOCTOU-Fenster |
| I-08 | Kein Löschen grosser Mengen ohne Rückfrage | `GuardPolicy` | `MirrorGuard.kt`, `MirrorEngine.runInner` | > `massDeletePercent` (Standard 20 %), ≥ 100 Löschungen, Unterordner > 5 Dateien weg, kleine Profile nur bei „alles“ | **[IMPL]**, gezählt werden nur `TRASH_*` (nicht `trashOther`) |
| I-09 | Fehlender Basisordner ist keine „Massenlöschung“ | beide Seiten | `runInner` | `LOCAL_MISSING`, `REMOTE_MISSING`; SAF-`children` wirft bei `null` | **[IMPL]** |
| I-10 | Falscher Ordner nach Wechsel wird erkannt | Stichprobe | `FolderCheck`, `folderMatches`, `GuardPolicy.sample/matches` | 20 Dateien + 20 Top-Ordner, ≥ 80 % gleiche Grösse | **[IMPL]** |
| I-11 | Höchstens ein Lauf gleichzeitig | Mutex | `SyncGate`, `MirrorLock` | `tryLock` mit Nachlauf-Merker; `withLock` | **[IMPL]** (R-N1 seit 0.15.0 behoben) |
| I-12 | Handeingriffe laufen nie parallel zum Ordner-Lauf | `MirrorLock` | `TrashCleaner`, `FolderFreer`, `MirrorEngine.run` | `MirrorLock.mutex.withLock` | **[IMPL]**; `SpaceFreer` (Fotos) ohne Sperre |
| I-13 | Zum Wiederherstellen vorgemerkte Versionen werden nie gelöscht | `reservedPaths/Keys` | `purgeTrash`, `TrashCleaner`, `LocalTree.purgeTrash` | `keep`-Pfade werden übersprungen | **[IMPL]** |
| I-14 | Nur Datumsordner werden automatisch gelöscht | `TrashPolicy.isExpired` | `purgeTrash`, `PurgeRule.matches` | Regex `\d{4}-\d{2}-\d{2}` und `name < cutoff` | **[IMPL]** |
| I-15 | Papierkorb und Temp-Dateien werden nie synchronisiert | Ausschluss | `FileFilter.ALWAYS_EXCLUDED`, `skipTrash` | feste Muster | **[IMPL]** |
| I-16 | Fortsetzen eines Uploads nur bei passendem Quellinhalt | Hash-Pflicht | `SmbSession.upload` | `expectedHash == null ⇒ offset = 0`; Prüfung nach Fortsetzen | **[IMPL]** |
| I-17 | Fortsetzen eines Downloads nur bei unverändertem PC-Original | Heuristik | `TransferPlan.resumeOffset`, `writeFileResumable` | Zeitstempelvergleich + Grössenprüfung | **[RISIKO]** kein Hash (R-T1) |
| I-18 | Fotos werden vom Handy nur nach bestätigter, verifizierter Sicherung entfernt | `DeleteState`, `DeletionManager` | `DeletionManager.sweep/buildRequests` | alle abdeckenden Profile `verified`; Systemdialog ab API 30; `stillSame` | **[IMPL]**, `stillSame` nur Grösse |
| I-19 | Kein stilles Löschen der Datenbank bei fehlender Migration | Schemaschutz | `AppDatabase` | kein `fallbackToDestructiveMigration` | **[IMPL]** |
| I-20 | Eindeutige Zielnamen bei parallelen Foto-Uploads | Namenssperre | `SyncEngine.transfer`, `Shared.nameLock/reserved` | Mutex + `reserved`; Rename ohne `replace` | **[IMPL]** |
| I-21 | Löschung mit gleichzeitiger Änderung wird als Konflikt behandelt | – | – | – | **[FEHLT]** (R-D1) |
| I-22 | Umbenennung wird nicht als Löschung + Neuanlage gewertet | – | – | – | **[FEHLT]** (R-D2) |
| I-23 | Ordner-Übertragungen werden nach dem Transfer inhaltlich geprüft | – | – | nur Grösse | **[FEHLT]** (R-T2) |
| I-24 | Stand und Dateisystem sind transaktional gekoppelt | – | – | keine Transaktionen | **[FEHLT]**; Ersatz: Neuplanung (12.4) |
| I-25 | Entscheidungen des Benutzers überstehen Unterbrechungen | – | `mirror_conflicts` | Konfliktzeile wird vor Ausführung gelöscht | **[RISIKO]** (R-K2) |
| I-26 | Windows-ungültige Namen werden in der Ordner-Engine behandelt | – | – | nur Foto-Engine `FolderNames.sanitize` | **[FEHLT]** |
| I-27 | Wiederherstellung übersteht Prozessabbruch | – | `runRestores` | Rollback nur bei Ausnahmen, nicht bei Prozessabbruch | **[OFFEN]** (R-D3) |
| I-28 | Manuelle Startanfrage geht bei laufendem Sync nicht verloren | – | – | `SyncGate`: Merker, genau ein Nachlauf (E2) | **[IMPL]** seit 0.15.0 (DEC-221) |

---

## 15. Tests und bekannte Einschränkungen

Befehle und Struktur → `docs/TESTING.md`. Es wurden **keine Tests oder Builds ausgeführt**; die Aussagen beruhen auf dem Lesen des Testcodes.

### 15.1 Vorhandene Testklassen (`app/src/test`, JUnit 4; `app/src/androidTest` für Room-Migration)

Es gibt **kein Mocking-Framework**, **keine Fakes für SMB oder SAF** und **keinen Test, der `MirrorEngine`, `SyncEngine`, `SmbSession`,
`LocalTree` (ausser der reinen Beschriftung `rawPathLabel`), `TargetResolver`, `DeletionManager`, `SyncWorker` oder `SyncScheduler` aufruft**
(Grep nach diesen Klassen im Testverzeichnis).

| Testklasse | Getestete Komponente | Relevant für die Engine |
|---|---|---|
| `MirrorGuardTest` | `GuardPolicy` (Massenlöschung, kleine Ordner, ab 100, Prozent pro Profil, verschwundener Unterordner, Stichprobe, `matches`), `PartialHash.thorough` | Schutzbremse und Inhaltsvergleich |
| `TransferPlanTest` | `TransferPlan.smallFirst`, `resumeOffset` | Fortsetzen von Downloads/Uploads, Reihenfolge |
| `VerifyLevelTest` | `PartialHash.levelRegions/forLevel`, `Profile.verify()`, Backup-Round-trip des Levels | Prüfstufen Fotos |
| `TrashIndexTest` | `TrashIndex` (Namen mit Uhrzeit, Rückwandlung, neueste Version je Datei, Auswahl) | Papierkorb-Zuordnung |
| `TrashPurgeTest` | `PurgeRule`, `TrashPurge.affected`, `reservedEntriesAreKept` | Leeren, Schutz vorgemerkter Einträge |
| `TrashPolicyTest` | `TrashPolicy.isExpired/cutoff` | Fristlogik |
| `FileFilterTest` | `FileFilter` (Standard, Einschluss, Ausschluss-Vorrang, Name vs. Pfad) | Filter |
| `FolderNamesTest` | `FolderNames` (Monat, Standardnamen, Konfliktnamen, `sameBaseName`, `sanitize`), `PartialHash` (Regionen, Abhängigkeit von Grösse) | Foto-Namenslogik |
| `FreeRulesTest` | `FreeRules` (Auswahlkriterien Speicher freigeben) | Speicher freigeben (UI-Regeln, nicht Löschausführung) |
| `ProfileOverlapTest`, `ProfileWizardTest`, `CredentialGroupsTest` | Profilkonfiguration | indirekt |
| `RunSummaryTest` | `RunSummary` (Lauftexte, Gruppierung) | Anzeige |
| `FilenameDateParserTest` | Datumsermittlung aus Dateinamen | Foto-Zielordner |
| `BackupCodecTest`, `DiagnosticsTest`, `LocalTreeLabelTest`, `StringsParityTest`, `TestTexts` | Nebenfunktionen | nicht Engine |
| `MigrationTest` (`androidTest`, Gerät/Emulator) | Room-Migrationen 1→6 | Erhalt von Profildaten bei DB-Updates |

### 15.2 Szenarien-Übersicht (nur nach tatsächlichen Tests bewertet)

| Szenario | Test vorhanden? | Testdatei | Bewertung |
|---|---|---|---|
| `plan()`-Entscheidungstabelle (Tabellen A–C) | **fehlt** (`plan` ist `private` und an Android-Typen gebunden) | – | nicht getestet |
| Neue/geänderte/gelöschte Datei im 3-Wege-Abgleich | **fehlt** | – | nicht getestet |
| Beidseitige Änderung, Konflikt, `NEWER`, `KEEP_BOTH` | **fehlt** | – | nicht getestet |
| Löschen gegen Ändern, Umbenennen | **fehlt** | – | nicht getestet |
| Schutzbremse: Schwellen und Stichprobe | ja | `MirrorGuardTest` | nur die reinen Regeln; die Einbindung in `runInner` ist ungetestet |
| `GuardStore`-Zustandsübergänge (Einmal-Freigaben, `block`) | **fehlt** | – | nicht getestet |
| Gründlicher Inhaltsvergleich | ja | `MirrorGuardTest` | Bereiche und Änderungserkennung; nicht der Aufruf in `sameContent` |
| Resume-Entscheidung | ja | `TransferPlanTest` | gut für die reine Funktion; `SmbSession.upload`/`writeFileResumable` ungetestet |
| Upload-Hash-Pflicht beim Fortsetzen | **fehlt** | – | nicht getestet |
| `.s2pold`-Ersetzen und Recovery | **fehlt** | – | nicht getestet |
| Prüfstufen (Regionen, `verify()`) | ja | `VerifyLevelTest` | Regeln getestet; Ablauf in `transfer()` nicht |
| Foto-Namensvergabe `name_2`, Duplikatsuche | teilweise | `FolderNamesTest` | Namensfunktionen; nicht die Duplikatlogik in `transfer()` |
| `TargetResolver` | **fehlt** | – | nicht getestet |
| Papierkorb-Namen, Zuordnung, Auswahl | ja | `TrashIndexTest` | gut |
| Papierkorb-Frist, Leeren, Schutz vorgemerkter Einträge | ja | `TrashPolicyTest`, `TrashPurgeTest` | reine Regeln; `LocalTree.purgeTrash`/`TrashCleaner.purgeRemote` ungetestet |
| Wiederherstellung inkl. Rollback | **fehlt** | – | nicht getestet |
| Löschen nach dem Sync (`DeletionManager`) | **fehlt** | – | nicht getestet |
| Fehlerklassifikation (`isNetworkError`, `friendlyError`) | **fehlt** | – | nicht getestet |
| SMB-Verbindung, Wiederverbindung, Timeouts | **fehlt** | – | nicht getestet (kein Fake) |
| `SyncWorker`/`SyncScheduler`/WorkManager | **fehlt** | – | nicht getestet |
| Wiederanlauf nach Prozessabbruch | **fehlt** | – | nicht getestet |
| Room-Migrationen | ja | `MigrationTest` | nur Profile (1→6 und 5→6); Sync-Tabellen werden nicht auf Inhalt geprüft |

### 15.3 Kritische, nicht ausreichend getestete Szenarien (Prioritäten)

1. **Alle `plan()`-Zweige** (inkl. Richtungs- und `propagate`-Kombinationen, `RECORD`-Pfad, Konfliktentscheidungen) – höchste Priorität, da dies die Datenverlust-Logik ist. Eine Prüfung wäre erst nach Herauslösen der reinen Entscheidung möglich (hier nicht vorgeschlagen, nur festgestellt).
2. **Löschen gegen Ändern / Umbenennen** (R-D1, R-D2).
3. **Wiederherstellung samt Rollback und Absturzfenster** (R-D3).
4. **`executeTransfer`-Reihenfolge** (Stand löschen → Papierkorb → Upload) bei Fehlschlag in jedem Schritt.
5. **`.s2ptmp`/`.s2pold`-Fortsetzen und Recovery**, besonders Download ohne Hash (R-T1).
6. **Konfliktzeile-Lebenszyklus** bei Unterbrechung (R-K2).
7. **Foto-Duplikatlogik und `verified = true`-Pfad** (R-F1).
8. **Netzwerkfehler-Wiederholung** (ein Versuch, dann Lauf abbrechen; Zustand der anderen Worker).
9. **Simulation vs. Echtlauf** (gleiche Ergebnisse der Planung).
10. **Manueller Start während eines Laufs** (R-N1): die Entscheidungslogik ist seit 0.15.0 per `SyncGateTest` geprüft; die Worker-Ebene (`SyncWorker`, `SyncScheduler`) bleibt ungetestet.

---

## 16. Konkrete Beispiele

Annahme: Ordner-Profil `Doku`, Zustand `S` je Datei als `(Grösse, Zeit)`.

### B1 – `BOTH`, `ASK`, beide Seiten ändern dieselbe Datei
`S(report.docx) = L(1000, t100) / R(1000, t200)`. Handy bearbeitet → `L(1200, t300)`; PC bearbeitet → `R(1100, t400)`.
`changedL` und `changedR` wahr; Grösse verschieden ⇒ ohne Hash „verschieden“ ⇒ `CONFLICT`. Der Benutzer sieht beide Versionen im Dialog; wählt er „Handy behalten“, wird im nächsten Lauf die PC-Version nach `.LeoSync-Bin\<Datum>\report.docx` verschoben und die Handy-Datei hochgeladen. `S` wird danach mit den frisch gelesenen Werten beschrieben.

### B2 – Zeitstempel verschoben, Inhalt gleich
PC-Tool setzt die Änderungszeit von `foto.pdf` neu (`R(500, t999)`), Grösse gleich, `dir = BOTH`. Nur `changedR`. `wouldTransfer = dir.down = true`, Grösse gleich ⇒ `thorough`-Hash beider Seiten gleich ⇒ `RECORD` (Stand neu merken), **kein Download**.

### B3 – `UPLOAD`-Profil, Erstabgleich, PC hat abweichende Datei
`a.txt` existiert auf beiden Seiten, `S` fehlt, Inhalt verschieden ⇒ Handy gewinnt: PC-Version nach `.LeoSync-Bin\<Datum>\a.txt`, Handy-Datei hochgeladen. (Bei `BOTH`/`ASK` stattdessen `CONFLICT`.)

### B4 – Löschung am Handy, `BOTH`
`S(b.txt)` vorhanden, `b.txt` vom Handy gelöscht, auf dem PC unverändert.
- `propagate = false` (Standard): Tabelle B ⇒ `DOWNLOAD` – die Datei wird auf dem Handy **wiederhergestellt**.
- `propagate = true`: `TRASH_REMOTE` – die PC-Datei geht nach `.LeoSync-Bin\<Datum>\b.txt`, der Stand wird gelöscht.

### B5 – Löschen gegen Ändern (R-D1)
`S(c.txt)` vorhanden. Der PC löscht `c.txt`; das Handy ändert `c.txt` danach. `propagate = true`, `BOTH`: `l != null, r == null, st != null` ⇒ `TRASH_LOCAL`. Die bearbeitete Handy-Datei landet im Handy-Papierkorb, **ohne Rückfrage**. Mit `propagate = false` stattdessen `UPLOAD` (Handy-Version bleibt erhalten).

### B6 – Umbenennen am Handy (R-D2)
`x.txt` → `y.txt`, `BOTH`, `propagate = false`: `y.txt` wird hochgeladen (neu); `x.txt` fehlt lokal, existiert auf dem PC mit Stand ⇒ `DOWNLOAD` ⇒ `x.txt` erscheint wieder auf dem Handy. Ergebnis: beide Namen auf beiden Seiten. Mit `propagate = true`: `x.txt` auf dem PC in den Papierkorb, `y.txt` hochgeladen.

### B7 – Schutzbremse
100 Dateien im Profil; auf dem PC wird ein Unterordner mit 30 Dateien gelöscht; `propagate = true`. `toTrash = 30` > 20 % ⇒ `MASS_DELETE`, Status `WAITING`, Benachrichtigung. „Nicht löschen“ ⇒ nächster Lauf mit `propagate = false` (stellt die 30 Dateien wieder her); „Trotzdem löschen“ ⇒ einmalige Freigabe.

### B8 – Foto bearbeitet
`DCIM/Camera/IMG_1.jpg` war gesichert (`size 4 MB, mod 1700000000`). Das Handy speichert die Datei neu (`size 3.8 MB`). Nicht mehr „bekannt“ ⇒ Zielname `IMG_1.jpg` existiert mit anderer Grösse ⇒ `IMG_1_2.jpg` wird hochgeladen und verifiziert; `synced_files` zeigt danach auf `IMG_1_2.jpg`; die ursprüngliche PC-Datei bleibt.

### B9 – Abbruch mitten in einem grossen Upload
WLAN verschwindet bei 60 %. Worker: Netzwerkfehler ⇒ Sitzung schliessen, 2 s warten, ein Neuversuch; scheitert dieser ebenfalls ⇒ `sh.stop`, `connectionFailed`, Lauf `INTERRUPTED`. `x.mp4.s2ptmp` bleibt auf dem PC. Beim nächsten Lauf: Resume nur, wenn `tmpModified >= Original-modified` **und** der Hash von Anfang/Mitte/Ende der Temp-Datei dem der Handy-Datei entspricht; sonst Neubeginn.

---

## 17. Risikoregister

| ID | Risiko | Wirkung | Schutz heute | Abschnitt |
|---|---|---|---|---|
| R-D1 | Löschung überstimmt Änderung der Gegenseite | geänderte Version in Papierkorb statt Konflikt | Papierkorb (30 Tage Standard), `propagate` standardmässig aus | 7.3 |
| R-D2 | Umbenennen/Verschieben = Löschung + Neu | Dubletten oder doppelte Übertragung | Papierkorb, Schutzbremse | 7.4 |
| R-D3 | Wiederherstellung nicht atomar bei Prozessabbruch | Restzustand, evtl. verworfene Vormerkung | Neuplanung, Papierkorb | 7.6 |
| R-K1 | `NEWER` vergleicht zwei Uhren | falsche Datei gewinnt | unterlegene Version im Papierkorb | 5.2 |
| R-K2 | Konfliktzeile vor Ausführung gelöscht | Entscheidung geht bei Unterbrechung verloren | Konflikt wird erneut gemeldet | 5.4 |
| R-K3 | Entscheidung gilt für neuere Version als angezeigt | unerwartete Version bleibt/geht | Papierkorb | 5.4 |
| R-T1 | Download-Resume ohne Hash | theoretische Mischdatei | Zeit-/Grössenprüfung | 6.2 |
| R-T2 | Kein Inhalts-Hash nach Übertragung (Ordner) | unentdeckte Bit-Fehler bei gleicher Grösse | Grössenprüfung | 6.4 |
| R-F1 | Foto „bereits vorhanden“ ⇒ `verified = true` unabhängig von Prüfstufe (bis 0.14.7) | Löschfreigabe auf Basis eines Teil-Hash | seit 0.15.0 bei `deleteMode ≠ KEEP` Prüfung mindestens „Mittel“ (`PresenceCheck`); bei `KEEP` bewusst unverändert (DEC-021); Systemdialog ab API 30 | 3.2, 7.5 |
| R-N1 | Start bei laufendem Sync wurde verworfen (alle fünf Sync-Namen; Mittel, kein Datenverlust) | Wirkung von `manual` ging verloren (u. a. Erststart neuer Profile) | **behoben in 0.15.0** (Nachlauf, `SyncGate`, DEC-221); bis dahin Sicherheitsnetz (nicht bei „Für immer pausiert“) | 11.2 |
| R-W1 | Windows-ungültige Dateinamen (Ordner-Engine) | Dateifehler in jedem Lauf | Fehlermeldung im Verlauf | 8.4 |
| R-M1 | `modified = 0` bei Anbietern ohne Zeit | Änderungen mit gleicher Grösse unsichtbar | – | 8.4 |
| R-C1 | Dateien nur in der Gross-/Kleinschreibung verschieden | eine Datei wird ignoriert | – | 8.4 |
| R-L1 | `SmbSession.listDir` liefert für fehlenden Ordner „leer“ | fehlender Unterordner = „alles gelöscht“ | `vanishedFolder` (> 5 Dateien), `massDelete` | 7.1 |
| R-V1 | `keepVersions = false` | einseitige Überschreibung ohne Vorversion | bewusst gewählte Einstellung | 6.5 |
| R-S1 | `GuardStore` mit `apply()` | Entscheidung/Block könnte bei Prozess-Kill fehlen | Schutzbremse greift erneut | 13.3 |
| R-I1 | Löschfreigabe Foto ohne erneute PC-Prüfung (Hoch, Codeanalyse; Architecture -125) | Original gelöscht trotz fehlender/abweichender PC-Kopie | **behoben in 0.15.0** (PC-Prüfung vor dem Löschen, `stillSame` mit Datum; DEC-223). Rest: Android 11+ keine erneute Prüfung nach dem Systemdialog | 7.5 |
| R-I2 | „Speicher freigeben“ am gespeicherten Pfad nur nach Grösse (Hoch) | Original entfernt trotz abweichender PC-Datei | **behoben in 0.15.0** (Grösse + LOW-Teil-Hash, `photoItems` mit Datum; DEC-223) | 7.5, 3.2 |
| R-I3 | Ordner-Sync wertet Stichproben-Gleichheit über 32 MB als Gleichheit (Hoch) | in-place geänderte grosse Datei erreicht die Gegenseite nie, Konflikt geht still verloren, `FolderFreer` kann Original entfernen | **behoben in 0.15.0** (Vollhash beider Seiten über 32 MB, abbrechbar; DEC-223). Rest: Hashen unter `nameLock`; kein Integrationstest | 5.2, 8.3 |
| R-I4 | Fehlgeschlagene Foto-Verifikation löscht die Zieldatei nur best-effort (Mittel) | liegen gebliebene Datei galt über R-F1 als „vorhanden“ | seit 0.15.0 durch `PresenceCheck` (Stufe ≥ „Mittel“) entschärft; Fehler beim Aufräumen im Verlauf; Löschen bleibt best-effort | 3.2, 14 (I-04) |
| R-I5 | Ersetzen am Ende der Übertragung ohne erneute Zielprüfung (Mittel; Upload `rename(replace)`, Download `replaceWith` mit alter Geschwisterliste); ungeklärt: `renameDocument` kann „name (1)“ vergeben | Zeitfenster = Übertragungsdauer | Papierkorb (`trashOther`); **offen** | 6.1, 6.3, 14 (I-07) |
| R-I6 | Download-Fortsetzen ohne Rückdatierung und mit Uhrenvergleich PC/Handy (Mittel; Präzisierung R-T1) | Mischdatei aus alter und neuer PC-Version | Grössenprüfung; **offen** | 6.2 |
| R-I7 | Kleinigkeiten (Niedrig): Ausweich-Kopieren in `moveToTrash`/`moveFile` ohne Prüfung; `.s2ptmp` auf dem PC (Fotos) nie aufgeräumt; `.s2pold`-Recovery löscht, sobald der Basisname existiert; `KEEP_BOTH`/`TRASH_*` ohne „unverändert seit Listing“ (I-07) | selten, kleine Wirkung | **offen** | 6.3 |
| R-BG1 | `PauseService` verbraucht das `dataSync`-Tagesbudget (Android 15/16, 6 h/24 h), keine eigene Laufzeitgrenze; unter Android ≤ 14 unbegrenzt (Hoch, Codeanalyse; Architecture -124) | Budget fehlt danach dem eigentlichen Sync | **offen**, Inhaberentscheidung (Zielkonflikt DEC-052) | 11, `ARCHITECTURE.md` |
| R-BG2 | Nicht expedited Läufe (`resume`, `followup`, `retry`, „nur beim Laden“) bekommen den Vordergrund voraussichtlich nicht (Mittel, ungeklärt) | Abbruch langer Läufe | **offen**, Gerätemessung nötig | 11 |
| R-BG3 | `PauseService.onTimeout` nur `stopSelf()`, keine Rückfall-Anzeige; „Pausiert“ verschwindet bei `resumeOnWifi = true` (Niedrig, Code nachgewiesen) | Anzeige verschwindet | **offen** | 11 |
| R-BG4 | Sperre blieb bei Ausnahme in `markInterrupted`/`cancelProgress` vor dem Freigeben hängen (Mittel, theoretisch) | jeder weitere Sync stumm verworfen bis Prozessende | **behoben in 0.15.0** (Freigabe in jedem Fall, verschachteltes `finally`) | 3.1 |
| R-BG5 | `enqueueResume` konnte vor dem Freigeben starten und verworfen werden (Niedrig, theoretisch) | Fortsetzung verworfen | **behoben in 0.15.0** (Freigabe vor dem Einreihen, `APPEND_OR_REPLACE`) | 3.1, 11.2 |
| R-BG6 | Android-16-Kontingente für FGS/expedited nicht geprüft (Mittel, ungeklärt; wie DEC-183) | unbekannt | **offen** | 11 |
| R-BG7 | Keine Tests für `SyncWorker`, `SyncScheduler`, `TriggerWorker`, `PauseService`, Receiver; kein `work-testing` (Mittel) | fehlende Absicherung | teilweise: `SyncGateTest` (Entscheidungslogik); Worker-Ebene **offen** (SYNC-20261110-127) | 15 |
| R-BG8 | `BootReceiver` mit `exported="true"` im Manifest (Niedrig, theoretisch) | – | **offen** (prüfen, ob `false` genügt) | – |
| R-SMB1 | Keine Verschlüsselung; Freigaben mit Verschlüsselungspflicht nicht nutzbar (Mittel, belegt) | Dateiinhalte im Netz lesbar | **behoben in 0.15.0**: Verschlüsselung je Profil (Aus / Wenn möglich (Standard) / Erforderlich, DEC-222); echter Test gegen Windows/NAS **offen** | 10.2 |
| R-SMB2 | Signieren clientseitig nicht verlangt (Mittel, belegt; setzt aktiven Angreifer im LAN voraus) | Dateiinhalte im Transport veränderbar bei Servern ohne Signierpflicht | **offen** (Signieren erzwingen nicht entschieden, erst nach Leistungsmessung) | 10.2 |
| R-SMB3 | Herabstufung: Dialekte ab 2.0.2 angeboten, keine Negotiate-Validierung für 3.0.x (Mittel) | Angreifer kann Verbindung ohne Verschlüsselung erzwingen | nur mit Stufe „Erforderlich“ geschützt (nur SMB 3.x, Pflichtprüfung); bei „Wenn möglich“ **offen** | 10.2 |
| R-SMB4 | NTLMv2-Antwort an gefälschten Server (GUID-Fälschung) erlaubt Offline-Angriff auf das Passwort (theoretisch; Mittel bei schwachem Passwort) | Passwort ratbar | Eigenschaft von NTLM; PC-Erkennung; Hilfe-Empfehlung eigenes Konto (offen, Documentation/UX) | 6.0 |
| R-SMB5 | `CredentialCrypto.decrypt` liefert bei Keystore-Verlust still `""` (Niedrig; kein Sicherheitsrisiko) | Meldung „Anmeldung fehlgeschlagen“ statt „Passwort neu eingeben“ | **offen** (kleine Development-Aufgabe) | – |

---

## 18. Offene Fragen, Nicht-Geprüftes, Geplantes

### 18.1 Nicht eindeutig nachvollziehbar / nicht geprüft
- Verhalten von `tree.rename` bei bereits existierendem `*_conflict-phone.*` (SAF-Anbieter-abhängig). Seit 0.16.0 durch freien Namen und Prüfung des Anbieter-Namens abgefangen (5.6); am Gerät nicht geprüft (QA SYNC-20261111-178).
- Verhalten von SAF-Anbietern ohne `COLUMN_LAST_MODIFIED` (R-M1) und von SD-Karten (Zeitstempel-Granularität).
- ~~Ob die Oberfläche einen verworfenen manuellen Start (R-N1) sichtbar macht~~ – geklärt: bis 0.14.7 nicht sichtbar (Home wertete nur RUNNING/ENQUEUED); seit 0.15.0 zeigt die Oberfläche den Nachlauf (Snackbar, Zeile «Danach: …», DEC-224).
- ~~SMB-Sicherheitsparameter~~ – belegter Ist-Stand in Abschnitt 10.2; offen bleiben Signieren erzwingen, Sicherheitsanbieter (BouncyCastle gegen JCE), Verhalten gegen echte Windows-/NAS-Server mit Verschlüsselungspflicht, Zusammenspiel `withMultiProtocolNegotiate` mit eingeschränkten Dialekten, Durchsatz mit/ohne Verschlüsselung, SMB-Verschlüsselung des lokalen QA-Testservers.
- Verhalten sehr langer Pfade und reservierter Windows-Namen.
- Exakte Reihenfolge der Datumsermittlung (`MediaAnalyzer`, nur nach `Entities.kt`-Kommentar und DEC-017).
- Rest von `SpaceFreer` (nach Zeile 120).
- Ob `SyncWorker`-Abbrüche in allen Android-Versionen gleich `STOP_REASON_*` melden.

### 18.2 Geplant bzw. nicht umgesetzt (nur zur Abgrenzung, nicht Teil des Ist-Stands)
Quelle `DECISIONS.md`/`ARCHITECTURE.md` §14: Paketwechsel `ch.leo.sync` und Play-Store-Integration (nur offene Punkte, ohne Versionsnummer) (Die Umsetzung von DEC-205 ist seit 0.13.1 erfolgt, Abschnitt 6.0.) Die PC-Erkennung nach DEC-204 ist im Code vorhanden (Abschnitt 6.0); APK ab 0.13.0 in `releases\` vorhanden. Der Rest betrifft die Synchronisationslogik nicht. („Ordner anlegen“, DEC-192, ist seit 0.11.3 umgesetzt; „Verlauf mit Kennungen“, DEC-159/DEC-202, seit 0.12.0/0.12.1, siehe Lauf-Schritt 1 „Lauf anlegen“.)

### 18.3 Offene Dokumentationspunkte
- Ergänzung nach Prüfung von `MediaAnalyzer`, `Notifications`, `WifiHelper`, `PauseService` (Abschnitte 2.3 und 11).
- Sobald Tests für `plan()` und `executeTransfer` existieren: Abschnitt 15.2 anpassen; Befehle gehören in `TESTING.md`.

---

## 19. Abgrenzung und Verweise

| Thema | Dokument |
|---|---|
| Modulstruktur, Bibliotheken, Android-Konfiguration | `docs/ARCHITECTURE.md` |
| Entscheidungen und Begründungen (DEC-001 ff.) | `docs/DECISIONS.md`, Abschnitte 2–4, 7, 8, 9 |
| Tabellen, Spalten, Migrationen, SharedPreferences-Schlüssel | `docs/DATA-MODEL.md` |
| Tests, Build- und Prüfbefehle | `docs/TESTING.md` |
| Patch-Historie | `PATCHNOTES.md` |

Diese Datei verändert keine anderen Dokumente.