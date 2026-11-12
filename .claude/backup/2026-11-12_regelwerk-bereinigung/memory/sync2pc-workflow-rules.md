---
name: sync2pc-workflow-rules
description: "Verbindliche Arbeitsregeln des Inhabers für Sync2PC (GO-Regel, nicht bauen, Nummerierung, Versionierung)"
metadata:
  node_type: memory
  type: feedback
  originSessionId: c8aa7513-d58d-445c-9e52-986c6a690ebc
  modified: 2026-11-10T09:01:01.262Z
---

Die App heisst seit 0.0.12 **LeoSync** (Projektordner/Repo bleiben „Sync2PC“, App-ID com.sync2pc.app). Der Inhaber baut eine Tool-Familie, deren Namen alle mit „Leo“ beginnen.

Massgeblich sind seit 2026-11-09 `CLAUDE.md` (Inhaberregeln, Berechtigungen je Rolle) und seit 2026-11-10 `coordination/README.md` (sechs Chat-Rollen inkl. UX, Aufträge, Übergaben); `.claude/README.md` gibt es nicht mehr. Bei Abweichung gilt `CLAUDE.md`. Ältere Kurzfassung:
- In neuen Chats nur Ideen sammeln und Rückfragen stellen; erst nach "GO" umsetzen. Die Kurzfassung vor dem GO **nur auf Aufforderung des Inhabers** geben und nicht von sich aus nach GO fragen (auch keine GO-Knöpfe im Abschluss) – Inhaber 2026-11-03.
- Rückfragen des Inhabers zuerst beantworten, bevor etwas geändert wird.
- Nie selbst bauen (`build.bat`, `assemble*`) – Inhaber baut die APK; gezielte Tests (`gradlew.bat testDebugUnitTest`) sind laut `CLAUDE.md` erlaubt.
- Rückfragen und Vorschläge nummerieren.
- Seit 2026-11-01 Beta 0.1.x (nur Fixes); neue Funktionen → 0.2.x, 0.3.x … bis 1.0.0. Releases normal, nicht Pre-release.
- Bei jeder Code-Änderung PATCH in version.properties erhöhen; PATCHNOTES.md und übrige Doku pflegt seit 2026-11-09 die Rolle Documentation (per Auftrag); .gitignore aktuell halten.
- Ausnahme (Inhaber 2026-11-03, bei 0.7.0): Build-Fehler einer neuen Version, die noch nie erfolgreich gebaut wurde, beheben **ohne** PATCH zu erhöhen – die Version bleibt (kein eigener Patchnotes-Eintrag).
- Sprache Deutsch (Schweiz, kein ß).
- Übersetzungen aktuell halten (Inhaber 2026-11-03, Regel 15): jeder sichtbare Text in values/ (EN) und values-de/ (DE), nie fest im Code; danach tools/make-translations.ps1 → .claude/translations.md.
- Antworten nach einer Änderung enden immer mit der Versionsnummer in eigener Zeile (z. B. "Version 0.3.3") – Inhaber nutzt sie als Commit-Kommentar.

**Why:** Vom Inhaber im ersten Chat (2026-11-01) als feste Projektregeln vorgegeben.
**How to apply:** Bei jedem Sync2PC-Chat Rolle klären (`CLAUDE.md` §0) und `CLAUDE.md` befolgen; ohne "GO" bzw. klaren Auftrag keine Dateien ändern.
