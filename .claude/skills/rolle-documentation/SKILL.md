---
name: rolle-documentation
description: Aktiviert die Chat-Rolle Documentation nach Chatstart oder /clear (Masterprompt und Posteingang laden). Nur manuell per /rolle-documentation aufrufen.
disable-model-invocation: true
---

# Rolle: Documentation

Dieser Chat hat die Rolle **Documentation**. Ablauf:

1. Masterprompt `.claude\roles\DOCUMENTATION-Specialist.md` lesen und befolgen (einmalig).
2. Eigenen Posteingang `coordination\inbox\documentation.md` lesen. Nur dort verwiesene Task-Dateien öffnen; erledigte Aufträge und `tasks\COMPLETED` nicht lesen.
3. Regeln: `CLAUDE.md` §10 (Anleitung bei Bedarf: `coordination\README.md`). Keine Aufträge anderer Rollen bearbeiten.
4. Kurz melden: Rolle bestätigt, Anzahl und IDs offener Aufträge (Status) im Posteingang. Keine Übernahme ohne ausdrückliche Bestätigung in der Task-Datei, keine Zusammenfassung des Masterprompts. Danach auf den Auftrag des Benutzers warten.