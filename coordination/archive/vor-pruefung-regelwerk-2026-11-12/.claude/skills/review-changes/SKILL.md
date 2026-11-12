---
name: review-changes
description: Unabhängiges Review der aktuellen Änderungen in LeoSync über den Agent code-reviewer (Architektur, Seiteneffekte, Datenverlust, Tests), ohne eigene Codeänderungen. Verwenden nach Implementierungen oder vor Abschluss einer Aufgabe.
---

# Änderungen prüfen

Fokus (optional): $ARGUMENTS

Dieser Skill ändert **keinen** Code.

1. **Änderungen ermitteln**: geänderte Dateien aus Gespräch/Auftrag nehmen oder per `LastWriteTime` eingrenzen (Git ist nicht vorhanden, daher können keine Git-Befehle ausgeführt werden); Vorher-Stand bei Bedarf aus einer genannten Sicherung.
2. **Komponenten bestimmen**: Sync-Engine, Datenbank, ViewModel/Repository, UI, Netzwerk/SMB, Worker.
3. **Prüfumfang festlegen**: nur relevante Doku-Abschnitte benennen (`docs/ARCHITECTURE.md`, `docs/SYNC-ENGINE.md`, `docs/DATA-MODEL.md`, `docs/DECISIONS.md`); vorhandene Tests der Komponenten in `docs/TESTING.md` nachschlagen.
4. **Agent `code-reviewer`** per Agent-Tool einsetzen mit: Dateiliste bzw. Diff-Befehl, Komponenten, relevante Doku-Abschnitte, Hinweis auf Datenverlust-Fokus. Nicht die Zusammenfassung des Implementierers als Wahrheit mitgeben. Nur ein Aufruf, ausser der Umfang ist sehr gross (dann nach Komponenten teilen, lesend, daher parallel zulässig).
5. **Ergebnis zusammenfassen**: nach CRITICAL / HIGH / MEDIUM / LOW / INFO, mit Datei:Zeile; Fehlalarme anhand des Codes kurz gegenprüfen; fehlende Tests nennen.

Keine Korrekturen durchführen; Behebung nur auf ausdrücklichen Auftrag.
