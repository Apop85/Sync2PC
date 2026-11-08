## Aufträge
(neueste oben)

### SYNC-20261108-020 – Unabhängige Prüfung: Schalter mobile Daten/VPN (DEC-126, 0.11.5/0.11.6)
- Erstellt am: 2026-11-08
- Absender: Development
- Empfänger: QA
- Priorität: MEDIUM
- Status: OPEN
- Übergabegrund: Änderung an Netzbedingungen, Wiederanlauf und Nebenläufigkeit des automatischen Syncs; unabhängige Prüfung nötig. Build 0.11.6 ist erfolgreich (Inhaber).
- Aufgabenbeschreibung: Funktionale Korrektheit, Regressionen und Datenverlustrisiken prüfen. Schwerpunkte: (1) Schalter AUS = Verhalten identisch zu vorher (WLAN-Regel, SSID, `UNMETERED`). (2) Schalter EIN: ausserhalb des WLAN keine SSID-Prüfung, alle aktiven Profile berechtigt; im WLAN (auch VPN über WLAN) weiter SSID-Regel. (3) Abbruch/Fortsetzen: Netzverlust mitten im Lauf (`shouldContinue`, `markInterrupted`, `enqueueResume`), Ordner-Profile mit Löschen/Spiegeln über instabile Mobilverbindung, Schutzbremse unverändert. (4) Umschalten EIN→AUS und AUS→EIN: bereits geplante Aufträge mit alter Netzwerkbedingung, `cancelShareRetry`, `scheduleSafetyNet`. (5) Sicherung: `syncOffWifi` Export/Import, ältere Sicherungen ohne Schlüssel. (6) Testlücken: keine Unit-Tests für `onAllowedNetwork`/`offWifiSyncActive`, kein Round-Trip-Test für `syncOffWifi`. Zusätzlich „Problem melden“ (-008): Mail-App öffnet, Anhang vorhanden, Fall ohne Mail-App.
- Technischer Kontext: Review (code-reviewer) zu 0.11.5: keine CRITICAL/HIGH-Funde; MEDIUM 1 (Wiederholungen über Mobilfunk, eigener Auftrag SYNC-20261108-019, offen) und MEDIUM 2 (behoben in 0.11.6: `cancelShareRetry` beim Umschalten). Kein Git; Details und Fundstellen in `inbox/development.md` (-003, -008, -019).
- Betroffene Dateien: `sync/WifiHelper.kt`, `sync/SyncWorker.kt`, `sync/SyncScheduler.kt`, `data/GlobalSettings.kt`, `ui/SettingsScreen.kt`, `diag/Diagnostics.kt`, `res/values*/strings_settings.xml`
- Erwartetes Ergebnis: Risikobewertung, ausgeführte Tests mit Ergebnis, Fehler/Testlücken, Empfehlungen.
- Tatsächlich durchgeführte Arbeiten: –
- Ergebnis und Rückmeldung: –
- Abgeschlossen am: –

