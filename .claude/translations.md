# LeoSync - Übersetzungen Deutsch / Englisch

Erzeugt mit `tools/make-translations.ps1` aus `res/values-de` (Deutsch) und `res/values` (Englisch). Korrekturen bitte mit Zeilennummer melden. Platzhalter wie `%1$s` werden in der App ersetzt (Zahl, Name, Datum ...). Zeilen mit `(one)` / `(other)` sind Einzahl / Mehrzahl.

## Allgemein

| Nr. | Schlüssel | Deutsch | Englisch |
|---:|---|---|---|
| 1 | `common_back` | Zurück | Back |
| 2 | `fmt_date_time` | dd.MM.yyyy HH:mm | yyyy-MM-dd HH:mm |
| 3 | `main_install_status` | Status %1$s | Status %1$s |
| 4 | `main_update_failed` | Update fehlgeschlagen: %1$s | Update failed: %1$s |
| 5 | `main_update_failed_key` | Update fehlgeschlagen: %1$s – Die neue Version ist evtl. mit einem anderen Schlüssel signiert. | Update failed: %1$s – the new version may be signed with a different key. |
| 6 | `main_files_removed (one)` | %1$s Datei entfernt | %1$s file removed |
| 7 | `main_files_removed (other)` | %1$s Dateien entfernt | %1$s files removed |
| 8 | `main_nothing_to_remove` | Nichts zu entfernen | Nothing to remove |

## Startseite, Schutzbremse, Berechtigungen

| Nr. | Schlüssel | Deutsch | Englisch |
|---:|---|---|---|
| 9 | `common_cancel` | Abbrechen | Cancel |
| 10 | `common_pause` | Pausieren | Pause |
| 11 | `common_sync_now` | Jetzt synchronisieren | Sync now |
| 12 | `common_save` | Speichern | Save |
| 13 | `common_ok` | OK | OK |
| 14 | `common_delete` | Löschen | Delete |
| 15 | `common_more` | Mehr dazu | More |
| 16 | `common_less` | Weniger | Less |
| 17 | `common_details` | Technische Details | Technical details |
| 18 | `common_details_hide` | Details ausblenden | Hide details |
| 19 | `home_profile_deleted` | Profil „%1$s“ gelöscht | Profile “%1$s” deleted |
| 20 | `home_undo` | Rückgängig | Undo |
| 21 | `home_pull_tip` | Tipp: Ganz oben nach unten ziehen startet ebenfalls eine Synchronisierung. | Tip: Pulling down at the very top also starts a sync. |
| 22 | `home_eta` | noch ca. %1$s | about %1$s left |
| 23 | `home_eta_calculating` | Restzeit wird berechnet … | Calculating remaining time… |
| 24 | `eta_minutes (one)` | %1$s Min. | %1$s min |
| 25 | `eta_minutes (other)` | %1$s Min. | %1$s min |
| 26 | `eta_hours` | %1$s Std. %2$s Min. | %1$s h %2$s min |
| 27 | `fmt_day_month` | dd.MM. | MM-dd |
| 28 | `home_wait_wifi` | Wartet auf das Heim-WLAN. | Waiting for the home Wi-Fi. |
| 29 | `home_wait_charger` | Wartet auf das Ladegerät („Nur beim Laden“ ist eingeschaltet). | Waiting for the charger (“Only while charging” is on). |
| 30 | `home_starting` | Synchronisierung startet gleich… | Sync is starting… |
| 31 | `home_tool_trash_text` | Dateien zurückholen oder endgültig löschen | Restore files or delete them permanently |
| 32 | `home_tool_trash_used` | Belegt: Handy %1$s · PC %2$s | Used: mobile %1$s · PC %2$s |
| 33 | `home_guard_note_all` | Wartet auf deine Entscheidung (Schutzbremse) – erst danach geht es weiter. | Waiting for your decision (safety brake) – it continues only after that. |
| 34 | `home_guard_note_some` | Bei einem Profil wartet es auf deine Entscheidung (Schutzbremse). | One profile is waiting for your decision (safety brake). |
| 35 | `home_pc_unreachable` | PC nicht erreichbar (%1$s) – nächste Prüfung um %2$s | PC not reachable (%1$s) – next check at %2$s |
| 36 | `home_cd_paused` | Pausiert – tippen zum Fortsetzen | Paused – tap to resume |
| 37 | `home_cd_running` | Läuft – tippen zum Pausieren | Running – tap to pause |
| 38 | `home_cd_stats` | Statistik | Statistics |
| 39 | `home_cd_log` | Verlauf | History |
| 40 | `home_cd_settings` | Einstellungen | Settings |
| 41 | `home_profile_fallback` | Profil | Profile |
| 42 | `home_update_title` | Update verfügbar | Update available |
| 43 | `home_update_text` | Version %1$s ist verfügbar (installiert: %2$s). | Version %1$s is available (installed: %2$s). |
| 44 | `home_update_button` | Ansehen | View |
| 45 | `home_conflicts_title` | Konflikte bei der Ordner-Synchronisierung | Folder sync conflicts |
| 46 | `home_conflicts_text (one)` | %1$s Datei wurde auf Handy und PC geändert. Wähle, welche Version gilt. | %1$s file was changed on both mobile and PC. Choose which version wins. |
| 47 | `home_conflicts_text (other)` | %1$s Dateien wurden auf Handy und PC geändert. Wähle, welche Version gilt. | %1$s files were changed on both mobile and PC. Choose which version wins. |
| 48 | `home_conflicts_button` | Entscheiden | Decide |
| 49 | `home_decisions_title` | Ordnerauswahl nötig | Folder selection needed |
| 50 | `home_decisions_text (one)` | %1$s Datei in %2$s wartet, weil es umbenannte Monatsordner gibt. | %1$s file in %2$s is waiting because there are renamed month folders. |
| 51 | `home_decisions_text (other)` | %1$s Dateien in %2$s warten, weil es umbenannte Monatsordner gibt. | %1$s files in %2$s are waiting because there are renamed month folders. |
| 52 | `home_groups (one)` | %1$s Gruppe | %1$s group |
| 53 | `home_groups (other)` | %1$s Gruppen | %1$s groups |
| 54 | `home_decisions_button` | Zuordnen | Assign |
| 55 | `home_delete_title` | Gesicherte Dateien entfernen | Remove backed-up files |
| 56 | `home_delete_text (one)` | %1$s Datei ist verifiziert auf dem PC. Android fragt einmal zur Bestätigung. | %1$s file is verified on the PC. Android asks once for confirmation. |
| 57 | `home_delete_text (other)` | %1$s Dateien sind verifiziert auf dem PC. Android fragt einmal zur Bestätigung. | %1$s files are verified on the PC. Android asks once for confirmation. |
| 58 | `home_delete_button` | Entfernen bestätigen | Confirm removal |
| 59 | `home_profiles` | Profile | Profiles |
| 60 | `home_add_profile` | Profil hinzufügen | Add profile |
| 61 | `home_no_profile` | Noch kein Profil. Lege ein Profil mit Ziel-Freigabe, WLAN und Quellordnern an. | No profile yet. Create a profile with a target Share, Wi-Fi and source folders. |
| 62 | `home_first_profile` | Erstes Profil anlegen | Create first profile |
| 63 | `home_tool_free` | Speicher freigeben | Free up space |
| 64 | `home_tool_free_text` | Alte, auf dem PC gesicherte Dateien vom Handy entfernen | Remove old files from the mobile that are backed up on the PC |
| 65 | `home_tool_free_disabled` | Nur bei Fotos-Profilen und Ordner-Profilen Handy → PC | Only for photo profiles and folder profiles mobile → PC |
| 66 | `home_tool_trash` | Papierkorb | Trash |
| 67 | `home_tool_trash_disabled` | Nur bei Ordner-Profilen | Only for folder profiles |
| 68 | `home_tool_trash_hint` | Den Papierkorb gibt es nur bei Ordner-Profilen. | The trash is only available for folder profiles. |
| 69 | `home_tools` | Tools | Tools |
| 70 | `home_purge_pending_title` | Papierkorb leeren vorgemerkt | Emptying the trash is scheduled |
| 71 | `home_purge_pending_text` | Der PC-Papierkorb wird bei der nächsten Synchronisierung mit erreichbarem PC geleert. | The PC trash will be emptied at the next sync with a reachable PC. |
| 72 | `home_restore_failed_title` | Wiederherstellen fehlgeschlagen | Restore failed |
| 73 | `home_restore_failed_text (one)` | %1$s Datei konnte nicht wiederhergestellt werden. Es wurde alles zurückgerollt; die nächste Synchronisierung versucht es erneut. Details stehen im Verlauf. | %1$s file could not be restored. Everything was rolled back; the next sync will try again. Details are in the history. |
| 74 | `home_restore_failed_text (other)` | %1$s Dateien konnten nicht wiederhergestellt werden. Es wurde alles zurückgerollt; die nächste Synchronisierung versucht es erneut. Details stehen im Verlauf. | %1$s files could not be restored. Everything was rolled back; the next sync will try again. Details are in the history. |
| 75 | `home_restore_gaveup_text (one)` | %1$s Datei konnte auch nach mehreren Versuchen nicht wiederhergestellt werden. Es wurde alles zurückgerollt und nichts geht verloren: Vormerkung und Papierkorb-Version bleiben erhalten. „Erneut versuchen“ oder „Verwerfen“ wählen. | %1$s file could not be restored after several attempts. Everything was rolled back and nothing is lost: the request and the trash version are kept. Choose “Try again” or “Discard”. |
| 76 | `home_restore_gaveup_text (other)` | %1$s Dateien konnten auch nach mehreren Versuchen nicht wiederhergestellt werden. Es wurde alles zurückgerollt und nichts geht verloren: Vormerkungen und Papierkorb-Versionen bleiben erhalten. „Erneut versuchen“ oder „Verwerfen“ wählen. | %1$s files could not be restored after several attempts. Everything was rolled back and nothing is lost: the requests and the trash versions are kept. Choose “Try again” or “Discard”. |
| 77 | `home_retry` | Erneut versuchen | Try again |
| 78 | `home_discard` | Verwerfen | Discard |
| 79 | `home_restore_pending_title` | Wiederherstellung vorgemerkt | Restore scheduled |
| 80 | `home_restore_pending_text (one)` | %1$s Datei wird bei der nächsten Synchronisierung mit erreichbarem PC wiederhergestellt. | %1$s file will be restored at the next sync with a reachable PC. |
| 81 | `home_restore_pending_text (other)` | %1$s Dateien werden bei der nächsten Synchronisierung mit erreichbarem PC wiederhergestellt. | %1$s files will be restored at the next sync with a reachable PC. |
| 82 | `home_version` | Version %1$s | Version %1$s |
| 83 | `home_tomorrow` | morgen %1$s | tomorrow %1$s |
| 84 | `home_pause_title` | Wie lange pausieren? | Pause for how long? |
| 85 | `home_pause_hint` | Eine laufende Synchronisierung wird angehalten. Bis zum Ende der Pause startet nichts automatisch. | A running sync is stopped. Nothing starts automatically until the pause ends. |
| 86 | `home_pause_hours` | Eigene Anzahl Stunden (1–168) | Custom number of hours (1–168) |
| 87 | `home_pause_forever` | Für immer (nur manuell) | Forever (manual only) |
| 88 | `home_pause_forever_hint` | Schaltet die Automatik ganz ab: Synchronisiert wird nur noch mit „Synchronisieren“, bis du sie über ▶ oben rechts wieder einschaltest. | Turns automatic syncing off completely: syncing only happens with “Sync” until you turn it back on with ▶ at the top right. |
| 89 | `home_pause_choose` | Wähle eine Dauer. | Choose a duration. |
| 90 | `home_status` | Status | Status |
| 91 | `home_all_saved` | ✓ Alles gesichert und aktuell | ✓ Everything backed up and up to date |
| 92 | `home_wifi` | WLAN: %1$s | Wi-Fi: %1$s |
| 93 | `home_wifi_none` | nicht verbunden / nicht lesbar | not connected / not readable |
| 94 | `home_progress` | %1$s: %2$s / %3$s Dateien – %4$s von %5$s | %1$s: %2$s / %3$s files – %4$s of %5$s |
| 95 | `home_running` | Synchronisierung läuft… | Sync running… |
| 96 | `home_manual_only` | ⏸ Automatik aus – nur manuelle Synchronisierungen | ⏸ Automatic off – manual syncs only |
| 97 | `home_manual_only_hint` | Es wird nur synchronisiert, wenn du „Synchronisieren“ tippst. ▶ oben rechts schaltet die Automatik wieder ein. | Syncing only happens when you tap “Sync”. ▶ at the top right turns automatic syncing back on. |
| 98 | `home_last_sync` | Letzte Synchronisierung: %1$s (%2$s) | Last sync: %1$s (%2$s) |
| 99 | `home_paused_until` | ⏸ Pausiert bis %1$s | ⏸ Paused until %1$s |
| 100 | `home_paused_until_hint` | Automatische Synchronisierungen ruhen bis dahin. „Fortsetzen“ oder „Synchronisieren“ beendet die Pause sofort. | Automatic syncs rest until then. “Resume” or “Sync” ends the pause immediately. |
| 101 | `home_paused_wifi` | ⏸ Pausiert – wartet auf das Heim-WLAN | ⏸ Paused – waiting for the home Wi-Fi |
| 102 | `home_paused_wifi_hint` | Die Synchronisierung wurde durch einen WLAN-Unterbruch angehalten und läuft automatisch weiter, sobald das WLAN wieder verbunden ist. | The sync was stopped by a Wi-Fi interruption and continues automatically as soon as the Wi-Fi is connected again. |
| 103 | `home_pc_unreachable_hint` | Ist der PC eingeschaltet und die Freigabe aktiv? „Synchronisieren“ prüft sofort. | Is the PC switched on and the Share active? “Sync” checks right away. |
| 104 | `home_nothing_to_do` | Nichts zu tun – alles aktuell | Nothing to do – everything up to date |
| 105 | `home_no_sync_yet` | Noch keine Synchronisierung durchgeführt. | No sync yet. |
| 106 | `home_next_check` | Nächste Prüfung um %1$s | Next check at %1$s |
| 107 | `home_skipped` | ⚠ Automatischer Start übersprungen – %1$s | ⚠ Automatic start skipped – %1$s |
| 108 | `home_resume` | Fortsetzen | Resume |
| 109 | `home_sync` | Synchronisieren | Sync |
| 110 | `home_sync_needs_profile` | Lege zuerst ein Profil an. | Create a profile first. |
| 111 | `home_status_attention (one)` | Entscheidung erforderlich | Decision required |
| 112 | `home_status_attention (other)` | %1$s Entscheidungen erforderlich | %1$s decisions required |
| 113 | `home_status_conflict_detail (one)` | %1$s Konflikt wartet auf deine Entscheidung. | %1$s conflict is waiting for your decision. |
| 114 | `home_status_conflict_detail (other)` | %1$s Konflikte warten auf deine Entscheidung. | %1$s conflicts are waiting for your decision. |
| 115 | `home_status_attention_detail` | Etwas wartet auf deine Entscheidung – siehe die Karten auf dieser Seite. | Something is waiting for your decision – see the cards on this page. |
| 116 | `home_status_no_connection` | Keine Verbindung | No connection |
| 117 | `home_no_connection_wifi` | Kein passendes WLAN – Synchronisierung wartet | No matching Wi-Fi – sync is waiting |
| 118 | `home_no_connection_hint` | Verbinde dich mit dem WLAN des Profils. „Synchronisieren“ versucht es trotzdem sofort. | Connect to the Wi-Fi set in the profile. “Sync” still tries right away. |
| 119 | `home_status_errors (one)` | Letzte Synchronisierung: %1$s Fehler | Last sync: %1$s error |
| 120 | `home_status_errors (other)` | Letzte Synchronisierung: %1$s Fehler | Last sync: %1$s errors |
| 121 | `home_status_failed` | Letzte Synchronisierung fehlgeschlagen | Last sync failed |
| 122 | `home_status_errors_hint` | Tippen öffnet den Verlauf. | Tap to open the history. |
| 123 | `home_status_not_configured` | Noch nichts eingerichtet | Nothing set up yet |
| 124 | `home_status_no_active` | Kein aktives Profil | No active profile |
| 125 | `home_no_active_detail` | Aktiviere ein Profil oder schliesse seine Einrichtung ab. | Turn on a profile or finish setting it up. |
| 126 | `home_status_idle` | Bereit | Ready |
| 127 | `home_sync_feedback_wifi` | Kein passendes WLAN – versuche es trotzdem | No matching Wi-Fi – trying anyway |
| 128 | `home_sync_feedback_pc` | PC nicht erreichbar – prüfe erneut | PC not reachable – checking again now |
| 129 | `home_profile_disabled` | deaktiviert | disabled |
| 130 | `home_profile_on_wifi` | im WLAN ✓ | on Wi-Fi ✓ |
| 131 | `home_profile_no_target` | Kein Ziel festgelegt | No target set |
| 132 | `home_profile_no_local` | kein Handy-Ordner | no mobile folder |
| 133 | `home_profile_propagate` | Löschungen übernehmen | apply deletions |
| 134 | `home_profile_sources` | Quellen: %1$s | Sources: %1$s |
| 135 | `home_profile_none` | keine | none |
| 136 | `home_profile_no_wifi` | keines – nur manuelle Synchronisierung | none – manual sync only |
| 137 | `home_profile_counts` | %1$d gesichert · %2$d offen | %1$d backed up · %2$d open |
| 138 | `home_profile_mirror_synced (one)` | %1$s Datei synchronisiert | %1$s file synced |
| 139 | `home_profile_mirror_synced (other)` | %1$s Dateien synchronisiert | %1$s files synced |
| 140 | `home_profile_open_conflicts (one)` | %1$s Konflikt offen | %1$s conflict open |
| 141 | `home_profile_open_conflicts (other)` | %1$s Konflikte offen | %1$s conflicts open |
| 142 | `home_profile_awaiting` | Wartet auf den ersten Start – zuerst im Profil simulieren oder „Synchronisieren“ tippen | Waiting for the first start – simulate in the profile first or tap “Sync” |
| 143 | `guard_choose_new` | Neuen Ordner wählen | Choose new folder |
| 144 | `guard_recheck` | Erneut prüfen (Ordner wiederhergestellt) | Check again (folder restored) |
| 145 | `guard_reset` | Neu anlegen und abgleichen | Create anew and compare |
| 146 | `guard_reset_hint` | „Neu anlegen“ gleicht alles neu ab, ohne etwas zu löschen. Bis du entscheidest, ruht nur dieses Profil. | “Create anew” compares everything again without deleting anything. Until you decide, only this profile rests. |
| 147 | `guard_keep` | Nicht löschen | Don't delete |
| 148 | `guard_delete_anyway` | Trotzdem in den Papierkorb | Move to trash anyway |
| 149 | `guard_keep_hint` | „Nicht löschen“ behält die Dateien bzw. stellt sie auf der anderen Seite wieder her. | “Don't delete” keeps the files or restores them on the other side. |
| 150 | `guard_choose_other` | Anderen Ordner wählen | Choose another folder |
| 151 | `guard_accept` | Trotzdem übernehmen | Accept anyway |
| 152 | `guard_as_new` | Als neuen Ordner behandeln | Treat as a new folder |
| 153 | `perm_incomplete` | Einrichtung unvollständig | Setup incomplete |
| 154 | `perm_grant` | Erteilen | Grant |
| 155 | `perm_granted` | ✓ erledigt | ✓ done |
| 156 | `perm_open` | Öffnen | Open |
| 157 | `perm_turn_on` | Einschalten | Turn on |
| 158 | `perm_media` | Zugriff auf Fotos & Videos | Access to photos & videos |
| 159 | `perm_media_location` | Standort in Fotos lesen (für Länderordner) | Read location in photos (for country folders) |
| 160 | `perm_notifications` | Benachrichtigungen (Fortschritt) | Notifications (progress) |
| 161 | `perm_location` | Standort (Android braucht ihn, um den WLAN-Namen zu lesen) | Location (Android needs it to read the Wi-Fi name) |
| 162 | `perm_location_always` | Standort „Immer zulassen“ (WLAN-Erkennung im Hintergrund) | Location “Allow all the time” (Wi-Fi detection in the background) |
| 163 | `perm_location_off` | Standortdienst ist ausgeschaltet | Location service is turned off |
| 164 | `perm_location_service` | Standortdienst eingeschaltet | Location service turned on |
| 165 | `perm_battery` | Akku-Optimierung deaktivieren (zuverlässige Hintergrund-Synchronisierung) | Disable battery optimization (reliable background sync) |
| 166 | `home_sync_deferred_snack` | Wird nach der laufenden Synchronisierung gestartet. | Will start after the current sync. |
| 167 | `home_next_all` | Danach: alle Profile | Next: all profiles |
| 168 | `home_next_auto` | Danach folgt eine weitere Synchronisierung. | Another sync follows. |
| 169 | `perm_blocked_title` | Berechtigung gesperrt | Permission blocked |
| 170 | `perm_blocked_text` | Android fragt nicht mehr. Öffne App-Info → Berechtigungen und erlaube den Zugriff. | Android no longer asks. Open App info → Permissions and allow the access. |

