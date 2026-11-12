---
name: powershell-alias-falle
description: "PowerShell-Funktionen nie wie Aliase/Cmdlets benennen (rd/ri/del = Remove-Item); oberste Regel in CLAUDE.md §1, seit 2026-11-12 per Hook gesperrt"
metadata:
  node_type: memory
  type: feedback
  originSessionId: ba5ac881-f187-44fc-96b8-b86e2b283225
  modified: 2026-11-12T14:28:06.036Z
---

In jedem PowerShell-Befehl, den ich ausführe (auch kurze Lese-/Schreib-/Ersetzbefehle), und in jedem Skript nie eine Funktion (`function`/`filter`) mit einem Alias- oder Cmdlet-Namen definieren (`rd`, `ri`, `del`, `rm`, `erase`, `r`, `sc`, `ac`, `gc`, `cp`, `mv`, `sl` …). Aliase haben Vorrang vor Funktionen: `function rd {…}` und `rd $p` ruft Remove-Item auf. Lieber ganz ohne eigene Funktionen, sonst Verb-Nomen-Namen (`Read-FileText`) oder Skriptblock-Variable (`$teil = {…}; & $teil`).

**Why:** Der Benutzer hat mehrfach Datenverlust erlebt (zuletzt `Rd $p` löschte zwei Task-Dateien), und trotz Regel gab es erneut Verstösse. Deshalb oberste Regel in CLAUDE.md §1 und technische Sperre im Hook `tools/Auftrag-guard.ps1` (SYNC-20261112-263).

**How to apply:** Lehnt der Hook einen Befehl ab, Namen ändern, nie umgehen. Testbefehle, die solche Muster nur als Text enthalten, aus Teilstrings bauen ('func' + 'tion'), sonst blockt der Hook zu Recht. Weitere PowerShell-Fallen: Array-Abflachung bei `@(@('a','b'))` (ein einzelnes Paar wird zu zwei Strings; Tuple oder `,@()` verwenden). Siehe [[sync2pc-workflow-rules]].
