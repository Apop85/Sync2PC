package com.sync2pc.app

import android.app.Application
import com.sync2pc.app.diag.CrashReporter
import com.sync2pc.app.sync.Notifications
import com.sync2pc.app.sync.SyncScheduler

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // Als Erstes, damit auch Abstürze beim Start festgehalten werden
        CrashReporter.install(this)
        Notifications.createChannels(this)
        SyncScheduler.setupAll(this)
    }
}
