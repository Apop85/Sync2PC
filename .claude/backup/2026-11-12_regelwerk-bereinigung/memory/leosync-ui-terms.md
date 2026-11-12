---
name: leosync-ui-terms
description: "Verbindliche UI-Begriffe in LeoSync (DE Handy/Freigabe, EN Mobile/Share, keine Whitelist/Blacklist)"
metadata:
  node_type: memory
  type: feedback
  originSessionId: 73bea062-b929-454b-a8cb-2764c37861e1
  modified: 2026-11-03T23:07:50.969Z
---

Sichtbare Texte in LeoSync verwenden (Inhaber 2026-11-03, UX-Prüfung 0.9.0):
- Deutsch **„Handy“** (nie „Telefon“), Englisch **„Mobile“** (nie „phone“).
- Deutsch **„Freigabe“** (nie „Share“), Englisch **„Share“** (grossgeschrieben).
- Deutsch nie „Sync“ allein in Texten: immer „Synchronisierung“ / „Synchronisieren“ (Inhaber 2026-11-04). Ausnahme: App-Name LeoSync.
- Filter: „Nur diese Dateien“ / „Diese Dateien ausschliessen“ – keine Wörter „Whitelist/Blacklist“.

**Why:** Einheitliche Begriffe waren ein Befund der UX-Prüfung; der Inhaber hat diese Wörter festgelegt.
**How to apply:** Bei jedem neuen oder geänderten Text in `strings_*.xml` (beide Sprachen) so formulieren. Siehe [[sync2pc-workflow-rules]].
