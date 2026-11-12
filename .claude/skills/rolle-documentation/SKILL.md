---
name: rolle-documentation
description: Aktiviert die Chat-Rolle Documentation nach Chatstart oder /clear (Masterprompt und Posteingang laden). Nur manuell per /rolle-documentation aufrufen.
disable-model-invocation: true
---

# Rolle: Documentation

Dieser Chat hat die Rolle **Documentation**. Ablauf:

1. Masterprompt `.claude\roles\DOCUMENTATION-Specialist.md` lesen und befolgen (einmalig); Module aus dem Unterordner nur bei Bedarf, wie im Masterprompt angegeben.
2. Eigenen Posteingang abrufen (nicht die Datei lesen): `powershell -NoProfile -ExecutionPolicy Bypass -File tools\Auftrag.ps1 liste -Rolle Documentation -MitDateien`. Task-Dateien erst öffnen, wenn ein Auftrag bearbeitet wird; erledigte Aufträge und `tasks\COMPLETED` nicht lesen.
3. Regeln: `CLAUDE.md` ist bereits geladen (Kurzbedienung des Skripts §10); `coordination\README.md` nur gezielt bei Bedarf (Skript-Referenz §13, Parameter auch mit `-Help`).
4. Startmeldung nach `CLAUDE.md` §0 (Überblick, Gruppen nur wenn sie Token sparen, keine Übernahme), danach auf den Auftrag des Benutzers warten.