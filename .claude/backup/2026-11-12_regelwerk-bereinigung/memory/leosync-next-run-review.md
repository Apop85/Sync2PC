---
name: leosync-next-run-review
description: "Offener Auftrag (2026-11-04): LeoSync mit design-skills und claude-security prüfen, nur Vorschläge"
metadata:
  node_type: memory
  type: project
  originSessionId: 73bea062-b929-454b-a8cb-2764c37861e1
  modified: 2026-11-12T14:30:15.902Z
---

Inhaber hat am 2026-11-04 die Skills **design-skills** und **claude-security** hinzugefügt und beauftragt: LeoSync damit prüfen und **nummerierte Vorschläge** machen, **nichts umsetzen**. Wegen fehlender Token verschoben auf den nächsten Lauf (Stand 0.10.0).

Geplanter Umfang:
- design-skills: design-critique, accessibility-audit (WCAG 2.2 auf Android übertragen), ux-writing (Texte DE/EN), interaction-design.
- claude-security: Schwerpunkte Zugangsdaten (`CredentialCrypto`, Keystore), Sicherung mit Export-Passwort (`BackupCodec`), SMB-Verbindung, Selbst-Update (`UpdateInstaller`, `REQUEST_INSTALL_PACKAGES`), FileProvider/Teilen, PendingIntents, exportierte Komponenten. Ein voller Scan ist sehr tokenintensiv: vorher mit dem Inhaber Umfang klären.

**Stand 2026-11-08:** Design-Prüfung erledigt und in 0.11.0 umgesetzt. claude-security lief nicht: braucht ausdrückliche Kosten-Zustimmung des Inhabers und die Einstellung „Dynamic workflows“ (`/config`), als Unteragent ohne Workflow-Werkzeug nicht möglich. Inhaber startet den Scan selbst in seiner Hauptsitzung.

**Why:** Auftrag des Inhabers, bewusst vertagt.
**How to apply:** Nicht von sich aus anbieten (Stand 2026-11-12: Design erledigt, Security-Scan startet der Inhaber selbst). Nur auf Nachfrage; Ergebnisse nur als Vorschläge, Umsetzung erst nach GO. Siehe [[sync2pc-workflow-rules]].
