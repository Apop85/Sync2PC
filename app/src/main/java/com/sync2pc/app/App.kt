package com.sync2pc.app

import android.app.Application
import android.content.res.Configuration
import com.sync2pc.app.diag.CrashReporter
import com.sync2pc.app.l10n.Texts
import com.sync2pc.app.sync.Notifications
import com.sync2pc.app.sync.SyncScheduler

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // Als Erstes, damit auch Abstürze beim Start festgehalten werden
        CrashReporter.install(this)
        // Vor allem anderen, das Einstellungen schreibt (erkennt so Neuinstallationen)
        com.sync2pc.app.data.GlobalSettings(this).applyInstallDefaults()
        Texts.init(this)
        // PC-Kennung: SmbSession liest/lernt den aktuellen DB-Stand (SYNC-20261109-047); Room öffnet die DB erst beim ersten Zugriff
        com.sync2pc.app.smb.SmbSession.pcStore =
            com.sync2pc.app.smb.RoomPcStore(com.sync2pc.app.data.AppDatabase.get(this).dao())
        Notifications.createChannels(this)
        SyncScheduler.setupAll(this)
    }

    /** Sprachwechsel (System oder App-Sprache ab Android 13): Texte und Kanalnamen neu laden */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Texts.init(this)
        Notifications.createChannels(this)
    }
}
