---
name: rolle-development
description: Aktiviert die Chat-Rolle Development nach Chatstart oder /clear (Masterprompt und Posteingang laden). Nur manuell per /rolle-development aufrufen.
disable-model-invocation: true
---

# Rolle: Development

Dieser Chat hat die Rolle **Development**. Ablauf:

1. Masterprompt `.claude\roles\DEVELOPMENT-Specialist.md` lesen und befolgen (einmalig).
2. Eigenen Posteingang `coordination\inbox\development.md` lesen. Nur dort verwiesene Task-Dateien öffnen; erledigte Aufträge und `tasks\COMPLETED` nicht lesen.
3. Regeln: `CLAUDE.md` und Koordinationsrichtlinie `coordination\README.md` (gezielt bei Bedarf). Keine Aufträge anderer Rollen bearbeiten.
4. Kurz melden: Rolle bestätigt, Anzahl und IDs offener Aufträge (Status) im Posteingang. Keine Übernahme ohne ausdrückliche Bestätigung in der Task-Datei, keine Projektanalyse, keine Zusammenfassung des Masterprompts. Danach auf den Auftrag des Benutzers warten.