# Documentation-Modul: Benutzerhilfe, Website, Web Developer

Zum Masterprompt `.claude/roles/DOCUMENTATION-Specialist.md`. Lesen, sobald sich sichtbares Verhalten ändert, ein Release- oder Funktionsauftrag ansteht oder `docs/hilfe/*` betroffen ist.

## Benutzerhilfe
- Ändert sich sichtbares Verhalten (Einstellungen, Meldungen, Assistent, Sicherheitsfunktionen, Abläufe), `docs/hilfe/hilfe-de.md` und `docs/hilfe/help-en.md` (Quelle der Wahrheit) in DE und EN anpassen (Schweizer Schreibweise, Begriffe nach `CLAUDE.md` §1). Bei Release- und Funktions-Aufträgen immer prüfen, ob die Hilfe betroffen ist.
- Die Website-Hilfe (`leosync/hilfe.html`, `leosync/en/help.html` im Repo apop85.github.io) richtet sich nach `docs/hilfe/*`. Änderungen an `docs/hilfe/*` dem Benutzer kurz in Stichpunkten melden; Website-Dateien änderst du nicht. Weicht die Website ab, gilt `docs/hilfe/*`; eine Website-Änderung, die dort fehlt, dem Benutzer melden und nach Bestätigung übernehmen.

## Aufträge für den Web Developer (Ausnahme, Benutzerauftrag 2026-11-12)
Haben sich die Hilfedateien geändert oder sind neue Funktionen hinzugekommen, erfasst du einen Auftrag für den Web Developer mit den Änderungen in `C:\Users\rbald\Documents\GitHub\apop85.github.io\.claude\tasks.md`. Diese Datei (nur diese im Website-Repo) darfst du anlegen und bearbeiten (`CLAUDE.md` §1); die Aufträge dort schreibst du von Hand, nicht über `tools\Auftrag.ps1` (das Skript gilt nur für `coordination/` dieses Projekts). Vorher lesen, nur ergänzen, nichts Fremdes überschreiben.
