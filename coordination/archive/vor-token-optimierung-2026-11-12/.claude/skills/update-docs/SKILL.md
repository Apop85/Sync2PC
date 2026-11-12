---
name: update-docs
description: Aktualisiert die technische Dokumentation von LeoSync (docs/DECISIONS, ARCHITECTURE, SYNC-ENGINE, DATA-MODEL, TESTING, PATCHNOTES) minimal und strukturerhaltend nach einer Änderung. Verwenden, wenn dokumentiertes Verhalten, Schema, Tests oder Entscheidungen sich geändert haben.
---

# Dokumentation aktualisieren

Anlass: $ARGUMENTS

Zuordnung (Dateien mit Unterstrich in `docs/` sind Auftragstexte, nicht anfassen):

| Inhalt | Datei |
|---|---|
| Entscheidungen | `docs/DECISIONS.md` |
| Architektur | `docs/ARCHITECTURE.md` |
| Synchronisation | `docs/SYNC-ENGINE.md` |
| Datenbank | `docs/DATA-MODEL.md` |
| Tests/Build | `docs/TESTING.md` |
| Releases | `PATCHNOTES.md` (Projektwurzel) |

Vorgehen:

0. **Rollenberechtigung prüfen**:
   - Änderungen an Dokumentationsdateien darf ausschliesslich die Rolle Documentation durchführen.
   - Dieser Skill ist ausschliesslich für die Rolle Documentation vorgesehen.
   - Andere Rollen ermitteln Dokumentationsbedarf im Rahmen ihrer eigenen Aufgaben und übergeben diesen gemäss `coordination/README.md` direkt an Documentation (Folgeauftrag `tools\Auftrag.ps1 erstellen -An Documentation`, `CLAUDE.md` §10).
   - Die Rolle Documentation führt die eigentliche Dokumentationsänderung durch.

1. Prüfen, ob die Änderung dokumentiertes Verhalten tatsächlich berührt. Wenn nein: nichts ändern und das sagen.
2. Zieldatei per Grep nach dem betroffenen Abschnitt durchsuchen; nur diesen Abschnitt lesen und ändern (Dateien sind sehr gross).
3. Nur Notwendiges ändern; Struktur, Kennzeichnungen (z. B. [IMPL], [RISIKO]) und vorhandene DEC-IDs erhalten. Neue Entscheidung = nächste freie DEC-ID (vorher höchste per Grep ermitteln), bestehende IDs nie umnummerieren oder wiederverwenden.
4. Dokumentiert wird nur der implementierte Zustand; Geplantes nur in den dafür vorgesehenen Abschnitten und als solches gekennzeichnet. Nichts behaupten, was nicht im Code steht.
5. `PATCHNOTES.md` nur bei Release-/Versionsänderung (neueste Version oben, Deutsch, gemäss `CLAUDE.md` §1), nicht bei jeder gewöhnlichen Aufgabe. Version in `version.properties` und Patchnotes gehören zusammen.
6. Kurz berichten: geänderte Dateien/Abschnitte, neue DEC-IDs, bewusst nicht geänderte Doku.
