---
name: qa-emulator-smb-testshare
description: Eigener Python-SMB-Testserver für Emulator-Tests (tools/qa-smb-testshare.ps1) und Emulator-Bedienhinweise für QA
metadata:
  node_type: memory
  type: reference
  originSessionId: 6fa5f839-3c98-4c87-a790-c416904a0c3e
  modified: 2026-11-10T12:26:18.601Z
---

`tools/qa-smb-testshare.ps1` (Start / `-Status` / `-Stop`) startet einen impacket-SMB-Server (127.0.0.1:4445, Freigabe QA, Benutzer qa / qa-test-1) und leitet per `adb reverse tcp:445 tcp:4445` um (braucht `adb root`). In der App `\\127.0.0.1\QA`; `\\localhost\QA` ist ein anderer Hostname auf denselben Server (Hostwechsel-Test, nur GUID-Modi «Nur PC-Kennung»/«Beides»).

**Why:** Die App verbindet fest auf Port 445 (smbj, Erreichbarkeitstest ebenfalls); ein eigener Port im Pfad geht nicht. Der Inhaber wollte zuerst einen eigenen Port statt Umleitung; wegen der App-Beschränkung bleibt die Umleitung.

**How to apply:** UI-Bedienung per adb: Koordinaten vorher mit `uiautomator dump` holen, Backslashes bei `input text` vervierfachen (`'\\\\host\\share'`), Screenshots per `screencap` + `adb pull` (nie per PowerShell-`>`), Hinweise in `.claude/ux/navigation.md`. [[leosync-app-screenshots]]
