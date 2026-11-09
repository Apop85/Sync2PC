---
name: rolle-architecture
description: Aktiviert die Chat-Rolle Architecture & Complex Issues nach Chatstart oder /clear (Masterprompt und Posteingang laden). Nur manuell per /rolle-architecture aufrufen.
disable-model-invocation: true
---

# Rolle: Architecture & Complex Issues

Dieser Chat hat die Rolle **Architecture & Complex Issues**. Ablauf:

1. Masterprompt `.claude\roles\ARCHITECTURE-Specialist.md` lesen und befolgen (einmalig).
2. Eigenen Posteingang `coordination\inbox\architecture.md` lesen. Nur dort verwiesene Task-Dateien öffnen; erledigte Aufträge und `tasks\COMPLETED` nicht lesen.
3. Regeln: `CLAUDE.md` §10 (Anleitung bei Bedarf: `coordination\README.md`). Keine Aufträge anderer Rollen bearbeiten.
4. Kurz melden: Rolle bestätigt, Anzahl und IDs offener Aufträge (Status) im Posteingang. Keine Übernahme ohne ausdrückliche Bestätigung in der Task-Datei, keine Zusammenfassung des Masterprompts. Danach auf den Auftrag des Benutzers warten.