## Einstellungen

| Nr. | Schlüssel | Deutsch | Englisch |
|---:|---|---|---|
| 171 | `settings_notifications` | Benachrichtigungen | Notifications |
| 172 | `settings_transfer` | Übertragung | Transfer |
| 173 | `settings_permissions` | App-Berechtigungen | App permissions |
| 174 | `settings_app` | Sprache & Sicherung | Language & backup |
| 175 | `settings_updates` | Updates | Updates |
| 176 | `settings_diagnose` | Diagnose | Diagnostics |
| 177 | `settings_help` | Hilfe & Produktseite | Help & product page |
| 178 | `settings_notify_summary` | Meldung nach jeder Synchronisierung | Message after every sync |
| 179 | `settings_notify_summary_sub` | z. B. „3 Dateien übertragen“ | e.g. “3 files transferred” |
| 180 | `settings_notify_update` | Update verfügbar | Update available |
| 181 | `settings_notify_update_sub` | Neue Version gefunden bzw. installiert | New version found or installed |
| 182 | `settings_notify_hints` | Hinweise | Hints |
| 183 | `settings_notify_hints_sub` | Ordnerauswahl nötig, Konflikte, Dateien entfernen, Fehler. Erscheinen auch ohne Benachrichtigung auf der Startseite. | Folder selection needed, conflicts, removing files, errors. Also shown on the home screen without a notification. |
| 184 | `settings_notify_fixed` | Die Fortschrittsanzeige während einer Synchronisierung und „Pausiert – wartet auf WLAN“ lassen sich nicht abschalten: Android verlangt sie, damit die Synchronisierung im Hintergrund weiterlaufen bzw. fortgesetzt werden kann. | The progress display during a sync and “Paused – waiting for Wi-Fi” cannot be turned off: Android requires them so the sync can keep running or resume in the background. |
| 185 | `settings_notify_android` | Android-Einstellungen für Benachrichtigungen | Android notification settings |
| 186 | `settings_stale` | Erinnerung | Reminder |
| 187 | `settings_stale_switch` | Erinnern, wenn lange nicht gesichert | Remind me when nothing was backed up for a long time |
| 188 | `settings_stale_sub` | Nur wenn neue Fotos/Videos warten. Wiederholt sich alle %1$s Tage. | Only when new photos/videos are waiting. Repeats every %1$s days. |
| 189 | `settings_stale_days` | Nach … Tagen ohne Sicherung | After … days without backup |
| 190 | `settings_auto` | Automatik | Automatic |
| 191 | `settings_auto_wifi` | Synchronisieren beim Verbinden mit Heim-WLAN | Sync when connecting to home Wi-Fi |
| 192 | `settings_auto_wifi_sub` | Startet sofort, wenn ein WLAN eines Profils verbunden wird | Starts as soon as a Wi-Fi network of a profile connects |
| 193 | `settings_auto_offwifi` | Auch über mobile Daten oder VPN synchronisieren | Also sync over mobile data or VPN |
| 194 | `settings_auto_offwifi_sub` | Ausserhalb des WLANs wird der WLAN-Name nicht geprüft. Startet bei neuen Fotos, über das Sicherheitsnetz und die Ordnerprüfung. Kann viel Datenvolumen verbrauchen und Kosten beim Mobilfunkabo verursachen (z. B. im Ausland). Der PC muss erreichbar sein (VPN). | Off Wi-Fi the Wi-Fi name is not checked. Starts on new photos, by the safety net and by folder checks. May use a lot of mobile data and cause costs with your mobile plan (e.g. roaming). The PC must be reachable (VPN). |
| 195 | `settings_auto_new` | Neue Fotos/Videos sofort übertragen | Transfer new photos/videos right away |
| 196 | `settings_auto_new_sub` | Nur, wenn gerade ein Heim-WLAN verbunden ist | Only while a home Wi-Fi is connected |
| 197 | `settings_auto_charging` | Nur beim Laden | Only while charging |
| 198 | `settings_auto_charging_sub` | Automatische Synchronisierungen nur am Ladegerät (manuelles Synchronisieren immer möglich) | Automatic syncs only on the charger (manual sync always possible) |
| 199 | `settings_min_age` | Mindestalter einer Datei (Minuten) | Minimum file age (minutes) |
| 200 | `settings_min_age_hint` | Verhindert, dass Fotos übertragen werden, die gerade noch bearbeitet werden. | Prevents transferring photos that are still being edited. |
| 201 | `settings_parallel` | Parallele Übertragungen | Parallel transfers |
| 202 | `settings_parallel_value` | Gleichzeitige Übertragungen: %1$s | Simultaneous transfers: %1$s |
| 203 | `settings_parallel_hint` | Mehr Verbindungen beschleunigen viele kleine Dateien. Bei grossen Videos bringt es wenig. Standard: 3 | More connections speed up many small files. For large videos it helps little. Default: 3 |
| 204 | `settings_perm_title` | Berechtigungen | Permissions |
| 205 | `settings_perm_hint` | Diese Berechtigungen braucht LeoSync für die automatische Synchronisierung im Hintergrund. | LeoSync needs these permissions for automatic syncing in the background. |
| 206 | `settings_perm_app_info` | App-Info öffnen | Open app info |
| 207 | `settings_background_hint` | Damit Android die Synchronisierung nicht verzögert oder abbricht, sollte die Akku-Optimierung für LeoSync aus sein. | To keep Android from delaying or stopping the sync, battery optimization should be off for LeoSync. |
| 208 | `settings_battery` | Akku-Optimierungen | Battery optimization |
| 209 | `settings_vendor_extra` | Zusätzlich bei %1$s: | Additionally on %1$s: |
| 210 | `settings_vendor_note` | Die Bezeichnungen können je nach Software-Stand leicht abweichen. | The labels may differ slightly depending on the software version. |
| 211 | `settings_vendor_web` | Anleitung im Browser | Instructions in the browser |
| 212 | `settings_language` | Sprache | Language |
| 213 | `settings_language_system` | Systemsprache | System language |
| 214 | `settings_language_hint` | „Systemsprache“ zeigt Deutsch, wenn das Handy auf Deutsch eingestellt ist, sonst Englisch. | “System language” shows German if the mobile is set to German, otherwise English. |
| 215 | `settings_geo_loading` | Lade Länderdaten… | Loading country data… |
| 216 | `settings_geo_loaded` | %1$s Länder geladen (offline) | %1$s countries loaded (offline) |
| 217 | `settings_geo_missing` | Länderdaten fehlen – Ordner werden ohne Land angelegt | Country data missing – folders are created without a country |
| 218 | `settings_country_folders` | Länderordner | Country folders |
| 219 | `settings_home_country` | Heimatland (ISO-Code) | Home country (ISO code) |
| 220 | `settings_home_country_hint` | Fotos aus diesem Land bekommen keinen Länderzusatz (Standard: CH = Schweiz). | Photos from this country get no country suffix (default: CH = Switzerland). |
| 221 | `settings_diag_title` | Beta / Diagnose | Beta / Diagnostics |
| 222 | `settings_diag_hint` | Sammelt Angaben zu Gerät, Hintergrund-Einschränkungen, Berechtigungen, Profilen und den letzten Läufen und kopiert sie in die Zwischenablage. Passwörter, Benutzernamen, IP-Adressen, Rechner-, WLAN- und Ordnernamen, Pfade und genaue Dateinamen fehlen oder sind maskiert. Geräte-ID: %1$s | Collects details about the device, background restrictions, permissions, profiles and the latest runs and copies them to the clipboard. Passwords, user names, IP addresses, computer, Wi-Fi and folder names, paths and exact file names are left out or masked. Device ID: %1$s |
| 223 | `settings_diag_failed` | Diagnose fehlgeschlagen: %1$s | Diagnostics failed: %1$s |
| 224 | `settings_diag_busy` | Sammle Daten… | Collecting data… |
| 225 | `settings_report` | Problem melden | Report a problem |
| 226 | `settings_report_hint` | Öffnet die Mail-App mit einer Nachricht an %1$s und den Diagnosedaten im Anhang. Es wird nichts automatisch gesendet: Nachricht prüfen und selbst senden. Die Daten können WLAN-Namen, Serveradressen und Dateinamen enthalten (keine Passwörter). | Opens your mail app with a message to %1$s and the diagnostic data attached. Nothing is sent automatically: check the message and send it yourself. The data may contain Wi-Fi names, server addresses and file names (no passwords). |
| 227 | `settings_report_no_mail` | Keine Mail-App gefunden. Die Adresse %1$s wurde in die Zwischenablage kopiert. | No mail app found. The address %1$s was copied to the clipboard. |
| 228 | `report_subject` | LeoSync %1$s – Problembericht | LeoSync %1$s – problem report |
| 229 | `report_body` | Bitte beschreibe hier das Problem (was hast du gemacht, was hast du erwartet, was ist passiert?): /  /  | Please describe the problem here (what did you do, what did you expect, what happened?): /  /  |
| 230 | `report_chooser` | Problem melden | Report a problem |
| 231 | `settings_diag_copy` | Diagnosedaten kopieren | Copy diagnostic data |
| 232 | `settings_about` | LeoSync %1$s – Open Source (MIT) | LeoSync %1$s – Open Source (MIT) |
| 233 | `settings_diag_copied` | Diagnosedaten in die Zwischenablage kopiert | Diagnostic data copied to the clipboard |
| 234 | `settings_err_days` | Bitte eine Zahl von 1 bis 365 eingeben. | Enter a number from 1 to 365. |
| 235 | `settings_err_minutes` | Bitte eine Zahl von 0 bis 1440 eingeben. | Enter a number from 0 to 1440. |
| 236 | `settings_err_country` | Bitte einen gültigen Ländercode aus zwei Buchstaben eingeben (z. B. CH). | Enter a valid two-letter country code (e.g. CH). |

## Profil und Assistent

