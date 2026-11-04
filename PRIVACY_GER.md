# Datenschutzerklärung – LeoSync

**Stand:** 4. November 2026  
**Version:** 1.0

Diese Datenschutzerklärung informiert darüber, welche Daten durch die Android-App LeoSync verarbeitet werden und wie mit diesen Daten umgegangen wird.

LeoSync ist eine Anwendung der Marke Leo und wurde mit dem Ziel entwickelt, Fotos und Videos ohne externe Cloud-Dienste zwischen Android-Geräten und eigenen Netzwerkfreigaben zu synchronisieren.

## 1. Verantwortlicher

Verantwortlich für die Datenverarbeitung:

- **Entwickler:** [Vor- und Nachname]
- **Land:** Schweiz
- **Kontakt:** [E-Mail-Adresse]

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

Je nach Umfang des Diagnoseberichts können auch technische Angaben wie WLAN-Namen, lokale Serveradressen, Freigabepfade oder Dateinamen enthalten sein.

**SMB-Passwörter werden nicht absichtlich in Diagnoseberichte aufgenommen.**

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

Der Nutzer entscheidet selbst, ob er einen Bericht exportiert und per E-Mail an den Entwickler sendet.

Der Versand erfolgt über die vom Nutzer ausgewählte E-Mail-Anwendung beziehungsweise den gewählten E-Mail-Dienst.

Mit dem Versand können die im Diagnosebericht enthaltenen technischen Informationen an den Entwickler übermittelt werden.

Vor dem Versand sollte der Nutzer den Bericht prüfen und berücksichtigen, dass technische Angaben Rückschlüsse auf seine Netzwerk- und Geräteumgebung ermöglichen können.

Für die Verarbeitung der E-Mail gelten zusätzlich die Datenschutzbestimmungen des jeweiligen E-Mail-Anbieters.

Der Entwickler verwendet freiwillig übermittelte Diagnoseberichte ausschließlich zur Untersuchung technischer Probleme und zur Verbesserung von LeoSync.

## 8. Drittanbieter und externe Dienste

LeoSync verwendet keine externen Analytics-, Werbe-, Tracking- oder Cloud-SDKs.

Es findet keine automatische Weitergabe von Nutzerdaten an Werbe- oder Analyseanbieter statt.

Daten können das Gerät insbesondere im Rahmen folgender, vom Nutzer veranlasster Aktionen verlassen:

- Synchronisierung mit der konfigurierten SMB-Netzwerkfreigabe.
- Freiwilliger Versand eines Diagnoseberichts per E-Mail.

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

E-Mail: [Kontakt-E-Mail]