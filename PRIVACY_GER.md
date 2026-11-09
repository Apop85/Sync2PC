# Datenschutzerklärung – LeoSync

**Stand:** 9. November 2026  
**Version:** 1.1

Diese Datenschutzerklärung informiert darüber, welche Daten durch die Android-App LeoSync verarbeitet werden und wie mit diesen Daten umgegangen wird.

LeoSync ist eine Anwendung der Marke Leo und wurde mit dem Ziel entwickelt, Fotos und Videos ohne externe Cloud-Dienste zwischen Android-Geräten und eigenen Netzwerkfreigaben zu synchronisieren.

## 1. Verantwortlicher

Verantwortlich für die Datenverarbeitung:

- **Entwickler:** [Vor- und Nachname]
- **Land:** Schweiz
- **Kontakt:** leotools@protonmail.com

## 2. Grundsätze der Datenverarbeitung

LeoSync wurde nach dem Grundsatz entwickelt, möglichst wenige Daten an externe Stellen zu übertragen.

Die Verarbeitung von Fotos, Videos, Metadaten und Synchronisationsinformationen erfolgt grundsätzlich lokal auf dem Android-Gerät.

Dateien werden direkt zwischen dem Android-Gerät und der vom Nutzer konfigurierten SMB-Netzwerkfreigabe übertragen.

LeoSync verwendet keine:

- Externen Cloud-Speicherdienste
- Werbenetzwerke
- Analytics- oder Tracking-Dienste
- Externen Synchronisationsdienste
- Werbe- oder Analyse-SDKs

Es findet keine automatische Übermittlung von Nutzungsstatistiken oder persönlichen Daten an den Entwickler statt.

Einzige Verbindung ins Internet ausserhalb der SMB-Synchronisierung ist die Prüfung auf App-Updates bei GitHub (siehe Abschnitt 8).

## 3. Fotos und Videos

LeoSync benötigt Zugriff auf die vom Nutzer zur Synchronisierung ausgewählten Fotos und Videos.

Die Dateien werden lokal auf dem Android-Gerät verarbeitet und direkt an die konfigurierte SMB-Netzwerkfreigabe übertragen.

Es erfolgt keine Übertragung über einen von LeoSync oder dem Entwickler betriebenen Cloud-Dienst.

Abhängig vom gewählten Synchronisationsmodus können Dateien auch vom SMB-Server auf das Android-Gerät übertragen werden.

Die Speicherung und Verwaltung der übertragenen Dateien erfolgt auf den vom Nutzer ausgewählten Speicherorten.

## 4. EXIF-Metadaten und Standortinformationen

LeoSync kann GPS-Koordinaten aus den EXIF-Metadaten von Fotos auslesen.

Diese Informationen werden lokal verarbeitet und mit einer integrierten Offline-Datenbank abgeglichen, um beispielsweise das Land zu bestimmen, in dem ein Foto aufgenommen wurde.

Dafür werden keine externen Geolocation-, Karten- oder Standortdienste kontaktiert.

GPS-Koordinaten werden nicht automatisch an den Entwickler übertragen.

Bei der Übertragung von Originalfotos können deren ursprüngliche EXIF-Metadaten Bestandteil der übertragenen Dateien bleiben.

### 4.1 WLAN-Name und Standortberechtigung

Damit automatische Synchronisierungen nur in den vom Nutzer festgelegten WLAN-Netzen starten, liest LeoSync den Namen (SSID) des aktuell verbundenen WLANs. Android gibt diesen Namen nur mit der Standortberechtigung heraus. Für den automatischen Start im Hintergrund wird die Berechtigung «Immer zulassen» benötigt, ausserdem muss der Standortdienst eingeschaltet sein. Zusätzlich benötigt die App Zugriff auf den Medienstandort (Berechtigung für GPS-Daten in Fotos, siehe Abschnitt 4).

Der WLAN-Name wird ausschliesslich lokal auf dem Gerät ausgewertet und nicht an den Entwickler oder Dritte übertragen. LeoSync erstellt keine Standortverläufe und verwendet den Gerätestandort nicht für andere Zwecke. Die Berechtigung kann jederzeit in den Android-Einstellungen widerrufen werden; die automatische Synchronisierung über den WLAN-Namen ist dann nicht möglich, die manuelle Synchronisierung bleibt nutzbar.

