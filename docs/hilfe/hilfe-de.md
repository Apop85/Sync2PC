# LeoSync – Hilfe

LeoSync sichert Fotos, Videos und beliebige Ordner vom Handy auf deinen Windows-PC – direkt im Heimnetz, ohne Cloud und ohne Konto. Die Dateien gehen nur zwischen Handy und PC.

## 1. Auf dem PC: Ordner freigeben

1. Im Explorer mit der rechten Maustaste auf den Zielordner (z. B. `D:\Fotos`) → **Eigenschaften** → **Freigabe** → **Erweiterte Freigabe**.
2. **Diesen Ordner freigeben** anhaken und einen Freigabenamen vergeben (z. B. `Fotos`).
3. Unter **Berechtigungen** deinem Windows-Benutzer **Ändern** und **Lesen** erlauben.
4. Die IP-Adresse des PCs findest du mit `ipconfig` in der Eingabeaufforderung (z. B. `192.168.1.20`). Am besten im Router eine feste Adresse für den PC einrichten.

## 2. In LeoSync: Profil anlegen

Tippe auf **Profil hinzufügen** und wähle den Typ:

- **Fotos & Videos sichern** – von der Kamera auf den PC, automatisch sortiert nach Jahr, Monat und Reiseland (z. B. `2026\06 - Ägypten`). Auf Wunsch werden die Fotos danach vom Handy entfernt.
- **Ordner synchronisieren** – ein beliebiger Ordner 1:1, in eine oder beide Richtungen.

Der Assistent führt Schritt für Schritt durch Name, Quelle, Ziel und Optionen. Beim Ziel genügt der PC (`\\192.168.1.20`) mit Benutzer und Passwort; **Ordner auf dem PC wählen…** zeigt dann die Freigaben und Ordner zur Auswahl. Mit **Verbindung testen** siehst du sofort, ob alles stimmt.

Neue Profile starten erst, wenn du einmal **Synchronisieren** tippst – du kannst vorher **simulieren**, was passieren würde.

## 3. Im Alltag

- **Automatisch:** Sobald das Handy mit einem eingetragenen WLAN verbunden ist, synchronisiert LeoSync im Hintergrund. Ohne WLAN-Namen nur, wenn du es auslöst.
- **Von Hand:** **Synchronisieren** tippen oder auf der Startseite ganz oben nach unten ziehen.
- **Pausieren:** ⏸ oben rechts – für eine Zeit oder „für immer“ (nur noch von Hand). ▶ setzt fort.
- **Der Hund** zeigt den Zustand: schläft = alles gesichert, läuft = Synchronisierung läuft, steht = wartet auf deine Entscheidung, sitzt = wartet (z. B. auf das WLAN).

## 4. Sicherheit deiner Dateien

- **Prüfung nach dem Hochladen:** LeoSync liest die Datei auf dem PC zurück und vergleicht sie mit dem Original. Stufen von *Aus* bis *Komplett*; Standard ist *Mittel*. Wer Fotos vom Handy entfernen lässt, prüft immer.
- **Papierkorb:** Bei Ordner-Profilen landen gelöschte und überschriebene Dateien im Ordner `.LeoSync-Bin` (auf Handy und PC) und lassen sich über **Tools → Papierkorb** zurückholen. Nach 30 Tagen (einstellbar) wird er geleert.
- **Schutzbremse:** Würden auf einmal viele Dateien gelöscht (Standard: mehr als 20 %) oder fehlt ein ganzer Ordner, hält LeoSync an und fragt dich zuerst.
- **Konflikte:** Wurde dieselbe Datei auf Handy und PC geändert, entscheidest du, welche Version gilt. Die andere kommt in den Papierkorb.

## 5. Häufige Meldungen

| Meldung | Was tun? |
|---|---|
| Benutzer oder Passwort stimmt nicht | Windows-Benutzer und Passwort prüfen. Bei einem Microsoft-Konto die E-Mail-Adresse als Benutzer verwenden. |
| Diese Freigabe gibt es auf dem PC nicht | Freigabenamen prüfen oder mit **Ordner auf dem PC wählen…** auswählen. |
| Kein Zugriff auf diesen Ordner | In den Freigabe-Berechtigungen dem Benutzer **Ändern** erlauben. |
| Der PC ist nicht erreichbar | PC eingeschaltet (nicht im Ruhezustand)? Handy im selben WLAN? Firewall erlaubt „Datei- und Druckerfreigabe“? |
| Der PC-Name wurde nicht gefunden | Statt des Namens die IP-Adresse eintragen, z. B. `\\192.168.1.20`. |
| PC-Ordner nicht gefunden (Schutzbremse) | Wurde der Ordner umbenannt oder verschoben? Zurückbenennen und **Erneut prüfen** – oder einen neuen Ordner wählen. |

## 6. Synchronisierung bricht im Hintergrund ab

Viele Hersteller beenden Apps im Hintergrund sehr streng. In LeoSync unter **Einstellungen → App-Berechtigungen** stehen die nötigen Schritte für dein Gerät (Akku-Optimierung aus, Hintergrundaktivität erlauben). Mehr dazu: [dontkillmyapp.com](https://dontkillmyapp.com).

## 7. Datenschutz

LeoSync hat keinen Server und sammelt keine Daten. Passwörter werden verschlüsselt auf dem Handy gespeichert (Android-Keystore). Diagnosedaten verlassen das Handy nur, wenn du sie selbst teilst.

## Kontakt

Fragen oder Probleme: [leotools@protonmail.com](mailto:leotools@protonmail.com)
