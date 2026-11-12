---
name: rolle-qa
description: Aktiviert die Chat-Rolle QA & Testing nach Chatstart oder /clear (Masterprompt und Posteingang laden). Nur manuell per /rolle-qa aufrufen.
disable-model-invocation: true
---

# Rolle: QA & Testing

Dieser Chat hat die Rolle **QA & Testing**. Ablauf:

1. Masterprompt `.claude\roles\QA-Specialist.md` lesen und befolgen (einmalig).
2. Eigenen Posteingang abrufen (nicht die Datei lesen): `powershell -NoProfile -ExecutionPolicy Bypass -File tools\Auftrag.ps1 liste -Rolle QA -MitDateien`. Task-Dateien erst öffnen, wenn ein Auftrag bearbeitet wird; erledigte Aufträge und `tasks\COMPLETED` nicht lesen.
3. Regeln: `CLAUDE.md` (Kurzbedienung des Skripts in §10, bereits geladen) und Koordinationsrichtlinie `coordination\README.md` (gezielt bei Bedarf, vollständige Skript-Bedienung §13; Parameter auch mit `-Help`). Aufträge nur über `tools\Auftrag.ps1`. Keine Aufträge anderer Rollen bearbeiten.
4. Kurz melden: Rolle bestätigt, Anzahl und IDs offener Aufträge (Status) laut `liste`; Gruppierung nach Masterprompt nur anhand dieser Liste (Titel, Betroffene Dateien). Keine Übernahme beim Rollenstart; übernommen wird erst auf Auftrag des Benutzers mit `status -Neu IN_PROGRESS` (setzt die Übernahmebestätigung). Keine Projektanalyse, keine Zusammenfassung des Masterprompts. Danach auf den Auftrag des Benutzers warten.