## 5. SMB-Verbindungen und Zugangsdaten

Für die Verbindung mit einer SMB-Netzwerkfreigabe benötigt LeoSync die vom Nutzer angegebenen Verbindungsinformationen.

Dazu gehören gegebenenfalls:

- Serveradresse
- Freigabepfad
- Benutzername
- Passwort
- Weitere Verbindungsparameter

SMB-Passwörter werden mithilfe des Android Keystore geschützt gespeichert.

Die Zugangsdaten werden nicht an den Entwickler oder externe Cloud-Dienste übertragen.

Die Dateiübertragung erfolgt direkt zwischen dem Android-Gerät und dem konfigurierten SMB-Server.

Die Sicherheit der Verbindung hängt zusätzlich von der Netzwerkkonfiguration und den Sicherheitseinstellungen des jeweiligen SMB-Servers ab.

## 6. Diagnose- und Fehlerberichte

LeoSync bietet eine Funktion zum Export technischer Diagnoseberichte.

Diese Berichte dienen dazu, Fehler zu untersuchen, Kompatibilitätsprobleme zu erkennen und die Stabilität der Anwendung zu verbessern.

Ein Diagnosebericht kann abhängig von der Implementierung und dem jeweiligen Gerät folgende Informationen enthalten:

### 6.1 Geräte- und Anwendungsinformationen

- Gerätehersteller und Gerätemodell
- Android-Version und SDK-Version
- App-Version und Build-Informationen
- Hersteller- und Betriebssystemversion

### 6.2 Systemstatus und Berechtigungen

- Akkustand und Ladezustand
- Status der Akkuoptimierung
- Energiespar- und Hintergrundausführungsstatus
- Status relevanter App-Berechtigungen
- Verfügbare Ordnerfreigaben

### 6.3 Netzwerk- und Synchronisationsinformationen

- WLAN-Verbindungsstatus
- Netzwerktyp und Verbindungsstatus
- Synchronisationseinstellungen
- Konfigurierte Synchronisationsprofile
- Technischer Status der SMB-Verbindungen
- Synchronisationsverlauf und Fehlerzustände

### 6.4 Technische Protokolle

- Hintergrundaufträge und WorkManager-Status
- Warnungen und Fehlermeldungen
- Informationen über abgebrochene Prozesse
- Synchronisationsstatistiken
- Zeitstempel relevanter Ereignisse

Diagnoseberichte («Problem melden», «Diagnosedaten kopieren») werden vor der Ausgabe pseudonymisiert und maskiert: Benutzernamen, IP-Adressen, Rechner-, Freigabe-, WLAN-, Profil- und Ordnernamen, Pfade und genaue Dateinamen werden durch Platzhalter (z. B. «Host-1», «WLAN-1») ersetzt; Freitext wie Fehlermeldungen wird zusätzlich nach Mustern maskiert (z. B. IP-Adressen, E-Mail-Adressen, Pfade, Koordinaten). Erhalten bleiben unter anderem Gerätemodell, Android-Version, Einstellungen, Zähler, Jahr-/Monat-Ordner, Standardordner wie DCIM sowie Dateiendungen. Die Maskierung kann nicht ausschliessen, dass einzelne Angaben in ungewöhnlichen Textformaten unmaskiert bleiben.

**SMB-Passwörter werden nicht in Diagnoseberichte aufgenommen.**

Der exportierte **Verlauf** («Verlauf teilen») ist davon ausgenommen: Er ist ein Export des Nutzers für sich selbst und kann IP-Adressen, Serverangaben und Dateinamen enthalten. Er sollte vor einer Weitergabe geprüft werden.

Die letzten Abstürze der App werden mit gekürztem Fehlerprotokoll lokal auf dem Gerät gespeichert und sind nur im Diagnosebericht enthalten.

### 6.5 Pseudonyme Installationskennung

Diagnoseberichte können eine zufällig generierte Installationskennung (UUID) enthalten.

Diese UUID:

