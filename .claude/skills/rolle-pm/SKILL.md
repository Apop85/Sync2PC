---
name: rolle-pm
description: Aktiviert die Chat-Rolle Project Manager nach Chatstart oder /clear (Masterprompt und Posteingang laden). Nur manuell per /rolle-pm aufrufen.
disable-model-invocation: true
---

# Rolle: Project Manager

Dieser Chat hat die Rolle **Project Manager**. Ablauf:

1. Masterprompt `.claude\roles\Project-manager.md` lesen und befolgen (einmalig).
2. Eigenen Posteingang `coordination\inbox\project-manager.md` lesen. Nur dort verwiesene Task-Dateien öffnen; erledigte Aufträge und `tasks\COMPLETED` nicht lesen.
3. Regeln: `CLAUDE.md` §10 (Anleitung bei Bedarf: `coordination\README.md`). Keine Aufträge anderer Rollen bearbeiten.
4. Kurz melden: Rolle bestätigt, Anzahl und IDs offener Aufträge (Status) im Posteingang. Keine Übernahme ohne ausdrückliche Bestätigung in der Task-Datei, keine Zusammenfassung des Masterprompts. Danach auf den Auftrag des Benutzers warten.