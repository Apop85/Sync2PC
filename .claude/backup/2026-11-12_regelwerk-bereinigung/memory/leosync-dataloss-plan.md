---
name: leosync-dataloss-plan
description: "Mit Inhaber vereinbarte Massnahmen gegen Datenverlust im Ordner-Sync (2026-11-03), noch ohne GO"
metadata:
  node_type: memory
  type: project
  originSessionId: 0f19a98c-e217-4a71-ba4c-eff6a525c1ce
  modified: 2026-11-12T14:30:23.457Z
---

**Historisch (Stand 2026-11-12, App 0.16.x):** Plan aus 0.4/0.5. Umsetzungsstand und gültige Regeln stehen in `docs/DECISIONS.md` (massgeblich); diese Notiz nur als Hintergrund nutzen, nie als offenen Auftrag.

Vereinbart im Chat „Beta 0.5.x“ am 2026-11-03:

1. Schutzbremse: Basisordner fehlt (PC oder Handy) -> Lauf stoppt, nichts neu anlegen; Optionen: neuen Ordner wählen / neu anlegen + Erstabgleich ohne Löschen / Abbrechen. >20 % der Dateien würden gelöscht -> stoppen, Warnung, „Trotzdem ausführen“/„Abbrechen“; im Hintergrund als Hinweis, wartet. Leere SAF-Abfrage (query null) = Fehler, nicht leer.
2./4. Ordnerwechsel (Profil oder Schutzbremse): Stichprobe (~20 zufällige mirror_state-Dateien + oberste Ordner, Existenz + Grösse). Alles ok -> Status behalten. Sonst nur einfache Warnung „richtiger Ordner?“ (keine Zahlen/Liste!) mit: Anderen Ordner wählen / Trotzdem übernehmen / Als neuen Ordner behandeln (Status zurücksetzen) / Abbrechen.
3. Importierte Ordner-Profile warten auf ersten manuellen Start.
4. trashOther: mirror_state der Datei vor dem Verschieben löschen.
5. Upload-Fortsetzen nur bei passender Temp-Datei, sonst neu; nach fortgesetztem Upload Teil-Hash prüfen.
6. Direkt vor dem Ersetzen Zielstand prüfen; geändert -> überspringen (nächster Lauf = Konflikt).
7. Handy-Schreiben: alt -> `<name>.s2pold`, tmp -> name, dann .s2pold löschen/Papierkorb; Rollback bei Fehler; Aufräumen beim nächsten Lauf; *.s2pold vom Sync ausschliessen.
8. Mirror-Inhaltsvergleich: bis 32 MB voller Hash, darüber 32 Stichproben à 64 KB. Foto-Sync unverändert.
9. SpaceFreer: Fallback-Suche in Nachbarordnern zusätzlich Teil-Hash.
10. Versionsschutz: Schalter pro Profil „Alte Versionen im Papierkorb behalten“ (Standard an, beide Seiten, bestehende Papierkorb-Frist). Neue Ansicht im Profil „Papierkorb & alte Versionen“: Wiederherstellen auf BEIDEN Seiten (aktuelle Version zuerst in Papierkorb, dann Status setzen), auch gelöschte Dateien, auch mehrere/ganzer Ordner/„alles vom Datum X“. PC muss erreichbar sein.
11. Verschachtelte Ordner-Profile: Warnung.

0.4.2 (umgesetzt): kleine Profile (≤ 5) nur wenn alle; ab 100 Löschungen immer; ganzer Unterordner > 5 Dateien; Prozent pro Profil (DB v4 massDeletePercent), Karte „Schutzbremse (optional)“ am Ende – im 0.5.0-Assistenten als letzte optionale Einstellung. 0.7.0 vorgemerkt: Option „auch über mobile Daten/VPN synchronisieren“.
0.6.0 geplant (noch kein GO): Tool „Papierkorb leeren“ – Details in .claude/README.md „Stand“. Damit Play-Vorbereitung 0.7.0, PC-Erkennung (+ mobile Daten/VPN) 0.8.0.
Versionen (Inhaber einverstanden 2026-11-03): **0.4.1** = Punkte 1–9, 11 + neues Zustandsbild `.claude/attention.png` → `dog_attention` (wartet auf Rückmeldung des Benutzers). **0.5.0** = Punkt 10 (Versionsschutz + Wiederherstellen). 0.6.0 Play-Vorbereitung, 0.7.0 PC-Erkennung.

0.5.0 Zusatz (Inhaber 2026-11-03): Startseite bekommt unterhalb „Profile“ eine neue Sektion **„Tools“** mit „Speicher freigeben“ und den Wiederherstellungsmöglichkeiten (nur wenn Versionsschutz aktiviert). Schöner darstellen als reine Links (z. B. Kacheln/Karten mit Icon + Kurztext). Wiederherstellen ist profilbezogen: Beim Öffnen zuerst Profil wählen (nur eins vorhanden → direkt öffnen), dann Wiederherstellung in diesem Profil. 0.4.1 hat GO bekommen (2026-11-03).
0.5.0 Zusatz 2: Profil-Assistent in einzelnen Fenstern/Schritten: 1. Profilname, 2. Quelle (Quellordner Telefon), 3. Ziel, 4. Sync-Optionen (4.1 WLAN/automatischer Sync, 4.2 nach erfolgreicher Übertragung), 5. Filter (als optional kennzeichnen). Erklärungen präziser: Was stellt man hier ein und warum. Inhaber fragte, ob WLAN-Name Pflicht ist (VPN-Nutzer). Die Karten in der Profilansicht (Bearbeiten) in derselben Reihenfolge wie die Assistent-Schritte anordnen. Ab dem ersten optionalen Schritt direkt „Speichern“ anbieten (optionale Schritte nicht durchklicken müssen).
Kurzfassung 0.5.0 vom Inhaber geprüft (2026-11-03): ok, mit Änderung zu Punkt 7: PC nicht erreichbar → Wiederherstellung vormerken und beim nächsten Lauf mit PC ausführen (nicht blockieren). Noch kein GO für 0.5.0.

**Why:** Inhaber: „Es darf nicht zu ungewollten Datenverlusten führen.“
**How to apply:** Bei Umsetzung genau so; Versionsnummer mit Inhaber klären. Siehe [[leosync-release-roadmap]].