- Wird lokal auf dem Gerät erzeugt.
- Wird nicht aus Hardware- oder Gerätekennungen abgeleitet.
- Enthält keine unmittelbar personenbezogenen Informationen.
- Dient dazu, mehrere Diagnoseberichte derselben App-Installation einander zuzuordnen.

Bei einer Neuinstallation der App wird eine neue Installationskennung erzeugt.

Die UUID ermöglicht keine direkte Identifizierung einer Person. Da sie jedoch zur Zuordnung mehrerer Berichte verwendet werden kann, ist sie als pseudonyme Kennung zu betrachten.

## 7. Versand von Diagnoseberichten

Diagnoseberichte werden nicht automatisch übertragen.

Der Nutzer entscheidet selbst, ob er einen Bericht exportiert und per E-Mail an den Entwickler sendet. Die Funktion «Problem melden» öffnet dazu die E-Mail-Anwendung mit vorbereitetem Text und Diagnosebericht; gesendet wird erst, wenn der Nutzer die E-Mail selbst absendet.

Der Versand erfolgt über die vom Nutzer ausgewählte E-Mail-Anwendung beziehungsweise den gewählten E-Mail-Dienst.

Mit dem Versand können die im Diagnosebericht enthaltenen technischen Informationen an den Entwickler übermittelt werden.

Vor dem Versand sollte der Nutzer den Bericht prüfen und berücksichtigen, dass technische Angaben Rückschlüsse auf seine Netzwerk- und Geräteumgebung ermöglichen können.

Für die Verarbeitung der E-Mail gelten zusätzlich die Datenschutzbestimmungen des jeweiligen E-Mail-Anbieters.

Der Entwickler verwendet freiwillig übermittelte Diagnoseberichte ausschliesslich zur Untersuchung technischer Probleme und zur Verbesserung von LeoSync.

## 8. Drittanbieter und externe Dienste

LeoSync verwendet keine externen Analytics-, Werbe-, Tracking- oder Cloud-SDKs.

Es findet keine automatische Weitergabe von Nutzerdaten an Werbe- oder Analyseanbieter statt.

### 8.1 Update-Prüfung über GitHub

Die über GitHub verteilte Variante der App prüft regelmässig (täglich, im WLAN) und beim Öffnen der Update-Seite, ob auf GitHub (api.github.com, öffentliches Repository von LeoSync) eine neuere Version veröffentlicht ist. Dabei wird eine Anfrage an GitHub gesendet; GitHub erhält technisch bedingt die IP-Adresse des Geräts sowie die Angabe «LeoSync» mit der installierten App-Version (User-Agent). Weitere Nutzerdaten, Profile, Dateien oder Zugangsdaten werden nicht übermittelt. Bei einer neueren Version lädt die App die Installationsdatei erst nach Tippen des Nutzers von GitHub herunter und installiert sie über den Android-Paketinstaller. Es gelten die Datenschutzbestimmungen von GitHub.

### 8.2 Hilfe und Produktseite

Der Menüpunkt «Hilfe & Produktseite» öffnet die Produktseite von LeoSync (apop85.github.io/leosync) im Browser des Nutzers. Es werden keine Daten aus der App übergeben.

### 8.3 Weitere Datenabflüsse

Daten können das Gerät insbesondere im Rahmen folgender, vom Nutzer veranlasster Aktionen verlassen:

- Synchronisierung mit der konfigurierten SMB-Netzwerkfreigabe.
- Freiwilliger Versand eines Diagnoseberichts per E-Mail.
- Teilen des Verlaufs oder einer Sicherungsdatei über das Android-Teilen-Menü an eine vom Nutzer gewählte App.

Das Android-Betriebssystem sowie vom Nutzer verwendete E-Mail- und Netzwerkdienste können im Rahmen ihrer eigenen Funktionen zusätzliche technische Daten verarbeiten. Dafür gelten die jeweiligen Datenschutzbestimmungen der Anbieter.

## 9. Datensicherheit

LeoSync verwendet die vom Android-Betriebssystem bereitgestellten Sicherheitsmechanismen.

Insbesondere werden gespeicherte SMB-Passwörter mithilfe des Android Keystore geschützt.