| Nr. | Schlüssel | Deutsch | Englisch |
|---:|---|---|---|
| 237 | `profile_default_photos` | Fotos | Photos |
| 238 | `profile_default_folder` | Ordner | Folder |
| 239 | `profile_folder_unusable` | Dieser Ordner kann nicht verwendet werden. Bitte einen Unterordner wählen. | This folder cannot be used. Please choose a subfolder. |
| 240 | `profile_no_persistent_access` | Für diesen Ordner wurde kein dauerhafter Zugriff erteilt. | No permanent access was granted for this folder. |
| 241 | `profile_saved_toast` | Gespeichert | Saved |
| 242 | `profile_title_new` | Neues Profil | New profile |
| 243 | `profile_title_edit` | Profil bearbeiten | Edit profile |
| 244 | `profile_step` | Schritt %1$s von %2$s: %3$s | Step %1$s of %2$s: %3$s |
| 245 | `profile_step_optional` | Schritt %1$s von %2$s: %3$s (optional) | Step %1$s of %2$s: %3$s (optional) |
| 246 | `profile_general` | Allgemein | General |
| 247 | `profile_name` | Profilname | Profile name |
| 248 | `profile_active` | Profil aktiv | Profile active |
| 249 | `profile_type_mirror` | Typ: %1$s – 1:1-Spiegelung mit Unterordnern | Type: %1$s – 1:1 mirror with subfolders |
| 250 | `profile_type_photos` | Typ: %1$s – sortiert nach Jahr und Monat | Type: %1$s – sorted by year and month |
| 251 | `profile_local_folder` | Ordner auf dem Handy | Folder on the mobile |
| 252 | `profile_no_folder_yet` | Noch kein Ordner gewählt. | No folder chosen yet. |
| 253 | `profile_access_lost` | Zugriff verloren – bitte erneut wählen. | Access lost – please choose again. |
| 254 | `profile_choose_folder` | Ordner wählen… | Choose folder… |
| 255 | `profile_choose_other_folder` | Anderen Ordner wählen… | Choose another folder… |
| 256 | `profile_local_folder_hint` | Alle Dateien in diesem Ordner und seinen Unterordnern werden mit dem PC abgeglichen. Android fragt einmalig, ob LeoSync auf den Ordner zugreifen darf (nötig für alle Dateitypen). | All files in this folder and its subfolders are compared with the PC. Android asks once whether LeoSync may access the folder (needed for all file types). |
| 257 | `profile_sources` | Quellordner auf dem Handy | Source folders on the mobile |
| 258 | `profile_sources_empty` | Noch kein Ordner gewählt (z. B. DCIM/Camera). | No folder chosen yet (e.g. DCIM/Camera). |
| 259 | `profile_remove` | Entfernen | Remove |
| 260 | `profile_add_folder` | Ordner hinzufügen… | Add folder… |
| 261 | `profile_sources_hint` | Öffnet die Android-Dateiauswahl: Ordner wählen und „Diesen Ordner verwenden“ tippen. Kamera-Fotos liegen meist in „DCIM/Camera“, Bildschirmfotos in „Pictures/Screenshots“. | Opens the Android file picker: choose a folder and tap “Use this folder”. Camera photos are usually in “DCIM/Camera”, screenshots in “Pictures/Screenshots”. |
| 262 | `profile_subfolders` | Unterordner einbeziehen | Include subfolders |
| 263 | `profile_subfolders_sub` | Auch Fotos in Ordnern innerhalb der gewählten Ordner sichern | Also back up photos in folders inside the chosen folders |
| 264 | `profile_photos` | Fotos | Photos |
| 265 | `profile_videos` | Videos | Videos |
| 266 | `profile_target` | Ziel (Windows-Freigabe) | Target (Windows Share) |
| 267 | `profile_copy_credentials_button` | Zugangsdaten übernehmen… | Copy credentials… |
| 268 | `profile_copy_credentials_title` | Zugangsdaten übernehmen | Copy credentials |
| 269 | `profile_copy_credentials_hint` | Übernimmt Benutzer, Passwort und Domäne. Der Freigabe-Pfad wird nur eingetragen, wenn er noch leer ist. | Copies user, password and domain. The Share path is only filled in if it is still empty. |
| 270 | `profile_copy_credentials_profiles` | Verwendet in: %1$s | Used in: %1$s |
| 271 | `profile_credentials_copied` | Zugangsdaten von „%1$s“ übernommen | Credentials of “%1$s” copied |
| 272 | `profile_share_path` | Freigabe-Pfad | Share path |
| 273 | `profile_share_example` | Fotos | Photos |
| 274 | `profile_share_format` | Es reicht der PC, z. B. \\192.168.1.20. Freigabe und Ordner wählst du danach mit „Ordner auf dem PC wählen…“. Eintippen geht auch: \\Server\Freigabe\Ordner (IP-Adresse oder Name). | The PC is enough, e.g. \\192.168.1.20. Then pick the Share and folder with “Choose folder on the PC…”. Typing works too: \\Server\Share\Folder (IP address or name). |
| 275 | `profile_pick_pc_folder` | Ordner auf dem PC wählen… | Choose folder on the PC… |
| 276 | `profile_user` | Benutzer | User |
| 277 | `profile_password` | Passwort | Password |
| 278 | `profile_domain` | Domäne / PC-Name (optional) | Domain / PC name (optional) |
| 279 | `profile_domain_hint` | Bei einem lokalen Windows-Konto meist leer lassen. Microsoft-Konto: E-Mail-Adresse als Benutzer. | Usually leave empty for a local Windows account. Microsoft account: use the e-mail address as user. |
| 280 | `profile_test` | Verbindung testen | Test connection |
| 281 | `profile_wifi` | WLAN (automatische Synchronisierung) | Wi-Fi (automatic sync) |
| 282 | `profile_wifi_hint` | Optional. Ist das Handy mit einem dieser WLANs verbunden, synchronisiert LeoSync automatisch im Hintergrund. Ohne WLAN-Namen synchronisiert dieses Profil nur, wenn du „Synchronisieren“ tippst (z. B. wenn du den PC über VPN erreichst). | Optional. When the mobile is connected to one of these Wi-Fi networks, LeoSync syncs automatically in the background. Without a Wi-Fi name this profile only syncs when you tap “Sync” (e.g. when you reach the PC via VPN). |
| 283 | `profile_wifi_add_current` | Aktuelles WLAN „%1$s“ hinzufügen | Add current Wi-Fi “%1$s” |
| 284 | `profile_wifi_name` | WLAN-Name | Wi-Fi name |
| 285 | `profile_add` | Hinzufügen | Add |
| 286 | `profile_direction` | Richtung und Änderungen | Direction and changes |
| 287 | `profile_direction_hint` | In welche Richtung wird abgeglichen? | In which direction should it sync? |
| 288 | `profile_propagate` | Löschungen übernehmen | Apply deletions |
| 289 | `profile_propagate_sub` | Wird eine Datei auf einer Seite gelöscht, kommt sie auf der anderen Seite in den Ordner „%1$s“. Aus: gelöschte Dateien werden wiederhergestellt. | If a file is deleted on one side, it goes to the folder “%1$s” on the other side. Off: deleted files are restored. |
| 290 | `profile_keep_versions` | Alte Versionen im Papierkorb behalten | Keep old versions in the trash |
| 291 | `profile_keep_versions_sub` | Wird eine Datei überschrieben, kommt die alte Version in den Ordner „%1$s“ und lässt sich über den Papierkorb zurückholen. Braucht etwas mehr Speicher. | If a file is overwritten, the old version goes to the folder “%1$s” and can be brought back via the trash. Needs a bit more storage. |
| 292 | `profile_trash_days` | Papierkorb leeren nach … Tagen | Empty trash after … days |
| 293 | `profile_trash_days_hint` | Ältere Einträge in „%1$s“ werden auf Handy und PC endgültig gelöscht. 0 = nie. Standard: 30 | Older entries in “%1$s” are permanently deleted on mobile and PC. 0 = never. Default: 30 |
| 294 | `profile_conflict_question` | Wenn eine Datei auf beiden Seiten geändert wurde: | If a file was changed on both sides: |
| 295 | `profile_conflict_newer_hint` | Die ältere Version wird in den Papierkorb verschoben, nicht gelöscht. | The older version is moved to the trash, not deleted. |
| 296 | `profile_poll` | Auf Änderungen prüfen alle … Minuten | Check for changes every … minutes |
| 297 | `profile_poll_hint` | Nur solange das Handy im Ziel-WLAN ist (mindestens 15). Zusätzlich sofort beim Verbinden mit dem WLAN. | Only while the mobile is on the target Wi-Fi (at least 15). Additionally right when connecting to the Wi-Fi. |
| 298 | `profile_after_upload` | Nach erfolgreicher Übertragung | After a successful transfer |
| 299 | `profile_after_upload_hint` | Was passiert mit den Fotos auf dem Handy, wenn sie sicher auf dem PC liegen? | What happens to the photos on the mobile once they are safely on the PC? |
| 300 | `profile_delete_hint` | Dateien werden nur entfernt, wenn der Upload geprüft wurde (Grösse + Prüfsumme). Android fragt dabei einmal pro Durchgang nach Bestätigung. | Files are only removed if the upload was verified (size + checksum). Android asks for confirmation once per run. |
| 301 | `profile_delete_final_hint` | Achtung: Endgültig gelöschte Fotos kommen nicht in den Papierkorb der Galerie und lassen sich auf dem Handy nicht wiederherstellen. Sie sind danach nur noch auf dem PC. | Caution: permanently deleted photos do not go to the gallery trash and cannot be restored on the mobile. Afterwards they only exist on the PC. |
| 302 | `profile_verify` | Prüfung nach dem Hochladen | Check after upload |
| 303 | `profile_verify_hint` | Liest die Datei auf dem PC zurück und vergleicht sie mit dem Original. Gründlicher heisst sicherer, aber langsamer. | Reads the file back from the PC and compares it with the original. More thorough means safer, but slower. |
| 304 | `profile_verify_required` | Mindestens „Wenig“ ist Pflicht, weil Dateien vom Handy entfernt werden. | At least “Low” is required because files are removed from the mobile. |
| 305 | `verify_off` | Aus | Off |
| 306 | `verify_off_sub` | Nur die Dateigrösse wird verglichen. Nur möglich, wenn die Fotos auf dem Handy bleiben. | Only the file size is compared. Only possible if the photos stay on the mobile. |
| 307 | `verify_low` | Wenig | Low |
| 308 | `verify_low_sub` | Anfang, Mitte und Ende (192 KB pro Datei) | Start, middle and end (192 KB per file) |
| 309 | `verify_medium` | Mittel | Medium |
| 310 | `verify_medium_sub` | Bis 32 MB die ganze Datei, darüber 32 Stichproben (2 MB). Standard | Up to 32 MB the whole file, above that 32 samples (2 MB). Default |
| 311 | `verify_high` | Viel | High |
| 312 | `verify_high_sub` | Bis 256 MB die ganze Datei, darüber 128 Stichproben (8 MB) | Up to 256 MB the whole file, above that 128 samples (8 MB) |
| 313 | `verify_full` | Komplett | Complete |
| 314 | `verify_full_sub` | Die ganze Datei. Braucht deutlich mehr Zeit und viel Akku. | The whole file. Takes much more time and a lot of battery. |
| 315 | `verify_full_title` | Komplett prüfen? | Check completely? |
| 316 | `verify_full_text` | Jede Datei wird nach dem Hochladen vollständig vom PC zurückgelesen. Bei Videos dauert die Sicherung dadurch etwa doppelt so lange und braucht deutlich mehr Akku. | Every file is read back completely from the PC after uploading. For videos the backup takes about twice as long and uses considerably more battery. |
| 317 | `verify_full_confirm` | Komplett prüfen | Check completely |
| 318 | `verify_raised` | Prüfung auf „Mittel“ gestellt: Wer Fotos vom Handy entfernt, braucht eine Prüfung. | Check set to “Medium”: removing photos from the mobile requires a check. |
| 319 | `profile_filter` | Filter (optional) | Filter (optional) |
| 320 | `profile_filter_include` | Nur diese Dateien | Only these files |
| 321 | `profile_filter_exclude` | Diese Dateien ausschliessen | Exclude these files |
| 322 | `profile_filter_hint` | Mehrere Muster mit Komma oder Zeilenumbruch trennen. * = beliebig, ? = ein Zeichen, Ordner/** = alles in diesem Ordner. Ist „Nur diese Dateien“ leer, gilt alles. „Ausschliessen“ hat Vorrang. | Separate several patterns with a comma or line break. * = anything, ? = one character, folder/** = everything in this folder. If “Only these files” is empty, everything counts. “Exclude” takes precedence. |
| 323 | `profile_section_unset` | Nicht eingestellt – antippen zum Öffnen | Not set – tap to open |
| 324 | `profile_section_collapse` | Einklappen | Collapse |
| 325 | `profile_guard` | Schutzbremse (optional) | Safety brake (optional) |
| 326 | `profile_guard_percent` | Nachfragen, wenn mehr als … % gelöscht würden | Ask if more than … % would be deleted |
| 327 | `profile_guard_hint` | Sollen auf einmal mehr als so viele Prozent der Dateien in den Papierkorb, hält die Synchronisierung an und fragt dich zuerst. Standard: 20. Unabhängig davon fragt LeoSync immer ab 100 Löschungen auf einmal und wenn ein ganzer Unterordner mit mehr als 5 Dateien verschwinden würde. Bei höchstens 5 Dateien im Profil nur, wenn alle gelöscht würden. | If more than this percentage of files would go to the trash at once, the sync stops and asks you first. Default: 20. Regardless of this, LeoSync always asks from 100 deletions at once and when a whole subfolder with more than 5 files would disappear. With at most 5 files in the profile only if all would be deleted. |
| 328 | `profile_next` | Weiter | Next |
| 329 | `profile_optional_hint` | Die weiteren Schritte sind optional – du kannst jetzt schon speichern. | The remaining steps are optional – you can already save now. |
| 330 | `profile_simulation` | Simulation | Simulation |
| 331 | `profile_simulation_hint` | Zeigt, was beim Synchronisieren mit diesem Profil passieren würde – ohne etwas zu übertragen. | Shows what would happen when syncing with this profile – without transferring anything. |
| 332 | `profile_simulate` | Simulieren | Simulate |
| 333 | `profile_simulation_save_first` | Zuerst speichern – simuliert wird der gespeicherte Stand. | Save first – the saved state is simulated. |
| 334 | `profile_maintenance` | Wartung | Maintenance |
| 335 | `profile_maintenance_hint` | Vergisst, welche Dateien bereits übertragen wurden. Bei der nächsten Synchronisierung wird alles mit der Freigabe abgeglichen (identische Dateien werden nicht erneut übertragen). | Forgets which files were already transferred. The next sync compares everything with the Share (identical files are not transferred again). |
| 336 | `profile_reset` | Synchronisierungsstatus zurücksetzen | Reset sync status |
| 337 | `profile_delete_button` | Profil löschen | Delete profile |
| 338 | `profile_maintenance_closed` | Synchronisierungsstatus zurücksetzen, Profil löschen – antippen zum Öffnen | Reset sync status, delete profile – tap to open |
| 339 | `profile_delete_section_hint` | Löscht Einstellungen und Synchronisierungsstatus dieses Profils. Dateien auf Handy und PC bleiben erhalten. | Deletes the settings and sync status of this profile. Files on the mobile and PC are kept. |
| 340 | `profile_delete_title` | Profil löschen? | Delete profile? |
| 341 | `profile_delete_text` | Einstellungen und Synchronisierungsstatus dieses Profils werden gelöscht. Dateien auf Handy und PC bleiben erhalten. | Settings and sync status of this profile are deleted. Files on the mobile and PC are kept. |
| 342 | `profile_created_title` | Profil gespeichert | Profile saved |
| 343 | `profile_created_text` | Wie weiter? Bis du dieses Profil einmal synchronisierst, startet es nicht automatisch. So kannst du zuerst in Ruhe simulieren. | What next? Until you sync this profile once, it does not start automatically. That way you can simulate first. |
| 344 | `profile_later` | Später | Later |
| 345 | `profile_leave_title` | Änderungen speichern? | Save changes? |
| 346 | `profile_leave_text` | Du hast Änderungen an diesem Profil noch nicht gespeichert. | You have unsaved changes to this profile. |
| 347 | `profile_exists_title` | Profil existiert bereits | Profile already exists |
| 348 | `profile_overlap_title` | Profile überschneiden sich | Profiles overlap |
| 349 | `profile_not_saved` | Das Profil wurde nicht gespeichert. | The profile was not saved. |
| 350 | `profile_layout_blocked_title` | Ordnerstruktur passt nicht | Folder structure does not match |
| 351 | `profile_layout_title` | Ordnerstruktur | Folder structure |
| 352 | `profile_layout_pick_title` | Ordnerstruktur wählen | Choose folder structure |
| 353 | `profile_layout_hint` | Kann nach der ersten Synchronisierung nicht mehr geändert werden. | Cannot be changed after the first synchronisation. |
| 354 | `profile_layout_example_none` | Ohne Land: 2026\06\IMG_1.jpg | Without a country: 2026\06\IMG_1.jpg |
| 355 | `profile_layout_ex_month_suffix` | Beispiel: 2026\06 - Ägypten\IMG_1.jpg | Example: 2026\06 - Egypt\IMG_1.jpg |
| 356 | `profile_layout_ex_country_folder` | Beispiel: 2026\Ägypten\06\IMG_1.jpg | Example: 2026\Egypt\06\IMG_1.jpg |
| 357 | `profile_layout_ex_country_subfolder` | Beispiel: 2026\06\Ägypten\IMG_1.jpg | Example: 2026\06\Egypt\IMG_1.jpg |
| 358 | `profile_layout_adopted` | Von Profil „%1$s“ übernommen, das dasselbe Ziel verwendet. Alle Fotoprofile mit demselben Ziel müssen dieselbe Struktur verwenden. | Taken over from profile “%1$s”, which uses the same target. All photo profiles of one target must use the same structure. |
| 359 | `profile_layout_locked` | Die Struktur ist fest, weil von diesem Profil bereits Dateien synchronisiert wurden. Sie kann nicht mehr geändert werden. | The structure is fixed because files of this profile have already been synchronised. It can no longer be changed. |
| 360 | `profile_layout_running` | Die Struktur kann nicht geändert werden, solange eine Synchronisierung läuft. Versuche es nach deren Ende erneut. | The structure cannot be changed while a synchronisation is running. Try again when it has finished. |
| 361 | `profile_save_anyway` | Trotzdem speichern | Save anyway |
| 362 | `profile_checking_folder` | Prüfe Ordner… | Checking folder… |
| 363 | `profile_checking_folder_text` | Passt der neue Ordner zum bisherigen Synchronisierungsstand? | Does the new folder match the previous sync state? |
| 364 | `profile_folder_warning_title` | Ist das wirklich der richtige Ordner? | Is this really the right folder? |
| 365 | `profile_folder_warning_text` | Der gewählte Ordner passt nicht zum bisherigen Synchronisierungsstand dieses Profils. Nach einer Neuinstallation oder einem Import ist das normal: Wähle dann „Trotzdem übernehmen“. | The chosen folder does not match the previous sync state of this profile. After a reinstall or an import this is expected: choose “Accept anyway”. |
| 366 | `profile_reset_title` | Synchronisierungsstatus zurücksetzen? | Reset sync status? |
| 367 | `profile_reset_text` | Die nächste Synchronisierung gleicht alle Dateien erneut mit der Freigabe ab. Das kann bei vielen Dateien länger dauern. | The next sync compares all files with the Share again. With many files this can take a while. |
| 368 | `profile_reset_done` | Zurückgesetzt | Reset |
| 369 | `profile_reset_button` | Zurücksetzen | Reset |
| 370 | `wizard_intro_name` | Wie soll das Profil heissen? Der Name erscheint auf der Startseite, im Verlauf und in den Benachrichtigungen, z. B. „Kamera“ oder „Dokumente“. | What should the profile be called? The name appears on the home screen, in the history and in notifications, e.g. “Camera” or “Documents”. |
| 371 | `wizard_intro_source_mirror` | Welcher Ordner auf dem Handy soll mit dem PC abgeglichen werden? Alle Dateien und Unterordner darin werden synchronisiert. | Which folder on the mobile should be compared with the PC? All files and subfolders in it are synced. |
| 372 | `wizard_intro_source_photos` | Welche Fotos und Videos sollen gesichert werden? Wähle die Ordner auf dem Handy, in denen sie liegen. | Which photos and videos should be backed up? Choose the folders on the mobile where they are stored. |
| 373 | `wizard_intro_target` | Wohin auf dem PC? Gib die Windows-Freigabe an und melde dich mit dem Windows-Benutzer an, der darauf zugreifen darf. „Verbindung testen“ zeigt, ob alles stimmt. | Where on the PC? Enter the Windows Share and sign in with the Windows user who may access it. “Test connection” shows whether everything is right. |
| 374 | `wizard_intro_options_mirror` | Wann wird synchronisiert, in welche Richtung, und was passiert mit gelöschten oder geänderten Dateien? | When does it sync, in which direction, and what happens to deleted or changed files? |
| 375 | `wizard_intro_options_photos` | Wann wird synchronisiert, und was passiert danach mit den Fotos auf dem Handy? | When does it sync, and what happens to the photos on the mobile afterwards? |
| 376 | `wizard_intro_filter` | Optional: Nur bestimmte Dateien synchronisieren oder einzelne ausschliessen. Ohne Angaben wird alles synchronisiert. | Optional: sync only certain files or exclude some. Without entries everything is synced. |
| 377 | `wizard_intro_guard` | Optional: Ab wann LeoSync bei vielen Löschungen zuerst nachfragt. Der Standard passt für die meisten. | Optional: from when LeoSync asks first when there are many deletions. The default suits most people. |
| 378 | `profile_pc_check` | PC-Kennung | PC ID |
| 379 | `profile_pc_check_hint` | Woran LeoSync deinen PC erkennt, bevor es die Anmeldedaten sendet. | How LeoSync recognises your PC before it sends the sign-in data. |
| 380 | `profile_pc_opt_wifi` | Nur WLAN-Name | Wi-Fi name only |
| 381 | `profile_pc_opt_wifi_sub` | Wie bisher. Schwächer: Ein fremdes Netz mit gleichem WLAN-Namen und gleicher Adresse könnte die Anmeldedaten erhalten. | As before. Weaker: a foreign network with the same Wi-Fi name and the same address could receive the sign-in data. |
| 382 | `profile_pc_opt_guid` | Nur PC-Kennung | PC ID only |
| 383 | `profile_pc_opt_guid_sub` | Funktioniert in jedem WLAN. Der PC muss erkannt sein, bevor die Anmeldedaten gesendet werden. | Works in any Wi-Fi. The PC must be recognised before the sign-in data is sent. |
| 384 | `profile_pc_opt_both` | Beides | Both |
| 385 | `profile_pc_opt_both_sub` | WLAN-Name und PC-Kennung müssen beide stimmen. Am sichersten. | Wi-Fi name and PC ID must both match. Safest. |
| 386 | `profile_pc_status_known` | PC erkannt | PC recognised |
| 387 | `profile_pc_status_unknown` | PC noch nicht erkannt. Einmal „Verbindung testen“ tippen. | PC not recognised yet. Tap “Test connection” once. |
| 388 | `profile_pc_status_new` | Der PC wird beim Speichern erkannt, sobald er erreichbar ist (bei „Beides“ im eingetragenen WLAN). | The PC is recognised when you save, as soon as it is reachable (with “Both” in the listed Wi-Fi). |
| 389 | `profile_pc_learning` | PC wird erkannt … | Recognising the PC … |
| 390 | `profile_pc_status_changed` | Ein anderer PC hat geantwortet. Mit „Verbindung testen“ prüfen. | A different PC answered. Tap “Test connection” to check it. |
| 391 | `profile_pc_status_wifi` | Bei „Nur WLAN-Name“ nicht verwendet. | Not used with “Wi-Fi name only”. |
| 392 | `profile_pc_switch_hint` | Die PC-Kennung wird jetzt geprüft: Die Verbindung wird einmal getestet. | The PC ID is checked now: the connection is tested once. |
| 393 | `profile_pc_trust_title` | Anderem PC vertrauen? | Trust a different PC? |
| 394 | `profile_pc_trust_body` | Unter %1$s antwortet ein anderer PC als bisher. Nur fortfahren, wenn du den PC neu installiert oder ersetzt hast. Deine Anmeldedaten werden nur gesendet, wenn du dem neuen PC vertraust. | At %1$s a different PC answers than before. Only continue if you reinstalled the PC or replaced it. Your sign-in data is only sent if you trust the new PC. |
| 395 | `profile_pc_trust_yes` | Neuem PC vertrauen | Trust new PC |
| 396 | `profile_pc_trust_no` | Nicht vertrauen | Do not trust |
| 397 | `profile_host_change_title` | Anderen PC verwenden? | Use a different PC? |
| 398 | `profile_host_change_body` | Du hast den Server von %1$s auf %2$s geändert. Dort ist noch kein PC erkannt. Deine Anmeldedaten werden an diesen PC gesendet, sobald du fortfährst. Nur fortfahren, wenn %2$s dein eigener PC ist. | You changed the server from %1$s to %2$s. No PC is recognised there yet. Your sign-in data will be sent to that PC as soon as you continue. Only continue if %2$s is your own PC. |
| 399 | `profile_host_change_yes` | Anmeldedaten senden | Send sign-in data |
| 400 | `home_profile_pc_known` | PC erkannt | PC recognised |
| 401 | `home_profile_pc_unknown` | PC noch nicht erkannt | PC not recognised yet |
| 402 | `profile_number_range` | Bitte eine Zahl von %1$d bis %2$d eingeben | Please enter a number from %1$d to %2$d |
| 403 | `profile_number_step_blocked` | Bitte zuerst das Zahlenfeld mit Fehler in diesem Schritt korrigieren. | Please correct the number field with an error in this step first. |
| 404 | `profile_number_blocked` | Nicht gespeichert: Bitte zuerst die Zahlenfelder mit Fehler korrigieren. | Not saved: please correct the number fields with errors first. |
| 405 | `profile_test_need_path` | Bitte zuerst den Freigabe-Pfad eingeben, z. B. %1$s. | Please enter the share path first, e.g. %1$s. |
| 406 | `profile_test_bad_input` | Die Eingabe ist ungültig. Bitte Freigabe-Pfad und Benutzer prüfen. | The input is invalid. Please check the share path and user. |
| 407 | `profile_confirm_title` | Änderung bestätigen | Confirm change |
| 408 | `profile_confirm_dir_up` | Die Richtung wird von „%1$s“ auf „%2$s“ geändert. Ab der nächsten Synchronisierung gilt das Handy als Quelle: abweichende Dateien auf dem PC werden durch die Handy-Version ersetzt. Die ersetzte Version kann in den Papierkorb „%3$s“ kommen. | The direction changes from “%1$s” to “%2$s”. From the next sync, the mobile is the source: differing files on the PC are replaced by the mobile version. The replaced version may go to the trash “%3$s”. |
| 409 | `profile_confirm_dir_down` | Die Richtung wird von „%1$s“ auf „%2$s“ geändert. Ab der nächsten Synchronisierung gilt der PC als Quelle: abweichende Dateien auf dem Handy werden durch die PC-Version ersetzt. Die ersetzte Version kann in den Papierkorb „%3$s“ kommen. | The direction changes from “%1$s” to “%2$s”. From the next sync, the PC is the source: differing files on the mobile are replaced by the PC version. The replaced version may go to the trash “%3$s”. |
| 410 | `profile_confirm_dir_both` | Die Richtung wird von „%1$s“ auf „%2$s“ geändert. Ab der nächsten Synchronisierung werden Änderungen in beide Richtungen übertragen. | The direction changes from “%1$s” to “%2$s”. From the next sync, changes are transferred in both directions. |
| 411 | `profile_confirm_propagate` | Gelöschte Dateien werden ab jetzt auf der anderen Seite ebenfalls entfernt (in den Papierkorb „%1$s“). | Files deleted on one side will now also be removed on the other side (to the trash “%1$s”). |
| 412 | `profile_folder_unusable_root` | Der gewählte Ordner ist der oberste Ordner des Speichers und kann nicht verwendet werden. Bitte einen Unterordner wählen. | The selected folder is the top level of the storage and cannot be used. Please choose a subfolder. |
| 413 | `profile_folder_unusable_provider` | Dieser Ordner wird von Android über einen Umweg (z. B. „Downloads“) geliefert und kann für Fotos nicht verwendet werden. Bitte den Ordner direkt im internen Speicher wählen. | Android provides this folder via a detour (e.g. “Downloads”), so it cannot be used for photos. Please choose the folder directly in internal storage. |
| 414 | `profile_filter_include_hint` | Alles andere wird nicht synchronisiert. | Everything else is not synchronized. |
| 415 | `profile_filter_bad` | Ungültiges Muster „%1$s“: Eine Klammer fehlt oder ist zu viel. | Invalid pattern “%1$s”: a bracket is missing or extra. |
| 416 | `profile_filter_summary_include` | Nur: %1$s | Only: %1$s |
| 417 | `profile_filter_summary_exclude` | Ausgeschlossen: %1$d Muster | Excluded: %1$d patterns |
| 418 | `profile_folder_warning_options` | Trotzdem übernehmen: der bisherige Synchronisierungsstand bleibt. / Als neuen Ordner behandeln: der Stand wird zurückgesetzt, alle Dateien werden neu abgeglichen. | Accept anyway: the previous sync state is kept. / Treat as new folder: the state is reset and all files are compared again. |
| 419 | `overlap_nested_remote` | Der Zielordner auf dem PC liegt innerhalb eines Ordners von Profil „%1$s“ (oder umgekehrt). Dieselben Dateien würden von beiden Profilen synchronisiert – das kann zu doppelten Kopien, Konflikten oder ungewollten Löschungen führen. | The target folder on the PC lies inside a folder of profile “%1$s” (or vice versa). The same files would be synchronized by both profiles – this can lead to duplicate copies, conflicts or unwanted deletions. |
| 420 | `overlap_cross_pc_from_photos` | Der Zielordner dieser Fotosicherung liegt im PC-Ordner von Profil „%1$s“ (oder umgekehrt). Das Ordner-Profil behandelt die gesicherten Fotos wie eigene Dateien: Sie können wieder aufs Handy geladen oder bei übernommenen Löschungen in den Papierkorb gelegt werden. | The target folder of this photo backup lies inside the PC folder of profile “%1$s” (or vice versa). The folder profile treats the backed-up photos like its own files: they can be downloaded to the mobile again or moved to the recycle bin when deletions are applied. |
| 421 | `overlap_cross_phone_from_photos` | Ein Quellordner dieser Fotosicherung liegt im Handy-Ordner von Profil „%1$s“, das Dateien aufs Handy lädt (oder umgekehrt). Dieses Profil kann dort Dateien ersetzen oder neue Dateien ablegen, die dann als Fotos gesichert würden. | A source folder of this photo backup lies inside the mobile folder of profile “%1$s”, which loads files onto the mobile (or vice versa). That profile can replace files there or add new files that would then be backed up as photos. |
| 422 | `overlap_cross_phone_from_mirror` | Der Handy-Ordner dieses Profils, das Dateien aufs Handy lädt, liegt in einem Quellordner der Fotosicherung „%1$s“ (oder umgekehrt). Dieses Profil kann dort Dateien ersetzen oder neue Dateien ablegen, die dann als Fotos gesichert würden. | The mobile folder of this profile, which loads files onto the mobile, lies inside a source folder of photo backup “%1$s” (or vice versa). This profile can replace files there or add new files that would then be backed up as photos. |
| 423 | `overlap_cross_pc_from_mirror` | Der PC-Ordner dieses Profils liegt im Zielordner der Fotosicherung „%1$s“ (oder umgekehrt). Dieses Profil behandelt die gesicherten Fotos wie eigene Dateien: Sie können aufs Handy geladen oder bei übernommenen Löschungen in den Papierkorb gelegt werden. | The PC folder of this profile lies inside the target folder of photo backup “%1$s” (or vice versa). This profile treats the backed-up photos like its own files: they can be downloaded to the mobile or moved to the recycle bin when deletions are applied. |
| 424 | `overlap_nested_local` | Der Ordner auf dem Handy liegt innerhalb eines Ordners von Profil „%1$s“ (oder umgekehrt). Dieselben Dateien würden von beiden Profilen synchronisiert – das kann zu doppelten Kopien, Konflikten oder ungewollten Löschungen führen. | The folder on the mobile lies inside a folder of profile “%1$s” (or vice versa). The same files would be synchronized by both profiles – this can lead to duplicate copies, conflicts or unwanted deletions. |
| 425 | `home_profile_filter_active` | Filter aktiv | Filter active |
| 426 | `enc_title` | Verschlüsselung | Encryption |
| 427 | `enc_title_value` | %1$s · %2$s | %1$s · %2$s |
| 428 | `enc_intro` | Schützt die Übertragung zum PC, damit niemand im Netz mitlesen kann. | Protects the transfer to the PC so nobody on the network can read along. |
| 429 | `enc_value_off` | Aus | Off |
| 430 | `enc_value_preferred` | Wenn möglich | If possible |
| 431 | `enc_value_required` | Erforderlich | Required |
| 432 | `enc_off` | Aus | Off |
| 433 | `enc_off_sub` | Keine Verschlüsselung, wie bisher. Nur in einem Netz empfohlen, dem du vertraust. | No encryption, as before. Only recommended on a network you trust. |
| 434 | `enc_preferred` | Wenn möglich (empfohlen) | If possible (recommended) |
| 435 | `enc_preferred_sub` | Verschlüsselt, sobald der PC es kann. Sonst läuft die Verbindung unverschlüsselt weiter. | Encrypted as soon as the PC supports it. Otherwise the connection stays unencrypted. |
| 436 | `enc_required` | Erforderlich | Required |
| 437 | `enc_required_sub` | Verbindet nur verschlüsselt. Kann der PC das nicht, kommt keine Verbindung zustande und es wird kein Passwort gesendet. Am sichersten. | Connects only if encrypted. If the PC cannot do that, no connection is made and no password is sent. Most secure. |
| 438 | `enc_test_ok` | ✓ Verschlüsselt (%1$s) | ✓ Encrypted (%1$s) |
| 439 | `enc_test_warn` | ⚠ Nicht verschlüsselt (%1$s). Dein PC oder NAS kann nicht verschlüsseln oder es ist ausgeschaltet. | ⚠ Not encrypted (%1$s). Your PC or NAS cannot encrypt, or it is switched off. |
| 440 | `enc_test_off` | Nicht verschlüsselt (%1$s), wie eingestellt. | Not encrypted (%1$s), as set. |
| 441 | `enc_help` | Hilfe: Verschlüsselung einschalten | Help: turn on encryption |
| 442 | `enc_test_required_fail` | ✗ Verbindung nicht möglich: Dein PC oder NAS kann keine verschlüsselte Verbindung herstellen. Es wurden keine Anmeldedaten gesendet. Schalte die Verschlüsselung am PC oder NAS ein (siehe Hilfe) oder wähle «Wenn möglich». | ✗ Connection not possible: Your PC or NAS cannot make an encrypted connection. No sign-in data was sent. Turn on encryption on the PC or NAS (see help) or choose «If possible». |

## Profil-Optionen, doppelte Profile, Typwahl

| Nr. | Schlüssel | Deutsch | Englisch |
|---:|---|---|---|
| 443 | `delete_mode_keep` | Dateien behalten | Keep files |
| 444 | `delete_mode_trash` | In den Papierkorb verschieben | Move to trash |
| 445 | `delete_mode_delete` | Endgültig löschen | Delete permanently |
| 446 | `profile_type_label_photos` | Fotos & Videos sichern | Back up photos & videos |
| 447 | `profile_type_label_mirror` | Ordner synchronisieren | Sync a folder |
| 448 | `direction_upload` | Handy → PC | Mobile → PC |
| 449 | `direction_upload_short` | → PC | → PC |
| 450 | `direction_download` | PC → Handy | PC → Mobile |
| 451 | `direction_download_short` | → Handy | → Mobile |
| 452 | `direction_both` | Beidseitig | Both ways |
| 453 | `direction_both_short` | ⇄ | ⇄ |
| 454 | `conflict_mode_ask` | Jedes Mal fragen | Ask every time |
| 455 | `conflict_mode_newer` | Immer die neuere Datei übernehmen | Always take the newer file |
| 456 | `wizard_step_name` | Profilname | Profile name |
| 457 | `wizard_step_source` | Quelle | Source |
| 458 | `wizard_step_target` | Ziel | Target |
| 459 | `wizard_step_options` | Synchronisierungsoptionen | Sync options |
| 460 | `wizard_step_filter` | Filter | Filter |
| 461 | `wizard_step_guard` | Schutzbremse | Safety brake |
| 462 | `wizard_missing_name` | Bitte gib dem Profil einen Namen. | Please give the profile a name. |
| 463 | `wizard_missing_local` | Bitte wähle den Ordner auf dem Handy. | Please choose the folder on the mobile. |
| 464 | `wizard_missing_sources` | Bitte wähle mindestens einen Ordner auf dem Handy. | Please choose at least one folder on the mobile. |
| 465 | `wizard_missing_media` | Bitte schalte Fotos, Videos oder beides ein. | Please turn on photos, videos or both. |
| 466 | `wizard_missing_target` | Bitte gib den Freigabe-Pfad ein, z. B. \\192.168.1.20\Fotos. | Please enter the Share path, e.g. \\192.168.1.20\Photos. |
| 467 | `overlap_identical` | Profil „%1$s“ hat bereits dasselbe Ziel und dieselben Quellen. | Profile “%1$s” already has the same target and the same sources. |
| 468 | `overlap_nested` | Ein Ordner dieses Profils liegt innerhalb eines Ordners von Profil „%1$s“ (oder umgekehrt). Dieselben Dateien würden von beiden Profilen synchronisiert – das kann zu doppelten Kopien, Konflikten oder ungewollten Löschungen führen. | A folder of this profile lies inside a folder of profile “%1$s” (or the other way round). The same files would be synced by both profiles – this can lead to duplicate copies, conflicts or unwanted deletions. |
| 469 | `overlap_shared_folders` | Gleiches Ziel wie Profil „%1$s“ und gemeinsame Quellordner: %2$s. Diese Dateien würden von beiden Profilen geprüft und übertragen. | Same target as profile “%1$s” and shared source folders: %2$s. These files would be checked and transferred by both profiles. |
| 470 | `overlap_blocked_layout` | Profil „%1$s“ verwendet dieses Ziel bereits mit einer anderen Ordnerstruktur (%2$s). Alle Fotoprofile mit demselben Ziel müssen dieselbe Struktur verwenden, deshalb kann dieses Profil nicht gespeichert werden. Wähle dieselbe Struktur oder ein anderes Ziel. | Profile “%1$s” already uses this target with a different folder structure (%2$s). All photo profiles of the same target must use the same structure, so this profile cannot be saved. Choose the same structure or another target. |
| 471 | `folder_layout_month_suffix` | Jahr\MM[ - Land] | Year\MM[ - Country] |
| 472 | `folder_layout_country_folder` | Jahr\[Land\]MM | Year\[Country\]MM |
| 473 | `folder_layout_country_subfolder` | Jahr\MM\[Land] | Year\MM\[Country] |
| 474 | `overlap_same_target` | Gleiches Ziel wie Profil „%1$s“. Diese Dateien würden von beiden Profilen geprüft und übertragen. | Same target as profile “%1$s”. These files would be checked and transferred by both profiles. |
| 475 | `type_chooser_title` | Was möchtest du synchronisieren? | What do you want to sync? |
| 476 | `type_chooser_photos` | Fotos und Videos vom Handy auf den PC. Ablage sortiert nach Jahr, Monat und Reiseland, z. B. 2026\06 - Ägypten. Optional danach vom Handy entfernen. | Photos and videos from the mobile to the PC. Stored by year, month and travel country, e.g. 2026\06 - Egypt. Optionally remove them from the mobile afterwards. |
| 477 | `type_chooser_mirror` | Einen beliebigen Ordner 1:1 spiegeln, mit Unterordnern und allen Dateitypen. Richtung wählbar: Handy → PC, PC → Handy oder beidseitig. | Mirror any folder 1:1, with subfolders and all file types. Direction selectable: mobile → PC, PC → mobile or both ways. |
| 478 | `remote_picker_title` | Ordner auf der Freigabe | Folder on the Share |
| 479 | `remote_picker_title_shares` | Freigaben auf dem PC | Shares on the PC |
| 480 | `remote_picker_no_shares` | Keine Freigaben gefunden. Ist ein Ordner auf dem PC freigegeben (Eigenschaften → Freigabe)? | No Shares found. Is a folder shared on the PC (Properties → Sharing)? |
| 481 | `remote_picker_empty` | Keine Unterordner | No subfolders |
| 482 | `remote_picker_use` | Diesen Ordner verwenden | Use this folder |
| 483 | `remote_picker_new_folder` | Neuer Ordner | New folder |
| 484 | `remote_picker_new_folder_name` | Ordnername | Folder name |
| 485 | `remote_picker_create` | Anlegen | Create |
| 486 | `remote_picker_name_invalid` | Ungültiger Name. Kein \ / : * ? " < > \|, kein Punkt oder Leerzeichen am Ende. | Invalid name. No \ / : * ? " < > \|, no dot or space at the end. |
| 487 | `remote_picker_name_exists` | Ein Ordner «%1$s» existiert bereits. | A folder named "%1$s" already exists. |
| 488 | `remote_picker_create_failed` | Der Ordner konnte nicht angelegt werden. | The folder could not be created. |

## Speicher freigeben

| Nr. | Schlüssel | Deutsch | Englisch |
|---:|---|---|---|
| 489 | `free_hint` | Speicher freigeben gibt es nur bei Fotos-Profilen und bei Ordner-Profilen Handy → PC ohne „Löschungen übernehmen“. | Free up space is only available for photo profiles and for folder profiles mobile → PC without “Apply deletions”. |
| 490 | `free_no_profile` | Kein passendes Profil. | No suitable profile. |
| 491 | `free_choose_profile` | Für welches Profil möchtest du Speicher freigeben? | For which profile do you want to free up space? |
| 492 | `free_kind_photos` | Fotos & Videos | Photos & videos |
| 493 | `free_kind_folder` | Ordner Handy → PC | Folder mobile → PC |
| 494 | `free_read_error` | Fehler beim Einlesen | Error while reading |
| 495 | `free_moved_to_gallery_trash (one)` | %1$s Datei in den Papierkorb der Galerie verschoben. | %1$s file moved to the gallery trash. |
| 496 | `free_moved_to_gallery_trash (other)` | %1$s Dateien in den Papierkorb der Galerie verschoben. | %1$s files moved to the gallery trash. |
| 497 | `free_reading` | Lese Dateien… | Reading files… |
| 498 | `free_checking_pc` | Prüfe auf dem PC… | Checking on the PC… |
| 499 | `free_confirmed (one)` | %1$s Datei auf dem PC bestätigt. | %1$s file confirmed on the PC. |
| 500 | `free_confirmed (other)` | %1$s Dateien auf dem PC bestätigt. | %1$s files confirmed on the PC. |
| 501 | `free_missing (one)` | %1$s wurde auf dem PC nicht gefunden oder ist anders – sie bleibt. | %1$s was not found on the PC or is different – it stays. |
| 502 | `free_missing (other)` | %1$s wurden auf dem PC nicht gefunden oder sind anders – sie bleiben. | %1$s were not found on the PC or are different – they stay. |
| 503 | `free_unreachable_hosts` | PC nicht erreichbar: %1$s. | PC not reachable: %1$s. |
| 504 | `free_other_pc_hosts` | Es antwortet ein anderer oder noch nicht erkannter PC: %1$s. Es wurden keine Anmeldedaten gesendet. | A different or not yet recognised PC answers: %1$s. No sign-in data was sent. |
| 505 | `free_check_failed` | Prüfung fehlgeschlagen: %1$s | Check failed: %1$s |
| 506 | `free_deleted (one)` | %1$s Datei gelöscht, %2$s frei. | %1$s file deleted, %2$s freed. |
| 507 | `free_deleted (other)` | %1$s Dateien gelöscht, %2$s frei. | %1$s files deleted, %2$s freed. |
| 508 | `free_folders_removed (one)` | %1$s leerer Ordner entfernt. | %1$s empty folder removed. |
| 509 | `free_folders_removed (other)` | %1$s leere Ordner entfernt. | %1$s empty folders removed. |
| 510 | `free_kept (one)` | %1$s Datei bleibt (auf dem PC nicht gefunden, anders oder inzwischen geändert). | %1$s file stays (not found on the PC, different or changed in the meantime). |
| 511 | `free_kept (other)` | %1$s Dateien bleiben (auf dem PC nicht gefunden, anders oder inzwischen geändert). | %1$s files stay (not found on the PC, different or changed in the meantime). |
| 512 | `free_pc_lost` | Der PC war nicht (mehr) erreichbar. | The PC was not (or no longer) reachable. |
| 513 | `free_intro_photos` | Verschiebt Fotos und Videos, die nachweislich auf dem PC gesichert sind, in den Papierkorb der Galerie. Vorher prüft LeoSync jede Datei auf dem PC. Was dort fehlt oder anders ist, bleibt. | Moves photos and videos that are verifiably backed up on the PC to the gallery trash. Before that LeoSync checks every file on the PC. Whatever is missing there or different stays. |
| 514 | `free_verify_off_hint` | Die Prüfung nach dem Hochladen steht bei diesem Profil auf „Aus“. Ungeprüfte Fotos werden hier nicht angeboten. Stelle im Profil mindestens „Wenig“ ein; ab der nächsten Synchronisierung erscheinen neu gesicherte Fotos. | The check after upload is set to “Off” for this profile. Unchecked photos are not offered here. Set at least “Low” in the profile; newly backed-up photos appear from the next sync on. |
| 515 | `free_intro_folder` | Löscht Dateien auf dem Handy, die nachweislich unverändert auf dem PC liegen. Auf dem PC bleiben sie erhalten. Ordner-Dateien kommen nicht in einen Papierkorb, sie werden nach der Prüfung direkt gelöscht. | Deletes files on the mobile that are verifiably unchanged on the PC. They are kept on the PC. Folder files do not go to a trash, they are deleted directly after the check. |
| 516 | `free_selection` | Auswahl | Selection |
| 517 | `free_profile` | Profil | Profile |
| 518 | `free_older_taken` | Älter als (Aufnahmedatum) | Older than (date taken) |
| 519 | `free_older_modified` | Älter als (Änderungsdatum) | Older than (date modified) |
| 520 | `free_any` | egal | any |
| 521 | `free_folders` | Ordner | Folders |
| 522 | `free_all` | Alle | All |
| 523 | `free_n_folders (one)` | %1$s Ordner | %1$s folder |
| 524 | `free_n_folders (other)` | %1$s Ordner | %1$s folders |
| 525 | `free_less` | Weitere Kriterien ausblenden | Hide more criteria |
| 526 | `free_more` | Weitere Kriterien… | More criteria… |
| 527 | `free_types` | Dateitypen | File types |
| 528 | `free_min_size` | Grösse ab | Minimum size |
| 529 | `free_no_minimum` | Kein Minimum | No minimum |
| 530 | `free_exclude` | Ausnehmen | Exclude |
| 531 | `free_nothing` | Nichts | Nothing |
| 532 | `free_n_patterns (one)` | %1$s Muster | %1$s pattern |
| 533 | `free_n_patterns (other)` | %1$s Muster | %1$s patterns |
| 534 | `free_keep_favorites` | Favoriten behalten | Keep favorites |
| 535 | `free_keep_favorites_sub` | In der Galerie als Favorit markierte Fotos bleiben auf dem Handy. | Photos marked as favorite in the gallery stay on the mobile. |
| 536 | `free_favorites_android11` | Favoriten kann Android erst ab Version 11 melden. | Android can only report favorites from version 11. |
| 537 | `free_matching (one)` | Passend: %1$s Datei · %2$s | Matching: %1$s file · %2$s |
| 538 | `free_matching (other)` | Passend: %1$s Dateien · %2$s | Matching: %1$s files · %2$s |
| 539 | `free_deselected` | %1$s in der Vorschau abgewählt. | %1$s deselected in the preview. |
| 540 | `free_not_backed_up (one)` | %1$s passende Datei ist (noch) nicht gesichert und bleibt. | %1$s matching file is not backed up (yet) and stays. |
| 541 | `free_not_backed_up (other)` | %1$s passende Dateien sind (noch) nicht gesichert und bleiben. | %1$s matching files are not backed up (yet) and stay. |
| 542 | `free_favorites_kept (one)` | %1$s Favorit bleibt. | %1$s favorite stays. |
| 543 | `free_favorites_kept (other)` | %1$s Favoriten bleiben. | %1$s favorites stay. |
| 544 | `free_preview_hide` | Vorschau ausblenden | Hide preview |
| 545 | `free_preview` | Vorschau | Preview |
| 546 | `free_run_trash` | Auf dem PC prüfen und in den Papierkorb | Check on the PC and move to trash |
| 547 | `free_run_delete` | Auf dem PC prüfen und löschen | Check on the PC and delete |
| 548 | `free_gallery_trash_hint` | Im Papierkorb der Galerie bleiben die Dateien 30 Tage wiederherstellbar. | Files in the gallery trash can be restored for 30 days. |
| 549 | `free_result` | Ergebnis | Result |
| 550 | `free_main_folder` | (Hauptordner) | (main folder) |
| 551 | `free_live_photos` | Live-Fotos (Bild + Video) werden nur gemeinsam entfernt. | Live photos (image + video) are only removed together. |
| 552 | `free_confirm_title (one)` | %1$s Datei löschen? | Delete %1$s file? |
| 553 | `free_confirm_title (other)` | %1$s Dateien löschen? | Delete %1$s files? |
| 554 | `free_confirm_text` | LeoSync prüft jede Datei auf dem PC (vorhanden, gleiche Grösse, gleicher Inhalt) und löscht sie danach endgültig vom Handy (%1$s). Auf dem PC bleiben sie erhalten. Was dort fehlt oder anders ist, bleibt auf dem Handy. | LeoSync checks every file on the PC (present, same size, same content) and then deletes it permanently from the mobile (%1$s). They are kept on the PC. Whatever is missing there or different stays on the mobile. |
| 555 | `free_confirm_button` | Prüfen und löschen | Check and delete |
| 556 | `free_direct_title` | Endgültig löschen? | Delete permanently? |
| 557 | `free_direct_text (one)` | Android 10 hat keinen Papierkorb. %1$s Datei wird endgültig vom Handy gelöscht. Sie ist auf dem PC vorhanden. | Android 10 has no trash. %1$s file will be permanently deleted from the mobile. It is present on the PC. |
| 558 | `free_direct_text (other)` | Android 10 hat keinen Papierkorb. %1$s Dateien werden endgültig vom Handy gelöscht. Sie sind auf dem PC vorhanden. | Android 10 has no trash. %1$s files will be permanently deleted from the mobile. They are present on the PC. |
| 559 | `free_direct_done (one)` | %1$s Datei gelöscht | %1$s file deleted |
| 560 | `free_direct_done (other)` | %1$s Dateien gelöscht | %1$s files deleted |
| 561 | `free_age_title` | Älter als | Older than |
| 562 | `free_apply` | Übernehmen | Apply |
| 563 | `free_age_any` | Alter egal | Any age |
| 564 | `free_age_older` | Älter als … | Older than … |
| 565 | `free_amount` | Anzahl | Amount |
| 566 | `free_all_folders` | Alle Ordner | All folders |
| 567 | `free_no_subfolders` | Keine Unterordner. | No subfolders. |
| 568 | `free_folder_includes_sub` | Ein Ordner schliesst seine Unterordner ein. | A folder includes its subfolders. |
| 569 | `free_all_types` | Alle Typen | All types |
| 570 | `free_own_extensions` | Eigene Endungen | Own extensions |
| 571 | `free_own_extensions_example` | z. B. iso, bak | e.g. iso, bak |
| 572 | `free_other_hint` | „%1$s“ = alles, was in keine der Gruppen passt. | “%1$s” = everything that fits none of the groups. |
| 573 | `free_custom_value` | Eigener Wert | Custom value |
| 574 | `free_exclude_hint` | Ein Muster pro Zeile. Passende Dateien bleiben immer auf dem Handy. | One pattern per line. Matching files always stay on the mobile. |
| 575 | `free_exclude_examples` | Beispiele: *.kdbx (alle Dateien mit dieser Endung), Wichtig/** (alles im Ordner „Wichtig“), Thumbs.db | Examples: *.kdbx (all files with this extension), Important/** (everything in the folder “Important”), Thumbs.db |
| 576 | `free_patterns` | Muster | Patterns |
| 577 | `kind_images` | Bilder | Images |
| 578 | `kind_videos` | Videos | Videos |
| 579 | `kind_audio` | Audio | Audio |
| 580 | `kind_documents` | Dokumente | Documents |
| 581 | `kind_archives` | Archive | Archives |
| 582 | `kind_other` | Andere | Other |
| 583 | `unit_days` | Tage | Days |
| 584 | `unit_weeks` | Wochen | Weeks |
| 585 | `unit_months` | Monate | Months |
| 586 | `unit_years` | Jahre | Years |
| 587 | `age_days (one)` | %1$s Tag | %1$s day |
| 588 | `age_days (other)` | %1$s Tage | %1$s days |
| 589 | `age_weeks (one)` | %1$s Woche | %1$s week |
| 590 | `age_weeks (other)` | %1$s Wochen | %1$s weeks |
| 591 | `age_months (one)` | %1$s Monat | %1$s month |
| 592 | `age_months (other)` | %1$s Monate | %1$s months |
| 593 | `age_years (one)` | %1$s Jahr | %1$s year |
| 594 | `age_years (other)` | %1$s Jahre | %1$s years |
| 595 | `free_enter_one_or_more` | Bitte 1 oder mehr eingeben. | Enter 1 or more. |
| 596 | `free_none_match` | Keine Datei erfüllt die Auswahl oder ist bereits auf dem PC gesichert. Auswahl lockern oder zuerst synchronisieren. | No file matches, or none is backed up on the PC yet. Relax the selection or sync first. |
| 597 | `free_no_trash_android10` | Android 10 hat keinen Papierkorb: Dateien werden endgültig vom Handy gelöscht. | Android 10 has no trash: files are permanently deleted from the mobile. |
| 598 | `free_deleted_from_mobile (one)` | %1$s Datei auf dem PC bestätigt und vom Handy gelöscht (%2$s frei). | %1$s file confirmed on the PC and deleted from the mobile (%2$s freed). |
| 599 | `free_deleted_from_mobile (other)` | %1$s Dateien auf dem PC bestätigt und vom Handy gelöscht (%2$s frei). | %1$s files confirmed on the PC and deleted from the mobile (%2$s freed). |
| 600 | `free_partial_result` | %1$d gelöscht, %2$d übersprungen. | %1$d deleted, %2$d skipped. |
| 601 | `free_skipped_note (one)` | %1$s Datei übersprungen (nicht auf dem PC bestätigt) und bleibt auf dem Handy. | %1$s file skipped (not confirmed on the PC) and stays on the mobile. |
| 602 | `free_skipped_note (other)` | %1$s Dateien übersprungen (nicht auf dem PC bestätigt) und bleiben auf dem Handy. | %1$s files skipped (not confirmed on the PC) and stay on the mobile. |

## Papierkorb

| Nr. | Schlüssel | Deutsch | Englisch |
|---:|---|---|---|
| 603 | `side_phone` | Handy | Mobile |
| 604 | `side_pc` | PC | PC |
| 605 | `trash_switch_profile` | Profil wechseln | Switch profile |
| 606 | `trash_choose_profile` | Welchen Papierkorb möchtest du öffnen? | Which trash do you want to open? |
| 607 | `trash_photos_hint` | Fotos-Profile haben keinen eigenen Papierkorb: Entfernte Fotos liegen im Papierkorb der Galerie und werden in der Galerie-App geleert. | Photo profiles have no trash of their own: removed photos are in the gallery trash and are emptied in the gallery app. |
| 608 | `intervention_waiting` | Wird nach der laufenden Synchronisierung ausgeführt. | Will run after the current sync. |
| 609 | `intervention_waiting_hint` | Sie können diese Ansicht verlassen. Beendet das System LeoSync, bevor die Aktion an der Reihe ist, wird nichts gelöscht. | You can leave this view. If the system ends LeoSync before it is the turn of this action, nothing is deleted. |
| 610 | `intervention_running` | Wird ausgeführt… | Running… |
| 611 | `intervention_running_hint` | Sie können diese Ansicht verlassen. Das Ergebnis erscheint hier oder als Hinweis. | You can leave this view. The result is shown here or as a notice. |
| 612 | `intervention_already_pending` | Für dieses Profil wartet oder läuft bereits eine Aktion. | Another action for this profile is already waiting or running. |
| 613 | `intervention_failed` | Fehlgeschlagen: %1$s | Failed: %1$s |
| 614 | `trash_nothing_deleted` | Nichts gelöscht: Die Einträge sind nicht mehr vorhanden, haben sich geändert oder sind zum Wiederherstellen vorgemerkt. | Nothing deleted: the entries are gone, have changed or are reserved for restoring. |
| 615 | `trash_reading` | Lese den Papierkorb auf Handy und PC… | Reading the trash on mobile and PC… |
| 616 | `trash_phone_summary (one)` | Handy: %1$s Datei, %2$s | Mobile: %1$s file, %2$s |
| 617 | `trash_phone_summary (other)` | Handy: %1$s Dateien, %2$s | Mobile: %1$s files, %2$s |
| 618 | `trash_pc_summary (one)` | PC: %1$s Datei, %2$s | PC: %1$s file, %2$s |
| 619 | `trash_pc_summary (other)` | PC: %1$s Dateien, %2$s | PC: %1$s files, %2$s |
| 620 | `trash_pc_unreachable` | PC: nicht erreichbar | PC: not reachable |
| 621 | `trash_pc_unreachable_hint` | Einträge aus dem PC-Papierkorb fehlen in der Liste. Wiederherstellen und Löschen auf dem PC werden vorgemerkt und bei der nächsten Synchronisierung mit erreichbarem PC ausgeführt. | Entries from the PC trash are missing from the list. Restoring and deleting on the PC are scheduled and carried out at the next sync with a reachable PC. |
| 622 | `trash_cleanup_button` | Papierkorb aufräumen… | Clean up trash… |
| 623 | `trash_reload` | Neu einlesen | Reload |
| 624 | `trash_empty` | Der Papierkorb ist leer. | The trash is empty. |
| 625 | `trash_intro` | Hier liegen gelöschte Dateien und ältere Versionen. Markiere Einträge, um sie wiederherzustellen oder endgültig zu löschen. | Deleted files and older versions are kept here. Mark entries to restore them or delete them permanently. |
| 626 | `trash_day` | Tag | Day |
| 627 | `trash_all_days` | Alle Tage | All days |
| 628 | `trash_select_day` | Alles vom %1$s auswählen | Select everything from %1$s |
| 629 | `trash_whole_folder` | Neueste Version je Datei wählen | Select newest version of each file |
| 630 | `trash_older_version` | ältere Version | older version |
| 631 | `trash_deleted` | gelöscht | deleted |
| 632 | `trash_reserved` | zum Wiederherstellen vorgemerkt | scheduled for restore |
| 633 | `trash_selected` | %1$s ausgewählt · %2$s | %1$s selected · %2$s |
| 634 | `trash_clear_selection` | Auswahl aufheben | Clear selection |
| 635 | `trash_restore` | Wiederherstellen | Restore |
| 636 | `trash_purged_pc_pending` | Handy geleert – PC wird bei der nächsten Synchronisierung mit PC geleert | Mobile emptied – the PC will be emptied at the next sync with the PC |
| 637 | `trash_purged` | Papierkorb aufgeräumt | Trash cleaned up |
| 638 | `trash_delete_title (one)` | %1$s Datei endgültig löschen? | Delete %1$s file permanently? |
| 639 | `trash_delete_title (other)` | %1$s Dateien endgültig löschen? | Delete %1$s files permanently? |
| 640 | `trash_delete_text` | Die markierten Einträge werden aus dem Papierkorb gelöscht und lassen sich danach nicht mehr wiederherstellen. | The marked entries are deleted from the trash and can no longer be restored afterwards. |
| 641 | `trash_delete_pc_later` | Der PC ist nicht erreichbar: Dort wird bei der nächsten Synchronisierung mit PC gelöscht. | The PC is not reachable: it will be deleted there at the next sync with the PC. |
| 642 | `trash_deleted_pc_pending` | Gelöscht – PC folgt bei der nächsten Synchronisierung mit PC | Deleted – the PC follows at the next sync with the PC |
| 643 | `trash_deleted_done` | Endgültig gelöscht | Permanently deleted |
| 644 | `trash_restore_title (one)` | %1$s Datei wiederherstellen? | Restore %1$s file? |
| 645 | `trash_restore_title (other)` | %1$s Dateien wiederherstellen? | Restore %1$s files? |
| 646 | `trash_restore_text` | Die aktuellen Versionen kommen zuerst in den Papierkorb. Die gewählten Versionen werden auf Handy und PC zurückgeschrieben. | The current versions go to the trash first. The chosen versions are written back on mobile and PC. |
| 647 | `trash_restore_now` | Das passiert gleich mit einer Synchronisierung. | This happens right away with a sync. |
| 648 | `trash_restore_later` | Der PC ist nicht erreichbar: Das passiert bei der nächsten Synchronisierung mit PC. | The PC is not reachable: this happens at the next sync with the PC. |
| 649 | `trash_cleanup` | Papierkorb aufräumen | Clean up trash |
| 650 | `trash_cleanup_hint` | Endgültig gelöschte Dateien lassen sich nicht mehr wiederherstellen. | Permanently deleted files can no longer be restored. |
| 651 | `trash_what` | Was? | What? |
| 652 | `trash_mode_older` | Älter als … Tage | Older than … days |
| 653 | `trash_mode_dates` | Einzelne Tage | Single days |
| 654 | `trash_no_days` | Keine Tage im Papierkorb. | No days in the trash. |
| 655 | `trash_none_chosen` | Keine gewählt | None chosen |
| 656 | `trash_days_chosen (one)` | %1$s Tag gewählt | %1$s day chosen |
| 657 | `trash_days_chosen (other)` | %1$s Tage gewählt | %1$s days chosen |
| 658 | `trash_mode_all` | Alles | Everything |
| 659 | `trash_where` | Wo? | Where? |
| 660 | `trash_phone_only_hint` | „%1$s“ schafft Platz auf dem Handy; die Kopien im PC-Papierkorb bleiben erhalten. | “%1$s” frees up space on the mobile; the copies in the PC trash are kept. |
| 661 | `trash_choose_what` | Bitte auswählen, was geleert werden soll. | Please choose what should be emptied. |
| 662 | `trash_affects (one)` | Betrifft %1$s Datei (%2$s) | Affects %1$s file (%2$s) |
| 663 | `trash_affects (other)` | Betrifft %1$s Dateien (%2$s) | Affects %1$s files (%2$s) |
| 664 | `trash_affects_pc_later` | – dazu der PC-Papierkorb, sobald der PC erreichbar ist | – plus the PC trash as soon as the PC is reachable |
| 665 | `trash_pc_later_hint` | Der PC ist nicht erreichbar. Das Handy wird sofort geleert, der PC bei der nächsten Synchronisierung mit PC. | The PC is not reachable. The mobile is emptied right away, the PC at the next sync with the PC. |
| 666 | `trash_kept_reserved (one)` | %1$s Datei ist zum Wiederherstellen vorgemerkt und bleibt. | %1$s file is scheduled for restore and stays. |
| 667 | `trash_kept_reserved (other)` | %1$s Dateien sind zum Wiederherstellen vorgemerkt und bleiben. | %1$s files are scheduled for restore and stay. |
| 668 | `trash_purge_confirm (one)` | %1$s Datei (%2$s) wird endgültig gelöscht (%3$s) | %1$s file (%2$s) will be permanently deleted (%3$s) |
| 669 | `trash_purge_confirm (other)` | %1$s Dateien (%2$s) werden endgültig gelöscht (%3$s) | %1$s files (%2$s) will be permanently deleted (%3$s) |
| 670 | `trash_purge_confirm_pc` | – auf dem PC bei der nächsten Synchronisierung mit PC | – on the PC at the next sync with the PC |
| 671 | `trash_purge_irreversible` | Das lässt sich nicht rückgängig machen. | This cannot be undone. |
| 672 | `trash_pick_days` | Tage wählen | Choose days |
| 673 | `trash_pick_day` | Tag wählen | Choose day |
| 674 | `trash_none` | Keine | None |
| 675 | `trash_day_files (one)` | %1$s Datei · %2$s | %1$s file · %2$s |
| 676 | `trash_day_files (other)` | %1$s Dateien · %2$s | %1$s files · %2$s |
| 677 | `trash_days_invalid` | Bitte Anzahl Tage eingeben (1 oder mehr). | Enter a number of days (1 or more). |
| 678 | `trash_delete_title_one` | «%1$s» endgültig löschen? | Delete “%1$s” permanently? |

## Ordner-Sync: Seiten

| Nr. | Schlüssel | Deutsch | Englisch |
|---:|---|---|---|
| 679 | `sides_both` | Handy und PC | Mobile and PC |
| 680 | `sides_phone_only` | Nur Handy | Mobile only |
| 681 | `sides_pc_only` | Nur PC | PC only |
| 682 | `err_local_access_lost` | Kein Zugriff mehr auf den Handy-Ordner – bitte im Profil neu auswählen | No access to the mobile folder anymore – please choose it again in the profile |

## Verlauf und Sicherung

| Nr. | Schlüssel | Deutsch | Englisch |
|---:|---|---|---|
| 683 | `log_export_file` | LeoSync-Verlauf_%1$s.txt | LeoSync-History_%1$s.txt |
| 684 | `log_export_failed` | Export fehlgeschlagen: %1$s | Export failed: %1$s |
| 685 | `log_share` | Verlauf teilen | Share history |
| 686 | `log_clear` | Verlauf löschen | Delete history |
| 687 | `log_filter_errors` | Fehler | Errors |
| 688 | `log_filter_transfers` | Übertragungen | Transfers |
| 689 | `log_empty` | Noch keine Einträge. | No entries yet. |
| 690 | `log_empty_both` | Keine Läufe mit Fehlern oder Übertragungen. | No runs with errors or transfers. |
| 691 | `log_empty_errors` | Keine Läufe mit Fehlern. | No runs with errors. |
| 692 | `log_empty_transfers` | Keine Läufe mit Übertragungen. | No runs with transfers. |
| 693 | `log_idle_group` | %1$s× geprüft – nichts zu tun | %1$s× checked – nothing to do |
| 694 | `log_status_partial` | Teilweise | Partial |
| 695 | `log_status_failed` | Fehler | Error |
| 696 | `log_status_paused` | Pausiert | Paused |
| 697 | `log_status_interrupted` | Unterbrochen | Interrupted |
| 698 | `log_status_waiting` | Wartet auf dich | Waiting for you |
| 699 | `log_status_running` | Läuft | Running |
| 700 | `log_trigger` | Auslöser: %1$s | Trigger: %1$s |
| 701 | `backup_exporting` | Exportiere… | Exporting… |
| 702 | `backup_exported (one)` | Gesichert: %1$s Profil | Backed up: %1$s profile |
| 703 | `backup_exported (other)` | Gesichert: %1$s Profile | Backed up: %1$s profiles |
| 704 | `backup_exported_sync (one)` | %1$s Datei Synchronisierungsstatus | sync status of %1$s file |
| 705 | `backup_exported_sync (other)` | %1$s Dateien Synchronisierungsstatus | sync status of %1$s files |
| 706 | `backup_passwords_encrypted` | Passwörter verschlüsselt | passwords encrypted |
| 707 | `backup_without_passwords` | ohne Passwörter | without passwords |
| 708 | `backup_reading` | Lese Sicherung… | Reading backup… |
| 709 | `backup_unreadable` | Sicherung nicht lesbar | Backup not readable |
| 710 | `backup_file_unreadable` | Datei nicht lesbar: %1$s | File not readable: %1$s |
| 711 | `backup_importing` | Importiere… | Importing… |
| 712 | `backup_imported (one)` | %1$s Profil importiert | %1$s profile imported |
| 713 | `backup_imported (other)` | %1$s Profile importiert | %1$s profiles imported |
| 714 | `backup_imported_sync (one)` | Synchronisierungsstatus für %1$s Datei | sync status for %1$s file |
| 715 | `backup_imported_sync (other)` | Synchronisierungsstatus für %1$s Dateien | sync status for %1$s files |
| 716 | `backup_skipped_identical` | Übersprungen, weil schon vorhanden (gleiches Ziel und gleiche Quellen): %1$s | Skipped because already present (same target and same sources): %1$s |
| 717 | `backup_overlapping` | Hinweis: Überschneidet sich mit einem bestehenden Profil (gleiches Ziel, gemeinsame Quellordner): %1$s | Note: overlaps with an existing profile (same target, shared source folders): %1$s |
| 718 | `backup_skipped_layout_blocked` | Übersprungen, weil das Ziel schon von einem anderen Fotoprofil mit anderer Ordnerstruktur verwendet wird: %1$s | Skipped because the target is already used by another photo profile with a different folder structure: %1$s |
| 719 | `backup_skipped_layout_unknown` | Nicht importiert, weil die Ordnerstruktur unbekannt ist (Sicherung aus einer neueren Version?): %1$s | Not imported because the folder structure is unknown (backup from a newer version?): %1$s |
| 720 | `backup_without_password` | Passwort neu eingeben bei: %1$s | Enter the password again for: %1$s |
| 721 | `backup_need_folder` | Handy-Ordner neu wählen bei: %1$s (Android überträgt die Ordner-Freigabe nicht). | Choose the mobile folder again for: %1$s (Android does not transfer the folder permission). |
| 722 | `backup_import_failed` | Import fehlgeschlagen: %1$s | Import failed: %1$s |
| 723 | `backup_title` | Sicherung | Backup |
| 724 | `backup_hint` | Sichert Profile, Einstellungen, Ordner-Zuordnungen und auf Wunsch den Synchronisierungsstatus in eine Datei – z. B. vor einem Handywechsel oder einer Neuinstallation. | Saves profiles, settings, folder assignments and optionally the sync status to a file – e.g. before changing mobiles or reinstalling. |
| 725 | `backup_export` | Exportieren… | Export… |
| 726 | `backup_import` | Importieren… | Import… |
| 727 | `backup_export_title` | Sicherung exportieren | Export backup |
| 728 | `backup_include_sync` | Synchronisierungsstatus mitsichern | Include sync status |
| 729 | `backup_include_sync_sub` | Nach dem Import ist sofort bekannt, welche Dateien schon auf dem PC sind. | After the import it is known right away which files are already on the PC. |
| 730 | `backup_password_optional` | Export-Passwort (optional) | Export password (optional) |
| 731 | `backup_password_repeat` | Export-Passwort wiederholen | Repeat export password |
| 732 | `backup_no_password_hint` | Ohne Export-Passwort enthält die Datei keine SMB-Passwörter; du gibst sie nach dem Import neu ein. | Without an export password the file contains no SMB passwords; you enter them again after the import. |
| 733 | `backup_password_hint` | Die SMB-Passwörter werden mit diesem Passwort verschlüsselt. Ohne es lassen sie sich nicht wiederherstellen. | The SMB passwords are encrypted with this password. Without it they cannot be restored. |
| 734 | `backup_choose_file` | Datei wählen… | Choose file… |
| 735 | `backup_password` | Export-Passwort | Export password |
| 736 | `backup_ask_password_hint` | Die Sicherung enthält verschlüsselte SMB-Passwörter. Ohne Export-Passwort werden die Profile trotzdem importiert, die Passwörter gibst du dann neu ein. | The backup contains encrypted SMB passwords. Without the export password the profiles are imported anyway, you then enter the passwords again. |
| 737 | `backup_without_password_button` | Ohne Passwort | Without password |
| 738 | `backup_import_title` | Sicherung importieren | Import backup |
| 739 | `backup_created` | Erstellt: %1$s (LeoSync %2$s) | Created: %1$s (LeoSync %2$s) |
| 740 | `backup_profiles` | Profile: %1$s | Profiles: %1$s |
| 741 | `backup_sync_status (one)` | Synchronisierungsstatus: %1$s Datei | Sync status: %1$s file |
| 742 | `backup_sync_status (other)` | Synchronisierungsstatus: %1$s Dateien | Sync status: %1$s files |
| 743 | `backup_pw_taken` | SMB-Passwörter: werden übernommen | SMB passwords: will be taken over |
| 744 | `backup_pw_wrong` | SMB-Passwörter: Export-Passwort fehlt oder ist falsch – Passwörter werden nicht übernommen | SMB passwords: export password missing or wrong – passwords are not taken over |
| 745 | `backup_pw_none` | SMB-Passwörter: nicht enthalten | SMB passwords: not included |
| 746 | `backup_replace_hint` | „Ersetzen“ löscht alle bisherigen Profile samt Synchronisierungsstatus und übernimmt auch die Einstellungen (nicht die Sprache der App, sie bleibt unverändert). „Hinzufügen“ legt die Profile zusätzlich an. | “Replace” deletes all existing profiles including sync status and also takes over the settings (not the app language, which stays as it is). “Add” creates the profiles in addition. |
| 747 | `backup_replace` | Ersetzen | Replace |
| 748 | `backup_import_done` | Import abgeschlossen | Import finished |
| 749 | `backup_err_replace_rejected` | Ersetzen abgebrochen, nichts wurde gelöscht: Die Sicherung enthält Profile, die nicht importiert werden können (unbekannte Ordnerstruktur, Sicherung aus einer neueren Version?): %1$s | Replace cancelled, nothing was deleted: the backup contains profiles that cannot be imported (unknown folder structure, backup from a newer version?): %1$s |
| 750 | `log_clear_title` | Gesamten Verlauf löschen? | Delete the whole history? |
| 751 | `log_clear_text` | %1$d Einträge werden gelöscht und lassen sich danach nicht wiederherstellen. Mit «Verlauf teilen» kannst du sie vorher sichern. | %1$d entries will be deleted and cannot be restored. Use “Share history” first to keep a copy. |
| 752 | `log_cleared` | Verlauf gelöscht | History deleted |
| 753 | `log_undo` | Rückgängig | Undo |
| 754 | `log_advice_missing` | Die Datei existiert auf dem Handy nicht mehr. Synchronisiere erneut; bleibt der Fehler, die Dateiliste im Profil prüfen. | The file no longer exists on the mobile. Sync again; if the error stays, check the file list in the profile. |
| 755 | `log_advice_connection` | Verbindung zum PC prüfen (WLAN, Freigabe), dann erneut synchronisieren. | Check the connection to the PC (Wi-Fi, share), then sync again. |
| 756 | `log_advice_unknown` | Synchronisiere erneut. Bleibt der Fehler, teile den Verlauf mit dem Entwickler. | Sync again. If the error stays, share the history with the developer. |

## Ordnerauswahl, Konflikte, Simulation, Statistik, Updates

| Nr. | Schlüssel | Deutsch | Englisch |
|---:|---|---|---|
| 757 | `trigger_folder_choice` | Ordnerauswahl | Folder selection |
| 758 | `decision_title` | Ordnerauswahl | Folder selection |
| 759 | `decision_apply_all` | Alle übernehmen | Apply all |
| 760 | `decision_none` | Keine offenen Entscheidungen. | No open decisions. |
| 761 | `decision_hint` | Für diese Monate gibt es auf dem PC bereits umbenannte Ordner. Wähle, wohin die Dateien sollen. Die Wahl gilt für alle Dateien der Gruppe und wird gemerkt. | For these months there are already renamed folders on the PC. Choose where the files should go. The choice applies to all files of the group and is remembered. |
| 762 | `decision_files (one)` | %1$s Datei (%2$s) | %1$s file (%2$s) |
| 763 | `decision_files (other)` | %1$s Dateien (%2$s) | %1$s files (%2$s) |
| 764 | `decision_profile` | Profil: %1$s | Profile: %1$s |
| 765 | `decision_new_folder` | Neuer Ordner: %1$s | New folder: %1$s |
| 766 | `decision_custom_name` | Eigener Name… | Custom name… |
| 767 | `decision_folder_name` | Ordnername (muss %1$s enthalten) | Folder name (must contain %1$s) |
| 768 | `decision_none_hint` | Hier fragt LeoSync, wenn es für einen Monat mehrere umbenannte Ordner auf dem PC gibt. | LeoSync asks here when there are several renamed folders on the PC for one month. |
| 769 | `decision_choose_first` | Wähle einen Ordner, um zu übernehmen. | Choose a folder to apply. |
| 770 | `decision_snack_one (one)` | %1$s Datei wird nach %2$s übertragen | %1$s file will be transferred to %2$s |
| 771 | `decision_snack_one (other)` | %1$s Dateien werden nach %2$s übertragen | %1$s files will be transferred to %2$s |
| 772 | `decision_snack_all (one)` | %1$s Datei wird übertragen (%2$s) | %1$s file will be transferred (%2$s) |
| 773 | `decision_snack_all (other)` | %1$s Dateien werden übertragen (%2$s) | %1$s files will be transferred (%2$s) |
| 774 | `decision_n_groups (one)` | %1$s Gruppe | %1$s group |
| 775 | `decision_n_groups (other)` | %1$s Gruppen | %1$s groups |
| 776 | `decision_n_files (one)` | %1$s Datei | %1$s file |
| 777 | `decision_n_files (other)` | %1$s Dateien | %1$s files |
| 778 | `decision_all_confirm (one)` | %1$s Gruppe mit %2$s wird übernommen. Gruppen ohne Wahl bleiben offen. | %1$s group with %2$s will be applied. Groups without a choice stay open. |
| 779 | `decision_all_confirm (other)` | %1$s Gruppen mit %2$s werden übernommen. Gruppen ohne Wahl bleiben offen. | %1$s groups with %2$s will be applied. Groups without a choice stay open. |
| 780 | `trigger_conflicts` | Konfliktlösung | Conflict resolution |
| 781 | `conflict_title` | Konflikte | Conflicts |
| 782 | `conflict_all_newer` | Alle: neuere | All: newer |
| 783 | `conflict_none` | Keine offenen Konflikte. | No open conflicts. |
| 784 | `conflict_hint` | Diese Dateien wurden auf dem Handy und auf dem PC geändert. Wähle, welche Version gilt. Die andere Version wird in den Ordner „%1$s“ verschoben, nicht gelöscht. Die Auswahl wird bei der nächsten Synchronisierung im WLAN angewendet. | These files were changed on the mobile and on the PC. Choose which version wins. The other version is moved to the folder “%1$s”, not deleted. The choice is applied at the next sync on Wi-Fi. |
| 785 | `conflict_newer` | (neuer) | (newer) |
| 786 | `conflict_keep_phone` | Handy behalten | Keep mobile |
| 787 | `conflict_keep_pc` | PC behalten | Keep PC |
| 788 | `conflict_keep_both` | Beide behalten | Keep both |
| 789 | `conflict_keep_phone_hint` | Handy behalten: Die PC-Version kommt in %1$s. | Keep mobile: the PC version goes to %1$s. |
| 790 | `conflict_keep_pc_hint` | PC behalten: Die Handy-Version kommt in %1$s. | Keep PC: the mobile version goes to %1$s. |
| 791 | `conflict_keep_both_hint` | Beide behalten: Beide Dateien bleiben; die Handy-Version erhält den Namenszusatz %1$s. | Keep both: both files stay; the mobile version gets the name suffix %1$s. |
| 792 | `conflict_snack_phone` | %1$s: Handy-Version behalten (gilt bei der nächsten Synchronisierung im WLAN) | %1$s: mobile version kept (applies at the next sync on Wi-Fi) |
| 793 | `conflict_snack_pc` | %1$s: PC-Version behalten (gilt bei der nächsten Synchronisierung im WLAN) | %1$s: PC version kept (applies at the next sync on Wi-Fi) |
| 794 | `conflict_snack_both` | %1$s: beide Versionen behalten (gilt bei der nächsten Synchronisierung im WLAN) | %1$s: both versions kept (applies at the next sync on Wi-Fi) |
| 795 | `conflict_snack_all (one)` | %1$s Konflikt entschieden: neuere Version behalten (gilt bei der nächsten Synchronisierung im WLAN) | %1$s conflict decided: newer version kept (applies at the next sync on Wi-Fi) |
| 796 | `conflict_snack_all (other)` | %1$s Konflikte entschieden: jeweils neuere Version behalten (gilt bei der nächsten Synchronisierung im WLAN) | %1$s conflicts decided: newer version kept (applies at the next sync on Wi-Fi) |
| 797 | `conflict_all_confirm (one)` | %1$s Konflikt: Die neuere Version bleibt, die andere kommt nach %2$s. Fortfahren? | %1$s conflict: the newer version is kept, the other goes to %2$s. Continue? |
| 798 | `conflict_all_confirm (other)` | %1$s Konflikte: Jeweils die neuere Version bleibt, die andere kommt nach %2$s. Fortfahren? | %1$s conflicts: in each case the newer version is kept, the other goes to %2$s. Continue? |
| 799 | `conflict_undo_late` | Nicht mehr rückgängig zu machen: Die Auswahl wurde bereits angewendet. | Can no longer be undone: the choice has already been applied. |
| 800 | `preview_starting` | Starte… | Starting… |
| 801 | `preview_hint` | Zeigt, was beim Synchronisieren passieren würde (Bildordner: Zielordner; Ordner-Synchronisierung: Aktion je Datei). Es wird nichts kopiert, angelegt oder gelöscht. | Shows what would happen when syncing (photo folders: target folder; folder sync: action per file). Nothing is copied, created or deleted. |
| 802 | `preview_only_first` | Nur die ersten | Only the first |
| 803 | `preview_files` | Dateien | files |
| 804 | `preview_all_files` | Alle Dateien | All files |
| 805 | `preview_all_hint` | Bei vielen Fotos dauert „Alle“ länger, weil jedes Foto auf Datum und Ort ausgewertet wird. | With many photos “All” takes longer because every photo is analysed for date and location. |
| 806 | `preview_start` | Simulation starten | Start simulation |
| 807 | `preview_limit_error` | Bitte eine Zahl von 1 bis 100000 eingeben. | Please enter a number from 1 to 100000. |
| 808 | `preview_offline_assumption` | Annahme: Der PC ist nicht bekannt, die Liste geht von einem leeren PC aus. Echte Aktionen können abweichen. | Assumption: the PC is not known, the list assumes an empty PC. Real actions can differ. |
| 809 | `preview_nothing_to_do` | Nichts zu tun – alles aktuell. | Nothing to do – everything is up to date. |
| 810 | `preview_nothing_new` | Nichts zu übertragen – keine neuen Dateien. | Nothing to transfer – no new files. |
| 811 | `preview_group_open` | %1$s (Ordner noch offen) | %1$s (folder still open) |
| 812 | `preview_summary (one)` | %1$s Datei in %2$s | %1$s file in %2$s |
| 813 | `preview_summary (other)` | %1$s Dateien in %2$s | %1$s files in %2$s |
| 814 | `preview_needs_decision` | Entscheidung nötig (Ordnerauswahl bzw. Konflikt) | Decision needed (folder selection or conflict) |
| 815 | `preview_more` | … und %1$s weitere | … and %1$s more |
| 816 | `stats_unknown` | Unbekannt | Unknown |
| 817 | `stats_no_location` | Ohne Standortdaten | Without location data |
| 818 | `stats_no_profile` | Noch kein Profil vorhanden. | No profile yet. |
| 819 | `stats_no_files` | Noch keine Dateien gesichert. | No files backed up yet. |
| 820 | `stats_total` | Gesamt | Total |
| 821 | `stats_photos_videos` | %1$s Fotos · %2$s Videos | %1$s photos · %2$s videos |
| 822 | `stats_per_year` | Pro Jahr | Per year |
| 823 | `stats_bar_hint` | Balkenlänge = Anzahl Dateien | Bar length = number of files |
| 824 | `stats_bucket_value (one)` | %1$s Datei · %2$s | %1$s file · %2$s |
| 825 | `stats_bucket_value (other)` | %1$s Dateien · %2$s | %1$s files · %2$s |
| 826 | `stats_per_country` | Pro Land (aus GPS) | Per country (from GPS) |
| 827 | `first_sync_title` | Erste Synchronisierung | First sync |
| 828 | `first_sync_text` | Die erste Synchronisierung überträgt alle vorhandenen Dateien. Je nach Menge dauert sie lange und braucht viel Akku. Schliesse das Handy am besten ans Ladegerät an. | The first sync transfers all existing files. Depending on the amount it takes a long time and uses a lot of battery. Best connect the mobile to the charger. |
| 829 | `first_sync_start` | Trotzdem starten | Start anyway |
| 830 | `dog_waiting` | Wartet | Waiting |
| 831 | `dog_idle` | Alles gesichert | Everything backed up |
| 832 | `dog_syncing` | Synchronisiert | Syncing |
| 833 | `update_checking` | Suche nach Updates… | Checking for updates… |
| 834 | `update_latest` | Du hast die neueste Version (%1$s). | You have the latest version (%1$s). |
| 835 | `update_downloading` | Lade Version %1$s… | Downloading version %1$s… |
| 836 | `update_installing` | Installation wird gestartet… | Starting installation… |
| 837 | `update_error` | Fehler: %1$s | Error: %1$s |
| 838 | `update_installed` | Installiert | Installed |
| 839 | `update_source` | Quelle: github.com/%1$s (Releases) | Source: github.com/%1$s (releases) |
| 840 | `update_last_check` | Zuletzt geprüft: %1$s | Last checked: %1$s |
| 841 | `update_daily` | Täglich nach Updates suchen | Check for updates daily |
| 842 | `update_daily_sub` | Nur im WLAN, Meldung per Benachrichtigung | Only on Wi-Fi, notification when found |
| 843 | `update_available` | Version %1$s verfügbar | Version %1$s available |
| 844 | `update_size` | Grösse: %1$s | Size: %1$s |
| 845 | `update_permission_hint` | Android muss LeoSync einmalig erlauben, Apps zu installieren. Danach mit «Zurück» hierher zurückkehren. | Android has to allow LeoSync once to install apps. Then use Back to return here. |
| 846 | `update_permission` | Erlaubnis erteilen | Grant permission |
| 847 | `update_install` | Herunterladen und installieren | Download and install |
| 848 | `update_install_hint` | Android fragt zur Bestätigung. Nach dem Update LeoSync wieder öffnen. | Android asks for confirmation. After the update, open LeoSync again. |
| 849 | `run_dangling_log` | Unterbrochen – die App wurde beendet (z. B. durch ein Update). Der Rest folgt bei der nächsten Synchronisierung. | Interrupted – the app was closed (e.g. by an update). The rest follows at the next sync. |
| 850 | `run_dangling` | Unterbrochen – App wurde beendet (z. B. Update), Rest folgt bei der nächsten Synchronisierung | Interrupted – app was closed (e.g. update), rest follows at the next sync |
| 851 | `update_offline` | Update-Prüfung nicht möglich. Keine Verbindung zum Internet. Verbinde dich mit dem WLAN und versuche es erneut. | Could not check for updates. No internet connection. Connect to Wi-Fi and try again. |
| 852 | `update_check_again` | Erneut prüfen | Check again |
| 853 | `update_check_now` | Jetzt prüfen | Check now |
| 854 | `update_changes_link` | Änderungen auf GitHub ansehen | View changes on GitHub |
| 855 | `update_download_cancelled` | Download abgebrochen. | Download cancelled. |

## Benachrichtigungen

| Nr. | Schlüssel | Deutsch | Englisch |
|---:|---|---|---|
| 856 | `notif_channel_progress` | Synchronisierung | Sync |
| 857 | `notif_channel_progress_desc` | Fortschritt der Übertragung | Transfer progress |
| 858 | `notif_channel_alerts_desc` | Ordnerauswahl, Löschbestätigung, Fehler | Folder selection, delete confirmation, errors |
| 859 | `notif_progress_title` | %1$s: %2$s / %3$s Dateien | %1$s: %2$s / %3$s files |
| 860 | `notif_progress_text` | %1$s von %2$s (%3$s %%) | %1$s of %2$s (%3$s %%) |
| 861 | `notif_summary_problem` | LeoSync – Problem | LeoSync – problem |
| 862 | `notif_summary_done` | LeoSync – fertig | LeoSync – done |
| 863 | `notif_decision_text (one)` | %1$s Datei in %2$s wartet: Es gibt umbenannte Monatsordner. Tippen zum Zuordnen. | %1$s file in %2$s is waiting: there are renamed month folders. Tap to assign. |
| 864 | `notif_decision_text (other)` | %1$s Dateien in %2$s warten: Es gibt umbenannte Monatsordner. Tippen zum Zuordnen. | %1$s files in %2$s are waiting: there are renamed month folders. Tap to assign. |
| 865 | `notif_delete_text (one)` | %1$s Datei ist verifiziert gesichert. Tippen, um sie vom Handy zu entfernen. | %1$s file is verified as backed up. Tap to remove it from the mobile. |
| 866 | `notif_delete_text (other)` | %1$s Dateien sind verifiziert gesichert. Tippen, um sie vom Handy zu entfernen. | %1$s files are verified as backed up. Tap to remove them from the mobile. |
| 867 | `notif_wait_wifi` | Wartet auf WLAN | Waiting for Wi-Fi |
| 868 | `notif_wait_wifi_named` | Wartet auf WLAN „%1$s“ | Waiting for Wi-Fi “%1$s” |
| 869 | `notif_paused_files` | %1$s / %2$s Dateien | %1$s / %2$s files |
| 870 | `notif_conflict_text (one)` | %1$s Datei wurde auf Handy und PC geändert. Tippen, um zu entscheiden, welche Version gilt. | %1$s file was changed on mobile and PC. Tap to decide which version wins. |
| 871 | `notif_conflict_text (other)` | %1$s Dateien wurden auf Handy und PC geändert. Tippen, um zu entscheiden, welche Version gilt. | %1$s files were changed on mobile and PC. Tap to decide which version wins. |
| 872 | `notif_guard_title` | LeoSync wartet auf dich | LeoSync is waiting for you |
| 873 | `notif_guard_text` | Die Ordner-Synchronisierung „%1$s“ wurde zur Sicherheit angehalten. Tippen, um zu entscheiden. | The folder sync “%1$s” was stopped for safety. Tap to decide. |
| 874 | `notif_stale_title (one)` | Seit %1$s Tag nicht gesichert | Not backed up for %1$s day |
| 875 | `notif_stale_title (other)` | Seit %1$s Tagen nicht gesichert | Not backed up for %1$s days |
| 876 | `notif_stale_text` | Ist das Heim-WLAN verbunden und der PC eingeschaltet? Tippen zum Öffnen. | Is the home Wi-Fi connected and the PC switched on? Tap to open. |
| 877 | `notif_update_text` | LeoSync %1$s ist verfügbar. Tippen zum Installieren. | LeoSync %1$s is available. Tap to install. |
| 878 | `notif_installed_title` | Update installiert | Update installed |
| 879 | `notif_installed_text` | LeoSync wurde auf Version %1$s aktualisiert. Tippen zum Öffnen. | LeoSync was updated to version %1$s. Tap to open. |

## Sync-Ablauf, Verlauf-Texte, Fehlermeldungen

| Nr. | Schlüssel | Deutsch | Englisch |
|---:|---|---|---|
| 880 | `trigger_auto` | Automatisch | Automatic |
| 881 | `trigger_manual` | Manuell | Manual |
| 882 | `trigger_wifi` | WLAN verbunden | Wi-Fi connected |
| 883 | `trigger_resume` | Fortsetzen nach WLAN-Unterbruch | Resume after Wi-Fi interruption |
| 884 | `trigger_resume_wifi` | Fortsetzen nach WLAN-Unterbruch (%1$s) | Resume after Wi-Fi interruption (%1$s) |
| 885 | `trigger_retry` | Erneute Prüfung (PC war nicht erreichbar) | Check again (PC was not reachable) |
| 886 | `trigger_poll` | Ordner-Prüfung (Intervall) | Folder check (interval) |
| 887 | `trigger_safety` | Sicherheitsnetz (%1$s h ohne Synchronisierung) | Safety net (%1$s h without sync) |
| 888 | `trigger_followup` | Nachholen (Mindestalter) | Catch up (minimum age) |
| 889 | `trigger_new_media` | Neue Fotos/Videos | New photos/videos |
| 890 | `trigger_pause_end` | Pause beendet | Pause ended |
| 891 | `trigger_resumed` | Fortgesetzt | Resumed |
| 892 | `trigger_deferred` | Nachgeholt (Start während einer laufenden Synchronisierung) | Caught up (start requested during a running sync) |
| 893 | `trigger_generic` | Auslöser | Trigger |
| 894 | `stop_wifi` | WLAN getrennt | Wi-Fi disconnected |
| 895 | `stop_charger` | nicht mehr am Ladegerät | no longer on the charger |
| 896 | `stop_timeout` | Zeitlimit von Android | Android time limit |
| 897 | `stop_system` | vom System gestoppt (%1$s) | stopped by the system (%1$s) |
| 898 | `stop_android` | von Android gestoppt (%1$s) | stopped by Android (%1$s) |
| 899 | `skip_manual_only` | Automatische Synchronisierungen ausgeschaltet (nur manuell) | Automatic syncs turned off (manual only) |
| 900 | `skip_paused_until` | Pausiert bis %1$s | Paused until %1$s |
| 901 | `skip_ssid_unreadable` | WLAN-Name nicht lesbar – Standort „Immer zulassen“ und Standortdienst prüfen | Wi-Fi name not readable – check location “Allow all the time” and the location service |
| 902 | `skip_ssid_unknown` | WLAN „%1$s“ ist in keinem Profil eingetragen | Wi-Fi “%1$s” is not set in any profile |
| 903 | `skip_pc_unreachable` | PC nicht erreichbar: %1$s | PC not reachable: %1$s |
| 904 | `skip_pc_not_recognised` | PC nicht erkannt: %1$s | PC not recognised: %1$s |
| 905 | `skip_pc_failed` | Verbindung zum PC nicht möglich: %1$s | Connection to the PC not possible: %1$s |
| 906 | `wait_for_pc` | Warte auf PC (%1$s) – Versuch %2$s/%3$s… | Waiting for PC (%1$s) – attempt %2$s/%3$s… |
| 907 | `summary_failed` | Synchronisierung fehlgeschlagen – %1$s | Sync failed – %1$s |
| 908 | `summary_transferred (one)` | %1$s Datei übertragen (%2$s) | %1$s file transferred (%2$s) |
| 909 | `summary_transferred (other)` | %1$s Dateien übertragen (%2$s) | %1$s files transferred (%2$s) |
| 910 | `summary_errors (one)` | %1$s Fehler – Details im Verlauf | %1$s error – details in the history |
| 911 | `summary_errors (other)` | %1$s Fehler – Details im Verlauf | %1$s errors – details in the history |
| 912 | `summary_no_space` | Zu wenig Speicher auf dem Handy – einige Dateien wurden nicht heruntergeladen. | Not enough storage on the mobile – some files were not downloaded. |
| 913 | `summary_pc_unreachable` | PC nicht erreichbar (%1$s). Ist er eingeschaltet und die Freigabe aktiv? | PC not reachable (%1$s). Is it switched on and the Share active? |
| 914 | `paused_connection_retry` | Verbindung unterbrochen – neuer Versuch in %1$s Minuten | Connection interrupted – next attempt in %1$s minutes |
| 915 | `run_nothing` | Nichts zu tun | Nothing to do |
| 916 | `run_uploaded` | %1$s übertragen | %1$s transferred |
| 917 | `run_found` | %1$s bereits vorhanden | %1$s already present |
| 918 | `run_failed (one)` | %1$s Fehler | %1$s error |
| 919 | `run_failed (other)` | %1$s Fehler | %1$s errors |
| 920 | `run_waiting (one)` | %1$s wartet auf Ordnerauswahl | %1$s waiting for folder selection |
| 921 | `run_waiting (other)` | %1$s warten auf Ordnerauswahl | %1$s waiting for folder selection |
| 922 | `run_up` | ↑ %1$s hochgeladen | ↑ %1$s uploaded |
| 923 | `run_down` | ↓ %1$s heruntergeladen | ↓ %1$s downloaded |
| 924 | `run_trashed` | %1$s in Papierkorb | %1$s in trash |
| 925 | `run_conflicts (one)` | %1$s Konflikt | %1$s conflict |
| 926 | `run_conflicts (other)` | %1$s Konflikte | %1$s conflicts |
| 927 | `run_space_full` | Speicher voll | storage full |
| 928 | `guard_title_remote` | PC-Ordner nicht gefunden | PC folder not found |
| 929 | `guard_title_local` | Handy-Ordner nicht gefunden | Mobile folder not found |
| 930 | `guard_title_mass` | Viele Dateien würden gelöscht | Many files would be deleted |
| 931 | `guard_text_remote` | Der PC-Ordner von „%1$s“ fehlt oder ist leer, obwohl schon synchronisiert wurde. Wurde er umbenannt oder verschoben? Bis du entscheidest, ruht dieses Profil. | The PC folder of “%1$s” is missing or empty although it was already synced. Was it renamed or moved? Until you decide, this profile rests. |
| 932 | `guard_text_local` | Der Handy-Ordner von „%1$s“ fehlt oder ist leer, obwohl schon synchronisiert wurde. Wurde er umbenannt oder verschoben? Bis du entscheidest, ruht dieses Profil. | The mobile folder of “%1$s” is missing or empty although it was already synced. Was it renamed or moved? Until you decide, this profile rests. |
| 933 | `guard_text_folder (one)` | In „%1$s“ würde der ganze Ordner „%2$s“ in den Papierkorb verschoben (insgesamt %3$s Datei). Wurde er wirklich gelöscht? Bis du entscheidest, ruht dieses Profil. | In “%1$s” the whole folder “%2$s” would be moved to the trash (%3$s file in total). Was it really deleted? Until you decide, this profile rests. |
| 934 | `guard_text_folder (other)` | In „%1$s“ würde der ganze Ordner „%2$s“ in den Papierkorb verschoben (insgesamt %3$s Dateien). Wurde er wirklich gelöscht? Bis du entscheidest, ruht dieses Profil. | In “%1$s” the whole folder “%2$s” would be moved to the trash (%3$s files in total). Was it really deleted? Until you decide, this profile rests. |
| 935 | `guard_text_mass` | In „%1$s“ würden %2$s von %3$s Dateien in den Papierkorb verschoben. Wurde wirklich so viel gelöscht? Bis du entscheidest, ruht dieses Profil. | In “%1$s”, %2$s of %3$s files would be moved to the trash. Was that much really deleted? Until you decide, this profile rests. |
| 936 | `guard_text_check` | Der gewählte Ordner von „%1$s“ passt nicht zum bisherigen Synchronisierungsstand. Bis du entscheidest, ruht dieses Profil. | The chosen folder of “%1$s” does not match the previous sync state. Until you decide, this profile rests. |
| 937 | `delete_log_done` | Gelöscht: %1$s | Deleted: %1$s |
| 938 | `delete_log_failed` | Löschen fehlgeschlagen: %1$s – %2$s | Deleting failed: %1$s – %2$s |
| 939 | `delete_log_pc_missing` | Nicht vom Handy entfernt: Die Kopie auf dem PC fehlt oder weicht ab, die Datei wird erneut gesichert: %1$s | Not removed from the phone: the copy on the PC is missing or differs, the file will be backed up again: %1$s |
| 940 | `delete_log_phone_unreadable` | Nicht vom Handy entfernt: Die Datei ist für die Prüfung nicht lesbar: %1$s | Not removed from the phone: the file cannot be read for the check: %1$s |
| 941 | `delete_pc_unreachable` | PC nicht erreichbar – nichts gelöscht. Die Dateien bleiben vorgemerkt. | PC not reachable – nothing deleted. The files stay queued. |
| 942 | `delete_pc_rejected` | PC nicht erkannt – nichts gelöscht. %1$s | PC not recognised – nothing deleted. %1$s |
| 943 | `delete_pc_reset` | %1$d Dateien nicht entfernt: Die Kopie auf dem PC fehlt oder weicht ab. Sie werden erneut gesichert. | %1$d files not removed: the copy on the PC is missing or differs. They will be backed up again. |
| 944 | `update_err_404` | Kein Release gefunden (Repository „%1$s“ privat, falsch geschrieben oder noch ohne Release) | No release found (repository “%1$s” private, misspelled or without a release yet) |
| 945 | `update_err_403` | GitHub-Abfragelimit erreicht – später erneut versuchen | GitHub request limit reached – try again later |
| 946 | `update_err_http` | GitHub antwortet mit HTTP %1$s | GitHub responds with HTTP %1$s |
| 947 | `update_err_no_apk` | Release %1$s enthält keine APK-Datei | Release %1$s contains no APK file |
| 948 | `update_err_repo` | Kein gültiges GitHub-Repository eingestellt (Format: benutzer/repo) | No valid GitHub repository set (format: user/repo) |
| 949 | `update_err_invalid_apk` | Die heruntergeladene Datei ist keine gültige APK | The downloaded file is not a valid APK |
| 950 | `update_err_package` | Die APK gehört zu „%1$s“, nicht zu dieser App | The APK belongs to “%1$s”, not to this app |
| 951 | `update_err_debug` | (Debug-Builds lassen sich nicht per Update aktualisieren) | (debug builds cannot be updated this way) |
| 952 | `update_err_not_newer` | Die APK ist nicht neuer als die installierte Version | The APK is not newer than the installed version |
| 953 | `err_friendly_login` | Benutzer oder Passwort stimmt nicht. Bitte im Profil prüfen (bei einem Microsoft-Konto die E-Mail-Adresse als Benutzer). | User name or password is wrong. Please check the profile (for a Microsoft account use the e-mail address as user). |
| 954 | `err_friendly_share_missing` | Diese Freigabe gibt es auf dem PC nicht. Prüfe die Schreibweise oder wähle sie mit „Ordner auf dem PC wählen…“. | This Share does not exist on the PC. Check the spelling or pick it with “Choose folder on the PC…”. |
| 955 | `err_friendly_folder_missing` | Der Ordner wurde auf dem PC nicht gefunden. Wurde er umbenannt oder verschoben? | The folder was not found on the PC. Was it renamed or moved? |
| 956 | `err_friendly_access` | Kein Zugriff auf diesen Ordner. Erlaube dem Benutzer auf dem PC Lesen und Schreiben (Eigenschaften → Freigabe). | No access to this folder. Allow the user to read and write on the PC (Properties → Sharing). |
| 957 | `err_friendly_pc_full` | Auf dem PC ist kein Speicherplatz mehr frei. | The PC has no free storage left. |
| 958 | `err_friendly_phone_full` | Auf dem Handy ist kein Speicherplatz mehr frei. | The mobile has no free storage left. |
| 959 | `err_friendly_unknown_host` | Der PC-Name wurde nicht gefunden. Trage am besten die IP-Adresse ein, z. B. \\192.168.1.20. | The PC name was not found. Best enter the IP address, e.g. \\192.168.1.20. |
| 960 | `err_friendly_unreachable` | Der PC ist nicht erreichbar. Ist er eingeschaltet und im selben WLAN? | The PC is not reachable. Is it switched on and on the same Wi-Fi? |
| 961 | `smb_err_path` | Ungültiger Freigabe-Pfad: „%1$s“ (erwartet \\server\freigabe) | Invalid Share path: “%1$s” (expected \\server\share) |
| 962 | `smb_err_not_disk` | „%1$s“ ist keine Datei-Freigabe | “%1$s” is not a file Share |
| 963 | `smb_err_incomplete` | Upload unvollständig: %1$s von %2$s Bytes | Upload incomplete: %1$s of %2$s bytes |
| 964 | `smb_err_resume` | Fortgesetzter Upload passt nicht zur Datei – wird neu übertragen | Resumed upload does not match the file – will be transferred again |
| 965 | `smb_err_pc_changed` | Unter %1$s antwortet ein anderer PC als bisher. Es wurden keine Anmeldedaten gesendet. Mit „Verbindung testen“ prüfen und den neuen PC übernehmen. | A different PC answers at %1$s than before. No sign-in data was sent. Use “Test connection” to check it and trust the new PC. |
| 966 | `smb_err_pc_unknown` | Der PC unter %1$s ist noch nicht erkannt. Es wurden keine Anmeldedaten gesendet. Profil öffnen und „Verbindung testen“ tippen (oder das Profil speichern), damit die App den PC kennenlernt. | The PC at %1$s is not recognised yet. No sign-in data was sent. Open the profile and tap “Test connection” (or save the profile) so the app learns the PC. |
| 967 | `smb_err_pc_wrong_net` | Der PC unter %1$s ist noch nicht erkannt, und das aktuelle WLAN steht nicht in der Liste des Profils (oder der WLAN-Name ist nicht lesbar). Es wurden keine Anmeldedaten gesendet. Im eingetragenen WLAN „Verbindung testen“ tippen, den Standortzugriff erlauben oder die PC-Kennung auf „Nur PC-Kennung“ stellen. | The PC at %1$s is not recognised yet, and the current Wi-Fi is not in the profile’s list (or the Wi-Fi name cannot be read). No sign-in data was sent. Tap “Test connection” in the listed Wi-Fi, allow location access, or set the PC ID to “PC ID only”. |
| 968 | `smb_err_pc_no_id` | Der PC unter %1$s liefert keine Kennung und kann nicht erkannt werden. Es wurden keine Anmeldedaten gesendet. Die PC-Kennung auf „Nur WLAN-Name“ stellen. | The PC at %1$s provides no ID, so it cannot be recognised. No sign-in data was sent. Set the PC ID to “Wi-Fi name only”. |
| 969 | `smb_err_encryption_required` | Für dieses Profil ist die Verschlüsselung erforderlich, aber der PC unter %1$s bietet keine SMB-3-Verschlüsselung an. Es wurden keine Anmeldedaten gesendet. Die Verschlüsselung im Profil ändern oder am PC die SMB-Verschlüsselung aktivieren. | Encryption is required for this profile, but the PC at %1$s does not offer SMB 3 encryption. No sign-in data was sent. Change the encryption setting of the profile or enable SMB encryption on the PC. |
| 970 | `smb_err_encryption_not_established` | Für dieses Profil ist die Verschlüsselung erforderlich, aber die Verbindung zu %1$s konnte nicht verschlüsselt werden. Die Verbindung wurde getrennt, es wurden keine Daten übertragen. Die Verschlüsselung im Profil ändern oder am PC die SMB-Verschlüsselung prüfen. | Encryption is required for this profile, but the connection to %1$s could not be encrypted. The connection was closed and no data was transferred. Change the encryption setting of the profile or check SMB encryption on the PC. |
| 971 | `smb_test_missing_folder` | Verbunden, aber Ordner „%1$s“ existiert nicht (wird bei der ersten Synchronisierung angelegt). | Connected, but folder “%1$s” does not exist (will be created at the first sync). |
| 972 | `smb_test_ok (one)` | Verbindung OK – %1$s (%2$s Eintrag) | Connection OK – %1$s (%2$s entry) |
| 973 | `smb_test_ok (other)` | Verbindung OK – %1$s (%2$s Einträge) | Connection OK – %1$s (%2$s entries) |
| 974 | `backup_err_write` | Datei kann nicht geschrieben werden | File cannot be written |
| 975 | `backup_err_read` | Datei kann nicht gelesen werden | File cannot be read |
| 976 | `backup_err_version` | Sicherung hat Format-Version %1$s – diese App kennt bis Version %2$s. Bitte LeoSync aktualisieren. | Backup has format version %1$s – this app knows up to version %2$s. Please update LeoSync. |
| 977 | `backup_err_json` | Die Datei ist keine LeoSync-Sicherung (kein gültiges JSON). | The file is not a LeoSync backup (no valid JSON). |
| 978 | `backup_err_format` | Die Datei ist keine LeoSync-Sicherung. | The file is not a LeoSync backup. |
| 979 | `stale_pending (one)` | %1$s: %2$s neue Datei wartet, letzte Sicherung %3$s | %1$s: %2$s new file waiting, last backup %3$s |
| 980 | `stale_pending (other)` | %1$s: %2$s neue Dateien warten, letzte Sicherung %3$s | %1$s: %2$s new files waiting, last backup %3$s |
| 981 | `stale_last` | %1$s: letzte Sicherung %2$s | %1$s: last backup %2$s |
| 982 | `space_profile_id` | Profil %1$s | Profile %1$s |
| 983 | `tree_err_unreadable` | Handy-Ordner nicht lesbar | Mobile folder not readable |
| 984 | `tree_err_mkdir` | Ordner „%1$s“ konnte nicht angelegt werden | Folder “%1$s” could not be created |
| 985 | `tree_err_read` | Datei nicht lesbar | File not readable |
| 986 | `tree_err_write` | Datei nicht beschreibbar | File not writable |
| 987 | `tree_err_rename` | Umbenennen von „%1$s“ fehlgeschlagen | Renaming “%1$s” failed |
| 988 | `tree_err_size` | Grösse nach Download stimmt nicht (%1$s statt %2$s Bytes) | Size after download does not match (%1$s instead of %2$s bytes) |
| 989 | `tree_err_move` | Verschieben nicht unterstützt | Moving not supported |
| 990 | `tree_err_changed` | Datei wurde während des Laufs geändert | File was changed during the run |
| 991 | `tree_err_create` | Datei „%1$s“ konnte nicht angelegt werden | File “%1$s” could not be created |
| 992 | `eng_start_mirror` | Start (%1$s) – Ordner-Synchronisierung „%2$s“ (%3$s) | Start (%1$s) – folder sync “%2$s” (%3$s) |
| 993 | `eng_reading_folders` | Lese Ordner… | Reading folders… |
| 994 | `eng_local_missing` | Handy-Ordner nicht gefunden – bitte im Profil neu auswählen | Mobile folder not found – please choose it again in the profile |
| 995 | `eng_offline_preview` | Offline-Vorschau (Freigabe nicht erreichbar): %1$s | Offline preview (Share not reachable): %1$s |
| 996 | `eng_counts` | Handy: %1$s Dateien, PC: %2$s Dateien | Mobile: %1$s files, PC: %2$s files |
| 997 | `eng_keep_files` | Löschungen werden in diesem Lauf nicht übernommen (deine Entscheidung) | Deletions are not applied in this run (your decision) |
| 998 | `eng_comparing` | Vergleiche… | Comparing… |
| 999 | `eng_would_stop` | ⚠ Der Lauf würde anhalten und nachfragen: %1$s | ⚠ The run would stop and ask: %1$s |
| 1000 | `eng_would_trash_folder` | Der Ordner „%1$s“ käme ganz in den Papierkorb | The folder “%1$s” would go to the trash completely |
| 1001 | `eng_would_trash_files` | %1$s von %2$s Dateien kämen in den Papierkorb | %1$s of %2$s files would go to the trash |
| 1002 | `eng_actions_first` | %1$s Aktionen – die ersten %2$s simuliert | %1$s actions – the first %2$s simulated |
| 1003 | `eng_actions_all` | %1$s Aktionen simuliert | %1$s actions simulated |
| 1004 | `eng_wifi_lost` | Abgebrochen: WLAN nicht mehr verbunden | Cancelled: Wi-Fi no longer connected |
| 1005 | `eng_interrupted_log` | Unterbrochen (%1$s) – wird automatisch fortgesetzt | Interrupted (%1$s) – resumes automatically |
| 1006 | `eng_interrupted_mirror` | Unterbrochen (%1$s) – ↑ %2$s / ↓ %3$s übertragen, Rest folgt automatisch | Interrupted (%1$s) – ↑ %2$s / ↓ %3$s transferred, the rest follows automatically |
| 1007 | `eng_paused_log` | Pausiert (von dir) – läuft beim nächsten automatischen Start weiter | Paused (by you) – continues at the next automatic start |
| 1008 | `eng_connection_log_mirror` | Verbindung unterbrochen (%1$s) – wird bei der nächsten Synchronisierung fortgesetzt | Connection interrupted (%1$s) – continues at the next sync |
| 1009 | `eng_connection_mirror` | Verbindung unterbrochen – ↑ %1$s / ↓ %2$s übertragen, Rest folgt bei der nächsten Synchronisierung | Connection interrupted – ↑ %1$s / ↓ %2$s transferred, the rest follows at the next sync |
| 1010 | `eng_run_failed` | Lauf fehlgeschlagen: %1$s | Run failed: %1$s |
| 1011 | `eng_waiting_decision` | Wartet auf deine Entscheidung: %1$s | Waiting for your decision: %1$s |
| 1012 | `eng_trash_local` | 🗑 Handy: %1$s (auf dem PC gelöscht) | 🗑 Mobile: %1$s (deleted on the PC) |
| 1013 | `eng_trash_remote` | 🗑 PC: %1$s (auf dem Handy gelöscht) | 🗑 PC: %1$s (deleted on the mobile) |
| 1014 | `eng_conflict` | ⚠ Konflikt: %1$s wurde auf Handy und PC geändert | ⚠ Conflict: %1$s was changed on mobile and PC |
| 1015 | `eng_transfer_mirror (one)` | Übertrage %1$s Datei mit %2$s parallelen Verbindungen | Transferring %1$s file with %2$s parallel connections |
| 1016 | `eng_transfer_mirror (other)` | Übertrage %1$s Dateien mit %2$s parallelen Verbindungen | Transferring %1$s files with %2$s parallel connections |
| 1017 | `eng_no_space` | Zu wenig Speicher auf dem Handy – übersprungen: %1$s | Not enough storage on the mobile – skipped: %1$s |
| 1018 | `eng_on_pc` | auf dem PC | on the PC |
| 1019 | `eng_on_phone` | auf dem Handy | on the mobile |
| 1020 | `eng_err_renamed_missing` | Umbenannte Datei nicht gefunden | Renamed file not found |
| 1021 | `eng_err_after_upload_missing` | Datei nach Upload nicht gefunden | File not found after upload |
| 1022 | `eng_err_after_upload_size` | Grösse nach Upload stimmt nicht | Size after upload does not match |
| 1023 | `eng_err_after_download_missing` | Datei nach Download nicht gefunden | File not found after download |
| 1024 | `eng_err_after_download_size` | Grösse nach Download stimmt nicht | Size after download does not match |
| 1025 | `eng_resumed_at` | (fortgesetzt ab %1$s) | (resumed from %1$s) |
| 1026 | `eng_restoring (one)` | Stelle %1$s Datei aus dem Papierkorb wieder her | Restoring %1$s file from the trash |
| 1027 | `eng_restoring (other)` | Stelle %1$s Dateien aus dem Papierkorb wieder her | Restoring %1$s files from the trash |
| 1028 | `eng_restore_gone` | ↺ %1$s: Version im Papierkorb nicht mehr vorhanden – übersprungen | ↺ %1$s: version in the trash no longer present – skipped |
| 1029 | `eng_err_restored_missing` | Wiederhergestellte Datei nicht gefunden | Restored file not found |
| 1030 | `eng_restored` | ↺ %1$s wiederhergestellt (Version vom %2$s) | ↺ %1$s restored (version from %2$s) |
| 1031 | `eng_restore_failed_rolled_back` | ✗ Wiederherstellen %1$s: %2$s – zurückgerollt, wird bei der nächsten Synchronisierung erneut versucht | ✗ Restore %1$s: %2$s – rolled back, will be tried again at the next sync |
| 1032 | `eng_restore_failed_partial` | ✗ Wiederherstellen %1$s: %2$s – nicht vollständig zurückgerollt, siehe Papierkorb | ✗ Restore %1$s: %2$s – not completely rolled back, see trash |
| 1033 | `eng_restore_gave_up` | ✗ Wiederherstellen %1$s: %2$s – zurückgerollt; nach %3$d Versuchen nicht mehr automatisch wiederholt. Vormerkung und Papierkorb-Version bleiben erhalten; auf der Startseite „Erneut versuchen“ oder „Verwerfen“ wählen | ✗ Restore %1$s: %2$s – rolled back; not tried again automatically after %3$d attempts. The request and the trash version are kept; choose “Try again” or “Discard” on the home screen |
| 1034 | `eng_purged_pc` | 🗑 PC-Papierkorb geleert (vorgemerkt): %1$s Tagesordner, %2$s einzelne Dateien endgültig gelöscht | 🗑 PC trash emptied (scheduled): %1$s day folders, %2$s single files permanently deleted |
| 1035 | `eng_purge_pc_failed` | ✗ PC-Papierkorb leeren: %1$s | ✗ Emptying PC trash: %1$s |
| 1036 | `eng_changed_during_run` | ⏭ %1$s: wurde während des Laufs %2$s geändert – folgt bei der nächsten Synchronisierung | ⏭ %1$s: was changed %2$s during the run – follows at the next sync |
| 1037 | `eng_syncing` | Synchronisiere… | Syncing… |
| 1038 | `eng_purge_local_failed` | Papierkorb auf dem Handy nicht geleert: %1$s | Trash on the mobile not emptied: %1$s |
| 1039 | `eng_purge_remote_failed` | Papierkorb auf dem PC nicht geleert: %1$s | Trash on the PC not emptied: %1$s |
| 1040 | `eng_purged_expired` | 🗑 Papierkorb: %1$s Ordner älter als %2$s Tage endgültig gelöscht | 🗑 Trash: %1$s folders older than %2$s days permanently deleted |
| 1041 | `eng_pv_upload_trash` | ↑ Handy → PC (PC-Version in Papierkorb) | ↑ Mobile → PC (PC version to trash) |
| 1042 | `eng_pv_upload` | ↑ Handy → PC | ↑ Mobile → PC |
| 1043 | `eng_pv_download_trash` | ↓ PC → Handy (Handy-Version in Papierkorb) | ↓ PC → Mobile (mobile version to trash) |
| 1044 | `eng_pv_download` | ↓ PC → Handy | ↓ PC → Mobile |
| 1045 | `eng_pv_trash_local` | 🗑 Auf dem Handy in den Papierkorb | 🗑 To the trash on the mobile |
| 1046 | `eng_pv_trash_remote` | 🗑 Auf dem PC in den Papierkorb | 🗑 To the trash on the PC |
| 1047 | `eng_pv_conflict` | ⚠ Konflikt – Entscheidung nötig | ⚠ Conflict – decision needed |
| 1048 | `eng_pv_keep_both` | ⇄ Beide Versionen behalten | ⇄ Keep both versions |
| 1049 | `eng_end` | Ende: %1$s | End: %1$s |
| 1050 | `eng_already_in (one)` | = %1$s Datei bereits in %2$s vorhanden | = %1$s file already present in %2$s |
| 1051 | `eng_already_in (other)` | = %1$s Dateien bereits in %2$s vorhanden | = %1$s files already present in %2$s |
| 1052 | `eng_start_photos` | Start (%1$s) – Profil „%2$s“ → %3$s | Start (%1$s) – profile “%2$s” → %3$s |
| 1053 | `eng_searching` | Suche Dateien… | Searching files… |
| 1054 | `eng_filtered (one)` | %1$s Datei durch Filter ausgeschlossen | %1$s file excluded by filter |
| 1055 | `eng_filtered (other)` | %1$s Dateien durch Filter ausgeschlossen | %1$s files excluded by filter |
| 1056 | `eng_found (one)` | %1$s Datei gefunden, %2$s neu, %3$s bereits gesichert | %1$s file found, %2$s new, %3$s already backed up |
| 1057 | `eng_found (other)` | %1$s Dateien gefunden, %2$s neu, %3$s bereits gesichert | %1$s files found, %2$s new, %3$s already backed up |
| 1058 | `eng_too_young` | %1$s noch zu jung (< %2$s min) | %1$s still too young (< %2$s min) |
| 1059 | `eng_connecting` | Verbinde… | Connecting… |
| 1060 | `eng_no_geo` | Länderdaten fehlen – Ordner ohne Länderzusatz | Country data missing – folders without country suffix |
| 1061 | `eng_no_media_location` | Berechtigung „Medien-Standort“ fehlt – GPS wird nicht ausgewertet | Permission “media location” missing – GPS is not evaluated |
| 1062 | `eng_analysing` | Analysiere… | Analysing… |
| 1063 | `eng_new_files_first (one)` | %1$s neue Datei – die ersten %2$s simuliert | %1$s new file – the first %2$s simulated |
| 1064 | `eng_new_files_first (other)` | %1$s neue Dateien – die ersten %2$s simuliert | %1$s new files – the first %2$s simulated |
| 1065 | `eng_new_files_all (one)` | %1$s neue Datei simuliert | %1$s new file simulated |
| 1066 | `eng_new_files_all (other)` | %1$s neue Dateien simuliert | %1$s new files simulated |
| 1067 | `eng_transfer_photos` | Übertrage mit %1$s parallelen Verbindungen | Transferring with %1$s parallel connections |
| 1068 | `eng_analysis_failed` | ✗ %1$s: Analyse fehlgeschlagen: %2$s | ✗ %1$s: analysis failed: %2$s |
| 1069 | `eng_waiting_groups (one)` | %1$s Datei wartet auf Ordnerauswahl (%2$s) | %1$s file waiting for folder selection (%2$s) |
| 1070 | `eng_waiting_groups (other)` | %1$s Dateien warten auf Ordnerauswahl (%2$s) | %1$s files waiting for folder selection (%2$s) |
| 1071 | `eng_interrupted_photos` | Unterbrochen (%1$s) – %2$s übertragen, Rest folgt automatisch | Interrupted (%1$s) – %2$s transferred, the rest follows automatically |
| 1072 | `eng_paused_photos` | Pausiert (von dir) – %1$s übertragen, Rest beim nächsten automatischen Start | Paused (by you) – %1$s transferred, the rest at the next automatic start |
| 1073 | `eng_connection_log_photos` | Verbindung unterbrochen (%1$s) – offene Dateien werden bei der nächsten Synchronisierung übertragen | Connection interrupted (%1$s) – open files are transferred at the next sync |
| 1074 | `eng_connection_photos` | Verbindung unterbrochen – %1$s übertragen, Rest folgt bei der nächsten Synchronisierung | Connection interrupted – %1$s transferred, the rest follows at the next sync |
| 1075 | `eng_transferring` | Übertrage… | Transferring… |
| 1076 | `eng_err_unreadable` | Datei nicht lesbar: %1$s | File not readable: %1$s |
| 1077 | `eng_err_verify` | Prüfung nach Upload fehlgeschlagen – Datei wird beim nächsten Lauf erneut übertragen | Check after upload failed – the file will be transferred again at the next run |
| 1078 | `eng_err_verify_cleanup` | Die fehlerhafte Kopie auf dem PC konnte nicht entfernt werden (%1$s): %2$s | The faulty copy on the PC could not be removed (%1$s): %2$s |
| 1079 | `eng_present_unverified` | Bereits auf dem PC, aber Gleichheit nicht bestätigt – bleibt auf dem Handy: %1$s | Already on the PC, but not confirmed identical – stays on the phone: %1$s |
| 1080 | `tree_sd_card` | SD-Karte | SD card |
| 1081 | `tree_err_rename_short` | Umbenennen fehlgeschlagen | Renaming failed |
| 1082 | `update_err_no_tag` | Release ohne Versions-Tag | Release without version tag |
| 1083 | `update_err_download` | Download fehlgeschlagen (HTTP %1$s) | Download failed (HTTP %1$s) |
| 1084 | `decision_folder_path` | Ordnerpfad unter dem Jahr, z. B. %1$s (der letzte Ordner muss %2$s enthalten) | Folder path below the year, e.g. %1$s (the last folder must contain %2$s) |
| 1085 | `decision_err_one` | Bitte genau einen Ordnernamen angeben, der %1$s enthält, z. B. „%1$s Urlaub“. | Please enter a single folder name that contains %1$s, e.g. “%1$s Holiday”. |
| 1086 | `decision_err_two` | Bitte <Land>\<MM> angeben, z. B. „%1$s“. Der letzte Ordner muss die Monatszahl enthalten. | Please enter <country>\<MM>, e.g. “%1$s”. The last folder must contain the month number. |
| 1087 | `decision_err_empty` | Ordnernamen dürfen nicht leer sein, „.“ oder „..“ lauten. | Folder names must not be empty, “.” or “..”. |
| 1088 | `decision_err_month` | Der letzte Ordner muss die Monatszahl %1$s enthalten. | The last folder must contain the month number %1$s. |

## Hersteller-Hinweise

| Nr. | Schlüssel | Deutsch | Englisch |
|---:|---|---|---|
| 1089 | `vendor_oplus_1` | App-Info → Akkunutzung: „Vordergrundaktivität zulassen“, „Hintergrundaktivität zulassen“ und „Automatischer Start“ einschalten | App info → Battery usage: turn on “Allow foreground activity”, “Allow background activity” and “Auto launch” |
| 1090 | `vendor_oplus_2` | In der Übersicht der zuletzt genutzten Apps LeoSync sperren (Karte lange drücken → Sperren) | Lock LeoSync in the recent apps overview (long-press the card → Lock) |
| 1091 | `vendor_samsung_1` | App-Info → Akku: „Nicht eingeschränkt“ wählen | App info → Battery: choose “Unrestricted” |
| 1092 | `vendor_samsung_2` | Einstellungen → Akku → Hintergrundnutzungslimits: LeoSync zu „Nie ruhende Apps“ hinzufügen | Settings → Battery → Background usage limits: add LeoSync to “Never sleeping apps” |
| 1093 | `vendor_xiaomi_1` | App-Info → „Autostart“ einschalten | App info → turn on “Autostart” |
| 1094 | `vendor_xiaomi_2` | App-Info → Energiesparmodus: „Keine Einschränkungen“ wählen | App info → Battery saver: choose “No restrictions” |
| 1095 | `vendor_xiaomi_3` | In der Übersicht der zuletzt genutzten Apps LeoSync sperren | Lock LeoSync in the recent apps overview |
| 1096 | `vendor_huawei_1` | Einstellungen → Akku → App-Start: LeoSync auf „Manuell verwalten“ stellen und alle drei Schalter einschalten | Settings → Battery → App launch: set LeoSync to “Manage manually” and turn on all three switches |

