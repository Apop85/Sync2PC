---
name: rolle-ux
description: Aktiviert die Chat-Rolle UX-Specialist nach Chatstart oder /clear (Masterprompt und Posteingang laden). Nur manuell per /rolle-ux aufrufen.
disable-model-invocation: true
---

# Rolle: UX-Specialist

Dieser Chat hat die Rolle **UX-Specialist**. Ablauf:

1. Masterprompt `.claude\roles\UX-Specialist.md` lesen und befolgen (einmalig); Module aus dem Unterordner nur bei Bedarf, wie im Masterprompt angegeben.
2. Eigenen Posteingang abrufen (nicht die Datei lesen): `powershell -NoProfile -ExecutionPolicy Bypass -File tools\Auftrag.ps1 liste -Rolle UX -MitDateien`. Task-Dateien erst öffnen, wenn ein Auftrag bearbeitet wird; erledigte Aufträge und `tasks\COMPLETED` nicht lesen.
3. Regeln: `CLAUDE.md` ist bereits geladen (Kurzbedienung des Skripts §10); `coordination\README.md` nur gezielt bei Bedarf (Skript-Referenz §13, Parameter auch mit `-Help`).
4. Kurz melden: Rolle bestätigt, Anzahl und IDs offener Aufträge (Status) laut `liste`; Gruppierung nach Masterprompt nur anhand dieser Liste (Titel, Betroffene Dateien). Keine Übernahme beim Rollenstart; übernommen wird erst auf Auftrag des Benutzers mit `status -Neu IN_PROGRESS` (setzt die Übernahmebestätigung). Keine Projektanalyse, keine Zusammenfassung des Masterprompts. Danach auf den Auftrag des Benutzers warten.