Die Anwendung ist darauf ausgelegt, unnötige Datenübertragungen zu vermeiden und die Verarbeitung möglichst lokal durchzuführen.

Die Sicherheit der Dateiübertragung hängt zusätzlich von folgenden Faktoren ab:

- Sicherheit des lokalen Netzwerks
- Konfiguration des SMB-Servers
- Verwendete SMB-Version
- Aktivierte Authentifizierungs- und Verschlüsselungsmechanismen

Nutzer sollten ihre Netzwerkfreigaben angemessen absichern und SMB-Dienste nicht ungeschützt über das öffentliche Internet erreichbar machen.

## 10. Speicherung und Löschung

LeoSync betreibt keinen eigenen Cloud-Speicher für Nutzerdaten.

Dateien werden auf dem Android-Gerät und den vom Nutzer konfigurierten SMB-Speicherorten gespeichert.

**Sicherung (Export/Import):** LeoSync kann Profile, Einstellungen und optional den Synchronisationsstatus in eine Datei exportieren. Sie enthält unter anderem Serveradressen, Freigabepfade und Benutzernamen. SMB-Passwörter werden nur auf Wunsch aufgenommen und dann mit einem vom Nutzer gewählten Export-Passwort verschlüsselt (AES-256); ohne Export-Passwort enthält die Datei keine Passwörter. Die Datei wird dort gespeichert beziehungsweise hin geteilt, wo der Nutzer es bestimmt; sie sollte entsprechend geschützt werden. Die App selbst legt keine automatische Android-Datensicherung (allowBackup) an.

**Papierkorb und Versionsschutz:** Beim Ersetzen oder Löschen von Dateien in Ordner-Profilen werden Dateien je nach Einstellung in einen versteckten Papierkorb-Ordner (`.LeoSync-Bin`) auf dem Handy beziehungsweise der Freigabe verschoben und nach der eingestellten Frist automatisch oder von Hand endgültig gelöscht. Diese Ordner liegen ausschliesslich auf den Speicherorten des Nutzers.

Lokale App-Daten und Einstellungen können über die Android-App-Verwaltung gelöscht werden. Beim Deinstallieren der Anwendung werden die zugehörigen lokalen App-Daten im Rahmen der Android-Systemfunktionen entfernt.

Bereits auf eine SMB-Freigabe übertragene Dateien werden durch die Deinstallation nicht automatisch gelöscht.

Diese müssen direkt auf dem jeweiligen Speicher verwaltet werden.

Diagnoseberichte werden zunächst lokal exportiert. Erst durch den freiwilligen Versand per E-Mail erhält der Entwickler Zugriff auf deren Inhalt.

## 11. Rechte betroffener Personen

Im Rahmen des anwendbaren Datenschutzrechts können betroffene Personen insbesondere folgende Rechte haben:

- Recht auf Auskunft über verarbeitete personenbezogene Daten
- Recht auf Berichtigung unrichtiger Daten
- Recht auf Löschung personenbezogener Daten, sofern die gesetzlichen Voraussetzungen erfüllt sind
- Recht auf Widerspruch gegen bestimmte Datenverarbeitungen

Da LeoSync keine Nutzerdaten automatisch an den Entwickler übermittelt, hat dieser grundsätzlich keinen Zugriff auf die lokal gespeicherten Fotos, Videos, Einstellungen oder Synchronisationsdaten der Nutzer.

Für Datenschutzanfragen kann der unter Abschnitt 1 genannte Kontakt verwendet werden.

Für Nutzer in der Schweiz ist zusätzlich der Eidgenössische Datenschutz- und Öffentlichkeitsbeauftragte (EDÖB) zuständig.

https://www.edoeb.admin.ch/

## 12. Änderungen dieser Datenschutzerklärung

Diese Datenschutzerklärung kann angepasst werden, wenn sich die Funktionen von LeoSync, die technische Datenverarbeitung oder die rechtlichen Anforderungen ändern.

Die jeweils aktuelle Fassung wird auf der offiziellen Website der Marke Leo veröffentlicht.

## 13. Kontakt

Bei Fragen zur Anwendung oder zum Datenschutz:

**Leo – Independent Software**

Land: Schweiz

E-Mail: leotools@protonmail.com